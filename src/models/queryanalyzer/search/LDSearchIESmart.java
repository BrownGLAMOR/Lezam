package models.queryanalyzer.search;

import java.util.Arrays;

import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimator;

public class LDSearchIESmart extends LDSearchSmart {
	int _maxIter;
	IEResult _best;
	ImpressionEstimator _ie;
	
	public LDSearchIESmart(int maxIter, QAInstance inst){
		_maxIter = maxIter;
		_ie = new ImpressionEstimator(inst);
	}
	
	@Override
	protected boolean evalPerm(int[] perm) {
		
//		System.out.println("Trying order: "+Arrays.toString(perm));
		IEResult best = _ie.search(perm);
//		System.out.println();
		
		if(best != null && (_best == null || best.getObj() < _best.getObj())){
			_best = best;
		}
		
		if(this.getIterations() == _maxIter)
			return true;
		else 
			return false;
	}

	public IEResult getBestSolution() {
		assert(_best != null) : "possibly called before search was run";
		return _best;
	}

}