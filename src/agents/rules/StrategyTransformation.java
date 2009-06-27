package agents.rules;

import java.util.Set;

import agents.GenericBidStrategy;
import edu.umich.eecs.tac.props.Query;

public abstract class StrategyTransformation {
	
	public void apply(Set<Query> queries, GenericBidStrategy strategy){
		for(Query q : queries){
			transform(q,strategy);
		}
	}
	
	public void apply(GenericBidStrategy strategy){
		apply(strategy.getQuerySpace(),strategy);
	}
	
	protected abstract void transform(Query q, GenericBidStrategy strategy);
}
