package newmodels.bidtonumclicks;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicBidToNumClicks extends AbstractBidToNumClicks {

	Query _query;
	public BasicBidToNumClicks(Query query) {
		super(query);
		_query = query;
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

	@Override
	public AbstractModel getCopy() {
		return new BasicBidToNumClicks(_query);
	}
	
	

}
