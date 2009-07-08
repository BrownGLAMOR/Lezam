/**
 * Used to be CHAgent
 */
package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class ClickSlotAgent extends SimAbstractAgent {
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected AbstractBidToCPC _bidToCPCModel;
	protected NewAbstractConversionModel _conversionPrModel;
	protected HashMap<Query, Double> _baseLineConversion;
	protected HashMap<Query, Double> _revenue;
	protected HashMap<Query, Double> _honestFactor;
	protected HashMap<Query, Double> _wantedSales;

	protected BidBundle _bidBundle;
	protected double _dailyCapacity;
	
	protected ArrayList<BidBundle> _bidBundles;

	protected final double _errorOfLimit = .1;
	protected final int MAX_TIME_HORIZON = 5;
	
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		// adjust parameters
		for (Query q : _querySpace) {
			adjustWantedSales(q);
		}
		
		double normalizeFactor = 0;
		for (Query query : _querySpace) {
			normalizeFactor += _wantedSales.get(query);
		}
		
		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}
		
		int targetCapacity = (int)Math.max(2*_dailyCapacity - unitsSold, _dailyCapacity*.5);
		normalizeFactor = targetCapacity/normalizeFactor;
		for (Query query : _querySpace) {
			_wantedSales.put(query, _wantedSales.get(query)*normalizeFactor);
		}

		for (Query q : _querySpace) {
			adjustHonestFactor(q);
		}
		
		// build bid bundle
		for (Query q : _querySpace) {
			_bidBundle.setBid(q, getQueryBid(q));
			//_bidBundle.setDailyLimit(q, setQuerySpendLimit(q));
		}
		
		_bidBundles.add(_bidBundle);
		
		printInfo();
		
		return _bidBundle;
	}

	@Override
	public void initBidder() {


		_honestFactor = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_honestFactor.put(q, .3);
		}

		_baseLineConversion = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_baseLineConversion.put(q, 0.1);
			if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				if (q.getComponent() == _compSpecialty)
					_baseLineConversion.put(q, 0.27);
				else
					_baseLineConversion.put(q, 0.2);
			}
			if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				if (q.getComponent() == _compSpecialty)
					_baseLineConversion.put(q, 0.39);
				else
					_baseLineConversion.put(q, 0.3);
			}
		}

		_dailyCapacity = 1.5*_capacity/_capWindow;
		
		double slice = _capacity*1.5 / (20 * _capWindow);
		_wantedSales = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (q.getManufacturer() == _manSpecialty)
				_wantedSales.put(q, 2 * slice);
			else
				_wantedSales.put(q, slice);

		}

		_revenue = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			if (query.getManufacturer() == _manSpecialty)
				_revenue.put(query, 15.0);
			else
				_revenue.put(query, 10.0);
		}

		_bidBundle = new BidBundle();
		
		_bidBundles = new ArrayList<BidBundle>();
		

		try {
			output = new PrintStream(new File("ch.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Set<AbstractModel> initModels() {
		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity,
				_capWindow);
		_bidToCPCModel = new RegressionBidToCPC(_querySpace);

		_conversionPrModel = new HistoricPrConversionModel(_querySpace);
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		
		if( (salesReport != null) && (queryReport != null)) {
			
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			_conversionPrModel.setTimeHorizon(timeHorizon);
			_conversionPrModel.updateModel(queryReport, salesReport);
			
			if (_bidBundles.size() > 1) 
				_bidToCPCModel.updateModel(_queryReport, _bidBundles.get(_bidBundles.size() - 2));
		}
		

	}

	protected double getQueryBid(Query q) {
		double prConv;
		if (_day <= 6) prConv = _baseLineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		return _revenue.get(q) * _honestFactor.get(q) * prConv;
	}

	protected double setQuerySpendLimit(Query q) {
		double prConv;
		if (_day <= 6) prConv = _baseLineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		double dailySalesLimit = Math.max(_wantedSales.get(q)/prConv,1);
		
		double bid = _bidBundle.getBid(q);
		double cpc;
		if (_day <= 6) cpc = .9*bid; 
		else cpc = _bidToCPCModel.getPrediction(q, bid);
		double dailyLimit = cpc*(dailySalesLimit - 1) + bid + _errorOfLimit;
		
		return dailyLimit;
	}

	protected void adjustHonestFactor(Query q) {
		double newHonest;

		/* if we sold less than what we expected, and we got bad position
		 and also wanted sales does not tend to go over capacity, then higher
		 our bid*/
		if (_salesReport.getConversions(q) < _wantedSales.get(q)) {
			if (!(_queryReport.getPosition(q) <= 4)) {

				newHonest = Math.min(_honestFactor.get(q) * 1.1, .9);
				_honestFactor.put(q, newHonest);

			}
		} else {
			/* if we sold more than what expected, and we got good position,
			 then lower the bid*/
			if (_salesReport.getConversions(q) >=  _wantedSales.get(q)) {
				if (_queryReport.getPosition(q) <= 3) {
					/*newHonest = (_queryReport.getCPC(q) - 0.01)
							/ (_revenue.get(q) * conversion);*/
					newHonest = Math.min(_honestFactor.get(q)*0.9, .1);
					_honestFactor.put(q, newHonest);
				}
			}
		}

	}

	protected void adjustWantedSales(Query q) {
			/* if we sold less than what we expected, but we got good position,
			 then lower our expectation*/
			if (_salesReport.getConversions(q) < _wantedSales.get(q)) {
				if (_queryReport.getPosition(q) <= 4) {
					_wantedSales.put(q, _wantedSales.get(q) * .8);
				}
			} else {
				/* if we sold more than what we expected, but we got bad
				 position, then increase our expectation*/
				if (!(_queryReport.getPosition(q) <= 4)) {
					_wantedSales.put(q, _wantedSales.get(q) * 1.25);
				}
			}
	}

	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Window Sold: ").append(
				_unitsSoldModel.getWindowSold()).append("\n");
		buff.append("\t").append("Yesterday sold: ").append(
				_unitsSoldModel.getLatestSample()).append("\n");
		buff.append("\t").append("Estimated sold: ").append(
				_unitsSoldModel.getEstimate()).append("\n");
		buff.append("\t").append("Manufacturer specialty: ").append(
				_advertiserInfo.getManufacturerSpecialty()).append("\n");
		for (Query q : _querySpace) {
			buff.append("\t").append("Day: ").append(_day).append("\n");
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bidBundle.getBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(_bidBundle.getDailyLimit(q)).append("\n");
			if (_salesReport.getConversions(q) > 0) 
				buff.append("\t").append("Revenue: ").append(_salesReport.getRevenue(q)/_salesReport.getConversions(q)).append("\n");
			else buff.append("\t").append("Revenue: ").append("0.0").append("\n");
			buff.append("\t").append("Predicted Revenue:").append(_revenue.get(q)).append("\n");
			if (_queryReport.getClicks(q) > 0) 
				buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)*1.0/_queryReport.getClicks(q)).append("\n");
			else buff.append("\t").append("Conversion Pr: ").append("No Clicks").append("\n");
			buff.append("\t").append("Predicted Conversion Pr:").append(_conversionPrModel.getPrediction(q)).append("\n");
			buff.append("\t").append("Conversions: ").append(_salesReport.getConversions(q)).append("\n");
			buff.append("\t").append("Desired Sales: ").append(_wantedSales.get(q)).append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}

		System.out.println(buff);
		output.append(buff);
		output.flush();

	}

}
