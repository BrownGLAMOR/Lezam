package agents.rules;

import agents.GenericBidStrategy;
import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ReinvestmentCap extends StrategyTransformation{	
	protected double _maxValue;
	
	public ReinvestmentCap(double maxValue){
		_maxValue = maxValue;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		if(strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR) > _maxValue){
			strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, _maxValue);
		}
	}

}
