package models.queryanalyzer.riep.iep.cp;

import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;

import org.apache.commons.math.fraction.Fraction;
import org.apache.commons.math.fraction.FractionConversionException;

import java.util.*;

public class ImpressionEstimatorExact implements AbstractImpressionEstimator {

   private boolean IE_DEBUG = false;
   QAInstance _instance;

   private boolean BRANCH_AROUND_PRIOR = false;
   private boolean SEARCH_ON_PRIOR = true;
   private boolean OPP_PRIOR_TIE_BREAK = true;
   private boolean GAUSSIAN_PR_SAMP = true;
   private double AVG_POS_STD_DEV;
   private double AVG_POS_POWER;
   private double OUR_AVG_POS_STD_DEV;
   private double OUR_AVG_POS_POWER;
   private double IMP_PRIOR_STD_DEV;
   private double IMP_PRIOR_POWER;
   private double OUR_IMP_PRIOR_STD_DEV;
   private double OUR_IMP_PRIOR_POWER;
   private boolean LOG_GAUSSIAN_PDF = true;
   private boolean DPARDOE_MODE = false;
   private boolean FULL_SELF_POS;
   private double FILL_SLOTS_AVG; //this contains the value of the average position of the person last added using fillSlots
   private int MIN_TOT_IMPR = 25;

   private int SAMPLING_FACTOR;
   private static int MAX_PROBE_IMPRESSIONS = 1; //warning this must be greater than 0
   private int _samplingImpressions;

   private int _nodeId;
   private Map<Integer, IESolution> _solutions;
   private int _bestNode;
   private double _ourAvgPosDiff;
   private double _bestTieBreak;
   private double _combinedObjectiveBound;

   private int _advertisers;
   private int _slots;
   private double[] _trueAvgPos;
   private int _ourIndex;
   private int _ourImpressions;
   private int _imprUB;
   private int[] _agentImprUB;
   private int[] _agentImprLB;

   private double[] _agentImpressionDistributionMean;
   private double[] _agentImpressionDistributionStdev;
   private int[] _agentIds;

   private double _startTime;
   private double _timeOut = 5; //in seconds

   public ImpressionEstimatorExact(QAInstance inst) {
      this(inst, 200, 0);
   }

   public ImpressionEstimatorExact(QAInstance inst, int samplingFactor, int fractionalBranches) {
      this(inst, samplingFactor, fractionalBranches, 0.082111,2.51308611,2.419581,0.8278350);
   }

   public ImpressionEstimatorExact(QAInstance inst, int samplingFactor, int fractionalBranches, double avgposstddev, double ouravgposstddev, double imppriorstddev, double ourimppriorstddev) {
      this(inst, samplingFactor, fractionalBranches, avgposstddev, ouravgposstddev, imppriorstddev, ourimppriorstddev,1,1,1,1);
   }

   public ImpressionEstimatorExact(QAInstance inst, int samplingFactor, int fractionalBranches, double avgposstddev, double ouravgposstddev, double imppriorstddev, double ourimppriorstddev,
                                   double avgpospower, double ouravgpospower, double imppriorpower, double ourimppriorpower) {
      _instance = inst;
      SAMPLING_FACTOR = samplingFactor;
      AVG_POS_STD_DEV = avgposstddev;
      OUR_AVG_POS_STD_DEV = ouravgposstddev;
      IMP_PRIOR_STD_DEV = imppriorstddev;
      OUR_IMP_PRIOR_STD_DEV = ourimppriorstddev;
      AVG_POS_POWER = avgpospower;
      OUR_AVG_POS_POWER = ouravgpospower;
      IMP_PRIOR_POWER = imppriorpower;
      OUR_IMP_PRIOR_POWER = ourimppriorpower;
      _advertisers = inst.getNumAdvetisers();
      _slots = inst.getNumSlots();
      _ourIndex = inst.getAgentIndex();

      //_trueAvgPos should contain exact average positions whenever possible.
      //If there is no exact average position, use sampled average position.
      _trueAvgPos = inst.getAvgPos().clone();
      for (int i = 0; i < _trueAvgPos.length; i++) {
         if (i == _ourIndex) {
            FULL_SELF_POS = !(_trueAvgPos[i] == -1);
         }
         if (_trueAvgPos[i] == -1) {
            _trueAvgPos[i] = inst.getSampledAvgPos()[i];
         }
      }

      _ourImpressions = inst.getImpressions();
      _imprUB = inst.getImpressionsUB();

      _agentImpressionDistributionMean = inst.getAgentImpressionDistributionMean().clone();
      _agentImpressionDistributionStdev = inst.getAgentImpressionDistributionStdev().clone();

      for(int i = 0; i < _agentImpressionDistributionMean.length; i++) {
         if(i == _ourIndex) {
            _agentImpressionDistributionMean[i] = _ourImpressions;
            _agentImpressionDistributionStdev[i] = _ourImpressions*OUR_IMP_PRIOR_STD_DEV;
         }
         else {
            _agentImpressionDistributionStdev[i] = _agentImpressionDistributionMean[i]*IMP_PRIOR_STD_DEV;
         }
      }

      _agentIds = inst.getAgentIds();

      //TODO: IT IS POSSIBLE TO HAVE A WHOLE # AVG POSITION, BUT HAVE BEEN IN MORE THAN ONE SLOT...
      int wholeAvgPos = 0;
      for (int i = 0; i < _advertisers; i++) {
         if (Double.isNaN(_trueAvgPos[i]) || (((_trueAvgPos[i] * 100000) % 100000) == 0)) {
            wholeAvgPos += 1;
         }
      }

      _samplingImpressions = Math.max(MAX_PROBE_IMPRESSIONS, (int)(_imprUB / ((double)SAMPLING_FACTOR) * wholeAvgPos * wholeAvgPos * ((wholeAvgPos >= 2) ? .5 : 1.0))); //value must be at least 2, becouse it must be greater than MAX_PROBE_IMPRESSIONS

      assert _ourImpressions > 0;

      _agentImprLB = new int[_advertisers];
      _agentImprUB = new int[_advertisers];

      for (int i = 0; i < _advertisers; i++) {
         _agentImprLB[i] = 1;
         _agentImprUB[i] = inst.getImpressionsUB();
      }

      _agentImprLB[_ourIndex] = _ourImpressions - _ourImpressions / 10;
      _agentImprUB[_ourIndex] = _ourImpressions + _ourImpressions / 10;

      if (IE_DEBUG) {
         System.out.println("IEDebug: avgPos=" + Arrays.toString(inst.getAvgPos()) + ", sampledAvgPos=" + Arrays.toString(inst.getSampledAvgPos()) + ", _trueAvgPos=" + Arrays.toString(_trueAvgPos));
      }

   }


   private String allDataString() {
      StringBuffer sb = new StringBuffer();
      sb.append("sampImp=" + _samplingImpressions + "\n");
      sb.append("sol=" + _solutions + "\n");
      sb.append("advertisers=" + _advertisers + "\n");
      sb.append("slots=" + _slots + "\n");
      sb.append("trueavgPos=" + Arrays.toString(_trueAvgPos) + "\n");
      sb.append("ourIdx=" + _ourIndex + "\n");
      sb.append("ourImps=" + _ourImpressions + "\n");
      sb.append("impsUB=" + _imprUB + "\n");
      sb.append("agentImprUB=" + Arrays.toString(_agentImprUB) + "\n");
      sb.append("agentImprLB=" + Arrays.toString(_agentImprLB) + "\n");
      sb.append("agentImprDistMean=" + Arrays.toString(_agentImpressionDistributionMean) + "\n");
      sb.append("agentImprDistStdev=" + Arrays.toString(_agentImpressionDistributionStdev) + "\n");
      sb.append("agentIds=" + Arrays.toString(_agentIds) + "\n");
      return sb.toString();
   }

   /**
    * This gets an IEResult that corresponds to the original QAInstance, before any
    * unsampled agents were removed or any padded agents were added.
    * Any unsampled agent is given a NaN prediction.
    *
    * @param result
    * @return
    */
   private IEResult reduceIEResult(IEResult result) {
      //FIXME: Add something here
      int[] originalIds = _instance.getAgentIds();
      _instance.getAgentNames();

      int[] paddedImpressions = result.getSol();
      int[] paddedOrder = result.getOrder();
      int[] paddedSlotImpressions = result.getSlotImpressions();
      String[] paddedAgentNames = result.getAgentNames();
      
      int[] reducedImpressions = new int[originalIds.length];
      String[] reducedAgentNames = new String[originalIds.length];
      Arrays.fill(reducedImpressions, -1);
      for (int i = 0; i < originalIds.length; i++) {
         int originalId = originalIds[i];
         for (int j = 0; j < _agentIds.length; j++) {
            if (originalId == _agentIds[j]) {
               reducedImpressions[i] = paddedImpressions[j];
               reducedAgentNames[i] = paddedAgentNames[j];
               //TODO: What should we do with reduced order? [2, 0, 1].
            }
         }
      }

      //order [A, B, C] says that agent A was in the first slot, B was in the 2nd, etc.
      //If a padded agent was in some slot, change that value to -1.
      //In other words, we'll still acknowledge that there was SOME agent in that slot, but we don't know who.
      int[] reducedOrder = paddedOrder.clone();
      Arrays.fill(reducedOrder, -1);
      for (int i = 0; i < paddedOrder.length; i++) {
         int paddedAgentIdx = paddedOrder[i];
         int paddedAgentId = _agentIds[paddedAgentIdx];

         //Find this agentID in the original instance
         for (int j = 0; j < originalIds.length; j++) {
            if (paddedAgentId == originalIds[j]) {
               int originalIdx = j;
               reducedOrder[i] = originalIdx;
            }
         }
      }

      //FIXME: Which names to use?
      return new IEResult(result.getObj(), reducedImpressions, reducedOrder, paddedSlotImpressions,result.getWaterfall(), reducedAgentNames);
//      return new IEResult(result.getObj(), reducedImpressions, reducedOrder, paddedSlotImpressions,result.getWaterfall(), originalNames);
   }


   public double[] getApproximateAveragePositions() {
      return _trueAvgPos.clone();
   }


   public QAInstance getInstance() {
      return _instance;
   }

   public ObjectiveGoal getObjectiveGoal() {
      //At least with the old LDS, lower objective values are better. Change this if necessary.
      return ObjectiveGoal.MINIMIZE;
   }

   private boolean feasibleOrder(int[] order) {
      for (int i = 0; i < order.length; i++) {
         int startPos = Math.min(i + 1, _slots);
         if (startPos < _trueAvgPos[order[i]]) {
            return false;
         }
      }
      return true;
   }

   public IEResult search(int[] order) {
      //System.out.println("Search on order=" + Arrays.toString(order));
      //System.out.println(allDataString());

      if (!feasibleOrder(order)) {
         System.out.println("Not a feasible ordering: " + Arrays.toString(order));
         System.out.print("_trueAvgPos (sorted by order): ");
         for (int i = 0; i < order.length; i++) {
            System.out.print(_trueAvgPos[order[i]] + " ");
         }
         System.out.println();

         assert (false) : "should have been eliminated in LDS search";
         return null;
      }

      if(order.length == 0) {
         int j =0;
      }
      else if(order.length == 1) {
         int j =0;
      }
      else if(order.length == 2) {
         int j =0;
      }

      _nodeId = 0;
      _solutions = new HashMap<Integer, IESolution>();
      _combinedObjectiveBound = Double.MAX_VALUE;
      _ourAvgPosDiff = Double.MAX_VALUE;
      _bestTieBreak = 0;

      int[] agentImpr = new int[_advertisers];
      for (int i = 0; i < _advertisers; i++) {
         agentImpr[i] = 0;
      }
      int[] slotImpr = new int[_slots];
      for (int i = 0; i < _slots; i++) {
         slotImpr[i] = 0;
      }

      _startTime = System.currentTimeMillis();
      checkImpressions(0, agentImpr, slotImpr, order);

      IESolution sol = _solutions.get(_bestNode);
      if (sol == null) {
         if (IE_DEBUG) {
            System.out.println("IE Solution is null");
         }
         //System.exit(-1);
         return null;
      } else {
    	  String[] agentNames = _instance.getAgentNames(); //FIXME: This will only work if there were no padded agents (there shouldn't be any in the exact problem).
         IEResult result = new IEResult(_combinedObjectiveBound, sol._agentImpr, order.clone(), sol._slotImpr, sol._waterfall, agentNames);
         IEResult reducedResult = reduceIEResult(result);
         if (IE_DEBUG) {
            System.out.println("Search completed for order " + Arrays.toString(order) + "\n" + result + "\nReduced " + reducedResult);
         }
         return reducedResult;
      }
   }

   void checkImpressions(int currIndex, int[] agentImpr, int[] slotImpr, int[] order) {
      _nodeId++;

//      System.out.println("nodeId=" + _nodeId + ", idx=" + currIndex + ", imps=" + Arrays.toString(agentImpr));
      //+ ", slotImps=" + Arrays.toString(slotImpr) );

//      double stop = System.currentTimeMillis();
//      double elapsed = (stop - _startTime) / 1000.0;
//      if (elapsed > _timeOut) {
//         return;
//      }

      if (slotImpr[0] > _imprUB) {
         return; //this is infeasible
      }

      if (currIndex >= _advertisers) {
         // We've come up with feasible impression estimates for all advertisers.
         // Compute objective value for this solution.

         //Prune if we have a waterfall with fewer than minimum impressions
         if (slotImpr[0] < MIN_TOT_IMPR) {
            return;
         }

//         if (imprObjVal > _bestImprObjVal) {
//            return;
//         }

         double probWaterfall;
         if(OPP_PRIOR_TIE_BREAK) {
            probWaterfall =  getImpressionModelObjective(agentImpr, order,true,false);
         }
         else {
            probWaterfall =  getImpressionModelObjective(agentImpr, order,true,true);
         }

         double sampleProb;
         double totImpsForNonSampled = 0;
         double ourAvgPosDiff = Double.MAX_VALUE;
         int[][] impsPerSlot = greedyAssign(_slots, agentImpr.length, order, agentImpr);
         if (GAUSSIAN_PR_SAMP) {
            double[] avgPosPred = new double[_trueAvgPos.length];
            for (int i = 0; i < order.length; i++) {
               int currAgent = order[i];
               double trueAvgPos = _trueAvgPos[currAgent];
               if (Double.isNaN(trueAvgPos)) {
                  totImpsForNonSampled += agentImpr[currAgent];
                  continue; //cannot evaluate those who weren't sampled
               }
               double weightedImps = 0.0;
               double imps = 0.0;
               for (int j = 0; j < _slots; j++) {
                  weightedImps += (j + 1) * impsPerSlot[currAgent][j];
                  imps += impsPerSlot[currAgent][j];
               }
               double waterfallAvgPos = weightedImps / imps;
               double agentDiff = Math.abs(waterfallAvgPos - trueAvgPos);

               if ((currAgent == _ourIndex) && FULL_SELF_POS) {
                  ourAvgPosDiff = agentDiff;
               }

               avgPosPred[currAgent] = waterfallAvgPos;

            }
            sampleProb = getAvgPosObjective(_trueAvgPos,avgPosPred);
         }
         else {
            double avgPosDiff = 0.0;
            for (int i = 0; i < order.length; i++) {
               int currAgent = order[i];
               double trueAvgPos = _trueAvgPos[currAgent];
               double weightedImps = 0.0;
               double imps = 0.0;
               for (int j = 0; j < _slots; j++) {
                  weightedImps += (j + 1) * impsPerSlot[currAgent][j];
                  imps += impsPerSlot[currAgent][j];
               }
               double waterfallAvgPos = weightedImps / imps;
               double agentDiff = Math.abs(waterfallAvgPos - trueAvgPos);

               if ((currAgent == _ourIndex) && FULL_SELF_POS) {
                  ourAvgPosDiff = agentDiff;
               }
               avgPosDiff += agentDiff;
            }
            sampleProb = avgPosDiff + 1;
         }


         double combinedObj = -1 * probWaterfall * sampleProb;

         //System.out.println("combinedObj=" + combinedObj + ", imprObjVal=" + imprObjVal + ", probWaterfall=" + probWaterfall + ", sampleProb=" + sampleProb);

         double tiebreak = 0.0;
         if(OPP_PRIOR_TIE_BREAK) {
            tiebreak =  getImpressionModelObjective(agentImpr, order,false,true);
         }

         if (combinedObj < _combinedObjectiveBound || ((combinedObj == _combinedObjectiveBound) && (OPP_PRIOR_TIE_BREAK && tiebreak > _bestTieBreak))) {
            _combinedObjectiveBound = combinedObj;
            _ourAvgPosDiff = ourAvgPosDiff;
            _bestTieBreak = tiebreak;
            _solutions.put(_nodeId, new IESolution(agentImpr, slotImpr,impsPerSlot));
            _bestNode = _nodeId;
         }

         //System.out.println("combinedObjBound=" + _combinedObjectiveBound + ", bestImprObj=" + _bestImprObjVal + ", bestNode=" + _bestNode);
         return;
      }

      int currAgent = order[currIndex];

      int bestImpr = calcMinImpressions(slotImpr, currIndex, _trueAvgPos[currAgent]);

      if (bestImpr > 0) {

         // Some #imps for this agent was successfully found.
         // If "this agent" is us, calculate how far the computed #imps is to our actual #imps.
         //   If prediction is not close enough, return (and possibly search with some new #imps estimate for opponents)
         // Go on to the next agent (assuming the #imps just found for this agent is true)

         LinkedList<Integer> branches = new LinkedList<Integer>();
         branches.add(bestImpr);

         int maxImpr = _agentImprUB[currAgent];
         if (DPARDOE_MODE) {
            int rank = currIndex + 1;
            int numSlots = slotImpr.length;
            double tmpAvgPos = _trueAvgPos[currAgent];
            long intPartAvgPos = (long) tmpAvgPos;
            double fracPartAvgPos = tmpAvgPos - intPartAvgPos;
            if (fracPartAvgPos != 0) {
               Fraction frac;
               try {
                  frac = new Fraction(tmpAvgPos, 0, 20);
               } catch (FractionConversionException e) {
                  e.printStackTrace();
                  throw new RuntimeException("Could not convert fraction");
               }
               int numerator = frac.getNumerator();
               int denominator = frac.getDenominator();

               int[] dropoutPoints = getDropoutPoints(agentImpr, order, numSlots);

               int starta;
               if (rank <= numSlots) {
                  starta = 0;
               } else {
                  starta = dropoutPoints[rank - numSlots - 1];
               }

               int sum = 0;
               for (int limitOrder = 2; limitOrder <= agentImpr.length; limitOrder++) {
                  if ((rank - (limitOrder - 1)) >= numSlots) {
                     //not in auction yet
                     continue;
                  }

                  if (limitOrder - 2 < dropoutPoints.length) {
                     int endb = dropoutPoints[limitOrder - 2];
                     sum += (endb - starta);
                  }

                  double gcd = sum / (numerator - denominator * (rank - limitOrder + 1));

                  if (gcd < 1) {
                     continue;
                  }

                  int newBranch = (int) (gcd * denominator);

                  if(newBranch <= maxImpr) {
                     branches.add(newBranch);
                  }
               }
            }
         } else {

//            Integer impPred = (int) _agentImpressionDistributionMean[currAgent];
//            if (SEARCH_ON_PRIOR && impPred >= 0) {
//               if (!branches.contains(impPred)) {
//                  branches.addFirst(impPred);
//               }
//            }
         }

         if (currAgent == _ourIndex) {
            if (!branches.contains(_ourImpressions)) {
               branches.addFirst(_ourImpressions);
            }
         }

         for (Integer branch : branches) {
            int[] agentImprCopy = agentImpr.clone();
            agentImprCopy[currAgent] = branch;
            int[] newSlotImpr = fillSlots(slotImpr, branch);

            if (currAgent == _ourIndex) {
               if (FULL_SELF_POS) {
                  if (Math.abs(_trueAvgPos[currAgent] - FILL_SLOTS_AVG) > _ourAvgPosDiff) {
                     return;
                  }
               }
            }

            checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order);
         }
      } else {

         if (bestImpr == -3) {
            return;
         }

         // BestImpr <= 0, i.e. avgPos is an integer for this agent (and thus #imps is not well specified).
         // Iterate through different possible #imps.
         // Iterate increasing/decreasing depending on whether or not this agent is in the 1st position.

         int firstSlot = Math.min(currIndex + 1, _slots);
         int maxImpr;
         if (Double.isNaN(_trueAvgPos[currAgent])) {
//            maxImpr = (firstSlot == 1) ? _agentImprUB[currAgent] : Math.min(_agentImprUB[currAgent],slotImpr[firstSlot - 2]);
            maxImpr = (firstSlot == 1) ? _agentImprUB[currAgent] : Math.min(_agentImprUB[currAgent], slotImpr[0]);
         } else {
            maxImpr = (firstSlot == 1) ? _agentImprUB[currAgent] : Math.min(_agentImprUB[currAgent], slotImpr[firstSlot - 2]);
         }

         LinkedList<Integer> branches = new LinkedList<Integer>();
         if (firstSlot == 1) {
            for (int i = _agentImprLB[currAgent]; i <= maxImpr; i++) {
               if (i % _samplingImpressions == 0 || i == maxImpr || i <= _agentImprLB[currAgent] + MAX_PROBE_IMPRESSIONS) {
                  branches.add(i);
               }
            }
         } else {
            for (int i = maxImpr; i >= _agentImprLB[currAgent]; i--) {
               if (i % _samplingImpressions == 0 || i == maxImpr || i <= _agentImprLB[currAgent] + MAX_PROBE_IMPRESSIONS) {
                  branches.add(i);
               }
            }
         }

         Integer impPred = (int) _agentImpressionDistributionMean[currAgent];
         if (impPred >= 0) {
            if (BRANCH_AROUND_PRIOR) {
               ArrayList<Integer> downBranches = new ArrayList<Integer>();
               ArrayList<Integer> upBranches = new ArrayList<Integer>();
               for (Integer branch : branches) {
                  if (branch < impPred) {
                     downBranches.add(branch);
                  } else if (branch > impPred) {
                     upBranches.add(branch);
                  }
               }
               branches.clear();
               if(SEARCH_ON_PRIOR) {
                  branches.add(impPred);
               }
               for (int i = 0; i < Math.max(upBranches.size(), downBranches.size()); i++) {
                  if (i < upBranches.size()) {
                     branches.add(upBranches.get(i));
                  }
                  if (i < downBranches.size()) {
                     branches.add(downBranches.get(i));
                  }
               }
            } else {
               if(SEARCH_ON_PRIOR) {
                  if (branches.contains(impPred)) {
                     branches.remove(impPred);
                  }
                  branches.addFirst(impPred);
               }
            }
         }


         if (currAgent == _ourIndex) {
            if (!branches.contains(_ourImpressions)) {
               branches.addFirst(_ourImpressions);
            }
         }

         for (Integer branch : branches) {
            int[] agentImprCopy = agentImpr.clone();
            agentImprCopy[currAgent] = branch;
            int[] newSlotImpr = fillSlots(slotImpr, branch);

            if (currAgent == _ourIndex) {
               if (FULL_SELF_POS) {
                  if (Math.abs(_trueAvgPos[currAgent] - FILL_SLOTS_AVG) > _ourAvgPosDiff) {
                     return;
                  }
               }
            }

            checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order);
         }
      }
   }

   private double getAvgPosObjective(double[] avgPos, double[] predAvgPos) {
      double obj;
      if (LOG_GAUSSIAN_PDF) {
         obj = 0.0;
      } else {
         obj = 1.0;
      }

      for (int i = 0; i < avgPos.length; i++) {
         double mean = avgPos[i];
         double stdDev;
         if(i == _ourIndex) {
            stdDev = mean * OUR_AVG_POS_STD_DEV;
         }
         else {
            stdDev = mean * AVG_POS_STD_DEV;
         }
         if (mean != -1 && stdDev != -1) {
            double avgpos = predAvgPos[i];
            if (LOG_GAUSSIAN_PDF) {
               double prob;
               if(i == _ourIndex) {
                  prob = OUR_AVG_POS_POWER*Math.log(gaussianPDF(avgpos, mean, stdDev));

               }
               else {
                  prob = AVG_POS_POWER*Math.log(gaussianPDF(avgpos, mean, stdDev));
               }
               obj += prob;
            } else {
               double prob;
               if(i == _ourIndex) {
                  prob = Math.pow(gaussianPDF(avgpos, mean, stdDev),OUR_AVG_POS_POWER);

               }
               else {
                  prob = Math.pow(gaussianPDF(avgpos, mean, stdDev),AVG_POS_POWER);
               }
               obj *= prob;
            }
            //System.out.println("currAgent=" + currAgent + ", gaussianPDF(imps=" + imps +", mean="+ mean + ", stdDev=" + stdDev + ") = " + prob);
         }
      }

//      System.out.println(obj);

      if (LOG_GAUSSIAN_PDF) {
         if(obj == 0) {
            return 0;
         }
         else {
            return Math.exp(obj);
         }
      }
      else {
         if (obj == 1.0) {
            //This means we didn't place anyone with a prediction yet
            //so return a bad objective
            return 0.0;
         } else {
            return obj;
         }
      }
   }

   private double getImpressionModelObjective(int[] agentImpr, int[] order, boolean ourPrior, boolean othersPrior) {
      double obj;
      if (LOG_GAUSSIAN_PDF) {
         obj = 0.0;
      } else {
         obj = 1.0;
      }
      int numSlotsFilled = agentImpr.length;
      for (int i = 0; i < numSlotsFilled; i++) {
         int currAgent = order[i];
         double mean = _agentImpressionDistributionMean[currAgent];
         double stdDev = _agentImpressionDistributionStdev[currAgent];
         if (mean != -1 && stdDev != -1) {
            double imps = agentImpr[currAgent];
            if (LOG_GAUSSIAN_PDF) {
               double prob;
               if(currAgent == _ourIndex) {
                  if(ourPrior) {
                     prob = gaussianPDF(imps, mean, stdDev);
                     if(prob > 0) {
                        prob = OUR_IMP_PRIOR_POWER*Math.log(prob);
                     }
                  }
                  else {
                     prob = 0.0;
                  }
               }
               else {
                  if(othersPrior) {
                     double avgPos = _trueAvgPos[currAgent];
                     if (!Double.isNaN(avgPos) && (((avgPos * 100000) % 100000) == 0)) {
                        prob = gaussianPDF(imps, mean, stdDev);
                        if(prob > 0) {
                           prob = IMP_PRIOR_POWER*Math.log(prob);
                        }
                     }
                     else {
                        prob = 0;
                     }
                  }
                  else {
                     prob = 0.0;
                  }
               }
               obj += prob;
            } else {
               double prob;
               if(currAgent == _ourIndex) {
                  prob = Math.pow(gaussianPDF(imps, mean, stdDev),OUR_IMP_PRIOR_POWER);
               }
               else {
                  prob = Math.pow(gaussianPDF(imps, mean, stdDev),IMP_PRIOR_POWER);
               }
               obj *= prob;
            }
            //System.out.println("currAgent=" + currAgent + ", gaussianPDF(imps=" + imps +", mean="+ mean + ", stdDev=" + stdDev + ") = " + prob);
         }
      }

//      System.out.println(obj);
      if (LOG_GAUSSIAN_PDF) {
         if(obj == 0) {
            return 0;
         }
         else {
            return Math.exp(obj);
         }
      }
      else {
         if (obj == 1.0) {
            //This means we didn't place anyone with a prediction yet
            //so return a bad objective
            return 0.0;
         } else {
            return obj;
         }
      }
   }


   private static double gaussianPDF(double x, double mean, double sigma) {
      if(sigma == 0) {
         return 0;
      }
      double diff = x - mean;
      double sigma2 = sigma * sigma;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
   }

   private int calcMinImpressions(int[] slotImpr, int currIndex, double trueAvgPos) {
      if (Double.isNaN(trueAvgPos)) {
         return -1;
      }

      int firstSlot = currIndex + 1;
      if (currIndex + 1 > _slots) {
         if (trueAvgPos == _slots) {
            firstSlot = _slots;
         } else {
            //Calculate the number of people who simultaneously dropped if any
            HashSet<Integer> duplicateMap = new HashSet<Integer>();
            for (int i = 0; i < slotImpr.length - 1; i++) {
               for (int j = i + 1; j < slotImpr.length; j++) {
                  if (slotImpr[i] == slotImpr[j]) {
                     duplicateMap.add(slotImpr[i]);
                  }
               }
            }

            //Duplicates of the # of imps seen in slot 1 do not indicate simultaneous dropouts
            if (duplicateMap.contains(slotImpr[0])) {
               duplicateMap.remove(slotImpr[0]);
            }

            //Make sure the dupes have happened
            ArrayList<Integer> toRemoveList = new ArrayList<Integer>();
            for (Integer dupe : duplicateMap) {
               if (dupe > slotImpr[slotImpr.length - (currIndex + 1 - _slots)]) {
                  toRemoveList.add(dupe);
               }
            }

            for (Integer toRemove : toRemoveList) {
               duplicateMap.remove(toRemove);
            }

            if (duplicateMap.size() > 0) {
               int dupeCount = 0;
               for (Integer dupe : duplicateMap) {
                  for (int i = 0; i < slotImpr.length; i++) {
                     if (slotImpr[i] == dupe) {
                        dupeCount++;
                     }
                  }
               }

               firstSlot = _slots - dupeCount + 1;
            } else {
               firstSlot = _slots;
            }
         }
      }

      double approxAvgPos = firstSlot;
      if (approxAvgPos == trueAvgPos) {
         return -1;
      }

      if (trueAvgPos > firstSlot) {
         return -3;
      }

      int fullSlots = firstSlot;
      for (int i = firstSlot; i > 1; i--) {
         int impTmp = 0;
         int wImpTmp = 0;
         for (int j = firstSlot; j >= i; j--) {
            impTmp += slotImpr[j - 2] - slotImpr[j - 1];
            wImpTmp += j * (slotImpr[j - 2] - slotImpr[j - 1]);
         }
         approxAvgPos = wImpTmp / (double) impTmp;

         if (approxAvgPos < trueAvgPos) {
            break;
         }

         fullSlots = i;
      }
      int finalSlot = fullSlots - 1;
      int fullSlotImp = 0;
      int wImp = 0;

      for (int i = fullSlots; i <= firstSlot; i++) {
         fullSlotImp += slotImpr[i - 2] - slotImpr[i - 1];
         wImp += i * (slotImpr[i - 2] - slotImpr[i - 1]);
      }

      if (fullSlotImp <= 0) {
         //TODO this is bad and it happens... so this is an ugly hack to fix it,
         fullSlotImp = 1;
         wImp = firstSlot;
//         throw new RuntimeException();
      }

      if (trueAvgPos <= finalSlot) {
         return -2;
      }

      double finalSlotImpFloat = (wImp - fullSlotImp * trueAvgPos) / (trueAvgPos - finalSlot);
      int finalSlotImp = (int) (finalSlotImpFloat + .5);

      return fullSlotImp + finalSlotImp;
   }

   private int[] fillSlots(int[] slotImpr, int newImpr) {
      int[] newSlotImpr = slotImpr.clone();
      FILL_SLOTS_AVG = 0;

      int firstSlot = 0;
      while (firstSlot < slotImpr.length - 1) {
         if (slotImpr[firstSlot] > 0) {
            firstSlot++;
         } else {
            break;
         }
      }

      int remainingImp = newImpr;
      for (int s = firstSlot; s >= 0; s--) {
         if (s == 0) {
            newSlotImpr[s] += remainingImp;
            FILL_SLOTS_AVG += remainingImp * (s + 1);
         } else {
            int r = slotImpr[s - 1] - slotImpr[s];
            assert (r >= 0);
            if (r < remainingImp) {
               remainingImp -= r;
               newSlotImpr[s] += r;
               FILL_SLOTS_AVG += r * (s + 1);
            } else {
               newSlotImpr[s] += remainingImp;
               FILL_SLOTS_AVG += remainingImp * (s + 1);
               break;
            }
         }
      }

      FILL_SLOTS_AVG /= newImpr;

      return newSlotImpr;
   }


   public int[] getDropoutPoints(int[] impsPerAgent, int[] order, int numSlots) {
      ArrayList<Integer> impsBeforeDropout = new ArrayList<Integer>();
      PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
      int nonZeroImps = 0;
      for (int i = 0; i < numSlots && i < impsPerAgent.length; i++) {
         if (impsPerAgent[order[i]] > 0) {
            queue.add(impsPerAgent[order[i]]);
            nonZeroImps++;
         }
      }

      int lastIdx = queue.size();

      while (!queue.isEmpty()) {
         int val = queue.poll();
         impsBeforeDropout.add(val);

         if (lastIdx < nonZeroImps) {
            queue.add(impsPerAgent[order[lastIdx]] + val);
            lastIdx++;
         }
      }

      return convertListToArr(impsBeforeDropout);
   }

   public static int[] convertListToArr(List<Integer> integers) {
      int[] ret = new int[integers.size()];
      for (int i = 0; i < ret.length; i++) {
         ret[i] = integers.get(i);
      }
      return ret;
   }

   /*
  Function input
  number of slots: int slots

  number of agents: int agents

  order of agents: int[]
  example: order = {1, 6, 0, 4, 3, 5, 2} means agent 1 was 1st, agent 6 2nd, 0 3rd, 4 4th, 3 5th, 5 6th, 2 7th
  NOTE: these agents are zero numbered 0 is first... other note agents that are not in the "auction" are
  ommitted so there might be less than 8 agents but that means the numbering must go up to the last agents
  number -1 so if there are 6 agents in the auction the ordering numbers are 0...5

  impressions: int[] impressions
  example: impressions  = {294,22, 8, 294,294,272,286} agent 0 (not the highest slot) has 294 impressions agent 1 22... agent 6 286 impressions
  NOTE: same as order of agents they only reflect the agents in the auction

  Function output
  This is a matrix where one direction is for each agent and the other direction is for the slot.
  The matrix represents is the number of impressions observed at that slot for each of the agents.
  *
  * -gnthomps
   */
   public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
      int[][] impressionsBySlot = new int[agents][slots];

      int[] slotStart = new int[slots];
      int a;

      for (int i = 0; i < agents; ++i) {
         a = order[i];
         //System.out.println(a);
         int remainingImp = impressions[a];
         //System.out.println("remaining impressions "+ impressions[a]);
         for (int s = Math.min(i + 1, slots) - 1; s >= 0; --s) {
            if (s == 0) {
               impressionsBySlot[a][0] = remainingImp;
               slotStart[0] += remainingImp;
            } else {

               int r = slotStart[s - 1] - slotStart[s];
               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
               assert (r >= 0);
               if (r < remainingImp) {
                  remainingImp -= r;
                  impressionsBySlot[a][s] = r;
                  slotStart[s] += r;
               } else {
                  impressionsBySlot[a][s] = remainingImp;
                  slotStart[s] += remainingImp;
                  break;
               }
            }
         }
      }
      return impressionsBySlot;
   }

   public String getName() {
      return "CJC";
   }

   private class IESolution {
      public int[] _agentImpr;
      public int[] _slotImpr;
      public int[][] _waterfall;

      public IESolution(int[] agentImpr, int[] slotImpr, int[][] waterfall) {
         _agentImpr = agentImpr;
         _slotImpr = slotImpr;
         _waterfall = waterfall;
      }
   }

}
