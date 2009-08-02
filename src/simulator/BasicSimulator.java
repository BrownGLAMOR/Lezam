package simulator;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import javax.management.RuntimeErrorException;

import newmodels.AbstractModel;
import newmodels.AvgPosToPos;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.targeting.BasicTargetModel;
import se.sics.tasim.aw.Message;
import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectBidToPrClick;
import simulator.models.PerfectBidToPrConv;
import simulator.models.PerfectQueryToNumImp;
import simulator.models.PerfectUnitsSoldModel;
import simulator.models.PerfectUserModel;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import usermodel.UserState;
import agents.AdMaxAgent;
import agents.BidPosModelTestAgent;
import agents.Cheap;
import agents.EquateProfitC;
import agents.ILPAgent;
import agents.MCKPAgentMkIIBids;
import agents.SimAbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
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

	private static final int NUM_PERF_ITERS = 1; //ALMOST ALWAYS HAVE THIS AT 2 MAX!!

	private static final boolean PERFECTMODELS = false;

	private static boolean CHART = false;

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

	private HashMap<String, AdvertiserInfo> _advInfos;

	private String _testId = "testId";

	private HashMap<Query,HashMap<Double,LinkedList<Reports>>> _singleQueryReports;

	private long lastSeed = 876451;

	private BidBundle _baseSolBundle;

	private ArrayList<SimUser> _pregenUsers;

	public HashMap<String,LinkedList<Reports>> runFullSimulation(GameStatus status, SimAbstractAgent agent, int advertiseridx) {
		System.out.println("Num Iterations: " + NUM_PERF_ITERS);
		HashMap<String,LinkedList<Reports>> reportsListMap = new HashMap<String, LinkedList<Reports>>();
		initializeBasicInfo(status, advertiseridx);
		agent.sendSimMessage(new Message("doesn't","matter",_pubInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_retailCatalog));
		agent.sendSimMessage(new Message("doesn't","matter",_slotInfo));
		agent.sendSimMessage(new Message("doesn't","matter",_ourAdvInfo));
		agent.initBidder();
		Set<AbstractModel> models = agent.initModels();
		agent.setModels(models);
		for(int i = 0; i < _agents.length; i++) {
			LinkedList<Reports> reports = new LinkedList<Reports>();
			reportsListMap.put(_agents[i], reports);
		}
		int firstDay = 0;

		for(int day = firstDay; day < 59; day++) {

			if(day >= 2) {
				LinkedList<Reports> reportsList = reportsListMap.get(_agents[advertiseridx]);
				/*
				 * Two day delay
				 */
				Reports reports = reportsList.get(day-2);
				SalesReport salesReport = reports.getSalesReport();
				QueryReport queryReport = reports.getQueryReport();
				agent.handleQueryReport(queryReport);
				agent.handleSalesReport(salesReport);
				if(!PERFECTMODELS) {
					agent.updateModels(salesReport, queryReport);
				}
			}
			lastSeed = getNewSeed();
			initializeDaySpecificInfo(day, advertiseridx);
			agent.setDay(day);
			/*
			 * Make the maps used in the perfect models
			 */
			_singleQueryReports = new HashMap<Query, HashMap<Double,LinkedList<Reports>>>();
			for(Query query : _querySpace) {
				_singleQueryReports.put(query,new HashMap<Double, LinkedList<Reports>>());
			}

			_baseSolBundle = null;
			_pregenUsers = null;
			HashMap<String, Reports> maps = runSimulation(agent);

			/*
			 * Keep track of capacities
			 */
			for(int i = 0; i < _agents.length; i++) {
				Integer[] sales = _salesOverWindow.get(_agents[i]);
				Integer[] newSales = new Integer[sales.length];

				int totConversions = 0;
				for(Query query : _querySpace) {
					totConversions += maps.get(_agents[i]).getSalesReport().getConversions(query);
				}
				newSales[0] = totConversions;

				for(int j = 0; j < sales.length-1; j++) {
					newSales[j+1] = sales[j];
				}
				_salesOverWindow.put(_agents[i], newSales);
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
		//		/*
		//		 * TESTING
		//		 */
		//		for(int i = 0; i < _agents.length; i++ ) {
		//			LinkedList<Reports> reports = reportsListMap.get(_agents[i]);
		//			System.out.println("Agent: "  + _agents[i]);
		//			double totalRevenue = 0;
		//			double totalCost = 0;
		//			double totalImp = 0;
		//			double totalClick = 0;
		//			double totalConv = 0;
		//			for(Reports report : reports) {
		//				QueryReport queryReport = report.getQueryReport();
		//				SalesReport salesReport = report.getSalesReport();
		//				for(Query query : _querySpace) {
		//					totalRevenue += salesReport.getRevenue(query);
		//					totalCost += queryReport.getCost(query);
		//					totalImp += queryReport.getImpressions(query);
		//					totalClick += queryReport.getClicks(query);
		//					totalConv += salesReport.getConversions(query);
		//				}
		//			}
		//			System.out.println("\tTotal Revenue: " + totalRevenue);
		//			System.out.println("\tTotal Cost: " + totalCost);
		//			System.out.println("\tTotal Impressions: " + totalImp);
		//			System.out.println("\tTotal Clicks: " + totalClick);
		//			System.out.println("\tTotal Conversions: " + totalConv);
		//			System.out.println("\tTotal Profit: " + (totalRevenue-totalCost));
		//		}
		return reportsListMap;
	}

	private long getNewSeed() {
		Random rand = new Random(lastSeed);
		long seed = rand.nextLong();
		return Math.abs(seed);
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
			if(i == _ourAdvIdx) {
				System.out.println("Our capacity" + adInfo.getDistributionCapacity());
			}
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
		HashMap<String, LinkedList<BidBundle>> bidBundles = _status.getBidBundles();
		_usersMap = userDists.get(_day);

		for(int i = 0; i < _agents.length; i++) {
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

	public LinkedList<Reports> getSingleQueryReport(Query query, double bid) {
		LinkedList<Reports> reports = _singleQueryReports.get(query).get(bid);
		if(reports == null) {
			HashMap<Double, LinkedList<Reports>> reportsMap = _singleQueryReports.get(query);
			Double lastBid = null;
			if(reportsMap.size() > 0) {
				ArrayList<Double> reportsKeyList = new ArrayList<Double>(reportsMap.keySet());
				Collections.sort(reportsKeyList);
				lastBid = reportsKeyList.get(reportsKeyList.size()-1);
			}
			if(lastBid != null) {
				LinkedList<Reports> lastReports = _singleQueryReports.get(query).get(lastBid);
				Reports lastReport = lastReports.getFirst();
				if(doubleEquals(lastReport.getQueryReport().getPosition(query),1.0)) {
					reportsMap.put(bid, lastReports);
					_singleQueryReports.put(query, reportsMap);
					return lastReports;
				}
			}
			LinkedList<Reports> tempReportsList = new LinkedList<Reports>();
			for(int i = 0; i < NUM_PERF_ITERS; i++) {
				Reports tempReports = runQuerySimulation(bid, new Ad(), query);
				if(lastBid != null) {
					LinkedList<Reports> lastReports = _singleQueryReports.get(query).get(lastBid);
					Reports lastReport = lastReports.getFirst();
					if(doubleEquals(lastReport.getQueryReport().getPosition(query), tempReports.getQueryReport().getPosition(query))) {
						reportsMap.put(bid, lastReports);
						_singleQueryReports.put(query, reportsMap);
						return lastReports;
					}
				}
				tempReportsList.add(tempReports);
			}
			reportsMap.put(bid, tempReportsList);
			_singleQueryReports.put(query, reportsMap);
			return tempReportsList;
		}
		else {
			return reports;
		}
	}

	private boolean doubleEquals(double position, double d) {
		double epsilon = .05;
		double diff = Math.abs(position - d);
		if(diff <= epsilon) {
			return true;
		}
		return false;
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public Set<AbstractModel> generatePerfectModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		PerfectUserModel userModel = new PerfectUserModel(_numUsers,_usersMap);
		PerfectQueryToNumImp queryToNumImp = new PerfectQueryToNumImp(userModel);
		PerfectUnitsSoldModel unitsSold = new PerfectUnitsSoldModel(_salesOverWindow.get(_agents[_ourAdvIdx]), _ourAdvInfo.getDistributionCapacity(), _ourAdvInfo.getDistributionWindow());
		AbstractBidToCPC bidToCPCModel = new PerfectBidToCPC(this);
		AbstractBidToPrClick bidToClickPrModel = new PerfectBidToPrClick(this);
		NewAbstractConversionModel bidToConvPrModel = new PerfectBidToPrConv(this);
		AbstractBidToSlotModel bidToSlotModel = new PerfectBidToPosition(this);
		BasicTargetModel basicTargModel = new BasicTargetModel(_ourAdvInfo.getManufacturerSpecialty(),_ourAdvInfo.getComponentSpecialty());
		models.add(userModel);
		models.add(queryToNumImp);
		models.add(unitsSold);
		models.add(bidToCPCModel);
		models.add(bidToClickPrModel);
		models.add(bidToConvPrModel);
		models.add(bidToSlotModel);
		models.add(basicTargModel);
		return models;
	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public BidBundle getBids(SimAbstractAgent agentToRun) {
		if(PERFECTMODELS) {
			agentToRun.setModels(generatePerfectModels());
		}
		BidBundle bidBundle = agentToRun.getBidBundle(agentToRun.getModels());
		return bidBundle;
	}

	public ArrayList<SimAgent> buildAgents(SimAbstractAgent agentToRun) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = null;
				if(PERFECTMODELS) {
					_baseSolBundle = null;
					int loopCounter = 0;
					ArrayList<double[]> bidVectors = new ArrayList<double[]>();
					while(true) {
						loopCounter++;
						_singleQueryReports = new HashMap<Query, HashMap<Double,LinkedList<Reports>>>();
						for(Query query : _querySpace) {
							_singleQueryReports.put(query,new HashMap<Double, LinkedList<Reports>>());
						}
						bundle = getBids(agentToRun);
						double[] bidVector = new double[16];
						int count = 0;
						for(Query query : _querySpace) {
							bidVector[count] = bundle.getBid(query);
							count++;
						}
						bidVectors.add(bidVector);
						if(_baseSolBundle != null) {
							if(bidMapEqual(bundle, _baseSolBundle)) {
								break;
							}

							//Search for cycles
							if(((loopCounter + 1) % 10 == 0) && cycleDetect(bidVectors)) {
								System.out.println("\n\n CYCLE \n\n");
								break;
							}
						}
						_baseSolBundle = bundle;
						HashMap<String, Reports> reports = runSimulation(_baseSolBundle);
						Reports ourReports = reports.get(_agents[_ourAdvIdx]);
						QueryReport queryReport = ourReports.getQueryReport();
						SalesReport salesReport = ourReports.getSalesReport();
						double profit = 0.0;
						for(Query query : _querySpace) {
							profit += salesReport.getRevenue(query) - queryReport.getCost(query);
						}
						System.out.println("Profit: " + profit);
					}
					System.out.println("Bids:");
					for(Query query : _querySpace) {
						System.out.println("\t" + bundle.getBid(query));
					}
					System.out.println(loopCounter);
				}
				else {
					bundle = getBids(agentToRun);
				}
				agentToRun.handleBidBundle(bundle);
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

	public ArrayList<SimAgent> buildAgents(BidBundle agentToRun) {
		ArrayList<SimAgent> agents = new ArrayList<SimAgent>();
		for(int i = 0; i < _agents.length; i++) {
			SimAgent agent;
			if(i == _ourAdvIdx) {
				BidBundle bundle = agentToRun;
				//				agentToRun.handleBidBundle(bundle);
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

	//Floyd's cycle-finding algorithm
	private boolean cycleDetect(ArrayList<double[]> bidVectors) {
		int len = bidVectors.size();
		int tortoise= 1;
		int hare = 2;
		while(hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			tortoise += 1;
			hare += 2;
		}

		if(hare > len) {
			return false;
		}

		System.out.println("Tortoise: " + tortoise);
		System.out.println("Hare: " + hare);

		int mu = 0;
		hare = tortoise;
		tortoise = 0; 
		while(tortoise < len && hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			tortoise += 1;
			hare += 1;
			mu += 1;
		}

		System.out.println("Mu: " + mu);

		int lam = 1;
		hare = tortoise + 1;
		while(tortoise < len && hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			hare += 1;
			lam += 1;
		}
		System.out.println("Lam: " + lam);
		return true;
	}

	private boolean bidVectorequal(double[] vector1, double[] vector2) {
		if(vector1.length != vector2.length) {
			throw new RuntimeException("Vectors unequal length");
		}
		double epsilon = .005;
		for(int i = 0; i < vector1.length; i++) {
			if(vector1[i] - vector2[i] > epsilon) {
				return false;
			}
		}
		return true;
	}

	private boolean bidMapEqual(BidBundle bundle, BidBundle baseSolBundle) {
		if(bundle.size() != baseSolBundle.size()) {
			throw new RuntimeException("Vectors unequal length");
		}
		double epsilon = .005;
		for(Query query : _querySpace) {
			if(bundle.getBid(query) - baseSolBundle.getBid(query) > epsilon) {
				return false;
			}
		}
		return true;
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
					if(query.equals(simQuery)) {
						bids.put(query, simBid);
						budgets.put(query,Double.NaN);
						adTypes.put(query, simAd);
					}
					else{
						if(_baseSolBundle != null) {
							bids.put(query,_baseSolBundle.getBid(query));
							budgets.put(query,_baseSolBundle.getDailyLimit(query));
							adTypes.put(query,_baseSolBundle.getAd(query));
						}
						else {
							bids.put(query,_bids.get(_agents[i]).get(query));
							budgets.put(query,_budgets.get(_agents[i]).get(query));
							adTypes.put(query,_adType.get(_agents[i]).get(query));
						}
					}
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
		_R.setSeed(lastSeed);
		for(Product prod : _retailCatalog) {
			for(UserState state : UserState.values()) {
				int users = usersMap.get(prod).get(state);
				for(int i = 0; i < users; i++) {
					SimUser user = new SimUser(prod,state,_R.nextLong());
					usersList.add(user);
				}
			}
		}
		return usersList;
	}

	public Reports runQuerySimulation(double simBid, Ad simAd, Query simQuery) {
		ArrayList<SimAgent> agents = buildSingleQueryAgents(simBid,simAd,simQuery);
		ArrayList<SimUser> users;
		_R.setSeed(lastSeed);
		if(_pregenUsers != null) {
			users = _pregenUsers;
		}
		else {
			users = buildSearchingUserBase(_usersMap);
			Random randGen = new Random(lastSeed);
			Collections.shuffle(users,randGen);
			_pregenUsers  = users;
		}
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.getUserQuery();
			if(query == null) {
				//This means the user is IS or T
				continue;
			}
			ArrayList<AgentBidPair> pairList = new ArrayList<AgentBidPair>();
			for(int j = 0; j < agents.size(); j++) {
				SimAgent agent = agents.get(j);
				double bid = agent.getBid(query);
				double budget = agent.getBudget(query);
				double cost = agent.getCost(query);
				if(budget > cost + bid || Double.isNaN(budget)) {
					double totBudget = agent.getTotBudget();
					double totCost = agent.getTotCost();
					if(totBudget > totCost + bid || Double.isNaN(totBudget)) {
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
					double cpc = squashedBidUnder / Math.pow(advEffect, _squashing) + .01;
					cpc = ((int) ((cpc * 100) + 0.5)) / 100.0;
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

					if(compSpecialty.equals(queryComp)) {
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
	public HashMap<String, Reports> runSimulation(Object agentToRun) {
		ArrayList<SimAgent> agents;
		if(agentToRun instanceof SimAbstractAgent) {
			agents = buildAgents((SimAbstractAgent) agentToRun);
		}
		else if(agentToRun instanceof BidBundle) {
			agents = buildAgents((BidBundle) agentToRun);
		}
		else {
			throw new RuntimeException("Build agents can only take type SimAbstractAgent or BidBundle");			
		}
		ArrayList<SimUser> users;
		if(_pregenUsers != null) {
			users = _pregenUsers;
		}
		else {
			users = buildSearchingUserBase(_usersMap);
			Random randGen = new Random(lastSeed);
			Collections.shuffle(users,randGen);
			_pregenUsers  = users;
		}
		_R.setSeed(lastSeed);
		for(int i = 0; i < users.size(); i++) {
			SimUser user = users.get(i);
			Query query = user.getUserQuery();
			if(query == null) {
				//This means the user is IS or T
				continue;
			}
			ArrayList<AgentBidPair> pairList = new ArrayList<AgentBidPair>();
			for(int j = 0; j < agents.size(); j++) {
				SimAgent agent = agents.get(j);
				double bid = agent.getBid(query);
				double budget = agent.getBudget(query);
				double cost = agent.getCost(query);
				if(budget > cost + bid || Double.isNaN(budget)) {
					double totBudget = agent.getTotBudget();
					double totCost = agent.getTotCost();
					if(totBudget > totCost + bid || Double.isNaN(totBudget)) {
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
					double cpc = squashedBidUnder / Math.pow(advEffect, _squashing) + .01;
					cpc = ((int) ((cpc * 100) + 0.5)) / 100.0;
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
					if(agent.getAdvId().equals(_agents[_ourAdvIdx])) {
						//						debug(agent.getAdvId() + " is overcap by " + overCap);
					}
					double convPr = Math.pow(_LAMBDA, Math.max(0.0, overCap))*baselineConv;

					String queryComp = query.getComponent();
					String compSpecialty = agent.getCompSpecialty();

					if(compSpecialty.equals(queryComp)) {
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
			AvgPosToPos avgPosModel20 = new AvgPosToPos(20);
			AvgPosToPos avgPosModel40 = new AvgPosToPos(40);
			AvgPosToPos avgPosModel80 = new AvgPosToPos(80);
			AvgPosToPos avgPosModel160 = new AvgPosToPos(160);
			AvgPosToPos avgPosModel320 = new AvgPosToPos(320);
			AvgPosToPos avgPosModel640 = new AvgPosToPos(640);
			AvgPosToPos avgPosModelall = new AvgPosToPos(1000000);
			for(int i = 0; i < agents.size(); i++) {
				SimAgent agent = agents.get(i);
				if(i == _ourAdvIdx) {
					debug("****US****");
					//				}
					debug("Adv Id: " + agent.getAdvId());
					debug("\tTotal Cost: " + agent.getTotCost());
					debug("\tTotal Budget: " + agent.getTotBudget());
					debug("\tTotal revenue: " + agent.getTotRevenue());
					debug("\tTotal Units Sold: " + agent.getTotUnitsSold());
					for(Query query : _querySpace) {
						debug("\t Query: " + query);
						debug("\t\t Bid: " + agent.getBid(query));
						debug("\t\t CPC: " + agent.getCPC(query));
						debug("\t\t Cost: " + agent.getCost(query));
						debug("\t\t Budget: " + agent.getBudget(query));
						debug("\t\t Revenue: " + agent.getRevenue(query));
						debug("\t\t Units Sold: " + agent.getUnitsSold(query));
						debug("\t\t Num Clicks: " + agent.getNumClicks(query));
						debug("\t\t Num Prom Slots: " + _numPromSlots);
						debug("\t\t Prom Impressions: " + agent.getNumPromImps(query));
						debug("\t\t Reg Impressions: " + agent.getNumRegImps(query));
						debug("\t\t Avg Pos per Imp: " + (agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))));
						double[] perQPos = agent.getPerQPosSum(query);
						for(int j = 0; j < 5; j++) {
							debug("\t\t Imps in Slot " + (j+1) + ": " + (perQPos[j]));
						}
//						if(!Double.isNaN((agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))))) {
//							double[] expPos = avgPosModel80.getPrediction(query, agent.getNumRegImps(query), agent.getNumPromImps(query), (agent.getPosSum(query)/(agent.getNumPromImps(query)+agent.getNumRegImps(query))), agent.getNumClicks(query), _numPromSlots);
//							for(int j = 0; j < 5; j++) {
//								debug("\t\t Estimated Imps in Slot " + (j+1) + ": " + (expPos[j]));
//							}
//							System.out.println("Likelihood: " + KLLikelihood(normalizeArr(perQPos),normalizeArr(expPos)));
//						}
					}
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

	/*
	 * Kullback Leibler Divergence Test
	 */
	public double KLDivergence(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("KL Divergence requires arrays of equal length");
		}

		double divergence = 0.0;

		for(int i = 0; i < P.length; i++) {
			divergence += P[i] * (Math.log(P[i])-Math.log(Q[i]));
		}

		return divergence;
	}

	/*
	 * When P and Q are discrete, we can get the likelihood of Q from
	 * the KL divergence.  We want to minimize the KL divergence, which will
	 * in turn maximize the likelihood
	 */
	public double KLLikelihood(double[] P, double[] Q) {
		double divergence = KLDivergence(P, Q);
		double likelihood = Math.exp(-1*divergence*P.length);
		return likelihood;
	}
	
	private double[] normalizeArr(double[] predictions) {
		double total = 0.0;
		for(int i = 0 ; i < predictions.length; i++) {
			total += predictions[i];
		}
		if(total == 1.0) {
			return predictions;
		}
		else {
			double[] newpredictions = new double[predictions.length];
			for(int i = 0 ; i < predictions.length; i++) {
				newpredictions[i] = predictions[i]/total;
			}
			return newpredictions;
		}
	}

	public String[] getUsableAgents() {
		String[] agentStrings = { "MCKP", "Cheap" , "Crest", "ILP", "newSSB"};
		return agentStrings;
	}

	public SimAbstractAgent stringToAgent(String string) {
		if(string.equals("MCKP")) {
			return new MCKPAgentMkIIBids();
		}
		else if(string.equals("Cheap")) {
			return new Cheap();
		}
		else if(string.equals("Crest")) {
			return new AdMaxAgent();
		}
		else if(string.equals("ILP")) {
			return new ILPAgent();
		}
		else if(string.equals("newSSB")) {
			//TODO
			return null;
		}
		else {
			return new MCKPAgentMkIIBids();
		}
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

	/*
	 * Returns the means and std deviation
	 * index 0 is the mean
	 * index 1 is the std deviation
	 */
	public double[] stdDeviation(Double[] revenueErrorArr) {
		double[] meanAndStdDev = new double[2];
		meanAndStdDev[0] = 0.0;
		meanAndStdDev[1] = 0.0;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[0] += revenueErrorArr[i];
		}
		meanAndStdDev[0] /= revenueErrorArr.length;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[1] +=  (revenueErrorArr[i] - meanAndStdDev[0])*(revenueErrorArr[i] - meanAndStdDev[0]);
		}
		meanAndStdDev[1] /= revenueErrorArr.length;
		meanAndStdDev[1] = Math.sqrt(meanAndStdDev[1]);
		return meanAndStdDev;
	}

	public double[] percentErrorOfSim() throws IOException, ParseException {
		double RMSRevenue = 0.0;
		double RMSCost = 0.0;
		double RMSImp = 0.0;
		double RMSClick = 0.0;
		double RMSConv = 0.0;
		double totAvgRevenue = 0.0;
		double totAvgCost = 0.0;
		double totAvgImp = 0.0;
		double totAvgClick = 0.0;
		double totAvgConv = 0.0;
		int numSims = 2;
		//		String baseFile = "/Users/jordan/Downloads/aa-server-0.9.6/logs/sims/localhost_sim";
		//		String baseFile = "/games/game";
		//		String baseFile = "/Users/jordanberg/Desktop/mckpgames/localhost_sim";
		String baseFile = "/u/jberg/Desktop/mckpgames/localhost_sim";
		//		String baseFile = "C:/mckpgames/localhost_sim";

		int min = 454;
		int max = 470;
		String[] filenames = new String[max-min];
		System.out.println("Min: " + min + "  Max: " + max + "  Num Sims: " + numSims);
		for(int i = min; i < max; i++) { 
			filenames[i-min] = baseFile + i + ".slg";
		}
		for(int fileIdx = 0; fileIdx < filenames.length; fileIdx++) {
			String filename = filenames[fileIdx];
			int advId = 4;
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			_querySpace = new LinkedHashSet<Query>();
			_querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				_querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				_querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				_querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			double[] totalRevenueList = new double[agents.length];
			double[] totalCostList = new double[agents.length];
			double[] totalImpList = new double[agents.length];
			double[] totalClickList = new double[agents.length];
			double[] totalConvList = new double[agents.length];		

			HashMap<String, LinkedList<QueryReport>> queryReportListMap = status.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReportListMap = status.getSalesReports();
			for(int i = 0; i < agents.length; i++ ) {
				LinkedList<QueryReport> queryReportList = queryReportListMap.get(agents[i]);
				LinkedList<SalesReport> salesReportList = salesReportListMap.get(agents[i]);
				double totalRevenue = 0;
				double totalCost = 0;
				double totalImp = 0;
				double totalClick = 0;
				double totalConv = 0;
				for(int j = 0; j < queryReportList.size(); j++) {
					QueryReport queryReport = queryReportList.get(j);
					SalesReport salesReport = salesReportList.get(j);
					for(Query query : _querySpace) {
						totalRevenue += salesReport.getRevenue(query);
						totalCost += queryReport.getCost(query);
						totalImp += queryReport.getImpressions(query);
						totalClick += queryReport.getClicks(query);
						totalConv += salesReport.getConversions(query);
					}
				}
				totalRevenueList[i] = totalRevenue;
				totalCostList[i] = totalCost;
				totalImpList[i] = totalImp;
				totalClickList[i] = totalClick;
				totalConvList[i] = totalConv;
				totAvgRevenue += totalRevenue;
				totAvgCost += totalCost;
				totAvgImp += totalImp;
				totAvgClick += totalClick;
				totAvgConv += totalConv;
			}

			HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap = new HashMap<String,LinkedList<LinkedList<Reports>>>();
			for(int i = 0; i < agents.length; i++) {
				LinkedList<LinkedList<Reports>> reportsList = new LinkedList<LinkedList<Reports>>();
				reportsListMap.put(agents[i], reportsList);
			}
			for(int i = 0; i < numSims; i++) {
				HashMap<String, LinkedList<Reports>> maps = runFullSimulation(status, new BidPosModelTestAgent(), advId);
				for(int j = 0; j < agents.length; j++) {
					LinkedList<LinkedList<Reports>> reportsList = reportsListMap.get(agents[j]);
					reportsList.add(maps.get(agents[j]));
					reportsListMap.put(agents[j], reportsList);
				}
			}

			for(int i = 0; i < _agents.length; i++ ) {
				double totRevenueReal = totalRevenueList[i];
				double totCostReal = totalCostList[i];
				double totImpReal = totalImpList[i];
				double totClickReal = totalClickList[i];
				double totConvReal = totalConvList[i];
				LinkedList<LinkedList<Reports>> reportsList = reportsListMap.get(_agents[i]);
				for(int j = 0; j < reportsList.size(); j++) {
					LinkedList<Reports> reports = reportsList.get(j);
					double totalRevenue = 0;
					double totalCost = 0;
					double totalImp = 0;
					double totalClick = 0;
					double totalConv = 0;
					for(Reports report : reports) {
						QueryReport queryReport = report.getQueryReport();
						SalesReport salesReport = report.getSalesReport();
						for(Query query : _querySpace) {
							totalRevenue += salesReport.getRevenue(query);
							totalCost += queryReport.getCost(query);
							totalImp += queryReport.getImpressions(query);
							totalClick += queryReport.getClicks(query);
							totalConv += salesReport.getConversions(query);
						}
					}
					RMSRevenue += ((totalRevenue-totRevenueReal)*(totalRevenue-totRevenueReal));
					RMSCost += ((totalCost-totCostReal)*(totalCost-totCostReal));
					RMSImp += ((totalImp-totImpReal)*(totalImp-totImpReal));
					RMSClick += ((totalClick-totClickReal)*(totalClick-totClickReal));
					RMSConv += ((totalConv-totConvReal)*(totalConv-totConvReal));
				}
			}
		}

		/*
		 * Average actual values
		 * We divide by the number of sims times the number of advertisers
		 */
		totAvgRevenue /= ((max-min)*(_agents.length));
		totAvgCost /= ((max-min)*(_agents.length));
		totAvgImp /= ((max-min)*(_agents.length));
		totAvgClick /= ((max-min)*(_agents.length));
		totAvgConv /= ((max-min)*(_agents.length));


		/*
		 * Mean Square calculation
		 * need to divide by the number of samples
		 */
		RMSRevenue /= ((max-min)*(_agents.length)*numSims);
		RMSCost /= ((max-min)*(_agents.length)*numSims);
		RMSImp /= ((max-min)*(_agents.length)*numSims);
		RMSClick /= ((max-min)*(_agents.length)*numSims);
		RMSConv /= ((max-min)*(_agents.length)*numSims);

		//RMS
		RMSRevenue = Math.sqrt(RMSRevenue);
		RMSCost = Math.sqrt(RMSCost);
		RMSImp = Math.sqrt(RMSImp);
		RMSClick = Math.sqrt(RMSClick);
		RMSConv = Math.sqrt(RMSConv);

		double[] error = new double[5];
		error[0] = RMSRevenue/totAvgRevenue;
		error[1] = RMSCost/totAvgCost;
		error[2] = RMSImp/totAvgImp;
		error[3] = RMSClick/totAvgClick;
		error[4] = RMSConv/totAvgConv;

		return error;
	}

	public static void main(String[] args) throws IOException, ParseException {
		BasicSimulator sim = new BasicSimulator();
		//		String filename = "/pro/aa/qual/logs/parsed/game167.slg";
		//		if(args.length > 0) { 
		//			filename = args[0] + ".slg";
		//		}
		//		int advId = 0;
		//		GameStatusHandler statusHandler = new GameStatusHandler(filename);
		//		GameStatus status = statusHandler.getGameStatus();
		//		
		double start = System.currentTimeMillis();

		//		int numSims = 1;
		//		String agent = "MCKP";
		//
		//		LinkedList<LinkedList<Reports>> reportsList = new LinkedList<LinkedList<Reports>>();
		//		for(int i = 0; i < numSims; i++) {
		//			//			HashMap<String, LinkedList<Reports>> maps = sim.runFullSimulation(status, new MCKPAgentMkIIBids(args[1]), advId);
		//			HashMap<String, LinkedList<Reports>> maps = sim.runFullSimulation(status, new Cheap(), advId);
		//			reportsList.add(maps.get(sim._agents[advId]));
		//		}
		//
		//		if(CHART ) {
		//
		//			String[] agents = new String[1];
		//			agents[0] = agent;
		//
		//			ChartUtils chartUtils = new ChartUtils(sim,agents);
		//
		//			HashMap<String,LinkedList<LinkedList<Reports>>> map = new HashMap<String, LinkedList<LinkedList<Reports>>>();
		//			map.put(agent, reportsList);
		//
		//			JFreeChart chart = chartUtils.dailyProfitsChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("dailyProfitsChart.png"), chart, 1280, 800);
		//
		//			chart = chartUtils.dailyClicksChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("dailyClicksChart.png"), chart, 1280, 800);
		//
		//			chart = chartUtils.dailyConvsChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("dailyConvsChart.png"), chart, 1280, 800);
		//
		//			chart = chartUtils.dailyImpsChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("dailyImpsChart.png"), chart, 1280, 800);
		//
		//			chart = chartUtils.dailyWindowChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("dailyWindowChart.png"), chart, 1280, 800);
		//
		//			chart = chartUtils.fullSimProfitsChart(map, agents);
		//			ChartUtilities.saveChartAsPNG(new File("fullSimProfitsChart.png"), chart, 1280, 800);
		//		}

		double[] percentError = sim.percentErrorOfSim();

		System.out.println(percentError[0]);
		System.out.println(percentError[1]);
		System.out.println(percentError[2]);
		System.out.println(percentError[3]);
		System.out.println(percentError[4]);

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");

	}

}
