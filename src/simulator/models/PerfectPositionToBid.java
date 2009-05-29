package simulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import modelers.AbstractModel;
import modelers.AbstractSlotToBidModel;

public class PerfectPositionToBid extends AbstractSlotToBidModel {
	
	private String[] _agents;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private double _squashing;
	private double _ourAdEffect;
	
	public PerfectPositionToBid(String[] agents,
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
		Collections.sort(bids,Collections.reverseOrder());
		int pos = (int) Math.ceil((Double) info);
		double bidtobeat = bids.get(pos);
		double bid = bidtobeat / Math.pow(_ourAdEffect, _squashing);
		return bid+.01;
	}

	@Override
	public void updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
	}

}
