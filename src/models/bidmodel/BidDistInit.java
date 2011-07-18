package models.bidmodel;

import edu.umich.eecs.tac.props.*;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Builds initial bid distributions from game logs
 * for the bid prediction models
 *
 * @author jberg
 */

public final class BidDistInit {

   public static void buildBidDistribution(ArrayList<String> filenames) throws IOException, ParseException {
      ArrayList<Double> bidDist = new ArrayList<Double>();
      double startVal = Math.pow(2, (1.0 / 25.0 - 2.0)) - 0.25;
      double aStep = Math.pow(2, (1.0 / 25.0));
      double maxBid = 3.5;
      int count = 0;
      for (double curKey = startVal; curKey <= maxBid + 0.001; curKey = (curKey + 0.25) * aStep - 0.25) {
         bidDist.add(curKey);
         count++;
      }

      int[] bidCountsF0 = new int[bidDist.size()];
      int[] bidCountsF1 = new int[bidDist.size()];
      int[] bidCountsF2 = new int[bidDist.size()];
      int totalF0 = 0;
      int totalF1 = 0;
      int totalF2 = 0;
      int maxF0 = 0;
      int maxF1 = 0;
      int maxF2 = 0;
      for (String file : filenames) {
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         String[] advertisers = gameStatus.getAdvertisers();
         HashMap<String, LinkedList<BidBundle>> allBundles = gameStatus.getBidBundles();
         HashMap<String, LinkedList<QueryReport>> allReports = gameStatus.getQueryReports();
         UserClickModel userClickModel = gameStatus.getUserClickModel();
         double squashing = gameStatus.getPubInfo().getSquashingParameter();
         for (int j = 0; j < advertisers.length; j++) {
            LinkedList<BidBundle> bundles = allBundles.get(advertisers[j]);
            LinkedList<QueryReport> reports = allReports.get(advertisers[j]);

            for (QueryReport report : reports) {
               for (Query q : report) {
                  if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                     if (report.getImpressions(q) > maxF0) {
                        maxF0 = report.getImpressions(q);
                     }
                  } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                     if (report.getImpressions(q) > maxF1) {
                        maxF1 = report.getImpressions(q);
                     }
                  } else {
                     if (report.getImpressions(q) > maxF2) {
                        maxF2 = report.getImpressions(q);
                     }
                  }
               }
            }

            for (BidBundle bundle : bundles) {
               for (Query q : bundle) {
//                  double bid = bundle.getBid(q) * Math.pow(userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), j), squashing);
                  double bid = bundle.getBid(q);
                  int index = Collections.binarySearch(bidDist, bid);
                  int insertIdx;
                  if (index < 0) {
                     insertIdx = -index - 1;
                  } else {
                     insertIdx = index;
                  }

                  if (insertIdx <= bidCountsF0.length - 1) {
                     if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                        bidCountsF0[insertIdx] += 1;
                        totalF0++;
                     } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                        bidCountsF1[insertIdx] += 1;
                        totalF1++;
                     } else if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
                        bidCountsF2[insertIdx] += 1;
                        totalF2++;
                     }
                     //							if(insertIdx == 0) {
                     //								System.out.println(bid + " is less than" + bidDist.get(0));
                     //							}
                     //							else {
                     //								System.out.println(bid + " is between " + bidDist.get(insertIdx-1) + "  and" + bidDist.get(insertIdx));
                     //							}
                  } else {
                     System.out.println("Bid (" + bid + ") by " + advertisers[j] + " larger than maxBid, throwing it out");
                  }
               }
            }
         }
      }

      double[] initDistF0 = new double[bidCountsF0.length];
      double[] initCDFF0 = new double[bidCountsF0.length];

      double[] initDistF1 = new double[bidCountsF1.length];
      double[] initCDFF1 = new double[bidCountsF1.length];

      double[] initDistF2 = new double[bidCountsF2.length];
      double[] initCDFF2 = new double[bidCountsF2.length];
      String outputF0 = "final static double[] initDistF0 = {";
      String outputF1 = "final static double[] initDistF1 = {";
      String outputF2 = "final static double[] initDistF2 = {";
      for (int i = 0; i < bidCountsF0.length; i++) {
         initDistF0[i] = bidCountsF0[i] / ((double) totalF0);
         initDistF1[i] = bidCountsF1[i] / ((double) totalF1);
         initDistF2[i] = bidCountsF2[i] / ((double) totalF2);
         if (i == 0) {
            initCDFF0[i] = initDistF0[i];
            initCDFF1[i] = initDistF1[i];
            initCDFF2[i] = initDistF2[i];
         } else {
            initCDFF0[i] = initDistF0[i] + initCDFF0[i - 1];
            initCDFF1[i] = initDistF1[i] + initCDFF1[i - 1];
            initCDFF2[i] = initDistF2[i] + initCDFF2[i - 1];
         }
         outputF0 += initDistF0[i] + ", ";
         outputF1 += initDistF1[i] + ", ";
         outputF2 += initDistF2[i] + ", ";
         System.out.println(bidDist.get(i) + ", " + initDistF0[i] + ", " + initCDFF0[i] + ", " + initDistF1[i] + ", " + initCDFF1[i] + ", " + initDistF2[i] + ", " + initCDFF2[i]);
         if ((i + 1) % 5 == 0) {
            outputF0 += "\n";
            outputF1 += "\n";
            outputF2 += "\n";
         }
      }

      outputF0 = outputF0.substring(0, outputF0.length() - 2);
      outputF0 += "};";
      System.out.println("\n\n\n" + outputF0 + "\n");

      outputF1 = outputF1.substring(0, outputF1.length() - 2);
      outputF1 += "};";
      System.out.println(outputF1 + "\n");

      outputF2 = outputF2.substring(0, outputF2.length() - 2);
      outputF2 += "};";
      System.out.println(outputF2);
      System.out.println("\n\n\n" + maxF0 + ", " + maxF1 + ", " + maxF2);
   }


   /**
    * @param args
    * @throws ParseException
    * @throws IOException
    */
   public static void main(String[] args) throws IOException, ParseException {
      ArrayList<String> filenames = new ArrayList<String>();
      
      String baseFile = "/Users/jordanberg/Desktop/tacaa2011/semi/server1/game";
      int min = 1414;
      int max = 1445;
      for(int i = min; i <= max; i++) {
         filenames.add(baseFile + i + ".slg");
      }

      baseFile = "/Users/jordanberg/Desktop/tacaa2011/semi/server2/game";
      min = 609;
      max = 640;
      for(int i = min; i <= max; i++) {
         filenames.add(baseFile + i + ".slg");
      }

      buildBidDistribution(filenames);
   }

}