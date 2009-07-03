package newmodels.bidtopos;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public abstract class BidToPositionModel extends AbstractComparableModel {
	
	public abstract double getPosition(Query q, double bid);
	
	public abstract void updateBidBundle(BidBundle bids);
	
	public abstract void updateQueryReport(QueryReport qr);
	
}
