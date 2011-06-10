package models.paramest;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import java.util.*;

public class BayesianParameterEstimation extends AbstractParameterEstimation {

   double[] _c;
   int _ourAdvIdx;
   int _numSlots;
   int _numPromSlots;
   Set<Query> _queryspace;
   HashMap<Query, BayesianQueryHandler> m_queryHandlers;

   public BayesianParameterEstimation(double[] c, int ourAdvIdx, int numSlots, int numPromSlots, Set<Query> queryspace) {
      _c = c;
      _ourAdvIdx = ourAdvIdx;
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;
      _queryspace = queryspace;

      m_queryHandlers = new HashMap<Query, BayesianQueryHandler>();

      for (Query q : _queryspace) {
         m_queryHandlers.put(q, new BayesianQueryHandler(q, _c, _numSlots, _numPromSlots));
      }
   }

   @Override
   public double[] getPrediction(Query q) {
      return m_queryHandlers.get(q).getPredictions();
   }

   @Override
   public boolean updateModel(QueryReport queryReport,
                              BidBundle bidBundle,
                              HashMap<Query, int[]> allOrders,
                              HashMap<Query, int[]> allImpressions,
                              HashMap<Query, int[][]> allWaterfalls,
                              HashMap<Product, HashMap<UserState, Integer>> userStates,
                              double[] c) {

      for (Query q : _queryspace) {

         int[][] impressionMatrix = allWaterfalls.get(q);
         //We can only update if we have a waterfall prediction
         if(impressionMatrix != null) {
            int[] order = allOrders.get(q);
            int[] impressions = allImpressions.get(q);

            int impSum = 0;
            for (int i = 0; i < impressions.length; i++) {
               impSum += impressions[i];
            }

            if (impSum > 0) {
               HashMap<String, Ad> query_ads = new HashMap<String, Ad>();
               for (int i = 0; i < 8; i++) {
                  if (i == _ourAdvIdx) {
                     query_ads.put("adv" + (i + 1), bidBundle.getAd(q));
                  } else {
                     query_ads.put("adv" + (i + 1), queryReport.getAd(q, "adv" + (i + 1)));
                  }
               }

               LinkedList<LinkedList<String>> advertisersAbovePerSlot = new LinkedList<LinkedList<String>>();
               LinkedList<Integer> impressionsPerSlot = new LinkedList<Integer>();

               //where are we in bid pair matrix?
               ArrayList<Integer> aboveUs = new ArrayList<Integer>();
               for (int agentID : order) {
                  if (agentID == _ourAdvIdx) {
                     //We only want advertisers above us so exit loop
                     break;
                  } else {
                     aboveUs.add(agentID);
                  }
               }

               for (int j = 0; j < _numSlots; j++) {
                  int numImpressions = impressionMatrix[_ourAdvIdx][j];
                  impressionsPerSlot.add(numImpressions);

                  LinkedList<String> advsAbove = null;
                  if (numImpressions != 0) {
                     advsAbove = new LinkedList<String>();
                     if(j > 0) {
                        //TODO actually find the people above us...
                        List<Integer> sublist = aboveUs.subList(0, j);
                        for (Integer id : sublist) {
                           advsAbove.add("adv" + (id + 1));
                        }
                     }
                  }

                  advertisersAbovePerSlot.add(advsAbove);
               }

               m_queryHandlers.get(q).update("adv" + (_ourAdvIdx + 1), queryReport, impressionsPerSlot, advertisersAbovePerSlot, query_ads, userStates, c);
            }
         }
      }
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new BayesianParameterEstimation(_c, _ourAdvIdx,_numSlots,_numPromSlots,_queryspace);
   }
}
