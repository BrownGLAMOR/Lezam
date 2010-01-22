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

public class EquatePR extends RuleBasedAgent{
	
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _PR;
	protected double _alphaIncPR;
	protected double _betaIncPR;
	protected double _alphaDecPR;
	protected double _betaDecPR;
	protected double _initPR;
	
	public EquatePR() {
		this(3.300000000000001,0.0010,-0.13333499999999998,0.0020,-0.266667);
	}

	public EquatePR(double initPR,double alphaIncPR,double betaIncPR,double alphaDecPR,double betaDecPR) {
		_alphaIncPR = alphaIncPR;
		_betaIncPR = betaIncPR;
		_alphaDecPR = alphaDecPR;
		_betaDecPR = betaDecPR;
		_initPR = initPR;
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
				_PR *=  (1-(_alphaDecPR*Math.abs(sum - _dailyCapacity)  +  _betaDecPR));
			}
			else {
				_PR *=  (1+_alphaIncPR*Math.abs(sum - _dailyCapacity)  +  _betaIncPR);
			}
			
			if(Double.isNaN(_PR)) {
				_PR = _initPR;
			}
			if(_PR <= 1.0) {
				_PR = 1.0;
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

		_PR = _initPR;
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
		double CPC = (rev * prConv)/_PR;
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePR";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePR(_initPR, _alphaIncPR, _betaIncPR, _alphaDecPR, _betaDecPR);
	}

}