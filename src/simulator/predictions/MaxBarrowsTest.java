package simulator.predictions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import models.mbarrows.AbstractMaxBarrows;
import models.mbarrows.MBarrowsImpl;
import models.usermodel.TacTexAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.UserClickModel;

public class MaxBarrowsTest {

	public ArrayList<String> getGameStrings() {
		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
		int min = 1440;
		int max = 1441;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void modelParamPredictionChallenge(AbstractMaxBarrows baseModel) throws IOException, ParseException {
		ArrayList<String> filenames = getGameStrings();
		int numSlots = 5;
		int numAdvertisers = 8;
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			UserClickModel userClickModel = status.getUserClickModel();

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

			int numPromSlots = status.getSlotInfo().getPromotedSlots();

			HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
			HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
			HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();
			LinkedList<HashMap<Product, HashMap<UserState, Integer>>> allUserDists = status.getUserDistributions();

			for(int agent = 0; agent < agents.length; agent++) {
				Class<? extends AbstractMaxBarrows> c = baseModel.getClass();

				AbstractMaxBarrows model = null;

				try {
					model = (AbstractMaxBarrows)(c.getConstructors()[0].newInstance(querySpace,agents[agent],numPromSlots));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);

				for(int i = 0; i < 57; i++) {

					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);

					HashMap<String,HashMap<Query,Ad>> allAds = new HashMap<String, HashMap<Query,Ad>>();
					for(int agentInner = 0; agentInner < agents.length; agentInner++) {
						BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
						HashMap<Query, Ad> advAds = new HashMap<Query, Ad>();
						for(Query q : querySpace) {
							advAds.put(q, innerBidBundle.getAd(q));
						}
						allAds.put(agents[agentInner],advAds);
					}

					HashMap<Query,LinkedList<LinkedList<String>>> allAdvertisersAbovePerSlot = new HashMap<Query,LinkedList<LinkedList<String>>>();
					HashMap<Query,LinkedList<Integer>> allImpressionsPerSlot = new HashMap<Query,LinkedList<Integer>>();
					for(Query q : querySpace) {
						int[] order = new int[numAdvertisers];
						int[] impressions = new int[numAdvertisers];
						ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
						for(int agentInner = 0; agentInner < agents.length; agentInner++) {
							QueryReport innerQueryReport = allQueryReports.get(agents[agentInner]).get(i);
							impressions[agentInner] = innerQueryReport.getImpressions(q);

							BidBundle innerBidBundle = allBidBundles.get(agents[agentInner]).get(i);
							bidPairs.add(new BidPair(agentInner, innerBidBundle.getBid(q)));
						}

						Collections.sort(bidPairs);

						for(int j = 0; j < bidPairs.size(); j++) {
							order[j] = bidPairs.get(j).getID();
						}

						LinkedList<LinkedList<String>> advertisersAbovePerSlot = new LinkedList<LinkedList<String>>();
						LinkedList<Integer> impressionsPerSlot = new LinkedList<Integer>();
						int[][] impressionMatrix = greedyAssign(numSlots, numAdvertisers, order, impressions);

						for(int j = 0; j < numSlots; j++) {
							int numImpressions = impressionMatrix[agent][j];
							impressionsPerSlot.add(numImpressions);

							LinkedList<String> advsAbove = new LinkedList<String>();
							if(!(numImpressions == 0 || numSlots == 0)) {
								for(int k = 0; k < numSlots; k++) {
									advsAbove.add(agents[bidPairs.get(k).getID()]);
								}
							}

							advertisersAbovePerSlot.add(advsAbove);
						}

						allAdvertisersAbovePerSlot.put(q, advertisersAbovePerSlot);
						allImpressionsPerSlot.put(q, impressionsPerSlot);
					}

					HashMap<Product,HashMap<UserState,Integer>> userStates = allUserDists.get(i);

					model.updateModel(queryReport, salesReport, allImpressionsPerSlot, allAdvertisersAbovePerSlot, allAds, userStates);
					for(Query q : querySpace) {
						double contProb = userClickModel.getContinuationProbability(userClickModel.queryIndex(q));
						double advEffect = userClickModel.getAdvertiserEffect(userClickModel.queryIndex(q), agent);

						double[] preds = model.getPrediction(q);

						/*
						 * TODO
						 * 
						 * This is probably where you want to add code to 
						 * do something with your predictions and the ground
						 * truth
						 */
					}
				}
			}
		}
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


	public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions){
		int[][] impressionsBySlot = new int[agents][slots];

		int[] slotStart= new int[slots];
		int a;

		int[] permOrder= new int[order.length];
		for(int i = 0; i < order.length; ++i){
			//System.out.println("Order["+i+"] = " + order[i]);
			permOrder[order[i]] = i;
			//System.out.println("permOrder["+order[i]+"] = " + i);
		}


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
		return impressionsBySlot;
	}

	public static void main(String[] args) throws IOException, ParseException  {
		MaxBarrowsTest evaluator = new MaxBarrowsTest();

		double start = System.currentTimeMillis();
		Set<Query> querySpace = new LinkedHashSet<Query>();
		evaluator.modelParamPredictionChallenge(new MBarrowsImpl(querySpace,"this will be set later",0));

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

}
