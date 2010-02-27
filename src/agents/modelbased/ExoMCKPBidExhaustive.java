package agents.modelbased;

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
public class ExoMCKPBidExhaustive extends AbstractAgent {

	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = false;

	private double _safetyBudget = 800;

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
	private ArrayList<Double> _capList;
	private int lagDays = 4;
	private boolean salesDistFlag;
	private int _capIncrement;

	public ExoMCKPBidExhaustive(int capIncrement) {
//		_R.setSeed(124962748);
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
		for(i = 1; i <= maxCap; i+= 10) {
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
		else {
			bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);
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

			HashMap<Query,Item> bestSolution = fillKnapsack(getIncItemsForOverCapLevel(remainingCap,0,allPredictionsMap), Math.max(0,remainingCap));
			double[] bestSolVal = solutionValueMultiDay(bestSolution,remainingCap,allPredictionsMap);
			for(int i = 0; i < _capList.size(); i++) {
				HashMap<Query,Item> solution = fillKnapsack(getIncItemsForOverCapLevel(remainingCap, Math.max(0,remainingCap)+_capList.get(i),allPredictionsMap), Math.max(0,remainingCap)+_capList.get(i));
				double[] solVal = solutionValueMultiDay(solution,remainingCap,allPredictionsMap);
				if(solVal[0] > bestSolVal[0]) {
					bestSolVal[0] = solVal[0];
					bestSolution = solution;
				}
			}

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
					else {
						bidBundle.setDailyLimit(q, Integer.MAX_VALUE);
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
						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .7);
					else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .7);
					else
						bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .7);

					//					System.out.println("Exploring " + q + "   bid: " + bid);
					bidBundle.addQuery(q, bid, new Ad(), bid*5);
				}
			}

			/*
			 * Pass expected conversions to unit sales model
			 */
			double solutionWeight = solutionWeight(remainingCap,bestSolution,allPredictionsMap);
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
		}
		else {
			for(Query q : _querySpace){
				if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
					double bid = randDouble(_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .35, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), Double.MAX_VALUE);
				}
				else {
					double bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), bid*10);
				}
			}
			bidBundle.setCampaignDailySpendLimit(800);
		}
		//		System.out.println(bidBundle);
		return bidBundle;
	}


	private double[] solutionValueMultiDay(HashMap<Query, Item> solution, double remainingCap, HashMap<Query,ArrayList<Predictions>> allPredictionsMap) {
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

		double valueLostWindow = Math.max(1, Math.min(_capWindow, 58 - _day));
		double multiDayLoss = 0;
		if(valueLostWindow > 0 && totalWeight > 0) {
			/*
			 * Calculate our avgUSp
			 */
			double avgUSP = 0;
			for(Query q : _querySpace) {
				if(_day < 2) {
					avgUSP += _salesPrices.get(q) / 16.0;
				}
				else {
					avgUSP += _salesPrices.get(q) * _salesDist.getPrediction(q);
				}
			}

			/*
			 * Calculate our avgCPC
			 */
			double avgCPC = 0;
			for(Query q : _querySpace) {
				int counter = 0;
				double tempCPC = 0;
				for(int j = 0; j < 5 && j < _queryReports.size(); j++) {
					counter++;
					tempCPC += _queryReports.get(_queryReports.size()-1-j).getCPC(q);
				}
				avgCPC += tempCPC/counter * _salesDist.getPrediction(q);
			}

			/*
			 * Get our sales history and add our guess for yesterday to it
			 */
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

			for(int day = 0; day < valueLostWindow; day++) {
				/*
				 * Calculate remaining capacity for given day
				 */
				double expectedBudget = _capacity;
				for(int j = 0; j < _capWindow-1; j++) {
					expectedBudget -= soldArray.get(soldArray.size()-1-j);
				}
				soldArray.add((int)totalWeight);

				double avgConvProb = 0; //the average probability of conversion;
				for(Query q : _querySpace) {
					avgConvProb += getConversionPrWithPenalty(q, 1.0) * _salesDist.getPrediction(q);
				}

				double valueLost = 0;
				for(int j = 1; j <= totalWeight; j++) {
					if(expectedBudget - j <= 0) {
						valueLost += 1.0/Math.pow(LAMBDA, Math.abs(expectedBudget-j-1)) - 1.0/Math.pow(LAMBDA, Math.abs(expectedBudget-j));
					}
				}
				multiDayLoss += Math.max(valueLost*avgCPC/avgConvProb,0);
				//				double baseConvProb;
				//				if(expectedBudget < 0) {
				//					baseConvProb = avgConvProb * Math.pow(LAMBDA, Math.abs(expectedBudget));
				//				}
				//				else {
				//					baseConvProb= avgConvProb;
				//				}
				//				for (int j = 1; j <= totalWeight; j++){
				//					if(expectedBudget - j < 0) {
				//						double iD = Math.pow(LAMBDA, Math.abs(expectedBudget-j));
				//						double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
				//						valueLost += (baseConvProb - worseConvProb)*avgUSP;
				//					}
				//				}
				//				multiDayLoss += Math.max(valueLost,0);
			}
		}
		double[] output = new double[2];
		output[0] = totalValue-multiDayLoss;
		output[1] = totalWeight;
		return output;
	}

	private ArrayList<Integer> getCopy(ArrayList<Integer> soldArrayTMP) {
		ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP.size());
		for(int i = 0; i < soldArrayTMP.size(); i++) {
			soldArray.add(soldArrayTMP.get(i));
		}
		return soldArray;
	}

	private ArrayList<IncItem> getIncItemsForOverCapLevel(double remainingCap, double desiredSales, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) {
		ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();
		double penalty = getPenalty(remainingCap,desiredSales);
		//		System.out.println("Creating KnapSack with " + overCap + " units over, penalty = " + penalty);
		for(Query q : _querySpace) {
			ArrayList<Item> itemList = new ArrayList<Item>();
			debug("Query: " + q);
			ArrayList<Predictions> queryPredictions = allPredictionsMap.get(q);
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

				double w = numClicks*convProb;				//weight = numClciks * convProv
				double v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
				itemList.add(new Item(q,w,v,_bidList.get(i),false,0,i));
			}
			debug("Items for " + q);
			Item[] items = itemList.toArray(new Item[0]);
			IncItem[] iItems = getIncremental(items);
			allIncItems.addAll(Arrays.asList(iItems));
		}
		Collections.sort(allIncItems);
		return allIncItems;
	}

	/**
	 * Greedily fill the knapsack by selecting incremental items
	 * @param incItems
	 * @param budget
	 * @return
	 */
	private HashMap<Query,Item> fillKnapsack(ArrayList<IncItem> incItems, double budget) {
		if(budget < 0) {
			return new HashMap<Query,Item>();
		}
		HashMap<Query,Item> solution = new HashMap<Query, Item>();
		for(IncItem ii: incItems) {
			//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
			//set replace lighter items as we want
			//TODO this can be >= ii.w() OR 0
			if(budget >= 0) {
				//				debug("adding item " + ii);
				solution.put(ii.item().q(), ii.item());
				budget -= ii.w();
			}
			else {
				break;
			}
		}
		return solution;
	}

	/**
	 * Get undominated items
	 * @param items
	 * @return
	 */
	public static Item[] getUndominated(Item[] items) {
		Arrays.sort(items,new ItemComparatorByWeight());
		//remove dominated items (higher weight, lower value)		
		ArrayList<Item> temp = new ArrayList<Item>();
		temp.add(items[0]);
		for(int i=1; i<items.length; i++) {
			Item lastUndominated = temp.get(temp.size()-1); 
			if(lastUndominated.v() < items[i].v()) {
				temp.add(items[i]);
			}
		}


		ArrayList<Item> betterTemp = new ArrayList<Item>();
		betterTemp.addAll(temp);
		for(int i = 0; i < temp.size(); i++) {
			ArrayList<Item> duplicates = new ArrayList<Item>();
			Item item = temp.get(i);
			duplicates.add(item);
			for(int j = i + 1; j < temp.size(); j++) {
				Item otherItem = temp.get(j);
				if(item.v() == otherItem.v() && item.w() == otherItem.w()) {
					duplicates.add(otherItem);
				}
			}
			if(duplicates.size() > 1) {
				betterTemp.removeAll(duplicates);
				double minBid = 10;
				double maxBid = -10;
				for(int j = 0; j < duplicates.size(); j++) {
					double bid = duplicates.get(j).b();
					if(bid > maxBid) {
						maxBid = bid;
					}
					if(bid < minBid) {
						minBid = bid;
					}
				}
				Item newItem = new Item(item.q(), item.w(), item.v(), (maxBid+minBid)/2.0, item.targ(), item.isID(),item.idx());
				betterTemp.add(newItem);
			}
		}

		//items now contain only undominated items
		items = betterTemp.toArray(new Item[0]);
		Arrays.sort(items,new ItemComparatorByWeight());

		//remove lp-dominated items
		ArrayList<Item> q = new ArrayList<Item>();
		q.add(new Item(new Query(),0,0,-1,false,1,0));//add item with zero weight and value

		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items now
			int l = q.size()-1;
			Item li = q.get(l);//last item
			Item nli = q.get(l-1);//next to last
			if(li.w() == nli.w()) {
				if(li.v() > nli.v()) {
					q.remove(l-1);
				}else{
					q.remove(l);
				}
			}
			l = q.size()-1; //reset in case an item was removed
			//while there are at least three elements and ...
			while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w()) 
					<= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
				q.remove(l-1);
				l--;
			}
		}

		//remove the (0,0) item
		if(q.get(0).w() == 0 && q.get(0).v() == 0) {
			q.remove(0);
		}

		Item[] uItems = (Item[]) q.toArray(new Item[0]);
		return uItems;
	}


	/**
	 * Get incremental items
	 * @param items
	 * @return
	 */
	public IncItem[] getIncremental(Item[] items) {
		debug("PRE INCREMENTAL");
		for(int i = 0; i < items.length; i++) {
			debug("\t" + items[i]);
		}

		Item[] uItems = getUndominated(items);

		debug("UNDOMINATED");
		for(int i = 0; i < uItems.length; i++) {
			debug("\t" + uItems[i]);
		}

		IncItem[] ii = new IncItem[uItems.length];

		if (uItems.length != 0){ //getUndominated can return an empty array
			ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0], null);
			for(int item=1; item<uItems.length; item++) {
				Item prev = uItems[item-1];
				Item cur = uItems[item];
				ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur, prev);
			}
		}
		debug("INCREMENTAL");
		for(int i = 0; i < ii.length; i++) {
			debug("\t" + ii[i]);
		}
		return ii;
	}

	private double getPenalty(double remainingCap, double solutionWeight) {
		double penalty;
		solutionWeight = Math.max(0,solutionWeight);
		if(remainingCap < 0) {
			if(solutionWeight <= 0) {
				penalty = Math.pow(LAMBDA, Math.abs(remainingCap));
			}
			else {
				penalty = 0.0;
				int num = 0;
				for(double j = Math.abs(remainingCap)+1; j <= Math.abs(remainingCap)+solutionWeight; j++) {
					penalty += Math.pow(LAMBDA, j);
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
						penalty += Math.pow(LAMBDA, j);
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
		return "ExoMCKPBidExhaustive(" + _capIncrement + ")";
	}

	@Override
	public AbstractAgent getCopy() {
		return new ExoMCKPBidExhaustive(_capIncrement);
	}
}
