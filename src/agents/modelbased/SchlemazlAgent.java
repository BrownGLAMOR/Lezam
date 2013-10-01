/**
 * 
 */
package agents.modelbased;

import static models.paramest.ConstantsAndFunctions._advertiserEffectBoundsAvg;
import static models.paramest.ConstantsAndFunctions.getISRatio;
import static models.paramest.ConstantsAndFunctions.queryTypeToInt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import simulator.parser.GameStatusHandler.UserState;
import tacaa.javasim;

import clojure.lang.PersistentHashMap;

import models.AbstractModel;
import models.ISratio.ISRatioModel;
import models.adtype.AbstractAdTypeEstimator;
import models.adtype.AdTypeEstimator;
import models.advertiserspecialties.AbstractSpecialtyModel;
import models.advertiserspecialties.SimpleSpecialtyModel;
import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import models.budgetEstimator.AbstractBudgetEstimator;
import models.budgetEstimator.BudgetEstimator;
import models.paramest.AbstractParameterEstimation;
import models.paramest.BayesianParameterEstimation;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.MIPandLDS_QueryAnalyzer;
import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.usermodel.UserModel;
import models.usermodel.UserModelInput;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.UserClickModel;
import agents.AbstractAgent;

/**
 * @author betsy
 * September 2013
 * 
 * An agent to separate the prediction and optimization routines.
 *
 */
public class SchlemazlAgent extends AbstractAgent {

	double[] _c;
	protected HashMap<Query, Double> _baseConvProbs;
	private HashMap<Query, Double> _baseClickProbs;
	protected HashMap<Query, Double> _salesPrices;

	private AbstractQueryAnalyzer _queryAnalyzer;  //the following variables are all models 
	private UserModel _userModel;
	protected AbstractUnitsSoldModel _unitsSold;
	private AbstractBidModel _bidModel;
	protected AbstractParameterEstimation _paramEstimation;
	private AbstractBudgetEstimator _budgetEstimator;
	private ISRatioModel _ISRatioModel;
	private PersistentHashMap _baseCljSim;
	private PersistentHashMap _perfectCljSim = null;
	private String _agentToReplace;
	private AbstractAdTypeEstimator _adTypeEstimator;
	private AbstractSpecialtyModel _specialtyModel;
	
	private double _randJump,_yestBid,_5DayBid,_bidStdDev;
	
	private int _lowCapacThresh, _midCapacThresh, _highCapacThresh;
	
	protected HashMap<Integer,Double> _capMod;
	
	private HashMap<Integer,Double> _totalBudgets;
	
	protected double _budgetMult;
	protected double _probeBidMult;
	
	private static double USER_MODEL_UB_MULT;
	private boolean USER_MODEL_UB = true;
	
	private boolean RANKABLE = false;
	protected boolean RESET_BUDGET = true;
	protected int lagDays = 2;
	protected int pcount = 0;
	protected int SEED = 0;
	protected Random _R;
	
	
	public SchlemazlAgent(PersistentHashMap perfectSim, String agentToReplace) {
		this(.04,.12,.30);
		_perfectCljSim = perfectSim;
		_agentToReplace = agentToReplace;

	}


	public SchlemazlAgent(double c1, double c2, double c3) {
		this(c1,c2,c3,750,1000,1250,.2,.8);//HC num
	}

	public SchlemazlAgent(double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, 
			double lowBidMult, double highBidMult) {
		 _R = makeRandom(SEED);
		//read in parameters from .parameters file and update hardcoded values

		

		// can we move these parameters outside of this method now that we can set them from a file?
		_probeBidMult = 2.5;//in file
		_budgetMult = 1.0;//in file


		//new variables that are directly from the game design
		_lowCapacThresh=300;//in file
		_midCapacThresh=450;//in file
		_highCapacThresh=600;//in file

		_capMod = new HashMap<Integer, Double>();
		_capMod.put( _lowCapacThresh,1.0); //why are these hardcoded in now?//HC num
		_capMod.put( _midCapacThresh,1.0);
		_capMod.put( _highCapacThresh,1.0);
		//      _capMod.put(300,c1);
		//      _capMod.put(450,c2);
		//      _capMod.put(600,c3);

		USER_MODEL_UB_MULT = 1.45;
		_totalBudgets = new HashMap<Integer, Double>();
		//      _totalBudgets.put(300,Double.MAX_VALUE);
		//      _totalBudgets.put(450,Double.MAX_VALUE);
		//      _totalBudgets.put(600,Double.MAX_VALUE);
		_totalBudgets.put( _lowCapacThresh,750.0);//HC num
		_totalBudgets.put( _midCapacThresh,1000.0);
		_totalBudgets.put( _highCapacThresh,1250.0);
		_randJump = .1;//in file
		_yestBid = .5;//in file
		_5DayBid = .4;//in file
		_bidStdDev = 2.0;//in file

		// updateParams(paramFile);

//		try {
//			new File(bidsAndProbesDirname).mkdirs(); // create directory if it doesn't already exist.
//			String bidsAndProbesFilename = bidsAndProbesDirname + "bidsandprobes.txt"; 
//			bwriter= new BufferedWriter(new FileWriter(new File(bidsAndProbesFilename)));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}


	}


	/* (non-Javadoc)
	 * @see agents.AbstractAgent#initModels()
	 */
	@Override
	public Set<AbstractModel> initModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		//		_queryAnalyzer = new CarletonQueryAnalyzer(_querySpace,_advertisers,_advId,true,true);
		_queryAnalyzer = new MIPandLDS_QueryAnalyzer(_querySpace,_advertisers,_advId,true,true, SolverType.CP);
		//_queryAnalyzer = new MIPandLDS_QueryAnalyzer(_querySpace,_advertisers,_advId,true,true, SolverType.ERIC_MIP_MinSlotEric);
		//_queryAnalyzer = new MIPandLDS_QueryAnalyzer(_querySpace,_advertisers,_advId,true,true, SolverType.CARLETON_SIMPLE_MIP_Sampled);
		_userModel = UserModel.build(new UserModelInput(_c, _numSlots, _numPS), UserModel.ModelType.JBERG_PARTICLE_FILTER);
		//new jbergParticleFilter(_c,_numSlots,_numPS);
		_unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
		_bidModel = new IndependentBidModel(_advertisersSet, _advId,1,_randJump,_yestBid,_5DayBid,_bidStdDev,_querySpace);//HC num
		//      _bidModel = new JointDistBidModel(_advertisersSet, _advId, 8, .7, 1000);
		_paramEstimation = new BayesianParameterEstimation(_advIdx,_numSlots, _numPS, _squashing, _querySpace);
		_budgetEstimator = new BudgetEstimator(_querySpace,_advIdx,_numSlots,_numPS,_squashing);
		_ISRatioModel = new ISRatioModel(_querySpace,_numSlots);
		System.out.println("_______________________INITING MODELS__________________________________________");
		_adTypeEstimator = new AdTypeEstimator(_querySpace, _advertisersSet, _products);
		_specialtyModel = new SimpleSpecialtyModel(_querySpace, _advertisersSet, _products, _numSlots);
		//_queryAnalyzer = new MIPandLDS_QueryAnalyzer(_querySpace,_advertisers,_advId,true,true);
		models.add(_queryAnalyzer);
		models.add(_userModel);
		models.add(_unitsSold);
		models.add(_bidModel);
		models.add(_paramEstimation);
		models.add(_budgetEstimator);
		models.add(_ISRatioModel);
		models.add(_adTypeEstimator);
		models.add(_specialtyModel);

		return models;
	}

	
	/*
	 * This method updates all of our models. Ideally, this would not 
	 * be in this class as this is the optimization component of the agent.
	 */
	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {

		//      System.out.println("Updating models on day " + _day);
		if(!hasPerfectModels()) {
			System.out.println(_adTypeEstimator.toString());

			_adTypeEstimator.updateModel(queryReport);
			_specialtyModel.updateModel(queryReport);

			BidBundle bidBundle = _bidBundles.get(_bidBundles.size()-2);//HC num

			if(!USER_MODEL_UB) {
				_maxImps = new HashMap<Query,Integer>();
				for(Query q : _querySpace) {
					int numImps;
					if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
						numImps = MAX_F0_IMPS;
					}
					else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
						numImps = MAX_F1_IMPS;
					}
					else {
						numImps = MAX_F2_IMPS;
					}
					_maxImps.put(q, numImps);
				}
			}
			else {
				HashMap<Product,HashMap<UserState,Double>> preUpdateUserStates = getUserStates(_userModel,_products);
				_maxImps = getMaxImpsPred(preUpdateUserStates,USER_MODEL_UB_MULT,_querySpace);
			}

			_queryAnalyzer.updateModel(queryReport, bidBundle, _maxImps);

			HashMap<Query,Integer> totalImpressions = new HashMap<Query,Integer>();
			HashMap<Query,HashMap<String, Integer>> ranks = new HashMap<Query,HashMap<String,Integer>>();
			HashMap<Query,HashMap<String, Boolean>> rankablesBid = new HashMap<Query,HashMap<String,Boolean>>();
			HashMap<Query,HashMap<String, Boolean>> rankables = new HashMap<Query,HashMap<String,Boolean>>(); //Agents that are padded or not in the auction are *not* rankable
			HashMap<Query,int[]> fullOrders = new HashMap<Query,int[]>();
			HashMap<Query,int[]> fullImpressions = new HashMap<Query,int[]>();
			HashMap<Query,int[][]> fullWaterfalls = new HashMap<Query, int[][]>();
			for(Query q : _querySpace) {
				int[] impsPred = _queryAnalyzer.getImpressionsPrediction(q);
				int[] ranksPred = _queryAnalyzer.getOrderPrediction(q);
				int[][] waterfallPred = _queryAnalyzer.getImpressionRangePrediction(q);
				int totalImps = _queryAnalyzer.getTotImps(q);

				//            System.out.println("Query Analyzer Results for " + q);
				//            System.out.println("impsPred: " + Arrays.toString(impsPred));
				//            System.out.println("ranksPred: " + Arrays.toString(ranksPred));
				//            if(waterfallPred != null) {
				//               System.out.println("waterfall: ");
				//               for(int i = 0; i < waterfallPred.length; i++) {
				//                  System.out.println("\t" + Arrays.toString(waterfallPred[i]));
				//               }
				//            }
				//            else {
				//               System.out.println("waterfall: null");
				//            }

				if(totalImps == 0) {
					//this means something bad happened
					System.out.println("ERROR totalImps == 0"); //ap
					totalImps = -1;
				}

				fullOrders.put(q, ranksPred);
				fullImpressions.put(q, impsPred);
				fullWaterfalls.put(q,waterfallPred);
				totalImpressions.put(q, totalImps);

				HashMap<String, Integer> perQRanks = null;
				if(waterfallPred != null) {
					perQRanks = new HashMap<String,Integer>();
					for(int i = 0; i < _advertisers.size(); i++) {
						perQRanks.put("adv" + (ranksPred[i] + 1),i);
					}
				}
				ranks.put(q, perQRanks);
				//            System.out.println("perQRanks: " + perQRanks);




				//This is checking which agents have an assigned ranking from the QA (for budget and bid estimation).
				//If RANKABLE==false, assume everyone was assigned a ranking
				HashMap<String, Boolean> rankable = null;
				HashMap<String, Boolean> rankableBid = null;
				if(waterfallPred != null) {
					if(RANKABLE) {
						rankable = _queryAnalyzer.getRankableMap(q);
					}
					else {
						rankable = new HashMap<String,Boolean>();
						for(int i = 0; i < _advertisers.size(); i++) {
							rankable.put("adv"+(i+1),true);
						}
					}

					rankableBid = new HashMap<String,Boolean>();
					for(int i = 0; i < _advertisers.size(); i++) {
						rankableBid.put("adv"+(i+1),true);
					}
				}
				rankables.put(q,rankable);
				rankablesBid.put(q,rankableBid);
			}

			_userModel.updateModel(totalImpressions);

			HashMap<Product,HashMap<UserState,Double>> userStates = getUserStates(_userModel,_products);

			_paramEstimation.updateModel(queryReport, bidBundle, fullImpressions, fullWaterfalls, userStates);

			for(Query q : _querySpace) {
				int qtIdx = queryTypeToInt(q.getType());
				_ISRatioModel.updateISRatio(q,getISRatio(q,_numSlots,_numPS,_advertiserEffectBoundsAvg[qtIdx],_paramEstimation.getContProbPrediction(q),_baseConvProbs.get(q),userStates));
			}

			HashMap<Query, Double> cpc = new HashMap<Query,Double>();
			HashMap<Query, Double> ourBid = new HashMap<Query,Double>();
			for(Query q : _querySpace) {
				cpc.put(q, queryReport.getCPC(q));
				ourBid.put(q, bidBundle.getBid(q));
			}
			_bidModel.updateModel(cpc, ourBid, ranks, rankablesBid);

			HashMap<Query,Double> contProbs = new HashMap<Query,Double>();
			HashMap<Query, double[]> allbids = new HashMap<Query,double[]>();
			for(Query q : _querySpace) {
				contProbs.put(q, _paramEstimation.getContProbPrediction(q));
				double oppAdvEffect = _advertiserEffectBoundsAvg[queryTypeToInt(q.getType())];
				double oppSquashedAdvEff = Math.pow(oppAdvEffect, _squashing);
				double[] bids = new double[_advertisers.size()];
				for(int j = 0; j < bids.length; j++) {
					if(j == _advIdx) {
						bids[j] = bidBundle.getBid(q) * Math.pow(_paramEstimation.getAdvEffectPrediction(q),_squashing);
					}
					else {
						bids[j] = _bidModel.getPrediction("adv" + (j+1), q) * oppSquashedAdvEff;
					}
				}
				allbids.put(q, bids);
			}

			double[] regReserve = new double[3];
			regReserve[0] = _paramEstimation.getRegReservePrediction(QueryType.FOCUS_LEVEL_ZERO);
			regReserve[1] = _paramEstimation.getRegReservePrediction(QueryType.FOCUS_LEVEL_ONE);
			regReserve[2] = _paramEstimation.getRegReservePrediction(QueryType.FOCUS_LEVEL_TWO);

			_budgetEstimator.updateModel(queryReport, bidBundle, contProbs, regReserve, fullOrders,fullImpressions,fullWaterfalls, rankables, allbids, userStates);

			_unitsSold.update(salesReport); //FIXME: Move this and salesDist to beginning, in case other models want to use them (e.g. QA)
		}
	}
	/*
	 * GET NEEDED INFORMATION OUT OF VARIOUS MODELS
	 * THESE SHOULD NOT BE HERE IN A GOOD Architecture.
	 */
	public static HashMap<Product,HashMap<UserState,Double>> getUserStates(UserModel userModel, Set<Product> products) {
		HashMap<Product,HashMap<UserState,Double>> userStates = new HashMap<Product,HashMap<UserState,Double>>();
		for(Product p : products) {
			HashMap<UserState,Double> userState = new HashMap<UserState,Double>();
			for(UserState s : UserState.values()) {
				userState.put(s, (double)userModel.getPrediction(p, s));
			}
			userStates.put(p, userState);
		}
		return userStates;
	}

	
	public static HashMap<Query,Integer> getMaxImpsPred(HashMap<Product,HashMap<UserState,Double>> userStates, double userModelUBMult, Set<Query> querySpace) {
		HashMap<Query,Integer> maxImps = new HashMap<Query, Integer>(querySpace.size());
		for (Query q : querySpace) {
			int numImps = 0;
			for (Product p : userStates.keySet()) {
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
					numImps += userStates.get(p).get(UserState.F0);
					numImps += userModelUBMult * userStates.get(p).get(UserState.IS) / 3.0;//HC num
				} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
					if (p.getComponent().equals(q.getComponent()) || p.getManufacturer().equals(q.getManufacturer())) {
						numImps += userStates.get(p).get(UserState.F1) / 2.0;
						numImps += userModelUBMult * userStates.get(p).get(UserState.IS) / 6.0;//HC num
					}
				} else if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
					if (p.getComponent().equals(q.getComponent()) && p.getManufacturer().equals(q.getManufacturer())) {
						numImps += userStates.get(p).get(UserState.F2);
						numImps += userModelUBMult * userStates.get(p).get(UserState.IS) / 3.0;//HC num
					}
				}
			}
			maxImps.put(q, numImps);
		}
		return maxImps;
	}
	
	public boolean hasPerfectModels() {
		return (_perfectCljSim != null);
	}


	/* (non-Javadoc)
	 * @see agents.AbstractAgent#initBidder()
	 */
	@Override
	public void initBidder() {
		_c = new double[3];
		_c[0] = .04;//HC num
		_c[1] = .12;
		_c[2] = .29;

		_baseConvProbs = new HashMap<Query, Double>();
		_baseClickProbs = new HashMap<Query, Double>();
		_salesPrices = new HashMap<Query,Double>();

		for(Query q : _querySpace) {

			//Define expected revenue for receiving a conversion for this query
			String manufacturer = q.getManufacturer();
			if(_manSpecialty.equals(manufacturer)) {
				//If the query is F1 or F2 for our manufacturer specialty, we get a sales bonus for every conversion from this query
				_salesPrices.put(q, 10*(_MSB+1));//HC num
			}
			else if(manufacturer == null) {
				//If the query is F0 or F1, we should consider that searchers will have our manufacturer specialty about 1/3 of the time, which will give us a sales bonus when they convert.
				_salesPrices.put(q, (10*(_MSB+1)) * (1/3.0) + (10)*(2/3.0));//HC num
			}
			else {
				//If the query is F2 and not our manufacturer specialty, we'll never get the sales bonus.
				_salesPrices.put(q, 10.0);//HC num
			}


			//Get baseline conversion probabilities.
			//We don't consider component specialty bonus here. They are handled in a method that also considers
			//our amount over capacity and the fraction of IS users.
			if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				_baseConvProbs.put(q, _piF0);
			}
			else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
				_baseConvProbs.put(q, _piF1);
			}
			else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
				_baseConvProbs.put(q, _piF2);
			}
			else {
				throw new RuntimeException("Malformed query");
			}

			/*
			 * These are the MAX e_q^a (they are randomly generated), which is our clickPr for being in slot 1!
			 *
			 * Taken from the spec
			 */

			/*
			 * TODO
			 *
			 * we can consider replacing these with our predicted clickPrs
			 *
			 */
			if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				_baseClickProbs.put(q, .3);//HC num
			}
			else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
				_baseClickProbs.put(q, .4);//HC num
			}
			else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
				_baseClickProbs.put(q, .5);//HC num
			}
			else {
				throw new RuntimeException("Malformed query");
			}
		}

		/*
		 * Initialize Simulator
		 */
		_baseCljSim = initClojureSim();
	}
	
	public PersistentHashMap initClojureSim() {
		return javasim.initClojureSim(_publisherInfo,_slotInfo,_advertiserInfo,_retailCatalog,_advertisers);
	}
	
	public PersistentHashMap setupSimForDay() {
		if(!hasPerfectModels()) {
			//String- references agent by Id
			HashMap<String,HashMap<Query,Double>> squashedBids = new HashMap<String, HashMap<Query, Double>>();
			HashMap<String,HashMap<Query,Double>> budgets = new HashMap<String, HashMap<Query, Double>>();
			HashMap<Product,double[]> userPop = new HashMap<Product, double[]>();
			HashMap<String,HashMap<Query,Double>> advAffects = new HashMap<String, HashMap<Query, Double>>();
			HashMap<Query,Double> contProbs = new HashMap<Query, Double>();
			HashMap<Query,Double> regReserves = new HashMap<Query, Double>();
			HashMap<Query,Double> promReserves = new HashMap<Query, Double>();
			HashMap<String,Integer> capacities = new HashMap<String, Integer>();
			HashMap<String,Integer> startSales = new HashMap<String, Integer>();
			HashMap<String,String> manSpecialties = new HashMap<String, String>();
			HashMap<String,String> compSpecialties = new HashMap<String, String>();
			HashMap<String,HashMap<Query,Ad>> ads = new HashMap<String, HashMap<Query, Ad>>();

			//Get predictions for continuation probabilities, reserve scores, and promoted reserve scores.
			for(Query q : _querySpace) {
				contProbs.put(q,_paramEstimation.getContProbPrediction(q));
				regReserves.put(q,_paramEstimation.getRegReservePrediction(q.getType()));
				promReserves.put(q, _paramEstimation.getPromReservePrediction(q.getType()));
			}

			//------------
			//Get predictions for each advertiser's bid, budget, advertiserEffect,
			//initialConversions, maxCapacity, manufacturer and component specialties,
			//whether they are targeting
			//------------
			for(int i = 0; i < _advertisers.size(); i++) {
				String agent = _advertisers.get(i);
				if(i != _advIdx) { //For everyone besides us...
					HashMap<Query,Double> aSquashedBids = new HashMap<Query, Double>();
					HashMap<Query,Double> aBudgets = new HashMap<Query, Double>();
					HashMap<Query,Double> aAdvAffects = new HashMap<Query, Double>();
					int aCapacities = 450;  //FIXME estimate opponent capacity //HC num
					//               int aStartSales = (int)((4.0*(aCapacities / ((double) _capWindow)) + aCapacities) / 2.0);  //FIXME Estimate opponent start-sales
					int aStartSales = aCapacities;
					for(Query q : _querySpace) {
						double bid = _bidModel.getPrediction("adv" + (i+1), q);
						double advEffect = _advertiserEffectBoundsAvg[queryTypeToInt(q.getType())]; //FIXME: Get this from an advertiser effect model?
						double squashedBid = bid*Math.pow(advEffect,_squashing);
						aSquashedBids.put(q, squashedBid);
						double budget = _budgetEstimator.getBudgetEstimate(q, "adv" + (i+1));
						//B: Double.MAX_VALUE is the largest poss finite value of type double
						if(!(Double.isInfinite(budget) || budget == Double.MAX_VALUE)) {
							budget *= _budgetMult;
						}
						aBudgets.put(q, budget);
						aAdvAffects.put(q,advEffect);
					}

					//---------------
					//Determine manufacturer/component specialties
					//---------------

					//Assume specialty is the prod of F2 query they are bidding most in
					//               String aManSpecialties = maxQuery.getManufacturer();
					//               String aCompSpecialties = maxQuery.getComponent();

					String aManSpecialties  = _specialtyModel.getManufacturerSpecialty("adv" + (i+1));
					String aCompSpecialties = _specialtyModel.getComponentSpecialty("adv" + (i+1));

					//---------------
					//Determine ad targeting
					//---------------

					//We can either assume the advertiser will target its specialty,
					//or we can look at its historical targeting.
					HashMap<Query,Ad> aAds = new HashMap<Query, Ad>();
					for(Query q : _querySpace) {
						Ad predictedAd = _adTypeEstimator.getAdTypeEstimate(q, "adv" + (i+1)); 
						//            	   Ad predictedAd = getTargetedAd(q,aManSpecialties,aCompSpecialties); //Old method
						aAds.put(q, predictedAd);
					}

					squashedBids.put(agent,aSquashedBids);
					budgets.put(agent,aBudgets);
					advAffects.put(agent,aAdvAffects);
					capacities.put(agent,aCapacities);
					startSales.put(agent,aStartSales);
					manSpecialties.put(agent,aManSpecialties);
					compSpecialties.put(agent,aCompSpecialties);
					ads.put(agent,aAds);
				}
				else {
					HashMap<Query,Double> aAdvAffects = new HashMap<Query, Double>();
					int aCapacities = _capacity;
					//               double remainingCap;
					//               if(_day < 4) {
					//                  remainingCap = _capacity/((double)_capWindow);
					//               }
					//               else {
					//                  remainingCap = _capacity - _unitsSold.getWindowSold();
					//               }
					//               int aStartSales = _capacity - (int)remainingCap;
					//               int aStartSales = (int)(_capacity - _capacity / ((double) _capWindow));
					int aStartSales = 0;

					String aManSpecialties = _manSpecialty;
					String aCompSpecialties = _compSpecialty;

					for(Query q : _querySpace) {
						aAdvAffects.put(q,_paramEstimation.getAdvEffectPrediction(q));
					}

					advAffects.put(agent,aAdvAffects);
					capacities.put(agent,aCapacities);
					startSales.put(agent,aStartSales);
					manSpecialties.put(agent,aManSpecialties);
					compSpecialties.put(agent,aCompSpecialties);
				}
			}
			//updating the user state for each product, userState is an array of the num of users in each state, userPop a user state for each product
			for(Product p : _products) {
				double[] userState = new double[UserState.values().length];
				userState[0] = _userModel.getPrediction(p, UserState.NS);
				userState[1] = _userModel.getPrediction(p, UserState.IS);
				userState[2] = _userModel.getPrediction(p, UserState.F0);
				userState[3] = _userModel.getPrediction(p, UserState.F1);
				userState[4] = _userModel.getPrediction(p, UserState.F2);
				userState[5] = _userModel.getPrediction(p, UserState.T);
				userPop.put(p, userState);
			}

			return javasim.mkFullStatus(_baseCljSim, squashedBids, budgets, userPop, advAffects, contProbs, regReserves,
					promReserves, capacities, startSales, manSpecialties, compSpecialties, ads);
		}
		else { //why is 0 hardcoded?
			return javasim.mkPerfectFullStatus(_perfectCljSim, (int)_day, _agentToReplace, 0);//HC num
		}
	}

	//returns an array of average impressions,clicks,costs,sales, and other stats
	public double[] simulateQuery(PersistentHashMap cljSim, Query query, double bid, double budget, Ad ad) {
		//System.out.println("Simulate Query");
		ArrayList result;
		if(hasPerfectModels()) {
			result = javasim.simQuery(cljSim, query, _agentToReplace, (int) _day, bid, budget, ad, 1, true);//HC num
		}
		else {
			result = javasim.simQuery(cljSim, query, _advId, (int) _day, bid, budget, ad, 1, false);//HC num
		}
		double[] resultArr = new double[result.size()];
		for(int i = 0; i < result.size(); i++) {
			resultArr[i] = (Double)result.get(i);
		}
		return resultArr;
	}

	//returns an array of total conversions and total costs for the day
	public double[] simulateDay(PersistentHashMap cljSim, BidBundle bundle) {
		ArrayList result;
		if(hasPerfectModels()) {
			result = javasim.simDay(cljSim, _agentToReplace, (int) _day, bundle, 1, true);//HC num
		}
		else {
			result = javasim.simDay(cljSim, _advId, (int) _day, bundle, 1, false); //HC num
		}
		double[] resultArr = new double[result.size()];
		for(int i = 0; i < result.size(); i++) {
			resultArr[i] = (Double)result.get(i);
		}
		return resultArr;
	}
	
	protected Ad getTargetedAd(Query q) {
		return getTargetedAd(q, _manSpecialty, _compSpecialty);
	}

	protected Ad getTargetedAd(Query q, String manSpecialty, String compSpecialty) {
		Ad ad;
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			/*
			 * F0 Query, target our specialty
			 */
			ad = new Ad(new Product(manSpecialty, compSpecialty));
		}
		else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			if(q.getComponent() == null) {
				/*
				 * F1 Query (comp = null), so target the subgroup that searches for this and our
				 * component specialty
				 */
				ad = new Ad(new Product(q.getManufacturer(), compSpecialty));
			}
			else {
				/*
				 * F1 Query (man = null), so target the subgroup that searches for this and our
				 * manufacturer specialty
				 */
				ad = new Ad(new Product(manSpecialty, q.getComponent()));
			}
		}
		else  {
			/*
			 * F2 Query, so target the subgroup that searches for this
			 */
			ad = new Ad(new Product(q.getManufacturer(), q.getComponent()));
		}
		return ad;
	}

	
	
	
	/*
	 * Get Bid and Budget Lists
	 * 
	 * These are the predictions the agent has made. They are used to create the
	 * "items" used in the KP algorithms.
	 */
	/**
	 * Get a single bid for each slot (or X bids per slot).
	 * The idea is that our bid models are noisy, so we don't want to
	 * even consider bids on the edges of slots, since we might be getting a completely
	 * different slot than we expect.
	 * @return
	 */
	protected HashMap<Query, ArrayList<Double>> getMinimalBidLists() {

		HashMap<Query,ArrayList<Double>> bidLists = new HashMap<Query,ArrayList<Double>>();

		for(Query q : _querySpace) {
			//         System.out.println("QUERY " + q);
			ArrayList<Double> ourBids = new ArrayList<Double>();
			if(!q.equals(new Query())) { //If not the F0 Query. FIXME: Don't hardcode

				double ourAdvertiserEffect = _paramEstimation.getAdvEffectPrediction(q);
				double ourSquashedAdvEff = Math.pow(ourAdvertiserEffect,_squashing);

				//Get list of all squashed opponent bids for this query
				ArrayList<Double> opponentScores = new ArrayList<Double>();
				for (String player : _advertisersSet) {
					if (!player.equals(_advId)) { //only get opponent bids
						double opponentBid = _bidModel.getPrediction(player, q);
						double opponentAdvertiserEffect = _advertiserEffectBoundsAvg[queryTypeToInt(q.getType())];
						double opponentScore = opponentBid * Math.pow(opponentAdvertiserEffect, _squashing);
						//                  System.out.println(player + " bid=" + opponentBid + ", advEffect=" + opponentAdvertiserEffect + ", score=" + opponentScore);
						opponentScores.add(opponentScore);
					}
				}

				//Add regular/promoted reserve score
				double reserveScore = _paramEstimation.getRegReservePrediction(q.getType());
				double promotedReserveScore = _paramEstimation.getPromReservePrediction(q.getType());
				opponentScores.add(reserveScore);
				opponentScores.add(promotedReserveScore);
				//            System.out.println("reserveScore=" + reserveScore + ", promoted=" + promotedReserveScore);


				//We will choose to target scores directly between opponent scores.
				Collections.sort(opponentScores);
				int BIDS_BETWEEN_SCORES = 2;//HC num
				ArrayList<Double> ourScores = new ArrayList<Double>();
				for (int i=1; i<opponentScores.size(); i++) {
					double lowScore = opponentScores.get(i-1);
					double highScore = opponentScores.get(i);
					for (int j=1; j<=BIDS_BETWEEN_SCORES; j++) {
						double ourScore = lowScore + j*(highScore-lowScore)/(BIDS_BETWEEN_SCORES+1);//HC num
						ourScores.add(ourScore);
					}
				}

				//            System.out.println("Our targeted scores: " + ourScores);

				//Also ad a score to bid directly above the reserve score,
				//And directly above the highest score (so we get the 1st slot)
				double scoreEpsilon = .01;//HC num
				double highestOpponentScore = opponentScores.get(opponentScores.size()-1);
				ourScores.add(reserveScore + scoreEpsilon);
				ourScores.add(highestOpponentScore + .1);//HC num

				//            System.out.println("After adding min/max, our targeted scores: " + ourScores);

				//Remove any duplicate scores, or any scores outside a reasonable boundry
				double FRACTION = 1; //_baseClickProbs.get(q); //FIXME: There's no reason this should be a clickProb. //HC num
				double ourVPC = _salesPrices.get(q) * _baseConvProbs.get(q) * FRACTION;//HC num
				double maxScore = ourVPC * ourSquashedAdvEff;
				ArrayList<Double> ourPrunedScores = new ArrayList<Double>();
				Collections.sort(ourScores);
				for (Double score : ourScores) {
					if (score >= reserveScore && score <= maxScore) { //within bounds
						int lastAddedIdx = ourPrunedScores.size()-1;
						if (lastAddedIdx == -1 || !score.equals(ourPrunedScores.get(lastAddedIdx))) { //not just added
							ourPrunedScores.add(score);
						}
					}
				}

				//            System.out.println("Our pruned targeted scores: " + ourPrunedScores);

				//Turn score into bids
				double maxBid = 3.5;//HC num
				for (double score : ourPrunedScores) {
					double ourBid = score / ourSquashedAdvEff;
					if(ourBid <= maxBid) {
						ourBids.add(ourBid);
					}
					else {
						ourBids.add(3.5);//HC num
						break;
					}
				}
		

				//            System.out.println("Our advEffect:" + ourAdvertiserEffect);
				//            System.out.println("Our bids: " + ourBids);
			}

			bidLists.put(q, ourBids);

		}
		return bidLists;
	}


	/**
	 * Get a single bid for each slot (or X bids per slot).
	 * The idea is that our bid models are noisy, so we don't want to
	 * even consider bids on the edges of slots, since we might be getting a completely
	 * different slot than we expect.
	 * @return
	 */
	protected HashMap<Query, ArrayList<Double>> getPerfectBidLists(String filename) {

		int BIDS_BETWEEN_SCORES = 1;


		HashMap<Query,ArrayList<Double>> bidLists = new HashMap<Query,ArrayList<Double>>();

		for(Query q : _querySpace) {
			//         System.out.println("QUERY " + q);
			ArrayList<Double> ourBids = new ArrayList<Double>();
			if(!q.equals(new Query())) { //If not the F0 Query. FIXME: Don't hardcode
				GameStatusHandler statusHandler;

				try {
					statusHandler = new GameStatusHandler(filename);
					GameStatus status = statusHandler.getGameStatus(); 
					String[] agents = status.getAdvertisers();
					double[] squashedBids = new double[agents.length];
					UserClickModel userClickModel = status.getUserClickModel();
					//double squashing = status.getPubInfo().getSquashingParameter();
					for (int a = 0; a < agents.length; a++) {
						String agentName = agents[a];
						try {
							//double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), a);
							double bid = status.getBidBundles().get(agentName).get((int)_day).getBid(q);
							squashedBids[a] = bid;// * Math.pow(advEffect, squashing);
						} catch (Exception e) {
							squashedBids[a] = Double.NaN;
						}
					}

					Arrays.sort(squashedBids);


					for (int i=1; i<squashedBids.length; i++) {
						double lowScore = squashedBids[i-1];
						double highScore = squashedBids[i];
						for (int j=1; j<=BIDS_BETWEEN_SCORES; j++) {
							double ourScore = lowScore + j*(highScore-lowScore)/(BIDS_BETWEEN_SCORES+1);//HC num
							ourBids.add(ourScore);
						}
					}

					//					while(ourBids.size()>5){
					//						ourBids.remove(0);
					//					}



					bidLists.put(q, ourBids);



				} catch (Exception e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}else{
				bidLists.put(q, new ArrayList<Double>());
			}
		}
		return bidLists;

	}

	protected HashMap<Query, ArrayList<Double>> getBudgetLists() {
		HashMap<Query,ArrayList<Double>> budgetLists = new HashMap<Query,ArrayList<Double>>();
		for(Query q : _querySpace) {
			if(!q.equals(new Query())) { //Skip over F0 Query FIXME: Make configurable
				ArrayList<Double> budgetList = new ArrayList<Double>();
				//budgetList.add(10.0);//HC num
				//budgetList.add(100.0);//HC num
				//budgetList.add(300.0);//HC num
				//            budgetList.add(400.0);
				//budgetList.add(1000.0);

				budgetList.add(10.0);//HC num
				//				budgetList.add(25.0);//HC num
				//				budgetList.add(50.0);//HC num
				//				budgetList.add(75.0);//HC num
				budgetList.add(100.0);//HC num
				//				budgetList.add(150.0);//HC num
				//				budgetList.add(200.0);//HC num
				budgetList.add(250.0);//HC num
				//				budgetList.add(300.0);//HC num
				//				budgetList.add(350.0);//HC num
				budgetList.add(400.0);//HC num
				//				budgetList.add(450.0);//HC num
				//				budgetList.add(500.0);//HC num
				//				budgetList.add(550.0);//HC num
				//				budgetList.add(600.0);//HC num
				//				budgetList.add(650.0);//HC num
				//				budgetList.add(700.0);//HC num
				budgetList.add(800.0);//HC num
				//budgetList.add(1000.0);//HC num
				budgetLists.put(q,budgetList);
			}
			else {
				budgetLists.put(q,new ArrayList<Double>());
			}
		}
		return budgetLists;
	}

	
	/*
	 * Handle the special/corner case of the first two days of 
	 * the game where we do not know what bids/budgets are.
	 */

	public BidBundle getFirst2DaysBundle() {
		BidBundle bundle = new BidBundle();
		for(Query q : _querySpace){
			if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
				if(_compSpecialty.equals(q.getComponent()) && _manSpecialty.equals(q.getManufacturer())) {
					double bid = randDouble(_paramEstimation.getPromReservePrediction(q.getType()), _salesPrices.get(q) * _baseConvProbs.get(q) * .7);//HC num
					bundle.addQuery(q, bid, getTargetedAd(q), 200);//HC num
				}
				else {
					System.out.println(_paramEstimation);
					double bid = randDouble(_paramEstimation.getPromReservePrediction(q.getType()), _salesPrices.get(q) * _baseConvProbs.get(q) * .7);//HC num
					bundle.addQuery(q, bid, getTargetedAd(q), 100);//HC num
				}
			}
		}
		bundle.setCampaignDailySpendLimit(_totalBudgets.get(_capacity));
		return bundle;
	}
	
	
	protected Ad getProbeAd(Query q, double bid, double budget) {
		return new Ad();
	}
	
	/*
	 * Here the agent picks a probe bid from a given range of slots. We 
	 * use predictions from out bid model to prdict what we need to bid
	 * to be in different slots. We then randomize among the different slots
	 * to keep costs down.
	 * 
	 */
	protected double[] getProbeSlotBidBudget(Query q) {
		int lowSlot = 5;
		int highSlot = 1;
		double[] bidBudget = new double[2];
		ArrayList<Double> ourBids = new ArrayList<Double>();
		ArrayList<Double> opponentScores = new ArrayList<Double>();
		for (String player : _advertisersSet) {
			if (!player.equals(_advId)) { //only get opponent bids
				double opponentBid = _bidModel.getPrediction(player, q);
				double opponentAdvertiserEffect = _advertiserEffectBoundsAvg[queryTypeToInt(q.getType())];
				double opponentScore = opponentBid * Math.pow(opponentAdvertiserEffect, _squashing);
				opponentScores.add(opponentScore);
			}
		}

		//Add regular/promoted reserve score
		double reserveScore = _paramEstimation.getRegReservePrediction(q.getType());
		opponentScores.add(reserveScore);


		//We will choose to target scores directly between opponent scores.
		Collections.sort(opponentScores);
		int BIDS_BETWEEN_SCORES = 1;//HC num
		ArrayList<Double> ourScores = new ArrayList<Double>();
		for (int i=1; i<opponentScores.size(); i++) {
			double lowScore = opponentScores.get(i-1);
			double highScore = opponentScores.get(i);
			for (int j=1; j<=BIDS_BETWEEN_SCORES; j++) {
				double ourScore = lowScore + j*(highScore-lowScore)/(BIDS_BETWEEN_SCORES+1);//HC num
				ourScores.add(ourScore);
			}
		}


		//Also ad a score to bid directly above the reserve score,
		//And directly above the highest score (so we get the 1st slot)
		double scoreEpsilon = .01;//HC num
		double highestOpponentScore = opponentScores.get(opponentScores.size()-1);
		ourScores.add(highestOpponentScore + scoreEpsilon);

		//add score epsilon below
		double lowestOpponentScore = opponentScores.get(0);
		//ourScores.add(lowestOpponentScore - scoreEpsilon);


		double FRACTION = 1; //_baseClickProbs.get(q); //FIXME: There's no reason this should be a clickProb. //HC num
		double ourVPC = _salesPrices.get(q) * _baseConvProbs.get(q) * FRACTION;//HC num
		double ourAdvertiserEffect = _paramEstimation.getAdvEffectPrediction(q);
		double ourSquashedAdvEff = Math.pow(ourAdvertiserEffect,_squashing);
		double maxScore = ourVPC * ourSquashedAdvEff;
		ArrayList<Double> ourPrunedScores = new ArrayList<Double>();
		Collections.sort(ourScores);
		for (Double score : ourScores) {
			if (score >= reserveScore && score <= maxScore) { //within bounds
				int lastAddedIdx = ourPrunedScores.size()-1;
				if (lastAddedIdx == -1 || !score.equals(ourPrunedScores.get(lastAddedIdx))) { //not just added
					ourPrunedScores.add(score);
				}
			}
		}
		//catches major problems
		if(ourPrunedScores.size()<=0){
			ourPrunedScores.add(reserveScore*1.5);
			ourPrunedScores.add(highestOpponentScore + scoreEpsilon);
		}

		//Turn score into bids
		for (double score : ourPrunedScores) {
			double ourBid = score / ourSquashedAdvEff;
			ourBids.add(ourBid);
		}
		Collections.sort(ourBids);

		int lastSlotPicked = lowSlot; //set this to pick lowest slot to consider (5 sets the range the widest)
		int lastSlot = Math.min(Math.min(_numSlots,ourBids.size()), lastSlotPicked);
		int firstSlot = Math.min(highSlot,ourBids.size() );
		//int numSlots = Math.min(_numSlots,ourBids.size());

		int distAboveLowestSlot = _R.nextInt(lastSlot-firstSlot+1);

		//int slot = (int) Math.ceil(numSlots/2.0);
		//int slot = ourBids.size()-1;
		//		if(numSlots>=3){
		//			slot = numSlots-1;
		//		}
		int slotInArray = ourBids.size()-firstSlot-distAboveLowestSlot;
		System.out.println("Slot: "+slotInArray+" Bids: "+ourBids.toString()); //to remove

		if(ourBids.size()<=slotInArray || slotInArray<0){
			slotInArray = ourBids.size()-1;
		}

		double bid = ourBids.get(slotInArray);
		//System.out.println("Probing: "+bid+" not "+ourBids.get(_R.nextInt(numSlots)));
		bidBudget[0] = bid;
		bidBudget[1] = bid * _probeBidMult;

		return bidBudget;
	}

	private Random makeRandom(int seed){
		if (seed==0){
			return new Random();
		}else{
			return new Random(seed);
		}
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	/* (non-Javadoc)
	 * @see agents.AbstractAgent#toString()
	 */
	@Override
	public String toString() {
		return "Schlemazl Agent";
	}

	/* (non-Javadoc)
	 * @see agents.AbstractAgent#getBidBundle()
	 */
	@Override
	public BidBundle getBidBundle() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see agents.AbstractAgent#getCopy()
	 */
	@Override
	public AbstractAgent getCopy() {
		// TODO Auto-generated method stub
		return null;
	}


}
