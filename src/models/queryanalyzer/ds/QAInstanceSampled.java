package models.queryanalyzer.ds;

import java.util.Arrays;

/**
 * This class does not implment sampling as it is done in the game (as of 2012)
 * It assumes sampling is just truncation of average position.
 * Classes ending in the name "All" implement sampling as is done in the game.
 * @author cjc
 *
 */
public class QAInstanceSampled extends AbstractQAInstance {
   double[] _avgPos;
   double[] _avgPosLB;
   double[] _avgPosUB;

   
   public QAInstanceSampled(int slots, int advetisers, int[] agentIds, int agentIndex,
                     int impressions, int impressionsUB, String[] agentNames, double[] avgPos, double[] avgPosLB, double[] avgPosUB) {
	   super(slots, advetisers, agentIds, agentIndex, impressions, impressionsUB, agentNames);
	   
	   assert (avgPos.length == _advetisers);
	   _avgPos = avgPos;
	   _avgPosLB = avgPosLB;
	   _avgPosUB = avgPosUB;
   }

   public double[] getAvgPos() {return _avgPos;}
   public double[] getAvgPosLB() {return _avgPosLB;}
   public double[] getAvgPosUB() {return _avgPosUB;}
   
   
   public QAInstanceSampled reorder(int[] order){
	   int agentIndex = -1;
	   for (int i=0; i<order.length; i++) {
		   if (order[i] == _agentIndex) {
			   agentIndex = i;
			   break;
		   }
	   }
	   
	   int[] agentIds = reorder(_agentIds,order);
	   String[] agentNames = reorder(_agentNames,order);   
	   double[] avgPos = reorder(_avgPos,order);
	   double[] avgPosLB = reorder(_avgPosLB,order);
	   double[] avgPosUB = reorder(_avgPosUB,order);
	     
	   return new QAInstanceSampled(_slots, _advetisers, agentIds, agentIndex, _impressions, _impressionsUB, agentNames, avgPos, avgPosLB, avgPosUB);
   }
   
   
   public String toString() {
      String temp =  super.toString();
      temp += "\tavgPos=   " + Arrays.toString(_avgPos) + "\n";
      temp += "\tavgPosLB= " + Arrays.toString(_avgPosLB) + "\n";
      temp += "\tavgPosUB= " + Arrays.toString(_avgPosUB);
      return temp;
   }

}
