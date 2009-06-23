package newmodels.bidtocpc;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicBidToCPC extends AbstractBidToCPC {

	public BasicBidToCPC(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double bid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return true;
	}

}
