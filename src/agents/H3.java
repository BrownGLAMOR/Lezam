package agents;


import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtoslot.BasicBidToClick;
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

public class H3 extends SimAbstractAgent{
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
    protected HashMap<Query, Double> _baselineConv;
	protected HashMap<Query,Double> _estimatedPrice;
	protected HashMap<Query, BasicBidToClick> _bidToclick;
	//estimate the difference between bid and cpc
	protected HashMap<Query, Double> _error;
	//k is a constant that equates EPPS across queries
	protected double k;
	protected BidBundle _bidBundle;
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		if(_day < 2) {
			return new BidBundle();
		}
		for(Query query: _querySpace){
			_bidToclick.get(query).updateModel(_salesReport, _queryReport);
		}
		_unitsSoldModel.update(_salesReport);
		updateError(_queryReport);
		if(_day > 2)
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
			_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}
		
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		
		    _unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);
			
		    _conversionPrModel = new HashMap<Query, AbstractPrConversionModel>();
			for (Query query : _querySpace) {
				_conversionPrModel.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
			}
		    
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
		    

			_bidToclick = new HashMap<Query, BasicBidToClick>();
			for (Query query : _querySpace) {
				_bidToclick.put(query, new BasicBidToClick(query, true));	
			}
		    	
			_bidBundle = new BidBundle();
		    for (Query query : _querySpace) {	
				_bidBundle.setBid(query, getQueryBid(query));
			}
		    
		    initializeK();
	}

	@Override
	public Set<AbstractModel> initModels() {
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
	
	}


   protected double updateK(){
	  double dailyLimit = _capacity/5;
	  double error = 1;
	  int counter = 0;
	  //initial guess of k is 5, and k never goes over 10
	  k = 5;
	  double sum = 0.0;
	  double hi =  10;
	  double lo = 0;
	  boolean done = false;
	  while(done == false && counter <= 20){
		  for (Query query: _querySpace){
			  sum += calcUnitSold(query, k);
		  }
		  if(sum < dailyLimit && dailyLimit - sum > error) {
			  hi =  k;
			  k = (lo + k)/2;
			  
		  }
		  else{
			  if(sum > dailyLimit && sum - dailyLimit > error) {
				  lo = k;
				  k = (hi + k)/2;
				  
			  }
			  else{ 
				  done = true;
				 
			  
			  } 
		  }
		  counter ++;
		  sum = 0.0;
	  }
	  if(k < 0.1) k = 0.1;
	  return k;
   }
   
   protected double calcUnitSold(Query q, double k){
	   double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
	  double bid = (_estimatedPrice.get(q)-k)*conversion;
	  //use the bid to click model to estimate #clicks
	  double clicks = _bidToclick.get(q).getPrediction(bid);
	  //estimated sales = clicks * conv prob 
	  return clicks*conversion;
   }
 
   
   protected void updateError(QueryReport queryReport){
	   for(Query query:_querySpace){
		   double dist = Math.abs(queryReport.getCPC(query) - getQueryBid(query)) ;
		   if(dist >= 0.2 && _queryReport.getPosition(query) == 1){
			   _error.put(query, dist*0.2);
		   }
		   
	   }
   }
   
   
   protected double initializeK(){
	   //will change later
	   return 5.0;
   }
 
   protected double getQueryBid(Query q){
	   double bid = 0.0;
	   if(_day >= 1 && _day <= 2){
		   if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) bid = 1.2;
		   if(q.getType() == QueryType.FOCUS_LEVEL_ONE){
			   if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
				   bid = 1.75;
				}
			   else bid = 1.5;
		   }
		   if(q.getType() == QueryType.FOCUS_LEVEL_TWO){
			   if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
					bid = 2.5;
				}
			   else bid = 1.75;
		   }
	   }
	   else bid = _estimatedPrice.get(q)*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity) - k + _error.get(q);
	   
	   if(bid <= 0) return 0;
	   else{
		   if(bid > 3) return 3;
		   else return bid;
	   }
	   
   }

   
  
   
   protected double setQuerySpendLimit(Query q){
		/*
	    double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		double clicks = Math.max(1,_capacity/(10*conversion));
		return getQueryBid(q)*clicks;
		*/
	   double remainCap = _capacity - _unitsSoldModel.getWindowSold();
		if(remainCap < 0) remainCap = 0;
		return getQueryBid(q)*remainCap/8;
	}

