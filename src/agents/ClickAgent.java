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

public class ClickAgent extends SimAbstractAgent{
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
    protected HashMap<Query, Double> _revenue;
    protected HashMap<Query, Double> _PM;
    protected double _desiredSale;
    protected final double _lamda = 0.9;
    protected BidBundle _bidBundle;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		for(Query q: _querySpace){ 
			adjustPM(q);
			_bidBundle.setBid(q, getQueryBid(q));
			_bidBundle.setDailyLimit(q, setQuerySpendLimit(q));
		}
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		// TODO Auto-generated method stub
		_desiredSale = _capacity/_capWindow/_querySpace.size();
		
		  _unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);
			
		_conversionPrModel = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			_conversionPrModel.put(query, new SimplePrConversion(query, _advertiserInfo, _unitsSoldModel));	
		}
			
		_PM = new HashMap<Query, Double>();
			for(Query q: _querySpace){
				_PM.put(q, 0.5);
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
	
	protected void adjustPM(Query q){
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		//if we does not get enough clicks, then decrease PM (increase bids, and hence slot)
			    if(_queryReport.getClicks(q)*conversion < _desiredSale){
			    		double newHonest = (1-_PM.get(q))*1.3 + .1;
      		               if(newHonest >= 0.9) newHonest = 0.9;
      		               _PM.put(q, 1-newHonest);
			              
			    }else{
			    	//if we get too many clicks, increase PM(decrease bids and hence slot)
			            double  newHonest = (_queryReport.getCPC(q)-0.01)/(_revenue.get(q)*conversion);
						if(newHonest < 0.1) newHonest = 0.1;
						_PM.put(q,1-newHonest);
			    }
	}
	   
	protected double setQuerySpendLimit(Query q){
		double conversion = _conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()- _capacity);
		/*double remainingCap = _capacity - _unitsSoldModel.getWindowSold();
		if(remainingCap < 0) remainingCap = 0;
		else if (_day < 2) remainingCap = _capacity/_capWindow;*/
		double remainingCap = _capacity/_capWindow;
		return _lamda*remainingCap/(16*conversion);
	}

	
	protected double getQueryBid(Query q){
	    return _revenue.get(q)*(1-_PM.get(q))*_conversionPrModel.get(q).getPrediction(_unitsSoldModel.getWindowSold()-_capacity);	
	}
}
