/**
 * Used to be EqpftAgent 
 */

package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.prconv.NoTargetHistoricPrConversionModel;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;


public class ClickProfitC extends SimAbstractAgent {
	protected BidBundle _bidBundle;
	
	protected AbstractUnitsSoldModel _unitsSoldModel; 
	protected HashMap<Query, RevenueMovingAvg> _revenueModels;
	protected NewAbstractConversionModel _prConversionModel;
	protected HashMap<Query, Double> _baselineConversions;

	protected double _avgProfit;
	protected double _oldAvgProfit;
	private Random _R = new Random();
	
	protected HashMap<Query, Double> _desiredSales;
	protected HashMap<Query, Double> _profitMargins;
	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	protected int _dailyCapacity;
	
	protected ArrayList<BidBundle> _bidBundleList;
	
	protected final int MAX_TIME_HORIZON = 5;
	protected final double _errorOfConversions = 2;
	protected final double _errorOfProfit = .1;
	protected final double _errorOfLimit = .1;
	protected final boolean BUDGET = true;
	protected final boolean TARGET = true;
	
	// for debug
	protected PrintStream output;
	
	@Override
	public void initBidder() {
		
		// set constants
		
		_distributionCapacity = _advertiserInfo.getDistributionCapacity();
		_distributionWindow = _advertiserInfo.getDistributionWindow();
		_dailyCapacity = (int) (_distributionCapacity /_distributionWindow);
		
		// initialize strategy related variables
		
		double initProfitMargin = .7;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
		}
		
		_desiredSales = new HashMap<Query, Double>();
		
		for (Query query : _querySpace) {
			_desiredSales.put(query, 1.0*_distributionCapacity/_querySpace.size());
		}
		
		// initialize models
		
		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _distributionCapacity, _distributionWindow);
		
		_revenueModels = new HashMap<Query, RevenueMovingAvg>(); 
		for (Query query : _querySpace) {
			_revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		_prConversionModel = new NoTargetHistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		_baselineConversions = new HashMap<Query, Double>();
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
			_baselineConversions.put(query, conv);
		}
		

		
		_avgProfit = 0;
		for (Query query: _querySpace) {
			double profit = _prConversionModel.getPrediction(query, 0.0)*_revenueModels.get(query).getRevenue()*_profitMargins.get(query);
			_avgProfit += profit;
		}
		_avgProfit /= _querySpace.size();
		_oldAvgProfit = _avgProfit;
		
		_bidBundleList = new ArrayList<BidBundle>();
		// setup the debug info recorder
		
		try {
			output = new PrintStream(new File("logeqpft.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		// initialize the bid bundle
		
		_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		this.setAvgProfit();
		
		// try to equate profit
		
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
		normalizeFactor = _distributionCapacity*1.25/_distributionWindow/normalizeFactor;
		for (Query query : _querySpace) {
			_desiredSales.put(query, _desiredSales.get(query)*normalizeFactor);
		}
		
		for (Query query: _querySpace) {
			//double latestProfit = _profitsModels.get(query).getLatestSample();
			double latestProfit = 0;
			if (_queryReport.getClicks(query) > 0)
				latestProfit = (_salesReport.getRevenue(query) - _queryReport.getCost(query))/_queryReport.getClicks(query);
			
			if (_salesReport.getConversions(query) + _errorOfConversions < _desiredSales.get(query)||
				_queryReport.getPosition(query) == Double.NaN) {
				double newProfitMargin;
				if (latestProfit > _avgProfit && _avgProfit > 0)
					newProfitMargin = Math.min(_avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModel.getPrediction(query, 0.0)),_profitMargins.get(query)*0.9);
				else newProfitMargin = _profitMargins.get(query)*0.9;
				newProfitMargin = Math.min(0.9, newProfitMargin);
				newProfitMargin = Math.max(0.1, newProfitMargin);
				_profitMargins.put(query, newProfitMargin);
			}
			else if (_salesReport.getConversions(query) - _errorOfConversions > _desiredSales.get(query)) {
				double newProfitMargin;
				if (latestProfit < _avgProfit && _avgProfit > 0)
					newProfitMargin = Math.max(_avgProfit/(_revenueModels.get(query).getRevenue()*_prConversionModel.getPrediction(query, 0.0)),_profitMargins.get(query)*1.1);
				else newProfitMargin = _profitMargins.get(query)*1.1;
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
			if (_day <= 6 || !(_prConversionModel.getPrediction(query, 0.0) > 0))
				prConv = _baselineConversions.get(query);
			else prConv= _prConversionModel.getPrediction(query, 0.0);
			
			double bid = prConv *_revenueModels.get(query).getRevenue()*(1 - _profitMargins.get(query));
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
			if (BUDGET || _day < 20) {
				double dailySalesLimit = Math.max(_desiredSales.get(query)/prConv,2);
				double dailyLimit = _bidBundle.getBid(query)*dailySalesLimit*1.1;
				_bidBundle.setDailyLimit(query, dailyLimit);
			}
		}
		
		//this.printInfo();
		 
		_bidBundleList.add(_bidBundle);
		
		return _bidBundle;
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
	
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Window Sold: ").append(_unitsSoldModel.getWindowSold()).append("\n");
		buff.append("\t").append("Daily Capacity: ").append(_dailyCapacity).append("\n");
		buff.append("\t").append("Yesterday Sold: ").append(_unitsSoldModel.getLatestSample()).append("\n");
		buff.append("\t").append("Manufacturer Specialty: ").append(_advertiserInfo.getManufacturerSpecialty()).append("\n");
		buff.append("\t").append("Average Profit:").append(_avgProfit).append("\n");
		buff.append("\t").append("Old Average Profit:").append(_oldAvgProfit).append("\n");
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
			buff.append("\t").append("Predicted Conversion Pr:").append(_prConversionModel.getPrediction(q, 0.0)).append("\n");
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
		if (_day > 1 && salesReport != null && queryReport != null) {
			_unitsSoldModel.update(salesReport);
			
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);

			_prConversionModel.setTimeHorizon(timeHorizon);
			_prConversionModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			
		
			for (Query query : _querySpace) {
				if (salesReport.getRevenue(query) > 0) 
					_revenueModels.get(query).update(salesReport.getRevenue(query)/salesReport.getConversions(query));
			}
		}
		
	}
	

}
