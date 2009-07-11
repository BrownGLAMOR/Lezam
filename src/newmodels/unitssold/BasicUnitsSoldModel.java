package newmodels.unitssold;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicUnitsSoldModel extends AbstractUnitsSoldModel {
	
	protected Set<Query> _querySpace;
	protected int _capacity;
	protected int _window;
	protected List<Integer> _sold;
	
	public BasicUnitsSoldModel(Set<Query> querySpace, int distributionCapacity, int distributionWindow) {
		_querySpace = querySpace;
		_capacity = distributionCapacity;
		_window = distributionWindow;
		_sold = new ArrayList<Integer>();
	}

	@Override
	public double getEstimate() {
		throw new RuntimeException("Do not use this method");
	}

	@Override
	public double getLatestSample() {
		throw new RuntimeException("Do not use this method");
	}

	@Override
	public double getWindowSold() {
		double total = 0;
		for (int i = 0; i < _window - 2; i++) {
			int index = _sold.size() - i - 1;
			if (index >= 0) {
				total += _sold.get(index);	
			}
			else {
				total += _capacity/((double) _window);
			}
		}
		total = (total + _capacity/((double) _window)) / 4.0 + total;
		return total;
	}
	
	public double getThreeDaysSold() {
		double total = 0;
		for (int i = 0; i < _window - 2; i++) {
			int index = _sold.size() - i - 1;
			if (index >= 0) {
				total += _sold.get(index);	
			}
			else {
				total += _capacity/((double) _window);
			}
		}
		return total;
	}

	@Override
	public void update(SalesReport salesReport) {
		int conversions = 0;
		for(Query q : _querySpace){
			conversions += salesReport.getConversions(q);
		}
		_sold.add(conversions);
	}

}
