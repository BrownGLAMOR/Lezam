package models.queryanalyzer.iep;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstance;

public class EricImpressionEstimator implements AbstractImpressionEstimator {

	private int _advertisers;
	private int _slots;
	private int _promotedSlots;
	private double[] _trueAvgPos;
	private int _ourIndex;
	private int _ourImpressions;
	private int _ourPromotedImpressions;
	private boolean _ourPromotedEligibilityVerified;
	private int _imprUB;
	private int[] _agentImprUB;
	private int[] _agentImprLB;
	boolean INTEGER_PROGRAM = false;
	boolean USE_EPSILON = true;
	
	public EricImpressionEstimator(QAInstance inst) {
		_advertisers = inst.getNumAdvetisers();
		_slots = inst.getNumSlots();
		_promotedSlots = inst.getNumPromotedSlots();
		_trueAvgPos = inst.getAvgPos();
		_ourIndex = inst.getAgentIndex(); //TODO is this ID or Index?
		_ourImpressions = inst.getImpressions();
		_ourPromotedImpressions = inst.getPromotedImpressions();
		_ourPromotedEligibilityVerified = inst.getPromotionEligibilityVerified();
		_imprUB = inst.getImpressionsUB();

	}

	
	public String getName() {
		if (INTEGER_PROGRAM) return "IP";
		else return "LP";
	}
	
	public IEResult search(int[] order) {
		System.out.println("DEBUG OUR PROMOTED IMPS: " + _ourPromotedImpressions);
		//Impressions seen by each agent
		double[] I_a = new double[_advertisers];
		double[] I_aPromoted = new double[_advertisers];
		boolean[] promotionEligiblityVerified = new boolean[_advertisers];
		Arrays.fill(I_a, -1);
		Arrays.fill(I_aPromoted, -1);
		I_a[_ourIndex] = _ourImpressions;
		I_aPromoted[_ourIndex] = _ourPromotedImpressions;
		promotionEligiblityVerified[_ourIndex] = _ourPromotedEligibilityVerified;
		
		//Average position for each agent
		double[] mu_a = _trueAvgPos;

		//Reorder values according to the specified order
		double[] orderedI_a = order(I_a, order);
		double[] orderedMu_a = order(mu_a, order);
		double[] orderedI_aPromoted = order(I_aPromoted, order);
		boolean[] orderedPromotionEligibilityVerified = order(promotionEligiblityVerified, order);

		//Get mu_a values, given impressions
		//Waterfall params: I_a, mu_a, I_aPromoted, isKnownPromotionEligible, numSlots, numPromotedSlots, integerProgram, useEpsilon
		WaterfallILP ilp = new WaterfallILP(orderedI_a, orderedMu_a, orderedI_aPromoted, orderedPromotionEligibilityVerified, _slots, _promotedSlots, INTEGER_PROGRAM, USE_EPSILON);
		double[][] I_a_s = ilp.solve();

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
		for (int a=0; a<_advertisers; a++) {
			for (int s=0; s<_slots; s++) {
				impsPerAgent[a] += Math.round( I_a_s[a][s] );
			}
		}
		return impsPerAgent;
	}

	public int[] getImpsPerSlot(double[][] I_a_s) {
		int[] impsPerSlot = new int[_slots];
		for (int s=0; s<_slots; s++) {
			for (int a=0; a<_advertisers; a++) {
				impsPerSlot[s] += Math.round( I_a_s[a][s] );
			}
		}
		return impsPerSlot;
	}


	/**
	 * Reorder the specified array. order's ith value returns
	 * the index of the original array that should be moved to 
	 * the ith position in the new array.
	 * @param arr
	 * @param order
	 * @return
	 */
	private static double[] order(double[] arr , int[] order) {
		double[] orderedArr = new double[arr.length];
		for(int i=0; i < order.length; i++){
			orderedArr[i] = arr[order[i]];
		}
		return orderedArr;
	}

	private static int[] unorder(int[] orderedArr, int[] order) {
		int[] arr = new int[orderedArr.length];
		for(int i=0; i < order.length; i++){
			arr[order[i]] = orderedArr[i];
		}
		return arr;		
	}
	
	private static boolean[] order(boolean[] arr , int[] order) {
		boolean[] orderedArr = new boolean[arr.length];
		for(int i=0; i < order.length; i++){
			orderedArr[i] = arr[order[i]];
		}
		return orderedArr;
	}

	private static double[] unorder(double[] orderedArr, int[] order) {
		double[] arr = new double[orderedArr.length];
		for(int i=0; i < order.length; i++){
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

		for (int ourAgentIdx = 0; ourAgentIdx<3; ourAgentIdx++) {

			//Configure these
			//int[] I_a = {742, 742, 556, 589, 222, 520, 186, 153}; //Actual imps (not input to the agent)
			//double[] avgPos = {1, 2, 3, 3.94397284, 5, 4.34807692, 4.17741935, 5};

			int[] I_a = {80, 20, 100};
			int[] I_aPromoted = {80, 0, 20};
			double[] avgPos = {1.0, 2.0, 2.0};
			int[] order = {0, 1, 2};
			boolean[] promotionKnownAllowed = {false, false, false};

			int ourImpressions = I_a[ourAgentIdx];
			int ourPromotedImpressions = I_aPromoted[ourAgentIdx];
			boolean ourPromotionKnownAllowed = promotionKnownAllowed[ourAgentIdx];
			int impressionsUB = 10000;
			int numSlots = 5;
			int numPromotedSlots = 1;
			
			
			int[] agentIds = {-1, -2, -3};
			int numAgents = avgPos.length;

			//int slots, int promotedSlots, int advetisers, double[] avgPos, int[] agentIds, int agentIndex, int impressions, int promotedImpressions, int impressionsUB, boolean considerPaddingAgents, boolean promotionEligibiltyVerified
			QAInstance carletonInst = new QAInstance(numSlots, numPromotedSlots, numAgents, avgPos, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed);
			QAInstance ericInst = new QAInstance(numSlots, numPromotedSlots, numAgents, avgPos, agentIds, ourAgentIdx, ourImpressions, ourPromotedImpressions, impressionsUB, false, ourPromotionKnownAllowed);

//			System.out.println("Carleton Instance:\n" + carletonInst);
//			System.out.println("Eric Instance:\n" + ericInst);

			ImpressionEstimator carletonImpressionEstimator = new ImpressionEstimator(carletonInst);
			EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst);

			IEResult carletonResult = carletonImpressionEstimator.search(order);
			IEResult ericResult = ericImpressionEstimator.search(order);

			System.out.println("ourAgentIdx=" + ourAgentIdx);
			System.out.println("  Carleton: " + carletonResult + "\tactual=" + Arrays.toString(I_a));
			System.out.println("        IP: " + ericResult + "\tactual=" + Arrays.toString(I_a));	
		}
	}



}
