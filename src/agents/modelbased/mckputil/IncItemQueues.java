package agents.modelbased.mckputil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import edu.umich.eecs.tac.props.Query;

public class IncItemQueues {
	
	
	private PriorityQueue<IncItem> highestEfficiency;
	private HashMap<Query, ArrayList<IncItem>> itemQueues;
	
	public IncItemQueues(){
		highestEfficiency = new PriorityQueue<IncItem>();
		itemQueues = new HashMap<Query, ArrayList<IncItem>>();
	}
	
	public void addItems(Query q, ArrayList<IncItem> i){
		itemQueues.put(q, i);
	}
	
	public void setHighest(){
		for (Query q : itemQueues.keySet()){
			if(itemQueues.containsKey(q) && itemQueues.get(q).size()>0){
				highestEfficiency.add(itemQueues.get(q).remove(0));
			}
		}
	}
	
	public IncItem getNextHighest(){
		IncItem high = null;
		if(highestEfficiency.size()>0){
			high = highestEfficiency.remove();
			IncItem newItem;
			if(itemQueues.containsKey(high.item().q()) && itemQueues.get(high.item().q()).size()>0){
				newItem = itemQueues.get(high.item().q()).remove(0);
				highestEfficiency.add(newItem);
			}
		}else{
			System.out.println("highestEfficiency list is empty");
		}
		
		return high;
	}
	
}
