package agents;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

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

public class temp extends SimAbstractAgent{
	
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
    protected HashMap<Query, Double> _reinvestmentModel;
    protected HashMap<Query, Double> _revenueModel;
    protected BidBundle _bidBundle;
    
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		_unitsSoldModel.update(_salesReport);
		
		for (Query query : _querySpace) {
			
			_conversionPrModel.get(query).setPrediction(_unitsSoldModel.getWindowSold() - _capacity);
			
			
			//adjust reinvestment factor
			//handle the case of no impression (the agent got no slot)
			handleNoImpression(query);
			//handle the case when the agent got the promoted slots
			handlePromotedSlots(query);
			//walk otherwise
			walking(query);
			
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
			buff.append("\t").append("bid: ").append(getQueryBid(query)).append("\n");
			buff.append("\t").append("Conversion: ").append(_conversionPrModel.get(query)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(_reinvestmentModel.get(query)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(_revenueModel.get(query)).append("\n");
			buff.append("\t").append("Spend Limit: ").append(setQuerySpendLimit(query)).append("\n");
			System.out.print(buff);
		}
	
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		// TODO Auto-generated method stub
		
		_unitsSoldModel = new UnitsSoldMovingAvg(_capWindow);
		
		_conversionPrModel = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			_conversionPrModel.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
		}
	
		_reinvestmentModel = new HashMap<Query,Double>();
		for (Query query : _querySpace){
			_reinvestmentModel.put(query,0.9);
		}
		
		_revenueModel = new HashMap<Query, Double>();
		for (Query query: _querySpace){
			if (query.getManufacturer() == _manSpecialty) _revenueModel.put(query, 15.0);
			else _revenueModel.put(query,10.0);
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
	public void updateModels(SalesReport salesReport,
			QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
   
	protected double getQueryBid(Query q)
	{
		return _conversionPrModel.get(q).getPrediction()*_reinvestmentModel.get(q)*_revenueModel.get(q);
	}
	
	protected void handleNoImpression(Query q){
		if(getQueryBid(q) <= 0.25) return;//this may be a bad idea, need bidding from yesterday... (cjc)
	    if(Double.isNaN(_queryReport.getPosition(q))){
				_reinvestmentModel.put(q,_reinvestmentModel.get(q)*1.3);
		}
	    
   }
	
   protected void handlePromotedSlots(Query q){
	   if(getQueryBid(q) <= 0.25) return;
			if(_queryReport.getPosition(q) < 1.1){
				if(_reinvestmentModel.get(q) <= 0.2) _reinvestmentModel.put(q, 0.1);
				_reinvestmentModel.put(q,_reinvestmentModel.get(q)-0.1);
			}
			if(_queryReport.getPosition(q)< 2.1 && _numPS ==2){
				if(_reinvestmentModel.get(q) <= 0.2) _reinvestmentModel.put(q, 0.1);
				_reinvestmentModel.put(q,_reinvestmentModel.get(q)-0.1);
			}
  }   
   
   protected void walking(Query q){
	   if(getQueryBid(q) <= 0.25) return;
		  if((_queryReport.getPosition(q) > 2 || (_queryReport.getPosition(q) > 1 && _numPS == 2)) && _queryReport.getPosition(q) <= 5){
		      Random random = new Random();
		      double current = _reinvestmentModel.get(q);
			  double currentBid = getQueryBid(q);
			  double y = currentBid/current;
			  double distance = Math.abs(currentBid - _queryReport.getCPC(q));
			  double rfDistance = (current - (distance/y))/10;
			
		      if(random.nextGaussian() < 0){
		        if(current + rfDistance >= 0.95)  _reinvestmentModel.put(q,0.95);
		        else _reinvestmentModel.put(q,current + rfDistance);
		      }
		      else{
                 
                if(current - rfDistance <= 0.1)	_reinvestmentModel.put(q,0.1);	    	
                else  _reinvestmentModel.put(q, current - rfDistance);
		      
		      }
	       }
   }
   
	protected double setQuerySpendLimit(Query q) {
		double remainingCap = _capacity - _unitsSoldModel.getWindowSold();
		if(remainingCap < 0){
			remainingCap = 0;
		}
		//8 is the magic divisor
		return getQueryBid(q)*remainingCap/8;
	}
	
 
}