package models.queryanalyzer.riep;

import models.queryanalyzer.riep.iep.IEResult;

public interface ImpressionAndRankEstimator {
	
	public IEResult getBestSolution();
}
