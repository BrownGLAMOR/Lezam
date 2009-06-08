package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.profits.ProfitsMovingAvg;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
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
	protected double _avgProfit;
	
	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;
	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	
	protected double overcap;
	
	// for debug
	protected PrintStream output;
	
	@Override
	public void initBidder() {
		
		// set constants
		
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distributionWindow = _advertiserInfo.getDistributionWindow();
		
		// initialize strategy related variables
		
		double initProfitMargin = .8;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
		}
		
		_desiredSales = new HashMap<Query, Double>();
		
		for (Query query : _querySpace) {
			_desiredSales.put(query, 1.0*_distributionCapacity/_querySpace.size());
		}
		
		// initialize models
		
		_unitsSoldModel = new UnitsSoldMovingAvg(_distributionCapacity, _distributionWindow);
		
		_revenueModels = new HashMap<Query, RevenueMovingAvg>(); 
		for (Query query : _querySpace) {
			_revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		_prConversionModels = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			_prConversionModels.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
		}
		
		_profitsModels = new HashMap<Query, ProfitsMovingAvg>();
		_avgProfit = 0;
		for (Query query: _querySpace) {
			//double profit = _prConversionModels.get(query).getPrediction()*_revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			double profit = _prConversionModels.get(query).getPrediction(0)*_revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_profitsModels.put(query, new ProfitsMovingAvg(query, profit));
			_avgProfit += profit;
		}
		_avgProfit /= _querySpace.size();
		
		// setup the debug info recorder
		
		try {
			output = new PrintStream(new File("log.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		// initialize the bid bundle
		
		_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		// update models
		if (_salesReport != null) _unitsSoldModel.update(_salesReport);
		overcap = _unitsSoldModel.getWindowSold() - _distributionCapacity; 
		
		for (Query query : _querySpace) {
			//_prConversionModels.get(query).setPrediction(_unitsSoldModel.getWindowSold() - _distributionCapacity);
			
			if (_salesReport != null && _salesReport.getRevenue(query) > 0) 
				_revenueModels.get(query).update(_salesReport.getRevenue(query)/_salesReport.getConversions(query));
			
			if (_salesReport != null && _salesReport.getConversions(query) > 0)
				_profitsModels.get(query).update(_salesReport.getRevenue(query)/_queryReport.getClicks(query) - _queryReport.getCPC(query));
			else _profitsModels.get(query).update(_prConversionModels.get(query).getPrediction(overcap)*_revenueModels.get(query).getRevenue()*.9);
		}
		
		this.setAvgProfit();
		
		// try to equate profit
		
		//double remainingCap = _distributionCapacity - _unitsSoldModel.getWindowSold();
		for (Query query: _querySpace) {
			
			double latestProfit = _profitsModels.get(query).getLatestSample();
			double avgProfit = this.getAvgProfit();
			
			if (latestProfit > avgProfit) {	
				if (_salesReport.getConversions(query) > _desiredSales.get(query)) 
					_desiredSales.put(query, _desiredSales.get(query)*2);
			}
			else {
				if (_salesReport.getConversions(query) < _desiredSales.get(query))
					_desiredSales.put(query, _desiredSales.get(query)*0.5);
			}
			
			if (_unitsSoldModel.getEstimate() < .8*_distributionCapacity/_distributionWindow
				||(_unitsSoldModel.getEstimate() < 1.5*_distributionCapacity/_distributionWindow && _salesReport.getConversions(query) < _desiredSales.get(query))
				|| _queryReport.getPosition(query) == Double.NaN) {
				double newProfitMargin;
				if (latestProfit > avgProfit)
					//newProfitMargin = Math.min(avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction()),
					//						_profitMargins.get(query)*0.9);
					newProfitMargin = Math.min(avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction(overcap)),_profitMargins.get(query)*0.9);
				else newProfitMargin = _profitMargins.get(query)*0.9;
				newProfitMargin = Math.min(0.9, newProfitMargin);
				newProfitMargin = Math.max(0.01, newProfitMargin);
				_profitMargins.put(query, newProfitMargin);
			}
			else if (_unitsSoldModel.getEstimate() > 1.2*_distributionCapacity/_distributionWindow || 
					(_unitsSoldModel.getEstimate() > .5*_distributionCapacity/_distributionWindow && _salesReport.getConversions(query) > _desiredSales.get(query))) {
				double newProfitMargin;
				if (latestProfit < avgProfit)
					//newProfitMargin = Math.max(avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction()),
					//						_profitMargins.get(query)*1.1);
					newProfitMargin = Math.max(avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModels.get(query).getPrediction(overcap)),_profitMargins.get(query)*1.1);
				else newProfitMargin = _profitMargins.get(query)*1.3;
				newProfitMargin = Math.min(0.9, newProfitMargin);
				newProfitMargin = Math.max(0.01, newProfitMargin);
				_profitMargins.put(query, newProfitMargin);
			}
			
		}
		
		// normalize desiredSales
		double normalizeFactor = 0;
		for (Query query : _querySpace) {
			normalizeFactor += _desiredSales.get(query);
		}
		normalizeFactor = _distributionCapacity*1.0/_distributionWindow/normalizeFactor;
		for (Query query : _querySpace) {
			_desiredSales.put(query, _desiredSales.get(query)*normalizeFactor);
		}
		
		return buildBidBundle();
	}
	
	protected BidBundle buildBidBundle()  {
		
		for (Query query : _querySpace) {
			// set bids
			_bidBundle.setBid(query, _prConversionModels.get(query).getPrediction(overcap)*_revenueModels.get(query).getRevenue()*(1 - _profitMargins.get(query)));
			
			// set spend limit
			//double dailySalesLimit = Math.max(_desiredSales.get(query)/_prConversionModels.get(query).getPrediction(),1);
			double dailySalesLimit = Math.max(_desiredSales.get(query)/_prConversionModels.get(query).getPrediction(overcap),1);
			double dailyLimit = _bidBundle.getBid(query)*dailySalesLimit*1.1;
			_bidBundle.setDailyLimit(query, dailyLimit);
		}
		
		this.printInfo();
		
		return _bidBundle;
	}
	
	protected void setAvgProfit() {
		double result = 0;
		int n = 0;
		for (Query query : _querySpace) 
			if (_salesReport.getConversions(query) > 0) {
				result += _profitsModels.get(query).getProfit()*_salesReport.getConversions(query);
				n += _salesReport.getConversions(query);
			}
		if  (n > 1.0*_distributionCapacity/_distributionWindow) {
			result /= n;
		}
		else result /= 1.0*_distributionCapacity/_distributionWindow;
		_avgProfit = .125*result+.875*_avgProfit;
	}
	
	protected double getAvgProfit() {
		return _avgProfit;
	}
	
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Window Sold: ").append(_unitsSoldModel.getWindowSold()).append("\n");
		buff.append("\t").append("Distribution Cap: ").append(_distributionCapacity).append("\n");
		buff.append("\t").append("Yesterday sold: ").append(_unitsSoldModel.getLatestSample()).append("\n");
		buff.append("\t").append("Estimated sold: ").append(_unitsSoldModel.getEstimate()).append("\n");
		buff.append("\t").append("Target: ").append(1.0*_distributionCapacity/_distributionWindow).append("\n");
		buff.append("\t").append("Manufacturer specialty: ").append(_advertiserInfo.getManufacturerSpecialty()).append("\n");
		buff.append("****************\n");
		for(Query q : _querySpace){
			buff.append("\t").append("Day: ").append(_day).append("\n");
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bidBundle.getBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(_bidBundle.getDailyLimit(q)).append("\n");
			if (_salesReport.getConversions(q) > 0) 
				buff.append("\t").append("Revenue: ").append(_salesReport.getRevenue(q)/_salesReport.getConversions(q)).append("\n");
			else buff.append("\t").append("Revenue: ").append("0.0").append("\n");
			buff.append("\t").append("Predicted Revenue:").append(_revenueModels.get(q).getRevenue()).append("\n");
			if (_queryReport.getClicks(q) > 0) 
				buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)*1.0/_queryReport.getClicks(q)).append("\n");
			else buff.append("\t").append("Conversion Pr: ").append("No Clicks").append("\n");
			//buff.append("\t").append("Predicted Conversion Pr:").append(_prConversionModels.get(q).getPrediction()).append("\n");
			buff.append("\t").append("Predicted Conversion Pr:").append(_prConversionModels.get(q).getPrediction(overcap)).append("\n");
			buff.append("\t").append("Conversions: ").append(_salesReport.getConversions(q)).append("\n");
			buff.append("\t").append("Desired Sales: ").append(_desiredSales.get(q)).append("\n");
			buff.append("\t").append("Profit: ").append(_profitsModels.get(q).getProfit()).append("\n");
			buff.append("\t").append("Profit Margin: ").append(_profitMargins.get(q)).append("\n");
			buff.append("\t").append("Average Profit:").append(this.getAvgProfit()).append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}
		
		System.out.println(buff);
		output.append(buff);
		output.flush();
	
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
