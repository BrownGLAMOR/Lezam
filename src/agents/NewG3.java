package agents;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

import newmodels.AbstractModel;
import newmodels.bidtoslot.BasicBidToClick;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class NewG3 extends SimAbstractAgent{
    protected HashMap<Query, Double> _baselineConv;
	protected HashMap<Query,Double> _estimatedPrice;
	protected HashMap<Query, BasicBidToClick> _bidToclick;
	//k is a constant that equates EPPS across queries
	protected double k;
	protected BidBundle _bidBundle;
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		if(_salesReport == null || _queryReport == null) {
			return new BidBundle();
		}
		updateModels(_salesReport, _queryReport);
		updateK();
		for(Query query: _querySpace){
	
			
			//if the query is focus_level_two, send the targeted ad; send generic ad otherwise;
			if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
			{
				_bidBundle.setBidAndAd(query, getQueryBid(query), new Ad(new Product(query.getManufacturer(), query.getComponent())));
			}
			else{
				_bidBundle.setBid(query, getQueryBid(query));
			}
		}
		
		return _bidBundle;
	}

	@Override
	public void initBidder() {
	    _baselineConv = new HashMap<Query,Double>();
	    for(Query query:_querySpace){
	    	if(query.getType()== QueryType.FOCUS_LEVEL_ZERO) _baselineConv.put(query, 0.1);
	    	if(query.getType()== QueryType.FOCUS_LEVEL_ONE){
	    		if(query.getComponent().equals(_compSpecialty)) _baselineConv.put(query,0.27);
	    		else _baselineConv.put(query,0.2);
	    	}
	    	if(query.getType()== QueryType.FOCUS_LEVEL_TWO){
	    		if(query.getComponent().equals(_compSpecialty)) _baselineConv.put(query,0.39);
	    		else _baselineConv.put(query,0.3);
	    	}
	    }
	    
	    _estimatedPrice = new HashMap<Query, Double>();
	    for(Query query:_querySpace){
	    	if(query.getType()== QueryType.FOCUS_LEVEL_ZERO){
	    		_estimatedPrice.put(query, 10.0 + 5/3);
	    	}
	    	if(query.getType()== QueryType.FOCUS_LEVEL_ONE){
	    	  if(query.getManufacturer().equals(_manSpecialty)) _estimatedPrice.put(query, 15.0);
	    	  else{
	    	     if(query.getManufacturer() != null) _estimatedPrice.put(query, 10.0);
	    	     else _estimatedPrice.put(query, 10.0 + 5/3);
	    	  }
	    	}
	    	if(query.getType()== QueryType.FOCUS_LEVEL_TWO){
	    		if(query.getManufacturer().equals(_manSpecialty)) _estimatedPrice.put(query, 15.0);
	    		else _estimatedPrice.put(query, 10.0);
	    	}
	    }
	    

		_bidToclick = new HashMap<Query, BasicBidToClick>();
		for (Query query : _querySpace) {
			_bidToclick.put(query, new BasicBidToClick(query, true));	
		}
	    	
	}

	@Override
	public Set<AbstractModel> initModels() {
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		for(Query query: _querySpace){
			_bidToclick.get(query).updateModel(salesReport, queryReport);
		}
	}


   protected double updateK(){
	  double dailyLimit = _capacity/5;
	  double error = 1e-2;
	  //initial guess of k is 5, and k never goes over 10
	  double k = 5;
	  double sum = 0.0;
	  boolean done = false;
	  while(done == false){
		  for (Query query: _querySpace){
			  sum += calcUnitSold(query, k);
		  }
		  if(sum < dailyLimit && dailyLimit - sum > error) {
			  k = k/2;
			  sum = 0.0;
		  }
		  else{
			  if(sum > dailyLimit && sum - dailyLimit > error) {
				  k = (10 + k)/2;
				  sum = 0.0;
			  }
			  else{ 
				  done = true;
				  sum = 0.0;
			  
			  }
		  }
	  }
	 
	  return k;
   }
   
   protected double calcUnitSold(Query q, double k){
	  double bid = (_estimatedPrice.get(q)-k)*_baselineConv.get(q);
	  //use the bid to click model to estimate #clicks
	  double clicks = _bidToclick.get(q).getPrediction(bid);
	  //estimated sales = clicks * conv prob 
	  return clicks*_baselineConv.get(q);
   }
 
   protected double initializeK(){
	   //will change later
	   return 5.0;
   }
   
   protected double getQueryBid(Query q){
	   return (_estimatedPrice.get(q) -k)*_baselineConv.get(q);
   }
}