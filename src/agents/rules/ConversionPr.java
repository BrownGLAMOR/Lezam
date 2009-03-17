package agents.rules;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class ConversionPr extends StrategyTransformation{	
	protected double _conversionPr;
	
	public ConversionPr(double conversionPr){
		_conversionPr = conversionPr;
	}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		strategy.setQueryConversion(q, _conversionPr);
	}

}
