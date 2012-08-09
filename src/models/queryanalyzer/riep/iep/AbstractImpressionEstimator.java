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
	 * State whether IEResults with high value or low value are better.
	 * @return
	 */
	public ObjectiveGoal getObjectiveGoal(); 
	
		
	public enum ObjectiveGoal {
		MINIMIZE, MAXIMIZE
	}
	
}
