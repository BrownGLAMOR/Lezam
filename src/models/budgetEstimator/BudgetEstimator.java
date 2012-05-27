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

	//the new algorithm improved our budget MAE, but for some reason hurt our overall profit.
	//I'll set it to false, but at some point the better estimates might be worthwhile.
	boolean USE_NEW_ORDER_MATRIX_ALG = false;
	
	
   HashMap<String, HashMap<Query, Double>> _budgetPredictions;
   HashMap<String,Integer> _agentToIdxMap;
   Set<Query> _querySpace;

   int numAdvertisers = 8;

//   int MIN_IMPS = 30;
//   int MIN_COST = 10;

   int MIN_IMPS = 0;
   int MIN_COST = 0;

   int _ourAdvIdx;
   int _numSlots;
   int _numPromSlots;

   double _squashParam;

   double[] c;

   public BudgetEstimator(Set<Query> querySpace, int ourAdvIdx, int numSlots, int numPromSlots, double squashParam) {
      _querySpace = querySpace;
      _budgetPredictions = new HashMap<String, HashMap<Query, Double>>();
      _ourAdvIdx = ourAdvIdx;
      _numSlots = numSlots;
      _numPromSlots = numPromSlots;
      _squashParam = squashParam;
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

      c = new double[3];
      c[0] = .11;
      c[1] = .23;
      c[2] = .36;
   }

   @Override
   public double getBudgetEstimate(Query q, String advertiser) {
//      return Double.MAX_VALUE;
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
                           HashMap<Query, Double> contProbs,
                           double[] regReserves,
                           HashMap<Query, int[]> allOrders,
                           HashMap<Query, int[]> allImps,
                           HashMap<Query, int[][]> allWaterfalls,
                           HashMap<Query,HashMap<String,Boolean>> rankables,
                           HashMap<Query, double[]> allSquashedBids,
                           HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> userStates) {
      for(Query q : _querySpace) {
   	   try {

    	  int[][] waterfall = allWaterfalls.get(q);

         if(waterfall != null) {

            //imps[a]: number of impressions seen by agent a
            int[] imps = allImps.get(q);

            //startOrder[i]: agent that started in the ith position
            int[] startOrder = allOrders.get(q);
            HashMap<String,Boolean> rankable = rankables.get(q);

            //squashedBids[a]: squashed bid of agent a
            double[] squashedBids = allSquashedBids.get(q);

            //Probability of getting a click, given your ad is viewed.
            int qtIdx = queryTypeToInt(q.getType());
            double baseClickPr = _advertiserEffectBoundsAvg[qtIdx];

            //Probability of seeing a view in each slot.
            double[] prViews = getPrView(q,_numSlots,_numPromSlots,baseClickPr,contProbs.get(q),c[qtIdx],userStates);

            //Number of impressions that occurred before an agent dropped out. 
            int[] dropoutPoints = getDropoutPoints(imps,startOrder,_numSlots);

            //orders[i][j]: before the ith dropout point, which agent was in the jth position?
            int[][] orders;
            if (USE_NEW_ORDER_MATRIX_ALG) orders = getOrderMatrix2(imps, startOrder, _numSlots);
            else orders = getOrderMatrix(dropoutPoints, imps, startOrder, waterfall, _numSlots);
            
            //int[][] orders = getOrderMatrix(dropoutPoints, imps, startOrder, waterfall, _numSlots);
            //int[][] orders = getOrderMatrix2(imps, startOrder, _numSlots);

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

                  if(agentIdx < 0 || !rankable.get("adv"+(agentIdx+1))) {
                     //This is a padded agent, skip it
                     continue;
                  }

                  //FIX ME: Shouldn't this be the number of imps the agent
                  //saw in this slot FOR THIS DROPOUT PERIOD?
                  //e.g. an agent that was in the 1st slot for 1000 imps
                  //may have been there while other agents were dropping out.
                  //It will be penalized by those slotImps for each dropout point.
                  //UNFIXME: Never mind. This is accounted for! :D
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
                        cpc = Math.max(regReserves[qtIdx],squashedBids[agentBelowIdx]);
                     }
                     else {
                        cpc = regReserves[qtIdx];
                     }

                     //FIXME: Use our advertiser effect prediction instead of baseClickPr.
                     cpc /= Math.pow(baseClickPr,_squashParam);

                     /*
                     * We only want to assign the number of imps to this
                     * advertiser as there are before the advertiser drops out
                     */
                     //TODO: Is the min necessary, or can we just take impsSinceLastDrop? 
                     //  (It's not harming anything to take the min, I'm just not sure if there's a case I'm missing)
                     costs[agentIdx] += Math.min(slotImps,impsSinceLastDrop)*prClick*cpc;

                     
                     //FIXME: It seems that this entire block can be commented out, 
                     //And the if condition below [if(slotImps == (impsSinceLastDrop + pastImpsInSlot))] can be removed
//                     //Compute how many impressions this agent previously spent in the same slot
//                     //(for different dropout points -- where other agents below this agent dropped out)
//                     int pastImpsInSlot = 0;
//                     if(i > 0) { 
//                    	 //If this is not the first dropout point, look at all past dropout points
//                        for(int k = i-1; k >= 0; k--) {
//                        	//If the agent was in the same slot at a previous dropoutPoint
//                           if(orders[k][j] == agentIdx) { 
//                              int dropOutInner = dropoutPoints[k];
//                              int impsSinceLastDropInner;
//                              if(k > 0) {
//                                 impsSinceLastDropInner = dropOutInner - dropoutPoints[k-1];
//                              }
//                              else {
//                                 impsSinceLastDropInner = dropOutInner;
//                              }
//                              pastImpsInSlot += impsSinceLastDropInner;
//                           }
//                           else {
//                              break;
//                           }
//                        }
//                     }
//                     else {
//                        int f = 0;
//                     }

                     //Check if the agent dropped out
                     //If they saw more slotImps in this slot they didn't drop
                     //if(slotImps == (impsSinceLastDrop + pastImpsInSlot)) { //FIXME: What is this?
                     //If they are in the last round they didn't drop
                        if(i < dropoutPoints.length-1) {
                           int[] nextOrder = orders[i+1];
                           boolean inNextRound = false;
                           
                           //(The agent can't later appear in a position higher than he's currently in,
                           //so we don't have to check every element of nextOrder)
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
                  else {
                     //No one is left in the auction
                     break;
                  }
               }
            }

            for(int i = 0; i < numAdvertisers; i++) {
               String currAdv = "adv" + (i+1);
               double cost = costs[i];
               //FIXME: Kind of suspicious that we would assume no budget if imps are below some threshold.
               // I think this only makes sense if imps[i] is an invalid value, e.g. negative
               // (MIN_IMPS is currently 0, but if it is increased, I'd take issue).
               //After conversation w/ Jordan: it is a question of what to do when agents make probe bids.
               //If an agent makes a probe bid today, does that necessarily mean they'll make a probe bid
               //tomorrow? If they're more likely to have a high budget the next day, maybe this makes sense.
               //TODO: What is rankable? Add a comment about it here.
               if(!droppedOut[i] || cost < MIN_COST || imps[i] < MIN_IMPS || !rankable.get(currAdv)) {
                  cost = Double.MAX_VALUE;
               }
               HashMap<Query,Double> budgetMap = _budgetPredictions.get(currAdv);
               budgetMap.put(q,cost);
               _budgetPredictions.put(currAdv, budgetMap);
            }
         }
         else {
        	 //Waterfall was null. Give default budgets.
            for(int i = 0; i < numAdvertisers; i++) {
               String currAdv = "adv" + (i+1);
               HashMap<Query,Double> budgets = _budgetPredictions.get(currAdv);
               budgets.put(q,Double.MAX_VALUE);
               _budgetPredictions.put(currAdv, budgets);
            }
         }
      } catch (Exception e) {
	   setDefaultBudgets(q);
	   e.printStackTrace();
      } 
   }
   }
   
   
   private void setDefaultBudgets() {
	   for (Query q : _querySpace) {
		   setDefaultBudgets(q);
	   }
   }
   
   private void setDefaultBudgets(Query q) {
       for(int i = 0; i < numAdvertisers; i++) {
           String currAdv = "adv" + (i+1);
           HashMap<Query,Double> budgets = _budgetPredictions.get(currAdv);
           budgets.put(q,Double.MAX_VALUE);
           _budgetPredictions.put(currAdv, budgets);
        }	   
   }
   
   @Override
   public AbstractModel getCopy() {
      return new BudgetEstimator(_querySpace, _ourAdvIdx, _numSlots, _numPromSlots,_squashParam);
   }

}
