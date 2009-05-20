package agents;

import java.util.*;
import modelers.*;
import modelers.bidtoposition.sam.PositionBidLinear;
import modelers.positiontoclick.PositionToClicksAverage;
import modelers.unitssold.UnitsSoldModel;
import modelers.unitssold.UnitsSoldModelMeanWindow;
import props.*;
import agents.mckp.*;
import edu.umich.eecs.tac.props.*;

/**
 * _unitsSold is an instance of UnitsSoldModelMeanWindow (day -> units sold during window model)
 * _positionBid is an instance of PositionBidLinear (position -> bid model)
 * _positionClicks is an instance of PositionToClicksAverage (position -> clicks model)
 * 
 * The simplifying assumptions made are mostly in the fillKnapsack method.  In the absence of 
 * a model for estimating the loss from going over capacity, I've made some simple assumptions
 * for the value the new and old average probability of conversion, as well as assuming that
 * the value lost is equal to the lost conversion probability times the average unit sales price.
 * 
 * @author spucci, carleton, and victor
 */

public class MCKPAgent extends AbstractAgent {
	protected int _day;
	
	protected int _distributionCapacity;
	private double _distCapacDiscount;
	protected int _distributionWindow;
	protected int _numSlots;
	
	//hash table to retain our previous bids
	private HashMap<Query, Double> recentBid; //query ID to bid;
	private HashMap<Query, Double> previousBid; //query ID to bid;
	double defaultBid;
	
	private int numUsers;
	
	/**
	 * These three fields are where this agent can be improved.  The success of this agent is definitely
	 * dependent on the accuracy of the capacity, bid, and click models.
	 */
	protected UnitsSoldModel _unitsSold;
	protected PositionBidLinear _positionBid;
	protected PositionToClicksAverage _positionClicks;
	
	protected Hashtable<Query,Integer> _queryId;
	
	protected Hashtable<Query,Double> _baseLineConversion;
	protected Hashtable<Query,Double> _noOversellConversion;
	protected Hashtable<Query,Double> _rpc;
	Set<Query> _F1componentSpecialty;
	Set<Query> _F2componentSpecialty;
	
	private int _capacityInc;
	protected BidBundle _bidBundle;
	
	public MCKPAgent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		numUsers = 4000; //I'm not sure how to get this value actually
		
		recentBid = new HashMap<Query, Double>();
		previousBid = new HashMap<Query, Double>();
		defaultBid = 1; /******************* this is a useful value for staying in the auctions for informational purposes, could be more specific though */
		
		_capacityInc = 18; //This sets how much we attempt to go over budget
		
		printAdvertiserInfo();
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distCapacDiscount = _advertiserInfo.getDistributionCapacityDiscounter();
		_distributionWindow = _advertiserInfo.getDistributionWindow();

		_numSlots = _slotInfo.getPromotedSlots()+ _slotInfo.getRegularSlots();
		
		_unitsSold = new UnitsSoldModelMeanWindow(_distributionWindow);
		_positionBid = new PositionBidLinear(_numSlots, .75);
		_positionClicks = new PositionToClicksAverage(_numSlots, _querySpace, numUsers/3); //estimate
		
		_baseLineConversion = new Hashtable<Query,Double>();
		_noOversellConversion = new Hashtable<Query,Double>();
		
		// BEGIN -> Set probability of conversion for queries
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ZERO)) {
			_baseLineConversion.put(q, 0.1); 
			_noOversellConversion.put(q, 0.1);
		}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ONE)) {
			_baseLineConversion.put(q, 0.2); 
			_noOversellConversion.put(q, 0.2);
		}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_TWO)) {
			_baseLineConversion.put(q, 0.3); 
			_noOversellConversion.put(q, 0.3);
		}
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		_F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		for(Query q : _F1componentSpecialty) 
			_noOversellConversion.put(q, 0.27);
		_F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		for(Query q : _F2componentSpecialty) 
			_noOversellConversion.put(q, 0.39);
		// END -> Set probability of conversion for queries
		
		Set<Query> manufacturerSpecialty = _queryManufacturer.get(_advertiserInfo.getManufacturerSpecialty());
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		
		// set revenue prices
		_rpc = new Hashtable<Query,Double>();
		for(Query q : _querySpace) {
			_rpc.put(q, 10.0);
			recentBid.put(q, defaultBid);
			previousBid.put(q, defaultBid);
		}
		for(Query q : manufacturerSpecialty) 
			_rpc.put(q, manufacturerBonus*_rpc.get(q));
		
		// assign IDs to the queries
		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			i++;
			_queryId.put(q, i);
		}
		
		_day = 0;
	}
	
	@Override
	protected void updateBidStrategy() {
		//update models
		if (!_queryReports.isEmpty()){
			QueryReport qr = _queryReports.remove();
			_positionBid.updateReport(qr, previousBid);
			
			previousBid = new HashMap<Query, Double>(recentBid);
			//saves the most recent bids to the previous bids hashMap
			
			_positionClicks.updateReport(qr);
			System.out.println(_positionClicks);
		}
		if (!_salesReports.isEmpty()){
			SalesReport sr = _salesReports.remove();
			_unitsSold.updateReport(sr);
		}
		
		if(_day > 2){
			
			//generate undominated items and convert them to inc items
			//LinkedList<IncItem> iis = new LinkedList<IncItem>();
			int numSlots = _slotInfo.getPromotedSlots()+ _slotInfo.getRegularSlots();
			
			LinkedList<IncItem> allIncItems = new LinkedList<IncItem>();
			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			for(Query q : _querySpace) {
				
				double rpc = _rpc.get(q);
				double convProb = _noOversellConversion.get(q);
				
				/**
				 * Uses a position click average model and a position bid linear model to 
				 * generate the item set for each query
				 */
				Item[] items = new Item[numSlots];
				for(int s=1; s<=_numSlots; s++) {//slot
					int numClicks = _positionClicks.getClicks(q, s);//!!!getClicks
					double bid = _positionBid.getBid(q, s);
					double CPC = _positionBid.getCPC(q, s);
					
					if (bid == 0) numClicks = 0;
					double w = numClicks*convProb; 				//weight = numClicks * convProb
					double v = numClicks*(rpc*convProb - CPC);	//value = numClicks(USP * convProb - CPC)
					
					int isID = _queryId.get(q);
					items[s-1] = new Item(q,w,v,bid,isID);	
					System.out.println(q + ", slot " + s + ", numCl " + numClicks + ", bid " + bid +
							", weight " + w + ", value " + v);
				}
				
				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
			}
			
			//pick items greedily is order of decreasing efficiency
			Collections.sort(allIncItems);
			Misc.printList(allIncItems,"\n", Output.OPTIMAL);
			
			double budget = _distributionCapacity + _distributionCapacity/_distributionWindow - _unitsSold.getWindowSold();
			// unitsSold.getWindowSold returns the items sold over the course of the last window, so this is :
			// budget = capacity(1+1/windowLength) - soldInWindow
			System.out.println("\nCap: " + _distributionCapacity + ", Window: " + 
					_unitsSold.getWindowSold() + ", Budget: " + budget + "\n");
			
			/**
			 * TODO I suggest adding some code here that checks to see what the budget value is, and if it is pretty
			 * high, and the _day value is past 59 (it goes to 61), then is jacks up the bid for the last two days
			 * to try and sneak in some last minute sales.
			 */
			
			HashMap<Integer,Item> solution = fillKnapsack(allIncItems, budget);
			
			//set bids
			_bidBundle = new BidBundle();
			for(Query q : _querySpace){
				
				Integer isID = _queryId.get(q);
				double bid;
				
				if(solution.containsKey(isID)) {
					bid = solution.get(isID).b();
					recentBid.put(q, bid);
				}
				else bid = defaultBid; // TODO this is a hack that was the result of the fact that the item sets were empty
				
				_bidBundle.addQuery(q, bid, new Ad(), bid*_distributionCapacity);
			}
		}
		//bid bundle for first two days
		//TODO The values .7, 1.3, and 1.7 are game specific, and then agent would be more 
		//     robust if it had a better estimate for starting defaults
		else {
			_bidBundle = new BidBundle();
			for(Query q : _querySpace){
				double bid;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = .7;
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = 1.3;
				else 
					bid = 1.7;
				_bidBundle.addQuery(q, bid, new Ad(), bid*_distributionCapacity);
				recentBid.put(q, bid);
			}
		}
		
		_day++;
	}
	
	/**
	 * Greedily fill the knapsack by selecting incremental items ---------TODO get real model
	 * @param incItems
	 * @param budget
	 * @return
	 */
	private HashMap<Integer,Item> fillKnapsack(LinkedList<IncItem> incItems, double budget){
		HashMap<Integer,Item> solution = new HashMap<Integer, Item>();
		LinkedList<IncItem> temp = new LinkedList<IncItem>();
		
		boolean incremented = false;
		double valueLost = 0;
		double valueGained = 0;
		int knapSackIter = 0;
		
		for(IncItem ii: incItems) {
			//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
			//set replace lighter items as we want
			
			if(budget >= ii.w()) {
				if (incremented){
					temp.addLast(ii);
					budget -= ii.w();
					valueGained += ii.v(); //amount gained as a result of extending capacity
				}
				else {
					Misc.println("adding item over capacity" + ii, Output.OPTIMAL);
					solution.put(ii.item().isID(), ii.item());
					budget -= ii.w();
				}
			}
			else{
				if (incremented){
					if (valueGained >= valueLost){ //checks to see if it was worth extending our capacity
						while (!temp.isEmpty()){
							IncItem inc = temp.removeFirst();
							Misc.println("adding item " + ii, Output.OPTIMAL);
							solution.put(inc.item().isID(), inc.item());
						}
						valueLost = 0;
						valueGained = 0;
					}
					else break;
				}
				double avgConvProb = .253; //the average probability of conversion;
				/*
				double avgUSP = 0;
				for (Query q : _querySpace){
					avgUSP += _rpc.get(q);
				}
				avgUSP /= 16;
				*/// This can be used later if the values actually change for the sales bonus
				double avgUSP = 11.25;
				for (int i = _capacityInc*knapSackIter+1; i <= _capacityInc*(knapSackIter+1); i++){
					double iD = Math.pow(_distCapacDiscount, i);
					double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
					valueLost += (avgConvProb - worseConvProb)*avgUSP;
				}
				budget+=_capacityInc;
				incremented = true;
				knapSackIter++;
			}
		}
		return solution;
	}
	
	/**
	 * Get undominated items ...................................................... getUndominated
	 * @param items
	 * @return
	 */
	public static Item[] getUndominated(Item[] items) {
		Arrays.sort(items,new ItemComparatorByWeight());
		
		//remove dominated items (higher weight, lower value)		
		LinkedList<Item> temp = new LinkedList<Item>();
		temp.add(items[0]);
		for(int i=1; i<items.length; i++) {
			Item lastUndominated = temp.get(temp.size()-1); 
			//Misc.println("prev " + lastUndominated.value() + " current " +  itemSet.get(j).value());
			if(lastUndominated.v() < items[i].v()) {
				//Misc.println("adding undominated item " + itemSet.get(j));
				temp.add(items[i]);
			}
		}
		
		//items now contain only undominated items
		items = temp.toArray(new Item[0]);
		Misc.printArray("undominated 1", items);
	
		//remove lp-dominated items
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(new Query(),0,0,-1,-1));//add item with zero weight and value
		
		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items now
			int l = q.size()-1;
			//Misc.println("l=" + l);
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
		Misc.printArray("undominated items", uItems);
		return uItems;
	}
	
	public void testGetUndominated() {
		//dominate
		Item[] items = new Item[2];
		items[0] = new Item(5, 2);
		items[1] = new Item(7, 1);
		Item[] uItems = getUndominated(items);
		Misc.myassert(uItems.length == 1 && uItems[0].w() == 5 && uItems[0].v() == 2);
		
		//lp dominate with zero item
		items = new Item[2];
		items[1] = new Item(4, 1);
		items[0] = new Item(5, 2);//.8*5,.8*2
		uItems = getUndominated(items);
		Misc.myassert(uItems.length == 1 && uItems[0].w() == 5 && uItems[0].v() == 2);

		//lp dominate: half of item 1 and 3 dominate item 2
		items = new Item[3];
		items[0] = new Item(1, 9);
		items[1] = new Item(2, 10);
		items[2] = new Item(3, 12);
		uItems = getUndominated(items);
		Misc.myassert(uItems.length == 2 && uItems[0].w() == 1 && uItems[0].v() == 9 && uItems[1].w() == 3 && uItems[1].v() == 12);
		
		IncItem[] iItems = getIncremental(items);
		Misc.myassert(iItems.length == 2 && iItems[0].w() == 1 && iItems[0].v() == 9 && iItems[1].w() == 2 && iItems[1].v() == 3);
		
	}

	/**
	 * Get incremental items ...................................................... getIncremental
	 * @param items
	 * @return
	 */
	public static IncItem[] getIncremental(Item[] items) {
		Misc.printArray("getIncremental", items);
		//items are still sorted by weight - create incremental items
		
		Item[] uItems = getUndominated(items);
		IncItem[] ii = new IncItem[uItems.length];
		
		if (uItems.length != 0){ //getUndominated can return an empty array
			ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
			for(int item=1; item<uItems.length; item++) {
				Item prev = uItems[item-1];
				Item cur = uItems[item];
				ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
			}
			Misc.printArray("inc items", ii);
		}
		
		return ii;
	}
		
	
	
	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**************************************** Day: " + _day);
		for (Query q: _querySpace){
			System.out.println(q + ", Bid: " + _bidBundle.getBid(q) + ", Budget: " + _bidBundle.getDailyLimit(q));
		}
		System.out.println("**************************************** Day: " + _day);
		return _bidBundle;
	}

	
	public static void main (String[] args) {
		MCKPAgent agent = new MCKPAgent();
		agent.testGetUndominated();
	}


	public static class Output {
		public static boolean OPTIMAL = true;
	}
	
	
}



