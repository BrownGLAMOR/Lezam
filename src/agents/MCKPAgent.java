package agents;

import java.util.*;
import modelers.*;
import modelers.bidtoposition.*;
import props.*;
import agents.mckp.*;
import edu.umich.eecs.tac.props.*;

public class MCKPAgent extends AbstractAgent {
	protected int _day;
	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	protected int _numSlots;
	
	//hashtable to retain our previous bids
	private HashMap<Integer, Double> lastBid; //query ID to bid;
	
	private int numUsers;
	
	/**
	 * These three fields are where this agent can be improved.  The success of this agent is definitely
	 * dependent on the accuracy of the capacity, bid, and click models.
	 */
	protected UnitsSoldModel _unitsSold;
	protected BidToPositionModel _positionBid;
	protected PositionToClicksAverage _positionClicks;
	
	protected Hashtable<Query,Integer> _queryId;
	
	protected Hashtable<Query,Double> _baseLineConversion;
	protected Hashtable<Query,Double> _noOversellConversion;
	protected Hashtable<Query,Double> _rpc;
	Set<Query> _F1componentSpecialty;
	Set<Query> _F2componentSpecialty;
	
	protected BidBundle _bidBundle;
	
	public MCKPAgent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		numUsers = 4000; //I'm not sure how to get this value actually
		lastBid = new HashMap<Integer, Double>();
		
		printAdvertiserInfo();
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distributionWindow = _advertiserInfo.getDistributionWindow();
		
		_numSlots = _slotInfo.getRegularSlots();
		
		_unitsSold = new UnitsSoldModelMean(_distributionWindow);
		_positionBid = new BucketBidToPositionModel(_querySpace, _numSlots);
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
		for(Query q : _querySpace) 
			_rpc.put(q, 10.0);
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
		QueryReport qr = _queryReports.remove();
		
		_positionBid.updateReport(qr);
		_positionClicks.updateReport(qr);
		
		SalesReport sr = _salesReports.remove();
		_unitsSold.updateReport(sr);
		
		if(_day > 4){
			//generate undominated items and convert them to inc items
			//LinkedList<IncItem> iis = new LinkedList<IncItem>();
			int numSlots = _slotInfo.getPromotedSlots()+ _slotInfo.getRegularSlots();
			Misc.println("num slots " + numSlots);
			
			LinkedList<IncItem> allIncItems = new LinkedList<IncItem>();
			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			for(Query q : _querySpace) {
				
				double rpc = _rpc.get(q);
				double convProb = _noOversellConversion.get(q);
				
				/**
				 * Uses the a position click exponential model and a position bid linear model to 
				 * generate the item set for each query
				 */
				Item[] items = new Item[numSlots];
				for(int s=0; s<items.length; s++) {//slot
					int numClicks = _positionClicks.getClicks(q, s);//!!!getClicks
					double bid = _positionBid.getBid(q, s);
					double cpc = bid;//!!!getCPC
					double w = numClicks*cpc;
					Misc.myassert(Double.isNaN(w) || w >= 0);
					double v = numClicks*rpc*convProb;
					int isID = _queryId.get(q);
					items[s] = new Item(q,w,v,bid, isID);	
				}
				
				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
			}
			
			//pick items greedily is order of decreasing efficiency
			Collections.sort(allIncItems);
			//Misc.println("sorted incremental items", Output.OPTIMAL);
			//Misc.printList(allIncItems,"\n", Output.OPTIMAL);
			
			HashMap<Integer,Item> solution = new HashMap<Integer,Item>();
			//_advertiserInfo.getDistributionCapacity()/_advertiserInfo.getDistributionWindow()
			double budget = (_distributionCapacity - _unitsSold.getWindowSold()) / _distributionWindow;
			
			//4. greedily fill the knapsack
			for(IncItem ii: allIncItems) {
				//Misc.println("adding item " + ii, Output.OPTIMAL);
				//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
				//set replace lighter items as we want
				if(budget >= ii.w()) {
					solution.put(ii.item().isID(), ii.item());
					budget -= ii.w();
				}else{
					break;
				}
			}
			
			//set bids
			_bidBundle = new BidBundle();
			for(Query q : _querySpace){
				Integer isID = _queryId.get(q);
				double bid;
				if(solution.containsKey(isID)) {
					bid = solution.get(isID).b();
					lastBid.put(isID, bid);
				}
				else bid = lastBid.get(isID);
				_bidBundle.addQuery(q, bid, new Ad());
			}
		}
		else {
			_bidBundle = new BidBundle();
			for(Query q : _querySpace){
				double bid = 1;
				_bidBundle.addQuery(q, bid, new Ad());
				lastBid.put(_queryId.get(q), bid);
			}
		}
		
		_day++;
	}
	
	
	public static Item[] getUndominated(Item[] items) {
		//Misc.printArray("getUndominated. all items", items);
		Arrays.sort(items,new ItemComparatorByWeight());

		//Misc.printArray("sorted by weight", items);
		
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
		
	
		//remove lp-dominated items
		//see Figure 2 in http://www.cs.brown.edu/people/vnarodit/troa.pdf
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(new Query(),0,0,-1,-1));//add item with zero weight and value
		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items
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

	
	public static IncItem[] getIncremental(Item[] items) {
		Misc.printArray("getIncremental", items);
		//items are still sorted by weight - create incremental items
		Item[] uItems = getUndominated(items);
		IncItem[] ii = new IncItem[uItems.length];
		ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
		for(int item=1; item<uItems.length; item++) {
			Item prev = uItems[item-1];
			Item cur = uItems[item];
			ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
		}
		Misc.printArray("inc items", ii);
		return ii;
	}
		
	
	
	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("****************************************");
		for (Query q: _querySpace){
			System.out.println("Query: " + q + ", Bid: " + _bidBundle.getBid(q));
		}
		System.out.println("****************************************");
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



