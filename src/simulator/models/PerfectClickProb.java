package simulator.models;

/*
 * DISCOUONT FOR IS USERS
 */

/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import simulator.AgentBidPair;
import simulator.SimAgent;
import usermodel.UserState;

import newmodels.AbstractModel;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectClickProb extends AbstractSlotToPrClick {
	
	/*
	 * TODO
	 * Change to make sure that our bids are not in the bids array!!!
	 * 
	 * Make overcap reflect what we are passing it
	 * 
	 * Figure out some way of estimating how overcapacity each person is based on how over capacity they are now
	 * (we will probably need more data, specifically there actual daily alotted capacity (_capacity/window), and 
	 * the sales from 5 days ago
	 * 
	 */
	
	private double LAMBDA = .995;
	private String[] _agents;
	private double[] _clickPr;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<Query, Double> _contProb;
	private HashMap<String, HashMap<Query, Ad>> _adType;
	private HashMap<String, String> _compSpecialties;
	private HashMap<String, Integer[]> _salesOverWindow;
	private double _ourAdEffect;
	private double _squashing;
	private int _numSlots;
	private int _numPromSlots;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	private int _ourAdvIdx;
	private HashMap<String, Integer> _capacities;
	private AbstractUserModel _userModel;
	private RetailCatalog _retailCatalog;
	private PerfectQueryToNumImp _queryToNumImp;
	
	public PerfectClickProb(String[] agents,
			HashMap<String,HashMap<Query,Double>> bids,
			HashMap<String,HashMap<Query,Double>> advEffect,
			HashMap<Query,Double> contProb,
			HashMap<String,HashMap<Query,Ad>> adType,
			HashMap<String,String> compSpecialties,
			HashMap<String, Integer[]> salesOverWindow,
			HashMap<String, Integer> capacities,
			double ourAdEffect,
			double squashing,
			int numPromSlots,
			int numSlots,
			double proReserve,
			double targEffect,
			double promSlotBonus,
			int ourAdvIdx,
			RetailCatalog retailCatalog,
			PerfectQueryToNumImp queryToNumImp,
			PerfectUserModel userModel,
			Query query) {
		
		super(query);
		_agents = agents;
		_bids = bids;
		_advEffect = advEffect;
		_contProb = contProb;
		_adType = adType;
		_compSpecialties = compSpecialties;
		_salesOverWindow = salesOverWindow;
		_capacities = capacities;
		_ourAdEffect = ourAdEffect;
		_squashing = squashing;
		_numPromSlots = numPromSlots;
		_numSlots = numSlots;
		_proReserve = proReserve;
		_targEffect = targEffect;
		_promSlotBonus = promSlotBonus;
		_ourAdvIdx = ourAdvIdx;
		_retailCatalog = retailCatalog;
		_queryToNumImp = queryToNumImp;
		_userModel = userModel;
		_clickPr = new double[_numSlots];
		initializeProbablities();
	}

	private void initializeProbablities() {
		double contProb = _contProb.get(_query);
		_clickPr[0] = _ourAdEffect;

		ArrayList<Double> realBids = new ArrayList<Double>();
		HashMap<Double,String> bidToAdv = new HashMap<Double,String>();
		for(int i = 0; i < _agents.length; i++) {
			if(i != _ourAdvIdx) {
				double advEff = _advEffect.get(_agents[i]).get(_query);
				double bid = _bids.get(_agents[i]).get(_query);
				double realbid = Math.pow(advEff,_squashing)*bid;
				realBids.add(realbid);
				bidToAdv.put(realbid, _agents[i]);
			}
		}
		
		Collections.sort(realBids,Collections.reverseOrder());
		

		double baselineconv;
		if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			baselineconv = .1;
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			baselineconv = .2;
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			baselineconv = .3;
		}
		else {
			throw new RuntimeException("Bad Query");
		}
		int numISUsers = 0;
		for(Product product : _retailCatalog) {
			if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				numISUsers += _userModel.getPrediction(product, UserState.IS) / 3;
			}
			else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
    			if(product.getComponent().equals(_query.getComponent()) || product.getManufacturer().equals(_query.getManufacturer())) {
    				numISUsers += _userModel.getPrediction(product, UserState.IS) / 6;
    			}
			}
			else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
    			if(product.getComponent().equals(_query.getComponent()) && product.getManufacturer().equals(_query.getManufacturer())) {
    				numISUsers += _userModel.getPrediction(product, UserState.IS) / 3;
    			}
			}
		}
		int numImps = _queryToNumImp.getPrediction(_query);
		double ISUserDiscount = 1 - numISUsers/numImps;
		baselineconv *= ISUserDiscount;
		
		String advertiser = bidToAdv.get(realBids.get(0));

		Ad ad = _adType.get(advertiser).get(_query);
		double bid = _bids.get(advertiser).get(_query);
		double ftarg = 1.0;
		if(ad != null && !ad.isGeneric()) {
			/*
			 * On average we get our target effect 1 in 9 times
			 */
			ftarg = ((1+_targEffect) + 8*(1/(1+_targEffect)))/9;
		}
		double fpro = 1.0;
		if(_numPromSlots > 0 && bid >= _proReserve) {
			fpro = 1 + _promSlotBonus;
		}
		
		double prevClickPr = eta(_advEffect.get(advertiser).get(_query),ftarg*fpro);
		
		Integer[] sales = _salesOverWindow.get(advertiser);
		double prevConvs = 0;
		int avgSales = 0;
		for(int j = 0; j < sales.length; j++) {
			avgSales += sales[j];
			if(j != sales.length-1) {
				prevConvs += sales[j];
			}
		}
		avgSales /= sales.length;
		double avgLambda = 0.0;
		for(int j = 0; j < avgSales; j++) {
			avgLambda += Math.pow(LAMBDA,prevConvs + avgSales - _capacities.get(advertiser));
		}
		avgLambda /= avgSales;
		double compBonus = 1;
		if(_compSpecialties.get(advertiser).equals(_query.getComponent())) {
			compBonus += _targEffect;
		}
		double prevConvPr = eta(baselineconv*avgLambda,compBonus);

		for(int i = 1; i < _numSlots; i++) {
			advertiser = bidToAdv.get(realBids.get(i));

			ad = _adType.get(advertiser).get(_query);
			bid = _bids.get(advertiser).get(_query);
			ftarg = 1.0;
			fpro = 1.0;
			if(i < _numSlots && bid >= _proReserve) {
				fpro = 1 + _promSlotBonus;
			}
			
			double clickPr = eta(_advEffect.get(advertiser).get(_query),ftarg*fpro)*Math.pow(contProb, i)*(1-prevConvPr*prevClickPr);
			prevClickPr = clickPr;
			_clickPr[i] = clickPr;
			
			sales = _salesOverWindow.get(advertiser);
			prevConvs = 0;
			avgSales = 0;
			for(int j = 0; j < sales.length; j++) {
				avgSales += sales[j];
				if(j != sales.length-1) {
					prevConvs += sales[j];
				}
			}
			avgSales /= sales.length;
			avgLambda = 0.0;
			for(int j = 0; j < avgSales; j++) {
				avgLambda += Math.pow(LAMBDA,prevConvs + avgSales - _capacities.get(advertiser));
			}
			avgLambda /= avgSales;
			compBonus = 1;
			if(_compSpecialties.get(advertiser).equals(_query.getComponent())) {
				compBonus += _targEffect;
			}
			prevConvPr = eta(baselineconv*avgLambda,compBonus);
		}
	}

	@Override
	public double getPrediction(double slot) {
		slot -= 1;
		int min = (int) Math.floor(slot);
		int max = (int) Math.ceil(slot);
		if(min == max) {
			return _clickPr[min];
		}
		else {
			double avg = (slot - min) * _clickPr[min] + (max - slot) * _clickPr[max];
			return avg;
		}
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
