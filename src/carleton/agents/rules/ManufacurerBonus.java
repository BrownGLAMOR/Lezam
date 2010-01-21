package carleton.agents.rules;

import carleton.strategies.GenericBidStrategy;
import carleton.strategies.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ManufacurerBonus extends StrategyTransformation{	
	protected double _bonusFactor;
	
	public ManufacurerBonus(double bonusFactor){
		_bonusFactor = bonusFactor;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		double current = strategy.getProperty(q, SSBBidStrategy.CONVERSION_REVENUE);
		strategy.setProperty(q, SSBBidStrategy.CONVERSION_REVENUE, current*(1+_bonusFactor));
	}

}
