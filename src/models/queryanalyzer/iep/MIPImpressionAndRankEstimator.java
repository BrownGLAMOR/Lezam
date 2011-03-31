package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

public class MIPImpressionAndRankEstimator implements ImpressionAndRankEstimator {

   QAInstance instance;
   EricImpressionEstimator estimator;

   public MIPImpressionAndRankEstimator(QAInstance instance, boolean useRankingConstraints, boolean integerProgram, boolean multipleSolutions) {
      this.instance = instance;
      this.estimator = new EricImpressionEstimator(instance, useRankingConstraints, integerProgram, multipleSolutions);
   }

   public IEResult getBestSolution() {

      int[] defaultOrder = new int[instance.getNumAdvetisers()];
      for (int a = 0; a < defaultOrder.length; a++) {
         defaultOrder[a] = a;
      }

      return estimator.search(defaultOrder);
   }

}
