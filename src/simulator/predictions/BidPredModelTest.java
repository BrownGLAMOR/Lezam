package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import models.bidmodel.JointDistBidModel;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.UserClickModel;

public class BidPredModelTest {

	public ArrayList<String> getGameStrings() {
//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game"; //jberg HOME FILES
				String baseFile = "/pro/aa/finals/day-2/server-1/game"; //CS DEPT Files
		//games 1425-1464
		int min = 1440;
		int max = 1441;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void bidPredictionChallenge(AbstractBidModel baseModel) throws IOException, ParseException {
		double start = System.currentTimeMillis();

		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

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
			HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

			for(int agent = 0; agent<agents.length; agent++) {
				AbstractBidModel model = (AbstractBidModel) baseModel.getCopy();
				model.setAdvertiser(agents[agent]); 
				//System.out.println("Testing for agent: " + agents[agent]);

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);

				for(int i = 0; i < 57; i++) {
					QueryReport queryReport = ourQueryReports.get(i);
					//System.out.print("Day " +(i+1) + " -- ");

					HashMap<Query, HashMap<String, Integer>> ranks = new HashMap<Query,HashMap<String,Integer>>();
					HashMap<Query, Double> cpc = new HashMap<Query,Double>();
					HashMap<Query, Double> ourBid = new HashMap<Query,Double>();
					for(Query q : querySpace) {
						cpc.put(q, queryReport.getCPC(q));

						ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
						for(int agentInner = 0; agentInner < agents.length; agentInner++) {
							BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
							double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agentInner);
							double bid = innerBidBundle.getBid(q);
							double squashedBid = bid * Math.pow(advEffect, squashing);
							bidPairs.add(new BidPair(agentInner, squashedBid));
							if(agentInner == agent) {
								ourBid.put(q, squashedBid);
							}
						}

						Collections.sort(bidPairs);

						HashMap<String, Integer> queryRanks = new HashMap<String,Integer>();
						for(int j = 0; j < bidPairs.size(); j++) {
							queryRanks.put(agents[bidPairs.get(j).getID()], j);
						}

						ranks.put(q, queryRanks);
					}

					model.updateModel(cpc,ourBid,ranks);

					for(Query q : querySpace) {
						//System.out.print("Query: "+q.getComponent()+", "+q.getManufacturer()+" -- ");
						for(int j = 0; j < agents.length; j++) {
							/*
							 * You guys don't really need to worry about predicting, because
							 * that is not really the point of this particle filter.
							 */
							//							double bid = allBidBundles.get(agents[j]).get(i+2).getBid(q);

							double bid = allBidBundles.get(agents[j]).get(i).getBid(q);
							double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), j);
							double squashedBid = bid * Math.pow(advEffect, squashing);

							double bidPred = model.getPrediction(agents[j],q);
							//if(agents[j].equals("munsey")&&q.getType()==QueryType.FOCUS_LEVEL_ZERO);
							//System.out.print("  \tAgent: " +agents[j]+" Act:"+(int)(squashedBid*100)+" <-> Pred: " + (int)(bidPred*100) +" -- ");
							double error = squashedBid - bidPred;

							error = error*error;
							ourTotActual += squashedBid;
							ourTotError += error;
							ourTotErrorCounter++;
						}
					}
					//System.out.println();
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);

		double[] rmseStd = getStdDevAndMean(RMSEList);
		//		double[] actualStd = getStdDevAndMean(actualList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
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

		public int compareTo(BidPair agentBidPair) {
			double ourBid = this._bid;
			double otherBid = agentBidPair.getBid();
			if(ourBid < otherBid) {
				return 1;
			}
			if(otherBid < ourBid) {
				return -1;
			}
			else {
				return 0;
			}
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
		BidPredModelTest evaluator = new BidPredModelTest();

		ArrayList<String> filenames = evaluator.getGameStrings();
		String filename = filenames.get(0);
		GameStatusHandler statusHandler = new GameStatusHandler(filename);
		GameStatus status = statusHandler.getGameStatus();
		String[] agents = status.getAdvertisers();

		LinkedHashSet<String> advertisers = new LinkedHashSet<String>();

		for(int i = 0; i < agents.length; i++) {
			advertisers.add(agents[i]);
		}

		System.out.println(advertisers);

		double start = System.currentTimeMillis();
		//		evaluator.bidPredictionChallenge(new IndependentBidModel(advertisers,  agents[0]));
		evaluator.bidPredictionChallenge(new JointDistBidModel(advertisers, agents[0]));

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

}
