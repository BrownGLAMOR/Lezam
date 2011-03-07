package simulator.predictions;

import edu.umich.eecs.tac.props.*;
import models.paramest.AbstractParameterEstimation;
import models.paramest.BayesianParameterEstimation;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class ParameterEstimationTest {

   public ArrayList<String> getGameStrings() {
      String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
      //		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
      int min = 1440;
      int max = 1441;

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = min; i < max; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public double modelParamPredictionChallenge(AbstractParameterEstimation baseModel) throws IOException, ParseException, InstantiationException, IllegalAccessException {
      ArrayList<String> filenames = getGameStrings();
      int numSlots = 5;
      int numAdvertisers = 8;
      HashMap<QueryType, ArrayList<Double>> advEffPercentError = new HashMap<QueryType, ArrayList<Double>>();
      advEffPercentError.put(QueryType.FOCUS_LEVEL_ZERO, new ArrayList<Double>());
      advEffPercentError.put(QueryType.FOCUS_LEVEL_ONE, new ArrayList<Double>());
      advEffPercentError.put(QueryType.FOCUS_LEVEL_TWO, new ArrayList<Double>());

      HashMap<QueryType, ArrayList<Double>> advEffBaselineError = new HashMap<QueryType, ArrayList<Double>>();
      advEffBaselineError.put(QueryType.FOCUS_LEVEL_ZERO, new ArrayList<Double>());
      advEffBaselineError.put(QueryType.FOCUS_LEVEL_ONE, new ArrayList<Double>());
      advEffBaselineError.put(QueryType.FOCUS_LEVEL_TWO, new ArrayList<Double>());

      HashMap<QueryType, ArrayList<Double>> contProbPercentError = new HashMap<QueryType, ArrayList<Double>>();
      contProbPercentError.put(QueryType.FOCUS_LEVEL_ZERO, new ArrayList<Double>());
      contProbPercentError.put(QueryType.FOCUS_LEVEL_ONE, new ArrayList<Double>());
      contProbPercentError.put(QueryType.FOCUS_LEVEL_TWO, new ArrayList<Double>());

      HashMap<QueryType, ArrayList<Double>> contProbBaselineError = new HashMap<QueryType, ArrayList<Double>>();
      contProbBaselineError.put(QueryType.FOCUS_LEVEL_ZERO, new ArrayList<Double>());
      contProbBaselineError.put(QueryType.FOCUS_LEVEL_ONE, new ArrayList<Double>());
      contProbBaselineError.put(QueryType.FOCUS_LEVEL_TWO, new ArrayList<Double>());
      for (int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
         String filename = filenames.get(fileIdx);
         GameStatusHandler statusHandler = new GameStatusHandler(filename);
         GameStatus status = statusHandler.getGameStatus();
         String[] agents = status.getAdvertisers();

         UserClickModel userClickModel = status.getUserClickModel();
         double squashing = status.getPubInfo().getSquashingParameter();

         //Make the query space
         LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
         querySpace.add(new Query(null, null));
         for (Product product : status.getRetailCatalog()) {
            // The F1 query classes
            // F1 Manufacturer only
            querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
         }

         int numPromSlots = status.getSlotInfo().getPromotedSlots();

         HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
         HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
         HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();
         LinkedList<HashMap<Product, HashMap<UserState, Integer>>> allUserDists = status.getUserDistributions();

         for (int agent = 0; agent < agents.length; agent++) {
            AbstractParameterEstimation model = (AbstractParameterEstimation) baseModel.getCopy();

            LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
            LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
            LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

            for (int i = 0; i < 57; i++) {

               SalesReport salesReport = ourSalesReports.get(i);
               QueryReport queryReport = ourQueryReports.get(i);
               BidBundle bidBundle = ourBidBundles.get(i);

               HashMap<String, HashMap<Query, Ad>> allAds = new HashMap<String, HashMap<Query, Ad>>();
               for (int agentInner = 0; agentInner < agents.length; agentInner++) {
                  BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                  HashMap<Query, Ad> advAds = new HashMap<Query, Ad>();
                  for (Query q : querySpace) {
                     advAds.put(q, innerBidBundle.getAd(q));
                  }
                  allAds.put(agents[agentInner], advAds);
               }

               HashMap<Query, int[]> allOrders = new HashMap<Query, int[]>();
               HashMap<Query, int[]> allImpressions = new HashMap<Query, int[]>();
               for (Query q : querySpace) {
                  int[] order = new int[numAdvertisers];
                  int[] impressions = new int[numAdvertisers];
                  ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
                  for (int agentInner = 0; agentInner < agents.length; agentInner++) {
                     if (agent == agentInner) {
                        QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
                        impressions[0] = innerQueryReport.getImpressions(q);

                        BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                        double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                        double bid = innerBidBundle.getBid(q);
                        double squashedBid = bid * Math.pow(advEffect, squashing);
                        bidPairs.add(new BidPair(0, squashedBid));
                     } else if (agentInner < agent) {
                        QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
                        impressions[agentInner + 1] = innerQueryReport.getImpressions(q);

                        BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                        double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                        double bid = innerBidBundle.getBid(q);
                        double squashedBid = bid * Math.pow(advEffect, squashing);
                        bidPairs.add(new BidPair(agentInner + 1, squashedBid));
                     } else {
                        QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
                        impressions[agentInner] = innerQueryReport.getImpressions(q);

                        BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                        double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                        double bid = innerBidBundle.getBid(q);
                        double squashedBid = bid * Math.pow(advEffect, squashing);
                        bidPairs.add(new BidPair(agentInner, squashedBid));
                     }
                  }

                  Collections.sort(bidPairs);

                  for (int j = 0; j < bidPairs.size(); j++) {
                     order[j] = bidPairs.get(j).getID();
                  }

                  allOrders.put(q, order);
                  allImpressions.put(q, impressions);
               }

               HashMap<Product, HashMap<UserState, Integer>> userStates = allUserDists.get(i);
               model.updateModel(queryReport, salesReport, bidBundle, numPromSlots, allOrders, allImpressions, userStates);
               for (Query q : querySpace) {
                  double[] preds = model.getPrediction(q);

                  double trueAdvertiserEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agent);
                  double trueContinuationProb = userClickModel.getContinuationProbability(userClickModel.queryIndex(q));

                  //System.out.println(agents[agent]);
                  //						System.out.println(q);
                  //System.out.println("Our guess: "+preds[0]);
                  //System.out.println(trueAdvertiserEffect);
                  //						System.out.println("Percent Error: "+Math.abs(trueAdvertiserEffect-preds[0])/trueAdvertiserEffect*100);
                  //						System.out.println("Continuation Prob: "+preds[1]);

                  double advEffAverage = 0.0;
                  double contProbAverage = 0.0;
                  if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                     advEffAverage = 0.25;
                     contProbAverage = .35;
                  } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                     advEffAverage = 0.35;
                     contProbAverage = .45;
                  } else {
                     advEffAverage = 0.45;
                     contProbAverage = .55;
                  }
                  //						System.out.println("Baseline:"+Math.abs(trueAdvertiserEffect-average)/trueAdvertiserEffect*100);
                  advEffPercentError.get(q.getType()).add(Math.abs(trueAdvertiserEffect - preds[0]) / trueAdvertiserEffect * 100);
                  advEffBaselineError.get(q.getType()).add(Math.abs(trueAdvertiserEffect - advEffAverage) / trueAdvertiserEffect * 100);

                  contProbPercentError.get(q.getType()).add(Math.abs(trueContinuationProb - preds[1]) / trueContinuationProb * 100);
                  contProbBaselineError.get(q.getType()).add(Math.abs(trueContinuationProb - contProbAverage) / trueContinuationProb * 100);
               }
            }
         }
      }

      double totalError = 0.0;
      for (QueryType qt : advEffPercentError.keySet()) {
         double advEffAveragePercentError = 0.0;
         double advEffBaselinePercentError = 0.0;
         for (Double d : advEffPercentError.get(qt)) {
            //System.out.println(percentError.get(qt).size());
            advEffAveragePercentError += d / advEffPercentError.get(qt).size();
         }
         for (Double d2 : advEffBaselineError.get(qt)) {
            advEffBaselinePercentError += d2 / advEffBaselineError.get(qt).size();
         }
         System.out.println(qt);
         System.out.println("Advertiser Effect Mean Percent Error Using Data: " + advEffAveragePercentError);
         System.out.println("Advertiser Effect Mean Percent Error Using Average: " + advEffBaselinePercentError);
         totalError += advEffAveragePercentError;
      }

      for (QueryType qt : advEffPercentError.keySet()) {
         double contProbAveragePercentError = 0.0;
         double contProbaselinePercentError = 0.0;
         for (Double d : contProbPercentError.get(qt)) {
            //System.out.println(percentError.get(qt).size());
            contProbAveragePercentError += d / contProbPercentError.get(qt).size();
         }
         for (Double d2 : contProbBaselineError.get(qt)) {
            contProbaselinePercentError += d2 / contProbPercentError.get(qt).size();
         }
         System.out.println(qt);
         System.out.println("Continuation Prob Mean Percent Error Using Data: " + contProbAveragePercentError);
         System.out.println("Continuation Prob Mean Percent Error Using Average: " + contProbaselinePercentError);
         totalError += contProbAveragePercentError;
      }
      return totalError;
   }

   public static class BidPair implements Comparable<BidPair> {

      private int _advIdx;
      private double _bid;

      public BidPair(int advIdx, double bid) {
         _advIdx = advIdx;
         _bid = bid;
      }

      public int getID() {
         return _advIdx;
      }

      public void setID(int advIdx) {
         _advIdx = advIdx;
      }

      public double getBid() {
         return _bid;
      }

      public void setBid(double bid) {
         _bid = bid;
      }

      public int compareTo(BidPair agentBidPair) {
         double ourBid = this._bid;
         double otherBid = agentBidPair.getBid();
         if (ourBid < otherBid) {
            return 1;
         }
         if (otherBid < ourBid) {
            return -1;
         } else {
            return 0;
         }
      }

   }

   public static class ImprPair implements Comparable<ImprPair> {

      private int _advIdx;
      private int _impr;

      public ImprPair(int advIdx, int impr) {
         _advIdx = advIdx;
         _impr = impr;
      }

      public int getID() {
         return _advIdx;
      }

      public void setID(int advIdx) {
         _advIdx = advIdx;
      }

      public double getImpr() {
         return _impr;
      }

      public void setImpr(int impr) {
         _impr = impr;
      }

      public int compareTo(ImprPair agentImprPair) {
         double ourBid = this._impr;
         double otherBid = agentImprPair.getImpr();
         if (ourBid > otherBid) {
            return 1;
         }
         if (otherBid > ourBid) {
            return -1;
         } else {
            return 0;
         }
      }

   }

   private double[] getStdDevAndMean(ArrayList<Double> list) {
      double n = list.size();
      double sum = 0.0;
      for (Double data : list) {
         sum += data;
      }
      double mean = sum / n;

      double variance = 0.0;

      for (Double data : list) {
         variance += (data - mean) * (data - mean);
      }

      variance /= (n - 1);

      double[] stdDev = new double[2];
      stdDev[0] = mean;
      stdDev[1] = Math.sqrt(variance);
      return stdDev;
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
   public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
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

   public static void main(String[] args) throws IOException, ParseException, InstantiationException, IllegalAccessException {
      ParameterEstimationTest evaluator = new ParameterEstimationTest();

      double start = System.currentTimeMillis();
      Set<Query> querySpace = new LinkedHashSet<Query>();
      //evaluator.modelParamPredictionChallenge(new MBarrowsImpl(querySpace,"this will be set later",0));
      //		evaluator.modelParamPredictionChallenge(new MBarrowsParameterEstimation());

      //		Random rand = new Random();
      //		double args0,args1,args2;
      //		do {
      //			args0 = rand.nextDouble()*.15;
      //			args1 = rand.nextDouble()*.25;
      //			args2 = rand.nextDouble()*.35;
      //		} while(!(args0 < args1 && args1 < args2));
      //
      //		double[] c = new double[3];
      //		c[0] = args0;
      //		c[1] = args1;
      //		c[2] = args2;

      double error = evaluator.modelParamPredictionChallenge(new BayesianParameterEstimation());

      double stop = System.currentTimeMillis();
      double elapsed = stop - start;
      //		System.out.println("This took " + (elapsed / 1000) + " seconds");
   }

}
