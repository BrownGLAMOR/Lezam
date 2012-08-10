package models.queryanalyzer.riep.iep.cplp;

import java.util.Arrays;

import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;

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
public class ImpressionEstimationLPAll extends AbstractImpressionEstimationLP {
		
	private double[] _knownSampledMu_a; //sampled average position
	private boolean[] _isKnownSampledMu_a; //(if sampledMu not known, a default -1 value is used)
	
		
	public ImpressionEstimationLPAll(int M, double[] knownI_a, double[] knownMu_a, double[] knownSampledMu_a, int numSlots) {
		super(M, knownI_a, knownMu_a, numSlots);
		
		_knownSampledMu_a = knownSampledMu_a;
		_isKnownSampledMu_a = new boolean[_numAgents];

		for (int a=0; a<_numAgents; a++) {
			//change NaN values to the agent's position (assumes their average position was roughly their starting position
			if(Double.isNaN( knownSampledMu_a[a] )) knownSampledMu_a[a] = Math.min(numSlots, a+1); 

			if(knownSampledMu_a[a] != -1) _isKnownSampledMu_a[a] = true; //mark whether it's known
		}
		
		updateObjecitve();
	}
	

	public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}
	
	

	@Override
	protected void postAveragePositionConstraints(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents) throws IloException {
		for (int a=0; a<effectiveNumAgents; a++) {
			if (_isKnownMu_a[a]) {
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, _numSlots-1); s++) {
					lhs.addTerm(s+1, vars.I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(_knownMu_a[a], vars.T_a[a]);

				if (USE_EPSILON) {
					cplex.addLe(lhs, cplex.sum(rhs, epsilon));
					cplex.addGe(lhs, cplex.sum(rhs, -epsilon));
				} else {
					cplex.addEq(lhs, rhs);
				}
			}
		}
	}


	@Override
	protected void postObjective(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents, double bestObj) throws IloException {
		if (DESIRED_OBJECTIVE == Objective.MAXIMIZE_IMPRESSIONS) {
			addObjective_closeToImpressionsUpperBound(cplex, vars.T_a, bestObj, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR) {
			addObjective_minimizeImpressionPriorError(cplex, vars.T_a, _T_aPriorMean, _T_aPriorStdev, bestObj, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SAMPLE_MU_DIFF) {
			addObjective_minimizeDistanceFromSampledMu(cplex, vars.I_a_s, vars.T_a, bestObj, effectiveNumAgents, _M);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF) {
			addObjective_minimizeImpressionPriorErrorAndDistanceFromSampledMu(cplex, vars.I_a_s, vars.T_a, effectiveNumAgents, _M, bestObj);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_SLOT_DIFF) {
			addObjective_minimizeSlotDiff(cplex, vars.S_a, bestObj, _numSlots, effectiveNumAgents);
		} else if (DESIRED_OBJECTIVE == Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SLOT_DIFF) {
			addObjective_minimizeImpressionPriorErrorSlotDiff(cplex, vars.S_a,  vars.T_a, _T_aPriorMean, bestObj, _numSlots, effectiveNumAgents);
		}
	}

	
	
	
	/**
	 * This will cause constraints to be created that try to make the average positions
	 * as close as possible to what were sampled (assuming you don't have exact info for that position).
	 * 
	 * @param cplex
	 * @param I_a_s
	 * @param I_a
	 * @throws IloException
	 */
	protected void addObjective_minimizeDistanceFromSampledMu(IloCplex cplex,
			IloNumVar[][] I_a_s, IloNumVar[] I_a, double bestObj, int numAgents, int maxImpsPerAgent) throws IloException {
		
		//The maximum allowed difference between the sample and observed average positions 
		int LARGE_CONSTANT = _numSlots*maxImpsPerAgent;
		
		//Create variable which will say how far an agent's predicted average position is from the observed sampleMu
		IloNumVar[] errors = cplex.numVarArray(numAgents, 0, LARGE_CONSTANT);	
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		
		//Add constraints stating resulting avgPositions have to be close to sampledMu
		for (int a=0; a<numAgents; a++) {
			if (!_isKnownMu_a[a] && _isKnownSampledMu_a[a]) {
				//Compute resulting avgPosition
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, _numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(_knownSampledMu_a[a], I_a[a]);

				//Make sure avgPosition is close to satisfied
				cplex.addLe(lhs, cplex.sum(rhs, errors[a]));
				cplex.addGe(lhs, cplex.sum(rhs, cplex.prod(-1, errors[a])));
				
				//Keep track of which agents have constraints here
				relevantErrors.addTerm(1, errors[a]);
			}
		}
		
		//Minimize total distance from sampledMus
		cplex.addMinimize(relevantErrors);
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(relevantErrors, bestObj);
		}
	}
	
	
	/**
	 * This will make the predicted agent impressions as close to our priors as possible. 
	 * @param cplex
	 * @param I_a
	 * @throws IloException
	 */
	protected void addObjective_minimizeImpressionPriorErrorAndDistanceFromSampledMu(
			IloCplex cplex, IloNumVar[][] I_a_s, IloNumVar[] I_a, int numAgents, int M, double bestObj) throws IloException {
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		
		
		//----- Impressions prior error
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr impressionsErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			//NOTE! This is assuming we have some impressions prior for each agent.
			if (_isKnownI_aDistributionMean[a] && !_isKnownI_a[a]) {
			//if (isKnownI_aDistributionMean[a] && !isKnownI_a[a]) {
				cplex.addLe(I_a[a], cplex.sum( _T_aPriorMean[a] , cplex.prod(_T_aPriorStdev[a], I_aError[a])  )  );
				cplex.addGe(I_a[a], cplex.diff( _T_aPriorMean[a] , cplex.prod(_T_aPriorStdev[a], I_aError[a])  )  );
				impressionsErrors.addTerm(1, I_aError[a]);
			}
		}
		
		
		//----- Average position error
				
		//The maximum allowed difference between the sample and observed average positions 
		int LARGE_CONSTANT = _numSlots*M;
		
		//Create variable which will say how far an agent's predicted average position is from the observed sampleMu
		IloNumVar[] errors = cplex.numVarArray(numAgents, 0, LARGE_CONSTANT);	
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		
		//Add constraints stating resulting avgPositions have to be close to sampledMu
		for (int a=0; a<numAgents; a++) {
			if (!_isKnownMu_a[a] && _isKnownSampledMu_a[a]) {
				//Compute resulting avgPosition
				IloLinearNumExpr lhs = cplex.linearNumExpr();
				for (int s=0; s<=Math.min(a, _numSlots-1); s++) {
					lhs.addTerm(s+1, I_a_s[a][s]);
				}
				IloNumExpr rhs = cplex.prod(_knownSampledMu_a[a], I_a[a]);

				//Make sure avgPosition is close to satisfied
				cplex.addLe(lhs, cplex.sum(rhs, errors[a]));
				cplex.addGe(lhs, cplex.sum(rhs, cplex.prod(-1, errors[a])));
				
				//Keep track of which agents have constraints here
				relevantErrors.addTerm(1, errors[a]);
			}
		}
		
		
		//Compute some weighted sum of these two errors
		//TODO: what should the weight be?
		double impressionsWeight = 1;
		double avgPosWeight = 10;
		IloNumExpr weightedError = cplex.sum( cplex.prod(impressionsWeight, impressionsErrors),
				cplex.prod(avgPosWeight, relevantErrors) );
		cplex.addMinimize(weightedError);
		
	
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(relevantErrors, bestObj);
		}
	}
	
	
	
	
	
	
	
	
	/**
	 * This will cause constraints to be created that try to make the average positions
	 * as close as possible to what were sampled (assuming you don't have exact info for that position).
	 * 
	 * @param cplex
	 * @param I_a_s
	 * @param T_a
	 * @throws IloException
	 */
//	private void addObjective_minimizeDistanceFromSampledMu(IloCplex cplex,
//			IloNumVar[][] I_a_s, IloNumVar[] T_a, double bestObj, int numAgents) throws IloException {
//		
//		//The maximum allowed difference between the sample and observed average positions 
//		int LARGE_CONSTANT = numSlots;
//		
//		//Create variable which will say how far an agent's predicted average position is from the observed sampleMu
//		IloNumVar[] errors = cplex.numVarArray(numAgents, 0, LARGE_CONSTANT);	
//		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
//		
//		//Add constraints stating resulting avgPositions have to be close to sampledMu
//		for (int a=0; a<numAgents; a++) {
//			if (!isKnownMu_a[a] && isKnownSampledMu_a[a]) {
//				//Compute resulting avgPosition
//				IloLinearNumExpr lhs = cplex.linearNumExpr();
//				for (int s=0; s<=Math.min(a, numSlots-1); s++) {
//					lhs.addTerm(s+1, I_a_s[a][s]);
//				}
//				IloNumExpr rhs = cplex.prod(knownSampledMu_a[a], T_a[a]);
//
//				//Make sure avgPosition is close to satisfied
//				cplex.addLe(lhs, cplex.sum(rhs, errors[a]));
//				cplex.addGe(lhs, cplex.sum(rhs, cplex.prod(-1, errors[a])));
//				
//				//Keep track of which agents have constraints here
//				relevantErrors.addTerm(1, errors[a]);
//			}
//		}
//		
//		//Minimize total distance from sampledMus
//		cplex.addMinimize(relevantErrors);
//		
//		//Must be better than previously best objective
//		if (bestObj != -1) {
//			cplex.addLe(relevantErrors, bestObj);
//		}
//	}
	
	
	

	
	
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

		int numAgents = 2;
		int numSlots = 5;
		double[] knownMu_a = {1.0, 2.0};
		double[] knownSampledMu_a = {-1, -1};
		int M = 1300;
		int us = 0;
		int imp = 300;
		int[] dropout_a = {0, 1};
		double bestObj = -1;
		double[] T_aPriorMean = {200, 200};
		double[] T_aPriorStdev = {50, 50};
		double[] knownI_a = new double[numAgents];
		Arrays.fill(knownI_a, -1);
		knownI_a[us] = imp;
		
		ImpressionEstimationLPAll carletonLP = new ImpressionEstimationLPAll(M, knownI_a, knownMu_a, knownSampledMu_a, numSlots);
		carletonLP.setPrior(T_aPriorMean, T_aPriorStdev);
		
		LPSolution sol = carletonLP.solveIt(dropout_a, bestObj);
		System.out.println(sol);
		
	}




}
