package models.queryanalyzer.iep;

public interface AbstractImpressionEstimator {
	
	/**
	 * Given a squashed bid ordering of agents, find the number of impressions for each agent
	 * @param order
	 * @return
	 */
	public IEResult search(int[] order);
	
	public String getName();
	
}
