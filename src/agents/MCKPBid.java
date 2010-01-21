package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.WEKAEnsembleBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.WEKAEnsembleBidToPrClick;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.BasicConvPrModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.sales.SalesDistributionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.BasicUnitsSoldModel;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
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
public class MCKPBid extends AbstractAgent {

	private int MAX_TIME_HORIZON = 5;
	private boolean SAFETYBUDGET = true;
	private boolean TARGET = false;
	private boolean BUDGET = false;
	private boolean BACKWARDUPDATING = true;
	private boolean FORWARDUPDATING = false;
	private boolean PRICELINES = false;

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
	private ArrayList<Double> bidList;
	private int lagDays = 5;
	private boolean salesDistFlag;

	public MCKPBid() {
		this(false,false,false,false);
	}
	
	public MCKPBid(boolean backward, boolean forward, boolean pricelines, boolean budget) {
		BUDGET = budget;
		BACKWARDUPDATING = backward;
		FORWARDUPDATING = forward;
		PRICELINES = pricelines;
//		_R.setSeed(124962748);
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
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		initBidder();
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
			i++;
			_queryId.put(q, i);
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
			double budget = _capacity/_capWindow;
			if(_day < 4) {
				//do nothing
			}
			else {
				//				budget = Math.max(20,_capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4);
				budget = _capacity - _unitsSold.getWindowSold();
				debug("Unit Sold Model Budget "  +budget);
			}

			debug("Budget: "+ budget);
			//NEED TO USE THE MODELS WE ARE PASSED!!!

			ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();

			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			double penalty = 1.0;
			if(budget < 0) {
				penalty = Math.pow(LAMBDA, Math.abs(budget));
			}
			HashMap<Query,ArrayList<Predictions>> allPredictionsMap = new HashMap<Query, ArrayList<Predictions>>();
			for(Query q : _querySpace) {
				ArrayList<Item> itemList = new ArrayList<Item>();
				ArrayList<Predictions> queryPredictions = new ArrayList<Predictions>();
				debug("Query: " + q);
				for(int i = 0; i < bidList.size(); i++) {
					double salesPrice = _salesPrices.get(q);
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

					debug("\tBid: " + bid);
					debug("\tCPC: " + CPC);
					debug("\tNumImps: " + numImps);
					debug("\tNumClicks: " + numClicks);
					debug("\tClickPr: " + clickPr);
					debug("\tConv Prob: " + convProb + "\n\n");

					int isID = _queryId.get(q);
					double w = numClicks*convProb*penalty;				//weight = numClciks * convProv
					double v = numClicks*convProb*penalty*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
					itemList.add(new Item(q,w,v,bid,false,isID,i));

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

						w = numClicks*convProb*penalty;				//weight = numClciks * convProv
						v = numClicks*convProb*penalty*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]

						itemList.add(new Item(q,w,v,bid,true,isID,i));
					}
					queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
				}
				debug("Items for " + q);
				Item[] items = itemList.toArray(new Item[0]);
				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
				allPredictionsMap.put(q, queryPredictions);
			}

			Collections.sort(allIncItems);
			HashMap<Query,Item> solution = fillKnapsackWithCapExt(allIncItems, budget, allPredictionsMap);

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

						bidBundle.setBid(q, bidList.get(bidIdx));

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
						bidBundle.addQuery(q, bidList.get(bidIdx), new Ad());
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
			double solutionWeight = solutionWeight(budget,solution,allPredictionsMap);
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
//		System.out.println("This took " + (elapsed / 1000) + " seconds");
		return bidBundle;
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



	private HashMap<Query,Item> fillKnapsackWithCapExt(ArrayList<IncItem> incItems, double budget, HashMap<Query,ArrayList<Predictions>> allPredictionsMap){
		HashMap<Query,Item> solution = new HashMap<Query, Item>();

		int expectedConvs = 0;
		double numOverCap = 0;

		for(int i = 0; i < incItems.size(); i++) {
			IncItem ii = incItems.get(i);
			double itemWeight = ii.w();
			double itemValue = ii.v();
			if(numOverCap == 0 && budget >= expectedConvs + itemWeight) {
				solution.put(ii.item().q(), ii.item());
				expectedConvs += itemWeight;
			}
			else {
				/*
				 * If backwards updating we update our solution weight now
				 */
				if(BACKWARDUPDATING) {
					expectedConvs = (int) solutionWeight(budget, solution, allPredictionsMap);
				}
				numOverCap = expectedConvs - budget;

				/*
				 * Discount the item based on the current penalty level
				 */
				double penalty;
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
				if(FORWARDUPDATING && !PRICELINES) {
					if(ii.itemLow() != null) {
						Predictions prediction1 = allPredictionsMap.get(ii.item().q()).get(ii.itemLow().idx());
						Predictions prediction2 = allPredictionsMap.get(ii.item().q()).get(ii.itemHigh().idx());
						itemValue = prediction2.getClickPr()*prediction2.getNumImp()*(prediction2.getConvPr()*penalty*_salesPrices.get(ii.item().q()) - prediction2.getCPC()) - 
						(prediction1.getClickPr()*prediction1.getNumImp()*(prediction1.getConvPr()*penalty*_salesPrices.get(ii.item().q()) - prediction1.getCPC())) ;
						itemWeight = prediction2.getClickPr()*prediction2.getNumImp()*prediction2.getConvPr()*penalty - 
						(prediction1.getClickPr()*prediction1.getNumImp()*prediction1.getConvPr()*penalty);
					}
					else {
						Predictions prediction = allPredictionsMap.get(ii.item().q()).get(ii.itemHigh().idx());
						itemValue = prediction.getClickPr()*prediction.getNumImp()*(prediction.getConvPr()*penalty*_salesPrices.get(ii.item().q()) - prediction.getCPC());
						itemWeight = prediction.getClickPr()*prediction.getNumImp()*prediction.getConvPr()*penalty;
					}
				}
				else if(PRICELINES) {
					ArrayList<IncItem> updatedItems = new ArrayList<IncItem>();
					for(int j = i+1; j < incItems.size(); j++) {
						IncItem incItem = incItems.get(j);
						Item itemLow = incItem.itemLow();
						Item itemHigh = incItem.itemHigh();

						double newWeight,newValue;

						if(itemLow != null) {
							Predictions prediction1 = allPredictionsMap.get(itemHigh.q()).get(itemLow.idx());
							Predictions prediction2 = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
							newValue = prediction2.getClickPr()*prediction2.getNumImp()*(prediction2.getConvPr()*penalty*_salesPrices.get(itemHigh.q()) - prediction2.getCPC()) - 
							(prediction1.getClickPr()*prediction1.getNumImp()*(prediction1.getConvPr()*penalty*_salesPrices.get(itemHigh.q()) - prediction1.getCPC())) ;
							newWeight = prediction2.getClickPr()*prediction2.getNumImp()*prediction2.getConvPr()*penalty - 
							(prediction1.getClickPr()*prediction1.getNumImp()*prediction1.getConvPr()*penalty);
						}
						else {
							Predictions prediction = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
							newValue = prediction.getClickPr()*prediction.getNumImp()*(prediction.getConvPr()*penalty*_salesPrices.get(itemHigh.q()) - prediction.getCPC());
							newWeight = prediction.getClickPr()*prediction.getNumImp()*prediction.getConvPr()*penalty;
						}
						IncItem newItem = new IncItem(newWeight,newValue,itemHigh, itemLow);
						updatedItems.add(newItem);
					}

					Collections.sort(updatedItems);

					while(incItems.size() > i+1) {
						incItems.remove(incItems.size()-1);
					}
					for(IncItem priceLineItem : updatedItems) {
						incItems.add(incItems.size(),priceLineItem);
					}
				}

				double min = numOverCap;
				double max = numOverCap + itemWeight;

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
				for (double j = min+1; j <= max; j++){
					double iD = Math.pow(LAMBDA, j);
					double worseConvProb = avgConvProb*iD;
					valueLost += (avgConvProb - worseConvProb)*avgUSP*valueLostWindow;
				}

				if(itemValue > valueLost) {
					solution.put(ii.item().q(), ii.item());
					expectedConvs += itemWeight;
				}
				else {
					break;
				}
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
		return "MCKPBid(Budget: " + BUDGET + ", Backward Update: " + BACKWARDUPDATING + ", Forward Update: " + FORWARDUPDATING + ", Pricelines: " + PRICELINES;
	}

	@Override
	public AbstractAgent getCopy() {
		return new MCKPBid(BACKWARDUPDATING,FORWARDUPDATING,PRICELINES,BUDGET);
	}
}
