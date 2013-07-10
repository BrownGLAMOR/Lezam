package models.queryanalyzer.riep.iep.mip;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.Arrays;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.riep.iep.IEResult;

public abstract class AbstractImpressionEstimatorSimpleMIP extends AbstractImpressionEstimatorMIP {
	protected final static boolean SUPPRESS_OUTPUT = true;
	private static double TIMEOUT_IN_SECONDS = 0;
	IloCplex cplex;
	
	int us;
	
	public AbstractImpressionEstimatorSimpleMIP(AbstractQAInstance inst, double timeout, IloCplex cplex){
		super(inst);
		if(timeout>0){
			TIMEOUT_IN_SECONDS = timeout;
		}
		this.cplex = cplex;
		//this.us = inst.getAgentIndex();
		
	}

	@Override
	public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}

	
	@Override
	public IEResult search(int[] order) {
	    try {
			//IloCplex cplex = new IloCplex();
	    	cplex.clearModel();
			
			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			if (SUPPRESS_OUTPUT) cplex.setOut(null);
			
			if(TIMEOUT_IN_SECONDS>0) cplex.setParam(IloCplex.DoubleParam.TiLim, TIMEOUT_IN_SECONDS);
			
			CPlexVariables vars = makeModel(cplex, order);
			long t0 = System.currentTimeMillis();
			
			//----------------------------- PRINT AND SOLVE MODEL --------------------------------------------------
			//System.out.println("CarletonLP Objective: " + DESIRED_OBJECTIVE);
			if (!SUPPRESS_OUTPUT) System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
			if ( cplex.solve() ) {
			    long runtime = System.currentTimeMillis() - t0;
			      
			    cplex.output().println("Runtime: "+runtime/1000.0);
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				//cplex.output().println("Objective function = " + cplex.getObjective());

				
				double objectiveVal = cplex.getObjValue();	
				double[] S_aVal = cplex.getValues(vars.S_a);
				double[] T_aVal = cplex.getValues(vars.T_a);
				double[][] I_a_sVal = new double[vars.I_a_s.length][];
				for (int a=0; a<I_a_sVal.length; a++) {
					I_a_sVal[a] = cplex.getValues(vars.I_a_s[a]);
				}			
				
				cplex.output().println("obj: " + objectiveVal);
				cplex.output().println("S_a: " + Arrays.toString(S_aVal));
				cplex.output().println("T_a: " + Arrays.toString(T_aVal));
				cplex.output().println();
				for (int a=0; a<vars.I_a_s.length; a++) {
					cplex.output().println("I_" + a + "_s: " + Arrays.toString(cplex.getValues(vars.I_a_s[a])));				
				}
				cplex.output().println();
				for (int a=0; a<vars.D_a_s.length; a++) {
					//System.out.println(D_a_s[a]);
					cplex.output().println("D_" + a + "_s: " + Arrays.toString(cplex.getValues(vars.D_a_s[a])));				
				}
				
				
				int[] impsPerAgent = makeFilterArray(vars.T_a, order, cplex);
		        int[] impsPerSlot = makeFilterArray(vars.S_a, _inst.getNumSlots(), cplex);

				return new IEResult(cplex.getObjValue(), impsPerAgent, order.clone(), impsPerSlot, _inst.getAgentNames()); 
				//solution = new LPSolution(objectiveVal, I_a_sVal, S_aVal, T_aVal);
			} else {
				System.out.println("Solver returned false.");
				//System.out.println("Model: " + cplex.getModel() );
			}
			
			//cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
	    
	    
	    return null;
	}
	
	protected abstract CPlexVariables makeModel(IloCplex cplex, int[] order) throws IloException;

	protected CPlexVariables makeModelVariables(IloCplex cplex, int advertisers, int M) throws IloException{
		//System.out.println("CJC M: "+M);
		IloIntVar[][] D_a_s = new IloIntVar[advertisers][]; 
		IloNumVar[][] I_a_s = new IloNumVar[advertisers][]; //(#imps per agent/slot)
		IloNumVar[] S_a = cplex.numVarArray(advertisers, 0, M);
		IloNumVar[] T_a = cplex.numVarArray(advertisers, 1, M);
		
		for (int a=0; a<advertisers; a++) {
			I_a_s[a] = cplex.numVarArray(a+1, 0, M);
			D_a_s[a] = cplex.boolVarArray(a+1);
		}
		
		return new CPlexVariables(D_a_s, I_a_s, S_a, T_a);
	}
	
	protected void postDropoutConstraints(IloCplex cplex, CPlexVariables vars, int advertisers, int M, int[] minDropOut, int[] maxDropOut) throws IloException {
		//dropout constraints
		for (int a=0; a<advertisers; a++) {
			cplex.addGe(vars.D_a_s[a][maxDropOut[a]], 1.0);
			if(minDropOut[a]-1 >= 0){
				cplex.addLe(vars.D_a_s[a][minDropOut[a]-1], 0);
			}
			
			for(int s=0; s<=a; s++) {
				IloLinearNumExpr expr= cplex.linearNumExpr();
				expr.addTerm(M, vars.D_a_s[a][s]);
				cplex.addGe(expr, vars.I_a_s[a][s]);
			}
			for(int s=1; s<=a; s++){
				cplex.addLe(vars.D_a_s[a][s-1], vars.D_a_s[a][s]);
			}
			
		}
		
		
		
		
	}
	

	protected void postCascadeConstraints(IloCplex cplex, CPlexVariables vars, int advertisers, int slots) throws IloException {
		//Waterfall constraints
		
		for (int a=0; a<advertisers; a++) {
			for (int s=0; s<=a; s++) {
				IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
				IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();
				//Create constraint for this value of $a$ and $k$
				for (int i=s; i<=a; i++) {
					myCumulativeImps.addTerm(1, vars.I_a_s[a][i]);
				}
				for (int opp=s; opp<=a; opp++) {
					oppCumulativeImps.addTerm(1, vars.I_a_s[opp][s]);
				}
				cplex.add(cplex.ifThen(cplex.ge(vars.D_a_s[a][s],1.0), cplex.eq(myCumulativeImps, oppCumulativeImps)) );
			}
		}
		
		//Total number of impressions constraint:
		//Make sure "Total Agent Impressions" variable actually equals the sum of the agent's impressions per slot.
		for (int a=0; a<advertisers; a++) {
			IloLinearNumExpr totalImps = cplex.linearNumExpr();
			for (int s=0; s<=Math.min(a, slots-1); s++) {
				totalImps.addTerm(1, vars.I_a_s[a][s]);
			}
			cplex.addEq(totalImps, vars.T_a[a]);
		}		
		//BETSY CHANGES
		//Total slot impressions constraint
		//System.out.println("ID And Imps: "+getInstance().getImpressions()+" "+getInstance().getAgentIndex());
		//IloLinearNumExpr exactImps = cplex.linearNumExpr();
		//exactImps.addTerm(1,vars.T_a[getInstance().getAgentIndex()]);
		//cplex.addEq((double)getInstance().getImpressions(),vars.T_a[getInstance().getAgentIndex()]);
		
		for (int s=0; s<advertisers; s++) {
			IloLinearNumExpr totalImpsInSlot = cplex.linearNumExpr();
			for (int a=s; a<advertisers; a++) {
				totalImpsInSlot.addTerm(1, vars.I_a_s[a][s]);
			}
			cplex.addEq(totalImpsInSlot, vars.S_a[s]);
		}
		
		//Slot totals must be (non-strictly) increasing
		for (int s=1; s<advertisers; s++) {
			cplex.addGe(vars.S_a[s-1], vars.S_a[s]);
		}
		
	}
	
	protected void postObjective(IloCplex cplex, CPlexVariables vars, int advertisers, int slots, int M) throws IloException {
		slots = Math.min(slots,advertisers);
		IloNumVar[] SDeltas = cplex.numVarArray(slots, 0, 2*M);
		//System.out.println("Slots:"+slots);
		for (int s=0; s < slots-1; s++) {
			
			IloLinearNumExpr delta = cplex.linearNumExpr();
			delta.addTerm(1, vars.S_a[s]);
			delta.addTerm(-1, vars.S_a[s+1]);
			cplex.addGe(SDeltas[s], delta);
			
			delta = cplex.linearNumExpr();
			delta.addTerm(-1, vars.S_a[s]);
			delta.addTerm(1, vars.S_a[s+1]);
			cplex.addGe(SDeltas[s], delta);
		}
		
		IloLinearNumExpr allSDeltas = cplex.linearNumExpr();
		//for (int a=0; a<numSlots-1; a++) {
		for(IloNumVar SDelta : SDeltas){
			allSDeltas.addTerm(1, SDelta);
		}
		//cplex.addMinimize(allSDeltas);		

		
		IloLinearNumExpr allTs = cplex.linearNumExpr();
		for(IloNumVar T : vars.T_a){
			allTs.addTerm(1, T);
		}
					
		cplex.addMinimize(cplex.sum(cplex.prod(100.0, allSDeltas), allTs));	
	}
	
   private int[] makeFilterArray(IloNumVar[] arr, int[] order, IloCplex cplex) throws IloException{
	   assert(arr.length == order.length);
	   int[] arri = new int[order.length];
	   for(int i=0; i<order.length; i++){
		   arri[order[i]] = (int)Math.round(cplex.getValue(arr[i]));
	   }
	   return arri;
   }
   
   private int[] makeFilterArray(IloNumVar[] arr, int ub, IloCplex cplex) throws IloException{
	   int length = Math.min(arr.length, ub);
	   int[] arri = new int[length];
	   for(int i=0; i<length; i++){
		   arri[i] = (int)Math.round(cplex.getValue(arr[i]));
	   }
	   return arri;
   }
   
	protected class CPlexVariables {
		IloIntVar[][] D_a_s;
		IloNumVar[][] I_a_s;
		IloNumVar[] S_a;
		IloNumVar[] T_a;
		
		CPlexVariables(IloIntVar[][] D_a_s, IloNumVar[][] I_a_s, IloNumVar[] S_a, IloNumVar[] T_a){
			this.D_a_s = D_a_s;
			this.I_a_s = I_a_s;
			this.S_a = S_a;
			this.T_a = T_a;
		}
	}

   
	
}
