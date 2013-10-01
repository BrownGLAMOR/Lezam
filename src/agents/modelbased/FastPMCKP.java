package agents.modelbased;

import static models.paramest.ConstantsAndFunctions._advertiserEffectBoundsAvg;
import static models.paramest.ConstantsAndFunctions.queryTypeToInt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import models.unitssold.BasicUnitsSoldModel;
import simulator.AgentSimulator;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import clojure.lang.PersistentHashMap;

import agents.AbstractAgent.Predictions;
import agents.modelbased.MCKP.KnapsackQueryCreator;
import agents.modelbased.MCKP.KnapsackQueryResult;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.UserClickModel;

/*
 * This class extend the PenalizedKnapsackOptimizer class which extends SchlemazlAgent
 * 
 * It should contain methods applicable to the FastPMCKP algorithm that 
 * do not apply to other PKP algorithms
 */
public class FastPMCKP extends PenalizedKnapsackOptimizer {

	private boolean DEBUG = true; //set false to not print

	//parameters to be set/tuned
	private double _iterMultiplier = 1.25;
	private double _threshMultiplier = 1.5;
	private double _kappaMultiplier = .5;
	private int _maxIters =15;
	private double _threashold = .5;
	private int _seed = 0; //if set to zero then no seed will be used, seed is for testing purposes

	private double _finalKappa = 0; 

	//these need to be created from info passed in to agent
	HashMap<Query, ArrayList<Item>> itemsLists;
	HashMap<Item, Predictions> itemPredictions;

	/*
	 * Constructor, to set Fast specific parameters (move above here, should call PKO)
	 */
	public FastPMCKP(PersistentHashMap perfectSim, String agentToReplace) {
		super(perfectSim, agentToReplace);
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
			double remainingCap = getRemainingCap();

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

			//Create simulator (to be used for knapsack creation)
			PersistentHashMap querySim = setupSimForDay();

			/*
			 * Create items by simulating each query (given some initial capacity, everyone's bids, and everyone's budgets)
			 * (bid,budget)->clickPr, CPC, convProb, numImps,slotDistr,isRatioArr,ISRatio
			 */

			itemsLists = new HashMap<Query, ArrayList<Item>>();
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
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems = makeQueryAuctionItems(itemsLists);
			HashMap<Query,Item> solution = getSolution(queryAuctionItems, itemPredictions);
			long solutionEndTime = System.currentTimeMillis();
			//debug("Seconds to solution: " + (solutionEndTime-solutionStartTime)/1000.0 );


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
			QueryAuction qa = new QueryAuction(q, 0);
			queryAuctionItems.put(qa, itemLists.get(q));
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
			HashMap<Query, ArrayList<Item>> itemsLists, HashMap<Item, Predictions> itemPredictions, PersistentHashMap querySim) {
		for(Query q : _querySpace){
			if(!q.equals(new Query())) { //Do not consider the (null, null) query. //FIXME: Don't hardcode the skipping of (null, null) query.

				int numItems = bidLists.get(q).size()*budgetLists.get(q).size();
				ArrayList<Item> itemList = new ArrayList<Item>(numItems);

				double salesPrice = _salesPrices.get(q);           

				//FIXME: Make configurable whether we allow for generic ads. Right now it's hardcoded that we're always targeting.
				for(int k = 1; k < 2; k++) { //For each possible targeting type (0=untargeted, 1=targetedToSpecialty)
					for(int i = 0; i < bidLists.get(q).size(); i++) { //For each possible bid
						for(int j = 0; j < budgetLists.get(q).size(); j++) { //For each possible budget

							Item newItem = makeNewItemandPredictions(k, i ,j, bidLists.get(q).get(i), budgetLists.get(q).get(j), 
									salesPrice, q, querySim, itemPredictions);
							itemList.add(newItem);
							if(itemPredictions.get(newItem).getCost() + newItem.b()*2 < newItem.budget()) {//HC num
								//If we don't hit our budget, we do not consider higher budgets, 
								//since we will have the same result so we break out of the budget loop
								break;
							}
						}
					}
				}
				Item zeroItem = new Item(q,0,0,0.0,0.0,false, true);
				itemList.add(zeroItem);
				
				itemPredictions.put(zeroItem, new Predictions());

				if(itemList.size() > 0) {
					itemsLists.put(q, itemList);
				}
			}
		}

	}


	/*
	 * Used in getBidBundle
	 */
	private Item makeNewItemandPredictions(int k, int i, int j, double bid, double budget, double salesPrice, 
			Query q, PersistentHashMap querySim, HashMap<Item, Predictions> itemPredictions){
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
		Item newItem = new Item(q,w,v,bid,budget,targeting);
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
		return remainingCap;
	}



	//helper method that passes in parameters set
	public double updateCapacity(QueryAuction thisQ, Item thisItem, HashMap<QueryAuction, ArrayList<Item>> qaItems, 
			HashMap<Item, Predictions> itemPredictions, double takenSoFar){
		return updateCapacity(thisQ, thisItem, qaItems, itemPredictions,
				_threashold, _maxIters, takenSoFar) ;
	}

	public double updateCapacity(QueryAuction thisQ, Item thisItem, HashMap<QueryAuction, ArrayList<Item>> qaItems, HashMap<Item, Predictions> itemPredictions,
			double threashold, int maxIters, double takenSoFar){ 

		double kappaLast = Double.MAX_VALUE;
		double kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, 0, thisQ, thisItem);
		debug("BaseKappa: "+kappa);
		double kappaBase = kappa;
		int numIters = 0;

		while(Math.abs(kappaLast-kappa) > threashold){
			numIters+=1;
			if(numIters > _maxIters){
				numIters = 0;
				kappa = resetKappa(kappaBase);
				threashold = increaseThreashold(threashold);
				maxIters = increaseMaxIters (maxIters);
			}

			kappaLast = kappa;
			kappa = getWeightOfKnapsack(qaItems, itemPredictions, takenSoFar, kappaLast, thisQ, thisItem);
			debug("Kappa: "+kappa);
		}

		return kappa;
	}


	private int increaseMaxIters(int maxIters) {

		return (int) Math.floor(maxIters*_iterMultiplier);
	}

	private double increaseThreashold(double threashold) {

		return threashold*_threshMultiplier;
	}

	private double resetKappa(double kappaBase) {

		Random rand = new Random(_seed);
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
			if(item!=null){
				weight+=itemPredictions.get(item).getNumImp()*itemPredictions.get(item).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(item).getISRatio());
			}
			}else{
				weight+=itemPredictions.get(thisItem).getNumImp()*itemPredictions.get(thisItem).getClickPr()*calculateConversionProb(qa, takenSoFar, kappaLast, itemPredictions.get(thisItem).getISRatio());
				
			}
		}
		return weight;
	}

	public double updatePenalty(QueryAuction thisQ, Item thisItem, 
			HashMap<QueryAuction, ArrayList<Item>> qaItems, HashMap<Item, Predictions> itemPredictions, double newKappa, double previousPenalty){
		double newPenalty = 0;
		Set<QueryAuction> queryAuctions = qaItems.keySet();
		for(QueryAuction qa : queryAuctions){
			//debug("QA: "+qa);
			Item oldItem = getCurrentItem(qaItems.get(qa));
			//debug("oldItem: "+oldItem.toString());

			if(qa == thisQ){
				Item qaItem = thisItem;
				//debug("qaItem: "+qaItem.toString());
				double conversionProb = calculateConversionProb(qa, (_capacity-getRemainingCap()), newKappa, itemPredictions.get(qaItem).getISRatio());
				double newValue = calcItemValue(qaItem, conversionProb, itemPredictions);
			}else{
				//debug("Inputs: ");
				//debug("qa "+ qa);
				//debug("newK " +newKappa);
				//debug("pred "+itemPredictions.get(oldItem));
				//debug("IS "+itemPredictions.get(oldItem).getISRatio());
				double conversionProb = calculateConversionProb(qa,(_capacity-getRemainingCap()), newKappa, itemPredictions.get(oldItem).getISRatio());
				double newValue = calcItemValue(oldItem, conversionProb, itemPredictions);
				newPenalty = newPenalty+Math.abs(oldItem.v()-newValue);
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
		debug("Val: "+marginalVal+" Weight: "+marginalWeight);
		if(marginalWeight!=0){
			return marginalVal/marginalWeight;
		}else{
			return marginalVal;
		}
	}


	public HashMap<Query, Item> getSolution(HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems, 
			HashMap<Item, Predictions> itemPredictions){
		debug("___________________________________________________GETTING NEW SOLUTION__________________________________________");
		//Initialize variables	
		double kappa = 0; 
		double newKappa = 0;
		double kappaBest = 0;

		Item itemBest = null;
		QueryAuction queryAuctionBest = null;

		double rho =0;
		double rhoBest = 0; 
		double newRho = 0;

		double marginalVal = 0;
		double marginalWeight = 0;
		double eNew = 0;
		double eBest = Double.MIN_VALUE;
		
		Set<QueryAuction> auctions = queryAuctionItems.keySet();
		while(eBest > 0){
			eBest = 0;
			eNew = 0;
			for (QueryAuction q : auctions){
				Item currentItem = getCurrentItem(queryAuctionItems.get(q));
				for(Item j : queryAuctionItems.get(q)){
					if(!j.hasBeenTaken()){
						
						newKappa = updateCapacity(q, j,queryAuctionItems, itemPredictions, _capacity-getRemainingCap());
						debug("newKappa = "+newKappa+" used: "+ (_capacity-getRemainingCap()));
						newRho = updatePenalty(q, j , queryAuctionItems, itemPredictions, newKappa, rho);
						marginalWeight = newKappa - kappa;

						//(oldItem's value- newItem's value)-(oldPenalty-newPenalty)
						debug("Val Calc: "+currentItem.v()+" "+ j.v()+" "+ rho+" "+newRho);
						marginalVal= (j.v()-currentItem.v() )-(newRho-rho);
						eNew = calculateEfficiency(marginalVal, marginalWeight);
						debug("eNew: "+eNew);
						if(eNew>eBest){
							eBest = eNew;
							queryAuctionBest = q;
							//debug("QA RESET");
							itemBest = j;
							kappaBest = newKappa;
							rhoBest = newRho;
							
						}
					}
				}
			}
			if(eBest>0){
				debug("WHILE: "+eBest);
				kappa = kappaBest;
				rho = rhoBest;
				resetSolution(queryAuctionItems, queryAuctionBest, itemBest);
			
			}
		}
		_finalKappa = kappa;
		return makeBidBundle(queryAuctionItems);

	}

	private HashMap<Query, Item> makeBidBundle(
			HashMap<QueryAuction, ArrayList<Item>> queryAuctionItems) {
		HashMap<Query, Item> solution = new HashMap<Query,Item>();
		Set<QueryAuction> queryAuctions = queryAuctionItems.keySet();
		for(QueryAuction qa : queryAuctions){
			Item item = getCurrentItem(queryAuctionItems.get(qa));
			solution.put(qa.getQuery(),  item);
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
				j.setHasBeenTaken();
			}
		}
		itemBest.setCurrentlyTaken(true);
		itemBest.setHasBeenTaken();

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

}
