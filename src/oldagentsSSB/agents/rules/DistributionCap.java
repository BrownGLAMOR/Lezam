package oldagentsSSB.agents.rules;

import oldagentsSSB.oldmodels.unitssold.UnitsSoldModel;
import oldagentsSSB.strategies.GenericBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class DistributionCap extends StrategyTransformation{	
	protected int _distributionCapacity;
	protected UnitsSoldModel _unitsSold;
	protected double _magicDevisor;	// I think this _magicDivisor corresponds to an estimated clicks/conversion. Carlton uses 8...
	
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
