package simulator.predictions;

import agents.AbstractAgent;
import agents.modelbased.MCKP;
import clojure.lang.PersistentHashMap;
import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.ISratio.ISRatioModel;
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
   boolean USER_MODEL_PERF_IMPS = false; //User model is perfect, not based on QA output

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
   boolean LIVE_MODE = true;

   public static final int MAX_F0_IMPS = 10969;
   public static final int MAX_F1_IMPS = 1801;
   public static final int MAX_F2_IMPS = 1468;

   public enum GameSet {
      finals2010, semifinals2010, test2010
   }


   public static boolean isMac(){
      String os = System.getProperty("os.name").toLowerCase();
      return (os.indexOf( "mac" ) >= 0); //Mac
   }

   public ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) baseFile = "./game";
      if (GAMES_TO_TEST == GameSet.finals2010) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
         }
         else baseFile = "/pro/aa/finals2010/game-tacaa1-";
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

      ArrayList<Double> impErrorList = new ArrayList<Double>();

      ArrayList<Double> impInOrderErrorList = new ArrayList<Double>();

      ArrayList<Double> bidErrorList = new ArrayList<Double>();

      ArrayList<Double> bidInOrderErrorList = new ArrayList<Double>();

      ArrayList<Double> budgetErrorType1List = new ArrayList<Double>();
      ArrayList<Double> budgetErrorType2List = new ArrayList<Double>();
      ArrayList<Double> budgetErrorType3List = new ArrayList<Double>();
      int numBudgetType4Errs_Tot = 0;

      ArrayList<Double> advEffectErrList = new ArrayList<Double>();

      ArrayList<Double> contProbEffectsErrList = new ArrayList<Double>();

      ArrayList<Double> userStateErrList = new ArrayList<Double>();

      ArrayList<Double> f0RegResErrList = new ArrayList<Double>();
      ArrayList<Double> f1RegResErrList = new ArrayList<Double>();
      ArrayList<Double> f2RegResErrList = new ArrayList<Double>();
      ArrayList<Double> f0PromResErrList = new ArrayList<Double>();
      ArrayList<Double> f1PromResErrList = new ArrayList<Double>();
      ArrayList<Double> f2PromResErrList = new ArrayList<Double>();

      ArrayList<Double> agentProfitDiffList = new ArrayList<Double>();

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
            System.out.println("BAD AGENT NAME(" + agentToReplace+ " ) quitting");
            break;
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

         double[] convProbs = new double[] {0.03, 0.05, 0.15};
         LinkedHashSet advertisersSet = new LinkedHashSet<String>();
         ArrayList<String> advertisers = new ArrayList<String>();
         for (int i = 1; i <= 8; i++) {
            advertisers.add("adv" + i);
            advertisersSet.add("adv" + i);
         }
         double _randJump = .1;
         double _yestBid = .5;
         double _5DayBid = .4;
         double _bidStdDev = 2.0;
         double USER_MODEL_UB_STD_DEV = .75;

         List<AbstractQueryAnalyzer> queryAnalyzerList = null;
         if (TEST_QA) {
            queryAnalyzerList = new ArrayList<AbstractQueryAnalyzer>();
            for (int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  AbstractQueryAnalyzer queryAnalyzer = new CarletonQueryAnalyzer(querySpace,advertisers,"adv"+(i+1),true,true);
                  queryAnalyzerList.add(queryAnalyzer);
               }
               else {
                  queryAnalyzerList.add(null);
               }
            }
         }

         List<ParticleFilterAbstractUserModel> userModelList = null;
         if (TEST_USER_MODEL) {
            userModelList = new ArrayList<ParticleFilterAbstractUserModel>();
            for (int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  ParticleFilterAbstractUserModel userModel = new jbergParticleFilter(convProbs, USER_MODEL_UB_STD_DEV);
                  userModelList.add(userModel);
               }
               else {
                  userModelList.add(null);
               }
            }
         }

         List<AbstractBudgetEstimator> budgetModelList = null;
         if (TEST_BUDGET) {
            budgetModelList = new ArrayList<AbstractBudgetEstimator>();
            for (int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  AbstractBudgetEstimator budgetModel = new BudgetEstimator(querySpace, i, NUM_SLOTS, NUM_PROMOTED_SLOTS);
                  budgetModelList.add(budgetModel);
               }
               else {
                  budgetModelList.add(null);
               }
            }
         }

         List<AbstractBidModel> bidModelList = null;
         if (TEST_BID) {
            bidModelList = new ArrayList<AbstractBidModel>();
            for (int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  AbstractBidModel bidModel = new IndependentBidModel(advertisersSet, "adv" + (i+1),1,_randJump,_yestBid,_5DayBid,_bidStdDev,querySpace);
                  bidModelList.add(bidModel);
               }
               else {
                  bidModelList.add(null);
               }
            }
         }

         List<AbstractParameterEstimation> paramModelList = null;
         if (TEST_PARAM_EST) {
            paramModelList = new ArrayList<AbstractParameterEstimation>();
            for (int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  AbstractParameterEstimation paramModel;
                  if(PARAM_EST_NAIVE) {
                     paramModel = new NaiveParameterEstimation();
                  }
                  else {
                     paramModel = new BayesianParameterEstimation(convProbs,i,NUM_SLOTS, NUM_PROMOTED_SLOTS, squashParam, querySpace);
                  }
                  paramModelList.add(paramModel);
               }
               else {
                  paramModelList.add(null);
               }
            }
         }

         List<AbstractUnitsSoldModel> unitSoldModelList = null;
         List<ISRatioModel> ISRatioModelList = null;
         List<AbstractAgent> agentList = null;
         List<ArrayList<BidBundle>> bidBundlesList = null;
         List<ArrayList<QueryReport>> queryReportsList = null;
         List<ArrayList<SalesReport>> salesReportsList = null;
         List<ArrayList<Integer>> salesListList = null;
         PersistentHashMap cljSim = null;
         if(LIVE_MODE) {
            //Fuck it! We'll do it live!
            unitSoldModelList = new ArrayList<AbstractUnitsSoldModel>();
            ISRatioModelList = new ArrayList<ISRatioModel>();
            agentList = new ArrayList<AbstractAgent>();
            bidBundlesList = new ArrayList<ArrayList<BidBundle>>();
            queryReportsList = new ArrayList<ArrayList<QueryReport>>();
            salesReportsList = new ArrayList<ArrayList<SalesReport>>();
            salesListList = new ArrayList<ArrayList<Integer>>();
            cljSim = setupSimulator(filename);
            for(int i = 0; i < advertisers.size(); i++) {
               if(i == replaceIdx) {
                  AbstractAgent agent = new MCKP();
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

                  Set<AbstractModel> modelList = new HashSet<AbstractModel>();
                  modelList.add(queryAnalyzerList.get(i));
                  modelList.add(userModelList.get(i));
                  modelList.add(budgetModelList.get(i));
                  modelList.add(bidModelList.get(i));
                  modelList.add(paramModelList.get(i));
                  modelList.add(unitsSoldModel);
                  modelList.add(ISRatioModel);

                  ArrayList<BidBundle> bidBundles = new ArrayList<BidBundle>();
                  ArrayList<QueryReport> queryReports = new ArrayList<QueryReport>();
                  ArrayList<SalesReport> salesReports = new ArrayList<SalesReport>();
                  ArrayList<Integer> salesList = new ArrayList<Integer>();

                  int avgCap = (int)(capacity / ((double) capWindow));
                  salesList.add(avgCap);
                  salesList.add(avgCap);
                  salesList.add(avgCap);
                  salesList.add(avgCap);

                  BidBundle firstBundle = agent.getBidBundle(modelList);
                  agent.setDay(1);
                  BidBundle secondBundle = agent.getBidBundle(modelList);
                  bidBundles.add(firstBundle);
                  bidBundles.add(secondBundle);

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
                     secondDaySales += firstSalesReport.getConversions(q);
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
            HashMap<String,HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>>> allAgentUserStatePredictions = new HashMap<String, HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>>>(advertisers.size());
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
                  HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> allUserStatePredictions;
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
                  if(!USER_MODEL_PERF_IMPS) {
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
                     HashMap<Product,HashMap<GameStatusHandler.UserState,Integer>> preUpdateUserStates = getUserStates(userModel,products);
                     _maxImps = getMaxImpsPred(preUpdateUserStates,querySpace);
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

                  paramModel.updateModel(queryReport, bidBundle, allImpressionPredictions, fullWaterfalls, allUserStatePredictions, convProbs);

                  HashMap<Query,double[]> ISRatios = new HashMap<Query, double[]>();
                  for(Query q : querySpace) {
                     int qtIdx = queryTypeToInt(q.getType());
                     double[] ISRatio = getISRatio(q,NUM_SLOTS,NUM_SLOTS,_advertiserEffectBoundsAvg[qtIdx],paramModel.getContProbPrediction(q),convProbs[qtIdx],allUserStatePredictions);
                     ISRatios.put(q,ISRatio);
                     ISRatioModelList.get(agent).updateISRatio(q,ISRatio);
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

                  budgetModel.updateModel(queryReport, bidBundle, convProbs, allContProbsPredictions, allRegReservePredictions, allRankPredictions,allImpressionPredictions,fullWaterfalls, rankablesBudget, allSquashedBidPredictions, allUserStatePredictions);

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
                     AbstractUnitsSoldModel unitsSoldModel = unitSoldModelList.get(agent);
                     unitsSoldModel.update(salesReportsList.get(agent).get(d));

                     int capWindow  = status.getAdvertiserInfos().get(statusAgents[agent]).getDistributionWindow();

                     Set<AbstractModel> modelList = new HashSet<AbstractModel>();
                     modelList.add(queryAnalyzerList.get(agent));
                     modelList.add(userModelList.get(agent));
                     modelList.add(budgetModelList.get(agent));
                     modelList.add(bidModelList.get(agent));
                     modelList.add(paramModelList.get(agent));
                     modelList.add(unitsSoldModel);
                     modelList.add(ISRatioModelList.get(agent));

                     AbstractAgent liveAgent = agentList.get(agent);
                     liveAgent.setDay(d+2);

                     int startSales = 0;
                     ArrayList<Integer> salesList = salesListList.get(agent);
                     for(int i = 0; i < (capWindow-1); i++) {
                        startSales += salesList.get(salesList.size()-1-i);
                     }

                     BidBundle bundle = liveAgent.getBidBundle(modelList);
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
                     int numBudgetType4Errs = 0;
                     for(int innerAgent = 0; innerAgent < advertisers.size(); innerAgent++) {
                        if(innerAgent != agent) {
                           double budget = budgets[innerAgent];
                           double budgetPred = budgetPredictions[innerAgent];
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


            HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> userStates = status.getUserDistributions().get(d);

            for(int agent = 0; agent < advertisers.size(); agent++) {
               if(agent == replaceIdx) {
                  String agentName = advertisers.get(agent);

                  HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> allUserStatePredictions = allAgentUserStatePredictions.get(agentName);
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

      double[] results = new double[21];
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
      results[11] = sumList(advEffectErrList) / ((double) advEffectErrList.size());
      results[12] = sumList(contProbEffectsErrList) / ((double) contProbEffectsErrList.size());
      results[13] = sumList(userStateErrList) / ((double) userStateErrList.size());
      results[14] = sumList(f0RegResErrList) / ((double) f0RegResErrList.size());
      results[15] = sumList(f1RegResErrList) / ((double) f1RegResErrList.size());
      results[16] = sumList(f2RegResErrList) / ((double) f2RegResErrList.size());
      results[17] = sumList(f0PromResErrList) / ((double) f0PromResErrList.size());
      results[18] = sumList(f1PromResErrList) / ((double) f1PromResErrList.size());
      results[19] = sumList(f2PromResErrList) / ((double) f2PromResErrList.size());
      results[20] = sumList(agentProfitDiffList) / ((double) ((END_GAME - START_GAME) + 1));

      /*
       * Print Results
       */
      if(SUMMARY) {
         System.out.println("Type I:   We said they had a budget and they did");
         System.out.println("Type II:  We said they had a budget and they didn't");
         System.out.println("Type III: We said they didn't have a budget and they did");
         System.out.println("Type IV:  We said they didn't have a budget and they didn't\n");

         System.out.println("Impression MAE: " + results[0] + "(" + stdDevList(impErrorList,results[0]) + ")");
         System.out.println("Impression in order MAE: " + results[1] + "(" + stdDevList(impInOrderErrorList,results[1]) + ")");
         System.out.println("Bid MAE: " + results[2] + "(" + stdDevList(bidErrorList,results[2]) + ")");
         System.out.println("Bid in order MAE: " + results[3] + "(" + stdDevList(bidInOrderErrorList,results[3]) + ")");
         double budgetTot = results[5] + results[7] + results[9] + results[10];
         System.out.println("Budget Type I MAE: " + results[4] + "(" + stdDevList(budgetErrorType1List,results[4]) + ") , % Type I: " + results[5] / budgetTot);
         System.out.println("Budget Type II MAE: " + results[6] + "(" + stdDevList(budgetErrorType2List,results[6]) + ") , % Type II: " + results[7] / budgetTot);
         System.out.println("Budget Type III MAE: " + results[8] + "(" + stdDevList(budgetErrorType3List,results[8]) + ") , % Type III: " + results[9] / budgetTot);
         System.out.println("Budget % Type IV: " + results[10] / budgetTot);
         System.out.println("Adv Effect MAE: " + results[11] + "(" + stdDevList(advEffectErrList,results[11]) + ")");
         System.out.println("Cont Prob MAE: " + results[12] + "(" + stdDevList(contProbEffectsErrList,results[12]) + ")");
         System.out.println("User State MAE: " + results[13] + "(" + stdDevList(userStateErrList,results[13]) + ")");
         System.out.println("F0 Reg MAE: " + results[14] + "(" + stdDevList(f0RegResErrList,results[14]) + ")");
         System.out.println("F1 Reg MAE: " + results[15] + "(" + stdDevList(f1RegResErrList,results[15]) + ")");
         System.out.println("F2 Reg MAE: " + results[16] + "(" + stdDevList(f2RegResErrList,results[16]) + ")");
         System.out.println("F0 Prom MAE: " + results[17] + "(" + stdDevList(f0PromResErrList,results[17]) + ")");
         System.out.println("F1 Prom MAE: " + results[18] + "(" + stdDevList(f1PromResErrList,results[18]) + ")");
         System.out.println("F2 Prom MAE: " + results[19] + "(" + stdDevList(f2PromResErrList,results[19]) + ")");
         System.out.println("Total Agent Profit Diff: " + results[20]);
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
            if((cost+bid)  > budget) {
               budgets[a] = cost;
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
      int numDays = 57;
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
      int numDays = 57;
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
      GameSet GAMES_TO_TEST = GameSet.finals2010;
      int START_GAME = 15159; //15148 //15127
      int END_GAME = 15159;
      int START_DAY = 0; //0
      int END_DAY = 57; //57
      int START_QUERY = 0; //0
      int END_QUERY = 15; //15
      String agentName = "TacTex";


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
