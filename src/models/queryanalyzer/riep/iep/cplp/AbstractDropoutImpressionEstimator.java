package models.queryanalyzer.riep.iep.cplp;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;

import java.util.Arrays;


public abstract class AbstractDropoutImpressionEstimator implements AbstractImpressionEstimator {
	
   protected AbstractQAInstance _instance;
   
   //Gloabl Variables for DFS Search.
   ImpressionEstimationLP _IELP;
   double _bestObj;
   LPSolution _bestSol;
   int _checked;
   int _bestChecked;
   

   public AbstractDropoutImpressionEstimator(AbstractQAInstance inst) {
      _instance = inst;
   }

   public abstract ObjectiveGoal getObjectiveGoal();
   public abstract String getName();

   public AbstractQAInstance getInstance() {
      return _instance;
   }

   public abstract IEResult search(int[] order);
   /*   
   public IEResult search(int[] order) {
      //Impressions seen by each agent
      double[] I_a = new double[_advertisers];
      double[] I_aPromoted = new double[_advertisers];
      boolean[] promotionEligiblityVerified = new boolean[_advertisers];
      Arrays.fill(I_a, -1);
      Arrays.fill(I_aPromoted, -1);
      
      
      //Modify our index to reflect ordering change
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

 
      
      //---------------------------------------------------------------
      // Calling Carleton's search will go here.
      //---------------------------------------------------------------    
      carletonLP = new ImpressionEstimationLP(orderedI_a, orderedMu_a, orderedSampledMu_a, _slots, orderedI_aDistributionMean, orderedI_aDistributionStdev);

      
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
    	  int ceilingSlot = ((int)Math.ceil(orderedMu_a[a]))-1;
    	  int floorSlot = ((int)Math.floor(orderedMu_a[a]))-1;
    	  if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == _slots-1)){
    		  minDropOut[a] = ceilingSlot;
    	  } else {
    		  minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents?  
    	  }
    	  maxDropOut[a] = Math.min(Math.min(a,_slots-1),floorSlot);
      }
      
      System.out.println(Arrays.toString(orderedMu_a));
      System.out.println(Arrays.toString(minDropOut));
      System.out.println(Arrays.toString(maxDropOut));
      
      
      long t0 = System.currentTimeMillis();
      
      dropoutDFS(dropout, minDropOut, maxDropOut, 0, _slots, orderedMu_a, _imprUB, us, _ourImpressions);      
      //_bestSol <== best solution found after the search
      
      long runtime = System.currentTimeMillis() - t0;
      
      int allPossible = 1;
      for(int a=0; a < _advertisers; a++){
    	  allPossible *= maxDropOut[a]+1;
      }
      System.out.println("Checked: "+_checked+"("+_bestChecked+ ")"+" / "+allPossible);
      System.out.println("Runtime: "+runtime/1000.0+" - "+runtime/1000.0/_checked);
      
      //---------------------------------------------------------------
      // Calling Carleton's search ends here.
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

      return new IEResult(obj, unorderedImpsPerAgent, order.clone(), impsPerSlot, waterfall, agentNames);
   }
*/

   //this method will implictly assume length of dropout,minDropOut,maxDropOut,avgPos_a are the same and this is the number of agents.
   protected void dropoutDFS(int[] dropout, int[] minDropOut, int[] maxDropOut, int agent, int numSlots, double[] avgPos_a, int M, int us, int imp){
//	   System.out.println("dropoutDFS: dropout=" + Arrays.toString(dropout) + ", minDropOut=" + Arrays.toString(minDropOut) +
//			   ", maxDropOut=" + Arrays.toString(maxDropOut) + ", agent=" + agent + ", numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + 
//			   ", M=" + M + ", us=" + us + ", imp=" + imp);
	   
	   // this is the base case of the tree search, all drop outs are fixed.
	   if(agent >= dropout.length){
		   //System.out.println("Checking Dropouts: " + Arrays.toString(dropout));
	
		   //This is a leaf in the tree search, solve the LP and see if you have a better solution.
		   //System.out.println("Solve: numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout=" + Arrays.toString(dropout) + ", bestObj=" + _bestObj);
		   //System.out.println(Arrays.toString(dropout));
		   LPSolution sol = _IELP.solveIt(M, us, imp, dropout, _bestObj);
		   _checked++;
		   //Here I assume a null value means the solution is infeasible or some other problem occurred.
		   //If the solution is non-null we know it's the best solution found so far, because of the _bestObj bound in the LP.
		   if(sol != null){
			   //assert(sol.objectiveVal >= _bestObj) : "this property is implicit in the LP solveIt method";
			   
			   _bestSol = sol;
			   _bestObj = sol.objectiveVal;
			   _bestChecked = _checked;
			   
			   //System.out.println(sol);
			   System.out.println(_bestChecked+": "+_bestObj);
		   }
		   
	   } else {
		   //This if block is the only tricky part of the search.  Omitting it provides a simple test if the DFS is working correctly
		   //This block should only speed up the finding of good solutions, and should NOT cut off an optimial solution.
		   //The functions of this if are:
		   //  1) check if the waterfall is feasible up to this point (with our impressions this can be determined)
		   //     (we can also see if it's infeasible for the specified dropout points, regardless of our impressions.
		   //      e.g., impressions an agent would need would have to go beyond the specified bounds to be feasible.)
		   
		   
		   //if(ALWAYS_SOLVE_PARTIAL_PROBLEM || agent-1 == us){
		   if(agent >= us && agent > 0){
			   //System.out.println(agent+", "+us);
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
			   LPSolution sol = _IELP.solveIt(effectiveNumAgents, M, us, imp, dropout_tmp, _bestObj);
			   _checked++;
			   
			   //Here I assume a null value means the solution is infeasible or some other problem occurred.
			   if(sol == null){ //if infeasible we can backtrack immediately.
				   //TODO: If we change the LP objective so that meeting average positions is not a hard constraint,
				   //  we could have non-null solutions that are still extremely bad. May want the if condition to check the objective value.
				   return;
			   } 
		   }  
		   
		   /*
		   //does not appear to break things, but also does not seem to bring any improvement
		   if (BOUND_DROPOUT_SEARCH_BY_AVERAGE_POSITION) {
			   int maxDropoutForAvgPos = (int) Math.floor(avgPos_a[agent]) - 1;
			   if (maxDropoutForAvgPos < maxDropOutLocal[agent]) {
				   maxDropOutLocal[agent] = maxDropoutForAvgPos;
				   avgPosBoundCount++;
			   }
		   }
		   //*/
		   
		   //if feasiblity is ok, then lets continue with the search
		   for(int d=maxDropOut[agent]; d >= minDropOut[agent]; d--){
			   dropout[agent] = d; //I will modify this arrary in place, but if we parallize we would need to deep copy this
			   dropoutDFS(dropout, minDropOut, maxDropOut, agent+1, numSlots, avgPos_a, M, us, imp);
		   }
		   
	   }
	   
   }
   
   //this function simply applies the waterfall effect to one agent
   //It assumes slots are 0 based.
  
   
   /**
    * Simple check to make sure what is being accessed is in bounds.
    * @param I_a_s
    * @param a
    * @param s
    * @return
    */
   private boolean isInBounds(double[][] I_a_s, int a, int s) {
	   if (a >= 0 && a < I_a_s.length) {
		   if (s >= 0 && s < I_a_s[a].length) {
			   return true;
		   }
	   }
	   return false;
   }
   
   
   private int[] getImpsPerAgent(double[][] I_a_s) {
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

   
   private int[] getImpsPerSlot(double[][] I_a_s) {
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
