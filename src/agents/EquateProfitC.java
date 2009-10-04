/**
 * Used to be NewG4
 */

package agents;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.prconv.AbstractConversionModel;
import newmodels.targeting.BasicTargetModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class EquateProfitC extends RuleBasedAgent{
	protected HashMap<Query,Double> _estimatedPrice;
	protected BasicTargetModel _targetModel;
	protected HashMap<Query, Double> _prClick;
	
	//k is a constant that equates EPPC across queries
	protected double k;
	protected BidBundle _bidBundle;
	protected ArrayList<BidBundle> _bidBundleList;
	
	protected final boolean TARGET = true;
	protected final boolean BUDGET = true;
	
	// for debug
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		buildMaps(models);
		
		buildMaps(models);
		if (_day > 1 && _salesReport != null && _queryReport != null) {
			updateK();
		}
		
		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			double bid = _CPCToBidModel.getPrediction(query, targetCPC);
			if(Double.isNaN(bid)) {
				bid = targetCPC;
			}
			_bidBundle.setBid(query, _CPCToBidModel.getPrediction(query, targetCPC));
			
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
			if (BUDGET) 
				_bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));
		}
		
		
		
		_bidBundleList.add(_bidBundle);
//		printInfo();
		return _bidBundle;
	}
	
	protected void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPCModel = bidToCPC; 
			}
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_conversionPrModel = convPrModel;
			}
		}
	}

	@Override
	public void initBidder() {
		setDailyQueryCapacity();
		_bidBundle = new BidBundle();
		for (Query query : _querySpace) {	
			_bidBundle.setBid(query, _CPCToBidModel.getPrediction(query, getTargetCPC(query)));
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
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		models.addAll(super.initModels());

		_targetModel = new BasicTargetModel(_manSpecialty,_compSpecialty);
		
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

		_baselineConversion = new HashMap<Query, Double>();
        for(Query q: _querySpace){
        	if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) _baselineConversion.put(q, 0.1);
        	if(q.getType() == QueryType.FOCUS_LEVEL_ONE){
        		if(q.getComponent() == _compSpecialty) _baselineConversion.put(q, 0.27);
        		else _baselineConversion.put(q, 0.2);
        	}
        	if(q.getType()== QueryType.FOCUS_LEVEL_TWO){
        		if(q.getComponent()== _compSpecialty) _baselineConversion.put(q, 0.39);
        		else _baselineConversion.put(q,0.3);
        	}
        }
        
        _prClick = new HashMap<Query, Double>();
        for (Query query: _querySpace) {
        	_prClick.put(query, .01);
        }
        return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		super.updateModels(salesReport, queryReport);
		
		if (_day > 1 && _salesReport != null && _queryReport != null) {
		  
			for (Query query: _querySpace) {
				if (queryReport.getImpressions(query) > 0) _prClick.put(query, queryReport.getClicks(query)*1.0/queryReport.getImpressions(query)); 
			}
		}
	}

	protected double updateK(){
		double sum = 0.0;
		for(Query query:_querySpace){
			sum+= _salesReport.getConversions(query);
		}

		if(sum <= 0.9* _dailyCapacity) {
			k *= .9;
		}
		if(sum >= 1.1* _dailyCapacity){
			k *= 1.1;
		}

		k = Math.max(2, k);
		k = Math.min(5, k);
	
		return k;
	}

	protected double getTargetCPC(Query q){
		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		
		double rev = _estimatedPrice.get(q);
		
		if (TARGET) {
			double clickPr = _prClick.get(q);
			if (clickPr <=0 || clickPr >= 1) clickPr = .5;
			prConv = _targetModel.getConvPrPrediction(q, clickPr, prConv, 0);
			rev = _targetModel.getUSPPrediction(q, clickPr, 0);
		}
		
		return rev * prConv -k;
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
	
	@Override
	public String toString() {
		return "EquateProfitC";
	}
	
}