package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.search.LDSearchIESmart;

public class LDSImpressionAndRankEstimator implements ImpressionAndRankEstimator {

	public final static int NUM_SLOTS = 5;
	public int NUM_ITERATIONS_2 = 10;
	private QAInstance inst;
	private AbstractImpressionEstimator ie;
	
	public LDSImpressionAndRankEstimator(AbstractImpressionEstimator ie) {
		this.ie = ie;
		this.inst = ie.getInstance();
	}
	
	public IEResult getBestSolution() {
		//int[] avgPosOrder = inst.getAvgPosOrder();
		int[] avgPosOrder = inst.getCarletonOrder();
		IEResult bestSol;
		
		if(inst.getImpressions() > 0) {
			if(avgPosOrder.length > 0) {
				LDSearchIESmart smartIESearcher = new LDSearchIESmart(NUM_ITERATIONS_2, ie);
				smartIESearcher.search(avgPosOrder, inst.getAvgPos());
				//LDSearchHybrid smartIESearcher = new LDSearchHybrid(NUM_ITERATIONS_1, NUM_ITERATIONS_2, inst);
				//smartIESearcher.search();
				bestSol = smartIESearcher.getBestSolution();
				if(bestSol == null || bestSol.getSol() == null) {
					int[] imps = new int[avgPosOrder.length];
					int[] slotimps = new int[NUM_SLOTS];
					bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
				}
			}
			else {
				int[] imps = new int[avgPosOrder.length];
				int[] slotimps = new int[NUM_SLOTS];
				bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
			}
		}
		else {
			int[] imps = new int[avgPosOrder.length];
			int[] slotimps = new int[NUM_SLOTS];
			bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
		}
		
		
		return bestSol;
	}

}
