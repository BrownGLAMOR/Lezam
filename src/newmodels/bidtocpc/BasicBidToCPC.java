package newmodels.bidtocpc;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class BasicBidToCPC extends AbstractBidToCPC {

	@Override
	public double getPrediction(Query query, double bid, BidBundle bidbundle) {
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryreport, BidBundle bidbundle) {
		return false;
	}


}
