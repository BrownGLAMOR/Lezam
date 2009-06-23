package simulator.models;

/**
 * @author jberg
 *
 */

import simulator.BasicSimulator;
import simulator.Reports;
import newmodels.bidtoslot.AbstractBidToSlotModel;
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
		Reports reports = _simulator.getSingleQueryReport(_query, bid);
		return reports.getQueryReport().getPosition(_query);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}


}
