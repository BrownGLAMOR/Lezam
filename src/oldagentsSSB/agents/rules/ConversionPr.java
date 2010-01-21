package oldagentsSSB.agents.rules;

import oldagentsSSB.strategies.GenericBidStrategy;
import oldagentsSSB.strategies.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ConversionPr extends StrategyTransformation{	
	protected double _conversionPr;
	
	public ConversionPr(double conversionPr){
		_conversionPr = conversionPr;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		strategy.setProperty(q, SSBBidStrategy.CONVERSION_PR, _conversionPr);
	}
	
}