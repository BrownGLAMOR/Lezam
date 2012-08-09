package models.queryanalyzer.riep.search;

import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

public class LDSearchIESmart extends LDSearchSmart {
	int _maxIter;
	
	AbstractImpressionEstimator _ie;
	
	public LDSearchIESmart(int maxIter, AbstractImpressionEstimator ie){
		super(ie.getInstance().getNumSlots());
		_maxIter = maxIter;
		_ie = ie;
	}
	
	@Override
	protected boolean evalPerm(int[] perm) {
		
		//System.out.println("Evaluating order: "+Arrays.toString(perm));
		IEResult best = _ie.search(perm);
		//System.out.println("Result: " + best);
		
		if (_ie.getObjectiveGoal() == AbstractImpressionEstimator.ObjectiveGoal.MINIMIZE) {
			if(best != null && (_best == null || best.getObj() < _best.getObj())){
				_best = best;
			}
		}
		if (_ie.getObjectiveGoal() == AbstractImpressionEstimator.ObjectiveGoal.MAXIMIZE) {
			if(best != null && (_best == null || best.getObj() > _best.getObj())){
				_best = best;
			}
		}
		
		if(this.getIterations() == _maxIter)
			return true;
		else 
			return false;
	}

}
