package models.queryanalyzer.iep;

import models.queryanalyzer.ds.QAInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImpressionEstimator implements AbstractImpressionEstimator {
   private static int SAMPLING_FACTOR = 200;
   private static int MAX_PROBE_IMPRESSIONS = 1; //warning this must be greater than 0
   private int _samplingImpressions;

   private int _nodeId;
   private Map<Integer, IESolution> _solutions;
   private int _bestNode;
   private int _bestSlotObjVal;
   private double _bestImprObjVal;
   private double _combinedObjectiveBound;

   private int _advertisers;
   private int _slots;
   private double[] _trueAvgPos;
   private int _ourIndex;
   private int _ourImpressions;
   private int _imprUB;
   private int[] _agentImprUB;
   private int[] _agentImprLB;
   private boolean[] _fractionalAvgPos;

   private double[] _agentImpressionDistributionMean;
   private double[] _agentImpressionDistributionStdev;

   private double _startTime;
   private double _timeOut = 1; //in seconds

   public ImpressionEstimator(QAInstance inst) {
      _advertisers = inst.getNumAdvetisers();
      _slots = inst.getNumSlots();

      //_trueAvgPos should contain exact average positions whenever possible.
      //If there is no exact average position, use sampled average position.
      _trueAvgPos = inst.getAvgPos().clone();
      for (int i=0; i<_trueAvgPos.length; i++) {
    	  if (_trueAvgPos[i] == -1) _trueAvgPos[i] = inst.getSampledAvgPos()[i];
      }
      
      _ourIndex = inst.getAgentIndex();
      _ourImpressions = inst.getImpressions();
      _imprUB = inst.getImpressionsUB();

      _agentImpressionDistributionMean = inst.getAgentImpressionDistributionMean();
      _agentImpressionDistributionStdev = inst.getAgentImpressionDistributionStdev();

      _fractionalAvgPos = new boolean[_advertisers];
      int wholeAvgPos = 0;
      for (int i = 0; i < _advertisers; i++) {
         _fractionalAvgPos[i] = ((_trueAvgPos[i] * 100000) % 100000) != 0;
         if (!_fractionalAvgPos[i]) {
            wholeAvgPos += 1;
         }
      }

      _samplingImpressions = Math.max(MAX_PROBE_IMPRESSIONS + 1, _imprUB / SAMPLING_FACTOR * wholeAvgPos * wholeAvgPos); //value must be at least 2, becouse it must be greater than MAX_PROBE_IMPRESSIONS

      assert _ourImpressions > 0;

      _agentImprLB = new int[_advertisers];
      _agentImprUB = new int[_advertisers];

      for (int i = 0; i < _advertisers; i++) {
         _agentImprLB[i] = 1;
         _agentImprUB[i] = inst.getImpressionsUB();
      }

      if (!_fractionalAvgPos[_ourIndex]) {
         _agentImprLB[_ourIndex] = _ourImpressions - _ourImpressions / 10;
         _agentImprUB[_ourIndex] = _ourImpressions + _ourImpressions / 10;
      }

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
      if (!feasibleOrder(order)) {
         System.out.println(Arrays.toString(order));
         System.out.println(Arrays.toString(order));
         for (int i = 0; i < order.length; i++) {
            System.out.print(_trueAvgPos[order[i]] + " ");
         }

         assert (false) : "should have been eliminated in LDS search";
         return null;
      }

      _nodeId = 0;
      _solutions = new HashMap<Integer, IESolution>();
      _combinedObjectiveBound = Integer.MAX_VALUE;
      _bestSlotObjVal = Integer.MAX_VALUE;
      _bestImprObjVal = Double.MAX_VALUE;

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
         return null;
      } else {
         return new IEResult(_combinedObjectiveBound, sol._agentImpr, copyArray(order), sol._slotImpr);
      }
   }

   void checkImpressions(int currIndex, int[] agentImpr, int[] slotImpr, int[] order) {
      _nodeId++;

      double stop = System.currentTimeMillis();
      double elapsed = (stop - _startTime) / 1000.0;
      if (elapsed > _timeOut) {
         return;
      }

      if (slotImpr[0] > _imprUB) {
         return; //this is infeasible
      }

      if (currIndex >= _advertisers) {

         // We've come up with feasible impression estimates for all advertisers.
         // Compute objective value for this solution.

         int imprObjVal = Math.abs(agentImpr[_ourIndex] - _ourImpressions);
         int slotImprObjVal = Math.max(0, slotImpr[0] - _imprUB);
         assert (slotImprObjVal == 0);
         int slotObjVal = 1;
         int slotObjCount = 1;
         for (int i = 0; i < slotImpr.length - 1; i++) {
            if (Math.abs(slotImpr[i] - slotImpr[i + 1]) < _samplingImpressions / 4 || imprObjVal == 0) { //FIXME: sodomka: 1) Should < in 1st condition be > ?   2) Why the second condition? Couldn't this make an objective w/ imprObjVal==1 better than one where imprObjVal==0? (note we are trying to minimize the objective)
               slotObjVal += Math.abs(slotImpr[i] - slotImpr[i + 1]);
               slotObjCount++;
            } else {
               break;
            }
         }

         double probWaterfall = getImpressionModelObjective(agentImpr, order);

         double combinedObj = imprObjVal * probWaterfall;

         if (combinedObj < _combinedObjectiveBound || (combinedObj == _combinedObjectiveBound && slotObjVal < _bestSlotObjVal)) {
            _combinedObjectiveBound = combinedObj;
            _bestImprObjVal = imprObjVal;
            _bestSlotObjVal = slotObjVal;
            _solutions.put(_nodeId, new IESolution(agentImpr, slotImpr));
            _bestNode = _nodeId;
         }

         return;
      }

      int currAgent = order[currIndex];

      int bestImpr = calcMinImpressions(slotImpr, currIndex, _trueAvgPos[currAgent]);

      if (bestImpr > 0) {

         // Some #imps for this agent was successfully found.
         // If "this agent" is us, calculate how far the computed #imps is to our actual #imps.
         //   If prediction is not close enough, return (and possibly search with some new #imps estimate for opponents)
         // Go on to the next agent (assuming the #imps just found for this agent is true)

         if (currAgent == _ourIndex) {
            int imprObjVal = Math.abs(bestImpr - _ourImpressions);
            if (imprObjVal > _bestImprObjVal) {
               return;
            }
         }

         agentImpr[currAgent] = bestImpr;
         int[] newSlotImpr = fillSlots(slotImpr, bestImpr);
         checkImpressions(currIndex + 1, agentImpr, newSlotImpr, order);
      } else {

         // BestImpr <= 0, i.e. avgPos is an integer for this agent (and thus #imps is not well specified).
         // Iterate through different possible #imps.
         // Iterate increasing/decreasing depending on whether or not this agent is in the 1st position.

         int firstSlot = Math.min(currIndex + 1, _slots);
         int maxImpr = (firstSlot == 1) ? _agentImprUB[currAgent] : slotImpr[firstSlot - 2] - 1; //FIXME: sodomka: Why -1?
         //FIXME: sodomka: if we're not sampling the same points, for #imps, it seems like we're more likely to have small diffs

         if (firstSlot == 1) {
            for (int i = _agentImprLB[currAgent]; i <= maxImpr; i++) {
               if (i % _samplingImpressions == 0 || i == maxImpr || i <= _agentImprLB[currAgent] + MAX_PROBE_IMPRESSIONS) {
                  int[] agentImprCopy = copyArray(agentImpr);
                  agentImprCopy[currAgent] = i;
                  int[] newSlotImpr = fillSlots(slotImpr, i);

                  checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order);
               }
            }
         } else {
            for (int i = maxImpr; i >= _agentImprLB[currAgent]; i--) {
               if (i % _samplingImpressions == 0 || i == maxImpr || i <= _agentImprLB[currAgent] + MAX_PROBE_IMPRESSIONS) {
                  int[] agentImprCopy = copyArray(agentImpr);
                  agentImprCopy[currAgent] = i;
                  int[] newSlotImpr = fillSlots(slotImpr, i);
                  checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order);
               }
            }
         }
      }
   }

   private double getImpressionModelObjective(int[] agentImpr, int[] order) {
      return getImpressionModelObjective(agentImpr, order, order.length);
   }

   private double getImpressionModelObjective(int[] agentImpr, int[] order, int numSlotsFilled) {
      double obj = 1.0;
      numSlotsFilled = Math.min(numSlotsFilled, agentImpr.length);
      for (int i = 0; i < numSlotsFilled; i++) {
         double mean = _agentImpressionDistributionMean[i];
         double stdDev = _agentImpressionDistributionStdev[i];
         if (mean != -1 && stdDev != -1) {
            double imps = agentImpr[i];
            obj *= gaussianPDF(imps, mean, stdDev);
         }
      }

      if (obj == 1.0) {
         //This means we didn't place anyone with a prediction yet
         //so return a bad objective
         return 1.0;
      } else {
         return (1.0 - obj);
      }
   }

   private double gaussianPDF(double x, double mean, double sigma) {
      double diff = x - mean;
      double sigma2 = sigma * sigma;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
   }

   private int calcMinImpressions(int[] slotImpr, int currIndex, double trueAvgPos) {
      int firstSlot = Math.min(currIndex + 1, _slots);

      double approxAvgPos = firstSlot;
      if (approxAvgPos == trueAvgPos) {
         return -1;
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
      }

      //int finalSlotImp = (int)ceil((wImp - fullSlotImp*trueAvgPos)/(trueAvgPos - finalSlot));
      int finalSlotImp = (int) ((wImp - fullSlotImp * trueAvgPos) / (trueAvgPos - finalSlot));
      if (finalSlotImp < 1) {
         finalSlotImp = 1;
      }

      return fullSlotImp + finalSlotImp;
   }

   private int[] fillSlots(int[] slotImpr, int newImpr) {
      int[] newSlotImpr = copyArray(slotImpr);

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
         } else {
            int r = slotImpr[s - 1] - slotImpr[s];
            assert (r >= 0);
            if (r < remainingImp) {
               remainingImp -= r;
               newSlotImpr[s] += r;
            } else {
               newSlotImpr[s] += remainingImp;
               break;
            }
         }
      }

      return newSlotImpr;
   }

   private int[] copyArray(int[] arr) {
      int[] newArr = new int[arr.length];
      for (int i = 0; i < arr.length; i++) {
         newArr[i] = arr[i];
      }
      return newArr;
   }

   public String getName() {
      return "CJC";
   }

   private class IESolution {
      public int[] _agentImpr;
      public int[] _slotImpr;

      public IESolution(int[] agentImpr, int[] slotImpr) {
         _agentImpr = agentImpr;
         _slotImpr = slotImpr;
      }
   }
}
