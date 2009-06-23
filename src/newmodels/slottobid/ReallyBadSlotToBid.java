package newmodels.slottobid;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class ReallyBadSlotToBid extends AbstractSlotToBidModel {

	public ReallyBadSlotToBid(Query query) {
		super(query);
	}

	@Override
	public double getPrediction(double slot) {
		if(slot <= 0 || slot >= 6 || Double.isInfinite(slot) || Double.isNaN(slot)) {
			return 0;
		}
		else {
			return (1.0/(slot*slot))*3;
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

}
