package newmodels.bidtocpc;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicBidToCPC extends AbstractBidToCPC {

	@Override
	public double getPrediction(Query query, double bid) {
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryreport, SalesReport salesReport, BidBundle bidbundle) {
		return false;
	}

	@Override
	public AbstractModel getCopy() {
		return new BasicBidToCPC();
	}


}
