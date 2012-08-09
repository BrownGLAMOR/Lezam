package models.queryanalyzer.riep.iep;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;
import java.util.PriorityQueue;

import weka.experiment.AveragingResultProducer;



//NOTE: CPLEX environment variable must be correctly set:
//setenvvar ILOG_LICENSE_FILE /com/ilm/access.ilm

public class WaterfallILP {

	//************************************ FIELDS ************************************

	// The objective function can be of any of these types.
	/**
	 * NONE, 
	 * MINIMIZE_SLOT_DIFF_PROXY, 
	 * MINIMIZE_SLOT_DIFF, 
	 * MINIMIZE_SLOT_DIFF_TIEBREAKER, 
	 * SPREAD_SAMPLES_LINEAR, 
	 * SPREAD_SAMPLES_QUADRATIC, 
	 * CLOSE_TO_IMPRESSIONS_UPPER_BOUND,
	 * MINIMIZE_SAMPLE_MU_DIFF
	 */
	public enum Objective
	{
		NONE, 
		MINIMIZE_SLOT_DIFF_PROXY, 
		MINIMIZE_SLOT_DIFF, 
		MINIMIZE_SLOT_DIFF_TIEBREAKER,
		SPREAD_SAMPLES_LINEAR, 
		SPREAD_SAMPLES_QUADRATIC, 
		CLOSE_TO_IMPRESSIONS_UPPER_BOUND,
		MINIMIZE_SAMPLE_MU_DIFF, //get resulting avgPos as close as possible to sampledMu
		MINIMIZE_IMPRESSION_PRIOR_ERROR, //get resulting I_a as close as possible to I_aDistributionMean
		MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF, 
		DEPENDS_ON_CIRCUMSTANCES,
		MAXIMIZE_IMPRESSIONS_IN_SAMPLED_BUCKETS,
		MAXIMIZE_MIN_IMPRESSIONS_IN_SAMPLED_BUCKETS,
	}

	
	boolean BUGGY_VERSION = false;
	
	//************************************ CONFIG ************************************
	private static boolean DEBUG = true;
	private Objective DESIRED_OBJECTIVE = Objective.DEPENDS_ON_CIRCUMSTANCES;
	private boolean RETURN_MULTIPLE_SOLUTIONS = false;
	private boolean LET_CPLEX_HANDLE_CONDITIONALS = false;
	private boolean USE_PROMOTED_SLOT_CONSTRAINTS = false;
	private double TIMEOUT_IN_SECONDS = 3;
	private boolean SUPPRESS_OUTPUT = true;
	private boolean SUPPRESS_OUTPUT_MODEL = true;
	private boolean USE_SAMPLING_CONSTRAINTS = false;
	private boolean USE_RANKING_CONSTRAINTS; //If this is true, we don't actually know the rankings of agents, so we'll have to add constraints. (If false, agent in element i has the ith highest rank)
	private boolean USE_BUDGET_CONSTRAINTS = false; //If someone didn't hit budget, nobody else can have more total imps than that person did.
	
	private boolean USE_NEW_SAMPLING_CONSTRAINTS = true;
	
	private boolean SAMPLES_POSSIBLE_AFTER_EVERYONE_DROPS = true;
	
	
	//************************************ FIELD DECLARATIONS ************************************
	boolean INTEGER_PROGRAM;
	boolean USE_EPSILON;
	int numAgents;
	int numSlots;
	int numPromotedSlots;
	int numBuckets;

	//Parameters that should eventually be set by the constructor
	int minImpsPerAgentPerSlot = 0;
	int maxImpsPerAgentPerSlot = 10000;
	int minImpsPerAgent = 1;
	int maxImpsPerAgent;
	int LARGE_CONSTANT_Y_a_k = 100000;
	double epsilon = .00001;
	double MIN_IMPRESSIONS_STDEV = 1; //The smallest standard deviation assumed by impressions models (Shouldn't be 0, or we can get no feasible solution if models are bad)

	//These are sorted by decreasing squashed bid rank:
	double[] knownI_a; //total impressions for each agent
	double[] knownMu_a; //average position for each agent
	double[] knownI_aPromoted; //total promoted impressions for each agent
	boolean[] isKnownPromotionEligible; //Is it known that a given agent was eligible for promotion? (i.e. did the agent bid high enough?) (When in doubt, set false. Setting true will add more constraints to problem.)
	int[] hitBudget; //1 if budget was hit, 0 if it was not, -1 if unknown.
	boolean[] isKnownI_a; //(if I_a is not known, a default -1 value is used)
	boolean[] isKnownMu_a; //(if mu is not known, a default -1 value is used)
	boolean[] isKnownI_aPromoted; //(if I_a is not known, a default -1 value is used)

	double[] knownSampledMu_a; //sampled average position
	boolean[] isKnownSampledMu_a; //(if sampledMu not known, a default -1 value is used)
	int numSamples;	//number of samples that were used to calculate sampled average position
	boolean samplingAnyAveragePositions; //If we're not doing any sampling, we can save some time by not including certain constraints.

	double[] knownI_aDistributionMean; 
	double[] knownI_aDistributionStdev;
	boolean[] isKnownI_aDistributionMean;
	
	//If minimizing impression error, this is the maximum number of standard deviations away
	//that the predicted number of agent impressions can be from the mean of the prior.
	//(Since our impression predictions won't necessarily be good, it's probably a good idea 
	// to make this a high value)
	double MAX_ERROR = 100000; 
	
	
	//If we are returning multiple solutions and then evaluating by a separate objective, these
	//parameters determine the size of the solution pool and how optimal a solution has to be to 
	//remain in the pool.
	//(Actually, for now we won't put any restrictions on optimality. We have two options here:
	//  1. Only consider solutions within X of optimal, and get N diverse solutions from this pool.
	//  2. Get the N most optimal solutions
	//Right now we are taking approach (2).
	int SOLUTION_POOL_SIZE = 100; //When returning multiple solutions, this is the maximum number that can be returned
	int POPULATE_LIM = 1000; //Number of solutions to populate (some of which may be replaced due to a smaller solution pool size)
	int SOLUTION_POOL_INTENSITY = 4; //1-4, 4 being slowest but most exhaustive
	
	
	

	   
	   
	
	//************************************ CONSTRUCTORS ************************************


	/**
	 * This constructor is for the usual case, when there are no sampled average positions.
	 * @param knownI_a
	 * @param knownMu_a
	 * @param knownI_aPromoted
	 * @param isKnownPromotionEligible
	 * @param numSlots
	 * @param numPromotedSlots
	 * @param integerProgram
	 * @param useEpsilon
	 */
	public WaterfallILP(double[] knownI_a, double[] knownMu_a, double[] knownI_aPromoted, 
			boolean[] isKnownPromotionEligible, int[] hitBudget, int numSlots, int numPromotedSlots, 
			boolean integerProgram, boolean useEpsilon, int maxImpsPerAgent,
			double[] knownI_aDistributionMean, double[] knownI_aDistributionStdev,
			boolean useRankingConstraints, boolean multipleSolutions, double timeoutInSeconds) {
		

		this.TIMEOUT_IN_SECONDS = timeoutInSeconds;
		this.RETURN_MULTIPLE_SOLUTIONS = multipleSolutions;
		this.USE_RANKING_CONSTRAINTS = useRankingConstraints;
		this.INTEGER_PROGRAM = integerProgram;
		this.USE_EPSILON = useEpsilon;
		this.knownI_a = knownI_a;
		this.knownMu_a = knownMu_a;
		this.knownI_aPromoted = knownI_aPromoted;
		this.isKnownPromotionEligible = isKnownPromotionEligible;
		this.hitBudget = hitBudget;
		numAgents = knownI_a.length;
		this.numBuckets = SAMPLES_POSSIBLE_AFTER_EVERYONE_DROPS ? (numAgents+1) : numAgents;
		this.numSlots = numSlots;
		this.numPromotedSlots = numPromotedSlots;
		this.maxImpsPerAgent = maxImpsPerAgent; //upper bound on agent imps
		this.knownI_aDistributionMean = knownI_aDistributionMean;
		this.knownI_aDistributionStdev = knownI_aDistributionStdev;
		
		//By default, sampled average positions are not given.
		this.samplingAnyAveragePositions = false; 
		this.knownSampledMu_a = new double[numAgents];
		Arrays.fill(this.knownSampledMu_a, -1);

		isKnownI_a = new boolean[numAgents];
		isKnownMu_a = new boolean[numAgents];
		isKnownI_aPromoted = new boolean[numAgents];
		isKnownSampledMu_a = new boolean[numAgents];
		isKnownI_aDistributionMean = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			if(knownI_a[a] != -1) isKnownI_a[a] = true;
			if(knownMu_a[a] != -1) isKnownMu_a[a] = true;
			if(knownI_aPromoted[a] != -1) isKnownI_aPromoted[a] = true;
			if(knownSampledMu_a[a] != -1) isKnownSampledMu_a[a] = true;
			if(knownI_aDistributionMean[a] != -1) isKnownI_aDistributionMean[a] = true;
			
			//Make sure the minimum I_a stdev is 1. (We don't want to ever assume we know exactly what the opponent imps will be).
			if (knownI_aDistributionStdev[a] != -1 && knownI_aDistributionStdev[a]< MIN_IMPRESSIONS_STDEV) knownI_aDistributionStdev[a] = MIN_IMPRESSIONS_STDEV;
		}
		
		
		//-------------------
		//(sampled problem if not all avgPositions are known exactly)
		//usingPriors, sampledProblem = (f,f): minimizeSlotDiff
		//usingPriors, sampledProblem = (f,t): spread_samples_linear (and USE_SAMPLING_CONSTRAINTS=true)
		//usingPriors, sampledProblem = (t,f): minimize_impression_prior_error
		//usingPriors, sampledProblem = (t,t): ??? (spread_samples_linear) : should be a combo, though
		//If we don't know any priors, don't use an objective that depends on it
		if (DESIRED_OBJECTIVE == Objective.DEPENDS_ON_CIRCUMSTANCES) {
			boolean usingPriors = false;
			boolean sampledProblem = false;
			for (int a=0; a<numAgents; a++) {
				if (isKnownI_aDistributionMean[a]) usingPriors = true;
				if (!isKnownMu_a[a]) sampledProblem = true; 
			}
		
			//Choose appropriate objective
			if (!usingPriors && !sampledProblem) {
				DESIRED_OBJECTIVE = Objective.MINIMIZE_SLOT_DIFF;
			} else if (!usingPriors && sampledProblem) {
				DESIRED_OBJECTIVE = Objective.MINIMIZE_SAMPLE_MU_DIFF;
				USE_SAMPLING_CONSTRAINTS = false;
				USE_NEW_SAMPLING_CONSTRAINTS = false;
				//INTEGER_PROGRAM = true;
				
//				DESIRED_OBJECTIVE = Objective.MAXIMIZE_MIN_IMPRESSIONS_IN_SAMPLED_BUCKETS;
//				USE_SAMPLING_CONSTRAINTS = true;
//				USE_NEW_SAMPLING_CONSTRAINTS = true;
//				INTEGER_PROGRAM = false;
			} else if (usingPriors && !sampledProblem) {
				DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR;
			} else { //usingPriors && sampledProblem
				DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF;
				//DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR;
				//DESIRED_OBJECTIVE = Objective.MINIMIZE_SAMPLE_MU_DIFF;
			}
			System.out.println("usingPriors: " + usingPriors + ", sampledProblem: " + sampledProblem + ", objective: " + DESIRED_OBJECTIVE);
		}
		//--------------------
		
	}

	
	public static String replaceBrackets(String str) {
		return str.replace('[', '{').replace(']', '}');
	}

	/**
	 * This constructor is for the case when average positions have been sampled.
	 * @param knownI_a
	 * @param knownMu_a
	 * @param knownI_aPromoted
	 * @param isKnownPromotionEligible
	 * @param numSlots
	 * @param numPromotedSlots
	 * @param integerProgram
	 * @param useEpsilon
	 * @param knownSampledMu_a
	 * @param numSamples
	 */
	public WaterfallILP(double[] knownI_a, double[] knownMu_a, double[] knownI_aPromoted, 
			boolean[] isKnownPromotionEligible, int[] hitBudget, int numSlots, int numPromotedSlots, 
			boolean integerProgram, boolean useEpsilon, 
			double[] knownSampledMu_a, int numSamples, int maxImpsPerAgent,
			double[] knownI_aDistributionMean, double[] knownI_aDistributionStdev,
			boolean useRankingConstraints, boolean multipleSolutions, double timeoutInSeconds) {
		this(knownI_a, knownMu_a, knownI_aPromoted, isKnownPromotionEligible, hitBudget,
				numSlots, numPromotedSlots, integerProgram, useEpsilon, maxImpsPerAgent,
				knownI_aDistributionMean, knownI_aDistributionStdev, useRankingConstraints, 
				multipleSolutions, timeoutInSeconds);
		

		//DEBUG
		System.out.println("Creating IP");
		System.out.println("double[] knownI_a = " + replaceBrackets(Arrays.toString(knownI_a)) + ";");
		System.out.println("double[] knownMu_a = " + replaceBrackets(Arrays.toString(knownMu_a)) + ";");
		System.out.println("double[] knownI_aPromoted= " + replaceBrackets(Arrays.toString(knownI_aPromoted)) + ";");
		System.out.println("boolean[] isKnownPromotionEligible = " + replaceBrackets(Arrays.toString(isKnownPromotionEligible)) + ";");
		System.out.println("int[] hitBudget = " + replaceBrackets(Arrays.toString(hitBudget)) + ";");
		System.out.println("int numSlots = " + numSlots + ";");
		System.out.println("int numPromotedSlots = " + numPromotedSlots + ";");
		System.out.println("boolean integerProgram = " + integerProgram + ";");
		System.out.println("boolean useEpsilon = " + useEpsilon + ";");
		System.out.println("double[] knownSampledMu_a = " + replaceBrackets(Arrays.toString(knownSampledMu_a)) + ";");
		System.out.println("int numSamples = " + numSamples + ";");
		System.out.println("int maxImpsPerAgent = " + maxImpsPerAgent + ";");
		System.out.println("double[] knownI_aDistributionMean = " + replaceBrackets(Arrays.toString(knownI_aDistributionMean)) + ";");
		System.out.println("double[] knownI_aDistributionStdev = " + replaceBrackets(Arrays.toString(knownI_aDistributionStdev)) + ";");
		System.out.println("boolean useRankingConstraints = " + useRankingConstraints + ";");
		System.out.println("boolean multipleSolutions = " + multipleSolutions + ";");		
		//END DEBUG
		
		
		
		
		this.knownSampledMu_a = knownSampledMu_a;
		this.isKnownSampledMu_a = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			//change NaN values to the agent's position (assumes their average position was roughly their starting position
			if(Double.isNaN( knownSampledMu_a[a] )) {
				knownSampledMu_a[a] = Math.min(numSlots, a+1);
				if (BUGGY_VERSION) knownSampledMu_a[a] = 0;
			}
			
			
			if(knownSampledMu_a[a] != -1) {
				isKnownSampledMu_a[a] = true; //mark whether it's known
				samplingAnyAveragePositions = true; //If any sampled avg positions are known, this is true.
			}
		}
		this.numSamples = numSamples;
		
		
	}




	//************************************ MAIN SOLVER METHOD ************************************

	/**
	 * 
	 * @param I_a, number of total impressions seen by each agent
	 * @return I_a_s, number of impressions seen by each agent in each slot
	 */
	public WaterfallResult solve() {

		StringBuffer sb1 = new StringBuffer();
//		sb1.append("Solving new Waterfall instance.\n");
		sb1.append("  I_a="+Arrays.toString(knownI_a) + ", Mu_a="+Arrays.toString(knownMu_a) + ", I_aPromoted="+Arrays.toString(knownI_aPromoted) + ", ");
		sb1.append("promOK="+Arrays.toString(isKnownPromotionEligible) + ", hitBudget=" + Arrays.toString(hitBudget) + ", ");
		sb1.append("numAgents=" + numAgents + ", numSlots=" + numSlots + ", numPromotedSlots=" + numPromotedSlots + ", ");
		sb1.append("sampledMu_a="+Arrays.toString(knownSampledMu_a) +", numSamples=" + numSamples + ", ");
		sb1.append("I_aDistMean="+Arrays.toString(knownI_aDistributionMean) +", I_aDistStdev=" + Arrays.toString(knownI_aDistributionStdev) + ", ");
		sb1.append("maxImpsPerAgent=" + maxImpsPerAgent);
		debug(sb1.toString());

		WaterfallResult result = null; //The solution that will ultimately be returned.


		try {
			IloCplex cplex = new IloCplex();

			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			if (SUPPRESS_OUTPUT) cplex.setOut(null);
			cplex.setParam(IloCplex.DoubleParam.TiLim, TIMEOUT_IN_SECONDS);
			if (RETURN_MULTIPLE_SOLUTIONS) {
			      //Setup solution pool to enumerate all solutions
			     //cplex.setParam(IloCplex.DoubleParam.SolnPoolAGap, 100); //Set the absolute gap in objective value for solutions in pool
			      cplex.setParam(IloCplex.IntParam.SolnPoolIntensity, SOLUTION_POOL_INTENSITY); //This will enumerate all solutions
			      cplex.setParam(IloCplex.IntParam.SolnPoolReplace, 1); //How to replace when pool capacity is reached. 1=Keep best sols, 2=diversify sols, 0=fifo
			      cplex.setParam(IloCplex.IntParam.SolnPoolCapacity, SOLUTION_POOL_SIZE);  //Set size of solution pool
			      cplex.setParam(IloCplex.IntParam.PopulateLim, POPULATE_LIM); //Set number of solutions to get in each populate step
			     // cplex.setParam(IloCplex.DoubleParam.WorkMem, 1000); //Give it 1GB of RAM
								
				
				//cplex.setParam(IntParam.SolnPoolIntensity, 4);
				//cplex.setParam(IntParam.SolnPoolReplace, 2);
				//cplex.setParam(IntParam.SolnPoolCapacity, maxSols);
			}


			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = createImpressionsPerAgentPerSlotVariables(cplex); //(#imps per agent/slot)
			IloIntVar[][] Y_a_k = createConditionalVariables1(cplex);
			IloNumVar[] I_a = createImpressionsPerAgentVariables(cplex);

			IloIntVar[][] r_a_agent = null;
			if(USE_RANKING_CONSTRAINTS) {
				r_a_agent = createRankingVariables(cplex);
			}
				
			//Create additional decision variables if we're sampling any average positions.
			IloNumVar[] U_k = null;
			IloIntVar[][] d_a_k = null;
			IloIntVar[][] h_a_k = null;
			IloIntVar[][] p_a_k = null;
			IloNumVar[] Istart_a = null;
			IloNumVar[] Iend_a = null;
			
			//New sampling constraint variables
			IloIntVar[][] V_i_k = null;
			IloNumVar[] z_i = null;
			IloNumVar[] U_i = null;
			IloIntVar[][] d_a_i = null;
			IloIntVar[][] h_a_i = null;
			IloNumVar[] z_k = null;
			
			
			if (USE_SAMPLING_CONSTRAINTS && !USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				U_k = createImpressionSamplingVariables(cplex); //which impression did sample k happen on?
				d_a_k = createDroppedOutVariables(cplex); //did agent a drop out of sample k?
				h_a_k = createHowDroppedOutVariables(cplex); //is the sample before or after the agent's impression interval?
				p_a_k = createPositionVariables(cplex); //position of agent a at sample k?
				Istart_a = createStartingImpressionVariables(cplex); //when did agent a see its first impression?
				Iend_a = createEndingImpressionVariables(cplex); //when did agent a see its last impression?
			}

			
			if (USE_SAMPLING_CONSTRAINTS && USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				V_i_k = createNewImpressionSamplingVariables(cplex);
				z_i = createImpressionsInBucketVariables(cplex);
				U_i = createLastImpressionInBucketVariables(cplex);
				Istart_a = createStartingImpressionVariables(cplex); //when did agent a see its first impression?
				Iend_a = createEndingImpressionVariables(cplex); //when did agent a see its last impression?
				d_a_i = createDroppedOutOfBucketVariables(cplex);
				h_a_i = createHowDroppedOutOfBucketVariables(cplex); //is the sample before or after the agent's impression interval?
				d_a_k = createDroppedOutVariables(cplex); //did agent a drop out of sample k?
				p_a_k = createPositionVariables(cplex); //position of agent a at sample k?
				z_k = createImpressionsInSampledBucketVariables(cplex);
			}
			
			
			
			
			
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			if (DESIRED_OBJECTIVE == Objective.NONE) {
				addObjective_indifferentBetweenFeasibleSolutions(cplex, I_a_s);				
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF_PROXY) {
				addObjective_minimizeSlotImpressionDifferenceProxy(cplex, I_a_s);
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF || DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF_TIEBREAKER) {
				addObjective_minimizeSlotImpressionDifference(cplex, I_a_s);
			} else if (DESIRED_OBJECTIVE == Objective.CLOSE_TO_IMPRESSIONS_UPPER_BOUND) {
				addObjective_closeToImpressionsUpperBound(cplex, I_a_s);
			} else if (DESIRED_OBJECTIVE == Objective.SPREAD_SAMPLES_LINEAR) {
				addObjective_spreadSamplesLinear(cplex, U_k, I_a_s);
			} else if (DESIRED_OBJECTIVE == Objective.SPREAD_SAMPLES_QUADRATIC) {
				addObjective_spreadSamplesQuadratic(cplex, U_k, I_a_s);
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SAMPLE_MU_DIFF) {
				addObjective_minimizeDistanceFromSampledMu(cplex, I_a_s, I_a);
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR) {
				if (!USE_RANKING_CONSTRAINTS) addObjective_minimizeImpressionPriorError(cplex, I_a);
				else addObjective_minimizeImpressionPriorErrorRankingUnknown(cplex, I_a, r_a_agent);
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF) {
				if (!USE_RANKING_CONSTRAINTS) {
					addObjective_minimizeImpressionPriorErrorAndDistanceFromSampledMu(cplex, I_a_s, I_a);
				}
				else {
					System.err.println("We don't have an objective for this. Switching to a default case.");
					addObjective_minimizeImpressionPriorErrorRankingUnknown(cplex, I_a, r_a_agent);
				}
			} else if (DESIRED_OBJECTIVE == Objective.MAXIMIZE_IMPRESSIONS_IN_SAMPLED_BUCKETS) {
				addObjective_maximizeImpressionsInSampledBuckets(cplex, z_k);
			} else if (DESIRED_OBJECTIVE == Objective.MAXIMIZE_MIN_IMPRESSIONS_IN_SAMPLED_BUCKETS) {
				addObjective_maximizeMinImpressionsInSampledBuckets(cplex, z_k);
			}


			//----------------------------- ADD CONSTRAINTS --------------------------------------------------

			// Waterfall ordering must be satisfied.
			if (LET_CPLEX_HANDLE_CONDITIONALS) {
				addConstraint_waterfallOrderingAutomaticConditionals(cplex, I_a_s);
			} else {
				addConstraint_waterfallOrdering(cplex, I_a_s);
				addConstraint_waterfallOrderingTight(cplex, I_a_s, Y_a_k);
			}

			// An agent's total impressions must sum to impressions per slot.
			addConstraint_totalImpressionsDefinition(cplex, I_a_s, I_a);

			
			// If we know any agents' total impressions (such as our own), these must be satisfied.
			if(!USE_RANKING_CONSTRAINTS) addConstraint_totalImpressionsKnown(cplex, I_a);
			else addConstraint_totalImpressionsKnownRankingUnknown(cplex, I_a, r_a_agent);
			
			// If we know any agents' total promoted impressions, these must be satisfied.
			if (USE_PROMOTED_SLOT_CONSTRAINTS) {
				if(!USE_RANKING_CONSTRAINTS) addConstraint_totalPromotedImpressionsKnown(cplex, I_a_s);
				else addConstraint_totalPromotedImpressionsKnownRankingUnknown(cplex, I_a_s, r_a_agent);
			}

			// If we know that some agent didn't hit their budget, then nobody could have seen more total impressions that that agent
			// (where "total impressions" includes out-of-slot impressions)
			if(USE_BUDGET_CONSTRAINTS) addConstraint_didntHitBudget(cplex, I_a_s);
			
			// Agent impressions per slot must result in the known (exact) average positions.
			if(!USE_RANKING_CONSTRAINTS) addConstraint_exactAveragePositionsKnown(cplex, I_a_s, I_a);
			else addConstraint_exactAveragePositionsKnownRankingUnknown(cplex, I_a_s, I_a, r_a_agent);

			
			//---Additional ranking constraints--
			if(USE_RANKING_CONSTRAINTS) {
				addConstraint_oneRankingPerAgent(cplex, r_a_agent);
				addConstraint_oneAgentPerRanking(cplex, r_a_agent);
			}

			//---Sampling constraints---
			//Note that there are some objectives that don't require these constraints, even if sampledMu are present
			if (USE_SAMPLING_CONSTRAINTS && !USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				//Define starting impression for each advertiser
				addConstraint_startingImpressionDefinition(cplex, I_a_s, Istart_a);

				//Define ending impression for each advertiser
				addConstraint_endingImpressionDefinition(cplex, I_a_s, Iend_a);

				//Define what it means to drop out (for agents that started in a slot)
				addConstraint_droppedOutDefinitionInitialAgents(cplex, d_a_k, U_k, Iend_a);

				//Define what it means to drop out (for agents that didn't start in a slot)
				addConstraint_droppedOutDefinitionLaterAgents(cplex, d_a_k, h_a_k, U_k, Istart_a, Iend_a);

				//Define each agent's position for each sample.
				addConstraint_samplePositionDefinition(cplex, p_a_k, d_a_k);

				//Agent impressions per slot must result in the known (sampled) average positions.
				addConstraint_sampledAveragePositionsKnown(cplex, p_a_k, d_a_k);

				//Samples have to be ordered by impression number (lower k = lower U_k)
				addConstraint_orderedSamples(cplex, U_k);

				//The highest sampled impression can't be higher than the total number of impressions seen.
				//(TODO: Is this true? Or can samples occur even if nobody is in the auction?)
				addConstraint_maxSample(cplex, U_k, I_a_s);

				//If the sample mu has been given for an agent, not all the samples can be dropped
				addConstraint_notAllSamplesDropped(cplex, d_a_k);
			}

			
			//---NEW Sampling constraints---
			if (USE_SAMPLING_CONSTRAINTS && USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				//Define number of impressions in each bucket
				addConstraint_impressionsInBucket(cplex, I_a_s, z_i);
				
				//Define last impression that occurred in each bucket
				addConstraint_lastImpressionInBucket(cplex, z_i, U_i);
				
				//Define starting impression for each advertiser
				addConstraint_startingImpressionDefinition(cplex, I_a_s, Istart_a);

				//Define ending impression for each advertiser
				addConstraint_endingImpressionDefinition(cplex, I_a_s, Iend_a);

				//Define what it means to drop out of a bucket (for agents that started in a slot)
				addConstraint_droppedOutOfBucketDefinitionInitialAgents(cplex, d_a_i, U_i, Iend_a);

				//Define what it means to drop out of a bucket (for agents that didn't start in a slot)
				addConstraint_droppedOutOfBucketDefinitionLaterAgents(cplex, d_a_i, h_a_i, U_i, Istart_a, Iend_a);

				//Define what it means to drop out of a sample
				addConstraint_droppedOutOfSampleDefinition(cplex, d_a_k, d_a_i, V_i_k);
				
				//Define each agent's position for each sample.
				addConstraint_samplePositionDefinition(cplex, p_a_k, d_a_k);

				//Agent impressions per slot must result in the known (sampled) average positions.
				addConstraint_sampledAveragePositionsKnown(cplex, p_a_k, d_a_k);

				//Each sample can only occur in a single bucket
				addConstraint_oneBucketPerSample(cplex, V_i_k);
				
				//There cannot be more samples in a bucket than there are impressions in that bucket
				addConstraint_noMoreSamplesThanImpressionsInBucket(cplex, V_i_k, z_i);
				
				//Samples have to be ordered by bucket (sample k occurs in bucket i --> sample k+1 occurs in bucket >= i)
				addConstraint_orderedSamplesByBucket(cplex, V_i_k);

				//Determine number of impressions in sampled buckets
				addConstraint_impressionsInSampledBuckets(cplex, V_i_k, z_k, z_i);
				
				//If the sample mu has been given for an agent, not all the samples can be dropped
				addConstraint_notAllSamplesDropped(cplex, d_a_k);
				
			}
			

			//----------------------------- PRINT AND SOLVE MODEL --------------------------------------------------

			/**
			 * Print and solve model
			 */
			System.out.println("WaterfallILP Objective: " + DESIRED_OBJECTIVE);
//			System.out.println("Constraints added.");
			if (!SUPPRESS_OUTPUT_MODEL) System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
			
			
			//if (!RETURN_MULTIPLE_SOLUTIONS) result = getSingleSolutionResult(cplex, I_a_s, I_a, U_k, V_i_k, r_a_agent);
			if (!RETURN_MULTIPLE_SOLUTIONS) result = getSingleSolutionResult(cplex, I_a_s, I_a, U_k, V_i_k, r_a_agent, z_k, z_i, d_a_k, d_a_i, p_a_k, Istart_a, Iend_a, U_i);
			else result = getMultiSolutionResult(cplex, I_a_s, I_a, U_k, V_i_k, r_a_agent);

			
			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}

		return result;
	}














	private WaterfallResult getMultiSolutionResult(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a, IloNumVar[] U_k, IloIntVar[][] V_i_k,
			IloIntVar[][] r_a_agent) throws IloException {

		//The current solution will be kept here
//		double objectiveVal = 0;
//		double[][] I_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
//		double[] U_kDouble = new double[numSamples];
		
		//The best solution will be kept here
		double bestObjectiveVal = Double.POSITIVE_INFINITY;
		double[][] bestI_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
		double[] bestU_kDouble = new double[numSamples];
		int[][] bestV_i_kInt = new int[numBuckets][numSamples];
		
		//Call Populate() to get as many solutions as possible.
		int lastNumSols = 0;
		while(cplex.getSolnPoolNsolns() < SOLUTION_POOL_SIZE) {
			cplex.populate();
			if(lastNumSols == cplex.getSolnPoolNsolns()) {
				break;
			}
			lastNumSols = cplex.getSolnPoolNsolns();
		}

		//TODO: First order solutions by approximated objective value
		//Then, instead of computing objective value for EVERY solution in the pool,
		//Skip solutions that are nearly identical (only looking at the dupe with the highest approximated objective).
		//[For now, we'll just compute the true objective value for every solution in the pool.]
		
		//For debugging
		PriorityQueue<WaterfallResult> allResults = new PriorityQueue<WaterfallResult>();
		
		//Compute true objective value of each solution
		for (int i=0; i<cplex.getSolnPoolNsolns(); i++) {			
			double[][] I_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
			double[] U_kDouble = new double[numSamples];
			int[][] V_i_kInt = new int[numBuckets][numSamples];
			
			//Get I_a_s values for this solution
			for (int a=0; a<numAgents; a++) {
				for (int s=0; s<=a; s++) {
					I_a_sDouble[a][s] = cplex.getValue(I_a_s[a][s],i);
				}
			}
			
			//Compute true objective value of this solution
			//FIXME: For unranked problem, we need to sort these I_a_s values first
			double trueObjective = getTrueObjectiveValue(I_a_sDouble);
			
			//If this is the best solution so far, save it.
			//NOTE: "Best" means smallest objective
			//FIXME: Make sure all our objective functions adhere to this. (They should all be minimization problems)
			if (trueObjective < bestObjectiveVal) {
				bestObjectiveVal = trueObjective;
				bestI_a_sDouble = I_a_sDouble;
				bestU_kDouble = U_kDouble;
				bestV_i_kInt = V_i_kInt;
			}
			
			double objectiveVal = cplex.getObjValue(i);
			allResults.add(new WaterfallResult(objectiveVal, I_a_sDouble, U_kDouble, V_i_kInt, trueObjective));
			//System.out.println(i + ": I_a_s=" + arrayString(I_a_sDouble) + ", value=" + objectiveVal + ", trueValue=" + trueObjective);
		}
		
		
		//Output all solutions in order
		while(!allResults.isEmpty()) {
			System.out.println(allResults.poll());
		}
		//TODO: Output #sols with better approximateVals than the trueBestSol
		//TODO: Output #sols with better trueVals than the approximateBestSol
		//TODO: Output approximateBestSolsTrueSol/trueBestSolsTrueSol //(how far off optimal would we have been?)
		//TODO: Output trueBestSolsApproxSol/approximateBestSolsApproxSol //(how far away was the true opt from also being the approximate opt?)
		
		
//		//Get a set of solutions to evaluate (potentially pruning near-duplicates)
//		double[][][] solution = new double[cplex.getSolnPoolNsolns()][][];
//		for(int i = 0; i < solution.length; i++) {
//			solution[i] = new double[numAgents][numAgents];
//		}
//		for(int i = 0; i < cplex.getSolnPoolNsolns(); i++) {
//			System.out.println("Solution " + i + " value  = " + cplex.getObjValue(i));
//			//Create double array to return
//			for (int a=0; a<numAgents; a++) {
//				for (int s=0; s<=a; s++) {
//					solution[i][a][s] = cplex.getValue(I_a_s[a][s],i);
//				}
//			}
//		}
//		System.out.println("HERE ARE ALL SOLUTIONS:\n" + arrayString(solution));
//		//----------------------
		
		return new WaterfallResult(bestObjectiveVal, bestI_a_sDouble, bestU_kDouble, bestV_i_kInt);
	}


	/**
	 * Get true objective value, based on how close impressions for each advertiser came to their priors.
	 * ASSUMING I_a_s is ordered in the same way as impression models!
	 * @param I_a_s
	 * @return
	 */
	private double getTrueObjectiveValue(double[][] I_a_s) {
		//Get total impressions for each agent
		int numAgents = I_a_s.length;
		double[] I_a = new double[numAgents];
		for (int a=0; a<numAgents; a++) {
			for (int s=0; s<I_a_s[a].length; s++) {
				I_a[a] += I_a_s[a][s];
			}
		}

		//Get order (for now we'll assume the proper order is given)
		//FIXME: Can't assume this in unranked problem
		int[] order = new int[numAgents];
		for (int a=0; a<numAgents; a++) order[a] = a;
		
		int numSlotsFilled = numAgents; //TODO: Get rid of this altogether.
		
		double obj = getImpressionModelObjective(I_a, order, numSlotsFilled);
		
		//System.out.println("I_a=" + Arrays.toString(I_a) + ", obj=" + obj);
		return obj;
	}



	private double getImpressionModelObjective(double[] agentImpr, int[] order, int numSlotsFilled) {
		boolean LOG_GAUSSIAN_PDF = true;
		
		double obj;
		if (LOG_GAUSSIAN_PDF) {
			obj = 0.0;
		} else {
			obj = 1.0;
		}
		numSlotsFilled = Math.min(numSlotsFilled, agentImpr.length);
		for (int i = 0; i < numSlotsFilled; i++) {
			int currAgent = order[i];
			double mean = knownI_aDistributionMean[currAgent];
			double stdDev = knownI_aDistributionStdev[currAgent];
			if (mean != -1 && stdDev != -1) {
				double imps = agentImpr[currAgent];
				double prob;
				if (LOG_GAUSSIAN_PDF) {
					prob = logGaussianPDF(imps, mean, stdDev);
					obj += prob;
				} else {
					prob = gaussianPDF(imps, mean, stdDev);
					obj *= prob;
				}
				//System.out.println("currAgent=" + currAgent + ", gaussianPDF(imps=" + imps +", mean="+ mean + ", stdDev=" + stdDev + ") = " + prob);
			}
		}

//		System.out.println(obj);

		if (LOG_GAUSSIAN_PDF) {
			return obj;
		} else {
			if (obj == 1.0) {
				//This means we didn't place anyone with a prediction yet
				//so return a bad objective
				return 1.0;
			} else {
				return (1.0 - obj);
			}
		}
	}

	private double logGaussianPDF(double x, double mean, double sigma) {
		double diff = x - mean;
		double sigma2 = sigma * sigma;
//		return (.5 * Math.log(2.0 * Math.PI * sigma2) + (diff * diff) / (2.0 * sigma2));
		return (diff * diff) / (2.0 * sigma2);
	}

	private double gaussianPDF(double x, double mean, double sigma) {
		double diff = x - mean;
		double sigma2 = sigma * sigma;
		return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
	}


	
	
	
	
	
	
	
	
	
	
	
	
//	private WaterfallResult getSingleSolutionResult(IloCplex cplex, 
//			IloNumVar[][] I_a_s, IloNumVar[] I_a, IloNumVar[] U_k, 
//			IloIntVar[][] V_i_k, IloIntVar[][] r_a_agent) throws UnknownObjectException, IloException {
	private WaterfallResult getSingleSolutionResult(IloCplex cplex, 
			IloNumVar[][] I_a_s, IloNumVar[] I_a, IloNumVar[] U_k, 
			IloIntVar[][] V_i_k, IloIntVar[][] r_a_agent,
			IloNumVar[] z_k, IloNumVar[] z_i, IloIntVar[][] d_a_k, IloIntVar[][] d_a_i, 
			IloIntVar[][] p_a_k, IloNumVar[] Istart_a, IloNumVar[] Iend_a, IloNumVar[] U_i) throws UnknownObjectException, IloException {
		double objectiveVal = Double.POSITIVE_INFINITY; //Initially give an arbitrarily high (i.e. bad) objective value
		double[][] I_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
		double[] U_kDouble = new double[numSamples];
		int[][] V_i_kInt = new int[numBuckets][numSamples];
		int[][] r_a_agentInt = new int[numAgents][numAgents];
		
		if ( cplex.solve() ) {
			cplex.output().println("Solution status = " + cplex.getStatus());
			cplex.output().println("Solution value = " + cplex.getObjValue());
			cplex.output().println("Objective function = " + cplex.getObjective());

			objectiveVal = cplex.getObjValue();
			
			//Create double array to return
			for (int a=0; a<numAgents; a++) {
				for (int s=0; s<=a; s++) {
					I_a_sDouble[a][s] = cplex.getValue(I_a_s[a][s]);
				}
			}
			
			
//			//DEBUG-------------
//			double[] z_kDouble = new double[numSamples];
//			double[] z_iDouble = new double[numBuckets];
//			double[][] d_a_kInt = new double[numAgents][numSamples];
//			double[][] d_a_iInt = new double[numAgents][numBuckets];
//			double[][] p_a_kInt = new double[numAgents][numSamples];
//			double[] Istart_aDouble = new double[numAgents];
//			double[] Iend_aDouble = new double[numAgents];
//			double[] U_iDouble = new double[numBuckets];
//			
//			z_kDouble = cplex.getValues(z_k);
//			z_iDouble = cplex.getValues(z_i);
//			for (int a=0; a<numAgents; a++) {
//				d_a_kInt[a] = cplex.getValues(d_a_k[a]);
//				d_a_iInt[a] = cplex.getValues(d_a_i[a]);
//				p_a_kInt[a] = cplex.getValues(p_a_k[a]);
//			}
//			Istart_aDouble = cplex.getValues(Istart_a);
//			Iend_aDouble = cplex.getValues(Iend_a);
//			U_iDouble = cplex.getValues(U_i);
//			
//			System.out.println("z_k = " + Arrays.toString(z_kDouble));
//			System.out.println("z_i = " + Arrays.toString(z_iDouble));
//			System.out.println("d_a_k = " + arrayString(d_a_kInt));
//			System.out.println("d_a_i = " + arrayString(d_a_iInt));
//			System.out.println("p_a_k = " + arrayString(p_a_kInt));
//			System.out.println("Istart_a = " + Arrays.toString(Istart_aDouble));
//			System.out.println("Iend_a = " + Arrays.toString(Iend_aDouble));
//			System.out.println("U_i = " + Arrays.toString(U_iDouble));
//			//END DEBUG---------
			
			
			

			if (USE_SAMPLING_CONSTRAINTS && !USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				for (int k=0; k<numSamples; k++) {
					U_kDouble[k] = cplex.getValue(U_k[k]);
				}
			}
			
			if (USE_SAMPLING_CONSTRAINTS && USE_NEW_SAMPLING_CONSTRAINTS && samplingAnyAveragePositions) {
				for (int i=0; i<numBuckets; i++) {
					for (int k=0; k<numSamples; k++) {
						double val = cplex.getValue(V_i_k[i][k]);
						V_i_kInt[i][k] = (int) Math.round(val);
					}
				}
			}
			
			
			
			
			if (USE_RANKING_CONSTRAINTS) {
				//Re-arrange I_a_s double to be in the proper order
				for (int a=0; a<numAgents; a++) {
					for (int agent=0; agent<numAgents; agent++) {
						double rVal = cplex.getValue(r_a_agent[a][agent]);
						r_a_agentInt[a][agent] = (int) Math.round(rVal);
					}
				}
								
				double[][] I_a_sDoubleReordered = new double[numAgents][];
				for (int agent=0; agent<numAgents; agent++) {
					for (int a=0; a<numAgents; a++) {
						//If this agent was in initial position a
						if (r_a_agentInt[a][agent] == 1) {
							I_a_sDoubleReordered[agent] = I_a_sDouble[a];
						}
					}
				}
				I_a_sDouble = I_a_sDoubleReordered;
			}

			
//			System.out.println("BEST SOLUTION:\n" + arrayString(I_a_sDouble));

			//Get total imps per agent
			StringBuffer sb = new StringBuffer();
			sb.append("Total imps per agent: ");
			for (int a=0; a<numAgents;a++) {
				sb.append(cplex.getValue(I_a[a]) + " ");
			}
//			System.out.println(sb);

		} else {
			System.out.println("Solve returned false");
			System.out.println("status: " + cplex.getStatus());
		}
		
		if (USE_RANKING_CONSTRAINTS) return new WaterfallResult(objectiveVal, I_a_sDouble, U_kDouble, V_i_kInt, r_a_agentInt);
		else return new WaterfallResult(objectiveVal, I_a_sDouble, U_kDouble, V_i_kInt);
		
	}


	//************************************ CREATE DECISION VARIABLES ************************************


	public IloNumVar[][] createImpressionsPerAgentPerSlotVariables(IloCplex cplex) throws IloException {
		IloNumVar[][] I_a_s = new IloNumVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			if (INTEGER_PROGRAM) {
				//Agents have a variable for every possible slot they could have been in.
				//(They don't have variables for slots that were below where they started).
				I_a_s[a] = cplex.intVarArray(a+1, minImpsPerAgentPerSlot, maxImpsPerAgentPerSlot); 
			} else {
				I_a_s[a] = cplex.numVarArray(a+1, minImpsPerAgentPerSlot, maxImpsPerAgentPerSlot);
			}
		}
		return I_a_s;
	}

	public IloIntVar[][] createConditionalVariables1(IloCplex cplex) throws IloException {
		IloIntVar[][] Y_a_k = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			Y_a_k[a] = cplex.intVarArray(a+1, 0, 1); //TODO: how many do we want for each agent?
		}
		return Y_a_k;
	}

	public IloNumVar[] createImpressionsPerAgentVariables(IloCplex cplex) throws IloException {
		IloNumVar[] I_a;
		if (INTEGER_PROGRAM) {
			I_a = cplex.intVarArray(numAgents, minImpsPerAgent, maxImpsPerAgent); //TODO: (numAgents should be numSlots? a+1?)
		} else {
			I_a = cplex.numVarArray(numAgents, minImpsPerAgent, maxImpsPerAgent); //TODO: (numAgents should be numSlots? a+1?)				
		}	
		return I_a;
	}

	
	private IloIntVar[][] createRankingVariables(IloCplex cplex) throws IloException {
		IloIntVar[][] r_a_agent = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			r_a_agent[a] = cplex.intVarArray(numAgents, 0, 1); 
		}
		return r_a_agent;
	}

	
	private IloNumVar[] createImpressionSamplingVariables(IloCplex cplex) throws IloException {
		IloNumVar[] U_k;
		if (INTEGER_PROGRAM) {
			//TODO: If U_k can be beyond the number of viewed impressions (e.g. after all agents
			//have dropped out), we may want to change this upper bound.
			U_k = cplex.intVarArray(numSamples, 1, maxImpsPerAgentPerSlot);
		} else {
			U_k = cplex.numVarArray(numSamples, 1, maxImpsPerAgentPerSlot);
		}
		return U_k;
	}

	private IloIntVar[][] createDroppedOutVariables(IloCplex cplex) throws IloException {
		IloIntVar[][] d_a_k = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			d_a_k[a] = cplex.intVarArray(numSamples, 0, 1); 
		}
		return d_a_k;
	}

	/**
	 * Creates variables specifying how the agent dropped out (e.g. did the sample occur
	 * before the agent had been in the auction yet, or after the agent had seen all
	 * its impressions?). Note that this variable only needs to exist for agents that
	 * didn't start in a slot, since agents starting in a slot obviously didn't drop out
	 * due to a sample occurring before they were in the auction.
	 * @param cplex
	 * @return
	 * @throws IloException
	 */
	private IloIntVar[][] createHowDroppedOutVariables(IloCplex cplex) throws IloException {
		//Even though we don't need these variables unless the agent starts in a position 
		//beyond the number of slots, we'll create variables for everyone for consistency
		//in indexing. (Or at least have the array be the appropriate size, even if 
		//some values are null.)
		IloIntVar[][] h_a_k = new IloIntVar[numAgents][];
		for (int a=numSlots; a<numAgents; a++) {
			h_a_k[a] = cplex.intVarArray(numSamples, 0, 1); 
		}
		return h_a_k;
	}

	private IloIntVar[][] createPositionVariables(IloCplex cplex) throws IloException {
		IloIntVar[][] p_a_k = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			p_a_k[a] = cplex.intVarArray(numSamples, 0, numSlots); 
		}
		return p_a_k;
	}

	/**
	 * Create variables specifying the starting impression for each agent.
	 * Agents starting in the auction will have a starting impression of 1.
	 * @param cplex
	 * @return
	 * @throws IloException
	 */
	private IloNumVar[] createStartingImpressionVariables(IloCplex cplex) throws IloException {
		IloNumVar[] Istart_a;
		if (INTEGER_PROGRAM) {
			Istart_a = cplex.intVarArray(numAgents, 1, maxImpsPerAgentPerSlot); 
		} else {
			Istart_a = cplex.numVarArray(numAgents, 1, maxImpsPerAgentPerSlot); 				
		}
		return Istart_a;
	}

	/**
	 * Create variables specifying the ending impression for each agent.
	 * @param cplex
	 * @return
	 * @throws IloException
	 */
	private IloNumVar[] createEndingImpressionVariables(IloCplex cplex) throws IloException {
		IloNumVar[] Iend_a;
		if (INTEGER_PROGRAM) {
			Iend_a = cplex.intVarArray(numAgents, 1, maxImpsPerAgentPerSlot); 
		} else {
			Iend_a = cplex.numVarArray(numAgents, 1, maxImpsPerAgentPerSlot); 				
		}
		return Iend_a;
	}

	
	//***** DECISION VARIABLES FOR NEW SAMPLED PROBLEM *******
	
	private IloIntVar[][] createNewImpressionSamplingVariables(IloCplex cplex) throws IloException {
		IloIntVar[][] V_i_k = new IloIntVar[numBuckets][];
		for (int i=0; i<numBuckets; i++) {
			V_i_k[i] = cplex.intVarArray(numSamples, 0, 1); 
		}
		return V_i_k;
	}


	private IloNumVar[] createImpressionsInBucketVariables(IloCplex cplex) throws IloException  {
		IloNumVar[] z_i;
		if (INTEGER_PROGRAM) {
			z_i = cplex.intVarArray(numBuckets, 0, maxImpsPerAgent); 
		} else {
			z_i = cplex.numVarArray(numBuckets, 0, maxImpsPerAgent); 
		}
		return z_i;
	}


	private IloNumVar[] createLastImpressionInBucketVariables(IloCplex cplex) throws IloException  {
		IloNumVar[] U_i;
		if (INTEGER_PROGRAM) {
			U_i = cplex.intVarArray(numBuckets, 0, maxImpsPerAgent); 
		} else {
			U_i = cplex.numVarArray(numBuckets, 0, maxImpsPerAgent); 
		}
		return U_i;
	}


	private IloIntVar[][] createDroppedOutOfBucketVariables(IloCplex cplex) throws IloException  {
		IloIntVar[][] d_a_i = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			d_a_i[a] = cplex.intVarArray(numBuckets, 0, 1); 
		}
		return d_a_i;
	}


	private IloIntVar[][] createHowDroppedOutOfBucketVariables(IloCplex cplex) throws IloException  {
		IloIntVar[][] h_a_i = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			h_a_i[a] = cplex.intVarArray(numBuckets, 0, 1); 
		}
		return h_a_i;
	}


	private IloNumVar[] createImpressionsInSampledBucketVariables(IloCplex cplex) throws IloException  {
		IloNumVar[] z_k;
		if (INTEGER_PROGRAM) {
			z_k = cplex.intVarArray(numSamples, 0, maxImpsPerAgent); 
		} else {
			z_k = cplex.numVarArray(numSamples, 0, maxImpsPerAgent); 
		}
		return z_k;
	}


	
	


	//************************************ OBJECTIVE FUNCTIONS ************************************


	/**
	 * This is just a generic objective function. It doesn't favor one
	 * feasible solution over another. (Maybe there's a way to specify
	 * this without the dummy objective, but I wasn't sure how.)
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addObjective_indifferentBetweenFeasibleSolutions(
			IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		IloLinearNumExpr expr = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			for (int s=0; s<=a; s++) {
				expr.addTerm(0, I_a_s[a][s]);
			}
		}
		cplex.addMaximize(expr);
	}


	/**
	 * This is a proxy to trying to maximize the number of slots
	 * that see the same amount of impressions as the first slot.
	 * The idea is that there is a finite number of impressions, and many
	 * agents could still be in the auction by the time these impressions run out.
	 * The reason it's a proxy is because it's computing the sum of distances from
	 * the first slot. If three slots are exact but one is very far away, this objective will
	 * (incorrectly?) favor two slots that are exact and two that are close by.
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addObjective_minimizeSlotImpressionDifferenceProxy(
			IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		// distance = (numSlots-1)*slot1Imps - sum(otherSlotImps)
		// minimize distance
		IloLinearNumExpr slot1ImpsMultiplied = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			slot1ImpsMultiplied.addTerm( (numSlots-1) , I_a_s[a][0]);
		}
		IloLinearNumExpr otherSlotImps = cplex.linearNumExpr();
		for (int a=1; a<numAgents; a++) {
			for (int s=1; s<=Math.min(a, numSlots-1); s++) {
				otherSlotImps.addTerm(1, I_a_s[a][s]);
			}
		}
		cplex.addMinimize(cplex.diff(slot1ImpsMultiplied, otherSlotImps));
	}

	
	
	private void addObjective_minimizeSlotImpressionDifference(
			IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		//Maximize the number of slots that end at the same time as the first slot.
		//Tiebreaker: maximize the number of impressions seen in the first slot.
		int LARGE_CONSTANT = maxImpsPerAgent + 1;

		//Create 1/0 variable which will say whether some other slot has the same #imps as slot 1.
		IloIntVar[] differentImpsThanSlot1 = cplex.intVarArray(numSlots-1, 0, 1);		

		//Determine how many imps were in slot 1.
		IloLinearNumExpr slot1Imps = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			slot1Imps.addTerm( 1, I_a_s[a][0]);
		}

		//Determine how many imps were in each of the other slots
		for (int s=1; s<numSlots; s++) {
			IloLinearNumExpr slotSImps = cplex.linearNumExpr();
			for (int a=s; a<numAgents; a++) {
				slotSImps.addTerm(1, I_a_s[a][s]);
			}
			//Add constraint: if different amount of imps in these slots, "differentImpsThanSlot1 = 1"
			cplex.addLe(cplex.diff(slot1Imps, slotSImps), cplex.prod(LARGE_CONSTANT, differentImpsThanSlot1[s-1])); //-1 because we don't have a var for the 0th slot
		}
		
		if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF) {
			cplex.addMinimize(cplex.sum(differentImpsThanSlot1));
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF_TIEBREAKER) {
			cplex.addMinimize(cplex.diff( cplex.prod(LARGE_CONSTANT , cplex.sum(differentImpsThanSlot1)) , slot1Imps) );
		}
	}
	
	

	private void addObjective_closeToImpressionsUpperBound(
			IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		IloLinearNumExpr slotImps = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			for (int s=0; s<=Math.min(a, numSlots-1); s++) {
				slotImps.addTerm(1, I_a_s[a][s]);
			}
		}
		cplex.addMaximize(slotImps);
		//cplex.addMinimize(cplex.diff(numSlots*maxImpsPerAgent, slotImps));		
	}

	
	


	private void addObjective_spreadSamplesLinear(IloCplex cplex,
			IloNumVar[] U_k, IloNumVar[][] I_a_s) throws IloException {

		IloLinearNumExpr slot1Imps = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			slot1Imps.addTerm( (numSlots-1) , I_a_s[a][0]);
		}

		IloNumExpr[] sampleDistances = new IloNumExpr[numSamples];
		for (int k=0; k<numSamples; k++) {
			//double intervalFraction = (k + .5) / (double) numSamples;
			double intervalFraction = (numSamples==1) ? .5 : k / (double) (numSamples - 1);
			sampleDistances[k] = cplex.abs(cplex.diff(U_k[k], cplex.prod(intervalFraction, slot1Imps)));
		}
		cplex.addMinimize(cplex.sum(sampleDistances));
	}


	private void addObjective_spreadSamplesQuadratic(IloCplex cplex,
			IloNumVar[] U_k, IloNumVar[][] I_a_s) throws IloException {

		IloLinearNumExpr slot1Imps = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			slot1Imps.addTerm( (numSlots-1) , I_a_s[a][0]);
		}

		IloNumExpr[] sampleDistances = new IloNumExpr[numSamples+1];
		sampleDistances[0] = cplex.prod( cplex.diff(U_k[0], 1), cplex.diff(U_k[0], 1) );
		for (int k=1; k<numSamples; k++) {
			sampleDistances[k] = cplex.prod( cplex.diff(U_k[k], U_k[k-1]), cplex.diff(U_k[k], U_k[k-1]) );
		}
		sampleDistances[numSamples] = cplex.prod( cplex.diff(slot1Imps, U_k[numSamples-1]), cplex.diff(slot1Imps, U_k[numSamples-1]) );

		cplex.addMinimize(cplex.sum(sampleDistances));
	}


	
	
	/**
	 * This will cause constraints to be created that try to make the average positions
	 * as close as possible to what were sampled (assuming you don't have exact info for that position).
	 * 
	 * @param cplex
	 * @param I_a_s
	 * @param I_a
	 * @throws IloException
	 */
	private void addObjective_minimizeDistanceFromSampledMu(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a) throws IloException {
		
		//The maximum allowed difference between the sample and observed average positions 
		int LARGE_CONSTANT = numSlots*maxImpsPerAgent;
		if (BUGGY_VERSION) LARGE_CONSTANT = numSlots;
		
		//Create variable which will say how far an agent's predicted average position is from the observed sampleMu
		IloNumVar[] errors = cplex.numVarArray(numAgents, 0, LARGE_CONSTANT);	
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		
		//Add constraints stating resulting avgPositions have to be close to sampledMu
		for (int a=0; a<numAgents; a++) {
			if (!isKnownMu_a[a] && isKnownSampledMu_a[a]) {
				//Compute resulting avgPosition
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(knownSampledMu_a[a], I_a[a]);

				//Make sure avgPosition is close to satisfied
				cplex.addLe(lhs, cplex.sum(rhs, errors[a]));
				cplex.addGe(lhs, cplex.sum(rhs, cplex.prod(-1, errors[a])));
				
				//Keep track of which agents have constraints here
				relevantErrors.addTerm(1, errors[a]);
			}
		}
		
		//Minimize total distance from sampledMus
		cplex.addMinimize(relevantErrors);
	}
	
	
	
	/**
	 * This will make the predicted agent impressions as close to our priors as possible. 
	 * @param cplex
	 * @param I_a
	 * @throws IloException
	 */
	private void addObjective_minimizeImpressionPriorError(IloCplex cplex, IloNumVar[] I_a) throws IloException {
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (isKnownI_aDistributionMean[a] && !isKnownI_a[a]) {
				cplex.addLe(I_a[a], cplex.sum( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])  )  );
				cplex.addGe(I_a[a], cplex.diff( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])  )  );
				relevantErrors.addTerm(1, I_aError[a]);
			}
		}
		cplex.addMinimize(relevantErrors);
	}
	
	
	
	
	/**
	 * This will make the predicted agent impressions as close to our priors as possible. 
	 * @param cplex
	 * @param I_a
	 * @throws IloException
	 */
	private void addObjective_minimizeImpressionPriorErrorAndDistanceFromSampledMu(IloCplex cplex, IloNumVar[][] I_a_s, IloNumVar[] I_a) throws IloException {
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		
		
		//----- Impressions prior error
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr impressionsErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (isKnownI_aDistributionMean[a] && !isKnownI_a[a]) {
				cplex.addLe(I_a[a], cplex.sum( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])  )  );
				cplex.addGe(I_a[a], cplex.diff( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])  )  );
				impressionsErrors.addTerm(1, I_aError[a]);
			}
		}
		
		
		//----- Average position error
				
		//The maximum allowed difference between the sample and observed average positions 
		int LARGE_CONSTANT = numSlots*maxImpsPerAgent;
		
		//Create variable which will say how far an agent's predicted average position is from the observed sampleMu
		IloNumVar[] errors = cplex.numVarArray(numAgents, 0, LARGE_CONSTANT);	
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		
		//Add constraints stating resulting avgPositions have to be close to sampledMu
		for (int a=0; a<numAgents; a++) {
			if (!isKnownMu_a[a] && isKnownSampledMu_a[a]) {
				//Compute resulting avgPosition
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(knownSampledMu_a[a], I_a[a]);

				//Make sure avgPosition is close to satisfied
				cplex.addLe(lhs, cplex.sum(rhs, errors[a]));
				cplex.addGe(lhs, cplex.sum(rhs, cplex.prod(-1, errors[a])));
				
				//Keep track of which agents have constraints here
				relevantErrors.addTerm(1, errors[a]);
			}
		}
		
		
		//Compute some weighted sum of these two errors
		//TODO: what should the weight be?
		double impressionsWeight = 1;
		double avgPosWeight = 10;
		IloNumExpr weightedError = cplex.sum( cplex.prod(impressionsWeight, impressionsErrors),
				cplex.prod(avgPosWeight, relevantErrors) );
		cplex.addMinimize(weightedError);
	}
	
	
	
	
	
	private void addObjective_minimizeImpressionPriorErrorRankingUnknown(IloCplex cplex, IloNumVar[] I_a, IloIntVar[][] r_a_agent) throws IloException {
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		boolean USE_SIMPLIFIED_ERROR = true; //ignore standard deviation. TODO: Implement true objective
		
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				

		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (!isKnownI_a[a]) {
				
				
				if (USE_SIMPLIFIED_ERROR) {
					//In the simplified version, we don't consider that each agent has a different I_aStdev prior. 
					//This reduces the number of constraints we need to add to the problem.
					IloLinearNumExpr I_aMean = cplex.linearNumExpr();
					for (int agent=0; agent<numAgents; agent++) {
						if (isKnownI_aDistributionMean[agent]) I_aMean.addTerm(knownI_aDistributionMean[agent], r_a_agent[a][agent]);
					}
					double I_aStdev = 1; //TODO: Make this more complex when using non-simplified error. (Note we'll have to add more constraints, otherwise we'll be multiplying two decision variables)
					
					cplex.addLe(I_a[a], cplex.sum( I_aMean , cplex.prod(I_aStdev, I_aError[a])  )  );
					cplex.addGe(I_a[a], cplex.diff( I_aMean , cplex.prod(I_aStdev, I_aError[a])  )  );
					relevantErrors.addTerm(1, I_aError[a]);
				}
				
				
				
			}
		}
		cplex.addMinimize(relevantErrors);
	}

	
//	/**
//	 * This constraint ensures that if we know any agents' total impressions, the total 
//	 * impressions CPLEX decides on will match this value. Ranking is unknown.
//	 * @param cplex
//	 * @param I_a
//	 * @throws IloException
//	 */
//	private void addConstraint_totalImpressionsKnownRankingUnknown(IloCplex cplex, IloNumVar[] I_a, IloIntVar[][] r_a_agent) throws IloException {
//		int LARGE_CONSTANT = maxImpsPerAgent + 1;
//		for (int agent=0; agent<numAgents; agent++) { 
//			if (isKnownI_a[agent]) {
//				for (int a=0; a<numAgents; a++) {
//					IloLinearNumExpr rhs1 = cplex.linearNumExpr();
//					rhs1.setConstant(knownI_a[agent] - LARGE_CONSTANT);
//					rhs1.addTerm(LARGE_CONSTANT, r_a_agent[a][agent]);
//
//					IloLinearNumExpr rhs2 = cplex.linearNumExpr();
//					rhs2.setConstant(knownI_a[agent] + LARGE_CONSTANT);
//					rhs2.addTerm(-LARGE_CONSTANT, r_a_agent[a][agent]);
//
//					if (USE_EPSILON) {
//						cplex.addGe(I_a[a], cplex.sum(rhs1, -epsilon));
//						cplex.addLe(I_a[a], cplex.sum(rhs2, epsilon));
//					} else {
//						cplex.addGe(I_a[a], rhs1);
//						cplex.addLe(I_a[a], rhs2);
//					}
//				}
//			}
//		}
//	}
//
//	
	
	

	private void addObjective_maximizeImpressionsInSampledBuckets(
			IloCplex cplex, IloNumVar[] z_k) throws IloException {
		
		//We'll actually minimize negative impressions, just to keep it as a minimization problem
		IloLinearNumExpr negativeImpressionsInSampledBuckets = cplex.linearNumExpr();
		for (int k=0; k<numSamples; k++) {
			negativeImpressionsInSampledBuckets.addTerm(-1, z_k[k]);
		}
		cplex.addMinimize(negativeImpressionsInSampledBuckets);
	}


	
	//See section 6.2 of:
	//http://www.aimms.com/aimms/download/manuals/aimms3om_linearprogrammingtricks.pdf
	private void addObjective_maximizeMinImpressionsInSampledBuckets(
			IloCplex cplex, IloNumVar[] z_k) throws IloException {

		IloNumVar fewestImpressions = cplex.numVar(0, maxImpsPerAgent);
		for (int k=0; k<numSamples; k++) {
			cplex.addLe(fewestImpressions, z_k[k]);
		}
		cplex.addMaximize(fewestImpressions);
	}

	
	
	

	//************************************ CONSTRAINTS ************************************

	/**
	 * This constraint states that an advertiser can't spend more time in some set of slots than the
	 * guys above him spent in the slot directly above that set.
	 * (Note we don't have this constraint for the 0th agent or the 0th slot)
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addConstraint_waterfallOrdering(IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		// Advertiser $a$ can't spend more time in slots $k$ through $a$ than 
		// advertisers above him spent in slot $k-1$.
		for (int a=1; a<numAgents; a++) {
			for (int k=1; k<=a; k++) {
				StringBuffer sb = new StringBuffer();					
				IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
				IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();

				//Create constraint for this value of $a$ and $k$
				for (int i=k; i<=a; i++) {
					sb.append("I_a"+a+"_s"+i + " + ");
					myCumulativeImps.addTerm(1, I_a_s[a][i]);
				}
				sb.append("<= ");
				for (int opp=k-1; opp<=a-1; opp++) {
					sb.append("I_a"+opp+"_s"+ (k-1) + " + ");
					oppCumulativeImps.addTerm(1, I_a_s[opp][k-1]);
				}
//				System.out.println(sb);
				cplex.addLe(myCumulativeImps, oppCumulativeImps); //add constraint

			}
		}		
	}



	/**
	 * This constraint states that, if an agent appeared at all in slot k, 
	 * then the amount of time he spent in lower slots (on the page) is the same
	 * as the amount of time agents above him spent in slot k. In other words, 
	 * the agent must have been in the other slots as long as necessary to even
	 * get to slot k.
	 * @param cplex
	 * @param I_a_s
	 * @param Y_a_k
	 * @throws IloException
	 */
	private void addConstraint_waterfallOrderingTight(IloCplex cplex,
			IloNumVar[][] I_a_s, IloIntVar[][] Y_a_k) throws IloException {
		// This is an A -> B conditional constraint.
		// We change to not(A) or B, and introduce a new boolean variable
		// so that either constraints A or B can be true.

		//not(A)
		for (int a=1; a<numAgents; a++) {
			for (int k=1; k<=a; k++) {
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(-LARGE_CONSTANT_Y_a_k, Y_a_k[a][k-1]);
				rhs.setConstant(LARGE_CONSTANT_Y_a_k);
				cplex.addLe(I_a_s[a][k-1], rhs);

				StringBuffer sb = new StringBuffer();
				sb.append("I_a"+a+"_s"+(k-1)+" <= " + LARGE_CONSTANT_Y_a_k + " - " + LARGE_CONSTANT_Y_a_k + " * Y_a"+a+"_k"+(k-1));
//				System.out.println(sb);
			}
		}

		//(Constraints 2 and 3)
		for (int a=1; a<numAgents; a++) {
			for (int k=1; k<=a; k++) {
				//-------
				StringBuffer sb = new StringBuffer();					
				IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
				IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();
				//Create constraint for this value of $a$ and $k$
				for (int i=k; i<=a; i++) {
					sb.append("I_a"+a+"_s"+i + " + ");
					myCumulativeImps.addTerm(1, I_a_s[a][i]);
				}
				sb.append("<= ");
				for (int opp=k-1; opp<=a-1; opp++) {
					sb.append("I_a"+opp+"_s"+ (k-1) + " + ");
					oppCumulativeImps.addTerm(1, I_a_s[opp][k-1]);
				}

				IloLinearNumExpr rhsGE = cplex.linearNumExpr();
				IloLinearNumExpr rhsLE = cplex.linearNumExpr();
				rhsGE.addTerm(-LARGE_CONSTANT_Y_a_k, Y_a_k[a][k-1]);
				rhsLE.addTerm(LARGE_CONSTANT_Y_a_k, Y_a_k[a][k-1]);
				rhsGE.add(oppCumulativeImps);
				rhsLE.add(oppCumulativeImps);
				cplex.addLe(myCumulativeImps, rhsLE); //add constraint
				cplex.addGe(myCumulativeImps, rhsGE);

				//System.out.println(sb + "(RHS and LHS)");
			}
		}
	}



	/**
	 * This constraint combines the two other waterfall ordering constraints
	 * and lets CPLEX handle the conditional constraints. It seems to do a 
	 * lot worse than the manual coding of conditional constraints. Is there
	 * something wrong with my code, or is CPLEX's conditional constraint just
	 * less efficient?
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addConstraint_waterfallOrderingAutomaticConditionals(
			IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		for (int a=1; a<numAgents; a++) {
			for (int k=1; k<=a; k++) {
				StringBuffer sb = new StringBuffer();					
				IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
				IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();

				//Create constraint for this value of $a$ and $k$
				for (int i=k; i<=a; i++) {
					sb.append("I_a"+a+"_s"+i + " + ");
					myCumulativeImps.addTerm(1, I_a_s[a][i]);
				}
				sb.append("<= ");
				for (int opp=k-1; opp<=a-1; opp++) {
					sb.append("I_a"+opp+"_s"+ (k-1) + " + ");
					oppCumulativeImps.addTerm(1, I_a_s[opp][k-1]);
				}
//				System.out.println(sb);
				cplex.addLe(myCumulativeImps, oppCumulativeImps); //add constraint

				//If I have anything in the above slot, the above constraint is tight.
				cplex.add( cplex.ifThen(cplex.ge(I_a_s[a][k-1], 1), cplex.eq(myCumulativeImps, oppCumulativeImps)) );
			}
		}
	}



	/**
	 * This constraint ensures that an agent's total impressions
	 * is the sum of impressions he received in each slot.
	 * @param cplex
	 * @param I_a_s
	 * @param I_a
	 * @throws IloException
	 */
	private void addConstraint_totalImpressionsDefinition(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a) throws IloException {
		//----------------------
		//Total number of impressions constraint:
		//Make sure "Total Agent Impressions" variable actually equals the sum of the agent's impressions per slot.
		//----------------------
		for (int a=0; a<numAgents; a++) {
			StringBuffer sb = new StringBuffer();	
			IloLinearNumExpr totalImps = cplex.linearNumExpr();
			for (int s=0; s<=Math.min(a, numSlots-1); s++) {
				sb.append("I_a"+a+"_s"+s+" + ");
				totalImps.addTerm(1, I_a_s[a][s]);
			}
			sb.append("= " + "I_a" + I_a[a]);
//			System.out.println(sb);
			cplex.addEq(totalImps, I_a[a]);
		}		
	}


	/**
	 * This constraint ensures that if we know any agents' total impressions, the total 
	 * impressions CPLEX decides on will match this value.
	 * @param cplex
	 * @param I_a
	 * @throws IloException
	 */
	private void addConstraint_totalImpressionsKnown(IloCplex cplex, IloNumVar[] I_a) throws IloException {
		for (int a=0; a<numAgents; a++) { 
			if (isKnownI_a[a]) {
				cplex.addEq(I_a[a], knownI_a[a]);
			}
		}
	}
	
	
	/**
	 * This constraint ensures that if we know any agents' total impressions, the total 
	 * impressions CPLEX decides on will match this value. Ranking is unknown.
	 * @param cplex
	 * @param I_a
	 * @throws IloException
	 */
	private void addConstraint_totalImpressionsKnownRankingUnknown(IloCplex cplex, IloNumVar[] I_a, IloIntVar[][] r_a_agent) throws IloException {
		int LARGE_CONSTANT = maxImpsPerAgent + 1;
		for (int agent=0; agent<numAgents; agent++) { 
			if (isKnownI_a[agent]) {
				for (int a=0; a<numAgents; a++) {
					IloLinearNumExpr rhs1 = cplex.linearNumExpr();
					rhs1.setConstant(knownI_a[agent] - LARGE_CONSTANT);
					rhs1.addTerm(LARGE_CONSTANT, r_a_agent[a][agent]);

					IloLinearNumExpr rhs2 = cplex.linearNumExpr();
					rhs2.setConstant(knownI_a[agent] + LARGE_CONSTANT);
					rhs2.addTerm(-LARGE_CONSTANT, r_a_agent[a][agent]);

					if (USE_EPSILON) {
						cplex.addGe(I_a[a], cplex.sum(rhs1, -epsilon));
						cplex.addLe(I_a[a], cplex.sum(rhs2, epsilon));
					} else {
						cplex.addGe(I_a[a], rhs1);
						cplex.addLe(I_a[a], rhs2);
					}
				}
			}
		}
	}

	
	
	
	/**
	 * This constraint ensures that if we know any agents' total PROMOTED impressions, the total 
	 * impressions CPLEX decides on for promoted slots will match this value.
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addConstraint_totalPromotedImpressionsKnown(IloCplex cplex,
			IloNumVar[][] I_a_s) throws IloException {
		for (int a=0; a<numAgents; a++) {
			if (isKnownI_aPromoted[a]) { //If we know how many promoted imps the agent received
				if (isKnownPromotionEligible[a] || knownI_aPromoted[a] > 0) { //If we know the agent was eligible for promotion or we observed promoted slots
					IloLinearNumExpr lhs = cplex.linearNumExpr();
					for (int s=0; s<=Math.min(a, numPromotedSlots-1); s++) {
						lhs.addTerm(1, I_a_s[a][s]);
					}
					cplex.addEq(lhs, knownI_aPromoted[a]);
				}
			}
		}		
	}



	/**
	 * This constraint ensures that if we know any agents' total PROMOTED impressions, the total 
	 * impressions CPLEX decides on for promoted slots will match this value.
	 * @param cplex
	 * @param I_a_s
	 * @throws IloException
	 */
	private void addConstraint_totalPromotedImpressionsKnownRankingUnknown(IloCplex cplex,
			IloNumVar[][] I_a_s, IloIntVar[][] r_a_agent) throws IloException {
		int LARGE_CONSTANT = 1000;
		for (int agent=0; agent<numAgents; agent++) {
			if (isKnownI_aPromoted[agent]) { //If we know how many promoted imps the agent received
				if (isKnownPromotionEligible[agent] || knownI_aPromoted[agent] > 0) { //If we know the agent was eligible for promotion or we observed promoted slots
					for (int a=0; a<numAgents; a++) {
						//Number of promoted impressions according to CPLEX
						IloLinearNumExpr lhs = cplex.linearNumExpr();
						for (int s=0; s<=Math.min(a, numPromotedSlots-1); s++) {
							lhs.addTerm(1, I_a_s[a][s]);
						}
						
						//This agent's known promoted impressions 
						IloLinearNumExpr rhs1 = cplex.linearNumExpr();
						rhs1.setConstant(knownI_aPromoted[agent] - LARGE_CONSTANT);
						rhs1.addTerm(LARGE_CONSTANT, r_a_agent[a][agent]);

						//This agent's known promoted impressions
						IloLinearNumExpr rhs2 = cplex.linearNumExpr();
						rhs2.setConstant(knownI_aPromoted[agent] + LARGE_CONSTANT);
						rhs2.addTerm(-LARGE_CONSTANT, r_a_agent[a][agent]);

						if (USE_EPSILON) {
							cplex.addGe(lhs, cplex.sum(rhs1, -epsilon));
							cplex.addLe(lhs, cplex.sum(rhs2, epsilon));
						} else {
							cplex.addGe(lhs, rhs1);
							cplex.addLe(lhs, rhs2);
						}
					}
				}
			}
		}
	}


	
	/**
	 * This constraint ensures that, if any agent didn't hit their budget,
	 * nobody else saw more "total impressions" than that agent.
	 * TODO: If an agent didn't hit their budget, their "total imps" equals the total number of user searches.
	 * TODO: If multiple agents didn't hit their budget, they must have seen the same number of impressions.
	 * TODO: What about when an agent is known to HAVE hit their budget? Can we say that they received less than the full amount of impressions?
	 * @param cplex
	 * @param i_a
	 * @throws IloException 
	 */
	private void addConstraint_didntHitBudget(IloCplex cplex, IloNumVar[][] I_a_s) throws IloException {
		for (int a=0; a<numAgents; a++) {
			if (hitBudget[a] == 0) {

				//Get the number of impressions this agent saw
				IloLinearNumExpr totalImps_a = cplex.linearNumExpr();
				for (int s=0; s<=a; s++) {
					totalImps_a.addTerm(1, I_a_s[a][s]);
				}

				//Get the number of impressions for some other agent
				for (int a2=0; a2<numAgents; a2++) {
					if (a2==a) continue; //skip the agent who didn't hit its budget
					
					IloLinearNumExpr totalImps_a2 = cplex.linearNumExpr();
					for (int s=0; s<=a2; s++) {
						totalImps_a2.addTerm(1, I_a_s[a2][s]);
					}
					//Make sure this other agent's impressions aren't more than the agent with no hit budget
					cplex.addLe(totalImps_a2, totalImps_a);
				}
			}
		}
	}

	
	

	/**
	 * This constraint ensures that agent impressions per slot must result in the known 
	 * (exact) average positions.
	 * @param cplex
	 * @param I_a_s
	 * @param I_a
	 * @throws IloException
	 */
	private void addConstraint_exactAveragePositionsKnown(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a) throws IloException {
		for (int a=0; a<numAgents; a++) {
			if (isKnownMu_a[a]) {
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(knownMu_a[a], I_a[a]);

				if (USE_EPSILON) {
					cplex.addLe(lhs, cplex.sum(rhs, epsilon));
					cplex.addGe(lhs, cplex.sum(rhs, -epsilon));
				} else {
					cplex.addEq(lhs, rhs);
				}
			}
		}
	}

	

	/**
	 * This constraint ensures that agent impressions per slot must result in the known 
	 * (exact) average positions. We do not know the ranking of advertisers in this case.
	 * @param cplex
	 * @param I_a_s
	 * @param I_a
	 * @throws IloException
	 */
	private void addConstraint_exactAveragePositionsKnownRankingUnknown(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a, IloIntVar[][] r_a_agent) throws IloException {
		//Choose a large constant so that constraints are trivially satisfied if constant is introduced. 
		//I think it only has to be (numSlots-1) * maxImpsPerAgent + 1, but we'll play it safe.
		//TODO: Do we need some checking for when knownMu_a = NaN??? (e.g. when we have people that weren't in the auction)
		//  (Since it's exactAvgPositions, I think we can just remove these "noImps" agents from the problem
		//   beforehand without losing generality) 
		int LARGE_CONSTANT = numSlots * maxImpsPerAgent + 1;

		for (int a=0; a<numAgents; a++) {
			if (isKnownMu_a[a]) {
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				
				for (int agent=0; agent<numAgents; agent++) {					
					IloLinearNumExpr rhs1 = cplex.linearNumExpr();
					rhs1.addTerm(knownMu_a[agent], I_a[a]);
					rhs1.addTerm(LARGE_CONSTANT, r_a_agent[a][agent]);
					rhs1.setConstant(-LARGE_CONSTANT);
					
					IloLinearNumExpr rhs2 = cplex.linearNumExpr();
					rhs2.addTerm(knownMu_a[agent], I_a[a]);
					rhs2.addTerm(-LARGE_CONSTANT, r_a_agent[a][agent]);
					rhs2.setConstant(LARGE_CONSTANT);
										
					if (USE_EPSILON) {
						cplex.addGe(lhs, cplex.sum(rhs1, -epsilon));
						cplex.addLe(lhs, cplex.sum(rhs2, epsilon));
					} else {
						cplex.addGe(lhs, rhs1);
						cplex.addLe(lhs, rhs2);
					}
				}
			}
		}
	}
	
	
	
	private void addConstraint_oneRankingPerAgent(IloCplex cplex, IloIntVar[][] r_a_agent) throws IloException {
		for (int agent=0; agent<numAgents; agent++) {
			IloLinearNumExpr lhs = cplex.linearNumExpr();
			for (int a=0; a<numAgents; a++) {
				lhs.addTerm(1, r_a_agent[a][agent]);
			}
			cplex.addEq(lhs, 1);
		}
	}

	private void addConstraint_oneAgentPerRanking(IloCplex cplex, IloIntVar[][] r_a_agent) throws IloException {
		for (int a=0; a<numAgents; a++) {
			IloLinearNumExpr lhs = cplex.linearNumExpr();
			for (int agent=0; agent<numAgents; agent++) {
				lhs.addTerm(1, r_a_agent[a][agent]);
			}
			cplex.addEq(lhs, 1);
		}
	}

	
	
	/**
	 * This constraint ensures that the last impression seen by the advertiser
	 * is actually consistent with the number of impressions seen per slot.
	 * @param cplex
	 * @param i_a_s
	 * @param Iend_a
	 * @throws IloException 
	 */
	private void addConstraint_endingImpressionDefinition(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] Iend_a) throws IloException {
		for (int a=0; a<numAgents; a++) {
			IloLinearNumExpr totalImps = cplex.linearNumExpr();
			for (int s=0; s<=a; s++) {
				totalImps.addTerm(1, I_a_s[a][s]);
			}
			cplex.addEq(totalImps, Iend_a[a]);
		}		
	}

	private void addConstraint_startingImpressionDefinition(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] Istart_a) throws IloException {
		for (int a=0; a<numAgents; a++) {
			IloLinearNumExpr totalImps = cplex.linearNumExpr();
			for (int s=numSlots; s<=a; s++) {
				totalImps.addTerm(1, I_a_s[a][s]);
			}
			//Add 1 to the total; we want the first impression seen, not the total unseen.
			totalImps.setConstant(1); 
			cplex.addEq(totalImps, Istart_a[a]);
		}		
	}

	/**
	 * This defines what it means for an agent in a starting slot to be dropped out for sample k.
	 * To be dropped out, the agent's ending impression must occur before the sampled impression.
	 * (And if not dropped out, the agent's ending impression must occur after the sampled impression.)
	 * @param cplex
	 * @param d_a_k
	 * @param u_k
	 * @param iend_a
	 * @throws IloException 
	 */
	private void addConstraint_droppedOutDefinitionInitialAgents(
			IloCplex cplex, IloIntVar[][] d_a_k, IloNumVar[] U_k,
			IloNumVar[] Iend_a) throws IloException {
		//Only one of these constraints is active, depending on whether or not
		//the agent dropped out. We choose a large constant to ensure one of the
		//constraints is always trivially satisfied.
		int LARGE_CONSTANT = maxImpsPerAgentPerSlot + 1;
		for (int a=0; a<Math.min(numAgents, numSlots); a++) {
			for (int k=0; k<numSamples; k++) {

				//When not dropped out, sample happened before agent's last impression. (Equation 19)
				cplex.addLe(U_k[k], cplex.sum(Iend_a[a], cplex.prod(LARGE_CONSTANT, d_a_k[a][k])));

				//When dropped out, sample happened after the agent's last impression. (Equation 21 reduced)		
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(1, Iend_a[a]);
				rhs.addTerm(LARGE_CONSTANT, d_a_k[a][k]);
				rhs.setConstant(-LARGE_CONSTANT + 1); //add 1 because >=, not >
				cplex.addGe(U_k[k], rhs);				
			}
		}
	}


	private void addConstraint_droppedOutDefinitionLaterAgents(IloCplex cplex,
			IloIntVar[][] d_a_k, IloIntVar[][] h_a_k, IloNumVar[] U_k,
			IloNumVar[] Istart_a, IloNumVar[] Iend_a) throws IloException {
		int LARGE_CONSTANT = maxImpsPerAgentPerSlot + 1;

		//for each agent that doesn't start in a slot
		for (int a=numSlots; a<numAgents; a++) {
			for (int k=0; k<numSamples; k++) {
				//When not dropped out, sample happened after the agent's first impression. (Equation 18)
				//(comes into play when d_a_k = 0)
				cplex.addGe(U_k[k], cplex.sum(Istart_a[a], cplex.prod(-LARGE_CONSTANT, d_a_k[a][k])));

				//When not dropped out, sample happened before agent's last impression. (Equation 19)
				//(comes into play when d_a_k = 0)
				cplex.addLe(U_k[k], cplex.sum(Iend_a[a], cplex.prod(LARGE_CONSTANT, d_a_k[a][k])));

				//When dropped out, sample may have happened before the agent's first impression. (Equation 20)
				//(comes into play when d_a_k = 1)
				//(comes into play when h_a_k = 0)
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(1, Istart_a[a]);
				rhs.addTerm(-LARGE_CONSTANT, d_a_k[a][k]);
				rhs.addTerm(LARGE_CONSTANT, h_a_k[a][k]);
				rhs.setConstant(LARGE_CONSTANT - 1); //subtract 1 because <=, not <
				cplex.addLe(U_k[k], rhs);				

				//When dropped out, sample may have happened after the agent's last impression. (Equation 21)
				//(comes into play when d_a_k = 1)
				//(comes into play when h_a_k = 1)
				IloLinearNumExpr rhs2 = cplex.linearNumExpr();
				rhs2.addTerm(1, Iend_a[a]);
				rhs2.addTerm(LARGE_CONSTANT, d_a_k[a][k]);
				rhs2.addTerm(LARGE_CONSTANT, h_a_k[a][k]);
				rhs2.setConstant(-2*LARGE_CONSTANT + 1); //add 1 because >=, not >
				cplex.addGe(U_k[k], rhs2);
			}
		}
	}



	/**
	 * This defines which position an agent is in for sample k, given which agents above him 
	 * dropped out and whether or not that agent itself was dropped out.
	 * @param cplex
	 * @param p_a_k
	 * @param d_a_k
	 * @throws IloException
	 */
	private void addConstraint_samplePositionDefinition(IloCplex cplex,
			IloIntVar[][] p_a_k, IloIntVar[][] d_a_k) throws IloException {
		int LARGE_CONSTANT = numSlots + 1;
		//(Equations 26-28)
		for (int a=0; a<numAgents; a++) {
			for (int k=0; k<numSamples; k++) {
				IloLinearNumExpr numDroppedOutAbove = cplex.linearNumExpr();
				numDroppedOutAbove.setConstant(a + 1); //***Add one to a so that 1st slot has value 1.
				for (int opp=0; opp<a; opp++) {
					numDroppedOutAbove.addTerm(-1, d_a_k[opp][k]);
				}

				//(Break equality into two inequality constraints.)
				//When not dropped out, position >= start slot - #agents above who dropped out
				cplex.addGe(p_a_k[a][k], cplex.sum(
						numDroppedOutAbove, 
						cplex.prod(-LARGE_CONSTANT, d_a_k[a][k])));
				//When not dropped out, position <= start slot - #agents above who dropped out
				cplex.addLe(p_a_k[a][k], cplex.sum(
						numDroppedOutAbove, 
						cplex.prod(LARGE_CONSTANT, d_a_k[a][k])));

				//When dropped out, position is 0. [Bounded by 0, so we only need an <= constraint]
				cplex.addLe(p_a_k[a][k], cplex.sum(
						LARGE_CONSTANT,
						cplex.prod(-LARGE_CONSTANT, d_a_k[a][k])));

			}
		}
	}


	private void addConstraint_sampledAveragePositionsKnown(IloCplex cplex,
			IloIntVar[][] p_a_k, IloIntVar[][] d_a_k) throws IloException {
		for (int a=0; a<numAgents; a++) {
			//FIXME: Don't check if isKnown. Even if mean was NaN (i.e. 0), this gives us some info!
			//(Better yet: translate mean of NaN into mean of 0). Then we don't have to remove this.
			//Done.
			if (isKnownSampledMu_a[a]) { 
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int k=0; k<numSamples; k++) {
					lhs.addTerm(1, p_a_k[a][k]);
				}

				IloLinearNumExpr samplesSeen = cplex.linearNumExpr();
				samplesSeen.setConstant(numSamples);
				for (int k=0; k<numSamples; k++) {
					samplesSeen.addTerm(-1, d_a_k[a][k]);
				}
				IloNumExpr rhs = cplex.prod(knownSampledMu_a[a], samplesSeen);

				if (USE_EPSILON) {
					cplex.addLe(lhs, cplex.sum(rhs, epsilon));
					cplex.addGe(lhs, cplex.sum(rhs, -epsilon));
				} else {
					cplex.addEq(lhs, rhs);
				}	
			}
		}
	}


	private void addConstraint_orderedSamples(IloCplex cplex, IloNumVar[] U_k) throws IloException {
		for (int k=1; k<numSamples; k++) {
			cplex.addGe(U_k[k], cplex.sum(1, U_k[k-1]) );
		}
	}


	private void addConstraint_maxSample(IloCplex cplex, IloNumVar[] U_k,
			IloNumVar[][] I_a_s) throws IloException {
		IloLinearNumExpr firstSlotImpressions = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			firstSlotImpressions.addTerm(1, I_a_s[a][0]);
		}

		if (numSamples>0) cplex.addLe(U_k[numSamples-1], firstSlotImpressions);

		//Actually only need this for the last sample, since they're ordered.
		//for (int k=0; k<numSamples; k++) { 
		//	cplex.addLe(U_k[k], firstSlotImpressions);
		//}
	}





	/**
	 * This constraint ensures that, if a sample average position has been given
	 * for the agent, the agent must appear in at least one of the samples.
	 * @param cplex
	 * @param d_a_k
	 * @throws IloException
	 */
	private void addConstraint_notAllSamplesDropped(IloCplex cplex,
			IloIntVar[][] d_a_k) throws IloException {
		for (int a=0; a<numAgents; a++) {
			if (isKnownSampledMu_a[a]) { 
				IloLinearNumExpr totalDropped = cplex.linearNumExpr();
				for (int k=0; k<numSamples; k++) {
					totalDropped.addTerm(1, d_a_k[a][k]);
				}
				//Not allowed to be dropped from all "numSamples" samples.
				if (knownSampledMu_a[a] != 0) {
					cplex.addLe(totalDropped, numSamples-1);
				} else {
					cplex.addEq(totalDropped, numSamples);
				}
			}
		}
	}


	
	
	
	
	//**** CONSTRAINTS FOR THE NEW SAMPLED PROBLEM ****
	
	

	private void addConstraint_impressionsInBucket(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] z_i) throws IloException {

		//[bucket is 0-indexed]
		//Number of impressions in bucket i is the number of impressions the last
		//(i+1) agents saw in "slot" numAgents-1-i, minus the number of impressions in
		//buckets 1 through (i-1). 		
		for (int i=0; i<numBuckets; i++) {
			int relevantSlot = numAgents-1-i;
			
			//Get total impressions that happened in the relevant "slot"
			IloLinearNumExpr totalImpsInSlot = cplex.linearNumExpr();
			if (SAMPLES_POSSIBLE_AFTER_EVERYONE_DROPS && i==numBuckets-1) {
				//Last bucket = total imps
				int NUM_SEARCHES = maxImpsPerAgent;
				totalImpsInSlot.setConstant(NUM_SEARCHES);				
			} else {
				for (int a=relevantSlot; a<numAgents; a++) {
					totalImpsInSlot.addTerm(1, I_a_s[a][relevantSlot]);
				}
			} 
				
			//Get total impressions in previous buckets
			IloLinearNumExpr totalImpsInPreviousBuckets = cplex.linearNumExpr();			
			for (int j=0; j<i; j++) {
				totalImpsInPreviousBuckets.addTerm(1, z_i[j]);
			}
			
			cplex.addEq(z_i[i],  cplex.diff(totalImpsInSlot, totalImpsInPreviousBuckets) );			
		}		
	}


	private void addConstraint_lastImpressionInBucket(IloCplex cplex,
			IloNumVar[] z_i, IloNumVar[] U_i) throws IloException {
		// TODO Auto-generated method stub
		for (int i=0; i<numBuckets; i++) {
			IloLinearNumExpr rhs = cplex.linearNumExpr();
			for (int j=0; j<=i; j++) {
				rhs.addTerm(1, z_i[j]);
			}
			cplex.addEq(U_i[i], rhs);
		}
		
	}


	private void addConstraint_droppedOutOfBucketDefinitionInitialAgents(
			IloCplex cplex, IloIntVar[][] d_a_i, IloNumVar[] U_i,
			IloNumVar[] Iend_a) throws IloException {
		//Only one of these constraints is active, depending on whether or not
		//the agent dropped out. We choose a large constant to ensure one of the
		//constraints is always trivially satisfied.
		int LARGE_CONSTANT = maxImpsPerAgentPerSlot + 1;
		for (int a=0; a<Math.min(numAgents, numSlots); a++) {
			for (int i=0; i<numBuckets; i++) {
				if (SAMPLES_POSSIBLE_AFTER_EVERYONE_DROPS && i==numBuckets-1) {
					//If this is the last bucket, everyone has dropped out.
					cplex.addEq(d_a_i[a][i], 1);
					continue;
				}
				
				//When not dropped out, sample happened before agent's last impression. (Equation 19)
				cplex.addLe(U_i[i], cplex.sum(Iend_a[a], cplex.prod(LARGE_CONSTANT, d_a_i[a][i])));

				//When dropped out, sample happened after the agent's last impression. (Equation 21 reduced)		
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(1, Iend_a[a]);
				rhs.addTerm(LARGE_CONSTANT, d_a_i[a][i]);
				rhs.setConstant(-LARGE_CONSTANT + 1); //add 1 because >=, not >
				cplex.addGe(U_i[i], rhs);				
			}
		}

		
		
		// TODO Auto-generated method stub
		
	}


	private void addConstraint_droppedOutOfBucketDefinitionLaterAgents(
			IloCplex cplex, IloIntVar[][] d_a_i, IloIntVar[][] h_a_i,
			IloNumVar[] U_i, IloNumVar[] Istart_a, IloNumVar[] Iend_a) throws IloException {
		int LARGE_CONSTANT = maxImpsPerAgentPerSlot + 1;

		//for each agent that doesn't start in a slot
		for (int a=numSlots; a<numAgents; a++) {
			for (int i=0; i<numBuckets; i++) {
				//When not dropped out, sample happened after the agent's first impression. (Equation 18)
				//(comes into play when d_a_k = 0)
				cplex.addGe(U_i[i], cplex.sum(Istart_a[a], cplex.prod(-LARGE_CONSTANT, d_a_i[a][i])));

				//When not dropped out, sample happened before agent's last impression. (Equation 19)
				//(comes into play when d_a_k = 0)
				cplex.addLe(U_i[i], cplex.sum(Iend_a[a], cplex.prod(LARGE_CONSTANT, d_a_i[a][i])));

				//When dropped out, sample may have happened before the agent's first impression. (Equation 20)
				//(comes into play when d_a_k = 1)
				//(comes into play when h_a_k = 0)
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(1, Istart_a[a]);
				rhs.addTerm(-LARGE_CONSTANT, d_a_i[a][i]);
				rhs.addTerm(LARGE_CONSTANT, h_a_i[a][i]);
				rhs.setConstant(LARGE_CONSTANT - 1); //subtract 1 because <=, not <
				cplex.addLe(U_i[i], rhs);				

				//When dropped out, sample may have happened after the agent's last impression. (Equation 21)
				//(comes into play when d_a_k = 1)
				//(comes into play when h_a_k = 1)
				IloLinearNumExpr rhs2 = cplex.linearNumExpr();
				rhs2.addTerm(1, Iend_a[a]);
				rhs2.addTerm(LARGE_CONSTANT, d_a_i[a][i]);
				rhs2.addTerm(LARGE_CONSTANT, h_a_i[a][i]);
				rhs2.setConstant(-2*LARGE_CONSTANT + 1); //add 1 because >=, not >
				cplex.addGe(U_i[i], rhs2);
			}
		}		
	}


	private void addConstraint_droppedOutOfSampleDefinition(IloCplex cplex,
			IloIntVar[][] d_a_k, IloIntVar[][] d_a_i, IloIntVar[][] V_i_k) throws IloException {
		int LARGE_CONSTANT = 1;
		for (int a=0; a<numAgents; a++) {
			for (int i=0; i<numBuckets; i++) {
				for (int k=0; k<numSamples; k++) {
					IloLinearNumExpr rhs = cplex.linearNumExpr();
					rhs.addTerm(1, d_a_i[a][i]);
					rhs.addTerm(LARGE_CONSTANT, V_i_k[i][k]);
					rhs.setConstant(-LARGE_CONSTANT);
					cplex.addGe(d_a_k[a][k], rhs);

					IloLinearNumExpr rhs2 = cplex.linearNumExpr();
					rhs2.addTerm(1, d_a_i[a][i]);
					rhs2.addTerm(-LARGE_CONSTANT, V_i_k[i][k]);
					rhs2.setConstant(LARGE_CONSTANT);
					cplex.addLe(d_a_k[a][k], rhs2);					
				}
			}
		}
	}



	/**
	 * Each sample can only occur in a single bucket 
	 */
	private void addConstraint_oneBucketPerSample(IloCplex cplex,
			IloIntVar[][] V_i_k) throws IloException {
		for (int k=0; k<numSamples; k++) {
			IloLinearNumExpr bucketsPerSample = cplex.linearNumExpr();
			for (int i=0; i<numBuckets; i++) {
				bucketsPerSample.addTerm(1, V_i_k[i][k]);
			}
			cplex.addEq(bucketsPerSample, 1); //TODO: eq or leq?
		}
	}


	/**
	 * There cannot be more samples in a bucket than there are impressions in that bucket
	 * @param cplex
	 * @param v_i_k
	 * @param z_i
	 * @throws IloException
	 */
	private void addConstraint_noMoreSamplesThanImpressionsInBucket(
			IloCplex cplex, IloIntVar[][] V_i_k, IloNumVar[] z_i) throws IloException {
		for (int i=0; i<numBuckets; i++) {
			IloLinearNumExpr samplesInBucket = cplex.linearNumExpr();
			for (int k=0; k<numSamples; k++) {
				samplesInBucket.addTerm(1, V_i_k[i][k]);
			}
			cplex.addLe(samplesInBucket, z_i[i]);
		}
	}


	/**
	 * Samples have to be ordered by bucket (sample k occurs in bucket i --> sample k+1 occurs in bucket >= i)
	 * @param cplex
	 * @param v_i_k
	 * @throws IloException
	 */
	private void addConstraint_orderedSamplesByBucket(IloCplex cplex,
			IloIntVar[][] V_i_k) throws IloException {
		for (int k=1; k<numSamples; k++) {
			for (int i=0; i<numBuckets; i++) {
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				for (int j=0; j<=i; j++) {
					rhs.addTerm(1, V_i_k[j][k-1]);
				}
				cplex.addLe(V_i_k[i][k], rhs);
			}
		}
	}


	/**
	 * Determine number of impressions in sampled buckets
	 * @param cplex
	 * @param V_i_k
	 * @param z_k
	 * @param z_i
	 * @throws IloException
	 */
	private void addConstraint_impressionsInSampledBuckets(IloCplex cplex,
			IloIntVar[][] V_i_k, IloNumVar[] z_k, IloNumVar[] z_i) throws IloException {
		for (int i=0; i<numBuckets; i++) {
			int LARGE_CONSTANT = 12345;
			for (int k=0; k<numSamples; k++) {
				IloLinearNumExpr rhs = cplex.linearNumExpr();
				rhs.addTerm(1, z_i[i]);
				rhs.addTerm(LARGE_CONSTANT, V_i_k[i][k]);
				rhs.setConstant(-LARGE_CONSTANT);
				cplex.addGe(z_k[k], rhs);

				IloLinearNumExpr rhs2 = cplex.linearNumExpr();
				rhs2.addTerm(1, z_i[i]);
				rhs2.addTerm(-LARGE_CONSTANT, V_i_k[i][k]);
				rhs2.setConstant(LARGE_CONSTANT);
				cplex.addLe(z_k[k], rhs2);
			}
		}
	}

	
	
	



	//************************************ UTILITIES ************************************


	/**
	 * Simple 2D array printing method
	 * @param a
	 * @return
	 */
	public static String arrayString(double[][] a) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<a.length; i++) {
			for (int j=0; j<a[i].length; j++) {
				sb.append(Math.round(a[i][j]) + " ");
			}
			sb.append(" | ");
		}
		return sb.toString();
	}

	public static String arrayString(int[][] a) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<a.length; i++) {
			for (int j=0; j<a[i].length; j++) {
				sb.append(Math.round(a[i][j]) + " ");
			}
			sb.append(" | ");
		}
		return sb.toString();
	}

	
	/**
	 * Simple 3D array printing method
	 * @param a
	 * @return
	 */
	public static String arrayString(double[][][] a) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<a.length; i++) {
			sb.append(arrayString(a[i]) + "\n");
		}
		return sb.toString();
	}

	private static void debug(String s) {
		if (DEBUG) {
			System.out.println(s);
		}
	}


	//************************************ RESULT DATA STRUCTURE ************************************
	public class WaterfallResult implements Comparable<WaterfallResult>{
		private double trueObjectiveVal; //for when objective is recomputed after MIP completes.
		private final double objectiveVal;
		private final double[][] I_a_s;
		private final double[] U_k;
		private final int[][] V_i_k;
		private int[] ordering; //ordering[a] specifies the index of the agent in the ath position

		public WaterfallResult(double objectiveVal, double[][] I_a_s, double[] U_k, int[][] V_i_k, double trueObjectiveVal, int[][] r_a_agent) {
			this(objectiveVal, I_a_s, U_k, V_i_k, r_a_agent);
			this.trueObjectiveVal = trueObjectiveVal;
		}

		public WaterfallResult(double objectiveVal, double[][] I_a_s, double[] U_k, int[][] V_i_k, double trueObjectiveVal) {
			this(objectiveVal, I_a_s, U_k, V_i_k);
			this.trueObjectiveVal = trueObjectiveVal;
		}

		public WaterfallResult(double objectiveVal, double[][] I_a_s, double[] U_k, int[][] V_i_k, int[][] r_a_agent) {
			this(objectiveVal, I_a_s, U_k, V_i_k);

			this.ordering = new int[r_a_agent.length];
			for (int a=0; a<r_a_agent.length; a++) {
				for (int agent=0; agent<r_a_agent[a].length; agent++) {
					if (r_a_agent[a][agent] == 1) ordering[a] = agent;
				}
			}
		}
		
		public WaterfallResult(double objectiveVal, double[][] I_a_s, double[] U_k, int[][] V_i_k) {
			this.objectiveVal = objectiveVal;
			this.trueObjectiveVal = objectiveVal; //default true objective value
			this.I_a_s = I_a_s;
			this.U_k = U_k;
			this.V_i_k = V_i_k;
			
			//Default ordering
			this.ordering = new int[I_a_s.length];
			for (int a=0; a<I_a_s.length; a++) {
				ordering[a] = a;
			}			
		}

		public double[][] getI_a_s() {return I_a_s;}
		public double[] getU_k() {return U_k;}
		public int[][] getV_i_k() {return V_i_k;}
		public double getObjectiveVal() {return objectiveVal;}
		public double getTrueObjectiveVal() {return trueObjectiveVal;}
		public int[] getOrdering() {return ordering;}

		public int compareTo(WaterfallResult o) {
			if (o.trueObjectiveVal < trueObjectiveVal) return 1;
			if (o.trueObjectiveVal > trueObjectiveVal) return -1;
			return 0;
		}
		
		public String toString() {
			return "trueObj=" + trueObjectiveVal + ", approxObj=" + objectiveVal + ", I_a_s=" + arrayString(I_a_s);
		}
	}



	//************************************ MAIN METHOD ************************************

	/**
	 * Main method. Solve an instance of the ILP.
	 * @param args
	 */
	public static void main(String[] args) {
		
		
//		  I_a=[-1.0, 562.0, -1.0], Mu_a=[-1.0, 1.0480427046263345, -1.0], I_aPromoted=[-1.0, 0.0, -1.0], 
//		promOK=[false, false, false], hitBudget=[-1, 0, -1], numAgents=3, numSlots=5, numPromotedSlots=0, 
//		sampledMu_a=[0.0, 1.0, 0.0], numSamples=10, I_aDistMean=[-1.0, -1.0, -1.0], I_aDistStdev=[-1.0, -1.0, -1.0], maxImpsPerAgent=674
//		  Solve returned false
//		  status: Infeasible
//		  result=Result:
//		  	obj=Infinity
//		  	sol=[0, 0, 0]
//		  	order=[2, 1, 0]
//		  	slotImpr=[0, 0, 0, 0, 0]Result:
//		  	obj=Infinity
//		  	order=[2, 1, 0]
//		  	slotImpr=[0, 0, 0, 0, 0]
//		  	sol=[0, 0, 0]
//		  err=[9.0, 562.0, 27.0]	pred=[0, 0, 0]	actual=[9, 562, 27]	g=0 d=6 a=1 q=(Query (lioneer,null)) avgPos=[3.0, 1.0480427046263345, 1.0] sampAvgPos=[NaN, 1.0, NaN] bids=[0.4638576360261577, 0.5677197818057882, 1.5461402750119222] imps=[9, 562, 27] order=[2, 1, 0] MIP

		
//		
//		  I_a=[-1.0, -1.0, -1.0, 252.0, -1.0, -1.0], Mu_a=[-1.0, -1.0, -1.0, 3.2738095238095237, -1.0, -1.0], I_aPromoted=[-1.0, -1.0, -1.0, 0.0, -1.0, -1.0], promOK=[false, false, false, false, false, false], 
//		hitBudget=[-1, -1, -1, 0, -1, -1], numAgents=6, numSlots=5, numPromotedSlots=0, sampledMu_a=[1.0, 1.2, 2.2, 3.2, 4.2, 5.0], numSamples=10, I_aDistMean=[-1.0, -1.0, -1.0, -1.0, -1.0, -1.0], 
//		I_aDistStdev=[-1.0, -1.0, -1.0, -1.0, -1.0, -1.0], maxImpsPerAgent=302
//		  Solve returned false
//		  status: Infeasible
//		  result=Result:
//		  	obj=Infinity
//		  	sol=[0, 0, 0, 0, 0, 0]
//		  	order=[4, 3, 0, 1, 2, 5]
//		  	slotImpr=[0, 0, 0, 0, 0]Result:
//		  	obj=Infinity
//		  	order=[4, 3, 0, 1, 2, 5]
//		  	slotImpr=[0, 0, 0, 0, 0]
//		  	sol=[0, 0, 0, 0, 0, 0]
//		  err=[252.0, 252.0, 252.0, 252.0, 69.0, 5.0]	pred=[0, 0, 0, 0, 0, 0]	actual=[252, 252, 252, 252, 69, 5]	g=0 d=7 a=1 q=(Query (lioneer,dvd)) avgPos=[2.2738095238095237, 3.2738095238095237, 4.273809523809524, 1.2738095238095237, 1.0, 5.0] sampAvgPos=[2.2, 3.2, 4.2, 1.2, 1.0, NaN] bids=[0.8871037256070207, 0.7673870025234946, 0.6036604488081566, 1.3486029689734846, 1.5366761665532283, 0.6036604488081565] imps=[252, 252, 252, 252, 69, 5] order=[4, 3, 0, 1, 2, 5] MIP
//		   
		
		
		
		double[] knownI_a = {-1.0, -1.0, -1.0, 252.0, -1.0, -1.0};
		double[] knownMu_a = {-1.0, -1.0, -1.0, 3.2738095238095237, -1.0, -1.0};
		double[] knownI_aPromoted= {-1.0, -1.0, -1.0, 0.0, -1.0, -1.0};
		boolean[] isKnownPromotionEligible = {false, false, false, false, false, false};
		int[] hitBudget = {-1, -1, -1, 0, -1, -1};
		int numSlots = 5;
		int numPromotedSlots = 0;
		boolean integerProgram = false;
		boolean useEpsilon = true;
		double[] knownSampledMu_a = {1.0, 1.2, 2.2, 3.2, 4.2, 5.0};
		int numSamples = 10;
		int maxImpsPerAgent = 302;
		double[] knownI_aDistributionMean = {-1.0, -1.0, -1.0, -1.0, -1.0, -1.0};
		double[] knownI_aDistributionStdev = {-1.0, -1.0, -1.0, -1.0, -1.0, -1.0};
		boolean useRankingConstraints = false;
		boolean multipleSolutions = false;
		double timeoutInSeconds = 3;
		
		
		//Get mu_a values, given impressions
		WaterfallILP ilp = new WaterfallILP(
				knownI_a, knownMu_a, knownI_aPromoted, isKnownPromotionEligible, hitBudget, numSlots, 
				numPromotedSlots, integerProgram, useEpsilon, knownSampledMu_a, 
				numSamples, maxImpsPerAgent, knownI_aDistributionMean, knownI_aDistributionStdev,
				useRankingConstraints, multipleSolutions, timeoutInSeconds);

		WaterfallResult result = ilp.solve();
		double[][] I_a_s = result.getI_a_s();
		double[] U_k = result.getU_k();
		int[][] V_i_k = result.getV_i_k();
		int[] ordering = result.getOrdering();
		System.out.println("I_a_s = " + arrayString(I_a_s));
		System.out.println("U_k = " + Arrays.toString(U_k));
		System.out.println("V_i_k = " + arrayString(V_i_k));
		System.out.println("ordering = " + Arrays.toString(ordering));
		System.out.println("objective = " + result.getObjectiveVal());
		System.out.println("Done");



		//Print each agent's impressions
		double[] agentImps = new double[I_a_s.length];
		for (int a=0; a<I_a_s.length; a++) {
//			for (int s=0; s<=Math.min(a, numSlots-1); s++) {
			for (int s=0; s<numSlots; s++) {
				agentImps[a] += I_a_s[a][s];
			}
		}
		System.out.println("Agent Imps: " + Arrays.toString(agentImps));

		//Get all impressions, given mu_a values and our agent's impressions

		
		
		
		
		
//		double[] knownI_a = {51.0, -1.0, -1.0, -1.0, -1.0};
//		double[] knownMu_a = {1.0, -1.0, -1.0, -1.0, -1.0};
//		double[] knownI_aPromoted= {0.0, -1.0, -1.0, -1.0, -1.0};
//		boolean[] isKnownPromotionEligible = {false, false, false, false, false};
//		int[] hitBudget = {0, -1, -1, -1, -1};
//		int numSlots = 5;
//		int numPromotedSlots = 0;
//		boolean integerProgram = false;
//		boolean useEpsilon = true;
//		double[] knownSampledMu_a = {1.0, 2.0, 1.2857142857142858, 2.2857142857142856, 2.6};
//		int numSamples = 10;
//		int maxImpsPerAgent = 1004;
//		double[] knownI_aDistributionMean = {-1.0, -1.0, -1.0, -1.0, -1.0};
//		double[] knownI_aDistributionStdev = {-1.0, -1.0, -1.0, -1.0, -1.0};
//		boolean useRankingConstraints = false;
//		boolean multipleSolutions = false;
//		double timeoutInSeconds = 3;
//		
//		
//		//Get mu_a values, given impressions
//		WaterfallILP ilp = new WaterfallILP(
//				knownI_a, knownMu_a, knownI_aPromoted, isKnownPromotionEligible, hitBudget, numSlots, 
//				numPromotedSlots, integerProgram, useEpsilon, knownSampledMu_a, 
//				numSamples, maxImpsPerAgent, knownI_aDistributionMean, knownI_aDistributionStdev,
//				useRankingConstraints, multipleSolutions, timeoutInSeconds);
//
//		WaterfallResult result = ilp.solve();
//		double[][] I_a_s = result.getI_a_s();
//		double[] U_k = result.getU_k();
//		int[][] V_i_k = result.getV_i_k();
//		int[] ordering = result.getOrdering();
//		System.out.println("I_a_s = " + arrayString(I_a_s));
//		System.out.println("U_k = " + Arrays.toString(U_k));
//		System.out.println("V_i_k = " + arrayString(V_i_k));
//		System.out.println("ordering = " + Arrays.toString(ordering));
//		System.out.println("objective = " + result.getObjectiveVal());
//		System.out.println("Done");
//
//
//
//		//Print each agent's impressions
//		double[] agentImps = new double[I_a_s.length];
//		for (int a=0; a<I_a_s.length; a++) {
////			for (int s=0; s<=Math.min(a, numSlots-1); s++) {
//			for (int s=0; s<numSlots; s++) {
//				agentImps[a] += I_a_s[a][s];
//			}
//		}
//		System.out.println("Agent Imps: " + Arrays.toString(agentImps));
//
//		//Get all impressions, given mu_a values and our agent's impressions






	}

}
