package models.queryanalyzer.riep;

import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;

public class MIPImpressionAndRankEstimator implements ImpressionAndRankEstimator {

   QAInstanceAll instance;
   EricImpressionEstimator estimator;

   public MIPImpressionAndRankEstimator(QAInstanceAll instance, boolean useRankingConstraints, boolean integerProgram, boolean multipleSolutions, double timeout) {
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
