package simulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectClickProb;
import simulator.models.PerfectConversionProb;
import simulator.models.PerfectPositionToBid;
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
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<String,String> _manfactBonus;
	private HashMap<String,String> _compBonus;
	private HashMap<String,Integer> _overCap;
	private HashMap<Query,Double> _contProb;
	private HashMap<UserState,Double> _users;
	private Set<Query> _querySpace;
	private HashMap<Query,Double> _ourAdvEffect;

	/*
	 * 
	 */
	public BasicSimulator() {
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
		_querySpace = new LinkedHashSet<Query>();
	}

	public void initializeGameState(String filename, int day, int advertiseridx) throws IOException, ParseException {
		assert day >= 2 && advertiseridx >= 0 && advertiseridx <= 7;
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
		RetailCatalog retailCatalog = status.getRetailCatalog();
		SlotInfo slotInfo = status.getSlotInfo();
		UserClickModel userClickModel = status.getUserClickModel();
    	_agents = status.getAdvertisers();
    	_compSpecialty = _compBonus.get(_agents[advertiseridx]);
    	_manSpecialty = _manfactBonus.get(_agents[advertiseridx]);
		
		AdvertiserInfo advInfo = advInfos.get(_agents[advertiseridx]);
		
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
    	
    	HashMap<Product, HashMap<UserState, Integer>> usersMap = userDists.get(day);
    	for(UserState state : UserState.values()) { 
    		_users.put(state,0.0);
    	}
		for(Product p : retailCatalog) {
			HashMap<UserState, Integer> users = usersMap.get(p);
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
    	for(Product p : retailCatalog) {
    		
    	}
    	
    	_querySpace.add(new Query(null, null));
        for(Product product : retailCatalog) {
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
    		BidBundle bidBundleTemp = bidBundles.get(_agents[i]).get(day);
    		String manfactBonus = adInfo.getManufacturerSpecialty();
    		_manfactBonus.put(_agents[i], manfactBonus);
    		String compBonus = adInfo.getComponentSpecialty();
    		_compBonus.put(_agents[i], compBonus);
    		_totBudget.put(_agents[i], bidBundleTemp.getCampaignDailySpendLimit());
    		HashMap<Query,Ad> adType = new HashMap<Query, Ad>();
    		HashMap<Query,Double> bids = new HashMap<Query, Double>();
    		HashMap<Query,Double> budgets = new HashMap<Query, Double>();
    		HashMap<Query,Double> advEffect = new HashMap<Query, Double>();

    		for(Query query : _querySpace) {
    			adType.put(query, bidBundleTemp.getAd(query));
    			bids.put(query, bidBundleTemp.getBid(query));
    			budgets.put(query,bidBundleTemp.getDailyLimit(query));
    			advEffect.put(query, userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), i));
    		}
    		_adType.put(_agents[i], adType);
    		_bids.put(_agents[i], bids);
    		_budgets.put(_agents[i], budgets);
    		_advEffect.put(_agents[i], advEffect);
    	}
    	_ourAdvEffect = _advEffect.get(_agents[advertiseridx]);
	}

	/*
	 * Generates the perfect models to pass to the bidder
	 */
	public Set<AbstractModel> generateModels() {
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		AbstractUserModel userModel = new PerfectUserModel(_numUsers,_users);
		models.add(userModel);
		for(Query query : _querySpace) {
			AbstractBidToSlotModel bidToSlotModel = new PerfectBidToPosition(_agents,_bids,_advEffect,_squashing,_ourAdvEffect.get(query),query);
			AbstractSlotToPrClick slotToClickModel = new PerfectClickProb(_agents,_bids,_advEffect,_contProb,_adType,_compBonus,_overCap,_ourAdvEffect.get(query),_squashing,_numPromSlots,_numSlots,_proReserve,_targEffect,_promSlotBonus,query);
			AbstractPrConversionModel convPrModel = new PerfectConversionProb(_ourOverCap,_CSB, _compSpecialty,query,userModel);
			AbstractSlotToBidModel slotToBidModel = new PerfectPositionToBid(_agents,_bids,_advEffect,_squashing,_ourAdvEffect.get(query),query);
			models.add(bidToSlotModel);
			models.add(slotToClickModel);
			models.add(convPrModel);
			models.add(slotToBidModel);
		}

		return models;
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

}
