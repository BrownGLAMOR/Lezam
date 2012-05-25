package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

import java.util.Arrays;

public class CarletonLPImpressionEstimator implements AbstractImpressionEstimator {

   private ObjectiveGoal _objectiveGoal = ObjectiveGoal.MINIMIZE; //maximize or minimize?
   private QAInstance _instance;
   private int _advertisers;
   private int _slots;
   private int _promotedSlots;
   private double[] _trueAvgPos;
   private double[] _sampledAvgPos;
   private int _ourIndex;
   private int _ourImpressions;
   private int _ourPromotedImpressions;
   private boolean _ourPromotedEligibilityVerified;
   private boolean hitOurBudget;
   private int _imprUB;
   private int[] _agentImprUB;
   private int[] _agentImprLB;
   private double[] _agentImpressionDistributionMean;
   private double[] _agentImpressionDistributionStdev;

   boolean INTEGER_PROGRAM;
   boolean USE_EPSILON = true;
   int NUM_SAMPLES = 10;
   boolean USE_RANKING_CONSTRAINTS;
   boolean MULTIPLE_SOLUTIONS; //Have the MIP return multiple solutions and evaluate with a better objective?
   double TIMEOUT_IN_SECONDS;

   double _bestObj;
   LPSolution _bestSol;
   int _checked;
   int _bestChecked;
   
   CarletonLP carletonLP;
   boolean BRANCH_AND_BOUND = true;
   
   
   static int boundCount = 0;
   
   public CarletonLPImpressionEstimator(QAInstance inst, boolean useRankingConstraints, boolean integerProgram, boolean multipleSolutions, double timeoutInSeconds) {
      TIMEOUT_IN_SECONDS = timeoutInSeconds;
      MULTIPLE_SOLUTIONS = multipleSolutions;
      INTEGER_PROGRAM = integerProgram;
      USE_RANKING_CONSTRAINTS = useRankingConstraints;
      _instance = inst;
      _advertisers = inst.getNumAdvetisers();
      _slots = inst.getNumSlots();
      _promotedSlots = inst.getNumPromotedSlots();
      _trueAvgPos = inst.getAvgPos();
      _sampledAvgPos = inst.getSampledAvgPos();
      _ourIndex = inst.getAgentIndex(); //TODO is this ID or Index?
      _ourImpressions = inst.getImpressions();
      _ourPromotedImpressions = inst.getPromotedImpressions();
      _ourPromotedEligibilityVerified = inst.getPromotionEligibilityVerified();
      hitOurBudget = inst.getHitOurBudget();
      _imprUB = inst.getImpressionsUB();
      _agentImpressionDistributionMean = inst.getAgentImpressionDistributionMean();
      _agentImpressionDistributionStdev = inst.getAgentImpressionDistributionStdev();      
   }

   public ObjectiveGoal getObjectiveGoal() {
      return _objectiveGoal;
   }

   public String getName() {
      if (INTEGER_PROGRAM) {
         return "IP";
      } else {
         return "LP";
      }
   }

   public QAInstance getInstance() {
      return _instance;
   }

   public IEResult search(int[] order) {
      //Impressions seen by each agent
      double[] I_a = new double[_advertisers];
      double[] I_aPromoted = new double[_advertisers];
      boolean[] promotionEligiblityVerified = new boolean[_advertisers];
      Arrays.fill(I_a, -1);
      Arrays.fill(I_aPromoted, -1);
      
      
      //Modify our index to reflect ordering change
      // NO!: int us = order[_ourIndex];
      //TODO: Is this correct?
      //  if order[i]==x, the agent in the ith slot can be found at index x.
      //  e.g., if order[i]==x, the agent in the ith slot had I_a[x] impressions. Or I_a[order[i]] impressions
      //  "us" is meant to be the slot that we were in. So we should find the index i s.t. order[i]=_ourIndex 
      int us = -1;
      for (int i=0; i<order.length; i++) {
    	  if (order[i] == _ourIndex) {
    		  us = i;
    	  }
      }
      
      
      I_a[_ourIndex] = _ourImpressions;
      I_aPromoted[_ourIndex] = _ourPromotedImpressions;
      promotionEligiblityVerified[_ourIndex] = _ourPromotedEligibilityVerified;

      int[] hitBudget = new int[_advertisers];
      Arrays.fill(hitBudget, -1);
      hitBudget[_ourIndex] = (hitOurBudget) ? 1 : 0;

      //Average position for each agent
      double[] mu_a = _trueAvgPos;
      double[] sampledMu_a = _sampledAvgPos;

      //Reorder values according to the specified order
      double[] orderedI_a = order(I_a, order);
      double[] orderedMu_a = order(mu_a, order);
      double[] orderedSampledMu_a = order(sampledMu_a, order);
      double[] orderedI_aPromoted = order(I_aPromoted, order);
      boolean[] orderedPromotionEligibilityVerified = order(promotionEligiblityVerified, order);
      int[] orderedHitBudget = order(hitBudget, order);
      double[] orderedI_aDistributionMean = order(_agentImpressionDistributionMean, order);
      double[] orderedI_aDistributionStdev = order(_agentImpressionDistributionStdev, order);

      

      //Temporary. If exact average position is unknown, fill in with sampled average position
      //FIXME: We should pass in both the exact and sampled average positions to the solver, so that it can treat them differently.
      for (int i=0; i<orderedMu_a.length; i++) {
    	  if (orderedMu_a[i] == -1) {
    		  orderedMu_a[i] = orderedSampledMu_a[i];
    	  }
      }
      
      
      //---------------------------------------------------------------
      //TODO Calling Carleton's search will go here.
      //---------------------------------------------------------------    
      carletonLP = new CarletonLP(orderedMu_a, orderedSampledMu_a, _slots, orderedI_aDistributionMean, orderedI_aDistributionStdev);

      
      //this causes pruning, and thus algorithm correctness, 
      //must be a global variable to share between various branches of the DFS
      _bestObj = -1; //FIXME: If we change objective functions, we'll have to make sure objectives can't be negative (since -1 could otherwise naturally arise)
      _bestSol = null; //this is kept in sync with _bestObj so we can recovery the best solution
      
      _checked = 0; //just for stats collecting
      _bestChecked = -1; //just for stats collecting
      
      int[] dropout = new int[_advertisers];
      int[] minDropOut = new int[_advertisers];
      int[] maxDropOut = new int[_advertisers];
      
      //set default values
      for(int a=0; a < _advertisers; a++){
    	  dropout[a] = -1;
    	  minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents?
    	  maxDropOut[a] = a;
      }
      
      
      dropoutDFS(dropout, minDropOut, maxDropOut, 0, _slots, orderedMu_a, _imprUB, us, _ourImpressions);
      
      //_bestSol <== best solution found after the search
      
      //---------------------------------------------------------------
      //TODO Calling Carleton's search ends here.
      //---------------------------------------------------------------    
      if (_bestSol == null) {
    	  return null;
      }

      
      double[][] I_a_s = _bestSol.I_a_s;


      int[][] waterfall = new int[I_a_s.length][I_a_s[0].length];
      for(int i = 0; i < waterfall.length; i++) {
         for(int j = 0; j < waterfall[i].length; j++) {
            waterfall[i][j] = (int) I_a_s[i][j];
         }
      }
	
      //relativeRanking[i]: the agent in initial position i had index relativeRanking[i]
      int[] relativeRanking = order.clone(); //result.getOrdering();

      //Convert this into the IEResult that Carleton's QueryAnalyzer likes.
      double obj = _bestSol.objectiveVal;
      int[] impsPerAgent = getImpsPerAgent(I_a_s);
      int[] impsPerSlot = getImpsPerSlot(I_a_s);

//		System.out.println("agents=" + _advertisers + "\tslots=" + _slots + "\tI_a_s size=" + I_a_s.length + " " + I_a_s[0].length);
      //TODO: do we have to undo the ordering?
      int[] unorderedImpsPerAgent = unorder(impsPerAgent, order);

      //TODO: WHY IS ORDER CALLED??? 
      //This is pretty confusing. When impsPerAgent is ordered, the impressions each agent saw are shuffled.
      //The end result has each index corresponding to an agent.
      //However, the result.getOrdering() does not have its slots corresponding to agent indices.
      //So calling unorder doesn't work.
      //But somehow, calling order() works?? (at least it seems to be empirically)
      //Confusing.
      //FIXME: This is probably a dormant bug that will break things if the LDS is used with the 
      //unranked version of the problem (which we never do). (verify?) [yes, it does.]
      int[] unorderedRelativeRanking = order(relativeRanking, order);

      //System.out.println("RelativeRanking=" + Arrays.toString(relativeRanking) + ", unorderedRelativeRanking=" + Arrays.toString(unorderedRelativeRanking));

      return new IEResult(obj, unorderedImpsPerAgent, order.clone(), impsPerSlot, waterfall);
   }


   //this method will implictly assume length of dropout,minDropOut,maxDropOut,avgPos_a are the same and this is the number of agents.
   private void dropoutDFS(int[] dropout, int[] minDropOut, int[] maxDropOut, int agent, int numSlots, double[] avgPos_a, int M, int us, int imp){
//	   System.out.println("dropoutDFS: dropout=" + Arrays.toString(dropout) + ", minDropOut=" + Arrays.toString(minDropOut) +
//			   ", maxDropOut=" + Arrays.toString(maxDropOut) + ", agent=" + agent + ", numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + 
//			   ", M=" + M + ", us=" + us + ", imp=" + imp);
	   //System.out.println(agent);
	   //System.out.println(dropout.length);
	   //System.out.println(Arrays.toString(dropout));
	   
	   
	   boolean DEBUG = false;
	   if (DEBUG) {
		   int[] correctDropout = {0, 0, 0, 0, 0, 1, 1, 2};
		   _bestSol = carletonLP.solveIt(M, us, imp, correctDropout, _bestObj);
		   return;
	   }
	   
	   
	   int[] minDropOutLocal = Arrays.copyOf(minDropOut, avgPos_a.length);
	   //System.out.println(Arrays.toString(maxDropOut));
	   //System.out.println(Arrays.toString(minDropOutLocal));
	   
	   
	   // this is the base case of the tree search, all drop outs are fixed.
	   if(agent >= dropout.length){
		   //System.out.println("Checking Dropouts: " + Arrays.toString(dropout));
	
		   //This is a leaf in the tree search, solve the LP and see if you have a better solution.
		   //System.out.println("Solve: numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout=" + Arrays.toString(dropout) + ", bestObj=" + _bestObj);
		   LPSolution sol = carletonLP.solveIt(M, us, imp, dropout, _bestObj);
		   _checked++;
		   //Here I assume a null value means the solution is infeasible or some other problem occurred.
		   //If the solution is non-null we know it's the best solution found so far, becouse of the _bestObj bound in the LP.
		   if(sol != null){
			   _bestSol = sol;
			   _bestObj = sol.objectiveVal;
			   _bestChecked = _checked;
		   }
		   
	   } else {

		   //This if block is the only tricky part of the search.  Omitting it provides a simple test if the DFS is working correctly
		   //This block should only speed up the finding of good solutions, and should NOT cut off an optimial solution.
		   //The functions of this if are:
		   //  1) check if the waterfall is feasible up to this point (with our impressions this can be determined)
		   //  2) IF the waterfall is feasible, then we can apply the greedy algorithm to all agents below us, until we hit a whole number avg pos.
		   //  3) the solution to the greedy algorithm provides tight bounds on the drop out of all agents it was applied to.
		   //*
		   if(agent-1 == us){
			   //At this step we want to solve the problem with agents "0" through "agent-1", I think this will work, maybe off by one;
			   double[] avgPos_tmp = new double[agent];
			   for(int i=0; i < agent; i++){
				   avgPos_tmp[i] = avgPos_a[i];
			   }
			   int[] dropout_tmp = new int[agent];
			   for(int i=0; i < agent; i++){
				   dropout_tmp[i] = dropout[i];
			   }
			   //System.out.println("Tricky Solve: numSlots=" + numSlots + ", avgPos_tmp=" + Arrays.toString(avgPos_tmp) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout_tmp=" + Arrays.toString(dropout_tmp) + ", bestObj=" + _bestObj);
			   int effectiveNumAgents = agent;
			   LPSolution sol = carletonLP.solveIt(effectiveNumAgents, M, us, imp, dropout_tmp, _bestObj);
			   _checked++;
			   //Here I assume a null value means the solution is infeasible or some other problem occurred.
			   if(sol == null){ //if infeasible we can backtrack immidately.
				   //TODO: If we change the LP objective so that meeting average positions is not a hard constraint,
				   //  we could have non-null solutions that are still extremely bad. May want the if condition to check the objective value.
				   return;
			   } else {
				   
				   //If feasible but a terrible solution (compared to the best found so far), we can also backtrack immediately 
				   if (BRANCH_AND_BOUND && _bestSol != null) {
					   
					   //Note: This may not work if objective values are not monotonically increasing/decreasing as a more complete solution is considered.
					   //We are assume that, if the objective is to minimize (e.g., error), the objective value of a partial solution will only grow larger that it currently is.
					   //Similarly, if the objective is to maximize the objective value will only grow smaller as more of the problem is considered.
					   //  [Note that "Maximizing total impressions" violates this assumption, so we can't use this branch and bound method without bounding later value.]
					   if (_objectiveGoal==ObjectiveGoal.MINIMIZE && sol.objectiveVal >= _bestObj || 
							   _objectiveGoal==ObjectiveGoal.MAXIMIZE && sol.objectiveVal <= _bestObj ) {
						   //Objective value is already worse than best solution, and will only get worse. Stop trying.
						   boundCount++;
						   System.out.println("Bound!");
						   return;
					   }
				   }
				   
				   
				   //we can check if the waterfall may be applied!
				   //I couldn't test this, so hi-probbality of buggy-ness!
				   //can be commented out without harm.
			        
			        double[] simps = new double[avgPos_a.length];
			        for(int s=0; s < avgPos_tmp.length; s++){ //assuming slots are 0 indexed
			          simps[s] = sol.S_a[s];
			        }
			        
			        for(int a2=agent; a2 < avgPos_a.length; a2++){
			          //range PSlots = 1..a2;
			          double[] pimps = new double[a2+1];
			          for(int s=0; s < a2;  s++){
			            pimps[s] = simps[s];
			          }
			          
			          double[] slotImps = calcMinDropOut(a2, numSlots, avgPos_a[a2], pimps);
			          
			          for(int s=0; s <= a2;  s++){
			            simps[s] += slotImps[s];
			          }

			          //Find the lowest slot (highest on the page) that this agent saw impressions, according to the waterfall.
			          //This agent's dropout point is constrained to be no lower than this. 
			          //TODO: Isn't this agent's dropout point constrained to this exactly?
			          //  Why just fixing the minDropout? Should the max also be fixed for that agent?
			          for(int s=0; s < a2;  s++){
			        	minDropOutLocal[a2] = s;
			            if(slotImps[s] > 0.1){
			              break;
			            }
			          }
			          //cout << a2 << " : " << slotImps << " - " << minDropOut[a2] << endl;

			          //Continue running the waterfall for agents starting in higher slots until an agent has an integer average position.
			          //if((avgPos_a[a2]-(int)Math.ceil(avgPos_a[a2])) == 0){ //this is a crappy check if an avg pos is a whole number, got a better one?
			          if (avgPos_a[a2] == (int)avgPos_a[a2]) {
			            break;
			          }
			        }
			   }
		   }  
		   //*/
		   
		   
		   //if feasiblity is ok, then lets continue with the search
		   for(int d=maxDropOut[agent]; d >= minDropOutLocal[agent]; d--){
			   dropout[agent] = d; //I will modify this arrary in place, but if we parallize we would need to deep copy this
			   
			   dropoutDFS(dropout, minDropOutLocal, maxDropOut, agent+1, numSlots, avgPos_a, M, us, imp);
		   }
		   
	   }
	   
   }
   
   //this function simply applies the waterfall effect to one agent
   //It assumes slots are 0 based.
   private static double[] calcMinDropOut(int agentInSlot, int slotLimit, double avgPos, double[] slotImp){
	   //System.out.println("calcMinDropOut: slots=" + agentInSlot + ", slotLimit=" + slotLimit + ", avgPos=" + avgPos + ", slotImp=" + Arrays.toString(slotImp));
	   
	   double[] currSlotImp = new double[agentInSlot+1];
	   Arrays.fill(currSlotImp, 0);
	   
	   if((avgPos-(int)Math.ceil(avgPos)) == 0){ //this is a crappy check if an avg pos is a whole number, got a better one?
		   //FIXME: Is filling all slots with value 1 meaningful? And it seems we shouldn't have called this if the agent had integer average position.
		   Arrays.fill(currSlotImp, 1); 
	     return currSlotImp;
	   } 

	   int currSlot = agentInSlot;
	   currSlotImp[currSlot] = 0;
	   double tmpAvgPos = agentInSlot;
	   while(currSlot > 0){
	     currSlotImp[currSlot] = slotImp[currSlot-1] - slotImp[currSlot];

	     //???
	     double currentImpSum = 0;
	     for(int s=0; s <= slotLimit && s <= agentInSlot; s++){
	    	 currentImpSum += currSlotImp[s];
	     }
	     
	     if(currentImpSum > 0){
	       double currentImpWeightedSum = 0;
	       for(int s=0; s <= slotLimit && s <= agentInSlot; s++){
	    	   currentImpWeightedSum += (s+1)*currSlotImp[s];
		   }
	       tmpAvgPos = currentImpWeightedSum/currentImpSum;
	     }
	     
	     
	     //cout << currSlot << " : " << tmpAvgPos << " - " << avgPos << endl;
	     //System.out.println("currSlot=" + currSlot + ", tmpAvgPos=" + tmpAvgPos + ", avgPos=" + avgPos);
	     if(tmpAvgPos <= avgPos){
	       break;
	     } else {
	       currSlot = currSlot-1;
	     }
	   }
	   
	   //----
	   //Calculate number of impressions the agent saw in its final slot
	   //----
	   
	   double currentImpSum = 0;
	   double currentImpWeightedSum = 0;
	   for(int s=currSlot+1; s <= slotLimit && s <= agentInSlot; s++){
		   currentImpSum += currSlotImp[s];
		   currentImpWeightedSum += (s+1)*currSlotImp[s];
	   }
	   //System.out.println("currentImpSum=" + currentImpSum + ", currentImpWeightedSum=" + currentImpWeightedSum);
	   
	   double finalImps = (avgPos*currentImpSum - currentImpWeightedSum) / ((currSlot+1)-avgPos);
	   //cout << finalImps << endl;
	   currSlotImp[currSlot] = finalImps;
	   //cout << currSlotImp << " - " << (sum(s in Slots : s <= slotLimit) s*currSlotImp[s])/(sum(s in Slots : s <= slotLimit) currSlotImp[s]) << " : " << avgPos << endl;
	   //System.out.println("  currSlotImp=" + Arrays.toString(currSlotImp));
	   return currSlotImp;
   }
   
   
   
   /**
    * Simple check to make sure what is being accessed is in bounds.
    * @param I_a_s
    * @param a
    * @param s
    * @return
    */
   public boolean isInBounds(double[][] I_a_s, int a, int s) {
	   if (a >= 0 && a < I_a_s.length) {
		   if (s >= 0 && s < I_a_s[a].length) {
			   return true;
		   }
	   }
	   return false;
   }
   
   
   public int[] getImpsPerAgent(double[][] I_a_s) {
      int[] impsPerAgent = new int[_advertisers];
      for (int a = 0; a < _advertisers; a++) {
    	  for (int s=0; s<_slots; s++) {
    		  if (isInBounds(I_a_s, a, s)) {
    			  impsPerAgent[a] += Math.round(I_a_s[a][s]);
    		  }
         }
      }
      return impsPerAgent;
   }

   
   public int[] getImpsPerSlot(double[][] I_a_s) {
      int[] impsPerSlot = new int[_slots];
      for (int s = 0; s < _slots; s++) {
         for (int a = 0; a < _advertisers; a++) {
        	 if (isInBounds(I_a_s, a, s)) {
        		 impsPerSlot[s] += Math.round(I_a_s[a][s]);
        	 }
         }
      }
      return impsPerSlot;
   }


   /**
    * Reorder the specified array. order's ith value returns
    * the index of the original array that should be moved to
    * the ith position in the new array.
    *
    * @param arr
    * @param order
    * @return
    */
   private static double[] order(double[] arr, int[] order) {
      double[] orderedArr = new double[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   private static int[] order(int[] arr, int[] order) {
      int[] orderedArr = new int[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   private static int[] unorder(int[] orderedArr, int[] order) {
      int[] arr = new int[orderedArr.length];
      for (int i = 0; i < order.length; i++) {
         arr[order[i]] = orderedArr[i];
      }
      return arr;
   }


//	//FIXME DEBUG: Don't do any ordering (see how the alg performs)
//	private static double[] order(double[] arr, int[] order) {
//		return arr.clone();
//	}
//	private static int[] unorder(int[] orderedArr, int[] order) {
//		return orderedArr.clone();
//	}
//	//END FIXME DEBUG


   private static boolean[] order(boolean[] arr, int[] order) {
      boolean[] orderedArr = new boolean[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   private static double[] unorder(double[] orderedArr, int[] order) {
      double[] arr = new double[orderedArr.length];
      for (int i = 0; i < order.length; i++) {
         arr[order[i]] = orderedArr[i];
      }
      return arr;
   }



   public static void testOrdering() {
      double[] a = {90, 80, 70, 10, 20, 30};
      int[] order = {0, 1, 2, 5, 4, 3};
      double[] orderedArr = order(a, order);
      double[] unorderedArr = unorder(orderedArr, order);

      System.out.println("a: " + Arrays.toString(a));
      System.out.println("order: " + Arrays.toString(order));
      System.out.println("orderedArr: " + Arrays.toString(orderedArr));
      System.out.println("unorderedArr: " + Arrays.toString(unorderedArr));
   }

   
   
   /**
    * Main method for testing.
    */
   public static void main(String[] args) {

	   
//	   int agentInSlot=3;
//	   int slotLimit=5;
//	   double avgPos=1.5;
//	   double[] slotImp={500, 300, 200, 0};
//	   double[] impsPerSlot = CarletonLPImpressionEstimator.calcMinDropOut(agentInSlot, slotLimit, avgPos, slotImp);
//	   System.out.println(Arrays.toString(impsPerSlot));
//	   System.exit(0);
	   
	   
      //EricImpressionEstimator.testOrdering();

      //err=[2800.0, 2702.0, 0.0]	pred=[398, 579, 202]	actual=[3198, 3281, 202]	g=1 d=8 a=2 q=(Query (null,null)) avgPos=[1.0, 2.0362694300518136, 2.0] bids=[0.3150841472838487, 0.126159214933152, 0.13126460037679655] imps=[3198, 3281, 202] order=[0, 2, 1] IP

	   
//	      for (int ourAgentIdx = 0; ourAgentIdx <= 0; ourAgentIdx++) {
//	          //These aren't actually used; everything is -1 except the current agentIdx
//	          double[] I_aFull = {300, 600};
//	          double[] mu_aFull = {1.0, 1.5};
//	          double[] agentImpressionDistributionMean = {-1, -1};
//	          double[] agentImpressionDistributionStdev = {-1, -1};
//	          double[] mu_a = new double[mu_aFull.length];
//	          Arrays.fill(mu_a, -1);
//	          mu_a[ourAgentIdx] = mu_aFull[ourAgentIdx];
//	          double[] I_aPromoted = {-1, -1};
//	          boolean[] isKnownPromotionEligible = {false, false};
//	          double[] knownSampledMu_a = {1.0, 1.5}; 
//	          int numSlots = 5;
//	          int numPromotedSlots = 0;
//	          int ourImpressions = (int) I_aFull[ourAgentIdx];
//	          int ourPromotedImpressions = (int) I_aPromoted[ourAgentIdx];
//	          boolean ourPromotionKnownAllowed = isKnownPromotionEligible[ourAgentIdx];
//	          int impressionsUB = 1300;
//	          int numAgents = mu_a.length;
//	          boolean hitOurBudget = true; //Did we hit our budget? (added constraint if we didn't)
//	          int[] predictedOrder = {-1, -1, -1, -1, -1, -1, -1, -1};
//	          int[] agentIds = new int[numAgents]; //Give arbitrary agent IDs
//	          for (int i = 0; i < agentIds.length; i++) {
//	             agentIds[i] = -(i + 1);
//	          }
//
//	          QAInstance carletonInst = new QAInstance(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, true, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder);
//	          QAInstance ericInst = new QAInstance(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder);
//	          ImpressionEstimatorExact carletonImpressionEstimator = new ImpressionEstimatorExact(carletonInst);
//	          EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst, false, true, false, 10);
//	          CarletonLPImpressionEstimator carletonLP = new CarletonLPImpressionEstimator(ericInst, false, true, false, 5);
//	          double[] cPos = carletonImpressionEstimator.getApproximateAveragePositions();
//	          //int[] cOrder = QAInstance.getAvgPosOrder(cPos);
//	          int[] cOrder = {0, 1};
////	          IEResult carletonResult = carletonImpressionEstimator.search(cOrder);
////	          IEResult ericResult = ericImpressionEstimator.search(cOrder);
//	          IEResult carletonLPResult = carletonLP.search(cOrder);
//
//	          System.out.println("ourAgentIdx=" + ourAgentIdx);
////	          System.out.println("  Carleton: " + carletonResult + "\tactual=" + Arrays.toString(I_aFull));
////	          System.out.println("  IP: " + ericResult + "\tactual=" + Arrays.toString(I_aFull));
//	          System.out.println("  CarletonLP: " + carletonLPResult + "\tactual=" + Arrays.toString(I_aFull));
//	       }
	   
	   
	   
	   
	   
	   
	   
//	  for (int ourAgentIdx = 2; ourAgentIdx <= 2; ourAgentIdx++) {
      for (int ourAgentIdx = 3; ourAgentIdx <= 3; ourAgentIdx++) {
         //These aren't actually used; everything is -1 except the current agentIdx
//    	 double[] I_aFull = {304, 603, 805, 841, 1037, 697, 434, 232};
     	 double[] I_aFull = {232, 304, 603, 805, 841, 1037, 697, 434};
//    	 double[] mu_aFull = {1, 1.50414594, 2.12670807, 3.03567182, 3.46190935, 3.91822095, 3.93087558, 4}; 
    	 double[] mu_aFull = {4, 1, 1.50414594, 2.12670807, 3.03567182, 3.46190935, 3.91822095, 3.93087558}; 
         
         //Get priors on impressions
//         double[] agentImpressionDistributionMean = {-1, -1, -1, -1, -1, -1, -1, -1};
//         double[] agentImpressionDistributionStdev = {-1, -1, -1, -1, -1, -1, -1, -1};
         double[] agentImpressionDistributionMean = {200, 300, 600, 800, 800, 1000, 700, 400};
         double[] agentImpressionDistributionStdev = {50, 50, 50, 50, 50, 50, 50, 50};

         //Get observed exact average positions (we only see one)
         double[] mu_a = new double[mu_aFull.length];
         Arrays.fill(mu_a, -1);
         mu_a[ourAgentIdx] = mu_aFull[ourAgentIdx];

         double[] I_aPromoted = {-1, -1, -1, -1, -1, -1, -1, -1};
         boolean[] isKnownPromotionEligible = {false, false, false, false, false, false, false, false};

//         double[] knownSampledMu_a = {1, 1.50414594, 2.12670807, 3.03567182, 3.46190935, 3.91822095, 3.93087558, 4}; 
         double[] knownSampledMu_a = {4, 1, 1.50414594, 2.12670807, 3.03567182, 3.46190935, 3.91822095, 3.93087558};
         int numSlots = 5;
         int numPromotedSlots = 0;

         int ourImpressions = (int) I_aFull[ourAgentIdx];
         int ourPromotedImpressions = (int) I_aPromoted[ourAgentIdx];
         boolean ourPromotionKnownAllowed = isKnownPromotionEligible[ourAgentIdx];
         int impressionsUB = 1300;
         int numAgents = mu_a.length;

         //Did we hit our budget? (added constraint if we didn't)
         boolean hitOurBudget = true;

         int[] predictedOrder = {-1, -1, -1, -1, -1, -1, -1, -1};

         //Give arbitrary agent IDs
         int[] agentIds = new int[numAgents];
         for (int i = 0; i < agentIds.length; i++) {
            agentIds[i] = -(i + 1);
         }

         QAInstance carletonInst = new QAInstance(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, true, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder);
         QAInstance ericInst = new QAInstance(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder);
         ImpressionEstimatorExact carletonImpressionEstimator = new ImpressionEstimatorExact(carletonInst);
         EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst, false, true, false, 10);
         CarletonLPImpressionEstimator carletonLP = new CarletonLPImpressionEstimator(ericInst, false, true, false, 5);

         double[] cPos = carletonImpressionEstimator.getApproximateAveragePositions();
         //int[] cOrder = QAInstance.getAvgPosOrder(cPos);
//         int[] cOrder = {0, 1, 2, 3, 4, 5, 6, 7};
         int[] cOrder = {1, 2, 3, 4, 5, 6, 7, 0};
         System.out.println("cPos: " + Arrays.toString(cPos) + ", cOrder: " + Arrays.toString(cOrder));
         IEResult carletonResult = carletonImpressionEstimator.search(cOrder);
         System.out.println("cOrder: " + Arrays.toString(cOrder));
         IEResult ericResult = ericImpressionEstimator.search(cOrder);
         System.out.println("cOrder: " + Arrays.toString(cOrder));
         IEResult carletonLPResult = carletonLP.search(cOrder);
         System.out.println("cOrder: " + Arrays.toString(cOrder));

         System.out.println("ourAgentIdx=" + ourAgentIdx);
         System.out.println("  Carleton: " + carletonResult + "\tactual=" + Arrays.toString(I_aFull));
         System.out.println("  IP: " + ericResult + "\tactual=" + Arrays.toString(I_aFull));
         System.out.println("  CarletonLP: " + carletonLPResult + "\tactual=" + Arrays.toString(I_aFull));
      }
      
      
      
      
   }



   public double[] getApproximateAveragePositions() {
      double[] avgPos = new double[_advertisers];
      for (int i=0; i<_advertisers; i++) {
         if (_trueAvgPos[i] != -1) avgPos[i] = _trueAvgPos[i];
         else if (_sampledAvgPos[i] != -1) avgPos[i] = _sampledAvgPos[i];
         else avgPos[i] = -1; //FIXME: Make this something other than a negative value!
      }
      return avgPos;
   }


}
