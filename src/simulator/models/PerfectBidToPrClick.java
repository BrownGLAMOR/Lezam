package simulator.models;

import simulator.BasicSimulator;
import simulator.Reports;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPrClick extends AbstractBidToPrClick {

	private BasicSimulator _simulator;

	public PerfectBidToPrClick(Query q, BasicSimulator simulator) {
		super(q);
		_simulator = simulator;
	}

	@Override
	public double getPrediction(double bid) {
		Reports reports = _simulator.getSingleQueryReport(_query, bid);
		//TODO may need to cast to double!
		return reports.getQueryReport().getClicks(_query)/reports.getQueryReport().getImpressions(_query);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
