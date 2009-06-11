package agents;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class newJESOM2 extends SimAbstractAgent{
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
	protected HashMap<Query, Double> _baseLineConversion;
    protected HashMap<Query, Double> _revenue;
    protected HashMap<Query, Double> _honestFactor;
    protected HashMap<Query, Double> _wantedSales;
    
    protected BidBundle _bidBundle;
    //protected int counter = 1;
    protected double magicDivisor = 8;
    //protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		for(Query q: _querySpace){
			handleTopPosition(q);
		    adjustHonestFactor(q);
		    adjustWantedSales(q);
			if(q.getType() == QueryType.FOCUS_LEVEL_TWO)
			{
				_bidBundle.setBidAndAd(q, getQueryBid(q), new Ad(new Product(q.getManufacturer(), q.getComponent())));
			}
			else{
				_bidBundle.setBid(q, getQueryBid(q));
			}
			_bidBundle.setDailyLimit(q, setQuerySpendLimit(q));
			
			}
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		// TODO Auto-generated method stub
	    _unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);
		
		_conversionPrModel = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			_conversionPrModel.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
		}
		
		_honestFactor = new HashMap<Query, Double>();
		for(Query q: _querySpace){
			_honestFactor.put(q, 0.4);
		}
		
		_baseLineConversion = new HashMap<Query, Double>();
        for(Query q: _querySpace){
        	if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) _baseLineConversion.put(q, 0.1);
        	if(q.getType() == QueryType.FOCUS_LEVEL_ONE){
        		if(q.getComponent() == _compSpecialty) _baseLineConversion.put(q, 0.27);
        		else _baseLineConversion.put(q, 0.2);
        	}
        	if(q.getType()== QueryType.FOCUS_LEVEL_TWO){
        		if(q.getComponent()== _compSpecialty) _baseLineConversion.put(q, 0.39);
        		else _baseLineConversion.put(q,0.3);
        	}
        }
        
        double slice = _capacity/(20*_capWindow);
        _wantedSales = new HashMap<Query, Double>();
        for(Query q:_querySpace){
        	if(q.getManufacturer()== _manSpecialty) _wantedSales.put(q, 2*slice);
        	else _wantedSales.put(q, slice);
        	
        }
		
    	_revenue = new HashMap<Query, Double>();
		for (Query query: _querySpace){
			if (query.getManufacturer() == _manSpecialty) _revenue.put(query, 15.0);
			else _revenue.put(query,10.0);
		}
		
		_bidBundle = new BidBundle();
	    for (Query query : _querySpace) {
			_bidBundle.setBid(query, getQueryBid(query));
			_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}
		
	}

	@Override
	public Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
	
	protected double getQueryBid(Query q){
	    return _revenue.get(q)*_honestFactor.get(q)*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity);	
	}
	
	protected double setQuerySpendLimit(Query q){
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		double clicks = Math.max(1,_wantedSales.get(q) / conversion);
		return getQueryBid(q)*clicks;
	}

	protected void adjustHonestFactor(Query q)
    {
		double newHonest;
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
	     //if we oversold, we lower our bid price to CPC
		 if (conversion < _baseLineConversion.get(q)) {
			newHonest = (_queryReport.getCPC(q)-1e-2)/(_revenue.get(q)*conversion);
			if(newHonest < 0.1) newHonest = 0.1;
			_honestFactor.put(q, newHonest);
		}else{
		  //if we sold less than what we expected, and we got bad position
     	  //and also wanted sales does not tend to go over capacity, then higher our bid
			    if(_queryReport.getClicks(q)*conversion < _wantedSales.get(q)){
			    	if(!(_queryReport.getPosition(q) < 4)){
			             if(_wantedSales.get(q) < (_capacity - _unitsSoldModel.getWindowSold())/magicDivisor){
        	                   newHonest = _honestFactor.get(q)*1.3 + .1;
        		               if(newHonest >= 0.95) newHonest = 0.95;
        		               _honestFactor.put(q, newHonest);
			              }
			        }
			    }else{
			  //if we sold more than what expected, and we got bad position, then lower the bid
			    	if(_queryReport.getClicks(q)*conversion >= _wantedSales.get(q)){
			    		if(!(_queryReport.getPosition(q) < 4)){
			    			newHonest = (_queryReport.getCPC(q)-0.01)/(_revenue.get(q)*conversion);
							if(newHonest < 0.1) newHonest = 0.1;
							_honestFactor.put(q,newHonest);
			    		}
			    	}
			    }  
		}
		 
	}
	
	protected void adjustWantedSales(Query q){
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		if(conversion >= _baseLineConversion.get(q)){
		//if we sold less than what we expected, but we got good position, then lower our expectation
		   if(_queryReport.getClicks(q)*conversion < _wantedSales.get(q)){
			    if(!(_queryReport.getPosition(q)< 4)){
				   _wantedSales.put(q, _wantedSales.get(q)*.625);
			     }
			     //if we sold more than what we expected, and we got bad position, then increase our expectation
			     if (!(_queryReport.getPosition(q) < 4)|| _queryReport.getCPC(q)< 0.2){
				    _wantedSales.put(q,_wantedSales.get(q)*1.6);
			     }
		    }else{
		   //if we sold more than what we expected, and we got bad position, then increase our expectation
			    if (!(_queryReport.getPosition(q) < 4)|| _queryReport.getCPC(q)< 0.2){
				    _wantedSales.put(q,_wantedSales.get(q)*1.6);
			    }
		    }
	}
		
	}
	
	protected void handleTopPosition(Query q){
		if(_queryReport.getPosition(q)==1){
			if(_queryReport.getCPC(q)< 0.8*getQueryBid(q)){
				double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
				double newHonest = newHonest = (_queryReport.getCPC(q)+0.01)/(_revenue.get(q)*conversion);
				if(newHonest < 0.1) newHonest = 0.1;
				_honestFactor.put(q, newHonest);
			}
		}
	}
}
	