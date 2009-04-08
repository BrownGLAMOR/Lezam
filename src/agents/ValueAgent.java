package agents;

import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.isl.transport.Transportable;
import edu.umich.eecs.tac.props.*;

import java.util.*;


public class ValueAgent extends AbstractAgent {

    public static final boolean DEBUG = true;
    public static final double AVERAGE_FX_PERCENT = .75;
    public static final double CONVERSION_F0 = .1;
    public static final double CONVERSION_F1 = .2;
    public static final double CONVERSION_F2 = .3;
    public static final int SALE_VALUE = 10;
    public static final double DECAY_PARAMETER = .8;
    public static final double INCREASE_PARAMETER = 1.5;
    public static final double OVERLOAD_FACTOR = 1.5;

    private HashMap<Query, Double> _bids;
    private int _capacity;
    private int _estimatedCapacity; 
    private LinkedList<Integer> _capacityWindow;
    private LinkedList<Integer> _capacityEstimates;
    private LinkedList<Integer> _actualCapacities;
    
    
    private int _debugDay;
    private int _laggedDay;
    private class QueryKey implements Comparable{
        public Query query;
        public double value;

        public QueryKey(Query q, double v) {
            query = q;
            value = v;
        }

        public int compareTo(Object o) {
            QueryKey other = (QueryKey)o;
            return (int)(value - other.value);
        }
    }

    public void initBidder() {
        
        _bids = new HashMap<Query, Double>();

        for(Query query:_querySpace) {
            if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
                _bids.put(query, CONVERSION_F0*SALE_VALUE*AVERAGE_FX_PERCENT);
            else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
                _bids.put(query, CONVERSION_F1*SALE_VALUE*AVERAGE_FX_PERCENT);
            else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
                _bids.put(query, CONVERSION_F2*SALE_VALUE*AVERAGE_FX_PERCENT);
        }
        
        _capacity = (int) (_advertiserInfo.getDistributionCapacity()*OVERLOAD_FACTOR);
        _capacityWindow = new LinkedList<Integer>();
        _capacityEstimates = new LinkedList<Integer>();
        _actualCapacities = new LinkedList<Integer>();

        _debugDay = 0;
        _laggedDay = 0;
    }

    protected void updateBidStrategy() {

        _debug("======================================");
        _debug("           Starting Day: "+_debugDay);
        _debug("======================================");

        
        QueryReport lastReport = _queryReports.poll();
        SalesReport sales = _salesReports.poll();

        int conversions = 0;

        // ======================================
        // Bookkeeping for the lagged capacity
        // ======================================

        // Keeps track of the current capacity
        if( _capacityWindow.size() >= 5) {
        	int actualCapacity = 0;
        	for(Integer i:_capacityWindow) {
        		_debug(i.toString());
        		actualCapacity += i;
        	}
        	actualCapacity =  _advertiserInfo.getDistributionCapacity() - actualCapacity;
        	_actualCapacities.add(actualCapacity);
        	
        	_debug("Information for day :" +_laggedDay);
        	_debug("Actual capacity :" +_actualCapacities.get(_laggedDay));
        	_debug("Estimated capacity :" + _capacityEstimates.get(_laggedDay));
        	
        	_laggedDay++;
            _capacity += _capacityWindow.poll();
        }
        // Day 1-2 case
        // Some crude estimate of how our capacity decreases
        if( _capacityWindow.size() < 2) 
            _capacity *= .9; 
      
        // Decrease the lagged capacity 
        for(Query query:_querySpace) {        
            _capacity -= sales.getConversions(query);
            conversions += sales.getConversions(query);
        }

        _debug(new Integer(conversions).toString());
        // Update the sliding window
        _capacityWindow.add(conversions);

        // ======================================
        // Modify it to project into the "future"
        // ======================================
        
        // Use estimated capacity (lagged)
        int estimatedCapacity = _capacity;
       
        
        // If there are more than 2 things in the list
        // take the change in conversions and continue the trend
        // to estimate the number of conversions during the
        // past two periods

        if( _capacityWindow.size() > 2) {
            int start = _capacityWindow.get(_capacityWindow.size() - 3);
            int mid = _capacityWindow.get(_capacityWindow.size() - 2);
            int end = _capacityWindow.get(_capacityWindow.size() - 1);

            // Using these start, mid, and end points, project into the future
            int slope1 = mid - start;
            int slope2 = end - mid;
            int deltaSlope = slope2 - slope1;

            int forwardSlope1 = slope2 + deltaSlope;
            int forwardSlope2 = forwardSlope1 + deltaSlope;

            int forwardSales1 = forwardSlope1 + end;
            int forwardSales2 = forwardSlope2 + forwardSales1;

            estimatedCapacity -= forwardSales1;
            estimatedCapacity -= forwardSales2;
        }

        // And also to remove things too far back in the past
        if( _capacityWindow.size() >= 5) {
            estimatedCapacity -= _capacityWindow.get(0);
            estimatedCapacity -= _capacityWindow.get(1);
        }

        // At this point, we have an estimate of capacity, and we can start
        // to build our bid bundle

        // Keep track of the actual capacity
        _debugDay++;
        _capacityEstimates.add(estimatedCapacity);
        _estimatedCapacity = estimatedCapacity;
    }

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
			double bid = _bids.get(query);
			
			avgbid += bid;
			
			double spendingLimit = (.3*_estimatedCapacity/CONVERSION_F2)*bid;
			
			bidBundle.addQuery(query,  bid, ad);
			bidBundle.setDailyLimit(query, spendingLimit);
		}

		avgbid = avgbid/_querySpace.size();
		
		double campaignSpendLimit = .3*_estimatedCapacity*avgbid/CONVERSION_F2;;
		bidBundle.setCampaignDailySpendLimit(campaignSpendLimit);

		return bidBundle;
    }

    private void _debug(String s) {
        if(DEBUG) System.out.println(s);
    }
}
