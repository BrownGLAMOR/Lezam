package agents.rules;

import agents.GenericBidStrategy;
import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class SetProperty extends StrategyTransformation{
	protected String _property;
	protected double _value;
	
	public SetProperty(String property, double value){
		_property = property;
		_value = value;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		strategy.setProperty(q, _property, _value);
	}
	
}
