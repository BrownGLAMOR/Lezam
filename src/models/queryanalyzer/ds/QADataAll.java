package models.queryanalyzer.ds;

import java.util.HashMap;
import java.util.Map;

import se.sics.tasim.is.AgentInfo;

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
   int _ourAgentNum;
   AdvertiserInfo[] _agentInfo;
   Map<Integer,AdvertiserInfo> _agentIdLookup;
   
   int[] _agentIds;
   

   public QADataAll(int agents, int slots,int ourAgentNum, AdvertiserInfo[] agentInfo) {
      assert (agents == agentInfo.length);
      _agents = agents;
      _slots = slots;
      _ourAgentNum=ourAgentNum;
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
	   if(advIndex==-1){
		   return null;
	   }else{
      assert (advIndex < _agents);
      assert (_agentInfo[advIndex].avgPos > 0);
      
      int usedAgents = 0;
      for (int i = 0; i < _agents; i++) {
    	  //System.out.println(_agentInfo[i].avgPos);
    	 if (_agentInfo[i].avgPos > 0) {
            usedAgents++;
         }
      }
      
      // Assign the usedAngent info
     
      AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
      //System.out.println(usedAgents);
      int index = 0;
      int newAdvIndex = 0;
      for (int i = 0; i < _agents; i++) {
         if (_agentInfo[i].avgPos > 0) {
            usedAgentInfo[index] = _agentInfo[i];
           
            if(i == advIndex-1){
            	
            	newAdvIndex = index;
            }
            index++;
         }
      }
      //System.out.println("OurAG NUM2 (new adv idx): "+newAdvIndex);
      
      double[] avgPos = new double[usedAgents];
      _agentIds = new int[usedAgents];
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
         _agentIds[i] = usedAgentInfo[i].id;
         sampledAvgPos[i]=usedAgentInfo[i].sampledAveragePositions;
         agentImpressionDistributionMean[i]=usedAgentInfo[i].impsDistMean;
         agentImpressionDistributionStdev[i]=usedAgentInfo[i].impsDistStdev;
       
         
         //Find the MAX Impression
         impressionsUB = Math.max(impressionsUB, usedAgentInfo[i].impressions);
      }
      impressionsUB = 2 * impressionsUB;
      
      String[] agentnames = new String[usedAgents];
      for (int i = 0; i < usedAgents; i++){
    	  
    	  agentnames[i]=usedAgentInfo[i].agentName;
    	  //agentnames[i] = "a"+i;
      }
      
      
      //No distinguishing between promoted and unpromoted slots
      int numPromotedSlots = 0;

      //Not considering how many promoted impressions we saw
      int numPromotedImpressions = 0;

      //Not considering whether our bid is high enough to be in a promoted slot
      boolean promotionEligibiltyVerified = false;
      
      boolean hitOurBudget = true;

      //Not using any prior knowledge about agent impressions
      //System.out.println(usedAgentInfo.length);
      //System.out.println("Used agent info imps: "+usedAgentInfo[newAdvIndex].impressions);
   
      
		return new QAInstanceAll(_slots, numPromotedSlots, usedAgents,
		          avgPos, sampledAvgPos, _agentIds, newAdvIndex,
		          usedAgentInfo[newAdvIndex].impressions, numPromotedImpressions, impressionsUB,
		          true, promotionEligibiltyVerified, hitOurBudget,agentImpressionDistributionMean, 
		          agentImpressionDistributionStdev, true, _agentIds, agentnames, getTotalImpsSeenFirstSlot());
	   }
	
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
	           
	            //System.out.println(i);
	            if(i == advIndex-1){
	            	newAdvIndex = index;
	            }
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
	
	public int getOurAgentId(){
		
		return this._ourAgentNum;
	}
   
   public String toString() {
      String temp = "";
      temp += "Slots: " + _slots + "\n";
      temp += "Agents: " + _agents + "\n";
      temp += "OurAgentNum:"+ _ourAgentNum +"\n"; 
      for (int i = 0; i < _agentInfo.length; i++) {
         temp += _agentInfo[i].toString() + "\n";
      }
      return temp;
   }


   public int getOurAgentNum(){
	   return _ourAgentNum;
   }

   public int getOtherAgentNum(String name){
	   for(int i=0;i<_agentInfo.length;i++){
		   if(_agentInfo[i].agentName.compareTo(name)==0){
			   return i;
		   }
	   }
	   return -1;
   }
	public QAInstanceSampled buildSampledInstance(int advIndex, int precision) {
		assert (advIndex < _agents);
	      assert (_agentInfo[advIndex].avgPos > 0) : "infeasible advertiser";
	      
	      int usedAgents = 0;
	      for (int i = 0; i < _agents; i++) {
	    	  //System.out.println("Samp"+_agentInfo[i].avgPos);
	         if (_agentInfo[i].avgPos >= 0) {
	        	 //System.out.println("Samp inc");
	            usedAgents++;
	         }
	      }
	      
	      // Assign the usedAngent info
	      AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
	      int index = 0;
	      int newAdvIndex = 0;
	      for (int i = 0; i < _agents; i++) {
	    
	         if (_agentInfo[i].avgPos >= 0) {
	           
	            //System.out.println(i);
	            if(i == advIndex-1){
	            	newAdvIndex = index;
	            }
	            usedAgentInfo[index] = _agentInfo[i];
	            index++;
	         }
	      }

	      
	      double accuracy = Math.pow(10, -precision);
	      //System.out.println(accuracy);
	      
	      double[] avgPos = new double[usedAgents];
	      double[] avgPosLB = new double[usedAgents];
	      double[] avgPosUB = new double[usedAgents];
	      int[] agentIds = new int[usedAgents];
	      int impressionsUB = 0;

	      for (int i = 0; i < usedAgents; i++) {
	    	 if(i == newAdvIndex){
    			avgPos[i] = usedAgentInfo[i].avgPos;
	         	avgPosLB[i] = avgPos[i];
	         	avgPosUB[i] = avgPos[i];	 
	    	 } else {
	    		avgPos[i] = truncate(usedAgentInfo[i].avgPos, precision);
	         	avgPosLB[i] = avgPos[i];
	         	avgPosUB[i] = avgPos[i]+accuracy;
	         	
	         	//useful for sanity checking
	    		 //avgPos[i] = usedAgentInfo[i].avgPos;
	    		 //double tmp = truncate(usedAgentInfo[i].avgPos, precision);
		         //avgPosLB[i] = tmp;
		         //avgPosUB[i] = tmp+accuracy;
	    	 }
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
	      
	      return new QAInstanceSampled(_slots, usedAgents, agentIds,  newAdvIndex, usedAgentInfo[newAdvIndex].impressions, impressionsUB, agentnames, avgPos, avgPosLB, avgPosUB);
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
	  *
	  * AdvertiserInfo[] _agentInfo;
	  * Map<Integer,AdvertiserInfo> _agentIdLookup;
	  * -gnthomps
	   */
	
	//TODO lookup with IDS
	   public int[][] greedyAssign(int[] agentIDs) {
		   //num agents by num slots array
	      int[][] impressionsBySlot = new int[agentIDs.length][_slots];
	      
	      //slots started at
	      int[] slotStart = new int[_slots];
	      int a;
	      // gets the original ordering of bids for a set of agent ids
	      int[] order = getBidOrder(agentIDs);
	      
	      //array where ith entry is agent_ID=i num impressions
	      int[] impressions = getTrueImpressions(agentIDs);
	      //System.out.println("L: "+order.length+" agents: "+agentIDs.length);
	      for (int i = 0; i < agentIDs.length; ++i) {

	         a = order[i];
	         //System.out.println(a);
	         int remainingImp = impressions[a];
	         //System.out.println("remaining impressions "+ impressions[a]);
	         for (int s = Math.min(i + 1, _slots) - 1; s >= 0; --s) {
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
	
	private double truncate(double value, int places) {
	    double multiplier = Math.pow(10, places);
	    //System.out.println(places+" - "+multiplier);
	    //System.out.println(value+" - "+multiplier * value+" - "+ Math.floor(multiplier * value));
	    return Math.floor(multiplier * value) / multiplier;
	}

	public double[] getBids(int[] agentIds) {
		int length = agentIds.length;
		double[] bids = new double[length];
		int[] bidOrder = new int[length];
      
      for (int i = 0; i < length; i++) {
         bids[i] = _agentIdLookup.get(agentIds[i]).bid;
         bidOrder[i] = i;
      }
     
      sortListsDecending(bidOrder, bids);

      return bids;
	}
	
	public boolean isProbing(){
		for(int i=0; i<_agentInfo.length; i++){
			if(_agentInfo[i].impressions<=15){
				return true;
			}
		}
		return false;
		
	}
	
	public boolean hasLongTermPlayer(){
		int maxSoFar = 0;
		int secondMaxSoFar = 0;
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].impressions>maxSoFar){
				maxSoFar = _agentInfo[i].impressions;
			}else if(_agentInfo[i].impressions>secondMaxSoFar){
				secondMaxSoFar = _agentInfo[i].impressions;
			}
		}
		if(maxSoFar-secondMaxSoFar > 50){
			return true;
		}else{
			return false;
		}
		
	}
	
	public boolean isAgentIn(String agent){
		
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].agentName.compareTo(agent)==0){
				return true;
			}
		}
		return false;
		
	}
	
	public boolean isIntegerPosition(String agent){
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].agentName.compareTo(agent)==0){
				if(_agentInfo[i].avgPos == Math.floor(_agentInfo[i].avgPos)){
					return true;
				}
			}
		}
		return false;
		
	}
	
	public double getPercentImpressionsSeen(String agent){
		double max = 0.0;
		double imps = 0.0;
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].agentName.compareTo(agent)==0){
				imps = _agentInfo[i].impressions;
			}
			if(_agentInfo[i].impressions> max){
				max = _agentInfo[i].impressions;
			}
		}
		double temp = imps/max;
		//System.out.println("imps: "+imps+" max: "+max+"Perc: "+temp);
		return temp;
		
	}

	public boolean isInTopSlot(String agent) {
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].agentName.compareTo(agent)==0){
				if(_agentInfo[i].avgPos == 1){
					return true;
				}
			}
		}
		return false;
	}

	public boolean isProbing(String name) {
		for(int i=0; i<_agentInfo.length; i++){
			if(_agentInfo[i].impressions<=15&& _agentInfo[i].agentName.compareToIgnoreCase(name)==0){
				return true;
			}
		}
		return false;
	}

	public boolean numLongTerm(String name) {
		int maxSoFar = 0;
		int secondMaxSoFar = 0;
		String agentName="";
		for(int i=0; i<_agentInfo.length;i++){
			if(_agentInfo[i].impressions>maxSoFar){
				maxSoFar = _agentInfo[i].impressions;
				agentName = _agentInfo[i].agentName;
			}else if(_agentInfo[i].impressions>secondMaxSoFar){
				secondMaxSoFar = _agentInfo[i].impressions;
			}
		}
		if(maxSoFar-secondMaxSoFar > 50 && agentName.compareToIgnoreCase(name)==0){
			return true;
		}else{
			return false;
		}
	}
	
	
	/*
	    * Adding to calc total Imps seen in slot 0
	    * Betsy Hilliard
	    * 
	    */
	   public int getTotalImpsSeenFirstSlot(){
		 return getMaxImps(_slots, _agents, 
				getBidOrder(_agentIds), getTrueImpressions(_agentIds));
	   }
	   
	   public int getMaxImps(int slots, int agents, int[] order, int[] impressions) {
		      int[][] impressionsBySlot = new int[agents][slots];

		      int[] slotStart = new int[slots];
		      int a;
		      
		      //System.out.println("Agents: "+agents);
		      for (int i = 0; i < order.length; i++) {
		    	   //System.out.println("i: "+i+" order length: "+order.length);
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

		      int totImps = 0;
		      for(int i = 0; i < impressionsBySlot.length; i++) {
		         totImps += impressionsBySlot[i][0];
		      }

		      return totImps;
		   }




}