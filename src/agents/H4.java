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
import newmodels.bidtoslot.BasicBidToClick;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.AbstractConversionModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class H4 extends AbstractAgent{
	private Random _R = new Random();
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected AbstractConversionModel _conversionPrModel;
    protected HashMap<Query, Double> _baselineConv;
	protected HashMap<Query,Double> _estimatedPrice;
	protected AbstractBidToCPC _bidToCPC;
	protected HashMap<Query, BasicBidToClick> _bidToclick;
	protected double oldSales = 0.0;
	protected double newSales = 0.0;
	//k is a constant that equates EPPS across queries
	protected double k;
	protected BidBundle _bidBundle;
	protected ArrayList<BidBundle> _bidBundles;

	protected int _timeHorizon;
	protected final int MAX_TIME_HORIZON = 5;
	protected PrintStream output;	
	
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		for(Query query: _querySpace){
			_bidToclick.get(query).updateModel(_salesReport, _queryReport);
		}
	
		if(_day >=5){
		    updateK();
		   adjustK();
		}
		for(Query query: _querySpace){
	
			_bidBundle.setBid(query, getQueryBid(query));
		
			//_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}
		
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		
	      _conversionPrModel = new HistoricPrConversionModel(_querySpace);
		    
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
		    
		    _bidToCPC = new RegressionBidToCPC(_querySpace);

			_bidToclick = new HashMap<Query, BasicBidToClick>();
			for (Query query : _querySpace) {
				_bidToclick.put(query, new BasicBidToClick(query, false));	
			}
		    	
			_bidBundle = new BidBundle();
		    for (Query query : _querySpace) {	
				_bidBundle.setBid(query, getQueryBid(query));
			}
		    
		    _bidBundles = new ArrayList<BidBundle>();
		    initializeK();

			try {
				output = new PrintStream(new File("h3.txt"));
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
		// update models
		if (_salesReport != null && _queryReport != null) {
			
			for(Query query: _querySpace){
				_bidToclick.get(query).updateModel(_salesReport, _queryReport);
			}
	
			_unitsSoldModel.update(_salesReport);
		    
			   _timeHorizon = (int)Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);

               _conversionPrModel.setTimeHorizon(_timeHorizon);
               _conversionPrModel.updateModel(queryReport, salesReport);

			if (_bidBundles.size() > 1) 
				_bidToCPC.updateModel(_queryReport, _bidBundles.get(_bidBundles.size() - 2));

		}

	}


   protected double updateK(){
	  oldSales = newSales;
	  newSales = 0.0;
	  for(Query query: _querySpace){
		  newSales += _salesReport.getConversions(query);
	  }
	  double dailyLimit = (2*_capacity/5) - newSales;
	  double error = 0.5;
	  int counter = 0;
	  //initial guess of k is 5, and k never goes over 10
	  k = 0.5;
	  double sum = 0.0;
	  double hi =  1;
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
	  double conversion = _conversionPrModel.getPrediction(q,0);
	  double cpc = (_estimatedPrice.get(q)-k)*conversion;
	  //use the bid to click model to estimate #clicks
	  double clicks = _bidToclick.get(q).getPrediction(cpc);
	  //estimated sales = clicks * conv prob 
	  return clicks*conversion;
   }

   protected void adjustK(){
	   double dailyCapacity = _capacity/5;
		if(newSales < 2*dailyCapacity - oldSales - 5){
			if(newSales < 2*dailyCapacity - oldSales - 20) k = k*0.7;
			else k = k*0.9;
		}
		if(newSales > 2*dailyCapacity - oldSales + 10){
			if(newSales > 2*dailyCapacity - oldSales + 30) k = k*1.3;
			else k = k*1.1;
		}
   }

	protected double cpcTobid(double cpc, Query query){
		return cpc + .1;
		/*return cpc + .1;
		double bid = cpc;
		while (_bidToCPC.getPrediction(query,bid,_bidBundles.get(_bidBundles.size() - 2)) < cpc){
			bid += 0.1;
		}  
		return bid;*/
	}
   
   protected void initializeK(){
	   //will change later
	   k = 1;
   }
 
   protected double getQueryBid(Query q){
	   double prConv;
		if(_day <= 6) {
			prConv = _baselineConv.get(q);
		}
		else prConv = _conversionPrModel.getPrediction(q,0);
		
		double bid;
		bid = cpcTobid((_estimatedPrice.get(q) - k)*prConv,q);
		if(bid <= 0) return 0;
		else{
			if(bid > 2.5) return 2.5;
			else return bid;
		}
   }


	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}
  
   protected double setQuerySpendLimit(Query q){
	   return 0;
	}
   
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		for(Query q : _querySpace){
			buff.append("\t").append("Day: ").append(_day).append("\n");
			buff.append(q).append("\n");
			buff.append("\t").append("k: ").append(k).append("\n");
			buff.append("\t").append("Bid: ").append(_bidBundle.getBid(q)).append("\n");
			buff.append("\t").append("Window Sold: ").append(_unitsSoldModel.getWindowSold()).append("\n");
			buff.append("\t").append("capacity: ").append(_capacity).append("\n");
			buff.append("\t").append("Yesterday Sold: ").append(_unitsSoldModel.getLatestSample()).append("\n");
			/*if (_salesReport.getConversions(q) > 0) 
				buff.append("\t").append("Revenue: ").append(_salesReport.getRevenue(q)/_salesReport.getConversions(q)).append("\n");
			else buff.append("\t").append("Revenue: ").append("0.0").append("\n");*/
			if (_queryReport.getClicks(q) > 0) 
				buff.append("\t").append("Conversion Pr: ").append(_salesReport.getConversions(q)*1.0/_queryReport.getClicks(q)).append("\n");
			else buff.append("\t").append("Conversion Pr: ").append("No Clicks").append("\n");
			//buff.append("\t").append("Predicted Conversion Pr:").append(_prConversionModels.get(q).getPrediction()).append("\n");
			buff.append("\t").append("Conversions: ").append(_salesReport.getConversions(q)).append("\n");
			if (_salesReport.getConversions(q) > 0)
				buff.append("\t").append("Profit: ").append((_salesReport.getRevenue(q) - _queryReport.getCost(q))/(_queryReport.getClicks(q))).append("\n");
			else buff.append("\t").append("Profit: ").append("0").append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("****************\n");
		}
		
		System.out.println(buff);
		output.append(buff);
		output.flush();
	
	}

}

