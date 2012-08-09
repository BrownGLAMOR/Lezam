package models.queryanalyzer.ds;

import java.util.Arrays;

/**
 * The QAData class that only reads new files 
 * After the TAC:AA game integrated sampled average positions 
 * 
 * @author cc26
 *
 */
public class QAData {
   int _agents;
   int _slots;
   AdvertiserInfo[] _agentInfo;

   public QAData(int agents, int slots, AdvertiserInfo[] agentInfo) {
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
   public QAInstanceExact buildInstances(int advIndex) {
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
      double[] sampledAvgPos= new double[usedAgents];
      double[] agentImpressionDistributionMean= new double[usedAgents];
      double[] agentImpressionDistributionStdev= new double[usedAgents];
      int impressionsUB = 0;

      for (int i = 0; i < usedAgents; i++) {
         avgPos[i] = usedAgentInfo[i].avgPos;
         agentIds[i] = usedAgentInfo[i].id;
         sampledAvgPos[i]=usedAgentInfo[i].avgPos;
         agentImpressionDistributionMean[i]=usedAgentInfo[i].impsDistMean;
         agentImpressionDistributionStdev[i]=usedAgentInfo[i].impsDistStdev;
       
         
         //Find the MAX Impression
         impressionsUB = Math.max(impressionsUB, usedAgentInfo[i].impressions);
      }
      impressionsUB = 2 * impressionsUB;
      
      String[] agentnames = new String[usedAgents];
      for (int i = 0; i < usedAgents; i++){
    	  agentnames[i] = "a"+i;
      }
      
      
      //No distinguishing between promoted and unpromoted slots
//      int numPromotedSlots = -1;
//
//      //Not considering how many promoted impressions we saw
//      int numPromotedImpressions = -1;
//
//      //Not considering whether our bid is high enough to be in a promoted slot
//      boolean promotionEligibiltyVerified = false;

      //Not using any prior knowledge about agent impressions

      
   
//      return new QAInstanceAll(_slots, numPromotedSlots, usedAgents,
//              avgPos, sampledAvgPos, agentIds, advIndex,
//              usedAgentInfo[advIndex].impressions, numPromotedImpressions, impressionsUB,
//              true, promotionEligibiltyVerified,
//              promotionEligibiltyVerified, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, agentIds, agentnames);
      return new QAInstanceExact(_slots, usedAgents, agentIds,  advIndex, usedAgentInfo[advIndex].impressions, impressionsUB, agentnames, avgPos);
     

    

     

   
      
      
  
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