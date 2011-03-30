package models.queryanalyzer.forecast;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.*;

/**
 * Exponential Moving Average Impression Forecaster
 */
public class EMAImpressionForecaster extends AbstractImpressionForecaster {

   double _alpha;
   int _preEMADays = 15;
   Set<Query> _querySpace;
   String[] _agents;
   int _ourIdx;
   List<Map<Query, List<Integer>>> _impressions;
   List<Map<Query, Integer>> _predictions;


   public EMAImpressionForecaster(double alpha, Set<Query> querySpace, String[] agents, int ourIdx) {
      _alpha = alpha;
      _querySpace = querySpace;
      _agents = agents;
      _ourIdx = ourIdx;
      _impressions = new ArrayList<Map<Query, List<Integer>>>();
      _predictions = new ArrayList<Map<Query, Integer>>();
      for (int i = 0; i < agents.length; i++) {
         HashMap<Query, List<Integer>> impressionMap = new HashMap<Query, List<Integer>>();
         HashMap<Query, Integer> predMap = new HashMap<Query, Integer>();
         for (Query q : _querySpace) {
            ArrayList<Integer> impressions = new ArrayList<Integer>();
            impressionMap.put(q, impressions);
            predMap.put(q, -1);
         }
         _impressions.add(impressionMap);
         _predictions.add(predMap);
      }
   }

   @Override
   public double getPrediction(int agent, Query q) {
      return _predictions.get(agent).get(q);
   }

   @Override
   public boolean updateModel(List<Map<Query, Integer>> allImpressions) {
      for (int i = 0; i < _impressions.size(); i++) {
         if (i != _ourIdx) {
            Map<Query, List<Integer>> impressionHistMap = _impressions.get(i);
            Map<Query, Integer> impressionMap = allImpressions.get(i);
            Map<Query, Integer> predMap = _predictions.get(i);
            for (Query q : _querySpace) {
               List<Integer> impHist = impressionHistMap.get(q);
               int imps;
               if (impressionMap.get(q) == null) {
                  imps = 0;
               } else {
                  imps = impressionMap.get(q);
               }

               if (imps > 0) {
                  impHist.add(imps);
                  impressionHistMap.put(q, impHist);

                  int numPreds = impHist.size();
                  int lastPred = predMap.get(q);
                  if (numPreds > _preEMADays) {
                     if (numPreds == _preEMADays + 1) {
                        for (int j = 0; j < numPreds; j++) {
                           lastPred = (int) (_alpha * impHist.get(j) + (1.0 - _alpha) * lastPred);
                        }
                        predMap.put(q, lastPred);
                     } else {
                        predMap.put(q, (int) (_alpha * imps + (1.0 - _alpha) * lastPred));
                     }
                  } else {
                     if (lastPred == -1) {
                        predMap.put(q, imps);
                     } else {
                        predMap.put(q, (int) ((imps + (numPreds - 1) * lastPred) / ((double) numPreds)));
                     }
                  }
               }
            }
         }
      }
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new EMAImpressionForecaster(_alpha, _querySpace, _agents, _ourIdx);
   }
}
