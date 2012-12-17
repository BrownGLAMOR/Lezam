/**
  /*
    * Start of bare-bones hill climbing solution.
    * Contains the following methods:
    * MultiDayOptimizer
    * FastGreedyMCKP
    * RecalculatePenalty
    * CreateItems
    * ItemPrep
    * NextIncrementalItem 
    * MakeRegularItems
    * SolutionProfit
    * In case of questions, contact Sebastian Sigmon at dweomer.bozwevial@gmail.com
 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import clojure.lang.PersistentHashMap;

import models.unitssold.BasicUnitsSoldModel;

import agents.AbstractAgent;
import agents.modelbased.MCKP.MultiDay;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;
import edu.umich.eecs.tac.props.Query;

/**
 * @author betsy
 *
 */

	
public class MultiDayOptimizer extends MCKP {

	/**
	 * 
	 */
	int _numTake = 1;
	boolean amyHack = false;
	boolean amyHack2 = false;
	boolean changeWandV = false;
	boolean changeBudget = false;
	int accountForProbing = 0;
	
	public MultiDayOptimizer(){
		super();
	}
	
	public MultiDayOptimizer(int numTake, boolean reCalc, boolean reCalcWithExtra, 
			boolean changeWandV, boolean changeBudget, int accountForProbing) {
		super();
		_numTake = numTake;
		amyHack = reCalc;
		amyHack2 = reCalcWithExtra;
		this.changeWandV = changeWandV;
		this.changeBudget = changeBudget;
		this.accountForProbing = accountForProbing;
		
	}
	
	public MultiDayOptimizer(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization,
			int numTake, boolean reCalc, boolean reCalcWithExtra, boolean changeWandV, boolean changeBudget, int accountForProbing) {
		super(c1, c2,c3, budgetL, budgetM, budgetH,  bidMultLow, bidMultHigh, multiDay, multiDayDiscretization);
		_numTake = numTake;
		amyHack = reCalc;
		amyHack2 = reCalcWithExtra;
		this.changeWandV = changeWandV;
		this.changeBudget = changeBudget;
		this.accountForProbing = accountForProbing;
	}

	public MultiDayOptimizer(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,	int multiDayDiscretization,
			int numTake, boolean reCalc, boolean reCalcWithExtra, boolean changeWandV, boolean changeBudget) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		_numTake = numTake;
		amyHack = reCalc;
		amyHack2 = reCalcWithExtra;
		this.changeWandV = changeWandV;
		this.changeBudget = changeBudget;
	}
	
	public MultiDayOptimizer(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL, budgetM, budgetH,  bidMultLow, bidMultHigh, multiDay, multiDayDiscretization);
	}

	public MultiDayOptimizer(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay, int multiDayDiscretization) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
	}

	/* (non-Javadoc)
	 * @see agents.modelbased.AbstractSolver#getSolution()
	 */
	@Override
	public HashMap<Query,Item> getSolution (ArrayList<IncItem> incItems, double budget, Map<Query, 
			ArrayList<Predictions>> allPredictionsMap, HashMap<Query, ArrayList<Double>> bidLists,
			HashMap<Query, ArrayList<Double>> budgetLists){
		//This method is what getBidBundle calls to return a solution.

		//First, create a capacity array containing target capacities for future days.
		//This array will be of size (number of days remaining+4), with the first 4 days occupied by the sales made on the past 4 days.
		int[] capacityArray = new int[_numDays-(int)_day+4];	   

		//The target capacity, to start, is the total capacity divided by the length of the capacity window. We fill the array with this.
		int targetCap = (int) ((_capacity*_capMod.get(_capacity))/((double) _capWindow));
		Arrays.fill(capacityArray, targetCap);

		//When we hill-climb, our increment will be the one chosen when setting up MCKP.
		int increment = _multiDayDiscretization;

		//For up to four days prior to the current day (fewer if we are on day 0-3), fill the array instead with the actual capacities we used.
		//This portion of code is borrowed from Jordan's code for the purposes of handling both model cases, with modifications made to account for different data structures.	   
		if(!hasPerfectModels()){
			ArrayList<Integer> soldArray = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			   Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			   soldArray.add(expectedConvsYesterday);
	
			for(int i=0; i<_capWindow-1; i++){
				if(soldArray.size()-(_capWindow-1+i) >= 0){
					capacityArray[i] = soldArray.get(soldArray.size()-(_capWindow-1)+i);
				} else {
					capacityArray[i] = targetCap;
				}
			}
		} else {
			for(int i = 0; i < (_capWindow-1); i++) {
				if(_perfectStartSales.length-1-i >= 0){
					capacityArray[_perfectStartSales.length-1-i] = _perfectStartSales[_perfectStartSales.length-1-i];
				} else {
					capacityArray[i] = targetCap;	
				}			   
			}
		}

		int currentDayIndex = 4;
		HashMap<Integer,HashMap<Integer,Double>> profitMemoizeMap = new HashMap<Integer,HashMap<Integer,Double>>();

		//So long as maximum profit increases, we do the following.
		//Each day, take the capacity and adjust it up one and down one, testing each to see which, if any, produces a profit gain.
		//If one of these changes produces a greater profit, and it is the most profitable change so far, note down the day and increment applied.
		//Once we reach the end of the loop, if we have a change that produces profit, apply it to the capacity targets and continue.

		//		   double changedProfit, profitTally;
		//		   double bestProfit = 0;
		//		   bestProfit = SolutionProfit(profitMemoizeMap, allPredictionsMap, bidLists, budgetLists, capacityArray);
		//		   changedProfit = bestProfit;
		//		   do {
		//			   bestProfit = changedProfit;
		//			   profitTally = 0;
		//			   int dayIndex = -1;
		//			   int dayIncrement = 0;
		//			   for (int i = currentDayIndex; i<capacityArray.length-1; i++){
		//				   for (int j = -1; j<2; j+=2){
		//					   if (j*increment+capacityArray[i]>=0){
		//						   capacityArray[i] = capacityArray[i] + j*increment;
		//						   profitTally = SolutionProfit(profitMemoizeMap, allPredictionsMap, bidLists, budgetLists, capacityArray);
		//						   capacityArray[i] = capacityArray[i] - j*increment;
		//						   if (profitTally > changedProfit){
		//							   changedProfit = profitTally;
		//							   dayIndex = i;
		//							   dayIncrement = j*increment;					   
		//						   }
		//						   profitTally = 0;
		//					   }
		//				   }
		//			   }
		//			   if (changedProfit > bestProfit){
		//				   capacityArray[dayIndex] = capacityArray[dayIndex] + dayIncrement;
		//				   System.out.print(dayIndex+","+dayIncrement+" ");
		//			   }
		//		   } while(changedProfit > bestProfit);   

		//Currently, my hill-climbing code seems to be bugged. As such, I'm wrapping SolutionProfit in Jordan's hill-climbing code for the time being, while leaving
		//my implementation commented out above.	 
		//Further testing indicates that maybe it's not so bugged? Jordan's hill-climbing code is now commented out.

		double currProfit;
		double bestProfit = SolutionProfit(profitMemoizeMap, allPredictionsMap, bidLists, budgetLists, capacityArray);
		do {
			currProfit = bestProfit;
			int bestIdx = -1;
			int bestIncrement = 0;
			for(int i = 4; i < capacityArray.length; i++) {
				for(int j = 0; j < 2; j++) {
					if(!(j == 1 && capacityArray[i] < increment)) { //capacity cannot be negative
						int capIncrement = increment * (j == 0 ? 1 : -1);//HC num
						capacityArray[i] += capIncrement;

						double profit = SolutionProfit(profitMemoizeMap, allPredictionsMap, bidLists, budgetLists, capacityArray);
						if(profit > bestProfit) {
							bestProfit = profit;
							bestIdx = i;
							bestIncrement = capIncrement;
						}

						capacityArray[i] -= capIncrement;
					}
				}
			}
			if(bestIdx > -1) {
				capacityArray[bestIdx] += bestIncrement;
			}
		}
		while(bestProfit > currProfit);	   

		//		   System.out.println("Our array:");//debugging statements
		//		   for (int i = 0; i<capacityArray.length; i++){
		//			   System.out.print(capacityArray[i]+" ");
		//		   }
		//		   System.out.println();

		//		   int remCap = (int) (_capacity*_capMod.get(_capacity));//debugging statements
		//		   for (int i = 0; i<4; i++){
		//			   remCap -= capacityArray[i];
		//		   }
		//		   System.out.println("Our remaining capacity: "+remCap);

		//		   System.out.println(profitMemoizeMap.toString());//debugging statements

		//		   System.out.println("Final capacity for current day: "+ capacityArray[currentDayIndex]);//debugging statements

		//For instances of testing Jordan's hack of subtracting 10 off for probe bids. Comment it out otherwise.
		capacityArray[currentDayIndex] -= accountForProbing;
		
		

		//At this point, hill-climbing has theoretically produced the best set of capacity targets. Our solution for today is obtained from FGMCKP.
		HashMap<Query,Item> solution = FastGreedyMCKP(allPredictionsMap, bidLists, budgetLists, capacityArray, currentDayIndex);
		return solution;	   
	}

	private HashMap<Query,Item> FastGreedyMCKP (Map<Query,ArrayList<Predictions>> allPredictionsMap, 
			HashMap<Query,ArrayList<Double>> bidLists, HashMap<Query,ArrayList<Double>> budgetLists, int[] capacityArray, int targetDay) {
		//This method takes a set of bids and budgets, as well as a capacity used/targeted array and a specific day, and returns the greedy MCKP
		//solution for the targeted day. 

		//First, we set up a priority queue into which we will put the incremental items between which we are choosing.
		PriorityQueue<IncItem> selectionQueue = new PriorityQueue<IncItem>();

		//Since we are no longer using MakeRegularItems, we no longer need to perform this step.
		//		   HashMap<Query,ArrayList<IncItem>> incItemsTaken = new HashMap<Query,ArrayList<IncItem>>();

		//We create a set of regular undominated items, weight-sorted, from the bids and budgets given to us.
		HashMap<Query,ArrayList<Item>> itemSet = CreateItems(allPredictionsMap, bidLists, budgetLists, capacityArray, targetDay);

		//We create a hashmap to store the index of the desired item for each query, initialized to 1 (since we are putting the zeroth item into the queue automatically).
		HashMap<Query,Integer> lastUsedIndices= new HashMap<Query,Integer>();
		for(Query q: _querySpace){
			if (q != new Query() && itemSet.get(q) != null){
				lastUsedIndices.put(q, 1);
				selectionQueue.add(NextIncrementalItem(itemSet.get(q),0));
				//As before, we do not need to perform this step if we are not using MakeRegularItems.
				//				   incItemsTaken.put(q, new ArrayList<IncItem>());
			}
		}

		//Variables to keep track of our target capacity and how much our solution has used.
		double capacityUsed = 0;
		double capacityTarget = capacityArray[targetDay];

		//Next, we cycle through a process where we select each item in turn from the queue. If it pushes our weight over capacity, we do not take it (and also stop taking
		//items). Otherwise, we add it to the set of incremental items taken so far and add a new item from that query to the queue.
		//This behavior is modified somewhat by a series of "hacks," detailed further below.
		IncItem bestItem;
		HashMap<Query,Item> solution = new HashMap<Query,Item>();
		int numToTake = _numTake;
		do{		   
			//System.out.println("Queue size before: "+selectionQueue.size());//Debugging statements
			//System.out.println(selectionQueue.toString());
			System.out.println("start loop");
			//Retrieve the best item from the selection queue. If there are no more items, we can break, as we are done.
			bestItem = selectionQueue.poll();
			if (bestItem == null){
				System.out.println("breaking: "+numToTake);
				break;
			}		   

			//System.out.println(bestItem);//Debugging statement

			Query q = bestItem.item().q();
			int itemIndex = lastUsedIndices.get(q);

			//		   System.out.println("Item index: "+itemIndex);//Debugging statements
			//		   System.out.println("Query: "+q);
			//		   System.out.println("Query's set size: "+itemSet.get(q).size());
			//		   System.out.println("Best item's efficiency: "+bestItem.eff());
			//		   System.out.println("Capacity used: "+capacityUsed);
			//		   System.out.println("Capacity target: "+capacityTarget);


			//In the static case, we don't need to consider that efficiency will ever be negative or zero. For dynamic purposes, however, the check is left as a reminder. 
			//		   if (bestItem.eff() <= 0){
			//			   break;
			//		   }

			//If we have enough space in our knapsack for the item, take it.
			if (capacityTarget-capacityUsed>=bestItem.w()){
				capacityUsed += bestItem.w();
				solution.put(q, bestItem.itemHigh());

				//As we're not using MakeRegularItems, this step is moot.
				//incItemsTaken.get(q).add(bestItem);			   

				//If there is another incremental item from this query to consier, place it into the queue and update the indices.
				if (itemIndex<itemSet.get(q).size()){
					selectionQueue.add(NextIncrementalItem(itemSet.get(q),itemIndex));				   
					lastUsedIndices.put(q, itemIndex+1);

					//System.out.println("Queue size after: "+selectionQueue.size());//Debugging statement
				}
			} else {
				//System.out.println("In heuristics");
				//There are two different heuristics which we have the option of using here: Jordan's hack, and Amy's hack, contained between two indicators here.
				//Uncomment everything between whichever one you're using, except of course for the actual comments.

				//=========================================================
				//Jordan's hack
				//=========================================================
				//Jordan's hack involves taking the item which doesn't quite fit, but adjusting the weight and value to his own specifications
				//(perhaps as a way of implicitly taking into account the conversion penalty incurred from going over budget).
				if(!amyHack){
					stuffKnapsackWithoutRecalc(capacityTarget-capacityUsed, bestItem, solution);
					break;
				}else if(!amyHack2){
//					//=========================================================
//					//Amy's hack
//					//=========================================================
//					//Amy's hack involves recalculating the weights and values of both the item being added and the items already taken to account for the solution's new weight.
//					//Unfortunately, this implementation of it is either flawed or not producing the desired results. My money is on flawed, personally, since this was written
//					//in something of a rush and I haven't had time to test it properly.
//
//					
					stuffKnapsackWithRecalcNoExtra(capacityUsed, capacityTarget, targetDay, capacityArray, itemSet,
							itemIndex,  selectionQueue, lastUsedIndices, allPredictionsMap, bestItem, solution);
					break;	

					//=========================================================
					//End of Amy's hack
					//=========================================================
				}else{
//					//=========================================================
//					//Amy's hack 2.0
//					//=========================================================
//					//This is a variant on Amy's hack, above, which keeps taking additional items until doing so would put us over budget, at which point we take
//					//one more and call it quits.

					stuffKnapsackWithRecalc(capacityUsed, capacityTarget, targetDay, capacityArray, itemSet,
							itemIndex,  selectionQueue, lastUsedIndices, allPredictionsMap, bestItem, solution, numToTake);
				}
			}
		} while(capacityUsed<capacityTarget || numToTake>0);
		System.out.println("Out while "+numToTake);
		numToTake = _numTake;
		//If we were using MakeRegularItems, we would use this to make our solution. But we're not, so we don't.
		//		   HashMap<Query,Item> solution = MakeRegularItems(incItemsTaken);	   

		return solution;
	}
	
	private HashMap<Query, Item> stuffKnapsackWithoutRecalc(double budget, IncItem bestItem, HashMap<Query, Item> soltn){
		Item itemHigh = bestItem.itemHigh();
		double incW = bestItem.w();
		double weightHigh = budget / incW;
		double weightLow = 1.0 - weightHigh;
		double lowVal = ((bestItem.itemLow() == null) ? 0.0 : bestItem.itemLow().v());
		double lowW = ((bestItem.itemLow() == null) ? 0.0 : bestItem.itemLow().w());
		
		if(changeWandV){
			double newValue = itemHigh.v()*weightHigh + lowVal*weightLow;
			if(changeBudget){
				double newBudget = itemHigh.budget()*weightHigh; //for changed budget
				soltn.put(bestItem.item().q(), new Item(bestItem.item().q(),budget+lowW,newValue,itemHigh.b(),newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx()));
			}else{
				soltn.put(bestItem.item().q(), new Item(bestItem.item().q(), budget+lowW, newValue, itemHigh.b(),itemHigh.budget(),itemHigh.targ(),itemHigh.isID(),itemHigh.idx()));
			}
		}else{
			if(changeBudget){
				double newBudget = itemHigh.budget()*weightHigh; //for changed budget
				soltn.put(bestItem.item().q(), new Item(bestItem.item().q(), itemHigh.w(),itemHigh.v(),itemHigh.b(),newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx()));

			}else{
				soltn.put(bestItem.item().q(), new Item(bestItem.item().q(), itemHigh.w(),itemHigh.v(),itemHigh.b(),itemHigh.budget(),itemHigh.targ(),itemHigh.isID(),itemHigh.idx()));

			}
		}
		return soltn;
	}

	private HashMap<Query, Item> stuffKnapsackWithRecalcNoExtra(double capacityUsed, double capacityTarget, int targetDay, 
			int[] capacityArray, HashMap<Query, ArrayList<Item>> itemSet, int itemIndex, PriorityQueue<IncItem> selectionQueue, 
			HashMap<Query, Integer> lastUsedIndices, Map<Query, ArrayList<Predictions>>allPredictionsMap, IncItem bestItem,
			HashMap<Query, Item> soltn){
		int capRemaining = (int)(_capacity*_capMod.get(_capacity));
		for (int i = targetDay-1; i>targetDay-5; i--){
			capRemaining -= capacityArray[i];
		}
		if (capRemaining <= 0){	
			//Recalculate the w/v of the item.
			capacityUsed += bestItem.w();
			Item solnItem = RecalculatePenalty(bestItem.itemHigh(), allPredictionsMap, (int) capacityUsed, capRemaining);
			//Recalculate the w/v of all items in the solution.
			double newCapacityUsed = 0;
			for (Query qu : _querySpace){
				if (soltn.get(qu) != null){
					soltn.put(qu, RecalculatePenalty(soltn.get(qu), allPredictionsMap, (int) capacityUsed, capRemaining));
					newCapacityUsed += soltn.get(qu).w();
				}
			}
			if (soltn.get(solnItem.q()) != null){
				newCapacityUsed -= soltn.get(solnItem.q()).w();					   
			}
			newCapacityUsed += solnItem.w();
			//If it still fits, take it
			if (newCapacityUsed<=capacityTarget){
				soltn.put(bestItem.item().q(), solnItem);
				capacityUsed = newCapacityUsed;
				if (itemIndex<itemSet.get(bestItem.item().q()).size()){
					selectionQueue.add(NextIncrementalItem(itemSet.get(bestItem.item().q()),itemIndex));
					lastUsedIndices.put(bestItem.item().q(), itemIndex+1);
				}
			}
		}
		
		return soltn;
	}
	
	private HashMap<Query, Item> stuffKnapsackWithRecalc(double capacityUsed, double capacityTarget, int targetDay, 
			int[] capacityArray, HashMap<Query, ArrayList<Item>> itemSet, int itemIndex, PriorityQueue<IncItem> selectionQueue, 
			HashMap<Query, Integer> lastUsedIndices, Map<Query, ArrayList<Predictions>>allPredictionsMap, IncItem bestItem,
			HashMap<Query, Item> soltn, int numToTake){
		int capRemaining = (int)(_capacity*_capMod.get(_capacity));
		for (int i = targetDay-1; i>targetDay-5; i--){
			capRemaining -= capacityArray[i];
		}
		System.out.println("cap rem: "+capRemaining+" num: "+numToTake);
		if (capRemaining <= 0 || numToTake>0){	
			System.out.println("In cap num if. num: "+numToTake);
			//Recalculate the w/v of the item.
			capacityUsed += bestItem.w();
			Item solnItem = RecalculatePenalty(bestItem.itemHigh(), allPredictionsMap, (int) capacityUsed, capRemaining);
			//Recalculate the w/v of all items in the solution.
			double newCapacityUsed = 0;
			for (Query qu : _querySpace){
				if (soltn.get(qu) != null){
					soltn.put(qu, RecalculatePenalty(soltn.get(qu), allPredictionsMap, (int) capacityUsed, capRemaining));
					newCapacityUsed += soltn.get(qu).w();
				}
			}
			if (soltn.get(solnItem.q()) != null){
				newCapacityUsed -= soltn.get(solnItem.q()).w();					   
			}
			newCapacityUsed += solnItem.w();
			//If it still fits, take it
			System.out.println("before 2nd cap num if. num: "+numToTake);
			if (newCapacityUsed<=capacityTarget || numToTake>1){
				System.out.println("In 2nd cap num if. num: "+numToTake);
				soltn.put(bestItem.item().q(), solnItem);
				if(newCapacityUsed>capacityTarget){
					numToTake -=1;
					//System.out.println("TOOK EXTRA: "+numToTake+" ___________________________________________________");
				}
				capacityUsed = newCapacityUsed;
				if (itemIndex<itemSet.get(bestItem.item().q()).size()){
					selectionQueue.add(NextIncrementalItem(itemSet.get(bestItem.item().q()),itemIndex));
					lastUsedIndices.put(bestItem.item().q(), itemIndex+1);
				}

			} else {
				System.out.println("before 2nd cap num if. num: "+numToTake);
				//Else take it anyway, just so we go over. We then break.
				//These next lines recalc the budget, don't think this is helping the agent at all
				//double newBudget = (capacityTarget-(newCapacityUsed - solnItem.w()))*solnItem.budget();
				//solnItem._budget=newBudget;
				soltn.put(bestItem.item().q(), solnItem);
				numToTake-=1;
				//System.out.println("__TOOK EXTRA: "+numToTake+" ____________________________________________");
				return soltn;
			}
			System.out.println("here");
		}
	
		return soltn;
	}

	private Item RecalculatePenalty (Item initialItem, Map<Query,ArrayList<Predictions>> allPredictionsMap, int capacityUsed, int remainingCap){
		//This method is used only when FastGreedyMCKP uses Amy's hack.
		//It recalculates the weight and value of an item based on a new solution weight.

		//Get the new penalty and item predictions for the given item.
		double newPenalty = getPenalty(remainingCap, capacityUsed);
		Predictions itemPredictions = allPredictionsMap.get(initialItem.q()).get(initialItem.idx());

		//Recalculate all the relevant values.
		double salesPrice = _salesPrices.get(initialItem.q());
		double clickPr = itemPredictions.getClickPr();
		double CPC = itemPredictions.getCPC();
		double numImps = itemPredictions.getNumImp();
		double convProb = getConversionPrWithPenalty(initialItem.q(), newPenalty, itemPredictions.getISRatio());
		int numClicks = (int) (numImps*clickPr);
		double cost = CPC*numClicks;
		double w = numClicks*convProb;
		double v = w*salesPrice - cost;

		//Create the new version of the item.
		Item finalItem = new Item(initialItem.q(), w, v, initialItem.b(), initialItem.budget(), initialItem.targ(), initialItem.isID(), initialItem.idx());

		//Return this version of the item.
		return finalItem;	   
	   }
	   
	   private HashMap<Query,ArrayList<Item>> CreateItems (Map<Query,ArrayList<Predictions>> allPredictionsMap, HashMap<Query,ArrayList<Double>> bidLists,
			   HashMap<Query,ArrayList<Double>> budgetLists, int[] capacityArray, int targetDay){
		   //This method takes bid and budget lists as well as a capacity used/targeted array and the current day. It returns the set of all items for those
		   //lists.      	   
		   
		   //Set up a set into which we put our items.
		   //Get the penalty to apply to the items.	   
		   int remainingCap = _capacity;
		   for (int i = targetDay-1; i>targetDay-5; i--){
			   remainingCap -= capacityArray[i];
		   }	   
		   HashMap<Query,ArrayList<Item>> rawItemSet = new HashMap<Query,ArrayList<Item>>();
		   double penalty = getPenalty(remainingCap, capacityArray[targetDay]);
		   
		   //Create items for each query (except the null query). 
		   for (Query q : _querySpace){	   
			   if (q != new Query()){
				   //Set up the set of items specific to this query and retrieve predictions as necessary
				   ArrayList<Item> queryItemSet = new ArrayList<Item>();
				   int itemCount = 0;
				   ArrayList<Predictions> queryPredictions = allPredictionsMap.get(q);
				   //Don't consider untargeted ads right now.
				   for (int i = 1; i<2; i++){
					   //For each bid:
					   for (int j = 0; j < bidLists.get(q).size(); j++){
						   //For each budget:
						   for (int k = 0; k < budgetLists.get(q).size(); k++){
							   //Retrieve the information necessary to create a new item from the lists and predictions.
							   boolean targeting = (i == 1);
							   double bid = bidLists.get(q).get(j); 
							   double budget = budgetLists.get(q).get(k);
							   Predictions itemPredictions = queryPredictions.get(itemCount);
							   double salesPrice = _salesPrices.get(q);
							   double clickPr = itemPredictions.getClickPr();
							   double CPC = itemPredictions.getCPC();
							   double numImps = itemPredictions.getNumImp();
							   double convProb = getConversionPrWithPenalty(q, penalty, itemPredictions.getISRatio());

							   int numClicks = (int) (numImps*clickPr);
							   double cost = CPC*numClicks;
							   double w = numClicks*convProb;
							   double v = w*salesPrice - cost;

							   //Create the new item and increment the item count.
							   queryItemSet.add(new Item(q, w, v, bid, budget, targeting, 0, itemCount));
							   itemCount++;

							   //This little bit of code keeps the item predictions in sync with those made in getBidBundle. Precisely why Jordan chose to 
							   //break it off there is beyond me.
							   if (cost+bid*2 < budget){
								   break;
							   }
						   }
					   }
				   }
				   //Sort the items for the given query by weight.
				   Collections.sort(queryItemSet, new ItemComparatorByWeight());			   
				   
				   //Process the item set by running it through ItemPrep to remove the dominated items and LP-dominated items if it has at least one item.			   
				   if(queryItemSet.size()>0){
//					   System.out.println(queryItemSet);//Debugging statement
					   queryItemSet = ItemPrep(queryItemSet);
				   }
				   
				   //If the resulting item set has at least one item, store it in our master item set.
				   if(queryItemSet.size()>0){
//					   System.out.println(queryItemSet);//Debugging statement
					   rawItemSet.put(q, queryItemSet);
				   }
			   }
		   }
		   //Finally, return the master set.
		   return rawItemSet;	   
	   }
	   
	   public ArrayList<Item> ItemPrep (ArrayList<Item> itemSet){
		   //This method takes a set of weight-sorted items and returns that same set, minus any dominated and LP-dominated items and still weight-sorted.
		   
		   //The first step is to strip the dominated items from the set. Since they are weight-sorted, any item following the first cannot have a lower weight.
		   //We thus only add it if it has a higher value.
		   ArrayList<Item> tempSet = new ArrayList<Item>();
		   tempSet.add(itemSet.get(0));
		   for (int i = 1; i<itemSet.size(); i++){
			   if (itemSet.get(i).v()>tempSet.get(tempSet.size()-1).v()){
				   tempSet.add(itemSet.get(i));
			   }
		   }
		   
		   //The many items in the set must now be checked for duplicates, or items of the same weight and value.
		   //We seek these duplicates out, strip them, and replace them with a single item whose bid is the median of all the duplicates' bids.
		   ArrayList<Item> nonDuplicateSet = new ArrayList<Item>();
		   
		   //This code originally came from Jordan's own implementation. It's being replaced by the version below, which should in theory do the same thing except faster.
		   
//		   nonDuplicateSet.addAll(tempSet);
//		   
//		   for (int j = 0; j<tempSet.size(); j++){
//			   ArrayList<Item> duplicateItems = new ArrayList<Item>();
//			   Item currentItem = tempSet.get(j);
//			   duplicateItems.add(currentItem);
//			   
//			   for (int k = j+1; k<tempSet.size(); k++){
//				   
//				   if ((tempSet.get(k).w()==currentItem.w()) && (tempSet.get(k).v()==currentItem.v())){
//					   duplicateItems.add(tempSet.get(k));				   
//				   }			   
//			   }
//			   if(duplicateItems.size()>1){
//				   nonDuplicateSet.removeAll(duplicateItems);
//				   double minBid = 10;
//				   double maxBid = -10;
//				   for (int l = 0; l<duplicateItems.size(); l++){
//					   if (minBid>duplicateItems.get(l).b()){
//						   minBid = duplicateItems.get(l).b();					   
//					   }
//					   if (maxBid<duplicateItems.get(l).b()){
//						   maxBid = duplicateItems.get(l).b();
//					   }
//				   }
//				   nonDuplicateSet.add(new Item(currentItem.q(), currentItem.w(), currentItem.v(), ((minBid+maxBid)/2), currentItem.budget(), currentItem.targ(), 0, currentItem.idx()));			   
//			   }
//		   }
		   
		   
		   ArrayList<Item> duplicateSet = new ArrayList<Item>();
		   for(int i = 0; i < tempSet.size(); i++){
			   double currWeight = tempSet.get(i).w();
			   boolean duplicates = false;
			   double bidLow = 10;
			   double bidHigh = -10;
			   //If the current item is a duplicate, skip over it.
			   if (duplicateSet.contains(tempSet.get(i))){
				   continue;
			   }		   
			   //Else, check to make sure that there are no duplicates of this weight/value pairing.
			   for (int j = i+1; j<tempSet.size(); j++){
				   if (tempSet.get(j).w()>currWeight){
					   //This item isn't a duplicate, so ignore it.
					   break;
				   } else {
					   //There's at least one duplicate item.
					   duplicates = true;
					   
					   //Keep track of the lowest and highest bids among the duplicate.
					   if (bidLow > tempSet.get(j).b()){
						   bidLow = tempSet.get(j).b();
					   }
					   if (bidHigh < tempSet.get(j).b()){
						   bidHigh = tempSet.get(j).b();
					   }
					   //Add this duplicate to the set.
					   duplicateSet.add(tempSet.get(j));
				   }			   
			   }
			   
			   //If we have duplicates, add a new item whose bid is the median point between the high bids and low bids of the duplicates.
			   if (duplicates){
				   nonDuplicateSet.add(new Item(tempSet.get(i).q(), tempSet.get(i).w(), tempSet.get(i).v(), ((bidLow+bidHigh)/2), tempSet.get(i).budget(), tempSet.get(i).targ(), 0, tempSet.get(i).idx()));				   
			   } else {
				   //Otherwise, keep the item since it's not a duplicate.
				   nonDuplicateSet.add(tempSet.get(i));
			   }
		   }	   
		   
		   //Dominated items are removed; now to remove LP-dominated items.
		   //With the new code, this sort shouldn't be necessary.	   
//		   Collections.sort(nonDuplicateSet, new ItemComparatorByWeight());
		   
		   ArrayList<Item> finalSet = new ArrayList<Item>();
		   //we add a dummy item to assist in removing LP-dominated items.
		   finalSet.add(new Item(new Query(), 0, 0, -1, false, 1, 0));
		   int lastAddedIndex = 0;
		   for (int i = 0; i<nonDuplicateSet.size(); i++){
			   Item currItem = nonDuplicateSet.get(i);
			   lastAddedIndex += 1;
			   finalSet.add(currItem);
			   //The process of stripping duplicates can create dominated items again. We remove these as we progress.
			   if (finalSet.get(lastAddedIndex).w()==finalSet.get(lastAddedIndex-1).w()){
				   if (finalSet.get(lastAddedIndex).v()>finalSet.get(lastAddedIndex-1).v()){
					   finalSet.remove(lastAddedIndex-1);				   
				   } else {
					   finalSet.remove(lastAddedIndex);
				   }
				   lastAddedIndex -= 1;
			   }
			   //As long as there are three items or more, we check to see if the second of the last three is LP-dominated by the first and the third.		   
			   while (lastAddedIndex>=2 && 
					   (((finalSet.get(lastAddedIndex).v()-finalSet.get(lastAddedIndex-1).v())/(finalSet.get(lastAddedIndex).w()-finalSet.get(lastAddedIndex-1).w()))
							   >=((finalSet.get(lastAddedIndex-1).v()-finalSet.get(lastAddedIndex-2).v())/(finalSet.get(lastAddedIndex-1).w()-finalSet.get(lastAddedIndex-2).w())))){
				   finalSet.remove(lastAddedIndex-1);
				   lastAddedIndex -= 1;
			   }		   
		   }
		   //Remove the dummy item at the very end.
		   if(finalSet.get(0).w()==0 && finalSet.get(0).v()==0){
			   finalSet.remove(0);
		   }
		   //Return the final set.
		   return finalSet;
	   }
	   
	   public static IncItem NextIncrementalItem (ArrayList<Item> itemSet, int desiredItem){
		   //This method takes a set of items and the index of the desired incremental item. It returns the desired incremental item from that set.
		   //Note: When desiredItem is equal to 0, the incremental item returned is simply the first item from the set in incremental form (with no itemLow).
		   //FastGreedyMCKP calls this with desiredItem at 0 in order to obtain the first incremental item from each set, then calls for the next item in turn as it
		   //uses them up. 
		   IncItem resultingItem;
		   if (desiredItem == 0){
			   resultingItem = new IncItem(itemSet.get(0).w(), itemSet.get(0).v(), itemSet.get(0), null);
		   } else {
			   Item lastUsedItem = itemSet.get(desiredItem-1);
			   Item currItem = itemSet.get(desiredItem);
			   resultingItem = new IncItem(currItem.w()-lastUsedItem.w(), currItem.v()-lastUsedItem.v(), currItem, lastUsedItem);
		   }	   
		   return resultingItem;
	   }
	   
	   //Currently MRI is unused, because it has been replaced by the methodology implemented in FastGreedyMCKP. It remains here in case that doesn't work out for whatever
	   //reason, but until that point it should be considered legacy code and ignored. Seriously, feel free to comment the entire thing out. It won't make any difference.
	   private HashMap<Query, Item> MakeRegularItems (HashMap<Query,ArrayList<IncItem>> incItemSet){
		   //This method takes all sets of incremental items and returns a set of items, one per query used in the incremental set, which each correspond to the incremental
		   //items taken.
		   HashMap<Query, Item> regularSet = new HashMap<Query, Item>();
		   for (Query q : _querySpace){
			   ArrayList<IncItem> currentList = incItemSet.get(q);
			   //Not all queries will have associated incremental items.
			   //The second part of the condition was added to catch empty lists.
			   if (currentList != null && currentList.size() != 0){
				   //The value and weight of the associated item are equal to the sum of the values and weights of the incremental items taken.
//				   double valTotal = 0;
//				   double wgtTotal = 0;
//				   //The bid, budget, targeting, isID, and idx parameters come from the last incremental item taken.
//				   double bid = currentList.get(currentList.size()-1).itemHigh().b();
//				   double budget = currentList.get(currentList.size()-1).itemHigh().budget();
//				   boolean targeting = currentList.get(currentList.size()-1).itemHigh().targ();
//				   int isID = currentList.get(currentList.size()-1).itemHigh().isID();
//				   int idx = currentList.get(currentList.size()-1).itemHigh().idx();
//				   
//				   for (int i = 0; i<currentList.size(); i++){
//					   valTotal += currentList.get(i).v();
//					   wgtTotal += currentList.get(i).w();
//				   }
//				   regularSet.put(q, new Item(q, wgtTotal, valTotal, bid, budget, targeting, isID, idx));
				   
				   //The correct item, ignoring all these calculations, is the itemHigh for the last selected incremental item.
				   regularSet.put(q, currentList.get(currentList.size()-1).itemHigh());
			   }
		   }
		   return regularSet;
	   }
	   
	   private double SolutionProfit (HashMap<Integer, HashMap<Integer,Double>> profitMemoizeMap, Map<Query,ArrayList<Predictions>> allPredictionsMap, HashMap<Query,ArrayList<Double>> bidLists,
			   HashMap<Query,ArrayList<Double>> budgetLists, int[] capacityArray){
		   //This method takes a solution and returns the expected profit from it.
		   
		   double totalProfit = 0;
		   
		   //This is the old version, which does not memoize and is generally worse than the new version in every way. It also only works for a single day's solution, so if you
		   //were to revert to this one, you would have to rewrite the hill-climbing code to iterate over the array using this...but why would you ever want to do that?
		   
//		   //Iterate through each query.
//		   for (Query q: _querySpace){
//			   Item currentItem = solution.get(q);
//			   //Not every query has a corresponding item in the solution.
//			   if (currentItem != null){
//				   //If it does, however, add that value to our profit tally.
//				   totalProfit += currentItem.v();
//			   }
//		   }
//		   //Return the expected profit.
		   
		   
		   //This is the new version, which does in fact memoize.
		   //The memoizing map takes two parameters: 1) How much capacity remains in the window on the given day, and 2) the target capacity for that day, as the former dictates
		   //the penalty and the latter dictates the knapsack's "size." Thus, expected profit on a given day is in theory the same for any two days where both those parameters
		   //are the same.
		   
//		   For each remaining day in the capacity array:
		   for(int i = 4; i<capacityArray.length; i++){
			   int salesRemaining = (int)(_capacity*_capMod.get(_capacity));
			   //Subtract off the last four days to get the sales remaining on this day.
			   for (int j = i-4; j<i; j++){
				   salesRemaining -= capacityArray[j];
			   }
			   double profit;
			   //If we have a matching entry in our profit map for X sales remaining AND Y target sales, we use that one.
			   if (profitMemoizeMap.get(salesRemaining) != null && profitMemoizeMap.get(salesRemaining).get(capacityArray[i]) != null){
				   profit = profitMemoizeMap.get(salesRemaining).get(capacityArray[i]);
			   }
			   else {
				   //Otherwise, we fetch the solution via FGMCKP.
				   profit = 0.0;
				   HashMap<Query, Item> solution = FastGreedyMCKP(allPredictionsMap, bidLists, budgetLists, capacityArray, i);
				   for (Query q : _querySpace){
					   if (solution.get(q) != null){
						   //Total the expected profit from this solution.
						   profit += solution.get(q).v();   
					   }				    
				   }
				   if (profitMemoizeMap.get(salesRemaining) == null){
					   //If we have absolutely no entry for X sales remaining, create one in which to store the result.
					   HashMap<Integer,Double> profitMap = new HashMap<Integer,Double>(capacityArray.length);
					   profitMap.put(capacityArray[i], profit);
					   profitMemoizeMap.put(salesRemaining, profitMap);
				   } else {
					   //If we do have one for X sales remaining but not Y target sales, add that entry.
					   profitMemoizeMap.get(salesRemaining).put(capacityArray[i], profit);
				   }
			   }
			   //Increment our profit total and continue.
			   totalProfit += profit;
		   }	   
		   //And return this solution's total profit.
		   return totalProfit;
	   }

	@Override
	public AbstractAgent getCopy() {
		MultiDayOptimizer copy = new MultiDayOptimizer(_numTake, amyHack,amyHack2, changeWandV, changeBudget, accountForProbing);
		return copy;
	}

	

	   /*
	    * End of bare-bones hill climbing solution.
	    * Sebastian Sigmon, dweomer.bozwevial@gmail.com
	    */
	   
	   

}
