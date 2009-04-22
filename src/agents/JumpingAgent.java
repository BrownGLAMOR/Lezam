package agents;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/*

INCOMPLETE

*/

public class JumpingAgent extends JESOMAgent {

	protected static final double FIRST_ADJUSTMENT = 0.65;
	protected static final double ADJUSTMENT = 0.05;
	
	protected Random _random;
	
	public static final boolean DEBUG = false;
	protected static final double SLOT_FRACTION = 0.30;
	protected static final double UNDERCUT_AMOUNT = 0.05; // could change to be a random interval
	
	public static final double CONVERSION_F0 = .1;
	public static final double CONVERSION_F1 = .2;
	public static final double CONVERSION_F2 = .3;
	public static final double SALE_VALUE = 10;
	
	public static final double DECREASE_FACTOR = .95;
	public static final double INCREASE_FACTOR = 1.5;
	protected Hashtable<Query, Double> _previousBids;
	protected Hashtable<Query, Double> _highBids;
	protected int _estimatedCapacity;
	protected Queue<Integer> _capacityWindow;
	
	
	@Override
	protected BidBundle buildBidBudle() {
		
		BidBundle bidBundle = new BidBundle();

		double avgbid = 0;
		for(Query query : _querySpace) {
				
			Product product;
			
			double rand = Math.random();
			if(rand < .4)
				product = new Product(_advertiserInfo.getManufacturerSpecialty(),_advertiserInfo.getComponentSpecialty());
			else if(rand < .6)
				product = new Product(_advertiserInfo.getManufacturerSpecialty(),(String)_retailCatalog.getComponents().toArray()[0]);
			else if(rand < .8)
				product = new Product(_advertiserInfo.getManufacturerSpecialty(),(String)_retailCatalog.getComponents().toArray()[1]);
			else
				product = new Product(_advertiserInfo.getManufacturerSpecialty(),(String)_retailCatalog.getComponents().toArray()[2]);
			
			Ad ad = new Ad(product);
			double bid = _previousBids.get(query);
			
			avgbid += bid;
			
			double spendingLimit = (.3*_estimatedCapacity/CONVERSION_F2)*bid;
			
			bidBundle.addQuery(query,  bid, ad);
			bidBundle.setDailyLimit(query, spendingLimit);
		}

		avgbid = avgbid/_querySpace.size();
		
		double campaignSpendLimit = .3*_estimatedCapacity*avgbid/CONVERSION_F2;;
		bidBundle.setCampaignDailySpendLimit(campaignSpendLimit);

		// Send the bid bundle to the publisher
		if (_advertiserInfo.getPublisherId() != null) {
			sendMessage(_advertiserInfo.getPublisherId(), bidBundle);
		}
		
		return null;
	}

	@Override
	protected void initBidder() {
		
		printAdvertiserInfo();

		_previousBids = new Hashtable<Query, Double>();
		_highBids = new Hashtable<Query, Double>();
		for(Query query:_querySpace) {
			_previousBids.put(query, _bidStrategy.getQueryBid(query) * FIRST_ADJUSTMENT);
			_highBids.put(query, _bidStrategy.getQueryBid(query) * Math.min(FIRST_ADJUSTMENT+.1,1.0));
			_bidStrategy.setQueryBid(query, _bidStrategy.getQueryBid(query) * FIRST_ADJUSTMENT);
		}
		
		_estimatedCapacity = _advertiserInfo.getDistributionCapacity();
		_capacityWindow = new LinkedList<Integer>();
	}

	@Override
	protected void updateBidStrategy() {

		QueryReport lastReport = _queryReports.poll();
		SalesReport sales = _salesReports.poll();
		
		dbg("=================================");
		dbg("         New Bids                ");
		dbg("=================================");
		
		int conversionsThisRound = 0;
		
		if(_capacityWindow.size() >= 4) 
			_estimatedCapacity += _capacityWindow.poll();
		
		for(Query query:_querySpace) {
			
			Double lastValue = _previousBids.get(query); 
		
			if( lastReport.getPosition(query,_advertiserInfo.getAdvertiserId()) > 1.01 )
				lastValue*=INCREASE_FACTOR;
			else
				lastValue*=DECREASE_FACTOR;
			
			_previousBids.put(query, lastValue);
			
			dbg("Position: " + lastReport.getPosition(query,_advertiserInfo.getAdvertiserId()) + query + " : " + lastValue);
			
			_estimatedCapacity -= sales.getConversions(query);
			conversionsThisRound += sales.getConversions(query);
		}
		
		_capacityWindow.add(conversionsThisRound);
		
		
		dbg("================================updateBidStrategy=");
		dbg("         Costs                   ");
		dbg("=================================");
		
		double totalRevenue = 0;
		double totalCosts = 0;
		for(Query query:_querySpace) {
			totalRevenue += sales.getRevenue(query);
			totalCosts += lastReport.getCost(query);
			
			dbg(query.toString());
			dbg("Revenue: " + sales.getRevenue(query));
			dbg("Cost: " + lastReport.getCost(query));
                        if(lastReport.getCost(query) != 0)
			dbg("Revenue / COST " + (sales.getRevenue(query) / lastReport.getCost(query)) );
		}
		
		dbg(" Total Revenue: " + totalRevenue);
		dbg(" Total Costs: " + totalCosts);
		dbg(" Capacity: " + _estimatedCapacity);
	}

	/*
	@Override
	protected void updateBidStrategy() {
		for(Query q : _querySpace){
			double prob = _random.nextDouble();
			double oldBid = _bidStrategy.getQueryBid(q);
			if (prob <= LOWER_CUTOFF){
				_bidStrategy.setQueryBid(q, oldBid * (1.0 - ADJUSTMENT));
			}
			else if (prob <= UPPER_CUTOFF){
				// Do nothing!
			}
			else { // Move up
				double newBid = _bidStrategy.getQueryBid(q) * (1.0 + ADJUSTMENT);
				double maxBid = _maxBids.get(q); 
				if(newBid >= maxBid){
					newBid = maxBid;
				}
				_bidStrategy.setQueryBid(q, newBid);
			}
		}
	}
	*/
	
	public static void dbg(String str) {
		if(DEBUG)
			System.out.println(str);
	}

}
