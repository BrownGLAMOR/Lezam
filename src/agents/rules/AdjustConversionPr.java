package agents.rules;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

public class AdjustConversionPr extends StrategyTransformation{	
	protected int _distributionCapacity;
	protected int _distributionWindow;
	protected SalesReport _salesReport;
	LinkedList<Integer> _sold;
	int _usedCapacity;
	private LinkedList<Integer> _capacityWindow;
	private int _estimatedLastDaySoldCap;
	private Set<Query> _componentSpecialty;
	
	protected Hashtable<Query,Double> _baseLineConversion;
	
	
	public AdjustConversionPr(int distributionCapacity, int distributionWindow, Hashtable<Query,Double> baseLineConversion, Set<Query> componentSpecialty){
		_distributionCapacity = distributionCapacity;
		_distributionWindow = distributionWindow;
		_baseLineConversion = baseLineConversion;
		_componentSpecialty = componentSpecialty;
		_sold = new LinkedList<Integer>();
		_capacityWindow = new LinkedList<Integer>();
		_usedCapacity = 0;
		_estimatedLastDaySoldCap = 0;
	}
	public int getOversold(){return _usedCapacity > _distributionCapacity ? _usedCapacity - _distributionCapacity : 0;}
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
		int overstock = 0;
		if(_usedCapacity > _distributionCapacity){
			overstock = _usedCapacity - _distributionCapacity;
		}
		
		double conversionPr = conversion_pr(overstock, _baseLineConversion.get(q), _componentSpecialty.contains(q));
		System.out.println(q+" Baseline Conversion: " + _baseLineConversion.get(q) + " current conversion: " + conversionPr + " overstock: " + overstock);
		strategy.setQueryConversion(q, conversionPr);
	}

	double conversion_pr(int overstock, double baseline, boolean bonus){
		if(bonus)
			return eta(baseline*inventory_penalty(overstock), 1+0.5);
		else
			return baseline*inventory_penalty(overstock);
	}
	
	double inventory_penalty(int overstock){
		return Math.pow(0.995, overstock);
	}
	
	double eta(double p, double x){
		return (p*x)/(p*x + (1-p));
	}
}
