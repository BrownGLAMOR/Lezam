package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionAndRankEstimator;
import models.queryanalyzer.iep.LDSImpressionAndRankEstimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;


public class CarletonQueryAnalyzer extends AbstractQueryAnalyzer {

   private HashMap<Query, ArrayList<IEResult>> _allResults;
   private HashMap<Query, ArrayList<int[]>> _allAgentIDs;
   private HashMap<Query, ArrayList<String[]>> _allAgentNames;

   private HashMap<Query, ArrayList<int[][]>> _allImpRanges;
   private Set<Query> _querySpace;
   private HashMap<String, Integer> _advToIdx;
   private ArrayList<String> _advertisers;
   private String _ourAdvertiser;
   public final static int NUM_SLOTS = 5;
   public int NUM_ITERATIONS_1 = 10;
   public int NUM_ITERATIONS_2 = 10;
   private boolean REPORT_FULLPOS_FORSELF = true;
   private boolean _isSampled;

   public CarletonQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser, int numIters1, int numIters2, boolean selfAvgPosFlag, boolean isSampled) {
      _querySpace = querySpace;

      NUM_ITERATIONS_1 = numIters1;
      NUM_ITERATIONS_2 = numIters2;
      REPORT_FULLPOS_FORSELF = selfAvgPosFlag;
      _isSampled = isSampled;

      _allResults = new HashMap<Query, ArrayList<IEResult>>();
      _allImpRanges = new HashMap<Query, ArrayList<int[][]>>();
      _allAgentIDs = new HashMap<Query, ArrayList<int[]>>();
      _allAgentNames = new HashMap<Query, ArrayList<String[]>>();
      for (Query q : _querySpace) {
         ArrayList<IEResult> resultsList = new ArrayList<IEResult>();
         _allResults.put(q, resultsList);

         ArrayList<int[][]> impRanges = new ArrayList<int[][]>();
         _allImpRanges.put(q, impRanges);

         ArrayList<int[]> agentIDs = new ArrayList<int[]>();
         _allAgentIDs.put(q, agentIDs);

         ArrayList<String[]> agentNames = new ArrayList<String[]>();
         _allAgentNames.put(q, agentNames);
      }

      _advertisers = advertisers;

      _ourAdvertiser = ourAdvertiser;

      _advToIdx = new HashMap<String, Integer>();
      for (int i = 0; i < advertisers.size(); i++) {
         _advToIdx.put(advertisers.get(i), i);
      }

   }


   @Override
   public int getImpressionsPrediction(Query q, String adv) {
      int size = _allResults.get(q).size();
      System.out.println("NAMES: " + _advToIdx.keySet());
      if (size > 0) {
         int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size() - 1);
         for (int i = 0; i < agentIDs.length; i++) {
            if (_advToIdx.get(adv) == agentIDs[i]) {
               return _allResults.get(q).get(size - 1).getSol()[i];
            }
         }
      }
      return 0;
   }


   @Override
   public int[] getImpressionsPrediction(Query q) {
      //For each advertiser, get the impressions predictions.
      //If an advertiser doesn't appear, their impressions prediction is 0.
      //Create the array that we'll return.
      int[] impressionsPredictions = new int[_advertisers.size()];

      //Get most recent impressions predictions (IEResult)
      int latestResultIdx = _allResults.get(q).size() - 1;
      int[] impressionsPredictionsReduced = _allResults.get(q).get(latestResultIdx).getSol();

      System.out.println("Impressions predictions reduced q=" + q + "\t" + Arrays.toString(impressionsPredictionsReduced));
      //Get names of agents that appear in the IEResult
      String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

      //Get the actual predictions from IEResult
      for (int a = 0; a < _advertisers.size(); a++) {
         String adv = _advertisers.get(a);

         for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
            if (adv.equals(agentNamesReduced[aReduced])) {
               impressionsPredictions[a] = impressionsPredictionsReduced[aReduced];
            }
         }
      }
      return impressionsPredictions;
   }


   @Override
   public int getOrderPrediction(Query q, String adv) {
      int size = _allResults.get(q).size();
      if (size > 0) {
         int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size() - 1);
         for (int i = 0; i < agentIDs.length; i++) {
            if (_advToIdx.get(adv) == agentIDs[i]) {
               return _allResults.get(q).get(size - 1).getOrder()[i];
            }
         }
      }
      return -1;
   }


   @Override
   public int[] getOrderPrediction(Query q) {
      //For each advertiser, get the order predictions.
      //If an advertiser doesn't appear, their order prediction is -1.
      //Create the array that we'll return.
      int[] orderPredictions = new int[_advertisers.size()];
      Arrays.fill(orderPredictions, -1);

      //Get most recent impressions predictions (IEResult)
      int latestResultIdx = _allResults.get(q).size() - 1;
      int[] orderPredictionsReduced = _allResults.get(q).get(latestResultIdx).getOrder();

      //Get names of agents that appear in the IEResult
      String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

      //Get the actual predictions from IEResult
      for (int a = 0; a < _advertisers.size(); a++) {
         String adv = _advertisers.get(a);

         for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
            if (adv.equals(agentNamesReduced[aReduced])) {
               orderPredictions[a] = orderPredictionsReduced[aReduced];
            }
         }
      }
      return orderPredictions;
   }


//	@Override
//	public int[] getOrderPrediction(Query q) {
//		int size = _allResults.get(q).size();
//		if(size > 0) {
//			return _allResults.get(q).get(size-1).getOrder();
//		}
//		return null;
//	}


   @Override
   public int[] getImpressionRangePrediction(Query q, String adv) {
      int size = _allResults.get(q).size();
      if (size > 0) {
         int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size() - 1);
         for (int i = 0; i < agentIDs.length; i++) {
            if (_advToIdx.get(adv) == agentIDs[i]) {
               return _allImpRanges.get(q).get(size - 1)[i];
            }
         }
      }
      return null;
   }

   @Override
   public int[][] getImpressionRangePrediction(Query q) {
      int size = _allResults.get(q).size();
      if (size > 0) {
         return _allImpRanges.get(q).get(size - 1);
      }
      return null;
   }


   @Override
   public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle, HashMap<Query, Integer> maxImps) {


      //For each query (independently)
      for (Query q : _querySpace) {

         //--------------
         //Initialization
         //--------------

         // Load data from the query report (specifically, the average position for each advertiser)
         ArrayList<Double> allAvgPos = new ArrayList<Double>();
         ArrayList<Integer> agentIds = new ArrayList<Integer>();
         ArrayList<String> agentNames = new ArrayList<String>();
         int ourIdx = 0;
         for (int i = 0; i < _advertisers.size(); i++) {
            double avgPos;
            if (_advertisers.get(i).equals(_ourAdvertiser)) {
               if (REPORT_FULLPOS_FORSELF) {
                  avgPos = queryReport.getPosition(q);
               } else {
                  avgPos = queryReport.getPosition(q, "adv" + (i + 1));
               }
               ourIdx = i;
            } else {
               avgPos = queryReport.getPosition(q, "adv" + (i + 1));
            }

            if (!Double.isNaN(avgPos)) {
               agentIds.add(i);
               agentNames.add(_advertisers.get(i));
               allAvgPos.add(avgPos);
            }
         }

         System.out.println("q=" + q + "\tavgPos=" + allAvgPos + "\tagentNames=" + agentNames);


         //			System.out.println(allAvgPos);
         //			System.out.println(agentIds);
         //			System.out.println(_ourAdvertiser);

         // Create arrays with query report data (instead of arrayLists)
         double[] allAvgPosArr = new double[allAvgPos.size()];
         for (int i = 0; i < allAvgPosArr.length; i++) {
            allAvgPosArr[i] = allAvgPos.get(i);
         }
         int[] agentIdsArr = new int[agentIds.size()];
         int ourNewIdx = 0;
         for (int i = 0; i < agentIdsArr.length; i++) {
            agentIdsArr[i] = agentIds.get(i);
            if (agentIds.get(i) == ourIdx) {
               ourNewIdx = i;
            }
         }

         //Create array of agent names that are actually considered in this instance.
         String[] agentNamesArr = new String[agentNames.size()];
         agentNamesArr = agentNames.toArray(agentNamesArr);


         //--------------
         //sodomka: 3/1/11: I added some things to the QAInstance that Carleton doesn't use. Let's just add dummy values.
         //(We could have just added a default constructor to take care of these things, but this
         // should make it more visible that these are possible extensions to CarletonQueryAnalyzer).

         //No distinguishing between exact and sampled positions.
         double[] dummySampledAvgPositions = new double[agentIds.size()];
         Arrays.fill(dummySampledAvgPositions, -1);

         //No distinguishing between promoted and unpromoted slots
         int numPromotedSlots = -1;

         //Not considering how many promoted impressions we saw
         int numPromotedImpressions = -1;

         //Not considering whether our bid is high enough to be in a promoted slot
         boolean promotionEligibiltyVerified = false;

         //Not using any prior knowledge about agent impressions
         double[] agentImpressionDistributionMean = new double[agentIds.size()];
         double[] agentImpressionDistributionStdev = new double[agentIds.size()];

         //--------------


         //Ideally:
         //-create QAInstance
         //-create ImpressionAndRankEstimator (either LDS or MIP)
         //-Have ImpressionAndRankEstimator find best solution
         //-return best solution

         QAInstance inst = new QAInstance(NUM_SLOTS, numPromotedSlots, allAvgPos.size(),
                                          allAvgPosArr, dummySampledAvgPositions, agentIdsArr, ourNewIdx,
                                          queryReport.getImpressions(q), numPromotedImpressions, maxImps.get(q),
                                          true, promotionEligibiltyVerified,
                                          agentImpressionDistributionMean, agentImpressionDistributionStdev, _isSampled);


         ImpressionAndRankEstimator estimator = new LDSImpressionAndRankEstimator();
         IEResult bestSol = estimator.getBestSolution(inst);


         _allResults.get(q).add(bestSol);
         _allImpRanges.get(q).add(greedyAssign(5, bestSol.getSol().length, bestSol.getOrder(), bestSol.getSol()));

         _allAgentNames.get(q).add(agentNamesArr);
         _allAgentIDs.get(q).add(agentIdsArr); //these are the IDs of each agent that was in the QAInstance for the given query. Corresponds to indices of IEResults

         System.out.println("Done solving. " + bestSol + "\t agentNames:" + Arrays.toString(agentNamesArr));
      }
      return true;
   }

   @Override
   public String toString() {
      return "CarletonQueryAnalyzer(" + NUM_ITERATIONS_1 + "," + NUM_ITERATIONS_2 + ")";
   }

   @Override
   public AbstractModel getCopy() {
      return new CarletonQueryAnalyzer(_querySpace, _advertisers, _ourAdvertiser, NUM_ITERATIONS_1, NUM_ITERATIONS_2, REPORT_FULLPOS_FORSELF, _isSampled);
   }

   @Override
   public void setAdvertiser(String ourAdv) {
      _ourAdvertiser = ourAdv;
   }

}
