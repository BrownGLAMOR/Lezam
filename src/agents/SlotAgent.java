package agents;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class SlotAgent extends SimAbstractAgent{
	
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected NewAbstractConversionModel _conversionPrModel;
    protected HashMap<Query, Double> _reinvestment;
    protected HashMap<Query, Double> _revenue;
    protected BidBundle _bidBundle;
    protected HashMap<Query, Double> _baselineConversion;
	protected final int MAX_TIME_HORIZON = 5;
    
	protected PrintStream output;
    
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		for (Query query : _querySpace) {			
			double current = _reinvestment.get(query);
			
			//handle the case of no impression (the agent got no slot)
			handleNoImpression(query, current);
			//handle the case when the agent got the promoted slots
			handlePromotedSlots(query);
			//walk otherwise
			walking(query, current);
			
			_bidBundle.setBid(query, getQueryBid(query));
	
			//_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
			
			//print out the properties
			StringBuffer buff = new StringBuffer("");
			buff.append("\t").append("day").append(_day).append("\n");
			buff.append("\t").append("product: ").append(query.getManufacturer()).append(", ").append(query.getComponent());
			buff.append("\t").append("bid: ").append(getQueryBid(query)).append("\n");
			buff.append("\t").append("Conversion: ").append(_salesReport.getConversions(query)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(_reinvestment.get(query)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(_revenue.get(query)).append("\n");
			buff.append("\t").append("Spend Limit: ").append(setQuerySpendLimit(query)).append("\n");
			buff.append("\t").append("Slot: ").append(_queryReport.getPosition(query)).append("\n");
			System.out.print(buff);
			//output.append(buff);
			
		}
//	   output.flush();
		return _bidBundle;
	}

	@Override
	public void initBidder() {
	  _reinvestment = new HashMap<Query,Double>();
		for (Query query : _querySpace){
			_reinvestment.put(query,0.4);
		}
		

		_revenue= new HashMap<Query, Double>();
		for(Query query:_querySpace){
			if(query.getType()== QueryType.FOCUS_LEVEL_ZERO){
				_revenue.put(query, 10.0 + 5/3);
			}
			if(query.getType()== QueryType.FOCUS_LEVEL_ONE){
				if(_manSpecialty.equals(query.getManufacturer())) _revenue.put(query, 15.0);
				else{
					if(query.getManufacturer() != null) _revenue.put(query, 10.0);
					else _revenue.put(query, 10.0 + 5/3);
				}
			}
			if(query.getType()== QueryType.FOCUS_LEVEL_TWO){
				if(_manSpecialty.equals(query.getManufacturer())) _revenue.put(query, 15.0);
				else _revenue.put(query, 10.0);
			}
		}

		_baselineConversion = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_baselineConversion.put(q, 0.1);
			if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				if (q.getComponent() == _compSpecialty)
					_baselineConversion.put(q, 0.27);
				else
					_baselineConversion.put(q, 0.2);
			}
			if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				if (q.getComponent() == _compSpecialty)
					_baselineConversion.put(q, 0.39);
				else
					_baselineConversion.put(q, 0.3);
			}
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
			
			//_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}
		
	}
   
	protected double getQueryBid(Query q)
	{
		double conversion;
		if(_day <= 5) conversion = _baselineConversion.get(q);
		else conversion = _conversionPrModel.getPrediction(q);
		return conversion*_reinvestment.get(q)*_revenue.get(q);
	}
	
	protected void handleNoImpression(Query q, double currentReinvest){
	    if(Double.isNaN(_queryReport.getPosition(q))){
	    	 double increase = Math.min(0.95,Math.max(currentReinvest*1.3,currentReinvest+0.1));
	    	 _reinvestment.put(q, increase);
	    	
		}
	    
   }
	
   protected void handlePromotedSlots(Query q){
	     double conversion;
		if(_day <= 5) conversion = _baselineConversion.get(q);
		else conversion = _conversionPrModel.getPrediction(q);
			if(_queryReport.getPosition(q) < 1.1){
				double newReinvest = Math.min(0.1,_queryReport.getCPC(q)/conversion*_revenue.get(q));
				_reinvestment.put(q,newReinvest);
			}
			if(_reinvestment.get(q) > 0.95) _reinvestment.put(q,0.95);
  }   
   
   protected void walking(Query q, double currentReinvest){

	   if((_queryReport.getPosition(q) > 1) && _queryReport.getPosition(q) < 5){
		      Random random = new Random();
			  double currentBid = getQueryBid(q);
			  double y = currentBid/currentReinvest;
			  double distance = Math.abs(currentBid - _queryReport.getCPC(q));
			  double rfDistance = (currentReinvest - (distance/y))/10;
			
		      if(random.nextDouble() < 0.5){
		        if(currentReinvest + rfDistance >= 0.90)  _reinvestment.put(q,0.90);
		        else _reinvestment.put(q,currentReinvest + rfDistance);
		      }
		      else{
                 
                if(currentReinvest - rfDistance <= 0.1)	_reinvestment.put(q,0.1);	    	
                else  _reinvestment.put(q, currentReinvest - rfDistance);
		      }
	       }
   }
 
   
	protected double setQuerySpendLimit(Query q) {
			
		double remainCap = _capacity/_capWindow;
		return getQueryBid(q)*remainCap/_conversionPrModel.getPrediction(q)/_querySpace.size()*.9;
	}
	
	@Override
	public Set<AbstractModel> initModels() {
		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity,
				_capWindow);

		_conversionPrModel = new GoodConversionPrModel(_querySpace);
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport,
			QueryReport queryReport) {
       if( (salesReport != null) && (queryReport != null)) {
			
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			_conversionPrModel.setTimeHorizon(timeHorizon);
			_conversionPrModel.updateModel(queryReport, salesReport);	
       }
	}
	
}
