package simulator.models;

import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;

public class PerfectBidToCPC extends AbstractBidToCPC {
	
	private BasicSimulator _simulator;

	public PerfectBidToCPC(Query query, BasicSimulator simulator) {
		super(query);
		_simulator = simulator;
	}

	@Override
	public double getPrediction(double bid) {
		Reports reports = _simulator.getSingleQueryReport(_query, bid);
		return reports.getQueryReport().getCost(_query);
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport) {
		return true;
	}

}
