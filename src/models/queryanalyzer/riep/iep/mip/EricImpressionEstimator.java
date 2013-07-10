 package models.queryanalyzer.riep.iep.mip;

 import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;
import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorAll;
import models.queryanalyzer.riep.iep.mip.WaterfallILP.Objective;
import models.queryanalyzer.riep.iep.mip.WaterfallILP.WaterfallResult;

import ilog.cplex.IloCplex;

import java.util.Arrays;

public class EricImpressionEstimator extends AbstractImpressionEstimatorMIP {

   private ObjectiveGoal _objectiveGoal = ObjectiveGoal.MINIMIZE; //maximize or minimize?
   private QAInstanceAll _instance;
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
   private String[] agentNames;

   boolean INTEGER_PROGRAM;
   boolean SAMPLED = false;
   boolean USE_EPSILON = false;
   int NUM_SAMPLES = 10;
   boolean USE_RANKING_CONSTRAINTS;
   boolean MULTIPLE_SOLUTIONS; //Have the MIP return multiple solutions and evaluate with a better objective?
   double TIMEOUT_IN_SECONDS;
   
   IloCplex cplex;
   Objective DESIRED_OBJECTIVE;


   public EricImpressionEstimator(QAInstanceAll inst, boolean useRankingConstraints, boolean integerProgram, 
		   boolean multipleSolutions, double timeoutInSeconds) {
	  super(inst);
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
      agentNames = inst.getAgentNames();
      
   }

   //Betsy addition, to use same cplex
   public EricImpressionEstimator(QAInstanceAll inst, boolean useRankingConstraints, boolean integerProgram, 
		   boolean multipleSolutions, double timeoutInSeconds, IloCplex cplex, boolean sampled, Objective desiredObjective) {
	  super(inst);
	  SAMPLED = sampled;
	  this.cplex = cplex;
	  this.DESIRED_OBJECTIVE = desiredObjective;
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
      
//      //BETSY ADD
      // System.out.println("Imp Upperbound "+_imprUB);
      _agentImpressionDistributionMean = inst.getAgentImpressionDistributionMean();
      _agentImpressionDistributionStdev = inst.getAgentImpressionDistributionStdev();
      agentNames = inst.getAgentNames();
      
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

   public QAInstanceAll getInstance() {
      return _instance;
   }

   public IEResult search(int[] order) {
	  // System.out.println("_____________________________________SEARCHING__________________________________");
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
      //System.out.println(Arrays.toString(orderedI_a));
      double[] orderedMu_a = order(mu_a, order);
      //System.out.println(Arrays.toString(orderedMu_a));
      double[] orderedSampledMu_a = order(sampledMu_a, order);
      //System.out.println(Arrays.toString(orderedSampledMu_a));
      double[] orderedI_aPromoted = order(I_aPromoted, order);
     // System.out.println(Arrays.toString(orderedI_aPromoted));
      boolean[] orderedPromotionEligibilityVerified = order(promotionEligiblityVerified, order);
     // System.out.println(Arrays.toString(orderedPromotionEligibilityVerified));
      int[] orderedHitBudget = order(hitBudget, order);
     //System.out.println(Arrays.toString(orderedHitBudget));
      double[] orderedI_aDistributionMean = order(_agentImpressionDistributionMean, order);
      System.out.println(Arrays.toString(orderedI_aDistributionMean));
      double[] orderedI_aDistributionStdev = order(_agentImpressionDistributionStdev, order);
    
      
      
      ///////////////////////////////////////
//      double[] knownI_a = {-1.0, -1.0, -1.0, 252.0, -1.0, -1.0};
//		double[] knownMu_a = {-1.0, -1.0, -1.0, 3.2738095238095237, -1.0, -1.0};
//		double[] knownI_aPromoted= {-1.0, -1.0, -1.0, 0.0, -1.0, -1.0};
//		boolean[] isKnownPromotionEligible = {false, false, false, false, false, false};
//		int[] hitBudget = {-1, -1, -1, 0, -1, -1};
//		int numSlots = 5;
//		int numPromotedSlots = 0;
//		boolean integerProgram = false;
//		boolean useEpsilon = true;
//		double[] knownSampledMu_a = {Double.NaN, 1.2, 2.2, 3.2, 4.2, 5.0};
//		//double[] knownSampledMu_a = {1.0, 1.2, 2.2, 3.2, 4.2, 5.0};
//		int numSamples = 10;
//		int maxImpsPerAgent = 302;
     // double[] orderedI_aDistributionMean  = new double[_advertisers];
      //Arrays.fill(orderedI_aDistributionMean, -1);
		//double[] orderedI_aDistributionStdev = new double[_advertisers];
		//Arrays.fill(orderedI_aDistributionStdev, -1);
//		boolean useRankingConstraints = false;
//		boolean multipleSolutions = false;
//		double timeoutInSeconds = 3;
      
      
      
//		WaterfallILP ilp = new WaterfallILP(
//				knownI_a, knownMu_a, knownI_aPromoted, isKnownPromotionEligible, hitBudget, numSlots, 
//				numPromotedSlots, integerProgram, useEpsilon, knownSampledMu_a, 
//				numSamples, maxImpsPerAgent, knownI_aDistributionMean, knownI_aDistributionStdev,
//				useRankingConstraints, multipleSolutions, timeoutInSeconds, cplex);
      
      
      ////////////////////////////////////////////
//      WaterfallILP ilp = new WaterfallILP(orderedI_a, orderedMu_a, orderedI_aPromoted, orderedPromotionEligibilityVerified, orderedHitBudget,
//                                          _slots, _promotedSlots, INTEGER_PROGRAM, USE_EPSILON, orderedSampledMu_a, NUM_SAMPLES, _imprUB,
//                                          orderedI_aDistributionMean, orderedI_aDistributionStdev, USE_RANKING_CONSTRAINTS, MULTIPLE_SOLUTIONS, TIMEOUT_IN_SECONDS, cplex);
      WaterfallILP ilp;
      if(SAMPLED){
    	 // System.out.println("UB 190 EIE: "+_imprUB);
    	  ilp = new WaterfallILP(orderedI_a, orderedMu_a, orderedI_aPromoted, orderedPromotionEligibilityVerified, orderedHitBudget,
              _slots, _promotedSlots, INTEGER_PROGRAM, USE_EPSILON, orderedSampledMu_a, NUM_SAMPLES, _imprUB,
              orderedI_aDistributionMean, orderedI_aDistributionStdev, USE_RANKING_CONSTRAINTS, MULTIPLE_SOLUTIONS, TIMEOUT_IN_SECONDS, 
              cplex, DESIRED_OBJECTIVE);
      }else{
    	  //System.out.println("UB 201 EIE: "+_imprUB);
    	  ilp = new  WaterfallILP(orderedI_a, orderedMu_a, orderedI_aPromoted, 
    			  orderedPromotionEligibilityVerified, orderedHitBudget, _slots, _promotedSlots, 
  			INTEGER_PROGRAM, USE_EPSILON, _imprUB,
  			orderedI_aDistributionMean, orderedI_aDistributionStdev,
  			USE_RANKING_CONSTRAINTS, MULTIPLE_SOLUTIONS, TIMEOUT_IN_SECONDS, cplex, DESIRED_OBJECTIVE);
      }

      WaterfallILP.WaterfallResult result = ilp.solve();
      
      if(result!=null){
      double[][] I_a_s = result.getI_a_s();
		double[] U_k = result.getU_k();
		int[][] V_i_k = result.getV_i_k();
		int[] ordering = result.getOrdering();
//		System.out.println("I_a_s = " + WaterfallILP.arrayString(I_a_s));
//		System.out.println("U_k = " + Arrays.toString(U_k));
//		System.out.println("V_i_k = " + WaterfallILP.arrayString(V_i_k));
//		System.out.println("ordering = " + Arrays.toString(ordering));
//		System.out.println("objective = " + result.getObjectiveVal());
//		System.out.println("Done");
      
      //System.out.println("_RESULT: "+result);
      //double[][] I_a_s = result.getI_a_s();

      int[][] waterfall = new int[I_a_s.length][I_a_s[0].length];
      for(int i = 0; i < waterfall.length; i++) {
         for(int j = 0; j < waterfall[i].length; j++) {
        	 //System.out.println("Ias "+I_a_s[i][j]);
            waterfall[i][j] = (int) Math.round(I_a_s[i][j]);
         }
      }
      int[][] unorderedWaterfall = unorder(waterfall, order);

      //relativeRanking[i]: the agent in initial position i had index relativeRanking[i]
      int[] relativeRanking = result.getOrdering();

      //Convert this into the IEResult that Carleton's QueryAnalyzer likes.
      double obj = result.getTrueObjectiveVal();
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
      //IEResult ieres = new IEResult(obj, unorderedImpsPerAgent, unorderedRelativeRanking, impsPerSlot, waterfall, agentNames);
      IEResult ieres = new IEResult(obj, unorderedImpsPerAgent, unorderedRelativeRanking, impsPerSlot, unorderedWaterfall, agentNames);
      //System.out.println("RESULES 2: "+ieres);
      return ieres;
      }else{
    	  System.out.println("______________________________________________NULL RESULTS________________________________________");
    	  return null;
      }
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
   
   // Unorder array by first element
   private static int[][] unorder(int[][] orderedArr, int[] order) {
	      int[][] arr = new int[orderedArr.length][];
	      for (int i = 0; i < order.length; i++) {
	         arr[order[i]] = orderedArr[i].clone();
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

      //EricImpressionEstimator.testOrdering();

      //err=[2800.0, 2702.0, 0.0]	pred=[398, 579, 202]	actual=[3198, 3281, 202]	g=1 d=8 a=2 q=(Query (null,null)) avgPos=[1.0, 2.0362694300518136, 2.0] bids=[0.3150841472838487, 0.126159214933152, 0.13126460037679655] imps=[3198, 3281, 202] order=[0, 2, 1] IP

      for (int ourAgentIdx = 0; ourAgentIdx <= 0; ourAgentIdx++) {


         //These aren't actually used; everything is -1 except the current agentIdx
         double[] I_aFull = {400, 200};
         double[] mu_aFull = {1.5, 1.0}; //{1.0, 1.5}; //{1.5, 2.5};


         //Get priors on impressions
         double[] agentImpressionDistributionMean = {-1, -1};
         double[] agentImpressionDistributionStdev = {-1, -1};


         //Get observed exact average positions (we only see one)
         double[] mu_a = new double[mu_aFull.length];
         Arrays.fill(mu_a, -1);
         mu_a[ourAgentIdx] = mu_aFull[ourAgentIdx];

         double[] I_aPromoted = {-1, -1};
         boolean[] isKnownPromotionEligible = {false, false};
         double[] knownSampledMu_a = {1.5, 1.0}; //{1.5, 2.5};
//         double[] knownSampledMu_a = {1, 1.5, 2.5};
         int numSlots = 5;
         int numPromotedSlots = 0;

         int ourImpressions = (int) I_aFull[ourAgentIdx];
         int ourPromotedImpressions = (int) I_aPromoted[ourAgentIdx];
         boolean ourPromotionKnownAllowed = isKnownPromotionEligible[ourAgentIdx];
         int impressionsUB = 500;
         int numAgents = mu_a.length;

         //Did we hit our budget? (added constraint if we didn't)
         boolean hitOurBudget = true;

         int[] predictedOrder = {-1, -1};

         //Give arbitrary agent IDs and names
         int[] agentIds = new int[numAgents];
         String[] agentNames = new String[numAgents];
         for (int i = 0; i < agentIds.length; i++) {
            agentIds[i] = -(i + 1);
            agentNames[i] = "A" + i;
         }


         //int slots, int promotedSlots, int advetisers, double[] avgPos, int[] agentIds, int agentIndex, int impressions, int promotedImpressions, int impressionsUB, boolean considerPaddingAgents, boolean promotionEligibiltyVerified
         QAInstanceAll carletonInst = new QAInstanceAll(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, true, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder, agentNames, 0);
         QAInstanceAll ericInst = new QAInstanceAll(numSlots, numPromotedSlots, numAgents, mu_a, knownSampledMu_a, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed, hitOurBudget, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, predictedOrder, agentNames, 0);

//			System.out.println("Carleton Instance:\n" + carletonInst);
//			System.out.println("Eric Instance:\n" + ericInst);

         ImpressionEstimatorExact carletonImpressionEstimator = new ImpressionEstimatorExact(carletonInst);
         EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst, false, true, false, 3);
         DropoutImpressionEstimatorAll carletonLP = new DropoutImpressionEstimatorAll(ericInst, false, true, false, 5);

         double[] cPos = carletonInst.getAvgPos();
         int[] cOrder = QAInstanceAll.getAvgPosOrder(cPos);
         //int[] cOrder = {1, 2, 0};
         System.out.println("cPos: " + Arrays.toString(cPos) + ", cOrder: " + Arrays.toString(cOrder));
         IEResult carletonResult = carletonImpressionEstimator.search(cOrder);
         IEResult ericResult = ericImpressionEstimator.search(cOrder);
         IEResult carletonLPResult = carletonLP.search(cOrder);

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
