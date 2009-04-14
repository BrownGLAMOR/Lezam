package agents.rules;

import modelers.UnitsSoldModel;

import agents.GenericBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class DistributionCap extends StrategyTransformation{	
	protected int _distributionCapacity;
	protected UnitsSoldModel _unitsSold;
	protected double _magicDevisor;
	
	public DistributionCap(int distributionCapacity, UnitsSoldModel unitsSold, double magicDevisor){
		_distributionCapacity = distributionCapacity;
		_unitsSold = unitsSold;
		_magicDevisor = magicDevisor;
	}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		int remainingCap = _distributionCapacity - _unitsSold.getWindowSold();
		if(remainingCap < 0){
			remainingCap = 0;
		}
		double bid = strategy.getQueryBid(q);
		strategy.setQuerySpendLimit(q, bid*remainingCap/_magicDevisor);
	}

}
