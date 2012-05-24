package models.bidmodel;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import models.AbstractModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IndependentBidModel extends AbstractBidModel {

   private boolean MLE = false;

   private static final double _aStep = Math.pow(2, (1.0 / 25.0));//HC num
   private HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> _allBidDists;
   private HashMap<Query, HashMap<String, ArrayList<Double>>> _allBidEsts;
   private HashMap<Query, HashMap<String, ArrayList<Double>>> _allPredDists;
   private double[] _transProbs;
   private int _numBidValues;
   private Set<Query> _querySpace;
   private double _randomJumpProb;
   private double _yesterdayProb;
   private double _nDaysAgoProb;
   private double _normVar;
   private int _numIterations;
   private String _ourAgent;
   Set<String> _advertisers;

   public IndependentBidModel(Set<String> advertisers, String me, int iterations, double randJump, double yesterday, double nDaysAgo, double var, Set<Query> querySpace) {

      _numIterations = iterations;
      _randomJumpProb = randJump;
      _yesterdayProb = yesterday;
      _nDaysAgoProb = nDaysAgo;
      _normVar = var;

      _advertisers = advertisers;

      _querySpace = querySpace;

      _ourAgent = me;

      _allBidDists = new HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>>(_querySpace.size());
      _allBidEsts = new HashMap<Query, HashMap<String, ArrayList<Double>>>(_querySpace.size());
      _allPredDists = new HashMap<Query, HashMap<String, ArrayList<Double>>>(_querySpace.size());
      Double startVal = Math.pow(2, (1.0 / 25.0 - 2.0)) - 0.25;//HC num
      for (Query q : _querySpace) {
         HashMap<String, ArrayList<ArrayList<Double>>> bidDistMap = new HashMap<String, ArrayList<ArrayList<Double>>>(advertisers.size());
         HashMap<String, ArrayList<Double>> bidEstMap = new HashMap<String, ArrayList<Double>>(advertisers.size());
         HashMap<String, ArrayList<Double>> predDistMap = new HashMap<String, ArrayList<Double>>(advertisers.size());
         for (String s : advertisers) {
            ArrayList<ArrayList<Double>> bidDists = new ArrayList<ArrayList<Double>>();
            ArrayList<Double> curDist = new ArrayList<Double>();
            _numBidValues = 0;
            int index = 0;
            for (Double curKey = startVal; curKey <= maxBid + 0.001; curKey = (curKey + 0.25) * _aStep - 0.25) {//HC num
               if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
                  curDist.add(InitDistributions.initDistF0[index]);
               } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
                  curDist.add(InitDistributions.initDistF1[index]);
               } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
                  curDist.add(InitDistributions.initDistF2[index]);
               }
               _numBidValues++;
               index++;
            }
            bidDists.add(curDist);
            bidDistMap.put(s, bidDists);

            ArrayList<Double> bidEsts = new ArrayList<Double>();
            bidEstMap.put(s, bidEsts);

            ArrayList<Double> predDists = new ArrayList<Double>();
            predDistMap.put(s, predDists);
         }
         _allBidDists.put(q, bidDistMap);
         _allBidEsts.put(q, bidEstMap);
         _allPredDists.put(q, predDistMap);
      }

      _transProbs = new double[_numBidValues];
      for (int i = 0; i < _numBidValues; i++) {
         _transProbs[i] = normalDensFn(i);
      }
      genCurEst();
      pushPredictionsForward();
   }

   private void genCurEst() {
      for (Query q : _querySpace) {
         HashMap<String, ArrayList<ArrayList<Double>>> bidDistsMap = _allBidDists.get(q);
         for (String s : bidDistsMap.keySet()) {
            ArrayList<ArrayList<Double>> bidDists = bidDistsMap.get(s);
            ArrayList<Double> curBidDist = new ArrayList<Double>(bidDists.get(bidDists.size() - 1));
            if(MLE) {
               _allBidEsts.get(q).get(s).add(maxAL(curBidDist));
            }
            else {
               _allBidEsts.get(q).get(s).add(averageAL(curBidDist));
            }
         }
      }
   }

   private void normalize(double[] arr) {
      double sum = 0.0;
      for (int i = 0; i < arr.length; i++) {
         sum += arr[i];
      }
      for (int i = 0; i < arr.length; i++) {
         arr[i] /= sum;
      }
   }

   private void normalizeAL(ArrayList<Double> lst) {
      double sum = 0.0;
      for (int i = 0; i < lst.size(); i++) {
         sum += lst.get(i);
      }
      for (int i = 0; i < lst.size(); i++) {
         lst.set(i, lst.get(i) / sum);
      }
   }

   private void normalizeAL(ArrayList<Double> lst, Query q) {
      double sum = 0.0;
      for (int i = 0; i < lst.size(); i++) {
         sum += lst.get(i);
      }

      if(!Double.isNaN(sum) && !Double.isInfinite(sum) && sum > 0) {
         for (int i = 0; i < lst.size(); i++) {
            lst.set(i, lst.get(i) / sum);
         }
      }
      else {
//         System.out.println("ReInitializing " + q);
         int index = 0;
         Double startVal = Math.pow(2, (1.0 / 25.0 - 2.0)) - 0.25;//HC num
         for (Double curKey = startVal; curKey <= maxBid + 0.001; curKey = (curKey + 0.25) * _aStep - 0.25) {//HC num
            if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
               lst.set(index, InitDistributions.initDistF0[index]);
            } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
               lst.set(index, InitDistributions.initDistF1[index]);
            } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
               lst.set(index, InitDistributions.initDistF2[index]);
            }
            index++;
         }
      }
   }

   private Double averageAL(ArrayList<Double> newPred) {
      double sum = 0.0;
      for (int i = 0; i < newPred.size(); i++) {
         sum += (newPred.get(i) * indexToBidValue(i));
      }
      if(Double.isNaN(sum) || Double.isInfinite(sum) || sum < 0) {
         sum = 0;
      }
      return sum;
   }

   private Double maxAL(ArrayList<Double> newPred) {
      double max = -1;
      ArrayList<Double> maxBids = new ArrayList<Double>();
      for (int i = 0; i < newPred.size(); i++) {
         if(newPred.get(i) > max) {
            max = newPred.get(i);
            maxBids = new ArrayList<Double>();
            maxBids.add(indexToBidValue(i));
         }
         else if(newPred.get(i) == max) {
            maxBids.add(indexToBidValue(i));
         }
      }
      double sum = 0.0;
      for(Double bid : maxBids) {
         sum += bid;
      }
      sum = sum / ((double)maxBids.size());
      return sum;
   }

   private double normalDensFn(double d) {
      return normalDensFn(d, _normVar);
   }

   private double normalDensFn(double d, double var) {
      return Math.exp((-(d * d)) / (2.0 * var)) / (Math.sqrt(2.0 * Math.PI * var));//HC num
   }

   private void pushPredictionsForward() {
      for(Query q: _querySpace){
         for(String s : _advertisers){
            ArrayList<ArrayList<Double>> bidDists = _allBidDists.get(q).get(s);

            //Push Forward Pred 2 Days
            ArrayList<Double> newPred = pushForward(bidDists, q);
            bidDists.add(newPred); //temp add new pred

            ArrayList<Double> newPred2 = pushForward(bidDists, q);
            _allPredDists.get(q).put(s,newPred2);

            bidDists.remove(bidDists.size()-1); //remove the temp added dist
         }
      }
   }

   private ArrayList<Double> pushForward(ArrayList<ArrayList<Double>> bidDists, Query q) {
      ArrayList<Double> tmpDist = new ArrayList<Double>();
      for (int j = 0; j < _numBidValues; j++) {
         double toAdd = 0.0;
         if(q.getType()==QueryType.FOCUS_LEVEL_ZERO){
            toAdd += _randomJumpProb*(InitDistributions.initDistF0[j]);
         }
         else if(q.getType()==QueryType.FOCUS_LEVEL_ONE){
            toAdd +=  _randomJumpProb*(InitDistributions.initDistF1[j]);
         }
         else if(q.getType()==QueryType.FOCUS_LEVEL_TWO){
            toAdd += _randomJumpProb*(InitDistributions.initDistF2[j]);
         }
         for (int k = 0; k < _numBidValues; k++) {
            toAdd += _yesterdayProb * _transProbs[Math.abs(k - j)] * bidDists.get(bidDists.size() - 1).get(k);
         }
         for (int k = 0; k < _numBidValues; k++) {
            if (bidDists.size() > 5) {//HC num
               toAdd += _nDaysAgoProb * _transProbs[Math.abs(k - j)] * bidDists.get(bidDists.size() - 6).get(k);//HC num
            } else {
               toAdd += _nDaysAgoProb * _transProbs[Math.abs(k - j)] * bidDists.get(0).get(k);
            }
         }
         toAdd = Math.max(0, toAdd);
         tmpDist.add(toAdd);
      }
      normalizeAL(tmpDist,q);
      return tmpDist;
   }

   private double indexToBidValue(int index) {
      return Math.pow(2.0, (((double) (index)) + 1.0) / 25.0 - 2.0) - 0.25;//HC num
   }

   @Override
   public double getPrediction(String player, Query q) {
      ArrayList<Double> predDist = _allPredDists.get(q).get(player);
      if(MLE) {
         return maxAL(predDist);
      }
      else {
         return averageAL(predDist);
      }
   }

   public double getCurrentEstimate(String player, Query q) {
      ArrayList<Double> toRet = _allBidEsts.get(q).get(player);
      return toRet.get(toRet.size() - 1);
   }

   @Override
   public boolean updateModel(HashMap<Query, Double> ourCPCs, HashMap<Query, Double> ourBids, HashMap<Query, HashMap<String, Integer>> ourRanks, HashMap<Query, HashMap<String, Boolean>> allRankable) {
      pushForwardCurEst(ourCPCs, ourBids, ourRanks);
      updateProbs(ourBids, ourRanks, allRankable);
      genCurEst();
      pushPredictionsForward();
      return true;
   }

   private void updateProbs(HashMap<Query,Double> ourBids, HashMap<Query, HashMap<String, Integer>> ranks, HashMap<Query, HashMap<String, Boolean>> allRankable) {
      for (Query q : _querySpace) {
         double ourBid = ourBids.get(q);
         HashMap<String, Integer> ranksMap = ranks.get(q);
         HashMap<String, Boolean> rankable = allRankable.get(q);
         HashMap<String, ArrayList<ArrayList<Double>>> bidDistMap = _allBidDists.get(q);
         Set<String> agents = bidDistMap.keySet();
         if(ranksMap != null) {
            for (int n = 0; n < _numIterations; n++) {
               HashMap<String, ArrayList<Double>> ordPrMap = new HashMap<String, ArrayList<Double>>();
               for (String agent : agents) {
                  ArrayList<Double> ordPr = new ArrayList<Double>();
                  for (int i = 0; i < _numBidValues; i++) {
                     ordPr.add(1.0); //HC num
                  }
                  ordPrMap.put(agent, ordPr);
               }
               for (String agent : agents) {
                  if (!agent.equals(_ourAgent) && rankable.get(agent)) {
                     for (String curAdv : agents) {
                        if(rankable.get(curAdv)) {
                           ArrayList<Double> yDist = bidDistMap.get(curAdv).get(bidDistMap.get(curAdv).size() - 1);
                           for (int i = 0; i < _numBidValues; i++) {
                              double toSet = 0.0;
                              if (!curAdv.equals(agent)) {
                                 if(!curAdv.equals(_ourAgent)) {
                                    if (ranksMap.get(agent) > ranksMap.get(curAdv)) {
                                       for (int j = i; j < _numBidValues; j++) {
                                          toSet += yDist.get(j);
                                       }
                                    } else if (ranksMap.get(agent) < ranksMap.get(curAdv)) {
                                       for (int j = i; j >= 0; j--) {
                                          toSet += yDist.get(j);
                                       }
                                    }
                                 } else {//HC num several in else statement
                                    double bid = indexToBidValue(i);
                                    if(ourBid < bid) {//HC num in if block
                                       if(ranksMap.get(agent) > ranksMap.get(curAdv)) {
                                          toSet = 0.0;
                                       }
                                       else if(ranksMap.get(agent) < ranksMap.get(curAdv)) {
                                          toSet = 1.0; 
                                       }
                                    }
                                    else {
                                       if(ranksMap.get(agent) > ranksMap.get(curAdv)) {
                                          toSet = 1.0;
                                       }
                                       else if(ranksMap.get(agent) < ranksMap.get(curAdv)) {
                                          toSet = 0.0;
                                       }
                                    }
                                 }
                              } else {
                                 toSet = 1.0;
                              }

                              toSet = Math.max(0, toSet);

                              ordPrMap.get(agent).set(i, ordPrMap.get(agent).get(i) * toSet);
                           }
                        }
                     }
                     normalizeAL(ordPrMap.get(agent),q);
                  }
               }
               for (String s : agents) {
                  if (!s.equals(_ourAgent)) {
                     ArrayList<ArrayList<Double>> bidDists = bidDistMap.get(s);
                     ArrayList<Double> bidDist = bidDists.get(bidDists.size() - 1);
                     ArrayList<Double> ordPr = ordPrMap.get(s);
                     for (int i = 0; i < _numBidValues; i++) {
                        bidDist.set(i, Math.max(0, bidDist.get(i) * ordPr.get(i)));
                     }
                     normalizeAL(bidDist,q);
                  }
               }
            }
         }
         else {
            //Can only update the probs if we actually had a ranking
         }
      }
   }

   private void pushForwardCurEst(HashMap<Query, Double> ourCPCs, HashMap<Query, Double> ourBids, HashMap<Query, HashMap<String, Integer>> allRanks) {
      for (Query q : _querySpace) {
         HashMap<String, ArrayList<ArrayList<Double>>> bidDistsMap = _allBidDists.get(q);
         Set<String> agents = bidDistsMap.keySet();
         HashMap<String, Integer> ranksMap = allRanks.get(q);
         if(ranksMap != null) {
            int ourRank = ranksMap.get(_ourAgent);
            int rankAfterUs = -1;
            if(ourRank > -1) {
               rankAfterUs = ourRank+1;
            }
            for (String s : agents) {
               ArrayList<ArrayList<Double>> bidDists = bidDistsMap.get(s);
               if (s.equals(_ourAgent)) {
                  ArrayList<Double> currDist = new ArrayList<Double>();
                  for (int i = 0; i < _numBidValues; i++) {
                     currDist.add(0.0);
                  }
                  double myBid = ourBids.get(q);
                  double theInd = ((((Math.log(myBid + 0.25) / Math.log(2.0)) + 2) * 25.0) - 1.0); //HC num
                  boolean onEdge = false;
                  int theIndex = (int) (theInd);
                  double firstProp = theInd - (double) theIndex;
                  if (theIndex <= 0) {
                     theIndex = 0;
                  }
                  if (theIndex >= _numBidValues - 1) {
                     theIndex = _numBidValues - 1;
                     onEdge = true;
                  }
                  if (!onEdge) {
                     currDist.set(theIndex, 1.0 - firstProp);
                     currDist.set(theIndex + 1, firstProp);
                  } else {
                     currDist.set(theIndex, 1.0);
                  }
                  bidDists.add(currDist);
               }
//               else if (!Double.isNaN(ourCPCs.get(q)) && (rankAfterUs == (ranksMap.get(s)))) {
//                  ArrayList<Double> currDist = new ArrayList<Double>();
//                  for (int i = 0; i < _numBidValues; i++) {
//                     currDist.add(0.0);
//                  }
//                  double ourCPC = ourCPCs.get(q);
//                  double theInd = ((((Math.log(ourCPC + 0.25) / Math.log(2.0)) + 2) * 25.0) - 1.0);
//                  boolean onEdge = false;
//                  int theIndex = (int) (theInd); 
//                  double firstProp = theInd - (double) theIndex;
//                  if (theIndex <= 0) {
//                     theIndex = 0;
//                  }
//                  if (theIndex >= _numBidValues - 1) {
//                     theIndex = _numBidValues - 1;
//                     onEdge = true;
//                  }
//                  if (!onEdge) {
//                     currDist.set(theIndex, 1.0 - firstProp);
//                     currDist.set(theIndex + 1, firstProp);
//                  } else {
//                     currDist.set(theIndex, 1.0);
//                  }
//
//                  normalizeAL(currDist,q);
//                  bidDists.add(currDist);
//               }
               else {
                  ArrayList<Double> currDist = pushForward(bidDists, q);
                  normalizeAL(currDist,q);
                  bidDists.add(currDist);
               }
            }
         }
         else {
            for (String s : agents) {
               ArrayList<ArrayList<Double>> bidDists = bidDistsMap.get(s);
               ArrayList<Double> currDist = pushForward(bidDists, q);
               normalizeAL(currDist,q);
               bidDists.add(currDist);
            }
         }
      }
   }

   @Override
   public AbstractModel getCopy() {
      return new IndependentBidModel(_advertisers, _ourAgent, _numIterations, _randomJumpProb, _yesterdayProb, _nDaysAgoProb, _normVar,_querySpace);
   }

   @Override
   public String toString() {
      return "IndependentBidModel(" + _numIterations + ", " + _randomJumpProb + ", " + _yesterdayProb + ", " + _nDaysAgoProb + ", " + _normVar + ")";
   }

   @Override
   public void setAdvertiser(String ourAdvertiser) {
      _ourAgent = ourAdvertiser;
   }

}
