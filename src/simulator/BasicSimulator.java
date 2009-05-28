package simulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.props.SimulationStatus;
import simulator.parser.GameLogParser;
import simulator.parser.SimParserMessage;
import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;

public class BasicSimulator {
	
	Random _R = new Random();					//Random number generator
	
	private double _squashing;
	private double _numUsers;
	private int _numAgents;
	private int _numPromSlots;
	private int _numSlots;
	private double _regReserve;
	private double _proReserve;
	private double _targEffect;
	private double _promSlotBonus;
	
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
	
	/*
	 * 
	 */
	public BasicSimulator() {
		_numUsers = 90000;
		_numAgents = 7;
		_numSlots = 5;
		_targEffect = .5;
		_promSlotBonus = .5;
		_agents = new String[_numAgents];
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
	
	public void parseGameLog(String filename) throws IOException, ParseException {
	    InputStream inputStream = new FileInputStream(filename);
    	GameLogParser parser = new GameLogParser(new LogReader(inputStream));
        parser.start();
        parser.stop();
	    LinkedList<SimParserMessage> messages = parser.getMessages();
	    for(int i = 0; i < messages.size(); i++) {
	    	SimParserMessage mes = messages.get(i);
	    	Transportable content = mes.getContent();
	    	if (content instanceof BankStatus) {
	    	}
	    	else if (content instanceof SimulationStatus) {
	    	}
	    	else if (content instanceof SlotInfo) {
	    	}
	    	else if (content instanceof ReserveInfo) {
	    	}
	    	else if (content instanceof PublisherInfo) {
	    	}
	    	else if (content instanceof SalesReport) {
	    	}
	    	else if (content instanceof QueryReport) {
	    	}
	    	else if (content instanceof RetailCatalog) {
	    	}
	    	else if (content instanceof BidBundle) {
	    	}
	    	else if (content instanceof UserClickModel) {
	    	}
	    	else if (content instanceof AdvertiserInfo) {
	    	}
	    	else {
	    		throw new RuntimeException("Unexpected parse token");
	    	}
	    }
	}
	
	public void initializeGameState(String filename, int day) throws IOException, ParseException {
	    InputStream inputStream = new FileInputStream(filename);
    	GameLogParser parser = new GameLogParser(new LogReader(inputStream));
        parser.start();
        parser.stop();
	    LinkedList<SimParserMessage> messages = parser.getMessages();
	    System.out.println(messages.size());
	}
	
	/*
	 * Initializes the state of the simulation
	 */
	public void initializeGameState() {
		_numPromSlots = 2;
		_regReserve = randDouble(.02, .05);
		_proReserve = randDouble(_regReserve,.4);
		/*
		 * Initialize QuerySpace
		 */
		_querySpace.add(new Query(null,null));
		_querySpace.add(new Query("pg",null));
		_querySpace.add(new Query("lioneer",null));
		_querySpace.add(new Query("flat",null));
		_querySpace.add(new Query(null,"tv"));
		_querySpace.add(new Query(null,"dvd"));
		_querySpace.add(new Query(null,"audio"));
		_querySpace.add(new Query("pg","tv"));
		_querySpace.add(new Query("pg","dvd"));
		_querySpace.add(new Query("pg","audio"));
		_querySpace.add(new Query("lioneer","tv"));
		_querySpace.add(new Query("lioneer","dvd"));
		_querySpace.add(new Query("lioneer","audio"));
		_querySpace.add(new Query("flat","tv"));
		_querySpace.add(new Query("flat","dvd"));
		_querySpace.add(new Query("flat","audio"));
		
		/*
		 * Initialize all the bidding information
		 */
		
		HashMap<Query,Double> bids;
		HashMap<Query,Double> budgets;
		HashMap<Query,Ad> ads;
		HashMap<Query,Double> advEffect;
		for(int i = 0; i < _numAgents; i++) {
			_agents[i] = "Agent" + i;
			bids = new HashMap<Query,Double>();
			budgets = new HashMap<Query,Double>();
			advEffect = new HashMap<Query, Double>();
			ads = new HashMap<Query,Ad>();
			for(Query query:_querySpace) {
				bids.put(query, randDouble(.5,3.5));
				budgets.put(query, bids.get(query)*50);
				ads.put(query,new Ad());
				if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					advEffect.put(query, randDouble(.2,.3));
					_contProb.put(query, randDouble(.2,.5));
				}
				else if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					advEffect.put(query, randDouble(.3,.4));
					_contProb.put(query, randDouble(.3,.6));
				}
				else if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
					advEffect.put(query, randDouble(.4,.5));
					_contProb.put(query, randDouble(.4,.7));
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
	
	
	
}
