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

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

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
   LinkedList<LinkedList<Ad>> _adsAbovePerSlot;
   HashMap<Product, LinkedList<double[]>> _userStatesOfSearchingUsers; // [IS, non-IS]
   boolean _targeted;
   Product _target;
   int _numSlots;
   double _weight;
   double[] _c;

   public BayesianDayHandler(Query q, int totalClicks, int numSlots, int numberPromotedSlots,
                             LinkedList<Integer> impressionsPerSlot,
                             double ourAdvertiserEffect, LinkedList<LinkedList<Ad>> adsAbovePerSlot,
                             HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers,
                             boolean targeted, Product target, double[] c) {

      _q = q;
      _totalClicks = totalClicks;
      _numSlots = numSlots;
      _numberPromotedSlots = numberPromotedSlots;
      _impressionsPerSlot = impressionsPerSlot;
      _ourAdvertiserEffect = ourAdvertiserEffect;
      _adsAbovePerSlot = adsAbovePerSlot;
      _userStatesOfSearchingUsers = userStatesOfSearchingUsers;
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

   public double otherAdvertiserConvProb() {
      return _c[_qTypeIdx];
   }

   public void updateEstimate(double ourAdvertiserEffect, double[] c) {
      _ourAdvertiserEffect = ourAdvertiserEffect;
      _c = c.clone();
      double[] coeff = new double[_numSlots];
      for (int i = 0; i < _numSlots; i++) {
         coeff[i] = 0;
      }
      double views = 0;
      for (Product p : _userStatesOfSearchingUsers.keySet()) {
         int ft = getFTargetIndex(_targeted, p, _target);
         for (int ourSlot = 0; ourSlot < _numSlots; ourSlot++) {
            if (_saw[ourSlot]) {
               double ftfp = fTargetfPro[ft][bool2int(_numberPromotedSlots >= ourSlot + 1)];
               double theoreticalClickProb = etaClickPr(_ourAdvertiserEffect, ftfp);
               double IS = _userStatesOfSearchingUsers.get(p).get(ourSlot)[0];
               double nonIS = _userStatesOfSearchingUsers.get(p).get(ourSlot)[1];
               LinkedList<Ad> advertisersAboveUs = _adsAbovePerSlot.get(ourSlot);
               for (int prevSlot = 0; prevSlot < ourSlot; prevSlot++) {
                  Ad otherAd = null;
                  if(advertisersAboveUs.size() > 0) {
                     otherAd = advertisersAboveUs.get(Math.min(prevSlot,advertisersAboveUs.size()-1));
                  }
                  int ftOther = 0;
                  if (otherAd != null) {
                     ftOther = getFTargetIndex(!otherAd.isGeneric(), p, otherAd.getProduct());
                  }
                  double ftfpOther = fTargetfPro[ftOther][bool2int(_numberPromotedSlots >= (prevSlot + 1))];
                  double otherAdvertiserClickProb = etaClickPr(_otherAdvertiserEffects, ftfpOther);
                  nonIS *= (1.0 - otherAdvertiserConvProb() * otherAdvertiserClickProb);
               }
               if (IS + nonIS > 0) {
                  views += IS + nonIS;
                  coeff[ourSlot] += (theoreticalClickProb * (IS + nonIS));
               }
            }
         }
      }

      if (views > _totalClicks) {
         double total = 0.0;
         for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
            double contProb = _contProbDist.get(i);
            double clicks = calculateNumClicks(coeff, contProb);
            double prob = getBinomialProb(views, _totalClicks, clicks);
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

   public double calculateNumClicks(double coeff[], double contProb) {
      double clicks = 0.0;
      double contProb2 = contProb * contProb;
      double contProb3 = contProb2 * contProb;
      double contProb4 = contProb3 * contProb;
      clicks += coeff[0];
      clicks += contProb * coeff[1];
      clicks += contProb2 * coeff[2];
      clicks += contProb3 * coeff[3];
      clicks += contProb4 * coeff[4];
      return clicks;
   }

   public double getInformationWeight() {
      return _weight;
   }
}
