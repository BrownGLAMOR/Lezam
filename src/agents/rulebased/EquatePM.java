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

public class EquatePM extends RuleBasedAgent{
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _PM;
	protected double _alphaIncPM;
	protected double _betaIncPM;
	protected double _alphaDecPM;
	protected double _betaDecPM;
	protected double _initPM;
	
	public EquatePM() {
		this(0.7500000000000001,0.0080,-0.10000199999999998,0.010000000000000002,-0.266667);
	}


	public EquatePM(double initPM,double alphaIncPM, double betaIncPM, double alphaDecPM, double betaDecPM) {
		_alphaIncPM = alphaIncPM;
		_betaIncPM = betaIncPM;
		_alphaDecPM = alphaDecPM;
		_betaDecPM = betaDecPM;
		_initPM = initPM;
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
			 * Equate PMs
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(sum <= _dailyCapacity) {
				_PM *=  (1-(_alphaDecPM*Math.abs(sum - _dailyCapacity)  +  _betaDecPM));
			}
			else {
				_PM *=  (1+_alphaIncPM*Math.abs(sum - _dailyCapacity)  +  _betaIncPM);
			}
			
			if(Double.isNaN(_PM) || _PM <= 0) {
				_PM = _initPM;
			}
			if(_PM > 1.0) {
				_PM = 1.0;
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
		_PM = _initPM;
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

		double CPC = (1 - _PM)*rev* prConv;
		
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePM";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePM(_initPM, _alphaIncPM, _betaIncPM, _alphaDecPM, _betaDecPM);
	}

}