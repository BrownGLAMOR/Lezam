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

public class EquateROI extends RuleBasedAgent{
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _ROI;
	protected double _alphaIncROI;
	protected double _betaIncROI;
	protected double _alphaDecROI;
	protected double _betaDecROI;
	protected double _initROI;
	
	public EquateROI() {
		this(3.100000000000001,0.0040,-0.03333599999999998,0.010000000000000002,-0.266667);
	}

	public EquateROI(double initROI,double alphaIncROI,double betaIncROI,double alphaDecROI,double betaDecROI) {
		_alphaIncROI = alphaIncROI;
		_betaIncROI = betaIncROI;
		_alphaDecROI = alphaDecROI;
		_betaDecROI = betaDecROI;
		_initROI = initROI;
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
				_ROI *=  (1-(_alphaDecROI*Math.abs(sum - _dailyCapacity)  +  _betaDecROI));
			}
			else {
				_ROI *=  (1+_alphaIncROI*Math.abs(sum - _dailyCapacity)  +  _betaIncROI);
			}
			
			if(Double.isNaN(_ROI) || _ROI <= 0.0) {
				_ROI = _initROI;
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
		_ROI = _initROI;
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
		double CPC = (rev * prConv)/(_ROI+1.0);
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquateROI";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquateROI(_initROI, _alphaIncROI, _betaIncROI, _alphaDecROI, _betaDecROI);
	}

}