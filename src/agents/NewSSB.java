package agents;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class NewSSB extends AbstractAgent{

	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected AbstractConversionModel _conversionPrModel;
	protected HashMap<Query, Double> _reinvestment;
	protected HashMap<Query, Double> _revenue;
	protected BidBundle _bidBundle;
	protected int counter = 1;

	protected double magicDivisor = 8;

	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		if(_salesReport == null || _queryReport == null) {
			return new BidBundle();
		}
		_unitsSoldModel.update(_salesReport);

		for (Query query : _querySpace) {

			_conversionPrModel.getPrediction(query);

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

			//			//print out the properties
			//			StringBuffer buff = new StringBuffer("");
			//			buff.append("\t").append("day").append(counter).append("\n");
			//			buff.append("\t").append("product: ").append(query.getManufacturer()).append(", ").append(query.getComponent());
			//			buff.append("\t").append("bid: ").append(getQueryBid(query)).append("\n");
			//			buff.append("\t").append("Conversion: ").append(_conversionPrModel.get(query)).append("\n");
			//			buff.append("\t").append("ReinvestFactor: ").append(_reinvestment.get(query)).append("\n");
			//			buff.append("\t").append("ConversionRevenue: ").append(_revenue.get(query)).append("\n");
			//			buff.append("\t").append("Spend Limit: ").append(setQuerySpendLimit(query)).append("\n");
			//			buff.append("\t").append("Slot: ").append(_queryReport.getPosition(query)).append("\n");
			//			System.out.print(buff);
			//			//output.append(buff);

		}
		//	   output.flush();
		counter++;
		return _bidBundle;
	}

	@Override
	public void initBidder() {

		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);

		BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty,_compSpecialty);

		_conversionPrModel = new HistoricPrConversionModel(_querySpace,basicTargModel);

		_reinvestment = new HashMap<Query,Double>();
		for (Query query : _querySpace){
			_reinvestment.put(query,0.9);
		}

		_revenue = new HashMap<Query, Double>();
		for (Query query: _querySpace){
			if (query.getManufacturer() == _manSpecialty) _revenue.put(query, 15.0);
			else _revenue.put(query,10.0);
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

		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport,
			QueryReport queryReport) {


	}

	protected double getQueryBid(Query q)
	{
		return _conversionPrModel.getPrediction(q)*_reinvestment.get(q)*_revenue.get(q);
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
			_reinvestment.put(q,_queryReport.getCPC(q)/(_conversionPrModel.getPrediction(q)*_revenue.get(q)));
		}
		if(_queryReport.getPosition(q)< 2.1 && _queryReport.getPosition(q) > 1.1 && _numPS ==2){
			//if(_reinvestment.get(q)*0.6 <= 0.2) _reinvestment.put(q, 0.1);
			//else _reinvestment.put(q,_reinvestment.get(q)*0.8);
			_reinvestment.put(q,_queryReport.getCPC(q)/(_conversionPrModel.getPrediction(q)));
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


	protected double setQuerySpendLimit(Query q) {

		double remainCap = _capacity - _unitsSoldModel.getWindowSold();
		if(remainCap < 0) remainCap = 0;
		return getQueryBid(q)*remainCap/8;
	}



}