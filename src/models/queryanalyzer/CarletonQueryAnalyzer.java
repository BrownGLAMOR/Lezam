package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.*;

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
   private boolean REPORT_FULLPOS_FORSELF = true;
   private boolean _isSampled;

   public CarletonQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser, int numIters1, boolean selfAvgPosFlag, boolean isSampled) {
      _querySpace = querySpace;

      NUM_ITERATIONS_1 = numIters1;
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
   public int getTotImps(Query query) {
      IEResult result = _allResults.get(query).get(_allResults.get(query).size()-1);
      if(result != null) {
         return result.getSlotImpressions()[0];
      }
      else {
         return -1;
      }
   }


   @Override
   public int[] getImpressionsPrediction(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         int[] impressionsPredictionsReduced = result.getSol();

         //Get names of agents that appear in the IEResult
         String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

         //For each advertiser, get the impressions predictions.
         //If an advertiser doesn't appear, their impressions prediction is 0.
         //Create the array that we'll return.
         int[] impressionsPredictions = new int[_advertisers.size()];
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
      else {
         return null;
      }
   }


   @Override
   public int[] getOrderPrediction(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         //Get most recent impressions predictions (IEResult)
         int[] orderPredictionsReduced = result.getOrder();

         //Get names of agents that appear in the IEResult
         String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

         //For each advertiser, get the order predictions.
         //If an advertiser doesn't appear, their order prediction is -1.
         //Create the array that we'll return.
         int[] orderPredictions = new int[_advertisers.size()];
         Arrays.fill(orderPredictions, -1);
         int numAdded = 0;
         for (int a = 0; a < _advertisers.size(); a++) {
            String adv = _advertisers.get(a);

            for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
               if (adv.equals(agentNamesReduced[aReduced])) {
                  orderPredictions[a] = orderPredictionsReduced[aReduced];
                  numAdded++;
               }
            }
         }

         //Fill in those who weren't in the auction in order of agentID
         for (int a = 0; a < _advertisers.size(); a++) {
            if(orderPredictions[a] == -1) {
               orderPredictions[a] = numAdded;
               numAdded++;
            }
         }

         return orderPredictions;
      }
      else {
         return null;
      }
   }

   @Override
   public int[][] getImpressionRangePrediction(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         return _allImpRanges.get(q).get(latestResultIdx);
      }
      else {
         return null;
      }
   }


   @Override
   public boolean updateModel(QueryReport queryReport, BidBundle bidBundle, HashMap<Query, Integer> maxImps) {

      for (Query q : _querySpace) {

         /*
          * For now we can only solve the auctions we were in
          */
         if(!Double.isNaN(queryReport.getPosition(q))) {

            // Load data from the query report (specifically, the average position for each advertiser)
            ArrayList<Double> allAvgPos = new ArrayList<Double>();
            ArrayList<Integer> agentIds = new ArrayList<Integer>();
            ArrayList<String> agentNames = new ArrayList<String>();
            int ourIdx = -1;
            for (int i = 0; i < _advertisers.size(); i++) {
               if (_advertisers.get(i).equals(_ourAdvertiser)) {
                  ourIdx = i;
               }

               double avgPos = queryReport.getPosition(q, "adv" + (i + 1));
               if (!Double.isNaN(avgPos) || i == ourIdx) {
                  agentIds.add(i);
                  agentNames.add(_advertisers.get(i));
                  allAvgPos.add(avgPos);
               }
            }

            // Create arrays with query report data (instead of arrayLists)
            double[] sampledAvgPos = new double[allAvgPos.size()];
            int[] agentIdsArr = new int[agentIds.size()];
            String[] agentNamesArr = new String[agentNames.size()];
            int ourNewIdx = -1;
            for (int i = 0; i < sampledAvgPos.length; i++) {
               sampledAvgPos[i] = allAvgPos.get(i);
               agentIdsArr[i] = agentIds.get(i);
               agentNamesArr[i] = agentNames.get(i);
               if (agentIds.get(i) == ourIdx) {
                  ourNewIdx = i;
               }
            }

            //No distinguishing between exact and sampled positions.
            double[] trueAvgPos = new double[agentIds.size()];
            Arrays.fill(trueAvgPos, -1);
            trueAvgPos[ourNewIdx] = queryReport.getPosition(q);

            //No distinguishing between promoted and unpromoted slots
            int numPromotedSlots = -1;

            //Not considering whether our bid is high enough to be in a promoted slot
            boolean promotionEligibiltyVerified = false;

            //Not considering whether we hit our budget
            boolean hitOurBudget = false;

            //Not using any prior knowledge about agent impressions
            double[] agentImpressionDistributionMean = new double[agentIds.size()];
            double[] agentImpressionDistributionStdev = new double[agentIds.size()];
            Arrays.fill(agentImpressionDistributionMean, -1);
            Arrays.fill(agentImpressionDistributionStdev, -1);

            //We don't know which agentIdx was in the ith position, for any i.
            int[] ordering = new int[agentIds.size()];
            Arrays.fill(ordering, -1);
            //--------------

            boolean padAgents = true;

            QAInstance inst = new QAInstance(NUM_SLOTS, numPromotedSlots, allAvgPos.size(),
                                             trueAvgPos, sampledAvgPos, agentIdsArr, ourNewIdx,
                                             queryReport.getImpressions(q), queryReport.getPromotedImpressions(q), maxImps.get(q),
                                             padAgents, promotionEligibiltyVerified, hitOurBudget,
                                             agentImpressionDistributionMean, agentImpressionDistributionStdev, _isSampled, ordering);

            ImpressionEstimatorSample ie = new ImpressionEstimatorSample(inst);
            ImpressionAndRankEstimator estimator = new LDSImpressionAndRankEstimator(ie);
            IEResult bestSol = estimator.getBestSolution();

            if(bestSol != null ) {
               _allResults.get(q).add(bestSol);
               _allImpRanges.get(q).add(greedyAssign(5, bestSol.getSol().length, bestSol.getOrder(), bestSol.getSol()));

               _allAgentNames.get(q).add(agentNamesArr);
               _allAgentIDs.get(q).add(agentIdsArr); //these are the IDs of each agent that was in the QAInstance for the given query. Corresponds to indices of IEResults

//               System.out.println("Done solving. " + bestSol + "\t agentNames:" + Arrays.toString(agentNamesArr));
            }
            else {
               _allResults.get(q).add(null);
               _allImpRanges.get(q).add(null);
               _allAgentNames.get(q).add(null);
               _allAgentIDs.get(q).add(null);

               System.out.println("********Query Analyzer failed to find a solution************");
            }
         }
         else {
            _allResults.get(q).add(null);
            _allImpRanges.get(q).add(null);
            _allAgentNames.get(q).add(null);
            _allAgentIDs.get(q).add(null);
         }
      }
      return true;
   }

   @Override
   public String toString() {
      return "CarletonQueryAnalyzer(" + NUM_ITERATIONS_1 + ")";
   }

   @Override
   public AbstractModel getCopy() {
      return new CarletonQueryAnalyzer(_querySpace, _advertisers, _ourAdvertiser, NUM_ITERATIONS_1, REPORT_FULLPOS_FORSELF, _isSampled);
   }

   @Override
   public void setAdvertiser(String ourAdv) {
      _ourAdvertiser = ourAdv;
   }

}
