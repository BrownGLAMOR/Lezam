package models.queryanalyzer;

import java.util.HashMap;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

public abstract class AbstractQueryAnalyzer extends AbstractModel {
	
	
	public abstract int getOrderPrediction(Query query, String adv);
	
	public abstract int getImpressionsPrediction(Query query, String adv);
	
	public abstract int[] getOrderPrediction(Query query);
	
	public abstract int[] getImpressionsPrediction(Query query);
	
	public abstract int[] getImpressionRangePrediction(Query query, String adv);
	
	public abstract int[][] getImpressionRangePrediction(Query query);

	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle, HashMap<Query, Integer> maxImps);
	
	public abstract void setAdvertiser(String ourAdv);
	
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


	public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
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

}
