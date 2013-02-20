package simulator.predictions;

import edu.umich.eecs.tac.props.*;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.forecast.*;
import models.queryanalyzer.riep.*;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorSample;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorAll;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPExact;
import models.usermodel.UserModel;
import models.usermodel.UserModelInput;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import simulator.predictions.AllModelTest.GameSet;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class ImpressionEstimatorTest {

   public ImpressionEstimatorTest(boolean sampleAvgPositions, boolean perfectImps, boolean useWaterfallPriors, double noiseFactor,  boolean useHistoricPriors, HistoricalPriorsType historicPriorsType, boolean orderingKnown, double upperBoundNoise) {
      _rand = new Random(_seed);
      SAMPLED_AVERAGE_POSITIONS = sampleAvgPositions;
      PERFECT_IMPS = perfectImps;
      USE_WATERFALL_PRIORS = useWaterfallPriors;
      PRIOR_NOISE_MULTIPLIER = noiseFactor; //this only matters if useWaterfallPriors=true
      this.USE_HISTORIC_PRIORS = useHistoricPriors;
      this.HISTORIC_PRIOR_TO_USE = historicPriorsType;
      this.ORDERING_KNOWN = orderingKnown;
      this.IMP_UB_FACTOR = upperBoundNoise;
   }

   private static boolean DEBUG = false;
   private static boolean LOGGING = true;
   private static boolean SUMMARY = true;

   private static boolean NO_F0 = true;

   private static void debug(String s) {
      if (DEBUG) {
         System.out.println(s);
      }
   }

   Random _rand;
   long _seed = 1616867;

   boolean CHEATING = true; //By cheating, we mean that we only pass the solver the auction participants
   private double PRIOR_STDEV_MULTIPLIER = 0.5; // 0 -> perfectPredictions. 1 -> stdev=1*meanImpsPrior
   boolean TEST_USER_MODEL = true;  //Turns user model testing on
   boolean USER_MODEL_PERF_IMPS = false; //User model is perfect, not based on QA output
   boolean USER_MODEL_PERF_WHEN_IN_AUCTION = false; //this only gives us estimates in the auctions we were in (like when we use it in the game) but they are perfect
   //GameSet GAMES_TO_TEST;

   //if we're sampling average positions, do we want to remove agents that received no samples?
   //(if we're using exact average positions anyway, true/false has no effect)
   boolean REMOVE_SAMPLE_NANS = false;


   private double IMP_UB_FACTOR;

   //////////////////
   //NOTE: THESE ARE SET IN THE MAIN METHOD.
   //SETTING THEM HERE WILL JUST GET OVERRIDDEN (unless the default constructor is called)
   private boolean SAMPLED_AVERAGE_POSITIONS;
   public boolean PERFECT_IMPS;
   boolean USE_WATERFALL_PRIORS;
   private double PRIOR_NOISE_MULTIPLIER; // 0 -> perfectPredictions. 1 -> stdev=1*meanImpsPrior
   boolean USE_HISTORIC_PRIORS;
   HistoricalPriorsType HISTORIC_PRIOR_TO_USE;
   boolean ORDERING_KNOWN;
   //////////////////



   BufferedWriter bufferedWriter = null;

   int NUM_SLOTS = 5;
   public static final int MAX_F0_IMPS = 10969;
   public static final int MAX_F1_IMPS = 1801;
   public static final int MAX_F2_IMPS = 1468;
   private static boolean REPORT_FULLPOS_FORSELF = true;

   //Performance metrics
   int numInstances = 0;
   int numSkips = 0;
   int numSkipsDuplicateBid = 0;
   int numSkipsInvalidData = 0;
   int numImprsPredictions = 0;
   int nullIEResult = 0;
   int numTotImprPredictions = 0;
   int numCorrectNumParticipants = 0; //#times the number of participants was correct
   int numCorrectParticipants = 0; //#times the list of participants was correct
   int numOrderingPredictions = 0; //#times the list of participants was correctly ordered
   int numCorrectlyOrderedInstances = 0;
   double aggregateAbsImprError = 0;
   double aggregateAbsTotImprError = 0;

   public enum SolverType {
      CP, MIP, MIP_LP, LDSMIP, MULTI_MIP, Carleton_LP, SIMPLE_MIP_Exact, Carleton_LP_Exact, SIMPLE_MIP_Sampled,
   }

   public enum GameSet {
      finals2010, semifinals2010, test2010,semi2011server1,semi2011server2
   }

   public enum HistoricalPriorsType {
      Naive, LastNonZero, SMA, EMA,
   }

   public ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) baseFile = "./game";
      if (GAMES_TO_TEST == GameSet.finals2010) baseFile = "/pro/aa/finals2010/game-tacaa1-";  //"/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-"; //"/Users/sodomka/Desktop/tacaa2010/game-tacaa1-";
     
      if (GAMES_TO_TEST == GameSet.finals2010) 
         baseFile = "/pro/aa/finals2010/game-tacaa1-";
      
      if (GAMES_TO_TEST == GameSet.semi2011server1)
         baseFile = "/pro/aa/tacaa2011/semi/server1/game";
      
      if (GAMES_TO_TEST == GameSet.semi2011server2) 
         baseFile = "/pro/aa/tacaa2011/semi/server2/game";
      

      
      
      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   /**
    * Debugging method used to print out whatever data I want from the game logs
    *
    * @throws ParseException
    * @throws IOException
    */
   public void printGameLogInfo() throws IOException, ParseException {
      ArrayList<String> filenames = getGameStrings(GameSet.test2010, 1, 1);
      for (int gameIdx = 0; gameIdx < filenames.size(); gameIdx++) {
         String filename = filenames.get(gameIdx);
         GameStatus status = new GameStatusHandler(filename).getGameStatus();
         double reserve = status.getReserveInfo().getRegularReserve();
         double promotedReserve = status.getReserveInfo().getPromotedReserve();
         double numPromotedSlots = status.getSlotInfo().getPromotedSlots();
//			double approxPromotedReserve = getApproximatePromotedReserveScore(status);
//			System.out.println("reserve="+reserve+", promotedReserve="+promotedReserve+", approxPromotedReserve="+approxPromotedReserve+", numPromotedSlots="+numPromotedSlots);
         // Make the query space
         LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
         querySpace.add(new Query(null, null)); //F0
         for (Product product : status.getRetailCatalog()) {
            querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
            querySpace.add(new Query(null, product.getComponent())); // F1 Component only
            querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
         }
         Query[] queryArr = new Query[querySpace.size()];
         querySpace.toArray(queryArr);
         int numQueries = queryArr.length;

         // Make predictions for each day/query in this game
         int numReports = 57;//57; //TODO: Why?
         for (int d = 0; d < numReports; d++) {
            for (int queryIdx = 0; queryIdx < numQueries; queryIdx++) {
               Query query = queryArr[queryIdx];
               Integer[] imps = getAgentImpressions(status, d, query);
               Integer[] promotedImps = getAgentPromotedImpressions(status, d, query);
               debug("promotedImps=" + Arrays.toString(promotedImps) + "\timps=" + Arrays.toString(imps));
            }
         }
      }
      throw new RuntimeException("Finished printing game log. Aborting.");
   }


   public void logGameData() throws IOException, ParseException {

      //------ CREATE DATA FILE
      String logFilename = "gamedata.txt";
      try {
         //Construct the BufferedWriter object
         bufferedWriter = new BufferedWriter(new FileWriter(logFilename));

         //Create header
         StringBuffer sb = new StringBuffer();
         sb.append("game\t");
         sb.append("agent\t");
         sb.append("query\t");
         sb.append("day\t");
         sb.append("approx.searchers\t");
         sb.append("bid\t");
         sb.append("squashed.bid\t");
         sb.append("budget\t");
         sb.append("imps\t");
         sb.append("clicks\t");
         sb.append("convs\t");
         sb.append("avg.pos\t");
         sb.append("sampled.avg.pos\t");
         sb.append("cost");

         //Start writing to the output stream
         bufferedWriter.write(sb.toString());
         bufferedWriter.newLine();
      } catch (FileNotFoundException ex) {
         ex.printStackTrace();
      } catch (IOException ex) {
         ex.printStackTrace();
      }


      //------------------- GET GAME DATA ---------------

      ArrayList<String> filenames = getGameStrings(GameSet.test2010, 1, 1);
      for (int gameIdx = 0; gameIdx < filenames.size(); gameIdx++) {
         String filename = filenames.get(gameIdx);

         // Load this game and its basic parameters
         GameStatus status = new GameStatusHandler(filename).getGameStatus();
         int NUM_PROMOTED_SLOTS = status.getSlotInfo().getPromotedSlots();
         HashMap<QueryType, Double> promotedReserveScore = getApproximatePromotedReserveScore(status);

         // Make the query space
         LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
         querySpace.add(new Query(null, null)); //F0
         for (Product product : status.getRetailCatalog()) {
            querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
            querySpace.add(new Query(null, product.getComponent())); // F1 Component only
            querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
         }

         Query[] queryArr = new Query[querySpace.size()];
         querySpace.toArray(queryArr);
         int numQueries = queryArr.length;

         // Make predictions for each day/query in this game
         int numReports = 57; //TODO: Why?
         for (int d = 0; d < numReports; d++) {
            for (int queryIdx = 0; queryIdx < numQueries; queryIdx++) {
               Query query = queryArr[queryIdx];
               String[] agents = status.getAdvertisers();
               Double[] bids = getBids(status, d, query);
               Double[] squashedBids = getSquashedBids(status, d, query);
               Double[] budgets = getBudgets(status, d, query);
               Integer[] impressions = getAgentImpressions(status, d, query);
               Integer[] clicks = getAgentClicks(status, d, query);
               Integer[] conversions = getAgentConversions(status, d, query);
               Double[] actualAveragePositions = getAveragePositions(status, d, query);
               Double[] cost = getAgentCosts(status, d, query);
               Double[] sampledAveragePositions = getSampledAveragePositions(status, d, query);
               //Integer[] promotedImpressions = getAgentPromotedImpressions(status, d, query);
               //Boolean[] promotionEligibility = getAgentPromotionEligibility(status, d, query, promotedReserveScore.get(query.getType()));
               int approxSearchers = getApproximateNumSearchersInQuery(status, d, query);

               StringBuffer sb = new StringBuffer();
               for (int a = 0; a < agents.length; a++) {
                  sb.append(filename + "\t");
                  sb.append(agents[a] + "\t");
                  sb.append(query + "\t");
                  sb.append(d + "\t");
                  sb.append(approxSearchers + "\t");
                  sb.append(bids[a] + "\t");
                  sb.append(squashedBids[a] + "\t");
                  sb.append(budgets[a] + "\t");
                  sb.append(impressions[a] + "\t");
                  sb.append(clicks[a] + "\t");
                  sb.append(conversions[a] + "\t");
                  sb.append(actualAveragePositions[a] + "\t");
                  sb.append(sampledAveragePositions[a] + "\t");
                  sb.append(cost[a] + "\n");
               }
               writeToLog(sb.toString());
            }
         }
      }
      closeLog();
   }



//   public void allModelPredictionChallenge(SolverType impressionEstimatorIdx) throws IOException, ParseException {
//      GameSet gamesToTest= GameSet.test2010;
//      int startGame = 1;
//      int endGame = 1;
//      int startDay = 0; //0
//      int endDay = 57; //57
//      int startQuery = 0; //0
//      int endQuery = 15; //15
//      String agentToTest = "all";
//      allModelPredictionChallenge(impressionEstimatorIdx, gamesToTest, startGame, endGame, startDay, endDay, startQuery, endQuery, agentToTest);
//   }

   /**
    * Load a game
    * For each day,
    * Get all query reports that came in on that day
    * From these, infer: numSlots, numAgents, avgPos[], agentIds[], ourAgentIdx, ourImpressions, impressionsUB
    * also infer squashed bid ordering.
    * Input these into the given Impression Estimator (via a QA instance)
    * Compare output impsPer
    *
    * @param impressionEstimatorIdx
    * @throws IOException
    * @throws ParseException
    */
   public double[] impressionEstimatorPredictionChallenge(SolverType impressionEstimatorIdx,
                                                          GameSet GAMES_TO_TEST, int START_GAME, int END_GAME,
                                                          int START_DAY, int END_DAY, int START_QUERY, int END_QUERY, String AGENT_TO_TEST, int sampFrac, double IP_TIMEOUT_IN_SECONDS, double convPrMult) throws IOException, ParseException {
      //printGameLogInfo();

      if (USE_HISTORIC_PRIORS && USE_WATERFALL_PRIORS) {
         throw new RuntimeException("Cannot use more than 1 type of prior");
      }

      int fractionalBran = 0;

      StringBuffer sb1 = new StringBuffer();
      sb1.append(impressionEstimatorIdx);
      if (!impressionEstimatorIdx.equals(SolverType.CP)) sb1.append("-" + (int)(1000*IP_TIMEOUT_IN_SECONDS) + "msec");
      else sb1.append("-" + sampFrac + "sampFrac");
      sb1.append(ORDERING_KNOWN ? ".ie" : ".rie");
      sb1.append(SAMPLED_AVERAGE_POSITIONS ? ".sampled" : ".exact");
      //sb1.append(PERFECT_IMPS ? ".impsPerfect" : ".impsUB");
      if (USE_WATERFALL_PRIORS) sb1.append(".perfectWithNoise-" + PRIOR_STDEV_MULTIPLIER);
      if (USE_HISTORIC_PRIORS) sb1.append(".historic-" + HISTORIC_PRIOR_TO_USE);
      if (!USE_WATERFALL_PRIORS && !USE_HISTORIC_PRIORS) sb1.append(".noPrior");
      sb1.append("." + GAMES_TO_TEST);
      sb1.append(".games-" + START_GAME + "-" + END_GAME);
      sb1.append(".days-" + START_DAY + "-" + END_DAY);
      sb1.append(".queries-" + START_QUERY + "-" + END_QUERY);
      sb1.append(".agents-" + AGENT_TO_TEST);
      sb1.append(".txt");
      String logFilename = sb1.toString();


      if(LOGGING) {
         initializeLog(logFilename);
      }
      numInstances = 0;
      numSkips = 0;
      nullIEResult = 0;
      numSkipsDuplicateBid = 0;
      numSkipsInvalidData = 0;
      numImprsPredictions = 0;
      numTotImprPredictions = 0;
      numOrderingPredictions = 0;
      numCorrectlyOrderedInstances = 0;
      aggregateAbsImprError = 0;
      aggregateAbsTotImprError = 0;

      ArrayList<String> filenames = getGameStrings(GAMES_TO_TEST, START_GAME, END_GAME);

      int numSampWithImp = 0;
      int numSampWithImpTot = 0;
      int numSampPadFixed = 0;
      int numSampPadTried = 0;
      int[] numSampWithImpDiff = new int[9];

      double totalUserModelMAE = 0.0;
      int totalUserModelPts = 0;
      int totalFracPreds = 0;
      int totalFracCorrect = 0;
      double totalAlgRunTime = 0.0;
      for (int gameIdx = 0; gameIdx < filenames.size(); gameIdx++) {
         String filename = filenames.get(gameIdx);
         if(SUMMARY) {
            System.out.println("Game " + gameIdx + ", " + filename);
         }

         // Load this game and its basic parameters
         GameStatus status = new GameStatusHandler(filename).getGameStatus();
         int NUM_PROMOTED_SLOTS = status.getSlotInfo().getPromotedSlots();
         int NUM_SLOTS = status.getSlotInfo().getRegularSlots();
         HashMap<QueryType, Double> promotedReserveScore = getApproximatePromotedReserveScore(status);

         // Make the query space
         LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
         querySpace.add(new Query(null, null)); //F0
         for (Product product : status.getRetailCatalog()) {
            querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
            querySpace.add(new Query(null, product.getComponent())); // F1 Component only
            querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
         }

         Query[] queryArr = new Query[querySpace.size()];
         querySpace.toArray(queryArr);
         int numQueries = queryArr.length;

         String[] agents = status.getAdvertisers();

         ArrayList<AbstractImpressionForecaster> naiveImpressionForecasters = null;
         if (USE_HISTORIC_PRIORS) {
            naiveImpressionForecasters = new ArrayList<AbstractImpressionForecaster>();
            for (int i = 0; i < agents.length; i++) {
               AbstractImpressionForecaster impressionForecaster = null;
               if (HISTORIC_PRIOR_TO_USE == HistoricalPriorsType.Naive) impressionForecaster = new NaiveImpressionForecaster(querySpace,agents,i);
               if (HISTORIC_PRIOR_TO_USE == HistoricalPriorsType.LastNonZero) impressionForecaster = new LastNonZeroImpressionForecaster(querySpace, agents, i);
               if (HISTORIC_PRIOR_TO_USE == HistoricalPriorsType.SMA) impressionForecaster = new SMAImpressionForecaster(15,querySpace,agents,i);
               if (HISTORIC_PRIOR_TO_USE == HistoricalPriorsType.EMA) impressionForecaster = new EMAImpressionForecaster(.1, querySpace, agents, i);
               //if (HISTORIC_PRIOR_TO_USE == HistoricalPriorsType.TimeSeries) impressionForecaster = new TimeSeriesImpressionForecaster(_rConsnection,i,querySpace,agents,i);
               naiveImpressionForecasters.add(impressionForecaster);
            }
         }

         List<UserModel> userModelList = null;
         if (TEST_USER_MODEL) {
            userModelList = new ArrayList<UserModel>();
            for (int i = 0; i < agents.length; i++) {
            	// ETHAN-TODO: Fix this up
            	UserModel userModel = UserModel.build(new UserModelInput(), UserModel.ModelType.JBERG_PARTICLE_FILTER);
//               ParticleFilterAbstractUserModel userModel = new jbergParticleFilter(new double[] {0.10753988514063796,0.187966273,0.339007416}, NUM_SLOTS, NUM_PROMOTED_SLOTS);
               userModelList.add(userModel);
            }
         }

         // Make predictions for each day/query in this game
         int numReports = 57; //TODO: Why?
         for (int d=START_DAY; d<=END_DAY; d++) {
//         for (int d = 0; d < numReports; d++) {

            List<List<Map<Query, Integer>>> impPredMapLists = null;
            if (USE_HISTORIC_PRIORS) {
               impPredMapLists = new ArrayList<List<Map<Query, Integer>>>(agents.length);
               for (int i = 0; i < agents.length; i++) {
                  List<Map<Query, Integer>> impPredMapList = new ArrayList<Map<Query, Integer>>(agents.length);
                  for (int j = 0; j < agents.length; j++) {
                     Map<Query, Integer> impPredMap = new HashMap<Query, Integer>(querySpace.size());
                     impPredMapList.add(impPredMap);
                  }
                  impPredMapLists.add(impPredMapList);
               }
            }

            List<Map<Query, Integer>> totalImpsMapList = null;
            if (TEST_USER_MODEL) {
               totalImpsMapList = new ArrayList<Map<Query, Integer>>(agents.length);
               for (int i = 0; i < agents.length; i++) {
                  HashMap<Query, Integer> totalImpsMap = new HashMap<Query, Integer>(querySpace.size());
                  totalImpsMapList.add(totalImpsMap);
               }
            }

            for (int queryIdx=START_QUERY; queryIdx<=END_QUERY; queryIdx++) {
//            for (int queryIdx = 0; queryIdx < numQueries; queryIdx++) {

               Query query = queryArr[queryIdx];

               if(!NO_F0 || !(query.getComponent() == null &&
                                      query.getManufacturer() == null)) {

                  numInstances++;
//               if(!"pg".equals(query.getManufacturer()) ||
//                       !(query.getComponent() == null)) continue;

                  debug("Game " + (gameIdx + 1) + "/" + filenames.size() + ", Day " + d + "/" + numReports + ", query=" + queryIdx);

                  // Get avg position for each agent
                  Double[] actualAveragePositions = getAveragePositions(status, d, query);

                  // Get avg position for each agent
                  Double[] sampledAveragePositions = getSampledAveragePositions(status, d, query);

                  Double[] bids = getBids(status, d, query);

                  Double[] advertiserEffects = getAdvertiserEffects(status, d, query);

                  // Get squashed bids for each agent
                  Double[] squashedBids = getSquashedBids(status, d, query);

                  Double[] budgets = getBudgets(status, d, query);

                  Double[] globalBudgets = getGlobalBudgets(status, d);

                  // Get total number of impressions for each agent
                  Integer[] impressions = getAgentImpressions(status, d, query);

                  Integer[] clicks = getAgentClicks(status, d, query);

                  Integer[] conversions = getAgentConversions(status, d, query);

                  Integer[] promotedImpressions = getAgentPromotedImpressions(status, d, query);

                  Boolean[] promotionEligibility = getAgentPromotionEligibility(status, d, query, promotedReserveScore.get(query.getType()));

                  //Determine whether each agent hit their query or global budget (1 if yes, 0 if not)
                  Boolean[] hitBudget = getAgentHitBudget(status, d, query);

                  double[] impsDistMean;
                  double[] impsDistStdev;
                  if (!USE_WATERFALL_PRIORS) {
                     impsDistMean = new double[numReports];
                     impsDistStdev = new double[numReports];
                     Arrays.fill(impsDistMean, -1);
                     Arrays.fill(impsDistStdev, -1);
                  } else {
                     impsDistMean = getAgentImpressionsDistributionMeanOrStdev(status, query, d, true);
                     impsDistStdev = getAgentImpressionsDistributionMeanOrStdev(status, query, d, false);
                  }

                  // Determine how many agents actually participated
                  int numParticipants = 0;
                  for (int a = 0; a < actualAveragePositions.length; a++) {
                     //if (!actualAveragePositions[a].isNaN()) numParticipants++;
                     if ( !CHEATING ||
                             (   !Double.isNaN(actualAveragePositions[a]) &&
                                         (!Double.isNaN(sampledAveragePositions[a]) || !SAMPLED_AVERAGE_POSITIONS || ORDERING_KNOWN || !REMOVE_SAMPLE_NANS))) {
                        numParticipants++;
                     }
                  }

                  // Reduce to only auction participants
                  String[] reducedAgents = new String[numParticipants];
                  double[] reducedAvgPos = new double[numParticipants];
                  double[] reducedSampledAvgPos = new double[numParticipants];
                  double[] reducedBids = new double[numParticipants];
                  int[] reducedImps = new int[numParticipants];
                  int[] reducedPromotedImps = new int[numParticipants];
                  boolean[] reducedPromotionEligibility = new boolean[numParticipants];
                  boolean[] reducedHitBudget = new boolean[numParticipants];
                  double[] reducedImpsDistMean = new double[numParticipants];
                  double[] reducedImpsDistStdev = new double[numParticipants];
                  int[] reducedIndices = new int[numParticipants]; //The true agent index at each reduced position
                  int rIdx = 0;
                  for (int a = 0; a < actualAveragePositions.length; a++) {
                     //If agent's actualPosition isn't NaN, AND
                     //  we're not removing sampled nans, OR we're not in the problem of sampled average positions, OR the sampledPosition isn't NaN
                     //Then add this agent.
                     //Note: Also add this agent if we're adding everybody (i.e. if we're not cheating)
                     if ( !CHEATING ||
                             (  !Double.isNaN(actualAveragePositions[a]) &&
                                        ( !Double.isNaN(sampledAveragePositions[a]) || !SAMPLED_AVERAGE_POSITIONS || ORDERING_KNOWN || !REMOVE_SAMPLE_NANS))) {
                        reducedAgents[rIdx] = status.getAdvertisers()[a];
                        reducedAvgPos[rIdx] = actualAveragePositions[a];
                        reducedSampledAvgPos[rIdx] = sampledAveragePositions[a]; //TODO: need to handle double.nan cases...
                        reducedBids[rIdx] = squashedBids[a];
                        reducedImps[rIdx] = impressions[a];
                        reducedPromotedImps[rIdx] = promotedImpressions[a];
                        reducedPromotionEligibility[rIdx] = promotionEligibility[a];
                        reducedHitBudget[rIdx] = hitBudget[a];
                        reducedImpsDistMean[rIdx] = impsDistMean[a];
                        reducedImpsDistStdev[rIdx] = impsDistStdev[a];
                        reducedIndices[rIdx] = a;
                        rIdx++;
                     }
                  }

                  // Get ordering of remaining squashed bids
                  int[] trueOrdering = getIndicesForDescendingOrder(reducedBids);

                  int numSamples = 10;
//                  if(SAMPLED_AVERAGE_POSITIONS) {
//                     int approxSearchers = getApproximateNumSearchersInQuery(status, d, query);
//                     double[] reSampledAveragePositions = resampleAgents(reducedImps,trueOrdering,numSamples,approxSearchers,false);
//                     reducedSampledAvgPos = reSampledAveragePositions;
//                  }
//                  else {
//                     DecimalFormat df = new DecimalFormat("#.00000");
//                     double[] roundedReducedAvgPos = new double[reducedAvgPos.length];
//                     for(int i = 0; i < reducedAvgPos.length; i++) {
//                        roundedReducedAvgPos[i] = Double.parseDouble(df.format(reducedAvgPos[i]));
//                     }
//                     reducedAvgPos = roundedReducedAvgPos;
//                  }

                  int numSampled = 0;
                  int numWithImps = 0;
                  for(int i = 0; i < reducedSampledAvgPos.length; i++) {
                     if(!Double.isNaN(reducedSampledAvgPos[i])) {
                        numSampled++;
                     }
                     if(reducedImps[i] > 0) {
                        numWithImps++;
                     }
                  }

                  if(numSampled == numWithImps) {
                     numSampWithImp++;
                  }
                  else {
                     numSampWithImpDiff[Math.abs(numSampled - numWithImps)]++;

                     for(int agent = 0; agent < sampledAveragePositions.length; agent++) {
                        int foundIdx = -1;
                        for(int i = 0; i < reducedIndices.length; i++) {
                           if(reducedIndices[i] == agent) {
                              if(!Double.isNaN(reducedSampledAvgPos[i])) {
                                 foundIdx = i;
                              }
                              break;
                           }
                        }

                        if(foundIdx > -1) {
                           //remove nans
                           double[] nonNaNAvgPos = new double[reducedSampledAvgPos.length - (numWithImps - numSampled)];
                           int idx = 0;
                           for(int i = 0; i < reducedSampledAvgPos.length; i++) {
                              if(!Double.isNaN(reducedSampledAvgPos[i])) {
                                 nonNaNAvgPos[idx] = reducedSampledAvgPos[i];
                                 if(foundIdx == idx) {
                                    nonNaNAvgPos[idx] = reducedAvgPos[i];
                                 }
                                 idx++;
                              }
                           }

                           int[] nonNaNOrder = QAInstanceAll.getAvgPosOrder(nonNaNAvgPos);

                           int foundTooBigStart = 0;
                           int foundTooBigStop = 0;
                           for (int i = 0; i < NUM_SLOTS && i < nonNaNAvgPos.length; i++) {
                              if (nonNaNAvgPos[nonNaNOrder[i]] > i + 1) {
                                 foundTooBigStart = i + 1;
                                 foundTooBigStop = (int) Math.ceil(nonNaNAvgPos[nonNaNOrder[i]]);
                                 break;
                              }
                           }

                           numSampled += (foundTooBigStop - foundTooBigStart);
                           numSampPadTried++;
                           if(numSampled == numWithImps) {
                              numSampPadFixed++;
                           }
                        }
                     }
                  }

                  numSampWithImpTot++;


                  //If we added everybody, we don't want the true ordering to contain an ordering over non-participants.
                  //Remove agents that didn't participate from the true ordering.
                  int[] reducedTrueOrdering = trueOrdering.clone();
                  if (!CHEATING) {
                     reducedTrueOrdering = reduceTrueOrderingToParticipants(trueOrdering, impressions);
                  }

                  // Create the ordering that will be used by the agent
                  // (This is the true ordering if the ordering is known; otherwise it's an arbitrary ordering)
                  int[] ordering;
                  if (ORDERING_KNOWN) {
                     ordering = trueOrdering.clone();
                  } else {
                     ordering = new int[reducedBids.length];
                     for (int i = 0; i < ordering.length; i++) {
                        ordering[i] = i;
                     }
                  }
//               int[] actualBidOrdering = getActualBidOrdering(ordering, reducedImps); //[a b c d] means the agent with index 0 has rank a
//               int[] sampledBidOrdering = getSampledBidOrdering(ordering, reducedImps, reducedSampledAvgPos); //The initial positions of agents that had at least one sample
//               int[] sampledBidRelativeOrdering = getRelativeOrdering(sampledBidOrdering); //The ordering for agents that had at least one sample


                  int impressionsUB = getAgentImpressionsUpperBound(status, PERFECT_IMPS, d, query, reducedImps, trueOrdering);

                  int impsInFirstSlot;
                  if(PERFECT_IMPS) {
                     impsInFirstSlot = impressionsUB;
//                     impressionsUB = (int)(impressionsUB * (1.0 + (IMP_UB_FACTOR - 1.0) * _rand.nextDouble()));
                     impressionsUB = (int)(impressionsUB * IMP_UB_FACTOR);
                  }
                  else {
                     impsInFirstSlot = getMaxImps(NUM_SLOTS, reducedImps.length, trueOrdering,reducedImps);
                  }

//                  LDSearchSmartTest ldsSearch = new LDSearchSmartTest(trueOrdering.clone());
//                  int[] avgPosOrder = QAInstance.getCarletonOrder(reducedAvgPos,5);
//                  ldsSearch.search(avgPosOrder,reducedAvgPos);

                  // If any agents have the same squashed bids, we won't know the definitive ordering.
                  // (TODO: Run the waterfall to determine the correct ordering (or at least a feasible one).)
                  // For now, we'll drop any instances with duplicate squashed bids.
                  if (duplicateSquashedBids(reducedBids, trueOrdering)) {
                     debug("Duplicate squashed bids found. Skipping instance.");
                     numSkips++;
                     numSkipsDuplicateBid++;
                     continue;
                  }

                  //More generally, make sure our data isn't corrupt
                  if (!validData(reducedBids)) {
                     debug("Invalid data. Skipping instance.");
                     numSkips++;
                     numSkipsInvalidData++;
                     continue;
                  }


                  // Some params needed for the QA instance
                  int[] agentIds = new int[numParticipants];
                  for (int i = 0; i < agentIds.length; i++) {
                     agentIds[i] = i; //-(i + 1);
                  }

                  int userModelUB = 0;
                  if(TEST_USER_MODEL) {
                     userModelUB = getApproximateNumSearchersInQuery(status, d, query);
                  }

                  // DEBUG: Print out some game values.
                  debug("d=" + d + "\tq=" + query + "\treserve=" + status.getReserveInfo().getRegularReserve() + "\tpromoted=" + status.getReserveInfo().getPromotedReserve() + "\t" + status.getSlotInfo().getPromotedSlots() + "/" + status.getSlotInfo().getRegularSlots());
                  debug("d=" + d + "\tq=" + query + "\tagents=" + Arrays.toString(status.getAdvertisers()));
                  debug("d=" + d + "\tq=" + query + "\taveragePos=" + Arrays.toString(actualAveragePositions));
                  debug("d=" + d + "\tq=" + query + "\tsampledAveragePos=" + Arrays.toString(sampledAveragePositions));
                  debug("d=" + d + "\tq=" + query + "\tsquashing=" + status.getPubInfo().getSquashingParameter());
                  debug("d=" + d + "\tq=" + query + "\tadvertiserEffects=" + Arrays.toString(advertiserEffects));
                  debug("d=" + d + "\tq=" + query + "\tsquashedBids=" + Arrays.toString(squashedBids));
                  debug("d=" + d + "\tq=" + query + "\timpressions=" + Arrays.toString(impressions));
                  debug("d=" + d + "\tq=" + query + "\thitBudget=" + Arrays.toString(hitBudget));
                  debug("d=" + d + "\tq=" + query + "\tqueryBudgets=" + Arrays.toString(budgets));
                  debug("d=" + d + "\tq=" + query + "\tquerySpend=" + Arrays.toString(getAgentQuerySpend(status, d, query)));
                  debug("d=" + d + "\tq=" + query + "\tbids=" + Arrays.toString(bids));
                  debug("d=" + d + "\tq=" + query + "\tglobalBudgets=" + Arrays.toString(globalBudgets));
                  debug("d=" + d + "\tq=" + query + "\tglobalSpend=" + Arrays.toString(getAgentGlobalSpend(status, d)));
                  debug("d=" + d + "\tq=" + query + "\tglobalBids=" + Arrays.toString(getAgentGlobalMaxAvgCPC(status, d)));
                  debug("d=" + d + "\tq=" + query + "\tpromotedImpressions=" + Arrays.toString(promotedImpressions));
                  debug("d=" + d + "\tq=" + query + "\tpromotionEligibility=" + Arrays.toString(promotionEligibility));
                  debug("d=" + d + "\tq=" + query + "\timpressionsUB=" + impressionsUB);
                  debug("d=" + d + "\tq=" + query + "\timpressionsDistMean=" + Arrays.toString(impsDistMean));
                  debug("d=" + d + "\tq=" + query + "\timpressionsDistStdev=" + Arrays.toString(impsDistStdev));
                  debug("");

                  debug("d=" + d + "\tq=" + query + "\treducedAvgPos=" + Arrays.toString(reducedAvgPos));
                  debug("d=" + d + "\tq=" + query + "\treducedSampledAvgPos=" + Arrays.toString(reducedSampledAvgPos));
                  debug("d=" + d + "\tq=" + query + "\treducedBids=" + Arrays.toString(reducedBids));
                  debug("d=" + d + "\tq=" + query + "\ttrueOrdering=" + Arrays.toString(trueOrdering));
                  debug("d=" + d + "\tq=" + query + "\treducedTrueOrdering=" + Arrays.toString(reducedTrueOrdering));

                  // For each agent, make a prediction (each agent sees a different num impressions)
                  for (int nonReducedIdx = 0; nonReducedIdx < agents.length; nonReducedIdx++) {

                     //Optionally only run predictions for a certain agent.
                     if (!AGENT_TO_TEST.equals("all") && !status.getAdvertisers()[nonReducedIdx].equalsIgnoreCase(AGENT_TO_TEST)) continue;

                     //Only run predictions for agents that actually participated in the auction
                     if (impressions[nonReducedIdx] == 0 ) continue;


                     List<Map<Query, Integer>> impPredMapList = null;
                     if (USE_HISTORIC_PRIORS) {
                        impPredMapList = impPredMapLists.get(nonReducedIdx);
                     }

                     if (TEST_USER_MODEL && USER_MODEL_PERF_IMPS) {
                        totalImpsMapList.get(nonReducedIdx).put(query, userModelUB);
                     }

                     int ourAgentIdx = -1;
                     for (int i = 0; i < reducedIndices.length; i++) {
                        if (nonReducedIdx == reducedIndices[i]) {
                           ourAgentIdx = i;
                        }
                     }

                     if (ourAgentIdx == -1) {
                        if (USE_HISTORIC_PRIORS) {
                           //This agent wasn't in the auction therefore we have no predictions for anyone
                           for (int i = 0; i < agents.length; i++) {
                              Map<Query, Integer> impPredMap = impPredMapList.get(i);
                              impPredMap.put(query, -1);
                           }
                        }

                        if (TEST_USER_MODEL) {
                           if (!USER_MODEL_PERF_IMPS) {
                              //We weren't in the auction so we have no prediction
                              totalImpsMapList.get(nonReducedIdx).put(query, -1);
                           }
                        }
                        continue;
                     }

                     if (USE_HISTORIC_PRIORS) {
                        int[] predImpressions = new int[agents.length];
                        AbstractImpressionForecaster impressionForecaster = naiveImpressionForecasters.get(nonReducedIdx);
                        for (int i = 0; i < agents.length; i++) {
                           predImpressions[i] = (int) impressionForecaster.getPrediction(i, query);
                        }

                        double[] reducedPredImpressions = new double[numParticipants];
                        double[] reducedPredImpressionsStdDev = new double[numParticipants];
                        int rIdx2 = 0;
                        for (int a = 0; a < actualAveragePositions.length; a++) {
                           if (!Double.isNaN(actualAveragePositions[a]) && (!Double.isNaN(sampledAveragePositions[a]) || !SAMPLED_AVERAGE_POSITIONS || !REMOVE_SAMPLE_NANS)) {
                              if (predImpressions[a] > 0) {
                                 reducedPredImpressions[rIdx2] = predImpressions[a];
                              } else {
                                 reducedPredImpressions[rIdx2] = -1;
                              }
                              reducedPredImpressionsStdDev[rIdx2] = reducedPredImpressions[rIdx2] * .3;
                              rIdx2++;
                           }
                        }

                        reducedImpsDistMean = reducedPredImpressions;
                        reducedImpsDistStdev = reducedPredImpressionsStdDev;
                     }

                     String ourAgentName = reducedAgents[ourAgentIdx];

                     debug("ourAgentIdx=" + ourAgentIdx + ", ourAgentName=" + ourAgentName);

                     int ourImps = reducedImps[ourAgentIdx];
                     int ourPromotedImps = reducedPromotedImps[ourAgentIdx];
                     boolean ourPromotionEligibility = reducedPromotionEligibility[ourAgentIdx];
                     boolean ourHitBudget = reducedHitBudget[ourAgentIdx];


                     //FIXME: we should be able to choose more elegantly at runtime what class we're going to load.
                     //This is annoying... ImpressionEstimator requires a QAInstance in the constructor,
                     //but this QAInstance isn't ready until now.
                     //Terrible, band-aid solution is to have an integer corresponding to each test.
                     QAInstanceAll inst;
                     ImpressionAndRankEstimator fullModel = null;
                     AbstractImpressionEstimator model = null;


                     //Remove any average position information that the agent doesn't have access to.
                     double[] avgPos;
                     double[] sAvgPos;
                     if (SAMPLED_AVERAGE_POSITIONS) {
                        //Avg position is -1 except for our agent.
                        avgPos = new double[reducedAvgPos.length];
                        Arrays.fill(avgPos, -1);
                        avgPos[ourAgentIdx] = reducedAvgPos[ourAgentIdx];
                        //Sampled avgPositions are their actual values
                        sAvgPos = reducedSampledAvgPos.clone();
                     } else {
                        //Avg positions are their actual values
                        avgPos = reducedAvgPos.clone();
                        //Sampled avgPositions are -1
                        sAvgPos = new double[reducedAvgPos.length];
                        Arrays.fill(sAvgPos, -1);
                     }

                     //Remove any ordering information that the agent doesn't have access to.
                     int[] knownInitialPositions = trueOrdering.clone();
                     if (!ORDERING_KNOWN) {
                        Arrays.fill(knownInitialPositions, -1);
                     }


                     //---------------- Create new QAInstance --------------
                     boolean considerPaddingAgents = true;
                     inst = new QAInstanceAll(NUM_SLOTS, NUM_PROMOTED_SLOTS, numParticipants, avgPos.clone(), sAvgPos.clone(), agentIds.clone(), ourAgentIdx,
                                           ourImps, ourPromotedImps, impressionsUB, considerPaddingAgents, ourPromotionEligibility, ourHitBudget,
                                           reducedImpsDistMean.clone(), reducedImpsDistStdev.clone(), SAMPLED_AVERAGE_POSITIONS, knownInitialPositions.clone(), reducedAgents.clone());

                     int slots = Math.min(NUM_SLOTS, numParticipants);
                	 QAInstanceExact instExact = new QAInstanceExact(slots, numParticipants, agentIds.clone(), ourAgentIdx,
                             ourImps, impressionsUB, reducedAgents.clone(), avgPos.clone());
                	 if(USE_HISTORIC_PRIORS){
                		 instExact.setAgentImpressionDistributionMean(reducedImpsDistMean.clone(), reducedImpsDistStdev.clone()); 
                	 }

                     //System.exit(0); //DEBUG

                     double start = System.currentTimeMillis(); //time the prediction time on this instance

                     //---------------- SOLVE INSTANCE ----------------------
                     if (impressionEstimatorIdx == SolverType.CP) {
                        if(SAMPLED_AVERAGE_POSITIONS) {
                           if (ORDERING_KNOWN) {
                              model = new ImpressionEstimatorSample(inst, sampFrac, fractionalBran,numSamples);
                              fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                           } else {
                              model = new ImpressionEstimatorSample(inst, sampFrac, fractionalBran,numSamples);
                              fullModel = new LDSImpressionAndRankEstimator(model);
                           }
                        }
                        else {
                           if (ORDERING_KNOWN) {
                              model = new ImpressionEstimatorExact(inst, sampFrac, fractionalBran);
                              fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                           } else {
                              model = new ImpressionEstimatorExact(inst, sampFrac, fractionalBran);
                              fullModel = new LDSImpressionAndRankEstimator(model);
                           }
                        }
                        
                     }
                     if (impressionEstimatorIdx == SolverType.MIP) {
                        boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
                        model = new EricImpressionEstimator(inst, useRankingConstraints, true, false, IP_TIMEOUT_IN_SECONDS);
                        fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                     }
                     if (impressionEstimatorIdx == SolverType.MULTI_MIP) {
                        boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
                        model = new EricImpressionEstimator(inst, useRankingConstraints, true, true, IP_TIMEOUT_IN_SECONDS);
                        fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                     }
                     if (impressionEstimatorIdx == SolverType.MIP_LP) {
                        boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
                        model = new EricImpressionEstimator(inst, useRankingConstraints, false, false, IP_TIMEOUT_IN_SECONDS);
                        fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                     }

                     if (impressionEstimatorIdx == SolverType.LDSMIP) {
                        boolean useRankingConstraints = false;
                        model = new EricImpressionEstimator(inst, useRankingConstraints, true, false, IP_TIMEOUT_IN_SECONDS);
                        if (ORDERING_KNOWN) {
                           fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                        } else {
                           fullModel = new LDSImpressionAndRankEstimator(model);
                        }
                     }
                     if (impressionEstimatorIdx == SolverType.Carleton_LP) {
                    	 if (ORDERING_KNOWN) {
                             model = new DropoutImpressionEstimatorAll(inst, false, false, false, IP_TIMEOUT_IN_SECONDS);
                             fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                    	 } else {
                             model = new DropoutImpressionEstimatorAll(inst, false, false, false, IP_TIMEOUT_IN_SECONDS);
                             fullModel = new LDSImpressionAndRankEstimator(model);
                    	 }
                     }
                     
                     if (impressionEstimatorIdx == SolverType.Carleton_LP_Exact) {
                    	 if (ORDERING_KNOWN) {
                             model = new DropoutImpressionEstimatorExact(instExact);
                             fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                    	 } else {
                             model = new DropoutImpressionEstimatorExact(instExact);
                             fullModel = new LDSImpressionAndRankEstimator(model);
                    	 }
                     }
                     
                     if (impressionEstimatorIdx == SolverType.SIMPLE_MIP_Exact) {
                    	 if (ORDERING_KNOWN) {
                             model = new ImpressionEstimatorSimpleMIPExact(instExact);
                             fullModel = new ConstantImpressionAndRankEstimator(model, ordering);
                    	 } else {
                             model = new ImpressionEstimatorSimpleMIPExact(instExact);
                             fullModel = new LDSImpressionAndRankEstimator(model);
                    	 }
                     }

                     //---------------- SOLVE FOR RESULT -----------------------
                     IEResult result = fullModel.getBestSolution();
                     //Get predictions (also provide dummy values for failure)
                     int[] predictedImpsPerAgent;
                     int[] predictedOrdering;
                     int predictedTotImpr;

                     if (result != null) {
                        predictedImpsPerAgent = result.getSol();
                        predictedOrdering = result.getOrder();
                        predictedTotImpr = result.getSlotImpressions()[0];
                        //int[] predictedRelativeOrdering = getRelativeOrdering(predictedOrdering);
                     } else {
                        debug("Result is null.");
                        predictedImpsPerAgent = new int[reducedImps.length];
                        Arrays.fill(predictedImpsPerAgent, -1);

                        predictedOrdering = new int[reducedImps.length];
                        Arrays.fill(predictedOrdering, -1);

                        predictedTotImpr = -1;

                        nullIEResult++;
                     }
                     
                     double stop = System.currentTimeMillis();
                     double secondsElapsed = (stop - start) / 1000.0;
                     totalAlgRunTime += secondsElapsed;

                     //System.out.println("predicted: " + Arrays.toString(predictedImpsPerAgent));
                     //System.out.println("actual: " + Arrays.toString(reducedImps));

                     System.out.println("-------------");
                     System.out.println("INSTANCE=" + inst);
                     System.out.println("RESULT=" + result);
                     System.out.println("-------------");
                     
                     
                     
                     //asdf
                     //For each item in the result, get the agent that the prediction corresponds to.
                     //Ignore any predictions that are made for "padded" agents.
//                     int[] agentIDs = inst.getAgentIds(); //Ids of agents (for reduced problem)
//                     boolean[] isPadded = inst.isPadded();
//                	 for (int j=0; j<agentIDs.length; j++) {
//                		 if (!isPadded[j]) {
//                    		 int agentID = agentIDs[j]; //This agentID corresponds to the agent's reduced index.
//                    		 int agentIdx = reducedIndices[agentID]; 
//                			 
//                		 }
//                		 
//                	 }
//                     for (int i = 0; i < agents.length; i++) {
//                    	 //Try to find this id in the QAInstance
//                     }
                     
                     

                     if (USE_HISTORIC_PRIORS) {
                        //Update forecast models
                        for (int i = 0; i < agents.length; i++) {
                        	
                           Map<Query, Integer> impPredMap = impPredMapList.get(i);
                           int agentIdx = -1;
                           
                           //Recover the predicted number of impressions for agent i.
                           //(need to find the agent's position in the reduced array)
                           //(reduced indices tells the true agent index for each position in the impression prediction array.)

                           
                           
                           
                           for (int j = 0; j < reducedIndices.length; j++) { //for each reduced position
                        	   
                              if (i == reducedIndices[j]) { //see if the true agent index at this position equals agent i's index
                                 if(!Double.isNaN(sAvgPos[j])) { //Make sure the sampled average position was not NaN (TODO: why? I guess this probably leads to bad impressions estimates...)
                                    agentIdx = j;
                                 }
                                 else { //If sampled average position was NaN, don't give any agent index (don't update impressions prediction) 
                                    break;
                                 }
                              }
                           }
                           
                           //CP may or may not add some dummy agents to receive impressions.
                           //If not enough dummy agents are added, 
                           //The length of the returned predictedImps array may be shorter than the actualImps array.
                           //The indices of predictedImps thus may not correspond to the indices of actualImps.
                           //How can we control for this so that the predictedImps are added to the correct historical priors?
                           //If the lengths of these arrays are different, we should perhaps look at the returned ordering?
                           //For now: just don't update predictions if there are junk values.
                           //FIXME: Is this temporary error handling sufficient? We could be losing lots of data
//                           int predictedImps = -1; 
//                           if (predictedImpsPerAgent.length != reducedIndices.length) {
//                        	   if (agentIdx >= 0 && agentIdx < predictedImpsPerAgent.length) {
//                                   predictedImps = predictedImpsPerAgent[agentIdx];                        		   
//                        	   } 
//                           }
//                           impPredMap.put(query, predictedImps);
                           
                           
                           
                           
                           if (agentIdx > -1) {
                              if(agentIdx >= predictedImpsPerAgent.length) {
                                 int j = 0; 
                              }
                              impPredMap.put(query, predictedImpsPerAgent[agentIdx]);
                           } else {
                              impPredMap.put(query, -1);
                           }

                        
                        }
                     }

                     if (TEST_USER_MODEL && !USER_MODEL_PERF_IMPS) {
                        Map<Query, Integer> totalImpsMap = totalImpsMapList.get(nonReducedIdx);
                        if (USER_MODEL_PERF_WHEN_IN_AUCTION) {
                           totalImpsMap.put(query, userModelUB);
                        } else {
                           if (result == null) {
                              totalImpsMap.put(query, -1);
                           } else {
                              totalImpsMap.put(query, result.getSlotImpressions()[0]);
                           }
                        }
                     }

                     //LOGGING
                     double[] err = new double[reducedImps.length];
                     for (int a = 0; a < reducedImps.length; a++) {
                        err[a] = Math.abs(predictedImpsPerAgent[a] - reducedImps[a]);
                     }
                     StringBuffer sb = new StringBuffer();
                     sb.append("result=" + result + "\n");
                     sb.append("err=" + Arrays.toString(err) + "\t");
                     sb.append("pred=" + Arrays.toString(predictedImpsPerAgent) + "\t");
                     sb.append("actual=" + Arrays.toString(reducedImps) + "\t");
                     sb.append("g=" + gameIdx + " ");
                     sb.append("d=" + d + " a=" + ourAgentIdx + " q=" + query + " avgPos=" + Arrays.toString(reducedAvgPos) + " ");
                     sb.append("sampAvgPos=" + Arrays.toString(reducedSampledAvgPos) + " ");
                     sb.append("bids=" + Arrays.toString(reducedBids) + " ");
                     sb.append("imps=" + Arrays.toString(reducedImps) + " ");
                     sb.append("order=" + Arrays.toString(ordering) + " ");
                     sb.append(((impressionEstimatorIdx == SolverType.CP) ? "CP" : "MIP"));
                     debug(sb.toString());

                     //asdf
                     
                     //Update performance metrics
                     updatePerformanceMetrics(predictedImpsPerAgent, reducedImps, predictedTotImpr, impsInFirstSlot, predictedOrdering, reducedTrueOrdering, reducedSampledAvgPos);
                     //outputPerformanceMetrics();

                     if(LOGGING) {
                        //Save all relevant data to file
                        sb = new StringBuffer();

                        //Go through all participating agents, (each "opponent" we want to make a prediction for)
                        for (int predictingAgentIdx = 0; predictingAgentIdx < numParticipants; predictingAgentIdx++) {

                           //get the actual bid rank of the opponent, as well as the rank of ourselves.
                           int ourBidRank = -1;
                           int oppBidRank = -1;
                           int predictedOppBidRank = -1;
                           for (int i = 0; i < trueOrdering.length; i++) {
                              if (trueOrdering[i] == ourAgentIdx) {
                                 ourBidRank = i;
                              }
                              if (trueOrdering[i] == predictingAgentIdx) {
                                 oppBidRank = i;
                              }

                              //Also get the predicted bid rank of the given opponent.
                              if (predictedOrdering[i] == predictingAgentIdx) {
                                 predictedOppBidRank = i;
                              }

                           }


                           sb.append(model.getName() + ",");
                           sb.append(START_GAME + gameIdx + ",");
                           sb.append(d + ",");
                           sb.append(queryIdx + ",");
                           sb.append(ourAgentName + ","); //our agent name
                           sb.append(ourBidRank + ","); //our bid rank (i.e. our initial position)
                           sb.append(oppBidRank + ","); //opponent bid rank  (i.e. opponent initial position)
                           sb.append(predictedOppBidRank + ","); //predicted opponent bid rank (i.e. predicted opponent initial position)
                           sb.append(reducedAvgPos[ourAgentIdx] + ","); //our avgPos
                           sb.append(reducedAvgPos[predictingAgentIdx] + ","); //opponent avgPos
                           sb.append(reducedSampledAvgPos[predictingAgentIdx] + ","); //opponent sampleAvgPos
                           sb.append(query.getType() + ",");
                           sb.append(numParticipants + ",");
                           sb.append(reducedImps[predictingAgentIdx] + ","); //actual
                           sb.append(predictedImpsPerAgent[predictingAgentIdx] + ","); //prediction
                           sb.append(secondsElapsed + "\n");
                        }
                        //Log the result (for later loading into R)
                        writeToLog(sb.toString());
                     }
                  }
               }
            }
//            System.out.println("End Day " + d);
//            outputPerformanceMetrics();

            if (USE_HISTORIC_PRIORS) {
               for (int i = 0; i < agents.length; i++) {
                  List<Map<Query, Integer>> impPredMapList = impPredMapLists.get(i);
                  naiveImpressionForecasters.get(i).updateModel(impPredMapList);
//                  for(Query q : querySpace) {
//                     for(int ag2 = 0; ag2 < agents.length; ag2++) {
//                  int pred1 = (int)impressionForecasters.get(i).getPrediction(q);
//                        int pred2 = (int)naiveImpressionForecasters.get(i).getPrediction(ag2,q);
//                        int imps  = status.getQueryReports().get(agents[ag2]).get(d+1).getImpressions(q);
//                  System.out.println("*, " + d + ", " +  i + ", " + imps + ", " + Math.abs(pred1 -imps) + ", " +  Math.abs(pred2 -imps));
//                  System.out.println("*, " + d + ", " +  i + ", " + imps + ", " + Math.abs(pred2 -imps));
//                     }
//                  }
               }
            }


            List<Map<Product, Map<GameStatusHandler.UserState, Integer>>> userPredictionList = null;
            if (TEST_USER_MODEL) {
               userPredictionList = new ArrayList<Map<Product, Map<GameStatusHandler.UserState, Integer>>>();
               HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> userDistributions = status.getUserDistributions().get(d);
               for (int i = 0; i < agents.length; i++) {
                  Map<Query, Integer> totalImpMap = totalImpsMapList.get(i);
                  UserModel userModel = userModelList.get(i);
                  userModel.updateModel(totalImpMap);

                  Map<Product, Map<GameStatusHandler.UserState, Integer>> userPredictionMap = new HashMap<Product, Map<GameStatusHandler.UserState, Integer>>();
                  for (Product product : status.getRetailCatalog()) {
                     HashMap<GameStatusHandler.UserState, Double> userDistProductMap = userDistributions.get(product);
                     Map<GameStatusHandler.UserState, Integer> userPredProductMap = new HashMap<GameStatusHandler.UserState, Integer>();
                     for (GameStatusHandler.UserState state : GameStatusHandler.UserState.values()) {
                        int userPred = (int)userModel.getPrediction(product, state);
                        userPredProductMap.put(state, userPred);

                        double users = userDistProductMap.get(state);
                        double MAE = Math.abs(userPred - users);

                        totalUserModelMAE += MAE;
                        totalUserModelPts++;
                     }
                     userPredictionMap.put(product, userPredProductMap);
                  }
                  userPredictionList.add(userPredictionMap);
               }
            }

         }
      }

      System.out.println(convPrMult + ", " + totalUserModelMAE / totalUserModelPts);
      if(SUMMARY) {
         outputPerformanceMetrics();
         System.out.println("User Model Error: " + totalUserModelMAE / totalUserModelPts);
         System.out.println("Continued Fraction Error: " + totalFracCorrect / ((double) totalFracPreds));
         System.out.println("Total QA Runtime: " + totalAlgRunTime / ((double) numOrderingPredictions));
      }

      double[] impDiffPerc = new double[numSampWithImpDiff.length];
      for(int i = 0; i < numSampWithImpDiff.length; i++) {
         impDiffPerc[i] = numSampWithImpDiff[i] / ((double) (numSampWithImpTot - numSampWithImp));
      }

      if(SUMMARY) {
         System.out.println("Percent numSamp == numWithImp: " + numSampWithImp / ((double) numSampWithImpTot));
         System.out.println("Average  |numSamp - numWithImp|: " + Arrays.toString(impDiffPerc));
         System.out.println("Percent fixed by padding: " + numSampPadFixed  / ((double) numSampPadTried));
         System.out.println("Number numSamp tested: " + numSampWithImpTot);
      }

      if(LOGGING) {
         closeLog();
      }

//      double[] results = new double[4];
//      double[] impRankErr = getPerformanceMetrics();
//      results[0] = totalAlgRunTime / ((double) numOrderingPredictions);
//      results[1] = impRankErr[0];
//      results[2] = impRankErr[1];
//      results[3] = impRankErr[2];
      
      double[] results = new double[4];
      double[] impRankErr = getPerformanceMetrics();
      results[0] = totalAlgRunTime;
      results[1] = impRankErr[0];
      results[2] = impRankErr[1];
      results[3] = impRankErr[2];

      
      return results;
   }


   private static boolean feasibleOrder(int[] order, double[] avgPos, int slots) {
      for (int i = 0; i < order.length; i++) {
         int startPos = Math.min(i + 1, slots);
         if (startPos < avgPos[order[i]]) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * 
    * @param filename
    * @param d
    * @param queryId
    * @throws IOException
    * @throws ParseException
    *
    *
    * This is a class that generate an output for debugging. This generate the agentinfo in specific game, day, and query
    *
    */
   
   
   public void generateTestingFiles(String filename, int d, int queryId) throws IOException, ParseException
  	{
	   BufferedWriter resultToCarleton=new BufferedWriter(new FileWriter("ResultToCarletonOutPut"));

//	         if(SUMMARY) {
//	            resultToCarleton.write(filename+'\n');
//	         }

	   		
	   
	         // Load this game and its basic parameters
	         GameStatus status = new GameStatusHandler(filename).getGameStatus();
	       
	         int NUM_SLOTS = status.getSlotInfo().getRegularSlots();
	        

	         // Make the query space
	         int numQueries=16;
	         Query[] queryArr = new Query[numQueries];
	  	   
	         	queryArr[0] = new Query(null,null);
	         	queryArr[1] = new Query(null,"audio");
	         	queryArr[2] = new Query(null,"dvd");
	         	queryArr[3] = new Query ("lioneer","dvd");
	         	queryArr[4] = new Query ("flat","tv");
	         	queryArr[5] = new Query ("lioneer",null);
	         	queryArr[6] = new Query ("pg","dvd");
	         	queryArr[7] = new Query ("flat","dvd");
		  	 	queryArr[8] = new Query ("flat","audio");
		  		queryArr[9] = new Query ("lioneer","tv");
		  		queryArr[10] = new Query ("flat",null);
		  		queryArr[11] = new Query (null,"tv");
		  		queryArr[12] = new Query ("lioneer","audio");
		  		queryArr[13] = new Query ("pg","tv");
		  		queryArr[14] = new Query ("pg","audio");
		  		queryArr[15] = new Query ("pg",null);
		  	   
	         
	         
	         String[] agents = status.getAdvertisers();
	         
  	 
         
	         // The original data without deducing
	         Double[] actualAveragePositions = getAveragePositions(status, d, queryArr[queryId]);

	         // Get avg position for each agent
	         Double[] sampledAveragePositions = getSampledAveragePositions(status, d, queryArr[queryId]);

	         Double[] bids = getBids(status, d, queryArr[queryId]);

	         // Get squashed bids for each agent
	         Double[] squashedBids = getSquashedBids(status, d, queryArr[queryId]);

	         Double[] budgets = getBudgets(status, d, queryArr[queryId]);


	         // Get total number of impressions for each agent
	         Integer[] impressions = getAgentImpressions(status, d, queryArr[queryId]);
         
	         double[] impsDistMean = getAgentImpressionsDistributionMeanOrStdev(status,queryArr[queryId] , d, true);
	         double[] impsDistStdev = getAgentImpressionsDistributionMeanOrStdev(status, queryArr[queryId], d, false);
       
	         System.out.println(agents.length+" "+NUM_SLOTS);
	         resultToCarleton.write(agents.length+" "+NUM_SLOTS+"\n");
	         for (int i = 0; i < agents.length; i++) {
	        	 System.out.println((i+1)+" "+actualAveragePositions[i]+" "+impressions[i]+" "+squashedBids[i]+" "+budgets[i]+" "+sampledAveragePositions[i]+impsDistMean[i]+impsDistStdev[i]);
	        	 try {
	        		 resultToCarleton.write((i+1)+" "+agents[i]+" "+actualAveragePositions[i]+" "+impressions[i]+" "+squashedBids[i]+" "+budgets[i]+" "+sampledAveragePositions[i]+" "+impsDistMean[i]+" "+impsDistStdev[i]+'\n');
  			
	        	 } catch (IOException e) {
  			
	        		 e.printStackTrace();
	        	 }
	         }
	         resultToCarleton.flush();
	         resultToCarleton.close();
         
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
  *
   */
   public int getMaxImps(int slots, int agents, int[] order, int[] impressions) {
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

      int totImps = 0;
      for(int i = 0; i < impressionsBySlot.length; i++) {
         totImps += impressionsBySlot[i][0];
      }

      return totImps;
   }

   public static int[] getDropoutPoints(int[] impsPerAgent, int[] order, int numSlots) {
      ArrayList<Integer> impsBeforeDropout = new ArrayList<Integer>();
      PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
      for (int i = 0; i < numSlots && i < impsPerAgent.length; i++) {
         queue.add(impsPerAgent[order[i]]);
      }

      int lastIdx = queue.size();

      while (!queue.isEmpty()) {
         int val = queue.poll();
         impsBeforeDropout.add(val);

         if (lastIdx < impsPerAgent.length) {
            queue.add(impsPerAgent[order[lastIdx]] + val);
            lastIdx++;
         }
      }

      return convertListToArr(impsBeforeDropout);
   }

   public static int[] getImpsSeen(int[] impsPerAgent, int[] order, int numSlots) {
      ArrayList<Integer> impsSeen = new ArrayList<Integer>();
      PriorityQueue<int[]> queue = new PriorityQueue<int[]>(impsPerAgent.length,new Comparator<int[]>(){
         public int compare(int[] o1, int[] o2) {
            return new Integer(o1[0]).compareTo(o2[0]);
         }
      });

      for (int i = 0; i < numSlots && i < impsPerAgent.length; i++) {
         queue.add(new int[]{impsPerAgent[order[i]],impsPerAgent[order[i]]});
      }

      int lastIdx = queue.size();

      while (!queue.isEmpty()) {
         int[] val = queue.poll();
         impsSeen.add(val[1]);

         if (lastIdx < impsPerAgent.length) {
            queue.add(new int[]{impsPerAgent[order[lastIdx]] + val[0], impsPerAgent[order[lastIdx]]});
            lastIdx++;
         }
      }

      return convertListToArr(impsSeen);
   }

   public static int[] convertListToArr(List<Integer> integers) {
      int[] ret = new int[integers.size()];
      for (int i = 0; i < ret.length; i++) {
         ret[i] = integers.get(i);
      }
      return ret;
   }

   public static int[][] makeOrderMatrix(int[] impressions, int[] order, int numSlots) {
      int[] impsSeenArr = getImpsSeen(impressions,order,numSlots);

      int[][] orderMat = new int[impsSeenArr.length][order.length];
      for(int i = 0; i < order.length; i++) {
         orderMat[0][i] = order[i];
      }

      ArrayList<int[]> impArr = new ArrayList<int[]>(impressions.length);
      for(int i = 0; i < impressions.length; i++) {
         impArr.add(new int[]{impressions[i], i});
      }

      for(int i = 0; i < (impsSeenArr.length - 1); i++) {
         int impsSeen = impsSeenArr[i];

         int impIdx = -1;
         int remIdx = -1;
         for(int j = 0; j < impArr.size(); j++) {
            if(impsSeen == impArr.get(j)[0]) {
               impIdx = impArr.get(j)[1];
               remIdx = j;
               break;
            }
         }

         if(impIdx > -1) {
            impArr.remove(remIdx);
            int agentRank = -1;
            for(int j = 0; j < order.length; j++) {
               if(impIdx == orderMat[i][j]) {
                  agentRank = j;
                  break;
               }
            }

            for(int j = 0; j < order.length; j++) {
               if(j < agentRank) {
                  orderMat[i+1][j] = orderMat[i][j];
               }
               else if(j < (order.length - 1 - i)) {
                  orderMat[i+1][j] = orderMat[i][j+1];
               }
               else {
                  orderMat[i+1][j] = -1;
               }
            }
         }
         else {
            throw new RuntimeException("This shouldn't happen");
         }
      }

      return orderMat;
   }

   /**
    *
    * @param impressions
    * @param order
    * @return sampled avgpos
    */
   private double[] resampleAgents(int[] impressions, int[] order, int numSamples, int approxSearchers, boolean withReplacement) {
      if(impressions.length == 0) {
         return new double[]{};
      }

      int[] dropoutPoints = getDropoutPoints(impressions,order,NUM_SLOTS);
      int[][] orderMat = makeOrderMatrix(impressions,order,NUM_SLOTS);


//      System.out.println(Arrays.toString(dropoutPoints));
//      for(int i = 0; i < orderMat.length; i++ ){
//         System.out.println(Arrays.toString(orderMat[i]));
//      }
//      System.out.println("***********************");

      int[] samples = new int[impressions.length];
      int[] posSum = new int[impressions.length];

      int maxImps = dropoutPoints[dropoutPoints.length-1];
      if(maxImps < .05 * approxSearchers) {
         maxImps = Math.max(maxImps,approxSearchers);
      }

      numSamples = Math.min(maxImps,numSamples);

      ArrayList<Integer> sampleArr = new ArrayList<Integer>(numSamples);
      if(withReplacement) {
         for(int i = 0; i < numSamples; i++) {
            sampleArr.add(_rand.nextInt(maxImps));
         }
      }
      else {
         while(sampleArr.size() < numSamples) {
            int sample = _rand.nextInt(maxImps);
            if(!sampleArr.contains(new Integer(sample))) {
               sampleArr.add(sample);
            }
         }
      }

      for(Integer sample : sampleArr) {
         int[] sampleOrder = null;
         for(int j = 0; j < dropoutPoints.length; j++) {
            if(sample < dropoutPoints[j]) {
//               if((j != (dropoutPoints.length-1)) &&
//                       dropoutPoints[j] == dropoutPoints[j+1]) {
//                  continue;
//               }

               sampleOrder = orderMat[j];
               break;
            }
         }

         if(sampleOrder != null) {
            for(int j = 0; j < sampleOrder.length && j < NUM_SLOTS; j++) {
               int agent = sampleOrder[j];
               if(agent > -1) {
                  samples[agent]++;
                  posSum[agent] += (j+1);
               }
               else {
                  break;
               }
            }
         }
         //otherwise sample is out of range
      }

      double[] savg = new double[impressions.length];
      for(int i = 0; i < impressions.length; i++) {
         savg[i] = posSum[i] / ((double) samples[i]);
      }

      return savg;
   }


   /**
    * Input: a squashed bid ordering of all agents (where ordering[i] is the agent index that has the ith highest bid)
    *   the number of impressions each agent saw. (where impressions[a] is the number of impressions seen by agentIdx a)
    * Output: a squashed bid ordering of participants (where reducedOrdering[i] is the agent index that has the ith highest bid, out of all participants)
    * @param trueOrdering
    * @param impressions
    * @return
    */
   private int[] reduceTrueOrderingToParticipants(int[] trueOrdering,
                                                  Integer[] impressions) {
      ArrayList<Integer> reducedOrdering = new ArrayList<Integer>();
      for (int i=0; i<trueOrdering.length; i++) {
         int agentIdx = trueOrdering[i];
         if (impressions[agentIdx] > 0) {
            reducedOrdering.add(agentIdx);
         }
      }

      int[] reducedOrderingArr = new int[reducedOrdering.size()];
      for (int i=0; i<reducedOrdering.size(); i++) {
         reducedOrderingArr[i] = reducedOrdering.get(i);
      }

      return reducedOrderingArr;
   }



   /**
    * Gets a QAInstance in the format that Carleton's algorithm wants.
    *
    * @param NUM_SLOTS
    * @param NUM_PROMOTED_SLOTS
    * @param numParticipants
    * @param reducedAvgPos
    * @param reducedSampledAvgPos
    * @param agentIds
    * @param ourAgentIdx
    * @param ourImps
    * @param ourPromotedImps
    * @param impressionsUB
    * @param ourPromotionEligibility
    * @param reducedImpsDistMean
    * @param reducedImpsDistStdev
    * @return
    */
   private QAInstanceAll getCarletonQAInstance(int NUM_SLOTS, int NUM_PROMOTED_SLOTS,
                                            int numParticipants, double[] reducedAvgPos,
                                            double[] reducedSampledAvgPos, int[] agentIds, int ourAgentIdx,
                                            int ourImps, int ourPromotedImps, int impressionsUB,
                                            boolean ourPromotionEligibility, boolean ourHitBudget,
                                            double[] reducedImpsDistMean, double[] reducedImpsDistStdev,
                                            int[] ordering, String[] reducedAgentNames) {

      double[] avgPos = new double[reducedAvgPos.length];
      boolean considerPadding = false;

      //If exact average positions, just return a standard query instance (with no padding)
      if (!SAMPLED_AVERAGE_POSITIONS) {
         avgPos = reducedAvgPos.clone();
         return new QAInstanceAll(NUM_SLOTS, NUM_PROMOTED_SLOTS, numParticipants, avgPos, reducedSampledAvgPos, agentIds, ourAgentIdx,
                               ourImps, ourPromotedImps, impressionsUB, false, ourPromotionEligibility, ourHitBudget,
                               reducedImpsDistMean, reducedImpsDistStdev, SAMPLED_AVERAGE_POSITIONS, null, reducedAgentNames);
      } else {
         avgPos = reducedSampledAvgPos.clone();

         if (REPORT_FULLPOS_FORSELF) {
            avgPos[ourAgentIdx] = reducedAvgPos[ourAgentIdx];
         }

         //If any agents have a NaN sampled average position, give them a dummy average position
         //equal to Min(their starting position, numSlots)
         for (int i = 0; i < avgPos.length; i++) {
            if (Double.isNaN(avgPos[ordering[i]])) {
               avgPos[ordering[i]] = Math.min(i + 1, NUM_SLOTS);
            }
         }

         debug("actual=" + Arrays.toString(reducedAvgPos) + ", sample=" + Arrays.toString(reducedSampledAvgPos) + ", newSample=" + Arrays.toString(avgPos));
         return new QAInstanceAll(NUM_SLOTS, NUM_PROMOTED_SLOTS, numParticipants, avgPos, reducedSampledAvgPos, agentIds, ourAgentIdx,
                               ourImps, ourPromotedImps, impressionsUB, false, ourPromotionEligibility, ourHitBudget,
                               reducedImpsDistMean, reducedImpsDistStdev, SAMPLED_AVERAGE_POSITIONS, null, reducedAgentNames);
      }
   }


   /**
    * Rank agents that saw at least one impression by squashed bid.
    *
    * @param ordering
    * @param reducedImps
    * @return
    */
   private int[] getActualBidOrdering(int[] ordering, int[] reducedImps) {
      int[] actualBidOrdering = new int[ordering.length]; //[a b c d] means the agent with index 0 has rank a
      Arrays.fill(actualBidOrdering, -1);
      int rank = 0;
      for (int i = 0; i < actualBidOrdering.length; i++) {
         if (reducedImps[ordering[i]] > 0) {
            actualBidOrdering[ordering[i]] = rank;
            rank++;
         }
      }
      return actualBidOrdering;
   }


   /**
    * This gives, for each agent that was in the auction (i.e. saw at least one impression):
    * The actual squashed bid ranking of the agent, if the agent had at least one sample
    * -1, if the agent didn't see any samples.
    *
    * @param ordering
    * @param reducedImps
    * @param sampleAvgPos
    * @return
    */
   private int[] getSampledBidOrdering(int[] ordering, int[] reducedImps, double[] sampleAvgPos) {
      int[] actualBidOrdering = new int[ordering.length]; //[a b c d] means the agent with index 0 has rank a
      Arrays.fill(actualBidOrdering, -1);
      int rank = 0;
      for (int i = 0; i < actualBidOrdering.length; i++) {
         if (reducedImps[ordering[i]] > 0) {
            if (!Double.isNaN(sampleAvgPos[ordering[i]])) {
               actualBidOrdering[ordering[i]] = rank;
            }
            rank++;
         }
      }
      return actualBidOrdering;
   }


   /**
    * Takes as input the initial position each sampled agent was in.
    * Returns the relative ordering of each agent.
    *
    * @param initialPositions
    * @return
    */
   private int[] getRelativeOrdering(int[] initialPositions) {
      int[] relativeOrdering = new int[initialPositions.length];
      Arrays.fill(relativeOrdering, -1);
      for (int i = 0; i < relativeOrdering.length; i++) {
         if (initialPositions[i] == -1) {
            continue;
         }
         int numWithLowerPosition = 0;
         for (int j = 0; j < relativeOrdering.length; j++) {
            if (initialPositions[j] == -1) {
               continue;
            }
            if (initialPositions[j] < initialPositions[i]) {
               numWithLowerPosition++;
            }
         }
         relativeOrdering[i] = numWithLowerPosition;
      }
      return relativeOrdering;
   }


   /**
    * Varifies that data is valid for testing.
    * Currently just makes sure that squashed bids aren't NaN values.
    *
    * @param reducedBids
    * @return
    */
   private boolean validData(double[] reducedBids) {
      for (int i = 0; i < reducedBids.length; i++) {
         if (Double.isNaN(reducedBids[i])) {
            return false;
         }
      }
      return true;
   }


   private boolean duplicateSquashedBids(double[] reducedBids, int[] ordering) {
      // If any agents have the same squashed bids, we won't know the definitive ordering.
      // (TODO: Run the waterfall to determine the correct ordering (or at least a feasible one).)
      // For now, we'll drop any instances with duplicate squashed bids.
      for (int i = 1; i < reducedBids.length; i++) {
         if (reducedBids[ordering[i]] == reducedBids[ordering[i - 1]]) {
            return true;
         }
      }
      return false;
   }


   private int getApproximateNumSearchersInQuery(GameStatus status, int d, Query q) {
      int imps = 0;

      for (Product product : status.getRetailCatalog()) {
         HashMap<GameStatusHandler.UserState, Double> userDist = status.getUserDistributions().get(d).get(product);
         if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
            imps += userDist.get(GameStatusHandler.UserState.F0);
            imps += (1.0 / 3.0) * userDist.get(GameStatusHandler.UserState.IS);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
            if (product.getComponent().equals(q.getComponent()) || product.getManufacturer().equals(q.getManufacturer())) {
               imps += (1.0 / 2.0) * userDist.get(GameStatusHandler.UserState.F1);
               imps += (1.0 / 6.0) * userDist.get(GameStatusHandler.UserState.IS);
            }
         } else {
            if (product.getComponent().equals(q.getComponent()) && product.getManufacturer().equals(q.getManufacturer())) {
               imps += userDist.get(GameStatusHandler.UserState.F2);
               imps += (1.0 / 3.0) * userDist.get(GameStatusHandler.UserState.IS);
            }
         }
      }
      return imps;
   }


   private int getAgentImpressionsUpperBound(GameStatus status, boolean perfectImps, int d, Query q, int[] impressions, int[] order) {
      int imps;

      if (perfectImps) {
         imps = getMaxImps(status.getSlotInfo().getRegularSlots(),order.length,order,impressions);
      } else {
         if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
            imps = MAX_F0_IMPS;
         } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
            imps = MAX_F1_IMPS;
         } else {
            imps = MAX_F2_IMPS;
         }
      }

      return imps;
   }

   private double[] getAgentImpressionsDistributionMeanOrStdev(GameStatus status, Query query, int d, boolean getMean) {
      String[] agents = status.getAdvertisers();
      double[] meanOrStdevImps = new double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         int imps = status.getQueryReports().get(agentName).get(d).getImpressions(query);

         //Potentially add some noise to the mean imps prior
         double noisyImps = Math.max(0, imps + _rand.nextGaussian() * imps * PRIOR_NOISE_MULTIPLIER);

         if (getMean) {
            meanOrStdevImps[a] = noisyImps;
         } else {
            meanOrStdevImps[a] = Math.max(1, imps * PRIOR_STDEV_MULTIPLIER); //Ensure stdev is never 0
         }
      }
      return meanOrStdevImps;
   }

   private double[] getAgentImpressionsDistributionMeanOrStdev(GameStatus status, Query query, boolean getMean) {
      String[] agents = status.getAdvertisers();
      double[] meanOrStdevImps = new double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         ArrayList<Double> dailyImps = new ArrayList<Double>();
         String agentName = agents[a];
         LinkedList<QueryReport> agentQueryReports = status.getQueryReports().get(agentName);
         for (int d = 0; d < agentQueryReports.size(); d++) {
            int imps = agentQueryReports.get(d).getImpressions(query);
            if (imps > 0) {
               dailyImps.add(new Double(imps));
            }
         }
         double[] meanAndStdev = getStdDevAndMean(dailyImps);
         if (getMean) {
            meanOrStdevImps[a] = meanAndStdev[0];
         } else {
            meanOrStdevImps[a] = meanAndStdev[1];
         }
      }
      return meanOrStdevImps;
   }

   private double[] getAgentImpressionsDistributionStdev(GameStatus status, Query query) {
      String[] agents = status.getAdvertisers();
      double[] meanImps = new double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         ArrayList<Double> dailyImps = new ArrayList<Double>();
         String agentName = agents[a];
         LinkedList<QueryReport> agentQueryReports = status.getQueryReports().get(agentName);
         for (int d = 0; d < agentQueryReports.size(); d++) {
            int imps = agentQueryReports.get(d).getImpressions(query);
            if (imps > 0) {
               dailyImps.add(new Double(imps));
            }
         }
         double[] meanAndStdev = getStdDevAndMean(dailyImps);
         meanImps[a] = meanAndStdev[1];
      }
      return meanImps;
   }


   private double[] getAgentImpressionsDistributionMean2(GameStatus status, Query query) {
      String[] agents = status.getAdvertisers();
      double[] meanImps = new double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         int daysWithImps = 0;
         int aggregateImps = 0;

         String agentName = agents[a];
         LinkedList<QueryReport> agentQueryReports = status.getQueryReports().get(agentName);
         for (int d = 0; d < agentQueryReports.size(); d++) {
            int imps = agentQueryReports.get(d).getImpressions(query);
            if (imps > 0) {
               daysWithImps++;
               aggregateImps += imps;
            }
         }
         if (daysWithImps > 0) {
            meanImps[a] = aggregateImps / (double) daysWithImps;
         } else {
            meanImps[a] = Double.NaN;
         }
      }
      return meanImps;
   }

   private double[] getAgentImpressionsDistributionStdev2(GameStatus status, Query query) {
      String[] agents = status.getAdvertisers();
      double[] stdevImps = new double[agents.length];

      for (int a = 0; a < agents.length; a++) {
         double daysWithImps = 0;
         double aggregateImps = 0;
         double aggregateSquaredImps = 0;

         String agentName = agents[a];
         LinkedList<QueryReport> agentQueryReports = status.getQueryReports().get(agentName);
         for (int d = 0; d < agentQueryReports.size(); d++) {
            int imps = agentQueryReports.get(d).getImpressions(query);
            if (imps > 0) {
               daysWithImps++;
               aggregateImps += imps;
               aggregateSquaredImps += Math.pow(imps, 2);
            }
         }
         if (daysWithImps > 0) {
            double mean = aggregateImps / daysWithImps;
            double variance = (aggregateSquaredImps / daysWithImps) - Math.pow(mean, 2);
            stdevImps[a] = Math.sqrt(variance);
         } else {
            stdevImps[a] = Double.NaN;
         }
      }
      return stdevImps;
   }


   //=======================================================================//
   //=============================== LOGGING ===============================//
   //=======================================================================//

   private void initializeLog(String filename) {
      try {
         //Construct the BufferedWriter object
         bufferedWriter = new BufferedWriter(new FileWriter(filename));

         //Create header
         StringBuffer sb = new StringBuffer();
         sb.append("model,");
         sb.append("game.idx,");
         sb.append("day.idx,");
         sb.append("query.idx,");
         sb.append("our.agent,");
         sb.append("our.bid.rank,");
         sb.append("opp.bid.rank,");
         sb.append("predicted.opp.bid.rank,");
         sb.append("our.avg.pos,");
         sb.append("opp.avg.pos,");
         sb.append("opp.sample.avg.pos,");
         sb.append("focus.level,");
         sb.append("num.participants,");
         sb.append("actual.imps,");
         sb.append("predicted.imps,");
         sb.append("seconds");

         //Start writing to the output stream
         bufferedWriter.write(sb.toString());
         bufferedWriter.newLine();

      } catch (FileNotFoundException ex) {
         ex.printStackTrace();
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }

   private void closeLog() {
      try {
         if (bufferedWriter != null) {
            bufferedWriter.flush();
            bufferedWriter.close();
         }
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }

   private void writeToLog(String data) {
      try {
         bufferedWriter.write(data);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


   /**
    * @param predictedImpsPerAgent
    * @param actualImpsPerAgent
    */
   private void updatePerformanceMetrics(int[] predictedImpsPerAgent, int[] actualImpsPerAgent, int predTotImpr, int actualTotImpr, int[] predictedOrdering, int[] actualOrdering, double[] reducedSampledAvgPos) {
      assert (predictedImpsPerAgent.length == actualImpsPerAgent.length);
      
      //System.out.println("reducedSampledAvgPos=" + Arrays.toString(reducedSampledAvgPos));
      
      for (int a = 0; a < actualImpsPerAgent.length; a++) {
    	  if (Double.isNaN(reducedSampledAvgPos[a])) continue; //Don't make predictions if no sampled avg position was received
         numImprsPredictions++;
         aggregateAbsImprError += Math.abs(predictedImpsPerAgent[a] - actualImpsPerAgent[a]);
      }

      aggregateAbsTotImprError += Math.abs(predTotImpr - actualTotImpr);
      numTotImprPredictions++;

      numOrderingPredictions++;
      //Keep track of the number of times that:
      //1. the predicted number of participants was correct
      //2. the predicted list of participants was correct
      //3. the predicted ordering was correct
      if (predictedOrdering.length == actualOrdering.length) {
         numCorrectNumParticipants++;

         int[] predictedOrderingSorted = predictedOrdering.clone();
         int[] actualOrderingSorted = actualOrdering.clone();
         Arrays.sort(predictedOrderingSorted);
         Arrays.sort(actualOrderingSorted);
         boolean isCorrectParticipants = true;
         boolean isCorrectOrdering = true;
         for (int a=0; a<actualOrderingSorted.length; a++) {
       	  	if (Double.isNaN(reducedSampledAvgPos[a])) continue; //Don't make predictions if no sampled avg position was received

            if (predictedOrderingSorted[a] != actualOrderingSorted[a]) isCorrectParticipants = false;
            if (predictedOrdering[a] != actualOrdering[a]) isCorrectOrdering = false;
         }
         if (isCorrectParticipants) numCorrectParticipants++;
         if (isCorrectOrdering) numCorrectlyOrderedInstances++;
      }

      debug("    (predicted ordering): " + Arrays.toString(predictedOrdering));
      debug("       (actual ordering): " + Arrays.toString(actualOrdering));
   }


   private void outputPerformanceMetrics() {
      double meanAbsImprError = aggregateAbsImprError / numImprsPredictions;
      double meanAbsTotImprError = aggregateAbsTotImprError / numTotImprPredictions;
      double pctCorrectNumParticipants = numCorrectNumParticipants / (double) numOrderingPredictions;
      double pctCorrectParticipants = numCorrectParticipants / (double) numOrderingPredictions;
      double pctCorrectOrdering = numCorrectlyOrderedInstances / (double) numOrderingPredictions;
      double pctSkipped = (numSkips) / ((double) numInstances);
      System.out.println("Pct Instances Skipped: " + pctSkipped + " (duplicateBids=" + numSkipsDuplicateBid + ", invalidData=" + numSkipsInvalidData);
      System.out.println("Pct null IEResult: " + nullIEResult / ((double) numOrderingPredictions));
      System.out.println("Mean absolute impr error: " + meanAbsImprError);
      System.out.println("Mean absolute tot impr error: " + meanAbsTotImprError);
      System.out.println("Pct correct num participants: " + numCorrectNumParticipants + "/" + numOrderingPredictions + "=" + pctCorrectNumParticipants);
      System.out.println("Pct correct participants: " + numCorrectParticipants + "/" + numOrderingPredictions + "=" + pctCorrectParticipants);
      System.out.println("Pct correctly ordered instances: " + numCorrectlyOrderedInstances + "/" + numOrderingPredictions + "=" + pctCorrectOrdering);
   }

   private double[] getPerformanceMetrics() {
      double[] impRankErr = new double[3];
      impRankErr[0] = aggregateAbsImprError / numImprsPredictions;
      impRankErr[1] = aggregateAbsTotImprError / numTotImprPredictions;
      impRankErr[2] = numCorrectlyOrderedInstances / (double) numOrderingPredictions;
      return impRankErr;
   }


   /**
    * Get an array of average positions (one element for each agent), for the given day/query
    *
    * @param status
    * @param d
    * @param query
    * @return
    */
   private Double[] getAveragePositions(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] averagePositions = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            averagePositions[a] = status.getQueryReports().get(agentName).get(d).getPosition(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            averagePositions[a] = Double.NaN;
            throw new RuntimeException("Exception when getting average positions");
         }
      }
      return averagePositions;
   }

   private Double[] getSampledAveragePositions(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] sampledAveragePositions = new Double[agents.length];

      //Samples should be the same for every agent. (TODO: verify this)
      String arbitraryAgentName = agents[0];
      for (int a = 0; a < agents.length; a++) {
         String agentName = "adv" + (a + 1);
         try {
            sampledAveragePositions[a] = status.getQueryReports().get(arbitraryAgentName).get(d).getPosition(query, agentName);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            sampledAveragePositions[a] = Double.NaN;
            throw new RuntimeException("Exception when getting sampled average positions");
         }
      }
      debug("sampleAvgPos=" + Arrays.toString(sampledAveragePositions) + ", agent=" + arbitraryAgentName + ", d=" + d + ", q=" + query);

      return sampledAveragePositions;
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

   private Double[] getSquashedBids(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] squashedBids = new Double[agents.length];
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


   private Double[] getBudgets(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] budgets = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            budgets[a] = status.getBidBundles().get(agentName).get(d).getDailyLimit(query);
         } catch (Exception e) {
            budgets[a] = Double.NaN;
         }
      }
      return budgets;
   }


   private Double[] getGlobalBudgets(GameStatus status, int d) {
      String[] agents = status.getAdvertisers();
      Double[] globalBudgets = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            globalBudgets[a] = status.getBidBundles().get(agentName).get(d).getCampaignDailySpendLimit();
         } catch (Exception e) {
            globalBudgets[a] = Double.NaN;
         }
      }
      return globalBudgets;
   }


   private Double[] getAdvertiserEffects(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] advEffects = new Double[agents.length];
      UserClickModel userClickModel = status.getUserClickModel();
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            advEffects[a] = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), a);
         } catch (Exception e) {
            advEffects[a] = Double.NaN;
         }
      }
      return advEffects;
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

   private Integer[] getAgentClicks(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Integer[] agentClicks = new Integer[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            agentClicks[a] = status.getQueryReports().get(agentName).get(d).getClicks(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            agentClicks[a] = null;
         }
      }
      return agentClicks;
   }

   private Integer[] getAgentConversions(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Integer[] agentConversions = new Integer[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            agentConversions[a] = status.getSalesReports().get(agentName).get(d).getConversions(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            agentConversions[a] = null;
         }
      }
      return agentConversions;
   }


   private Double[] getAgentCosts(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] agentCosts = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         try {
            agentCosts[a] = status.getQueryReports().get(agentName).get(d).getCost(query);
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            agentCosts[a] = null;
         }
      }
      return agentCosts;
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


   private Boolean[] getAgentPromotionEligibility(GameStatus status, int d, Query query, double promotedReserveScore) {
      Double[] squashedBids = getSquashedBids(status, d, query);
      Boolean[] promotionEligibility = new Boolean[squashedBids.length];
      //double promotedReserveScore = status.getReserveInfo().getPromotedReserve(); //This is always returning 0.
      for (int a = 0; a < squashedBids.length; a++) {
         try {
            //TODO: Squashed bid must be > or >= promoted reserve score?
            if (squashedBids[a] >= promotedReserveScore) {
               promotionEligibility[a] = true;
            } else {
               promotionEligibility[a] = false;
            }
         } catch (Exception e) {
            //May get here if the agent doesn't have a query report (does this ever happen?)
            promotionEligibility[a] = false;
         }
      }
      return promotionEligibility;
   }


   /**
    * Determines whether or not each agent hit their query or global budget for the given day.
    * FIXME: it is assumed that an agent hits their budget if an additional click would put them
    * over their spending limit. We are also being slightly liberal about who hit their budget,
    * since we're assuming that the CPC for the last click equals the agent's average CPC.
    * We should check to see how hitting budget is actually determined. (i.e. make sure it's not
    * bids that determine whether the agent has hit its budget)
    *
    * @param status
    * @param d
    * @param query
    * @return
    */
   private Boolean[] getAgentHitBudget(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Boolean[] agentHitBudget = new Boolean[agents.length];
      for (int a = 0; a < agentHitBudget.length; a++) {
         String agentName = agents[a];
         QueryReport queryReport = status.getQueryReports().get(agentName).get(d);
         BidBundle bidBundle = status.getBidBundles().get(agentName).get(d);

         //Get total amount spent for this agent (across queries)
         double globalSpend = 0;
         double maxBid = 0;
         for (Query q : queryReport.keys()) {
            globalSpend += queryReport.getCost(q);

            double bid = queryReport.getCPC(q); //double bid = bidBundle.getBid(q);
            if (!Double.isNaN(bid) && bid > maxBid) {
               maxBid = bid;
               //maxBid = Math.max(maxBid, bidBundle.getBid(q) );
            }
         }

         //Get amount spent for this agent (for this query only)
         double querySpend = queryReport.getCost(query);
         double queryBid = queryReport.getCPC(query); //double queryBid = bidBundle.getBid(query);

         //Get budgets for this agent
         double maxQuerySpend = bidBundle.getDailyLimit(query);
         double maxGlobalSpend = bidBundle.getCampaignDailySpendLimit();

         //If an extra click would have put us over a spending limit, we've hit our budget
         //NOTE: This is a liberal estimate: we could say we hit our budget even though we didn't. See method description.
         //TODO: > or >=? What about global budget? Does agent drop out of every auction at once when global budget is hit,
         //  or would it stay in auctions that it can still afford? (should we use maxBid or queryBid?)
         if (querySpend + queryBid > maxQuerySpend ||
                 globalSpend + maxBid > maxGlobalSpend) {
            agentHitBudget[a] = true;
         } else {
            agentHitBudget[a] = false;
         }
      }
      return agentHitBudget;
   }


   private Double[] getAgentQuerySpend(GameStatus status, int d, Query query) {
      String[] agents = status.getAdvertisers();
      Double[] agentCost = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         agentCost[a] = status.getQueryReports().get(agentName).get(d).getCost(query);
      }
      return agentCost;
   }

   private Double[] getAgentGlobalSpend(GameStatus status, int d) {
      String[] agents = status.getAdvertisers();
      Double[] agentCost = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         QueryReport queryReport = status.getQueryReports().get(agentName).get(d);

         //Get total amount spent for this agent (across queries)
         double globalSpend = 0;
         for (Query q : queryReport.keys()) {
            globalSpend += queryReport.getCost(q);
         }
         agentCost[a] = globalSpend;
      }
      return agentCost;
   }

   private Double[] getAgentGlobalMaxAvgCPC(GameStatus status, int d) {
      String[] agents = status.getAdvertisers();
      Double[] maxCPCs = new Double[agents.length];
      for (int a = 0; a < agents.length; a++) {
         String agentName = agents[a];
         QueryReport queryReport = status.getQueryReports().get(agentName).get(d);
         BidBundle bidBundle = status.getBidBundles().get(agentName).get(d);

         //Get total amount spent for this agent (across queries)
         double maxCPC = 0;
         for (Query q : queryReport.keys()) {
            double cpc = queryReport.getCPC(q);
            //System.out.println("a=" + a + ", q=" + q + ", bid=" + bidBundle.getBid(q) + ", CPC=" + queryReport.getCPC(q));
            if (!Double.isNaN(cpc) && cpc > maxCPC) {
               maxCPC = cpc;
            }
         }
         maxCPCs[a] = maxCPC;
      }
      return maxCPCs;
   }


   //TODO: Make this more precise. (Isn't this value logged anywhere??)
   private HashMap<QueryType, Double> getApproximatePromotedReserveScore(GameStatus status) {

      //This is our approximation of promoted reserve score: the lowest score for which someone received a promoted slot.
      HashMap<QueryType, Double> currentLowPromotedScore = new HashMap<QueryType, Double>();
      //double currentLowPromotedScore = Double.POSITIVE_INFINITY;
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ZERO, Double.POSITIVE_INFINITY);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_ONE, Double.POSITIVE_INFINITY);
      currentLowPromotedScore.put(QueryType.FOCUS_LEVEL_TWO, Double.POSITIVE_INFINITY);

      // Make the query space
      //TODO: Don't hardcode this here.
      LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
      querySpace.add(new Query(null, null)); //F0
      for (Product product : status.getRetailCatalog()) {
         querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
         querySpace.add(new Query(null, product.getComponent())); // F1 Component only
         querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
      }
      int numDays = 57; //TODO: Don't hardcode this.
      for (int d = 0; d < numDays; d++) {
         for (Query q : querySpace) {
            Integer[] promotedImps = getAgentPromotedImpressions(status, d, q);
            Double[] squashedBids = getSquashedBids(status, d, q);

            for (int a = 0; a < promotedImps.length; a++) {
               if (promotedImps[a] > 0 && squashedBids[a] < currentLowPromotedScore.get(q.getType())) {
                  currentLowPromotedScore.put(q.getType(), squashedBids[a]);
               }
            }
         }
      }
      return currentLowPromotedScore;
   }


   //Get the indices of the vals, starting with the highest val and decreasing.
   public static int[] getIndicesForDescendingOrder(double[] valsUnsorted) {
      double[] vals = valsUnsorted.clone(); //these values will be modified
      int length = vals.length;

      int[] ids = new int[length];
      for (int i = 0; i < length; i++) {
         ids[i] = i;
      }

      for (int i = 0; i < length; i++) {
         for (int j = i + 1; j < length; j++) {
            if (vals[i] < vals[j]) {
               double tempVal = vals[i];
               int tempId = ids[i];

               vals[i] = vals[j];
               ids[i] = ids[j];

               vals[j] = tempVal;
               ids[j] = tempId;
            }
         }
      }
      return ids;
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


   
   
   
//   sampled=true, perfectImps=false, usePrior=false, noiseFactor=0.0, solver=CP
//
//
//   Game 1/1, Day 2/57, query=1
//   
   public static void runDebuggingTests() throws IOException, ParseException {
	   boolean useWaterfallPriors = false;
	   double noiseFactor = 0;
	   HistoricalPriorsType historicPriorsType = HistoricalPriorsType.EMA;
	   GameSet GAMES_TO_TEST = GameSet.finals2010;
	   int START_GAME = 15127;
	   int END_GAME = 15127;
	   //	      int END_GAME = 15136;
	   //	      int END_GAME = 15170;
	   int START_DAY = 0; //0
	   int END_DAY = 57; //57
	   int START_QUERY = 0; //0
	   int END_QUERY = 15; //15
	   String AGENT_NAME = "Schlemazl"; //(Schlemazl, crocodileagent, McCon, Nanda_AA, TacTex, tau, Mertacor, MetroClick, all)
	   double IP_TIMEOUT_IN_SECONDS = 30;
	   int sampFrac = 100;
	   double upperBoundNoise = 1.2;
	   double convPrMult = .1;


	   boolean exactAvgPositions=true;
	   boolean orderingKnown=true; //FALSE? 
	   boolean perfectImps=false;
	   boolean useHistoricPriors = false;

	   StringBuffer sb = new StringBuffer();
	   boolean sampledAvgPositions = !exactAvgPositions;
	   ImpressionEstimatorTest evaluator = new ImpressionEstimatorTest(sampledAvgPositions, perfectImps, useWaterfallPriors, noiseFactor, useHistoricPriors, historicPriorsType, orderingKnown, upperBoundNoise);
	   double start;
	   double stop;
	   double secondsElapsed;


	   SolverType solverType = SolverType.CP; //SolverType.Carleton_LP_Exact; //SolverType.SIMPLE_MIP_Exact; //SolverType.Carleton_LP;

	   
	   String tmp = "SUMMARY: ---------------\n";
	   tmp += "SUMMARY: STARTING TEST: exactAvgPos=" + exactAvgPositions + ", rankingKnown=" + orderingKnown + ", perfectImps=" + perfectImps + ", useHistoricPrior=" + useHistoricPriors + ", solver=" + solverType + "\n";
	   System.out.println(tmp);
	   sb.append(tmp);
	   start = System.currentTimeMillis();
	   double[] results = evaluator.impressionEstimatorPredictionChallenge(solverType, GAMES_TO_TEST, START_GAME, END_GAME,
			   START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,sampFrac, IP_TIMEOUT_IN_SECONDS,convPrMult);
	   stop = System.currentTimeMillis();
	   secondsElapsed = (stop - start) / 1000.0;
	   tmp = "SUMMARY: results=" + Arrays.toString(results) + "\n";
	   tmp += "SUMMARY: Seconds elapsed: " + secondsElapsed + "\n";
	   System.out.println(tmp);
	   sb.append(tmp);
	   System.out.println("\n\n\n\n\n" + sb.toString());
	   
	   
	   
//	   solverType = SolverType.MIP_LP;
//	   tmp = "SUMMARY: ---------------\n";
//	   tmp += "SUMMARY: STARTING TEST: exactAvgPos=" + exactAvgPositions + ", rankingKnown=" + orderingKnown + ", perfectImps=" + perfectImps + ", useHistoricPrior=" + useHistoricPriors + ", solver=" + solverType + "\n";
//	   System.out.println(tmp);
//	   sb.append(tmp);
//	   start = System.currentTimeMillis();
//	   double[] results2 = evaluator.impressionEstimatorPredictionChallenge(solverType, GAMES_TO_TEST, START_GAME, END_GAME,
//			   START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,sampFrac, IP_TIMEOUT_IN_SECONDS,convPrMult);
//	   stop = System.currentTimeMillis();
//	   secondsElapsed = (stop - start) / 1000.0;
//	   tmp = "SUMMARY: results=" + Arrays.toString(results2) + "\n";
//	   tmp += "SUMMARY: Seconds elapsed: " + secondsElapsed + "\n";
//	   System.out.println(tmp);
//	   sb.append(tmp);
//	   System.out.println("\n\n\n\n\n" + sb.toString());
	   
	   
	   
   }

   
   
   
   public static void runAllTests() throws IOException, ParseException {

	   boolean useWaterfallPriors = false;
	   double noiseFactor = 0;
	   HistoricalPriorsType historicPriorsType = HistoricalPriorsType.EMA;
	   GameSet GAMES_TO_TEST = GameSet.finals2010;
	   int START_GAME = 15127;
	   int END_GAME = 15127;
	   //	      int END_GAME = 15136;
	   //	      int END_GAME = 15170;
	   int START_DAY = 0; //0
	   int END_DAY = 57; //57
	   int START_QUERY = 0; //0
	   int END_QUERY = 15; //15
	   String AGENT_NAME = "Schlemazl"; //(Schlemazl, crocodileagent, McCon, Nanda_AA, TacTex, tau, Mertacor, MetroClick, all)
	   double IP_TIMEOUT_IN_SECONDS = 6;
	   int sampFrac = 100;
	   double upperBoundNoise = 1.2;
	   double convPrMult = .1;

	   boolean[] trueArr = {true};
	   boolean[] falseArr = {false};
	   boolean[] trueOrFalseArr = {true, false};
	   //double[] noiseFactorArr = {0, .5, 1, 1.5};
	   //SolverType[] solverArr = SolverType.values();
	   //SolverType[] solverArr = {SolverType.CP, SolverType.MIP, SolverType.MIP_LP, SolverType.LDSMIP}; //, SolverType.MULTI_MIP};
//	   SolverType[] solverArr = {SolverType.CP, SolverType.MIP, SolverType.Carleton_LP}; //, SolverType.MULTI_MIP};
	   SolverType[] solverArr = {SolverType.LDSMIP, SolverType.Carleton_LP}; //, SolverType.MULTI_MIP};

	   StringBuffer sb = new StringBuffer();
	   for (boolean exactAvgPositions : trueOrFalseArr) {
		   for (boolean orderingKnown : trueOrFalseArr) {
			   for (boolean perfectImps : trueOrFalseArr) {
				   for (boolean useHistoricPriors : trueArr) {
					   //for (double noiseFactor : noiseFactorArr) {
					   //If we're not using priors, don't iterate through a bunch of noise factors (just use the first)
					   //  if (!useWaterfallPriors && noiseFactor != noiseFactorArr[0]) {
					   //     continue;
					   //  }
					   boolean sampledAvgPositions = !exactAvgPositions;
					   ImpressionEstimatorTest evaluator = new ImpressionEstimatorTest(sampledAvgPositions, perfectImps, useWaterfallPriors, noiseFactor, useHistoricPriors, historicPriorsType, orderingKnown, upperBoundNoise);
					   double start;
					   double stop;
					   double secondsElapsed;

					   
					   for (SolverType solverType : solverArr) {
						   String tmp = "SUMMARY: ---------------\n";
						   tmp += "SUMMARY: STARTING TEST: exactAvgPos=" + exactAvgPositions + ", rankingKnown=" + orderingKnown + ", perfectImps=" + perfectImps + ", useHistoricPrior=" + useHistoricPriors + ", solver=" + solverType + "\n";
						   System.out.println(tmp);
						   sb.append(tmp);
						   
						   start = System.currentTimeMillis();

						   double[] results = evaluator.impressionEstimatorPredictionChallenge(solverType, GAMES_TO_TEST, START_GAME, END_GAME,
								   START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,sampFrac, IP_TIMEOUT_IN_SECONDS,convPrMult);

						   stop = System.currentTimeMillis();
						   secondsElapsed = (stop - start) / 1000.0;

						   tmp = "SUMMARY: results=" + Arrays.toString(results) + "\n";
						   tmp += "SUMMARY: Seconds elapsed: " + secondsElapsed + "\n";
						   System.out.println(tmp);
						   sb.append(tmp);
						   
					   }
				   }
			   }
		   }
	   }
	   
	   System.out.println("\n\n\n\n\n" + sb.toString());
   }




   public static void main(String[] args) throws IOException, ParseException {

      /**
       * Command line:
       *   sample?
       *   perfect priors?
       *   perfect prior noise
       *   historic priors?
       *   historic prior type (Naive, LastNonZero, SMA, EMA)
       *   ordering known?
       *   solver type (CP, MIP, MIP_LP, ...)
       *   game set (finals2010, test2010, semifinals2010)
       *   start game
       *   end game
       *   start day
       *   end day
       *   start query
       *   end query
       *   agent to test (Schlemazl, crocodileagent, McCon, Nanda_AA, TacTex, tau, all)
       *
       */

      // java -cp ./bin:./lib/* simulator.predictions.ImpressionEstimatorTest SAMPLE PERFECTPRIORS NOISE HISTORICPRIORS Naive_LastNonZero_SMA_EMA ORDERING_KNOWN
      // java -cp ./bin:./lib/*:/lib/cplex.jar: simulator.predictions.ImpressionEstimatorTest false false 0 false null true CP test2010 2 2 0 57 0 15 all

      //System.out.println("qsub -cwd run.sh " + Arrays.toString(args).replace('[', ' ').replace(']', ' ').replace(',', ' '));
      //     evaluator.logGameData();


//	   ImpressionEstimatorTest.runAllTests();


	   
	   //*
	   boolean debuggingTests=true;
	   if (debuggingTests) {
		   runDebuggingTests();
		   System.exit(0);
	   }
	   //*/
	   
//	   
//	   
//	   boolean allTests=true;
//	   if (allTests) {
//		   runAllTests();
//		   System.exit(0);
//	   }
	   
	   
	   
      boolean sampleAvgPositions = true;
      boolean perfectImps = false;
      boolean useWaterfallPriors = false;
      double noiseFactor = 0.0;
      boolean useHistoricPriors = true;
      HistoricalPriorsType historicPriorsType = HistoricalPriorsType.EMA; //Naive, LastNonZero, SMA, EMA,
      boolean orderingKnown = true;
      SolverType solverToUse = SolverType.MIP; //SolverType.CP;

//      GameSet GAMES_TO_TEST = GameSet.test2010;
//      int START_GAME = 1;
//      int END_GAME = 4;
//      GameSet GAMES_TO_TEST = GameSet.finals2010;
//      int START_GAME = 15127;
//      int END_GAME = 15127;
      
      GameSet GAMES_TO_TEST = GameSet.semi2011server1;
      int START_GAME = 1414;
      int END_GAME = 1414;
      
//      int END_GAME = 15136;
//      int END_GAME = 15170;
      int START_DAY = 0; //0
      int END_DAY = 57; //57
      int START_QUERY = 0; //0
      int END_QUERY = 15; //15
      String AGENT_NAME = "Schlemazl"; //(Schlemazl, crocodileagent, McCon, Nanda_AA, TacTex, tau, Mertacor, MetroClick, all)
      double IP_TIMEOUT_IN_SECONDS = 6;
      int sampFrac = 100;
      double upperBoundNoise = 1.2;


      ImpressionEstimatorTest evaluator;

      //boolean sampleAvgPositions = true
//      if (args.length >= 7) {
//         sampleAvgPositions = new Boolean(args[0]);
//         useWaterfallPriors = new Boolean(args[1]);
//         noiseFactor = new Double(args[2]);
//         useHistoricPriors = new Boolean(args[3]);
//
//         HistoricalPriorsType[] types = HistoricalPriorsType.values();
//         for (HistoricalPriorsType type : types) {
//            if (args[4].equalsIgnoreCase(type.toString())) {
//               historicPriorsType = type;
//            }
//         }
//         if (useHistoricPriors && historicPriorsType == null) {
//            System.out.println("Error: passed historic priors type not found");
//            System.exit(0);
//         }
//
//         orderingKnown = new Boolean(args[5]);
//
//
//         SolverType[] solverTypes = SolverType.values();
//         for (SolverType solverType : solverTypes) {
//            if (args[6].equalsIgnoreCase(solverType.toString())) {
//               solverToUse = solverType;
//            }
//         }
//
//         if (args.length == 18) {
//            //Add game/day/query constraints if they were passed
//
//            GameSet[] gameSets = GameSet.values();
//            for (GameSet gameSet : gameSets) {
//               if (args[7].equalsIgnoreCase(gameSet.toString())) {
//                  GAMES_TO_TEST = gameSet;
//               }
//            }
//
//            START_GAME = new Integer(args[8]);
//            END_GAME = new Integer(args[9]);
//            START_DAY = new Integer(args[10]);
//            END_DAY = new Integer(args[11]);
//            START_QUERY = new Integer(args[12]);
//            END_QUERY = new Integer(args[13]);
//            AGENT_NAME = args[14];
//            IP_TIMEOUT_IN_SECONDS = new Double(args[15]);
//
//            sampFrac = new Integer(args[16]);
//            upperBoundNoise = new Double(args[17]);
//
//         }
//      } else {
//         if(DEBUG) {
//            System.out.println("Failed to read command line arguments. Will use defaults.");
//         }
//      }
//
//
//      if(args.length == 2) {
//         sampFrac = Integer.parseInt(args[0]);
//         upperBoundNoise = Double.parseDouble(args[1]);
//      }

      evaluator = new ImpressionEstimatorTest(sampleAvgPositions, perfectImps, useWaterfallPriors, noiseFactor, useHistoricPriors, historicPriorsType, orderingKnown, upperBoundNoise);
      ArrayList<String> gameString= evaluator.getGameStrings(GAMES_TO_TEST,START_GAME,END_GAME);
      evaluator.generateTestingFiles(gameString.get(0),0,0);

      if(SUMMARY || DEBUG) {
         //PRINT PARAMETERS
         System.out.println();
         System.out.println("sampleAvgPositions=" + sampleAvgPositions);
         System.out.println("perfectImps=" + perfectImps);
         System.out.println("useWaterfallPriors=" + useWaterfallPriors);
         System.out.println("noiseFactor=" + noiseFactor);
         System.out.println("useHistoricPriors=" + useHistoricPriors);
         System.out.println("historicPriorsType=" + historicPriorsType);
         System.out.println("orderingKnown=" + orderingKnown);
         System.out.println("solverToUse=" + solverToUse);
         System.out.println("GAMES_TO_TEST=" + GAMES_TO_TEST);
         System.out.println("START_GAME=" + START_GAME);
         System.out.println("END_GAME=" + END_GAME);
         System.out.println("START_DAY=" + START_DAY);
         System.out.println("END_DAY=" + END_DAY);
         System.out.println("START_QUERY=" + START_QUERY);
         System.out.println("END_QUERY=" + END_QUERY);
         System.out.println("AGENT_NAME=" + AGENT_NAME);
         System.out.println("IP_TIMEOUT_IN_SECONDS=" + IP_TIMEOUT_IN_SECONDS);
         System.out.println("sampFrac=" + sampFrac);
         System.out.println("upperBoundNoise=" + upperBoundNoise);
         System.out.println();
         //
      }

      double start;
      double stop;
      double secondsElapsed;
      start = System.currentTimeMillis();

      double[] results;

      double convPrMult = .1;
      if(args.length == 1) {
         convPrMult = Double.parseDouble(args[0]);
      }

      results = evaluator.impressionEstimatorPredictionChallenge(solverToUse, GAMES_TO_TEST, START_GAME, END_GAME,
                                                                 START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,sampFrac, IP_TIMEOUT_IN_SECONDS,convPrMult);

//      System.out.println(numSamples + "," + results[1] + "," + results[2]);

//	   evaluator.allModelPredictionChallenge(SolverType.LDSMIP, ORDERING_KNOWN);
//	   evaluator.allModelPredictionChallenge(SolverType.MIP);
//      evaluator.allModelPredictionChallenge(SolverType.MIP_LP);
//	   evaluator.allModelPredictionChallenge(SolverType.MULTI_MIP, ORDERING_KNOWN);
//      evaluator.allModelPredictionChallenge(SolverType.CP);
//      evaluator.allModelPredictionChallenge(SolverType.CP, ORDERING_KNOWN,Integer.parseInt(args[0]),Integer.parseInt(args[1]));
//      evaluator.allModelPredictionChallenge(SolverType.CP, ORDERING_KNOWN,Integer.parseInt(args[0]),Integer.parseInt(args[1]));
      stop = System.currentTimeMillis();
      secondsElapsed = (stop - start) / 1000.0;
      if(SUMMARY) {
         System.out.println("SECONDS ELAPSED: " + secondsElapsed);
      }

//      ImpressionEstimatorTest evaluator = new ImpressionEstimatorTest();
//      double start;
//      double stop;
//      double secondsElapsed;

//      System.out.println("\n\n\n\n\nSTARTING TEST 1");
//      start = System.currentTimeMillis();
//      evaluator.rankedImpressionEstimatorPredictionChallenge(SolverType.CP);
//      stop = System.currentTimeMillis();
//      secondsElapsed = (stop - start) / 1000.0;
//      System.out.println("SECONDS ELAPSED: " + secondsElapsed);

//      System.out.println("\n\n\n\n\nSTARTING TEST 1");
//      start = System.currentTimeMillis();
//      evaluator.allModelPredictionChallenge(SolverType.CP);
//      stop = System.currentTimeMillis();
//      secondsElapsed = (stop - start) / 1000.0;
//      System.out.println("SECONDS ELAPSED: " + secondsElapsed);

//		System.out.println("\n\n\n\n\nSTARTING TEST 2");
//		start = System.currentTimeMillis();
//		evaluator.allModelPredictionChallenge(SolverType.MIP);
//		stop = System.currentTimeMillis();
//		secondsElapsed = (stop - start)/1000.0;
//		System.out.println("SECONDS ELAPSED: " + secondsElapsed);

      //evaluator.logGameData();

      //		double[] vals = {2, 7, 3, 4};
//		System.out.println(Arrays.toString(ImpressionEstimatorTest.getIndicesForDescendingOrder(vals)));
   }

}
