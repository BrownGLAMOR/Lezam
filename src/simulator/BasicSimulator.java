package simulator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class BasicSimulator {
	
	Random _R = new Random();					//Random number generator
	
	private double _squashing;
	private double _numUsers;
	private int _numAgents;
	
	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<Query,Double> _contProb;
	private HashMap<QueryType,Double> _users;
	protected Set<Query> _querySpace;
	
	/*
	 * 
	 */
	public BasicSimulator() {
		_numUsers = 90000;
		_numAgents = 7;
		_agents = new String[_numAgents];
		_adType = new HashMap<String,HashMap<Query,Ad>>();
		_bids = new HashMap<String,HashMap<Query,Double>>();
		_budgets = new HashMap<String,HashMap<Query,Double>>();
		_totBudget = new HashMap<String,Double>();
		_advEffect = new HashMap<String,HashMap<Query,Double>>();
		_contProb = new HashMap<Query,Double>();
		_users = new HashMap<QueryType,Double>();
		_querySpace = new LinkedHashSet<Query>();
	}
	
	/*
	 * Initializes the state of the simulation
	 */
	public void initializeGameState() {
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
		}
		
		/*
		 * Initialize the User Population
		 */
		_users.put(QueryType.FOCUS_LEVEL_ZERO, _numUsers/10);
		_users.put(QueryType.FOCUS_LEVEL_ONE, _numUsers/10);
		_users.put(QueryType.FOCUS_LEVEL_TWO, _numUsers/10);
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
