package models.queryanalyzer.ds;

import java.util.Arrays;

public class QAInstanceExact extends AbstractQAInstance {
   double[] _avgPos;
   
   public QAInstanceExact(int slots, int advetisers, int[] agentIds, int agentIndex,
                     int impressions, int impressionsUB, String[] agentNames, double[] avgPos) {
	   super(slots, advetisers, agentIds, agentIndex, impressions, impressionsUB, agentNames);
	   
	   assert (avgPos.length == _advetisers);
	   _avgPos = avgPos;
   }

   public double[] getAvgPos() {return _avgPos;}
   
   public String toString() {
      String temp =  super.toString();
      temp += "\tavgPos=" + Arrays.toString(_avgPos) + "\n";
      return temp;
   }

}
