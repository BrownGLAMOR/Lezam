package simulator.models;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.bidtonumclicks.AbstractBidToNumClicks;

public class PerfectBidToNumClicks extends AbstractBidToNumClicks {

	public PerfectBidToNumClicks(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getPrediction(double bid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		// TODO Auto-generated method stub
		return false;
	}

}
