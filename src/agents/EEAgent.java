package agents;


import java.util.*;

import modelers.PositionGivenBid;

import se.sics.tasim.props.SimulationStatus;

import agents.rules.Constants;
import edu.umich.eecs.tac.props.*;

public class EEAgent extends AbstractAgent {
	
	private static boolean DEBUG = false;

	Random _R = new Random();

	protected ArrayList<HashMap<Query, Double>> _allconversions;
	protected ArrayList<HashMap<Query,Double>> _allpositions;
	protected ArrayList<HashMap<Query,Double>> _allpositionguesses;
	protected ArrayList<HashMap<Query,Double>> _allCPC;
	protected ArrayList<HashMap<Query,Double>> _allbids;
	protected HashMap<Query,Double> _bids;
	protected HashMap<Query,Double> USP;
	protected HashMap<Query,PositionGivenBid> pgbModels;
		
	protected int[] _totalConversions;
	
	protected int _capacity;
	protected int _window;
	
	private double CSB;			//Component Specialty Bonus
	private double MSB;			//Manufacturer Specialty Bonus
	private double PSB;			//Promoted Slot Bonus
	private int NumRS;			//Number of Regular Slots
	private int NumPS;			//Number of Promoted Slots
	protected double LAMBDA;	//Capacity Discount Factor
	protected int _currentday;

	private String manufacturerSpecialty;
	private Set<Query> ourspecialty;

	private String ADVERTISER;

	private double LAST = 6.5;

	protected final static double INITIAL_CHEAPNESS = 1.0; //currently obsolete because of the entire cheapness factor
	protected final static double CHEAPNESS = .5;
	protected final static double BUDGETCHEAPNESS = .8;
	protected final static double LEARNING_RATE = .05;
	
	protected final static double INC_RATE = 1.1;
	protected final static double DEC_RATE = .9;
	
	

	@Override
	protected void initBidder() {

		debug("===================================");
		debug("Initializing bidder");
		debug("===================================");
		
		CSB = _advertiserInfo.getComponentBonus();
		MSB = _advertiserInfo.getManufacturerBonus();
		PSB = _slotInfo.getPromotedSlotBonus();
		NumPS = _slotInfo.getPromotedSlots();
		NumRS = _slotInfo.getRegularSlots();
		manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		ourspecialty = _queryManufacturer.get(manufacturerSpecialty);
		_capacity = _advertiserInfo.getDistributionCapacity();
		_window = _advertiserInfo.getDistributionWindow();
		LAMBDA = _advertiserInfo.getDistributionCapacityDiscounter();
		ADVERTISER = _advertiserInfo.getAdvertiserId();		
		
		_allconversions = new ArrayList<HashMap<Query, Double>>();
		_allpositions = new ArrayList<HashMap<Query, Double>>();
		_allpositionguesses = new ArrayList<HashMap<Query, Double>>();
		_allpositions = new ArrayList<HashMap<Query, Double>>();
		_allCPC = new ArrayList<HashMap<Query, Double>>();
		_allbids = new ArrayList<HashMap<Query, Double>>();
		_bids = new HashMap<Query, Double>();
		USP = new HashMap<Query, Double>();
		_totalConversions = new int[5];
		pgbModels = new HashMap<Query,PositionGivenBid>();
		
		for(Query query: _querySpace) {
			USP.put(query, _retailCatalog.getSalesProfit(new Product(query.getManufacturer(), query.getComponent())));
			pgbModels.put(query, new PositionGivenBid(query));
		}
		
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
		
		_allbids.add(_bids);
		_allbids.add(_bids);
	}

	@Override
	protected void updateBidStrategy() {

		debug("===================================");
		debug("Updating bidding strategy");
		debug("===================================");

		QueryReport queryReport = _queryReports.poll();
		SalesReport salesReport = _salesReports.poll();


		if(!(queryReport == null || salesReport == null)) {
			debug("Day: " + _currentday);
			debug("Advertiser ID: "+_advertiserInfo.getAdvertiserId());
			debug("Squashing Parameter: " + _publisherInfo.getSquashingParameter());
			for(Query query : _querySpace) {
				debug("\tQuery: " + query);
				String[] advertisers = (String[]) queryReport.advertisers(query).toArray(new String[queryReport.advertisers(query).size()]);
				debug("\t\t "+_advertiserInfo.getAdvertiserId()+" Position: " + queryReport.getPosition(query, _advertiserInfo.getAdvertiserId()));
				for(int i = 0; i < advertisers.length; i++) {
					debug("\t\t "+advertisers[i]+" Position: " + queryReport.getPosition(query, advertisers[i]));		
				}
			}
			if(_currentday > 1) {
				HashMap<Query,Double> conv = new HashMap<Query,Double>();
				HashMap<Query,Double> pos = new HashMap<Query,Double>();
				HashMap<Query,Double> cpc = new HashMap<Query,Double>();
				int conversions = 0;

				for(Query query:_querySpace) {
					conv.put(query, (double)salesReport.getConversions(query));
					pos.put(query, Math.ceil(queryReport.getPosition(query, ADVERTISER)));
					cpc.put(query,queryReport.getCPC(query));
					conversions += salesReport.getConversions(query);
				}

				_allconversions.add(conv);
				_allpositions.add(pos);
				_allCPC.add(cpc);
				
				//UPDATE MODELS!!!
				for(Query query: _querySpace) {
					PositionGivenBid model = pgbModels.get(query);
					HashMap<Query,Double> bids = _allbids.get(_allconversions.size());
					//Make sure that we actually got a position
					if(!(pos.get(query).isNaN() || cpc.get(query).isNaN())) {
						//If we were in position 1 we only get one data point
						if(pos.get(query) < 2) {
							model.addDataPoint(_allpositions.size(), cpc.get(query), 1.0);
						}
						//If we were in position 5 we only get one data point
						else if(pos.get(query) > 4.5) {
							model.addDataPoint(_allpositions.size(), cpc.get(query), 5.0);
						}
						//Otherwise we get two data points
						else {
							model.addDataPoint(_allpositions.size(), bids.get(query), Math.ceil(pos.get(query)-.99));
							model.addDataPoint(_allpositions.size(), cpc.get(query), Math.ceil(pos.get(query)));
						}
					}
					//If we didn't get a position we may or may not put a point in
					else {
						//We are putting points that didn't make it into the data as position 6.5 for now
						//This is vaguely arbitrary :)
						model.addDataPoint(_allpositions.size(), bids.get(query), LAST);
					}
				}

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
						maxbid = Constants.CONVERSION_F0*USP.get(query);
					else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
						maxbid = Constants.CONVERSION_F1*USP.get(query);
					else
						maxbid = Constants.CONVERSION_F2*USP.get(query);
					
					maxbid *= 1.25;  
					
					_bids.put(query, randDouble(minbid, maxbid));

					debug("New bid: " + query + " = " + _bids.get(query));
				}
				
				_allbids.add(_bids);

				
				//TEST MODELS!!
				HashMap<Query, Double> posg = new HashMap<Query, Double>();
				for(Query query:_querySpace) {
					PositionGivenBid model = pgbModels.get(query);
					if(model.updateModel(_bids.size())) {
						double posguess = model.getPosition(_bids.get(query));
						posg.put(query, posguess);
						debug("Query: "+query+"  PosGues: "+posguess);
					}
					else {
						
					}
				}
				_allpositionguesses.add(posg);
				
				debug("PosGuessArraySize: " + _allpositionguesses.size());
				debug("PosArraySize: " + _allpositions.size());
				debug("ConvArraySize: " + _allconversions.size());
				debug("BidArraySize: " + _allbids.size());
				
			}
		}
		else {
			debug("\n\n\n\n\n QUERY REPORT NULL!!!!! \n\n\n");
			HashMap<Query,ArrayList<Double>> rmse2to20map = new HashMap<Query,ArrayList<Double>>();
			HashMap<Query,ArrayList<Double>> rmse21to40map = new HashMap<Query,ArrayList<Double>>();
			HashMap<Query,ArrayList<Double>> rmse41to58map = new HashMap<Query,ArrayList<Double>>();
			HashMap<Query,ArrayList<Double>> rmse2to58map = new HashMap<Query,ArrayList<Double>>();
			for(Query query: _querySpace){
				rmse2to20map.put(query, new ArrayList<Double>());
				rmse21to40map.put(query, new ArrayList<Double>());
				rmse41to58map.put(query, new ArrayList<Double>());
				rmse2to58map.put(query, new ArrayList<Double>());
			}
			for(int i = 2; i < _allpositionguesses.size(); i++) {
				HashMap<Query, Double> posmap = _allpositions.get(i);
				HashMap<Query, Double> posguessmap = _allpositionguesses.get(i);
				for(Query query: _querySpace) {
					if(posguessmap.get(query) != null) {
						ArrayList<Double> rmse2to20 = rmse2to20map.get(query);
						ArrayList<Double> rmse21to40 = rmse21to40map.get(query);
						ArrayList<Double> rmse41to58 = rmse41to58map.get(query);
						ArrayList<Double> rmse2to58 = rmse2to58map.get(query);
						Double pos = posmap.get(query);
						double posguess = posguessmap.get(query);
						if(pos.isNaN()) {
							pos = LAST;
						}
						if(posguess < 1.0) {
							posguess = 1.0;
						}
						if(posguess > 5) {
							posguess = LAST;
						}
						double diff = pos - posguess;
						if(i <= 20) {
							rmse2to20.add(diff);
							rmse2to58.add(diff);
						}
						else if(i > 20 && i <= 40) {
							rmse21to40.add(diff);
							rmse2to58.add(diff);
						}
						else if(i > 40) {
							rmse41to58.add(diff);
							rmse2to58.add(diff);
						}
						debug(diff);
					}
				}
			}
			double totalerr = 0;
			double totgood = 0;
			for(Query query:_querySpace) {
				debug("Query: "+query);
				ArrayList<Double> rmse2to20 = rmse2to20map.get(query);
				ArrayList<Double> rmse21to40 = rmse21to40map.get(query);
				ArrayList<Double> rmse41to58 = rmse41to58map.get(query);
				ArrayList<Double> rmse2to58 = rmse2to58map.get(query);
				double tot = 0;
				for(int i = 0; i < rmse2to20.size(); i++) {
					tot += rmse2to20.get(i)*rmse2to20.get(i);
				}
				tot = tot/rmse2to20.size();
				tot = Math.sqrt(tot);
				debug("\tError 2-20: "+tot);

				tot = 0;
				for(int i = 0; i < rmse21to40.size(); i++) {
					tot += rmse21to40.get(i)*rmse21to40.get(i);
				}
				tot = tot/rmse21to40.size();
				tot = Math.sqrt(tot);
				debug("\tError 21-40: "+tot);
				
				tot = 0;
				for(int i = 0; i < rmse41to58.size(); i++) {
					tot += rmse41to58.get(i)*rmse41to58.get(i);
				}
				tot = tot/rmse41to58.size();
				tot = Math.sqrt(tot);
				debug("\tError 41-58: "+tot);
				
				tot = 0;
				for(int i = 0; i < rmse2to58.size(); i++) {
					if(Math.abs(rmse2to58.get(i)*rmse2to58.get(i)) < 1) {
						totgood += 1;
					}
					tot += rmse2to58.get(i)*rmse2to58.get(i);
				}
				totalerr += tot;
				tot = tot/rmse2to58.size();
				tot = Math.sqrt(tot);
				debug("\tError 2-58: "+tot);
			}
			totalerr = totalerr/(rmse2to58map.get(_querySpace.iterator().next()).size()*_querySpace.size());
			totgood = totgood/(rmse2to58map.get(_querySpace.iterator().next()).size()*_querySpace.size());
			totalerr = Math.sqrt(totalerr);
			System.out.println("THE NUMBER: "+totalerr);
			System.out.println("GOOD GUESS(%): "+totgood);
			throw new RuntimeException("Query or Sales Report Null");
		}
	}
	
	
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
		
		
		debug("\n\n\n CAPDIFF: " + capdiff);
		
		//This is the amount that our capacity will be reduced due too being over capacity
		double capratio = Math.pow(LAMBDA,Math.max(capdiff, 0));
		
		// For each query, figure out how much to devote to that specific query
		for(Query query:_querySpace) {
			//get the most recent weights
			//Determine how many conversions we are looking for in this query
			int quota = (int) Math.ceil((1.0/((double)_querySpace.size()))* _capacity * .2);
		
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
			double budget = clicks*_bids.get(query);
			bidBundle.addQuery(query, _bids.get(query), new Ad(), budget);	
		}
		
		// There is no whole limit, as we set limits on the individual parts
		return bidBundle;
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
		if(DEBUG) {
			System.out.println(o.toString());
		}
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
		long startTime = System.nanoTime();
		long endTime;
		_currentday++;
		if(_firstDay){
			_firstDay = false;
			_currentday = 0;
			initBidder();
		}
		sendBidAndAds();
		endTime = System.nanoTime();
		long duration = endTime - startTime;
		debug("\n\n\n TIME FOR SIMULATION "+_currentday+":   "+ 10E-9 *duration+" seconds \n\n\n");
	}
	
}
