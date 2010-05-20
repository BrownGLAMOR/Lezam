package models.bidmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

import models.AbstractModel;

public class JointDistBidModel extends AbstractBidModel{

	ArrayList<HashMap<String, Integer>> rankhistory; 
	HashMap<Query, JointDistFilter> filters;
	ArrayList<Query> _queries;
	Set<String> _advertisers;
	String _ourAdvertiser;
	
	public JointDistBidModel(Set<String> advertisers, String ourAdvertiser) {
		
		_advertisers = advertisers;
		_ourAdvertiser = ourAdvertiser; 
		
		//rankhistory = new ArrayList<HashMap<String, Integer>>();		 
		filters = new HashMap<Query, JointDistFilter>();

		_queries = new ArrayList<Query>();
		
		_queries.add(new Query("flat", "dvd"));
		_queries.add(new Query("flat", "tv"));
		_queries.add(new Query("flat", "audio"));
		_queries.add(new Query("pg", "dvd"));
		_queries.add(new Query("pg", "tv"));
		_queries.add(new Query("pg", "audio"));
		_queries.add(new Query("lioneer", "dvd"));
		_queries.add(new Query("lioneer", "tv"));
		_queries.add(new Query("lioneer", "audio"));
		Query q = new Query();
		q.setComponent("tv");
		_queries.add(q);
		q = new Query();
		q.setComponent("dvd");
		_queries.add(q);
		q = new Query();
		q.setComponent("audio");
		_queries.add(q);
		q = new Query();
		q.setManufacturer("pg");
		_queries.add(q);
		q = new Query();
		q.setManufacturer("flat");
		_queries.add(q);
		q = new Query();
		q.setManufacturer("lioneer");		
		_queries.add(q);
		_queries.add(new Query());
			
		
		for(Query qr : _queries) {
			double curMaxBid = 0;
			if(qr.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				curMaxBid = maxReasonableBidF0;
			}else if(qr.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
				curMaxBid = maxReasonableBidF1;
			}else if(qr.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
				curMaxBid = maxReasonableBidF2;
			}
			filters.put(qr, new JointDistFilter(advertisers, qr.getType(), curMaxBid, ourAdvertiser));
		}
	
	}
	
	public void setAdvertiser(String ourAdvertiser) {
		_ourAdvertiser = ourAdvertiser;
		System.out.println("updating advertiser: " + ourAdvertiser);
	}
	
	
	@Override
	public double getPrediction(String player, Query q) {
		return filters.get(q).getBid(player);
	}

	@Override
	public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks) {
		for(Query q : filters.keySet()) {
			//System.out.println("ff");
			filters.get(q).simulateDay(ourBid.get(q), cpc.get(q), ranks.get(q));
			
		}
		return true; //Why does this return a boolean again?
	}

	public static void main(String[] args) {		
		
		//JointDistBidModel j = new JointDistBidModel();
		
	}
	
	@Override
	public AbstractModel getCopy() {
		return new JointDistBidModel(_advertisers, _ourAdvertiser);
	}

}
