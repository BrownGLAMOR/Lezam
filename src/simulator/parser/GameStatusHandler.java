package simulator.parser;

import edu.umich.eecs.tac.props.*;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import se.sics.isl.transport.Transportable;
import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * @author jberg
 */
public class GameStatusHandler {


   private GameStatus _gameStatus;


   public GameStatusHandler(String filename) throws IOException, ParseException {
      _gameStatus = parseGameLog(filename);
   }

   private GameStatus parseGameLog(String filename) throws IOException, ParseException {
      InputStream inputStream = new FileInputStream(filename);
      GameLogParser parser = new GameLogParser(new LogReader(inputStream));
      parser.start();
      parser.stop();
      LinkedList<SimParserMessage> messages = parser.getMessages();

      //TODO REPLACE THIS CREATION OF QUERYSPACE
      Set<Query> querySpace = new LinkedHashSet<Query>();
      querySpace.add(new Query(null, null));
      querySpace.add(new Query("lioneer", null));
      querySpace.add(new Query(null, "tv"));
      querySpace.add(new Query("lioneer", "tv"));
      querySpace.add(new Query(null, "audio"));
      querySpace.add(new Query("lioneer", "audio"));
      querySpace.add(new Query(null, "dvd"));
      querySpace.add(new Query("lioneer", "dvd"));
      querySpace.add(new Query("pg", null));
      querySpace.add(new Query("pg", "tv"));
      querySpace.add(new Query("pg", "audio"));
      querySpace.add(new Query("pg", "dvd"));
      querySpace.add(new Query("flat", null));
      querySpace.add(new Query("flat", "tv"));
      querySpace.add(new Query("flat", "audio"));
      querySpace.add(new Query("flat", "dvd"));

      ParticipantInfo[] participants = parser.getParticipants();
      String[] participantNames = parser.getParticipantNames();
      boolean[] isAdvertiser = parser.getIsAdvertiser();
      int numadvertisers = 0;
      ArrayList<String> advertiserslist = new ArrayList<String>();
      for (int i = 0; i < participants.length; i++) {
         if (isAdvertiser[i]) {
            numadvertisers++;
            advertiserslist.add(participantNames[i]);
         }
      }
      String[] advertisers = new String[numadvertisers];
      advertisers = advertiserslist.toArray(advertisers);

      /*
         * These variables will be used save the gamestate
         */
      HashMap<String, LinkedList<BankStatus>> bankStatuses = new HashMap<String, LinkedList<BankStatus>>();
      HashMap<String, LinkedList<BidBundle>> bidBundles = new HashMap<String, LinkedList<BidBundle>>();
      HashMap<String, LinkedList<QueryReport>> queryReports = new HashMap<String, LinkedList<QueryReport>>();
      HashMap<String, LinkedList<SalesReport>> salesReports = new HashMap<String, LinkedList<SalesReport>>();
      HashMap<String, AdvertiserInfo> advertiserInfos = new HashMap<String, AdvertiserInfo>();
      LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists = new LinkedList<HashMap<Product, HashMap<UserState, Integer>>>();

      SlotInfo slotInfo = null;
      ReserveInfo reserveInfo = null;
      PublisherInfo pubInfo = null;
      AdvertiserInfo advInfo = null;
      RetailCatalog retailCatalog = null;
      UserClickModel userClickModel = null;

      for (int i = 0; i < advertisers.length; i++) {
         LinkedList<BankStatus> bankStatuslist = new LinkedList<BankStatus>();
         LinkedList<BidBundle> bidBundlelist = new LinkedList<BidBundle>();
         LinkedList<QueryReport> queryReportlist = new LinkedList<QueryReport>();
         LinkedList<SalesReport> salesReportlist = new LinkedList<SalesReport>();
         bankStatuses.put(advertisers[i], bankStatuslist);
         bidBundles.put(advertisers[i], bidBundlelist);
         queryReports.put(advertisers[i], queryReportlist);
         salesReports.put(advertisers[i], salesReportlist);
      }

      boolean slotinfoflag = false;
      boolean reserveinfoflag = false;
      boolean pubinfoflag = false;
      boolean retailcatalogflag = false;
      boolean userclickmodelflag = false;

      for (int i = 0; i < messages.size(); i++) {
         SimParserMessage mes = messages.get(i);
         int from = mes.getSender();
         int to = mes.getReceiver();
         int messageDay = mes.getDay();
         Transportable content = mes.getContent();
         if (content instanceof BankStatus) {
            if (messageDay >= 1) {
               BankStatus bankstatustemp = (BankStatus) content;
               String name = participantNames[to];
               LinkedList<BankStatus> bankstatuslist = bankStatuses.get(name);
               bankstatuslist.addLast(bankstatustemp);
               bankStatuses.put(name, bankstatuslist);
            }
         } else if (content instanceof SlotInfo && !slotinfoflag) {
            slotInfo = (SlotInfo) content;
            slotinfoflag = true;
         } else if (content instanceof ReserveInfo && !reserveinfoflag) {
            reserveInfo = (ReserveInfo) content;
            reserveinfoflag = true;
         } else if (content instanceof PublisherInfo && !pubinfoflag) {
            pubInfo = (PublisherInfo) content;
            pubinfoflag = true;
         } else if (content instanceof SalesReport) {
            /*
                 * The first three salesreports messages we get are all zeros.
                 * The first one is sent pregame, and the next two are sent on
                 * days one and two before we actually get bank information back
                 * because of the two day lag.
                 */
            if (messageDay >= 2) {
               SalesReport salesreporttemp = (SalesReport) content;
               String name = participantNames[to];
               LinkedList<SalesReport> salesreportlist = salesReports.get(name);
               salesreportlist.addLast(salesreporttemp);
               salesReports.put(name, salesreportlist);
            }
         } else if (content instanceof QueryReport) {
            /*
                 * The first three queryreports messages we get are all zeros.
                 * The first one is sent pregame, and the next two are sent on
                 * days one and two before we actually get bank information back
                 * because of the two day lag.
                 */
            if (messageDay >= 2) {
               QueryReport queryreporttemp = (QueryReport) content;
               String name = participantNames[to];
               LinkedList<QueryReport> queryreportlist = queryReports.get(name);
               queryreportlist.addLast(queryreporttemp);
               queryReports.put(name, queryreportlist);
            }
         } else if (content instanceof RetailCatalog && !retailcatalogflag) {
            retailCatalog = (RetailCatalog) content;
            retailcatalogflag = true;
         } else if (content instanceof BidBundle) {
            if (messageDay <= 58) {
               BidBundle bidbundletemp = (BidBundle) content;
               String name = participantNames[from];
               LinkedList<BidBundle> bidbundlelist = bidBundles.get(name);
               /*
                     * If a bid bundle is missing, it means the person uses the same
                     * bundle as the last time
                     */
               int listsize = bidbundlelist.size();
               if (messageDay > listsize) {
                  for (int j = 0; j < (messageDay - listsize); j++) {
                     if (bidbundlelist.size() > 0) {
                        BidBundle bundleTemp = bidbundlelist.getLast();
                        BidBundle newBundle = copyBundle(bundleTemp);
                        bidbundlelist.add(newBundle);
                     } else {
                        bidbundlelist.add(new BidBundle());
                     }
                  }
               }


               /*
                     * This means they sent a second bid bundle, so write over the last entry
                     */
               while (messageDay < bidbundlelist.size()) {
                  bidbundlelist.removeLast();
               }

               /*
               * Check for NaN's and Negative bids which
               * are replaced by our bid from last round
               */
               for (Query query : bidbundletemp.keys()) {
                  double bid = bidbundletemp.getBid(query);
                  double budget = bidbundletemp.getDailyLimit(query);
                  Ad ad = bidbundletemp.getAd(query);

                  if (bidbundlelist.size() > 0) {
                     if (Double.isNaN(bid) || bid < 0) {
                        bid = bidbundlelist.get(bidbundlelist.size() - 1).getBid(query);
                     }
                     if (Double.isNaN(budget)) {
                        budget = bidbundlelist.get(bidbundlelist.size() - 1).getDailyLimit(query);
                     }
                     if (ad == null) {
                        ad = bidbundlelist.get(bidbundlelist.size() - 1).getAd(query);
                     }
                  } else {
                     if (Double.isNaN(bid) || bid < 0) {
                        bid = 0;
                     }
                     if (Double.isNaN(budget)) {
                        budget = Double.MAX_VALUE;
                     }
                     if (ad == null) {
                        ad = new Ad();
                     }
                  }

                  bidbundletemp.addQuery(query, bid, ad, budget);
               }

               for (Query query : querySpace) {
                  if (!bidbundletemp.containsQuery(query)) {
                     bidbundletemp.addQuery(query, 0, new Ad(), 0);
                  }
               }

               double totalBudget = bidbundletemp.getCampaignDailySpendLimit();
               if (Double.isNaN(totalBudget)) {
                  if (bidbundlelist.size() > 0) {
                     bidbundletemp.setCampaignDailySpendLimit(bidbundlelist.get(bidbundlelist.size() - 1).getCampaignDailySpendLimit());
                  } else {
                     bidbundletemp.setCampaignDailySpendLimit(0);
                  }
               }

               bidbundlelist.addLast(bidbundletemp);
               bidBundles.put(name, bidbundlelist);
            }
         } else if (content instanceof UserClickModel && !userclickmodelflag) {
            userClickModel = (UserClickModel) content;
            userclickmodelflag = true;
         } else if (content instanceof AdvertiserInfo) {
            advInfo = (AdvertiserInfo) content;
            String name = participantNames[to];
            advertiserInfos.put(name, advInfo);
         } else if (content instanceof UserPopulationState) {
            if (messageDay >= 1) {
               UserPopulationState userPopState = (UserPopulationState) content;
               HashMap<Product, HashMap<UserState, Integer>> userDist = new HashMap<Product, HashMap<UserState, Integer>>();
               for (Product p : retailCatalog) {
                  int[] users = userPopState.getDistribution(p);
                  HashMap<UserState, Integer> userDistperProd = new HashMap<UserState, Integer>();
                  userDistperProd.put(UserState.NS, users[0]);
                  userDistperProd.put(UserState.IS, users[1]);
                  userDistperProd.put(UserState.F0, users[2]);
                  userDistperProd.put(UserState.F1, users[3]);
                  userDistperProd.put(UserState.F2, users[4]);
                  userDistperProd.put(UserState.T, users[5]);
                  userDist.put(p, userDistperProd);
               }
               userDists.add(userDist);
            }
         } else {
            //	    		throw new RuntimeException("Unexpected parse token");
         }
      }


      //Ensure everyone has the right number of bid bundles! (59)
      for (int i = 0; i < participants.length; i++) {
         if (isAdvertiser[i]) {
            String name = participantNames[i];
            LinkedList<BidBundle> bidbundlelist = bidBundles.get(name);
            int bundleListSize = bidbundlelist.size();
            if (bundleListSize < 59) {
               if (bundleListSize == 0) {
                  for (int j = 0; j < 59; j++) {
                     bidbundlelist.add(new BidBundle());
                  }
               } else {
                  for (int j = 0; j < (59 - bundleListSize); j++) {
                     BidBundle bundleTemp = bidbundlelist.getLast();
                     BidBundle newBundle = copyBundle(bundleTemp);
                     bidbundlelist.add(newBundle);
                  }
               }
            }
         }
      }


      return new GameStatus(advertisers, bankStatuses, bidBundles, queryReports, salesReports, advertiserInfos,
                            userDists, slotInfo, reserveInfo, pubInfo, retailCatalog, userClickModel);
   }


   private BidBundle copyBundle(BidBundle bundleTemp) {
      BidBundle newBundle = new BidBundle();
      for (Query query : bundleTemp) {
         newBundle.addQuery(query, bundleTemp.getBid(query), bundleTemp.getAd(query), bundleTemp.getDailyLimit(query));
      }
      newBundle.setCampaignDailySpendLimit(bundleTemp.getCampaignDailySpendLimit());
      return newBundle;
   }

   public GameStatus getGameStatus() {
      return _gameStatus;
   }

   @SuppressWarnings("unused")
   private static double[] getStdDevAndMean(ArrayList<Double> list) {
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

   public static String generateWEKADataSet() throws IOException, ParseException {
      String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
      String output = "";
      int min = 1425;
      int max = 1426;
      for (int i = min; i < max; i++) {
         String file = filename + i + ".slg";
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
         HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
         HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
         output += "%" + "\n";
         output += "%" + "\n";
         output += "@RELATION TACAA" + "\n";
         output += "" + "\n";
         output += "@ATTRIBUTE bid NUMERIC" + "\n";
         output += "@ATTRIBUTE position NUMERIC" + "\n";
         output += "@ATTRIBUTE cpc NUMERIC" + "\n";
         output += "@ATTRIBUTE prclick NUMERIC" + "\n";
         output += "@ATTRIBUTE prconv NUMERIC" + "\n";
         output += "@ATTRIBUTE query {null-null,lioneer-null,null-tv,lioneer-tv,null-audio,lioneer-audio,null-dvd,lioneer-dvd,pg-null,pg-tv,pg-audio,pg-dvd,flat-null,flat-tv,flat-audio,flat-dvd}" + "\n";
         output += "" + "\n";
         output += "@DATA" + "\n";
         for (int j = 0; j < 59; j++) {
            Iterator<Query> iter = bidBundles.get("Schlemazl").get(j).iterator();
            while (iter.hasNext()) {
               Query query = (Query) iter.next();
               String adv = "Schlemazl";
               BidBundle bundle = bidBundles.get(adv).get(j);
               QueryReport qreport = queryReports.get(adv).get(j);
               SalesReport sreport = salesReports.get(adv).get(j);
               //bid
               output += bundle.getBid(query);
               //position
               if (Double.isNaN(qreport.getPosition(query))) {
                  output += ",6.0";
               } else {
                  output += "," + qreport.getPosition(query);
               }
               //CPC
               if (Double.isNaN(qreport.getCPC(query))) {
                  output += ",0.0";
               } else {
                  output += "," + qreport.getCPC(query);
               }
               //clickpr
               if (qreport.getClicks(query) == 0 || qreport.getImpressions(query) == 0) {
                  output += ",0.0";
               } else {
                  output += "," + (qreport.getClicks(query) / ((double) qreport.getImpressions(query)));
               }
               //convPr
               if (qreport.getClicks(query) == 0 || sreport.getConversions(query) == 0) {
                  output += ",0.0";
               } else {
                  output += "," + (sreport.getConversions(query) / ((double) qreport.getClicks(query)));
               }
               output += "," + query.getManufacturer() + "-" + query.getComponent() + "\n";
            }
         }
      }
      return output;
   }

   public static HashMap<Query, String> generateRDataSetOnlyBids(int min, int max, String adv) throws IOException, ParseException {
      String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
      //		int min = 1425;
      //		int max = 1426;
      //		String adv = "AstonTAC";
      //		String adv = "MetroClick";
      //		String adv = "Schlemazl";
      //		String adv = "epflagent";
      //		String adv = "QuakTAC";
      //		String adv = "UMTac09";
      //		String adv = "munsey";
      //		String adv = "TacTex";

      HashMap<Query, String> outputs = new HashMap<Query, String>();

      Set<Query> querySpace = new LinkedHashSet<Query>();
      querySpace.add(new Query(null, null));
      querySpace.add(new Query("lioneer", null));
      querySpace.add(new Query(null, "tv"));
      querySpace.add(new Query("lioneer", "tv"));
      querySpace.add(new Query(null, "audio"));
      querySpace.add(new Query("lioneer", "audio"));
      querySpace.add(new Query(null, "dvd"));
      querySpace.add(new Query("lioneer", "dvd"));
      querySpace.add(new Query("pg", null));
      querySpace.add(new Query("pg", "tv"));
      querySpace.add(new Query("pg", "audio"));
      querySpace.add(new Query("pg", "dvd"));
      querySpace.add(new Query("flat", null));
      querySpace.add(new Query("flat", "tv"));
      querySpace.add(new Query("flat", "audio"));
      querySpace.add(new Query("flat", "dvd"));

      for (Query query : querySpace) {
         outputs.put(query, "bid\n");
      }

      for (int i = min; i < max; i++) {
         String file = filename + i + ".slg";
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
         for (int j = 0; j < 59; j++) {
            Iterator<Query> iter = bidBundles.get(adv).get(j).iterator();
            while (iter.hasNext()) {
               Query query = (Query) iter.next();
               BidBundle bundle = bidBundles.get(adv).get(j);
               String output = outputs.get(query);
               output += bundle.getBid(query) + "\n";
               outputs.put(query, output);
            }
         }
      }

      for (Query query : querySpace) {
         String output = outputs.get(query);
         outputs.put(query, output.substring(0, output.length() - 1));
      }

      return outputs;
   }

   public static String generateRDataSet() throws IOException, ParseException {
      String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
      String output = "";
      int min = 1425;
      int max = 1426;
      String adv = "Schlemazl";
      for (int i = min; i < max; i++) {
         String file = filename + i + ".slg";
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
         HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
         HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
         output += "bid pos cpc prclick prconv query \n";
         for (int j = 0; j < 59; j++) {
            Iterator<Query> iter = bidBundles.get(adv).get(j).iterator();
            while (iter.hasNext()) {
               Query query = (Query) iter.next();
               BidBundle bundle = bidBundles.get(adv).get(j);
               QueryReport qreport = queryReports.get(adv).get(j);
               SalesReport sreport = salesReports.get(adv).get(j);
               //bid
               output += bundle.getBid(query);
               //position
               if (Double.isNaN(qreport.getPosition(query))) {
                  output += " 6.0";
               } else {
                  output += " " + qreport.getPosition(query);
               }
               //CPC
               if (Double.isNaN(qreport.getCPC(query))) {
                  output += " 0.0";
               } else {
                  output += " " + qreport.getCPC(query);
               }
               //clickpr
               if (qreport.getClicks(query) == 0 || qreport.getImpressions(query) == 0) {
                  output += " 0.0";
               } else {
                  output += " " + (qreport.getClicks(query) / ((double) qreport.getImpressions(query)));
               }
               //convPr
               if (qreport.getClicks(query) == 0 || sreport.getConversions(query) == 0) {
                  output += " 0.0";
               } else {
                  output += " " + (sreport.getConversions(query) / ((double) qreport.getClicks(query)));
               }
               output += " " + query.getManufacturer() + "-" + query.getComponent() + "\n";
            }
         }
      }
      output = output.substring(0, output.length() - 1);
      return output;
   }

   public static void generateCarletonDataSet2() throws IOException, ParseException {
      String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
      int min = 1425;
      int max = 1430;
      int numSlots = 5;
      System.out.println("Num Advertisers\tNum slots");
      System.out.println("AgentID\tAvgPos\t#Imps\tBids\tBudgets\n");
      Random R = new Random();
      for (int i = min; i < max; i++) {
         String file = filename + i + ".slg";
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
         HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
         String[] advertisers = gameStatus.getAdvertisers();
         int numAdvs = advertisers.length;
         ReserveInfo reserveInfo = gameStatus.getReserveInfo();
         PublisherInfo pubInfo = gameStatus.getPubInfo();
         double squashing = pubInfo.getSquashingParameter();
         UserClickModel clickModel = gameStatus.getUserClickModel();
         for (int j = 0; j < 59; j++) {
            Iterator<Query> iter = bidBundles.get(advertisers[0]).get(j).iterator();
            while (iter.hasNext()) {
               Query query = (Query) iter.next();
               String output = "";
               output += numAdvs + " " + numSlots + "\n";
               for (int k = 0; k < advertisers.length; k++) {
                  String adv = advertisers[k];
                  BidBundle bundle = bidBundles.get(adv).get(j);
                  QueryReport qreport = queryReports.get(adv).get(j);
                  double avgPos = Double.isNaN(qreport.getPosition(query)) ? -1 : qreport.getPosition(query);
                  double squashedBid = bundle.getBid(query) * Math.pow(clickModel.getAdvertiserEffect(clickModel.queryIndex(query), k), squashing);
                  double budget = Double.isNaN(bundle.getDailyLimit(query)) ? -1 : bundle.getDailyLimit(query);
                  if (Double.isNaN(squashedBid)) {
                     throw new RuntimeException();
                  }
                  int numImps = qreport.getImpressions(query);
                  output += (k + 1) + " " + avgPos + " " + numImps + " " + squashedBid + " " + budget + "\n";
               }
               output = output.substring(0, output.length() - 1);
               // Stream to write file
               FileOutputStream fout;

               try {
                  // Open an output stream
                  fout = new FileOutputStream("carleton" + R.nextDouble() + ".o");

                  // Print a line of text
                  new PrintStream(fout).print(output);

                  // Close our output stream
                  fout.close();
               }
               // Catches any error conditions
               catch (IOException e) {
                  System.err.println("Unable to write to file");
                  System.exit(-1);
               }
            }
         }
      }
   }

   public static class AdvBidPair implements Comparable<AdvBidPair> {

      private int _advIdx;
      private double _bid;
      private int _numImps;
      private double _avgPos;

      public AdvBidPair(int advIdx, double bid, int numImps, double avgPos) {
         _advIdx = advIdx;
         _bid = bid;
         _numImps = numImps;
         _avgPos = avgPos;
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

      public int getNumImps() {
         return _numImps;
      }

      public void setNumImps(int numImps) {
         _numImps = numImps;
      }

      public double getAvgPos() {
         return _avgPos;
      }

      public void setAvgPos(double avgPos) {
         _avgPos = avgPos;
      }

      public int compareTo(AdvBidPair agentBidPair) {
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

      public String toString() {
         return _bid + ", " + _avgPos + ", " + _advIdx + ", " + _numImps + "\n";
      }

   }

   public static void main(String[] args) throws IOException, IllegalConfigurationException, ParseException {

      String baseFile = "./game"; //games 1425-1464
      int min = 1;
      int max = 5;
      for (int i = min; i < max; i++) {
         String file = baseFile + i + ".slg";
         System.out.println("PARSING " + file);
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         System.out.println("FINISHED PARSING " + file);
      }


//      int min = 1442;
//      int max = 1443;
//      String adv = "AstonTAC";
      //				String adv = "MetroClick";
      //				String adv = "Schlemazl";
      //				String adv = "epflagent";
      //				String adv = "QuakTAC";
      //				String adv = "UMTac09";
      //				String adv = "munsey";
      //		String adv = "TacTex";
//      HashMap<Query, String> outputs = generateRDataSetOnlyBids(min, max, adv);
//      for (Query query : outputs.keySet()) {
//         FileOutputStream fout;
//
//         try {
//            // Open an output stream
//            fout = new FileOutputStream("Rdata" + min + adv + query.getComponent() + query.getManufacturer() + ".data");
//
//            // Print a line of text
//            new PrintStream(fout).println(outputs.get(query));
//
//            // Close our output stream
//            fout.close();
//         }
//         // Catches any error conditions
//         catch (IOException e) {
//            System.err.println("Unable to write to file");
//            System.exit(-1);
//         }
//      }

      //		generateCarletonDataSet2();


      //		// Stream to write file
      //		FileOutputStream fout;
      //
      //		try
      //		{
      //			// Open an output stream
      //			fout = new FileOutputStream ("Rdata.data");
      //
      //			// Print a line of text
      //			new PrintStream(fout).println(Rdata);
      //
      //			// Close our output stream
      //			fout.close();
      //		}
      //		// Catches any error conditions
      //		catch (IOException e)
      //		{
      //			System.err.println ("Unable to write to file");
      //			System.exit(-1);
      //		}
   }


}
