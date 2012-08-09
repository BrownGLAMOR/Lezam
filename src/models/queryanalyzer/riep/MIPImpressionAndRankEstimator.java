package models.queryanalyzer.riep;

import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.riep.iep.EricImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

public class MIPImpressionAndRankEstimator implements ImpressionAndRankEstimator {

   QAInstance instance;
   EricImpressionEstimator estimator;

   public MIPImpressionAndRankEstimator(QAInstance instance, boolean useRankingConstraints, boolean integerProgram, boolean multipleSolutions, double timeout) {
      this.instance = instance;
      this.estimator = new EricImpressionEstimator(instance, useRankingConstraints, integerProgram, multipleSolutions,timeout);
   }

   public IEResult getBestSolution() {

      int[] defaultOrder = new int[instance.getNumAdvetisers()];
      for (int a = 0; a < defaultOrder.length; a++) {
         defaultOrder[a] = a;
      }

      return estimator.search(defaultOrder);
   }

}
