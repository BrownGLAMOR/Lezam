package simulator.models;

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
		Reports reports = _simulator.getSingleQueryReport(_query, bid);
		//TODO may need to cast to double!
		if(reports.getQueryReport().getClicks(_query) == 0) {
			return 0;
		}
		else {
			return reports.getSalesReport().getConversions(_query)/((double)reports.getQueryReport().getClicks(_query));
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
