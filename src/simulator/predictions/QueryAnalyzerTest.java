package simulator.predictions;

import edu.umich.eecs.tac.props.*;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import simulator.predictions.BidPredModelTest.BidPair;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class QueryAnalyzerTest {

   public static final int MAX_F0_IMPS = 10969;
   public static final int MAX_F1_IMPS = 1801;
   public static final int MAX_F2_IMPS = 1423;

   public static int numImpSamples = 10;
   public static int randSeed = 13473; //for sampling
   public Random randGen;

   public static boolean PERFECT_IMPS = true;

   public static int LDS_ITERATIONS_1 = 10;
   public static int LDS_ITERATIONS_2 = 10;
   private static boolean REPORT_FULLPOS_FORSELF = true;

   public QueryAnalyzerTest() {
      randGen = new Random(randSeed);
   }

   public ArrayList<String> getGameStrings() {
      String baseFile = "/Users/jordanberg/Desktop/games/game";
      //		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
//      String baseFile = "./game"; //games 1425-1464
      int min = 1;
      int max = 5;

      //		String baseFile = "/Users/jordanberg/Desktop/qualifiers/game";
      //		String baseFile = "/pro/aa/qualifiers/game"; //games 1425-1464
      //		int min = 309;
      //		int max = 484;



      ArrayList<String> filenames = new ArrayList<String>();
      for(int i = min; i < max; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public void queryAnalyzerPredictionChallenge(AbstractQueryAnalyzer baseModel) throws IOException, ParseException {
      double start = System.currentTimeMillis();

      /*
         * All these maps they are like this: <fileName<agentName,error>>
         */
      HashMap<String,HashMap<String,Double>> ourTotRankErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Double>> ourTotNoMatchRankErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Double>> ourTotRankActualMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Integer>> ourTotRankErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();

      HashMap<String,HashMap<String,Double>> ourTotImpErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Double>> ourTotImpPercErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Double>> ourTotImpActualMegaMap = new HashMap<String,HashMap<String,Double>>();
      HashMap<String,HashMap<String,Integer>> ourTotImpErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();

      ArrayList<String> filenames = getGameStrings();

      int numSlots = 5;
      int numAdvertisers = 8;

      int numInstances = 0;
      int rankCorrect = 0;
      int rankNoMatchCorrect = 0;
      int numNulls = 0;

      for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
         String filename = filenames.get(fileIdx);
         GameStatusHandler statusHandler = new GameStatusHandler(filename);
         GameStatus status = statusHandler.getGameStatus();
         String[] agents = status.getAdvertisers();

         /*
             * One map for each advertiser
             */
         HashMap<String,Double> ourTotRankErrorMap = new HashMap<String, Double>();
         HashMap<String,Double> ourTotNoMatchRankErrorMap = new HashMap<String, Double>();
         HashMap<String,Double> ourTotRankActualMap = new HashMap<String, Double>();
         HashMap<String,Integer> ourTotRankErrorCounterMap = new HashMap<String, Integer>();

         HashMap<String,Double> ourTotImpErrorMap = new HashMap<String, Double>();
         HashMap<String,Double> ourTotImpPercErrorMap = new HashMap<String, Double>();
         HashMap<String,Double> ourTotImpActualMap = new HashMap<String, Double>();
         HashMap<String,Integer> ourTotImpErrorCounterMap = new HashMap<String, Integer>();

         UserClickModel userClickModel = status.getUserClickModel();
         double squashing = status.getPubInfo().getSquashingParameter();

         //Make the query space
         LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
         querySpace.add(new Query(null, null));
         for(Product product : status.getRetailCatalog()) {
            // The F1 query classes
            // F1 Manufacturer only
            querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
         }

         HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
         HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
         HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

         for(int agent = 0; agent<agents.length; agent++) {
            System.gc(); System.gc(); System.gc(); System.gc();
            if(agents[agent].equals("TacTex")) {
               AbstractQueryAnalyzer model = (AbstractQueryAnalyzer) baseModel.getCopy();
               model.setAdvertiser(agents[agent]);

               double ourTotRankError = 0;
               double ourTotNoMatchRankError = 0;
               double ourTotRankActual = 0;
               int ourTotRankErrorCounter = 0;

               double ourTotImpError = 0;
               double ourTotImpPercError = 0;
               double ourTotImpActual = 0;
               int ourTotImpErrorCounter = 0;


               LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
               LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
               LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);
               LinkedList<HashMap<Product, HashMap<UserState, Integer>>> allUserDists = status.getUserDistributions();

               for(int i = 0; i < 57; i++) {
                  System.out.println(i);
                  QueryReport queryReport = ourQueryReports.get(i);
                  SalesReport salesReport = ourSalesReports.get(i);
                  BidBundle bidBundle = ourBidBundles.get(i);

                  HashMap<Query, Integer> totalImpressions = new HashMap<Query,Integer>();
                  if(PERFECT_IMPS) {
                     HashMap<Product, HashMap<UserState, Integer>> userDists = allUserDists.get(i);
                     for(Query q : querySpace) {
                        int imps = 0;
                        for(Product product : status.getRetailCatalog()) {
                           HashMap<UserState, Integer> userDist = userDists.get(product);
                           if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
                              imps += userDist.get(UserState.F0);
                              imps += (1.25/3.0)*userDist.get(UserState.IS);
                           }
                           else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
                              if(product.getComponent().equals(q.getComponent()) || product.getManufacturer().equals(q.getManufacturer())) {
                                 imps += (1.25/2.0)*userDist.get(UserState.F1);
                                 imps += (1.25/6.0)*userDist.get(UserState.IS);
                              }
                           }
                           else {
                              if(product.getComponent().equals(q.getComponent()) && product.getManufacturer().equals(q.getManufacturer())) {
                                 imps += userDist.get(UserState.F2);
                                 imps += (1.25/3.0)*userDist.get(UserState.IS);
                              }
                           }
                        }
                        totalImpressions.put(q, imps);
                     }
                  }
                  else {
                     for(Query q : querySpace) {
                        int numImps;
                        if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                           numImps = MAX_F0_IMPS;
                        }
                        else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                           numImps = MAX_F1_IMPS;
                        }
                        else {
                           numImps = MAX_F2_IMPS;
                        }
                        totalImpressions.put(q, numImps);
                     }
                  }

                  //Generate true waterfall (with all agents with > 0 imps)
                  HashMap<Query, int[]> allOrders = new HashMap<Query,int[]>();
                  HashMap<Query, int[]> allImpressions = new HashMap<Query,int[]>();
                  HashMap<Query, int[][]> allWaterfalls = new HashMap<Query,int[][]>();
                  HashMap<Query, HashMap<Integer,Integer>> allArrayIDMaps = new HashMap<Query, HashMap<Integer, Integer>>();
                  for(Query q : querySpace) {
                     HashMap<Integer,Integer> arrayIDMaps = new HashMap<Integer, Integer>();
                     int[] impressions = new int[numAdvertisers];
                     ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
                     int numSkipped = 0;
                     for(int agentInner = 0; agentInner < agents.length; agentInner++) {
                        QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
                        impressions[agentInner] = innerQueryReport.getImpressions(q);
                        if(impressions[agentInner]  > 0) {
                           BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                           double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                           double bid = innerBidBundle.getBid(q);
                           double squashedBid = bid * Math.pow(advEffect, squashing);
                           bidPairs.add(new BidPair(agentInner, squashedBid));
                           arrayIDMaps.put(agentInner,agentInner-numSkipped);
                        }
                        else {
                           arrayIDMaps.put(agentInner,-1);
                           numSkipped++;
                        }
                     }

                     Collections.sort(bidPairs);

                     int[] order = new int[bidPairs.size()];
                     int[] nonZeroImps = new int[bidPairs.size()];
                     for(int j = 0; j < bidPairs.size(); j++) {
                        int agentID = bidPairs.get(j).getID();
                        int nonZeroID = arrayIDMaps.get(agentID);
                        order[j] = nonZeroID;
                        nonZeroImps[nonZeroID] = impressions[agentID];
                     }

                     int[][] waterfall = greedyAssign(numSlots,bidPairs.size(),order,nonZeroImps);

                     allOrders.put(q, order);
                     allImpressions.put(q, nonZeroImps);
                     allWaterfalls.put(q, waterfall);
                     allArrayIDMaps.put(q,arrayIDMaps);
                  }

                  QueryReport newSampledQueryReport = new QueryReport();
                  for(Query q : querySpace) {
                     newSampledQueryReport.addQuery(q,queryReport.getRegularImpressions(q),
                                                    queryReport.getPromotedImpressions(q),
                                                    queryReport.getClicks(q),
                                                    queryReport.getCost(q),
                                                    queryReport.getPosition(q)*(queryReport.getRegularImpressions(q)+queryReport.getPromotedImpressions(q)));

                     int[][] waterfall = allWaterfalls.get(q);
                     int[] impressions = allImpressions.get(q);
                     int[] order = allOrders.get(q);

                     int totalImps = 0;
                     for(int agentInner = 0; agentInner<waterfall.length; agentInner++) {
                        totalImps += waterfall[agentInner][0];
                     }

                     if(totalImps > 0) {

                        ArrayList<Integer> waterfallSamples = new ArrayList<Integer>();
                        for(int sample = 0; sample < numImpSamples; sample++) {
                           waterfallSamples.add(randGen.nextInt(totalImps)+1);
                        }

                        Collections.sort(waterfallSamples);

                        HashMap<Integer,Integer> arrayIDMap = allArrayIDMaps.get(q);
                        for(int agentInner = 0; agentInner<agents.length; agentInner++) {
                           if(agent != agentInner) {
                              int arrayID = arrayIDMap.get(agentInner);
                              if(arrayID  > 0) {
                                 double avgPos = 0.0;
                                 for(Integer sample : waterfallSamples) {
                                    //it would probably be faster if we calculated the average for all agents at the same time
                                    int pos = getPosition(arrayID, sample, order,impressions,waterfall);
                                    if(pos > 0) {
                                       avgPos += pos;
                                    }
                                    else if(pos == 0) {
                                       //out of auctions
                                       break;
                                    }
                                    else {
                                       //not in auctions yet
                                       continue;
                                    }
                                 }

                                 if(avgPos > 0)  {
                                    avgPos /= numImpSamples;
                                 }
                                 else {
                                    avgPos = Double.NaN;
                                 }

                                 newSampledQueryReport.setPosition(q,"adv" + (agentInner+1),avgPos);
                              }
                              else {
                                 newSampledQueryReport.setPosition(q,"adv" + (agentInner+1),Double.NaN);
                              }
                           }
                        }
                     }
                  }


                  model.updateModel(newSampledQueryReport, salesReport, bidBundle, totalImpressions);

                  for(Query q : querySpace) {
                     boolean skip = false;
                     HashMap<Integer,Integer> imps = new HashMap<Integer,Integer>();
                     for(int agentInner = 0; agentInner < agents.length; agentInner++) {
                        QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
                        int imp = innerQueryReport.getImpressions(q);
                        if(agentInner == agent && imp == 0) {
                           skip = true;
                        }
                        imps.put(agentInner, imp);
                     }

                     if(!skip) {
                        ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
                        for(int agentInner = 0; agentInner < agents.length; agentInner++) {
                           BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
                           double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
                           double bid = innerBidBundle.getBid(q);
                           double squashedBid = bid * Math.pow(advEffect, squashing);
                           bidPairs.add(new BidPair(agentInner, squashedBid));
                        }

                        Collections.sort(bidPairs);

                        HashMap<Integer, Integer> queryRanks = new HashMap<Integer,Integer>();
                        for(int j = 0; j < bidPairs.size(); j++) {
                           queryRanks.put(bidPairs.get(j).getID(), j);
                        }

                        int[] rankPred = model.getOrderPrediction(q);
                        int[] impsPred = model.getImpressionsPrediction(q);

                        int totImps = 0;
                        for(int j = 0; j < impsPred.length; j++) {
                           totImps += impsPred[j];
                        }

                        if(totImps > 0) {
                           HashMap<Integer, Integer> rankPredMap = new HashMap<Integer,Integer>();
                           for(int j = 0; j < rankPred.length; j++) {
                              rankPredMap.put(rankPred[j], j);
                           }

                           int skipped = 0;
                           int totalDiff = 0;
                           int totalNoMatchDiff = 0;
                           for(int j = 0; j < agents.length; j++) {
                              double avgPos;
                              if(j == agent) {
                                 if(REPORT_FULLPOS_FORSELF) {
                                    avgPos = allQueryReports.get(agents[j]).get(i).getPosition(q);
                                 }
                                 else {
                                    avgPos = allQueryReports.get(agents[j]).get(i).getPosition(q, "adv" + (j+1));
                                 }
                              }
                              else {
                                 avgPos = allQueryReports.get(agents[j]).get(i).getPosition(q, "adv" + (j+1));
                              }

                              if(Double.isNaN(avgPos) || avgPos < 1) {
                                 skipped++;
                                 continue;
                              }

//                              System.out.println("AvgPos: " + avgPos);
//                              System.out.println("j: " + j);
//                              System.out.println("skipped: " + skipped);
//                              System.out.println(rankPredMap);

                              if(j - skipped >= rankPred.length) {
                                 continue;
                              }

                              int rankPredVal = rankPredMap.get(j-skipped);
                              int queryRankVal = queryRanks.get(j);
                              double rankError = Math.abs(rankPredVal - queryRankVal); //MAE
                              ourTotRankActual += queryRankVal;
                              ourTotRankError += rankError;
                              ourTotRankErrorCounter++;
                              totalDiff += rankError;

                              boolean matchingAvgPos = false;
                              for(int k = 0; k < agents.length; k++) {
                                 double otherAvgPos;
                                 if(k == agent) {
                                    if(REPORT_FULLPOS_FORSELF) {
                                       otherAvgPos = allQueryReports.get(agents[k]).get(i).getPosition(q);
                                    }
                                    else {
                                       otherAvgPos = allQueryReports.get(agents[k]).get(i).getPosition(q, "adv" + (k+1));
                                    }
                                 }
                                 else {
                                    otherAvgPos = allQueryReports.get(agents[k]).get(i).getPosition(q, "adv" + (k+1));
                                 }
                                 if((k != j) && otherAvgPos == avgPos) {
                                    matchingAvgPos = true;
                                    break;
                                 }
                              }

                              if(!matchingAvgPos) {
                                 ourTotNoMatchRankError += rankError;
                                 totalNoMatchDiff += rankError;
                              }

                              double impError = Math.abs(impsPred[j-skipped] - imps.get(j)); //MAE
                              ourTotImpActual += imps.get(j);
                              ourTotImpError += impError;
                              ourTotImpPercError += (impError / imps.get(j))*100;
                              ourTotImpErrorCounter++;
                           }


                           numInstances++;
                           if(totalDiff == 0) {
                              rankCorrect++;
                           }

                           if(totalNoMatchDiff == 0) {
                              rankNoMatchCorrect++;
                           }
                        }
                        else {
                           numNulls++;
                        }
                     }
                  }
               }
               ourTotRankErrorMap.put(agents[agent],ourTotRankError);
               ourTotNoMatchRankErrorMap.put(agents[agent],ourTotNoMatchRankError);
               ourTotRankActualMap.put(agents[agent],ourTotRankActual);
               ourTotRankErrorCounterMap.put(agents[agent],ourTotRankErrorCounter);

               ourTotImpErrorMap.put(agents[agent],ourTotImpError);
               ourTotImpPercErrorMap.put(agents[agent],ourTotImpPercError);
               ourTotImpActualMap.put(agents[agent],ourTotImpActual);
               ourTotImpErrorCounterMap.put(agents[agent],ourTotImpErrorCounter);
            }
         }

         ourTotRankErrorMegaMap.put(filename,ourTotRankErrorMap);
         ourTotNoMatchRankErrorMegaMap.put(filename,ourTotNoMatchRankErrorMap);
         ourTotRankActualMegaMap.put(filename,ourTotRankActualMap);
         ourTotRankErrorCounterMegaMap.put(filename,ourTotRankErrorCounterMap);

         ourTotImpErrorMegaMap.put(filename,ourTotImpErrorMap);
         ourTotImpPercErrorMegaMap.put(filename,ourTotImpPercErrorMap);
         ourTotImpActualMegaMap.put(filename,ourTotImpActualMap);
         ourTotImpErrorCounterMegaMap.put(filename,ourTotImpErrorCounterMap);
      }


      ArrayList<Double> rankRMSEList = new ArrayList<Double>();
      ArrayList<Double> rankNoMatchRMSEList = new ArrayList<Double>();
      ArrayList<Double> rankActualList = new ArrayList<Double>();

      ArrayList<Double> impRMSEList = new ArrayList<Double>();
      ArrayList<Double> impPercRMSEList = new ArrayList<Double>();
      ArrayList<Double> impActualList = new ArrayList<Double>();
      for(String file : filenames) {
         HashMap<String, Double> totRankErrorMap = ourTotRankErrorMegaMap.get(file);
         HashMap<String, Double> totNoMatchRankErrorMap = ourTotNoMatchRankErrorMegaMap.get(file);
         HashMap<String, Double> totRankActualMap = ourTotRankActualMegaMap.get(file);
         HashMap<String, Integer> totRankErrorCounterMap = ourTotRankErrorCounterMegaMap.get(file);

         HashMap<String, Double> totImpErrorMap = ourTotImpErrorMegaMap.get(file);
         HashMap<String, Double> totImpPercErrorMap = ourTotImpPercErrorMegaMap.get(file);
         HashMap<String, Double> totImpActualMap = ourTotImpActualMegaMap.get(file);
         HashMap<String, Integer> totImpErrorCounterMap = ourTotImpErrorCounterMegaMap.get(file);

         for(String agent : totRankErrorCounterMap.keySet()) {
            double totRankError = totRankErrorMap.get(agent);
            double totNoMatchRankError = totNoMatchRankErrorMap.get(agent);
            double totRankActual = totRankActualMap.get(agent);
            double totRankErrorCounter = totRankErrorCounterMap.get(agent);
            double rankMAE;
            rankMAE = (totRankError/totRankErrorCounter);
            double rankActual = totRankActual/totRankErrorCounter;
            rankRMSEList.add(rankMAE);
            rankActualList.add(rankActual);

            double rankNoMatchMAE;
            rankNoMatchMAE = (totNoMatchRankError/totRankErrorCounter);
            rankNoMatchRMSEList.add(rankNoMatchMAE);

            double totImpError = totImpErrorMap.get(agent);
            double totImpActual = totImpActualMap.get(agent);
            double totImpErrorCounter = totImpErrorCounterMap.get(agent);
            double impMAE;
            impMAE = (totImpError/totImpErrorCounter);
            double impActual = totImpActual/totImpErrorCounter;
            impRMSEList.add(impMAE);
            impActualList.add(impActual);

            double totImpPercError = totImpPercErrorMap.get(agent);
            double impPercMAE = (totImpPercError/totImpErrorCounter);
            impPercRMSEList.add(impPercMAE);
         }
      }

      double[] rankRmseStd = getStdDevAndMean(rankRMSEList);
      double[] rankNoMatchRmseStd = getStdDevAndMean(rankNoMatchRMSEList);
      double[] impRmseStd = getStdDevAndMean(impRMSEList);
      double[] impPercRmseStd = getStdDevAndMean(impPercRMSEList);
      double stop = System.currentTimeMillis();
      double elapsed = (stop - start)/1000.0;
      System.out.println("Model, Rank Err, No Match Rank Err, Imp Err, Imp % err, Num Instances, % null, % correct rank, % no match correct rank, time");
      System.out.println(baseModel + ", " + rankRmseStd[0] + ", " + rankNoMatchRmseStd[0] + ", " + impRmseStd[0] + ", " + impPercRmseStd[0] + ", " + numInstances + ", " +
                                 (numNulls/((double) (numInstances+numNulls))) + ", " + (rankCorrect/((double) (numInstances))) + ", " +
                                 (rankNoMatchCorrect/((double) (numInstances))) + ", " + elapsed);
   }


   /*
   * Returns the positions of the agentID at the impression, 0 if they are out of the auction, and -1
   * if they aren't in the auction yet
   *
   */
   private int getPosition(int agentID, int impression, int[] order, int[] impressions, int[][] waterfall) {

      //check to see if the arrayID is valid
      if(agentID < 0 || agentID >= order.length) {
         return 0;
      }

      int ourOrder = 0;
      for(int i = 0; i < order.length; i++) {
         if (order[i] == agentID) {
            ourOrder = i;
            break;
         }
      }

      int[] impsPerSlot = waterfall[agentID];
      int numSlots = impsPerSlot.length;

      //calculate the impression offset (i.e. number of impressions seen before we enter the auction)
      int offset;
      if(order.length <= numSlots || ourOrder < numSlots) {
         offset = 0;
      }
      else {
         offset = 0;
         int numAgentsAhead = ourOrder - (numSlots-1);
         int[] offsets = new int[numAgentsAhead];
         for(int i = 0; i < numAgentsAhead; i++) {
            ArrayList<Integer> imps = new ArrayList<Integer>();
            for(int j = (int)(i*1.0); j < numSlots+i && j < impressions.length; j++) {
               if(j < numSlots) {
                  imps.add(impressions[j]);
               }
               else {
                  imps.add(impressions[j] + offsets[j - numSlots]);
               }
            }

            //find min number of imps before drop
            int minImps = Collections.min(imps);

            offsets[i] = minImps;
         }
         offset = offsets[numAgentsAhead-1];
      }

      //check to see if we are in auction yet
      if (offset > impression) {
         return -1;
      }

      impression -= offset;

      int totalImps = impressions[agentID];

      //check to see if we are out of auction
      if(impression > totalImps) {
         return 0;
      }

      int lastImps = 0;
      int currImps = 0;
      for(int i = 0; i < numSlots; i++) {
         currImps += impsPerSlot[numSlots-1-i];
         if(impression >= lastImps &&
                 impression <= currImps) {
            return numSlots-i;
         }
         else {
            lastImps = currImps+1;
         }
      }
      return 0;
   }

   public class ImpPair {

      private int _id;
      private int _imp;
      public ImpPair(int id, int imp) {
         _id = id;
         _imp = imp;
      }

      public int getID() {
         return _id;
      }

      public int getImp() {
         return _imp;
      }
   }

   private double[] getStdDevAndMean(ArrayList<Double> list) {
      double n = list.size();
      double sum = 0.0;
      for(Double data : list) {
         sum += data;
      }
      double mean = sum/n;

      double variance = 0.0;

      variance /= (n-1);

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

      int[] slotStart= new int[slots];
      int a;

      for(int i = 0; i < agents; ++i){
         a = order[i];
         //System.out.println(a);
         int remainingImp = impressions[a];
         //System.out.println("remaining impressions "+ impressions[a]);
         for(int s = Math.min(i+1, slots)-1; s>=0; --s){
            if(s == 0){
               impressionsBySlot[a][0] = remainingImp;
               slotStart[0] += remainingImp;
            }
            else{
               int r = slotStart[s-1] - slotStart[s];
               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
               assert(r >= 0);
               if(r < remainingImp){
                  remainingImp -= r;
                  impressionsBySlot[a][s] = r;
                  slotStart[s] += r;
               }
               else {
                  impressionsBySlot[a][s] = remainingImp;
                  slotStart[s] += remainingImp;
                  break;
               }
            }
         }
      }
      return impressionsBySlot;
   }

   public static void main(String[] args) throws IOException, ParseException  {
      QueryAnalyzerTest evaluator = new QueryAnalyzerTest();

      ArrayList<String> filenames = evaluator.getGameStrings();
      String filename = filenames.get(0);
      GameStatusHandler statusHandler = new GameStatusHandler(filename);
      GameStatus status = statusHandler.getGameStatus();
      String[] agents = status.getAdvertisers();

      ArrayList<String> advertisers = new ArrayList<String>();

      for(int i = 0; i < agents.length; i++) {
         advertisers.add(agents[i]);
      }

      //Make the query space
      LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
      querySpace.add(new Query(null, null));
      for(Product product : status.getRetailCatalog()) {
         // The F1 query classes
         // F1 Manufacturer only
         querySpace.add(new Query(product.getManufacturer(), null));
         // F1 Component only
         querySpace.add(new Query(null, product.getComponent()));

         // The F2 query class
         querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
      }

      double start = System.currentTimeMillis();

      //		evaluator.queryAnalyzerPredictionChallenge(new GreedyQueryAnalyzer(querySpace, advertisers, "this will be overwritten"));
      evaluator.queryAnalyzerPredictionChallenge(new CarletonQueryAnalyzer(querySpace, advertisers, "this will be overwritten", LDS_ITERATIONS_1, LDS_ITERATIONS_2, REPORT_FULLPOS_FORSELF));
//		evaluator.queryAnalyzerPredictionChallenge(new GQA(querySpace, advertisers, "this will be overwritten"));


      double stop = System.currentTimeMillis();
      double elapsed = stop - start;
      //		System.out.println("This took " + (elapsed / 1000) + " seconds");
   }

}
