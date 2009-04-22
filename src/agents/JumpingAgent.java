package agents;

import java.util.Hashtable;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/*

INCOMPLETE

*/

public class JumpingAgent extends JESOMAgent {

	protected static final double FIRST_ADJUSTMENT = 0.65;
	//protected static final double ADJUSTMENT = 0.05;
	
	public static final double CONVERSION_F0 = .1;
	public static final double CONVERSION_F1 = .2;
	public static final double CONVERSION_F2 = .3;
	public static final double SALE_VALUE = 10;
	
	protected Hashtable<Query, Double> _maxBids;
	
	//protected static final double LOWER_CUTOFF = 0.30;
	//protected static final double UPPER_CUTOFF = 0.70;
	
	protected Hashtable<Query,Double> _previousBids;
	
	@Override
	protected void initBidder(){
		super.initBidder();
		_maxBids = new Hashtable<Query, Double>();
		for(Query q : _querySpace){
			_maxBids.put(q, _bidStrategy.getQueryBid(q));
			_bidStrategy.setQueryBid(q, _bidStrategy.getQueryBid(q) * FIRST_ADJUSTMENT);
		}
		
		_previousBids = new Hashtable<Query, Double>();
		for(Query query:_querySpace) {
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_previousBids.put(query, CONVERSION_F0*SALE_VALUE*1.1);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				_previousBids.put(query, CONVERSION_F1*SALE_VALUE*1.1);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				_previousBids.put(query, CONVERSION_F2*SALE_VALUE*1.1);
		}
	}
	
/*	@Override
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
			
			double oldBid = _bidStrategy.getQueryBid(q);
		}
*/
/*
	@Override
	protected void updateBidStrategy() {

		QueryReport lastReport = _queryReports.poll();
		SalesReport sales = _salesReports.poll();
		
		int conversionsThisRound = 0;
		
		for(Query query: _querySpace) {
			
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

	}
*/
}
