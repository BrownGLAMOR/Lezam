package models.queryanalyzer.riep.iep.cplp;

import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * LP Solver based on Carleton's model.
 * @author sodomka
 *
 */
public abstract class AbstractImpressionEstimationLP {
	
	public enum Objective {
		MAXIMIZE_IMPRESSIONS,
		MINIMIZE_IMPRESSION_PRIOR_ERROR, //get resulting I_a as close as possible to I_aDistributionMean
		MINIMIZE_SAMPLE_MU_DIFF,
		MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF,
		DEPENDS_ON_CIRCUMSTANCES, 
		MINIMIZE_SLOT_DIFF,
		MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SLOT_DIFF, 
		MINIMIZE_SLOT_DIFF_AND_IMPRESSIONS,
	}
	
	protected final static boolean USE_EPSILON = false;
	protected final static double epsilon = .00001;
	protected final static boolean SUPPRESS_OUTPUT = true;
	protected final static boolean SUPPRESS_OUTPUT_MODEL = true;
	protected final static double TIMEOUT_IN_SECONDS = 3;
	protected Objective DESIRED_OBJECTIVE = Objective.DEPENDS_ON_CIRCUMSTANCES; //Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SLOT_DIFF; //Objective.DEPENDS_ON_CIRCUMSTANCES; //Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF;

	double MIN_IMPRESSIONS_STDEV = 1; //The smallest standard deviation assumed by impressions models (Shouldn't be 0, or we can get no feasible solution if models are bad)

	//If minimizing impression error, this is the maximum number of standard deviations away
	//that the predicted number of agent impressions can be from the mean of the prior.
	//(Since our impression predictions won't necessarily be good, it's probably a good idea 
	// to make this a high value)
	protected final static double MAX_ERROR = 100000; 
	
	
	//Predicted agent impressions
	protected int _M;
	protected int _numAgents;
	protected int _numSlots;
	protected double[] _knownMu_a; //average position for each agent
	protected boolean[] _isKnownMu_a; //(if mu is not known, a default -1 value is used)
	protected boolean[] _isKnownI_a; //(if I_a is not known, a default -1 value is used)
	protected double[] _knownI_a;
	
	protected boolean _hasPrior;
	protected double[] _T_aPriorMean;	
	protected double[] _T_aPriorStdev;
	protected boolean[] _isKnownI_aDistributionMean; //Is T_a prior known? TODO: Should probably use this convention instead of T_aPrior (to match WaterfallILP)
	
	
	public AbstractImpressionEstimationLP(int M, double[] knownI_a, double[] knownMu_a, int numSlots) {
		_M = M;

		_numAgents = knownMu_a.length;
		_numSlots = numSlots;
		_knownMu_a = knownMu_a;
		_knownI_a = knownI_a;
		
		_isKnownI_a = new boolean[_numAgents];
		_isKnownMu_a = new boolean[_numAgents];

		for (int a=0; a<_numAgents; a++) {
			if(knownI_a[a] != -1) _isKnownI_a[a] = true;
			if(knownMu_a[a] != -1) _isKnownMu_a[a] = true;
		}
		
		updateObjecitve();
	}
	//--------------------
	
	public void setPrior(double[] T_aPriorMean, double[] T_aPriorStdev){
		assert(T_aPriorMean.length == _numAgents && T_aPriorStdev.length == _numAgents);
		_hasPrior = true;
		_T_aPriorMean = T_aPriorMean;	
		_T_aPriorStdev = T_aPriorStdev;
		_isKnownI_aDistributionMean = new boolean[_numAgents];
		
		for (int a=0; a<_numAgents; a++) {
			//Make sure the minimum I_a stdev is 1. (We don't want to ever assume we know exactly what the opponent imps will be).
			if (T_aPriorStdev[a] != -1 && T_aPriorStdev[a]< MIN_IMPRESSIONS_STDEV) T_aPriorStdev[a] = MIN_IMPRESSIONS_STDEV;
			if(T_aPriorMean[a] != -1) _isKnownI_aDistributionMean[a] = true;
		}
		
		updateObjecitve();
	}
	
	
	//public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}
	
	
	/**
	 * Updates the objective depending on the model inputs
	 */
	protected void updateObjecitve(){
		//-------------------
		//(sampled problem if not all avgPositions are known exactly)
		//usingPriors, sampledProblem = (f,f): minimizeSlotDiff
		//usingPriors, sampledProblem = (f,t): spread_samples_linear (and USE_SAMPLING_CONSTRAINTS=true)
		//usingPriors, sampledProblem = (t,f): minimize_impression_prior_error
		//usingPriors, sampledProblem = (t,t): ??? (spread_samples_linear) : should be a combo, though
		//If we don't know any priors, don't use an objective that depends on it
		if (DESIRED_OBJECTIVE == Objective.DEPENDS_ON_CIRCUMSTANCES) {
			boolean sampledProblem = false;
			for (int a=0; a<_numAgents; a++) {
				if (!_isKnownMu_a[a]) sampledProblem = true; 
			}
		
			//Choose appropriate objective
			if (!_hasPrior && !sampledProblem) {
				//DESIRED_OBJECTIVE = Objective.MINIMIZE_SLOT_DIFF;
				DESIRED_OBJECTIVE = Objective.MAXIMIZE_IMPRESSIONS;
			} else if (!_hasPrior && sampledProblem) {
				DESIRED_OBJECTIVE = Objective.MINIMIZE_SAMPLE_MU_DIFF;
				//USE_SAMPLING_CONSTRAINTS = false;
				//USE_NEW_SAMPLING_CONSTRAINTS = false;
				//INTEGER_PROGRAM = true;
				
//				DESIRED_OBJECTIVE = Objective.MAXIMIZE_MIN_IMPRESSIONS_IN_SAMPLED_BUCKETS;
//				USE_SAMPLING_CONSTRAINTS = true;
//				USE_NEW_SAMPLING_CONSTRAINTS = true;
//				INTEGER_PROGRAM = false;
			} else if (_hasPrior && !sampledProblem) {
				DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR;
			} else { //usingPriors && sampledProblem
				DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR_AND_SAMPLE_MU_DIFF;
				//DESIRED_OBJECTIVE = Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR;
				//DESIRED_OBJECTIVE = Objective.MINIMIZE_SAMPLE_MU_DIFF;
			}
			
			System.out.println("Objective Update:  usingPriors: " + _hasPrior + ", sampledProblem: " + sampledProblem + ", objective: " + DESIRED_OBJECTIVE);
		}
	}
	
	//************************************ MAIN SOLVER METHOD ************************************

	public LPSolution solveIt(int[] dropout_a, double bestObj) {
		return solveIt(_numAgents, dropout_a, bestObj);
	}
	

	//Solve problem with some (potentially reduced) number of agents.
	//public abstract LPSolution solveIt(int effectiveNumAgents, int[] dropout_a, double bestObj);
	public LPSolution solveIt(int effectiveNumAgents, int[] dropout_a, double bestObj) {
		//System.out.println("solveIt: effectiveNumAgents=" + effectiveNumAgents + ", numSlots=" + numSlots + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout_a=" + Arrays.toString(dropout_a) + ", bestObj=" + bestObj + ", avgPos=" + Arrays.toString(knownMu_a) + ", sampledAvgPos=" + Arrays.toString(knownSampledMu_a));
		
		LPSolution solution = null; // The solution that will ultimately be returned.

		try {
			IloCplex cplex = new IloCplex();
			
			//-------------------------------- SET CPLEX PARAMS -------------------------------------
			if (SUPPRESS_OUTPUT) cplex.setOut(null);
			//cplex.setParam(IloCplex.DoubleParam.TiLim, TIMEOUT_IN_SECONDS);
			
			
			//-------------------------------- CREATE DECISION VARIABLES -------------------------------------
			IloNumVar[][] I_a_s = new IloNumVar[effectiveNumAgents][]; //(#imps per agent/slot)
			IloNumVar[] S_a = cplex.numVarArray(effectiveNumAgents, 0, _M);
			IloNumVar[] T_a = cplex.numVarArray(effectiveNumAgents, 1, _M);
			CPlexVariables vars = new CPlexVariables(I_a_s, S_a, T_a);
			
			for (int a=0; a<effectiveNumAgents; a++) {
				I_a_s[a] = new IloNumVar[a+1]; //cplex.numVarArray(a+1, 0, M);
				for (int s=0; s<=a; s++) {
					if (s < dropout_a[a]) {
						I_a_s[a][s] = cplex.numVar(0, 0);
					} else if (s==dropout_a[a] || s==a){
						I_a_s[a][s] = cplex.numVar(1, _M);
					} else {
						I_a_s[a][s] = cplex.numVar(0, _M);						
					}
				}
			}
			
			
			//Waterfall constraints
			//FIXME: Does this catch all cases? What about with two agents, both w/ integer avgPos.
			//  Seems that we are relying on the objective to maximize total impressions.
			for (int a=0; a<effectiveNumAgents; a++) {
				//for (int s=dropout_a[a]; s<=Math.min(a, _numSlots-1); s++) {
				for (int s=dropout_a[a]; s<=a; s++) {
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
			
			//Total number of impressions constraint:
			//Make sure "Total Agent Impressions" variable actually equals the sum of the agent's impressions per slot.
			for (int a=0; a<effectiveNumAgents; a++) {
				IloLinearNumExpr totalImps = cplex.linearNumExpr();
				for (int s=dropout_a[a]; s<=Math.min(a, _numSlots-1); s++) {
					totalImps.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImps, T_a[a]);
			}		
			
			//Total slot impressions constraint
			for (int s=0; s<effectiveNumAgents; s++) {
				IloLinearNumExpr totalImpsInSlot = cplex.linearNumExpr();
				for (int a=s; a<effectiveNumAgents; a++) {
					totalImpsInSlot.addTerm(1, I_a_s[a][s]);
				}
				cplex.addEq(totalImpsInSlot, S_a[s]);
			}
			
			//Slot totals must be (non-strictly) increasing
			for (int s=1; s<effectiveNumAgents; s++) {
				cplex.addGe(S_a[s-1], S_a[s]);
			}
			
			for (int a=0; a<effectiveNumAgents; a++) { 
				if (_isKnownI_a[a]) {
					cplex.addEq(T_a[a], _knownI_a[a]);
				}
			}
			
			postAveragePositionConstraints(cplex, vars, effectiveNumAgents);
			
			
			//-------------------------------- CREATE OBJECTIVE FUNCITON -------------------------------------
			postObjective(cplex, vars, effectiveNumAgents, bestObj);

			//----------------------------- PRINT AND SOLVE MODEL --------------------------------------------------
			//System.out.println("CarletonLP Objective: " + DESIRED_OBJECTIVE);
			if (!SUPPRESS_OUTPUT_MODEL) System.out.println("MODEL:\n" + cplex.getModel() + "\n\n\nEND MODEL\n");
			if ( cplex.solve() ) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				cplex.output().println("Objective function = " + cplex.getObjective());

				
				double objectiveVal = cplex.getObjValue();	
				double[] S_aVal = cplex.getValues(S_a);
				double[] T_aVal = cplex.getValues(T_a);
				double[][] I_a_sVal = new double[effectiveNumAgents][];
				for (int a=0; a<effectiveNumAgents; a++) {
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
	
	protected class CPlexVariables {
		IloNumVar[][] I_a_s;
		IloNumVar[] S_a;
		IloNumVar[] T_a;
		
		CPlexVariables(IloNumVar[][] I_a_s, IloNumVar[] S_a, IloNumVar[] T_a){
			this.I_a_s = I_a_s;
			this.S_a = S_a;
			this.T_a = T_a;
		}
	}


	protected abstract void postAveragePositionConstraints(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents) throws IloException;

	protected abstract void postObjective(IloCplex cplex, CPlexVariables vars, int effectiveNumAgents, double bestObj) throws IloException;

	
	
	
	

	protected void addObjective_minimizeImpressionPriorErrorSlotDiff(IloCplex cplex, IloNumVar[] S_a, IloNumVar[] T_a,
			double[] T_aPriorMean, double bestObj, int numSlots, int effectiveNumAgents) throws IloException  {
		
		int trueSlots = Math.min(numSlots, effectiveNumAgents);
		double maxImp = S_a[0].getUB();
		
		IloNumVar[] SDeltas = cplex.numVarArray(trueSlots-1, 0, 2*maxImp);
		IloNumVar[] IDeltas = cplex.numVarArray(effectiveNumAgents, 0, maxImp);
		
		//System.out.println(Arrays.toString(T_aPriorMean));
		
		//T_aPriorMean
		for (int a=0; a < effectiveNumAgents-1; a++) {
			if(T_aPriorMean[a] > 0){
				cplex.addGe(IDeltas[a], cplex.sum(T_a[a], -T_aPriorMean[a]));
				cplex.addGe(IDeltas[a], cplex.sum(cplex.negative(T_a[a]), T_aPriorMean[a]));
			} else {
				cplex.eq(IDeltas[a], 0);
			}
		}
		
		IloLinearNumExpr allIDeltas = cplex.linearNumExpr();
		//for (int a=0; a<numSlots-1; a++) {
		for(IloNumVar IDelta : IDeltas){
			allIDeltas.addTerm(1, IDelta);
		}
		
		
		for (int s=0; s < trueSlots-1; s++) {
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
		
		
		cplex.addMinimize(cplex.sum(cplex.prod(1.0, allSDeltas), allIDeltas));	
		//cplex.addMinimize(allSDeltas);	
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(cplex.sum(cplex.prod(1.0, allSDeltas), allIDeltas), bestObj);
			//cplex.addLe(allSDeltas, bestObj);
		}
		
	}





	protected void addObjective_minimizeSlotDiff(IloCplex cplex, IloNumVar[] S_a, double bestObj, int numSlots, int effectiveNumAgents) throws IloException  {
		int trueSlots = Math.min(numSlots, effectiveNumAgents);
		double maxImp = S_a[0].getUB();
		
		IloNumVar[] SDeltas = cplex.numVarArray(trueSlots-1, 0, 2*maxImp);
		
		for (int s=0; s < trueSlots-1; s++) {
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
		cplex.addMinimize(allSDeltas);		
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(allSDeltas, bestObj);
		}
	}


	
	protected void addObjective_minimizeSlotDiff_maxImpressions(IloCplex cplex, IloNumVar[] S_a, IloNumVar[] T_a, double bestObj, int numSlots, int effectiveNumAgents) throws IloException  {
		int trueSlots = Math.min(numSlots, effectiveNumAgents);
		double maxImp = S_a[0].getUB();
		
		IloNumVar[] SDeltas = cplex.numVarArray(trueSlots-1, 0, 2*maxImp);
		
		for (int s=0; s < trueSlots-1; s++) {
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
	
		IloLinearNumExpr allTs = cplex.linearNumExpr();
		//for (int a=0; a<numSlots-1; a++) {
		for(IloNumVar T : T_a){
			allTs.addTerm(1, T);
		}
		
		
		cplex.addMinimize(cplex.sum(cplex.prod(100.0, allSDeltas), allTs));	
		//cplex.addMinimize(allSDeltas);	
		
		//Must be better than previously best objective
		if (bestObj != -1) {
			cplex.addLe(cplex.sum(cplex.prod(100.0, allSDeltas), allTs), bestObj);
			//cplex.addLe(allSDeltas, bestObj);
		}
	}




	protected void addObjective_closeToImpressionsUpperBound(IloCplex cplex,
			IloNumVar[] T_a, double bestObj, int numAgents) throws IloException {
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
	protected void addObjective_minimizeImpressionPriorError(IloCplex cplex, IloNumVar[] T_a, double[] T_aPriorMean, double[] T_aPriorStdev, double bestObj, int numAgents) throws IloException {
		
		// These variables will determine how close the resulting waterfall is to our prior I_a distributions
		IloNumVar[] I_aError = cplex.numVarArray(numAgents, 0, MAX_ERROR);				
		
		// This constraint ensures that any difference between the waterfall's predicted impressions and 
		// our prior impression predictions is accounted for in an error term (some objectives will be trying to minimize this).
		IloLinearNumExpr relevantErrors = cplex.linearNumExpr();
		for (int a=0; a<numAgents; a++) {
			//Only consider this agent's impressions model if we don't know its exact impressions.
			if (_isKnownI_aDistributionMean[a] && !_isKnownI_a[a]) {
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
	


}
