package oldagentsSSB.agents;

import java.util.HashMap;
import java.util.LinkedList;


import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;


public class ValueAgent extends CarletonAbstractAgent {

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
    private LinkedList<Integer> _conversionHistory;
    private LinkedList<Integer> _capacityEstimates;
    private LinkedList<Integer> _conversionGuessHistory;
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
        _conversionHistory = new LinkedList<Integer>();
        _capacityEstimates = new LinkedList<Integer>();
        _actualCapacities = new LinkedList<Integer>();
        _conversionGuessHistory = new LinkedList<Integer>();
        _debugDay = 0;
        _laggedDay = 0;
    }

    protected void updateBidStrategy() {

        _debug("======================================");
        _debug("           Starting Day: "+_debugDay);
        _debug("======================================");

        
        QueryReport lastReport = _queryReports.poll();
        SalesReport sales = _salesReports.poll();


        // ======================================
        // Bookkeeping for the lagged capacity
        // ======================================

        // Keeps track of the current capacity
        if( _conversionHistory.size() > 2) {
        	
            
        	
            _debug("Information for day :" +_laggedDay);
            
            int actualCapacity = 0;
            for(Integer i:_conversionHistory) { 
            	actualCapacity += i;
            	_debug(new Integer(i).toString());
            }
            
            _debug(_conversionGuessHistory.poll());
            _debug(_conversionGuessHistory.poll());
            _debug(_conversionGuessHistory.poll());
            _debug(_conversionGuessHistory.poll());
            _debug(_conversionGuessHistory.poll());
            
            actualCapacity =  _advertiserInfo.getDistributionCapacity() - actualCapacity;
            _actualCapacities.add(actualCapacity);
        
            
            _debug("Actual capacity :" +_actualCapacities.get(_laggedDay));
            _debug("Estimated capacity :" + _capacityEstimates.get(_laggedDay));
        	
            _laggedDay++;
            
        }

        
        // Get the number of conversions in this day
        int conversions = 0;
        for(Query query:_querySpace) {        
            conversions += sales.getConversions(query);
        }

        // Update the sliding window
        if(_conversionHistory.size() > 4)
        	_conversionHistory.poll();
        _conversionHistory.add(conversions);
                

        // Day 1-2 case
        // Some crude estimate of how our capacity decreases
        if( _conversionHistory.size() < 5) 
            _capacity *= .9; 

        // ======================================
        // Modify it to project into the "future"
        // ======================================
        
        // Use estimated capacity (lagged)
        int estimatedCapacity = _capacity;
      
        if( _conversionHistory.size() >= 5) {
        	
        	_debug("HERE");
        	
            estimatedCapacity = _conversionHistory.get(_conversionHistory.size() - 3);
            estimatedCapacity+= _conversionHistory.get(_conversionHistory.size() - 2);
            estimatedCapacity+= _conversionHistory.get(_conversionHistory.size() - 1);

            
            
            estimatedCapacity = _advertiserInfo.getDistributionCapacity() - estimatedCapacity;
            _debug(estimatedCapacity);
        }

        // If there are more than 2 things in the list
        // take the change in conversions and continue the trend
        // to estimate the number of conversions during the
        // past two periods

        if( _conversionHistory.size() > 2) {
            int start = _conversionHistory.get(_conversionHistory.size() - 3);
            int mid = _conversionHistory.get(_conversionHistory.size() - 2);
            int end = _conversionHistory.get(_conversionHistory.size() - 1);
            
            _debug(new Integer(start).toString());
            _debug(new Integer(mid).toString());
            _debug(new Integer(end).toString());
            // Using these start, mid, and end points, project into the future
            int slope1 = mid - start;
            int slope2 = end - mid;
            int deltaSlope = slope2 - slope1/4;

            int forwardSlope1 = slope2 + deltaSlope;
            forwardSlope1/=2;
            int forwardSlope2 = forwardSlope1 + deltaSlope;
            forwardSlope2/=4;
            
            int forwardSales1 = forwardSlope1 + end;
            forwardSales1 = Math.max(forwardSales1,0);
            int forwardSales2 = forwardSlope2 + forwardSales1;
            forwardSales2 = Math.max(forwardSales2	,0);
            _debug(new Integer(forwardSales1).toString());
            _debug(new Integer(forwardSales2).toString());
            estimatedCapacity -= forwardSales1;
            estimatedCapacity -= forwardSales2;

            _debug(estimatedCapacity);
            _debug("==");
            _conversionGuessHistory.add(start);
            _conversionGuessHistory.add(mid);
            _conversionGuessHistory.add(end);
            _conversionGuessHistory.add(forwardSales1);
            _conversionGuessHistory.add(forwardSales2);
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

    private void _debug(Object o) {
        if(DEBUG) System.out.println(o.toString());
    }
}

