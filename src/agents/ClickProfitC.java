/**
 * Used to be EqpftAgent 
 */

package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.revenue.RevenueMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;


public class ClickProfitC extends RuleBasedAgent {
	protected BidBundle _bidBundle;

	protected HashMap<Query, RevenueMovingAvg> _revenueModels;

	protected double _avgProfit;
	protected double _oldAvgProfit;

	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;

	protected ArrayList<BidBundle> _bidBundleList;

	protected final int MAX_TIME_HORIZON = 5;
	protected final double _errorOfConversions = 2;
	protected final double _errorOfProfit = .1;
	protected final double _errorOfLimit = .1;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = true;

	@Override
	public void initBidder() {
		setDailyQueryCapacity();

		// initialize strategy related variables

		double initProfitMargin = .7;
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

		_baselineConversion = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			double conv = 0;
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				conv = .1;
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				conv = .2;
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				conv = .3;

			double componentBonus = 1;
			if (query.getComponent() != null && query.getComponent().equals(_advertiserInfo.getComponentSpecialty()))
				componentBonus = _advertiserInfo.getComponentBonus();
			conv = (conv*componentBonus)/(componentBonus + 1 - conv);
			_baselineConversion.put(query, conv);
		}



		_avgProfit = 0;
		for (Query query: _querySpace) {
			double profit = _baselineConversion.get(query)*_revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_avgProfit += profit;
		}
		_avgProfit /= _querySpace.size();
		_oldAvgProfit = _avgProfit;

		// initialize the bid bundle

		_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);

		this.setAvgProfit();

		// try to equate profit
		if(_day > 1) {

			for (Query query: _querySpace) {

				//double latestProfit = _profitsModels.get(query).getLatestSample();
				double latestProfit = 0;
				if (_queryReport.getClicks(query) > 0)
					latestProfit = (_salesReport.getRevenue(query) - _queryReport.getCost(query))/_queryReport.getClicks(query);

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
		}
		return buildBidBundle();
	}

	protected BidBundle buildBidBundle()  {

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

		//this.printInfo();

		_bidBundleList.add(_bidBundle);

		return _bidBundle;
	}
	
	@Override
	protected double getDailySpendingLimit(Query q, double targetCPC) {
		double conversion;
		if (_day <= 6 || !(_conversionPrModel.getPrediction(q) > 0))
			conversion = _baselineConversion.get(q);
		else conversion= _conversionPrModel.getPrediction(q);
		
		double dailySalesLimit = Math.max(_desiredSales.get(q)/conversion,2);
		return targetCPC * dailySalesLimit * 1.1;
	}
	
	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6 || !(_conversionPrModel.getPrediction(q) > 0))
			conversion = _baselineConversion.get(q);
		else conversion= _conversionPrModel.getPrediction(q);
		
		return conversion *_revenueModels.get(q).getRevenue()*(1 - _profitMargins.get(q));
	}

	protected void setAvgProfit() {
		double result = 0;
		int n = 0;
		for (Query query : _querySpace) 
			if (_salesReport != null && _salesReport.getConversions(query) > 0) {
				result += _salesReport.getRevenue(query) - _queryReport.getCost(query);
				n += _queryReport.getClicks(query);
			}
		result /= n;

		_oldAvgProfit = _avgProfit;
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
				if (salesReport.getRevenue(query) > 0) 
					_revenueModels.get(query).update(salesReport.getRevenue(query)/salesReport.getConversions(query));
			}
		}

	}
	@Override
	public String toString() {
		return "ClickProfC";
	}

}
