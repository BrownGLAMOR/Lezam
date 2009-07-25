package simulator.models;

import java.util.LinkedList;

import newmodels.bidtoprconv.AbstractBidToPrConv;
import newmodels.prconv.NewAbstractConversionModel;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPrConv extends NewAbstractConversionModel {

	private BasicSimulator _simulator;

	public PerfectBidToPrConv(BasicSimulator simulator) {
		_simulator = simulator;
	}


	@Override
	public double getPrediction(Query query, double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(query, bid);
		double avgPrConv = 0;
		for(Reports report : reports) {
			if(report.getQueryReport().getClicks(query) != 0) {
				avgPrConv += report.getSalesReport().getConversions(query)/((double)report.getQueryReport().getClicks(query));
			}
		}
		avgPrConv = avgPrConv/((double) reports.size());
		return avgPrConv;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}

}
