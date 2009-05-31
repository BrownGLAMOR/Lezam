package simulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import newmodels.AbstractModel;
import newmodels.bidtoslot.AbstractBidToSlotModel;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPosition extends AbstractBidToSlotModel {
	
	private String[] _agents;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private double _squashing;
	private double _ourAdEffect;
	
	public PerfectBidToPosition(String[] agents,
			HashMap<String,HashMap<Query,Double>> bids,
			HashMap<String,HashMap<Query,Double>> advEffect,
			double squashing,
			double ourAdEffect,
			Query query) {
		
		super(query);
		_agents = agents;
		_bids = bids;
		_advEffect = advEffect;
		_squashing = squashing;
		_ourAdEffect = ourAdEffect;
	}

	@Override
	public double getPrediction(double info) {
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
	public void updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
	}

}
