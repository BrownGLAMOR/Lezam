package models.queryanalyzer.riep.iep.cplp;

import java.util.Arrays;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

public abstract class AbstractDropoutImpressionEstimator implements AbstractImpressionEstimator {
	
   protected AbstractQAInstance _inst;
   
   //Gloabl Variables for DFS Search.
   protected double _bestObj;
   protected LPSolution _bestSol;
   protected int _checked;
   protected int _bestChecked;

   public AbstractDropoutImpressionEstimator(AbstractQAInstance inst) {
      _inst = inst;
   }

   public ObjectiveGoal getObjectiveGoal() {return ObjectiveGoal.MINIMIZE;}
   
   public abstract String getName();

   public AbstractQAInstance getInstance() {return _inst;}

   public abstract IEResult search(int[] order);
   
   protected IEResult search(int[] order, AbstractImpressionEstimationLP IELP, int[] minDropOut, int[] maxDropOut, int us){
	  assert(order.length == minDropOut.length && order.length == maxDropOut.length);
	  
	  _bestObj = -1; //FIXME: If we change objective functions, we'll have to make sure objectives can't be negative (since -1 could otherwise naturally arise)
      _bestSol = null; //this is kept in sync with _bestObj so we can recovery the best solution
      
      _checked = 0; //just for stats collecting
      _bestChecked = -1; //just for stats collecting
      
      int[] dropout = new int[order.length];
      Arrays.fill(dropout, -1);
      
      long t0 = System.currentTimeMillis();
      
      dropoutDFS(IELP, dropout, minDropOut, maxDropOut, us, 0);      
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
          
          int[][] waterfall = _bestSol.getImpressions(); //kinda strange we distill the waterfall before re-ordering, but I am just replicating current semantics [cjc]
          LPSolution orginalOrder = _bestSol.unorder(order);
                   
          int[] impsPerAgent = makeFilterArray(orginalOrder.T_a, _inst.getNumAdvetisers());
          int[] impsPerSlot = makeFilterArray(orginalOrder.S_a, _inst.getNumSlots());

          return new IEResult(orginalOrder.objectiveVal, impsPerAgent, order.clone(), impsPerSlot, waterfall, _inst.getAgentNames());
      }
   }
  
   private int[] makeFilterArray(double[] arr, int ub){
	   int length = Math.min(arr.length, ub);
	   int[] arri = new int[length];
	   for(int i=0; i<length; i++){
		   arri[i] = (int)Math.round(arr[i]);
	   }
	   return arri;
   }
   
   //this method will implictly assume length of dropout,minDropOut,maxDropOut,avgPos_a are the same and this is the number of agents.
   protected void dropoutDFS(AbstractImpressionEstimationLP IELP, int[] dropout, int[] minDropOut, int[] maxDropOut, int us, int agent){
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
		   if(agent >= us && agent > 0){
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
			   dropoutDFS(IELP, dropout, minDropOut, maxDropOut, us, agent+1);
		   }
		   
	   }
	   
   }
   
}
