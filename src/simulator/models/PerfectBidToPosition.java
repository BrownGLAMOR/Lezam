package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.LinkedList;

import newmodels.bidtoslot.AbstractBidToSlotModel;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPosition extends AbstractBidToSlotModel {

	private BasicSimulator _simulator;

	public PerfectBidToPosition(Query query, BasicSimulator simulator) {
		super(query);
		_simulator = simulator;
	}

	@Override
	public double getPrediction(double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(_query, bid);
		double avgPos = 0;
		for(Reports report : reports) {
			avgPos += report.getQueryReport().getPosition(_query);
		}
		avgPos = avgPos/((double) reports.size());
		return avgPos;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}


}
