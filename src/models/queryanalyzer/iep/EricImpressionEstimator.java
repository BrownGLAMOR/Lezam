package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

import java.util.Arrays;

public class EricImpressionEstimator implements AbstractImpressionEstimator {

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

   boolean INTEGER_PROGRAM = true;
   boolean USE_EPSILON = false;
   int NUM_SAMPLES = 10;
   boolean USE_RANKING_CONSTRAINTS;
   
   public EricImpressionEstimator(QAInstance inst, boolean useRankingConstraints) {
	   USE_RANKING_CONSTRAINTS = useRankingConstraints;
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


   public String getName() {
      if (INTEGER_PROGRAM) {
         return "IP";
      } else {
         return "LP";
      }
   }

   public IEResult search(int[] order) {
      //Impressions seen by each agent
      double[] I_a = new double[_advertisers];
      double[] I_aPromoted = new double[_advertisers];
      boolean[] promotionEligiblityVerified = new boolean[_advertisers];
      Arrays.fill(I_a, -1);
      Arrays.fill(I_aPromoted, -1);
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

      //Get mu_a values, given impressions
      WaterfallILP ilp = new WaterfallILP(orderedI_a, orderedMu_a, orderedI_aPromoted, orderedPromotionEligibilityVerified, orderedHitBudget,
                                          _slots, _promotedSlots, INTEGER_PROGRAM, USE_EPSILON, orderedSampledMu_a, NUM_SAMPLES, _imprUB,
                                          orderedI_aDistributionMean, orderedI_aDistributionStdev, USE_RANKING_CONSTRAINTS);

      WaterfallILP.WaterfallResult result = ilp.solve();
      double[][] I_a_s = result.getI_a_s();

      //Convert this into the IEResult that Carleton's QueryAnalyzer likes.
      int obj = 0;
      int[] impsPerAgent = getImpsPerAgent(I_a_s);
      int[] impsPerSlot = getImpsPerSlot(I_a_s);

//		System.out.println("agents=" + _advertisers + "\tslots=" + _slots + "\tI_a_s size=" + I_a_s.length + " " + I_a_s[0].length);
      //TODO: do we have to undo the ordering?
      int[] unorderedImpsPerAgent = unorder(impsPerAgent, order);

      return new IEResult(obj, unorderedImpsPerAgent, order, impsPerSlot);
   }


   public int[] getImpsPerAgent(double[][] I_a_s) {
      int[] impsPerAgent = new int[_advertisers];
      for (int a = 0; a < _advertisers; a++) {
         for (int s = 0; s < _slots; s++) {
            impsPerAgent[a] += Math.round(I_a_s[a][s]);
         }
      }
      return impsPerAgent;
   }

   public int[] getImpsPerSlot(double[][] I_a_s) {
      int[] impsPerSlot = new int[_slots];
      for (int s = 0; s < _slots; s++) {
         for (int a = 0; a < _advertisers; a++) {
            impsPerSlot[s] += Math.round(I_a_s[a][s]);
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

//		EricImpressionEstimator.testOrdering();

      //err=[2800.0, 2702.0, 0.0]	pred=[398, 579, 202]	actual=[3198, 3281, 202]	g=1 d=8 a=2 q=(Query (null,null)) avgPos=[1.0, 2.0362694300518136, 2.0] bids=[0.3150841472838487, 0.126159214933152, 0.13126460037679655] imps=[3198, 3281, 202] order=[0, 2, 1] IP

      for (int ourAgentIdx = 0; ourAgentIdx < 8; ourAgentIdx++) {


         //These aren't actually used; everything is -1 except the current agentIdx
         double[] I_aFull = {742, 742, 556, 589, 222, 520, 186, 153};
         double[] mu_aFull = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};


         //Get priors on impressions
         double[] agentImpressionDistributionMean = {742, 742, 556, 589, 222, 520, 186, 153};
         double[] agentImpressionDistributionStdev = {10, 10, 10, 10, 10, 10, 10, 10};


         //Get observed exact average positions (we only see one)
         double[] mu_a = new double[mu_aFull.length];
         Arrays.fill(mu_a, -1);
         mu_a[ourAgentIdx] = mu_aFull[ourAgentIdx];

         double[] I_aPromoted = {-1, -1, -1, -1, -1, -1, -1, -1};
         boolean[] isKnownPromotionEligible = {false, false, false, false, false, false, false, false};
         double[] knownSampledMu_a = {1.0, 2.0, 3.0, 3.75, 5, 4, 4.5, 5};
         int numSlots = 5;
         int numPromotedSlots = 0;

         int ourImpressions = (int) I_aFull[ourAgentIdx];
         int ourPromotedImpressions = (int) I_aPromoted[ourAgentIdx];
         boolean ourPromotionKnownAllowed = isKnownPromotionEligible[ourAgentIdx];
         int impressionsUB = 10000;
         int numAgents = mu_a.length;

         //Did we hit our budget? (added constraint if we didn't)
         boolean hitOurBudget = true;
			
         //By default, ordering will be from first to last position
         int[] order = new int[numAgents];
         for (int i = 0; i < order.length; i++) {
            order[i] = i;
         }

         //Give arbitrary agent IDs
         int[] agentIds = new int[numAgents];
         for (int i = 0; i < agentIds.length; i++) {
            agentIds[i] = -(i + 1);
         }


         //Carleton wants average positions to be used when available, otherwise use sampled average positions
         double[] carletonAvgPos = new double[mu_a.length];
         for (int i = 0; i < mu_a.length; i++) {
            if (mu_a[i] != -1) {
               carletonAvgPos[i] = mu_a[i];
            } else {
               carletonAvgPos[i] = knownSampledMu_a[i];
            }
         }

         //int slots, int promotedSlots, int advetisers, double[] avgPos, int[] agentIds, int agentIndex, int impressions, int promotedImpressions, int impressionsUB, boolean considerPaddingAgents, boolean promotionEligibiltyVerified
         QAInstance carletonInst = new QAInstance(numSlots, numPromotedSlots, numAgents, carletonAvgPos, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, true, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, order);
         QAInstance ericInst = new QAInstance(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, order);

//			System.out.println("Carleton Instance:\n" + carletonInst);
//			System.out.println("Eric Instance:\n" + ericInst);

         ImpressionEstimator carletonImpressionEstimator = new ImpressionEstimator(carletonInst);
         EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst, false);

         IEResult carletonResult = carletonImpressionEstimator.search(order);
         IEResult ericResult = ericImpressionEstimator.search(order);

         System.out.println("ourAgentIdx=" + ourAgentIdx);
         System.out.println("  Carleton: " + carletonResult + "\tactual=" + Arrays.toString(I_aFull));
         System.out.println("        IP: " + ericResult + "\tactual=" + Arrays.toString(I_aFull));
      }
   }


}
