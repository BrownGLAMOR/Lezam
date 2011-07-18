package simulator.parser;

import edu.umich.eecs.tac.props.*;
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


   /**
    * This enum represents all the states a user
    * can be in in TAC AA
    *
    * @author jberg
    */
   public static enum UserState {
      NS, IS, F0, F1, F2, T
   }

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
                        bidbundlelist.add(mkEmptyBundle(querySpace));
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
               BidBundle newBidBundle = new BidBundle();
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

                  newBidBundle.addQuery(query, bid, ad, budget);
               }

               for (Query query : querySpace) {
                  if (!newBidBundle.containsQuery(query)) {
                     newBidBundle.addQuery(query, 0, new Ad(), 0);
                  }
               }

               double totalBudget = bidbundletemp.getCampaignDailySpendLimit();
               if (Double.isNaN(totalBudget)) {
                  if (bidbundlelist.size() > 0) {
                     totalBudget = bidbundlelist.get(bidbundlelist.size() - 1).getCampaignDailySpendLimit();
                  } else {
                     totalBudget = 0;
                  }
               }

               newBidBundle.setCampaignDailySpendLimit(totalBudget);

               bidbundlelist.addLast(newBidBundle);
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

   private BidBundle mkEmptyBundle(Set<Query> querySpace) {
      BidBundle bundle = new BidBundle();
      for(Query q : querySpace) {
         bundle.addQuery(q,0.0,new Ad(),Double.MAX_VALUE);
      }
      bundle.setCampaignDailySpendLimit(Double.MAX_VALUE);
      return bundle;
   }

   public GameStatus getGameStatus() {
      return _gameStatus;
   }

   public static void main(String[] args) throws IOException, IllegalConfigurationException, ParseException {

      String baseFile = "/Users/jordanberg/Desktop/tacaa2011/semi/server1/game";
      int min = 1414;
      int max = 1445;

//      String baseFile = "/Users/jordanberg/Desktop/tacaa2011/semi/server2/game";
//      int min = 609;
//      int max = 640;


      for (int i = min; i <= max; i++) {
         String file = baseFile + i + ".slg";
         System.out.println("PARSING " + file);
         double start = System.currentTimeMillis();
         GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
         double stop = System.currentTimeMillis();
         double secondsElapsed = (stop - start) / 1000.0;
         System.out.println("SECONDS ELAPSED: " + secondsElapsed);
         GameStatus gameStatus = gameStatusHandler.getGameStatus();
         System.out.println(i + ": " + Arrays.toString(gameStatus.getAdvertisers()));
         Set<Query> querySpace = gameStatus.getQuerySpace();
         for(String agent : gameStatus.getAdvertisers()) {
            LinkedList<BidBundle> bundles = gameStatus.getBidBundles().get(agent);
            int numBundle = 0;
            for (BidBundle bundle : bundles) {
               for (Query query : querySpace) {
                  if (Double.isNaN(bundle.getBid(query))) {
                     System.out.println(bundle);
                     throw new RuntimeException(numBundle + ", " + agent + ", " + query.toString());
                  } else if (Double.isNaN(bundle.getDailyLimit(query))) {
                     throw new RuntimeException(numBundle + ", " + agent + ", " + query.toString());
                  }
               }
               if (Double.isNaN(bundle.getCampaignDailySpendLimit())) {
                  throw new RuntimeException(numBundle + ", " + agent);
               }
               numBundle++;
            }
         }
//         int capacity = gameStatus.getAdvertiserInfos().get("TacTex").getDistributionCapacity();
//         if(capacity == 300) {
//            System.out.println("mv Result-" + i + " .txt low");
//         }
//         else if(capacity == 450) {
//            System.out.println("mv Result-" + i + " .txt med");
//         }
//         else if(capacity == 600) {
//            System.out.println(i);
//            System.out.println("mv Result-" + i + " .txt high");
//         }
         System.out.println("FINISHED PARSING " + file);
      }
   }
}
