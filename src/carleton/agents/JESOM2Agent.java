package carleton.agents;

import java.util.Hashtable;
import java.util.Set;

import carleton.strategies.JESOM2BidStrategy;

import modelers.unitssold.UnitsSoldModel;
import modelers.unitssold.UnitsSoldModelMean;

import agents.rules.ConversionPr;
import agents.rules.DistributionCap;
import agents.rules.ManufacurerBonus;
import agents.rules.SetProperty;
import agents.rules.Targeted;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class JESOM2Agent extends CarletonAbstractAgent {
	protected JESOM2BidStrategy _bidStrategy;
	protected Hashtable<Query,Double> _baseLineConversion;
	protected DistributionCap _distributionCap;

	protected UnitsSoldModel _unitsSold;

	/*
	 * Commented by Max. All comments made by spucci are marked with "(spucci)"
	 * 
	 * Simplifying assumptions:
	 * 
	 * - CPC is close to bid
	 * - conversion probability is close to what it would be with no informational searchers
	 * - the 2 assumptions above sort of cancel each other out
	 * 
	 * 
	 * 
	 * Constants:
	 * 
	 * - reinvestment caps
	 * 
	 * 
	 * 
	 * Models:
	 * 
	 * - ConversionPrModel;
	 * - ConversionPrModelNoIS;
	 * - UnitsSoldModel; (also in: DistributionCap.java)
	 * - UnitsSoldModelMean;
	 * 
	 */

	public JESOM2Agent(){}

	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
		_baseLineConversion = new Hashtable<Query,Double>();
		_bidStrategy = new JESOM2BidStrategy(_querySpace);
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		String manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		_unitsSold = new UnitsSoldModelMean(distributionWindow); //new UnitsSoldModelMaxWindow(distributionWindow);
		_distributionCap = new DistributionCap(distributionCapacity, _unitsSold, 8);
		
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ZERO)) {_baseLineConversion.put(q, 0.1);}  // constant set by game server info
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ONE)) {_baseLineConversion.put(q, 0.2);}  // constant set by game server info
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_TWO)) {_baseLineConversion.put(q, 0.3);}  // constant set by game server info
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());

		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy); // constant set by game server info
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), _bidStrategy); // constant set by game server info
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy); // constant set by game server info

		new ManufacurerBonus(manufacturerBonus).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy);

		Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy); // constant set by game server info
		for(Query q : F1componentSpecialty) {_baseLineConversion.put(q, 0.27);} 

		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy); // constant set by game server info
		for(Query q : F2componentSpecialty) {_baseLineConversion.put(q, 0.39);} 


		//new Targeted().apply(F1componentSpecialty, _bidStrategy);
		new Targeted().apply(F2componentSpecialty, _bidStrategy);
		

		int slice = distributionCapacity/(20*distributionWindow);
		new SetProperty(JESOM2BidStrategy.WANTED_SALES, slice).apply(_querySpace, _bidStrategy); //constant to be entered by user
		new SetProperty(JESOM2BidStrategy.WANTED_SALES, 2*slice).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy); //constant to be entered by user

		new SetProperty(JESOM2BidStrategy.HONESTY_FACTOR, 0.4).apply(_querySpace, _bidStrategy); //constant to be entered by user
	}




	@Override
	protected void updateBidStrategy() {
		QueryReport qr = _queryReports.remove();


		SalesReport sr = _salesReports.remove();
		_unitsSold.updateReport(sr);

		//_adjustConversionPr.apply(_bidStrategy); (spucci)

		if (qr!=null){
			for (Query q : _querySpace){//_bidStrategy.getQuerySpace()){ (spucci)
				double clicksGot = qr.getClicks(q);//_unitsSold.getYesterday(); (spucci)
				double wantedSales = _bidStrategy.getProperty(q, JESOM2BidStrategy.WANTED_SALES);
				double honestyFactor = _bidStrategy.getProperty(q, JESOM2BidStrategy.HONESTY_FACTOR);
				double conversionRevenue = _bidStrategy.getProperty(q, JESOM2BidStrategy.CONVERSION_REVENUE);
				double conversionPr = _bidStrategy.getProperty(q, JESOM2BidStrategy.CONVERSION_PR);

				int distributionCapacity = _advertiserInfo.getDistributionCapacity();
				int distributionWindow = _advertiserInfo.getDistributionWindow();
				int remainingCap = distributionCapacity - _unitsSold.getWindowSold();
				int magicDivisor = 8;
				
				// if we are overselling, lower our bid
				if (_bidStrategy.getProperty(q, JESOM2BidStrategy.CONVERSION_PR) < _baseLineConversion.get(q) ) {
					double cpc = qr.getCPC(q)-.01;
					double newHonesty = cpc / (conversionRevenue * conversionPr);
					_bidStrategy.setProperty(q, JESOM2BidStrategy.HONESTY_FACTOR, newHonesty);
				}
				else if (clicksGot < wantedSales/conversionPr){
					// Didn't get enough sales, but had a good position (spucci)
					// -> raise other sales
					if (qr.getPosition(q) < 4){  //constant to be entered by user (spucci)
						_bidStrategy.setProperty(q, JESOM2BidStrategy.WANTED_SALES, wantedSales*.625); //constant to be entered by user
					}
					// Not enough sales, bad position (spucci)
					// -> raise bid (spucci)
					else if (wantedSales < remainingCap/magicDivisor) {
						_bidStrategy.setProperty(q, JESOM2BidStrategy.HONESTY_FACTOR, honestyFactor*1.3 + .1); //constant to be entered by user
						
						System.out.println("\n=============" + q + "\nGot: " + clicksGot + ", Wanted: " +
								(wantedSales/conversionPr) + ", Position: " + qr.getPosition(q) + "\n remainingCap: "+ remainingCap + "\n=============\n");
						
					}
				}
				else {
					// Have enough sales, and our slot is quite low (spucci)
					// -> increase desired sales in this query
					if (!(qr.getPosition(q) < 4) || qr.getCPC(q) < .2){
						_bidStrategy.setProperty(q, JESOM2BidStrategy.WANTED_SALES, wantedSales*1.6);
					}
					// Have enough sales, and our slot is high (spucci)
					// -> lower bid (spucci)
					else{
						double cpc = qr.getCPC(q)-.01;
						double newHonesty = cpc / (conversionRevenue * conversionPr);
						_bidStrategy.setProperty(q, JESOM2BidStrategy.HONESTY_FACTOR, newHonesty);
					}
				}
				
				// set spending limit
				double bid = _bidStrategy.getQueryBid(q);
				double clicks = Math.max(1,_bidStrategy.getProperty(q,_bidStrategy.WANTED_SALES)) / _bidStrategy.getProperty(q,_bidStrategy.CONVERSION_PR);
				_bidStrategy.setQuerySpendLimit(q, bid * clicks);
				//_distributionCap.apply(_bidStrategy);
				
				
			}
		}
		


	}


	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}


}
