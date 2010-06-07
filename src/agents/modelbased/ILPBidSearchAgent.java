package agents.modelbased;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import models.AbstractModel;
import models.bidtocpc.AbstractBidToCPC;
import models.bidtocpc.EnsembleBidToCPC;
import models.bidtocpc.RegressionBidToCPC;
import models.bidtocpc.WEKAEnsembleBidToCPC;
import models.bidtoprclick.AbstractBidToPrClick;
import models.bidtoprclick.EnsembleBidToPrClick;
import models.bidtoprclick.RegressionBidToPrClick;
import models.bidtoprclick.WEKAEnsembleBidToPrClick;
import models.postoprclick.RegressionPosToPrClick;
import models.prconv.AbstractConversionModel;
import models.prconv.BasicConvPrModel;
import models.prconv.GoodConversionPrModel;
import models.prconv.HistoricPrConversionModel;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.BasicQueryToNumImp;
import models.sales.SalesDistributionModel;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.unitssold.UnitsSoldMovingAvg;
import models.usermodel.AbstractUserModel;
import models.usermodel.BasicUserModel;
import agents.AbstractAgent;
import agents.AbstractAgent.Predictions;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;
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
public class ILPBidSearchAgent extends AbstractAgent {

	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = false;

	private double _safetyBudget = 800;

	private Random _R = new Random();
	private boolean DEBUG = false;
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
	private ArrayList<Double> _capList;
	private int lagDays = 5;
	private boolean salesDistFlag;
	private IloCplex _cplex;
	private int _capIncrement;

	public ILPBidSearchAgent(int capIncrement) {

		try {
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
			_cplex = cplex;
		} catch (IloException e) {
			throw new RuntimeException("Could not initialize CPLEX");
		}

		_bidList = new LinkedList<Double>();
		//		double increment = .25;
		double bidIncrement  = .05;
		double bidMin = .04;
		double bidMax = 1.65;
		int tot = (int) Math.ceil((bidMax-bidMin) / bidIncrement);
		for(int i = 0; i < tot; i++) {
			_bidList.add(bidMin+(i*bidIncrement));
		}

		salesDistFlag = false;
		
		_capIncrement = capIncrement;
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
		AbstractBidToCPC bidToCPC = new WEKAEnsembleBidToCPC(_querySpace, 10, 10, true, false);
		AbstractBidToPrClick bidToPrClick = new WEKAEnsembleBidToPrClick(_querySpace, 10, 10, basicTargModel, true, true);
		BasicConvPrModel convPrModel = new BasicConvPrModel(userModel, _querySpace, _baseConvProbs);
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
				//				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)"+model);
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

		}

		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			_queryId.put(q, i);
			i++;
		}
		
		_capList = new ArrayList<Double>();
		double maxCap = _capacity;
		for(i = 1; i <= maxCap; i+= _capIncrement) {
			_capList.add(1.0*i);
		}
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
				//				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)");
			}
		}
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();

		if(SAFETYBUDGET) {
			bidBundle.setCampaignDailySpendLimit(_safetyBudget);
		}

//		System.out.println("Day: " + _day);

		if(_day > 1) {
			if(!salesDistFlag) {
				SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
				_salesDist = salesDist;
				salesDistFlag = true;
			}
			_salesDist.updateModel(_salesReport);
		}


		if(_day > lagDays){
			buildMaps(models);
			//NEED TO USE THE MODELS WE ARE PASSED!!!

			/*
			 * Setting up CPLEX
			 */

			try {
				_cplex.clearModel();

				double remainingCap;
				if(_day < 4) {
					remainingCap = _capacity/((double)_capWindow);
				}
				else {
//					capacity = Math.max(_capacity/((double)_capWindow)*(1/3.0),_capacity - _unitsSold.getWindowSold());
					remainingCap = _capacity - _unitsSold.getWindowSold();
					debug("Unit Sold Model Budget "  +remainingCap);
				}

				debug("Budget: "+ remainingCap);

				HashMap<Query,ArrayList<Predictions>> allPredictionsMap = new HashMap<Query, ArrayList<Predictions>>();
				for(Query q : _querySpace) {
					debug("Query: " + q);
					ArrayList<Predictions> queryPredictions = new ArrayList<Predictions>();
					for(int i = 0; i < _bidList.size(); i++) {
						double bid = _bidList.get(i);
						double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
						double numImps = _queryToNumImpModel.getPrediction(q,(int) (_day+1));
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
						queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
					}
					allPredictionsMap.put(q, queryPredictions);
				}

				HashMap<Query,Item> bestSolution = solveWithCPLEX(remainingCap,0,allPredictionsMap);
				double[] bestSolVal = solutionValueMultiDay2(bestSolution,remainingCap,allPredictionsMap,5);
				int bestIdx = -1;
				//			System.out.println("Init val: " + bestSolVal);
				for(int i = 0; i < _capList.size(); i++) {
					HashMap<Query,Item> solution = solveWithCPLEX(remainingCap, Math.max(0,remainingCap)+_capList.get(i),allPredictionsMap);
					double[] solVal = solutionValueMultiDay2(solution,remainingCap,allPredictionsMap,5);
					if(solVal[0] > bestSolVal[0]) {
						bestSolVal[0] = solVal[0];
						bestSolution = solution;
						bestIdx = i;
					}
					//				System.out.println("OverCap By: " + capList.get(i) + ", val: " + solVal);
				}
				//			System.out.println("Best Index: " + bestIdx + ", Best val: " + bestSolVal);

				if(bestSolVal[0] < 0) {
					bestSolution = new HashMap<Query,Item>();
				}

				//set bids
				for(Query q : _querySpace) {
					ArrayList<Predictions> queryPrediction = allPredictionsMap.get(q);
					double bid;

					if(bestSolution.containsKey(q)) {
						int bidIdx = bestSolution.get(q).idx();
						Predictions predictions = queryPrediction.get(bidIdx);
						double clickPr = predictions.getClickPr();
						double numImps = predictions.getNumImp();
						int numClicks = (int) (clickPr * numImps);
						double CPC = predictions.getCPC();

						if(bestSolution.get(q).targ()) {

							bidBundle.setBid(q, _bidList.get(bidIdx));

							if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
								bidBundle.setAd(q, new Ad(new Product(_manSpecialty, _compSpecialty)));
							if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getComponent() == null)
								bidBundle.setAd(q, new Ad(new Product(q.getManufacturer(), _compSpecialty)));
							if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getManufacturer() == null)
								bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
							if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && q.getManufacturer().equals(_manSpecialty)) 
								bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
						}
						else {
							bidBundle.addQuery(q, _bidList.get(bidIdx), new Ad());
						}

						if(BUDGET) {
							bidBundle.setDailyLimit(q, numClicks*CPC);
						}
					}
					else {
						/*
						 * We decided that we did not want to be in this query, so we will use it to explore the space
						 */
						//					bid = 0.0;
						//					bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
						//					System.out.println("Bidding " + bid + "   for query: " + q);

						if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
							bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
						else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
							bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
						else
							bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);

						//					System.out.println("Exploring " + q + "   bid: " + bid);
						bidBundle.addQuery(q, bid, new Ad(), bid*10);
					}
				}
				
				/*
				 * Pass expected conversions to unit sales model
				 */
				double solutionWeight = solutionWeight(remainingCap,bestSolution,allPredictionsMap);
				((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);

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
//		System.out.println(bidBundle);
		return bidBundle;
	}
	
	public HashMap<Query,Item> solveWithCPLEX(double remainingCap, double desiredSales, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) throws IloException {
		_cplex.clearModel();
		
		if(remainingCap <= 0 && desiredSales <= 0) {
			return new HashMap<Query,Item>();
		}

		/*
		 * Setup Maximization
		 */
		IloLinearNumExpr profitExpr = _cplex.linearNumExpr();
		IloLinearNumExpr conversionExpr = _cplex.linearNumExpr();
		IloIntVar[] binVars = _cplex.boolVarArray(_bidList.size()*_querySpace.size());
		double penalty = getPenalty(remainingCap,desiredSales);
		for(Query q : _querySpace) {
			ArrayList<Predictions> queryPredictions = allPredictionsMap.get(q);
			IloLinearIntExpr bidOnceExpr = _cplex.linearIntExpr();
			for(int i = 0; i < _bidList.size(); i++) {
				Predictions predictions = queryPredictions.get(i);
				double salesPrice = _salesPrices.get(q);
				double clickPr = predictions.getClickPr();
				double numImps = predictions.getNumImp();
				int numClicks = (int) (clickPr * numImps);
				double CPC = predictions.getCPC();
				double convProb = getConversionPrWithPenalty(q, penalty);

				if(Double.isNaN(CPC)) {
					CPC = 0.0;
				}

				if(Double.isNaN(clickPr)) {
					clickPr = 0.0;
				}

				if(Double.isNaN(convProb)) {
					convProb = 0.0;
				}

				int isID = _queryId.get(q);
				double w = numClicks*convProb;				//weight = numClciks * convProv
				double v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
				int idx = isID*_bidList.size() + i;
				profitExpr.addTerm(v, binVars[idx]);
				conversionExpr.addTerm(w, binVars[idx]);
				bidOnceExpr.addTerm(1, binVars[idx]);
			}
			_cplex.addLe(bidOnceExpr, 1);
		}

		_cplex.addMaximize(profitExpr);
		_cplex.addLe(conversionExpr, desiredSales);

		double start = System.currentTimeMillis();
		_cplex.solve();
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
//		System.out.println("CPLEX took " + (elapsed / 1000) + " seconds");

//		System.out.println("Expected Profit: " + _cplex.getObjValue());

		double[] bidVal = _cplex.getValues(binVars);

		HashMap<Query,Item> solution = new HashMap<Query,Item>();
		for(Query q : _querySpace) {
			Integer isID = _queryId.get(q);
			double bid = 0.0;
			for(int i = 0; i < _bidList.size(); i++) {
				int idx = isID*_bidList.size() + i;
				if(bidVal[idx] == 1) {
					bid = _bidList.get(i);
					Item item = new Item(q, 0, 0, bid, false, isID, i);
					solution.put(q,item);
					break;
				}
			}
		}
		return solution;
	}
	
	private double[] solutionValueMultiDay2(HashMap<Query, Item> solution, double remainingCap, HashMap<Query,ArrayList<Predictions>> allPredictionsMap, int numDays) {
		double totalWeight = solutionWeight(remainingCap, solution, allPredictionsMap);
		double penalty = getPenalty(remainingCap, totalWeight);

		double totalValue = 0;
		for(Query q : _querySpace) {
			if(solution.containsKey(q)) {
				Item item = solution.get(q);
				Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
				totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penalty)*_salesPrices.get(item.q()) - prediction.getCPC());
			}
		}

		double daysLookahead = Math.max(0, Math.min(numDays, 58 - _day));
		if(daysLookahead > 0 && totalWeight > 0) {
			ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			ArrayList<Integer> soldArray = getCopy(soldArrayTMP);

			Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			if(expectedConvsYesterday == null) {
				expectedConvsYesterday = 0;
				int counter2 = 0;
				for(int j = 0; j < 5 && j < soldArray.size(); j++) {
					expectedConvsYesterday += soldArray.get(soldArray.size()-1-j);
					counter2++;
				}
				expectedConvsYesterday /= (double)counter2;
			}
			soldArray.add(expectedConvsYesterday);
			soldArray.add((int) totalWeight);

			for(int i = 0; i < daysLookahead; i++) {
				double expectedBudget = _capacity;
				for(int j = 0; j < _capWindow-1; j++) {
					expectedBudget -= soldArray.get(soldArray.size()-1-j);
				}

				double numSales = solutionWeight(expectedBudget, solution, allPredictionsMap);
				soldArray.add((int) numSales);

				double penaltyNew = getPenalty(expectedBudget, numSales);
				for(Query q : _querySpace) {
					if(solution.containsKey(q)) {
						Item item = solution.get(q);
						Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
						totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penaltyNew)*_salesPrices.get(item.q()) - prediction.getCPC());
					}
				}
			}
		}
		double[] output = new double[2];
		output[0] = totalValue;
		output[1] = totalWeight;
		return output;
	}

	private double[] solutionValueMultiDay(HashMap<Query, Item> solution, double remainingCap, HashMap<Query,ArrayList<Predictions>> allPredictionsMap) {
		double totalWeight = solutionWeight(remainingCap, solution, allPredictionsMap);

		double penalty = getPenalty(remainingCap, totalWeight);

		double totalValue = 0;
		double solWeight = 0;
		HashMap<Query, Double> currentSalesDist = new HashMap<Query,Double>();
		for(Query q : _querySpace) {
			if(solution.containsKey(q)) {
				Item item = solution.get(q);
				Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
				totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penalty)*_salesPrices.get(item.q()) - prediction.getCPC());
				double tmpWeight = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(q, penalty);
				solWeight += tmpWeight;
				currentSalesDist.put(q, tmpWeight);
			}
		}

		for(Query q : _querySpace) {
			if(currentSalesDist.containsKey(q)) {
				currentSalesDist.put(q, currentSalesDist.get(q)/solWeight);
			}
			else {
				currentSalesDist.put(q, 0.0);
			}
		}

		double valueLostWindow = Math.max(0, Math.min(_capWindow-1, 58 - _day));
		//		double valueLostWindow = Math.max(1, Math.min(_capWindow, 58 - _day));
		double multiDayLoss = 0;
		if(valueLostWindow > 0 && solWeight > 0) {
			ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			ArrayList<Integer> soldArray = getCopy(soldArrayTMP);

			Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			if(expectedConvsYesterday == null) {
				expectedConvsYesterday = 0;
				int counter2 = 0;
				for(int j = 0; j < 5 && j < soldArray.size(); j++) {
					expectedConvsYesterday += soldArray.get(soldArray.size()-1-j);
					counter2++;
				}
				expectedConvsYesterday /= (double)counter2;
			}
			soldArray.add(expectedConvsYesterday);
			soldArray.add((int) solWeight);

			for(int i = 0; i < valueLostWindow; i++) {
				double expectedBudget = _capacity;

				for(int j = 0; j < _capWindow-1; j++) {
					expectedBudget -= soldArray.get(soldArray.size()-1-j);
				}

				double valueLost = 0;

				double numSales = solutionWeight(expectedBudget, solution, allPredictionsMap);
				soldArray.add((int) numSales);

				for(int j = 1; j <= numSales; j++) {
					if(expectedBudget - j <= 0) {
						valueLost += 1.0/Math.pow(_lambda, Math.abs(expectedBudget-j-1)) - 1.0/Math.pow(_lambda, Math.abs(expectedBudget-j));
					}
				}

				double avgConvProb = 0; //the average probability of conversion;
				for(Query q : _querySpace) {
					avgConvProb += getConversionPrWithPenalty(q, getPenalty(expectedBudget, numSales)) * currentSalesDist.get(q);
				}

				double avgCPC = 0;
				for(Query q : _querySpace) {
					if(solution.containsKey(q)) {
						Item item = solution.get(q);
						Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
						avgCPC += prediction.getCPC() * currentSalesDist.get(q);
					}
				}

				double avgCPA = avgCPC/avgConvProb;

				multiDayLoss += Math.max(valueLost*avgCPA,0);
			}
		}
		double[] output = new double[2];
		output[0] = totalValue-multiDayLoss;
		output[1] = solWeight;
		return output;
	}

	private ArrayList<Integer> getCopy(ArrayList<Integer> soldArrayTMP) {
		ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP.size());
		for(int i = 0; i < soldArrayTMP.size(); i++) {
			soldArray.add(soldArrayTMP.get(i));
		}
		return soldArray;
	}
	
	private double getPenalty(double remainingCap, double solutionWeight) {
		double penalty;
		solutionWeight = Math.max(0,solutionWeight);
		if(remainingCap < 0) {
			if(solutionWeight <= 0) {
				penalty = Math.pow(_lambda, Math.abs(remainingCap));
			}
			else {
				penalty = 0.0;
				int num = 0;
				for(double j = Math.abs(remainingCap)+1; j <= Math.abs(remainingCap)+solutionWeight; j++) {
					penalty += Math.pow(_lambda, j);
					num++;
				}
				penalty /= (num);
			}
		}
		else {
			if(solutionWeight <= 0) {
				penalty = 1.0;
			}
			else {
				if(solutionWeight > remainingCap) {
					penalty = remainingCap;
					for(int j = 1; j <= solutionWeight-remainingCap; j++) {
						penalty += Math.pow(_lambda, j);
					}
					penalty /= (solutionWeight);
				}
				else {
					penalty = 1.0;
				}
			}
		}
		if(Double.isNaN(penalty)) {
			penalty = 1.0;
		}
		return penalty;
	}
	
	private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap, BidBundle bidBundle) {
		double threshold = .5;
		int maxIters = 40;
		double lastSolWeight = Double.MAX_VALUE;
		double solutionWeight = 0.0;

		/*
		 * As a first estimate use the weight of the solution
		 * with no penalty
		 */
		for(Query q : _querySpace) {
			if(solution.get(q) == null) {
				continue;
			}
			Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
			double dailyLimit = Double.NaN;
			if(bidBundle != null) {
				dailyLimit  = bidBundle.getDailyLimit(q);
			}
			double clickPr = predictions.getClickPr();
			double numImps = predictions.getNumImp();
			int numClicks = (int) (clickPr * numImps);
			double CPC = predictions.getCPC();
			double convProb = getConversionPrWithPenalty(q, 1.0);

			if(Double.isNaN(CPC)) {
				CPC = 0.0;
			}

			if(Double.isNaN(clickPr)) {
				clickPr = 0.0;
			}

			if(Double.isNaN(convProb)) {
				convProb = 0.0;
			}

			if(!Double.isNaN(dailyLimit)) {
				if(numClicks*CPC > dailyLimit) {
					numClicks = (int) (dailyLimit/CPC);
				}
			}

			solutionWeight += numClicks*convProb;
		}

		double originalSolWeight = solutionWeight;

		int numIters = 0;
		while(Math.abs(lastSolWeight-solutionWeight) > threshold) {
			numIters++;
			if(numIters > maxIters) {
				numIters = 0;
				solutionWeight = (_R.nextDouble() + .5) * originalSolWeight; //restart the search
				threshold *= 1.5; //increase the threshold
				maxIters *= 1.25;
			}
			lastSolWeight = solutionWeight;
			solutionWeight = 0;
			double penalty = getPenalty(budget, lastSolWeight);
			for(Query q : _querySpace) {
				if(solution.get(q) == null) {
					continue;
				}
				Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
				double dailyLimit = Double.NaN;
				if(bidBundle != null) {
					dailyLimit  = bidBundle.getDailyLimit(q);
				}
				double clickPr = predictions.getClickPr();
				double numImps = predictions.getNumImp();
				int numClicks = (int) (clickPr * numImps);
				double CPC = predictions.getCPC();
				double convProb = getConversionPrWithPenalty(q, penalty);

				if(Double.isNaN(CPC)) {
					CPC = 0.0;
				}

				if(Double.isNaN(clickPr)) {
					clickPr = 0.0;
				}

				if(Double.isNaN(convProb)) {
					convProb = 0.0;
				}

				if(!Double.isNaN(dailyLimit)) {
					if(numClicks*CPC > dailyLimit) {
						numClicks = (int) (dailyLimit/CPC);
					}
				}

				solutionWeight += numClicks*convProb;
			}
		}
		return solutionWeight;
	}

	private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) {
		return solutionWeight(budget, solution, allPredictionsMap, null);
	}
	
	public double getConversionPrWithPenalty(Query q, double penalty) {
		double convPr;
		String component = q.getComponent();
		if(_compSpecialty.equals(component)) {
			convPr = eta(_convPrModel.getPrediction(q)*penalty,1+_CSB);
		}
		else if(component == null) {
			convPr = eta(_convPrModel.getPrediction(q)*penalty,1+_CSB) * (1/3.0) + _convPrModel.getPrediction(q)*penalty*(2/3.0);
		}
		else {
			convPr = _convPrModel.getPrediction(q)*penalty;
		}
		return convPr;
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

	@Override
	public String toString() {
		return "ILPBidSearch(" + _capIncrement + ")";
	}

	@Override
	public AbstractAgent getCopy() {
		return new ILPBidSearchAgent(_capIncrement);
	}
}
