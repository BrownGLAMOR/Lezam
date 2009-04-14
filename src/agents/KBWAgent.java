package agents;


import java.util.*;

import agents.rules.Constants;
import edu.umich.eecs.tac.props.*;

public class KBWAgent extends AbstractAgent {

	protected HashMap<Query,Double> _weights;
	protected HashMap<Query,Double> _bids;
	
	protected int _capacity;
	protected int _window;
	
	protected final static double QUOTA = .27;
	protected final static double INITIAL_CHEAPNESS = .6;
	protected final static double LEARNING_RATE = .05;
	
	protected final static double INC_RATE = 1.3;
	protected final static double DEC_RATE = .9;
	
	@Override
	protected BidBundle buildBidBudle() {
		
		BidBundle bidBundle = new BidBundle();
		
		// To start out, here are the assumptions for the current 
		// version of the bidder
		// 1. We will use a fixed amount of our capacity every day 
		// with the intent of overselling slightly
		// 2. We will bid for the third slot and try to maintain that position
		// this is from our observations in class about how people drop out.
		// This simplifies the problem for the time being.
		// 3. Capacity does not affect conversion rates... yet!
		
		
		// For each query, figure out how much to devote to that specific query
		for(Query query:_querySpace) {
			int quota = (int) Math.ceil( _weights.get(query) * _capacity * QUOTA );
		
			// How many clicks does it take to get to that point?
			int clicks = 0;
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				clicks = (int) (quota/Constants.CONVERSION_F0);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				clicks = (int) (quota/Constants.CONVERSION_F1);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				clicks = (int) (quota/Constants.CONVERSION_F2);
			else
				System.exit(0); // Death condition, just to be sure
				
			// How much does it cost to get to that point?
			double budget = clicks*_bids.get(query);
		
			// Set the limit
			bidBundle.addQuery(query, _bids.get(query), new Ad(), budget);	
		}
		
		// There is no whole limit, as we set limits on the individual parts
		return bidBundle;
	}

	@Override
	protected void initBidder() {

		debug("===================================");
		debug("Initializing bidder");
		debug("===================================");
		
		_weights = new HashMap<Query, Double>();
		_bids = new HashMap<Query, Double>();
		
		// Initialize all of the queries to one
		// Then normalize
		String manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nManufacturer Specialty: " + manufacturerSpecialty);
		Set<Query> ourspecialty = _queryManufacturer.get(manufacturerSpecialty);
		double manufacturerBonus = _advertiserInfo.getManufacturerBonus();
		
		for(Query query: _querySpace) {
			if (ourspecialty.contains(query)) {
				System.out.println(query);
				_weights.put(query, 1.0 + manufacturerBonus);
			}
			else {
				_weights.put(query, 1.0);
			}
		}
		_normalizeWeights();

		_capacity = _advertiserInfo.getDistributionCapacity();
		_window = _advertiserInfo.getDistributionWindow();
		  
		// Initialize the bids to slightly below the honest value
		// Using the "cheapness" value
		// From observations, we've seen that people tend to sell out and drop out
		// even on the first day. We will use the cheapness value until we get 
		// more information from reports (when we can use different heuristics)
		
		double conversionProbability;
		for(Query query: _querySpace) {
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_bids.put(query, Constants.CONVERSION_F0*Constants.SALE_VALUE*INITIAL_CHEAPNESS);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				_bids.put(query, Constants.CONVERSION_F1*Constants.SALE_VALUE*INITIAL_CHEAPNESS);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				_bids.put(query, Constants.CONVERSION_F2*Constants.SALE_VALUE*INITIAL_CHEAPNESS);
			
			debug("Initial bid: " + query + " = " +_bids.get(query));
		}
		
		
	}

	@Override
	protected void updateBidStrategy() {
		
		debug("===================================");
		debug("Updating bidding strategy");
		debug("===================================");
		
		QueryReport queryReport = _queryReports.poll();
		SalesReport salesReport = _salesReports.poll();

		HashMap<Query,Double> relatives = new HashMap<Query, Double>();
		for(Query query:_querySpace) {
			
			// Zero case, not in the query at all
			// We don't want to not explore this at all, so for the time being
			// we will set it to 1.
			if( Math.abs( queryReport.getCost(query) ) <= .001)
				relatives.put(query, 1.0);
			// Otherwise we put the actual relative in there
			else
				relatives.put(query, salesReport.getRevenue(query) / queryReport.getCost(query));
		}
		
		// Debug messages
		for(Query query:_querySpace)
			debug("Original weight: " + query + " = " + _weights.get(query));
		
		// Update weights using the EG algorithm
		// First get the dot product
		double dotProduct = 0;
		for(Query query:_querySpace) 
			dotProduct += _weights.get(query)*relatives.get(query);
		
		// Now get the denominator
		double totalWeights = 0;
		for(Query query:_querySpace)
			totalWeights += _weights.get(query)*Math.exp((LEARNING_RATE)*relatives.get(query)/dotProduct);
		
		// Now update the weights
		for(Query query:_querySpace)
			_weights.put(query, (_weights.get(query)*Math.exp((LEARNING_RATE)*relatives.get(query)/dotProduct))/totalWeights);
		
		// Debug messages
		for(Query query:_querySpace)
			debug("New weight: " + query + " = " + _weights.get(query));
		
		
		// Update the bids to get the third place;
		// Just try to maintain that position
		for(Query query:_querySpace) {
			
			debug("Original bid: " + query + " = " + _bids.get(query));
			
			if(queryReport.getPosition(query) > 4.1)
				_bids.put(query, _bids.get(query) * INC_RATE);
			else 
				_bids.put(query, _bids.get(query) * DEC_RATE);
			
			debug("New bid: " + query + " = " + _bids.get(query));
			
		}
		
		
	}

	/*
	 * Given a vector of Query-Weights
	 * Or rather, weights in the portfolio,
	 * this function will normalize the sum of the
	 * weights.
	 */
	protected void _normalizeWeights() {
		double total = 0;
		for(Query query:_weights.keySet())
			total += _weights.get(query);
		
		for(Query query:_weights.keySet()) {
			double value = _weights.get(query)/total;
			_weights.put(query, value);
		}
	}
	
	protected static void debug(Object o) {
		if(Constants.DEBUG)
			System.out.println(o.toString());
	}
}
