package agents;

import java.util.Hashtable;
import java.util.Set;

import agents.rules.AdjustConversionPr;
import agents.rules.ConversionPr;
import agents.rules.DistributionCap;
import agents.rules.ManufacurerBonus;
import agents.rules.NoImpressions;
import agents.rules.ReinvestmentCap;
import agents.rules.Targeted;
import agents.rules.TopPosition;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class SSBAgent extends AbstractAgent {
	protected SSBBidStrategy _bidStrategy;
	protected DistributionCap _distributionCap;
	protected ReinvestmentCap _reinvestmentCap;
	protected TopPosition _topPosition;
	protected NoImpressions _noImpressions;
	protected AdjustConversionPr _adjustConversionPr;
	protected Hashtable<Query,Double> _baseLineConversion;
	
	public SSBAgent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
		_baseLineConversion = new Hashtable<Query,Double>();
		_bidStrategy = new SSBBidStrategy(_querySpace);
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		String manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		
		_distributionCap = new DistributionCap(distributionCapacity, distributionWindow);
		_reinvestmentCap = new ReinvestmentCap(0.80);
		_topPosition = new TopPosition(_advertiserInfo.getAdvertiserId(), 0.05);
		_noImpressions = new NoImpressions(_advertiserInfo.getAdvertiserId(), 0.10);
		
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ZERO)) {_baseLineConversion.put(q, 0.1);}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ONE)) {_baseLineConversion.put(q, 0.2);}
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_TWO)) {_baseLineConversion.put(q, 0.3);}
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		
		_adjustConversionPr = new AdjustConversionPr(distributionCapacity, distributionWindow, _baseLineConversion, componentSpecialty);
		
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
		new Targeted().apply(F2componentSpecialty, _bidStrategy);
	}
	

	
	
	@Override
	protected void updateBidStrategy() {
		QueryReport qr = _queryReports.remove();
		_topPosition.updateReport(qr);
		_noImpressions.updateReport(qr);
		
		SalesReport sr = _salesReports.remove();
		//_distributionCap.updateReport(sr);
		_adjustConversionPr.updateReport(sr);
		
		//int oversold = _adjustConversionPr.getOversold();
		//if(oversold > _advertiserInfo.getDistributionCapacity()/2){
		
		//}
		
		_adjustConversionPr.apply(_bidStrategy);
		
		_topPosition.apply(_bidStrategy);
		_noImpressions.apply(_bidStrategy);
		
		_reinvestmentCap.apply(_bidStrategy);
		//_distributionCap.apply(_bidStrategy);
	}
	
	
	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	
}
