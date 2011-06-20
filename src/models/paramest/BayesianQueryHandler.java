package models.paramest;

/**
 *
 * @author mbarrows, jberg
 */

import edu.umich.eecs.tac.props.*;
import simulator.parser.GameStatusHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import static models.paramest.ConstantsAndFunctions.*;

public class BayesianQueryHandler {
   final Query _query;
   final QueryType _queryType;

   ArrayList<Double> _advEffDist;
   ArrayList<Double> _advEfWeights;
   public static final int MIN_IMPS = 15;
   public static final int MIN_CLICKS = 3;
   public static final int MAX_MLE_SOLS = 2;
   public static final int NUM_DISCRETE_PROBS = 30;
   public static final double BASE_WEIGHT = 1.0 / NUM_DISCRETE_PROBS;
   public static final boolean MLE = false;
   int _qTypeIdx;

   LinkedList<BayesianDayHandler> _dayHandlers;
   double[] _lastPredictions;

   double[] _c;
   int _numSlots;
   int _numPromSlots;

   public BayesianQueryHandler(Query q, double[] c, int numSlots, int numPromSlots) {

      _c = c.clone();
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;

      _query = q;
      _queryType = q.getType();

      _dayHandlers = new LinkedList<BayesianDayHandler>();

      _qTypeIdx = queryTypeToInt(_queryType);

      _advEffDist = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      _advEfWeights = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
         _advEffDist.add(_advertiserEffectBounds[_qTypeIdx][0] + (_advertiserEffectBounds[_qTypeIdx][1] - _advertiserEffectBounds[_qTypeIdx][0]) * (1.0 / (NUM_DISCRETE_PROBS - 1.0)) * i);
         _advEfWeights.add(BASE_WEIGHT);
      }

      _lastPredictions = new double[2];
      _lastPredictions[0] = _advertiserEffectBoundsAvg[_qTypeIdx];
      _lastPredictions[1] = _continuationProbBoundsAvg[_qTypeIdx];
   }

   // Returns advertiser effect and continuation probability
   public double[] getPredictions() {
      return _lastPredictions;
   }

   public boolean update(int ourIdx, BidBundle bundle, QueryReport queryReport, int[][] impressionMatrix,
                         HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> userStates, double[] c) {
      _c = c.clone();

      int totImpressions = queryReport.getImpressions(_query);
      int totClicks = queryReport.getClicks(_query);

      if (totImpressions > MIN_IMPS && totClicks > MIN_CLICKS) {

         LinkedList<Integer> impressionsPerSlot = new LinkedList<Integer>();
         for (int j = 0; j < _numSlots; j++) {
            impressionsPerSlot.add(impressionMatrix[ourIdx][j]);
         }

         // Were we ever not in a top slot
         boolean notinslot1 = false;
         for (int i = 1; i < _numSlots; i++) {
            if (impressionsPerSlot.get(i) > 0) {
               notinslot1 = true;
               break;
            }
         }

         // Were we in the top slot the whole time
         boolean inslot1 = false;
         if (!notinslot1 && impressionsPerSlot.get(0) > 0) {
            inslot1 = true;
         }

         Ad ourAd = bundle.getAd(_query);
         boolean ourAdTargeted = true;
         if (ourAd == null || ourAd.isGeneric()) {
            ourAdTargeted = false;
         }

         Product ourAdProduct = null;
         if (ourAdTargeted) {
            ourAdProduct = ourAd.getProduct();
         }

         // If we were not in top slot for any part of the day, make day handler (to estimate cont prob)
         if (notinslot1) {
            int totalClicks = queryReport.getClicks(_query);

            BayesianDayHandler latestday = new BayesianDayHandler(_query, totalClicks, _numSlots,
                                                                  _numPromSlots, impressionsPerSlot, _lastPredictions[0],
                                                                  userStates, ourAdTargeted,
                                                                  ourAdProduct, _c);

            _dayHandlers.add(latestday);

         } else if (inslot1) {
            // Update advertiser effect
            HashMap<Product, Double> clickdist = getClickDist(_query,userStates, queryReport.getClicks(_query), ourAdTargeted, ourAdProduct);
            HashMap<Product, Double> imprdist = getImpressionDist(_query,userStates, queryReport.getImpressions(_query));

            int promoted = 0;
            if (queryReport.getPromotedImpressions(_query) > 0) {
               promoted = 1;
            }

            double imps = 0.0;
            double trueClicks = 0.0;
            for (Product p : userStates.keySet()) {
               imps += imprdist.get(p);
               trueClicks += clickdist.get(p);
            }

            double total = 0.0;
            for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
               double advEff = _advEffDist.get(i);

               double estClicks = 0.0;
               for (Product p : userStates.keySet()) {
                  int targeted = getFTargetIndex(ourAdTargeted, p, ourAdProduct);
                  double ftargfpro = fTargetfPro[targeted][promoted];
                  double clickPr = etaClickPr(advEff, ftargfpro);
                  estClicks += clickPr * imprdist.get(p);
               }

               double prob = getBinomialProb(imps, trueClicks, estClicks);
               double newProb = _advEfWeights.get(i) * prob;

               _advEfWeights.set(i, newProb);
               total += newProb;
            }

            for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
               _advEfWeights.set(i, _advEfWeights.get(i) / total);
            }

            double currentEstimate = 0.0;
            if (MLE) {
               ArrayList<Integer> MLEIndices = new ArrayList<Integer>();
               double maxWeight = 0.0;
               for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
                  if (_advEfWeights.get(i) > maxWeight) {
                     MLEIndices = new ArrayList<Integer>();
                     maxWeight = _advEfWeights.get(i);
                     MLEIndices.add(i);
                  } else if (_advEfWeights.get(i) == maxWeight) {
                     MLEIndices.add(i);
                  }
               }

               if ((MLEIndices.size() >= 1) && (MLEIndices.size() <= MAX_MLE_SOLS)) {
                  for (int i = 0; i < MLEIndices.size(); i++) {
                     currentEstimate += (1.0 / (MLEIndices.size())) * _advEffDist.get(MLEIndices.get(i));
                  }
               } else {
                  for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
                     currentEstimate += _advEfWeights.get(i) * _advEffDist.get(i);
                  }
               }
            }
            else {
               for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
                  currentEstimate += _advEfWeights.get(i) * _advEffDist.get(i);
               }
            }

            if (!Double.isNaN(currentEstimate)) {

               if(currentEstimate > _advertiserEffectBounds[_qTypeIdx][1]) {
                  currentEstimate = _advertiserEffectBounds[_qTypeIdx][1];
               }
               else if(currentEstimate < _advertiserEffectBounds[_qTypeIdx][0]) {
                  currentEstimate = _advertiserEffectBounds[_qTypeIdx][0];
               }

               _lastPredictions[0] = currentEstimate;

               // Update all previous continuation probability estimates
               for (BayesianDayHandler dh : _dayHandlers) {
                  dh.updateEstimate(_lastPredictions[0],_c);
               }
            }
            else {
               System.out.println("bad estimate!!!!!!");
            }

         }

         if (notinslot1 || inslot1) {
            double contProb = 0;
            double totalWeight = 0;
            for (BayesianDayHandler dh : _dayHandlers) {
               if (!Double.isNaN(dh.getContinuationProbability())) {
                  double weight = dh.getInformationWeight();
                  contProb += dh.getContinuationProbability() * weight;
                  totalWeight += weight;
               }
            }

            if (totalWeight > 0) {
               contProb = contProb / totalWeight;
               _lastPredictions[1] = contProb;
            }
         }
      }

      return true;
   }

}
