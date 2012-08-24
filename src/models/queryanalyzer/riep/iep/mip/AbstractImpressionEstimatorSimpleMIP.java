package models.queryanalyzer.riep.iep.mip;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.riep.iep.IEResult;

public abstract class AbstractImpressionEstimatorSimpleMIP extends AbstractImpressionEstimatorMIP {
	protected final static boolean SUPPRESS_OUTPUT = true;
	
	public AbstractImpressionEstimatorSimpleMIP(AbstractQAInstance inst){
		super(inst);
	}

	@Override
	public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}

	@Override
	public IEResult search(int[] order) {
		QAInstanceExact orderedInst = _instExact.reorder(order); 
		int agentIndex = orderedInst.getAgentIndex();
		int agentImpressions = orderedInst.getImpressions();
		
		//System.out.println(Arrays.toString(_instanceExact.getAvgPos()));
		//System.out.println(Arrays.toString(orderedInst.getAvgPos()));
		   
		int advertisers = orderedInst.getNumAdvetisers();
		int slots = orderedInst.getNumSlots();
		int M = orderedInst.getImpressionsUB();
		
		double[] avgPos = orderedInst.getAvgPos();
		  
	    double[] I_a = new double[advertisers];
	    Arrays.fill(I_a, -1);
	      
	    I_a[agentIndex] = orderedInst.getImpressions();
	      
	    int[] minDropOut = new int[advertisers];
	    int[] maxDropOut = new int[advertisers];
	      
	    //set default values
	    for(int a=0; a < advertisers; a++){
	    	int ceilingSlot = ((int)Math.ceil(orderedInst.getAvgPos()[a]))-1;
	    	int floorSlot = ((int)Math.floor(orderedInst.getAvgPos()[a]))-1;
	    	if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == slots-1)){
	    		minDropOut[a] = ceilingSlot;
	    	} else {
	    		minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents?  
	    	}
	    	maxDropOut[a] = Math.min(Math.min(a,slots-1),floorSlot);
	    }
	    
	    System.out.println(Arrays.toString(orderedInst.getAvgPos()));
	    System.out.println(Arrays.toString(minDropOut));
	    System.out.println(Arrays.toString(maxDropOut));
	      
	      
	    
	    
	    try {
			IloCplex cplex = new IloCplex();
			
			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			if (SUPPRESS_OUTPUT) cplex.setOut(null);
			//cplex.setParam(IloCplex.DoubleParam.TiLim, TIMEOUT_IN_SECONDS);
			
			
			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloIntVar[][] D_a_s = new IloIntVar[advertisers][]; 
			IloNumVar[][] I_a_s = new IloNumVar[advertisers][]; //(#imps per agent/slot)
			IloNumVar[] S_a = cplex.numVarArray(advertisers, 0, M);
			IloNumVar[] T_a = cplex.numVarArray(advertisers, 1, M);
			
			

			for (int a=0; a<advertisers; a++) {
				I_a_s[a] = cplex.numVarArray(a+1, 0, M);
				D_a_s[a] = cplex.boolVarArray(a+1);
			}
			
			//dropout constraints
			for (int a=0; a<advertisers; a++) {
				cplex.addGe(D_a_s[a][maxDropOut[a]], 1.0);
				if(minDropOut[a]-1 >= 0){
					cplex.addLe(D_a_s[a][minDropOut[a]-1], 0);
				}
				
				for(int s=0; s<=a; s++) {
					IloLinearNumExpr expr= cplex.linearNumExpr();
					expr.addTerm(M, D_a_s[a][s]);
					cplex.addGe(expr, I_a_s[a][s]);
				}
				for(int s=1; s<=a; s++){
					cplex.addLe(D_a_s[a][s-1], D_a_s[a][s]);
				}
			}
			
			//Waterfall constraints
			for (int a=0; a<advertisers; a++) {
				for (int s=0; s<=a; s++) {
					IloLinearNumExpr myCumulativeImps = cplex.linearNumExpr();
					IloLinearNumExpr oppCumulativeImps = cplex.linearNumExpr();
					//Create constraint for this value of $a$ and $k$
					for (int i=s; i<=a; i++) {
						myCumulativeImps.addTerm(1, I_a_s[a][i]);
					}
					for (int opp=s; opp<=a; opp++) {
						oppCumulativeImps.addTerm(1, I_a_s[opp][s]);
					}
					cplex.add(cplex.ifThen(cplex.ge(D_a_s[a][s],1.0), cplex.eq(myCumulativeImps, oppCumulativeImps)) );
				}
			}
			
			//Total number of impressions constraint:
			//Make sure "Total Agent Impressions" variable actually equals the sum of the agent's impressions per slot.
			for (int a=0; a<advertisers; a++) {
				IloLinearNumExpr totalImps = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, slots-1); s++) {
					totalImps.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImps, T_a[a]);
			}		
			
			//Total slot impressions constraint
			for (int s=0; s<advertisers; s++) {
				IloLinearNumExpr totalImpsInSlot = cplex.linearNumExpr();
				for (int a=s; a<advertisers; a++) {
					totalImpsInSlot.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImpsInSlot, S_a[s]);
			}
			
			//Slot totals must be (non-strictly) increasing
			for (int s=1; s<advertisers; s++) {
				cplex.addGe(S_a[s-1], S_a[s]);
			}
		
			
			System.out.println(agentIndex+" - "+agentImpressions);
			cplex.addEq(T_a[agentIndex], agentImpressions);
			
			for (int a=0; a<advertisers; a++) {
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, slots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(avgPos[a], T_a[a]);
				cplex.addEq(lhs, rhs);
			}
			
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			//postObjective(cplex, vars, effectiveNumAgents, bestObj);
			IloNumVar[] SDeltas = cplex.numVarArray(slots, 0, 2*M);
			
			for (int s=0; s < slots-1; s++) {
				IloLinearNumExpr delta = cplex.linearNumExpr();
				delta.addTerm(1, S_a[s]);
				delta.addTerm(-1, S_a[s+1]);
				cplex.addGe(SDeltas[s], delta);
				
				delta = cplex.linearNumExpr();
				delta.addTerm(-1, S_a[s]);
				delta.addTerm(1, S_a[s+1]);
				cplex.addGe(SDeltas[s], delta);
			}
			
			IloLinearNumExpr allSDeltas = cplex.linearNumExpr();
			//for (int a=0; a<numSlots-1; a++) {
			for(IloNumVar SDelta : SDeltas){
				allSDeltas.addTerm(1, SDelta);
			}
			//cplex.addMinimize(allSDeltas);		

			
			IloLinearNumExpr allTs = cplex.linearNumExpr();
			for(IloNumVar T : T_a){
				allTs.addTerm(1, T);
			}
						
			cplex.addMinimize(cplex.sum(cplex.prod(100.0, allSDeltas), allTs));	
			
			
			
			
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
				double[] S_aVal = cplex.getValues(S_a);
				double[] T_aVal = cplex.getValues(T_a);
				double[][] I_a_sVal = new double[advertisers][];
				for (int a=0; a<advertisers; a++) {
					I_a_sVal[a] = cplex.getValues(I_a_s[a]);
				}			
				
				cplex.output().println("obj: " + objectiveVal);
				cplex.output().println("S_a: " + Arrays.toString(cplex.getValues(S_a)));
				cplex.output().println("T_a: " + Arrays.toString(cplex.getValues(T_a)));
				cplex.output().println();
				for (int a=0; a<I_a_s.length; a++) {
					cplex.output().println("I_" + a + "_s: " + Arrays.toString(cplex.getValues(I_a_s[a])));				
				}
				cplex.output().println();
				for (int a=0; a<D_a_s.length; a++) {
					//System.out.println(D_a_s[a]);
					cplex.output().println("D_" + a + "_s: " + Arrays.toString(cplex.getValues(D_a_s[a])));				
				}
				
				
				int[] impsPerAgent = makeFilterArray(T_a, order, cplex);
		        int[] impsPerSlot = makeFilterArray(S_a, _inst.getNumSlots(), cplex);

				return new IEResult(cplex.getObjValue(), impsPerAgent, order.clone(), impsPerSlot, _inst.getAgentNames()); 
				//solution = new LPSolution(objectiveVal, I_a_sVal, S_aVal, T_aVal);
			} else {
				System.out.println("Solver returned false.");
				//System.out.println("Model: " + cplex.getModel() );
			}
			
			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
	    
	    
	    return null;
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
   
	
}
