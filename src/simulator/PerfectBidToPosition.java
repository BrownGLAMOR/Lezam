package simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import modelers.AbstractModel;

public class PerfectBidToPosition extends AbstractModel {
	
	private String[] _agents;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private double _squashing;
	private double _ourAdEffect;
	private Query _query;
	
	public PerfectBidToPosition(String[] agents,
			HashMap<String,HashMap<Query,Double>> bids,
			HashMap<String,HashMap<Query,Double>> advEffect,
			double squashing,
			double ourAdEffect,
			Query query) {
		
		_agents = agents;
		_bids = bids;
		_advEffect = advEffect;
		_squashing = squashing;
		_ourAdEffect = ourAdEffect;
		_query = query;
	}

	@Override
	public Object getPrediction(Object info) {
		/*
		 * The incoming info for a bid to position model
		 * should always be a double which is the bid
		 */
		ArrayList<Double> bids = new ArrayList<Double>();
		for(int i = 0; i < _agents.length; i++) {
			double advEff = _advEffect.get(_agents[i]).get(_query);
			double bid = _bids.get(_agents[i]).get(_query);
			double realbid = Math.pow(advEff,_squashing)*bid;
			bids.add(realbid);
		}
		double bid = (Double) info;
		double realbid = Math.pow(_ourAdEffect, _squashing)*bid;
		bids.add(realbid);
		Collections.sort(bids,Collections.reverseOrder());
		for(int i = 0; i < bids.size()-1;i++) {
			if(bids.get(i) == realbid) {
				/*
				 * Return the position this bid puts us in
				 */
				return i+1;
			}
		}
		throw new RuntimeException("Error on Bid Calculation");
	}

	@Override
	public void updateModel(QueryReport queryReport, SalesReport salesReport, Object otherInfo) {
		//Nothing needs to be updated
	}

}
