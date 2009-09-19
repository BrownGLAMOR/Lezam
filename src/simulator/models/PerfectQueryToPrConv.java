package simulator.models;

import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectQueryToPrConv extends AbstractConversionModel {

	private BasicSimulator _simulator;

	public PerfectQueryToPrConv(BasicSimulator simulator) {
		_simulator = simulator;
	}


	@Override
	public double getPrediction(Query query) {
		//TODO
		/*
		 * Figure out how to properly deal with what the bid should be.....
		 */
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(query, 2.0);
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


	@Override
	public AbstractModel getCopy() {
		return new PerfectQueryToPrConv(_simulator);
	}

}
