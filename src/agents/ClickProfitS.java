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


public class ClickProfitS extends RuleBasedAgent {
	protected BidBundle _bidBundle;
	protected HashMap<Query, RevenueMovingAvg> _revenueModels;
	protected BasicTargetModel _targetModel;
	protected double _avgProfit;
	
	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;
	
	protected final double _errorOfConversions = 2;
	protected final double _errorOfProfit = .1;
	protected final double _errorOfLimit = .1;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = false;

	
	@Override
	public void initBidder() {		
		super.initBidder();
		setDailyQueryCapacity();
		
		double initProfitMargin = .75;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
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
		
		if(_salesReport == null || _queryReport == null) {
			return new BidBundle();
		}
		

		
		this.setAvgProfit();
		
		// try to equate profit
		
		for (Query query: _querySpace) {
			
			double latestProfit = 0;
			if (_queryReport.getClicks(query) > 0)
				latestProfit = (_salesReport.getRevenue(query) - _queryReport.getCost(query))/_salesReport.getConversions(query);
			
			if (latestProfit > _avgProfit) {	
				if (_salesReport.getConversions(query) > _desiredSales.get(query) && _queryReport.getPosition(query) > 1.5) 
					_desiredSales.put(query, _desiredSales.get(query)*1.25);
			}
			else if  (_queryReport.getClicks(query) > 0) {
				if (_salesReport.getConversions(query) < _desiredSales.get(query))
					_desiredSales.put(query, _desiredSales.get(query)*0.8);
			}
		}
		
		// normalize desiredSales
		double normalizeFactor = 0;
		for (Query query : _querySpace) {
			normalizeFactor += _desiredSales.get(query);
		}
		normalizeFactor = _capacity*1.25/_capWindow/normalizeFactor;
		for (Query query : _querySpace) {
			_desiredSales.put(query, _desiredSales.get(query)*normalizeFactor);
		}
		
		for (Query query: _querySpace) {
		
			if (_salesReport.getConversions(query) + _errorOfConversions < _desiredSales.get(query) &&
					!(_queryReport.getPosition(query) <= 1.5)) {
				double newProfitMargin = _profitMargins.get(query)*0.9;
				newProfitMargin = Math.min(0.9, newProfitMargin);
				newProfitMargin = Math.max(0.1, newProfitMargin);
				_profitMargins.put(query, newProfitMargin);
			}
			else if (_salesReport.getConversions(query) - _errorOfConversions > _desiredSales.get(query)) {
				double newProfitMargin = _profitMargins.get(query)*1.1;
				newProfitMargin = Math.min(0.9, newProfitMargin);
				newProfitMargin = Math.max(0.1, newProfitMargin);
				_profitMargins.put(query, newProfitMargin);
			}
			
		}
		
		return buildBidBundle();
	}
	
	protected BidBundle buildBidBundle()  {
		
		_bidBundle = new BidBundle();
		
		for (Query query : _querySpace) {
			// set bids
			
			double targetCPC = getTargetCPC(query);
			double bid = _CPCToBidModel.getPrediction(query, targetCPC);
			if(Double.isNaN(bid)) {
				bid = targetCPC;
			}
			_bidBundle.setBid(query, _CPCToBidModel.getPrediction(query, targetCPC));
			
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
		
		double dailySalesLimit = Math.max(_desiredSales.get(q)/prConv,2);
		return targetCPC * dailySalesLimit*1.1;
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
		return "ClickProfS";
	}

}

