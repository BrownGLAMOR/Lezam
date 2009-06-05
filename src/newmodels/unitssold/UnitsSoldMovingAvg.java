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
	
	public UnitsSoldMovingAvg(int distributionWindow) {
		_distributionWindow = distributionWindow;
		_sold = new ArrayList<Integer>();
	}
	
	
	@Override
	public double getWindowSold(){
		double total = 0;
		for (int i = 0; i < _distributionWindow; i++) {
			int index = _sold.size() - i + 1;
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
		estimate = alpha*conversions + (1 - alpha)*estimate;
	}

	public double getEstimate() {
		return estimate;
	}
}
