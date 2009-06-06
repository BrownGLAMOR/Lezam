package simulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import agents.MCKPAgentMkII;
import agents.SimAbstractAgent;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectClickProb;
import simulator.models.PerfectConversionProb;
import simulator.models.PerfectPositionToBid;
import simulator.models.PerfectSlotToNumImp;
import simulator.models.PerfectUserModel;
import simulator.parser.GameLogParser;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import simulator.parser.SimParserMessage;
import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;

/**
 *
 * @author jberg
 * 
 */
public class BasicSimulator {

	Random _R = new Random();					//Random number generator
	
	private double _LAMBDA = .995;

	private double _squashing;
	private int _numUsers;
	private int _numPromSlots;
	private int _numSlots;
	private double _regReserve;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	private double _CSB;
	private double _MSB;
	//TODO!!!
	private double _ourOverCap;
	private String _compSpecialty;
	private String _manSpecialty;

	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _bidsWithoutOurs;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<String,String> _manSpecialties;
	private HashMap<String,String> _compSpecialties;
	private HashMap<String,Integer> _overCap;
	private HashMap<Query,Double> _contProb;
	private HashMap<UserState,Double> _users;
	private Set<Query> _querySpace;
	private HashMap<Query,Double> _ourAdvEffect;

	private RetailCatalog _retailCatalog;

	private HashMap<Product, HashMap<UserState, Integer>> _usersMap;

	private int _ourAdvIdx;

	private int _day;

	
	public BasicSimulator() {
	}

	public void initializeGameState(String filename, int day, int advertiseridx) throws IOException, ParseException {
		assert day >= 2 && advertiseridx >= 0 && advertiseridx <= 7;
		_ourAdvIdx = advertiseridx;
		_day = day;
		_adType = new HashMap<String,HashMap<Query,Ad>>();
		_bids = new HashMap<String,HashMap<Query,Double>>();
		_bidsWithoutOurs = new HashMap<String,HashMap<Query,Double>>();
		_budgets = new HashMap<String,HashMap<Query,Double>>();
		_totBudget = new HashMap<String,Double>();
		_advEffect = new HashMap<String,HashMap<Query,Double>>();
		_manSpecialties = new HashMap<String,String>();
		_compSpecialties = new HashMap<String,String>();
		_overCap = new HashMap<String, Integer>();
		_contProb = new HashMap<Query,Double>();
		_users = new HashMap<UserState,Double>();
		_querySpace = new LinkedHashSet<Query>();
		GameStatusHandler statusHandler = new GameStatusHandler(filename);
		GameStatus status = statusHandler.getGameStatus();
		LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists = status.getUserDistributions();
		HashMap<String, AdvertiserInfo> advInfos = status.getAdvertiserInfos();
		HashMap<String, LinkedList<BankStatus>> bankStatuses = status.getBankStatuses();
		HashMap<String, LinkedList<BidBundle>> bidBundles = status.getBidBundles();
		HashMap<String, LinkedList<QueryReport>> queryReports = status.getQueryReports();
		HashMap<String, LinkedList<SalesReport>> salesReports = status.getSalesReports();
		PublisherInfo pubInfo = status.getPubInfo();
		ReserveInfo reserveInfo = status.getReserveInfo();
		_retailCatalog = status.getRetailCatalog();
		SlotInfo slotInfo = status.getSlotInfo();
		UserClickModel userClickModel = status.getUserClickModel();
    	_agents = status.getAdvertisers();
    	_compSpecialty = _compSpecialties.get(_agents[_ourAdvIdx]);
    	_manSpecialty = _manSpecialties.get(_agents[_ourAdvIdx]);
		
		AdvertiserInfo advInfo = advInfos.get(_agents[_ourAdvIdx]);
		
    	_squashing = pubInfo.getSquashingParameter();
    	_numUsers = 90000;
    	_numPromSlots = slotInfo.getPromotedSlots();
    	_numSlots = slotInfo.getRegularSlots();
    	_regReserve = reserveInfo.getRegularReserve();
    	_proReserve = reserveInfo.getPromotedReserve();
    	_targEffect = advInfo.getTargetEffect();
    	_promSlotBonus = slotInfo.getPromotedSlotBonus();
    	_CSB = advInfo.getComponentBonus();
    	_MSB = advInfo.getManufacturerBonus();
    	
    	_usersMap = userDists.get(_day);
    	for(UserState state : UserState.values()) { 
    		_users.put(state,0.0);
    	}
		for(Product p : _retailCatalog) {
			HashMap<UserState, Integer> users = _usersMap.get(p);
			for(UserState state : UserState.values()) {
				_users.put(state, _users.get(state) + users.get(state));
			}
		}
		double tot = 0.0;
		for(UserState userState : UserState.values()) {
			tot += _users.get(userState);
		}
		for(UserState userState : UserState.values()) {
			_users.put(userState,_users.get(userState)/tot);
		}    	
    	for(Product p : _retailCatalog) {
    		
    	}
    	
    	_querySpace.add(new Query(null, null));
        for(Product product : _retailCatalog) {
            // The F1 query classes
            // F1 Manufacturer only
            _querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            _querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
        }
        
        for(Query query : _querySpace) {
        	_contProb.put(query, userClickModel.getContinuationProbability(userClickModel.queryIndex(query)));
        }
    	
    	for(int i = 0; i < _agents.length; i++) {
    		AdvertiserInfo adInfo = advInfos.get(_agents[i]);
    		BidBundle bidBundleTemp = bidBundles.get(_agents[i]).get(_day);
    		String manfactBonus = adInfo.getManufacturerSpecialty();
    		_manSpecialties.put(_agents[i], manfactBonus);
    		String compBonus = adInfo.getComponentSpecialty();
    		_compSpecialties.put(_agents[i], compBonus);
    		_totBudget.put(_agents[i], bidBundleTemp.getCampaignDailySpendLimit());
    		HashMap<Query,Ad> adType = new HashMap<Query, Ad>();
    		HashMap<Query,Double> bids = new HashMap<Query, Double>();
    		HashMap<Query,Double> budgets = new HashMap<Query, Double>();
    		HashMap<Query,Double> bidsWithoutOurs = new HashMap<Query, Double>();
    		HashMap<Query,Double> advEffect = new HashMap<Query, Double>();

    		for(Query query : _querySpace) {
    			adType.put(query, bidBundleTemp.getAd(query));
    			bids.put(query, bidBundleTemp.getBid(query));
    			if(i != _ourAdvIdx) {
    				bidsWithoutOurs.put(query, bidBundleTemp.getBid(query));
    			}
    			budgets.put(query,bidBundleTemp.getDailyLimit(query));
    			advEffect.put(query, userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), i));
    		}
    		_adType.put(_agents[i], adType);
    		_bids.put(_agents[i], bids);
    		if(i != _ourAdvIdx) {
    			_bidsWithoutOurs.put(_agents[i], bids);
    		}
    		_budgets.put(_agents[i], budgets);
    		_advEffect.put(_agents[i], advEffect);
    	}
    	_ourAdvEffect = _advEffect.get(_agents[_ourAdvIdx]);
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public Set<AbstractModel> generatePerfectModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		AbstractUserModel userModel = new PerfectUserModel(_numUsers,_users);
		models.add(userModel);
		for(Query query : _querySpace) {
			AbstractBidToSlotModel bidToSlotModel = new PerfectBidToPosition(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),query);
			AbstractBidToCPC bidToCPCModel = new PerfectBidToCPC(query,bidToSlotModel);
			AbstractSlotToPrClick slotToClickModel = new PerfectClickProb(_agents,_bidsWithoutOurs,_advEffect,_contProb,_adType,_compSpecialties,_overCap,_ourAdvEffect.get(query),_squashing,_numPromSlots,_numSlots,_proReserve,_targEffect,_promSlotBonus,query);
			AbstractPrConversionModel convPrModel = new PerfectConversionProb(_CSB, _compSpecialty,query,userModel);
			AbstractSlotToBidModel slotToBidModel = new PerfectPositionToBid(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),query);
			AbstractSlotToNumImp slotToNumImp = new PerfectSlotToNumImp(_usersMap,_numSlots,_retailCatalog,query);
			models.add(bidToSlotModel);
			models.add(bidToCPCModel);
			models.add(slotToClickModel);
			models.add(convPrModel);
			models.add(slotToBidModel);
			models.add(slotToNumImp);
		}
		return models;
	}

	/*
	 * Initializes a bidding agent with the proper models
	 */
	public SimAbstractAgent intializeBidder() {
		SimAbstractAgent agent = new MCKPAgentMkII();
		return agent;
	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public BidBundle getBids() {
		Set<AbstractModel> models = generatePerfectModels();
		SimAbstractAgent agent = intializeBidder();
		BidBundle bundle = agent.getBidBundle(models);
		return bundle;
	}
	
	public ArrayList<SimAgent> buildAgents() {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = getBids();
				double totBudget = bundle.getCampaignDailySpendLimit();
				HashMap<Query,Double> bids = new HashMap<Query, Double>();
				HashMap<Query,Double> budgets = new HashMap<Query, Double>();
				HashMap<Query,Ad> adTypes = new HashMap<Query, Ad>();
				for(Query query : _querySpace) {
					bids.put(query, bundle.getBid(query));
					budgets.put(query,bundle.getDailyLimit(query));
					adTypes.put(query, bundle.getAd(query));
				}
				agent = new SimAgent(bids,budgets,totBudget,_advEffect.get(_agents[i]),adTypes,_overCap.get(_agents[i]),_manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			else {
				agent = new SimAgent(_bids.get(_agents[i]),_budgets.get(_agents[i]),_totBudget.get(_agents[i]),_advEffect.get(_agents[i]),_adType.get(_agents[i]),_overCap.get(_agents[i]),_manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			agents.add(agent);
		}
		return agents;
	}
	
	public ArrayList<SimUser> buildSearchingUserBase(HashMap<Product, HashMap<UserState, Integer>> usersMap) {
		ArrayList<SimUser> usersList = new ArrayList<SimUser>();
		for(Product prod : _retailCatalog) {
			for(UserState state : UserState.values()) {
				int users = usersMap.get(prod).get(state);
				for(int i = 0; i < users; i++) {
					SimUser user = new SimUser(prod,state);
					usersList.add(user);
				}
			}
		}
		return usersList;
	}

	/*
	 * Runs the simulation and generates reports
	 */
	public void runSimulation() {
		ArrayList<SimAgent> agents = buildAgents();
		ArrayList<SimUser> users = buildSearchingUserBase(_usersMap);
		Collections.shuffle(users);
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.generateQuery();
			ArrayList<AgentBidPair> pairList = new ArrayList<AgentBidPair>();
			for(int j = 0; j < agents.size(); j++) {
				SimAgent agent = agents.get(j);
				double budget = agent.getBudget(query);
				double cost = agent.getCost(query);
				if(budget > cost) {
					double totBudget = agent.getTotBudget(query);
					double totCost = agent.getTotCost();
					if(totBudget > totCost) {
						double squashedBid = agent.getSquashedBid(query);
						AgentBidPair pair = new AgentBidPair(agents.get(j),squashedBid);
						pairList.add(pair);
					}
				}
			}
			/*
			 * This sorts the list by squashed bid
			 */
			Collections.sort(pairList);
			for(int j = 1; j < _numSlots; j++) {
				
				//do something with this
				if(j <= _numPromSlots) {
					
				}
				else {
					
				}
				
			}
		}
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
