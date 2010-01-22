package agents.rulebased;


import java.util.HashMap;
import java.util.Set;

import agents.AbstractAgent;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class EquatePPS extends RuleBasedAgent{
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _PPS;
	protected double _alphaIncPPS;
	protected double _betaIncPPS;
	protected double _alphaDecPPS;
	protected double _betaDecPPS;
	protected double _initPPS;
	
	public EquatePPS() {
		this(12.0,0.0050,-0.23333399999999999,0.0,0.09999600000000003);
	}

	public EquatePPS(double initPPS,double alphaIncPPS, double betaIncPPS, double alphaDecPPS, double betaDecPPS) {
		_alphaIncPPS = alphaIncPPS;
		_betaIncPPS = betaIncPPS;
		_alphaDecPPS = alphaDecPPS;
		_betaDecPPS = betaDecPPS;
		_initPPS = initPPS;
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();
		if(_day < 2) { 
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}

		if (_day > 1 && _salesReport != null && _queryReport != null) {
			/*
			 * Equate PPS
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(sum <= _dailyCapacity) {
				_PPS *=  (1-(_alphaDecPPS*Math.abs(sum - _dailyCapacity)  +  _betaDecPPS));
			}
			else {
				_PPS *=  (1+_alphaIncPPS*Math.abs(sum - _dailyCapacity)  +  _betaIncPPS);
			}
			
			if(Double.isNaN(_PPS) || _PPS <= 0) {
				_PPS = _initPPS;
			}
			if(_PPS > 15.0) {
				_PPS = 15.0;
			}
		}

		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, targetCPC+.01);
		}

		if(BUDGET) {
			_bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
		}

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();

		_PPS = _initPPS;
	}

	protected double getTargetCPC(Query q){		

		double prConv;
		if(_day <= 6) {
			prConv = _baselineConversion.get(q);
		}
		else {
			prConv = _conversionPrModel.getPrediction(q);
		}

		double rev = _salesPrices.get(q);
		double CPC = (rev - _PPS)* prConv;
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePPS(_initPPS, _alphaIncPPS, _betaIncPPS, _alphaDecPPS, _betaDecPPS);
	}

}