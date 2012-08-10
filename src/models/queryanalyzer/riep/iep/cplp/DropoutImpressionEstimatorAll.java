package models.queryanalyzer.riep.iep.cplp;

import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;

import java.util.Arrays;


/**
 * TODO: Add a timeout that limits the amount of loops over dropout points
 * TODO: Add padded agents to IE problem (with sampled average position)
 * TODO: Get a quick solution with CP, then solve with CarletonLP or MIP with the remaining time.
 * TODO: Get a quick solution with CP, and actually use that to guide the CarletonLP search (or at least use as an initial bound on best solution for branch and bound)
 * @author sodomka
 *
 */

public class DropoutImpressionEstimatorAll extends AbstractDropoutImpressionEstimator {
	static int boundCount = 0;
	static int avgPosBoundCount = 0;
	   
   protected QAInstanceAll _instanceAll;
   
   protected boolean INTEGER_PROGRAM;
   protected boolean USE_EPSILON = true;
   protected int NUM_SAMPLES = 10;
   protected boolean USE_RANKING_CONSTRAINTS;
   protected boolean MULTIPLE_SOLUTIONS; //Have the MIP return multiple solutions and evaluate with a better objective?
   protected double TIMEOUT_IN_SECONDS;

   protected boolean BRANCH_AND_BOUND = true;
   protected boolean ALWAYS_SOLVE_PARTIAL_PROBLEM = false;
   protected boolean BOUND_DROPOUT_SEARCH_BY_AVERAGE_POSITION = true;
   protected boolean SET_WATERFALL_MAX_DROPOUT = true;
   

   public DropoutImpressionEstimatorAll(QAInstanceAll inst, boolean useRankingConstraints, boolean integerProgram, boolean multipleSolutions, double timeoutInSeconds) {
      super(inst);
      _instanceAll = inst;
	  TIMEOUT_IN_SECONDS = timeoutInSeconds;
      MULTIPLE_SOLUTIONS = multipleSolutions;
      INTEGER_PROGRAM = integerProgram;
      USE_RANKING_CONSTRAINTS = useRankingConstraints;
   }

   public String getName() { return "IELP_All"; }

   public QAInstanceAll getInstanceAll() {return _instanceAll;}

   public IEResult search(int[] order) {
	  QAInstanceAll orderedInst = _instanceAll.reorder(order); 
	  int agentIndex = orderedInst.getAgentIndex();
	  
	  int advertisers = orderedInst.getNumAdvetisers();
	  int slots = orderedInst.getNumSlots();
	  
	  
      double[] I_a = new double[advertisers];
      double[] I_aPromoted = new double[advertisers];
      boolean[] promotionEligiblityVerified = new boolean[advertisers];
      Arrays.fill(I_a, -1);
      Arrays.fill(I_aPromoted, -1);
	  
      I_a[agentIndex] = orderedInst.getImpressions();
      I_aPromoted[agentIndex] = orderedInst.getPromotedImpressions();
      promotionEligiblityVerified[agentIndex] = orderedInst.getPromotionEligibilityVerified();

      
      int[] minDropOut = new int[advertisers];
      int[] maxDropOut = new int[advertisers];
      
      //set default values
      for(int a=0; a < advertisers; a++){
    	  int ceilingSlot = ((int)Math.ceil(orderedInst.getAvgPos()[a]))-1;
    	  int floorSlot = ((int)Math.floor(orderedInst.getAvgPos()[a]))-1;
    	  if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == slots-1)){
    		  minDropOut[a] = ceilingSlot;
    	  } else {
    		  minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents?  
    	  }
    	  maxDropOut[a] = Math.min(Math.min(a,slots-1),floorSlot);
      }
      
      System.out.println(Arrays.toString(orderedInst.getAvgPos()));
      System.out.println(Arrays.toString(minDropOut));
      System.out.println(Arrays.toString(maxDropOut));
      
      
      ImpressionEstimationLPAll IELP = new ImpressionEstimationLPAll(orderedInst.getImpressionsUB(), agentIndex, orderedInst.getImpressions(), I_a, orderedInst.getAvgPos(), orderedInst.getSampledAvgPos(), slots, orderedInst.getAgentImpressionDistributionMean(), orderedInst.getAgentImpressionDistributionStdev());

      return search(order, IELP, minDropOut, maxDropOut, agentIndex);
   }

   
   /**
    * Main method for testing.
    */
   public static void main(String[] args) {

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

         //Give arbitrary agent IDs and names
         int[] agentIds = new int[numAgents];
         String[] agentNames = new String[numAgents];
         for (int i = 0; i < agentIds.length; i++) {
            agentIds[i] = -(i + 1);
            agentNames[i] = "A" + i;
         }

         QAInstanceAll carletonInst = new QAInstanceAll(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, true, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder, agentNames);
         QAInstanceAll ericInst = new QAInstanceAll(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder, agentNames);
         ImpressionEstimatorExact carletonImpressionEstimator = new ImpressionEstimatorExact(carletonInst);
         EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst, false, true, false, 10);

         DropoutImpressionEstimatorAll carletonLP = new DropoutImpressionEstimatorAll(ericInst, false, true, false, 5);

         double[] cPos = carletonInst.getAvgPos();
         //int[] cOrder = QAInstance.getAvgPosOrder(cPos);
//         int[] cOrder = {0, 1, 2, 3, 4, 5, 6, 7};
         int[] cOrder = {1, 2, 3, 4, 5, 6, 7, 0};
         System.out.println("cPos: " + Arrays.toString(cPos) + ", cOrder: " + Arrays.toString(cOrder));
         IEResult carletonResult = carletonImpressionEstimator.search(cOrder);
         System.out.println("cOrder: " + Arrays.toString(cOrder));
         IEResult ericResult = ericImpressionEstimator.search(cOrder);
         System.out.println("cOrder: " + Arrays.toString(cOrder));

         long start = System.currentTimeMillis();
         IEResult carletonLPResult = carletonLP.search(cOrder);
         long end = System.currentTimeMillis();
         double seconds = (end - start)/1000.0;
         
         System.out.println("cOrder: " + Arrays.toString(cOrder));

         System.out.println("ourAgentIdx=" + ourAgentIdx);
         System.out.println("  Carleton: " + carletonResult + "\tactual=" + Arrays.toString(I_aFull));
         System.out.println("  IP: " + ericResult + "\tactual=" + Arrays.toString(I_aFull));
         System.out.println("  CarletonLP: " + carletonLPResult + "\tactual=" + Arrays.toString(I_aFull));
         
         System.out.println("boundcount=" + DropoutImpressionEstimatorAll.boundCount);
         System.out.println("avgPosBoundcount=" + DropoutImpressionEstimatorAll.avgPosBoundCount);
         System.out.println("carletonLPSeconds=" + seconds);
      }
      
      
      
      
   }


}
