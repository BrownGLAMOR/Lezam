package agents;

import java.util.LinkedHashSet;
import java.util.Set;

import agents.rules.ConversionPr;
import agents.rules.NoSlot;
import agents.rules.ReinvestmentCap;
import agents.rules.Targeted;
import agents.rules.TopSlot;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;

/*
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.isl.transport.Transportable;
import edu.umich.eecs.tac.props.*;
*/

public class SSBAgent extends AbstractAgent {
	protected SSBBidStrategy _bidStrategy;
	protected ReinvestmentCap _reinvestmentCap;
	protected TopSlot _topSlot;
	protected NoSlot _noSlot;
	
	public SSBAgent(){}
	
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		_bidStrategy = new SSBBidStrategy(_querySpace);
		
		_reinvestmentCap = new ReinvestmentCap(0.80);
		_topSlot = new TopSlot(_advertiserInfo.getAdvertiserId(), 0.10);
		_noSlot = new NoSlot(_advertiserInfo.getAdvertiserId(), 0.10);
		
		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy);
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), _bidStrategy);
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy);
		
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		//new Targeted().apply(componentSpecialty, _bidStrategy);
		
		//Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		//new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy);
		
		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy);
		
		new Targeted().apply(F2componentSpecialty, _bidStrategy);
	}
	
	@Override
	protected void updateBidStratagy() {
		QueryReport qr = _queryReports.peek();
		_topSlot.updateReport(qr);
		_noSlot.updateReport(qr);
		
		_topSlot.apply(_bidStrategy);
		_noSlot.apply(_bidStrategy);
		_reinvestmentCap.apply(_bidStrategy);
	}
	
	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	
	private Set<Query> intersect(Set<Query> s1, Set<Query> s2){
		Set<Query> inter = new LinkedHashSet<Query>();
		inter.addAll(s1);
		inter.retainAll(s2);
		return inter;
	}

	
}
