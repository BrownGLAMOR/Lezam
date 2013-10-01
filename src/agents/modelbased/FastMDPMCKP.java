/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import models.unitssold.BasicUnitsSoldModel;
import clojure.lang.PersistentHashMap;

import agents.modelbased.mckputil.Item;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

/*
 * This class extend the PenalizedKnapsackOptimizer class which extends SchlemazlAgent
 * 
 * It should contain methods applicable to the FastPMCKP algorithm that 
 * do not apply to other PKP algorithms
 */

public class FastMDPMCKP extends PenalizedKnapsackOptimizer {

	private boolean DEBUG = true; //set false to not print

	private int D = 2;
	private int W = 5;

	//parameters to be set/tuned
	private double _iterMultiplier = 2;
	private double _threshMultiplier = 1.5;
	private double _kappaMultiplier = .5;
	private int _maxIters =10;
	private double _threashold = .5;
	private int _seed = 0; //if set to zero then no seed will be used, seed is for testing purposes

	private double _finalKappa = 0; 
	private double _finalValue = 0;

	//these need to be created from info passed in to agent
	HashMap<QueryAuction, ArrayList<Item>> itemsLists;
	HashMap<Item, Predictions> itemPredictions;

	private double _bestKappa;

	private double _bestValue;

	private long _timeConstraint = 200000;

	/*
	 * Constructor, to set Fast specific parameters (move above here, should call PKO)
	 */
	public FastMDPMCKP(int daysToLook, long timeConstraint, PersistentHashMap perfectSim, String agentToReplace) {
		super(perfectSim, agentToReplace);
		D=daysToLook;
		_timeConstraint = timeConstraint;
	}


	/*
	 * (non-Javadoc)
	 * @see agents.modelbased.SchlemazlAgent#getBidBundle()
	 */
	@Override
	public BidBundle getBidBundle(){

		//initialize variables
		BidBundle bidBundle = new BidBundle();
		debug("BID BUNDLE");
		bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);


		//If we are past the initial couple days (where we perform a different strategy, since we have no query/sales reports)
		//(Or if we are using perfect models)
		if(_day >= lagDays || hasPerfectModels()){
			//Get remaining capacity
			//double remainingCap = getRemainingCap();

			// Create lists of possible bids/budgets we can place for each query

			//Only consider Nbids that would put us in each starting position.
			HashMap<Query,ArrayList<Double>> bidLists = getMinimalBidLists();
			//debug("bidLists"+ bidLists.toString());
			//bidLists = getPerfectBidLists(_filename);
			//debug("bidLists"+ bidLists.toString());

			HashMap<Query,ArrayList<Double>> budgetLists = getBudgetLists();
			//for (Query q : _querySpace) {
			//	debug(q + ": " + bidLists.get(q));
			//}
			//debug(" qsBB: "+_querySpace.size());
			//Create simulator (to be used for knapsack creation)
			PersistentHashMap querySim = setupSimForDay();

			/*
			 * Create items by simulating each query (given some initial capacity, everyone's bids, and everyone's budgets)
			 * (bid,budget)->clickPr, CPC, convProb, numImps,slotDistr,isRatioArr,ISRatio
			 */

			itemsLists = new HashMap<QueryAuction, ArrayList<Item>>();
			itemPredictions = new HashMap<Item, Predictions>();



			/*
			 * Make all Items for Knapsack
			 */
			long knapsackStart = System.currentTimeMillis(); ///Timing
			makeItemsForKnapsack(bidLists, budgetLists, itemsLists, itemPredictions, querySim);
			long knapsackEnd = System.currentTimeMillis(); //timing
			debug("Time to build knapsacks: " + (knapsackEnd-knapsackStart)/1000.0 );//HC num

			/*
			 * GET SOLUTION 
			 */

			long solutionStartTime = System.currentTimeMillis();
			//HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems = makeQueryAuctionItems(itemsLists);
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems = itemsLists;
			HashMap<Query,Item> solution = getSolution(itemsLists, itemPredictions);
			long solutionEndTime = System.currentTimeMillis();
			debug("Seconds to solution: " + (solutionEndTime-solutionStartTime)/1000.0 );


			/*
			 * Create BidBundle
			 */

			bidBundle = makeBidBundleFromSolution(solution, itemPredictions);
			debug("BidBundle: "+bidBundle);
			//Pass expected conversions to unit sales model
			double solutionWeight = _finalKappa;
			//debug("We expect to get " + (int)solutionWeight + " conversions");
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) (solutionWeight));

		}else{
			bidBundle = getFirst2DaysBundle();
		}

		//Just in case...
		for(Query q : _querySpace) {
			if(Double.isNaN(bidBundle.getBid(q)) || bidBundle.getBid(q) < 0) {
				//debug("ERROR bid bundle bib is NaN"); //ap
				bidBundle.setBid(q, 0.0);//HC num
			}
		}
		return bidBundle;
	}


	private HashMap<QueryAuction, ArrayList<Item>> makeQueryAuctionItems(HashMap<Query, ArrayList<Item>> itemLists) {

		HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems = new HashMap<QueryAuction, ArrayList<Item>>();
		Set<Query> queries = itemLists.keySet();
		for(Query q : queries){

			for(int d = 0; d<D;d++){
				ArrayList<Item> newItems = new ArrayList<Item>();
				//debug("Day "+d);
				for(Item i : itemLists.get(q)){

					if(i.getDaysFromNow()== d){
						//debug("Item day: "+i.getDaysFromNow());
						newItems.add(i);
						//debug("Adding: "+i);
					}else{
						//debug("not adding: "+i);
					}
				}

				QueryAuction qa = new QueryAuction(q, d);
				queryAuctionItems.put(qa, newItems);
				//debug("QA: "+qa.getDay()+" "+qa.getQuery()+" d:"+d);
				//debug("QAAUCTIONITEMS: "+queryAuctionItems.get(qa));
			}
		}
		return queryAuctionItems;
	}


	private BidBundle makeBidBundleFromSolution(HashMap<Query, Item> solution, HashMap<Item, Predictions> itemPredictions) {

		//initialize variables
		BidBundle bidBundle = new BidBundle();
		debug("BID BUNDLE");
		bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);

		for(Query q : _querySpace) {

			if(solution.containsKey(q)) {
				Item item = solution.get(q);
				double bid = item.b();
				//System.out.println("Bidding query: "+q.toString());
				if(bid< _paramEstimation.getRegReservePrediction(q.getType())){
					//debug("_Reset bid?_");
					bid = _paramEstimation.getRegReservePrediction(q.getType());
				}

				double budget = item.budget();

				if(solution.get(q).targ()) {
					bidBundle.setBid(q, bid);
					bidBundle.setAd(q, getTargetedAd(q,_manSpecialty,_compSpecialty));
				}
				else {
					bidBundle.addQuery(q, bid, new Ad());
				}

				// Only override the budget if the flag is setand we didn't choose to set a budget
				if(RESET_BUDGET && budget == Double.MAX_VALUE) {
					Predictions predictions = itemPredictions.get(solution.get(q));
					double clickPr = predictions.getClickPr();
					double numImps = predictions.getNumImp();
					int numClicks = (int) (clickPr * numImps);
					double CPC = predictions.getCPC();
					bidBundle.setDailyLimit(q, numClicks*CPC);
				}
				else {
					bidBundle.setDailyLimit(q, budget);
				}
			}else {
				/*
				 * We don't want to be in this query, so we use it to explore the space
				 * FIXME: Add a more sophisticated exploration strategy?
				 */
				if(!hasPerfectModels() && !q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
					double[] bidBudget = getProbeSlotBidBudget(q);
					double bid = bidBudget[0];
					double budget = bidBudget[1];
					Ad ad = getProbeAd(q,bid,budget);
					bidBundle.addQuery(q, bid, ad, budget);
					pcount+=1;
					debug("Q: "+ q.toString()+"Probing count: "+pcount);
				}
				else {
					bidBundle.addQuery(q,0.0,new Ad(),0.0);//HC num
				}
			}
		}
		return bidBundle;
	}


	private void makeItemsForKnapsack(HashMap<Query, ArrayList<Double>> bidLists, HashMap<Query, ArrayList<Double>> budgetLists,
			HashMap<QueryAuction, ArrayList<Item>> itemsLists, HashMap<Item, Predictions> itemPredictions, PersistentHashMap querySim) {
		for(int d=0;d<D;d++){
			for(Query q : _querySpace){
				if(!q.equals(new Query())&& q.getComponent()!=null && q.getManufacturer()!=null) { //Do not consider the (null, null) query. //FIXME: Don't hardcode the skipping of (null, null) query.

					int numItems = bidLists.get(q).size()*budgetLists.get(q).size();
					ArrayList<Item> itemList = new ArrayList<Item>(numItems);

					double salesPrice = _salesPrices.get(q);           

					//FIXME: Make configurable whether we allow for generic ads. Right now it's hardcoded that we're always targeting.
					for(int k = 1; k < 2; k++) { //For each possible targeting type (0=untargeted, 1=targetedToSpecialty)
						for(int i = 0; i < bidLists.get(q).size(); i++) { //For each possible bid
							for(int j = 0; j < budgetLists.get(q).size(); j++) { //For each possible budget

								Item newItem = makeNewItemandPredictions(k, i ,j, bidLists.get(q).get(i), budgetLists.get(q).get(j), 
										salesPrice, q, d, querySim, itemPredictions);
								//debug("DFN: "+newItem.getDaysFromNow()+" d: "+d);

								itemList.add(newItem);
								if(itemPredictions.get(newItem).getCost() + newItem.b()*2 < newItem.budget()) {//HC num
									//If we don't hit our budget, we do not consider higher budgets, 
									//since we will have the same result so we break out of the budget loop
									break;
								}
							}
						}
					}
					Item zeroItem = new Item(q,d, 0,0,0.0,0.0,false, true);
					itemList.add(zeroItem);
					//debug("Zero Item: "+zeroItem);

					itemPredictions.put(zeroItem, new Predictions());

					if(itemList.size() > 0) {
						QueryAuction qa = new QueryAuction(q,d);
						itemsLists.put(qa, itemList);
					}
				}
			}
		}

	}


	/*
	 * Used in getBidBundle
	 */
	private Item makeNewItemandPredictions(int k, int i, int j, double bid, double budget, double salesPrice, 
			Query q, int day, PersistentHashMap querySim, HashMap<Item, Predictions> itemPredictions){
		boolean targeting = (k == 0) ? false : true;

		Ad ad = (k == 0) ? new Ad() : getTargetedAd(q);               	  	

		double[] impsClicksAndCost = simulateQuery(querySim,q,bid,budget,ad);
		double numImps = impsClicksAndCost[0];
		double numClicks = impsClicksAndCost[1];
		double cost = impsClicksAndCost[2];

		//Amount of impressions our agent sees in each slot
		double[] slotDistr = new double[] {impsClicksAndCost[3],
				impsClicksAndCost[4],impsClicksAndCost[5],
				impsClicksAndCost[6],impsClicksAndCost[7]};

		//Fraction of IS users that occurred in each slot
		double[] isRatioArr = new double[] {impsClicksAndCost[8],
				impsClicksAndCost[9],impsClicksAndCost[10],
				impsClicksAndCost[11],impsClicksAndCost[12]};

		double ISRatio = impsClicksAndCost[13];
		double CPC = cost / numClicks;
		double clickPr = numClicks / numImps;
		double convProb = calculateConversionProb(q, getRemainingCap(), 0, ISRatio);
		//double weightSim = weightSimulator(q, numClicks, remainingCap, .996, ISRatio, _baseConvProbs.get(q));
		//debug("HERE OUT OF CALC");
		//debug("Bid: " + bid+ " Budget: " + budget+" Targeting: " + targeting+" numImps: " +
		//numImps+" numClicks: " + numClicks+" cost: " + cost+" CPC: " + CPC+" clickPr: " + clickPr);

		if(Double.isNaN(CPC)) {
			//debug("ERROR CPC NaN2"); //ap
			CPC = 0.0;//HC num
		}

		if(Double.isNaN(clickPr)) {
			//debug("ERROR clickPr NaN2"); //ap
			clickPr = 0.0;//HC num
		}

		if(Double.isNaN(convProb)) {
			//debug("ERROR convProWithPen NaN2"); //ap
			convProb = 0.0;//HC num
		}

		double w = numClicks*convProb;				
		//debug("w: "+w+"WeightSim: "+weightSim);
		//w=weightSim;
		double v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
		//debug("DAY: "+day);
		Item newItem = new Item(q, day, w,v,bid,budget,targeting);
		itemPredictions.put(newItem, new Predictions(clickPr, CPC, convProb, numImps,slotDistr,isRatioArr,ISRatio));

		return newItem; //HC num

	}

	private double getRemainingCap(){
		double remainingCap;
		if(!hasPerfectModels()) {
			remainingCap = _capacity*_capMod.get(_capacity) - _unitsSold.getWindowSold();
		}
		else {
			remainingCap = _capacity;

			int saleslen = _perfectStartSales.length;
			for(int i = saleslen-1; i >= 0 && i > saleslen-_capWindow; i--) {
				remainingCap -= _perfectStartSales[i];
			}

			if(saleslen < (_capWindow-1)) {
				remainingCap -= _capacity/((double)_capWindow) * (_capacity - 1 - saleslen);
			}
		}
		//debug("remainingCap: "+remainingCap);
		return remainingCap;
	}



	//helper method that passes in parameters set
	public double updateCapacity(QueryAuction thisQ, Item thisItem, HashMap<QueryAuction, ArrayList<Item>> qaItems, 
			HashMap<Item, Predictions> itemPredictions, double takenSoFar, int day){
		return updateCapacity(thisQ, thisItem, qaItems, itemPredictions,
				_threashold, _maxIters, takenSoFar, day) ;
	}

	public double updateCapacity(QueryAuction thisQ, Item thisItem, HashMap<QueryAuction, ArrayList<Item>> qaItems, HashMap<Item, Predictions> itemPredictions,
			double threashold, int maxIters, double takenSoFar, int day){ 
		int max = maxIters;
		double threash = threashold;
		double kappaLast = Double.MAX_VALUE;
		double kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, 0, thisQ, thisItem, day);
		debug("BaseKappa: "+kappa+" TSF: "+takenSoFar);
		double kappaBase = kappa;
		int numIters = 0;

		while(Math.abs(kappaLast-kappa) > threash){

			numIters+=1;
			if(numIters > max){

				numIters = 0;
				kappa = resetKappa(kappaBase);
				threash = increaseThreashold(threash);
				max = increaseMaxIters(max);
				//debug("HERE 2");
			}
			//debug("HERE");
			kappaLast = kappa;
			kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, kappaLast, thisQ, thisItem,day);
			//debug("Kappa from get W: "+kappa);
		}
		//debug("___________________________________________Converged________________________________");
		return kappa;
	}

//	public double updateCapacity(QueryAuction thisQ, Item thisItem, HashMap<QueryAuction, ArrayList<Item>> qaItems, HashMap<Item, Predictions> itemPredictions,
//			double threashold, int maxIters, double takenSoFar, int day){ 
//		int max = maxIters;
//		double threash = threashold;
//		double kappaLast = Double.MAX_VALUE;
//		double kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, 0, thisQ, thisItem);
//		debug("BaseKappa: "+kappa+" TSF: "+takenSoFar);
//		double kappaBase = kappa;
//		int numIters = 0;
//
//		while(Math.abs(kappaLast-kappa) > threash){
//
//			numIters+=1;
//			if(numIters > max){
//
//				numIters = 0;
//				kappa = resetKappa(kappaBase);
//				threash = increaseThreashold(threash);
//				max = increaseMaxIters(max);
//				//debug("HERE 2");
//			}
//			//debug("HERE");
//			kappaLast = kappa;
//			kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, kappaLast, thisQ, thisItem);
//			//debug("Kappa from get W: "+kappa);
//		}
//		//debug("___________________________________________Converged________________________________");
//		return kappa;
//	}
	
	private int increaseMaxIters(int maxIters) {

		return (int) Math.floor(maxIters*_iterMultiplier);
	}

	private double increaseThreashold(double threashold) {

		return threashold*_threshMultiplier;
	}

	private double resetKappa(double kappaBase) {

		Random rand = new Random();
		if(_seed !=0){
			rand = new Random(_seed);
		}
		return rand.nextDouble()+(_kappaMultiplier*kappaBase);
	}

	private double getWeightOfKnapsack(HashMap<QueryAuction, ArrayList<Item>> qaItems, 
			HashMap<Item, Predictions> itemPredictions, double takenSoFar, 
			double kappaLast, QueryAuction thisQA, Item thisItem) {
		Set<QueryAuction> queryAuctions = qaItems.keySet();

		double weight = 0;
		for (QueryAuction qa : queryAuctions){

			if(qa!=thisQA){
				Item item = getCurrentItem(qaItems.get(qa));
				debug("Item: "+item);
				if(item!=null){
					//debug("Conv PR: "+qa+" "+ takenSoFar);
					//debug(kappaLast+""); 
					//debug(""+itemPredictions.get(item));
					//debug(""+itemPredictions.get(item).getISRatio());
					double convProb = calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
					//debug("HERE getting Weight of K: KL: "+ kappaLast+" tsf: "+takenSoFar+" conv pr: "+convProb);
					double newW = itemPredictions.get(item).getNumImp()*itemPredictions.get(item).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
					//debug("TSF: "+takenSoFar);
					debug("1 newW: "+newW);
					weight+=newW;
				}
			}else{
				//debug("thisItem: "+thisItem);
				//debug("Conv PR: "+qa+" "+ takenSoFar);
				//debug("K last: "+kappaLast); 
				//debug("Num Imp "+itemPredictions.get(thisItem).getNumImp());
				//	debug("IS Ratio "+itemPredictions.get(thisItem).getISRatio());
				double newW2 = itemPredictions.get(thisItem).getNumImp()*itemPredictions.get(thisItem).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(thisItem).getISRatio());
				debug("2 newW: "+newW2);
				weight+=newW2;

			}
		}
		//debug("W: "+weight);
		return weight;
	}
	
	private double getWeightOfKnapsack(HashMap<QueryAuction, ArrayList<Item>> qaItems, 
			HashMap<Item, Predictions> itemPredictions, double takenSoFar, 
			double kappaLast, QueryAuction thisQA, Item thisItem, int day) {
		Set<QueryAuction> queryAuctions = qaItems.keySet();

		double weight = 0;
		for (QueryAuction qa : queryAuctions){
			if(qa.getDay()== day){
			if(qa!=thisQA){
				Item item = getCurrentItem(qaItems.get(qa));
				//debug("Item: "+item);
				if(item!=null){
					//debug("Conv PR: "+qa+" "+ takenSoFar);
					//debug(kappaLast+""); 
					//debug(""+itemPredictions.get(item));
					//debug(""+itemPredictions.get(item).getISRatio());
					double convProb = calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
					//debug("HERE getting Weight of K: KL: "+ kappaLast+" tsf: "+takenSoFar+" conv pr: "+convProb);
					double newW = itemPredictions.get(item).getNumImp()*itemPredictions.get(item).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
					//debug("TSF: "+takenSoFar);
					//debug("1 newW: "+newW);
					weight+=newW;
				}
			}else{
				//debug("thisItem: "+thisItem);
				//debug("Conv PR: "+qa+" "+ takenSoFar);
				//debug("K last: "+kappaLast); 
				//debug("Num Imp "+itemPredictions.get(thisItem).getNumImp());
				//	debug("IS Ratio "+itemPredictions.get(thisItem).getISRatio());
				double newW2 = itemPredictions.get(thisItem).getNumImp()*itemPredictions.get(thisItem).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(thisItem).getISRatio());
				//debug("2 newW: "+newW2);
				weight+=newW2;

			}
		}
		}
		//debug("W: "+weight);
		return weight;
	}

	private double getValueOfKnapsack(HashMap<QueryAuction, ArrayList<Item>> qaItems, 
			HashMap<Item, Predictions> itemPredictions, double takenSoFar, 
			double kappaLast, QueryAuction thisQA, Item thisItem, ArrayList<Double> dailyCapacities) {
		Set<QueryAuction> queryAuctions = qaItems.keySet();
		double value = 0;
		double salesPrice = 10;
		double numClicks = 0;
		double convProb = 0;
		double CPC = 0;
		for (QueryAuction qa : queryAuctions){
			takenSoFar = getSoldInWindow(W, D, dailyCapacities, qa.getDay());
			kappaLast = dailyCapacities.get(W+qa.getDay()-2);
			if(qa!=thisQA){
				Item item = getCurrentItem(qaItems.get(qa));
				if(item!=null){
					//debug("o item v: "+item.v());
					numClicks =itemPredictions.get(item).getNumImp()*itemPredictions.get(item).getClickPr();
					convProb = calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
					CPC = itemPredictions.get(item).getCPC();
					salesPrice = _salesPrices.get(qa.getQuery());
				}
			}else{
				//debug("n item v: "+thisItem.v());
				numClicks =itemPredictions.get(thisItem).getNumImp()*itemPredictions.get(thisItem).getClickPr();
				convProb = calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(thisItem).getISRatio());
				CPC = itemPredictions.get(thisItem).getCPC();
				salesPrice = _salesPrices.get(qa.getQuery());
				//double salesPrice = 10; //CHANGE BACK HERE


			}

			//debug("1: #clicks: "+numClicks+" convP: "+convProb+" CPC: "+CPC);
			double rev = (numClicks*convProb*salesPrice);
			double cost = (numClicks*CPC);
			double val = rev-cost; 
			//debug("Val: "+val+" rev: "+rev+" cost: "+cost);
			value += val;

		}
		debug("value: "+value);
		return value;
	}

	public double updatePenalty(QueryAuction thisQ, Item thisItem, 
			HashMap<QueryAuction, ArrayList<Item>> qaItems, HashMap<Item, Predictions> itemPredictions,
			ArrayList<Double> dailyCapacities, int d){
		double newPenalty = 0;
		Set<QueryAuction> queryAuctions = qaItems.keySet();
		for(QueryAuction qa : queryAuctions){
			if(qa.getDay()==d){
				//debug("QA: "+qa+" qaitemsQa size: "+qaItems.get(qa));
				Item oldItem = getCurrentItem(qaItems.get(qa));
				//debug("oldItem: "+oldItem.toString());
				debug("DC before window: "+dailyCapacities);
				double soldInWindow = getSoldInWindow(W, D, dailyCapacities, d);
				if(qa == thisQ){
					Item qaItem = thisItem;
					//debug("qaItem: "+qaItem.toString());
					double conversionProb = calculateConversionProb(qa, soldInWindow, dailyCapacities.get(d+W-1), itemPredictions.get(qaItem).getISRatio());
					double newValue = calcItemValue(qaItem, conversionProb, itemPredictions);
					newPenalty = newPenalty+Math.abs(oldItem.v()-newValue);
					debug("HERE AND OUT");
				}else{
					//debug("Inputs: ");
					//debug("qa "+ qa);
					//debug("newK " +dailyCapacities.get(d+W-1));
					//debug("old item "+oldItem);
					//debug("pred "+itemPredictions.get(oldItem));
					//debug("IS "+itemPredictions.get(oldItem).getISRatio());
					double conversionProb = calculateConversionProb(qa, soldInWindow, dailyCapacities.get(d+W-1), itemPredictions.get(oldItem).getISRatio());
					double newValue = calcItemValue(oldItem, conversionProb, itemPredictions);
					newPenalty = newPenalty+Math.abs(oldItem.v()-newValue);
				}
			}
		}

		return newPenalty;
	}



	private double calcItemValue(Item item, double conversionProb, HashMap<Item,Predictions> itemPredictions) {
		//debug("Pred Size: "+itemPredictions.size());
		//debug("Item: "+item.toString());
		Predictions predictions = itemPredictions.get(item);

		double numClicks = predictions.getClickPr()*predictions.getNumImp();

		return numClicks*conversionProb;
	}

	public double calculateEfficiency(double marginalVal, double marginalWeight){


		if(marginalWeight!=0){
			//debug("c");
			if(marginalWeight<10){
				debug("Val: "+marginalVal+" Weight: "+marginalWeight);
				return marginalVal;
			}
			return marginalVal/marginalWeight;
		}else{

			return Double.POSITIVE_INFINITY;
		}
	}

	




	


	public HashMap<Query, Item> getSolution(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems, 
			HashMap<Item, Predictions> itemPredictions){
		debug("___________________________________________________GETTING NEW SOLUTION__________________________________________");
		//Initialize variables	
		
		HashMap<Query, Item> bestBidBundle = null;
		double kappa = 0; 
		double newKappa = 0;
		double kappaBest = 0;

		Item itemBest = null;
		QueryAuction queryAuctionBest = null;

		double rho =0;
		double newRho = 0;
		double rhoBest = 0; 

		double eBest = Double.MIN_VALUE;
		double EBest = 0;

		double eNew = 0;
		double ENew = 0;

		double bestRhoEver = 0;
		double bestKappaEver = 0;

//		_capacity = 200;
//		double remainingCap = _capacity - (4*(_capacity/5));
		ArrayList<Double> dailyCapacities = initDailyCapacities(W,D);
		debug("DC: "+dailyCapacities);
		//ArrayList<Double> dailyCapacities = makeDailyCapacities();
		Set<QueryAuction> auctions = queryAuctionItems.keySet();

		int count = 0;
		double valDiff = 0;
		long time = 0;
		
		long timeStart = System.currentTimeMillis();
		long timeNow = System.currentTimeMillis();

		while((EBest>0 || eBest>0) && count <=200){
		//while((EBest>0 || eBest>0) && time < _timeConstraint){
			//debug("HERE "+currE);
			EBest = 0;
			ENew = 0;
			eBest = 0;
			eNew = 0;
			valDiff = 0;

			for (QueryAuction qa : auctions){
				//Item currentItem = getCurrentItem(queryAuctionItems.get(qa));
				for(Item j : queryAuctionItems.get(qa)){
					if(!j.hasBeenTaken()){
						debug("Consid: "+j);
						//newKappa = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);

						//calculate new Kappa
						dailyCapacities = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);
						debug("DC after update: "+dailyCapacities);
						newKappa = getSoldInKnapsack(W, D, dailyCapacities);
						//debug("Sold in Knapsack");
						double remainingCap = getRemainingCap(); 
						newRho = getValueOfKnapsack(queryAuctionItems, itemPredictions, remainingCap, newKappa, qa, j, dailyCapacities);
						//						if(newRho > bestRhoEver){
						//							//							debug("__Value "+newValue);
						//							bestRhoEver = rho;
						//							bestKappaEver = kappa;
						//						}
						



						if(newRho >rho){
							if(kappa == newKappa && newRho > rho){
								if((newRho-rho)>valDiff){
									valDiff = newRho-rho;
									//debug("Count: "+count+" considering: "+j+ " valDiff= "+valDiff);
								}
							}else if(newKappa>kappa && newRho > rho && valDiff==0){
								eNew = (newRho-rho)/(newKappa-kappa);
								//debug("Count: "+count+" considering: "+j+ " e= "+eNew);
							}
						}else if(newKappa < kappa && newRho>0 && valDiff == 0 && eBest ==0){
							ENew = (newRho/newKappa)-(rho/kappa);
							//debug("Count: "+count+" considering: "+j+ " E= "+ENew);
						}

						if(valDiff>0){
							EBest = 1;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;
						}else if(eNew>eBest){
							//debug("e Item Better: "+eNew+ " currBest: "+eBest);
							eBest = eNew;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;

						}else if(ENew>EBest){
							EBest = ENew;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;
						}

					}
				}
				//debug("BEFORE IF EBest: "+EBest + "currE: "+currE+" eBest: "+eBest);
				if(valDiff >0 || eBest>0){
					debug("e taken");
					count+=1;
					kappa = kappaBest; 
					rho = rhoBest;
					resetSolution(queryAuctionItems, queryAuctionBest, itemBest);
					//					if(rhoBest > bestRhoEver){
					//						debug("VeVeVe Marking a best Value "+rhoBest);
					//						bestRhoEver = rhoBest;
					//						bestKappaEver = kappaBest;
					//					}

				}else if(EBest>0){
					if(rho > bestRhoEver){
						debug("VeVeVe Marking a best Value "+rho);
						bestRhoEver = rho;
						bestKappaEver = kappa;
						bestBidBundle = makeBidBundle(queryAuctionItems);
					}
					debug("E taken");
					count+=1;
					kappa = kappaBest; 
					rho = rhoBest;
					resetSolution(queryAuctionItems, queryAuctionBest, itemBest);
					queryAuctionItems = resetAllItems(queryAuctionItems);
					//					if(rhoBest > bestRhoEver){
					//						debug("EEEE Marking a best Value "+newRho);
					//						bestRhoEver = rhoBest;
					//						bestKappaEver = kappaBest;
					//					}

				}

			}
			//debug("EBest: "+EBest+" eBest"+eBest+ " valDiff: "+valDiff);
			timeNow = System.currentTimeMillis();
			time = timeNow - timeStart;
		}
		

		_bestKappa = Math.max(bestKappaEver, kappa);
		_bestValue = Math.max(bestRhoEver, rho);

		_finalKappa = _bestKappa;
		
		debug("finalKappa: "+_finalKappa+ " bestK: "+_bestKappa+" bestP: "+_bestValue);
		if(bestBidBundle == null){
			return makeBidBundle(queryAuctionItems);
		}else{
			return bestBidBundle;
		}
		

	}

	
	public HashMap<Query, Item> getSolutionTest(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems, 
			HashMap<Item, Predictions> itemPredictions){
		debug("___________________________________________________GETTING NEW SOLUTION__________________________________________");
		//Initialize variables	
		double kappa = 0; 
		double newKappa = 0;
		double kappaBest = 0;

		Item itemBest = null;
		QueryAuction queryAuctionBest = null;

		double rho =0;
		double newRho = 0;
		double rhoBest = 0; 

		double eBest = Double.MIN_VALUE;
		double EBest = 0;

		double eNew = 0;
		double ENew = 0;

		double bestRhoEver = 0;
		double bestKappaEver = 0;

		_capacity = 200;
		double remainingCap = _capacity - (4*(_capacity/5));
		//ArrayList<Double> dailyCapacities = initDailyCapacities(W,D);
		ArrayList<Double> dailyCapacities = makeDailyCapacities();
		Set<QueryAuction> auctions = queryAuctionItems.keySet();

		int count = 0;
		double valDiff = 0;

		while((EBest>0 || eBest>0) && count <=100){

			//debug("HERE "+currE);
			EBest = 0;
			ENew = 0;
			eBest = 0;
			eNew = 0;
			valDiff = 0;

			for (QueryAuction qa : auctions){
				Item currentItem = getCurrentItem(queryAuctionItems.get(qa));
				for(Item j : queryAuctionItems.get(qa)){
					if(!j.hasBeenTaken()){

						//newKappa = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);

						//calculate new Kappa
						dailyCapacities = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);
						newKappa = getSoldInKnapsack(W, D, dailyCapacities);


						newRho = getValueOfKnapsack(queryAuctionItems, itemPredictions, remainingCap, newKappa, qa, j, dailyCapacities);
						//						if(newRho > bestRhoEver){
						//							//							debug("__Value "+newValue);
						//							bestRhoEver = rho;
						//							bestKappaEver = kappa;
						//						}
						//double remainingCap = getRemainingCap(); //CHANGE BACK;



						if(newRho >rho){
							if(kappa == newKappa && newRho > rho){
								if((newRho-rho)>valDiff){
									valDiff = newRho-rho;
									debug("Count: "+count+" considering: "+j+ " valDiff= "+valDiff);
								}
							}else if(newKappa>kappa && newRho > rho && valDiff==0){
								eNew = (newRho-rho)/(newKappa-kappa);
								//debug("Count: "+count+" considering: "+j+ " e= "+eNew);
							}
						}else if(newKappa < kappa && newRho>0 && valDiff == 0 && eBest ==0){
							ENew = (newRho/newKappa)-(rho/kappa);
							//debug("Count: "+count+" considering: "+j+ " E= "+ENew);
						}

						if(valDiff>0){
							EBest = 1;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;
						}else if(eNew>eBest){
							//debug("e Item Better: "+eNew+ " currBest: "+eBest);
							eBest = eNew;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;

						}else if(ENew>EBest){
							EBest = ENew;
							queryAuctionBest = qa;
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;
						}

					}
				}
				//debug("BEFORE IF EBest: "+EBest + "currE: "+currE+" eBest: "+eBest);
				if(valDiff >0 || eBest>0){
					debug("e taken");
					count+=1;
					kappa = kappaBest; 
					rho = rhoBest;
					resetSolution(queryAuctionItems, queryAuctionBest, itemBest);
					//					if(rhoBest > bestRhoEver){
					//						debug("VeVeVe Marking a best Value "+rhoBest);
					//						bestRhoEver = rhoBest;
					//						bestKappaEver = kappaBest;
					//					}

				}else if(EBest>0){
					if(rho > bestRhoEver){
						debug("VeVeVe Marking a best Value "+rho);
						bestRhoEver = rho;
						bestKappaEver = kappa;
					}
					debug("RESET");
					count+=1;
					kappa = kappaBest; 
					rho = rhoBest;
					resetSolution(queryAuctionItems, queryAuctionBest, itemBest);
					queryAuctionItems = resetAllItems(queryAuctionItems);
					//					if(rhoBest > bestRhoEver){
					//						debug("EEEE Marking a best Value "+newRho);
					//						bestRhoEver = rhoBest;
					//						bestKappaEver = kappaBest;
					//					}

				}

			}
			//debug("EBest: "+EBest+" eBest"+eBest+ " valDiff: "+valDiff);
		}
		_finalKappa = kappa;

		_bestKappa = bestKappaEver;
		_bestValue = bestRhoEver;


		debug("finalKappa: "+_finalKappa+ " bestK: "+bestKappaEver+" bestP: "+bestRhoEver);
		return makeBidBundle(queryAuctionItems);

	}
	
	//WORKING VERSION BELOW TO KEEP AROUND
	//	public HashMap<Query, Item> getSolution(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems, 
	//			HashMap<Item, Predictions> itemPredictions){
	//		debug("___________________________________________________GETTING NEW SOLUTION__________________________________________");
	//		//Initialize variables	
	//		double kappa = 0; 
	//		double newKappa = 0;
	//		double kappaBest_e = 0;
	//		double kappaBest_E = 0;
	//
	//		Item itemBest_E = null;
	//		QueryAuction queryAuctionBest_E = null;
	//		Item itemBest_e = null;
	//		QueryAuction queryAuctionBest_e = null;
	//
	//		double newRho = 0;
	//		double rho =0;
	//
	//		double rhoBest_E = 0; 
	//		double rhoBest_e = 0; 
	//
	//		double value = 0;
	//		double newValue = 0;
	//		double bestValue_e = 0;
	//		double bestValue_E = 0;
	//
	//
	//		double eBest = Double.MIN_VALUE;
	//		double EBest = 0;
	//
	//
	//		double marginalVal = 0;
	//		double marginalWeight = 0;
	//
	//		double eNew = 0;
	//		double ENew = 0;
	//		double currE = 0; 
	//
	//		_capacity = 200;
	//		double remainingCap = _capacity - (4*(_capacity/5));
	//		//ArrayList<Double> dailyCapacities = initDailyCapacities(W,D);
	//		ArrayList<Double> dailyCapacities = makeDailyCapacities();
	//		Set<QueryAuction> auctions = queryAuctionItems.keySet();
	//		int count = 0;
	//		double bestValueEver = 0;
	//		double bestWeightEver = 0;
	//		while((EBest>currE || eBest>0) && count < 15){
	//			if(kappa!=0){
	//				currE = value/kappa;
	//			}else{
	//				currE = 0;
	//			}
	//			//debug("HERE "+currE);
	//			EBest = currE;
	//			ENew = 0;
	//			eBest = 0;
	//			eNew = 0;
	//
	//			for (QueryAuction qa : auctions){
	//				Item currentItem = getCurrentItem(queryAuctionItems.get(qa));
	//				for(Item j : queryAuctionItems.get(qa)){
	//					if(!j.hasBeenTaken()){
	//						//debug("Count: "+count+" considering: "+j);
	//						//newKappa = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);
	//						dailyCapacities = updateMDKCapacity(qa, j, queryAuctionItems, itemPredictions, dailyCapacities);
	//						newKappa = getSoldInKnapsack(W, D, dailyCapacities);
	//						//debug("newKappa = "+newKappa+" item W: "+j.w());
	//						//debug("Used: "+ (_capacity-getRemainingCap()));
	//						//newRho = updateMDKPenalty(qa, j , queryAuctionItems, itemPredictions, dailyCapacities);
	//						//double remainingCap = getRemainingCap(); //CHANGE BACK;
	//
	//						debug(" newK: "+newKappa);
	//						newValue = getValueOfKnapsack(queryAuctionItems, itemPredictions, remainingCap, newKappa, qa, j);
	//						if(newValue > bestValueEver){
	//							debug("__Value "+newValue);
	//							bestValueEver = value;
	//							bestWeightEver = kappa;
	//						}
	//						marginalWeight = newKappa - kappa;
	//						//debug("Inputs: "+j.v());
	//						//debug(" "+currentItem);
	//						//debug(" "+newRho+" "+rho);
	//						//marginalVal= (j.v()-currentItem.v() )-(newRho-rho);
	//						marginalVal= newValue-value;
	//						//debug("MarginalW: "+marginalWeight);
	//
	//						//(oldItem's value- newItem's value)-(oldPenalty-newPenalty)
	//
	//
	//						//debug("Val: "+marginalVal+" Val Calc: "+currentItem.v()+" "+ j.v()+" "+ rho+" "+newRho);
	//						eNew = calculateEfficiency(marginalVal, marginalWeight);
	//						//debug("NUMS: newV: "+newValue+" newK: "+newKappa+" V: "+value+" K: "+kappa);
	//						if(newKappa!=0){
	//							ENew = Math.abs(newValue/newKappa);
	//						}else if(newValue>0){
	//							ENew = Double.POSITIVE_INFINITY;
	//						}else{
	//							debug(" BAD ELSE ");
	//							ENew = Double.NEGATIVE_INFINITY;
	//						}
	//						//debug("ENew: "+ENew+" EBest: "+EBest+" eNew: "+eNew+" eBest: "+eBest);
	//						//eNew = marginalVal;
	//						//debug("eNew: "+eNew);
	//						if(newKappa<=kappa){
	//							if(ENew>EBest){
	//								//debug("***********************E Item Better: "+ENew+ " currBest: "+EBest);
	//								EBest = ENew;
	//								queryAuctionBest_E = qa;					
	//								itemBest_E = j;
	//								kappaBest_E = newKappa;
	//								bestValue_E = newValue;
	//								rhoBest_E = newRho;
	//							}
	//
	//						}else{
	//							if(eNew>eBest){
	//								//debug("e Item Better: "+eNew+ " currBest: "+eBest);
	//								eBest = eNew;
	//								queryAuctionBest_e = qa;
	//								itemBest_e = j;
	//								kappaBest_e = newKappa;
	//								bestValue_e = newValue;
	//								rhoBest_e = newRho;
	//							}
	//
	//						}
	//
	//
	//						//						if(ENew>eBest && (newValue>=value)){//||((newValue<value) && (newKappa<kappa)))){
	//						//						//if(ENew>eBest){
	//						//							//debug(" NUMS: "+newValue+" "+newKappa+" "+value+" "+kappa);
	//						//							debug("Item Better: "+ENew+ " currBest: "+eBest);
	//						//							eBest = ENew;
	//						//							queryAuctionBest = qa;
	//						//
	//						//							itemBest = j;
	//						//							kappaBest = newKappa;
	//						//							bestValue = newValue;
	//						//							rhoBest = newRho;
	//
	//						//						}else if(ENew>eBest && newValue>bestValue&& ENew<0){
	//						//							debug("Item Better: "+ENew+ " valBest: "+newValue+" bestVal: "+bestValue);
	//						//							eBest = ENew;
	//						//							queryAuctionBest = qa;
	//						//							
	//						//							itemBest = j;
	//						//							kappaBest = newKappa;
	//						//							bestValue = newValue;
	//						//							rhoBest = newRho;
	//
	//					}
	//				}
	//			}
	//			//debug("BEFORE IF EBest: "+EBest + "currE: "+currE+" eBest: "+eBest);
	//			if (eBest>0){
	//				count+=1;
	//				debug("e Taking "+count+": "+eBest+"  "+itemBest_e);
	//				kappa = kappaBest_e; 
	//				value = bestValue_e;
	//				//debug("K: "+kappa+" V: "+value);
	//				rho = rhoBest_e;
	//				resetSolution(queryAuctionItems, queryAuctionBest_e, itemBest_e);
	//				currE = value/kappa;
	//
	//			}else if(EBest>currE){
	//				count+=1;
	//				debug("E Taking "+count+": "+EBest+"  "+itemBest_E);
	//				kappa = kappaBest_E; 
	//				value = bestValue_E;
	//				//debug("K: "+kappa+" V: "+value);
	//				rho = rhoBest_E;
	//				resetSolution(queryAuctionItems, queryAuctionBest_E, itemBest_E);
	//				//debug("Taken? "+itemBest_E.hasBeenTaken());
	//				queryAuctionItems = resetAllItems(queryAuctionItems);
	//
	//			}
	//			if(value > bestValueEver){
	//				printCurrentKnapsack(queryAuctionItems);
	//				debug("__Value "+value);
	//				bestValueEver = value;
	//				bestWeightEver = kappa;
	//			}
	//			
	//		}
	//
	//		_finalKappa = kappa;
	//
	//		_bestKappa = bestWeightEver;
	//		_bestValue = bestValueEver;
	//
	//		//debug("BestV: "+bestValueEver+" bestW: "+bestWeightEver);
	//		return makeBidBundle(queryAuctionItems);
	//
	//	}

	public void getOptimal(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems, 
			HashMap<Item, Predictions> itemPredictions){
		//debug("___________________________________________________GETTING Optimal SOLUTION__________________________________________");
		//Initialize variables	

		double newKappa = 0;
		double newValue = 0;

		_capacity = 200;
		double remainingCap = _capacity - (4*(_capacity/5));
		//ArrayList<Double> dailyCapacities = initDailyCapacities(W,D);
		ArrayList<Double> dailyCapacities = makeDailyCapacities();
		Set<QueryAuction> auctions = queryAuctionItems.keySet();
		int count = 0;
		double bestValueEver = 0;
		double bestWeightEver = 0;
		QueryAuction[] qas = new QueryAuction[4];
		int q=0;
		for(QueryAuction qa : auctions){
			qas[q] = qa;
			q+=1;
		}

		//debug("HERE");
		for(Item j : queryAuctionItems.get(qas[0])){
			j.setCurrentlyTaken(true);

			for(Item k : queryAuctionItems.get(qas[1])){

				k.setCurrentlyTaken(true);
				for(Item m : queryAuctionItems.get(qas[2])){

					m.setCurrentlyTaken(true);

					for(Item l : queryAuctionItems.get(qas[3])){

						//get value and weight of knapsack
						dailyCapacities = updateMDKCapacity(qas[3], l, queryAuctionItems, itemPredictions, dailyCapacities);
						l.setCurrentlyTaken(true);
						newKappa = getSoldInKnapsack(W, D, dailyCapacities);
						//debug("newKappa = "+newKappa+" item W: "+j.w());
						//debug("Used: "+ (_capacity-getRemainingCap()));
						//newRho = updateMDKPenalty(qa3, j , queryAuctionItems, itemPredictions, dailyCapacities);
						//double remainingCap = getRemainingCap(); //CHANGE BACK;

						//debug(" newK: "+newKappa);
						newValue = getValueOfKnapsack(queryAuctionItems, itemPredictions, remainingCap, newKappa, qas[3], l, dailyCapacities);

						//debug("V: "+newValue+" W: "+newKappa);
						if(newValue>bestValueEver){
							//											clearOptKnapsack(queryAuctionItems);
							//											l.setInOpt();
							//											j.setInOpt();
							//											k.setInOpt();
							bestValueEver = newValue;
							bestWeightEver = newKappa;
							if(newValue > 13000){
								//							debug("_________________________________________________________________________");
								//							newKappa = 123.41417510053826;
								//							debug(" newK: "+newKappa);
								//							newValue = getValueOfKnapsack(queryAuctionItems, itemPredictions, remainingCap, newKappa, qas[2], l);
								//
								//							debug("V: "+newValue+" W: "+newKappa);
								printCurrentKnapsack(queryAuctionItems);
								//							bestValueEver = newValue;
								//							bestWeightEver = newKappa;
							}
							if(l.w()==56.16 || k.w()==56.16 || j.w()==56.16){
								debug("HERE 2");
							}

						}
						l.setCurrentlyTaken(false);
					}

					m.setCurrentlyTaken(false);
				}
				k.setCurrentlyTaken(false);
			}


			j.setCurrentlyTaken(false);
		}



		//debug("Best: V: "+bestValueEver+" W: "+bestWeightEver);
		_bestKappa = bestWeightEver;
		_bestValue = bestValueEver;
	}

	

	private ArrayList<Double> makeDailyCapacities() {
		ArrayList<Double> dailyCaps = new ArrayList<Double>();
		dailyCaps.add(40.0);
		dailyCaps.add(40.0);
		dailyCaps.add(40.0);
		dailyCaps.add(40.0);
		dailyCaps.add(0.0);
		//dailyCaps.add(0.0);


		return dailyCaps;
	}


	private double updateMDKPenalty(QueryAuction qa, Item j,
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems,
			HashMap<Item, Predictions> itemPredictions,
			ArrayList<Double> dailyCapacities) {
		double newPenalty =0;
		for(int d = 0;d<D;d++){
			newPenalty+=updatePenalty(qa, j , queryAuctionItems, itemPredictions, dailyCapacities, d);
		}
		return newPenalty;
	}

	//	private double updateMDKValue(QueryAuction qa, Item j,
	//			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems,
	//			HashMap<Item, Predictions> itemPredictions,
	//			ArrayList<Double> dailyCapacities) {
	//		double newPenalty =0;
	//		for(int d = 0;d<D;d++){
	//			newPenalty+=updatePenalty(qa, j , queryAuctionItems, itemPredictions, dailyCapacities, d);
	//		}
	//		return newPenalty;
	//	}


	private ArrayList<Double> updateMDKCapacity(QueryAuction qa, Item item,
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems,
			HashMap<Item, Predictions> itemPredictions,
			ArrayList<Double> dailyCapacities) {
		double dKappa = 0;
		double sold = 0;
		//debug("HERE BEFORE LOOP");
		for(int d=0; d< D; d++){
			sold = getSoldInWindow(W,D, dailyCapacities, d);
			debug("sold: "+sold);
			dKappa = updateCapacity(qa, item, queryAuctionItems, itemPredictions, sold, d);
			
			dailyCapacities = updateDailyCapacities( W, D, dailyCapacities, d, dKappa);
			//total+=dKappa;
			//debug("Update: d:"+d+" dKappa: "+dKappa);
		}

		return dailyCapacities;
	}


	private ArrayList<Double> initDailyCapacities(int W, int D) {
		ArrayList<Double> dailyCapacities = new ArrayList<Double>();
		_capWindow = 5;
		int[] preDaySales = new int[_capWindow-1];
		if(!hasPerfectModels()) {
			ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP);

			Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			soldArray.add(expectedConvsYesterday);

			for(int i = 0; i < (_capWindow-1); i++) {
				int idx = soldArray.size()-1-i;//HC num
				if(idx >= 0) {
					preDaySales[_capWindow-2-i] = soldArray.get(idx);//HC num
				}
				else {
					preDaySales[_capWindow-2-i] = (int)(_capacity / ((double) _capWindow));//HC num
				}
			}
		}
		else {
			for(int i = 0; i < (_capWindow-1); i++) {
				int idx = _perfectStartSales.length-1-i;
				if(idx >= 0) {
					preDaySales[_capWindow-2-i] = _perfectStartSales[idx];//HC num
				}
				else {
					preDaySales[_capWindow-2-i] = (int)(_capacity / ((double) _capWindow));//HC num
				}
			}
		}

		//for(int d=0;d<(2*W+D-2);d++){
		for(int d=0;d<(W+D-1);d++){
			if(d<(W-1)){
				//debug("d: "+d+" length: "+preDaySales.length);
				dailyCapacities.add(d, (double) preDaySales[d]);
			}else{
				dailyCapacities.add(d, 0.0);
			}
		}
		return dailyCapacities;
	}

	private ArrayList<Double> updateDailyCapacities(int W, int D, ArrayList<Double> dailyCapacities, int d, double newKappa) {
		//debug("DC size: "+dailyCapacities.size());
		dailyCapacities.set((W-1+d), newKappa);
		return dailyCapacities;
	}

	private double getSoldInKnapsack(int W, int D, ArrayList<Double> dailyCapacities) {

		double sold = 0.0;
		for(int d=0;d<dailyCapacities.size();d++){
			//debug("here  dc: "+dailyCapacities.get(d)+" d: "+d);
			//sold+= dailyCapacities.get(d);
		}
		for(int d=(W-1);d<(W-1)+D;d++){
			//debug("here "+" dc: "+dailyCapacities.get(d));
			sold+= dailyCapacities.get(d);
		}
		//debug("sold: "+sold);
		return sold;
	}

	private double getSoldInWindow(int W, int D, ArrayList<Double> dailyCapacities, int today) {

		double sold = 0.0;
		for(int d=(today+W-3);d>=today;d--){
			//double temp =  dailyCapacities.get(d);
			//debug("temp: "+temp);
			sold+= dailyCapacities.get(d);
		}

		return sold;
	}

	private HashMap<Query, Item> makeBidBundle(
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems) {
		HashMap<Query, Item> solution = new HashMap<Query,Item>();
		Set<QueryAuction> queryAuctions = queryAuctionItems.keySet();
		for(QueryAuction qa : queryAuctions){
			if(qa.getDay()==0){
				Item item = getCurrentItem(queryAuctionItems.get(qa));
				solution.put(qa.getQuery(),  item);
			}
		}
		return solution;
	}

	private void resetSolution(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems,
			QueryAuction queryAuctionBest, Item itemBest) {
		//debug("QAI size: " +queryAuctionItems.size());
		//debug("QA best:" +queryAuctionBest);
		for(Item j : queryAuctionItems.get(queryAuctionBest)){
			if(j.isCurrentlyTaken()){
				//debug("Untake");
				j.setCurrentlyTaken(false);
				//j.setHasBeenTaken();
			}
		}
		itemBest.setCurrentlyTaken(true);
		itemBest.setHasBeenTaken();

	}

	private HashMap<QueryAuction, ArrayList<Item>>  resetAllItems(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems) {
		//debug("QAI size: " +queryAuctionItems.size());
		//debug("QA best:" +queryAuctionBest);
		Set<QueryAuction> qas = queryAuctionItems.keySet();
		for(QueryAuction qa : qas){
			for(Item j : queryAuctionItems.get(qa)){
				if(!j.isCurrentlyTaken()){
					//debug("Untake");
					j.setHasBeenTaken(false);
				}
			}

		}
		return queryAuctionItems;

	}

	private Item getCurrentItem(ArrayList<Item> items) {
		for(Item j : items){
			if(j.isCurrentlyTaken()){
				return j;
			}else{
				//debug("Not taken: "+j);
			}
		}
		return null;
	}

	private void debug(String output){
		if(DEBUG){
			System.out.println(output);
		}
	}

	//_____________________________________________________________________

	public Set<Query> makeQuerySet(int numQueries){

		Set<Query> newQuerySpace = new HashSet<Query>();

		for(int i = 65;i<(65+numQueries);i++){
			//Query(manufact, comp)
			Query q = new Query(Character.toString((char)i), "Comp"+(i-65));
			newQuerySpace.add(q);

		}
		return newQuerySpace;

	}

	public void makeItems(int numPerQuery, Set<Query> newQuerySpace, int seed){
		itemPredictions = new HashMap<Item, Predictions>();
		itemsLists = new HashMap<QueryAuction, ArrayList<Item>>();
		Random rand = new Random(seed);
		int[] budgets = {50,100, 400, 800};

		for(Query q : newQuerySpace){
			ArrayList<Item> itemsList = new ArrayList<Item>();
			Item zeroItem = new Item(q,1, 0,0,0.0,0.0,false, false);
			itemsList.add(zeroItem);
			itemPredictions.put(zeroItem, new Predictions());
			debug("Item: "+zeroItem);
			debug("Pred: "+itemPredictions.get(zeroItem));
			for(int i =0;i<numPerQuery;i++){

				//Not Needed, doesn't need to change
				double[] isRatioArr = {0,0};
				double[] slotDistr = {0,0};
				boolean targeting = true;
				int day = 1;

				//generate randomly
				double bid = rand.nextInt(3)+rand.nextDouble();
				if(bid< .5){
					bid+=.2;
				}
				double budget = budgets[rand.nextInt(budgets.length)];

				double convProb = .36; //from spec
				double clickPr = .45; //
				double salesPrice = 10;
				double ISRatio = .2;

				double numImps = (budget/bid)+ (budget/bid)*(1-clickPr);
				double CPC = bid;
				int numClicks = (int) (numImps*clickPr);
				double w = numClicks*convProb;	
				double v = numClicks*convProb*salesPrice - numClicks*CPC;

				Item newItem = new Item(q, day, w,v,bid,budget,targeting);
				//debug(clickPr+" "+CPC+" "+convProb+" "+newItem);
				//debug(numImps+" "+slotDistr+" "+isRatioArr+" "+ISRatio);
				debug("Item: "+newItem);
				itemPredictions.put(newItem, new Predictions(clickPr, CPC, convProb, numImps,slotDistr,isRatioArr,ISRatio));
				debug("Pred: "+itemPredictions.get(newItem));
				itemsList.add(newItem);

			}

			QueryAuction qa = new QueryAuction(q, 1);
			itemsLists.put(qa, itemsList);
		}


	}

	public void printCurrentKnapsack(HashMap<QueryAuction, ArrayList<Item>> knapsack){
		Set<QueryAuction> qas = knapsack.keySet();
		for(QueryAuction qa : qas){
			System.out.println(""+getCurrentItem(knapsack.get(qa)));
		}

	}
	public HashMap<QueryAuction, ArrayList<Item>> getItemsLists(){
		return itemsLists;
	}

	public HashMap<Item, Predictions> getItemPredictions(){
		return itemPredictions;
	}

	public static void main (String[] args){
		int[] seeds = {320};//, 450, 620, 320, 16784, 9382108, 28495, 209480, 384, 3958, 57094, 276, 2948, 333, 994, 59485, 92222};
		double totalV = 0;
		double totalVLoss = 0;
		double totalELoss = 0;

		for(int i = 0;i<seeds.length; i++){
			FastMDPMCKP testing = new FastMDPMCKP(2, 10000, null, "Schlemazl");
			testing.getBidBundle();

			Set<Query> querySet = testing.makeQuerySet(4);
			testing.makeItems(30, querySet, seeds[i]);
			//testing.getSolutionTest(testing.getItemsLists(), testing.getItemPredictions());

			double bestSolV = testing._bestValue;
			double bestSolW = testing._bestKappa;

			//testing = new FastMDPMCKP(null, "Schlemazl");
			//testing.getBidBundle();
			//querySet = testing.makeQuerySet(3);
			//testing.makeItems(30, querySet, seeds[i]);
			testing.getOptimal(testing.getItemsLists(), testing.getItemPredictions());
			double optSolV = testing._bestValue;
			double optSolW = testing._bestKappa;
			testing.printCurrentKnapsack(testing.itemsLists);
			testing.debug("sol: "+bestSolV);
			testing.debug("opt: "+optSolV);
			System.out.println("V Diff: "+ (optSolV-bestSolV)+" W Diff: "+(optSolW-bestSolW)+" Eff loss: "+((optSolV/optSolW)-(bestSolV/bestSolW)));

			totalV += optSolV;
			totalVLoss += (optSolV-bestSolV);
			totalELoss += ((optSolV/optSolW)-(bestSolV/bestSolW));

		}
		System.out.println("TV: "+totalV+" Vloss: "+totalVLoss+" ELoss: "+totalELoss);
	}

}

