package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustPM extends RuleBasedAgent {

	protected HashMap<Query, Double> _revenue;
	protected HashMap<Query, Double> _PM;

	protected BidBundle _bidBundle;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = true;
	private double goodslot = 3;
	private double badslot = 2;
	private Double decPM = .8;
	private Double incPM = 1.2;
	private double minPM = .4;
	private double maxPM = .8;
	private double initPM = .4;
	
	public AdjustPM() {
		budgetModifier = 1.25;
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();
		
		if(_day < 2) { 
			_bidBundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}
		
		for (Query query : _querySpace) {
			if(_day > 1) {
				adjustPM(query);
			}
			
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

		_PM = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PM.put(q, initPM);
		}

		_revenue = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			if (query.getManufacturer() == _manSpecialty)
				_revenue.put(query, 15.0);
			else
				_revenue.put(query, 10.0);
		}
	}

	protected void adjustPM(Query q) {
		double tmp = _PM.get(q);
		// if we does not get enough clicks (and bad position), then decrease PM
		// (increase bids, and hence slot)
		if (_salesReport.getConversions(q) >= _dailyQueryCapacity) {
			tmp = _PM.get(q) * incPM;
			tmp = Math.min(maxPM, tmp);
		} else if(_salesReport.getConversions(q) < _dailyQueryCapacity) {
			// if we get too many clicks (and good position), increase
			// PM(decrease bids and hence slot)
			tmp = _PM.get(q) * decPM;
			tmp = Math.max(minPM, tmp);
		}
		_PM.put(q, tmp);
	}

	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return _revenue.get(q)*(1 - _PM.get(q))*conversion;
	}

	@Override
	public String toString() {
		return "AdjustPM";
	}

}
