package models.paramest;

/**
 *
 * @author mbarrows, jberg
 */

import edu.umich.eecs.tac.props.*;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class BayesianQueryHandler extends ConstantsAndFunctions {
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

      _c = c;
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;

      _query = q;
      _queryType = q.getType();

      _dayHandlers = new LinkedList<BayesianDayHandler>();

      if (_query.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         _qTypeIdx = 0;
      } else if (_query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
         _qTypeIdx = 1;
      } else {
         _qTypeIdx = 2;
      }

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

   public boolean update(String ourAgent, QueryReport queryReport,
                         LinkedList<Integer> impressionsPerSlot,
                         LinkedList<LinkedList<String>> advertisersAbovePerSlot,
                         HashMap<String, Ad> ads,
                         HashMap<Product, HashMap<UserState, Integer>> userStates, double[] c) {
      _c = c;

      int totImpressions = queryReport.getImpressions(_query);
      int totClicks = queryReport.getClicks(_query);

      if (totImpressions > MIN_IMPS && totClicks > MIN_CLICKS) {

         // Were we ever not in a top slot
         boolean notinslot1 = false;
         for (int i = 1; i < 5; i++) {
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


         Ad ourAd = ads.get(ourAgent);
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

            // Make list of Ads above
            LinkedList<LinkedList<Ad>> adsAbovePerSlot = getAdsAbovePerSlot(advertisersAbovePerSlot, ads);

            // get states of searching users
            HashMap<Product, Double> imprdist = getImpressionDist(userStates, queryReport.getImpressions(_query));
            HashMap<Product, LinkedList<double[]>> statesSearchingUsers = getStatesOfSearchingUsers(userStates, impressionsPerSlot, imprdist);

            BayesianDayHandler latestday = new BayesianDayHandler(_query, totalClicks, _numSlots,
                                                                  _numPromSlots, impressionsPerSlot, _lastPredictions[0],
                                                                  adsAbovePerSlot, statesSearchingUsers, (ourAdTargeted),
                                                                  ourAdProduct, _c);

            _dayHandlers.add(latestday);

         } else if (inslot1) {
            // Update advertiser effect
            HashMap<Product, Double> clickdist = getClickDist(userStates, queryReport.getClicks(_query), ourAdTargeted, ourAdProduct);
            HashMap<Product, Double> imprdist = getImpressionDist(userStates, queryReport.getImpressions(_query));

            int promoted = 0;
            if (queryReport.getPromotedImpressions(_query) > 0) {
               promoted = 1;
            }

            double total = 0.0;
            for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
               double advEff = _advEffDist.get(i);

               double imps = 0.0;
               double trueClicks = 0.0;
               double estClicks = 0.0;
               for (Product p : userStates.keySet()) {
                  int targeted = getFTargetIndex(ourAdTargeted, p, ourAdProduct);
                  double ftargfpro = fTargetfPro[targeted][promoted];
                  double clickPr = etaClickPr(advEff, ftargfpro);

                  trueClicks += clickdist.get(p);
                  imps += imprdist.get(p);
                  estClicks += clickPr * imprdist.get(p);
               }

               assert (imps > trueClicks);
               assert (imps > estClicks);
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
            } else {
               for (int i = 0; i < NUM_DISCRETE_PROBS; i++) {
                  currentEstimate += _advEfWeights.get(i) * _advEffDist.get(i);
               }
            }

            if (Double.isNaN(currentEstimate)) {
               System.out.println("bad estimate!!!!!!");
            }

            _lastPredictions[0] = currentEstimate;

            // Update all previous continuation probability estimates
            for (BayesianDayHandler dh : _dayHandlers) {
               dh.updateEstimate(_lastPredictions[0], _c);
            }

         } else {
            // do nothing if we saw no impressions
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

      //TODO: take me out for the competition
      if (Double.isNaN(_lastPredictions[0]) || Double.isNaN(_lastPredictions[1])) {
         throw new RuntimeException();
      }

      return true;
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


   public LinkedList<LinkedList<Ad>> getAdsAbovePerSlot(LinkedList<LinkedList<String>> advertisersAbovePerSlot, HashMap<String, Ad> ads) {
      LinkedList<LinkedList<Ad>> adsAbovePerSlot = new LinkedList<LinkedList<Ad>>();
      // for every slot
      for (LinkedList<String> thoseAbove : advertisersAbovePerSlot) {
         // make a linked list of Ads
         LinkedList<Ad> adsAbove = null;
         if(thoseAbove != null) {
            adsAbove = new LinkedList<Ad>();
            // for each advertiser above
            for (String advertiser : thoseAbove) {
               // add their add to the list
               adsAbove.add(ads.get(advertiser));
            }
         }
         adsAbovePerSlot.add(adsAbove);
      }
      return adsAbovePerSlot;
   }

   // Get Click Distribution
   public HashMap<Product, Double> getClickDist(HashMap<Product, HashMap<UserState, Integer>> userStates, int clicks, boolean ourAdTargeted, Product ourAdProduct) {
      HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
      // for each product
      int total = 0;
      int totalMatching = 0;
      int totalNotMatching = 0;
      HashMap<Product, Double> userDist = new HashMap<Product, Double>();
      for (Product p : userStates.keySet()) {
         HashMap<UserState, Integer> states = userStates.get(p);
         // add up the number of searching users
         double searching = 0;
         if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO) &&
                 _query.getComponent().equals(p.getComponent()) &&
                 _query.getManufacturer().equals(p.getManufacturer())) {
            searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F2);
         }
         if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE) &&
                 ((_query.getComponent() != null && _query.getComponent().equals(p.getComponent())) ||
                          (_query.getManufacturer() != null && _query.getManufacturer().equals(p.getManufacturer())))) {
            searching = 1.0 / 6.0 * states.get(UserState.IS) + 0.5 * states.get(UserState.F1);
         }
         if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
            searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F0);
         }
         total += searching;
         userDist.put(p, searching);
         if (ourAdTargeted) {
            if (p.equals(ourAdProduct)) {
               totalMatching += searching;
            } else {
               totalNotMatching += searching;
            }
         }
      }
      // TODO split up clicks intelligently if it's targeted, else do for loop
      // note: use totalMatching and totalNotMatching to determine how to
      // weight the clicks. I tried to do this on paper, but the eta equation
      // screws it up and makes it not easy. good luck?
      for (Product p : userStates.keySet()) {
         toreturn.put(p, 1.0 * userDist.get(p) * ((double) clicks) / ((double) total));
      }
      return toreturn;
   }

   // Get Impression Distribution
   public HashMap<Product, Double> getImpressionDist(HashMap<Product, HashMap<UserState, Integer>> userStates, int impressions) {
      HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
      // for each product
      int total = 0;
      HashMap<Product, Double> userDist = new HashMap<Product, Double>();
      for (Product p : userStates.keySet()) {
         HashMap<UserState, Integer> states = userStates.get(p);
         // add up the number of searching users
         double searching = 0;
         if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO) &&
                 _query.getComponent().equals(p.getComponent()) &&
                 _query.getManufacturer().equals(p.getManufacturer())) {
            searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F2);
         }
         if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE) &&
                 ((_query.getComponent() != null && _query.getComponent().equals(p.getComponent())) ||
                          (_query.getManufacturer() != null && _query.getManufacturer().equals(p.getManufacturer())))) {
            searching = 1.0 / 6.0 * states.get(UserState.IS) + 0.5 * states.get(UserState.F1);
         }
         if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
            searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F0);
         }
         total += searching;
         userDist.put(p, searching);
      }
      for (Product p : userStates.keySet()) {
         toreturn.put(p, 1.0 * userDist.get(p) * ((double) impressions) / ((double) total));
      }
      return toreturn;
   }

   public HashMap<Product, LinkedList<double[]>> getStatesOfSearchingUsers(HashMap<Product, HashMap<UserState, Integer>> userStates, LinkedList<Integer> impressionsPerSlot, HashMap<Product, Double> imprdist) {

      double impressions = 0;
      for (Product p : imprdist.keySet()) {
         impressions += imprdist.get(p);
      }

      HashMap<Product, LinkedList<double[]>> toreturn = new HashMap<Product, LinkedList<double[]>>();
      // for each product
      for (Product p : userStates.keySet()) {
         if (imprdist.get(p) > 0) {
            HashMap<UserState, Integer> states = userStates.get(p);
            // count up how many searching users there were
            double ISusers = 0.0;
            double nonISusers = 0.0;
            if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO) &&
                    _query.getComponent().equals(p.getComponent()) &&
                    _query.getManufacturer().equals(p.getManufacturer())) {
               ISusers = 1.0 / 3.0 * states.get(UserState.IS);
               nonISusers = states.get(UserState.F2);
            }
            if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE) &&
                    ((_query.getComponent() != null && _query.getComponent().equals(p.getComponent())) ||
                             (_query.getManufacturer() != null && _query.getManufacturer().equals(p.getManufacturer())))) {
               ISusers = 1.0 / 6.0 * states.get(UserState.IS);
               nonISusers = 0.5 * states.get(UserState.F1);
            }
            if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
               ISusers = 1.0 / 3.0 * states.get(UserState.IS);
               nonISusers = states.get(UserState.F0);
            }
            // double nonISusers =
            // states.get(UserState.F0)+states.get(UserState.F1)+states.get(UserState.F2);
            // make a list of Is, non Is users arrays, one for each slot
            LinkedList<double[]> userSlotDist = new LinkedList<double[]>();
            double sum = ISusers + nonISusers;
            for (Integer integer : impressionsPerSlot) {
               double ISusersPerSlot = 0.0;
               double nonISusersPerSlot = 0.0;
               if (sum > 0.0) {
                  ISusersPerSlot = (((double) ISusers * ((double) integer)) / sum) * (imprdist.get(p) / impressions);
                  nonISusersPerSlot = (((double) nonISusers * ((double) integer)) / sum) * (imprdist.get(p) / impressions);
               }
               double[] statesOfSearchingUsersPerSlot = {ISusersPerSlot, nonISusersPerSlot};
               userSlotDist.add(statesOfSearchingUsersPerSlot);
            }
            toreturn.put(p, userSlotDist);
         } else {
            /*
                 * These will always be zero, because we are not getting any
                 * impressions from this product
                 */
            LinkedList<double[]> userSlotDist = new LinkedList<double[]>();
            for (Integer imps : impressionsPerSlot) {
               double[] statesOfSearchingUsersPerSlot = {0.0, 0.0};
               userSlotDist.add(statesOfSearchingUsersPerSlot);
            }
            toreturn.put(p, userSlotDist);
         }
      }
      return toreturn;
   }
}
