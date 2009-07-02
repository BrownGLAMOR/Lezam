package agents;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
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
	protected AbstractBidToCPC _bidToCPC;
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
		
		
		_unitsSoldModel.update(_salesReport);
		if (_bidBundles.size() > 1) 
			_bidToCPC.updateModel(_queryReport, _bidBundles.get(_bidBundles.size() - 2));
		
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

		AbstractBidToCPC bidToCPC = new RegressionBidToCPC(_querySpace);
	    
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

   protected double updateK(){
	  double dailyLimit = _capacity/_capWindow;
	  double sum = 0.0;
	  for(Query query:_querySpace){
		  sum+= _salesReport.getConversions(query);
	  }
	 if(sum <= 0.9*dailyLimit) k = k*1.3;
	 if(sum >= 1.3*dailyLimit) k = k*0.7;
	  
	  if(k > 2)  k = 2;
	  if(k < 0.01) k = 0.01;
	  return k;
   }
      
   protected double cpcTobid(double cpc, Query query){
	      double bid = 0;
	      while(_bidToCPC.getPrediction(query,bid, _bidBundles.get(_bidBundle.size() - 2)) >= 0.1){
	    	  bid += 0.1;
	      }     
	      return bid;
	      
   }
   
   protected void initializeK(){
	   //will change later
	    k = 0.75;
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
	   else bid = cpcTobid(_estimatedPrice.get(q)*_conversionPrModel.get(q).getPrediction(0)-k,q);
	   
	   if(bid <= 0) return 0;
	   else{
		   if(bid > 3) return 3;
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