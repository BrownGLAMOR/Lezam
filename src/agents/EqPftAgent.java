package agents;

import java.util.HashMap;
import java.util.Set;

import agents.rules.AbstractRule;
import agents.rules.eqpft.CapacityCap;
import agents.rules.eqpft.EquateProfits;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.profits.ProfitsMovingAvg;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * 
 * @author czhang
 *
 */

public class EqPftAgent extends SimAbstractAgent {
	protected BidBundle _bidBundle;
	
	protected AbstractUnitsSoldModel _unitsSoldModel; 
	protected HashMap<Query, RevenueMovingAvg> _revenueModels;
	protected HashMap<Query, AbstractPrConversionModel> _prConversionModels;
	protected HashMap<Query, ProfitsMovingAvg> _profitsModels;
	//HashMap<Query,AbstractModel> bidToClicks;
	
	/*protected AbstractRule _capacityCap;
	protected AbstractRule _equateProfits;*/
	
	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;
	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	
	@Override
	protected void initBidder() {
		
		// set constants
		
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distributionWindow = _advertiserInfo.getDistributionWindow();
		
		// initialize strategy related variables
		
		double initProfitMargin = .2;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
		}
		
		_desiredSales = new HashMap<Query, Double>();
		
		for (Query query : _querySpace) {
			_desiredSales.put(query, (double)_distributionCapacity/_querySpace.size());
		}
		
		// initialize models
		
		_unitsSoldModel = new UnitsSoldMovingAvg(_distributionWindow);
		
		_revenueModels = new HashMap<Query, RevenueMovingAvg>(); 
		for (Query query : _querySpace) {
			_revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		_prConversionModels = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			_prConversionModels.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
		}
		
		_profitsModels = new HashMap<Query, ProfitsMovingAvg>();
		for (Query query: _querySpace) {
			double profit = _prConversionModels.get(query).getPrediction()*_revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_profitsModels.put(query, new ProfitsMovingAvg(query, profit));
		}
		
		// initialize the bid bundle

		_bidBundle = new BidBundle();
		buildBidBundle();
	}

	@Override
	protected BidBundle getBidBundle(Set<AbstractModel> models) {
		
		// update models
		_unitsSoldModel.update(_salesReport);
		
		for (Query query : _querySpace) {
			_prConversionModels.get(query).setPrediction(_unitsSoldModel.getWindowSold() - _distributionCapacity);
			
			if (_salesReport.getRevenue(query) > 0) 
				_revenueModels.get(query).update(_salesReport.getRevenue(query)/_salesReport.getConversions(query));
			
			if (_salesReport.getConversions(query) > 0)
				_profitsModels.get(query).update(_salesReport.getRevenue(query)/_salesReport.getConversions(query) - _queryReport.getCPC(query));
		}
		
		// try to equate profit
		
		double overcap = _unitsSoldModel.getWindowSold() - _distributionCapacity;
		for (Query query: _querySpace) {
			double error = 1e-2;
			if (overcap > 0) {
				double newProfitMargin = 1 - (_queryReport.getCPC(query) - error)/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction());
				if (newProfitMargin >= 1) newProfitMargin = .9;
				_profitMargins.put(query, newProfitMargin);
				
			}
			
			double latestProfit = _profitsModels.get(query).getLatestSample();
			double avgProfit = this.getAvgProfit();
			
			if ( (latestProfit > avgProfit && _queryReport.getPosition(query) <= 1.1) || 
					_queryReport.getPosition(query) == Double.NaN || 
					_queryReport.getPosition(query) >= _slotInfo.getRegularSlots() + _slotInfo.getPromotedSlots() - 1)
			{	
				if (latestProfit > avgProfit) 
					_desiredSales.put(query, _desiredSales.get(query)*1.6);
				if (_salesReport.getConversions(query) < _desiredSales.get(query)) {
					double newProfitMargin;
					if (avgProfit > 0)
						newProfitMargin = avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction());
					else newProfitMargin = _profitMargins.get(query)*0.8;
					newProfitMargin = Math.min(0.9, newProfitMargin);
					_profitMargins.put(query, newProfitMargin);
				}
				
			}
			else {
				_desiredSales.put(query, _desiredSales.get(query)*0.625);
				if (_salesReport.getConversions(query) > _desiredSales.get(query)) {
					double newProfitMargin;
					if (avgProfit > 0)
						newProfitMargin = avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction());
					else newProfitMargin = _profitMargins.get(query)*1.25;
					newProfitMargin = Math.min(0.9, newProfitMargin);
					_profitMargins.put(query, newProfitMargin);
				}
			}
			
		}
		
		return buildBidBundle();
	}
	
	protected BidBundle buildBidBundle() {
		
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Remaining Cap: ").append(_unitsSoldModel.getWindowSold()).append("\n");
		buff.append("****************\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bidBundle.getBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(_bidBundle.getDailyLimit(q)).append("\n");
			if (_salesReport.getConversions(q) > 0) 
				buff.append("\t").append("Revenue: ").append(_salesReport.getRevenue(q)/_salesReport.getConversions(q)).append("\n");
			else buff.append("\t").append("Revenue: ").append("0.0").append("\n");
			buff.append("\t").append("Predicted Revenue:").append(_revenueModels.get(q).getRevenue()).append("\n");
			if (_queryReport.getClicks(q) > 0) 
				buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)/_queryReport.getClicks(q)).append("\n");
			else buff.append("\t").append("Conversion Pr: ").append("No Clicks").append("\n");
			buff.append("\t").append("Predicted Conversion Pr:").append(_prConversionModels.get(q).getPrediction()).append("\n");
			buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)).append("\n");
			buff.append("\t").append("Desired Sales: ").append(_desiredSales.get(q)).append("\n");
			buff.append("\t").append("Profit: ").append(_profitsModels.get(q).getProfit()).append("\n");
			buff.append("\t").append("Profit Margin: ").append(_profitMargins.get(q)).append("\n");
			buff.append("\t").append("Average Profit:").append(this.getAvgProfit()).append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}
		System.out.println(buff);
		
		for (Query query : _querySpace) {
			// set bids
			_bidBundle.setBid(query, _prConversionModels.get(query).getPrediction()*_revenueModels.get(query).getRevenue()*(1 - _profitMargins.get(query)));
			
			// set spend limit
			double dailySalesLimit = Math.max(_desiredSales.get(query)*1.2/_prConversionModels.get(query).getPrediction(),1);
			double dailyLimit = _bidBundle.getBid(query)*dailySalesLimit;
			_bidBundle.setDailyLimit(query, dailyLimit);
		}
		
		return _bidBundle;
	}
	
	protected double getAvgProfit() {
		double result = 0;
		for (Query query : _querySpace) 
			result += _profitsModels.get(query).getProfit();
		result /= _querySpace.size();
		return result;
	}

	@Override
	protected Set<AbstractModel> initModels() {
		// Not used
		return null;
	}

	@Override
	protected void updateModels(SalesReport salesReport,
			QueryReport queryReport, Set<AbstractModel> models) {
		// Not used		
	}


}
