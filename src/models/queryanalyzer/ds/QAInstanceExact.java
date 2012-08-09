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
   public double[] getExactAvgPos() {return _avgPos;}
   
   public int[] getBidOrder(QADataExactOnly data) {
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
   
   
   public int[] getTrueImpressions(QADataExactOnly data) {
      int[] impressions = new int[_advetisers];
      for (int i = 0; i < _advetisers; i++) {
         impressions[i] = data._agentInfo[_agentIds[i] - 1].impressions;
      }
      return impressions;
   }
   
   
   /**
    * This method converts a QAInstanceExactOnly instance into a QAInstanceAll, 
    * by using with dummy data.  This is intended for testing only!
    * @return
    */
   public QAInstanceAll makeQAInstanceAll(){
      double[] dummySampledAvgPositions = new double[_agentIds.length];
      Arrays.fill(dummySampledAvgPositions, -1);

      //No distinguishing between promoted and unpromoted slots
      int numPromotedSlots = -1;

      //Not considering how many promoted impressions we saw
      int numPromotedImpressions = -1;

      //Not considering whether our bid is high enough to be in a promoted slot
      boolean promotionEligibiltyVerified = false;

      //Not using any prior knowledge about agent impressions
      double[] agentImpressionDistributionMean = new double[_agentIds.length];
      double[] agentImpressionDistributionStdev = new double[_agentIds.length];
      //--------------
      
      return new QAInstanceAll(_slots, numPromotedSlots, _advetisers,
            _avgPos, dummySampledAvgPositions, _agentIds, _agentIndex,
            _impressions, numPromotedImpressions, _impressionsUB,
            true, promotionEligibiltyVerified,
            promotionEligibiltyVerified, agentImpressionDistributionMean, agentImpressionDistributionStdev, true, _agentIds, _agentNames);
   }

   public String toString() {
      String temp =  super.toString();
      temp += "\tavgPos=" + Arrays.toString(_avgPos) + "\n";
      return temp;
   }

}
