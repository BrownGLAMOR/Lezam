package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.queryanalyzer.GreedyQueryAnalyzer;
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

public class QueryAnalyzerTest {

	public static final int MAX_F0_IMPS = 10969;
	public static final int MAX_F1_IMPS = 1801;
	public static final int MAX_F2_IMPS = 1423;

	public ArrayList<String> getGameStrings() {
		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
		int min = 1440;
		int max = 1441;

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
		HashMap<String,HashMap<String,Double>> ourTotRankActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotRankErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();

		HashMap<String,HashMap<String,Double>> ourTotImpErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotImpActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotImpErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotRankErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotRankActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotRankErrorCounterMap = new HashMap<String, Integer>();

			HashMap<String,Double> ourTotImpErrorMap = new HashMap<String, Double>();
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
					double ourTotRankActual = 0;
					int ourTotRankErrorCounter = 0;

					double ourTotImpError = 0;
					double ourTotImpActual = 0;
					int ourTotImpErrorCounter = 0;

					LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
					LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
					LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

					for(int i = 0; i < 57; i++) {
						QueryReport queryReport = ourQueryReports.get(i);
						SalesReport salesReport = ourSalesReports.get(i);
						BidBundle bidBundle = ourBidBundles.get(i);

						HashMap<Query,Integer> maxImps = new HashMap<Query,Integer>();
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
							maxImps.put(q, numImps);
						}

						model.updateModel(queryReport,salesReport,bidBundle,maxImps);

						for(Query q : querySpace) {
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

							HashMap<Integer,Integer> imps = new HashMap<Integer,Integer>();
							for(int agentInner = 0; agentInner < agents.length; agentInner++) {
								QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
								int imp = innerQueryReport.getImpressions(q);
								imps.put(agentInner, imp);
							}


							int[] rankPred = model.getOrderPrediction(q);
							int[] impsPred = model.getImpressionsPrediction(q);

							HashMap<Integer, Integer> rankPredMap = new HashMap<Integer,Integer>();
							for(int j = 0; j < rankPred.length; j++) {
								rankPredMap.put(rankPred[j], j);
							}

							int agentOffset = 0;
							int skipped = 0;
							for(int j = 0; j < agents.length; j++) {
								double avgPos;
								if(j == agent) {
									avgPos = allQueryReports.get(agents[j]).get(i).getPosition(q);
									agentOffset++;
								}
								else {
									avgPos = allQueryReports.get(agents[j]).get(i).getPosition(q, "adv" + (j+2-agentOffset));
								}

								if(Double.isNaN(avgPos) || avgPos < 0) {
									skipped++;
									continue;
								}

								double rankError = Math.abs(rankPredMap.get(j-skipped) - queryRanks.get(j)); //MAE
								ourTotRankActual += queryRanks.get(j);
								ourTotRankError += rankError;
								ourTotRankErrorCounter++;

								double impError = Math.abs(impsPred[j-skipped] - imps.get(j)); //MAE
								ourTotImpActual += imps.get(j);
								ourTotImpError += impError;
								ourTotImpErrorCounter++;
							}
						}
					}
					ourTotRankErrorMap.put(agents[agent],ourTotRankError);
					ourTotRankActualMap.put(agents[agent],ourTotRankActual);
					ourTotRankErrorCounterMap.put(agents[agent],ourTotRankErrorCounter);
					System.out.print("Rank Error: " + (ourTotRankError/((double)ourTotRankErrorCounter)) + ", ");

					ourTotImpErrorMap.put(agents[agent],ourTotImpError);
					ourTotImpActualMap.put(agents[agent],ourTotImpActual);
					ourTotImpErrorCounterMap.put(agents[agent],ourTotImpErrorCounter);
					System.out.println("Imp Error: " + (ourTotImpError/((double)ourTotImpErrorCounter)));
				}
			}

			ourTotRankErrorMegaMap.put(filename,ourTotRankErrorMap);
			ourTotRankActualMegaMap.put(filename,ourTotRankActualMap);
			ourTotRankErrorCounterMegaMap.put(filename,ourTotRankErrorCounterMap);

			ourTotImpErrorMegaMap.put(filename,ourTotImpErrorMap);
			ourTotImpActualMegaMap.put(filename,ourTotImpActualMap);
			ourTotImpErrorCounterMegaMap.put(filename,ourTotImpErrorCounterMap);
		}


		ArrayList<Double> rankRMSEList = new ArrayList<Double>();
		ArrayList<Double> rankActualList = new ArrayList<Double>();

		ArrayList<Double> impRMSEList = new ArrayList<Double>();
		ArrayList<Double> impActualList = new ArrayList<Double>();
		for(String file : filenames) {
			HashMap<String, Double> totRankErrorMap = ourTotRankErrorMegaMap.get(file);
			HashMap<String, Double> totRankActualMap = ourTotRankActualMegaMap.get(file);
			HashMap<String, Integer> totRankErrorCounterMap = ourTotRankErrorCounterMegaMap.get(file);

			HashMap<String, Double> totImpErrorMap = ourTotImpErrorMegaMap.get(file);
			HashMap<String, Double> totImpActualMap = ourTotImpActualMegaMap.get(file);
			HashMap<String, Integer> totImpErrorCounterMap = ourTotImpErrorCounterMegaMap.get(file);
			for(String agent : totRankErrorCounterMap.keySet()) {
				double totRankError = totRankErrorMap.get(agent);
				double totRankActual = totRankActualMap.get(agent);
				double totRankErrorCounter = totRankErrorCounterMap.get(agent);
				double rankMAE;
				rankMAE = (totRankError/totRankErrorCounter);
				double rankActual = totRankActual/totRankErrorCounter;
				rankRMSEList.add(rankMAE);
				rankActualList.add(rankActual);

				double totImpError = totImpErrorMap.get(agent);
				double totImpActual = totImpActualMap.get(agent);
				double totImpErrorCounter = totImpErrorCounterMap.get(agent);
				double impMAE;
				impMAE = (totImpError/totImpErrorCounter);
				double impActual = totImpActual/totImpErrorCounter;
				impRMSEList.add(impMAE);
				impActualList.add(impActual);
			}
		}

		double[] rankRmseStd = getStdDevAndMean(rankRMSEList);
		double[] impRmseStd = getStdDevAndMean(impRMSEList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
		System.out.println(baseModel + ", " + rankRmseStd[0] + ", " + impRmseStd[0] + ", " + elapsed);
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

		int numIters = Integer.parseInt(args[0]);
		//		int numIters = 1;
		//		evaluator.queryAnalyzerPredictionChallenge(new GreedyQueryAnalyzer(querySpace, advertisers, "this will be overwritten"));
		evaluator.queryAnalyzerPredictionChallenge(new CarletonQueryAnalyzer(querySpace, advertisers, "this will be overwritten", numIters));


		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		//		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

}