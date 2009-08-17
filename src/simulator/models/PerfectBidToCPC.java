package simulator.models;

import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToCPC extends AbstractBidToCPC {
	
	private BasicSimulator _simulator;

	public PerfectBidToCPC(BasicSimulator simulator) {
		_simulator = simulator;
	}

	@Override
	public double getPrediction(Query query, double bid) {
		LinkedList<Reports> reports = _simulator.getSingleQueryReport(query, bid);
		double avgCPC = 0;
		for(Reports report : reports) {
			avgCPC += report.getQueryReport().getCPC(query);
		}
		avgCPC = avgCPC/((double) reports.size());
		return avgCPC;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectBidToCPC(_simulator);
	}

}
