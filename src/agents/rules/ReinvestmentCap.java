package agents.rules;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ReinvestmentCap extends StrategyTransformation{	
	protected double _maxValue;
	
	public ReinvestmentCap(double maxValue){
		_maxValue = maxValue;
	}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		if(strategy.getQueryReinvestFactor(q) > _maxValue){
			strategy.setQueryReinvestFactor(q, _maxValue);
		}
	}

}
