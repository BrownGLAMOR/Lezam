package models.budgetEstimator;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;
import java.util.Set;

import static models.paramest.ConstantsAndFunctions.*;

public class BudgetEstimator extends AbstractBudgetEstimator {

   public HashMap<String, HashMap<Query, Double>> _budgetPredictions;
   public HashMap<String,Integer> _agentToIdxMap;
   private Set<Query> _querySpace;

   private int numAdvertisers = 8;
   private double[] _c;

   int _ourAdvIdx;
   int _numSlots;
   int _numPromSlots;

   public BudgetEstimator(Set<Query> querySpace, int ourAdvIdx, int numSlots, int numPromSlots) {
      _querySpace = querySpace;
      _budgetPredictions = new HashMap<String, HashMap<Query, Double>>();
      _ourAdvIdx = ourAdvIdx;
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;
      for (int i = 0; i < numAdvertisers; i++) {
         HashMap<Query, Double> budgets = new HashMap<Query, Double>();
         for (Query q : _querySpace) {
            budgets.put(q, Double.MAX_VALUE);
         }
         _budgetPredictions.put("adv" + (i + 1), budgets);
      }

      _agentToIdxMap = new HashMap<String, Integer>();
      for(int i = 0; i < numAdvertisers; i++) {
         _agentToIdxMap.put("adv"+(i+1),i);
      }
   }

   @Override
   public double getBudgetEstimate(Query q, String advertiser) {
      return _budgetPredictions.get(advertiser).get(q);
   }

   /*
    * TODO
    *   -add promoted reserve bound
    *
    */
   @Override
   public void updateModel(QueryReport queryReport,
                           BidBundle bidBundle,
                           double[] convProbs,
                           HashMap<Query, Double> contProbs,
                           double[] regReserves,
                           HashMap<Query, int[]> allOrders,
                           HashMap<Query, int[]> allImps,
                           HashMap<Query, int[][]> allWaterfalls,
                           HashMap<Query, double[]> allBids,
                           HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> userStates) {

      _c = convProbs.clone();

      for(Query q : _querySpace) {
         int[][] waterfall = allWaterfalls.get(q);

         if(waterfall != null) {
            int[] imps = allImps.get(q);
            int[] startOrder = allOrders.get(q);
            double[] bids = allBids.get(q);

            int qtIdx = queryTypeToInt(q.getType());
            double baseClickPr = _advertiserEffectBoundsAvg[qtIdx];
            double[] prViews = getPrView(q,_numSlots,_numPromSlots,baseClickPr,contProbs.get(q),convProbs[qtIdx],userStates);
            int[] dropoutPoints = getDropoutPoints(imps,startOrder,_numSlots);
            int[][] orders = getOrderMatrix(dropoutPoints, imps, startOrder, waterfall, _numSlots);

            double[] costs = new double[numAdvertisers];
            boolean[] droppedOut = new boolean[numAdvertisers];
            for(int i = 0; i < numAdvertisers; i++) {
               droppedOut[i] = false;
            }

            for(int i = 0; i < dropoutPoints.length; i++) {
               int dropOut = dropoutPoints[i];
               int impsSinceLastDrop;
               if(i > 0) {
                  impsSinceLastDrop = dropOut - dropoutPoints[i-1];
               }
               else {
                  impsSinceLastDrop = dropOut;
               }

               int[] order = orders[i];
               for(int j = 0; j < order.length && j < _numSlots; j++) {
                  int agentIdx = order[j];

                  if(agentIdx < 0) {
                     //This is a padded agent, skip it
                     continue;
                  }

                  int slotImps = waterfall[agentIdx][j];
                  if(slotImps > 0) {
                     //Determine the probability of click in slot j
                     double prClick = prViews[j]*baseClickPr;

                     int agentBelowIdx;
                     if(j < (order.length-1)) {
                        agentBelowIdx = order[j+1];
                     }
                     else {
                        agentBelowIdx = -1;
                     }

                     double cpc;
                     if(agentBelowIdx > -1) {
                        cpc = bids[agentBelowIdx];
                     }
                     else {
                        cpc = regReserves[queryTypeToInt(q.getType())];
                     }

                     /*
                     * We only want to assign the number of imps to this
                     * advertiser as there are before the advertsier drops out
                     */
                     costs[agentIdx] += Math.min(slotImps,impsSinceLastDrop)*prClick*cpc;

                     //Check if the agent dropped out
                     //If they saw more slotImps in this slot they didn't drop
                     if(slotImps == impsSinceLastDrop) {
                        //If they are in the last round they didn't drop
                        if(i < dropoutPoints.length-1) {
                           int[] nextOrder = orders[i+1];
                           boolean inNextRound = false;
                           for(int k = 0; k <= j; k++) {
                              if((k < nextOrder.length) && (nextOrder[k] == agentIdx)) {
                                 inNextRound = true;
                                 break;
                              }
                           }
                           if(!inNextRound) {
                              droppedOut[agentIdx] = true;
                           }
                        }
                     }
                  }
                  else {
                     //No one is left in the auction
                     break;
                  }
               }
            }

            for(int i = 0; i < numAdvertisers; i++) {
               double cost = costs[i];
               if(!droppedOut[i] || cost == 0.0) {
                  cost = Double.MAX_VALUE;
               }
               String currAdv = "adv" + (i+1);
               HashMap<Query,Double> budgetMap = _budgetPredictions.get(currAdv);
               budgetMap.put(q,cost);
               _budgetPredictions.put(currAdv, budgetMap);
            }
         }
         else {
            for(int i = 0; i < numAdvertisers; i++) {
               String currAdv = "adv" + (i+1);
               HashMap<Query,Double> budgets = _budgetPredictions.get(currAdv);
               budgets.put(q,Double.MAX_VALUE);
               _budgetPredictions.put(currAdv, budgets);
            }
         }
      }
   }

   @Override
   public AbstractModel getCopy() {
      return new BudgetEstimator(_querySpace, _ourAdvIdx, _numSlots, _numPromSlots);
   }

}
