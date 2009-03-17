package agents;

import java.util.Iterator;
import java.util.Set;

import agents.rules.ConversionPr;
import agents.rules.DistributionCap;
import agents.rules.ManufacurerBonus;
import agents.rules.NoSlot;
import agents.rules.ReinvestmentCap;
import agents.rules.Targeted;
import agents.rules.TopSlot;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class SSBAgent extends AbstractAgent {
	protected SSBBidStrategy _bidStrategy;
	protected DistributionCap _distributionCap;
	protected ReinvestmentCap _reinvestmentCap;
	protected TopSlot _topSlot;
	protected NoSlot _noSlot;
	
	public SSBAgent(){}
	
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
		_bidStrategy = new SSBBidStrategy(_querySpace);
		
		_distributionCap = new DistributionCap(_advertiserInfo.getDistributionCapacity(), _advertiserInfo.getDistributionWindow());
		_reinvestmentCap = new ReinvestmentCap(0.80);
		_topSlot = new TopSlot(_advertiserInfo.getAdvertiserId(), 0.10);
		_noSlot = new NoSlot(_advertiserInfo.getAdvertiserId(), 0.10);
		
		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy);
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), _bidStrategy);
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy);
		
		new ManufacurerBonus(_advertiserInfo.getManufacturerBonus()).apply(_queryManufacturer.get(_advertiserInfo.getManufacturerSpecialty()), _bidStrategy);
		
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		
		Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy);
		
		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy);
		
		new Targeted().apply(F2componentSpecialty, _bidStrategy);
	}
	
	@Override
	protected void updateBidStratagy() {
		QueryReport qr = _queryReports.remove();
		_topSlot.updateReport(qr);
		_noSlot.updateReport(qr);
		
		SalesReport sr = _salesReports.remove();
		_distributionCap.updateReport(sr);
		
		_topSlot.apply(_bidStrategy);
		_noSlot.apply(_bidStrategy);
		_reinvestmentCap.apply(_bidStrategy);
		_distributionCap.apply(_bidStrategy);
	}
	
	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	
	

	
}
