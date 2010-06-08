package agents.modelbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import models.AbstractModel;
import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import models.budgetEstimator.AbstractBudgetEstimator;
import models.budgetEstimator.BudgetEstimator;
import models.paramest.AbstractParameterEstimation;
import models.paramest.BayesianParameterEstimation;
import models.prconv.NewBasicConvPrModel;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.NewBasicQueryToNumImp;
import models.sales.SalesDistributionModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import models.usermodel.jbergParticleFilter;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import agents.AbstractAgent;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;

public class AgentOrange extends AbstractAgent {

	/*
	 * TODO:
	 * 
	 * 1) Predict opponent MSB and CSB
	 * 2) Predict opponent ad type
	 * 3) Dynamic or at least different capacity numbers
	 */
	double[] _c;

	private boolean DEBUG = false;
	private Random _R;
	private boolean SAFETYBUDGET = true;
	private boolean BUDGET = false;
	private boolean FORWARDUPDATING = false;
	private boolean PRICELINES = false;

	private double _safetyBudget = 950;
	private int lagDays = 4;

	private double[] _regReserveLow = {.08, .29, .46};
	private double[] _regReserveHigh = {.29, .46, .6};
	/*
	 * FIXME:
	 * 
	 * We are just assuming that the reserve is half way between max and min
	 */
	private double[] _regReserve = {(_regReserveLow[0] + _regReserveHigh[0]) / 2.0,
			(_regReserveLow[1] + _regReserveHigh[1]) / 2.0,
			(_regReserveLow[2] + _regReserveHigh[2]) / 2.0};

	/*
	 * FIXME
	 * 
	 * We are just assuming that the pro reserve is 1/2 of the promoted boost
	 * more than the regular reserve
	 */
	private double _proReserveBoost = .5;
	private double[] _proReserve = {_regReserve[0] + _proReserveBoost * (1.0/2.0), 
			_regReserve[1] + _proReserveBoost * (1.0/2.0),
			_regReserve[2] + _proReserveBoost * (1.0/2.0)};

	private HashMap<Query, Double> _baseConvProbs;
	private HashMap<Query, Double> _baseClickProbs;
	private HashMap<Query, Double> _salesPrices;

	private AbstractQueryAnalyzer _queryAnalyzer;
	private ParticleFilterAbstractUserModel _userModel;
	private AbstractQueryToNumImp _queryToNumImp;
	private AbstractUnitsSoldModel _unitsSold;
	private NewBasicConvPrModel _convPrModel;
	private AbstractBidModel _bidModel;
	private AbstractParameterEstimation _paramEstimation;
	private AbstractBudgetEstimator _budgetEstimator;
	private SalesDistributionModel _salesDist;

	double[][] _advertiserEffectBounds;

	// Average advertiser effect
	double[] _advertiserEffectBoundsAvg;

	// Continuation Probability lower bound <> upper bound
	double[][] _continuationProbBounds;

	// Average continuation probability
	double[] _continuationProbBoundsAvg;

	// first index:
	// 0 - untargeted
	// 1 - targeted correctly
	// 2 - targeted incorrectly
	// second index:
	// 0 - not promoted
	// 1 - promoted
	double[][] fTargetfPro;

	// Turns a boolean into binary
	int bool2int(boolean bool) {
		if (bool) {
			return 1;
		}
		return 0;
	}

	// returns the corresponding index for the targeting part of fTargetfPro
	int getFTargetIndex(boolean targeted, Product p, Product target) {
		if (!targeted || p == null || target == null) {
			return 0; //untargeted
		}
		else if(p.equals(target)) {
			return 1; //targeted correctly
		}
		else {
			return 2; //targeted incorrectly
		}
	}

	public AgentOrange() {
		this(0.10753988514063796,0.187966273,0.339007416);
	}

	public AgentOrange(double c1, double c2, double c3) {
		_R = new Random();
//		_R.setSeed(616866);
		_c = new double[3];
		_c[0] = c1;
		_c[1] = c2;
		_c[2] = c3;
	}

	@Override
	public void initBidder() {

		_advertiserEffectBounds = new double[][]{ { 0.2, 0.3 },
				{ 0.3, 0.4 }, 
				{ 0.4, 0.5 } };

		// Average advertiser effect
		_advertiserEffectBoundsAvg = new double[]{
				(_advertiserEffectBounds[0][0] + _advertiserEffectBounds[0][1]) / 2,
				(_advertiserEffectBounds[1][0] + _advertiserEffectBounds[1][1]) / 2,
				(_advertiserEffectBounds[2][0] + _advertiserEffectBounds[2][1]) / 2 };

		// Continuation Probability lower bound <> upper bound
		_continuationProbBounds = new double[][]{ { 0.2, 0.5 }, 
				{ 0.3, 0.6 },
				{ 0.4, 0.7 } };

		// Average continuation probability
		_continuationProbBoundsAvg = new double[]{
				(_continuationProbBounds[0][0] + _continuationProbBounds[0][1]) / 2,
				(_continuationProbBounds[1][0] + _continuationProbBounds[1][1]) / 2,
				(_continuationProbBounds[2][0] + _continuationProbBounds[2][1]) / 2 };

		// first index:
		// 0 - untargeted
		// 1 - targeted correctly
		// 2 - targeted incorrectly
		// second index:
		// 0 - not promoted
		// 1 - promoted
		fTargetfPro = new double[][]{ { (1.0), (1.0) * (1.0 + _PSB) },
				{ (1.0 + _targEffect), (1.0 + _targEffect) * (1.0 + _PSB) },
				{ (1.0) / (1.0 + _targEffect), ((1.0) / (1.0 + _targEffect)) * (1.0 + _PSB) } };

		_baseConvProbs = new HashMap<Query, Double>();
		_baseClickProbs = new HashMap<Query, Double>();
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

			/*
			 * TODO
			 * 
			 * we can consider replacing these with our predicted clickPrs
			 * 
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
	}

	@Override
	public Set<AbstractModel> initModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		/*
		 * TODO
		 * 
		 * re-tune all parameters on new data sets
		 */
		_queryAnalyzer = new CarletonQueryAnalyzer(_querySpace,_advertisers,_advId,10,10,true);
		_userModel = new jbergParticleFilter(0.004932699,0.263532334,0.045700011,0.174371757,0.188113883,0.220140091);
		_queryToNumImp = new NewBasicQueryToNumImp(_userModel);
		_unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
		_convPrModel = new NewBasicConvPrModel(_userModel, _querySpace, _baseConvProbs);
		_bidModel = new IndependentBidModel(_advertisersSet, _advId,1,0,.8,.2,2.0);
		_paramEstimation = new BayesianParameterEstimation(_c,_advIdx);
		_budgetEstimator = new BudgetEstimator(_querySpace,_advIdx);
		_salesDist = new SalesDistributionModel(_querySpace);

		models.add(_queryAnalyzer);
		models.add(_userModel);
		models.add(_queryToNumImp);
		models.add(_unitsSold);
		models.add(_convPrModel);
		models.add(_bidModel);
		models.add(_paramEstimation);
		models.add(_budgetEstimator);
		models.add(_salesDist);
		return models;
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

		if(_day > lagDays){
			double remainingCap;
			if(_day < 4) {
				remainingCap = _capacity/_capWindow;
			}
			else {
				remainingCap = _capacity - _unitsSold.getWindowSold();
				debug("Unit Sold Model Budget "  +remainingCap);
			}

			debug("Budget: "+ remainingCap);

			HashMap<Query,ArrayList<Double>> bidLists = new HashMap<Query,ArrayList<Double>>();
			HashMap<Query,ArrayList<Double>> budgetLists = new HashMap<Query,ArrayList<Double>>();
			for(Query q : _querySpace) {
				ArrayList<Double> bids = new ArrayList<Double>();
				double unSquash = 1.0 / Math.pow(_paramEstimation.getPrediction(q)[0],_squashing);

				for(int i = 0; i < _advertisers.size(); i++) {
					/*
					 * We need to unsquash opponent bids
					 */
					if(i != _advIdx) { //only care about opponent bids
						bids.add(_bidModel.getPrediction("adv" + (i+1), q) * unSquash);
					}
				}

				/*
				 * This sorts low to high
				 */
				Collections.sort(bids);

				ArrayList<Double> noDupeBids = removeDupes(bids);

				ArrayList<Double> newBids = new ArrayList<Double>();
				int NUM_SAMPLES = 2;
				for(int i = 0; i < noDupeBids.size(); i++) {
					newBids.add(noDupeBids.get(i) - .01);
					//					newBids.add(noDupeBids.get(i)); //TODO may want to include this since we requash
					newBids.add(noDupeBids.get(i) + .01);

					if((i == 0 && noDupeBids.size() > 1) || (i > 0 && i != noDupeBids.size()-1)) {
						for(int j = 1; j < NUM_SAMPLES+1; j++) {
							newBids.add(noDupeBids.get(i) + (noDupeBids.get(i+1) - noDupeBids.get(i)) * (j / ((double)(NUM_SAMPLES+1))));
						}
					}
				}

				if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					double increment  = .2;
					double min = .08;
					double max = 1.0;
					int tot = (int) Math.ceil((max-min) / increment);
					for(int i = 0; i < tot; i++) {
						newBids.add(min+(i*increment));
					}
				}
				else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
					double increment  = .2;
					double min = .29;
					double max = 1.95;
					int tot = (int) Math.ceil((max-min) / increment);
					for(int i = 0; i < tot; i++) {
						newBids.add(min+(i*increment));
					}

				}
				else {
					double increment  = .2;
					double min = .46;
					double max = 2.35;
					int tot = (int) Math.ceil((max-min) / increment);
					for(int i = 0; i < tot; i++) {
						newBids.add(min+(i*increment));
					}
				}

				Collections.sort(newBids);
				bidLists.put(q, newBids);


				ArrayList<Double> budgetList = new ArrayList<Double>();
				budgetList.add(35.0);
				budgetList.add(50.0);
				budgetList.add(75.0);
				budgetList.add(100.0);
				budgetList.add(200.0);
				budgetList.add(300.0);

				budgetLists.put(q,budgetList);
			}

			HashMap<Product,HashMap<UserState,Integer>> userStates = new HashMap<Product,HashMap<UserState,Integer>>();
			for(Product p : _products) {
				HashMap<UserState,Integer> userState = new HashMap<UserState,Integer>();
				for(UserState s : UserState.values()) {
					userState.put(s, _userModel.getPrediction(p, s));
				}
				userStates.put(p, userState);
			}

			ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();

			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			double penalty = getPenalty(remainingCap, 0);
			HashMap<Query,ArrayList<Predictions>> allPredictionsMap = new HashMap<Query, ArrayList<Predictions>>();
			for(Query q : _querySpace) {
				ArrayList<Item> itemList = new ArrayList<Item>();
				ArrayList<Predictions> queryPredictions = new ArrayList<Predictions>();
				debug("Query: " + q);
				double convProbWithPen = getConversionPrWithPenalty(q, penalty);
				double convProb = _convPrModel.getPrediction(q);
				double salesPrice = _salesPrices.get(q);
				int itemCount = 0;
				for(int i = 0; i < bidLists.get(q).size(); i++) {
					for(int j = 0; j < budgetLists.get(q).size(); j++) {
						for(int k = 0; k < 2; k++) {
							boolean targeting = (k == 0) ? false : true;
							double bid = bidLists.get(q).get(i);
							double budget = budgetLists.get(q).get(j);

							double[] impsClicksAndCost = getImpsClicksAndCost(q,bid,budget,targeting,userStates);

							double numImps = impsClicksAndCost[0];
							double numClicks = impsClicksAndCost[1];
							double CPC = impsClicksAndCost[2] / impsClicksAndCost[1];

							double clickPr = numClicks / numImps;

							//							System.out.println("Bid: " + bid);
							//							System.out.println("Budget: " + budget);
							//							System.out.println("Targetting: " + targeting);
							//							System.out.println("numImps: " + numImps);
							//							System.out.println("numClicks: " + numClicks);
							//							System.out.println("CPC: " + CPC);
							//							System.out.println("clickPr: " + clickPr);
							//							System.out.println();

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

							double w = numClicks*convProbWithPen;				//weight = numClciks * convProv
							double v = numClicks*convProbWithPen*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
							itemList.add(new Item(q,w,v,bid,budget,targeting,0,itemCount));
							queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
							itemCount++;
						}
					}
				}
				debug("Items for " + q);
				Item[] items = itemList.toArray(new Item[0]);
				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
				allPredictionsMap.put(q, queryPredictions);
			}

			Collections.sort(allIncItems);
			HashMap<Query,Item> solution = fillKnapsackWithCapExt(allIncItems, remainingCap, allPredictionsMap);

			//set bids
			for(Query q : _querySpace) {
				ArrayList<Predictions> queryPrediction = allPredictionsMap.get(q);

				if(solution.containsKey(q)) {
					Item item = solution.get(q);
					double bid = item.b();
					double budget = item.budget();
					int idx = solution.get(q).idx();
					Predictions predictions = queryPrediction.get(idx);
					double clickPr = predictions.getClickPr();
					double numImps = predictions.getNumImp();
					int numClicks = (int) (clickPr * numImps);
					double CPC = predictions.getCPC();

					if(solution.get(q).targ()) {
						bidBundle.setBid(q, bid);
						bidBundle.setAd(q, getTargetedAd(q));
					}
					else {
						bidBundle.addQuery(q, bid, new Ad());
					}

					if(BUDGET && budget == Double.MAX_VALUE) {
						/*
						 * Only override the budget if the flag is set
						 * and we didn't choose to set a budget
						 */
						bidBundle.setDailyLimit(q, numClicks*CPC);
					}
					else {
						bidBundle.setDailyLimit(q, budget);
					}
				}
				else {
					/*
					 * We decided that we did not want to be in this query, so we will use it to explore the space
					 */
					//					bid = 0.0;
					//					bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
					//					System.out.println("Bidding " + bid + "   for query: " + q);

					double bid = randDouble(_regReserveLow[queryTypeToInt(q.getType())],_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .75);

					//					System.out.println("Exploring " + q + "   bid: " + bid);
					if(q.getType() != QueryType.FOCUS_LEVEL_ZERO) {
						if(q.getType() != QueryType.FOCUS_LEVEL_ONE) {
							bidBundle.addQuery(q, bid, new Ad(), bid*10);
						}
						else {
							bidBundle.addQuery(q, bid, new Ad(), bid*20);
						}
					}
				}
			}

			/*
			 * Pass expected conversions to unit sales model
			 */

			double solutionWeight = solutionWeight(remainingCap,solution,allPredictionsMap);
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
		}
		else {
			/*
			 * Bound these with the reseve scores
			 */
			for(Query q : _querySpace){
				if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
					double bid = randDouble(_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .35, _salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .85);
					bidBundle.addQuery(q, bid, new Ad(), Double.MAX_VALUE);
				}
				else {
					double bid = randDouble(_regReserveLow[queryTypeToInt(q.getType())],_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .85);
					bidBundle.addQuery(q, bid, new Ad(), bid*20);
				}
			}
			bidBundle.setCampaignDailySpendLimit(925);
		}
		/*
		 * Just in case...
		 */
		for(Query q : _querySpace) {
			if(Double.isNaN(bidBundle.getBid(q)) || bidBundle.getBid(q) < 0) {
				bidBundle.setBid(q, 0.0);
			}
		}

		System.out.println(bidBundle);
		return bidBundle;
	}

	// Turns a query type into 0/1/2
	int queryTypeToInt(QueryType qt) {
		if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
			return 0;
		}
		if (qt.equals(QueryType.FOCUS_LEVEL_ONE)) {
			return 1;
		}
		if (qt.equals(QueryType.FOCUS_LEVEL_TWO)) {
			return 2;
		}
		System.out.println("Error in queryTypeToInt");
		return 2;
	}

	private ArrayList<Double> removeDupes(ArrayList<Double> bids) {
		ArrayList<Double> noDupeList = new ArrayList<Double>();
		for(int i = 0; i < bids.size()-1; i++) {
			noDupeList.add(bids.get(i));
			while((i+1 < bids.size()-1) && (bids.get(i) == bids.get(i+1))) {
				i++;
			}
		}
		return noDupeList;
	}

	private double[] getImpsClicksAndCost(Query q, double bid, double budget, boolean targeting, HashMap<Product, HashMap<UserState, Integer>> userStates) {
		int queryTypeIdx;
		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			queryTypeIdx = 0;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			queryTypeIdx = 1;
		}
		else {
			queryTypeIdx = 2;
		}
		/*
		 * Setup problem
		 */
		Ad ad;
		if(targeting) {
			ad = getTargetedAd(q);
		}
		else {
			ad = new Ad();
		}

		ArrayList<BidBudgetAdPair> bidBudgetAdPairs = new ArrayList<BidBudgetAdPair>();
		//need to squash our own bid
		BidBudgetAdPair ourPair = new BidBudgetAdPair(_advIdx,bid * Math.pow(_paramEstimation.getPrediction(q)[0],_squashing),budget,ad);
		bidBudgetAdPairs.add(ourPair);
		for(int i = 0; i < _advertisers.size(); i++) {
			if(i != _advIdx) {
				BidBudgetAdPair pair = new BidBudgetAdPair(i,_bidModel.getPrediction("adv" + (i+1), q),
						_budgetEstimator.getBudgetEstimate(q,"adv" + (i+1)),
						new Ad()); //TODO predict ads
				bidBudgetAdPairs.add(pair);
			}
		}

		Collections.sort(bidBudgetAdPairs);

		//remove bids under reserve
		int size = bidBudgetAdPairs.size();
		for(int i = 0; i < size; i++) {
			if(bidBudgetAdPairs.get(i).getBid() < _regReserve[queryTypeIdx]) {
				for(int j = i; j < size; j++) {
					bidBudgetAdPairs.remove(i);
				}
				break;
			}
		}

		boolean inAuction = false;
		for(int i = 0; i < bidBudgetAdPairs.size(); i++) {
			if(bidBudgetAdPairs.get(i).getID() == _advIdx) {
				inAuction = true;
				break;
			}
		}

		if(inAuction) {
			return recursiveImpsClicksAndCost(q,0,bidBudgetAdPairs,userStates);
		}
		else {
			/*
			 * If we are not in the auction (i.e. under the reserve), then
			 * we know our imps, clicks and cpc are all zero
			 */
			double[] impsClicksAndCost = new double[3];
			return impsClicksAndCost;
		}
	}

	private double[] recursiveImpsClicksAndCost(Query q, double impressionsSeen, ArrayList<BidBudgetAdPair> bidBudgetAdPairs, HashMap<Product, HashMap<UserState, Integer>> userStates) {
		int queryTypeIdx;
		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			queryTypeIdx = 0;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			queryTypeIdx = 1;
		}
		else {
			queryTypeIdx = 2;
		}

		double[] CPCs = new double[_advertisers.size()]; //indexed by idx in pair
		double otherAdvertiserEffect;
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			otherAdvertiserEffect = _advertiserEffectBoundsAvg[0];
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			otherAdvertiserEffect = _advertiserEffectBoundsAvg[1];
		} else {
			otherAdvertiserEffect = _advertiserEffectBoundsAvg[2];
		}

		double squashedAdvEff = Math.pow(otherAdvertiserEffect,_squashing);

		for(int i = 0; i < bidBudgetAdPairs.size()-1; i++) {
			if(bidBudgetAdPairs.get(i).getID() == _advIdx) {
				CPCs[_advIdx] = (bidBudgetAdPairs.get(i+1).getBid() / Math.pow(_paramEstimation.getPrediction(q)[0],_squashing)) + .01;
			}
			else {
				CPCs[bidBudgetAdPairs.get(i).getID()] = (bidBudgetAdPairs.get(i+1).getBid() / squashedAdvEff) + .01;
			}
		}

		//the person in last is special because they pay the reserve
		/*
		 * Check that we aren't in a promoted slot....
		 */
		if(bidBudgetAdPairs.size()-1 < _numPS) {
			if(bidBudgetAdPairs.get(bidBudgetAdPairs.size()-1).getID() == _advIdx) {
				CPCs[_advIdx] = _proReserve[queryTypeIdx] / Math.pow(_paramEstimation.getPrediction(q)[0],_squashing) + .01;
			}
			else {
				CPCs[bidBudgetAdPairs.get(bidBudgetAdPairs.size()-1).getID()] = _proReserve[queryTypeIdx] / squashedAdvEff + .01;
			}
		}
		else {
			if(bidBudgetAdPairs.get(bidBudgetAdPairs.size()-1).getID() == _advIdx) {
				CPCs[_advIdx] = _regReserve[queryTypeIdx] / Math.pow(_paramEstimation.getPrediction(q)[0],_squashing) + .01;
			}
			else {
				CPCs[bidBudgetAdPairs.get(bidBudgetAdPairs.size()-1).getID()] = _regReserve[queryTypeIdx] / squashedAdvEff + .01;
			}
		}

		double[] impToViewRatios = new double[_advertisers.size()];
		double[] clickPrs = new double[_advertisers.size()];
		HashMap<Product, double[]> userStatesOfSearchingUsers = getStatesOfSearchingUsers(q,userStates);
		double totalImps = _queryToNumImp.getPrediction(q, (int) (_day+1));
		double remainingImps = totalImps - impressionsSeen;
		for (Product p : userStates.keySet()) {
			double IS = userStatesOfSearchingUsers.get(p)[0];
			double nonIS = userStatesOfSearchingUsers.get(p)[1];
			double sum = IS + nonIS;
			for (int slot = 0; slot < _numSlots && slot < bidBudgetAdPairs.size(); slot++) {
				BidBudgetAdPair pair = bidBudgetAdPairs.get(slot);
				Ad ad = pair.getAd();
				boolean targeted;
				Product target;
				if(ad == null || ad.isGeneric()) {
					targeted = false;
					target = null;
				}
				else {
					targeted= true;
					target = ad.getProduct();
				}
				int ft = getFTargetIndex(targeted, p, target);

				double ftfp = fTargetfPro[ft][bool2int(_numPS >= slot + 1)];

				double advertiserEffect;
				if(pair.getID() == _advIdx) {
					advertiserEffect = _paramEstimation.getPrediction(q)[0];
				}
				else {
					if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
						advertiserEffect = _advertiserEffectBoundsAvg[0];
					} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
						advertiserEffect = _advertiserEffectBoundsAvg[1];
					} else {
						advertiserEffect = _advertiserEffectBoundsAvg[2];
					}
				}
				double theoreticalClickProb = etaClickPr(advertiserEffect, ftfp);

				impToViewRatios[pair.getID()] += (IS+nonIS) / totalImps;
				clickPrs[pair.getID()] += theoreticalClickProb * (sum / totalImps);

				/*
				 * Decrement the users based on clicking, converting,
				 * and continuing
				 * 
				 */
				double contProb = _paramEstimation.getPrediction(q)[1];
				nonIS *= (1.0 - otherAdvertiserConvProb(q) * theoreticalClickProb)*contProb;
				IS *= contProb;
			}
		}

		/*
		 * Find the minimum number of impressions each advertiser can see
		 * before hitting their budget
		 */
		ArrayList<ImprPair> maxImpPairs = new ArrayList<ImprPair>();
		double ourMaxImps = Double.MAX_VALUE;
		for(int i = 0; i < _numSlots && i < bidBudgetAdPairs.size(); i++) {
			BidBudgetAdPair pair = bidBudgetAdPairs.get(i);
			int pairID = pair.getID();
			double CPC = CPCs[pairID];
			double impToViewRatio = impToViewRatios[pairID];
			double clickPr = clickPrs[pairID];
			double budget = pair.getBudget();
			double costPerImp = impToViewRatio*clickPr*CPC;
			double maxImps = budget / costPerImp;
			ImprPair impPair = new ImprPair(pairID, maxImps);
			maxImpPairs.add(impPair);

			if(pairID == _advIdx) {
				ourMaxImps = maxImps;
			}
		}

		Collections.sort(maxImpPairs);

		ImprPair minImpPair = maxImpPairs.get(0);
		int minID = minImpPair.getID();
		double minImps = minImpPair.getImpr();

		boolean done = false;

		if(minImps >= ourMaxImps) {
			done = true;
			ourMaxImps = minImps;
			if(ourMaxImps > remainingImps) {
				ourMaxImps = remainingImps;
			}
		}

		if(minImps > remainingImps) {
			done = true;
			ourMaxImps = remainingImps;
		}

		if(done) {
			double CPC = CPCs[_advIdx];
			double impToViewRatio = impToViewRatios[_advIdx];
			double clickPr = clickPrs[_advIdx];
			double clicks = ourMaxImps*impToViewRatio*clickPr;
			double cost = clicks*CPC;

			double[] impsClicksAndCost = new double[3];
			impsClicksAndCost[0] = ourMaxImps;
			impsClicksAndCost[1] = clicks;
			impsClicksAndCost[2] = cost;

			return impsClicksAndCost;
		}
		else {
			double CPC = CPCs[_advIdx];
			double impToViewRatio = impToViewRatios[_advIdx];
			double clickPr = clickPrs[_advIdx];
			double clicks = minImps*impToViewRatio*clickPr;
			double cost = clicks*CPC;

			double[] impsClicksAndCost = new double[3];
			impsClicksAndCost[0] = minImps;
			impsClicksAndCost[1] = clicks;
			impsClicksAndCost[2] = cost;


			/*
			 * Remake the problem with fewer impressions, lower budgets, and without
			 * the person who hit their budget
			 */
			int idxToRemove = 0;
			for(int i = 0; i < _numSlots && i < bidBudgetAdPairs.size(); i++) {
				BidBudgetAdPair innerPair = bidBudgetAdPairs.get(i);
				int innerPairID = innerPair.getID();
				if(innerPairID == minID) {
					idxToRemove = i;
				}
				double innerCPC = CPCs[innerPairID];
				double innerImpToViewRatio = impToViewRatios[innerPairID];
				double innerClickPr = clickPrs[innerPairID];
				double innerBudget = innerPair.getBudget();
				double innerCostPerImp = innerImpToViewRatio*innerClickPr*innerCPC;
				double innerCost = innerCostPerImp * minImps;
				double newBudget = innerBudget-innerCost;

				innerPair.setBudget(newBudget);
			}

			bidBudgetAdPairs.remove(idxToRemove);

			double[] newImpsClicksAndCost = recursiveImpsClicksAndCost(q, impressionsSeen + minImps, bidBudgetAdPairs, userStates);

			newImpsClicksAndCost[0] += impsClicksAndCost[0];
			newImpsClicksAndCost[1] += impsClicksAndCost[1];
			newImpsClicksAndCost[2] += impsClicksAndCost[2];

			return newImpsClicksAndCost;
		}
	}

	private double otherAdvertiserConvProb(Query q) {
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			return _c[0];
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			return _c[1];
		} else {
			return _c[2];
		}
	}

	public HashMap<Product, double[]> getStatesOfSearchingUsers(Query q, HashMap<Product, HashMap<UserState, Integer>> userStates) {

		HashMap<Product, double[]> toreturn = new HashMap<Product, double[]>();
		// for each product
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// count up how many searching users there were
			double ISusers = 0.0;
			double nonISusers = 0.0;
			if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && 
					q.getComponent().equals(p.getComponent()) &&
					q.getManufacturer().equals(p.getManufacturer())) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = states.get(UserState.F2);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && 
					((q.getComponent() != null && q.getComponent().equals(p.getComponent())) || 
							(q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
				ISusers = 1.0 / 6.0 * states.get(UserState.IS);
				nonISusers = 0.5 * states.get(UserState.F1);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = states.get(UserState.F0);
			}
			double ISusersPerSlot = 0.0;
			double nonISusersPerSlot = 0.0;
			if (ISusers + nonISusers > 0.0) {
				ISusersPerSlot = ISusers;
				nonISusersPerSlot = nonISusers;
			}
			double[] statesOfSearchingUsersPerSlot = { ISusersPerSlot, nonISusersPerSlot };
			toreturn.put(p, statesOfSearchingUsersPerSlot);
		}
		return toreturn;
	}

	// Calculate the forward click probability as defined on page 14 of the
	// spec.
	public double etaClickPr(double advertiserEffect, double fTargetfPro) {
		return (advertiserEffect * fTargetfPro) / ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
	}

	private Ad getTargetedAd(Query q) {
		Ad ad;
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			/*
			 * F0 Query, target our specialty
			 */
			ad = new Ad(new Product(_manSpecialty, _compSpecialty));
		}
		else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			if(q.getComponent() == null) {
				/*
				 * F1 Query (comp = null), so target the subgroup that searches for this and our 
				 * component specialty
				 */
				ad = new Ad(new Product(q.getManufacturer(), _compSpecialty));
			}
			else {
				/*
				 * F1 Query (man = null), so target the subgroup that searches for this and our 
				 * manufacturer specialty
				 */
				ad = new Ad(new Product(_manSpecialty, q.getComponent()));
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

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		BidBundle bidBundle = _bidBundles.get(_bidBundles.size()-2);

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

		_queryAnalyzer.updateModel(queryReport, salesReport, bidBundle, _maxImps);

		HashMap<Query,Integer> totalImpressions = new HashMap<Query,Integer>();
		HashMap<Query, HashMap<String, Integer>> ranks = new HashMap<Query,HashMap<String,Integer>>();
		HashMap<Query,int[]> fullOrders = new HashMap<Query,int[]>();
		HashMap<Query,int[]> fullImpressions = new HashMap<Query,int[]>();
		for(Query q : _querySpace) {
			int[] impsPred = _queryAnalyzer.getImpressionsPrediction(q);
			int[] ranksPred = _queryAnalyzer.getOrderPrediction(q);

			int numNonNaN = 0;
			for(int i = 0; i < _advertisers.size(); i++) {
				double avgPos;
				if(i == _advIdx) {
					avgPos = queryReport.getPosition(q);
				}
				else {
					avgPos = queryReport.getPosition(q, "adv" + (i+1));
				}
				if(!Double.isNaN(avgPos)) {
					numNonNaN++;
				}
			}

			int numToRemove = 0;
			for(int i = 0; i < ranksPred.length; i++) {
				if(ranksPred[i] >= numNonNaN) {
					numToRemove++;
				}
			}

			int[] newImpPred = new int[impsPred.length - numToRemove];
			int[] newRanksPred = new int[impsPred.length - numToRemove];

			for(int i = 0; i < newImpPred.length; i++) {
				newImpPred[i] = impsPred[i];
			}

			int numSkipped = 0;
			for(int i = 0; i < ranksPred.length; i++) {
				if(ranksPred[i] < numNonNaN) {
					newRanksPred[i-numSkipped] = ranksPred[i];
				}
				else {
					numSkipped++;
				}
			}

			impsPred = newImpPred;
			ranksPred = newRanksPred;

			int totalImps = getMaxImps(5,impsPred.length,ranksPred,impsPred);

			totalImps *= .6;

			if(totalImps == 0) {
				//this means something bad happened
				totalImps = -1;
			}
			totalImpressions.put(q, totalImps);

			int[] fullOrder = new int[_advertisers.size()];
			int[] fullImpression = new int[_advertisers.size()];
			HashMap<String, Integer> perQRanks = new HashMap<String,Integer>();
			int agentOffset = 0;
			for(int i = 0; i < _advertisers.size(); i++) {
				double avgPos;
				if(i == _advIdx) {
					avgPos = queryReport.getPosition(q);
					//					avgPos = queryReport.getPosition(q, "adv" + (i+1));
					/*
					 * In the new game these return two different things.
					 * The first returns our position with full decimal place,
					 * the second returns the sampled version
					 */
				}
				else {
					avgPos = queryReport.getPosition(q, "adv" + (i+1));
				}

				if(!Double.isNaN(avgPos)) {
					/*
					 * If the agent had a non-NaN avgPosition then
					 * they were in the auction, and can use there
					 * rank
					 */
					for(int j = 0; j < ranksPred.length; j++) {
						if(ranksPred[j]+agentOffset == i) {
							perQRanks.put("adv" + (i+1), j);
							fullImpression[i] = impsPred[j];
						}
					}
				}
				else {
					/*
					 * If the agent has NaN for an average position
					 * then they weren't in the auction and we will
					 * order them by their agent ID
					 */
					perQRanks.put("adv" + (i+1), ranksPred.length+agentOffset);
					agentOffset++;
					fullImpression[i] = 0; //someone with NaN position saw no impressions
				}
				assert (perQRanks.get("adv" + (i+1)) < 8) : "poo";
				fullOrder[perQRanks.get("adv" + (i+1))] = i;
			}
			ranks.put(q, perQRanks);
			fullOrders.put(q, fullOrder);
			fullImpressions.put(q, fullImpression);
		}


		_userModel.updateModel(totalImpressions);

		HashMap<Product,HashMap<UserState,Integer>> userStates = new HashMap<Product,HashMap<UserState,Integer>>();
		for(Product p : _products) {
			HashMap<UserState,Integer> userState = new HashMap<UserState,Integer>();
			for(UserState s : UserState.values()) {
				userState.put(s, _userModel.getCurrentEstimate(p, s));
			}
			userStates.put(p, userState);
		}

		_queryToNumImp.updateModel(queryReport, salesReport);
		_unitsSold.update(salesReport);
		_convPrModel.updateModel(queryReport, salesReport,bidBundle);

		_paramEstimation.updateModel(queryReport, salesReport, bidBundle, _numPS, fullOrders, fullImpressions, userStates, _c);

		HashMap<Query, Double> cpc = new HashMap<Query,Double>();
		HashMap<Query, Double> ourBid = new HashMap<Query,Double>();
		for(Query q : _querySpace) {
			cpc.put(q, queryReport.getCPC(q)* Math.pow(_paramEstimation.getPrediction(q)[0], _squashing));
			ourBid.put(q, bidBundle.getBid(q) * Math.pow(_paramEstimation.getPrediction(q)[0], _squashing));
		}
		_bidModel.updateModel(cpc, ourBid, ranks);

		HashMap<Query,Double> contProbs = new HashMap<Query,Double>();
		HashMap<Query, double[]> allbids = new HashMap<Query,double[]>();
		for(Query q : _querySpace) {
			contProbs.put(q, _paramEstimation.getPrediction(q)[1]);
			double[] bids = new double[_advertisers.size()];
			for(int j = 0; j < bids.length; j++) {
				if(j == _advIdx) {
					bids[j] = bidBundle.getBid(q) * Math.pow(_paramEstimation.getPrediction(q)[0], _squashing);
				}
				else {
					bids[j] = _bidModel.getPrediction("adv" + (j+1), q);
				}
			}
			allbids.put(q, bids);
		}

		_budgetEstimator.updateModel(queryReport, salesReport, bidBundle, _numPS, _c, contProbs, fullOrders, fullImpressions, allbids, userStates);

		_salesDist.updateModel(salesReport);
	}

	public int getMaxImps(int slots, int agents, int[] order, int[] impressions) {
		int[][] impressionsBySlot = new int[agents][slots];

		int[] slotStart= new int[slots];
		int a;

		for(int i = 0; i < agents; ++i){
			a = order[i];
			//System.out.println(a);
			int remainingImp = impressions[a];
			//System.out.println("remaining impressions "+ impressions[a]);
			for(int s = Math.min(i+1, slots)-1; s>=0; --s){
				if(s == 0){
					impressionsBySlot[a][0] = remainingImp;
					slotStart[0] += remainingImp;
				}else{

					int r = slotStart[s-1] - slotStart[s];
					//System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
					assert(r >= 0);
					if(r < remainingImp){
						remainingImp -= r;
						impressionsBySlot[a][s] = r;
						slotStart[s] += r;
					} else {
						impressionsBySlot[a][s] = remainingImp;
						slotStart[s] += remainingImp;
						break;
					}
				}
			}
		}
		return slotStart[0];
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

	@SuppressWarnings("unchecked")
	private HashMap<Query,Item> fillKnapsackWithCapExt(ArrayList<IncItem> incItems, double budget, HashMap<Query,ArrayList<Predictions>> allPredictionsMap){
		HashMap<Query,Item> solution = new HashMap<Query, Item>();

		int expectedConvs = 0;

		for(int i = 0; i < incItems.size(); i++) {
			IncItem ii = incItems.get(i);
			double itemWeight = ii.w();
			//			double itemValue = ii.v();
			if(budget >= expectedConvs + itemWeight) {
				solution.put(ii.item().q(), ii.item());
				expectedConvs += itemWeight;
			}
			else {
				//				double[] currSolVal = solutionValueMultiDay(solution, budget, allPredictionsMap);
				double[] currSolVal = solutionValueMultiDay2(solution, budget, allPredictionsMap,10);

				HashMap<Query, Item> solutionCopy = (HashMap<Query, Item>)solution.clone();
				solutionCopy.put(ii.item().q(), ii.item());
				//				double[] newSolVal = solutionValueMultiDay(solutionCopy, budget, allPredictionsMap);
				double[] newSolVal = solutionValueMultiDay2(solutionCopy, budget, allPredictionsMap,10);

				//				System.out.println("[" + _day +"] CurrSolVal: " + currSolVal[0] + ", NewSolVal: " + newSolVal[0]);

				if(newSolVal[0] > currSolVal[0]) {
					solution.put(ii.item().q(), ii.item());
					expectedConvs = (int) newSolVal[1];

					if(i != incItems.size() - 1) {
						/*
						 * Discount the item based on the current penalty level
						 */
						double penalty = getPenalty(budget, newSolVal[1]);

						if(FORWARDUPDATING && !PRICELINES) {
							//Update next item
							IncItem nextItem  = incItems.get(i+1);
							double v,w;
							if(nextItem.itemLow() != null) {
								Predictions prediction1 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemLow().idx());
								Predictions prediction2 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
								v = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction2.getCPC()) - 
								(prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction1.getCPC())) ;
								w = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty) - 
								(prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty));
							}
							else {
								Predictions prediction = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
								v = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction.getCPC());
								w = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty);
							}
							IncItem newNextItem = new IncItem(w, v, nextItem.itemHigh(), nextItem.itemLow());
							incItems.remove(i+1);
							incItems.add(i+1, newNextItem);
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
									newValue = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), penalty)*_salesPrices.get(itemHigh.q()) - prediction2.getCPC()) - 
									(prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), penalty)*_salesPrices.get(itemHigh.q()) - prediction1.getCPC())) ;
									newWeight = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), penalty) - 
									(prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), penalty));
								}
								else {
									Predictions prediction = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
									newValue = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), penalty)*_salesPrices.get(itemHigh.q()) - prediction.getCPC());
									newWeight = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), penalty);
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
					}
				}
				else {
					solution.put(ii.item().q(), ii.item());
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

		Item[] uItems = q.toArray(new Item[0]);
		return uItems;
	}


	/**
	 * Get incremental items
	 * @param items
	 * @return
	 */
	public IncItem[] getIncremental(Item[] items) {
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

	public double getConversionPrWithPenalty(Query q, double penalty) {
		double convPr;
		String component = q.getComponent();
		double pred = _convPrModel.getPrediction(q);
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

	public void debug(Object str) {
		if(DEBUG) {
			System.out.println(str);
		}
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	@Override
	public String toString() {
		return "AgentOrange";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AgentOrange(_c[0],_c[1],_c[2]);
	}

	public static class BidBudgetAdPair implements Comparable<BidBudgetAdPair> {

		private int _advIdx;
		private double _bid;
		private double _budget;
		private Ad _ad;

		public BidBudgetAdPair(int advIdx, double bid, double budget, Ad ad) {
			_advIdx = advIdx;
			_bid = bid;
			_budget = budget;
			_ad = ad;
		}

		public int getID() {
			return _advIdx;
		}

		public void setID(int advIdx) {
			_advIdx = advIdx;
		}

		public double getBid() {
			return _bid;
		}

		public void setBid(double bid) {
			_bid = bid;
		}

		public double getBudget() {
			return _budget;
		}

		public void setBudget(double budget) {
			_budget = budget;
		}

		public Ad getAd() {
			return _ad;
		}

		public void setAd(Ad ad) {
			_ad = ad;
		}

		@Override
		public String toString() {
			return _bid + "";
		}

		public int compareTo(BidBudgetAdPair agentBidPair) {
			double ourBid = this._bid;
			double otherBid = agentBidPair.getBid();
			if(ourBid < otherBid) {
				return 1;
			}
			if(otherBid < ourBid) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}

	public static class ImprPair implements Comparable<ImprPair> {

		private int _advIdx;
		private double _impr;

		public ImprPair(int advIdx, double impr) {
			_advIdx = advIdx;
			_impr = impr;
		}

		public int getID() {
			return _advIdx;
		}

		public void setID(int advIdx) {
			_advIdx = advIdx;
		}

		public double getImpr() {
			return _impr;
		}

		public void setImpr(double impr) {
			_impr = impr;
		}

		@Override
		public String toString() {
			return _impr + "";
		}

		public int compareTo(ImprPair agentImprPair) {
			double ourBid = this._impr;
			double otherBid = agentImprPair.getImpr();
			if(ourBid > otherBid) {
				return 1;
			}
			if(otherBid > ourBid) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
}
