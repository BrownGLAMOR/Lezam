package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

public interface ImpressionAndRankEstimator {
	
	public IEResult getBestSolution(QAInstance instance);
}
