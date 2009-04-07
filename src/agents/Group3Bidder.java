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

public class Group3Bidder extends AbstractAgent {
	//protected SSBBidStrategy _bidStrategy;
	//protected DistributionCap _distributionCap;
	//protected ReinvestmentCap _reinvestmentCap;
	//protected TopPosition _topPosition;
	//protected NoImpressions _noImpressions;
	int _capacity;
	int _pp; //Product Price
	double _revenue;
	protected Hashtable<Query, Double> _CPS; //Cost Per Sale
	protected Hashtable<Query, Double> _CTR;
	protected Hashtable<Query, Double> _convProb;
	protected Hashtable<Query, Double> _bid;

	protected double _campaignSpendLimit;
	
	public Group3Bidder(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();
		for(Query q : _querySpace) {
			System.out.println(q.getComponent() + "XXXX");}
		for(Query q : _querySpace) {
			_CPS.put(q, 5.0);
			_bid.put(q, 1.0);
			_convProb.put(q, 0.2);
		}
		_capacity = _advertiserInfo.getDistributionCapacity();
/*		_bidStrategy = new SSBBidStrategy(_querySpace);
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		String manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		
		_distributionCap = new DistributionCap(distributionCapacity, distributionWindow);
		_reinvestmentCap = new ReinvestmentCap(0.80);
		_topPosition = new TopPosition(_advertiserInfo.getAdvertiserId(), 0.10);
		_noImpressions = new NoImpressions(_advertiserInfo.getAdvertiserId(), 0.10);
		
		new ConversionPr(0.10).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_ZERO), _bidStrategy);
		new ConversionPr(0.20).apply(_queryFocus.get(QueryType.FOCU S_LEVEL_ONE), _bidStrategy);
		new ConversionPr(0.30).apply(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), _bidStrategy);
		
		new ManufacurerBonus(manufacturerBonus).apply(_queryManufacturer.get(manufacturerSpecialty), _bidStrategy);
		
		Set<Query> componentSpecialty = _queryComponent.get(_advertiserInfo.getComponentSpecialty());
		
		Set<Query> F1componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_ONE), componentSpecialty);
		new ConversionPr(0.27).apply(F1componentSpecialty, _bidStrategy);
		
		Set<Query> F2componentSpecialty = intersect(_queryFocus.get(QueryType.FOCUS_LEVEL_TWO), componentSpecialty);
		new ConversionPr(0.39).apply(F2componentSpecialty, _bidStrategy);
		
		//??? new Targeted().apply(F1componentSpecialty, _bidStrategy);
		new Targeted().apply(F2componentSpecialty, _bidStrategy);*/
		
	}
	

	
	
	@Override
	protected void updateBidStratagy() {
/*		QueryReport qr = _queryReports.remove();
		_topPosition.updateReport(qr);
		_noImpressions.updateReport(qr);
		
		SalesReport sr = _salesReports.remove();
		_distributionCap.updateReport(sr);
		
		_topPosition.apply(_bidStrategy);
		_noImpressions.apply(_bidStrategy);
		_reinvestmentCap.apply(_bidStrategy);
		_distributionCap.apply(_bidStrategy);*/

		
	}
	
	@Override
	protected BidBundle buildBidBudle(){
		/*System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();*/
		BidBundle bidBundle = new BidBundle();
		QueryReport qr = _queryReports.remove();
		SalesReport sr = _salesReports.remove();
		if (qr.size() < 1) {
			return null;
		}
		int quantity = 0;
		//double oldRevenue = _revenue;
		//_revenue = 0;
		//for(Query q : _querySpace) {_revenue =+ sr.getRevenue(q);}
		//double delta = (_revenue - oldRevenue) / getAvaregeProductPrice();
		//for(Query q : _querySpace) {_quantity =+ (int)sr.getRevenue(q) / (int)_retailCatalog.getSalesProfit(1);} //not the right calc
		for(Query q : _querySpace) {
			_CTR.put(q, (qr.getClicks(q) / (double)qr.getImpressions(q)));
			quantity =+ sr.getConversions(q);
			_convProb.put(q, (double)sr.getConversions(q) / qr.getClicks(q));
			_CPS.put(q, (_bid.get(q) / _convProb.get(q)));
		}
		double distanceFrom = (_capacity - quantity) / _capacity;
		for(Query q : _querySpace) {
			double increment = getAvaregeProductPrice()/100;
			double newCPS = _CPS.get(q) + distanceFrom*increment;
			double queryBid = newCPS * _convProb.get(q);
			if (queryBid > 0.9*getAvaregeProductPrice()) queryBid = 0.9*getAvaregeProductPrice(); //??
			_bid.put (q, queryBid);
			System.out.println ("##(" + q.getComponent() + "," + q.getManufacturer() + ")## Bid on " + queryBid);
			bidBundle.addQuery(q, queryBid, bidBundle.getAd(q));
			//bidBundle.setDailyLimit(q, bidBundle.getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;
		
	}

	protected double getAvaregeProductPrice() {
		double price = 0;
		int n = 0;
		for (Product p : _retailCatalog) {
			price =+ _retailCatalog.getSalesProfit(p);
			n++;
		}
		return (price / n);
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(getQueryBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
			buff.append("\t").append("Conversion: ").append(getQueryConversion(q)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(getQueryReinvestFactor(q)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(getQueryConversionRevenue(q)).append("\n");
		}
		return buff.toString();
	}

}
