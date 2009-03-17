package agents.rules;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ManufacurerBonus extends StrategyTransformation{	
	protected double _bonusFactor;
	
	public ManufacurerBonus(double bonusFactor){
		_bonusFactor = bonusFactor;
	}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		double current = strategy.getQueryConversionRevenue(q);
		strategy.setQueryConversionRevenue(q, current*(1+_bonusFactor));
	}

}
