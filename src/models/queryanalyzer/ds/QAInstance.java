package models.queryanalyzer.ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


public class QAInstance {
   private int _slots;
   private int _promotedSlots;
   private int _advetisers;
   private double[] _avgPos;
   private double[] _sampledAvgPos;
   private int[] _agentIds;
   private int _agentIndex;
   private int _impressions;
   private int _promotedImpressions;
   private int _impressionsUB;
   private boolean _considerPaddingAgents;
   private boolean _promotionEligibilityVerified;
	private boolean _hitOurBudget; //1 if agent hit budget, 0 if didn't hit budget, -1 if unknown
   private double[] _agentImpressionDistributionMean; //prior on agent impressions
   private double[] _agentImpressionDistributionStdev; //prior on agent impressions
   private boolean _isSampled;
   private int[] _initialPosition; //The agentIdx in the ith position in (-1 if unknown)
   int MIN_PADDED_AGENT_ID = 100;

   public QAInstance(int slots, int promotedSlots, int advetisers, double[] avgPos, double[] sampledAvgPos, int[] agentIds, int agentIndex,
                     int impressions, int promotedImpressions, int impressionsUB, boolean considerPaddingAgents, boolean promotionEligibiltyVerified,
                     boolean hitOurBudget,
                     double[] agentImpressionDistributionMean, double[] agentImpressionDistributionStdev, boolean isSampled, 
                     int[] initialPosition) {
      assert (avgPos.length == advetisers);
      assert (agentIds.length == advetisers);
      assert (advetisers == 0 || (advetisers > agentIndex && agentIndex >= 0));
      _slots = slots;
      _promotedSlots = promotedSlots;
      _advetisers = advetisers;
      _avgPos = avgPos;
      _sampledAvgPos = sampledAvgPos;
      _agentIds = agentIds;
      _agentIndex = agentIndex;
      _impressions = impressions;
      _promotedImpressions = promotedImpressions;
      _impressionsUB = impressionsUB;
      _considerPaddingAgents = considerPaddingAgents;
      _promotionEligibilityVerified = promotionEligibiltyVerified;
      _agentImpressionDistributionMean = agentImpressionDistributionMean;
      _agentImpressionDistributionStdev = agentImpressionDistributionStdev;
      _isSampled = isSampled;
      _initialPosition = initialPosition;

      if (_considerPaddingAgents) {
         while (!feasibleOrder(getAvgPosOrder(avgPos))) {
            int[] apOrder = getAvgPosOrder(avgPos);

            int foundTooBigStart = 0;
            int foundTooBigStop = 0;
            for (int i = 0; i < _slots && i < _advetisers; i++) {
               if (_avgPos[apOrder[i]] > i + 1) {
                  foundTooBigStart = i + 1;
                  foundTooBigStop = (int) Math.ceil(_avgPos[apOrder[i]]);
                  break;
               }
            }

            if (foundTooBigStart > 0) {
               addPaddingAgents(foundTooBigStart, foundTooBigStop);
            }
         }
         assert (feasibleOrder(getAvgPosOrder(avgPos))) : "addPaddingAgents broke did not work...";
      }
   }

   //pads the auction with "fake" advertisers so that the instance is feasible
   //Feasible means every agent starts in a position greater or equal to their avg pos
   //assumes the highest agent ID is 99
   private void addPaddingAgents(int startSlot, int stopSlot) {
      int oldAdvertisers = _advetisers;
      double[] oldAvgPos = _avgPos;
      int[] oldAgentIds = _agentIds;

      int newAdvertisers = oldAdvertisers + stopSlot - startSlot;
      double[] newAvgPos = new double[newAdvertisers];
      int[] newAgentIds = new int[newAdvertisers];

      for (int i = 0; i < oldAdvertisers; i++) {
         newAvgPos[i] = oldAvgPos[i];
         newAgentIds[i] = oldAgentIds[i];
      }

      for (int i = 0; i < stopSlot - startSlot; i++) {
         newAvgPos[oldAdvertisers + i] = startSlot + i;
         newAgentIds[oldAdvertisers + i] = MIN_PADDED_AGENT_ID + i;
      }

      _advetisers = newAdvertisers;
      _avgPos = newAvgPos;
      _agentIds = newAgentIds;
   }

   public int getNumSlots() {
      return _slots;
   }

   public int getNumPromotedSlots() {
      return _promotedSlots;
   }

   public int getNumAdvetisers() {
      return _advetisers;
   }

   public double[] getAvgPos() {
      return _avgPos;
   }

   public double[] getSampledAvgPos() {
      return _sampledAvgPos;
   }

   public int[] getAgentIds() {
      return _agentIds;
   }

   public int getAgentIndex() {
      return _agentIndex;
   }

   public int getImpressions() {
      return _impressions;
   }

   public int getPromotedImpressions() {
      return _promotedImpressions;
   }

   public int getImpressionsUB() {
      return _impressionsUB;
   }

   public boolean getPromotionEligibilityVerified() {
      return _promotionEligibilityVerified;
   }

   public boolean getHitOurBudget() {
	   return _hitOurBudget;
   }
	
   public double[] getAgentImpressionDistributionMean() {
      return _agentImpressionDistributionMean;
   }

   public double[] getAgentImpressionDistributionStdev() {
      return _agentImpressionDistributionStdev;
   }

   public boolean[] isPadded() {
      boolean[] padded = new boolean[_advetisers];
      for (int i = 0; i < _advetisers; i++) {
         if (_agentIds[i] >= MIN_PADDED_AGENT_ID) {
            padded[i] = true;
         }
      }
      return padded;
   }
   
   //The ith index contains the index of the agent that started in the ith position.
   //TODO: This is confusing, since for all the other "get" arrays, the ith index refers to the ith agent.
   public int[] getInitialPositionOrdering() {
	   return _initialPosition;
   }

   /**
    * If any initial positions are not known (i.e. they are -1), return false.
    * @return
    */
   public boolean allInitialPositionsKnown() {
	   for (int i=0; i<_advetisers; i++) {
		   if (_initialPosition[i] == -1 || Double.isNaN(_initialPosition[i])) return false;
	   }
	   return true;
   }

   public int[] getBidOrder(QAData data) {
      double[] bids = new double[_advetisers];
      int[] bidOrder = new int[_advetisers];
      for (int i = 0; i < _advetisers; i++) {
         bids[i] = data._agentInfo[_agentIds[i] - 1].bid;
         bidOrder[i] = i;
      }

      sortListsDecending(bidOrder, bids);

      //System.out.println("Bid order "+Arrays.toString(bidOrder));
      //System.out.println("Bid value "+Arrays.toString(bids));

      return bidOrder;
   }

   private boolean feasibleOrder(int[] order) {
      for (int i = 0; i < order.length; i++) {
         int startPos = Math.min(i + 1, _slots);
         if (startPos < _avgPos[order[i]]) {
            return false;
         }
      }
      return true;
   }

   public static int[] getAvgPosOrder(double[] averagePositions) {
      double[] pos = new double[averagePositions.length];
      int[] posOrder = new int[averagePositions.length];
      for (int i = 0; i < averagePositions.length; i++) {
//         pos[i] = -_avgPos[i];
    	  pos[i] = -averagePositions[i];
         posOrder[i] = i;
      }

      sortListsDecending(posOrder, pos);

      //System.out.println("Pos order "+Arrays.toString(posOrder));
      //System.out.println("Pos value "+Arrays.toString(pos));

      //not nessissary in general, but makes results consistent with the comet model
      sortTiesAccending(posOrder, pos);

      //System.out.println("Pos order (break ties) "+Arrays.toString(posOrder));
      //System.out.println("Pos value (break ties) "+Arrays.toString(pos));


      return posOrder;
   }

   public static int[] getCarletonOrder(double[] averagePositions, int numSlots) {
	   int numAdvertisers = averagePositions.length;
      int[] avgPosOrder = getAvgPosOrder(averagePositions);
      boolean[] avdAssigned = new boolean[numAdvertisers];
      //boolean[] posAssigned = new boolean[_advetisers];
      int[] carletonOrder = new int[numAdvertisers];
      for (int i = 0; i < numAdvertisers; i++) {
         avdAssigned[i] = false;
         carletonOrder[i] = -1;
      }

      ArrayList<HashSet<Integer>> advWholePos = new ArrayList<HashSet<Integer>>();
      for (int i = 0; i < numSlots; i++) {
         advWholePos.add(i, new HashSet<Integer>());
      }

      for (int i = 0; i < numAdvertisers; i++) {
         if ((((int) (averagePositions[i] * 100000) % 100000)) == 0) {
            int slot = (int) averagePositions[i];
            if (slot < numSlots) { //very important not to keep the last slot
            	//System.out.println("slot=" + slot + ", slots=" + numSlots);
               HashSet<Integer> advs = advWholePos.get(slot - 1);
               advs.add(i);
            }
         }
      }

      for (int i = 0; i < numSlots - 1; i++) { //this should hold as long as we don't consider the last slot
         HashSet<Integer> advs = advWholePos.get(i);
         //assert(advs.size() <= 1) : "this may need to go away in new game data";
         if (advs.size() > 0) {
            int adv = advs.iterator().next(); //no good idea on how to pick, just do random.

            assert (carletonOrder[i] < 0);
            carletonOrder[i] = adv;
            assert (!avdAssigned[adv]);
            avdAssigned[adv] = true;
         }
      }

      for (int i = 0; i < numAdvertisers; i++) {
         int a = avgPosOrder[i];
         if (!avdAssigned[a]) {
            for (int j = 0; j < numAdvertisers; j++) {
               if (carletonOrder[j] < 0) {
                  carletonOrder[j] = a;
                  avdAssigned[a] = true;
                  break;
               }
            }
         }
      }


      return carletonOrder;
   }

   public int[] getTrueImpressions(QAData data) {
      int[] impressions = new int[_advetisers];
      for (int i = 0; i < _advetisers; i++) {
         impressions[i] = data._agentInfo[_agentIds[i] - 1].impressions;
      }

      return impressions;
   }

   private static void sortListsDecending(int[] ids, double[] vals) {
      assert (ids.length == vals.length);
      int length = ids.length;

      for (int i = 0; i < length; i++) {
         for (int j = i + 1; j < length; j++) {
            if (vals[i] < vals[j]) {
               double tempVal = vals[i];
               int tempId = ids[i];

               vals[i] = vals[j];
               ids[i] = ids[j];

               vals[j] = tempVal;
               ids[j] = tempId;
            }
         }
      }
   }

   /**
    * sorts ties by accending agent id
    * This has 0 impact on the algorithm, it was added just to have identical results
    * with the comet model
    *
    * @param ids
    * @param vals
    */
   private static void sortTiesAccending(int[] ids, double[] vals) {
      assert (ids.length == vals.length);
      int length = ids.length;

      for (int i = 0; i < length; i++) {
         for (int j = i + 1; j < length; j++) {
            if (vals[i] == vals[j] && ids[i] > ids[j]) {
               double tempVal = vals[i];
               int tempId = ids[i];

               vals[i] = vals[j];
               ids[i] = ids[j];

               vals[j] = tempVal;
               ids[j] = tempId;
            }
         }
      }
   }


   public String toString() {
      String temp = "";
      temp += "Slots: " + _slots + "\n";
      temp += "Advertisers: " + _advetisers + "\n";
      temp += Arrays.toString(_agentIds) + "\n";
      temp += Arrays.toString(_avgPos) + "\n";
      temp += "index: " + _agentIndex + "\n";
      temp += "impressions: " + _impressions + "\n";
      return temp;
   }


}
