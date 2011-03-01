package models.queryanalyzer.iep;

import java.util.Arrays;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;



//NOTE: CPLEX environment variable must be correctly set:
//setenvvar ILOG_LICENSE_FILE /com/ilm/access.ilm

public class WaterfallILP {
	
	//************************************ FIELDS ************************************
	
	// The objective function can be of any of these types.
	public enum Objective
	{
	  NONE, MINIMIZE_SLOT_DIFF_PROXY, SPREAD_SAMPLES_LINEAR
	}
		
	boolean INTEGER_PROGRAM;
	boolean USE_EPSILON;
	int numAgents;
	int numSlots;
	int numPromotedSlots;

	//Parameters that should eventually be set by the constructor
	int minImpsPerSlot = 0;
	int maxImpsPerSlot = 10000;
	int minImpsPerAgent = 1;
	int maxImpsPerAgent = 10000;
	int LARGE_CONSTANT_Y_a_k = 100000;
	double epsilon = .00001;

	//These are sorted by decreasing squashed bid rank:
	double[] knownI_a; //total impressions for each agent
	double[] knownMu_a; //average position for each agent
	double[] knownI_aPromoted;
	boolean[] isKnownPromotionEligible; //Is it known that a given agent was eligible for promotion? (When in doubt, set false. Setting true will add more constraints to problem.)
	boolean[] isKnownI_a; //(if I_a is not known, a default -1 value is used)
	boolean[] isKnownMu_a;
	boolean[] isKnownI_aPromoted; //(if I_a is not known, a default -1 value is used)

	double[] knownSampledMu_a; //sampled average position
	boolean[] isKnownSampledMu_a;
	int numSamples;	

	private boolean RETURN_MULTIPLE_SOLUTIONS = false;
	private boolean LET_CPLEX_HANDLE_CONDITIONALS = false;
	private boolean USE_PROMOTED_SLOT_CONSTRAINTS = false;
	private Objective DESIRED_OBJECTIVE = Objective.SPREAD_SAMPLES_LINEAR;
	
	
	
	
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
			boolean[] isKnownPromotionEligible, int numSlots, int numPromotedSlots, 
			boolean integerProgram, boolean useEpsilon) {
		this.INTEGER_PROGRAM = integerProgram;
		this.USE_EPSILON = useEpsilon;
		this.knownI_a = knownI_a;
		this.knownMu_a = knownMu_a;
		this.knownI_aPromoted = knownI_aPromoted;
		this.isKnownPromotionEligible = isKnownPromotionEligible;
		numAgents = knownI_a.length;
		this.numSlots = numSlots;
		this.numPromotedSlots = numPromotedSlots;

		//By default, sampled average positions are not given.
		this.knownSampledMu_a = new double[numAgents];
		Arrays.fill(this.knownSampledMu_a, -1);
		
		isKnownI_a = new boolean[numAgents];
		isKnownMu_a = new boolean[numAgents];
		isKnownI_aPromoted = new boolean[numAgents];
		isKnownSampledMu_a = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			if(knownI_a[a] != -1) isKnownI_a[a] = true;
			if(knownMu_a[a] != -1) isKnownMu_a[a] = true;
			if(knownI_aPromoted[a] != -1) isKnownI_aPromoted[a] = true;
			if(knownSampledMu_a[a] != -1) isKnownSampledMu_a[a] = true;
		}		
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
			boolean[] isKnownPromotionEligible, int numSlots, int numPromotedSlots, 
			boolean integerProgram, boolean useEpsilon, 
			double[] knownSampledMu_a, int numSamples) {
		this(knownI_a, knownMu_a, knownI_aPromoted, isKnownPromotionEligible,
				numSlots, numPromotedSlots, integerProgram, useEpsilon);
		this.knownSampledMu_a = knownSampledMu_a;
		this.isKnownSampledMu_a = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			if(knownSampledMu_a[a] != -1) isKnownSampledMu_a[a] = true;
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
		sb1.append("Solving new Waterfall instance.\n");
		sb1.append("  I_a="+Arrays.toString(knownI_a) + ", Mu_a="+Arrays.toString(knownMu_a) + ", I_aPromoted="+Arrays.toString(knownI_aPromoted) + ", ");
		sb1.append("promOK="+Arrays.toString(isKnownPromotionEligible) + ", numAgents=" + numAgents + ", numSlots=" + numSlots + ", numPromotedSlots=" + numPromotedSlots + ", ");
		sb1.append("sampledMu_a="+Arrays.toString(knownSampledMu_a) +", numSamples=" + numSamples);
		System.out.println(sb1);

		
		
		
		double[][] I_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
		double[] U_kDouble = new double[numSamples];
		
		try {			
			IloCplex cplex = new IloCplex();

			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			cplex.setOut(null);
			
			if (RETURN_MULTIPLE_SOLUTIONS) {
				cplex.setParam(IntParam.SolnPoolIntensity, 4);
				cplex.setParam(IntParam.SolnPoolReplace, 2);
				cplex.setParam(IntParam.SolnPoolCapacity, 1000000);
			}


			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = createImpressionsPerAgentPerSlotVariables(cplex); //(#imps per agent/slot)
			IloIntVar[][] Y_a_k = createConditionalVariables1(cplex);
			IloNumVar[] I_a = createImpressionsPerAgentVariables(cplex);

			IloNumVar[] U_k = createImpressionSamplingVariables(cplex); //which impression did sample k happen on?
			IloIntVar[][] d_a_k = createDroppedOutVariables(cplex); //did agent a drop out of sample k?
			IloIntVar[][] h_a_k = createHowDroppedOutVariables(cplex); //is the sample before or after the agent's impression interval?
			IloIntVar[][] p_a_k = createPositionVariables(cplex); //position of agent a at sample k?
			IloNumVar[] Istart_a = createStartingImpressionVariables(cplex); //when did agent a see its first impression?
			IloNumVar[] Iend_a = createEndingImpressionVariables(cplex); //when did agent a see its last impression?
			
			
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			if (DESIRED_OBJECTIVE == Objective.NONE) {
				addObjective_indifferentBetweenFeasibleSolutions(cplex, I_a_s);				
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF_PROXY) {
				addObjective_minimizeSlotImpressionDifferenceProxy(cplex, I_a_s);				
			} else if (DESIRED_OBJECTIVE == Objective.SPREAD_SAMPLES_LINEAR) {
				addObjective_spreadSamplesLinear(cplex, U_k, I_a_s);
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
			addConstraint_totalImpressionsKnown(cplex, I_a);

			// If we know any agents' total promoted impressions, these must be satisfied.
			if (USE_PROMOTED_SLOT_CONSTRAINTS) {
				addConstraint_totalPromotedImpressionsKnown(cplex, I_a_s);
			}

			// Agent impressions per slot must result in the known (exact) average positions.
			addConstraint_exactAveragePositionsKnown(cplex, I_a_s, I_a);

			//---Sampling constraints---
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
			
			
			//----------------------------- PRINT AND SOLVE MODEL --------------------------------------------------

			/**
			 * Print and solve model
			 */
//			System.out.println("Constraints added.");
//			System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
			if ( cplex.solve() ) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				cplex.output().println("Objective function = " + cplex.getObjective());

				//Create double array to return
				for (int a=0; a<numAgents; a++) {
					for (int s=0; s<=a; s++) {
						I_a_sDouble[a][s] = cplex.getValue(I_a_s[a][s]);
					}
				}
				
				for (int k=0; k<numSamples; k++) {
					U_kDouble[k] = cplex.getValue(U_k[k]);
				}
				
//				System.out.println("BEST SOLUTION:\n" + arrayString(I_a_sDouble));

				//Get total imps per agent
				StringBuffer sb = new StringBuffer();
				sb.append("Total imps per agent: ");
				for (int a=0; a<numAgents;a++) {
					sb.append(cplex.getValue(I_a[a]) + " ");
				}
//				System.out.println(sb);

			} else {
				System.out.println("Solve returned false");
			}





//			/**
//			* Find multiple solutions
//			*/
//			int lastNumSols = 0;
//			int numSols = 50;
//			while(cplex.getSolnPoolNsolns() < numSols) {
//			cplex.populate();
//			if(lastNumSols == cplex.getSolnPoolNsolns()) {
//			break;
//			}
//			lastNumSols = cplex.getSolnPoolNsolns();
//			}

//			double[][][] solution = new double[cplex.getSolnPoolNsolns()][][];
//			for(int i = 0; i < solution.length; i++) {
//			solution[i] = new double[numAgents][numAgents];
//			}
//			for(int i = 0; i < cplex.getSolnPoolNsolns(); i++) {
//			System.out.println("Solution " + i + " value  = " + cplex.getObjValue(i));
//			//Create double array to return
//			for (int a=0; a<numAgents; a++) {
//			for (int s=0; s<=a; s++) {
//			solution[i][a][s] = cplex.getValue(I_a_s[a][s],i);
//			}
//			}
//			}
//			System.out.println("HERE ARE ALL SOLUTIONS:\n" + arrayString(solution));
//			//----------------------

			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}

		return new WaterfallResult(I_a_sDouble, U_kDouble);
	}



	
	
	
	





	//************************************ CREATE DECISION VARIABLES ************************************


	public IloNumVar[][] createImpressionsPerAgentPerSlotVariables(IloCplex cplex) throws IloException {
		IloNumVar[][] I_a_s = new IloNumVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			if (INTEGER_PROGRAM) {
				//Agents have a variable for every possible slot they could have been in.
				//(They don't have variables for slots that were below where they started).
				I_a_s[a] = cplex.intVarArray(a+1, minImpsPerSlot, maxImpsPerSlot); 
			} else {
				I_a_s[a] = cplex.numVarArray(a+1, minImpsPerSlot, maxImpsPerSlot);
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

	private IloNumVar[] createImpressionSamplingVariables(IloCplex cplex) throws IloException {
		IloNumVar[] U_k;
		if (INTEGER_PROGRAM) {
			//TODO: If U_k can be beyond the number of viewed impressions (e.g. after all agents
			//have dropped out), we may want to change this upper bound.
			U_k = cplex.intVarArray(numSamples, 1, maxImpsPerSlot);
		} else {
			U_k = cplex.numVarArray(numSamples, 1, maxImpsPerSlot);
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
			Istart_a = cplex.intVarArray(numAgents, 1, maxImpsPerSlot); 
		} else {
			Istart_a = cplex.numVarArray(numAgents, 1, maxImpsPerSlot); 				
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
			Iend_a = cplex.intVarArray(numAgents, 1, maxImpsPerSlot); 
		} else {
			Iend_a = cplex.numVarArray(numAgents, 1, maxImpsPerSlot); 				
		}
		return Iend_a;
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
		IloLinearNumExpr slot1Imps = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			slot1Imps.addTerm( (numSlots-1) , I_a_s[a][0]);
		}
		IloLinearNumExpr otherSlotImps = cplex.linearNumExpr();
		for (int a=1; a<numAgents; a++) {
			for (int s=1; s<=Math.min(a, numSlots-1); s++) {
				otherSlotImps.addTerm(1, I_a_s[a][s]);
			}
		}
		cplex.addMinimize(cplex.diff(slot1Imps, otherSlotImps));
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
		int LARGE_CONSTANT = maxImpsPerSlot + 1;
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
		int LARGE_CONSTANT = maxImpsPerSlot + 1;

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


	//************************************ RESULT DATA STRUCTURE ************************************
	public class WaterfallResult {
		private final double[][] I_a_s;
		private final double[] U_k;
		
		public WaterfallResult(double[][] I_a_s, double[] U_k) {
			this.I_a_s = I_a_s;
			this.U_k = U_k;
		}
		
		public double[][] getI_a_s() {return I_a_s;}
		public double[] getU_k() {return U_k;}
	}


	
	//************************************ MAIN METHOD ************************************
	
	/**
	 * Main method. Solve an instance of the ILP.
	 * @param args
	 */
	public static void main(String[] args) {

//		double[] I_a = {4, 6, 9, 5, 12};
//		double[] mu_a = {-1, -1, -1, -1, -1};
		//double[] I_a = {-1, -1, -1, -1, 12};
		//double[] mu_a = {1, , -1, -1, -1};

		//double[] I_a = {-1, 4};
		//double[] mu_a = {1.0, 1.5};

		//double[] I_a = {742, 742, 556, 589, 222, 520, 186, 153};
		//double[] mu_a = {-1, -1, -1, -1, -1, -1, -1, -1};

		//  double[] I_a = {742, 742, 556, 589, 222, 520, 186, 153};
		double[] I_a = {-1, -1, -1, 589, -1, -1, -1, -1};
		//  double[] mu_a = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};
		double[] mu_a = {-1, -1, -1, 3.94397284, -1, -1, -1, -1};
		double[] I_aPromoted = {-1, -1, -1, -1, -1, -1, -1, -1};
		boolean[] isKnownPromotionEligible = {false, false, false, false, false, false, false, false};
		double[] knownSampledMu_a = {1.0, 2.0, 3.0, 3.75, 5, 4, 4.5, 5};
		int numSamples = 5;

		
//		//  double[] I_a = {742, 742, 556, 589, 222, 520, 186, 153};
//		double[] I_a = {100, -1, -1, -1, -1, -1};
//		//  double[] mu_a = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};
//		double[] mu_a = {-1, -1, -1, -1, -1, -1};
//		double[] I_aPromoted = {-1, -1, -1, -1, -1, -1};
//		boolean[] isKnownPromotionEligible = {false, false, false, false, false, false};
//		double[] knownSampledMu_a = {1.0, 2.0, 3.0, 4.0, 5, 5}; 
//		int numSamples = 5;


//		//  double[] I_a = {742, 742, 556, 589, 222, 520, 186, 153};
//		double[] I_a = {100, -1, -1, -1, -1, -1};
//		//  double[] mu_a = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};
//		double[] mu_a = {-1, -1, -1, -1, -1, -1};
//		double[] I_aPromoted = {-1, -1, -1, -1, -1, -1};
//		boolean[] isKnownPromotionEligible = {false, false, false, false, false, false};
//		double[] knownSampledMu_a = {1.0, 2.0, 3.0, 4.0, 5, 5}; 
//		int numSamples = 5;

		
		
		

//		double[] I_a = {-1, 80, -1};
//		double[] mu_a = {-1, 1.5, -1};		
//		double[] I_aPromoted = {-1, -1, -1};
//		boolean[] isKnownPromotionEligible = {false, false, false};
//		double[] knownSampledMu_a = {1.0, 2.0, 2.5}; 
//		int numSamples = 4;

//		double[] I_a = {80};
//		double[] mu_a = {1.0};		
//		double[] I_aPromoted = {-1};
//		boolean[] isKnownPromotionEligible = {false};
//		double[] knownSampledMu_a = {1.0}; 
//		int numSamples = 3;

		
		int numSlots = 5;
		int numPromotedSlots = 0;
		boolean integerProgram = true;
		boolean useEpsilon = false;
		
		//Get mu_a values, given impressions
		WaterfallILP ilp = new WaterfallILP(I_a, mu_a, I_aPromoted, isKnownPromotionEligible, numSlots, numPromotedSlots, integerProgram, useEpsilon, knownSampledMu_a, numSamples);
		WaterfallResult result = ilp.solve();
		double[][] I_a_s = result.getI_a_s();
		double[] U_k = result.getU_k();
		System.out.println(I_a_s);
		System.out.println("I_a_s = " + arrayString(I_a_s));
		System.out.println("U_k = " + Arrays.toString(U_k));
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






	}

}
