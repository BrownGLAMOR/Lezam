package models.paramest;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

import java.util.*;

import static models.paramest.ConstantsAndFunctions.getDropoutPoints;
import static models.paramest.ConstantsAndFunctions.getOrderMatrix;
import static simulator.parser.GameStatusHandler.UserState;

public class ConstantsAndFunctions {

   // Target Effect
   public static final double _TE = 0.5;
   // Promoted Slot Bonus
   public static final double _PSB = 0.5;
   // Advertiser effect lower bound <> upper bound
   public static final double[][] _advertiserEffectBounds = {{0.2, 0.3},
           {0.3, 0.4},
           {0.4, 0.5}};

   // Average advertiser effect
   //TODO: If we are going to average these, should it really be unweighted? It seems that
   //  users not satisfying the advertiser effect will be more likely. 
   //  (Does a weighting of 2/3 no advertiser effect and 1/3 advertiser effect make more sense?)
   public static final double[] _advertiserEffectBoundsAvg = {
           (_advertiserEffectBounds[0][0] + _advertiserEffectBounds[0][1]) / 2,
           (_advertiserEffectBounds[1][0] + _advertiserEffectBounds[1][1]) / 2,
           (_advertiserEffectBounds[2][0] + _advertiserEffectBounds[2][1]) / 2};

   // Continuation Probability lower bound <> upper bound
   public static final double[][] _continuationProbBounds = {{0.2, 0.5},
           {0.3, 0.6},
           {0.4, 0.7}};

   // Average continuation probability
   public static final double[] _continuationProbBoundsAvg = {
           (_continuationProbBounds[0][0] + _continuationProbBounds[0][1]) / 2,
           (_continuationProbBounds[1][0] + _continuationProbBounds[1][1]) / 2,
           (_continuationProbBounds[2][0] + _continuationProbBounds[2][1]) / 2};


   public static final double[] _regReserveLow = {.08, .29, .46};
   public static final double[] _regReserveHigh = {.29, .46, .6};

   // first index:
   // 0 - untargeted
   // 1 - targeted correctly
   // 2 - targeted incorrectly
   // second index:
   // 0 - not promoted
   // 1 - promoted
   public static final double[][] fTargetfPro = {{(1.0), (1.0) * (1.0 + _PSB)},
           {(1.0 + _TE), (1.0 + _TE) * (1.0 + _PSB)},
           {(1.0) / (1.0 + _TE), ((1.0) / (1.0 + _TE)) * (1.0 + _PSB)}};

   // Turns a boolean into binary
   public static int bool2int(boolean bool) {
      if (bool) {
         return 1;
      }
      return 0;
   }

   // returns the corresponding index for the targeting part of fTargetfPro
   public static int getFTargetIndex(boolean targeted, Product p, Product target) {
      if (!targeted || p == null || target == null) {
         return 0; //untargeted
      } else if (p.equals(target)) {
         return 1; //targeted correctly
      } else {
         return 2; //targeted incorrectly
      }
   }

   // Turns a query type into 0/1/2
   public static int queryTypeToInt(QueryType qt) {
      if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return 0;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_ONE)) {
         return 1;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_TWO)) {
         return 2;
      }
      System.out.println("Error in queryTypeToInt");
      return 2;
   }

   // Calculate the forward click probability as defined on page 14 of the
   // spec.
   public static double etaClickPr(double advertiserEffect, double fTargetfPro) {
      return (advertiserEffect * fTargetfPro) / ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
   }

   // Calculate the inverse of the forward click probability
   public static double clickPrtoE(double probClick, double fTargetfPro) {
      return probClick / (probClick + fTargetfPro - probClick * fTargetfPro);
   }

   public static double getBinomialProb(double numImps, double numClicks, double observedClicks) {
      return getBinomialProbUsingGaussian(numImps, numClicks / numImps, observedClicks);
   }

   public static double getBinomialProbUsingGaussian(double n, double p, double k) {
      double mean = n * p;
      double sigma2 = mean * (1.0 - p);
      double diff = k - mean;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
   }

   // Get Click Distribution
   public static HashMap<Product, Double> getClickDist(Query q, HashMap<Product, HashMap<UserState, Double>> userStates, int clicks, boolean ourAdTargeted, Product ourAdProduct) {
      QueryType qt = q.getType();
      HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
      // for each product
      int total = 0;
      int totalMatching = 0;
      int totalNotMatching = 0;
      HashMap<Product, Double> userDist = new HashMap<Product, Double>();
      for (Product p : userStates.keySet()) {
         HashMap<UserState, Double> states = userStates.get(p);
         // add up the number of searching users
         double searching = 0;
         if (qt.equals(QueryType.FOCUS_LEVEL_TWO) &&
                 q.getComponent().equals(p.getComponent()) &&
                 q.getManufacturer().equals(p.getManufacturer())) {
            searching = (1.0 / 3.0) * states.get(UserState.IS) + states.get(UserState.F2);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ONE) &&
                 ((q.getComponent() != null && q.getComponent().equals(p.getComponent())) ||
                          (q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
            searching = (1.0 / 6.0) * states.get(UserState.IS) + 0.5 * states.get(UserState.F1);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
            searching = (1.0 / 3.0) * states.get(UserState.IS) + states.get(UserState.F0);
         }

         total += searching;
         userDist.put(p, searching);
         if (ourAdTargeted) {
            if (p.equals(ourAdProduct)) {
               totalMatching += searching;
            } else {
               totalNotMatching += searching;
            }
         }
      }
      // TODO split up clicks intelligently if it's targeted, else do for loop
      // note: use totalMatching and totalNotMatching to determine how to
      // weight the clicks. I tried to do this on paper, but the eta equation
      // screws it up and makes it not easy. good luck?
      for (Product p : userStates.keySet()) {
         toreturn.put(p, 1.0 * userDist.get(p) * ((double) clicks) / ((double) total));
      }
      return toreturn;
   }

   // Get Impression Distribution
   public static HashMap<Product, Double> getImpressionDist(Query q, HashMap<Product, HashMap<UserState, Double>> userStates, int impressions) {
      QueryType qt = q.getType();
      HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
      // for each product
      int total = 0;
      HashMap<Product, Double> userDist = new HashMap<Product, Double>();
      for (Product p : userStates.keySet()) {
         HashMap<UserState, Double> states = userStates.get(p);
         // add up the number of searching users
         double searching = 0;
         if (qt.equals(QueryType.FOCUS_LEVEL_TWO) &&
                 q.getComponent().equals(p.getComponent()) &&
                 q.getManufacturer().equals(p.getManufacturer())) {
            searching = (1.0 / 3.0) * states.get(UserState.IS) + states.get(UserState.F2);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ONE) &&
                 ((q.getComponent() != null && q.getComponent().equals(p.getComponent())) ||
                          (q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
            searching = (1.0 / 6.0) * states.get(UserState.IS) + 0.5 * states.get(UserState.F1);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
            searching = (1.0 / 3.0) * states.get(UserState.IS) + states.get(UserState.F0);
         }
         total += searching;
         userDist.put(p, searching);
      }
      for (Product p : userStates.keySet()) {
         toreturn.put(p, 1.0 * userDist.get(p) * ((double) impressions) / ((double) total));
      }
      return toreturn;
   }

   public static double[] getPrView(Query q,int numSlots, int numPromSlots, double advEffect, double contProb, double convProb, HashMap<Product, HashMap<UserState, Double>> userStates) {
      return getViewOrIS(false,q,numSlots,numPromSlots,advEffect,contProb,convProb,userStates);
   }

   public static double[] getISRatio(Query q,int numSlots, int numPromSlots, double advEffect, double contProb, double convProb, HashMap<Product, HashMap<UserState, Double>> userStates) {
      return getViewOrIS(true,q,numSlots,numPromSlots,advEffect,contProb,convProb,userStates);
   }

   public static double[] getViewOrIS(boolean isRatio,Query q,int numSlots, int numPromSlots, double advEffect, double contProb, double convProb, HashMap<Product, HashMap<UserState, Double>> userStates) {
      QueryType qt = q.getType();

      double ISusers = 0.0;
      double nonISusers = 0.0;
      for(Product p : userStates.keySet()) {
         HashMap<UserState, Double> states = userStates.get(p);
         if (qt.equals(QueryType.FOCUS_LEVEL_TWO) &&
                 q.getComponent().equals(p.getComponent()) &&
                 q.getManufacturer().equals(p.getManufacturer())) {
            ISusers += (1.0 / 3.0) * states.get(UserState.IS);
            nonISusers += states.get(UserState.F2);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ONE) &&
                 ((q.getComponent() != null && q.getComponent().equals(p.getComponent())) ||
                          (q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
            ISusers += (1.0 / 6.0) * states.get(UserState.IS);
            nonISusers += 0.5 * states.get(UserState.F1);
         }
         else if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
            ISusers += (1.0 / 3.0) * states.get(UserState.IS);
            nonISusers += states.get(UserState.F0);
         }
      }

      double[] prView = new double[numSlots];
      prView[0] = 1.0;

      double[] ISRatio = new double[numSlots];
      ISRatio[0] = ISusers/(ISusers+nonISusers);
      for(int i = 1; i < numSlots; i++) {
         double lastPrView = prView[i-1];
         double lastClickPr;
         if((i-1) < numPromSlots) {
            lastClickPr = clickPrtoE(advEffect,fTargetfPro[0][1]);
         }
         else {
            lastClickPr = advEffect;
         }

         prView[i] = contProb*(lastPrView*(1-lastClickPr) + lastPrView*lastClickPr*(1.0-convProb*(1.0 - ISRatio[i-1])));

         //Remove non-IS Users that converted
         nonISusers -= nonISusers*lastPrView*lastClickPr*convProb*(1.0 - ISRatio[i-1]);

         ISRatio[i] = ISusers/(ISusers+nonISusers);
      }

      if(isRatio) {
         return ISRatio;
      }
      else {
         return prView;
      }
   }

   public static int[] getDropoutPoints(int[] impsPerAgent, int[] order, int numSlots) {
      ArrayList<Integer> impsBeforeDropout = new ArrayList<Integer>();
      PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
      
      //Add the impressions seen by any agents that started in a slot
      for (int i = 0; i < numSlots && i < impsPerAgent.length; i++) {
         int imps = impsPerAgent[order[i]];
         if(imps > 0) {
            queue.add(imps);
         }
      }

      int lastIdx = queue.size();

      while (!queue.isEmpty()) {
    	 //Get the number of impressions seen before an agent currently in a slot dropped out
         int val = queue.poll();
         impsBeforeDropout.add(val);

         //If there is another agent to consider that dropped in, add that agent to the queue.
         //(Add some # of impressions to that agent's impressions seen, so that its impressions value
         // is the number of impressions that occurred before it dropped out.)
         if (lastIdx < impsPerAgent.length) {
            queue.add(impsPerAgent[order[lastIdx]] + val);
            lastIdx++;
         }
      }

      //If two agents dropped out at the same time, we only consider that to be a single dropout point.
      impsBeforeDropout = removeDupes(impsBeforeDropout);

      return convertListToArr(impsBeforeDropout);
   }

   public static ArrayList<Integer> removeDupes(ArrayList<Integer> list) {
      ArrayList<Integer> arrNoDupes = new ArrayList<Integer>();
      for (Integer val : list) {
         if (!arrNoDupes.contains(val)) {
            arrNoDupes.add(val);
         }
      }
      return arrNoDupes;
   }

   public static int[] convertListToArr(List<Integer> integers) {
      int[] ret = new int[integers.size()];
      for (int i = 0; i < ret.length; i++) {
         ret[i] = integers.get(i);
      }
      return ret;
   }

   
   


   public static int[][] getOrderMatrix2(int[] impsPerAgent, int[] orderArr, int numSlots) {

	   //This will store order of agents at each dropout point
	   ArrayList<int[]> orders = new ArrayList<int[]>();

	   //Create initial order list
	   ArrayList<Integer> order = new ArrayList<Integer>();
	   for (int i=0; i<orderArr.length; i++) order.add(orderArr[i]);
	   
	   //Get imps per agent. This will be modified to be NOT the imps the agent saw,
	   //but the number of imps that occurred before the agent dropped out.
	   //(These values are the same unless the agent started outside of a slot)
	   int[] modifiedImpsPerAgent = impsPerAgent.clone();

	   while (order.size() > 0) {
		   //Add this ordering
		   orders.add(convertListToArr(order)); 

		   //i.e. Determine the total number of impressions that occurred before someone new dropped out
		   //(and the agents who had that number of impressions)
		   double fewestImps = Integer.MAX_VALUE;
		   ArrayList<Integer> agentsWithFewestImps = new ArrayList<Integer>();
		   for (int i=0; i<numSlots && i<order.size(); i++) {
			   int agent = order.get(i);
			   int imps = modifiedImpsPerAgent[agent];
			   if (imps < fewestImps) {
				   fewestImps = imps;
				   agentsWithFewestImps.clear();
			   }
			   if (imps == fewestImps) {
				   agentsWithFewestImps.add(agent);
			   }
		   }

		   //Add to impsPerAgent of other guys
		   int numAgentsRemoved = agentsWithFewestImps.size();
		   for (int i=numSlots; i<numSlots+numAgentsRemoved && i<order.size(); i++) { //for each agent that wasn't previously in the auction that is now being added
			   int agent = order.get(i);
			   modifiedImpsPerAgent[agent] += fewestImps;
		   }

		   //Remove these agents from the order
		   order.removeAll(agentsWithFewestImps);
	   }

	   //convert orders into a 2d array
	   int[][] ordersArr = new int[orders.size()][];
	   for (int i=0; i<orders.size(); i++) {
		   ordersArr[i] = orders.get(i);
	   }
	   return ordersArr;
   }

   
   public static void main(String[] args) {
	   int[] impsPerAgent = {190, 80, 120, 130};
	   int[] orderArr = {2, 1, 0, 3};
	   int numSlots = 2;
	   
	   int[][] orders = getOrderMatrix2(impsPerAgent, orderArr, numSlots);
	   System.out.println("orders:");
	   for (int[] order : orders) {
		   System.out.println(Arrays.toString(order));
	   }
   
   }
   
   public static int[][] getOrderMatrix(int[] impsPerAgent, int[] order, int[][] waterfall, int numSlots) {
      return getOrderMatrix(getDropoutPoints(impsPerAgent,order,numSlots),impsPerAgent,order,waterfall,numSlots);
   }

   public static int[][] getOrderMatrix(int[] dropouts, int[] impsPerAgent, int[] order, int[][] waterfall, int numSlots) {
      //Calculate how how many agents saw some number of impressions
	  int nonZeroImp = 0;
      for(Integer imps : impsPerAgent) {
         if(imps > 0) {
            nonZeroImp++;
         }
      }

      //The order for before the first dropout point is just the initial order
      int[][] orders = new int[dropouts.length][nonZeroImp];
      for(int i = 0; i < nonZeroImp; i++) {
         orders[0][i] = order[i];
      }

      //Orders.length is the number of dropout points. If there's more than 1, do the following.
      if(orders.length > 1) {

         for(int i = 1; i < orders.length; i++) {
            /*
            * Initialize all positions to -1, so we
            * only have to set the positions that have
            * advertisers
            */
            Arrays.fill(orders[i], -1);
         }

         //For each agent that saw some impressions,
         for (int i = 0; i < nonZeroImp; i++) {
            int currAgent = order[i]; //starting with the agent in the 1st slot
            int[] ourImpsPerSlot = waterfall[currAgent]; //get number of imps this agent saw in each slot
            int ourTotImps = impsPerAgent[currAgent]; //get total imps this agent saw

            //Compute how many impressions occurred before this agent entered the auction
            int impsBeforeEntry = 0;
            if(i >= numSlots) {
               int numDropsBeforeEntry = i - (numSlots - 1);
               int dropIdx = -1;
               //Since some dropouts have multiplicity, determine
               //which dropout point they come in on
               //FIXME: This is acting as if impsPerAgent gives the number of impressions
               // that occurred before an agent dropped out, but actually it is the number
               // of impressions SEEN by an agent. Problems can occur when multiple agents start
               // outside of a slot.
               for(int k = 0; k < dropouts.length; k++) {
                  int dropImp = dropouts[k];
                  for(int l = 0; l < impsPerAgent.length; l++) {
                     if(impsPerAgent[l] == dropImp) {
                        numDropsBeforeEntry--;
                     }
                  }
                  if(numDropsBeforeEntry <= 0) {
                     dropIdx = k;
                     break;
                  }
               }

               impsBeforeEntry = dropouts[dropIdx];
            }

            
            //---------
            //FOR THIS METHOD, CODE REVIEW LEFT OFF HERE!!!
            //--------
            
            //Add positions for all future dropout points
            for (int j = 1; j < dropouts.length; j++) {
               int totalImpsSeen = dropouts[j - 1];
               if (i < numSlots) {
                  //Started in the auction
                  if (ourTotImps <= totalImpsSeen) {
                     //We are out of the auction, do nothing
                  } else {
                     //Still in auction, determine position
                     int currPos = -1;
                     int innerImpsSeen = 0;
                     for (int k = 0; k <= i; k++) {
                        int idx = i - k;
                        innerImpsSeen += ourImpsPerSlot[idx];
                        if (totalImpsSeen < innerImpsSeen) {
                           currPos = idx;
                           break;
                        }
                     }
                     orders[j][currPos] = currAgent;
                  }
               } else {
                  //Started out of the auction
                  if (impsBeforeEntry > totalImpsSeen) {
                     //We aren't in the auction yet
                     int numDrops = 0;
                     for(int k = 0; k < j; k++) {
                        int dropImp = dropouts[k];
                        for(int l = 0; l < impsPerAgent.length; l++) {
                           if(impsPerAgent[l] == dropImp) {
                              numDrops++;
                           }
                        }
                     }
                     orders[j][i-numDrops] = currAgent;
                  } else if ((impsBeforeEntry + ourTotImps) <= totalImpsSeen) {
                     //We are out of the auction, do nothing
                  } else {
                     //Still in auction, determine position
                     int currPos = -1;
                     int innerImpsSeen = impsBeforeEntry;
                     for (int k = 0; k <= i; k++) {
                        int idx = i - k;
                        if (idx < numSlots) {
                           innerImpsSeen += ourImpsPerSlot[idx];
                           if (totalImpsSeen < innerImpsSeen) {
                              currPos = idx;
                              break;
                           }
                        }
                     }
                     orders[j][currPos] = currAgent;
                  }
               }
            }
         }
      }
      return orders;
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
  * -gnthomps
   */
   public static int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
      int[][] impressionsBySlot = new int[agents][slots];

      int[] slotStart = new int[slots];
      int a;

      for (int i = 0; i < agents; ++i) {
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

      return impressionsBySlot;
   }
}