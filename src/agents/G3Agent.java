package agents;

import java.util.Hashtable;
import java.util.Set;

import agents.rules.ConversionPr;
import agents.rules.DistributionCap;
import agents.rules.ManufacurerBonus;
import agents.rules.NoImpressions;
import agents.rules.ReinvestmentCap;
import agents.rules.Targeted;
import agents.rules.TopPosition;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class G3Agent extends AbstractAgent {
	protected G3BidStrategy _bidStrategy;
	protected DistributionCap _distributionCap;
	protected ReinvestmentCap _reinvestmentCap;
	protected TopPosition _topPosition;
	protected NoImpressions _noImpressions;

	protected double _campaignSpendLimit;
	
	
	public G3Agent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();

		_bidStrategy = new G3BidStrategy(_querySpace);
		for(Query q : _querySpace) {
			_bidStrategy.setData(q, -1.0, 0.2);
		}
		
/*		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		String manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		
		_distributionCap = new DistributionCap(distributionCapacity, distributionWindow);
		_reinvestmentCap = new ReinvestmentCap(0.80);
		_topPosition = new TopPosition(_advertiserInfo.getAdvertiserId(), 0.10);
		_noImpressions = new NoImpressions(_advertiserInfo.getAdvertiserId(), 0.10);
		
		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy);
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), _bidStrategy);
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy);
		
		new ManufacurerBonus(manufacturerBonus).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy);
		
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		
		Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy);
		
		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy);
		
		//??? new Targeted().apply(F1componentSpecialty, _bidStrategy);
		new Targeted().apply(F2componentSpecialty, _bidStrategy);*/
		
		_bidStrategy.setDistributionCapacity(_advertiserInfo.getDistributionCapacity());
		
	}
	
	
	@Override
	protected void updateBidStrategy() {
		QueryReport qr = _queryReports.remove();
		SalesReport sr = _salesReports.remove();
		if (qr == null || sr == null) {
			return;
		}
		int quantity = 0;
		for(Query q : _querySpace) {
			if (qr.getPosition(q) == 0) return;
			else System.out.println("i'm in!");
			double CTR = qr.getClicks(q) / (double)qr.getImpressions(q);
			double convProb = (double)sr.getConversions(q) / qr.getClicks(q);
			//double CPS = _bidStrategy.getQueryBid(q) / convProb;
			//System.out.println ("ubs_CPS: " + CPS);
			System.out.println ("ubs_CTR: " + CTR);
			System.out.println ("ubs_CcP: " + convProb);
			System.out.println ("ubs_getClicks: " + qr.getClicks(q));
			System.out.println ("ubs_getImpressions: " + qr.getImpressions(q));
			System.out.println ("ubs_getConversions(q): " + sr.getConversions(q));
			
			_bidStrategy.setData(q, CTR, convProb);
			quantity =+ sr.getConversions(q);
		}
		_bidStrategy.setConvertions (quantity);
		_bidStrategy.setPP(getAvaregeProductPrice());
		
	}
	

	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}



}
