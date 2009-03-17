package agents.rules;

import java.util.Set;

import edu.umich.eecs.tac.props.Query;

import agents.SSBBidStrategy;

public abstract class StrategyTransformation {
	
	public void apply(Set<Query> queries, SSBBidStrategy strategy){
		for(Query q : queries){
			transform(q,strategy);
		}
	}
	
	public void apply(SSBBidStrategy strategy){
		apply(strategy.getQuerySpace(),strategy);
	}
	
	protected abstract void transform(Query q, SSBBidStrategy strategy);
}
