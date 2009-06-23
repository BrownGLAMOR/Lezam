package simulator.models;

import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.bidtonumclicks.AbstractBidToNumClicks;

public class PerfectBidToNumClicks extends AbstractBidToNumClicks {

	private BasicSimulator _simulator;

	public PerfectBidToNumClicks(Query query, BasicSimulator simulator) {
		super(query);
		_simulator = simulator;
	}

	@Override
	public int getPrediction(double bid) {
		Reports reports = _simulator.getSingleQueryReport(_query, bid);
		return reports.getQueryReport().getClicks(_query);
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

}
