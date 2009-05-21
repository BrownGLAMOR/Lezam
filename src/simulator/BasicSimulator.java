package simulator;

import java.util.HashMap;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class BasicSimulator {
	
	private String[] _agents;
	private HashMap<String,HashMap<Query,Ad>> _adType;
	private HashMap<String,HashMap<Query,Double>> _bids;
	private HashMap<String,HashMap<Query,Double>> _budgets;
	private HashMap<String,Double> _totBudget;
	private HashMap<String,HashMap<Query,Double>> _advEffect;
	private HashMap<Query,Double> _contProb;
	private double _squashing;
	private double _numUsers;
	private HashMap<QueryType,Double> _users;
	
	
	
	/*
	 * 
	 */
	public BasicSimulator() {
		
	}
	
	/*
	 * Initializes the state of the simulation
	 */
	public void initializeGameState() {
		
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
	
}
