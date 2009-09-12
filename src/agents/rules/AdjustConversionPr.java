package agents.rules;

import carleton.strategies.GenericBidStrategy;
import modelers.conversionprob.ConversionPrModel;
import edu.umich.eecs.tac.props.Query;

public class AdjustConversionPr extends StrategyTransformation{	
	protected ConversionPrModel _conversionPr;
	
	public AdjustConversionPr(ConversionPrModel conversionPr){
		_conversionPr = conversionPr;
	}
	
	//public int getOversold(){return _unitsSold.getWindowSold() > _distributionCapacity ? _unitsSold.getWindowSold() - _distributionCapacity : 0;}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		double conversionPr = _conversionPr.getCoversionPr(q);
		
		if(conversionPr < 0.01){
			conversionPr = 0;
		}
		
		strategy.setProperty(q, GenericBidStrategy.CONVERSION_PR, conversionPr);
	}
}
