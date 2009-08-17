package newmodels.unitssold;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import newmodels.AbstractModel;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

public class UnitsSoldMovingAvg extends AbstractUnitsSoldModel {

	protected Set<Query> _querySpace;
	protected int _distributionWindow;
	protected List<Integer> _sold;
	
	protected final double alpha = .75;
	protected double estimate;
	protected double latestSample;
	private int _distributionCapacity;
	
	public UnitsSoldMovingAvg(Set<Query> querySpace, int distributionCapacity, int distributionWindow) {
		_querySpace = querySpace;
		_distributionWindow = distributionWindow;
		_distributionCapacity = distributionCapacity;
		_sold = new ArrayList<Integer>();
		estimate = 1.0*distributionCapacity/distributionWindow;
	}
	
	
	@Override
	public double getWindowSold(){
		double total = 0;
		for (int i = 0; i < _distributionWindow - 2; i++) {
			int index = _sold.size() - i - 1;
			if (index >= 0) total += _sold.get(index);	
		}
		total += estimate;
		return total;
	}

	@Override
	public void update(SalesReport salesReport) {
		int conversions = 0;
		for(Query q : _querySpace){
			conversions += salesReport.getConversions(q);
		}
		_sold.add(conversions);
		latestSample = conversions;
		if (_sold.size() == 1) estimate = conversions;
		else estimate = alpha*conversions + (1 - alpha)*estimate;
	}

	public double getEstimate() {
		return estimate;
	}
	
	public double getLatestSample() {
		return latestSample;
	}


	@Override
	public double getThreeDaysSold() {
		double total = 0;
		for (int i = 0; i < _distributionWindow - 2; i++) {
			int index = _sold.size() - i - 1;
			if (index >= 0) {
				total += _sold.get(index);	
			}
			else {
				total += _distributionCapacity/((double) _distributionWindow);
			}
		}
		return total;
	}


	@Override
	public AbstractModel getCopy() {
		return new UnitsSoldMovingAvg(_querySpace, _distributionCapacity, _distributionWindow);
	}
}
