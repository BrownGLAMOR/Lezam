/**
 * Used to be EqpftAgent2
 * Used to be CH2Agent
 */
package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.targeting.BasicTargetModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;


public class PPSBidder extends RuleBasedAgent {
	protected BidBundle _bidBundle;
	protected HashMap<Query, RevenueMovingAvg> _revenueModels;
	protected double _avgProfit;

	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;

	protected final double _errorOfConversions = 2;
	protected final double _errorOfProfit = .1;
	protected final double _errorOfLimit = .1;
	protected boolean TARGET = false;
	protected boolean BUDGET = true;
	private double incTS = 1.2;
	private double decTS = .8;
	private double goodslot = 3;
	private double badslot = 2;
	private Double decPM = .8;
	private Double incPM = 1.2;
	private double minPM = .4;
	private double maxPM = .8;
	private double initPM = .4;
	
	public PPSBidder() {
		budgetModifier = 1.0;
	}
	

	@Override
	public void initBidder() {		
		super.initBidder();
		setDailyQueryCapacity();

		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initPM);
		}

		_desiredSales = new HashMap<Query, Double>();

		for (Query query : _querySpace) {
			_desiredSales.put(query, 1.0*_capacity/_querySpace.size());
		}

		// initialize models

		_revenueModels = new HashMap<Query, RevenueMovingAvg>(); 
		for (Query query : _querySpace) {
			_revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}

		_avgProfit = 0;
		for (Query query: _querySpace) {
			double profit = _revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_avgProfit += profit;
		}
		_avgProfit /= _querySpace.size();

	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);

		if(_day < 2) { 
			_bidBundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}


		this.setAvgProfit();

		// try to equate profit

		for (Query query: _querySpace) {

			double latestProfit = 0;
			if (_queryReport.getClicks(query) > 0)
				latestProfit = (_salesReport.getRevenue(query) - _queryReport.getCost(query))/_salesReport.getConversions(query);

			if (latestProfit > _avgProfit) {	
				_desiredSales.put(query, _desiredSales.get(query)*incTS);
			}
			else if  (_queryReport.getClicks(query) > 0) {
				if (_salesReport.getConversions(query) < _desiredSales.get(query)) {
					_desiredSales.put(query, _desiredSales.get(query)*decTS);
				}
			}
		}

		// normalize desiredSales
		double normalizeFactor = 0;
		for (Query query : _querySpace) {
			normalizeFactor += _desiredSales.get(query);
		}
		normalizeFactor = _dailyCapacity/normalizeFactor;
		for (Query query : _querySpace) {
			_desiredSales.put(query, _desiredSales.get(query)*normalizeFactor);
		}

		for(Query q : _querySpace) {
			adjustPM(q);
		}

		return buildBidBundle();
	}

	protected BidBundle buildBidBundle()  {

		_bidBundle = new BidBundle();

		for (Query query : _querySpace) {
			// set bids

			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, targetCPC+.01);

			// set target ads
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

			// set spend limit
			// must set spend limit in the first few days 
			if (BUDGET || _day < 10) {
				_bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));
			}

		}

		return _bidBundle;
	}

	@Override
	protected double getDailySpendingLimit(Query q, double targetCPC) {
		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);

		double dailySalesLimit = Math.max(_desiredSales.get(q)/prConv,3);
		return targetCPC * dailySalesLimit*budgetModifier;
	}

	protected double getTargetCPC(Query q) {
		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);

		double rev = _revenueModels.get(q).getRevenue();
		return prConv*rev*(1 - _profitMargins.get(q));
	}

	protected void setAvgProfit() {
		double result = 0;
		int n = 0;
		for (Query query : _querySpace) 
			if (_salesReport != null && _salesReport.getConversions(query) > 0) {
				result += _salesReport.getRevenue(query) - _queryReport.getCost(query);
				n += _salesReport.getConversions(query);
			}
		result /= n;

		_avgProfit = result;
	}

	protected double getAvgProfit() {
		return _avgProfit;
	}


	protected void adjustPM(Query q) {
		double tmp = _profitMargins.get(q);
		// if we does not get enough clicks (and bad position), then decrease PM
		// (increase bids, and hence slot)
		if (_salesReport.getConversions(q) >= _desiredSales.get(q) && _queryReport.getPosition(q) < goodslot) {
			tmp = _profitMargins.get(q) * incPM;
			tmp = Math.min(maxPM, tmp);
		} else if(_salesReport.getConversions(q) < _desiredSales.get(q) && _queryReport.getPosition(q) >= badslot) {
			// if we get too many clicks (and good position), increase
			// PM(decrease bids and hence slot)
			tmp = _profitMargins.get(q) * decPM;
			tmp = Math.max(minPM, tmp);
		}
		_profitMargins.put(q, tmp);
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		super.updateModels(salesReport, queryReport);

		if (_day > 1 && salesReport != null && queryReport != null) {

			for (Query query : _querySpace) {
				if (salesReport.getConversions(query) > 0)
					_revenueModels.get(query).update(salesReport.getRevenue(query)/salesReport.getConversions(query));
			}
		}

	}

	@Override
	public String toString() {
		return "QualBidder";
	}

}

