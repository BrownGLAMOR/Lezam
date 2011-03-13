package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

public class MIPImpressionAndRankEstimator implements ImpressionAndRankEstimator {

	QAInstance instance;
	EricImpressionEstimator estimator;
	
	public MIPImpressionAndRankEstimator(QAInstance instance) {
		this.instance = instance;
		this.estimator = new EricImpressionEstimator(instance);
	}
	
	public IEResult getBestSolution() {
		
		int[] defaultOrder = new int[instance.getNumAdvetisers()];
		for (int a=0; a<defaultOrder.length; a++) {
			defaultOrder[a] = a;
		}
		
		return estimator.search(defaultOrder);		
	}

}
