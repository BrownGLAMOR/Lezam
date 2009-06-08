package simulator;

/*
 * TODO
 * NEED TO CHECK PERFECT MODELS!!!
 * 
 * 
 * really few things sell to fifth slot.....
 */

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

import agents.Cheap;
import agents.MCKPAgentMkII;
import agents.SimAbstractAgent;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.usermodel.AbstractUserModel;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Message;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectClickProb;
import simulator.models.PerfectConversionProb;
import simulator.models.PerfectPositionToBid;
import simulator.models.PerfectSlotToNumClicks;
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

	private boolean DEBUG = false;

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
	//TODO!!
	private HashMap<String,Integer> _overCap;
	private HashMap<Query,Double> _contProb;
	private HashMap<UserState,Double> _users;
	private Set<Query> _querySpace;

	private HashMap<Query,Double> _ourAdvEffect;

	private RetailCatalog _retailCatalog;

	private HashMap<Product, HashMap<UserState, Integer>> _usersMap;

	private int _ourAdvIdx;

	private int _day;

	private GameStatus _status;

	private PublisherInfo _pubInfo;

	private ReserveInfo _reserveInfo;

	private SlotInfo _slotInfo;

	private UserClickModel _userClickModel;

	private AdvertiserInfo _ourAdvInfo;

	private SimulationStatus _ourSimulationStatus;

	
	public BasicSimulator() {
	}

	public void initializeGameState(GameStatus gameStatus, int day, int advertiseridx) throws IOException, ParseException {
//		assert day >= 2 && advertiseridx >= 0 && advertiseridx <= 7;
		_status = gameStatus;
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
		LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists = _status.getUserDistributions();
		HashMap<String, AdvertiserInfo> advInfos = _status.getAdvertiserInfos();
		HashMap<String, LinkedList<BankStatus>> bankStatuses = _status.getBankStatuses();
		HashMap<String, LinkedList<BidBundle>> bidBundles = _status.getBidBundles();
		HashMap<String, LinkedList<QueryReport>> queryReports = _status.getQueryReports();
		HashMap<String, LinkedList<SalesReport>> salesReports = _status.getSalesReports();
		HashMap<String, LinkedList<SimulationStatus>> simulationStatuses = _status.getSimulationStatuses();
		_pubInfo = _status.getPubInfo();
		_reserveInfo = _status.getReserveInfo();
		_retailCatalog = _status.getRetailCatalog();
		_slotInfo = _status.getSlotInfo();
		_userClickModel = _status.getUserClickModel();
    	_agents = _status.getAdvertisers();
    	_compSpecialty = _compSpecialties.get(_agents[_ourAdvIdx]);
    	_manSpecialty = _manSpecialties.get(_agents[_ourAdvIdx]);
		
		_ourAdvInfo = advInfos.get(_agents[_ourAdvIdx]);
		_ourSimulationStatus = simulationStatuses.get(_agents[_ourAdvIdx]).get(day);
		
    	_squashing = _pubInfo.getSquashingParameter();
    	_numUsers = 90000;
    	_numPromSlots = _slotInfo.getPromotedSlots();
    	_numSlots = _slotInfo.getRegularSlots();
    	_regReserve = _reserveInfo.getRegularReserve();
    	_proReserve = _reserveInfo.getPromotedReserve();
    	_targEffect = _ourAdvInfo.getTargetEffect();
    	_promSlotBonus = _slotInfo.getPromotedSlotBonus();
    	_CSB = _ourAdvInfo.getComponentBonus();
    	_MSB = _ourAdvInfo.getManufacturerBonus();
    	
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
        	_contProb.put(query, _userClickModel.getContinuationProbability(_userClickModel.queryIndex(query)));
        }
    	
    	for(int i = 0; i < _agents.length; i++) {
    		AdvertiserInfo adInfo = advInfos.get(_agents[i]);
    		_overCap.put(_agents[i], adInfo.getDistributionCapacity()/adInfo.getDistributionWindow());
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
    			advEffect.put(query, _userClickModel.getAdvertiserEffect(_userClickModel.queryIndex(query), i));
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
			AbstractBidToSlotModel bidToSlotModel = new PerfectBidToPosition(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),_ourAdvIdx,query);
			AbstractBidToCPC bidToCPCModel = new PerfectBidToCPC(query,bidToSlotModel);
			AbstractSlotToPrClick slotToClickModel = new PerfectClickProb(_agents,_bidsWithoutOurs,_advEffect,_contProb,_adType,_compSpecialties,_overCap,_ourAdvEffect.get(query),_squashing,_numPromSlots,_numSlots,_proReserve,_targEffect,_promSlotBonus,_ourAdvIdx,query);
			AbstractPrConversionModel convPrModel = new PerfectConversionProb(_CSB, _compSpecialty,query,userModel);
			AbstractSlotToBidModel slotToBidModel = new PerfectPositionToBid(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),_ourAdvIdx,query);
			AbstractSlotToNumImp slotToNumImp = new PerfectSlotToNumImp(_usersMap,_numSlots,_retailCatalog,query);
			AbstractSlotToNumClicks slotToNumClicks = new PerfectSlotToNumClicks(slotToClickModel,slotToNumImp,query);
			models.add(bidToSlotModel);
			models.add(bidToCPCModel);
			models.add(slotToClickModel);
			models.add(convPrModel);
			models.add(slotToBidModel);
			models.add(slotToNumImp);
			models.add(slotToNumClicks);
		}
		return models;
	}

	/*
	 * Initializes a bidding agent with the proper models
	 */
	public SimAbstractAgent intializeBidder(String agentToRun) {
		SimAbstractAgent agent = stringToAgent(agentToRun);
		agent.setDay(_day);
		agent.sendSimMessage(new Message("doesn't","matter",_pubInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_retailCatalog));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_ourAdvInfo));
		agent.initBidder();
		return agent;
	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public BidBundle getBids(String agentToRun) {
		Set<AbstractModel> models = generatePerfectModels();
		SimAbstractAgent agent = intializeBidder(agentToRun);
		BidBundle bundle = agent.getBidBundle(models);
		for(Query query : _querySpace) {
			debug(query+" :");
			debug("\t" + bundle.getBid(query));
			debug("\t" + bundle.getDailyLimit(query));
		}
		return bundle;
	}
	
	public ArrayList<SimAgent> buildAgents(String agentToRun) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = getBids(agentToRun);
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
	public ArrayList<SimAgent> runSimulation(String agentToRun) {
		ArrayList<SimAgent> agents = buildAgents(agentToRun);
		ArrayList<SimUser> users = buildSearchingUserBase(_usersMap);
		Collections.shuffle(users);
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.generateQuery();
			if(query == null) {
				//This means the user is IS or T
				continue;
			}
			ArrayList<AgentBidPair> pairList = new ArrayList<AgentBidPair>();
			for(int j = 0; j < agents.size(); j++) {
				SimAgent agent = agents.get(j);
				double budget = agent.getBudget(query);
				double cost = agent.getCost(query);
				if(budget > cost || Double.isNaN(budget)) {
					double totBudget = agent.getTotBudget();
					double totCost = agent.getTotCost();
					if(totBudget > totCost || Double.isNaN(totBudget)) {
						double squashedBid = agent.getSquashedBid(query);
						if(squashedBid >= _regReserve) {
							AgentBidPair pair = new AgentBidPair(agents.get(j),squashedBid);
							pairList.add(pair);
						}
					}
				}
			}
			/*
			 * This sorts the list by squashed bid
			 */
			Collections.sort(pairList);
			debug(query);
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j);
				double squashedBid = pair.getSquashedBid();
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					pair.getAgent().addImpressions(query, 0, 1, j);
				}
				else {
					pair.getAgent().addImpressions(query, 1, 0, j);
				}

				debug(pair.getAgent().getAdvId() + ": " + pair.getAgent().getSquashedBid(query));
			}
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j-1);
				SimAgent agent = pair.getAgent();
				double squashedBid = pair.getSquashedBid();
				double contProb = _contProb.get(query);
				Ad ad = agent.getAd(query);
				double advEffect = agent.getAdvEffect(query);
				double fTarg = 1;
				if(ad != null && !ad.isGeneric()) {
					if(ad.getProduct() == new Product(query.getManufacturer(),query.getComponent())) {
						fTarg = 1 + _targEffect;
					}
					else {
						fTarg = 1/(1+_targEffect);
					}
				}

				double fProm = 1;
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					fProm = 1 + _promSlotBonus;
				}
				double clickPr = eta(advEffect,fTarg*fProm);
				double rand = _R.nextDouble();
				if(clickPr >= rand) {
					AgentBidPair underPair = pairList.get(j);
					SimAgent agentUnder = underPair.getAgent();
					double bidUnder = agentUnder.getBid(query);
					double advEffUnder = agentUnder.getAdvEffect(query);
					double squashedBidUnder = Math.pow(advEffUnder, _squashing) * bidUnder;
					if(j <= _numPromSlots && squashedBid >= _proReserve && squashedBidUnder <= _proReserve) {
						squashedBidUnder = _proReserve;
					}
					double cpc = squashedBidUnder / Math.pow(advEffect, _squashing);
					agent.addCost(query, cpc);

					double baselineConv;

					if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
						baselineConv = .1;
					}
					else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
						baselineConv = .2;
					}
					else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
						baselineConv = .3;
					}
					else {
						throw new RuntimeException("Malformed Query");
					}

					double overCap = agent.getOverCap();
					double convPr = Math.pow(_LAMBDA, Math.max(0.0, overCap))*baselineConv;

					String queryComp = query.getComponent();
					String compSpecialty = agent.getCompSpecialty();

					if(queryComp == compSpecialty) {
						convPr = eta(convPr,1+_CSB);
					}

					rand = _R.nextDouble();

					if(convPr >= rand) {
						String queryMan = query.getManufacturer();
						String manSpecialty = agent.getManSpecialty();
						double revenue = 10;
						if(manSpecialty.equals(queryMan)) {
							revenue = (1+_MSB)*10;
						}
						agent.addRevenue(query, revenue);
						break;
					}
					else {
						rand = _R.nextDouble();
						if(contProb >= rand) {
							continue;
						}
						else {
							break;
						}
					}
				}
				else {
					if(contProb >= rand) {
						continue;
					}
					else {
						break;
					}
				}
			}
		}
		for(int i = 0; i < agents.size(); i++) {
			SimAgent agent = agents.get(i);
			if(i == _ourAdvIdx) {
				debug("****US****");
			}
			debug("Adv Id: " + agent.getAdvId());
			debug("\tTotal Cost: " + agent.getTotCost());
			debug("\tTotal Budget: " + agent.getTotBudget());
			debug("\tTotal revenue: " + agent.getTotRevenue());
			debug("\tTotal Units Sold: " + agent.getTotUnitsSold());
			for(Query query : _querySpace) {
				debug("\t Query: " + query);
				debug("\t\t Bid: " + agent.getBid(query));
				debug("\t\t Cost: " + agent.getCost(query));
				debug("\t\t Budget: " + agent.getBudget(query));
				debug("\t\t Revenue: " + agent.getRevenue(query));
				debug("\t\t Units Sold: " + agent.getUnitsSold(query));
				debug("\t\t Num Clicks: " + agent.getNumClicks(query));
				debug("\t\t Prom Impressions: " + agent.getNumPromImps(query));
				debug("\t\t Reg Impressions: " + agent.getNumRegImps(query));
				debug("\t\t Avg Pos per Imp: " + (agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))));
			}
		}
		return agents;
	}
	
	public String[] getUsableAgents() {
		String[] agentStrings = { "MCKP", "Cheap" };
		return agentStrings;
	}
	
	public SimAbstractAgent stringToAgent(String string) {
		if(string.equals("MCKP")) {
			return new MCKPAgentMkII();
		}
		else if(string.equals("Cheap")) {
			return new Cheap();
		}
		else {
			return new MCKPAgentMkII();
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
	
	public void debug(Object str) {
		if(DEBUG) {
			System.out.println(str);
		}
	}
	
	public Set<Query> getQuerySpace() {
		return _querySpace;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		BasicSimulator sim = new BasicSimulator();
		String filename = "/Users/jordan/Desktop/Class/CS2955/Server/ver0.9.5/logs/sims/localhost_sim51.slg";
		int advId = 7;
		int day = 59;
		GameStatusHandler statusHandler = new GameStatusHandler(filename);
		GameStatus status = statusHandler.getGameStatus();
		double start = System.currentTimeMillis();
		int numSims = 1;
		for(int i = 0; i < numSims; i++) {
			sim.initializeGameState(status, day, advId);
			ArrayList<SimAgent> agents = sim.runSimulation("Cheap");
		}
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + ((elapsed / 1000)/numSims) + " seconds");
	}
	
}
