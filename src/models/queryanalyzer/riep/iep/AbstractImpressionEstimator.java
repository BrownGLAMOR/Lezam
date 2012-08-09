package models.queryanalyzer.riep.iep;

import models.queryanalyzer.ds.AbstractQAInstance;

public interface AbstractImpressionEstimator {
	
	/**
	 * Given a squashed bid ordering of agents, find the number of impressions for each agent
	 * @param order
	 * @return
	 */
	public IEResult search(int[] order);
	
	public String getName();
	
	public AbstractQAInstance getInstance();


	/**
	 * Gets the approximate average position of each agent.
	 * If exact average positions are known, those should be used.
	 * Otherwise, use sampled average positions.
	 * This can possibly include padded agents if they exist.
	 */
	public double[] getApproximateAveragePositions();
	
	
	/**
	 * State whether IEResults with high value or low value are better.
	 * @return
	 */
	public ObjectiveGoal getObjectiveGoal(); 
	
		
	public enum ObjectiveGoal {
		MINIMIZE, MAXIMIZE
	}
	
}