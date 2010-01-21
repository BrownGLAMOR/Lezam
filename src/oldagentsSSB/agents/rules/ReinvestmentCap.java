package oldagentsSSB.agents.rules;

import oldagentsSSB.strategies.GenericBidStrategy;
import oldagentsSSB.strategies.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ReinvestmentCap extends StrategyTransformation{	
	protected double _maxValue;
	
	public ReinvestmentCap(double maxValue){
		_maxValue = maxValue;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		//System.out.println("!@#$*********** "+q+" " + strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR) + " : "+_maxValue);
		if(strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR) > _maxValue){
			//System.out.println("!@#$***********2 "+q+" " + strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR) + " <- "+_maxValue);
			strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, _maxValue);
		}
	}

}
