package models.queryanalyzer.iep;

import java.util.Arrays;


import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;


/**
 * LP Solver based on Carleton's model.
 * @author sodomka
 *
 */
public class CarletonLP {
	
	public enum Objective
	{
		CLOSE_TO_IMPRESSIONS_UPPER_BOUND,
		MINIMIZE_IMPRESSION_PRIOR_ERROR, //get resulting I_a as close as possible to I_aDistributionMean
	}
	
	private final static boolean USE_EPSILON = false;
	private final static double EPSILON = .0001;
	private final static boolean SUPPRESS_OUTPUT = true;
	private final static boolean SUPPRESS_OUTPUT_MODEL = true;
	private final static double TIMEOUT_IN_SECONDS = 3;
	private final static Objective DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR;
	
	//If minimizing impression error, this is the maximum number of standard deviations away
	//that the predicted number of agent impressions can be from the mean of the prior.
	//(Since our impression predictions won't necessarily be good, it's probably a good idea 
	// to make this a high value)
	private final static double MAX_ERROR = 100000; 
	
	//Predicted agent impressions
	double[] T_aPriorMean;	
	double[] T_aPriorStdev;
	
	
	public CarletonLP(double[] T_aPriorMean, double[] T_aPriorStdev) {
		this.T_aPriorMean = T_aPriorMean;
		this.T_aPriorStdev = T_aPriorStdev;
		System.out.println("mean=" + Arrays.toString(T_aPriorMean) + ", stdev=" + Arrays.toString(T_aPriorStdev));
	}
	
	
	
	
	
	//************************************ MAIN SOLVER METHOD ************************************
	public LPSolution solveIt(int numSlots, double[] avgPos_a, 
			int M, int us, int imp, int[] dropout_a, double bestObj) {
		//System.out.println("solveIt: numSlots=" + numSlots + ", avgPos_a=" + Arrays.toString(avgPos_a) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout_a=" + Arrays.toString(dropout_a) + ", bestObj=" + bestObj);
		
		int numAgents = avgPos_a.length;
		LPSolution solution = null; // The solution that will ultimately be returned.

		try {
			IloCplex cplex = new IloCplex();
			
			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			if (SUPPRESS_OUTPUT) cplex.setOut(null);
			cplex.setParam(IloCplex.DoubleParam.TiLim, TIMEOUT_IN_SECONDS);
			
			
			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = new IloNumVar[numAgents][]; //(#imps per agent/slot)
			IloNumVar[] S_a = cplex.numVarArray(numAgents, 0, M);
			IloNumVar[] T_a = cplex.numVarArray(numAgents, 1, M);
			for (int a=0; a<numAgents; a++) {
				I_a_s[a] = new IloNumVar[a+1]; //cplex.numVarArray(a+1, 0, M);
				for (int s=0; s<=a; s++) {
					if (s < dropout_a[a]) {
						I_a_s[a][s] = cplex.numVar(0, 0);
					} else if (s==dropout_a[a] || s==a){
						I_a_s[a][s] = cplex.numVar(1, M);
					} else {
						I_a_s[a][s] = cplex.numVar(0, M);						
					}
				}
			}
			
			
			
			
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			
			if (DESIRED_OBJECTIVE == Objective.CLOSE_TO_IMPRESSIONS_UPPER_BOUND) {
				addObjective_closeToImpressionsUpperBound(cplex, T_a, bestObj);
			}
			else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR) {
				addObjective_minimizeImpressionPriorError(cplex, T_a, us, T_aPriorMean, T_aPriorStdev, bestObj);
			}
			
			
			//----------------------------- ADD CONSTRAINTS --------------------------------------------------

			

			
			//Our total impressions constraint
			cplex.addEq(T_a[us], imp);
			
			
			//Total number of impressions constraint:
			//Make sure "Total Agent Impressions" variable actually equals the sum of the agent's impressions per slot.
			for (int a=0; a<numAgents; a++) {
				IloLinearNumExpr totalImps = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					totalImps.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImps, T_a[a]);
			}		
			
			
			//Average position constraint
			for (int a=0; a<numAgents; a++) {
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(avgPos_a[a], T_a[a]);
				if (USE_EPSILON) {
					cplex.addLe(lhs, cplex.sum(rhs, EPSILON));
					cplex.addGe(lhs, cplex.sum(rhs, -EPSILON));
				} else {
					cplex.addEq(lhs, rhs);
				}	
			}
			
			
			//Waterfall constraints
			//FIXME: Does this catch all cases? What about with two agents, both w/ integer avgPos.
			//  Seems that we are relying on the objective to maximize total impressions.
			for (int a=0; a<numAgents; a++) {
				for (int s=dropout_a[a]; s<=Math.min(a, numSlots-1); s++) {
					IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
					IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();
					//Create constraint for this value of $a$ and $k$
					for (int i=s; i<=a; i++) {
						myCumulativeImps.addTerm(1, I_a_s[a][i]);
					}
					for (int opp=s; opp<=a; opp++) {
						oppCumulativeImps.addTerm(1, I_a_s[opp][s]);
					}
					cplex.addEq(myCumulativeImps, oppCumulativeImps);
				}
			}
			
			
			//Total slot impressions constraint
			for (int s=0; s<numAgents; s++) {
				IloLinearNumExpr totalImpsInSlot = cplex.linearNumExpr();
				for (int a=s; a<numAgents; a++) {
					totalImpsInSlot.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImpsInSlot, S_a[s]);
			}
			
			
			//Slot totals must be (non-strictly) increasing
			for (int s=1; s<numAgents; s++) {
				cplex.addGe(S_a[s-1], S_a[s]);
			}
			
			
			
			//----------------------------- PRINT AND SOLVE MODEL --------------------------------------------------
			if (!SUPPRESS_OUTPUT_MODEL) System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
			if ( cplex.solve() ) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				cplex.output().println("Objective function = " + cplex.getObjective());

				
				double objectiveVal = cplex.getObjValue();	
				double[] S_aVal = cplex.getValues(S_a);
				double[] T_aVal = cplex.getValues(T_a);
				double[][] I_a_sVal = new double[numAgents][];
				for (int a=0; a<numAgents; a++) {
					I_a_sVal[a] = cplex.getValues(I_a_s[a]);
				}				
				solution = new LPSolution(objectiveVal, I_a_sVal, S_aVal, T_aVal);
				
				
			} else {
				//System.out.println("Solver returned false.");
				//System.out.println("Model: " + cplex.getModel() );
			}
			
			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
		
		
		//System.out.println(solution);
		return solution;
	}
	
	
	
	
	
	
	
	
	
	
	
	

	private void addObjective_closeToImpressionsUpperBound(IloCplex cplex,
			IloNumVar[] T_a, double bestObj) throws IloException {
		int numAgents = T_a.length;
		IloLinearNumExpr allAgentImpressions = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			allAgentImpressions.addTerm(1, T_a[a]);
		}
		cplex.addMaximize(allAgentImpressions);		
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addGe(allAgentImpressions, bestObj);
		}
	}





	/**
	 * This will make the predicted agent impressions as close to our priors as possible. 
	 * @param cplex
	 * @param T_a
	 * @throws IloException
	 */
	private void addObjective_minimizeImpressionPriorError(IloCplex cplex, IloNumVar[] T_a, int us, double[] T_aPriorMean, double[] T_aPriorStdev, double bestObj) throws IloException {
		
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		int numAgents = T_a.length;
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (a != us) {
				cplex.addLe(T_a[a], cplex.sum( T_aPriorMean[a] , cplex.prod(T_aPriorStdev[a], I_aError[a])  )  );
				cplex.addGe(T_a[a], cplex.diff( T_aPriorMean[a] , cplex.prod(T_aPriorStdev[a], I_aError[a])  )  );
				relevantErrors.addTerm(1, I_aError[a]);
			}
		}
		cplex.addMinimize(relevantErrors);
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(relevantErrors, bestObj);
		}
	}
	
	
	
	
	
	
	
	
	
	

	
	
	
	
	public static void main(String[] args) {
		
		
		
//		int numSlots = 5;
//		double[] avgPos_a = {1, 1.5, 1.5}; 
//		int M = 1500;
//		int us = 2;
//		int imp = 1500;
//		int[] dropout_a = {0, 0, 0};
//		double bestObj = -1;
//		CarletonLP.solveIt(numSlots, avgPos_a, M, us, imp, dropout_a, bestObj);
		
		
//		int numSlots = 5;
//		double[] avgPos_a = {1, 2, 1.87800454, 1.81100666, 3.09521291}; 
//		int M = 5106;
//		int us = 2;
//		int imp = 2205;
//		int[] dropout_a = {0, 1, 0, 0, 1};
//		double bestObj = -1;
//		CarletonLP.solveIt(numSlots, avgPos_a, M, us, imp, dropout_a, bestObj);
		
		
//		int numSlots = 5;
////		double[] avgPos_a = {1, 1.504146, 2.126708, 3.035672, 3.461909, 3.918221, 3.930876, 4}; 
//		double[] avgPos_a = {1, 1.50414594, 2.12670807, 3.03567182, 3.46190935, 3.91822095, 3.93087558, 4};
//		int M = 1037;
//		int us = 2;
//		int imp = 805;
//		int[] dropout_a = {0, 0, 0, 0, 0, 1, 1, 2};
//		double bestObj = -1;
//		CarletonLP.solveIt(numSlots, avgPos_a, M, us, imp, dropout_a, bestObj);

		
		int numSlots = 5;
		double[] avgPos_a = {1.0, 2.0};
		int M = 1300;
		int us = 0;
		int imp = 300;
		int[] dropout_a = {0, 1};
		double bestObj = -1;
		double[] T_aPriorMean = {200, 200};
		double[] T_aPriorStdev = {50, 50};
		CarletonLP carletonLP = new CarletonLP(T_aPriorMean, T_aPriorStdev);
		LPSolution sol = carletonLP.solveIt(numSlots, avgPos_a, M, us, imp, dropout_a, bestObj);
		System.out.println(sol);
		
	}
	
}
