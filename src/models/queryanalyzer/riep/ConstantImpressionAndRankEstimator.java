package models.queryanalyzer.riep;

import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

/**
 * This chooses a constant ordering of squashed bids, and tries to find impressions
 * given this ordering.
 * @author sodomka
 *
 */
public class ConstantImpressionAndRankEstimator implements ImpressionAndRankEstimator {

	AbstractImpressionEstimator ie;
	
	//[2 3 0 1] states that the highest ranked agent is in index 2. 
	int[] ordering; 
	
	public ConstantImpressionAndRankEstimator(AbstractImpressionEstimator ie, int[] ordering) {
		this.ie = ie;
		this.ordering = ordering;
	}
	
	public IEResult getBestSolution() {
		return ie.search(ordering);
	}
	
}
