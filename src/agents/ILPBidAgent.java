package agents;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.EnsembleBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.postoprclick.RegressionPosToPrClick;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.AbstractConversionModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.sales.SalesDistributionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.BasicUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import agents.mckp.IncItem;
import agents.mckp.Item;
import agents.mckp.ItemComparatorByWeight;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public class ILPBidAgent extends AbstractAgent {

	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean TARGET = false;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = true;
	private static final boolean BOOST = true;

	private double _safetyBudget = 750;

	//Days since Last Boost
	private double lastBoost;
	private double boostCoeff = 1.2;

	private Random _R = new Random();
	private boolean DEBUG = false;
	private double LAMBDA = .995;
	private HashMap<Query, Double> _salesPrices;
	private HashMap<Query, Double> _baseConvProbs;
	private HashMap<Query, Double> _baseClickProbs;
	private AbstractUserModel _userModel;
	private AbstractQueryToNumImp _queryToNumImpModel;
	private AbstractBidToCPC _bidToCPC;
	private AbstractBidToPrClick _bidToPrClick;
	private AbstractUnitsSoldModel _unitsSold;
	private AbstractConversionModel _convPrModel;
	private SalesDistributionModel _salesDist;
	private BasicTargetModel _targModel;
	private Hashtable<Query, Integer> _queryId;
	private LinkedList<Double> _bidList;
	private LinkedList<Integer> _capList;
	private int _capacityInc = 10;
	private int lagDays = 5;
	private boolean salesDistFlag;
	private IloCplex _cplex;

	public ILPBidAgent() {

		try {
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
			_cplex = cplex;
		} catch (IloException e) {
			throw new RuntimeException("Could not initialize CPLEX");
		}

		_bidList = new LinkedList<Double>();
		//		double increment = .25;
		double bidIncrement  = .1;
		double bidMin = .04;
		double bidMax = 3.0;
		int tot = (int) Math.ceil((bidMax-bidMin) / bidIncrement);
		for(int i = 0; i < tot; i++) {
			_bidList.add(bidMin+(i*bidIncrement));
		}

		_capList = new LinkedList<Integer>();
		int increment = 10;
		int min = 10;
		int max = 50;
		for(int i = min; i <= max; i+= increment) {
			_capList.add(i);
		}

		salesDistFlag = false;
	}



	@Override
	public Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		AbstractUserModel userModel = new BasicUserModel();
		AbstractQueryToNumImp queryToNumImp = new BasicQueryToNumImp(userModel);
		AbstractUnitsSoldModel unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
		BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty,_compSpecialty);
		AbstractBidToCPC bidToCPC = new EnsembleBidToCPC(_querySpace, 10, 20, false, true);
		AbstractBidToPrClick bidToPrClick = new EnsembleBidToPrClick(_querySpace, 10, 20, basicTargModel, true, true);
		GoodConversionPrModel convPrModel = new GoodConversionPrModel(_querySpace,basicTargModel);
		models.add(userModel);
		models.add(queryToNumImp);
		models.add(bidToCPC);
		models.add(bidToPrClick);
		models.add(unitsSold);
		models.add(convPrModel);
		models.add(basicTargModel);
		return models;
	}

	protected void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				_userModel = userModel;
			}
			else if(model instanceof AbstractQueryToNumImp) {
				AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
				_queryToNumImpModel = queryToNumImp;
			}
			else if(model instanceof AbstractUnitsSoldModel) {
				AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
				_unitsSold = unitsSold;
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPC = bidToCPC; 
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				_bidToPrClick = bidToPrClick;
			}
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_convPrModel = convPrModel;
			}
			else if(model instanceof BasicTargetModel) {
				BasicTargetModel targModel = (BasicTargetModel) model;
				_targModel = targModel;
			}
			else {
				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)"+model);
			}
		}
	}

	@Override
	public void initBidder() {

		_baseConvProbs = new HashMap<Query, Double>();
		_baseClickProbs = new HashMap<Query, Double>();

		// set revenue prices
		_salesPrices = new HashMap<Query,Double>();
		for(Query q : _querySpace) {

			String manufacturer = q.getManufacturer();
			if(_manSpecialty.equals(manufacturer)) {
				_salesPrices.put(q, 10*(_MSB+1));
			}
			else if(manufacturer == null) {
				_salesPrices.put(q, (10*(_MSB+1)) * (1/3.0) + (10)*(2/3.0));
			}
			else {
				_salesPrices.put(q, 10.0);
			}

			if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_baseConvProbs.put(q, _piF0);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baseConvProbs.put(q, _piF1);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
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

			if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_baseClickProbs.put(q, .3);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baseClickProbs.put(q, .4);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baseClickProbs.put(q, .5);
			}
			else {
				throw new RuntimeException("Malformed query");
			}

			String component = q.getComponent();
			if(_compSpecialty.equals(component)) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB));
			}
			else if(component == null) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB)*(1/3.0) + _baseConvProbs.get(q)*(2/3.0));
			}
		}

		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			_queryId.put(q, i);
			i++;
		}

		lastBoost = 5;
		boostCoeff = 1.2;
	}


	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {

		for(AbstractModel model: _models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				userModel.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractQueryToNumImp) {
				AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
				queryToNumImp.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractUnitsSoldModel) {
				AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
				unitsSold.update(salesReport);
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				bidToCPC.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				bidToPrClick.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
				if(model instanceof GoodConversionPrModel) {
					GoodConversionPrModel adMaxModel = (GoodConversionPrModel) convPrModel;
					adMaxModel.setTimeHorizon(timeHorizon);
				}
				if(model instanceof HistoricPrConversionModel) {
					HistoricPrConversionModel adMaxModel = (HistoricPrConversionModel) convPrModel;
					adMaxModel.setTimeHorizon(timeHorizon);
				}
				convPrModel.updateModel(queryReport, salesReport,_bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof BasicTargetModel) {
				//Do nothing
			}
			else {
				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)");
			}
		}
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();

		if(SAFETYBUDGET) {
			bidBundle.setCampaignDailySpendLimit(_safetyBudget);
		}

		System.out.println("Day: " + _day);

		if(_day > 1) {
			if(!salesDistFlag) {
				SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
				_salesDist = salesDist;
				salesDistFlag = true;
			}
			_salesDist.updateModel(_salesReport);
		}

		if(_day > lagDays + 2) {
			QueryReport queryReport = _queryReports.getLast();
			SalesReport salesReport = _salesReports.getLast();
		}

		if(_day > lagDays){
			buildMaps(models);
			//NEED TO USE THE MODELS WE ARE PASSED!!!

			/*
			 * Setting up CPLEX
			 */

			try {
				_cplex.clearModel();

				/*
				 * Setup the arrays we will need
				 */
				double[] profit = new double[_bidList.size()*_querySpace.size()];
				double[] conversions = new double[_bidList.size()*_querySpace.size()];
				double[] penalty = new double[_capList.size()];
				int[] capList = new int[_capList.size()];

				/*
				 * Fill in profit and conversion arrays
				 */

				for(Query q : _querySpace) {
					debug("Query: " + q);
					for(int i = 0; i < _bidList.size(); i++) {
						double salesPrice = _salesPrices.get(q);
						double bid = _bidList.get(i);
						double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
						double numImps = _queryToNumImpModel.getPrediction(q);
						int numClicks = (int) (clickPr * numImps);
						double CPC = _bidToCPC.getPrediction(q, bid);
						double convProb = _convPrModel.getPrediction(q);

						if(Double.isNaN(CPC)) {
							CPC = 0.0;
						}

						if(Double.isNaN(clickPr)) {
							clickPr = 0.0;
						}

						if(Double.isNaN(convProb)) {
							convProb = 0.0;
						}

						debug("\tBid: " + bid);
						debug("\tCPC: " + CPC);
						debug("\tNumImps: " + numImps);
						debug("\tNumClicks: " + numClicks);
						debug("\tClickPr: " + clickPr);
						debug("\tConv Prob: " + convProb + "\n\n");

						int isID = _queryId.get(q);
						double w = numClicks*convProb;				//weight = numClicks * convProv  [conversions]
						double v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]

						if(TARGET) {
							/*
							 * add a targeted version of our bid as well
							 */
							if(clickPr != 0) {
								numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
								if(convProb != 0) {
									convProb *= _targModel.getConvPrPredictionMultiplier(q, clickPr, convProb, false);
								}
								salesPrice = _targModel.getUSPPrediction(q, clickPr, false);
							}

							w = numClicks*convProb;				//weight = numClciks * convProv
							v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
						}

						int idx = isID*_bidList.size() + i;
						profit[idx] = v;
						conversions[idx] = w;

					}
				}


				/*
				 * Fill in penalty and overcap arrays
				 */

				double avgConvProb = 0; //the average probability of conversion;
				for(Query q : _querySpace) {
					if(_day < 2) {
						avgConvProb += _baseConvProbs.get(q) / 16.0;
					}
					else {
						avgConvProb += _baseConvProbs.get(q) * _salesDist.getPrediction(q);
					}
				}

				double avgUSP = 0;
				for(Query q : _querySpace) {
					if(_day < 2) {
						avgUSP += _salesPrices.get(q) / 16.0;
					}
					else {
						avgUSP += _salesPrices.get(q) * _salesDist.getPrediction(q);
					}
				}

				int valueLostWindow = (int) Math.max(1, Math.min(_capWindow, 59 - _day));

				double valueLost = 0.0;
				for (int i = 0; i < _capList.size(); i++){
					if(i == 0) {
						for(int j = 0; j <= _capList.get(i); j++) {
							double iD = Math.pow(LAMBDA, j);
							double worseConvProb = avgConvProb*iD;
							valueLost += (avgConvProb - worseConvProb)*avgUSP*valueLostWindow;
						}
					}
					else {
						for(int j = _capList.get(i-1)+1; j <= _capList.get(i); j++) {
							double iD = Math.pow(LAMBDA, j);
							double worseConvProb = avgConvProb*iD;
							valueLost += (avgConvProb - worseConvProb)*avgUSP*valueLostWindow;
						}
					}

					penalty[i] = valueLost;
					capList[i] = _capList.get(i);
				}

				/*
				 * Setup Maximization
				 */
				IloLinearNumExpr linearNumExpr = _cplex.linearNumExpr();
				IloIntVar[] bids = _cplex.intVarArray(profit.length, 0, 1);
				for(Query q : _querySpace) {
					for(int i = 0; i < _bidList.size(); i++) {
						int isID = _queryId.get(q);
						int idx = isID*_bidList.size() + i;
						linearNumExpr.addTerm(profit[idx], bids[idx]);
					}
				}

				IloIntVar[] overcap = _cplex.intVarArray(profit.length, 0, 1);
				for(int i = 0; i < _capList.size(); i++) {
					linearNumExpr.addTerm(-1.0 * penalty[i], overcap[i]);
				}

				_cplex.maximize(linearNumExpr);


				/*
				 * Add Constraints
				 */

				/*
				 * Bid constraint
				 *  -Can only bid once in each query
				 */
				for(Query query : _querySpace) {
					IloLinearIntExpr linearIntExpr = _cplex.linearIntExpr();
					for(int i = 0; i < _bidList.size(); i++) {
						int isID = _queryId.get(query);
						int idx = isID*_bidList.size() + i;
						linearIntExpr.addTerm(1, bids[idx]);
					}
					_cplex.addLe(linearIntExpr, 1);
				}

				/*
				 * Capacity constraint I
				 *  -Can only pick one way to go overcap
				 */
				IloLinearIntExpr linearIntExpr = _cplex.linearIntExpr();
				for(int i = 0; i < _capList.size(); i++) {
					linearIntExpr.addTerm(1, bids[i]);
				}
				_cplex.addLe(linearIntExpr, 1);

				/*
				 * Capacity constraint II
				 *  -Cannot sell more items than our capacity + overcap
				 */
				double capacity = _capacity/_capWindow;
				if(_day < 4) {
					//do nothing
				}
				else {
					capacity = _capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4;
					if(capacity < 20) {
						capacity = 20;
					}
					debug("Unit Sold Model Budget "  +capacity);
				}

				if(BOOST) {
					if(lastBoost >= 3 && (_unitsSold.getThreeDaysSold() < (_capacity * (3.0/5.0)))) {
						debug("\n\nBOOOOOOOOOOOOOOOOOOOST\n\n");
						lastBoost = -1;
						capacity *= boostCoeff;
					}
					lastBoost++;
				}

				debug("Budget: "+ capacity);

				linearNumExpr = _cplex.linearNumExpr();
				for(Query q : _querySpace) {
					for(int i = 0; i < _bidList.size(); i++) {
						int isID = _queryId.get(q);
						int idx = isID*_bidList.size() + i;
						linearNumExpr.addTerm(conversions[idx], bids[idx]);
					}
				}

				for(int i = 0; i < _capList.size(); i++) {
					linearNumExpr.addTerm(-1.0 * capList[i], overcap[i]);
				}

				_cplex.addLe(linearNumExpr, capacity);

				double start = System.currentTimeMillis();
				_cplex.solve();
				double stop = System.currentTimeMillis();
				double elapsed = stop - start;
				System.out.println("CPLEX took " + (elapsed / 1000) + " seconds");
				
				System.out.println("Expected Profit: " + _cplex.getObjValue());

				double[] bidVal = _cplex.getValues(bids);
				double[] overcapVal = _cplex.getValues(overcap);
				
				double totOverCap = 0;
				for(int i = 0; i < overcapVal.length; i++) {
					if(overcapVal[i] == 1) {
						totOverCap = _capList.get(i);
						break;
					}
				}


				//set bids
				for(Query q : _querySpace) {

					Integer isID = _queryId.get(q);
					double bid = 0.0;
					for(int i = 0; i < _bidList.size(); i++) {
						int idx = isID*_bidList.size() + i;
						if(bidVal[idx] == 1) {
							bid = _bidList.get(i);
							break;
						}
					}

					if(bid != 0.0) {
						//					bid *= randDouble(.97,1.03);  //Mult by rand to avoid users learning patterns.
						//					System.out.println("Bidding " + bid + "   for query: " + q);
						double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
						double numImps = _queryToNumImpModel.getPrediction(q);
						int numClicks = (int) (clickPr * numImps);
						double CPC = _bidToCPC.getPrediction(q, bid);

						bidBundle.addQuery(q, bid, new Ad());


						//					if(solution.get(isID).targ()) {
						//
						//						if(clickPr != 0) {
						//							numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
						//						}
						//
						//						if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						//							bidBundle.setAd(q, new Ad(new Product(_manSpecialty, _compSpecialty)));
						//						if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getComponent() == null)
						//							bidBundle.setAd(q, new Ad(new Product(q.getManufacturer(), _compSpecialty)));
						//						if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getManufacturer() == null)
						//							bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
						//						if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && q.getManufacturer().equals(_manSpecialty)) 
						//							bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
						//					}

						if(BUDGET) {
							bidBundle.setDailyLimit(q, numClicks*CPC);
						}
					}
					else {
						/*
						 * We decided that we did not want to be in this query, so we will use it to explore the space
						 */
						bid = 0.0;
						bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
						//					System.out.println("Bidding " + bid + "   for query: " + q);
						//					if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						//						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
						//					else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
						//						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
						//					else
						//						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
						//
						//					System.out.println("Exploring " + q + "   bid: " + bid);
						//					bidBundle.addQuery(q, bid, new Ad(), bid*10);
					}
				}
			} catch (IloException e) {
				e.printStackTrace();
			}
		}
		else {
			for(Query q : _querySpace){
				double bid = 0.0;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
				else
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .9);
				bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
			}
		}

		return bidBundle;
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	public void debug(Object str) {
		if(DEBUG) {
			System.out.println(str);
		}
	}

}
