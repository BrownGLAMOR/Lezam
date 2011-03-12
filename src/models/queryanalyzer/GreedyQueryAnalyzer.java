package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class GreedyQueryAnalyzer extends AbstractQueryAnalyzer {

   private HashMap<Query, ArrayList<IEResult>> _allResults;
   private HashMap<Query, ArrayList<int[]>> _allAgentIDs;
   private HashMap<Query, ArrayList<int[][]>> _allImpRanges;
   private Set<Query> _querySpace;
   private HashMap<String, Integer> _advToIdx;
   private ArrayList<String> _advertisers;
   private String _ourAdvertiser;
   public final static int NUM_SLOTS = 5;
   private boolean _isSampled;

   public GreedyQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser, boolean isSampled) {
      _querySpace = querySpace;

      _isSampled = isSampled;

      _allResults = new HashMap<Query, ArrayList<IEResult>>();
      _allImpRanges = new HashMap<Query, ArrayList<int[][]>>();
      _allAgentIDs = new HashMap<Query, ArrayList<int[]>>();
      for (Query q : _querySpace) {
         ArrayList<IEResult> resultsList = new ArrayList<IEResult>();
         _allResults.put(q, resultsList);

         ArrayList<int[][]> impRanges = new ArrayList<int[][]>();
         _allImpRanges.put(q, impRanges);

         ArrayList<int[]> agentIDs = new ArrayList<int[]>();
         _allAgentIDs.put(q, agentIDs);
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
      int size = _allResults.get(q).size();
      if (size > 0) {
         return _allResults.get(q).get(size - 1).getSol();
      }
      return null;
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
      int size = _allResults.get(q).size();
      if (size > 0) {
         return _allResults.get(q).get(size - 1).getOrder();
      }
      return null;
   }

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
      for (Query q : _querySpace) {
         ArrayList<Double> allAvgPos = new ArrayList<Double>();
         ArrayList<Integer> agentIds = new ArrayList<Integer>();
         for (int i = 0; i < _advertisers.size(); i++) {
            double avgPos;
            if (_advertisers.get(i).equals(_ourAdvertiser)) {
               avgPos = queryReport.getPosition(q);
            } else {
               avgPos = queryReport.getPosition(q, "adv" + (i + 1));
            }

            if (!Double.isNaN(avgPos)) {
               agentIds.add(i);
               allAvgPos.add(avgPos);
            }
         }

         double[] allAvgPosArr = new double[allAvgPos.size()];
         for (int i = 0; i < allAvgPosArr.length; i++) {
            allAvgPosArr[i] = allAvgPos.get(i);
         }

         int[] agentIdsArr = new int[agentIds.size()];
         for (int i = 0; i < agentIdsArr.length; i++) {
            agentIdsArr[i] = agentIds.get(i);
         }


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


         QAInstance inst = new QAInstance(NUM_SLOTS, numPromotedSlots, allAvgPos.size(),
                                          allAvgPosArr, dummySampledAvgPositions, agentIdsArr, _advToIdx.get(_ourAdvertiser),
                                          queryReport.getImpressions(q), numPromotedImpressions, maxImps.get(q),
                                          true, promotionEligibiltyVerified,
                                          agentImpressionDistributionMean, agentImpressionDistributionStdev, _isSampled);

         int[] avgPosOrder = inst.getAvgPosOrder();
         IEResult bestSol;
         if (queryReport.getImpressions(q) > 0) {
            if (avgPosOrder.length > 0) {
               ImpressionEstimator ie = new ImpressionEstimator(inst);
               bestSol = ie.search(avgPosOrder);
               if (bestSol == null || bestSol.getSol() == null) {
                  System.out.println(q);
                  int[] imps = new int[avgPosOrder.length];
                  int[] slotimps = new int[NUM_SLOTS];
                  bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
               }
            } else {
               int[] imps = new int[avgPosOrder.length];
               int[] slotimps = new int[NUM_SLOTS];
               bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
            }
         } else {
            int[] imps = new int[avgPosOrder.length];
            int[] slotimps = new int[NUM_SLOTS];
            bestSol = new IEResult(0, imps, avgPosOrder, slotimps);
         }
         _allResults.get(q).add(bestSol);
         _allImpRanges.get(q).add(greedyAssign(5, bestSol.getSol().length, bestSol.getOrder(), bestSol.getSol()));
         _allAgentIDs.get(q).add(agentIdsArr);
      }
      return true;
   }


   @Override
   public AbstractModel getCopy() {
      return new GreedyQueryAnalyzer(_querySpace, _advertisers, _ourAdvertiser, _isSampled);
   }

   @Override
   public void setAdvertiser(String ourAdv) {
      _ourAdvertiser = ourAdv;
   }
}
