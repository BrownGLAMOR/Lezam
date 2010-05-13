package models.bidmodel;

import java.util.ArrayList;
import java.util.HashMap;

import edu.umich.eecs.tac.props.Query;

import models.AbstractModel;

public class JointDistBidModel extends AbstractBidModel{

	ArrayList<HashMap<String, Integer>> rankhistory; 
	HashMap<Query, JointDistFilter> filters;
	
	
	public JointDistBidModel() {
		
		rankhistory = new ArrayList<HashMap<String, Integer>>();		 
		filters = new HashMap<Query, JointDistFilter>();
		
		
		
	}
	
	
	
	
	@Override
	public double getPrediction(String player, Query q) {
		return filters.get(q).getBid(player);
	}

	@Override
	public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks) {
		for(Query q : filters.keySet()) {
			
			filters.get(q).simulateDay(ourBid.get(q), cpc.get(q), ranks.get(q));
			
		}
		return true; //Why does this return a boolean again?
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
