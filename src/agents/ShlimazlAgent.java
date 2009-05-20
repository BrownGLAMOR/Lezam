package agents;


import java.util.*;

import modelers.ClickRatioModel;
import modelers.PositionGivenBid;
import modelers.UserSteadyStateDist;

import se.sics.tasim.props.SimpleContent;
import se.sics.tasim.props.SimulationStatus;

import agents.rules.Constants;
import edu.umich.eecs.tac.props.*;

public class ShlimazlAgent extends AbstractAgent {
	
	private static boolean DEBUG = true;		//Whether or not to print debug comments
	private double CHEAPNESS = .6;				//Discounter for bidding
	private double BUDGETCHEAPNESS = .7;		//Discounter for budget
	private double LAST = 6.5;					//What slot we say that out of the auction is
	private double MINBID = .5;				//Our minimum bid for all auctions
	private double CSB;							//Component Specialty Bonus
	private double MSB;							//Manufacturer Specialty Bonus
	private double PSB;							//Promoted Slot Bonus
	private double LAMBDA;						//Capacity Discount Factor
	private double capratio;					//Discounter for capacity based on capacity window
	
	private int numUsers = 4000;				//Number of users in the simulation
	private int _capacity;						//Our Capacity limit
	private int _window;						//The capacity window
	private int NumRS;							//Number of Regular Slots
	private int NumPS;							//Number of Promoted Slots
	private int _currentday;					//The current day
	private int[] _totalConversions;			//An array as long as the capacity window keeping track of our conversions

	private String manufacturerSpecialty;		//The manufacturer specialty
	private String componentSpecialty;			//The component specialty
	private String ouradvID;					//Our advertiser ID
	private Set<Query> ourspecialtyC;			//The set of our component specialty queries
	private Set<Query> ourspecialtyM;			//The set of our manufacturer specialty queries
	
	Random _R = new Random();					//Random number generator
	private UserSteadyStateDist userDistModel;	//Our User Distribution model
	
	private HashMap<Query,Double> _goalpos;		//A hashmap from queries to our goal positions
	private HashMap<Query,Double> MAXBID;		//A hashmap from queries to our maximum bid for that auction
	private double maxbidcoeff = 1.25;
	private HashMap<Query,Double> _bids;		//A hashmap from queries to our bid for that auction
	private HashMap<Query,Double> _budget;		//A hashmap from queries to our budget for that auction
	private HashMap<Query,Double> USP;			//A hashmap from queries to our sales price query
	
	private HashMap<Query,Boolean> pgbModelUpdated;			//A hashmap from queries to whether or not our position bid model updated
	
	private HashMap<Query,PositionGivenBid> pgbModels;		//A hashmap from queries to our Position-Bid Model
	private HashMap<Query, ClickRatioModel> crModels;		//A hashmap from queries to our Click ration Model

	private ArrayList<HashMap<Query,Double>> _allconversions;			//A list of hashmaps from queries to conversions
	private ArrayList<HashMap<Query,Double>> _allpositions;				//A list of hashmaps from queries to positions
	private ArrayList<HashMap<Query,Double>> _allpositionguesses;		//A list of hashmaps from queries to our guess for our next position
	private ArrayList<HashMap<Query,Double>> _allCPC;					//A list of hashmaps from queries to CPC
	private ArrayList<HashMap<Query,Double>> _allbids;					//A list of hashmaps from queries to our bid
	
	@Override
	protected void initBidder() {
		CSB = _advertiserInfo.getComponentBonus();
		MSB = _advertiserInfo.getManufacturerBonus();
		PSB = _slotInfo.getPromotedSlotBonus();
		NumPS = _slotInfo.getPromotedSlots();
		NumRS = _slotInfo.getRegularSlots();
		manufacturerSpecialty = _advertiserInfo.getManufacturerSpecialty();
		componentSpecialty = _advertiserInfo.getComponentSpecialty();
		ourspecialtyM = _queryManufacturer.get(manufacturerSpecialty);
		ourspecialtyC = _queryComponent.get(componentSpecialty);
		_capacity = _advertiserInfo.getDistributionCapacity();
		_window = _advertiserInfo.getDistributionWindow();
		LAMBDA = _advertiserInfo.getDistributionCapacityDiscounter();
		ouradvID = _advertiserInfo.getAdvertiserId();		
		
		_allconversions = new ArrayList<HashMap<Query, Double>>();
		_allpositions = new ArrayList<HashMap<Query, Double>>();
		_allpositionguesses = new ArrayList<HashMap<Query, Double>>();
		_allCPC = new ArrayList<HashMap<Query, Double>>();
		_allbids = new ArrayList<HashMap<Query, Double>>();
		_bids = new HashMap<Query, Double>();
		_budget = new HashMap<Query, Double>();
		USP = new HashMap<Query, Double>();
		_totalConversions = new int[5];
		_totalConversions[0] = 0;
		_totalConversions[1] = 0;
		_totalConversions[2] = 0;
		_totalConversions[3] = 0;
		_totalConversions[4] = 0;
		pgbModels = new HashMap<Query,PositionGivenBid>();
		pgbModelUpdated = new HashMap<Query, Boolean>();
		userDistModel = new UserSteadyStateDist();
		crModels = new HashMap<Query,ClickRatioModel>();
		_goalpos = new HashMap<Query, Double>();
		MAXBID = new HashMap<Query, Double>();
		int[] users = userDistModel.getBadEstimates(numUsers);
		
		for(Query query: _querySpace) {
			//Initialize base price per query
			if(ourspecialtyM.contains(query)) {
				USP.put(query, (1+MSB)*_retailCatalog.getSalesProfit(query.getType().ordinal()));
			}
			else {
				USP.put(query, _retailCatalog.getSalesProfit(query.getType().ordinal()));
			}
			//Initialize Bid-Pos and Click Ratio Models
			pgbModels.put(query, new PositionGivenBid(query));
			pgbModelUpdated.put(query, false);
			ClickRatioModel crm = new ClickRatioModel(query,NumRS);
			crModels.put(query, crm);
			//Initialize goal position array
			_goalpos.put(query, 5.0);
			//Initialize maximal bids
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				MAXBID.put(query, (USP.get(query)*Constants.CONVERSION_F0)*.8);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				MAXBID.put(query, (USP.get(query)*Constants.CONVERSION_F1)*.8);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				MAXBID.put(query, (USP.get(query)*Constants.CONVERSION_F2)*.8);

			//Initialize bids
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_bids.put(query, Constants.CONVERSION_F0*Constants.SALE_VALUE*CHEAPNESS);
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				_bids.put(query, Constants.CONVERSION_F1*Constants.SALE_VALUE*CHEAPNESS);
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				_bids.put(query, Constants.CONVERSION_F2*Constants.SALE_VALUE*CHEAPNESS);
			
			debug("Initial bid: " + query + " = " +_bids.get(query));
			
			//Determine how many clicks we get based on goal positions
			double goalpos = _goalpos.get(query);
			double clickprob = crm.getClickProb()[(int) (goalpos-1)];
			int u = users[query.getType().ordinal()];
			int clicks = (int) (u*clickprob);
			//Initialize budgets
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_budget.put(query,clicks*_bids.get(query));
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				_budget.put(query,clicks*_bids.get(query)*(1.0/6.0));
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				_budget.put(query,clicks*_bids.get(query)*(1.0/9.0));
		}
		_allbids.add(_bids);
		_allbids.add(_bids);
	}

	@Override
	protected void updateBidStrategy() {

		QueryReport queryReport = _queryReports.poll();
		SalesReport salesReport = _salesReports.poll();
		

		if(!(queryReport == null || salesReport == null)) {
			debug("Day: " + _currentday);
			for(Query query : _querySpace) {
				debug("\tQuery: " + query);
				String[] advertisers = (String[]) queryReport.advertisers(query).toArray(new String[queryReport.advertisers(query).size()]);
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
					pos.put(query, Math.ceil(queryReport.getPosition(query, ouradvID)));
					cpc.put(query,queryReport.getCPC(query));
					conversions += salesReport.getConversions(query);
				}

				_allconversions.add(conv);
				_allpositions.add(pos);
				_allCPC.add(cpc);
				_totalConversions[0] = _totalConversions[1];
				_totalConversions[1] = _totalConversions[2];
				_totalConversions[2] = _totalConversions[3];
				_totalConversions[3] = _totalConversions[4];
				_totalConversions[4] = conversions;
				
				
				//sum up conversions from last 5 days
				int conv1 = 0;
				for(int i = 0; i < 5; i++) {
					conv1 += _totalConversions[i];
				}
				
				int capdiff = conv1 - _capacity;
				
				//This is the amount that our capacity will be reduced due too being over capacity
				capratio = Math.pow(LAMBDA,Math.max(capdiff, 0));
				
				updateModels(pos,cpc);

				if(_currentday < 5) {
					for(Query query: _querySpace) {
						pgbModelUpdated.put(query, false);
					}
				}


				_goalpos = new HashMap<Query,Double>();
				for(Query query: _querySpace) {
					_goalpos.put(query, 5.0);
				}
				
				greedySlotChoice();
				
				setBids();
				
				_allbids.add(_bids);
				
				setBudgets();
				
				HashMap<Query, Double> posguess = new HashMap<Query, Double>();
				for(Query query:_querySpace) {
					if(pgbModelUpdated.get(query)) {
						PositionGivenBid pgbmodel = pgbModels.get(query);
						posguess.put(query, pgbmodel.getPosition(_bids.get(query)));
					}
					else {
						posguess.put(query, _goalpos.get(query));
					}
				}
				_allpositionguesses.add(posguess);
				
				debug("\n\n\n CAPDIFF: " + capdiff);				
			}
		}
		else {
			//This means the simulation is over!
			bidPosStats();
		}
	}

	private void bidPosStats() {
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
				if(Math.abs(rmse2to58.get(i)) <= 1) {
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
	}

	private void updateModels(HashMap<Query, Double> pos, HashMap<Query, Double> cpc) {
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
		
		for(Query query: _querySpace) {
			PositionGivenBid model = pgbModels.get(query);
			if(model.updateModel(_bids.size())) {
				pgbModelUpdated.put(query, true);
			}
			else {
				pgbModelUpdated.put(query, false);
			}
		}
		
	}
	
	private void setBudgets() {
		int[] users = userDistModel.getBadEstimates(numUsers);
		for(Query query:_querySpace) {
			double pos = _goalpos.get(query);
			ClickRatioModel crm = crModels.get(query);
			double clickprob = crm.getClickProb()[(int) (pos-1)];
			int u = users[query.getType().ordinal()];
			int clicks = (int) (u*clickprob);
			if(pgbModelUpdated.get(query)) {
				PositionGivenBid pgbmodel = pgbModels.get(query);
				double cpc;
				if(pos > 4) {
					cpc = pgbmodel.getBid(LAST-.5);
				}
				else {
					cpc = pgbmodel.getBid(pos + 1);  //Clicks * CPC
				}
				if(cpc < MINBID) {
					cpc = MINBID;
				}
				else if(cpc > MAXBID.get(query)) {
					cpc = MAXBID.get(query);
				}
				_budget.put(query,clicks*cpc);
			}
			else {
				//Our models aren't good so set our budget using our bid
				_budget.put(query,clicks*_bids.get(query));
			}
		}
	}

	private void setBids() {
		for(Query query:_querySpace) {
			double maxbid = MAXBID.get(query)*randDouble(1.0, maxbidcoeff);
			if(pgbModelUpdated.get(query) && _goalpos.get(query) <= 4) {
				PositionGivenBid pgbmodel = pgbModels.get(query);
				debug(query+"  bid:"+pgbmodel.getBid(_goalpos.get(query)));
				_bids.put(query, pgbmodel.getBid(_goalpos.get(query)));
			}
			else {
				//Our Model's aren't good so bid randomly
				if(_goalpos.get(query) > 4) {
					debug("EXPLORING INSTEAD OF SLOT 5");
				}
				_bids.put(query, randDouble(MINBID, maxbid));
			}
		}
		//Make sure bids are in a reasonable interval
		for(Query query:_querySpace) {
			double maxbid = MAXBID.get(query)*randDouble(1.0, maxbidcoeff);
			double bid = _bids.get(query);
			if(bid < MINBID) {
				_bids.put(query, MINBID);
			}
			else if(bid > MAXBID.get(query)) {
				_bids.put(query, maxbid);
			}
			else if(Double.isNaN(bid)) {
				_bids.put(query,randDouble(MINBID, maxbid));
			}
		}
		//Modify bid slightly to make it not the same everytime
		for(Query query: _querySpace) {
			if(_currentday < 4) {
				_bids.put(query,_bids.get(query)*randDouble(.80, .90));
			}
			else {
				_bids.put(query,_bids.get(query)*randDouble(.95, 1.05));
			}
			debug("New bid: " + query + " = " + _bids.get(query));	
		}
	}

	private void greedySlotChoice() {
		int[] users = userDistModel.getBadEstimates(numUsers);
		while(true) {
			double maxval = 0;
			Query bestquery = null;
			for(Query query: _querySpace) {
				double pos = _goalpos.get(query);
				if(pos < 2) {
					continue;
				}
				else {
					if(pgbModelUpdated.get(query)) {
						PositionGivenBid pgbmodel = pgbModels.get(query);
						double cpc = pgbmodel.getBid(pos);
						double cpconv;
						//We actually want cost per conversion
						if(ourspecialtyC.contains(query)) {
							if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
								cpconv = (cpc/eta(Constants.CONVERSION_F0*capratio,1+CSB));
							else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
								cpconv = (cpc/eta(Constants.CONVERSION_F1*capratio,1+CSB));
							else
								cpconv = (cpc/eta(Constants.CONVERSION_F2*capratio,1+CSB));
						}
						else {
							if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
								cpconv = (cpc/(Constants.CONVERSION_F0*capratio));
							else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
								cpconv = (cpc/(Constants.CONVERSION_F1*capratio));
							else
								cpconv = (cpc/(Constants.CONVERSION_F2*capratio));
						}
						double ppc = USP.get(query)-cpconv; //profit per conversion
						if(ppc > maxval) {
							maxval = ppc;
							bestquery = query;
						}
					}
					else {
						continue;
					}
				}
			}
			if(bestquery != null) {
				_goalpos.put(bestquery,_goalpos.get(bestquery)-1);
			}
			else{
				break;
			}
			//Test to see if we are getting enough conversions overall, if so break
			double totconv = 0;
			for(Query query: _querySpace) {
				double pos = _goalpos.get(query);
				ClickRatioModel crm = crModels.get(query);
				double clickprob = crm.getClickProb()[(int) (pos-1)];
				double u = users[query.getType().ordinal()];
				double clicks = u*clickprob;
				double conversions;
				if(ourspecialtyC.contains(query)) {
					if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
						conversions = (clicks*eta(Constants.CONVERSION_F0,1+CSB));
					else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
						conversions = (clicks*eta(Constants.CONVERSION_F1,1+CSB))*(1.0/6.0);
					else
						conversions = (clicks*eta(Constants.CONVERSION_F2,1+CSB))*(1.0/9.0);
				}
				else {
					if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
						conversions = (clicks*(Constants.CONVERSION_F0));
					else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
						conversions = (clicks*(Constants.CONVERSION_F1))*(1.0/6.0);
					else
						conversions = (clicks*(Constants.CONVERSION_F2))*(1.0/9.0);
				}
				debug(query+"   conversions: "+conversions);
				totconv += conversions;
			}
			debug("Total conversions: "+totconv);
			if(totconv >= _capacity * .2) {
				break;
			}
		}
		for(Query query: _querySpace) {
			debug(query+" Slot: "+_goalpos.get(query));
		}
	}

	@Override
	protected BidBundle buildBidBudle() {
		
		BidBundle bidBundle = new BidBundle();
		
		// For each query, figure out how much to devote to that specific query
		for(Query query:_querySpace) {
//			bidBundle.addQuery(query, _bids.get(query), new Ad(), _budget.get(query)*BUDGETCHEAPNESS);
				bidBundle.addQuery(query, _bids.get(query), new Ad());
		}
		
		// There is no whole limit, as we set limits on the individual parts
		return bidBundle;
	}
	
	private static void debug(Object o) {
		if(DEBUG) {
			System.out.println(o.toString());
		}
	}
	
	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}
	
	private double eta(double p, double x) {
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
