package models.queryanalyzer.ds;

import java.util.HashMap;
import java.util.Map;

/**
 * The QAData class that only reads new files 
 * After the TAC:AA game integrated sampled average positions 
 * 
 * @author cc26
 *
 */
public class QADataAll extends AbstractQAData {
   int _agents;
   int _slots;
   AdvertiserInfo[] _agentInfo;
   Map<Integer,AdvertiserInfo> _agentIdLookup;
   

   public QADataAll(int agents, int slots, AdvertiserInfo[] agentInfo) {
      assert (agents == agentInfo.length);
      _agents = agents;
      _slots = slots;
      // Agent info contains agent id, avg position, impression(total impression?), bid, budget
      _agentInfo = agentInfo;
      
      _agentIdLookup = new HashMap<Integer,AdvertiserInfo>();
      for(AdvertiserInfo info : _agentInfo){
    	  _agentIdLookup.put(info.id, info);
      }
   }

   /**
    * @param advIndex the value of adv can be a bit missleading.  This is the index of the i-th advetiser after non-participants are dropped
    * @return
    */
   public QAInstanceAll buildInstances(int advIndex) {
      assert (advIndex < _agents);
      assert (_agentInfo[advIndex].avgPos > 0);
      
      int usedAgents = 0;
      for (int i = 0; i < _agents; i++) {
    	 if (_agentInfo[i].avgPos > 0) {
            usedAgents++;
         }
      }
      
      // Assign the usedAngent info
      AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
      int index = 0;
      int newAdvIndex = 0;
      for (int i = 0; i < _agents; i++) {
         if (_agentInfo[i].avgPos > 0) {
            usedAgentInfo[index] = _agentInfo[i];
            if(i == advIndex){
            	newAdvIndex = index;
            }
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
    	 if(i == newAdvIndex){
    		 avgPos[i] = usedAgentInfo[i].avgPos;
    	 } else {
    		 avgPos[i] = -1;
    	 }
         agentIds[i] = usedAgentInfo[i].id;
         sampledAvgPos[i]=usedAgentInfo[i].sampledAveragePositions;
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
      int numPromotedSlots = -1;

      //Not considering how many promoted impressions we saw
      int numPromotedImpressions = -1;

      //Not considering whether our bid is high enough to be in a promoted slot
      boolean promotionEligibiltyVerified = false;

      //Not using any prior knowledge about agent impressions

      
   
      return new QAInstanceAll(_slots, numPromotedSlots, usedAgents,
              avgPos, sampledAvgPos, agentIds, newAdvIndex,
              usedAgentInfo[newAdvIndex].impressions, numPromotedImpressions, impressionsUB,
              true, promotionEligibiltyVerified,
              promotionEligibiltyVerified, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, agentIds, agentnames);
   }

   public QAInstanceExact buildExactInstance(int advIndex) {
	      assert (advIndex < _agents);
	      assert (_agentInfo[advIndex].avgPos > 0);
	      
	      int usedAgents = 0;
	      for (int i = 0; i < _agents; i++) {
	    	 
	         if (_agentInfo[i].avgPos >= 0) {
	            usedAgents++;
	         }
	      }
	      
	      // Assign the usedAngent info
	      AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
	      int index = 0;
	      int newAdvIndex = 0;
	      for (int i = 0; i < _agents; i++) {
	         if (_agentInfo[i].avgPos >= 0) {
	            usedAgentInfo[index] = _agentInfo[i];
	            if(i == advIndex){
	            	newAdvIndex = index;
	            }
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
	      
	      String[] agentnames = new String[usedAgents];
	      for (int i = 0; i < usedAgents; i++){
	    	  agentnames[i] = "a"+i;
	      }
	      
	      return new QAInstanceExact(_slots, usedAgents, agentIds,  newAdvIndex, usedAgentInfo[newAdvIndex].impressions, impressionsUB, agentnames, avgPos);
	   }
   
   
	@Override
	public int[] getBidOrder(int[] agentIds) {
		int length = agentIds.length;
		double[] bids = new double[length];
		int[] bidOrder = new int[length];
      
      for (int i = 0; i < length; i++) {
         bids[i] = _agentIdLookup.get(agentIds[i]).bid;
         bidOrder[i] = i;
      }
     
      sortListsDecending(bidOrder, bids);

      return bidOrder;
	}
	
	@Override
	public int[] getTrueImpressions(int[] agentIds) {
		int length = agentIds.length;
		int[] impressions = new int[length];
		for (int i = 0; i < length; i++) {
			impressions[i] = _agentIdLookup.get(agentIds[i]).impressions;
		}
		return impressions;
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