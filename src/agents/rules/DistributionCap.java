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
	private LinkedList<Integer> _capacityWindow;
	private int _estimatedLastDaySoldCap;
	
	public DistributionCap(int distributionCapacity, int distributionWindow){
		_distributionCapacity = distributionCapacity;
		_distributionWindow = distributionWindow;
		_sold = new LinkedList<Integer>();
		_capacityWindow = new LinkedList<Integer>();
		_usedCapacity = 0;
		_estimatedLastDaySoldCap = 0;
	}
	
	public void updateReport(SalesReport salesReport){
		_salesReport = salesReport;
		int conversions = 0;
		for(Query q : _salesReport){//.iterator()){
			conversions += _salesReport.getConversions(q);
		}
		
		_sold.addFirst(conversions);
		_capacityWindow.addFirst(conversions);
		
		// Subtract the previous last day estimated capacity (since now we have real data for that day).
		_usedCapacity -= _estimatedLastDaySoldCap;
		// Since last day capacity is unknown, we assume that it was the same as on the day
		// before yesterday. 
		_estimatedLastDaySoldCap = conversions;
		
		if(_capacityWindow.size() > (_distributionWindow-1)){
			int restored = _capacityWindow.removeLast();
			_usedCapacity += conversions - restored;	
		}
		else
		{
			_usedCapacity += conversions;
		}
		_usedCapacity += _estimatedLastDaySoldCap;	
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
