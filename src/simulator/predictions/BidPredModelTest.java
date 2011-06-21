package simulator.predictions;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.UserClickModel;
import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class BidPredModelTest {

   public static final boolean printlns = false;
   public static final boolean doRMSE = false;

   public enum GameSet {
      finals2010, semifinals2010, test2010
   }

   public static ArrayList<String> getGameStrings() {
      return getGameStrings(GameSet.finals2010, 15127, 15130);
   }


   public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) baseFile = "./game";
      if (GAMES_TO_TEST == GameSet.finals2010) baseFile = "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-";  //"/pro/aa/finals2010/game-tacaa1-";    //"/Users/sodomka/Desktop/tacaa2010/game-tacaa1-";

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public void bidPredictionChallenge(AbstractBidModel baseModel) throws IOException, ParseException {
      double start = System.currentTimeMillis();

      /*
         * All these maps they are like this: <fileName<agentName,error>>
         */
      HashMap<String, HashMap<String, Double>> ourTotErrorMegaMap = new HashMap<String, HashMap<String, Double>>();
      HashMap<String, HashMap<String, Double>> ourTotActualMegaMap = new HashMap<String, HashMap<String, Double>>();
      HashMap<String, HashMap<String, Integer>> ourTotErrorCounterMegaMap = new HashMap<String, HashMap<String, Integer>>();
      ArrayList<String> filenames = getGameStrings();
      for (int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
         String filename = filenames.get(fileIdx);
         GameStatusHandler statusHandler = new GameStatusHandler(filename);
         GameStatus status = statusHandler.getGameStatus();
         String[] agents = status.getAdvertisers();

         /*
             * One map for each advertiser
             */
         HashMap<String, Double> ourTotErrorMap = new HashMap<String, Double>();
         HashMap<String, Double> ourTotActualMap = new HashMap<String, Double>();
         HashMap<String, Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

         UserClickModel userClickModel = status.getUserClickModel();
         double squashing = status.getPubInfo().getSquashingParameter();

         Set<Query> querySpace = status.getQuerySpace();

         HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
         HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

         for (int agent = 0; agent < agents.length; agent++) {
            if (agents[agent].equals("TacTex")) {
               AbstractBidModel model = (AbstractBidModel) baseModel.getCopy();
               model.setAdvertiser(agents[agent]);
               if (printlns) {
                  System.out.println("Testing for agent: " + agents[agent]);
               }

               double ourTotError = 0;
               double ourTotActual = 0;
               int ourTotErrorCounter = 0;

               LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);

               for (int i = 0; i < 57; i++) {
                  QueryReport queryReport = ourQueryReports.get(i);
                  //						System.out.print(""+(i+1) + ", ");

                  HashMap<Query, HashMap<String, Integer>> ranks = new HashMap<Query, HashMap<String, Integer>>();
                  HashMap<Query, HashMap<String, Boolean>> rankables = new HashMap<Query, HashMap<String, Boolean>>();
                  HashMap<Query, Double> cpc = new HashMap<Query, Double>();
                  HashMap<Query, Double> ourBid = new HashMap<Query, Double>();
                  for (Query q : querySpace) {
                     if(!Double.isNaN(queryReport.getPosition(q))) {
//                     cpc.put(q, queryReport.getCPC(q) * Math.pow(userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agent), squashing)); //need to squash CPC!!!
                        cpc.put(q, queryReport.getCPC(q));

                        ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
                        for (int agentInner = 0; agentInner < agents.length; agentInner++) {
                           BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                           double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                           double bid = innerBidBundle.getBid(q);
                           double squashedBid = bid * Math.pow(advEffect, squashing);
                           bidPairs.add(new BidPair(agentInner, squashedBid));
                           if (agentInner == agent) {
                              ourBid.put(q, bid);
//                           ourBid.put(q, squashedBid);
                           }
                        }

                        Collections.sort(bidPairs);

                        HashMap<String, Integer> queryRanks = new HashMap<String, Integer>();
                        HashMap<String, Boolean> rankable = new HashMap<String, Boolean>();
                        for (int j = 0; j < bidPairs.size(); j++) {
                           String adv = agents[bidPairs.get(j).getID()];
                           queryRanks.put(adv, j);
                           double bid = bidPairs.get(j).getBid();
                           rankable.put(adv,true);
//                        if(bid > 0) {
//                           rankable.put(adv,true);
//                        }
//                        else {
//                           rankable.put(adv,false);
//                        }
                        }

                        ranks.put(q, queryRanks);
                        rankables.put(q,rankable);
                     }
                     else {
                        ourBid.put(q,0.0);
                        cpc.put(q,0.0);
                        ranks.put(q,null);
                        rankables.put(q,null);
                     }
                  }

                  model.updateModel(cpc, ourBid, ranks, rankables);

                  for (Query q : querySpace) {
                     if (printlns) {
                        System.out.print("Query: " + q.getComponent() + ", " + q.getManufacturer() + " -- ");
                     }
                     for (int j = 0; j < agents.length; j++) {
                        /*
                                 * You guys don't really need to worry about predicting, because
                                 * that is not really the point of this particle filter.
                                 */
                        double bid = allBidBundles.get(agents[j]).get(i).getBid(q);
                        double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), j);
                        double squashedBid = bid;
//                        double squashedBid = bid * Math.pow(advEffect, squashing);

                        double bidPred = model.getPrediction(agents[j], q);

                        if (!Double.isNaN(bid) && squashedBid < 3.75) {

                           if (printlns) {
                              System.out.print("  \tAgent: " + agents[j] + " Act:" +/*(int)*/(squashedBid * 100) + " <-> Pred: " + /*(int)*/(bidPred * 100) + " -- ");
                           }
                           double error;
                           if (!doRMSE) {
                              error = Math.abs(squashedBid - bidPred); //MAE
                           } else {
                              error = (squashedBid - bidPred) * (squashedBid - bidPred);
                           }
                           //							error = error*error;  //RMSE
                           ourTotActual += squashedBid;
                           ourTotError += error;
                           ourTotErrorCounter++;

                           //								if(q.equals(new Query("pg","tv"))) {
                           //									System.out.print(squashedBid + ", " + bidPred + ", ");
                           //								}
                        }
                     }
                  }
                  if (printlns) {
                     System.out.println();
                  }
               }
               //					System.out.println();
               ourTotErrorMap.put(agents[agent], ourTotError);
               ourTotActualMap.put(agents[agent], ourTotActual);
               ourTotErrorCounterMap.put(agents[agent], ourTotErrorCounter);
               //					System.out.println("Error: " + (ourTotError/((double)ourTotErrorCounter)));
            }
         }

         ourTotErrorMegaMap.put(filename, ourTotErrorMap);
         ourTotActualMegaMap.put(filename, ourTotActualMap);
         ourTotErrorCounterMegaMap.put(filename, ourTotErrorCounterMap);
      }
      ArrayList<Double> RMSEList = new ArrayList<Double>();
      ArrayList<Double> actualList = new ArrayList<Double>();
      //		System.out.println("Model: " + baseModel);
      for (String file : filenames) {
         //			System.out.println("File: " + file);
         HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
         HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
         HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
         for (String agent : totErrorCounterMap.keySet()) {
            //				System.out.println("\t Agent: " + agent);
            double totError = totErrorMap.get(agent);
            double totActual = totActualMap.get(agent);
            double totErrorCounter = totErrorCounterMap.get(agent);
            //				System.out.println("\t\t Predictions: " + totErrorCounter);
            double MAE;
            if (!doRMSE) {
               MAE = (totError / totErrorCounter);
            } else {
               MAE = Math.sqrt(totError / totErrorCounter);
            }
            double actual = totActual / totErrorCounter;
            RMSEList.add(MAE);
            actualList.add(actual);
         }
      }
      //		System.out.println("Data Points: " + dataPointCounter);
      Collections.sort(RMSEList);

      double[] rmseStd = getStdDevAndMean(RMSEList);
      //		double[] actualStd = getStdDevAndMean(actualList);
      double stop = System.currentTimeMillis();
      double elapsed = (stop - start) / 1000.0;
      System.out.println(baseModel + ", " + rmseStd[0] + ", " + elapsed);
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

      @Override
      public String toString() {
         return _bid + "";
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

   public static void main(String[] args) throws IOException, ParseException {
      BidPredModelTest evaluator = new BidPredModelTest();

      ArrayList<String> filenames = evaluator.getGameStrings();
      String filename = filenames.get(0);
      GameStatusHandler statusHandler = new GameStatusHandler(filename);
      GameStatus status = statusHandler.getGameStatus();
      String[] agents = status.getAdvertisers();
      Set<Query> querySpace = status.getQuerySpace();

      LinkedHashSet<String> advertisers = new LinkedHashSet<String>();

      for (int i = 0; i < agents.length; i++) {
         advertisers.add(agents[i]);
      }

      //		System.out.println(advertisers);

      //		Random r = new Random();
      //		double randomJumpProb = r.nextDouble()*.3;
      //		double yesterdayProb = r.nextDouble();
      //		double nDaysAgoProb = r.nextDouble();
      //		double var = r.nextDouble()*10;
      //
      //		double total = randomJumpProb + yesterdayProb + nDaysAgoProb;
      //		randomJumpProb /= total;
      //		yesterdayProb /= total;
      //		nDaysAgoProb /= total;

      double start = System.currentTimeMillis();
      evaluator.bidPredictionChallenge(new IndependentBidModel(advertisers, agents[0], 1, .1, .5, .4, 1.9,querySpace));
//		evaluator.bidPredictionChallenge(new JointDistBidModel(advertisers, agents[0], 8, .7, 1000));

      //		ArrayList<AbstractBidModel> models = new ArrayList<AbstractBidModel>();
      //		ArrayList<Double> weights = new ArrayList<Double>();
      //		models.add(new IndependentBidModel(advertisers, agents[0],1,0,.8,.2,8.0));
      //		models.add(new JointDistBidModel(advertisers, agents[0], 15, .8, 1000));
      //		weights.add(.5);
      //		weights.add(.5);
      //		evaluator.bidPredictionChallenge(new LinearComboBidModel(models, weights));


      double stop = System.currentTimeMillis();
      double elapsed = stop - start;
      //		System.out.println("This took " + (elapsed / 1000) + " seconds");
   }

}
