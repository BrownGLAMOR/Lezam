package agents;

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

public class NewG3 extends SimAbstractAgent{
    HashMap<Query, Double> _baselineConv;
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		return null;
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
	}

	@Override
	public Set<AbstractModel> initModels() {
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		
	}


   public double solveK(){
	  double dailyLimit = _capacity/5;
	  //sum of prob(conv) across queries
	  double sum1 = 0.0;
	  //sum of 5*prob(conv) for queries with manufacture specialty
	  double sum2 = 0.0;
	  for (Query query:_querySpace){
		  if(query.getManufacturer().equals(_manSpecialty)){
			  sum1 += _baselineConv.get(query);
			  sum2 += _baselineConv.get(query)*5;
		  }
		  else sum1 += _baselineConv.get(query);
	  }
	  return (dailyLimit - sum2)/sum1;
   }
   
}