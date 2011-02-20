package models.queryanalyzer.iep;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstance;

public class EricImpressionEstimator implements AbstractImpressionEstimator {

	private int _advertisers;
	private int _slots;
	private double[] _trueAvgPos;
	private int _ourIndex;
	private int _ourImpressions;
	private int _imprUB;
	private int[] _agentImprUB;
	private int[] _agentImprLB;

	public EricImpressionEstimator(QAInstance inst) {
		_advertisers = inst.getNumAdvetisers();
		_slots = inst.getNumSlots();
		_trueAvgPos = inst.getAvgPos();
		_ourIndex = inst.getAgentIndex(); //TODO is this ID or Index?
		_ourImpressions = inst.getImpressions();
		_imprUB = inst.getImpressionsUB();

	}
	
	public IEResult search(int[] order) {
		
		//Impressions seen by each agent
		double[] I_a = new double[_advertisers];
		Arrays.fill(I_a, -1);
		I_a[_ourIndex] = _ourImpressions;
		
		//Average position for each agent
		double[] mu_a = _trueAvgPos;
		
		//Reorder values according to the specified order
		double[] orderedI_a = order(I_a, order);
		double[] orderedMu_a = order(mu_a, order);		
		int numSlots = _slots;
		
		//Get mu_a values, given impressions
		WaterfallILP ilp = new WaterfallILP(orderedI_a, orderedMu_a, numSlots);
		double[][] I_a_s = ilp.solve();

		//Convert this into the IEResult that Carleton's QueryAnalyzer likes.
		int obj = 0;
		int[] impsPerAgent = getImpsPerAgent(I_a_s);
		int[] impsPerSlot = getImpsPerSlot(I_a_s);
		
		//TODO: do we have to undo the ordering?
		return new IEResult(obj, impsPerAgent, order, impsPerSlot);
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
	private double[] order(double[] arr , int[] order) {
		double[] orderedArr = new double[arr.length];
		for(int i=0; i < order.length; i++){
			orderedArr[i] = arr[order[i]];
		}
		return orderedArr;
	}

	
	
	
	/**
	 * Main method for testing.
	 */
	public static void main(String[] args) {
		int numSlots = 3;
		int numAgents = 3;
		double[] avgPos = {1.0, 1.5, 2.5};
//		double[] avgPos = {2.5, 1.0, 1.5};
		int[] agentIds = {-1, -2, -3};
		int ourAgentIdx = 1;
//		int ourAgentIdx = 2;
		int ourImpressions = 20;
		int impressionsUB = 100;
		QAInstance carletonInst = new QAInstance(numSlots, numAgents, avgPos, agentIds, ourAgentIdx, ourImpressions, impressionsUB, true);
		QAInstance ericInst = new QAInstance(numSlots, numAgents, avgPos, agentIds, ourAgentIdx, ourImpressions, impressionsUB, false);

		ImpressionEstimator carletonImpressionEstimator = new ImpressionEstimator(carletonInst);
		EricImpressionEstimator ericImpressionEstimator = new EricImpressionEstimator(ericInst);
		int[] order = {0, 1, 2};
//		int[] order = {2, 0, 1};
		IEResult carletonResult = carletonImpressionEstimator.search(order);
		IEResult ericResult = ericImpressionEstimator.search(order);
		
		System.out.println("Carleton " + carletonResult);
		System.out.println("\n\nEric " + ericResult);		
	}
	
	
	
}
