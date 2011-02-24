package simulator.predictions;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.apache.commons.math.stat.descriptive.AggregateSummaryStatistics;

import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.queryanalyzer.GreedyQueryAnalyzer;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.greg.GQA;
import models.queryanalyzer.iep.AbstractImpressionEstimator;
import models.queryanalyzer.iep.EricImpressionEstimator;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimator;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import simulator.predictions.BidPredModelTest.BidPair;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.UserClickModel;

public class ImpressionEstimatorTest {

	BufferedWriter bufferedWriter = null;


	public static final int MAX_F0_IMPS = 10969;
	public static final int MAX_F1_IMPS = 1801;
	public static final int MAX_F2_IMPS = 1423;
	public static boolean PERFECT_IMPS = true;
	public static int LDS_ITERATIONS_1 = 10;
	public static int LDS_ITERATIONS_2 = 10;
	private static boolean REPORT_FULLPOS_FORSELF = true;


	//Performance metrics
	int numInstances = 0;
	double aggregateAbsError = 0;

	public ArrayList<String> getGameStrings() {
		String baseFile = "./game"; //games 1425-1464
		int min = 1;
		int max = 1;//5;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i <= max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}


	/**
	 * Debugging method used to print out whatever data I want from the game logs
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public void printGameLogInfo() throws IOException, ParseException {
		ArrayList<String> filenames = getGameStrings();
		for (int gameIdx=0; gameIdx<filenames.size(); gameIdx++) {
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
			for(Product product : status.getRetailCatalog()) {
				querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
				querySpace.add(new Query(null, product.getComponent())); // F1 Component only
				querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
			}
			Query[] queryArr = new Query[querySpace.size()];
			querySpace.toArray(queryArr);
			int numQueries = queryArr.length;

			// Make predictions for each day/query in this game
			int numReports = 57;//57; //TODO: Why?
			for (int d=0; d<numReports; d++) {
				for (int queryIdx=0; queryIdx<numQueries; queryIdx++) {
					Query query = queryArr[queryIdx];
					Integer[] imps = getAgentImpressions(status, d, query);
					Integer[] promotedImps = getAgentPromotedImpressions(status, d, query);
					System.out.println("promotedImps=" + Arrays.toString(promotedImps) + "\timps="+Arrays.toString(imps));
				}
			}
		}
		throw new RuntimeException("Finished printing game log. Aborting.");
	}
	
	
	
	/**
	 * Load a game
	 * For each day,
	 * Get all query reports that came in on that day
	 * From these, infer: numSlots, numAgents, avgPos[], agentIds[], ourAgentIdx, ourImpressions, impressionsUB
	 * also infer squashed bid ordering.
	 * Input these into the given Impression Estimator (via a QA instance)
	 * Compare output impsPer
	 * @param del
	 * @throws IOException
	 * @throws ParseException
	 */
	public void impressionEstimatorPredictionChallenge(int impressionEstimatorIdx) throws IOException, ParseException {
		//printGameLogInfo();
		
		initializeLog("iePred" + impressionEstimatorIdx + ".txt");
		numInstances = 0;
		aggregateAbsError = 0;		
		ArrayList<String> filenames = getGameStrings();

//		for (int gameIdx=3; gameIdx<=3; gameIdx++) {
		for (int gameIdx=0; gameIdx<filenames.size(); gameIdx++) {
			String filename = filenames.get(gameIdx);

			// Load this game and its basic parameters
			GameStatus status = new GameStatusHandler(filename).getGameStatus();
			int NUM_PROMOTED_SLOTS = status.getSlotInfo().getPromotedSlots();
			HashMap<QueryType, Double> promotedReserveScore = getApproximatePromotedReserveScore(status);
			
			
			// Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null)); //F0
			for(Product product : status.getRetailCatalog()) {
				querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
				querySpace.add(new Query(null, product.getComponent())); // F1 Component only
				querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
			}

			Query[] queryArr = new Query[querySpace.size()];
			querySpace.toArray(queryArr);
			int numQueries = queryArr.length;
			
			// Make predictions for each day/query in this game
			int numReports = 15;//57; //TODO: Why?
//			for (int d=31; d<=31; d++) {
			for (int d=0; d<numReports; d++) {
				
				System.out.println("Game " + (gameIdx+1) + "/" + filenames.size() + ", Day " + d + "/" + numReports);
				
//				for (int queryIdx=5; queryIdx<=5; queryIdx++) {
				for (int queryIdx=0; queryIdx<numQueries; queryIdx++) {
					Query query = queryArr[queryIdx];

					// Get avg position for each agent
					Double[] actualAveragePositions = getAveragePositions(status, d, query);

					// Get squashed bids for each agent
					Double[] squashedBids = getSquashedBids(status, d, query);

					Double[] budgets = getBudgets(status, d, query);

					// Get total number of impressions for each agent
					Integer[] impressions = getAgentImpressions(status, d, query);

					Integer[] promotedImpressions = getAgentPromotedImpressions(status, d, query);
					
					Boolean[] promotionEligibility = getAgentPromotionEligibility(status, d, query, promotedReserveScore.get(query.getType()));
					
					// DEBUG: Print out some game values.
					System.out.println("d="+d + "\tq=" + query + "\treserve=" + status.getReserveInfo().getRegularReserve() + "\tpromoted=" + status.getReserveInfo().getPromotedReserve() + "\t" + status.getSlotInfo().getPromotedSlots() + "/" + status.getSlotInfo().getRegularSlots());
					System.out.println("d="+d + "\tq=" + query + "\tagents=" + Arrays.toString(status.getAdvertisers()));
					System.out.println("d="+d + "\tq=" + query + "\taveragePos=" + Arrays.toString(actualAveragePositions));
					System.out.println("d="+d + "\tq=" + query + "\tsquashedBids=" + Arrays.toString(squashedBids));
					System.out.println("d="+d + "\tq=" + query + "\tbudgets=" + Arrays.toString(budgets));
					System.out.println("d="+d + "\tq=" + query + "\timpressions=" + Arrays.toString(impressions));
					System.out.println("d="+d + "\tq=" + query + "\tpromotedImpressions=" + Arrays.toString(promotedImpressions));
					System.out.println("d="+d + "\tq=" + query + "\tpromotionEligibility=" + Arrays.toString(promotionEligibility));
					System.out.println();

					// Determine how many agents actually participated
					int numParticipants = 0;
					for (int a=0; a<actualAveragePositions.length; a++) {
						//if (!actualAveragePositions[a].isNaN()) numParticipants++;
						if (! Double.isNaN( actualAveragePositions[a] ) ) numParticipants++;
					}

					// Reduce to only auction participants
					double[] reducedAvgPos = new double[numParticipants];
					double[] reducedBids = new double[numParticipants];
					int[] reducedImps = new int[numParticipants];
					int[] reducedPromotedImps = new int[numParticipants];
					boolean[] reducedPromotionEligibility = new boolean[numParticipants];
					int rIdx = 0;
					for (int a=0; a<actualAveragePositions.length; a++) {
						if (!actualAveragePositions[a].isNaN()) {
							reducedAvgPos[rIdx] = actualAveragePositions[a];
							reducedBids[rIdx] = squashedBids[a];
							reducedImps[rIdx] = impressions[a];
							reducedPromotedImps[rIdx] = promotedImpressions[a];
							reducedPromotionEligibility[rIdx] = promotionEligibility[a];
							rIdx++;
						}
					}

					// Get ordering of remaining squashed bids
					int[] ordering = getIndicesForDescendingOrder(reducedBids);

					// Some params needed for the QA instance
					// TODO: Have some of these configurable.
					int[] agentIds = new int[numParticipants]; //Just have them all be 0. TODO: Is carleton using this?
					int NUM_SLOTS = 5;
					int impressionsUB = 11000;


					// For each agent, make a prediction (each agent sees a different num impressions)
//					for (int ourAgentIdx=2; ourAgentIdx<=2; ourAgentIdx++) {
					for (int ourAgentIdx=0; ourAgentIdx<numParticipants; ourAgentIdx++) {
						double start = System.currentTimeMillis(); //time the prediction time on this instance
						
						int ourImps = reducedImps[ourAgentIdx];
						int ourPromotedImps = reducedPromotedImps[ourAgentIdx];
						boolean ourPromotionEligibility = reducedPromotionEligibility[ourAgentIdx];
						
						//DEBUG TEMP FIXME: Just want to see how much promotion constraint helps.
						//if (ourPromotedImps <= 0) continue;
						//ourPromotionEligibility = false; //FIXME just temporary
						//if (ourPromotionEligibility == false) continue; 
						
						QAInstance inst = new QAInstance(NUM_SLOTS, NUM_PROMOTED_SLOTS, numParticipants, reducedAvgPos, agentIds, ourAgentIdx, ourImps, ourPromotedImps, impressionsUB, false, ourPromotionEligibility);

						//FIXME: we should be able to choose more elegantly at runtime what class we're going to load.
						//This is annoying... ImpressionEstimator requires a QAInstance in the constructor,
						//but this QAInstance isn't ready until now. 
						//Terrible, band-aid solution is to have an integer corresponding to each test.
						AbstractImpressionEstimator model = null;
						String modelName = null;
						if(impressionEstimatorIdx == 1) model = new ImpressionEstimator(inst);
						if(impressionEstimatorIdx == 2) model = new EricImpressionEstimator(inst);

						IEResult result = model.search(ordering);

						//Get predictions (also provide dummy values for failure)
						int[] predictedImpsPerAgent;
						if (result != null) predictedImpsPerAgent = result.getSol();
						else {
							predictedImpsPerAgent = new int[reducedImps.length];
							Arrays.fill(predictedImpsPerAgent, -1);
						}

						double stop = System.currentTimeMillis();
						double secondsElapsed = (stop - start)/1000.0;

						//System.out.println("predicted: " + Arrays.toString(predictedImpsPerAgent));
						//System.out.println("actual: " + Arrays.toString(reducedImps));

						//Update performance metrics
						updatePerformanceMetrics(predictedImpsPerAgent, reducedImps);
						//outputPerformanceMetrics();


						//LOGGING
						double[] err = new double[predictedImpsPerAgent.length];
						for (int a=0; a<predictedImpsPerAgent.length; a++) {
							err[a] = Math.abs(predictedImpsPerAgent[a] - reducedImps[a]);
						}
						StringBuffer sb = new StringBuffer();
						sb.append("err="+Arrays.toString(err) + "\t");
						sb.append("pred="+Arrays.toString(predictedImpsPerAgent) + "\t");
						sb.append("actual="+Arrays.toString(reducedImps) + "\t");
						sb.append("g=" + gameIdx + " ");
						sb.append("d="+d + " a=" + ourAgentIdx + " q=" + query + " avgPos=" + Arrays.toString(reducedAvgPos) + " ");
						sb.append("bids=" + Arrays.toString(reducedBids) + " ");
						sb.append("imps=" + Arrays.toString(reducedImps) + " ");
						sb.append("order=" + Arrays.toString(ordering) + " ");
						sb.append( ((impressionEstimatorIdx==1) ? "CJC" : "IP") ) ;
						System.out.println(sb);

						
						//Save all relevant data to file
						sb = new StringBuffer();
						for (int predictingAgentIdx=0; predictingAgentIdx<numParticipants; predictingAgentIdx++) {
							int ourBidRank = -1;
							int oppBidRank = -1;
							for (int i=0; i<ordering.length; i++) {
								if (ordering[i] == ourAgentIdx) ourBidRank = i;
								if (ordering[i] == predictingAgentIdx) oppBidRank = i;
							}
														
							
							sb.append(model.getName()+",");
							sb.append(gameIdx + ",");
							sb.append(d + ",");
							sb.append(queryIdx + ",");
							sb.append(ourBidRank + ","); 
							sb.append(oppBidRank + ",");  
							sb.append(reducedAvgPos[ourAgentIdx] + ","); //our avgPos
							sb.append(reducedAvgPos[predictingAgentIdx] + ","); //opponent avgPos            
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

		outputPerformanceMetrics();
		closeLog();
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
			sb.append("our.bid.rank,");
			sb.append("opp.bid.rank,");
			sb.append("our.avg.pos,");
			sb.append("opp.avg.pos,");        
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
		try{
			bufferedWriter.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}






	/**
	 * 
	 * @param predictedImpsPerAgent
	 * @param actualImpsPerAgent
	 */
	private void updatePerformanceMetrics(int[] predictedImpsPerAgent, int[] actualImpsPerAgent) {
		assert(predictedImpsPerAgent.length == actualImpsPerAgent.length);

		for (int a=0; a<predictedImpsPerAgent.length; a++) {
			numInstances++;
			aggregateAbsError += Math.abs(predictedImpsPerAgent[a] - actualImpsPerAgent[a]);
		}
	}

	private void outputPerformanceMetrics() {
		double meanAbsError = aggregateAbsError / numInstances;
		System.out.println("Mean absolute error: " + meanAbsError);
	}


	/**
	 * Get an array of average positions (one element for each agent), for the given day/query 
	 * @param allQueryReports
	 * @param d
	 * @param query
	 * @return
	 */
	private Double[] getAveragePositions(GameStatus status, int d, Query query) {
		String[] agents = status.getAdvertisers();
		Double[] averagePositions = new Double[agents.length];
		for (int a=0; a<agents.length; a++) {
			String agentName = agents[a];
			try {
				averagePositions[a] = status.getQueryReports().get(agentName).get(d).getPosition(query);
			} catch(Exception e) {
				//May get here if the agent doesn't have a query report (does this ever happen?)
				averagePositions[a] = Double.NaN;
				throw new RuntimeException("ahhhhhhhhhhhhhhhhhhhhhhhh");
			}	
		}
		return averagePositions;
	}


	private Double[] getSquashedBids(GameStatus status, int d, Query query) {
		String[] agents = status.getAdvertisers();
		Double[] squashedBids = new Double[agents.length];
		UserClickModel userClickModel = status.getUserClickModel();
		double squashing = status.getPubInfo().getSquashingParameter();
		for (int a=0; a<agents.length; a++) {
			String agentName = agents[a];
			try{
				double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(query), a);
				double bid = status.getBidBundles().get(agentName).get(d).getBid(query);
				squashedBids[a] = bid * Math.pow(advEffect, squashing);
			} catch(Exception e) {
				squashedBids[a] = Double.NaN;
			}
		}
		return squashedBids;		
	}


	private Double[] getBudgets(GameStatus status, int d, Query query) {
		String[] agents = status.getAdvertisers();
		Double[] budgets = new Double[agents.length];
		for (int a=0; a<agents.length; a++) {
			String agentName = agents[a];
			try{
				budgets[a] = status.getBidBundles().get(agentName).get(d).getDailyLimit(query);
			} catch(Exception e) {
				budgets[a] = Double.NaN;
			}
		}
		return budgets;		
	}

	private Integer[] getAgentImpressions(GameStatus status, int d, Query query) {
		String[] agents = status.getAdvertisers();
		Integer[] agentImpressions= new Integer[agents.length];
		for (int a=0; a<agents.length; a++) {
			String agentName = agents[a];
			try {
				agentImpressions[a] = status.getQueryReports().get(agentName).get(d).getImpressions(query);
			} catch(Exception e) {
				//May get here if the agent doesn't have a query report (does this ever happen?)
				agentImpressions[a] = null;
			}	
		}
		return agentImpressions;
	}

	private Integer[] getAgentPromotedImpressions(GameStatus status, int d, Query query) {
		String[] agents = status.getAdvertisers();
		Integer[] agentImpressions= new Integer[agents.length];
		for (int a=0; a<agents.length; a++) {
			String agentName = agents[a];
			try {
				agentImpressions[a] = status.getQueryReports().get(agentName).get(d).getPromotedImpressions(query);
			} catch(Exception e) {
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
		for (int a=0; a<squashedBids.length; a++) {
			try {
				//TODO: Squashed bid must be > or >= promoted reserve score?
				if(squashedBids[a] >= promotedReserveScore) promotionEligibility[a] = true;
				else promotionEligibility[a] = false;
			} catch(Exception e) {
				//May get here if the agent doesn't have a query report (does this ever happen?)
				promotionEligibility[a] = false;
			}	
		}
		return promotionEligibility;
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
		for(Product product : status.getRetailCatalog()) {
			querySpace.add(new Query(product.getManufacturer(), null)); // F1 Manufacturer only
			querySpace.add(new Query(null, product.getComponent())); // F1 Component only
			querySpace.add(new Query(product.getManufacturer(), product.getComponent())); // The F2 query class
		}
		int numDays = 57; //TODO: Don't hardcode this.
		for (int d=0; d<numDays; d++) {
			for (Query q : querySpace) {
				Integer[] promotedImps = getAgentPromotedImpressions(status, d, q);
				Double[] squashedBids = getSquashedBids(status, d, q);
				
				for (int a=0; a<promotedImps.length; a++) {
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
		for (int i=0; i<length; i++) ids[i] = i;

		for(int i=0; i < length; i++){
			for(int j=i+1; j < length; j++){
				if(vals[i] < vals[j]){
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
		for(Double data : list) {
			sum += data;
		}
		double mean = sum/n;

		double variance = 0.0;

		for(Double data : list) {
			variance += (data-mean)*(data-mean);
		}

		variance /= (n-1);

		double[] stdDev = new double[2];
		stdDev[0] = mean;
		stdDev[1] = Math.sqrt(variance);
		return stdDev;
	}


	public static void main(String[] args) throws IOException, ParseException  {
		ImpressionEstimatorTest evaluator = new ImpressionEstimatorTest();
		System.out.println("\n\n\n\n\nSTARTING TEST 1");
		evaluator.impressionEstimatorPredictionChallenge(1);
		System.out.println("\n\n\n\n\nSTARTING TEST 2");
		evaluator.impressionEstimatorPredictionChallenge(2);
//		double[] vals = {2, 7, 3, 4};  
//		System.out.println(Arrays.toString(ImpressionEstimatorTest.getIndicesForDescendingOrder(vals)));
	}

}
