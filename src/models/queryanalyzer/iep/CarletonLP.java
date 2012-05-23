package models.queryanalyzer.iep;

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
	
	
	
	private final static boolean USE_EPSILON = true;
	private final static double EPSILON = .0001;
	
	
	
	
	//************************************ MAIN SOLVER METHOD ************************************
	public static LPSolution solveIt(int numSlots, double[] avgPos_a, 
			int M, int us, int imp, int[] dropout_a, double bestObj) {

		int numAgents = avgPos_a.length;
		LPSolution solution = null; // The solution that will ultimately be returned.

		try {
			IloCplex cplex = new IloCplex();
			
			
			
			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = new IloNumVar[numAgents][]; //(#imps per agent/slot)
			IloNumVar[] S_a = cplex.numVarArray(numAgents, 0, M);
			IloNumVar[] T_a = cplex.numVarArray(numAgents, 0, M);
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
			IloLinearNumExpr allAgentImpressions = cplex.linearNumExpr();
			for (int a=0; a<numAgents; a++) {
				allAgentImpressions.addTerm(1, T_a[a]);
			}
			cplex.addMaximize(allAgentImpressions);
			
			
			
			//----------------------------- ADD CONSTRAINTS --------------------------------------------------

			
			//Must be better than previously best objective
			if (bestObj != -1) {
				cplex.addGe(allAgentImpressions, bestObj);
			}
			
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
				System.out.println("Solver returned false.");
				//System.out.println("Model: " + cplex.getModel() );
			}
			
			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
		
		
		System.out.println(solution);
		return solution;
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
		
		
		int numSlots = 5;
		double[] avgPos_a = {1, 1.504146, 2.126708, 3.035672, 3.461909, 3.918221, 3.930876, 4}; 
		int M = 1037;
		int us = 2;
		int imp = 805;
		int[] dropout_a = {0, 0, 0, 0, 0, 1, 1, 2};
		double bestObj = -1;
		CarletonLP.solveIt(numSlots, avgPos_a, M, us, imp, dropout_a, bestObj);
		
	}
	
}
