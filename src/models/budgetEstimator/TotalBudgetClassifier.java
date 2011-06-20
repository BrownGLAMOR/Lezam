package models.budgetEstimator;

import edu.umich.eecs.tac.props.*;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class TotalBudgetClassifier {

	public static final int NUMQUERIES = 16;

	public ArrayList<String> getGameStrings() {
		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
		int min = 1425;
		int max = 1465;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void generateTotalBudgetDataSet() throws IOException, ParseException, InstantiationException, IllegalAccessException {
		ArrayList<String> filenames = getGameStrings();
		int numSlots = 5;
		int numAdvertisers = 8;

		int[] queryStats = new int[17];
		int[] budgetTypeStats = new int[BudgetType.values().length];

		String output = "";
		output += "%" + "\n";
		output += "%" + "\n";
		output += "@RELATION BudgetData" + "\n";
		output += "" + "\n";
		output += "@ATTRIBUTE mean NUMERIC" + "\n";
		output += "@ATTRIBUTE stddev NUMERIC" + "\n";
		for(int i = 0; i < NUMQUERIES; i++) {
			output += "@ATTRIBUTE c" + i + " NUMERIC" + "\n";
		}
		output += "@ATTRIBUTE budgetType {NONE, PERQUERY, TOTAL}" + "\n";
		output += "" + "\n";
		output += "@DATA" + "\n";

		System.out.println(output);

		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

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

			for(int i = 0; i < 57; i++) {
				HashMap<String,Boolean> totalBudgetSet = new HashMap<String,Boolean>();
				HashMap<String,Boolean> totalBudgetHit = new HashMap<String,Boolean>();

				HashMap<Query,HashMap<String,Boolean>> perQueryBudgetSet = new HashMap<Query,HashMap<String,Boolean>>();
				HashMap<Query,HashMap<String,Boolean>> perQueryBudgetHit = new HashMap<Query,HashMap<String,Boolean>>();
				for(Query q : querySpace) {
					perQueryBudgetSet.put(q, new HashMap<String, Boolean>());
					perQueryBudgetHit.put(q, new HashMap<String, Boolean>());
				}

				for(int agent = 0; agent < agents.length; agent++) {
					BidBundle bidBundle = allBidBundles.get(agents[agent]).get(i);
					QueryReport queryReport = allQueryReports.get(agents[agent]).get(i);

					double totalCost = 0.0;
					double minBid = 0;
					//					double minBid = Double.MAX_VALUE;
					for(Query q : querySpace) {
						double cost = queryReport.getCost(q);
						double budget = bidBundle.getDailyLimit(q);
						double bid = bidBundle.getBid(q);

						/*
						 * get the lowest bid of auctions we were in
						 */
						//						if(!Double.isNaN(bid) && cost > 0 && bid < minBid) {
						if(!Double.isNaN(bid) && cost > 0 && bid > minBid) {
							minBid = bid;
						}

						totalCost += cost;

						HashMap<String, Boolean> budgetHits = perQueryBudgetHit.get(q);
						HashMap<String, Boolean> budgetSets = perQueryBudgetSet.get(q);

						if(!Double.isNaN(budget) && budget < cost + bid) {
							budgetHits.put(agents[agent], true);
						}
						else {
							budgetHits.put(agents[agent], false);
						}

						if(Double.isNaN(budget) || Double.isInfinite(budget)) {
							budgetSets.put(agents[agent], false);
						}
						else {
							budgetSets.put(agents[agent], true);
						}

						perQueryBudgetHit.put(q, budgetHits);
						perQueryBudgetSet.put(q, budgetSets);
					}

					double totalBudget = bidBundle.getCampaignDailySpendLimit();

					if(!Double.isNaN(totalBudget) && totalBudget < totalCost + minBid) {
						totalBudgetHit.put(agents[agent], true);
					}
					else {
						totalBudgetHit.put(agents[agent], false);
					}

					if(Double.isNaN(totalBudget) || Double.isInfinite(totalBudget)) {
						totalBudgetSet.put(agents[agent], false);
					}
					else {
						totalBudgetSet.put(agents[agent], true);
					}
				}

				HashMap<Query,HashMap<String,Double>> agentDropOuts = new HashMap<Query,HashMap<String,Double>>();

				for(Query q : querySpace) {
					int[] order = new int[numAdvertisers];
					int[] orderReverseLookup = new int[numAdvertisers];
					int[] impressions = new int[numAdvertisers];
					ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
					for(int agent = 0; agent < agents.length; agent++) {
						QueryReport innerQueryReport = allQueryReports.get(agents[agent]).get(i);
						impressions[agent] = innerQueryReport.getImpressions(q);

						BidBundle innerBidBundle = allBidBundles.get(agents[agent]).get(i);
						double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agent);
						double bid = innerBidBundle.getBid(q);
						double squashedBid = bid * Math.pow(advEffect, squashing);
						bidPairs.add(new BidPair(agent, squashedBid));
					}

					Collections.sort(bidPairs);

					for(int j = 0; j < bidPairs.size(); j++) {
						order[j] = bidPairs.get(j).getID();
						orderReverseLookup[order[j]] = j;
					}

					ImpressionData data = greedyAssign(numSlots, numAdvertisers, order, impressions);
					int[][] greedyAssign = data.getAssign();
					int maxImpressions = data.getMaxImpressions();

					HashMap<String, Double> queryDropOuts = new HashMap<String,Double>();
					for(int agent = 0; agent < agents.length; agent++) {
						int minSlot;
						for(minSlot = 0; minSlot < numSlots; minSlot++) {
							if(greedyAssign[agent][minSlot] > 0) {
								break;
							}
						}

						double imps = 0.0;
						if(minSlot < numSlots) {
							for(int j = 0; j <= orderReverseLookup[agent]; j++) {
								imps += greedyAssign[order[orderReverseLookup[agent] - j]][minSlot];
							}
						}

						queryDropOuts.put(agents[agent], imps / ((double) maxImpressions));
					}
					agentDropOuts.put(q, queryDropOuts);
				}

				HashMap<Query,Double> dropoutMean = new HashMap<Query,Double>();
				HashMap<Query,Double> dropoutStdDev = new HashMap<Query,Double>();

				for(Query q : querySpace) {
					HashMap<String, Double> queryDropouts = agentDropOuts.get(q);

					ArrayList<Double> dropouts = new ArrayList<Double>();
					for(int agent = 0; agent < agents.length; agent++) {
						double dropout = queryDropouts.get(agents[agent]);
						if(dropout > 0) {
							dropouts.add(dropout);
						}
					}

					double[] stdDev = stdDeviation(dropouts);

					dropoutMean.put(q, stdDev[0]);
					dropoutStdDev.put(q, stdDev[1]);
				}

				for(int agent = 0; agent < agents.length; agent++) {
					int numQueriesIn = 0;
					for(Query q : querySpace) {
						double dropout = agentDropOuts.get(q).get(agents[agent]);
						if(dropout > 0) {
							numQueriesIn++;
						}
					}
					queryStats[numQueriesIn] += 1;

					boolean tBudgetSet = totalBudgetSet.get(agents[agent]);
					boolean tBudgetHit = totalBudgetHit.get(agents[agent]);

					if(numQueriesIn == NUMQUERIES) {
						for(Query q1 : querySpace) {
							double dropout1 = agentDropOuts.get(q1).get(agents[agent]);
							if(dropout1 > 0 && dropout1 != 1) { // if dropout = 1 then we know they didn't hit they're budget
								String outputStr = dropoutMean.get(q1) + ", " + 
								dropoutStdDev.get(q1) + ", " + 
								dropout1 + ", ";
								/*
								 * Get relative dropouts
								 */
								for(Query q2 : querySpace) {
									if(q1 != q2) {
										double dropout2 = agentDropOuts.get(q2).get(agents[agent]);
										if(dropout2 > 0) {
											outputStr += (dropout1 - dropout2) + ", ";
										}
									}
								}

								BudgetType budgetType = BudgetType.NONE;

								boolean qBudgetSet = perQueryBudgetSet.get(q1).get(agents[agent]);
								boolean qBudgetHit = perQueryBudgetHit.get(q1).get(agents[agent]);

								if(qBudgetSet) {
									if(qBudgetHit) {
										budgetType = BudgetType.PERQUERY;
									}
								}

								if(budgetType != BudgetType.PERQUERY && tBudgetSet) {
									if(tBudgetHit) {
										budgetType = BudgetType.TOTAL;
									}
								}

								budgetTypeStats[budgetType.ordinal()] += 1;

								if(budgetType != BudgetType.NONE) {
									System.out.println(outputStr + budgetType);
								}
							}
						}
					}
				}
			}
		}

		//		for (int j = 0; j < queryStats.length; j++) {
		//			System.out.println("in " + j + " auctions: " + queryStats[j]);
		//		}
		//
		//		for (int j = 0; j < budgetTypeStats.length; j++) {
		//			System.out.println("# of " + BudgetType.values()[j] + " budget hits: " + budgetTypeStats[j]);
		//		}
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
	public ImpressionData greedyAssign(int slots, int agents, int[] order, int[] impressions){
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
				}else{
					int r = slotStart[s-1] - slotStart[s];
					//System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
					assert(r >= 0);
					if(r < remainingImp){
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
		ImpressionData data = new ImpressionData(impressionsBySlot,slotStart[0]);
		return data;
	}

	public final class ImpressionData {
		public final int[][] _assign;
		public final int _maxImpressions;

		public ImpressionData(int[][] greedyAssign, int maxImpressions) {
			_assign = greedyAssign;
			_maxImpressions = maxImpressions;
		}

		public int[][] getAssign() {
			return _assign;
		}

		public int getMaxImpressions() {
			return _maxImpressions;
		}
	}

	public enum BudgetType { NONE, PERQUERY, TOTAL};

	/*
	 * Returns the means and std deviation
	 * index 0 is the mean
	 * index 1 is the std deviation
	 */
	public static double[] stdDeviation(ArrayList<Double> revenueErrorArr) {
		double[] meanAndStdDev = new double[2];
		meanAndStdDev[0] = 0.0;
		meanAndStdDev[1] = 0.0;
		for(int i = 0; i < revenueErrorArr.size(); i++) {
			meanAndStdDev[0] += revenueErrorArr.get(i);
		}
		meanAndStdDev[0] /= revenueErrorArr.size();
		for(int i = 0; i < revenueErrorArr.size(); i++) {
			meanAndStdDev[1] +=  (revenueErrorArr.get(i) - meanAndStdDev[0])*(revenueErrorArr.get(i) - meanAndStdDev[0]);
		}
		meanAndStdDev[1] /= revenueErrorArr.size();
		meanAndStdDev[1] = Math.sqrt(meanAndStdDev[1]);
		return meanAndStdDev;
	}

	public static void main(String[] args) throws IOException, ParseException, InstantiationException, IllegalAccessException  {
		TotalBudgetClassifier classifier = new TotalBudgetClassifier();
		classifier.generateTotalBudgetDataSet();
	}

}
