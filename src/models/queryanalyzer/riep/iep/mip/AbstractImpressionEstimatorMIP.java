package models.queryanalyzer.riep.iep.mip;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;

public abstract class AbstractImpressionEstimatorMIP implements AbstractImpressionEstimator {
	   protected AbstractQAInstance _inst;
	   
	   public AbstractImpressionEstimatorMIP(AbstractQAInstance inst){
		   _inst = inst;
	   }
	   
		@Override
		public AbstractQAInstance getInstance() {return _inst;}
}
