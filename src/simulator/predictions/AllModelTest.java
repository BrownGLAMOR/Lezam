package simulator.predictions;

import agents.modelbased.MCKP;
import clojure.lang.PersistentHashMap;
import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.ISratio.ISRatioModel;
import models.adtype.AdTypeEstimator;
import models.advertiserspecialties.SimpleSpecialtyModel;
import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import models.budgetEstimator.AbstractBudgetEstimator;
import models.budgetEstimator.BudgetEstimator;
import models.paramest.AbstractParameterEstimation;
import models.paramest.BayesianParameterEstimation;
import models.paramest.NaiveParameterEstimation;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import models.usermodel.jbergParticleFilter;
import se.sics.tasim.aw.Message;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import tacaa.javasim;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static agents.modelbased.MCKP.getMaxImpsPred;
import static agents.modelbased.MCKP.getUserStates;
import static models.paramest.ConstantsAndFunctions.*;
import static simulator.AgentSimulator.setupSimulator;
import static simulator.predictions.ImpressionEstimatorTest.getIndicesForDescendingOrder;

public class AllModelTest {

   public AllModelTest() {
      _rand = new Random(_seed);
   }

   private static boolean SUMMARY = true;

   Random _rand;
   long _seed = 1616867;

   /*
    * Query Analyzer Params
    */
   boolean TEST_QA = true;

   /*
    * User Model Params
    */
   boolean TEST_USER_MODEL = true;  //Turns user model testing on
   boolean USER_MODEL_UB = true; //User model is perfect, not based on QA output

   /*
    * Bid Model Params
    */
   boolean TEST_BID = true;
   boolean BID_RANKABLE = false;

   /*
    * Budget Model Params
    */
   boolean TEST_BUDGET = true;
   boolean BUDGET_RANKABLE = false;

   /*
    * ParamEst Model Params
    */
   boolean TEST_PARAM_EST = true;
   boolean PARAM_EST_NAIVE = false;

   /*
    * LIVE MODE
    *  -Actually bid with our agent instead of using a log!
    */
   boolean LIVE_MODE = false;

   PersistentHashMap cljSim = null;

   public static final int MAX_F0_IMPS = 10969;
   public static final int MAX_F1_IMPS = 1801;
   public static final int MAX_F2_IMPS = 1468;

   public enum GameSet {
      finals2010, semifinals2010, test2010, semi2011server1, semi2011server2
   }

   public static boolean isMac(){
      String os = System.getProperty("os.name").toLowerCase();
      return (os.indexOf( "mac" ) >= 0); //Mac
   }

   public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) {
         baseFile = "./game";
      }
      else if (GAMES_TO_TEST == GameSet.finals2010) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
         }
         else baseFile = "/pro/aa/finals2010/game-tacaa1-";
      }
      else if (GAMES_TO_TEST == GameSet.semi2011server1) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2011/semi/server1/game";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2011/semi/server1/game";
         }
         else baseFile = "/pro/aa/tacaa2011/semi/server1/game";
      }
      else if (GAMES_TO_TEST == GameSet.semi2011server2) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2011/semi/server2/game";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2011/semi/server2/game";
         }
         else baseFile = "/pro/aa/tacaa2011/semi/server2/game";
      }

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public double[] allModelPredictionChallenge(GameSet GAMES_TO_TEST, int START_GAME, int END_GAME,
                                               int START_DAY, int END_DAY, int START_QUERY, int END_QUERY,
                                               String agentToReplace) throws IOException, ParseException {

      int numEstsPerQuery = (END_DAY-START_DAY+1)*(END_QUERY-START_QUERY+1)*8;
      int numEsts = (END_DAY-START_DAY+1);

      ArrayList<Double> impErrorList = new ArrayList<Double>(numEstsPerQuery);

      ArrayList<Double> impInOrderErrorList = new ArrayList<Double>(numEstsPerQuery);

      ArrayList<Double> bidErrorList = new ArrayList<Double>(numEstsPerQuery);

      ArrayList<Double> bidInOrderErrorList = new ArrayList<Double>(numEstsPerQuery);

      ArrayList<Double> budgetErrorType1List = new ArrayList<Double>(numEstsPerQuery);
      ArrayList<Double> budgetErrorType2List = new ArrayList<Double>(numEstsPerQuery);
      ArrayList<Double> budgetErrorType3List = new ArrayList<Double>(numEstsPerQuery);
      ArrayList<Double> budgetErrorType5List = new ArrayList<Double>(numEstsPerQuery);
      int numBudgetType4Errs_Tot = 0;
      int numBudgetType6Errs_Tot = 0;

      ArrayList<Double> advEffectErrList = new ArrayList<Double>(numEsts);

      ArrayList<Double> contProbEffectsErrList = new ArrayList<Double>(numEsts);

      ArrayList<Double> userStateErrList = new ArrayList<Double>(numEstsPerQuery);

      ArrayList<Double> f0RegResErrList = new ArrayList<Double>(numEsts);
      ArrayList<Double> f1RegResErrList = new ArrayList<Double>(numEsts);
      ArrayList<Double> f2RegResErrList = new ArrayList<Double>(numEsts);
      ArrayList<Double> f0PromResErrList = new ArrayList<Double>(numEsts);
      ArrayList<Double> f1PromResErrList = new ArrayList<Double>(numEsts);
      ArrayList<Double> f2PromResErrList = new ArrayList<Double>(numEsts);

      ArrayList<Double> agentProfitDiffList = new ArrayList<Double>(numEsts);

      ArrayList<String> filenames = getGameStrings(GAMES_TO_TEST, START_GAME, END_GAME);
      for (int gameIdx = 0; gameIdx < filenames.size(); gameIdx++) {
         String filename = filenames.get(gameIdx);
         if(SUMMARY) {
            System.out.println("Game " + gameIdx + ", " + filename);
         }

         // Load this game and its basic parameters
         GameStatus status = new GameStatusHandler(filename).getGameStatus();
         String[] statusAgents = status.getAdvertisers();
         System.out.println("Agents: " + Arrays.toString(statusAgents));
         int NUM_PROMOTED_SLOTS = status.getSlotInfo().getPromotedSlots();
         int NUM_SLOTS = status.getSlotInfo().getRegularSlots();
         HashMap<QueryType, Double> promotedReserveScores = getApproximatePromotedReserveScore(status);
         HashMap<QueryType, Double> regularReserveScores = getApproximateRegularReserveScore(status);

         int replaceIdx = -1;
         for(int i = 0; i < statusAgents.length; i++) {
            String statusAgent = statusAgents[i];
            if(agentToReplace.equalsIgnoreCase(statusAgent)) {
               replaceIdx = i;
            }
         }

         if(replaceIdx == -1) {
            System.out.println("Agent (" + agentToReplace+ " ) not present, skipping game " + filename);
            continue;
         }

         double squashParam = status.getPubInfo().getSquashingParameter();

         Set<Product> products = new HashSet<Product>();
         for(Product p : status.getRetailCatalog()) {
            products.add(p);
         }

         // Make the query space
         Set<Query> querySpace = status.getQuerySpace();

         Query[] queryArr = new Query[querySpace.size()];
         querySpace.toArray(queryArr);

         double[] convProbs = new double[] {0.04, 0.12, 0.29};
         HashSet<String> advertisersSet = new HashSet<String>();
         ArrayList<String> advertisers = new ArrayList<String>();
         for (int i = 1; i <= 8; i++) {
            advertisers.add("adv" + i);
            advertisersSet.add("adv" + i);
         }
         double _randJump = .1;
         double _yestBid = .5;
         double _5DayBid = .4;
         double _bidStdDev = 2.0;

         List<AbstractQueryAnalyzer> queryAnalyzerList = new ArrayList<AbstractQueryAnalyzer>();
         for (int i = 0; i < advertisers.size(); i++) {
            if(i == replaceIdx) {
               AbstractQueryAnalyzer queryAnalyzer = new CarletonQueryAnalyzer(querySpace,advertisers,"adv"+(i+1),true,true);
               queryAnalyzerList.add(queryAnalyzer);
            }
            else {
               queryAnalyzerList.add(null);
            }
         }

         List<ParticleFilterAbstractUserModel> userModelList = new ArrayList<ParticleFilterAbstractUserModel>();
         for (int i = 0; i < advertisers.size(); i++) {
            if(i == replaceIdx) {
               ParticleFilterAbstractUserModel userModel = new jbergParticleFilter(convProbs, NUM_SLOTS, NUM_PROMOTED_SLOTS);
               userModelList.add(userModel);
            }
            else {
               userModelList.add(null);
            }
         }

         List<AbstractBudgetEstimator> budgetModelList = new ArrayList<AbstractBudgetEstimator>();
         for (int i = 0; i < advertisers.size(); i++) {
            if(i == replaceIdx) {
               AbstractBudgetEstimator budgetModel = new BudgetEstimator(querySpace, i, NUM_SLOTS, NUM_PROMOTED_SLOTS,squashParam);
               budgetModelList.add(budgetModel);
            }
            else {
               budgetModelList.add(null);
            }
         }

         List<AbstractBidModel> bidModelList = new ArrayList<AbstractBidModel>();
         for (int i = 0; i < advertisers.size(); i++) {
            if(i == replaceIdx) {
               AbstractBidModel bidModel = new IndependentBidModel(advertisersSet, "adv" + (i+1),1,_randJump,_yestBid,_5DayBid,_bidStdDev,querySpace);
               bidModelList.add(bidModel);
            }
            else {
               bidModelList.add(null);
            }
         }


         List<AbstractParameterEstimation> paramModelList = new ArrayList<AbstractParameterEstimation>();
         for (int i = 0; i < advertisers.size(); i++) {
            if(i == replaceIdx) {
               AbstractParameterEstimation paramModel;
               if(PARAM_EST_NAIVE) {
                  paramModel = new NaiveParameterEstimation();
               }
               else {
                  paramModel = new BayesianParameterEstimation(i,NUM_SLOTS, NUM_PROMOTED_SLOTS, squashParam, querySpace);
               }
               paramModelList.add(paramModel);
            }
            else {
               paramModelList.add(null);
            }
         }

         List<AbstractUnitsSoldModel> unitSoldModelList = null;
         List<ISRatioModel> ISRatioModelList = null;
         List<AdTypeEstimator> adTypeEstimatorList = null;
         List<SimpleSpecialtyModel> specialtyModelList = null;
         List<MCKP> agentList = null;
         List<ArrayList<BidBundle>> bidBundlesList = null;
         List<ArrayList<QueryReport>> queryReportsList = null;
         List<ArrayList<SalesReport>> salesReportsList = null;
         List<ArrayList<Integer>> salesListList = null;
         if(LIVE_MODE) {
            //Fuck it! We'll do it live!
            unitSoldModelList = new ArrayList<AbstractUnitsSoldModel>();
            ISRatioModelList = new ArrayList<ISRatioModel>();
            adTypeEstimatorList = new ArrayList<AdTypeEstimator>();
            specialtyModelList = new ArrayList<SimpleSpecialtyModel>();
            agentList = new ArrayList<MCKP>();
            bidBundlesList = new ArrayList<ArrayList<BidBundle>>();
            queryReportsList = new ArrayList<ArrayList<QueryReport>>();
            salesReportsList = new ArrayList<ArrayList<SalesReport>>();
            salesListList = new ArrayList<ArrayList<Integer>>();
            cljSim = setupSimulator(filename);
            for(int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  MCKP agent = new MCKP(0.3,0.5,0.1,1.0,1.0,1.0,1.0,1.0, MCKP.MultiDay.HillClimbing,10);
                  agent.sendSimMessage(new Message("bla","bla", status.getPubInfo()));
                  agent.sendSimMessage(new Message("bla","bla", status.getSlotInfo()));
                  agent.sendSimMessage(new Message("bla","bla", status.getRetailCatalog()));
                  agent.sendSimMessage(new Message("bla","bla", status.getAdvertiserInfos().get(statusAgents[i])));
                  agent.setDay(0);
                  agent.initBidder();
                  agentList.add(agent);

                  int capacity = status.getAdvertiserInfos().get(statusAgents[i]).getDistributionCapacity();
                  int capWindow  = status.getAdvertiserInfos().get(statusAgents[i]).getDistributionWindow();
                  AbstractUnitsSoldModel unitsSoldModel = new BasicUnitsSoldModel(querySpace,capacity,capWindow);
                  unitSoldModelList.add(unitsSoldModel);

                  ISRatioModel ISRatioModel = new ISRatioModel(querySpace,NUM_SLOTS);
                  ISRatioModelList.add(ISRatioModel);


                  AdTypeEstimator adTypeEstimator = new AdTypeEstimator(querySpace, advertisersSet, products);
                  adTypeEstimatorList.add(adTypeEstimator);

                  SimpleSpecialtyModel specialtyModel = new SimpleSpecialtyModel(querySpace, advertisersSet, products, NUM_SLOTS);
                  specialtyModelList.add(specialtyModel);

                  Set<AbstractModel> modelList = new HashSet<AbstractModel>();
                  modelList.add(queryAnalyzerList.get(i));
                  modelList.add(userModelList.get(i));
                  modelList.add(budgetModelList.get(i));
                  modelList.add(bidModelList.get(i));
                  modelList.add(paramModelList.get(i));
                  modelList.add(unitsSoldModel);
                  modelList.add(ISRatioModel);
                  modelList.add(adTypeEstimator);
                  modelList.add(specialtyModel);

                  agent.setModels(modelList);

                  ArrayList<BidBundle> bidBundles = new ArrayList<BidBundle>();
                  ArrayList<QueryReport> queryReports = new ArrayList<QueryReport>();
                  ArrayList<SalesReport> salesReports = new ArrayList<SalesReport>();
                  ArrayList<Integer> salesList = new ArrayList<Integer>();

                  int avgCap = (int)(capacity / ((double) capWindow));
                  salesList.add(avgCap);
                  salesList.add(avgCap);
                  salesList.add(avgCap);
                  salesList.add(avgCap);

                  BidBundle firstBundle = agent.getBidBundle();
                  agent.setDay(1);
                  BidBundle secondBundle = agent.getBidBundle();
                  bidBundles.add(firstBundle);
                  agent.handleBidBundle(firstBundle);
                  bidBundles.add(secondBundle);
                  agent.handleBidBundle(secondBundle);

                  int startCap = capacity - avgCap;
                  ArrayList firstReports = javasim.simDayForReports(cljSim,statusAgents[i],0,firstBundle,startCap);
                  QueryReport firstQueryReport = (QueryReport) firstReports.get(0);
                  SalesReport firstSalesReport = (SalesReport) firstReports.get(1);
                  int sales = 0;
                  for(Query q : querySpace) {
                     sales += firstSalesReport.getConversions(q);
                  }

                  queryReports.add(firstQueryReport);
                  salesReports.add(firstSalesReport);
                  salesList.add(sales);

                  int secondCap = startCap + sales - avgCap;
                  ArrayList secondReports = javasim.simDayForReports(cljSim,statusAgents[i],1,secondBundle,secondCap);
                  QueryReport secondQueryReport = (QueryReport) secondReports.get(0);
                  SalesReport secondSalesReport = (SalesReport) secondReports.get(1);

                  int secondDaySales = 0;
                  for(Query q : querySpace) {
                     secondDaySales += secondSalesReport.getConversions(q);
                  }

                  queryReports.add(secondQueryReport);
                  salesReports.add(secondSalesReport);
                  salesList.add(secondDaySales);


                  bidBundlesList.add(bidBundles);
                  queryReportsList.add(queryReports);
                  salesReportsList.add(salesReports);
                  salesListList.add(salesList);
               }
               else {
                  agentList.add(null);
                  bidBundlesList.add(null);
                  queryReportsList.add(null);
                  salesReportsList.add(null);
                  salesListList.add(null);
                  unitSoldModelList.add(null);
                  ISRatioModelList.add(null);
                  adTypeEstimatorList.add(null);
                  specialtyModelList.add(null);
               }
            }
         }

         // Make predictions for each day/query in this game
         for (int d=START_DAY; d<=END_DAY; d++) {
            System.out.println("Start day: " + d);

            boolean[] totalBudgetHits = hitTotalBudget(status,d);

            /*
            * Update Models and get predictions
            */
            HashMap<String,HashMap<Query,int[]>> allAgentRankPredictions = new HashMap<String, HashMap<Query, int[]>>(advertisers.size());
            HashMap<String,HashMap<Query,int[]>> allAgentImpressionPredictions = new HashMap<String, HashMap<Query, int[]>>(advertisers.size());
            HashMap<String,HashMap<Query,double[]>> allAgentBidPredictions = new HashMap<String, HashMap<Query, double[]>>(advertisers.size());
            HashMap<String,HashMap<Query,double[]>> allAgentBudgetPredictions = new HashMap<String, HashMap<Query, double[]>>(advertisers.size());
            HashMap<String,HashMap<Product, HashMap<GameStatusHandler.UserState, Double>>> allAgentUserStatePredictions = new HashMap<String, HashMap<Product, HashMap<GameStatusHandler.UserState, Double>>>(advertisers.size());
            HashMap<String,double[]> allAgentRegReservePredictions = new HashMap<String, double[]>(advertisers.size());
            HashMap<String,double[]> allAgentPromReservePredictions = new HashMap<String, double[]>(advertisers.size());
            HashMap<String,HashMap<Query,Double>> allAgentAdvEffectPredictions = new HashMap<String, HashMap<Query, Double>>(advertisers.size());
            HashMap<String,HashMap<Query,Double>> allAgentContProbsPredictions = new HashMap<String, HashMap<Query, Double>>(advertisers.size());
            for(int agent = 0; agent < advertisers.size(); agent++) {
               if(agent == replaceIdx) {
                  String agentName = advertisers.get(agent);
                  String statusAgent = statusAgents[agent];

                  /*
                   * Initialize Prediction Maps
                   */
                  HashMap<Query,int[]> allRankPredictions = new HashMap<Query, int[]>(querySpace.size());
                  HashMap<Query,int[]> allImpressionPredictions = new HashMap<Query, int[]>(querySpace.size());
                  HashMap<Query,double[]> allBidPredictions = new HashMap<Query, double[]>(querySpace.size());
                  HashMap<Query,double[]> allBudgetPredictions = new HashMap<Query, double[]>(querySpace.size());
                  HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> allUserStatePredictions;
                  double[] allRegReservePredictions = new double[QueryType.values().length];
                  double[] allPromReservePredictions = new double[QueryType.values().length];
                  HashMap<Query,Double> allAdvEffectPredictions = new HashMap<Query, Double>(querySpace.size());
                  HashMap<Query,Double> allContProbsPredictions = new HashMap<Query, Double>(querySpace.size());


                  /*
                   * Get Reports and Models
                   */
                  BidBundle bidBundle;
                  QueryReport queryReport;
                  if(LIVE_MODE) {
                     bidBundle = bidBundlesList.get(agent).get(d);
                     queryReport = queryReportsList.get(agent).get(d);
                  }
                  else {
                     bidBundle = status.getBidBundles().get(statusAgent).get(d);
                     queryReport = status.getQueryReports().get(statusAgent).get(d);
                  }

                  AbstractQueryAnalyzer queryAnalyzer = queryAnalyzerList.get(agent);
                  ParticleFilterAbstractUserModel userModel = userModelList.get(agent);
                  AbstractBudgetEstimator budgetModel = budgetModelList.get(agent);
                  AbstractBidModel bidModel = bidModelList.get(agent);
                  AbstractParameterEstimation paramModel = paramModelList.get(agent);

                  HashMap<Query,Integer> _maxImps;
                  if(!USER_MODEL_UB) {
                     _maxImps = new HashMap<Query,Integer>();
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
                        _maxImps.put(q, numImps);
                     }
                  }
                  else {
                     HashMap<Product,HashMap<GameStatusHandler.UserState,Double>> preUpdateUserStates = getUserStates(userModel,products);
                     _maxImps = getMaxImpsPred(preUpdateUserStates,1.45,querySpace);
                  }

                  queryAnalyzer.updateModel(queryReport, bidBundle, _maxImps);

                  HashMap<Query,Integer> totalImpressions = new HashMap<Query,Integer>();
                  HashMap<Query,HashMap<String, Integer>> ranks = new HashMap<Query,HashMap<String,Integer>>();
                  HashMap<Query,HashMap<String, Boolean>> rankablesBid = new HashMap<Query,HashMap<String,Boolean>>();
                  HashMap<Query,HashMap<String, Boolean>> rankablesBudget = new HashMap<Query,HashMap<String,Boolean>>(); //Agents that are padded or not in the auction are *not* rankable
                  HashMap<Query,int[][]> fullWaterfalls = new HashMap<Query, int[][]>();
                  for(Query q : querySpace) {
                     int[] impsPred = queryAnalyzer.getImpressionsPrediction(q);
                     int[] ranksPred = queryAnalyzer.getOrderPrediction(q);
                     int[][] waterfallPred = queryAnalyzer.getImpressionRangePrediction(q);
                     int totalImps = queryAnalyzer.getTotImps(q);

                     if(totalImps == 0) {
                        //this means something bad happened
                        totalImps = -1;
                     }

                     allRankPredictions.put(q,ranksPred);
                     allImpressionPredictions.put(q,impsPred);
                     fullWaterfalls.put(q,waterfallPred);
                     totalImpressions.put(q, totalImps);

                     HashMap<String, Integer> perQRanks = null;
                     if(waterfallPred != null) {
                        perQRanks = new HashMap<String,Integer>();
                        for(int i = 0; i < advertisers.size(); i++) {
                           perQRanks.put("adv" + (ranksPred[i] + 1),i);
                        }
                     }
                     ranks.put(q, perQRanks);

                     HashMap<String, Boolean> rankableBudget = null;
                     HashMap<String, Boolean> rankableBid = null;
                     if(waterfallPred != null) {
                        if(BUDGET_RANKABLE) {
                           rankableBudget = queryAnalyzer.getRankableMap(q);
                        }
                        else {
                           rankableBudget = new HashMap<String,Boolean>();
                           for(int i = 0; i < advertisers.size(); i++) {
                              rankableBudget.put("adv"+(i+1),true);
                           }
                        }

                        if(BID_RANKABLE) {
                           rankableBid = queryAnalyzer.getRankableMap(q);
                        }
                        else {
                           rankableBid = new HashMap<String,Boolean>();
                           for(int i = 0; i < advertisers.size(); i++) {
                              rankableBid.put("adv"+(i+1),true);
                           }
                        }
                     }
                     rankablesBudget.put(q,rankableBudget);
                     rankablesBid.put(q,rankableBid);
                  }

                  userModel.updateModel(totalImpressions);

                  HashMap<Query, Double> cpc = new HashMap<Query,Double>();
                  HashMap<Query, Double> ourBid = new HashMap<Query,Double>();
                  for(Query q : querySpace) {
                     cpc.put(q, queryReport.getCPC(q));
                     ourBid.put(q, bidBundle.getBid(q));
                  }
                  bidModel.updateModel(cpc, ourBid, ranks, rankablesBid);

                  allUserStatePredictions = getUserStates(userModel,products);

                  paramModel.updateModel(queryReport, bidBundle, allImpressionPredictions, fullWaterfalls, allUserStatePredictions);

                  if(LIVE_MODE) {
                     for(Query q : querySpace) {
                        int qtIdx = queryTypeToInt(q.getType());
                        double[] ISRatio = getISRatio(q,NUM_SLOTS,NUM_SLOTS,_advertiserEffectBoundsAvg[qtIdx],paramModel.getContProbPrediction(q),convProbs[qtIdx],allUserStatePredictions);
                        ISRatioModelList.get(agent).updateISRatio(q,ISRatio);
                     }
                  }

                  HashMap<Query,double[]> allSquashedBidPredictions = new HashMap<Query, double[]>(querySpace.size());
                  for(Query q : querySpace) {
                     allContProbsPredictions.put(q, paramModel.getContProbPrediction(q));
                     allAdvEffectPredictions.put(q, paramModel.getAdvEffectPrediction(q));
                     double oppAdvEffect = _advertiserEffectBoundsAvg[queryTypeToInt(q.getType())];
                     double oppSquashedAdvEff = Math.pow(oppAdvEffect, squashParam);
                     double[] squashedBidsArr = new double[advertisers.size()];
                     double[] bidsArr = new double[advertisers.size()];
                     for(int j = 0; j < squashedBidsArr.length; j++) {
                        if(j == agent) {
                           bidsArr[j] = bidBundle.getBid(q);
                           squashedBidsArr[j] = bidsArr[j] * Math.pow(paramModel.getAdvEffectPrediction(q),squashParam);
                        }
                        else {
                           bidsArr[j] = bidModel.getPrediction("adv" + (j+1), q);
                           squashedBidsArr[j] = bidsArr[j] * oppSquashedAdvEff;
                        }
                     }
                     allBidPredictions.put(q, bidsArr);
                     allSquashedBidPredictions.put(q, squashedBidsArr);
                  }

                  for(int qtIdx = 0; qtIdx < QueryType.values().length; qtIdx++) {
                     allRegReservePredictions[qtIdx] = paramModel.getRegReservePrediction(QueryType.values()[qtIdx]);
                     allPromReservePredictions[qtIdx] = paramModel.getPromReservePrediction(QueryType.values()[qtIdx]);
                  }

                  budgetModel.updateModel(queryReport, bidBundle, allContProbsPredictions, allRegReservePredictions, allRankPredictions,allImpressionPredictions,fullWaterfalls, rankablesBudget, allSquashedBidPredictions, allUserStatePredictions);

                  for(Query q : querySpace) {
                     double[] budgetArr = new double[advertisers.size()];
                     for(int i = 0; i < advertisers.size(); i++) {
                        budgetArr[i] = budgetModel.getBudgetEstimate(q,"adv" + (i+1));
                     }
                     allBudgetPredictions.put(q,budgetArr);
                  }


                  /*
                  * Put Predictions in Maps
                  */
                  allAgentRankPredictions.put(agentName,allRankPredictions);
                  allAgentImpressionPredictions.put(agentName,allImpressionPredictions);
                  allAgentBidPredictions.put(agentName,allBidPredictions);
                  allAgentBudgetPredictions.put(agentName,allBudgetPredictions);
                  allAgentUserStatePredictions.put(agentName,allUserStatePredictions);
                  allAgentRegReservePredictions.put(agentName,allRegReservePredictions);
                  allAgentPromReservePredictions.put(agentName,allPromReservePredictions);
                  allAgentAdvEffectPredictions.put(agentName,allAdvEffectPredictions);
                  allAgentContProbsPredictions.put(agentName,allContProbsPredictions);
               }
            }


            /*
             * Bid and update bidbundle,queryreport,salesreport if live mode
             */
            if(LIVE_MODE && ((d+2) <= END_DAY)) {
               for(int agent = 0; agent < advertisers.size(); agent++) {
                  if(agent == replaceIdx) {
                     /*
                     * Update unit sold and IS Ratio
                     */
                     unitSoldModelList.get(agent).update(salesReportsList.get(agent).get(d));
                     adTypeEstimatorList.get(agent).updateModel(queryReportsList.get(agent).get(d));
                     specialtyModelList.get(agent).updateModel(queryReportsList.get(agent).get(d));

                     int capWindow  = status.getAdvertiserInfos().get(statusAgents[agent]).getDistributionWindow();

                     agentList.get(agent).setDay(d+2);
                     agentList.get(agent).handleQueryReport(queryReportsList.get(agent).get(d));
                     agentList.get(agent).handleSalesReport(salesReportsList.get(agent).get(d));

                     int startSales = 0;
                     ArrayList<Integer> salesList = salesListList.get(agent);
                     for(int i = 0; i < (capWindow-1); i++) {
                        startSales += salesList.get(salesList.size()-1-i);
                     }

                     BidBundle bundle = agentList.get(agent).getBidBundle();
                     agentList.get(agent).handleBidBundle(bundle);
                     ArrayList reports = javasim.simDayForReports(cljSim,statusAgents[agent],d+2,bundle,startSales);
                     QueryReport queryReport = (QueryReport) reports.get(0);
                     SalesReport salesReport = (SalesReport) reports.get(1);
                     int sales = 0;
                     for(Query q : querySpace) {
                        sales += salesReport.getConversions(q);
                     }

                     bidBundlesList.get(agent).add(bundle);
                     queryReportsList.get(agent).add(queryReport);
                     salesReportsList.get(agent).add(salesReport);
                     salesListList.get(agent).add(sales);
                  }
               }
            }


            /*
            * Calculate all errors
            */
            for (int queryIdx=START_QUERY; queryIdx<=END_QUERY; queryIdx++) {
               Query query = queryArr[queryIdx];
               Integer[] impressions = getAgentImpressions(status, d, query);
               Double[] bids = getBids(status, d, query);
               double[] squashedBids = getSquashedBids(status, d, query);
               int[] ranks = getIndicesForDescendingOrder(squashedBids);
               Double[] budgets = getHitBudgets(status, d, query,totalBudgetHits);
               Double[] advertiserEffects = getAdvertiserEffects(status, query);
               double contProb = getContProbs(status, query);


               for(Double bid : bids) {
                  if(Double.isNaN(bid)) {
                     int j = 0;
                  }
               }

               for(int agent = 0; agent < advertisers.size(); agent++) {
                  if(agent == replaceIdx) {
                     String agentName = advertisers.get(agent);

                     int[] rankPredictions = allAgentRankPredictions.get(agentName).get(query);
                     int[] impressionPredictions = allAgentImpressionPredictions.get(agentName).get(query);
                     double[] bidPredictions = allAgentBidPredictions.get(agentName).get(query);
                     double[] budgetPredictions = allAgentBudgetPredictions.get(agentName).get(query);
                     double advEffectPredictions = allAgentAdvEffectPredictions.get(agentName).get(query);
                     double contProbsPredictions = allAgentContProbsPredictions.get(agentName).get(query);

                     /*
                     * Rank Error
                     */


                     /*
                     * Impression Error
                     */
                     for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                        if(innerAgent != agent) {
                           if(impressionPredictions != null) {
                              impErrorList.add((double) Math.abs(impressions[innerAgent] - impressionPredictions[innerAgent]));
                           }
                        }
                     }

                     if(rankPredictions != null) {
                        for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                           int orderedAgent = ranks[agent];
                           int orderedInnerAgent = rankPredictions[innerAgent];
                           if(orderedInnerAgent != orderedAgent) {
                              if(impressionPredictions != null) {
                                 impInOrderErrorList.add((double) Math.abs(impressions[orderedAgent] - impressionPredictions[orderedInnerAgent]));
                              }
                           }
                        }
                     }

                     /*
                     * Bid Error
                     */
                     for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                        if(innerAgent != agent) {
                           bidErrorList.add(Math.abs(Math.min(bids[innerAgent], 3.5) - bidPredictions[innerAgent]));
                        }
                     }


                     Double[] sortedBids = bids.clone();
                     Arrays.sort(sortedBids);
                     double[] sortedBidPreds = bidPredictions.clone();
                     Arrays.sort(sortedBidPreds);
                     for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                        if(innerAgent != agent) {
                           bidInOrderErrorList.add(Math.abs(Math.min(sortedBids[innerAgent], 3.5) - sortedBidPreds[innerAgent]));
                        }
                     }


                     /*
                     * Budget Error
                     */
//                     System.out.println("Budgets: " + Arrays.toString(budgets));
//                     System.out.println("Budget Preds: " + Arrays.toString(budgetPredictions));
//                     System.out.println("--------");
                     for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                        if(innerAgent != agent) {
                           double budget = budgets[innerAgent];
                           double budgetPred = budgetPredictions[innerAgent];
                           if(!(Double.isInfinite(budgetPred) || budgetPred == Double.MAX_VALUE)) {
                              budgetPred *= 1.5;
                           }
                           if(Double.isInfinite(budget) || budget == Double.MAX_VALUE) {
                              if(Double.isInfinite(budgetPred) || budgetPred == Double.MAX_VALUE) {
                                 //We said they didn't have a budget and they didn't
                                 numBudgetType4Errs_Tot++;
                              }
                              else {
                                 //We said they had a budget and they didn't
                                 budgetErrorType2List.add(Math.abs(budgetPred));
                              }
                           }
                           else if(budget == 0) {
                              if(Double.isInfinite(budgetPred) || budgetPred == Double.MAX_VALUE) {
                                 //We said they didn't have a budget and they had a budget of 0
                                 numBudgetType6Errs_Tot++;
                              }
                              else {
                                 //We said they had a budget and they set it to zero
                                 budgetErrorType5List.add(Math.abs(budget - budgetPred));
                              }
                           }
                           else {
                              if(Double.isInfinite(budgetPred) || budgetPred == Double.MAX_VALUE) {
                                 //We said they didn't have a budget and they did
                                 budgetErrorType3List.add(Math.abs(budget));
                              }
                              else {
                                 //We said they had a budget and they did
                                 budgetErrorType1List.add(Math.abs(budget - budgetPred));
                              }
                           }
                        }
                     }

                     /*
                     * Adv Effect Error
                     */
                     advEffectErrList.add(Math.abs(advertiserEffects[agent] - advEffectPredictions));

                     /*
                     * Cont Prob Error
                     */
                     contProbEffectsErrList.add(Math.abs(contProb - contProbsPredictions));
                  }
               }
            }


            HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> userStates = status.getUserDistributions().get(d);

            for(int agent = 0; agent < advertisers.size(); agent++) {
               if(agent == replaceIdx) {
                  String agentName = advertisers.get(agent);

                  HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> allUserStatePredictions = allAgentUserStatePredictions.get(agentName);
                  double[] allRegReservePredictions = allAgentRegReservePredictions.get(agentName);
                  double[] allPromReservePredictions = allAgentPromReservePredictions.get(agentName);

                  /*
                  * UserState Error
                  */
                  for(Product p : products) {
                     for(GameStatusHandler.UserState s : GameStatusHandler.UserState.values()) {
                        userStateErrList.add((double)Math.abs(allUserStatePredictions.get(p).get(s) - userStates.get(p).get(s)));
                     }
                  }

                  /*
                  * Reserve error
                  */
                  f0RegResErrList.add(Math.abs(allRegReservePredictions[0] - regularReserveScores.get(QueryType.FOCUS_LEVEL_ZERO)));
                  f1RegResErrList.add(Math.abs(allRegReservePredictions[1] - regularReserveScores.get(QueryType.FOCUS_LEVEL_ONE)));
                  f2RegResErrList.add(Math.abs(allRegReservePredictions[2] - regularReserveScores.get(QueryType.FOCUS_LEVEL_TWO)));
                  if(NUM_PROMOTED_SLOTS > 0) {
                     f0PromResErrList.add(Math.abs(allPromReservePredictions[0] - promotedReserveScores.get(QueryType.FOCUS_LEVEL_ZERO)));
                     f1PromResErrList.add(Math.abs(allPromReservePredictions[1] - promotedReserveScores.get(QueryType.FOCUS_LEVEL_ONE)));
                     f2PromResErrList.add(Math.abs(allPromReservePredictions[2] - promotedReserveScores.get(QueryType.FOCUS_LEVEL_TWO)));
                  }

                  /*
                  * Agent Profit Error
                  */
                  if(LIVE_MODE) {
                     double agentProfit = 0.0;
                     double agentSimProfit = 0.0;

                     QueryReport actualQueryReport = status.getQueryReports().get(status.getAdvertisers()[agent]).get(d);
                     SalesReport actualSalesReport = status.getSalesReports().get(status.getAdvertisers()[agent]).get(d);

                     QueryReport simQueryReport = queryReportsList.get(agent).get(d);
                     SalesReport simSalesReport = salesReportsList.get(agent).get(d);
                     for(Query q : querySpace) {
                        agentProfit += (actualSalesReport.getRevenue(q) - actualQueryReport.getCost(q));
                        agentSimProfit += (simSalesReport.getRevenue(q) - simQueryReport.getCost(q));
                     }

                     agentProfitDiffList.add(agentSimProfit - agentProfit);
                  }
               }
            }
         }
      }

      double[] results = new double[24];
      results[0] = sumList(impErrorList) / ((double) impErrorList.size());
      results[1] = sumList(impInOrderErrorList) / ((double) impInOrderErrorList.size());
      results[2] = sumList(bidErrorList) / ((double) bidErrorList.size());
      results[3] = sumList(bidInOrderErrorList) / ((double) bidInOrderErrorList.size());
      results[4] = sumList(budgetErrorType1List) / ((double) budgetErrorType1List.size());
      results[5] = budgetErrorType1List.size();
      results[6] = sumList(budgetErrorType2List) / ((double) budgetErrorType2List.size());
      results[7] = budgetErrorType2List.size();
      results[8] = sumList(budgetErrorType3List) / ((double) budgetErrorType3List.size());
      results[9] = budgetErrorType3List.size();
      results[10] = numBudgetType4Errs_Tot;
      results[11] = sumList(budgetErrorType5List) / ((double) budgetErrorType5List.size());
      results[12] = budgetErrorType5List.size();
      results[13] = numBudgetType6Errs_Tot;
      results[14] = sumList(advEffectErrList) / ((double) advEffectErrList.size());
      results[15] = sumList(contProbEffectsErrList) / ((double) contProbEffectsErrList.size());
      results[16] = sumList(userStateErrList) / ((double) userStateErrList.size());
      results[17] = sumList(f0RegResErrList) / ((double) f0RegResErrList.size());
      results[18] = sumList(f1RegResErrList) / ((double) f1RegResErrList.size());
      results[19] = sumList(f2RegResErrList) / ((double) f2RegResErrList.size());
      results[20] = sumList(f0PromResErrList) / ((double) f0PromResErrList.size());
      results[21] = sumList(f1PromResErrList) / ((double) f1PromResErrList.size());
      results[22] = sumList(f2PromResErrList) / ((double) f2PromResErrList.size());
      results[23] = sumList(agentProfitDiffList) / ((double) ((END_GAME - START_GAME) + 1));

      /*
       * Print Results
       */
      if(SUMMARY) {
         System.out.println("Type I:   We said they had a budget and they did");
         System.out.println("Type II:  We said they had a budget and they didn't");
         System.out.println("Type III: We said they didn't have a budget and they did");
         System.out.println("Type IV:  We said they didn't have a budget and they didn't");
         System.out.println("Type V:  We said they had a budget and they set it to zero");
         System.out.println("Type VI:  We said they didn't have a budget and they had a budget of 0\n");

         System.out.println("Impression MAE: " + results[0] + "(" + stdDevList(impErrorList,results[0]) + ")");
         System.out.println("Impression in order MAE: " + results[1] + "(" + stdDevList(impInOrderErrorList,results[1]) + ")");
         System.out.println("Bid MAE: " + results[2] + "(" + stdDevList(bidErrorList,results[2]) + ")");
         System.out.println("Bid in order MAE: " + results[3] + "(" + stdDevList(bidInOrderErrorList,results[3]) + ")");
         double budgetTot = results[5] + results[7] + results[9] + results[10] + results[12] + results[13];
         System.out.println("Budget Type I MAE: " + results[4] + "(" + stdDevList(budgetErrorType1List,results[4]) + ") , % Type I: " + results[5] / budgetTot);
         System.out.println("Budget Type II MAE: " + results[6] + "(" + stdDevList(budgetErrorType2List,results[6]) + ") , % Type II: " + results[7] / budgetTot);
         System.out.println("Budget Type III MAE: " + results[8] + "(" + stdDevList(budgetErrorType3List,results[8]) + ") , % Type III: " + results[9] / budgetTot);
         System.out.println("Budget % Type IV: " + results[10] / budgetTot);
         System.out.println("Budget Type V MAE: " + results[11] + "(" + stdDevList(budgetErrorType3List,results[11]) + ") , % Type V: " + results[12] / budgetTot);
         System.out.println("Budget % Type VI: " + results[13] / budgetTot);
         System.out.println("Adv Effect MAE: " + results[14] + "(" + stdDevList(advEffectErrList,results[14]) + ")");
         System.out.println("Cont Prob MAE: " + results[15] + "(" + stdDevList(contProbEffectsErrList,results[15]) + ")");
         System.out.println("User State MAE: " + results[16] + "(" + stdDevList(userStateErrList,results[16]) + ")");
         System.out.println("F0 Reg MAE: " + results[17] + "(" + stdDevList(f0RegResErrList,results[17]) + ")");
         System.out.println("F1 Reg MAE: " + results[18] + "(" + stdDevList(f1RegResErrList,results[18]) + ")");
         System.out.println("F2 Reg MAE: " + results[19] + "(" + stdDevList(f2RegResErrList,results[19]) + ")");
         System.out.println("F0 Prom MAE: " + results[20] + "(" + stdDevList(f0PromResErrList,results[20]) + ")");
         System.out.println("F1 Prom MAE: " + results[21] + "(" + stdDevList(f1PromResErrList,results[21]) + ")");
         System.out.println("F2 Prom MAE: " + results[22] + "(" + stdDevList(f2PromResErrList,results[22]) + ")");
         System.out.println("Total Agent Profit Diff: " + results[23]);
      }

      return results;
   }

   private static double sumList(ArrayList<Double> list) {
      double sum = 0.0;
      for(Double val : list) {
         sum += val;
      }
      return sum;
   }

   private static double stdDevList(ArrayList<Double> list) {
      if(list.size() == 0) {
         return 0.0;
      }
      else {
         return stdDevList(list, sumList(list) / ((double) list.size()));
      }
   }

   private static double stdDevList(ArrayList<Double> list, double mean) {
      if(list.size() == 0) {
         return 0.0;
      }
      else {
         double sumResiduals = 0.0;
         for(Double val : list) {
            sumResiduals += (val - mean) * (val - mean);
         }
         double variance = sumResiduals / (((double) list.size()) - 1);
         return Math.sqrt(variance);
      }
   }

   private Double[] getBids(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] bids = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            bids[a] = status.getBidBundles().get(agentName).get(d).getBid(query);
         } catch (Exception e) {
            bids[a] = Double.NaN;
            throw new RuntimeException("Exception when getting bids");
         }
      }
      return bids;
   }

   private double[] getSquashedBids(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      double[] squashedBids = new double[agents.length];
      UserClickModel userClickModel = status.getUserClickModel();
      double squashing = status.getPubInfo().getSquashingParameter();
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), a);
            double bid = status.getBidBundles().get(agentName).get(d).getBid(query);
            squashedBids[a] = bid * Math.pow(advEffect, squashing);
         } catch (Exception e) {
            squashedBids[a] = Double.NaN;
         }
      }
      return squashedBids;
   }

   private boolean[] hitTotalBudget(GameStatus status, int d) {
      String[] agents = status.getAdvertisers();
      boolean[] totalBudgetHits = new boolean[agents.length];
      for(int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         double totalBudget = status.getBidBundles().get(agentName).get(d).getCampaignDailySpendLimit();
         if(Double.isInfinite(totalBudget) || totalBudget == Double.MAX_VALUE) {
            totalBudgetHits[a] = false;
         }
         else {
            double totalCost = 0.0;
            double maxBid = 0.0;
            for(Query q : status.getQuerySpace()) {
               totalCost += status.getQueryReports().get(agentName).get(d).getCost(q);
               double bid = status.getBidBundles().get(agentName).get(d).getBid(q);
               if(bid > maxBid) {
                  maxBid = bid;
               }
            }

            totalBudgetHits[a] = ((totalCost + maxBid) >= totalBudget);
         }
      }

      return totalBudgetHits;
   }


   private Double[] getHitBudgets(GameStatus status, int d, Query query, boolean[] totalBudgetHits) {
      String[] agents = status.getAdvertisers();
      Double[] budgets = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         if(totalBudgetHits[a]) {
            //If we hit total budget just report cost as per query budget
            budgets[a] = status.getQueryReports().get(agentName).get(d).getCost(query);
         }
         else {
            double budget = status.getBidBundles().get(agentName).get(d).getDailyLimit(query);
            double cost = status.getQueryReports().get(agentName).get(d).getCost(query);
            double bid = status.getBidBundles().get(agentName).get(d).getBid(query);
            if((cost+bid) > budget) {
               budgets[a] = budget;
            }
            else {
               budgets[a] = Double.MAX_VALUE;
            }
         }
      }
      return budgets;
   }

   private Double[] getAdvertiserEffects(GameStatus status, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] advEffects = new Double[agents.length];
      UserClickModel userClickModel = status.getUserClickModel();
      for (int a = 0; a < agents.length; a++) {
         try {
            advEffects[a] = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), a);
         } catch (Exception e) {
            advEffects[a] = Double.NaN;
         }
      }
      return advEffects;
   }

   private double getContProbs(GameStatus status, Query query) {
      UserClickModel userClickModel = status.getUserClickModel();
      double contProb;
      try {
         contProb = userClickModel.getContinuationProbability(userClickModel.queryIndex(query));
      } catch (Exception e) {
         contProb = Double.NaN;
      }
      return contProb;
   }

   private Integer[] getAgentImpressions(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Integer[] agentImpressions = new Integer[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            agentImpressions[a] = status.getQueryReports().get(agentName).get(d).getImpressions(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            agentImpressions[a] = null;
         }
      }
      return agentImpressions;
   }

   private Integer[] getAgentPromotedImpressions(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Integer[] agentImpressions = new Integer[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            agentImpressions[a] = status.getQueryReports().get(agentName).get(d).getPromotedImpressions(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            agentImpressions[a] = null;
         }
      }
      return agentImpressions;
   }

   private HashMap<QueryType, Double> getApproximatePromotedReserveScore(GameStatus status) {

      //This is our approximation of promoted reserve score: the lowest score for which someone received a promoted slot.
      HashMap<QueryType, Double> currentLowPromotedScore = new HashMap<QueryType, Double>();
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ZERO, Double.MAX_VALUE);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ONE, Double.MAX_VALUE);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_TWO, Double.MAX_VALUE);

      // Make the query space
      Set<Query> querySpace = status.getQuerySpace();
      int numDays = 59;
      for (int d = 0; d < numDays; d++) {
         for (Query q : querySpace) {
            Integer[] promotedImps = getAgentPromotedImpressions(status, d, q);
            double[] squashedBids = getSquashedBids(status, d, q);

            for (int a = 0; a < promotedImps.length; a++) {
               if (promotedImps[a] > 0 && squashedBids[a] < currentLowPromotedScore.get(q.getType())) {
                  currentLowPromotedScore.put(q.getType(), squashedBids[a]);
               }
            }
         }
      }
      return currentLowPromotedScore;
   }

   private HashMap<QueryType, Double> getApproximateRegularReserveScore(GameStatus status) {

      //This is our approximation of promoted reserve score: the lowest score for which someone received a promoted slot.
      HashMap<QueryType, Double> currentLowPromotedScore = new HashMap<QueryType, Double>();
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ZERO, Double.MAX_VALUE);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ONE, Double.MAX_VALUE);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_TWO, Double.MAX_VALUE);

      // Make the query space
      Set<Query> querySpace = status.getQuerySpace();
      int numDays = 59;
      for (int d = 0; d < numDays; d++) {
         for (Query q : querySpace) {
            Integer[] impressions = getAgentImpressions(status, d, q);
            double[] squashedBids = getSquashedBids(status, d, q);

            for (int a = 0; a < impressions.length; a++) {
               if (impressions[a] > 0 && squashedBids[a] < currentLowPromotedScore.get(q.getType())) {
                  currentLowPromotedScore.put(q.getType(), squashedBids[a]);
               }
            }
         }
      }
      return currentLowPromotedScore;
   }

   public static void main(String[] args) throws IOException, ParseException {
      GameSet GAMES_TO_TEST = GameSet.semi2011server1;
      int START_GAME = 1414;
      int END_GAME = 1414;
      int START_DAY = 0; //0
      int END_DAY = 58; //57
      int START_QUERY = 0; //0
      int END_QUERY = 15; //15
      String agentName = "Schlemazl";

      if(args.length == 1) {
         START_GAME = Integer.parseInt(args[0]);
         END_GAME = START_GAME;
      }

      AllModelTest evaluator = new AllModelTest();

      if(SUMMARY) {
         //PRINT PARAMETERS
         System.out.println();
         System.out.println("GAMES_TO_TEST=" + GAMES_TO_TEST);
         System.out.println("START_GAME=" + START_GAME);
         System.out.println("END_GAME=" + END_GAME);
         System.out.println("START_DAY=" + START_DAY);
         System.out.println("END_DAY=" + END_DAY);
         System.out.println("START_QUERY=" + START_QUERY);
         System.out.println("END_QUERY=" + END_QUERY);
         System.out.println();
      }

      double start;
      double stop;
      double secondsElapsed;
      start = System.currentTimeMillis();
      double[] results;
      results = evaluator.allModelPredictionChallenge(GAMES_TO_TEST, START_GAME, END_GAME, START_DAY, END_DAY, START_QUERY, END_QUERY,agentName);
      stop = System.currentTimeMillis();
      secondsElapsed = (stop - start) / 1000.0;
      if(SUMMARY) {
         System.out.println("SECONDS ELAPSED: " + secondsElapsed);
      }
      System.out.println(Arrays.toString(results));
   }

}
