package newmodels.bidtonumclicks;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicBidToNumClicks extends AbstractBidToNumClicks {

	public BasicBidToNumClicks(Query query) {
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
		return true;
	}

}
