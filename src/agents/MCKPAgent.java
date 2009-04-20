package agents;


import java.util.*;


import props.*;
import agents.mckp.*;
import edu.umich.eecs.tac.props.*;

public class MCKPAgent extends AbstractAgent {
	protected BidBundle _bidBundle;
	
	public MCKPAgent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
	}
	
	@Override
	protected void updateBidStrategy() {
		//generate undominated items and convert them to inc items
		LinkedList<IncItem> iis = new LinkedList<IncItem>();
		int numSlots = _slotInfo.getPromotedSlots()+ _slotInfo.getRegularSlots();
		Misc.println("num slots " + numSlots);
		
		LinkedList<IncItem> allIncItems = new LinkedList<IncItem>();
		//want the queries to be in a guaranteed order - put them in an array
		//index will be used as the id of the query
		Query[] queries = (Query[])_querySpace.toArray();
		for(int i=0; i<queries.length; i++) {
			Query q = queries[i];
			//let's generate an item set for this query
			Item[] items = new Item[numSlots];
			for(int s=0; s<items.length; s++) {//slot
				int numClicks = numSlots - s;//!!!getClicks
				double bid = numSlots - s;//!!!getBid for position
				double cpc = bid;//!!!getCPC
				double w = numClicks*cpc;
				double rpc = getAvaregeProductPrice();
				double convProb = .1;//!!!get from report
				double v = numClicks*rpc*convProb;
				int isID = i;
				items[s] = new Item(w,v,bid, isID);
			
				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
			}
		}
		
		//pick items greedily is order of decreasing efficiency
		Collections.sort(allIncItems);
		Misc.println("sorted incremental items", Output.OPTIMAL);
		Misc.printList(allIncItems,"\n", Output.OPTIMAL);
		
		HashMap<Integer,Item> solution = new HashMap<Integer,Item>();
		//_advertiserInfo.getDistributionCapacity()/_advertiserInfo.getDistributionWindow()
		double budget = 100;//!!!getbudget
		//4. greedily fill the knapsack
		for(IncItem ii: allIncItems) {
			Misc.println("adding item " + ii, Output.OPTIMAL);
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
		for(int i=0; i<queries.length; i++) {
			Query q = queries[i];
			double bid = 0;
			Integer isID = i;
			if(solution.containsKey(isID)) { 
				bid = solution.get(isID).b();
				_bidBundle.addQuery(q, bid, new Ad());//!!! is new Ad() the right argument to pass
			}
			
		}
		
	}
	
	
	public static Item[] getUndominated(Item[] items) {
		Misc.printArray("getUndominated. all items", items);
		Arrays.sort(items,new ItemComparatorByWeight());

		Misc.printArray("sorted by weight", items);
		
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
		q.add(new Item(0,0,-1,-1));//add item with zero weight and value
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
			while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w()) <= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
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
	}

	
	public static IncItem[] getIncremental(Item[] items) {	
		//items are still sorted by weight - create incremental items
		Item[] uItems = getUndominated(items);
		IncItem[] ii = new IncItem[uItems.length];
		ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
		for(int item=1; item<uItems.length; item++) {
			Item prev = uItems[item-1];
			Item cur = uItems[item];
			ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
		}
		
		return ii;
	}
		
	
	
	@Override
	protected BidBundle buildBidBudle(){
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



