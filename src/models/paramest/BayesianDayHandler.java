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

public class BayesianDayHandler extends ConstantsAndFunctions {

   public final static int numSlots = 5;
   double _otherAdvertiserEffects;
   double _curProbClick;
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
   int _totalClicks;
   int _numberPromotedSlots;
   LinkedList<Integer> _impressionsPerSlot;
   double _ourAdvertiserEffect;
   LinkedList<LinkedList<Ad>> _advertisersAdsAbovePerSlot;
   HashMap<Product, LinkedList<double[]>> _userStatesOfSearchingUsers; // [IS, non-IS]
   boolean _targeted;
   Product _target;
   private double[] _c;

   public BayesianDayHandler(Query q, int totalClicks, int numberPromotedSlots,
                             LinkedList<Integer> impressionsPerSlot,
                             double ourAdvertiserEffect,
                             LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot, // <our slot < their slots <ad>>
                             HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers,
                             boolean targeted, Product target, double[] c) {

      _q = q;
      _totalClicks = totalClicks;
      _numberPromotedSlots = numberPromotedSlots;
      _impressionsPerSlot = impressionsPerSlot;
      _ourAdvertiserEffect = ourAdvertiserEffect;
      _advertisersAdsAbovePerSlot = advertisersAdsAbovePerSlot;
      _userStatesOfSearchingUsers = userStatesOfSearchingUsers;
      _targeted = targeted;
      _target = target;
      _c = c;

      if (_q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         _otherAdvertiserEffects = _advertiserEffectBoundsAvg[0];
      } else if (_q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
         _otherAdvertiserEffects = _advertiserEffectBoundsAvg[1];
      } else {
         _otherAdvertiserEffects = _advertiserEffectBoundsAvg[2];
      }

      _saw = new boolean[numSlots];
      for (int i = 0; i < numSlots; i++) {
         _saw[i] = (_impressionsPerSlot.get(i) > 0);
      }

      int qTypeIdx;
      if (_q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         qTypeIdx = 0;
      } else if (_q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
         qTypeIdx = 1;
      } else {
         qTypeIdx = 2;
      }

      _contProbDist = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      _contProbWeights = new ArrayList<Double>(NUM_DISCRETE_PROBS);
      for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
         _contProbDist.add(_continuationProbBounds[qTypeIdx][0] + (_continuationProbBounds[qTypeIdx][1] - _continuationProbBounds[qTypeIdx][0]) * (1.0 / (NUM_DISCRETE_PROBS - 1.0)) * i);
         _contProbWeights.add(BASE_WEIGHT);
      }

      updateEstimate(_ourAdvertiserEffect, _c);
   }

   private double otherAdvertiserConvProb() {
      if (_q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return _c[0];
      } else if (_q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
         return _c[1];
      } else {
         return _c[2];
      }
   }

   public void updateEstimate(double ourAdvertiserEffect, double[] c) {
      _c = c;
      double[] coeff = new double[numSlots];
      for (int i = 0; i < numSlots; i++) {
         coeff[i] = 0;
      }
      double views = 0;
      for (Product p : _userStatesOfSearchingUsers.keySet()) {
         int ft = getFTargetIndex(_targeted, p, _target);
         for (int ourSlot = 0; ourSlot < numSlots; ourSlot++) {
            if (_saw[ourSlot]) {
               LinkedList<Ad> advertisersAboveUs = _advertisersAdsAbovePerSlot.get(ourSlot);
               double ftfp = fTargetfPro[ft][bool2int(_numberPromotedSlots >= ourSlot + 1)];
               double theoreticalClickProb = etaClickPr(ourAdvertiserEffect, ftfp);
               double IS = _userStatesOfSearchingUsers.get(p).get(ourSlot)[0];
               double nonIS = _userStatesOfSearchingUsers.get(p).get(ourSlot)[1];
               if (IS + nonIS > 0) {
                  views += IS + nonIS;
                  for (int prevSlot = 0; prevSlot < ourSlot; prevSlot++) {
                     Ad otherAd = advertisersAboveUs.get(prevSlot);
                     int ftOther = 0;
                     if (otherAd != null) {
                        ftOther = getFTargetIndex(!otherAd.isGeneric(), p, otherAd.getProduct());
                     }
                     double ftfpOther = fTargetfPro[ftOther][bool2int(_numberPromotedSlots >= prevSlot + 1)];
                     double otherAdvertiserClickProb = etaClickPr(_otherAdvertiserEffects, ftfpOther);
                     nonIS *= (1.0 - otherAdvertiserConvProb() * otherAdvertiserClickProb);
                  }
                  coeff[(numSlots - 1) - ourSlot] += (theoreticalClickProb * (IS + nonIS));
               }
            }
         }
      }

      if (views > _totalClicks) {

         double total = 0.0;
         for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
            double contProb = _contProbDist.get(i);
            double clicks = calculateNumClicks(coeff, contProb);
            assert (views > clicks);
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
      } else {
         _currentEstimate = Double.NaN;
      }
   }

   public double getContinuationProbability() {
      return _currentEstimate;
   }

   public double getBinomialProb(double numImps, double numClicks, double observedClicks) {
      return getBinomialProbUsingGaussian(numImps, numClicks / ((double) numImps), observedClicks);
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
      clicks += coeff[4];
      clicks += contProb * coeff[3];
      clicks += contProb2 * coeff[2];
      clicks += contProb3 * coeff[1];
      clicks += contProb4 * coeff[0];
      return clicks;
   }

   public double getInformationWeight() {
      /*
         * Weight by the number of impressions in the sample
         */
      double weight = 0.0;
      for (int i = 0; i < _impressionsPerSlot.size(); i++) {
         weight += _impressionsPerSlot.get(i);
      }
      return weight;
   }
}
