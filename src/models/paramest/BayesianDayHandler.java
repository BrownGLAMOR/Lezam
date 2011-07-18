package models.paramest;

/**
 *
 * @author mbarrows, jberg
 */

/*
 * TODO:
 * 
 * 1) Work on otherAdvertiserConvProb.  It should use some estimate of component specialties
 * and over capacity
 * 
 * 2) updateEstimate assumes that if an agent is in a slot that CAN be promoted than it is
 * promoted.  This isn't true if the advertiser's bid is below the reserve
 * 
 */

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import simulator.parser.GameStatusHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import static models.paramest.ConstantsAndFunctions.*;

public class BayesianDayHandler {

   double _otherAdvertiserEffects;
   double _currentEstimate;
   ArrayList<Double> _contProbDist;
   ArrayList<Double> _contProbWeights;
   public static final int MAX_MLE_SOLS = 4;
   public static final int NUM_DISCRETE_PROBS = 30;
   public static final double BASE_WEIGHT = 1.0 / NUM_DISCRETE_PROBS;
   public static final boolean MLE = false;

   boolean[] _saw;

   // inputs defined by constructor
   Query _q;
   QueryType _qType;
   int _qTypeIdx;
   int _totalClicks;
   int _numberPromotedSlots;
   LinkedList<Integer> _impressionsPerSlot;
   double _ourAdvertiserEffect;
   HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> _userStates;
   boolean _targeted;
   Product _target;
   int _numSlots;
   double _weight;
   double[] _c;

   public BayesianDayHandler(Query q, int totalClicks, int numSlots, int numberPromotedSlots,
                             LinkedList<Integer> impressionsPerSlot,
                             double ourAdvertiserEffect,HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> userStates,
                             boolean targeted, Product target, double[] c) {

      _q = q;
      _totalClicks = totalClicks;
      _numSlots = numSlots;
      _numberPromotedSlots = numberPromotedSlots;
      _impressionsPerSlot = impressionsPerSlot;
      _ourAdvertiserEffect = ourAdvertiserEffect;
      _userStates = userStates;
      _targeted = targeted;
      _target = target;
      _c = c.clone();
      _qType = _q.getType();
      _qTypeIdx = queryTypeToInt(_qType);
      _otherAdvertiserEffects = _advertiserEffectBoundsAvg[_qTypeIdx];

      /*
         * Weight by the number of impressions in the sample
         */
      _weight = 0.0;
      for (int i = 0; i < _impressionsPerSlot.size(); i++) {
         _weight += _impressionsPerSlot.get(i);
      }

      _saw = new boolean[_numSlots];
      for (int i = 0; i < _numSlots; i++) {
         _saw[i] = (_impressionsPerSlot.get(i) > 0);
      }

      _contProbDist = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      _contProbWeights = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
         _contProbDist.add(_continuationProbBounds[_qTypeIdx][0] + (_continuationProbBounds[_qTypeIdx][1] - _continuationProbBounds[_qTypeIdx][0]) * (1.0 / (NUM_DISCRETE_PROBS - 1.0)) * i);
         _contProbWeights.add(BASE_WEIGHT);
      }

      updateEstimate(_ourAdvertiserEffect,_c);
   }

   public void updateEstimate(double ourAdvertiserEffect, double[] c) {
      _ourAdvertiserEffect = ourAdvertiserEffect;
      _c = c.clone();

      int imps = 0;
      for (int i = 0; i < _impressionsPerSlot.size(); i++) {
         imps += _impressionsPerSlot.get(i);
      }

      if (imps > _totalClicks) {
         double total = 0.0;
         for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
            double contProb = _contProbDist.get(i);
            double[] prView = getPrView(_q,_numSlots,_numberPromotedSlots,_advertiserEffectBoundsAvg[_qTypeIdx],contProb,_c[_qTypeIdx],_userStates);
            double clicks = 0.0;
            for(int slot = 0; slot < _numSlots; slot++) {
               clicks += _impressionsPerSlot.get(slot)*prView[slot]*_ourAdvertiserEffect;
            }
            double prob = getBinomialProb(imps, _totalClicks, clicks);
            double newProb = _contProbWeights.get(i) * prob;
            _contProbWeights.set(i, newProb);
            total += newProb;
         }

         for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
            _contProbWeights.set(i, _contProbWeights.get(i) / total);
         }

         if (MLE) {
            ArrayList<Integer> MLEIndices = new ArrayList<Integer>();
            double maxWeight = 0.0;
            for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
               if (_contProbWeights.get(i) > maxWeight) {
                  MLEIndices = new ArrayList<Integer>();
                  maxWeight = _contProbWeights.get(i);
                  MLEIndices.add(i);
               } else if (_contProbWeights.get(i) == maxWeight) {
                  MLEIndices.add(i);
               }
            }

            if ((MLEIndices.size() >= 1) && (MLEIndices.size() <= MAX_MLE_SOLS)) {
               _currentEstimate = 0.0;
               for (int i = 0; i < MLEIndices.size(); i++) {
                  _currentEstimate += (1.0 / (MLEIndices.size())) * _contProbDist.get(MLEIndices.get(i));
               }
            } else {
               _currentEstimate = 0.0;
               for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
                  _currentEstimate += _contProbWeights.get(i) * _contProbDist.get(i);
               }
            }
         } else {
            _currentEstimate = 0.0;
            for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
               _currentEstimate += _contProbWeights.get(i) * _contProbDist.get(i);
            }
         }
      }
      else {
         _currentEstimate = _continuationProbBoundsAvg[_qTypeIdx];
      }

      if(_currentEstimate > _continuationProbBounds[_qTypeIdx][1]) {
         _currentEstimate = _continuationProbBounds[_qTypeIdx][1];
      }
      else if(_currentEstimate < _continuationProbBounds[_qTypeIdx][0]) {
         _currentEstimate = _continuationProbBounds[_qTypeIdx][0];
      }
   }

   public double getContinuationProbability() {
      return _currentEstimate;
   }

   public double getBinomialProb(double numImps, double numClicks, double observedClicks) {
      return getBinomialProbUsingGaussian(numImps, numClicks /  numImps, observedClicks);
   }

   public double getBinomialProbUsingGaussian(double n, double p, double k) {
      double mean = n * p;
      double sigma2 = mean * (1.0 - p);
      double diff = k - mean;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
   }

   public double getInformationWeight() {
      return _weight;
   }
}
