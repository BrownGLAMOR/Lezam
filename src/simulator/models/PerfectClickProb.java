package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import newmodels.AbstractModel;
import newmodels.slottoprclick.AbstractSlotToPrClick;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectClickProb extends AbstractSlotToPrClick {
	
	private double LAMBDA = .995;
	private String[] _agents;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<Query, Double> _contProb;
	private HashMap<String, HashMap<Query, Ad>> _adType;
	private HashMap<String, String> _compbonus;
	private HashMap<String, Integer> _overCap;
	private double _ourAdEffect;
	private double _squashing;
	private int _numSlots;
	private int _numPromSlots;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	
	public PerfectClickProb(String[] agents,
			HashMap<String,HashMap<Query,Double>> bids,
			HashMap<String,HashMap<Query,Double>> advEffect,
			HashMap<Query,Double> contProb,
			HashMap<String,HashMap<Query,Ad>> adType,
			HashMap<String,String> compBonus,
			HashMap<String,Integer> overCap,
			double ourAdEffect,
			double squashing,
			int numPromSlots,
			int numSlots,
			double proReserve,
			double targEffect,
			double promSlotBonus,
			Query query) {
		
		super(query);
		_agents = agents;
		_bids = bids;
		_advEffect = advEffect;
		_contProb = contProb;
		_adType = adType;
		_compbonus = compBonus;
		_overCap = overCap;
		_ourAdEffect = ourAdEffect;
		_squashing = squashing;
		_numPromSlots = numPromSlots;
		_numSlots = numSlots;
		_proReserve = proReserve;
		_targEffect = targEffect;
		_promSlotBonus = promSlotBonus;
	}

	@Override
	public double getPrediction(double info) {
		/*
		 * Click Prob Models should take in a slot
		 * and return the click probability
		 */
		double slot = (Double) info;
		if(slot > _numSlots) {
			return 0.0;
		}
		ArrayList<Double> bids = new ArrayList<Double>();
		HashMap<Double,String> bidToAdv = new HashMap<Double,String>();
		for(int i = 0; i < _agents.length; i++) {
			double advEff = _advEffect.get(_agents[i]).get(_query);
			double bid = _bids.get(_agents[i]).get(_query);
			double realbid = Math.pow(advEff,_squashing)*bid;
			bids.add(realbid);
			bidToAdv.put(realbid, _agents[i]);
		}
		
		Collections.sort(bids,Collections.reverseOrder());
		double clickprob = 1.0;
		
		for(int i = 0; i < slot; i++) {
			double bid = bids.get(i);
			String adv = bidToAdv.get(bid);
			double advEffect = _advEffect.get(adv).get(_query);
			double contProb = _contProb.get(_query);
			Ad ad = _adType.get(adv).get(_query);
			int overCap = _overCap.get(_query);
			boolean generic = ad.isGeneric();
			double ftarg = 1.0;
			if(!generic) {
				/*
				 * On average we get our target effect 1 in 9 times
				 */
				ftarg = ((1+_targEffect) + 8*(1/(1+_targEffect)))/9;
			}
			double fpro = 1.0;
			if(i < _numPromSlots && bid >= _proReserve) {
				fpro = 1 + _promSlotBonus;
			}
			double baseline = eta(advEffect,ftarg*fpro);
			baseline *= Math.pow(contProb, i);
			if(i == 0) {
				clickprob *= baseline;
				continue;
			}
			else {
				double convProb;
				if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					convProb = .1;
				}
				else if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					convProb = .2;
				}
				else if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					convProb = .3;
				}
				else {
					throw new RuntimeException("Bad QuerySpace");
				}
				convProb *= Math.pow(LAMBDA,overCap);
				clickprob = baseline*(1 - clickprob*convProb);
			}
		}
		
		return clickprob;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
		return true;
	}
	
	public double eta(double p, double x) {
		return (p*x)/(p*x + (1-p));
	}

}
