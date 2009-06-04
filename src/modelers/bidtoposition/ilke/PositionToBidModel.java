package modelers.bidtoposition.ilke;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public abstract class PositionToBidModel extends AbstractComparableModel{
	
	public abstract double getBid(Query q, double position);
	
	public abstract void updateBidBundle(BidBundle bids);
	
	public abstract void updateQueryReport(QueryReport qr);
	
}
