package models.queryanalyzer.riep.iep.cplp;

import java.util.Arrays;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

public abstract class AbstractDropoutImpressionEstimator implements AbstractImpressionEstimator {
	
   protected AbstractQAInstance _inst;
   
   //Gloabl Variables for DFS Search.
   //protected ImpressionEstimationLP _IELP;
   protected double _bestObj;
   protected LPSolution _bestSol;
   protected int _checked;
   protected int _bestChecked;
   protected int _us;

   public AbstractDropoutImpressionEstimator(AbstractQAInstance inst) {
      _inst = inst;
   }

   public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}
   
   public abstract String getName();

   public AbstractQAInstance getInstance() {return _inst;}

   public abstract IEResult search(int[] order);
   
   protected IEResult search(int[] order, ImpressionEstimationLP IELP, int[] minDropOut, int[] maxDropOut){
	  assert(order.length == minDropOut.length && order.length == maxDropOut.length);
	  
	  _bestObj = -1; //FIXME: If we change objective functions, we'll have to make sure objectives can't be negative (since -1 could otherwise naturally arise)
      _bestSol = null; //this is kept in sync with _bestObj so we can recovery the best solution
      
      _checked = 0; //just for stats collecting
      _bestChecked = -1; //just for stats collecting
      
      int[] dropout = new int[order.length];
      Arrays.fill(dropout, -1);
      
      long t0 = System.currentTimeMillis();
      
      dropoutDFS(IELP, dropout, minDropOut, maxDropOut, 0);      
      //_bestSol <== best solution found after the search
      
      long runtime = System.currentTimeMillis() - t0;
      
      int allPossible = 1;
      for(int a=0; a < maxDropOut.length; a++){
    	  allPossible *= maxDropOut[a]+1;
      }
      System.out.println("Checked: "+_checked+"("+_bestChecked+ ")"+" / "+allPossible);
      System.out.println("Runtime: "+runtime/1000.0+" - "+runtime/1000.0/_checked);
      
      if (_bestSol == null) { //no solution found  =(
    	  return null;
      } else { //solution found! need to un-order it...

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

//    			System.out.println("agents=" + _advertisers + "\tslots=" + _slots + "\tI_a_s size=" + I_a_s.length + " " + I_a_s[0].length);
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

          return new IEResult(obj, unorderedImpsPerAgent, order.clone(), impsPerSlot, waterfall, _inst.getAgentNames());
      }
   }
  
   
   //this method will implictly assume length of dropout,minDropOut,maxDropOut,avgPos_a are the same and this is the number of agents.
   protected void dropoutDFS(ImpressionEstimationLP IELP, int[] dropout, int[] minDropOut, int[] maxDropOut, int agent){
//	   System.out.println("dropoutDFS: dropout=" + Arrays.toString(dropout) + ", minDropOut=" + Arrays.toString(minDropOut) +
//			   ", maxDropOut=" + Arrays.toString(maxDropOut) + ", agent=" + agent + ", numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + 
//			   ", M=" + M + ", us=" + us + ", imp=" + imp);
	   
	   // this is the base case of the tree search, all drop outs are fixed.
	   if(agent >= dropout.length){
		   //System.out.println("Checking Dropouts: " + Arrays.toString(dropout));
	
		   //This is a leaf in the tree search, solve the LP and see if you have a better solution.
		   //System.out.println("Solve: numSlots=" + numSlots + ", avgPos=" + Arrays.toString(avgPos_a) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout=" + Arrays.toString(dropout) + ", bestObj=" + _bestObj);
		   //System.out.println(Arrays.toString(dropout));
		   LPSolution sol = IELP.solveIt(dropout, _bestObj);
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
		   if(agent >= _us && agent > 0){
			   //System.out.println(agent+", "+us);
			   //At this step we want to solve the problem with agents "0" through "agent-1", I think this will work, maybe off by one;
			   int[] dropout_tmp = new int[agent];
			   for(int i=0; i < agent; i++){
				   dropout_tmp[i] = dropout[i];
			   }
			   //System.out.println("Tricky Solve: numSlots=" + numSlots + ", avgPos_tmp=" + Arrays.toString(avgPos_tmp) + ", M=" + M + ", us=" + us + ", imp=" + imp + ", dropout_tmp=" + Arrays.toString(dropout_tmp) + ", bestObj=" + _bestObj);
			   int effectiveNumAgents = agent;
			   LPSolution sol = IELP.solveIt(effectiveNumAgents, dropout_tmp, _bestObj);
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
			   dropoutDFS(IELP, dropout, minDropOut, maxDropOut, agent+1);
		   }
		   
	   }
	   
   }
   
   //this function simply applies the waterfall effect to one agent
   //It assumes slots are 0 based.
  
   
   private boolean isInBounds(double[][] I_a_s, int a, int s) {
	   if (a >= 0 && a < I_a_s.length) {
		   if (s >= 0 && s < I_a_s[a].length) {
			   return true;
		   }
	   }
	   return false;
   }
   
   
   protected int[] getImpsPerAgent(double[][] I_a_s) {
      int[] impsPerAgent = new int[_inst.getNumAdvetisers()];
      for (int a = 0; a < _inst.getNumAdvetisers(); a++) {
    	  for (int s=0; s<_inst.getNumSlots(); s++) {
    		  if (isInBounds(I_a_s, a, s)) {
    			  impsPerAgent[a] += Math.round(I_a_s[a][s]);
    		  }
         }
      }
      return impsPerAgent;
   }

   
   protected int[] getImpsPerSlot(double[][] I_a_s) {
      int[] impsPerSlot = new int[_inst.getNumSlots()];
      for (int s = 0; s < _inst.getNumSlots(); s++) {
         for (int a = 0; a < _inst.getNumAdvetisers(); a++) {
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
   protected static double[] order(double[] arr, int[] order) {
      double[] orderedArr = new double[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   protected static int[] order(int[] arr, int[] order) {
      int[] orderedArr = new int[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   protected static int[] unorder(int[] orderedArr, int[] order) {
      int[] arr = new int[orderedArr.length];
      for (int i = 0; i < order.length; i++) {
         arr[order[i]] = orderedArr[i];
      }
      return arr;
   }

   protected static boolean[] order(boolean[] arr, int[] order) {
      boolean[] orderedArr = new boolean[arr.length];
      for (int i = 0; i < order.length; i++) {
         orderedArr[i] = arr[order[i]];
      }
      return orderedArr;
   }

   protected static double[] unorder(double[] orderedArr, int[] order) {
      double[] arr = new double[orderedArr.length];
      for (int i = 0; i < order.length; i++) {
         arr[order[i]] = orderedArr[i];
      }
      return arr;
   }

}
