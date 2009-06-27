package simulator.models;

import java.util.LinkedList;

import newmodels.bidtonumclicks.AbstractBidToNumClicks;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToNumClicks extends AbstractBidToNumClicks {

	private BasicSimulator _simulator;

	public PerfectBidToNumClicks(Query query, BasicSimulator simulator) {
		super(query);
		_simulator = simulator;
	}

	@Override
	public int getPrediction(double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(_query, bid);
		int avgNumClicks = 0;
		for(Reports report : reports) {
			avgNumClicks += report.getQueryReport().getClicks(_query);
		}
		avgNumClicks = (int) (avgNumClicks/((double) reports.size()));
		return avgNumClicks;
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

}
