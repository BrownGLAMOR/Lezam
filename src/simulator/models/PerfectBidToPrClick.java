package simulator.models;

import java.util.LinkedList;

import newmodels.bidtoprclick.AbstractBidToPrClick;
import simulator.BasicSimulator;
import simulator.Reports;
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
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(_query, bid);
		double avgPrClick = 0;
		for(Reports report : reports) {
			if(report.getQueryReport().getImpressions(_query) != 0) {
				avgPrClick += report.getQueryReport().getClicks(_query)/((double)report.getQueryReport().getImpressions(_query));
			}
		}
		avgPrClick = avgPrClick/((double) reports.size());
		return avgPrClick;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
