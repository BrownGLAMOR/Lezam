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
   AdvertiserInfoExactOnly[] _agentInfo;

   public QADataExactOnly(int agents, int slots, AdvertiserInfoExactOnly[] agentInfo) {
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
      AdvertiserInfoExactOnly[] usedAgentInfo = new AdvertiserInfoExactOnly[usedAgents];
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
      
      String[] agentname={"a1","a2","a3","a4","a5","a6","a7","a8"};
      
      return new QAInstanceExact(_slots, usedAgents, agentIds,  advIndex, usedAgentInfo[advIndex].impressions, impressionsUB, agentname, avgPos);
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
