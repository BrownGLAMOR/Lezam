package agents;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class Cheap extends AbstractAgent {

	@Override
	protected BidBundle buildBidBudle() {
		BidBundle bidBundle = new BidBundle();
		
		for(Query q : _querySpace) {
			double queryBid = .4;
			double queryBudget = 100;

			bidBundle.addQuery(q, queryBid, null);
			bidBundle.setDailyLimit(q, queryBudget);
		}
		
		return bidBundle;
	}

	@Override
	protected void initBidder() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateBidStrategy() {
		// TODO Auto-generated method stub
		
	}

}
