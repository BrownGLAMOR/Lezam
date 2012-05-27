package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.search.LDSearchIESmart;

public class LDSImpressionAndRankEstimator implements ImpressionAndRankEstimator {

   public final static int NUM_SLOTS = 5;
   public int NUM_ITERATIONS_2 = 20;
   private QAInstance inst;
   private AbstractImpressionEstimator ie;

   public LDSImpressionAndRankEstimator(AbstractImpressionEstimator ie) {
      this.ie = ie;
      this.inst = ie.getInstance();
   }

   public IEResult getBestSolution() {
      double[] avgPos = ie.getApproximateAveragePositions();
      int[] avgPosOrder;
      if(inst.isPadding()) {
         avgPosOrder = QAInstance.getAvgPosOrder(avgPos);
      }
      else {
         avgPosOrder = QAInstance.getCarletonOrder(avgPos, NUM_SLOTS);
      }
      int numActualAgents = inst.getNumAdvetisers(); //regardless of any padding

      IEResult bestSol;
      if(inst.getImpressions() > 0) {
         if(avgPosOrder.length > 0) {
            LDSearchIESmart smartIESearcher = new LDSearchIESmart(NUM_ITERATIONS_2, ie);
            smartIESearcher.search(avgPosOrder, avgPos);
            bestSol = smartIESearcher.getBestSolution();
            
            if(bestSol == null || bestSol.getSol() == null) {
               int[] imps = new int[numActualAgents];
               int[] slotimps = new int[NUM_SLOTS];
               int[][] waterfall = new int[numActualAgents][NUM_SLOTS];
               String[] agentNames = new String[numActualAgents];
               bestSol = new IEResult(0, imps, avgPosOrder, slotimps,waterfall, agentNames);
            }
         }
         else {
            int[] imps = new int[numActualAgents];
            int[] slotimps = new int[NUM_SLOTS];
            int[][] waterfall = new int[numActualAgents][NUM_SLOTS];
            String[] agentNames = new String[numActualAgents];
            bestSol = new IEResult(0, imps, avgPosOrder, slotimps,waterfall, agentNames);
         }
      }
      else {
         int[] imps = new int[numActualAgents];
         int[] slotimps = new int[NUM_SLOTS];
         int[][] waterfall = new int[numActualAgents][NUM_SLOTS];
         String[] agentNames = new String[numActualAgents];
         bestSol = new IEResult(0, imps, avgPosOrder, slotimps,waterfall, agentNames);
      }


      return bestSol;
   }

}
