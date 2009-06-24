package simulator.models;

import java.util.LinkedList;

import simulator.BasicSimulator;
import simulator.Reports;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprconv.AbstractBidToPrConv;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPrConv extends AbstractBidToPrConv {

	private BasicSimulator _simulator;

	public PerfectBidToPrConv(Query q, BasicSimulator simulator) {
		super(q);
		_simulator = simulator;
	}

	@Override
	public double getPrediction(double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(_query, bid);
		double avgPrConv = 0;
		for(Reports report : reports) {
			if(report.getQueryReport().getClicks(_query) != 0) {
				avgPrConv += report.getSalesReport().getConversions(_query)/((double)report.getQueryReport().getClicks(_query));
			}
		}
		avgPrConv = avgPrConv/((double) reports.size());
		return avgPrConv;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
