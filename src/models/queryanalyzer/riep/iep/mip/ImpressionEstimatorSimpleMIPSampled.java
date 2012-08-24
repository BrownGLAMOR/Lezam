package models.queryanalyzer.riep.iep.mip;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.ds.QAInstanceSampled;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.mip.AbstractImpressionEstimatorSimpleMIP.CPlexVariables;

public class ImpressionEstimatorSimpleMIPSampled extends AbstractImpressionEstimatorSimpleMIP {
	protected final static boolean SUPPRESS_OUTPUT = true;
	
	public QAInstanceSampled _instSampled;
	
	public ImpressionEstimatorSimpleMIPSampled(QAInstanceSampled inst){
		super(inst);
		_instSampled = inst;
	}

	@Override
	public String getName() {return "SimpleMIPSampled";}

	

	@Override
	protected CPlexVariables makeModel(IloCplex cplex, int[] order) throws IloException {
		QAInstanceSampled orderedInst = _instSampled.reorder(order); 
		int agentIndex = orderedInst.getAgentIndex();
		int agentImpressions = orderedInst.getImpressions();
		
		//System.out.println(Arrays.toString(_instanceExact.getAvgPos()));
		//System.out.println(Arrays.toString(orderedInst.getAvgPos()));
		   
		int advertisers = orderedInst.getNumAdvetisers();
		int slots = orderedInst.getNumSlots();
		int M = orderedInst.getImpressionsUB();
		
		double[] avgPos = orderedInst.getAvgPos();
		double[] avgPosLB = orderedInst.getAvgPosLB();
		double[] avgPosUB = orderedInst.getAvgPosUB();
		
	    double[] I_a = new double[advertisers];
	    Arrays.fill(I_a, -1);
	      
	    I_a[agentIndex] = orderedInst.getImpressions();
	      
	    int[] minDropOut = new int[advertisers];
	    int[] maxDropOut = new int[advertisers];
	      
	    //set default values
	    for(int a=0; a < advertisers; a++){
	    	int ceilingSlot = ((int)Math.ceil(avgPosUB[a]))-1;
	    	int floorSlot = ((int)Math.floor(avgPosLB[a]))-1;
	    	if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == slots-1)){
	    		minDropOut[a] = ceilingSlot;
	    	} else {
	    		minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents  
	    	}
	    	maxDropOut[a] = Math.min(Math.min(a,slots-1),floorSlot);
	    }
	    
	    System.out.println(Arrays.toString(avgPosLB));
	    System.out.println(Arrays.toString(avgPosUB));
	    System.out.println(Arrays.toString(minDropOut));
	    System.out.println(Arrays.toString(maxDropOut));
	      
	      
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
			
			if(a == agentIndex){
				IloNumExpr rhs = cplex.prod(avgPos[a], vars.T_a[a]);
				cplex.addEq(lhs, rhs);
			} else {
				IloNumExpr rhs;
				rhs = cplex.prod(avgPosLB[a], vars.T_a[a]);
				cplex.addGe(lhs, rhs);
				
				rhs = cplex.prod(avgPosUB[a], vars.T_a[a]);
				cplex.addLe(lhs, rhs);
			}
		}
		
		//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
		//postObjective(cplex, vars, effectiveNumAgents, bestObj);
		postObjective(cplex, vars, advertisers, slots, M);
		
		return vars;
	}   
	
}
