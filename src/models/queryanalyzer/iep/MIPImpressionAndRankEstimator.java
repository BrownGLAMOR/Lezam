package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

public class MIPImpressionAndRankEstimator implements ImpressionAndRankEstimator {

	public IEResult getBestSolution(QAInstance instance) {
		
		EricImpressionEstimator estimator = new EricImpressionEstimator(instance);

		int[] defaultOrder = new int[instance.getNumAdvetisers()];
		for (int a=0; a<defaultOrder.length; a++) {
			defaultOrder[a] = a;
		}
		
		return estimator.search(defaultOrder);		
	}

}
