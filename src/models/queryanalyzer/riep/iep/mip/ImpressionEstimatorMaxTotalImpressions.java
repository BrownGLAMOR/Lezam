/**
 * 
 */
package models.queryanalyzer.riep.iep.mip;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstanceExact;

/**
 * @author betsy
 *
 */
public class ImpressionEstimatorMaxTotalImpressions extends
		AbstractImpressionEstimatorSimpleMIP {

	public QAInstanceExact _instExact;
	/**
	 * @param inst
	 */
	public ImpressionEstimatorMaxTotalImpressions(QAInstanceExact inst, double timeout, IloCplex cplex) {
		super(inst, timeout, cplex);
		_instExact = inst;
	}
	/**
	 * copied from ImpressionEstimatorSimpleMipExact
	 */
	@Override
	protected CPlexVariables makeModel(IloCplex cplex, int[] order) throws IloException {
		QAInstanceExact orderedInst = _instExact.reorder(order); 
		int agentIndex = orderedInst.getAgentIndex();
		int agentImpressions = orderedInst.getImpressions();
		
		//System.out.println(Arrays.toString(_instanceExact.getAvgPos()));
		//System.out.println(Arrays.toString(orderedInst.getAvgPos()));
		   
		int advertisers = orderedInst.getNumAdvetisers();
		int slots = Math.min(advertisers, orderedInst.getNumSlots());
		int M = orderedInst.getImpressionsUB();
		
		double[] avgPos = orderedInst.getAvgPos();
		  
	    double[] I_a = new double[advertisers];
	    Arrays.fill(I_a, -1);
	      
	    I_a[agentIndex] = orderedInst.getImpressions();
	      
	    int[] minDropOut = new int[advertisers];
	    int[] maxDropOut = new int[advertisers];
	      
	    //THis is CJC's constraint and should probably be put back in
	    //set default values
	    for(int a=0; a < advertisers; a++){
	    	int ceilingSlot = ((int)Math.ceil(orderedInst.getAvgPos()[a]))-1;
	    	int floorSlot = ((int)Math.floor(orderedInst.getAvgPos()[a]))-1;
	    	if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == slots-1)){
	    		minDropOut[a] = ceilingSlot;
	    	} else {
	    		minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents 
	    	}
	    	maxDropOut[a] = Math.min(Math.min(a,slots-1),floorSlot);
	    }
	  
	    //set default values
//	    for(int a=0; a < advertisers; a++){
//	    	minDropOut[a] = 0;
//	    	maxDropOut[a] = Math.min(a,slots-1);
//	    }
	    
	    
	    System.out.println(Arrays.toString(orderedInst.getAvgPos()));
	    System.out.println(Arrays.toString(minDropOut));
	    System.out.println(Arrays.toString(maxDropOut));
	      
	      
		//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
		CPlexVariables vars = makeModelVariables(cplex, advertisers, M);
		
		postDropoutConstraints(cplex, vars, advertisers, M, minDropOut, maxDropOut);
		
		postCascadeConstraints(cplex, vars, advertisers, slots);
		
		
		System.out.println(agentIndex+" - "+agentImpressions);
		cplex.addEq(vars.T_a[agentIndex], agentImpressions);
		
		for (int a=0; a<advertisers; a++) {
			IloLinearNumExpr lhs = cplex.linearNumExpr();
			for (int s=0; s<=Math.min(a, slots-1); s++) {
				lhs.addTerm(s+1, vars.I_a_s[a][s]);
			}
			IloNumExpr rhs = cplex.prod(avgPos[a], vars.T_a[a]);
			cplex.addEq(lhs, rhs);
		}
		
		//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
		//postObjective(cplex, vars, effectiveNumAgents, bestObj);
		postObjective(cplex, vars, advertisers, slots, M);
		
		return vars;
	}

	
	protected void postObjective(IloCplex cplex, CPlexVariables vars, int advertisers, int slots, int M) throws IloException {
		slots = Math.min(slots,advertisers);
			

		
		IloLinearNumExpr allTs = cplex.linearNumExpr();
		for(IloNumVar T : vars.T_a){
			allTs.addTerm(1, T);
		}
		//TODO: Change back to max			
		cplex.addMinimize(allTs);	
		//cplex.addMinimize(cplex.sum(cplex.prod(100.0, allSDeltas), allTs));	
	}
	

	/* (non-Javadoc)
	 * @see models.queryanalyzer.riep.iep.AbstractImpressionEstimator#getName()
	 */
	@Override
	public String getName() {
		
		return "SimpleMipExact-MaxImpressions";
	}

}
