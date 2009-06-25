package agents;

/**
 * 
 * @author hchen
 *
 */
import java.util.HashMap;
import java.util.Set;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

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

public class NewG4 extends SimAbstractAgent{
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
	protected HashMap<Query,Double> _estimatedPrice;
	protected HashMap<Query, BasicBidToClick> _bidToclick;
	//estimate the difference between bid and cpc
	protected HashMap<Query, Double> _error;
	//k is a constant that equates EPPC across queries
	protected double k;
	protected BidBundle _bidBundle;
	// for debug
	protected PrintStream output;
	
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		if(_salesReport == null || _queryReport == null) {
			return new BidBundle();
		}
		for(Query query: _querySpace){
			_bidToclick.get(query).updateModel(_salesReport, _queryReport);
		}
		
		_unitsSoldModel.update(_salesReport);
		
		updateError(_queryReport);
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
		printInfo();
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
	    	
		_error = new HashMap<Query, Double>();
		for (Query query: _querySpace){
			_error.put(query, 0.1);
		}
		
		_bidBundle = new BidBundle();
	    for (Query query : _querySpace) {	
			_bidBundle.setBid(query, getQueryBid(query));
		}
	    

		try {
			output = new PrintStream(new File("newg4.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public Set<AbstractModel> initModels() {
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		
	}


   protected void updateK(){
	  double dailyLimit = _capacity/5;
	  double err = 1;
	  int counter = 0;
	  //initial guess of k is 5, and k never goes over 10
	  k = 0.5;
	  double sum = 0.0;
	  double hi = 1;
	  double low = 0;
	  boolean done = false;
	  while(done == false && counter <= 100){
		  for (Query query: _querySpace){
			  sum += calcUnitSold(query, k);
		  }
		  if(sum < dailyLimit && dailyLimit - sum > err) {
			  hi = k;
			  k = (low + k)/2;
		  }
		  else{
			  if(sum > dailyLimit && sum - dailyLimit > err) {
				  low = k;
				  k = (hi + k)/2;
			  }
			  else{ 
				  done = true;
			  
			  }
		  }
		  counter++;
		  sum = 0.0;
	  }
	
   }
   
   protected double calcUnitSold(Query q, double k){
	  //double bid = _estimatedPrice.get(q)*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity) - k + _error.get(q);
	   double cpc = _estimatedPrice.get(q)*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity) -k;
	   //use the bid to click model to estimate #clicks
	  double clicks = _bidToclick.get(q).getPrediction(cpc);
	  //estimated sales = clicks * conv prob 
	  return clicks*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity);
   }
   
   protected double initializeK(){
	   //will change later
	   return 0.5;
   }
   
   
   /*protected void adjustK(){
	   double sum = 0.0;
	   double dailyLimit = _capacity/5;
	   for(Query query: _querySpace){
		   sum += _salesReport.getConversions(query);
	   }
	   if(sum > dailyLimit + 5) k = k*0.7 - 0.1;
	   if(sum < dailyLimit - 10) k = k*1.3 + 0.1;
   }*/
   
   protected void updateError(QueryReport queryReport){
	   for(Query query:_querySpace){
		   double dist = Math.abs(queryReport.getCPC(query) - getQueryBid(query)) ;
		   if(dist >= 0.2){
			   _error.put(query, dist*0.8);
		   }
		   
	   }
   }
   
   protected double getQueryBid(Query q){
	   double bid = _estimatedPrice.get(q)*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity) - k + _error.get(q);
	   if(bid <= 0) return 0;
	   else return bid;
   }
   
   protected double setQuerySpendLimit(Query q){
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		double clicks = Math. max(1.1, Math.min(_capacity/(40*conversion), calcUnitSold(q, k)));
		return getQueryBid(q)*clicks;
	}
   
   protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("K is").append(k).append("\n");
		buff.append("\t").append("Window Sold: ").append(_unitsSoldModel.getWindowSold()).append("\n");
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