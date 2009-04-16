package agents;

import java.util.Hashtable;
import java.util.Set;

import modelers.UnitsSoldModel;
import modelers.UnitsSoldModelMaxWindow;

import agents.rules.AdjustConversionPr;
import agents.rules.ConversionPr;
import agents.rules.ManufacurerBonus;
import agents.rules.SetProperty;
import agents.rules.Targeted;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class JESOM2Agent extends AbstractAgent {
	protected JESOM2BidStrategy _bidStrategy;
	
	protected AdjustConversionPr _adjustConversionPr;
	protected Hashtable<Query,Double> _baseLineConversion;
	
	protected UnitsSoldModel _unitsSold;
	
	
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
		
		_unitsSold = new UnitsSoldModelMaxWindow(distributionWindow);
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ZERO)) {_baseLineConversion.put(q, 0.1);}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ONE)) {_baseLineConversion.put(q, 0.2);}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_TWO)) {_baseLineConversion.put(q, 0.3);}
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		
		_adjustConversionPr = new AdjustConversionPr(distributionCapacity, _unitsSold, _baseLineConversion, componentSpecialty);
		
		
		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy);
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), _bidStrategy);
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy);
		
		new ManufacurerBonus(manufacturerBonus).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy);
		
		Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy);
		//for(Query q : F1componentSpecialty) {_baseLineConversion.put(q, 0.27);}
		
		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy);
		//for(Query q : F2componentSpecialty) {_baseLineConversion.put(q, 0.39);}
		
		
		//??? new Targeted().apply(F1componentSpecialty, _bidStrategy);
		//new Targeted().apply(F2componentSpecialty, _bidStrategy);
		
		int slice = distributionCapacity/(20*distributionWindow);
		new SetProperty(JESOM2BidStrategy.WANTED_SALES, slice).apply(_querySpace, _bidStrategy);
		new SetProperty(JESOM2BidStrategy.WANTED_SALES, 2.5*slice).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy);
		
		new SetProperty(JESOM2BidStrategy.HONESTY_FACTOR, 0.4).apply(_querySpace, _bidStrategy);
	}
	

	
	
	@Override
	protected void updateBidStrategy() {
		QueryReport qr = _queryReports.remove();
		
		
		SalesReport sr = _salesReports.remove();
		_unitsSold.updateReport(sr);
		
		//_adjustConversionPr.apply(_bidStrategy);
		
		if (qr!=null){
			for (Query q : _querySpace){//_bidStrategy.getQuerySpace()){
				double conversionsGot = sr.getConversions(q);//_unitsSold.getYesterday();
				double wantedSales = _bidStrategy.getProperty(q, JESOM2BidStrategy.WANTED_SALES);
				
				//double clicksAim = _bidStrategy.getQuerySpendLimit(q)/_bidStrategy.getQueryBid(q);
				if (conversionsGot < wantedSales){
					double honestyFactor = _bidStrategy.getProperty(q, JESOM2BidStrategy.HONESTY_FACTOR);
					_bidStrategy.setProperty(q, JESOM2BidStrategy.HONESTY_FACTOR, honestyFactor*1.14 + .01);
					
					//_bidStrategy.setQueryBudget(q, _bidStrategy.getQueryBid(q)*clicksAim);
				}
				else {
					double conversionRevenue = _bidStrategy.getProperty(q, JESOM2BidStrategy.CONVERSION_REVENUE);
					double conversionPr = _bidStrategy.getProperty(q, JESOM2BidStrategy.CONVERSION_PR);
					double cpc = qr.getCPC(q)+.01;
					//just in case the CPC is 0.0
					
					double newHonesty = cpc / (conversionRevenue * conversionPr);
					
					_bidStrategy.setProperty(q, JESOM2BidStrategy.HONESTY_FACTOR, newHonesty);
				}
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
