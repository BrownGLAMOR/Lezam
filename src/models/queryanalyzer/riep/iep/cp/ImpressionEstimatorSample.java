package models.queryanalyzer.riep.iep.cp;

import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.probsample.SampleProbability;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;

import java.util.*;


//TODO: We have bug in padAgentsWithknownPositions,  array out of boundary. The ordering array is passing values bigger than size of trueAvgPos, Data type: old
public class ImpressionEstimatorSample implements AbstractImpressionEstimator {

   private boolean IE_DEBUG = false;
   QAInstanceAll _instance;
   int MIN_PADDED_AGENT_ID = 100;

   private double AVG_POS_EPSILON = 0;

   private boolean PAD_AGENTS;
   private boolean BRANCH_AROUND_PRIOR = false;
   private boolean SEARCH_ON_PRIOR = true;
   private boolean OPP_PRIOR_TIE_BREAK = true;
   private boolean SIMPLE_PR_SAMP = false;
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
   private boolean FULL_SELF_POS;
   private double FILL_SLOTS_AVG; //this contains the value of the average position of the person last added using fillSlots
   private int MIN_TOT_IMPR = 25;
   private int _numSamples = 10;
   private int _fractionalBranches; //The number of branches will be double this(1 above and 1 below solved imps)
   SampleProbability _sampleP;

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
   private double[] _sampledAvgPos;
   private int _ourIndex;
   private int _ourImpressions;
   private int _imprUB;
   private int[] _agentImprUB;
   private int[] _agentImprLB;

   private double[] _agentImpressionDistributionMean;
   private double[] _agentImpressionDistributionStdev;
   private boolean[] _agentSawSample; //true if the agent saw at least one sample.
   private boolean[] _agentIsPadded; //true if the agent is a dummy or "padded" agent.
   private int[] _agentIds;
   String[] _agentNames;

   private double _startTime;
   private double _timeOut = 5; //in seconds

   public ImpressionEstimatorSample(QAInstanceAll inst) {
      this(inst, 150, 0, 10);
   }

   public ImpressionEstimatorSample(QAInstanceAll inst, int samplingFactor, int fractionalBranches, int numSamples) {
      this(inst, samplingFactor, fractionalBranches, 0.77,0.88,0.029,0.433,numSamples);
   }

   public ImpressionEstimatorSample(QAInstanceAll inst, int samplingFactor, int fractionalBranches, double avgposstddev, double ouravgposstddev, double imppriorstddev, double ourimppriorstddev, int numSamples) {
      this(inst, samplingFactor, fractionalBranches, avgposstddev, ouravgposstddev, imppriorstddev, ourimppriorstddev,1,1,1,1,numSamples);
   }

   public ImpressionEstimatorSample(QAInstanceAll inst, int samplingFactor, int fractionalBranches, double avgposstddev, double ouravgposstddev, double imppriorstddev, double ourimppriorstddev,
                                    double avgpospower, double ouravgpospower, double imppriorpower, double ourimppriorpower, int numSamples) {
      _instance = inst;
      SAMPLING_FACTOR = samplingFactor;
      _fractionalBranches = fractionalBranches;
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
      _numSamples = numSamples;
      PAD_AGENTS = inst.isPadding();

      //_trueAvgPos should contain exact average positions whenever possible.
      //If there is no exact average position, use sampled average position.
      _trueAvgPos = inst.getAvgPos().clone();
      _sampledAvgPos = inst.getSampledAvgPos().clone();
      for (int i = 0; i < _trueAvgPos.length; i++) {
         if (i == _ourIndex) {
            FULL_SELF_POS = !(_trueAvgPos[i] == -1);
         }
         if (_trueAvgPos[i] == -1) {
            _trueAvgPos[i] = _sampledAvgPos[i];
         }
      }

      //Determine which agents saw at least one sample
      _agentSawSample = new boolean[_sampledAvgPos.length];
      for (int i = 0; i < _sampledAvgPos.length; i++) {
         if (!Double.isNaN(_sampledAvgPos[i])) {
            _agentSawSample[i] = true;
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
            if(Double.isNaN(_trueAvgPos[i])) {
               _agentImpressionDistributionMean[i] = 0.5 * _imprUB / ((double) _numSamples);
               _agentImpressionDistributionStdev[i] = 2 * _imprUB / ((double) _numSamples);
            }
            else {
               _agentImpressionDistributionStdev[i] = _agentImpressionDistributionMean[i]*IMP_PRIOR_STD_DEV;
            }
         }
      }

      _agentIds = inst.getAgentIds();
      _agentNames = inst.getAgentNames();
      
      if (PAD_AGENTS) {
         //Initially, none of the agents are padded
         //(This isn't really needed when we know the rankings of everybody)
         _agentIsPadded = new boolean[_advertisers];

         if (inst.allInitialPositionsKnown()) {
            padAgentsWithKnownPositions(inst.getInitialPositionOrdering());
         } else {
            removeUnsampledAgents();
            padAgentsWithUnknownPositions();
         }
      }

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
         _agentImprLB[i] = 5;
         _agentImprUB[i] = _imprUB;
      }

      _agentImprLB[_ourIndex] = _ourImpressions - _ourImpressions / 10;
      _agentImprUB[_ourIndex] = _ourImpressions + _ourImpressions / 10;

      if (!SIMPLE_PR_SAMP && !GAUSSIAN_PR_SAMP) {
         _sampleP = new SampleProbability();
      }


      if (IE_DEBUG) {
         System.out.println("IEDebug: avgPos=" + Arrays.toString(inst.getAvgPos()) + ", sampledAvgPos=" + Arrays.toString(inst.getSampledAvgPos()) + ", _trueAvgPos=" + Arrays.toString(_trueAvgPos) + ", _agentSawSample=" + Arrays.toString(_agentSawSample));
      }

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
	   
	   //If we didn't use padded agents, we don't need to reduce the result.
	   if (!PAD_AGENTS) return result;
	   
	   
      int[] originalIds = _instance.getAgentIds();

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
               break;
            }
         }
      }

      //order [A, B, C] says that agent A was in the first slot, B was in the 2nd, etc.
      //If a padded agent was in some slot, change that value to -1.
      //In other words, we'll still acknowledge that there was SOME agent in that slot, but we don't know who.
      int[] reducedOrder = new int[paddedOrder.length];
      Arrays.fill(reducedOrder, -1);
      for (int i = 0; i < paddedOrder.length; i++) {
         int paddedAgentIdx = paddedOrder[i];
         int paddedAgentId = _agentIds[paddedAgentIdx];

         //Find this agentID in the original instance
         for (int j = 0; j < originalIds.length; j++) {
            if (paddedAgentId == originalIds[j]) {
               reducedOrder[i] = j;
               break;
            }
         }
      }

      IEResult reducedResult = new IEResult(result.getObj(), reducedImpressions, reducedOrder, paddedSlotImpressions, result.getWaterfall(), reducedAgentNames);
      System.out.println("Reduced IE Result: " + result);
      System.out.println("Reduced IE Result: " + reducedResult);
      
      return reducedResult;
   }


   /**
    * This removes any agents that have an either NaN or unknown average position.
    * In effect, it is removing opponent agents that weren't sampled
    */
   private void removeUnsampledAgents() {
      //Determine how many agents must be removed
      int numAgentsToRemove = 0;
      int ourIndexOffset = 0;
      for (int i = 0; i < _advertisers; i++) {
         if (Double.isNaN(_trueAvgPos[i]) || _trueAvgPos[i] == -1) {
            numAgentsToRemove++;
            if (i < _ourIndex) {
               ourIndexOffset++; //Adjust our agent's index accordingly
            }
         }
      }

      //If nobody needs to be removed, just return now.
      if (numAgentsToRemove == 0) {
         return;
      }

      //Otherwise, remove anyone that didn't see a sample.
      int numAgents = _advertisers - numAgentsToRemove;
      int[] agentIds = new int[numAgents];
      String[] agentNames = new String[numAgents];
      double[] avgPos = new double[numAgents];
      double[] sampledAvgPos = new double[numAgents];
      boolean[] agentIsPadded = new boolean[numAgents];
      double[] agentImpressionDistributionMean = new double[numAgents];
      double[] agentImpressionDistributionStdev = new double[numAgents];
      boolean[] agentSawSample = new boolean[numAgents];
      int idx = 0;
      for (int i = 0; i < _advertisers; i++) {
         if (!Double.isNaN(_trueAvgPos[i]) && _trueAvgPos[i] != -1) {
            agentIds[idx] = _agentIds[i];
            agentNames[idx] = _agentNames[i];
            avgPos[idx] = _trueAvgPos[i];
            sampledAvgPos[idx] = _sampledAvgPos[i];
            agentIsPadded[idx] = _agentIsPadded[i];
            agentImpressionDistributionMean[idx] = _agentImpressionDistributionMean[i];
            agentImpressionDistributionStdev[idx] = _agentImpressionDistributionStdev[i];
            agentSawSample[idx] = _agentSawSample[i];
            idx++;
         }
      }
      _advertisers = numAgents;
      _agentIds = agentIds;
      _agentNames = agentNames;
      _trueAvgPos = avgPos;
      _sampledAvgPos = sampledAvgPos;
      _agentIsPadded = agentIsPadded;
      _agentImpressionDistributionMean = agentImpressionDistributionMean;
      _agentImpressionDistributionStdev = agentImpressionDistributionStdev;
      _agentSawSample = agentSawSample;
      _ourIndex = _ourIndex - ourIndexOffset;
   }


   private void padAgentsWithUnknownPositions() {
      int[] apOrder = QAInstanceAll.getAvgPosOrder(_trueAvgPos);
      while (!feasibleOrder(apOrder)) {
         int foundTooBigStart = 0;
         int foundTooBigStop = 0;
         for (int i = 0; i < _slots && i < _advertisers; i++) {
            if (_trueAvgPos[apOrder[i]] > i + 1) {
               foundTooBigStart = i + 1;
               foundTooBigStop = (int) Math.ceil(_trueAvgPos[apOrder[i]]);
               break;
            }
         }

         if (foundTooBigStart > 0) {
            addPaddingAgents(foundTooBigStart, foundTooBigStop);
         }

         apOrder = QAInstanceAll.getAvgPosOrder(_trueAvgPos);
      }
   }

   //pads the auction with "fake" advertisers so that the instance is feasible
   //Feasible means every agent starts in a position greater or equal to their avg pos
   private void addPaddingAgents(int startSlot, int stopSlot) {
      int newAdvertisers = _advertisers + stopSlot - startSlot;
      double[] newAvgPos = new double[newAdvertisers];
      double[] newSampledAvgPos = new double[newAdvertisers];
      int[] newAgentIds = new int[newAdvertisers];
      String[] newAgentNames = new String[newAdvertisers];
      boolean[] newAgentIsPadded = new boolean[newAdvertisers];
      double[] newAgentImpressionDistributionMean = new double[newAdvertisers];
      double[] newAgentImpressionDistributionStdev = new double[newAdvertisers];
      boolean[] newAgentSawSample = new boolean[newAdvertisers];

      for (int i = 0; i < _advertisers; i++) {
         newAvgPos[i] = _trueAvgPos[i];
         newSampledAvgPos[i] = _sampledAvgPos[i];
         newAgentIds[i] = _agentIds[i];
         newAgentNames[i] = _agentNames[i];
         newAgentIsPadded[i] = _agentIsPadded[i];
         newAgentImpressionDistributionMean[i] = _agentImpressionDistributionMean[i];
         newAgentImpressionDistributionStdev[i] = _agentImpressionDistributionStdev[i];
         newAgentSawSample[i] = _agentSawSample[i];
      }

      for (int i = 0; i < stopSlot - startSlot; i++) {
         newAvgPos[_advertisers + i] = startSlot + i;
         newSampledAvgPos[_advertisers + i] = Double.NaN;
         newAgentIds[_advertisers + i] = MIN_PADDED_AGENT_ID + i;
         newAgentNames[_advertisers + i] = "PaddedAgent" + i;
         newAgentIsPadded[_advertisers + i] = true;
         newAgentImpressionDistributionMean[_advertisers + i] = 0.5 * _imprUB / ((double) _numSamples);
         newAgentImpressionDistributionStdev[_advertisers + i] = 2 * _imprUB / ((double) _numSamples);
         newAgentSawSample[_advertisers + i] = false;
      }

      _advertisers = newAdvertisers;
      _trueAvgPos = newAvgPos;
      _sampledAvgPos = newSampledAvgPos;
      _agentIds = newAgentIds;
      _agentNames = newAgentNames;
      _agentIsPadded = newAgentIsPadded;
      _agentImpressionDistributionMean = newAgentImpressionDistributionMean;
      _agentImpressionDistributionStdev = newAgentImpressionDistributionStdev;
      _agentSawSample = newAgentSawSample;
   }
   
   //TODO: We have bug here,  array out of boundary. The ordering array is passing values bigger than size of trueAvgPos, Data type: old 
   private void padAgentsWithKnownPositions(int[] ordering) {
      //If any agents have a NaN sampled average position,
      //give them a dummy average position equal to min(their starting position, numSlots).
      for (int i = 0; i < _trueAvgPos.length; i++) {
    	  System.out.println(ordering[i]);
         if (Double.isNaN(_trueAvgPos[ordering[i]])) {
            _trueAvgPos[ordering[i]] = Math.min(i + 1, _slots);
         }
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

      if(order.length == 1) {
         return reduceIEResult(  new IEResult(0,new int[]{ _ourImpressions }, new int[] { 0 }, new int[] { _ourImpressions, 0, 0, 0, 0 }, _agentNames)  );
      }
      else if(order.length == 2) {
         if(_trueAvgPos[_ourIndex] == 1.0) {
            double oppPrior;
            double oppAvgPos;
            if(_ourIndex == 0) {
               oppAvgPos = _trueAvgPos[1];
               oppPrior = _agentImpressionDistributionMean[1];
            }
            else {
               oppAvgPos = _trueAvgPos[0];
               oppPrior = _agentImpressionDistributionMean[0];
            }

            int oppImps;
            if(Double.isNaN(oppAvgPos)) {
               //Opponent agent did not get sampled, assign 1/2 the maximum
               //expected imps before seeing an impression
               oppImps = (int)(.5 * (_imprUB / ((double)_numSamples)));
            }
            else if(oppAvgPos == 2.0) {
               //Assume they saw the same amount as us (should probably be something with priors)
               if(oppPrior > 0 && oppPrior < _ourImpressions) {
                  oppImps = Math.min(_imprUB, (int)oppPrior);
               }
               else {
                  oppImps = _ourImpressions;
               }
            }
            else if(oppAvgPos == 1.0) {
               //Both agents have avgPos 1.0, which means we were sampled
               //we don't know how many imps other agent saw so assume max
               //(should probably be somethng with priors)
               if(oppPrior > _ourImpressions) {
                  oppImps = Math.min(_imprUB, (int)oppPrior);
               }
               else {
                  oppImps = _imprUB;
               }
            }
            else {
               //Otherwise they have a fractional avg position and we can solve for the amount
               oppImps = Math.min(_imprUB, _ourImpressions + (int)((2.0 - oppAvgPos) * _ourImpressions / (oppAvgPos - 1.0)));
            }

            if(_ourIndex == 0) {
               if(_ourImpressions >= oppImps) {
                  return reduceIEResult(  new IEResult(0,new int[]{ _ourImpressions, oppImps }, new int[] { 0, 1 }, new int[] { _ourImpressions, oppImps, 0, 0, 0 }, _agentNames)  );
               }
               else {
                  return reduceIEResult(  new IEResult(0,new int[]{ _ourImpressions, oppImps }, new int[] { 0, 1 }, new int[] { oppImps, _ourImpressions, 0, 0, 0 }, _agentNames)  );
               }
            }
            else {
               if(_ourImpressions >= oppImps) {
                  return reduceIEResult(  new IEResult(0,new int[]{ oppImps, _ourImpressions }, new int[] { 1, 0 }, new int[] { _ourImpressions, oppImps, 0, 0, 0 }, _agentNames)  );
               }
               else {
                  return reduceIEResult(  new IEResult(0,new int[]{ oppImps, _ourImpressions }, new int[] { 1, 0 }, new int[] { oppImps, _ourImpressions, 0, 0, 0 }, _agentNames)  );
               }
            }
         }
         else {
            double ourAvgPos;
            double oppPrior;
            if(_ourIndex == 0) {
               ourAvgPos = _trueAvgPos[0];
               oppPrior = _agentImpressionDistributionMean[1];
            }
            else {
               ourAvgPos = _trueAvgPos[1];
               oppPrior = _agentImpressionDistributionMean[0];
            }

            int oppImps;
            if(ourAvgPos == 2.0) {
               //Agent saw more impressions than us
               //Assume they saw as the upper bound
               //(This should probably be influenced by the prior)
               if(oppPrior > _ourImpressions) {
                  oppImps = Math.min(_imprUB, (int)oppPrior);
               }
               else {
                  oppImps = _imprUB;
               }
            }
            else {
               //Otherwise they have a fractional avg position and we can solve for the amount
               oppImps = Math.min(_imprUB, (int)((ourAvgPos - 1.0) * _ourImpressions));
            }

            if(_ourIndex == 0) {
               if(_ourImpressions >= oppImps) {
                  return reduceIEResult(  new IEResult(0,new int[]{ _ourImpressions, oppImps }, new int[] { 1, 0 }, new int[] { _ourImpressions, oppImps, 0, 0, 0 }, _agentNames)  );
               }
               else {
                  return reduceIEResult(  new IEResult(0,new int[]{ _ourImpressions, oppImps }, new int[] { 1, 0 }, new int[] { oppImps, _ourImpressions, 0, 0, 0 }, _agentNames)  );
               }
            }
            else {
               if(_ourImpressions >= oppImps) {
                  return reduceIEResult(  new IEResult(0,new int[]{ oppImps, _ourImpressions }, new int[] { 0, 1 }, new int[] { _ourImpressions, oppImps, 0, 0, 0 }, _agentNames)  );
               }
               else {
                  return reduceIEResult(  new IEResult(0,new int[]{ oppImps, _ourImpressions }, new int[] { 0, 1 }, new int[] { oppImps, _ourImpressions, 0, 0, 0 }, _agentNames)  );
               }
            }
         }
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
      checkImpressions(0, agentImpr, slotImpr, order,false);

      IESolution sol = _solutions.get(_bestNode);
      if (sol == null) {
         if (IE_DEBUG) {
            System.out.println("IE Solution is null");
         }
         //System.exit(-1);
         return null;
      } else {
         IEResult result = new IEResult(_combinedObjectiveBound, sol._agentImpr, order.clone(), sol._slotImpr, _agentNames);
         if (IE_DEBUG) {
            System.out.println("Search completed for order " + Arrays.toString(order) + "\n" + result);
         }
         if(PAD_AGENTS) {
            result = reduceIEResult(result);
            if (IE_DEBUG) {
               System.out.println("Reduced " + result);
            }
         }

         return result;
      }
   }

   void checkImpressions(int currIndex, int[] agentImpr, int[] slotImpr, int[] order, boolean isMult) {
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

         if(!isMult && slotImpr[0]*2 < _imprUB) {
            int multiples = (int)Math.floor(_imprUB / ((double) slotImpr[0]));
            //Explore around 5 multiples less than the impression upperbound
            for(int mult = 2; mult <= multiples; mult += (int)Math.ceil(multiples / 5.0)) {
               int[] newAgentImpr = agentImpr.clone();
               int[] newSlotImpr = slotImpr.clone();
               int[] newOrder = order.clone();

               for(int i = 0; i < newAgentImpr.length; i++) {
                  newAgentImpr[i] = newAgentImpr[i] * mult;
               }

               fillSlots(newSlotImpr,newAgentImpr,newOrder);

               checkImpressions(currIndex,newAgentImpr,newSlotImpr,newOrder,true);
            }
         }

         //Prune if we have a waterfall with fewer than minimum impressions
         if (slotImpr[0] < MIN_TOT_IMPR) {
            int j = 0;
            return;
         }

         double probWaterfall;
         if(OPP_PRIOR_TIE_BREAK) {
            probWaterfall =  getImpressionModelObjective(agentImpr, order,true,false);
         }
         else {
            probWaterfall =  getImpressionModelObjective(agentImpr, order,true,true);
         }

         double sampleProb;
         double ourAvgPosDiff = Double.MAX_VALUE;
         int[][] impsPerSlot = greedyAssign(_slots, agentImpr.length, order, agentImpr);
         if (SIMPLE_PR_SAMP) {
            double avgPosDiff = 0.0;
            for (int i = 0; i < order.length; i++) {
               int currAgent = order[i];
               if (!PAD_AGENTS || !_agentIsPadded[currAgent]) {
                  double trueAvgPos = _trueAvgPos[currAgent];
                  if (Double.isNaN(trueAvgPos)) {
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
                  avgPosDiff += agentDiff;
               }
            }
            sampleProb = avgPosDiff + 1;
         }
         else if(GAUSSIAN_PR_SAMP) {
            double[] avgPosPred = new double[_trueAvgPos.length];
            for (int i = 0; i < order.length; i++) {
               int currAgent = order[i];
               if (!PAD_AGENTS || !_agentIsPadded[currAgent]) {
                  double trueAvgPos = _trueAvgPos[currAgent];
                  if (Double.isNaN(trueAvgPos)) {
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
            }
            sampleProb = getAvgPosObjective(_trueAvgPos,avgPosPred);
         }
         else {
            double prSample = 0.0;
//            try {
//               double[] sampleAverages = _instance.getSampledAvgPos();
//               prSample = 1.0 - _sampleP.getProbabilityofSample(sampleAverages, order, agentImpr, _imprUB, _slots, _numSamples);
//            } catch (IloException e) {
//               e.printStackTrace();
//               throw new RuntimeException("CPLEX Failed to Load");
//            }
            sampleProb = prSample + 1;
         }

         double combinedObj = -1 * probWaterfall * sampleProb;

         //System.out.println("combinedObj=" + combinedObj + ", imprObjVal=" + imprObjVal + ", probWaterfall=" + probWaterfall + ", sampleProb=" + sampleProb);

         double tiebreak = 0.0;
         if(OPP_PRIOR_TIE_BREAK) {
            tiebreak =  getImpressionModelObjective(agentImpr, order,false,true);
         }

         if (combinedObj < _combinedObjectiveBound || (OPP_PRIOR_TIE_BREAK && (combinedObj == _combinedObjectiveBound) && (tiebreak >= _bestTieBreak))) {
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

      int bestImpr = calcMinImpressions(slotImpr, currIndex, order, _trueAvgPos[currAgent]);

      if (bestImpr > 0) {

         // Some #imps for this agent was successfully found.
         // If "this agent" is us, calculate how far the computed #imps is to our actual #imps.
         //   If prediction is not close enough, return (and possibly search with some new #imps estimate for opponents)
         // Go on to the next agent (assuming the #imps just found for this agent is true)

         int maxImpr = _agentImprUB[currAgent];
         bestImpr = Math.min(maxImpr,bestImpr);

         LinkedList<Integer> branches = new LinkedList<Integer>();
         branches.add(bestImpr);

         if (_fractionalBranches > 0) {
            //we don't do fractional branching on ourselves,
            //because we have an exact average
            if(currAgent != _ourIndex) {
               LinkedList<Integer> upBranches = new LinkedList<Integer>();
               for (int i = bestImpr + 1; i <= maxImpr; i++) {
                  if (i % _samplingImpressions == 0 || i == maxImpr) {
                     upBranches.add(i);
                     if (upBranches.size() >= _fractionalBranches) {
                        break;
                     }
                  }
               }

               int minImpr = _agentImprLB[currAgent];
               LinkedList<Integer> downBranches = new LinkedList<Integer>();
               for (int i = bestImpr - 1; i >= minImpr; i--) {
                  if (i % _samplingImpressions == 0 || i == minImpr) {
                     downBranches.add(i);
                     if (downBranches.size() >= _fractionalBranches) {
                        break;
                     }
                  }
               }

               if (BRANCH_AROUND_PRIOR) {
                  for (int i = 0; i < Math.max(upBranches.size(), downBranches.size()); i++) {
                     if (i < upBranches.size()) {
                        branches.add(upBranches.get(i));
                     }
                     if (i < downBranches.size()) {
                        branches.add(downBranches.get(i));
                     }
                  }
               } else {
                  branches.addAll(upBranches);
                  branches.addAll(downBranches);
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
                  if (Math.abs(_trueAvgPos[currAgent] - FILL_SLOTS_AVG) > (_ourAvgPosDiff + .01)) {
                     return;
                  }
               }
            }

            checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order, false);
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
         if(bestImpr == -2 && firstSlot != 1) {
            //Determine how many directly above us have NaN average position
            int numNaN = 0;
            for(int i = currIndex-1; i >= 0; i--) {
               if(Double.isNaN(_sampledAvgPos[order[i]])) {
                  numNaN++;
               }
            }

            maxImpr = ((firstSlot - numNaN) == 1) ? _agentImprUB[currAgent] : Math.min(_agentImprUB[currAgent], slotImpr[Math.max((firstSlot - numNaN) - 2,0)]);
         }
         else {
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
                  if (Math.abs(_trueAvgPos[currAgent] - FILL_SLOTS_AVG) > (_ourAvgPosDiff + .01)) {
                     return;
                  }
               }
            }

            checkImpressions(currIndex + 1, agentImprCopy, newSlotImpr, order, false);
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
         if(!Double.isNaN(mean)) {
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
                  if((((mean * 100000) % 100000) != 0) && i != _ourIndex && Math.abs(avgpos - mean) < AVG_POS_EPSILON) {
                     prob = Math.log(gaussianPDF(mean - AVG_POS_EPSILON / 2.0, mean, stdDev));
                  }
                  else {
                     prob = Math.log(gaussianPDF(avgpos, mean, stdDev));
                  }

                  if(i == _ourIndex) {
                     prob *= OUR_AVG_POS_POWER;
                  }
                  else {
                     prob *= AVG_POS_POWER;
                  }
                  obj += prob;
               } else {
                  double prob;
                  if((((mean * 100000) % 100000) != 0) && i != _ourIndex && Math.abs(avgpos - mean) < AVG_POS_EPSILON) {
                     prob = gaussianPDF(mean - AVG_POS_EPSILON / 2.0, mean, stdDev);
                  }
                  else {
                     prob = gaussianPDF(avgpos, mean, stdDev);
                  }

                  if(i == _ourIndex) {
                     prob = Math.pow(prob,OUR_AVG_POS_POWER);
                  }
                  else {
                     prob = Math.pow(prob,AVG_POS_POWER);
                  }
                  obj *= prob;
               }
               //System.out.println("currAgent=" + currAgent + ", gaussianPDF(imps=" + imps +", mean="+ mean + ", stdDev=" + stdDev + ") = " + prob);
            }
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

   private int calcMinImpressions(int[] slotImpr, int currIndex, int[] order, double trueAvgPos) {
      if (Double.isNaN(trueAvgPos)) {
         return -1;
      }

      int currAgent = order[currIndex];

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


      if(((trueAvgPos * 100000) % 100000) == 0) {
         if(currAgent != _ourIndex) {
            return -2;
         }
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

   public static void fillSlots(int[] slotImpr, int[] newImprs, int[] order) {
      for (int i = 0; i < newImprs.length; ++i) {
         int remainingImp = newImprs[order[i]];
         for (int s = Math.min(i + 1, slotImpr.length) - 1; s >= 0; --s) {
            if (s == 0) {
               slotImpr[0] += remainingImp;
            } else {
               int r = slotImpr[s - 1] - slotImpr[s];
               assert (r >= 0);
               if (r < remainingImp) {
                  remainingImp -= r;
                  slotImpr[s] += r;
               } else {
                  slotImpr[s] += remainingImp;
                  break;
               }
            }
         }
      }
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
   public static int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
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

   public double[] getApproximateAveragePositions() {
      return _trueAvgPos.clone();
   }

   public QAInstanceAll getInstance() {
      return _instance;
   }

   public ObjectiveGoal getObjectiveGoal() {
      //At least with the old LDS, lower objective values are better. Change this if necessary.
      return ObjectiveGoal.MINIMIZE;
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
      sb.append("agentSawSample=" + Arrays.toString(_agentSawSample) + "\n");
      sb.append("agentIsPadded=" + Arrays.toString(_agentIsPadded) + "\n");
      sb.append("agentIds=" + Arrays.toString(_agentIds) + "\n");
      sb.append("agentNames=" + Arrays.toString(_agentNames) + "\n");
      return sb.toString();
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
