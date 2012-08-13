package models.queryanalyzer.ds;

import java.util.Set;

public abstract class AbstractQAData {
	public abstract int[] getBidOrder(Set<Integer> AgentIds);
	public abstract int[] getTrueImpressions(Set<Integer> AgentIds);
}
