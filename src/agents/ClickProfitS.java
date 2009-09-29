/**
 * Used to be EqpftAgent2
 * Used to be CH2Agent
 */
package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.profits.ProfitsMovingAvg;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.targeting.BasicTargetModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;


public class ClickProfitS extends AbstractAgent {
	protected BidBundle _bidBundle;
	protected HashMap<Query, RevenueMovingAvg> _revenueModels;
	protected AbstractConversionModel _prConversionModel;
	protected HashMap<Query, Double> _baselineConv;
	protected BasicTargetModel _targetModel;
	protected double _avgProfit;
	
	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;
	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	
	protected int _timeHorizon;
	protected final int MAX_TIME_HORIZON = 5;
	protected final double _errorOfConversions = 2;
	protected final double _errorOfProfit = .1;
	protected final double _errorOfLimit = .1;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = false;

	
	// for debug
	protected PrintStream output;
	
	@Override
	public void initBidder() {
		
		// set constants
		
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distributionWindow = _advertiserInfo.getDistributionWindow();
		
		// initialize strategy related variables
		
		double initProfitMargin = .75;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
		}
		
		_desiredSales = new HashMap<Query, Double>();
		
		for (Query query : _querySpace) {
			_desiredSales.put(query, 1.0*_distributionCapacity/_querySpace.size());
		}
		
		// initialize models
		
		_revenueModels = new HashMap<Query, RevenueMovingAvg>(); 
		for (Query query : _querySpace) {
			_revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		_targetModel = new BasicTargetModel(_manSpecialty,_compSpecialty);

		_prConversionModel = new HistoricPrConversionModel(_querySpace, _targetModel);
		_baselineConv = new HashMap<Query, Double>();
        for(Query q: _querySpace){
        	if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) _baselineConv.put(q, 0.1);
        	if(q.getType() == QueryType.FOCUS_LEVEL_ONE){
        		if(q.getComponent() == _compSpecialty) _baselineConv.put(q, 0.27);
        		else _baselineConv.put(q, 0.2);
        	}
        	if(q.getType()== QueryType.FOCUS_LEVEL_TWO){
        		if(q.getComponent()== _compSpecialty) _baselineConv.put(q, 0.39);
        		else _baselineConv.put(q,0.3);
        	}
        }
		
		
		_avgProfit = 0;
		for (Query query: _querySpace) {
			double profit = _revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_avgProfit += profit;
		}
		_avgProfit /= _querySpace.size();
		
		// setup the debug info recorder
		
		try {
			output = new PrintStream(new File("log2.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		// initialize the bid bundle
		
		_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
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
		normalizeFactor = _distributionCapacity*1.25/_distributionWindow/normalizeFactor;
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
		
		for (Query query : _querySpace) {
			// set bids
			double prConv;
			if(_day <= 6) prConv = _baselineConv.get(query);
			else prConv = _prConversionModel.getPrediction(query);
			
			double rev = _revenueModels.get(query).getRevenue();
			double bid = prConv*rev*(1 - _profitMargins.get(query));
			
			_bidBundle.setBid(query, bid);
			
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
				double dailySalesLimit = Math.max(_desiredSales.get(query)/prConv,2);
				double dailyLimit = _bidBundle.getBid(query)*dailySalesLimit*1.1;
				_bidBundle.setDailyLimit(query, dailyLimit);
			}
			
		}
		
		this.printInfo();
		
		return _bidBundle;
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
	
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Target: ").append(1.0*_distributionCapacity/_distributionWindow).append("\n");
		buff.append("\t").append("Average Profit:").append(_avgProfit).append("\n");
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
			buff.append("\t").append("Conversions: ").append(_salesReport.getConversions(q)).append("\n");
			buff.append("\t").append("Desired Sales: ").append(_desiredSales.get(q)).append("\n");
			if (_salesReport.getConversions(q) > 0)
				buff.append("\t").append("Profit: ").append((_salesReport.getRevenue(q) - _queryReport.getCost(q))/(_queryReport.getClicks(q))).append("\n");
			else buff.append("\t").append("Profit: ").append("0").append("\n");
			buff.append("\t").append("Profit Margin: ").append(_profitMargins.get(q)).append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}
		
		System.out.println(buff);
		output.append(buff);
		output.flush();
	
	}

	@Override
	public Set<AbstractModel> initModels() {
		// Not used
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// update models
		
		if (_day > 1 && salesReport != null && queryReport != null) {
		
			_timeHorizon = (int)Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
	
	        ((HistoricPrConversionModel) _prConversionModel).setTimeHorizon(_timeHorizon);
	        _prConversionModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			
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

