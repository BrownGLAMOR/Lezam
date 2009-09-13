/**
 * Used to be NewG4
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
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.AbstractConversionModel;
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

public class EquateProfitC extends AbstractAgent{
	protected HashMap<Query, Double> _baselineConv;
	protected AbstractConversionModel _conversionPrModel;
	protected HashMap<Query,Double> _estimatedPrice;
	protected AbstractBidToCPC _bidToCPC;
	protected BasicTargetModel _targetModel;
	protected HashMap<Query, Double> _prClick;
	
	//k is a constant that equates EPPC across queries
	protected double k;
	protected BidBundle _bidBundle;
	protected ArrayList<BidBundle> _bidBundleList;
	
	protected final double MAX_BID_CPC_GAP = 1.5;
	protected int _timeHorizon;
	protected final int MAX_TIME_HORIZON = 5;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = true;
	
	// for debug
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		if (_day > 1 && _salesReport != null && _queryReport != null) {
			updateK();
		}
		
		for(Query query: _querySpace){
			_bidBundle.setBid(query, getQueryBid(query));
			
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
		
		if (BUDGET) _bidBundle.setCampaignDailySpendLimit(1200);
		
		_bidBundleList.add(_bidBundle);
//		printInfo();
		_bidToCPC.updatePredictions(_bidBundle);
		return _bidBundle;
	}

	@Override
	public void initBidder() {

		_bidBundle = new BidBundle();
		for (Query query : _querySpace) {	
			_bidBundle.setBid(query, getQueryBid(query));
		}

		_bidBundleList = new ArrayList<BidBundle>();
		
		k = 3;
        
		try {
			output = new PrintStream(new File("newg4.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public Set<AbstractModel> initModels() {
		_targetModel = new BasicTargetModel(_manSpecialty,_compSpecialty);

		_conversionPrModel = new HistoricPrConversionModel(_querySpace, _targetModel);
		
		_estimatedPrice = new HashMap<Query, Double>();
		for(Query query:_querySpace){
			if(query.getType()== QueryType.FOCUS_LEVEL_ZERO){
				_estimatedPrice.put(query, 10.0 + 5/3);
			}
			if(query.getType()== QueryType.FOCUS_LEVEL_ONE){
				if(_manSpecialty.equals(query.getManufacturer())) _estimatedPrice.put(query, 15.0);
				else{
					if(query.getManufacturer() != null) _estimatedPrice.put(query, 10.0);
					else _estimatedPrice.put(query, 10.0 + 5/3);
				}
			}
			if(query.getType()== QueryType.FOCUS_LEVEL_TWO){
				if(_manSpecialty.equals(query.getManufacturer())) _estimatedPrice.put(query, 15.0);
				else _estimatedPrice.put(query, 10.0);
			}
		}

		_bidToCPC = new EnsembleBidToCPC(_querySpace, 12, 30, false, true);
		
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
        
        _prClick = new HashMap<Query, Double>();
        for (Query query: _querySpace) {
        	_prClick.put(query, .01);
        }
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// update models
		if (_day > 1 && _salesReport != null && _queryReport != null) {
		    
			_timeHorizon = (int)Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);

			((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(_timeHorizon);
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));

			for (Query query: _querySpace) {
				if (queryReport.getImpressions(query) > 0) _prClick.put(query, queryReport.getClicks(query)*1.0/queryReport.getImpressions(query)); 
			}
               
			if (_bidBundleList.size() > 1) 
				_bidToCPC.updateModel(_queryReport, salesReport, _bidBundleList.get(_bidBundleList.size() - 2));

		}
	}

	protected double updateK(){
		double dailyLimit = _capacity/_capWindow;
		double sum = 0.0;
		for(Query query:_querySpace){
			sum+= _salesReport.getConversions(query);
		}

		if(sum <= 0.9*dailyLimit) {
			k *= .9;
		}
		if(sum >= 1.1*dailyLimit){
			k *= 1.1;
		}

		k = Math.max(2, k);
		k = Math.min(5, k);
	
		return k;
	}

	protected double cpcTobid(double cpc, Query query){
		if (_day <= 6) return cpc + .1;
		double bid = cpc + .1;
		while (_bidToCPC.getPrediction(query,bid) < cpc){
			bid += 0.1;
			if (bid - cpc >= MAX_BID_CPC_GAP) break;
		}     
		return bid;

	}

	protected double getQueryBid(Query q){
		double prConv;
		if(_day <= 6) prConv = _baselineConv.get(q);
		else prConv = _conversionPrModel.getPrediction(q, 0.0);
		
		double rev = _estimatedPrice.get(q);
		
		if (TARGET) {
			double clickPr = _prClick.get(q);
			if (clickPr <=0 || clickPr >= 1) clickPr = .5;
			prConv = _targetModel.getConvPrPrediction(q, clickPr, prConv, 0);
			rev = _targetModel.getUSPPrediction(q, clickPr, 0);
		}
		
		double bid = cpcTobid(rev*prConv - k,q);
		
		if(bid <= 0) return 0;
		else{
			if(bid > 2.5) return 2.5;
			else return bid;
		}

	}


	protected double setQuerySpendLimit(Query q){
		return 0;
	}
	
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("K is").append(k).append("\n");
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
			if (_queryReport.getClicks(q) > 0) 
				buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)*1.0/_queryReport.getClicks(q)).append("\n");
			else buff.append("\t").append("Conversion Pr: ").append("No Clicks").append("\n");

			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}

		//System.out.println(buff);
		output.append(buff);
		output.flush();

	}
}