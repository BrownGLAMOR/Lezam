package models.queryanalyzer.forecast;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.*;

/**
 * Simple Moving Average Impression Forecaster
 */
public class SMAImpressionForecaster extends AbstractImpressionForecaster {

   int _days;
   Set<Query> _querySpace;
   String[] _agents;
   int _ourIdx;
   List<Map<Query, List<Integer>>> _impressions;
   List<Map<Query, Integer>> _predictions;


   public SMAImpressionForecaster(int days, Set<Query> querySpace, String[] agents, int ourIdx) {
      _days = days;
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
                  if (numPreds > _days) {
                     predMap.put(q, lastPred + (int) ((-impHist.get(numPreds - 1 - _days) + imps) / ((double) _days)));
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
      return new SMAImpressionForecaster(_days, _querySpace, _agents, _ourIdx);
   }
}
