package newmodels.bidtoslot;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class ReallyBadBidToSlot extends AbstractBidToSlotModel {

	public ReallyBadBidToSlot(Query query) {
		super(query);
	}

	@Override
	public double getPrediction(double bid) {
		if(bid <= 0 || bid >= 5 || Double.isInfinite(bid) || Double.isNaN(bid)) {
			return 5;
		}
		else {
			return Math.sqrt(bid/3.0);
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
