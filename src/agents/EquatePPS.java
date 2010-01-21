package agents;


import java.util.HashMap;
import java.util.Set;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class EquatePPS extends RuleBasedAgent{
	protected HashMap<Query, Double> _prClick;
	protected BidBundle _bidBundle;
	protected boolean TARGET = false;
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

			if (TARGET) {
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getComponent() == null)
					_bidBundle.setAd(query, new Ad(new Product(query.getManufacturer(), _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getManufacturer() == null)
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO) && query.getManufacturer().equals(_manSpecialty)) 
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
			}
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

		_prClick = new HashMap<Query, Double>();
		for (Query query: _querySpace) {
			_prClick.put(query, .01);
		}

	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		super.updateModels(salesReport, queryReport);
		if (_day > 1 && salesReport != null && queryReport != null) {	
			for (Query query: _querySpace) {
				if (queryReport.getImpressions(query) > 0) _prClick.put(query, queryReport.getClicks(query)*1.0/queryReport.getImpressions(query)); 
			}

		}
	}

	protected double getTargetCPC(Query q){		

		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);

		double rev = _salesPrices.get(q);

		if (TARGET) {
			double clickPr = _prClick.get(q);
			if (clickPr <=0 || clickPr >= 1) clickPr = .5;
			prConv = _targetModel.getConvPrPrediction(q, clickPr, prConv, 0);
			rev = _targetModel.getUSPPrediction(q, clickPr, 0);
		}

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