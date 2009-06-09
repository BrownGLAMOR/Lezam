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

public class newSSB extends SimAbstractAgent{
	
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
    protected HashMap<Query, Double> _reinvestment;
    protected HashMap<Query, Double> _revenue;
    protected HashMap<Query, Double> _wantedSales;
    protected BidBundle _bidBundle;
    protected int counter = 1;
    
    protected double magicDivisor = 8;
    
	protected PrintStream output;
    
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		_unitsSoldModel.update(_salesReport);
		
		for (Query query : _querySpace) {
			
			_conversionPrModel.get(query).getPrediction(_unitsSoldModel.getWindowSold() - _capacity);
			
			//handle the case of no impression (the agent got no slot)
			handleNoImpression(query);
			//handle the case when the agent got the promoted slots
			handlePromotedSlots(query);
			//walk otherwise
			walking(query);
			
			//adjust reinvestment factor and wanted sales
			modify(query);
			
			//if the query is focus_level_two, send the targeted ad; send generic ad otherwise;
			if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
			{
				_bidBundle.setBidAndAd(query, getQueryBid(query), new Ad(new Product(query.getManufacturer(), query.getComponent())));
			}
			else{
				_bidBundle.setBid(query, getQueryBid(query));
			}
	
			
			_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
			
			//print out the properties
			StringBuffer buff = new StringBuffer("");
			buff.append("\t").append("day").append(counter).append("\n");
			buff.append("\t").append("product: ").append(query.getManufacturer()).append(", ").append(query.getComponent());
			buff.append("\t").append("bid: ").append(getQueryBid(query)).append("\n");
			buff.append("\t").append("Conversion: ").append(_conversionPrModel.get(query)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(_reinvestment.get(query)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(_revenue.get(query)).append("\n");
			buff.append("\t").append("Spend Limit: ").append(setQuerySpendLimit(query)).append("\n");
			buff.append("\t").append("Slot: ").append(_queryReport.getPosition(query)).append("\n");
			System.out.print(buff);
			//output.append(buff);
			
		}
	   output.flush();
	   counter++;
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
	
		_reinvestment = new HashMap<Query,Double>();
		for (Query query : _querySpace){
			_reinvestment.put(query,0.9);
		}
		
		_revenue = new HashMap<Query, Double>();
		for (Query query: _querySpace){
			if (query.getManufacturer() == _manSpecialty) _revenue.put(query, 15.0);
			else _revenue.put(query,10.0);
		}
		
		//from JESOM2
		double slice = _capacity/(20*_capWindow);
		_wantedSales = new HashMap<Query,Double>();
		for (Query query: _querySpace){
			if(query.getManufacturer() == _manSpecialty) _wantedSales.put(query, 2*slice);
			else _wantedSales.put(query, slice);
		}
		
		
		/*
		try {
			output = new PrintStream(new File("log.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		
		
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
	public void updateModels(SalesReport salesReport,
			QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
   
	protected double getQueryBid(Query q)
	{
		return _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity)*_reinvestment.get(q)*_revenue.get(q);
	}
	
	protected void handleNoImpression(Query q){
	    if(Double.isNaN(_queryReport.getPosition(q))){
	    	 double increase = Math.max(_reinvestment.get(q)*1.3, _reinvestment.get(q)+0.1);
	    	 if(increase > 0.95) _reinvestment.put(q,0.95);
	    	 else _reinvestment.put(q, increase);
	    	
		}
	    
   }
	
   protected void handlePromotedSlots(Query q){
			if(_queryReport.getPosition(q) < 1.1){
				//if(_reinvestment.get(q)*0.6 <= 0.1) _reinvestment.put(q, 0.1);
				//else _reinvestment.put(q,_reinvestment.get(q)*0.6);
				_reinvestment.put(q,_queryReport.getCPC(q)/(_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity)*_revenue.get(q)));
			}
			if(_queryReport.getPosition(q)< 2.1 && _queryReport.getPosition(q) > 1.1 && _numPS ==2){
				//if(_reinvestment.get(q)*0.6 <= 0.2) _reinvestment.put(q, 0.1);
				//else _reinvestment.put(q,_reinvestment.get(q)*0.8);
				_reinvestment.put(q,_queryReport.getCPC(q)/(_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity)*_revenue.get(q)));
			}
  }   
   
   protected void walking(Query q){
	   if(getQueryBid(q) <= 0.25) return;
		  if((_queryReport.getPosition(q) > 1 || (_queryReport.getPosition(q) > 2 && _numPS == 2)) && _queryReport.getPosition(q) <= 5){
		      Random random = new Random();
		      double current = _reinvestment.get(q);
			  double currentBid = getQueryBid(q);
			  double y = currentBid/current;
			  double distance = Math.abs(currentBid - _queryReport.getCPC(q));
			  double rfDistance = (current - (distance/y))/10;
			
		      if(random.nextDouble() < 0.5){
		        if(current + rfDistance >= 0.90)  _reinvestment.put(q,0.90);
		        else _reinvestment.put(q,current + rfDistance);
		      }
		      else{
                 
                if(current - rfDistance <= 0.1)	_reinvestment.put(q,0.1);	    	
                else  _reinvestment.put(q, current - rfDistance);
                //_reinvestment.put(q,_queryReport.getCPC(q)/_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity)*_revenue.get(q));
		      }
	       }
   }
  
   //from JESOM2
   protected void modify(Query q){
	   if(Double.isNaN(_queryReport.getCPC(q))) return; 
	   double _conversionPr = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity);
	   if(_queryReport.getClicks(q) < _wantedSales.get(q)/_conversionPr)
	   {
		   if(_queryReport.getPosition(q)< 4) _wantedSales.put(q,_wantedSales.get(q)*0.625);
		   else{
			   /*if(_wantedSales.get(q) < (_capacity - _unitsSoldModel.getWindowSold())/magicDivisor){
				  double newReinvest = _reinvestment.get(q)*1.3 + .1;
				  if(newReinvest >= 0.95) _reinvestment.put(q, 0.95);
				  else _reinvestment.put(q,newReinvest);
			   }*/
		   }
	   }
	   else{
		  if (_queryReport.getPosition(q) >= 4 || _queryReport.getCPC(q) < .2){
			  _wantedSales.put(q,_wantedSales.get(q)*1.6);
		  }
		  else{
			  /*
			  double cpc = _queryReport.getCPC(q)-.01;
		      double newReinvest = _queryReport.getCPC(q) / (_revenue.get(q) * _conversionPr);
			  if(newReinvest >= 0.95) _reinvestment.put(q, 0.95);
			  else _reinvestment.put(q,newReinvest);*/
		  }
	   }
		
	   
   }
   
   //mix JESOM2 & SSB
	protected double setQuerySpendLimit(Query q) {
		
		double conversionPr = _conversionPrModel. get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		double bid = getQueryBid(q);
		double clicks = Math.max(1, _wantedSales.get(q)/conversionPr);
		
		double remainCap = _capacity - _unitsSoldModel.getWindowSold();
		if(remainCap < 0) remainCap = 0;
		//if(_capacity == 500) return bid*remainCap/8;
		//else return bid*clicks;
		return Math.max(bid*remainCap/8,bid*clicks);
	}
	
 
	
}