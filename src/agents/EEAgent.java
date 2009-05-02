package agents;


import java.util.*;

import se.sics.tasim.props.SimulationStatus;

import agents.rules.Constants;
import edu.umich.eecs.tac.props.*;

public class EEAgent extends AbstractAgent {
	
	Random _R = new Random();

	protected ArrayList<HashMap<Query,Double>> _allweights;
	protected ArrayList<HashMap<Query, Double>> _allconversions;
	protected ArrayList<HashMap<Query, Double>> _percenterror;
	protected HashMap<Query,Double> _bids;
	protected HashMap<Query,Double> _oldprices;
	protected HashMap<Query,Double> _goalpositions;
	
	protected ArrayList<Double> _quotas;
	
	protected int[] _totalConversions;
	
	protected int _capacity;
	protected int _window;
	
	protected double QUOTA = .2;
	private double CSB;
	private double MSB;
	protected int _currentday;

	private String manufacturerSpecialty;
	private Set<Query> ourspecialty;

	protected final static double INITIAL_CHEAPNESS = 1.0; //currently obsolete because of the entire cheapness factor
	protected final static double CHEAPNESS = .5;
	protected final static double BUDGETCHEAPNESS = .8;
	protected final static double LEARNING_RATE = .05;
	
	protected final static double INC_RATE = 1.1;
	protected final static double DEC_RATE = .9;
	
	protected final static double lambda = .995;
	
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
		// 3. Capacity is taken into consideration
		
		//sum up conversions from last 5 days
		int conv = 0;
		for(int i = 0; i < 5; i++) {
			conv += _totalConversions[i];
		}
		
		int capdiff = conv - _capacity;
		
		_quotas.add(QUOTA);
		
		debug("\n\n\n CAPDIFF: " + capdiff);
		
		//This is the amount that our capacity will be reduced due too being over capacity
		double capratio = Math.pow(lambda,Math.max(capdiff, 0));
		
		// For each query, figure out how much to devote to that specific query
		for(Query query:_querySpace) {
			//get the most recent weights
			//Determine how many conversions we are looking for in this query
			int quota = (int) Math.ceil((1.0/((double)_querySpace.size()))* _capacity * QUOTA );
		
			// How many clicks does it take to get to that point?
			int clicks = 0;
			
			if(ourspecialty.contains(query)) {
				if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
					clicks = (int) (quota/eta(Constants.CONVERSION_F0*capratio,1+CSB));
				else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
					clicks = (int) (quota/eta(Constants.CONVERSION_F1*capratio,1+CSB));
				else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
					clicks = (int) (quota/eta(Constants.CONVERSION_F2*capratio,1+CSB));
				else
					System.exit(0); // Death condition, just to be sure
			}
			else {
				if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
					clicks = (int) (quota/(Constants.CONVERSION_F0*capratio));
				else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
					clicks = (int) (quota/(Constants.CONVERSION_F1*capratio));
				else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
					clicks = (int) (quota/(Constants.CONVERSION_F2*capratio));
				else
					System.exit(0); // Death condition, just to be sure
			}
			
			// How much does it cost to get to that point?
			//We multiply by cheapness to reflect the fact that we are actually going to multiply our bid by that
			//we add a cheapness into our budget to reflect the fact that we are actually going to pay less per click than our bid
			double budget = 5.0;
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
		
		CSB = _advertiserInfo.getComponentBonus();
		MSB = _advertiserInfo.getManufacturerBonus();
		manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		ourspecialty = _queryManufacturer.get(manufacturerSpecialty);
		_capacity = _advertiserInfo.getDistributionCapacity();
		_window = _advertiserInfo.getDistributionWindow();
		
		_allweights = new ArrayList<HashMap<Query, Double>>();
		_allconversions = new ArrayList<HashMap<Query, Double>>();
		_percenterror = new ArrayList<HashMap<Query, Double>>();
		_bids = new HashMap<Query, Double>();
		_oldprices = new HashMap<Query, Double>();
		_goalpositions = new HashMap<Query, Double>();
		_totalConversions = new int[5];
		
		
		_quotas = new ArrayList<Double>();
				
		_totalConversions[0] = 0;
		_totalConversions[1] = 0;
		_totalConversions[2] = 0;
		_totalConversions[3] = 0;
		_totalConversions[4] = 0;
		  
		// Initialize the bids to slightly below the honest value
		// Using the "cheapness" value
		// From observations, we've seen that people tend to sell out and drop out
		// even on the first day. We will use the cheapness value until we get 
		// more information from reports (when we can use different heuristics)

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


		if(!(queryReport == null || salesReport == null)) {
			System.out.println("Day: " + _currentday);
			System.out.println("Advertiser ID: "+_advertiserInfo.getAdvertiserId());
			System.out.println("Squashing Parameter: " + _publisherInfo.getSquashingParameter());
			for(Query query : _querySpace) {
				System.out.println("\tQuery: " + query);
				for(int i = 1; i <= 8; i++) {
					System.out.println("\t\t Adv"+i+" Position: " + queryReport.getPosition(query, "adv"+i));		
				}
			}
			if(_currentday > 1) {
				HashMap<Query,Double> relatives = new HashMap<Query, Double>();
				HashMap<Query,Double> c = new HashMap<Query,Double>();
				HashMap<Query,Double> percenterror = new HashMap<Query,Double>();
				int conversions = 0;

				for(Query query:_querySpace) {
					double conv = salesReport.getConversions(query);
					c.put(query, conv);
					conversions += conv;
				}

				_allconversions.add(c);

				_totalConversions[0] = _totalConversions[1];
				_totalConversions[1] = _totalConversions[2];
				_totalConversions[2] = _totalConversions[3];
				_totalConversions[3] = _totalConversions[4];
				_totalConversions[4] = conversions;

				// Update bids based on how close we were to getting the weights right 2 days ago
				for(Query query:_querySpace) {
					
					//RANDOMLY CHOOSE BIDS TO EXPLORE SPACE
					
					double minbid = .40;
					double maxbid;
					if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
						maxbid = Constants.CONVERSION_F0*Constants.SALE_VALUE;
					else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
						maxbid = Constants.CONVERSION_F1*Constants.SALE_VALUE;
					else
						maxbid = Constants.CONVERSION_F2*Constants.SALE_VALUE;
					
					maxbid *= 1.5;  
					
					_bids.put(query, randDouble(minbid, maxbid));

					debug("New bid: " + query + " = " + _bids.get(query));
				}
				
				_percenterror.add(percenterror);
				for(Query query: _querySpace) {
					debug("Percent error for " + query.toString() + ":  " + percenterror.get(query));
				}
				
			}
		}
		else {
			System.out.println("\n\n\n\n\n QUERY REPORT NULL!!!!! \n\n\n");
			throw new RuntimeException("Query or Sales Report Null");
		}
	}

	/*
	 * Given a vector of Query-Weights
	 * Or rather, weights in the portfolio,
	 * this function will normalize the sum of the
	 * weights.
	 */
	protected void normalizeWeights(HashMap<Query, Double> weights) {
		double total = 0;
		for(Query query:weights.keySet())
			total += weights.get(query);
		
		for(Query query:weights.keySet()) {
			double value = weights.get(query)/total;
			weights.put(query, value);
		}
	}
	
	protected static void debug(Object o) {
		if(Constants.DEBUG)
			System.out.println(o.toString());
	}
	
	//Returns a random double rand such that a <= r < b
	protected double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}
	
	protected double eta(double p, double x) {
		return (p*x) / (p*x + (1-p));
	}
	
	protected void handleSimulationStatus(SimulationStatus simulationStatus) {
		_currentday++;
		if(_firstDay){
			_firstDay = false;
			_currentday = 0;
			initBidder();
		}
		sendBidAndAds();
	}
	
}
