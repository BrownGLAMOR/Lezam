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
public class DynamicMCKP extends AbstractAgent {


	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = false;

	private double _safetyBudget = 800;

	private Random _R;
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
	private int lagDays = 4;
	private boolean salesDistFlag;
	HashMap<Query, LinkedList<Double>> _prClickNoise, _CPCNoise, _convPrNoise;
	public boolean NOISE;

	public DynamicMCKP() {
		//		_R.setSeed(124962748);
		_R = new Random(616866);
		_bidList = new LinkedList<Double>();
		//		double increment = .25;
		double bidIncrement  = .07;
		double bidMin = .04;
		double bidMax = 1.65;
		int tot = (int) Math.ceil((bidMax-bidMin) / bidIncrement);
		for(int i = 0; i < tot; i++) {
			_bidList.add(bidMin+(i*bidIncrement));
		}
		NOISE = false;
	}

	public DynamicMCKP(HashMap<Query, LinkedList<Double>> prClickNoise, HashMap<Query, LinkedList<Double>> CPCNoise, HashMap<Query, LinkedList<Double>> convPrNoise) {
		_prClickNoise = prClickNoise;
		_CPCNoise = CPCNoise;
		_convPrNoise = convPrNoise;
		NOISE = true;
		//		_R.setSeed(124962748);
		_R = new Random(616866);
		_bidList = new LinkedList<Double>();
		//		double increment = .25;
		double bidIncrement  = .07;
		double bidMin = .04;
		double bidMax = 1.65;
		int tot = (int) Math.ceil((bidMax-bidMin) / bidIncrement);
		for(int i = 0; i < tot; i++) {
			_bidList.add(bidMin+(i*bidIncrement));
		}
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
		AbstractBidToCPC bidToCPC = new WEKAEnsembleBidToCPC(_querySpace, 10, 15, true, true);
		AbstractBidToPrClick bidToPrClick = new WEKAEnsembleBidToPrClick(_querySpace, 10, 15, basicTargModel, true, true);
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
		salesDistFlag = false;
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

		buildMaps(models);

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
			//NEED TO USE THE MODELS WE ARE PASSED!!!

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

					/*
					 * Click probability should always be increasing
					 * with our current models
					 */
					if(i > 0) {
						if(clickPr < queryPredictions.get(i-1).getClickPr()) {
							clickPr = queryPredictions.get(i-1).getClickPr();
						}
					}

					if(Double.isNaN(convProb)) {
						convProb = 0.0;
					}


					if(NOISE) {
						clickPr += _prClickNoise.get(q).get((int) _day);
						CPC += _CPCNoise.get(q).get((int) _day);
						convProb += _convPrNoise.get(q).get((int) _day);
					}

					clickPr = Math.max(0.0, clickPr);
					CPC = Math.max(0.0, CPC);
					convProb = Math.max(0.0, convProb);

					queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
				}
				allPredictionsMap.put(q, queryPredictions);
			}


			HashMap<Query,Integer> intSolution = dynFillKnapsack(allPredictionsMap, remainingCap);
			//Turn into same form as solution
			HashMap<Query,Item> solution = new HashMap<Query,Item>();
			for(Query q : _querySpace) {
				if(intSolution.containsKey(q) && intSolution.get(q) >= 0) {
					solution.put(q, new Item(q, 0, 0,_bidList.get(intSolution.get(q)),false, 0,  intSolution.get(q)));
				}
			}

			//set bids
			for(Query q : _querySpace) {
				ArrayList<Predictions> queryPrediction = allPredictionsMap.get(q);
				double bid;

				if(solution.containsKey(q)) {
					int bidIdx = solution.get(q).idx();
					Predictions predictions = queryPrediction.get(bidIdx);
					double clickPr = predictions.getClickPr();
					double numImps = predictions.getNumImp();
					int numClicks = (int) (clickPr * numImps);
					double CPC = predictions.getCPC();

					if(solution.get(q).targ()) {

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

					bid = randDouble(.04,_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .7);

					//					System.out.println("Exploring " + q + "   bid: " + bid);
					bidBundle.addQuery(q, bid, new Ad(), bid*5);
				}
			}

			/*
			 * Pass expected conversions to unit sales model
			 */
			double solutionWeight = solutionWeight(remainingCap,solution,allPredictionsMap);
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
		}
		else {
			for(Query q : _querySpace){
				if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
					double bid = randDouble(_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .35, _salesPrices.get(q) * getConversionPrWithPenalty(q,1.0)* _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), Double.MAX_VALUE);
				}
				else {
					double bid = randDouble(.04,_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), bid*10);
				}
			}
			bidBundle.setCampaignDailySpendLimit(800);
		}
		//		System.out.println(bidBundle);
		return bidBundle;
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

	private ArrayList<Integer> getCopy(ArrayList<Integer> soldArrayTMP) {
		ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP.size());
		for(int i = 0; i < soldArrayTMP.size(); i++) {
			soldArray.add(soldArrayTMP.get(i));
		}
		return soldArray;
	}



	private HashMap<Query,Integer> dynFillKnapsack(HashMap<Query,ArrayList<Predictions>> allPredictionsMap, double remCap) {
		HashMap<Query,Integer> solution = new HashMap<Query,Integer>();
		HashMap<Query,Integer> nextUndomIndex = new HashMap<Query,Integer>();
		for(Query q : _querySpace) {
			solution.put(q, -1);
			nextUndomIndex.put(q, 0);
		}
		while(true) {
			HashMap<Query,Item> itemSolution = new HashMap<Query,Item>();
			for(Query q : _querySpace) {
				if(solution.containsKey(q) && solution.get(q) >= 0) {
					itemSolution.put(q, new Item(q, 0, 0,_bidList.get(solution.get(q)),false, 0,  solution.get(q)));
				}
			}

			double[] solVal = solutionValueMultiDay2(itemSolution, remCap, allPredictionsMap, 10);
			double penalty = getPenalty(remCap, solVal[1]);

			double bestEff = 0;
			Query bestQ = null;
			for(Query q : _querySpace) {
				ArrayList<Predictions> predictions = allPredictionsMap.get(q);
				if(solution.get(q) == predictions.size()-1 || nextUndomIndex.get(q) >= predictions.size()) {
					continue; //we are done with this query
				}

				while(isDominatedEric(predictions, solution.get(q), nextUndomIndex.get(q), penalty, q)) {
					nextUndomIndex.put(q, nextUndomIndex.get(q)+1);
				}

				if(nextUndomIndex.get(q) >= predictions.size()) {
					continue;
				}

				double eff;
				if(solution.get(q) > -1) {
					double[] currVW = getValueAndWeight(predictions.get(solution.get(q)),penalty,q);
					double[] nextVW = getValueAndWeight(predictions.get(nextUndomIndex.get(q)),penalty,q);
					eff = (nextVW[0] - currVW[0])/(nextVW[1]-currVW[1]);
				}
				else {
					double[] nextVW = getValueAndWeight(predictions.get(nextUndomIndex.get(q)),penalty,q);
					eff = nextVW[0]/nextVW[1];
				}

				if(eff > bestEff) {
					bestEff = eff;
					bestQ = q;
				}
				else if(eff == bestEff) {
					//					System.out.println("Equality is happening and we aren't handling it!");
				}
			}
			if(bestQ == null) {
				break;
			}

			solution.put(bestQ, nextUndomIndex.get(bestQ));
			nextUndomIndex.put(bestQ,nextUndomIndex.get(bestQ)+1);

			HashMap<Query,Item> newItemSolution = new HashMap<Query,Item>();
			for(Query q : _querySpace) {
				if(solution.containsKey(q) && solution.get(q) >= 0) {
					newItemSolution.put(q, new Item(q, 0, 0,_bidList.get(solution.get(q)),false, 0,  solution.get(q)));
				}
			}

			double[] newSolVal = solutionValueMultiDay2(newItemSolution, remCap, allPredictionsMap, 10);
			//			System.out.println("(" + newSolVal[0] + ", " + newSolVal[1] + ")");

			if(newSolVal[0] < solVal[0]) {
				break;
			}

			/*
			 * Check if there are any items left
			 */
			boolean itemsLeft = false;
			for(Query q : _querySpace) {
				if(nextUndomIndex.get(q) <= allPredictionsMap.get(q).size()-1) {
					itemsLeft = true;
					break;
				}
			}

			if(!itemsLeft) {
				break;
			}
		}
		return solution;
	}

	private boolean isDominatedEric(ArrayList<Predictions> predictions, int lastIndex, int currIndex, double penalty, Query q) {
		//If we are currently considering an item that's out of bounds, return false
		int numPredictions = predictions.size();
		if(currIndex >= numPredictions) {
			return false;
		}

		//---
		//Check for dominated items
		//---

		//Make sure there's no item with <= weight but >= value
		//if there is such a thing, this item is dominated.
		double[] item_i = getValueAndWeight(predictions.get(currIndex),penalty,q);
		double v_i = item_i[0];
		double w_i = item_i[1];

		//Check all items with <= weight.
		//NOTE: this would be only items that come before this one,
		// but items that come after could have the same weight and higher value.
		//Split this up into 2 cases.

		//Case 1: items with <= weight, and potentially higher value
		for (int j=currIndex-1; j>=0; j--) {
			double[] item_j = getValueAndWeight(predictions.get(j),penalty,q);
			double v_j = item_j[0];
			double w_j = item_j[1];

			// See if item j dominates item i.
			//(we already know w_i >= w_j, but I'm being redundant...)
			//v_i<=v_j instead of strictly < to handle duplicate items. 
			//  (assume the higher-bid item is dominated)
			if (w_i >= w_j && v_i <= v_j) {
				return true;
			}
		}


		//Case 2: items with == weight, and potentially higher value
		for (int j=currIndex+1; j<numPredictions; j++) {
			double[] item_j = getValueAndWeight(predictions.get(j),penalty,q);
			double v_j = item_j[0];
			double w_j = item_j[1];

			//Slight speedup: we can stop checking if we find a w_i < w_j.
			if (w_i < w_j) break;

			if (w_i >= w_j && v_i < v_j) {
				return true;
			}
		}

		//---
		//Check for LP dominated items
		//TODO: This is not nearly as efficient as it could be.
		//---

		//Get item j where w_j < w_i
		for (int j=currIndex-1; j>=-1; j--) {			
			double v_j;
			double w_j;

			//NOTE: When j=-1, this is the "0" item: w_j = v_j = 0.
			//Check to see if this results in a dominated item as well.
			if (j==-1) {
				v_j = 0;
				w_j = 0;
			} else {
				double[] item_j = getValueAndWeight(predictions.get(j),penalty,q);
				v_j = item_j[0];
				w_j = item_j[1];
			}

			//Get item k where w_k > w_i	
			for (int k=currIndex+1; k<numPredictions; k++) {
				double[] item_k = getValueAndWeight(predictions.get(k),penalty,q);
				double v_k = item_k[0];
				double w_k = item_k[1];

				//Check to see if LP dominated.
				if (w_j < w_i && w_i < w_k) {
					if (v_j < v_i && v_i < v_k) {
						double efficiency1 = (v_k - v_i)/(w_k-w_i);
						double efficiency2 = (v_i - v_j)/(w_i-w_j);
						if (efficiency1 >= efficiency2) {
							return true;
						}
					}
				}
			}
		}

		//If you get here, item is not dominated or LP dominated.
		return false;
	}


	//FIXME: There is similar code between this and getIncItemsForOverCapLevel. Abstract this out.
	private double[] getValueAndWeight(Predictions prediction, double penalty, Query q) {
		double salesPrice = _salesPrices.get(q);
		double clickPr = prediction.getClickPr();
		double numImps = prediction.getNumImp();
		//int numClicks = (int) (clickPr * numImps);
		double numClicks = clickPr * numImps;
		double CPC = prediction.getCPC();
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


		double[] preds = new double[2];
		preds[0] = v;
		preds[1] = w;

		//		preds[0] =  prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penalty)*_salesPrices.get(q) - prediction.getCPC());
		//		preds[1] =  prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(q, penalty);
		return preds;
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
				//TODO: What if item has the same value? currently it is removed!
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
		double pred = _convPrModel.getPrediction(q);
		pred += _convPrNoise.get(q).get((int) _day);
		pred = Math.max(0.0, pred);
		if(_compSpecialty.equals(component)) {
			convPr = eta(pred*penalty,1+_CSB);
		}
		else if(component == null) {
			convPr = eta(pred*penalty,1+_CSB) * (1/3.0) + pred*penalty*(2/3.0);
		}
		else {
			convPr = pred*penalty;
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
		return "DynamicMCKPBid";
	}

	@Override
	public AbstractAgent getCopy() {
		if(NOISE) {
			return new DynamicMCKP(_prClickNoise, _CPCNoise, _convPrNoise);
		}
		else {
			return new DynamicMCKP();
		}
	}		
}
