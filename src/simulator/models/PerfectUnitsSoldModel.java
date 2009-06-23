package simulator.models;

import java.util.HashMap;

import edu.umich.eecs.tac.props.SalesReport;
import newmodels.unitssold.AbstractUnitsSoldModel;


public class PerfectUnitsSoldModel extends AbstractUnitsSoldModel {
	
	private Integer[] _salesOverWindow;
	private int totOver4Days;

	public PerfectUnitsSoldModel(Integer[] salesOverWindow) {
		_salesOverWindow = salesOverWindow;
		totOver4Days = 0;
		for(int i = 0; i < _salesOverWindow.length-1; i++) {
			totOver4Days += _salesOverWindow[i];
			System.out.println("Sales " + (i+1) + " days ago: " + _salesOverWindow[i]);
		}
		System.out.println("Total over 4 days: " + totOver4Days);
		System.out.println("Total over 5 days: " + (totOver4Days+_salesOverWindow[_salesOverWindow.length-1]));
	}

	@Override
	public double getEstimate() {
		return totOver4Days;
	}

	@Override
	public double getLatestSample() {
		return totOver4Days;
	}

	@Override
	public double getWindowSold() {
		return totOver4Days;
	}

	@Override
	public void update(SalesReport salesReport) {
	}

}
