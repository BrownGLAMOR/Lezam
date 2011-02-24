package models.queryanalyzer.iep;

import java.util.Arrays;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplexModeler;
import ilog.cplex.IloCplex.IntParam;



//NOTE: CPLEX environment variable must be correctly set:
//setenvvar ILOG_LICENSE_FILE /com/ilm/access.ilm

public class WaterfallILP {

	public enum Objective
	{
	  NONE, MINIMIZE_SLOT_DIFF_PROXY
	}
	
	
	boolean INTEGER_PROGRAM;
	boolean USE_EPSILON;
	int numAgents;
	int numSlots;
	int numPromotedSlots;
	int minImpsPerSlot;
	int maxImpsPerSlot;
	int minImpsPerAgent;
	int maxImpsPerAgent;
	int LARGE_CONSTANT = 100000;
	double epsilon = .00001;

	//These are sorted by decreasing squashed bid rank:
	double[] knownI_a; //total impressions for each agent
	double[] knownMu_a; //average position for each agent
	double[] knownI_aPromoted;
	boolean[] isKnownPromotionEligible; //Is it known that a given agent was eligible for promotion? (When in doubt, set false. Setting true will add more constraints to problem.)
	boolean[] isKnownI_a; //(if I_a is not known, a default -1 value is used)
	boolean[] isKnownMu_a;
	boolean[] isKnownI_aPromoted; //(if I_a is not known, a default -1 value is used)
	
	
	private boolean LET_CPLEX_HANDLE_CONDITIONALS = false;
	private boolean USE_PROMOTED_SLOT_CONSTRAINTS = false;
	private Objective DESIRED_OBJECTIVE = Objective.MINIMIZE_SLOT_DIFF_PROXY;
	
	public WaterfallILP(double[] knownI_a, double[] knownMu_a, double[] knownI_aPromoted, boolean[] isKnownPromotionEligible, int numSlots, int numPromotedSlots, boolean integerProgram, boolean useEpsilon) {
		this.INTEGER_PROGRAM = integerProgram;
		this.USE_EPSILON = useEpsilon;
		this.knownI_a = knownI_a;
		this.knownMu_a = knownMu_a;
		this.knownI_aPromoted = knownI_aPromoted;
		this.isKnownPromotionEligible = isKnownPromotionEligible;
		numAgents = knownI_a.length;
		this.numSlots = numSlots;
		this.numPromotedSlots = numPromotedSlots;
		minImpsPerSlot = 0;
		maxImpsPerSlot = 10000;
		minImpsPerAgent = 1;
		maxImpsPerAgent = 10000;

		isKnownI_a = new boolean[numAgents];
		isKnownMu_a = new boolean[numAgents];
		isKnownI_aPromoted = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			if(knownI_a[a] != -1) isKnownI_a[a] = true;
			if(knownMu_a[a] != -1) isKnownMu_a[a] = true;
			if(knownI_aPromoted[a] != -1) isKnownI_aPromoted[a] = true;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("New Waterfall instance created.\n");
		sb.append("  I_a="+Arrays.toString(knownI_a) + ", Mu_a="+Arrays.toString(knownMu_a) + ", I_aPromoted="+Arrays.toString(knownI_aPromoted) + ", ");
		sb.append("promOK="+Arrays.toString(isKnownPromotionEligible) + ", numAgents=" + numAgents + ", numSlots=" + numSlots + ", numPromotedSlots=" + numPromotedSlots);
		System.out.println(sb);
	}

	
	public IloNumVar[][] createImpressionsPerAgentPerSlotVariables(IloCplex cplex) throws IloException {
		IloNumVar[][] I_a_s = new IloNumVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			if (INTEGER_PROGRAM) {
				I_a_s[a] = cplex.intVarArray(a+1, minImpsPerSlot, maxImpsPerSlot); //TODO: (numAgents should be numSlots? a+1?)
			} else {
				I_a_s[a] = cplex.numVarArray(a+1, minImpsPerSlot, maxImpsPerSlot); //TODO: (numAgents should be numSlots? a+1?)				
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


	/**
	 * 
	 * @param I_a, number of total impressions seen by each agent
	 * @return I_a_s, number of impressions seen by each agent in each slot
	 */
	public double[][] solve() {


		double[][] I_a_sDouble = new double[numAgents][Math.max(numAgents, numSlots)]; //[numAgents][numSlots]?
		try {
			IloCplex cplex = new IloCplex();

			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			cplex.setOut(null);
//			cplex.setParam(IntParam.SolnPoolIntensity, 4);
//			cplex.setParam(IntParam.SolnPoolReplace, 2);
//			cplex.setParam(IntParam.SolnPoolCapacity, 1000000);


			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = createImpressionsPerAgentPerSlotVariables(cplex); //(#imps per agent/slot)
			IloIntVar[][] Y_a_k = createConditionalVariables1(cplex);
			IloNumVar[] I_a = createImpressionsPerAgentVariables(cplex);

						
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			if (DESIRED_OBJECTIVE == Objective.NONE) {
				createObjective_indifferentBetweenFeasibleSolutions(cplex, I_a_s);				
			} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF_PROXY) {
				createObjective_minimizeSlotImpressionDifferenceProxy(cplex, I_a_s);				
			}
			

			
			//----------------------------- ADD CONSTRAINTS --------------------------------------------------
			
			

//			if (LET_CPLEX_HANDLE_CONDITIONALS ) {
//
//			//-------------------------HOPEFULLY SPEEDUP---------------------------------------------------------------
//				
//			//----------------------
//			//Using built-in implication: Constraints are sometimes tight
//			//----------------------				
//			for (int a=1; a<numAgents; a++) {
//				for (int k=1; k<=a; k++) {
//					StringBuffer sb = new StringBuffer();					
//					IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
//					IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();
//
//					//Create constraint for this value of $a$ and $k$
//					for (int i=k; i<=a; i++) {
//						sb.append("I_a"+a+"_s"+i + " + ");
//						myCumulativeImps.addTerm(1, I_a_s[a][i]);
//					}
//					sb.append("<= ");
//					for (int opp=k-1; opp<=a-1; opp++) {
//						sb.append("I_a"+opp+"_s"+ (k-1) + " + ");
//						oppCumulativeImps.addTerm(1, I_a_s[opp][k-1]);
//					}
////					System.out.println(sb);
//					cplex.addLe(myCumulativeImps, oppCumulativeImps); //add constraint
//					
//					//If I have anything in the above slot, the above constraint is tight.
//					cplex.add( cplex.ifThen(cplex.ge(I_a_s[a][k-1], 1), cplex.eq(myCumulativeImps, oppCumulativeImps)) );
//				}
//			}
//			//----------------------------------------------------------------------------------------
//			
//			} else {

			
			
			
			
			
			
			/**
			Create constraints
			 */
			//----------------------
			//Advertiser $a$ can't spend more time in slots $k$ through $a$ than 
			//advertisers above him spent in slot $k-1$.
			//(Note we don't have this constraint for the 0th agent or the 0th slot)
			//----------------------
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
//					System.out.println(sb);
					cplex.addLe(myCumulativeImps, oppCumulativeImps); //add constraint
					
				}
			}


			
			//----------------------
			//Constraints are sometimes tight
			//----------------------
//			System.out.println("Constraints are sometimes tight");
			for (int a=1; a<numAgents; a++) {
				for (int k=1; k<=a; k++) {
					IloLinearNumExpr rhs = cplex.linearNumExpr();
					rhs.addTerm(-LARGE_CONSTANT, Y_a_k[a][k-1]);
					rhs.setConstant(LARGE_CONSTANT);
					cplex.addLe(I_a_s[a][k-1], rhs);

					StringBuffer sb = new StringBuffer();
					sb.append("I_a"+a+"_s"+(k-1)+" <= " + LARGE_CONSTANT + " - " + LARGE_CONSTANT + " * Y_a"+a+"_k"+(k-1));
//					System.out.println(sb);
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
					rhsGE.addTerm(-LARGE_CONSTANT, Y_a_k[a][k-1]);
					rhsLE.addTerm(LARGE_CONSTANT, Y_a_k[a][k-1]);
					rhsGE.add(oppCumulativeImps);
					rhsLE.add(oppCumulativeImps);
					cplex.addLe(myCumulativeImps, rhsLE); //add constraint
					cplex.addGe(myCumulativeImps, rhsGE);

					//System.out.println(sb + "(RHS and LHS)");
				}
			}
			////////---------------------

			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

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
//				System.out.println(sb);
				cplex.addEq(totalImps, I_a[a]);
			}




			//-----------------
			//Known total agent impressions.
			//-----------------
			for (int a=0; a<numAgents; a++) { 
				if (isKnownI_a[a]) {
					cplex.addEq(I_a[a], knownI_a[a]);
				}
			}

			
			//---------------
			//Known total agent promoted impressions.
			//---------------
			if (USE_PROMOTED_SLOT_CONSTRAINTS) {
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
			

			//-----------------
			//Known average positions
			//-----------------
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



//sodomka: I don't think we can get this information...
//			//-----------------
//			//Known promoted average positions
//			//-----------------
//			for (int a=0; a<numAgents; a++) {
//				if (isKnownPromotionEligible[a]) { //If we know the agent was eligible for promotion
//					//If we know the agent's average promoted position and promoted imps, or if there were no promoted imps
//					if ( (isKnownMu_aPromoted[a] && isKnownI_aPromoted[a]) || (knownI_aPromoted[a]==0) ) { 
//						IloLinearNumExpr lhs = cplex.linearNumExpr();
//						for (int s=0; s<=Math.min(a, numPromotedSlots-1); s++) {
//							lhs.addTerm(s+1, I_a_s[a][s]);
//						}
//						//TODO: Is there going to be rounding error garbage if muPromoted=-1 and I_aPromoted=0 ??? Let's just play it safe.
//						// (Result should be the same even without the conditional).
//						double rhs = 0;
//						if (isKnownMu_aPromoted[a]) rhs = knownMu_aPromoted[a] * knownI_aPromoted[a];
//
//						if (USE_EPSILON) {
//							cplex.addLe(lhs, rhs + epsilon);
//							cplex.addGe(lhs, rhs - epsilon);	
//						} else {
//							cplex.addEq(lhs, rhs);
//						}
//					}
//				}
//			}










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

		return I_a_sDouble;
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
	private void createObjective_indifferentBetweenFeasibleSolutions(
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
	private void createObjective_minimizeSlotImpressionDifferenceProxy(
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

		//double[] I_a = {-1, 742, -1, -1, -1, -1, -1, -1};
		//double[] mu_a = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};
		int numSlots = 5;
		int numPromotedSlots = 1;

		//int[] I_a = {3198, 202, 3281};
		double[] I_a = {-1, -1, 100};
		double[] mu_a = {1, 2, 2};
		//4.15580737
		
		double[] I_aPromoted = {-1, -1, 20};
		boolean[] isKnownPromotionEligible = {false, false, true};
		
		boolean integerProgram = true;
		boolean useEpsilon = false;
		
		//Get mu_a values, given impressions
		WaterfallILP ilp = new WaterfallILP(I_a, mu_a, I_aPromoted, isKnownPromotionEligible, numSlots, numPromotedSlots, integerProgram, useEpsilon);
		double[][] I_a_s = ilp.solve();
		System.out.println(I_a_s);
		System.out.println("I_a_s = " + arrayString(I_a_s));
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
