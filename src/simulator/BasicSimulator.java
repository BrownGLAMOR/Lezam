package simulator;

/*
 * TODO
 * NEED TO CHECK PERFECT MODELS!!!
 * 
 * CHANGE USER MODELS TO BE ACCESSED BY PRODUCT AND USERSTATE AND RETURN AN INT!
 * 
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
import agents.EqPftAgent;
import agents.ILPAgent;
import agents.MCKPAgentMkII;
import agents.SimAbstractAgent;
import agents.NewSSB;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.slottobid.AbstractSlotToBidModel;
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
import simulator.models.PerfectQueryToNumImp;
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
	private int _numUsers = 90000;
	private int _numPromSlots;
	private int _numSlots;
	private double _regReserve;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	private double _CSB;
	private double _MSB;
	private String _ourCompSpecialty;
	private String _ourManSpecialty;

	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _bidsWithoutOurs;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<String,String> _manSpecialties;
	private HashMap<String,String> _compSpecialties;
	/*
	 * Index 0: most recent day of Sales
	 * 
	 * Index CapacityWindow-1: Sales from the last day of the window...
	 */
	private HashMap<String,Integer[]> _salesOverWindow;
	private HashMap<String,Integer> _capacities;
	private HashMap<Query,Double> _contProb;
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

	private HashMap<String, AdvertiserInfo> _advInfos;

	private String _testId = "testId";

	private HashMap<Query, HashMap<Double, Reports>> singleQueryReports;

	public HashMap<String,LinkedList<Reports>> runFullSimulation(GameStatus status, String agentIn, int advertiseridx) {
		HashMap<String,LinkedList<Reports>> reportsListMap = new HashMap<String, LinkedList<Reports>>();
		SimAbstractAgent agent = stringToAgent(agentIn);
		initializeBasicInfo(status, advertiseridx);
		agent.sendSimMessage(new Message("doesn't","matter",_pubInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_retailCatalog));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_ourAdvInfo));
		agent.initBidder();
		agent.initModels();
		for(int i = 0; i < _agents.length; i++) {
			LinkedList<Reports> reports = new LinkedList<Reports>();
			reportsListMap.put(_agents[i], reports);
		}
		int firstDay = 0;

		for(int day = firstDay; day < 60; day++) {

			if(day >= 2) {
				LinkedList<Reports> reportsList = reportsListMap.get(_agents[advertiseridx]);
				/*
				 * Two day delay
				 */
				//Why is there one less query report?
				Reports reports = reportsList.get(day-2);
				SalesReport salesReport = reports.getSalesReport();
				QueryReport queryReport = reports.getQueryReport();
				agent.handleQueryReport(queryReport);
				agent.handleSalesReport(salesReport);
			}
			initializeDaySpecificInfo(day, advertiseridx);
			agent.setDay(day);
			/*
			 * Make the maps used in the perfect models
			 */
			singleQueryReports = new HashMap<Query, HashMap<Double,Reports>>();
			for(Query query : _querySpace) {
				singleQueryReports.put(query,new HashMap<Double, Reports>());
			}
			HashMap<String, Reports> maps = runSimulation(agent);

			/*
			 * Keep track of capacities
			 */
			for(int i = 0; i < _agents.length; i++) {
				Integer[] sales = _salesOverWindow.get(_agents[i]);
				for(int j = sales.length-1; j >=1; j--) {
					sales[j] = sales[j-1];
				}
				int totConversions = 0;
				for(Query query : _querySpace) {
					totConversions += maps.get(_agents[i]).getSalesReport().getConversions(query);
				}
				sales[0] = totConversions;
				_salesOverWindow.put(_agents[i], sales);
			}
			/*
			 * Keep track of all the reports
			 */
			for(int i = 0; i < _agents.length; i++) {
				LinkedList<Reports> reports = reportsListMap.get(_agents[i]);
				reports.add(maps.get(_agents[i]));
				reportsListMap.put(_agents[i],reports);
			}
		}
		return reportsListMap;
	}

	/**
	 * This initializes all of the one time info that is received at the beginning of the game.
	 * Basic things like the retail catalogs, to more game specific (but still one time things) such
	 * as advertiser effects
	 * @param status
	 * @param advertiseridx
	 */
	public void initializeBasicInfo(GameStatus status, int advertiseridx) {
		_status = status;
		_ourAdvIdx = advertiseridx;

		_advEffect = new HashMap<String,HashMap<Query,Double>>();
		_manSpecialties = new HashMap<String,String>();
		_compSpecialties = new HashMap<String,String>();
		_contProb = new HashMap<Query,Double>();
		_salesOverWindow = new HashMap<String, Integer[]>();
		_capacities = new HashMap<String, Integer>();

		_advInfos = _status.getAdvertiserInfos();
		_agents = _status.getAdvertisers();
		_ourAdvInfo = _advInfos.get(_agents[_ourAdvIdx]);
		_targEffect = _ourAdvInfo.getTargetEffect();
		_CSB = _ourAdvInfo.getComponentBonus();
		_MSB = _ourAdvInfo.getManufacturerBonus();

		_pubInfo = _status.getPubInfo();
		_reserveInfo = _status.getReserveInfo();
		_retailCatalog = _status.getRetailCatalog();
		_slotInfo = _status.getSlotInfo();
		_userClickModel = _status.getUserClickModel();

		_squashing = _pubInfo.getSquashingParameter();
		_numPromSlots = _slotInfo.getPromotedSlots();
		_numSlots = _slotInfo.getRegularSlots();
		_promSlotBonus = _slotInfo.getPromotedSlotBonus();
		_regReserve = _reserveInfo.getRegularReserve();
		_proReserve = _reserveInfo.getPromotedReserve();


		_querySpace = new LinkedHashSet<Query>();
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
			AdvertiserInfo adInfo = _advInfos.get(_agents[i]);
			String manfactBonus = adInfo.getManufacturerSpecialty();
			_manSpecialties.put(_agents[i], manfactBonus);
			String compBonus = adInfo.getComponentSpecialty();
			_compSpecialties.put(_agents[i], compBonus);

			HashMap<Query,Double> advEffect = new HashMap<Query, Double>();

			for(Query query : _querySpace) {
				advEffect.put(query, _userClickModel.getAdvertiserEffect(_userClickModel.queryIndex(query), i));
			}
			_advEffect.put(_agents[i], advEffect);
			Integer[] sales = new Integer[adInfo.getDistributionWindow()];
			for(int j = 0; j < adInfo.getDistributionWindow(); j++) {
				sales[j] = adInfo.getDistributionCapacity()/adInfo.getDistributionWindow();
			}
			_salesOverWindow.put(_agents[i], sales);
			_capacities.put(_agents[i],adInfo.getDistributionCapacity());
		}
		_ourAdvEffect = _advEffect.get(_agents[_ourAdvIdx]);
		_ourCompSpecialty = _compSpecialties.get(_agents[_ourAdvIdx]);
		_ourManSpecialty = _manSpecialties.get(_agents[_ourAdvIdx]);
	}

	/**
	 * This initializes all the day specific info that we need
	 * @param day
	 * @param advertiseridx
	 */
	public void initializeDaySpecificInfo(int day, int advertiseridx) {
		_ourAdvIdx = advertiseridx;
		_day = day;
		_adType = new HashMap<String,HashMap<Query,Ad>>();
		_bids = new HashMap<String,HashMap<Query,Double>>();
		_bidsWithoutOurs = new HashMap<String,HashMap<Query,Double>>();
		_budgets = new HashMap<String,HashMap<Query,Double>>();
		_totBudget = new HashMap<String,Double>();

		LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists = _status.getUserDistributions();
		HashMap<String, LinkedList<BankStatus>> bankStatuses = _status.getBankStatuses();
		HashMap<String, LinkedList<BidBundle>> bidBundles = _status.getBidBundles();
		HashMap<String, LinkedList<QueryReport>> queryReports = _status.getQueryReports();
		HashMap<String, LinkedList<SalesReport>> salesReports = _status.getSalesReports();
		HashMap<String, LinkedList<SimulationStatus>> simulationStatuses = _status.getSimulationStatuses();

		_ourSimulationStatus = simulationStatuses.get(_agents[_ourAdvIdx]).get(day);

		if(_day >= 59) {
			_usersMap = userDists.get(59);
		}
		else {
			_usersMap = userDists.get(_day + 1);
		}

		for(int i = 0; i < _agents.length; i++) {
			AdvertiserInfo adInfo = _advInfos.get(_agents[i]);
			BidBundle bidBundleTemp = bidBundles.get(_agents[i]).get(_day);
			_totBudget.put(_agents[i], bidBundleTemp.getCampaignDailySpendLimit());
			HashMap<Query,Ad> adType = new HashMap<Query, Ad>();
			HashMap<Query,Double> bids = new HashMap<Query, Double>();
			HashMap<Query,Double> budgets = new HashMap<Query, Double>();
			HashMap<Query,Double> bidsWithoutOurs = new HashMap<Query, Double>();
			for(Query query : _querySpace) {
				adType.put(query, bidBundleTemp.getAd(query));
				bids.put(query, bidBundleTemp.getBid(query));
				if(i != _ourAdvIdx) {
					bidsWithoutOurs.put(query, bidBundleTemp.getBid(query));
				}
				budgets.put(query,bidBundleTemp.getDailyLimit(query));
			}
			_adType.put(_agents[i], adType);
			_bids.put(_agents[i], bids);
			if(i != _ourAdvIdx) {
				_bidsWithoutOurs.put(_agents[i], bids);
			}
			_budgets.put(_agents[i], budgets);
		}
	}
	
	public Reports getSingleQueryReport(Query query, double bid) {
		Reports reports = singleQueryReports.get(query).get(bid);
		if(reports == null) {
			HashMap<Double, Reports> reportsMap = singleQueryReports.get(query);
			reportsMap.put(bid, runQuerySimulation(bid, new Ad(), query));
			singleQueryReports.put(query, reportsMap);
		}
		else {
			return reports;
		}
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public Set<AbstractModel> generatePerfectModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		PerfectUserModel userModel = new PerfectUserModel(_numUsers,_usersMap);
		PerfectQueryToNumImp queryToNumImp = new PerfectQueryToNumImp(userModel);
		models.add(userModel);
		models.add(queryToNumImp);
		for(Query query : _querySpace) {
			AbstractBidToSlotModel bidToSlotModel = new PerfectBidToPosition(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),_ourAdvIdx,query);
			AbstractBidToCPC bidToCPCModel = new PerfectBidToCPC(query,bidToSlotModel);
			AbstractSlotToPrClick slotToClickModel = new PerfectClickProb(_agents,_bidsWithoutOurs,_advEffect,_contProb,_adType,_compSpecialties,_salesOverWindow,_capacities,_ourAdvEffect.get(query),_squashing,_numPromSlots,_numSlots,_proReserve,_targEffect,_promSlotBonus,_ourAdvIdx,_retailCatalog, queryToNumImp, userModel,query);
			AbstractPrConversionModel convPrModel = new PerfectConversionProb(_CSB, _ourCompSpecialty,query,_retailCatalog,userModel, queryToNumImp);
			AbstractSlotToBidModel slotToBidModel = new PerfectPositionToBid(_agents,_bidsWithoutOurs,_advEffect,_squashing,_ourAdvEffect.get(query),_ourAdvIdx,query);
			AbstractSlotToNumClicks slotToNumClicks = new PerfectSlotToNumClicks(slotToClickModel,queryToNumImp,query);
			models.add(bidToSlotModel);
			models.add(bidToCPCModel);
			models.add(slotToClickModel);
			models.add(convPrModel);
			models.add(slotToBidModel);
			models.add(slotToNumClicks);
		}
		return models;
	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public BidBundle getBids(SimAbstractAgent agentToRun) {
		Set<AbstractModel> models = generatePerfectModels();
		BidBundle bundle = agentToRun.getBidBundle(models);
		return bundle;
	}

	public ArrayList<SimAgent> buildAgents(SimAbstractAgent agentToRun) {
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
				agent = new SimAgent(bids,budgets,totBudget,_advEffect.get(_agents[i]),adTypes,_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			else {
				agent = new SimAgent(_bids.get(_agents[i]),_budgets.get(_agents[i]),_totBudget.get(_agents[i]),_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
			}
			agents.add(agent);
		}
		return agents;
	}

	public ArrayList<SimAgent> buildSingleQueryAgents(double simBid, Ad simAd, Query simQuery) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				HashMap<Query,Double> bids = new HashMap<Query, Double>();
				HashMap<Query,Double> budgets = new HashMap<Query, Double>();
				HashMap<Query,Ad> adTypes = new HashMap<Query, Ad>();
				double totBudget = Double.NaN;
				for(Query query : _querySpace) {
					if(query == simQuery) {
						bids.put(query, simBid);
						budgets.put(query,Double.NaN);
						adTypes.put(query, simAd);
					}
					bids.put(query,0.0);
					budgets.put(query,0.0);
					adTypes.put(query,new Ad());
				}
				agent = new SimAgent(bids,budgets,totBudget,_advEffect.get(_agents[i]),adTypes,_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_testId ,_squashing,_querySpace);
			}
			else {
				agent = new SimAgent(_bids.get(_agents[i]),_budgets.get(_agents[i]),_totBudget.get(_agents[i]),_advEffect.get(_agents[i]),_adType.get(_agents[i]),_salesOverWindow.get(_agents[i]),_capacities.get(_agents[i]), _manSpecialties.get(_agents[i]),_compSpecialties.get(_agents[i]),_agents[i],_squashing,_querySpace);
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

	public Reports runQuerySimulation(double simBid, Ad simAd, Query simQuery) {
		ArrayList<SimAgent> agents = buildSingleQueryAgents(simBid,simAd,simQuery);
		ArrayList<SimUser> users = buildSearchingUserBase(_usersMap);
		Collections.shuffle(users);
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.generateQuery();
			if(query == null || query != simQuery) {
				//This means the user is IS or T
				//Second part is because we are only simulating one query
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
			/*
			 * Adds impressions
			 */
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j-1);
				double squashedBid = pair.getSquashedBid();
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					pair.getAgent().addImpressions(query, 0, 1, j);
				}
				else {
					pair.getAgent().addImpressions(query, 1, 0, j);
				}

			}
			/*
			 * Actually generates clicks and what not
			 */
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
					rand = _R.nextDouble();
					if(contProb >= rand) {
						continue;
					}
					else {
						break;
					}
				}
			}
		}
		SimAgent ourAgent = agents.get(_ourAdvIdx);
		QueryReport queryReport = ourAgent.buildQueryReport();
		SalesReport salesReport = ourAgent.buildSalesReport();
		Reports reports = new Reports(queryReport,salesReport);
		return reports;
	}
	
	/*
	 * Runs the simulation and generates reports
	 */
	public HashMap<String, Reports> runSimulation(SimAbstractAgent agentToRun) {
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
					if(totBudget/10.0 > totCost || Double.isNaN(totBudget)) {
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
			/*
			 * Adds impressions
			 */
			for(int j = 1; j <= _numSlots && j < pairList.size(); j++) {
				AgentBidPair pair = pairList.get(j-1);
				double squashedBid = pair.getSquashedBid();
				if(j <= _numPromSlots && squashedBid >= _proReserve) {
					pair.getAgent().addImpressions(query, 0, 1, j);
				}
				else {
					pair.getAgent().addImpressions(query, 1, 0, j);
				}

			}
			/*
			 * Actually generates clicks and what not
			 */
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
					rand = _R.nextDouble();
					if(contProb >= rand) {
						continue;
					}
					else {
						break;
					}
				}
			}
		}
		if(DEBUG) {
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
		}
		//		////TESTIOGs
		//		for(Query q : _querySpace) {
		//			System.out.println(q);
		//			for(int i = 0; i < agents.size(); i++) {
		//				if(i == _ourAdvIdx) {
		//					SimAgent agent = agents.get(i);
		//					System.out.println("Slot " + (agent.getPosSum(q)/(agent.getNumPromImps(q)+agent.getNumRegImps(q))) + "  conversions " + agent.getUnitsSold(q));
		//				}
		//			}
		//		}
		//
		//		//ASPIDMPASD
		HashMap<String,Reports> reportsMap = new HashMap<String, Reports>();
		for(SimAgent agent : agents) {
			QueryReport queryReport = agent.buildQueryReport();
			SalesReport salesReport = agent.buildSalesReport();
			Reports reports = new Reports(queryReport,salesReport);
			reportsMap.put(agent.getAdvId(),reports);
		}
		return reportsMap;
	}

	public String[] getUsableAgents() {
		String[] agentStrings = { "MCKP", "Cheap" , "EqProfit", "ILP", "newSSB"};
		return agentStrings;
	}

	public SimAbstractAgent stringToAgent(String string) {
		if(string.equals("MCKP")) {
			return new MCKPAgentMkII();
		}
		else if(string.equals("Cheap")) {
			return new Cheap();
		}
		else if(string.equals("EqProfit")) {
			return new EqPftAgent();
		}
		else if(string.equals("ILP")) {
			return new ILPAgent();
		}
		else if(string.equals("newSSB")) {
			return new NewSSB();
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
		String filename = "/game156.slg";
		int advId = 7;
		GameStatusHandler statusHandler = new GameStatusHandler(filename);
		GameStatus status = statusHandler.getGameStatus();
		double start = System.currentTimeMillis();
		int numSims = 1;
		sim.initializeBasicInfo(status,advId);
		for(int i = 0; i < numSims; i++) {
			sim.runFullSimulation(status, "MCKP",advId);
		}
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + ((elapsed / 1000)/numSims) + " seconds");
	}

}
