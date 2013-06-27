/**
 * 
 */
package models.queryanalyzer.riep.iep.mip;

import java.util.Arrays;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;

/**
 * @author betsy
 *
 */
public class ImpressionEstimatorSimpleMIPPriors extends
		AbstractImpressionEstimatorSimpleMIP {
	
	int MIN_IMPRESSIONS_STDEV = 1;

	public QAInstanceAll _inst;
	
	
	double[] knownI_aDistributionMean; 
	double[] knownI_aDistributionStdev;
	boolean[] isKnownI_aDistributionMean;
	boolean[] isKnownI_a;
	
	/**
	 * @param inst
	 */
//	public ImpressionEstimatorSimpleMIPMinDiffTotalImpressions(QAInstanceExact inst, double timeout) {
//		super(inst, timeout);
//		_instExact = inst;
//	}

	public ImpressionEstimatorSimpleMIPPriors(QAInstanceAll inst, double timeout, IloCplex cplex) {
		super(inst, timeout, cplex);
		_inst= inst;
		
	
		this.knownI_aDistributionMean = inst.getAgentImpressionDistributionMean();
		this.knownI_aDistributionStdev = inst.getAgentImpressionDistributionStdev();
		//PRIORS PRINT HERE
		//System.out.println("MEAN: "+Arrays.toString(this.knownI_aDistributionMean));
		//System.out.println("StDev: "+Arrays.toString(this.knownI_aDistributionStdev));
	
		
		
		int numAgents = knownI_aDistributionStdev.length;
		isKnownI_a = new boolean[numAgents];
		isKnownI_aDistributionMean = new boolean[numAgents];
		for (int a=0; a<numAgents; a++) {
			//System.out.println("Ag Index: "+_inst.getAgentIndex()+" imps: "+inst.getImpressions());
			if(a==_inst.getAgentIndex()){
				//this.isKnownI_a[a] = true;
				//knownI_aDistributionStdev[a]=0;
				//knownI_aDistributionMean[a]=inst.getImpressions();
			}else{
				//this.isKnownI_a[a]=false;
			}
			
			if(knownI_aDistributionMean[a] != -1) 
				isKnownI_aDistributionMean[a] = true;
			//Make sure the minimum I_a stdev is 1. (We don't want to ever assume we know exactly what the opponent imps will be).
			if (knownI_aDistributionStdev[a] != -1 && knownI_aDistributionStdev[a]< MIN_IMPRESSIONS_STDEV) 
				knownI_aDistributionStdev[a] = MIN_IMPRESSIONS_STDEV;
		}
	}
	/**
	 * copied from ImpressionEstimatorSimpleMipExact
	 */
	@Override
	protected CPlexVariables makeModel(IloCplex cplex, int[] order) throws IloException {
		QAInstanceAll orderedInst = _inst.reorder(order); 
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
	    
//	    //set default values
//	    for(int a=0; a < advertisers; a++){
//	    	minDropOut[a] = 0;
//	    	maxDropOut[a] = Math.min(a,slots-1);
//	    }
//	    
	    
//	    System.out.println(Arrays.toString(orderedInst.getAvgPos()));
//	    System.out.println(Arrays.toString(minDropOut));
//	    System.out.println(Arrays.toString(maxDropOut));
	      
	      
		//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
		CPlexVariables vars = makeModelVariables(cplex, advertisers, M);
		
		postDropoutConstraints(cplex, vars, advertisers, M, minDropOut, maxDropOut);
		
		postCascadeConstraints(cplex, vars, advertisers, slots);
		
		
		//System.out.println(agentIndex+" - "+agentImpressions);
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
// change objective
	
	
	protected void postObjective(IloCplex cplex, CPlexVariables vars, int advertisers, int slots, int M) throws IloException {
		
		
		/*
		 * 	IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
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
		 * 
		 */
		slots = Math.min(slots,advertisers);
		IloNumVar[] I_aError = cplex.numVarArray(advertisers, 0, 2*M);
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		//System.out.println("Slots:"+slots);
		for (int a=0; a < advertisers; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (isKnownI_aDistributionMean[a]&& !isKnownI_a[a]) {
				
				//System.out.println("__________________________HERE_______________________________________");
				cplex.addLe(vars.T_a[a], cplex.sum( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])));
				cplex.addGe(vars.T_a[a], cplex.diff( knownI_aDistributionMean[a] , cplex.prod(knownI_aDistributionStdev[a], I_aError[a])));
				relevantErrors.addTerm(1, I_aError[a]);
			}
		}
	
		
		cplex.addMinimize(relevantErrors);
		


//		IloLinearNumExpr allTs = cplex.linearNumExpr();
//		for(IloNumVar T : vars.T_a){
//			allTs.addTerm(1, T);
//		}
//					
//		cplex.addMaximize(allTs);	
		//cplex.addMinimize(cplex.sum(cplex.prod(100.0, allSDeltas), allTs));	
	}
	/* (non-Javadoc)
	 * @see models.queryanalyzer.riep.iep.AbstractImpressionEstimator#getName()
	 */
	@Override
	public String getName() {
		return "SimpleMipExact-Priors";
	}

}
