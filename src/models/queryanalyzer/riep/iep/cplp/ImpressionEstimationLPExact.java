package models.queryanalyzer.riep.iep.cplp;


import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;


/**
 * LP Solver based on Carleton's model.
 * @author sodomka
 *
 */
public class ImpressionEstimationLPExact extends AbstractImpressionEstimationLP {
		
	public ImpressionEstimationLPExact(int M, double[] knownI_a, double[] knownMu_a, int numSlots) {
		super(M, knownI_a, knownMu_a, numSlots);
		
		if(_hasPrior){
			DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SLOT_DIFF;
		} else {
			DESIRED_OBJECTIVE = Objective.MINIMIZE_SLOT_DIFF;
		}
		System.out.println("Objective: "+DESIRED_OBJECTIVE);
	}
	
	@Override
	protected void postAveragePositionConstraints(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents) throws IloException {
		for (int a=0; a<effectiveNumAgents; a++) {
			assert(_isKnownMu_a[a]);
			IloLinearNumExpr lhs = cplex.linearNumExpr();
			for (int s=0; s<=Math.min(a, _numSlots-1); s++) {
				lhs.addTerm(s+1, vars.I_a_s[a][s]);
			}
			IloNumExpr rhs = cplex.prod(_knownMu_a[a], vars.T_a[a]);
			cplex.addEq(lhs, rhs);
		}
	}


	@Override
	protected void postObjective(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents, double bestObj) throws IloException {
		if (DESIRED_OBJECTIVE == Objective.MAXIMIZE_IMPRESSIONS) {
			addObjective_closeToImpressionsUpperBound(cplex, vars.T_a, bestObj, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR) {
			addObjective_minimizeImpressionPriorError(cplex, vars.T_a, _T_aPriorMean, _T_aPriorStdev, bestObj, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF) {
			addObjective_minimizeSlotDiff(cplex, vars.S_a, bestObj, _numSlots, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SLOT_DIFF) {
			addObjective_minimizeImpressionPriorErrorSlotDiff(cplex, vars.S_a,  vars.T_a, _T_aPriorMean, bestObj, _numSlots, effectiveNumAgents);
		} else {
			throw new RuntimeException("Objective: "+DESIRED_OBJECTIVE+" not supported by this model");
		}
	}




}
