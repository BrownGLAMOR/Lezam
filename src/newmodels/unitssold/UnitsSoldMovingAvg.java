package newmodels.unitssold;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

public class UnitsSoldMovingAvg extends AbstractUnitsSoldModel {

	protected int _distributionWindow;
	protected List<Integer> _sold;
	
	protected final double alpha = .75;
	protected double estimate;
	protected double latestSample;
	
	public UnitsSoldMovingAvg(int distributionCapacity, int distributionWindow) {
		_distributionWindow = distributionWindow;
		_sold = new ArrayList<Integer>();
		estimate = 1.0*distributionCapacity/distributionWindow;
	}
	
	
	@Override
	public double getWindowSold(){
		double total = 0;
		for (int i = 0; i < _distributionWindow - 1; i++) {
			int index = _sold.size() - i - 1;
			if (index >= 0) total += _sold.get(index);	
		}
		total += estimate;
		return total;
	}

	@Override
	public void update(SalesReport salesReport) {
		int conversions = 0;
		for(Query q : salesReport){
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
}
