/**
 * Used to be NewG3
 * Used to be G3Agent
 */

package agents;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class EquateProfitS extends RuleBasedAgent{
	protected HashMap<Query,Double> _estimatedPrice;
	protected HashMap<Query, Double> _prClick;
	
	protected double k; //k is a constant that equates EPPS across queries
	protected BidBundle _bidBundle;
	protected ArrayList<BidBundle> _bidBundleList;
	
	protected boolean TARGET = true;
	protected boolean BUDGET = false;
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		buildMaps(models);
		
		_bidBundle = new BidBundle();

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
			if(BUDGET)
				_bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));
		}

		_bidBundleList.add(_bidBundle);
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();
		_bidBundleList = new ArrayList<BidBundle>();
		
		if (_capacity == 500) k = 12;
		else if (_capacity == 400) k = 11;
		else k = 10;
		
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
		
        _prClick = new HashMap<Query, Double>();
        for (Query query: _querySpace) {
        	_prClick.put(query, .01);
        }
		
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		super.updateModels(salesReport, queryReport);
		if (_day > 1 && salesReport != null && queryReport != null) {	
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
		if(sum >= 1.1*_dailyCapacity){
			k *= 1.1;
		}	
		
		k = Math.max(7, k);
		k = Math.min(14.5, k);
		
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
		
		return (rev - k)*prConv;
	}
 
	@Override
	public String toString() {
		return "EquateProfitS";
	}

}