package agents;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class MeanAgent extends AbstractAgent {

	public static final boolean DEBUG = true;
	public static final double CONVERSION_F0 = .1;
	public static final double CONVERSION_F1 = .2;
	public static final double CONVERSION_F2 = .3;
	public static final double SALE_VALUE = 10;
	
	public static final double DECREASE_FACTOR = .95;
	public static final double INCREASE_FACTOR = 1.5;
	protected Hashtable<Query,Double> _previousBids;
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
		for(Query query:_querySpace) {
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_previousBids.put(query, CONVERSION_F0*SALE_VALUE*1.1);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				_previousBids.put(query, CONVERSION_F1*SALE_VALUE*1.1);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				_previousBids.put(query, CONVERSION_F2*SALE_VALUE*1.1);
		}
		
		_estimatedCapacity = _advertiserInfo.getDistributionCapacity();
		_capacityWindow = new LinkedList<Integer>();
	}

	
	
	@Override
	protected void updateBidStratagy() {

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
		
		
		dbg("=================================");
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

	public static void dbg(String str) {
		if(DEBUG)
			System.out.println(str);
	}
}

