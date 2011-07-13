package models.paramest;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;
import java.util.Set;

public class BayesianParameterEstimation extends AbstractParameterEstimation {

   double[] _c;
   int _ourAdvIdx;
   int _numSlots;
   int _numPromSlots;
   double _squashParam;
   Set<Query> _queryspace;
   HashMap<Query, BayesianQueryHandler> _queryHandlers;
   HashMap<QueryType, ReserveEstimator> _reserveHandlers;

   public BayesianParameterEstimation(double[] c, int ourAdvIdx, int numSlots, int numPromSlots, double squashParam, Set<Query> queryspace) {
      _c = c.clone();
      _ourAdvIdx = ourAdvIdx;
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;
      _queryspace = queryspace;
      _squashParam = squashParam;

      _queryHandlers = new HashMap<Query, BayesianQueryHandler>();

      for (Query q : _queryspace) {
         _queryHandlers.put(q, new BayesianQueryHandler(q, _c, _numSlots, _numPromSlots));
      }

      _reserveHandlers = new HashMap<QueryType, ReserveEstimator>();
      _reserveHandlers.put(QueryType.FOCUS_LEVEL_ZERO, new ReserveEstimator(_numPromSlots,_squashParam,QueryType.FOCUS_LEVEL_ZERO,queryspace));
      _reserveHandlers.put(QueryType.FOCUS_LEVEL_ONE, new ReserveEstimator(_numPromSlots,_squashParam,QueryType.FOCUS_LEVEL_ONE,queryspace));
      _reserveHandlers.put(QueryType.FOCUS_LEVEL_TWO, new ReserveEstimator(_numPromSlots,_squashParam,QueryType.FOCUS_LEVEL_TWO,queryspace));
   }

   @Override
   public double getAdvEffectPrediction(Query q) {
      return _queryHandlers.get(q).getPredictions()[0];
   }

   @Override
   public double getContProbPrediction(Query q) {
      return _queryHandlers.get(q).getPredictions()[1];
   }

   @Override
   public double getRegReservePrediction(QueryType qt) {
      return _reserveHandlers.get(qt).getRegPrediction();
   }

   @Override
   public double getPromReservePrediction(QueryType qt) {
      return _reserveHandlers.get(qt).getPromPrediction();
   }

   @Override
   public boolean updateModel(QueryReport queryReport,
                              BidBundle bidBundle,
                              HashMap<Query, int[]> allImpressions,
                              HashMap<Query, int[][]> allWaterfalls,
                              HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> userStates,
                              double[] c) {

      for(QueryType qt : _reserveHandlers.keySet()) {
         ReserveEstimator reserveEstimator = _reserveHandlers.get(qt);
         reserveEstimator.updateModel(bidBundle, queryReport);
      }

      for (Query q : _queryspace) {

         int[][] impressionMatrix = allWaterfalls.get(q);
         //We can only update if we have a waterfall prediction
         if(impressionMatrix != null) {
            int[] impressions = allImpressions.get(q);

            int impSum = 0;
            for (int impression : impressions) {
               impSum += impression;
            }

            if (impSum > 0) {
               _queryHandlers.get(q).update(_ourAdvIdx, bidBundle, queryReport, impressionMatrix,userStates,c);
            }
         }
      }
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new BayesianParameterEstimation(_c, _ourAdvIdx,_numSlots,_numPromSlots,_squashParam,_queryspace);
   }
}
