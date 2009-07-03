package agents;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.NewAbstractConversionModel;
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
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected NewAbstractConversionModel _conversionPrModel;
	protected HashMap<Query, Double> _baselineConv;
	protected HashMap<Query,Double> _estimatedPrice;
	protected AbstractBidToCPC _bidToCPC;
	//k is a constant that equates EPPS across queries
	protected double k;
	protected BidBundle _bidBundle;
	protected ArrayList<BidBundle> _bidBundles;

	protected PrintStream output;
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		if (_salesReport != null && _queryReport != null) {
			updateK();
		}

		for(Query query: _querySpace){
			_bidBundle.setBid(query, getQueryBid(query));
			//_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}

		_bidBundles.add(_bidBundle);

		return _bidBundle;
	}

	@Override
	public void initBidder() {

		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);

		_conversionPrModel = new GoodConversionPrModel(_querySpace);

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

		_bidBundle = new BidBundle();
		for (Query query : _querySpace) {	
			_bidBundle.setBid(query, getQueryBid(query));
		}

		_bidBundles = new ArrayList<BidBundle>();

		initializeK();
		
		try {
			output = new PrintStream(new File("logg3.txt"));
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

			_unitsSoldModel.update(_salesReport);
			_conversionPrModel.updateModel(queryReport, salesReport);
			
			if (_bidBundles.size() > 1) 
				_bidToCPC.updateModel(_queryReport, _bidBundles.get(_bidBundles.size() - 2));

		}

	}


	protected double updateK(){
		double dailyLimit = _capacity/_capWindow;
		double sum = 0.0;
		for(Query query:_querySpace){
			sum+= _salesReport.getConversions(query);
		}
		if(sum <= 0.9*dailyLimit) k = k*0.9;
		if(sum >= 1.3*dailyLimit) k = k*1.1;

		if (k > 10)  k = 10;
		if (k < 1) k = 1;
		return k;
	}

	protected double cpcTobid(double cpc, Query query){
		return cpc + .1;
		/*if (_day <= 6) return cpc + .1;
		double bid = cpc;
		while (_bidToCPC.getPrediction(query,bid,_bidBundles.get(_bidBundles.size() - 2)) < cpc){
			bid += 0.1;
		}     
		return bid;*/
	}

	protected void initializeK(){
		//will change later
		k = 8;
	}

	protected double getQueryBid(Query q){
		double bid = 0.0;
		if(_day <= 2){
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
		
		else bid = cpcTobid((_estimatedPrice.get(q) - k)*_conversionPrModel.getPrediction(q),q);

		if(bid <= 0) return 0;
		else{
			if(bid > 2.5) return 2.5;
			else return bid;
		}

	}

	protected double setQuerySpendLimit(Query q){
	     return 0.0;
	}

}