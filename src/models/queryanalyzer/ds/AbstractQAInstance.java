package models.queryanalyzer.ds;

import java.util.Arrays;

public abstract class AbstractQAInstance {
	   protected int _slots;
	   protected int _advetisers;
	   protected int[] _agentIds;
	   protected int _agentIndex;
	   protected int _impressions;
	   protected int _impressionsUB;
	   protected String[] _agentNames;

	   protected boolean _hasImpressionPrior;
	   protected double[] _agentImpressionDistributionMean; //prior on agent impressions
	   protected double[] _agentImpressionDistributionStdev; //prior on agent impressions
	   
	   
	   protected AbstractQAInstance(int slots, int advetisers, int[] agentIds, int agentIndex,
	                     int impressions, int impressionsUB, String[] agentNames) {
	      assert (agentIds.length == advetisers);
	      assert (agentNames.length == advetisers);
	      assert (advetisers == 0 || (advetisers > agentIndex && agentIndex >= 0));
	      
	      _slots = slots;
	      _advetisers = advetisers;
	      _agentIds = agentIds;
	      _agentIndex = agentIndex;
	      _impressions = impressions;
	      _impressionsUB = impressionsUB;
	      _hasImpressionPrior = false;
	      
	      _agentNames = agentNames;
	   }

	   public int getNumSlots() { return _slots; }
	   public int getNumAdvetisers() { return _advetisers; }
	   public int[] getAgentIds() { return _agentIds; }
	   public int getAgentIndex() { return _agentIndex; }
	   public int getImpressions() { return _impressions; }
	   public int getImpressionsUB() { return _impressionsUB; }

	   //returns some form of average positions (sampled or exact) for use in ranking agents
	   public abstract double[] getAvgPos();
	   
	   public boolean hasImpressionPrior() { return _hasImpressionPrior; }
	   public double[] getAgentImpressionDistributionMean() {
		  if(_hasImpressionPrior){
			  return _agentImpressionDistributionMean;
		  } else {
			  return null;
		  }
	   }

	   public double[] getAgentImpressionDistributionStdev() {
		   if(_hasImpressionPrior){
			  return _agentImpressionDistributionStdev;
		   } else {
			   return null; 
		   }
	   }

	   
	   public int[] getTrueImpressions(QADataExactOnly data) {
	      int[] impressions = new int[_advetisers];
	      for (int i = 0; i < _advetisers; i++) {
	         impressions[i] = data._agentInfo[_agentIds[i] - 1].impressions;
	      }
	      return impressions;
	   }
	   
	   

	   protected static void sortListsDecending(int[] ids, double[] vals) {
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


	   public String toString() {
	      String temp = "Instance:\n";
	      temp += "\tnumSlots: " + _slots + "\n";
	      temp += "\tnumAgents: " + _advetisers + "\n";
	      temp += "\tagentIds=" + Arrays.toString(_agentIds) + "\n";
	      temp += "\touragentIdx=" + _agentIndex + "\n";
	      temp += "\tourImpressions=" + _impressions + "\n";
	      return temp;
	   }
	   
}
