package agents;


import java.util.HashMap;
import java.util.Set;
import newmodels.AbstractModel;
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
	protected double _decPPS;
	protected double _incPPS;
	protected double _minPPs;
	protected double _maxPPS;
	protected double _initPPS;
	
	public EquatePPS(double initPPS,double decPPS, double incPPS, double minPPS, double maxPPS, double budgetModifier) {
		_decPPS = decPPS;
		_incPPS = incPPS;
		_minPPs = minPPS;
		_maxPPS = maxPPS;
		_initPPS = initPPS;
		_budgetModifier = budgetModifier;
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

			if(sum <= _dailyCapacity*_decPPS) {
				_PPS = Math.max(_minPPs, _PPS * _decPPS);
			}
			else {
				_PPS = Math.min(_maxPPS, _PPS * _incPPS);
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
		
		return (rev - _PPS)* prConv;
	}
 
	@Override
	public String toString() {
		return "EquatePPS";
	}
	
	@Override
	public AbstractAgent getCopy() {
		return new EquatePPS(_initPPS, _decPPS, _incPPS, _minPPs, _maxPPS, _budgetModifier);
	}

}