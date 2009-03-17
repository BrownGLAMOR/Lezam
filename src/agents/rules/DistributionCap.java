package agents.rules;

import java.util.LinkedList;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

public class DistributionCap extends StrategyTransformation{	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	protected SalesReport _salesReport;
	LinkedList<Integer> _sold;
	int _usedCapacity;
	
	public DistributionCap(int distributionCapacity, int distributionWindow){
		_distributionCapacity = distributionCapacity;
		_distributionWindow = distributionWindow;
		_sold = new LinkedList<Integer>();
		_usedCapacity = 0;
	}
	
	public void updateReport(SalesReport salesReport){
		_salesReport = salesReport;
		int conversions = 0;
		for(Query q : _salesReport){//.iterator()){
			conversions += _salesReport.getConversions(q);
		}
		
		_sold.addFirst(conversions);
		
		if(_sold.size() > _distributionWindow){
			int restored = _sold.removeLast();
			_usedCapacity += conversions - restored;
		}
		else {
			_usedCapacity += conversions;
		}
		
	}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		int remainingCap = _distributionCapacity - _usedCapacity;
		if(remainingCap < 0){
			remainingCap = 0;
		}
		double bid = strategy.getQueryBid(q);
		strategy.setQuerySpendLimit(q, bid*remainingCap/8);
	}

}
