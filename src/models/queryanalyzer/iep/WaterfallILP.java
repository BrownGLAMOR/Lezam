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

	boolean INTEGER_PROGRAM = true;
	int numAgents;
	int numSlots;
	int minImpsPerSlot;
	int maxImpsPerSlot;
	int minImpsPerAgent;
	int maxImpsPerAgent;
	int LARGE_CONSTANT = 10000;
	double epsilon = .001;
	
	double[] knownI_a;
	double[] knownMu_a;
	boolean[] isKnownI_a;
	boolean[] isKnownMu_a;

	public WaterfallILP(double[] knownI_a, double[] knownMu_a, int numSlots) {
		this.knownI_a = knownI_a;
		this.knownMu_a = knownMu_a;
		numAgents = knownI_a.length;
		this.numSlots = numSlots;
		minImpsPerSlot = 0;
		maxImpsPerSlot = 1000;
		minImpsPerAgent = 1;
		maxImpsPerAgent = 1000;

		isKnownI_a = new boolean[numAgents];
		isKnownMu_a = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			if(knownI_a[a] != -1) isKnownI_a[a] = true;
			if(knownMu_a[a] != -1) isKnownMu_a[a] = true;
		}
	}


	public IloNumVar[][] createDecisionVariables1(IloCplex cplex) throws IloException {
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

	public IloIntVar[][] createDecisionVariables2(IloCplex cplex) throws IloException {
		IloIntVar[][] Y_a_k = new IloIntVar[numAgents][];
		for (int a=0; a<numAgents; a++) {
			Y_a_k[a] = cplex.intVarArray(a+1, 0, 1); //TODO: how many do we want for each agent?
		}
		return Y_a_k;
	}

	public IloNumVar[] createDecisionVariables3(IloCplex cplex) throws IloException {
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


		double[][] I_a_sDouble = new double[numAgents][numAgents]; //[numAgents][numSlots]?
		try {
			IloCplex cplex = new IloCplex();
//			cplex.setOut(null);
//			cplex.setParam(IntParam.SolnPoolIntensity, 4);
//			cplex.setParam(IntParam.SolnPoolReplace, 2);
//			cplex.setParam(IntParam.SolnPoolCapacity, 1000000);


			//Create I_a_s decision variables (#imps per agent/slot)
			IloNumVar[][] I_a_s = createDecisionVariables1(cplex);
			IloIntVar[][] Y_a_k = createDecisionVariables2(cplex);
			IloNumVar[] I_a = createDecisionVariables3(cplex);

			/**
			Create objective function
			 */
			//Slot1Imps
			//OtherSlotImps
			//distance = (numSlots-1)*slot1Imps - sum(otherSlotImps)
			//minimize distance
			IloLinearNumExpr slot1Imps = cplex.linearNumExpr();
			for (int a=0; a<numAgents; a++) {
				slot1Imps.addTerm( (numSlots-1) , I_a_s[a][0]);
			}
			IloLinearNumExpr otherSlotImps = cplex.linearNumExpr();
			for (int a=1; a<numAgents; a++) {
				for (int s=1; s<Math.min(a, numSlots); s++) {
					otherSlotImps.addTerm(1, I_a_s[a][s]);
				}
			}
			cplex.addMinimize(cplex.diff(slot1Imps, otherSlotImps));
			
			
//			IloLinearNumExpr expr = cplex.linearNumExpr();
//			for (int a=0; a<numAgents; a++) {
//				for (int s=0; s<=a; s++) {
//					expr.addTerm(1, I_a_s[a][s]); //TODO: I just made this up as a placeholder
//				}
//			}
//			cplex.addMaximize(expr);


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
					System.out.println(sb);
					cplex.addLe(myCumulativeImps, oppCumulativeImps); //add constraint
				}
			}


			//----------------------
			//Constraints are sometimes tight
			//----------------------
			System.out.println("Constraints are sometimes tight");
			for (int a=1; a<numAgents; a++) {
				for (int k=1; k<=a; k++) {
					IloLinearNumExpr rhs = cplex.linearNumExpr();
					rhs.addTerm(-LARGE_CONSTANT, Y_a_k[a][k-1]);
					rhs.setConstant(LARGE_CONSTANT);
					cplex.addLe(I_a_s[a][k-1], rhs);

					StringBuffer sb = new StringBuffer();
					sb.append("I_a"+a+"_s"+(k-1)+" <= " + LARGE_CONSTANT + " - " + LARGE_CONSTANT + " * Y_a"+a+"_k"+(k-1));
					System.out.println(sb);
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



			//----------------------
			//Total number of impressions constraint
			//----------------------
			for (int a=0; a<numAgents; a++) {
				StringBuffer sb = new StringBuffer();	
				IloLinearNumExpr totalImps = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
					sb.append("I_a"+a+"_s"+s+" + ");
					totalImps.addTerm(1, I_a_s[a][s]);
				}
				sb.append("= " + "I_a" + I_a[a]);
				System.out.println(sb);
				cplex.addEq(totalImps, I_a[a]);
			}




			//-----------------
			//Known total impressions
			//-----------------
			for (int a=0; a<numAgents; a++) { 
				if (isKnownI_a[a]) {
					cplex.addEq(I_a[a], knownI_a[a]);
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
					cplex.addEq(lhs, rhs);
					//cplex.addLe(lhs, cplex.sum(rhs, epsilon));
					//cplex.addGe(lhs, cplex.sum(rhs, -epsilon));
				}
			}


			/**
			 * Print and solve model
			 */
			System.out.println("Constraints added.");
			System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
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
				
				
				//Get total imps per agent
				StringBuffer sb = new StringBuffer();
				sb.append("Total imps per agent: ");
				for (int a=0; a<numAgents;a++) {
					sb.append(cplex.getValue(I_a[a]) + " ");
				}
				System.out.println(sb);
				
			} else {
				System.out.println("Solve returned false");
			}





//			/**
//			 * Find multiple solutions
//			 */
//			int lastNumSols = 0;
//			int numSols = 50;
//			while(cplex.getSolnPoolNsolns() < numSols) {
//				cplex.populate();
//				if(lastNumSols == cplex.getSolnPoolNsolns()) {
//					break;
//				}
//				lastNumSols = cplex.getSolnPoolNsolns();
//			}
//
//			double[][][] solution = new double[cplex.getSolnPoolNsolns()][][];
//			for(int i = 0; i < solution.length; i++) {
//				solution[i] = new double[numAgents][numAgents];
//			}
//			for(int i = 0; i < cplex.getSolnPoolNsolns(); i++) {
//				System.out.println("Solution " + i + " value  = " + cplex.getObjValue(i));
//				//Create double array to return
//				for (int a=0; a<numAgents; a++) {
//					for (int s=0; s<=a; s++) {
//						solution[i][a][s] = cplex.getValue(I_a_s[a][s],i);
//					}
//				}
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
		int numSlots = 3;
		
		double[] I_a = {-1, 20, -1};
		double[] mu_a = {1, 1.5, 2.5};
		
		
		//Get mu_a values, given impressions
		WaterfallILP ilp = new WaterfallILP(I_a, mu_a, numSlots);
		double[][] I_a_s = ilp.solve();
		System.out.println(I_a_s);
		System.out.println("I_a_s = " + arrayString(I_a_s));
		System.out.println("Done");
		
		
		
		//Print each agent's impressions
		double[] agentImps = new double[I_a_s.length];
		for (int a=0; a<I_a_s.length; a++) {
			for (int s=0; s<numSlots; s++) {
				agentImps[a] += I_a_s[a][s];
			}
		}
		System.out.println("Agent Imps: " + Arrays.toString(agentImps));
		
		//Get all impressions, given mu_a values and our agent's impressions
		
		
		
		
		

	}

}
