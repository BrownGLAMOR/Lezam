package simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.IncItemQueues;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;
import edu.umich.eecs.tac.props.Query;

public class TestAgent {
	FileWriter errorFile;  
	public TestAgent(String filename) throws IOException{
		String[] name = filename.split("\\/");
		String fname = name[name.length-1].split("\\.")[0];
		 errorFile = new FileWriter(System.getProperty("user.dir")+System.getProperty("file.separator")+fname+"_error_test.txt");
    	 
	}
	
	public TestAgent() {
		
	}
	
	public boolean isFillKnapsackEquiv(HashMap<Query, Item> solution, HashMap<Query, Item> solutionW, double day) throws IOException{
		for (Query q : solution.keySet()){
			if(solutionW.containsKey(q)){
				if (!solution.get(q).compareTo(solutionW.get(q))){
					System.out.println("FALSE___________________________");
					try {
						errorFile.write("FALSE: Items for query "+q+" not equal on day "+day+"\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return false;
				}
			}else{
				System.out.println("FALSE_Q___________________________");
				try {
					errorFile.write("FALSE: Query "+q+" not found on day "+day+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return false;
			}
		}
		System.out.println("TRUE___________________________");
		errorFile.write("True: day "+day+"\n");
	
		
		return true;
	}
	
	public void closeFile() throws IOException{
		System.out.println("CLOSING FILE__________________________________________________");
		errorFile.close();
	}
	

	public double testTimeQvsNQ(){
		 // PUT TEST HERE
		Random random = new Random(123);
		
		ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();
		IncItemQueues allIncItemsQ = new IncItemQueues();
		
		double epsilon = .2;
		long totalQtime = 0;
		long totalNQtime = 0;
		Query[] qSpace ={new Query (null,"audio"), 
		new Query (null,"dvd"),
		new Query ("lioneer","dvd"),
		new Query ("flat","tv"), 
		new Query ("lioneer",null),
		new Query ("pg","dvd"), 
		new Query ("flat","dvd"), 
		new Query ("flat","audio"), 
		new Query ("lioneer","tv"),
		new Query ("flat",null),
		new Query (null,"tv"),
		new Query ("lioneer","audio"),
		new Query ("pg","tv"), 
		new Query ("pg","audio"),
		new Query ("pg",null)};
		
		for (Query q : qSpace) {
	         if(q != new Query()) {
	        	 double[] bids = new double[500];
	        	 double[] budgets = new double[500];
	        	 double val = .0;
	        	 double val2 = .0;
	        	 
	        	 for (int i = 0; i<bids.length; i++){
	        		 val += .01;
	        		 val2 += 2;
	        		 bids[i] = val;
	        		 budgets[i] = val2;
	        	 }
	            ArrayList<Item> itemList = new ArrayList<Item>();
	            int itemCount = 0;
	            //make tons of items here
	            double numImps = random.nextInt(300)+1;
	            for(int k = 1; k < 2; k++) {
	                for(int bi = 0; bi < bids.length; bi++) {
	                   for(int bu = 0; bu < budgets.length; bu++) {
	                      boolean targeting = (k != 0);
	                      double bid = bids[bi];
	                      double budget = budgets[bu];
	                      double salesPrice = 10;
	                      double clickPr = .1;
	                      int numClicks = (int) (clickPr * numImps);
	                      double CPC = bid-epsilon;
	                      double cost = numClicks*CPC;
	                      double convProb = .3;
	                      double w = numClicks * convProb;            //weight = numClicks * convProv
	                      double v = w * salesPrice - cost;   //value = revenue - cost	[profit]
	                      itemList.add(new Item(q,w,v,bid,budget,targeting,0,itemCount));
	                      itemCount++;
	                   }
	                }
	            }
	            
	            if(itemList.size() > 0){
	            	Item[] items = itemList.toArray(new Item[0]);
	            	Item[] itemsNQ = itemList.toArray(new Item[0]);
	            	
	            
	            
	            
	            long startQtimeperQ = System.currentTimeMillis();
	            if(itemList.size() > 0) {
	            	System.out.println("Q "+itemList.size());
	            	//System.out.println("Items for " + q);
	            	//Item[] items = itemList.toArray(new Item[0]);
	            	IncItem[] iItems = getIncremental(items);
	            	// System.out.println("iItems length: "+iItems.length);
	            	allIncItemsQ.addItems(q, new ArrayList<IncItem>(Arrays.asList(iItems)));
	            	//System.out.println("IncItems for " + q);
//           			for( IncItem ii : iItems){
//        	 				  //System.out.println(ii);
//          			 }
	            }
	            long endQtimeperQ = System.currentTimeMillis();
	            totalQtime += (endQtimeperQ-startQtimeperQ);
	            
	            long startNQtimeperQ = System.currentTimeMillis();
	            if(itemList.size() > 0) {
	            	System.out.println("NQ "+itemList.size());
	            	//System.out.println("Items for " + q);
	            	//Item[] itemsNQ = itemList.toArray(new Item[0]);
	            	IncItem[] iItemsNQ = getIncremental(itemsNQ);
	            	System.out.println("NQ inc "+iItemsNQ.length);
	            	// System.out.println("iItems length: "+iItems.length);
	            	allIncItems.addAll(Arrays.asList(iItemsNQ));
	            	//System.out.println("IncItems for " + q);
//           			for( IncItem ii : iItems){
//        	 				  //System.out.println(ii);
//          			 }
	            }
	            long endNQtimeperQ = System.currentTimeMillis();
	            totalNQtime += (endNQtimeperQ-startNQtimeperQ);
	            }
	            
	            
	            
	         }
		}
		
		long startNQsort = System.currentTimeMillis();
		Collections.sort(allIncItems);
		long endNQsort = System.currentTimeMillis();
		
		long startQsort = System.currentTimeMillis();
		allIncItemsQ.setHighest();
		long endQsort = System.currentTimeMillis();
		
		System.out.println("Q time: "+(totalQtime+(endQsort-startQsort)));
		System.out.println("NQ time: "+(totalNQtime+(endNQsort-startNQsort)));
		
		return (totalNQtime+(endNQsort-startNQsort))-(totalQtime+(endQsort-startQsort));
	}
	
	public IncItem[] getIncremental(Item[] items) {
	      

	      //Item[] uItems = getUndominated(items);
		  Item[] uItems = items;
	      IncItem[] ii = new IncItem[uItems.length];

	      if (uItems.length != 0){ //getUndominated can return an empty array
	         ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0], null);
	         for(int item=1; item<uItems.length; item++) {
	            Item prev = uItems[item-1];
	            Item cur = uItems[item];
	            ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur, prev);
	            if (ii[item-1].eff()< ii[item].eff()){
	            	System.out.println("ERROR IncItem out of efficiency order");
	            	System.out.println(ii[item-1].eff()+" "+ii[item].eff());
	            }
	         }
	      }
	      
	      return ii;
	   }
	
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
	            double minBid = 10;//HC num
	            double maxBid = -10;//HC num
	            for(int j = 0; j < duplicates.size(); j++) {
	               double bid = duplicates.get(j).b();
	               if(bid > maxBid) {
	                  maxBid = bid;
	               }
	               if(bid < minBid) {
	                  minBid = bid;
	               }
	            }
	            Item newItem = new Item(item.q(), item.w(), item.v(), (maxBid+minBid)/2.0, item.targ(), item.isID(),item.idx());//HC num
	            betterTemp.add(newItem);
	         }
	      }

	      //items now contain only undominated items
	      items = betterTemp.toArray(new Item[0]);
	      Arrays.sort(items,new ItemComparatorByWeight());

	      //remove lp-dominated items
	      ArrayList<Item> q = new ArrayList<Item>();
	      q.add(new Item(new Query(),0,0,-1,false,1,0));//add item with zero weight and value//HC num

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

	public static void main (String[] args){
		TestAgent tester = new TestAgent();
		long totaldiff = 0;
		for (int i=0; i<300; i++){
			totaldiff+=tester.testTimeQvsNQ();
		}
		System.out.println("Final total Diff: "+totaldiff);
	}
	
}
