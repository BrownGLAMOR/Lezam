package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.HashMap;
import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.bidtopos.AbstractBidToPos;
import newmodels.bidtopos.SpatialBidToPos;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPosition extends AbstractBidToPos {

	private BasicSimulator _simulator;

	public PerfectBidToPosition(BasicSimulator simulator) {
		_simulator = simulator;
	}

	@Override
	public double getPrediction(Query query, double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(query, bid);
		double avgPos = 0;
		for(Reports report : reports) {
			avgPos += report.getQueryReport().getPosition(query);
		}
		avgPos = avgPos/((double) reports.size());
		return avgPos;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectBidToPosition(_simulator);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

	@Override
	public void setNumPromSlots(int numPromSlots) {
		//not used in this class
	}

}
