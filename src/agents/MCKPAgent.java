package agents;


import java.util.*;


import props.*;
import agents.mckp.*;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class MCKPAgent extends AbstractAgent {
	protected MCKPBidStrategy _bidStrategy;
	
	public MCKPAgent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
		_bidStrategy = new MCKPBidStrategy(_querySpace);
	}
	

	
	
	@Override
	protected void updateBidStrategy() {
		//generate undominated items and convert them to inc items
		LinkedList<IncItem> iis = new LinkedList<IncItem>();
		int numSlots = _slotInfo.getPromotedSlots()+ _slotInfo.getRegularSlots();
		
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
				double rpc = 10;//!!!how do i get unit sales profit? _retailCatalog.getSalesProfit();//!!include manufacturer bonus
				double v = numClicks*rpc;
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
		
		//set bids!!!
		for(int i=0; i<queries.nlength; i++) {
			
		}
		
	}
	
	
	public static Item[] getUndominated(Item[] items) {
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(0,0,-1,-1));//add item with zero weight and value
		Arrays.sort(items,new ItemComparatorByWeight());

		for(int i=0; i<items.length; i++) {
			q.add(items[i]);
			int l = q.size()-1;
			Item li = q.get(l);//last item
			Item nli = q.get(l-1);//next to last
			if(li.w() == nli.w()) {
				if(li.v() > nli.v()) {
					q.remove(l-1);
				}else{
					q.remove(l);
				}
				l = q.size() - 1;
				//while there are at least three elements and ...
				while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w()) <= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
					q.remove(l-1);
					l--;
				}
			}			
		}
		
		//remove the (0,0) item
		if(q.get(0).w() == 0 && q.get(0).v() == 0) {
			q.remove(0);
		}
		
		return (Item[]) q.toArray(new Item[0]);
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
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	



	public static class Output {
		public static boolean OPTIMAL = true;
	}
	
	
}



