package models.paramest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import simulator.predictions.ParameterEstimationTest.ImprPair;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BayesianParameterEstimation extends AbstractParameterEstimation {
	
	private ArrayList<Query> m_queries;
	private HashMap<Query, BayesianQueryHandler> m_queryHandlers;
	
	private int numSlots = 5;
	private int numAdvertisers = 8;
	
	double _probClick;
	
	double[] _c;
	
	public BayesianParameterEstimation() {
		this(new double[] {0.126114132,0.153193911,0.246344682});
	}
	
	public BayesianParameterEstimation(double[] c)
	{
		_c = c;
		
		m_queries = new ArrayList<Query>();
		m_queryHandlers = new HashMap<Query, BayesianQueryHandler>();
		
		 // Get the 16 queries
		 m_queries.add(new Query(null, null));
		 m_queries.add(new Query("lioneer", null));
		 m_queries.add(new Query(null, "tv"));
		 m_queries.add(new Query("lioneer", "tv"));
		 m_queries.add(new Query(null, "audio"));
		 m_queries.add(new Query("lioneer", "audio"));
		 m_queries.add(new Query(null, "dvd"));
		 m_queries.add(new Query("lioneer", "dvd"));
		 m_queries.add(new Query("pg", null));
		 m_queries.add(new Query("pg", "tv"));
		 m_queries.add(new Query("pg", "audio"));
		 m_queries.add(new Query("pg", "dvd"));
		 m_queries.add(new Query("flat", null));
		 m_queries.add(new Query("flat", "tv"));
		 m_queries.add(new Query("flat", "audio"));
		 m_queries.add(new Query("flat", "dvd"));
		 
		 for(Query q: m_queries){
			 m_queryHandlers.put(q, new BayesianQueryHandler(q,_c));
		 }
	}

	@Override
	public double[] getPrediction(Query q) {
		return m_queryHandlers.get(q).getPredictions();
	}
	
	@Override
	public boolean updateModel(QueryReport queryReport, 
							   SalesReport salesReport,
							   BidBundle bidBundle,
							   int numberPromotedSlots,
							   HashMap<Query,int[]> allOrders,
							   HashMap<Query,int[]> allImpressions,
							   HashMap<Product,HashMap<UserState,Integer>> userStates) {
		for(Query q: m_queries){
			
			/*
			 * TODO
			 * 
			 * if things broke skip the update
			 */
			
			int[] order = allOrders.get(q);
			int[] impressions = allImpressions.get(q);
			
			HashMap<String, Ad> query_ads = new HashMap<String,Ad>();
			query_ads.put("adv1",bidBundle.getAd(q));
			for(int i = 2; i <= 8; i++) {
				query_ads.put("adv" + i, queryReport.getAd(q, "adv" + i));
			}
			
			LinkedList<LinkedList<String>> advertisersAbovePerSlot = new LinkedList<LinkedList<String>>();
			LinkedList<Integer> impressionsPerSlot = new LinkedList<Integer>();
			int[][] impressionMatrix = greedyAssign(numSlots, numAdvertisers, order, impressions);

			int[] impressionPerAgent = new int[numAdvertisers];
			//for each agent
			for(int ag = 0; ag < numAdvertisers; ag++){
				int sum = 0;
				//for each slot
				for(int slt = 0; slt < numSlots; slt++){
					sum+=impressionMatrix[ag][slt];
				}
				impressionPerAgent[ag]=sum;
			}

			//where are we in bid pair matrix?
			ArrayList<ImprPair> higherthanus = new ArrayList<ImprPair>();
			for(int i = 0; i < order.length; i++){
				if(order[i]==0){
					break;
				}else{
					higherthanus.add(new ImprPair(order[i],impressionPerAgent[order[i]]));
				}
			}

			Collections.sort(higherthanus);
			for(int j = 0; j < numSlots; j++) {
				int numImpressions = impressionMatrix[0][j];
				impressionsPerSlot.add(numImpressions);

				LinkedList<String> advsAbove = new LinkedList<String>();
				if(!(numImpressions == 0 || numSlots == 0)) {
					//This List is NOT SORTED!!!
					List<ImprPair> sublist = higherthanus.subList(0, j);
					for(ImprPair imp : sublist){
						advsAbove.add("adv" + (imp.getID()+1));
					}
				}

				advertisersAbovePerSlot.add(advsAbove);
			}
			
			m_queryHandlers.get(q).update("adv1",queryReport,salesReport,numberPromotedSlots,impressionsPerSlot,advertisersAbovePerSlot,query_ads,userStates);
		}
		return true;
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

	@Override
	public AbstractModel getCopy() {
		return new BayesianParameterEstimation(_c);
	}
}
