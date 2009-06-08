package OneDaySimulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import agents.G3Agent;
import agents.ILPAgent;
import agents.old.OldILPAgent;

import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
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
 * @author ml63 (taken from jberg)
 * 
 */
public class OneDaySimulator extends ILPAgent{	// You only need to change this line in order to choose an agent

	Random _R = new Random();

	private static final String[] manSet = {"pg","lioneer","flat"};
	private static final String[] prodSet = {"tv","dvd","audio"};
	private double _numUsers;
	private int _numAgents;
	private int _numSlots;

	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<String,String> _manfactBonus;
	private HashMap<String,String> _compBonus;
	private HashMap<String,Integer> _overCap;
	private HashMap<Query,Double> _contProb;
	private HashMap<UserState,Double> _users;
	private Set<Query> _thisQuerySpace;

	/*
	 * 
	 */
	public OneDaySimulator(String[] agents) {
		_numUsers = 90000;
		_numAgents = agents.length;
		_numSlots = 5;
		_agents = new String[_numAgents];
		_agents = agents.clone();
		_adType = new HashMap<String,HashMap<Query,Ad>>();
		_bids = new HashMap<String,HashMap<Query,Double>>();
		_budgets = new HashMap<String,HashMap<Query,Double>>();
		_totBudget = new HashMap<String,Double>();
		_advEffect = new HashMap<String,HashMap<Query,Double>>();
		_manfactBonus = new HashMap<String,String>();
		_compBonus = new HashMap<String,String>();
		_overCap = new HashMap<String, Integer>();
		_contProb = new HashMap<Query,Double>();
		_users = new HashMap<UserState,Double>();
		_thisQuerySpace = new LinkedHashSet<Query>();

		for (String sm : manSet) {
			for (String sp : prodSet) {
				_thisQuerySpace.add(new Query(sm,sp));
			}
			_thisQuerySpace.add(new Query(sm,null));			
		}
		_thisQuerySpace.add(new Query(null,"tv"));
		_thisQuerySpace.add(new Query(null,"dvd"));
		_thisQuerySpace.add(new Query(null,"audio"));
		_thisQuerySpace.add(new Query(null,null));
		
	}

	/*
	 * Initializes the state of the simulation
	 */
	public void initializeGameState() {
		/*
		 * Initialize all the bidding information
		 */

		HashMap<Query,Double> bids;
		HashMap<Query,Double> budgets;
		HashMap<Query,Ad> ads;
		HashMap<Query,Double> advEffect;
		for(int i = 0; i < _numAgents; i++) {
			bids = new HashMap<Query,Double>();
			budgets = new HashMap<Query,Double>();
			advEffect = new HashMap<Query, Double>();
			ads = new HashMap<Query,Ad>();
			for(Query query:_thisQuerySpace) {
				bids.put(query, randDouble(.5,3.5));
				budgets.put(query, bids.get(query)*50);
				ads.put(query,new Ad());
				if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					advEffect.put(query, 0.2);
					_contProb.put(query, 0.2);
				}
				else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
					advEffect.put(query, 0.3);
					_contProb.put(query, 0.3);
				}
				else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
					advEffect.put(query, 0.4);
					_contProb.put(query, 0.4);
				}
				else {
					throw new RuntimeException("Bad QuerySpace");
				}
			}
			_bids.put(_agents[i], bids);
			_budgets.put(_agents[i], budgets);
			_adType.put(_agents[i], ads);
			_totBudget.put(_agents[i], Double.NaN);
			_advEffect.put(_agents[i], advEffect);
			_manfactBonus.put(_agents[i], "lioneer");
			_compBonus.put(_agents[i],"tv");
			_overCap.put(_agents[i], (int) Math.ceil(randDouble(1,50)));
		}

		/*
		 * Initialize the User Population
		 */
		_users.put(UserState.IS, (_numUsers*2)/10);
		_users.put(UserState.F0, _numUsers/10);
		_users.put(UserState.F1, _numUsers/10);
		_users.put(UserState.F2, _numUsers/10);
		_users.put(UserState.T, _numUsers/10);
		_users.put(UserState.NS, (_numUsers*4)/10);
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public void generateModels() {

	}

	/*
	 * Initializes a bidding agent with the proper models
	 */
	public void intializeBidder() {

	}

	/*
	 * Gets the bids from the agent using perfect models
	 */
	public void getBids() {

	}

	/*
	 * Runs the simulation and generates reports
	 */
	public void runSimulation() {

	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}
	
	private Set<Query> getQuerySpace() {
		/**
		 * List of all the possible queries made available in the {@link RetailCatalog retail catalog}.
		 */
		return _thisQuerySpace;
	}

	private StartInfo getStartInfo() {
		/**
		 * Basic simulation information. {@link StartInfo} contains
		 * <ul>
		 * <li>simulation ID</li>
		 * <li>simulation start time</li>
		 * <li>simulation length in simulation days</li>
		 * <li>actual seconds per simulation day</li>
		 * </ul>
		 * An agent should receive the {@link StartInfo} at the beginning of the game or during recovery.
		 */
		StartInfo si = new myStartInfo();
		return si;
	}

	private SlotInfo getSlotInfo () {
		/**
		 * Basic auction slot information. {@link SlotInfo} contains
		 * <ul>
		 * <li>the number of regular slots</li>
		 * <li>the number of promoted slots</li>
		 * <li>promoted slot bonus</li>
		 * </ul>
		 * An agent should receive the {@link SlotInfo} at the beginning of the game or during recovery.
		 * This information is identical for all auctions over all query classes.
		 */
		SlotInfo si = new SlotInfo();
		si.setPromotedSlotBonus(0.5);
		si.setPromotedSlots(2);
		si.setRegularSlots(3);
		return si;
	}
	private RetailCatalog getRetailCatalog() {
	/**
	 * The retail catalog. {@link RetailCatalog} contains
	 * <ul>
	 * <li>the product set</li>
	 * <li>the sales profit per product</li>
	 * <li>the manufacturer set</li>
	 * <li>the component set</li>
	 * </ul>
	 * An agent should receive the {@link RetailCatalog} at the beginning of the game or during recovery.
	 */
	
		RetailCatalog rc = new RetailCatalog();
//		rc.setSalesProfit(new Product("pg" , "tv"), 10);
//		rc.setSalesProfit(new Product("pg" , "dvd"), 10);
//		rc.setSalesProfit(new Product("pg" , "audio"), 10);
//		rc.setSalesProfit(new Product("lioneer" , "tv"), 10);
//		rc.setSalesProfit(new Product("lioneer" , "dvd"), 10);
//		rc.setSalesProfit(new Product("lioneer" , "audio"), 10);
//		rc.setSalesProfit(new Product("flat" , "tv"), 10);
//		rc.setSalesProfit(new Product("flat" , "dvd"), 10);
//		rc.setSalesProfit(new Product("flat" , "audio"), 10);
		
		for (String sm : manSet) {
			for (String sp : prodSet) {
				rc.setSalesProfit(new Product(sm , sp), 10);				
			}
		}
		
		return rc;
	}
	
	private AdvertiserInfo getAdvertiserInfo(String agent) {
	/**
	 * The basic advertiser specific information. {@link AdvertiserInfo} contains
	 * <ul>
	 * <li>the manufacturer specialty</li>
	 * <li>the component specialty</li>
	 * <li>the manufacturer bonus</li>
	 * <li>the component bonus</li>
	 * <li>the distribution capacity discounter</li>
	 * <li>the address of the publisher agent</li>
	 * <li>the distribution capacity</li>
	 * <li>the address of the advertiser agent</li>
	 * <li>the distribution window</li>
	 * <li>the target effect</li>
	 * <li>the focus effects</li>
	 * </ul>
	 * An agent should receive the {@link AdvertiserInfo} at the beginning of the game or during recovery.
	 */
		
		AdvertiserInfo ai = new AdvertiserInfo();
		ai.setAdvertiserId(agent);
		ai.setComponentBonus(0.5);
		ai.setComponentSpecialty(prodSet[1]);
		ai.setDistributionCapacity(400);
		ai.setDistributionCapacityDiscounter(0.995);
		ai.setDistributionWindow(5);
		ai.setFocusEffects(QueryType.FOCUS_LEVEL_ZERO, 0.1);
		ai.setFocusEffects(QueryType.FOCUS_LEVEL_ONE, 0.2);
		ai.setFocusEffects(QueryType.FOCUS_LEVEL_TWO, 0.3);
		ai.setManufacturerBonus(0.5);
		ai.setManufacturerSpecialty(manSet[2]);
		//ai.setPublisherId(publisherId);
		ai.setTargetEffect(0.5);
		
		return ai;
	}
	private PublisherInfo getPublisherInfo() {
	/**
	 * The basic publisher information. {@link PublisherInfo} contains
	 * <ul>
	 * <li>the squashing parameter</li>
	 * </ul>
	 * An agent should receive the {@link PublisherInfo} at the beginning of the game or during recovery.
	 */
	
		PublisherInfo pi = new PublisherInfo();
		pi.setSquashingParameter(0.05);
		return pi;
	}
	
	private Queue<SalesReport> getSalesReport(){
	/**
	 * The list contains all of the {@link SalesReport sales report} delivered to the agent.  Each
	 * {@link SalesReport sales report} contains the conversions and sales revenue accrued by the agent for each query
	 * class during the period.
	 */
		
		Queue<SalesReport> srQueue = new LinkedList<SalesReport>();
		SalesReport sr = new SalesReport();
		int add = 0;
		for (Query q : _thisQuerySpace) {
			sr.setConversionsAndRevenue(q, 50+add, 750+add*10);
			add += 5;
		}
		srQueue.add(sr);
		
		return srQueue;
	}
		
	private Queue<QueryReport> getQueryReport() {
		/**
		 * The list contains all of the {@link QueryReport query reports} delivered to the agent.  Each
		 * {@link QueryReport query report} contains the impressions, clicks, cost, average position, and ad displayed
		 * by the agent for each query class during the period as well as the positions and displayed ads of all advertisers
		 * during the period for each query class.
		 */
		Queue<QueryReport> qrQueue = new LinkedList<QueryReport>();
		QueryReport qr = new QueryReport();
		for (Query q : _thisQuerySpace) {
			qr.setAd(q, new Ad());
			qr.setClicks(q, 500);
			qr.setCost(q, 500);
			qr.setImpressions(q, 5000, 1000);
			for (int i=0 ; i<_numAgents ; i++) qr.setPosition(q, _agents[i], Math.min(2+i, _numSlots+1));
		}
		qrQueue.add(qr);
		return qrQueue;
	}

	private SimulationStatus getSimulationStatus(String s) {
		SimulationStatus ss;
		if (s.equals("first day")) ss = new SimulationStatus();
		else ss = new mySimulationStatus();
		return ss;
	}

	private BankStatus getBankStatus() {
		BankStatus bs = new BankStatus();
		bs.setAccountBalance(10000);
		return bs;
	}
	
	private ReserveInfo getReserveInfo() {
		ReserveInfo ri = new ReserveInfo();
		ri.setPromotedReserve(0.2);
		ri.setRegularReserve(0.4);
		return ri;
	}

	
	public static void main(String[] args) {
		String agents[] = {"MyAgent"};
		OneDaySimulator cds = new OneDaySimulator(agents);
		Set<Query> querySpace = new HashSet<Query>(cds.getQuerySpace());
		Queue<QueryReport> queryReports = new LinkedList<QueryReport>(cds.getQueryReport());
		StartInfo startInfo = cds.getStartInfo(); 
		SlotInfo slotInfo = cds.getSlotInfo();
		RetailCatalog retailCatalog = cds.getRetailCatalog();
		AdvertiserInfo advertiserInfo = cds.getAdvertiserInfo(agents[0]);
		PublisherInfo publisherInfo = cds.getPublisherInfo();
		Queue<SalesReport> salesReports = cds.getSalesReport();
		SimulationStatus simulationStatus = cds.getSimulationStatus("first day");
		BankStatus bankStatus = cds.getBankStatus();	// there is no handler, so this is never used
		ReserveInfo reserveInfo = cds.getReserveInfo();	// there is no handler, so this is never used

		cds.initializeGameState();
		
		cds.handlePublisherInfo(publisherInfo);
		cds.handleSlotInfo(slotInfo);
		cds.handleRetailCatalog(retailCatalog);
		cds.handleAdvertiserInfo(advertiserInfo);
		cds.handleStartInfo(startInfo);

		cds.handleSimulationStatus(simulationStatus);
		
		simulationStatus = cds.getSimulationStatus("after first day");
		cds.handleQueryReport(queryReports.remove());
		cds.handleSalesReport(salesReports.remove());
		cds.handleSimulationStatus(simulationStatus);

	}
}
