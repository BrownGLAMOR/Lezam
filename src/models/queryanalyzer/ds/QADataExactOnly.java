package models.queryanalyzer.ds;

import java.util.Arrays;

/**
 * The QAData class that only reads old files 
 * before the TAC:AA game integrated sampled average positions 
 * 
 * @author cjc
 *
 */
public class QADataExactOnly {
   int _agents;
   int _slots;
   AdvertiserInfo[] _agentInfo;

   public QADataExactOnly(int agents, int slots, AdvertiserInfo[] agentInfo) {
      assert (agents == agentInfo.length);
      _agents = agents;
      _slots = slots;
      // Agent info contains agent id, avg position, impression(total impression?), bid, budget
      _agentInfo = agentInfo;
   }

   /**
    * @param advIndex the value of adv can be a bit missleading.  This is the index of the i-th advetiser after non-participants are dropped
    * @return
    */
   public QAInstanceAll buildInstances(int advIndex) {
      assert (advIndex < _agents);
      int usedAgents = 0;
      for (int i = 0; i < _agents; i++) {
    	 
         if (_agentInfo[i].avgPos >= 0) {
            usedAgents++;
         }
      }
      
      // Assign the usedAngent info
      AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
      int index = 0;
      for (int i = 0; i < _agents; i++) {
         if (_agentInfo[i].avgPos >= 0) {
            usedAgentInfo[index] = _agentInfo[i];
            index++;
         }
      }

      
      double[] avgPos = new double[usedAgents];
      int[] agentIds = new int[usedAgents];
      int impressionsUB = 0;

      for (int i = 0; i < usedAgents; i++) {
         avgPos[i] = usedAgentInfo[i].avgPos;
         agentIds[i] = usedAgentInfo[i].id;
         //impressionsUB += usedAgentInfo[i].impressions;
         
         //Find the MAX Impression
         impressionsUB = Math.max(impressionsUB, usedAgentInfo[i].impressions);
      }
      impressionsUB = 2 * impressionsUB;

      //--------------
      //sodomka: 3/1/11: I added some things to the QAInstance that Carleton doesn't use. Let's just add dummy values.
      //(We could have just added a default constructor to take care of these things, but this
      // should make it more visible that these are possible extensions to CarletonQueryAnalyzer).

      //No distinguishing between exact and sampled positions.
      double[] dummySampledAvgPositions = new double[agentIds.length];
      Arrays.fill(dummySampledAvgPositions, -1);

      //No distinguishing between promoted and unpromoted slots
      int numPromotedSlots = -1;

      //Not considering how many promoted impressions we saw
      int numPromotedImpressions = -1;

      //Not considering whether our bid is high enough to be in a promoted slot
      boolean promotionEligibiltyVerified = false;

      //Not using any prior knowledge about agent impressions
      double[] agentImpressionDistributionMean = new double[agentIds.length];
      double[] agentImpressionDistributionStdev = new double[agentIds.length];

      for (int i = 0; i < agentIds.length; i++) {
    	  //agentImpressionDistributionMean[i] = usedAgentInfo[i].impressions;
      }
      //--------------

      String[] agentname={"a1","a2","a3","a4","a5","a6","a7","a8"};
      
   
      
      
      return new QAInstanceAll(_slots, numPromotedSlots, usedAgents,
                            avgPos, dummySampledAvgPositions, agentIds, advIndex,
                            usedAgentInfo[advIndex].impressions, numPromotedImpressions, impressionsUB,
                            true, promotionEligibiltyVerified,
                            promotionEligibiltyVerified, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, agentIds, agentname);
   }

   public String toString() {
      String temp = "";
      temp += "Slots: " + _slots + "\n";
      temp += "Agents: " + _agents + "\n";
      for (int i = 0; i < _agentInfo.length; i++) {
         temp += _agentInfo[i].toString() + "\n";
      }
      return temp;
   }
}
