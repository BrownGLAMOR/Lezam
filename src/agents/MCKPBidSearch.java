package agents;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.ArrayList;
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
import agents.AbstractAgent.Predictions;
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
 * @author jberg, spucci, vnarodit
 *
 */
public class MCKPBidSearch extends AbstractAgent {

	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean TARGET = false;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = true;

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
	private ArrayList<Double> bidList;
	private int lagDays = 5;
	private boolean salesDistFlag;
	private int _capIncrement = 15;
	ArrayList<Double> capList;
	
	public MCKPBidSearch() {
		this(30);
	}

	public MCKPBidSearch(int capIncrement) {
		_R.setSeed(124962748);
		_capIncrement = capIncrement;
		bidList = new ArrayList<Double>();
		//		double increment = .25;
		double increment  = .04;
		double min = .04;
		double max = 1.65;
		int tot = (int) Math.ceil((max-min) / increment);
		for(int i = 0; i < tot; i++) {
			bidList.add(min+(i*increment));
		}

		salesDistFlag = false;
	}



	@Override
	public Set<AbstractModel> initModels() {
		initBidder();
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
//		GoodConversionPrModel convPrModel = new GoodConversionPrModel(_querySpace,basicTargModel);
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

			String component = q.getComponent();
			if(_compSpecialty.equals(component)) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB));
			}
			else if(component == null) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB)*(1/3.0) + _baseConvProbs.get(q)*(2/3.0));
			}
		}

		capList = new ArrayList<Double>();
		double maxCap = _capacity;
		for(int i = 1; i <= maxCap; i+= _capIncrement) {
			capList.add(1.0*i);
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
		double start = System.currentTimeMillis();
		BidBundle bidBundle = new BidBundle();

		if(SAFETYBUDGET) {
			bidBundle.setCampaignDailySpendLimit(_safetyBudget);
		}

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

			double budget = _capacity/_capWindow;
			if(_day < 4) {
				//do nothing
			}
			else {
				//				budget = Math.max(20,_capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4);
				budget = _capacity - _unitsSold.getWindowSold();
				debug("Unit Sold Model Budget "  +budget);
			}

			HashMap<Query,ArrayList<Predictions>> allPredictionsMap = new HashMap<Query, ArrayList<Predictions>>();
			for(Query q : _querySpace) {
				ArrayList<Predictions> queryPredictions = new ArrayList<Predictions>();
				for(int i = 0; i < bidList.size(); i++) {
					double bid = bidList.get(i);
					double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
					double numImps = _queryToNumImpModel.getPrediction(q,(int) (_day+1));
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

					if(TARGET) {
						/*
						 * add a targeted version of our bid as well
						 */
						if(clickPr != 0) {
							numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
							if(convProb != 0) {
								convProb *= _targModel.getConvPrPredictionMultiplier(q, clickPr, convProb, false);
							}
						}
					}
					queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
				}
				allPredictionsMap.put(q, queryPredictions);
			}


			HashMap<Query,Item> bestSolution = fillKnapsack(getIncItemsForOverCapLevel(budget,0,allPredictionsMap), budget);
			double bestSolVal = solutionValue(bestSolution,budget,allPredictionsMap);
			int bestIdx = -1;
			//			System.out.println("Init val: " + bestSolVal);
			for(int i = 0; i < capList.size(); i++) {
				HashMap<Query,Item> solution = fillKnapsack(getIncItemsForOverCapLevel(budget,capList.get(i),allPredictionsMap), budget+capList.get(i));
				double solVal = solutionValue(solution,budget,allPredictionsMap);
				if(solVal > bestSolVal) {
					bestSolVal = solVal;
					bestSolution = solution;
					bestIdx = i;
				}
				//				System.out.println("OverCap By: " + capList.get(i) + ", val: " + solVal);
			}
			//			System.out.println("Best Index: " + bestIdx + ", Best val: " + bestSolVal);

			if(bestSolVal < 0) {
				bestSolution = new HashMap<Query,Item>();
			}

			//set bids
			for(Query q : _querySpace) {

				double bid;

				if(bestSolution.containsKey(q)) {
					bid = bestSolution.get(q).b();
					//					bid *= randDouble(.97,1.03);  //Mult by rand to avoid users learning patterns.
					//					System.out.println("Bidding " + bid + "   for query: " + q);
					double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
					double numImps = _queryToNumImpModel.getPrediction(q,(int) (_day+1));
					int numClicks = (int) (clickPr * numImps);
					double CPC = _bidToCPC.getPrediction(q, bid);

					if(bestSolution.get(q).targ()) {

						if(clickPr != 0) {
							numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
						}

						bidBundle.setBid(q, bid);

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
						bidBundle.addQuery(q, bid, new Ad());
					}

					if(BUDGET) {
						bidBundle.setDailyLimit(q, numClicks*CPC*1.3);
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
			double solutionWeight = solutionWeight(budget,bestSolution,allPredictionsMap);
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
		}
		else {
			for(Query q : _querySpace){
				double bid = 0.0;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				else
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
			}
		}
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
		return bidBundle;
	}


	private double solutionValue(HashMap<Query, Item> solution, double budget, HashMap<Query,ArrayList<Predictions>> allPredictionsMap) {
		double totalWeight = solutionWeight(budget, solution, allPredictionsMap);
		double overCap = totalWeight - budget;
		overCap = Math.max(overCap, 0);

		double penalty;
		if(budget < 0) {
			penalty = 0.0;
			int num = 0;
			for(double j = Math.abs(budget)+1; j <= overCap; j++) {
				penalty += Math.pow(LAMBDA, j);
				num++;
			}
			penalty /= (num);
		}
		else {
			if(overCap <= 0) {
				penalty = 1.0;
			}
			else {
				penalty = budget;
				for(int j = 1; j <= overCap; j++) {
					penalty += Math.pow(LAMBDA, j);
				}
				penalty /= (budget + overCap);
			}
		}
		if(Double.isNaN(penalty)) {
			penalty = 1.0;
		}
		double totalValue = 0;
		for(Query q : _querySpace) {
			if(solution.containsKey(q)) {
				Item item = solution.get(q);
				Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
				totalValue += prediction.getClickPr()*prediction.getNumImp()*(prediction.getConvPr()*penalty*_salesPrices.get(item.q()) - prediction.getCPC());
			}
		}
		
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

		double valueLostWindow = Math.max(1, Math.min(_capWindow, 59 - _day));
		double valueLost = 0;
		if(budget < 0) {
			for (double i = Math.abs(budget)+1; i <= overCap; i++){
				double iD = Math.pow(LAMBDA, i);
				double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
				valueLost += (avgConvProb - worseConvProb)*avgUSP*valueLostWindow; //You also lose conversions in the future (for 5 days)
			}
		}
		else {
			for (double i = 1; i <= overCap; i++){
				double iD = Math.pow(LAMBDA, i);
				double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
				valueLost += (avgConvProb - worseConvProb)*avgUSP*valueLostWindow; //You also lose conversions in the future (for 5 days)
			}
		}

		return totalValue-valueLost;
	}

	private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap, BidBundle bidBundle) {
		double threshold = 2;
		int maxIters = 10;
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
			double convProb = predictions.getConvPr();

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
				threshold += 1; //increase the threshold
			}
			lastSolWeight = solutionWeight;
			solutionWeight = 0;
			double penalty;
			double numOverCap = lastSolWeight - budget;
			if(budget < 0) {
				penalty = 0.0;
				int num = 0;
				for(double j = Math.abs(budget)+1; j <= numOverCap; j++) {
					penalty += Math.pow(LAMBDA, j);
					num++;
				}
				penalty /= (num);
			}
			else {
				if(numOverCap <= 0) {
					penalty = 1.0;
				}
				else {
					penalty = budget;
					for(int j = 1; j <= numOverCap; j++) {
						penalty += Math.pow(LAMBDA, j);
					}
					penalty /= (budget + numOverCap);
				}
			}
			if(Double.isNaN(penalty)) {
				penalty = 1.0;
			}
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
				double convProb = predictions.getConvPr() * penalty;

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


	private ArrayList<IncItem> getIncItemsForOverCapLevel(double initBudget, double overCap, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) {
		ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();
		double penalty;
		if(initBudget < 0) {
			penalty = 0.0;
			int num = 0;
			for(double j = Math.abs(initBudget)+1; j <= overCap; j++) {
				penalty += Math.pow(LAMBDA, j);
				num++;
			}
			penalty /= (num);
		}
		else {
			if(overCap <= 0) {
				penalty = 1.0;
			}
			else {
				penalty = initBudget;
				for(int j = 1; j <= overCap; j++) {
					penalty += Math.pow(LAMBDA, j);
				}
				penalty /= (initBudget + overCap);
			}
		}
		if(Double.isNaN(penalty)) {
			penalty = 1.0;
		}
		//		System.out.println("Creating KnapSack with " + overCap + " units over, penalty = " + penalty);
		for(Query q : _querySpace) {
			ArrayList<Item> itemList = new ArrayList<Item>();
			debug("Query: " + q);
			ArrayList<Predictions> queryPredictions = allPredictionsMap.get(q);
			for(int i = 0; i < bidList.size(); i++) {
				Predictions predictions = queryPredictions.get(i);
				double salesPrice = _salesPrices.get(q);
				double clickPr = predictions.getClickPr();
				double numImps = predictions.getNumImp();
				int numClicks = (int) (clickPr * numImps);
				double CPC = predictions.getCPC();
				double convProb = predictions.getConvPr()*penalty;

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
				itemList.add(new Item(q,w,v,bidList.get(i),false,0,i));

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

					itemList.add(new Item(q,w,v,bidList.get(i),true,0,i));
				}
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
			if(budget >= ii.w()) {
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
		return "MCKPBidSearch(capIncrement=" + _capIncrement + ")";
	}

	@Override
	public AbstractAgent getCopy() {
		return new MCKPBidSearch(_capIncrement);
	}

}
