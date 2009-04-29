package agents;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;

public class Cheap extends AbstractAgent {

	@Override
	protected void handleAdvertiserInfo(AdvertiserInfo advertiserInfo) {
		// TODO Auto-generated method stub
		super.handleAdvertiserInfo(advertiserInfo);
	}
	
	@Override
	protected BidBundle buildBidBudle() {
		BidBundle bidBundle = new BidBundle();
		double distCap = (double) _advertiserInfo.getDistributionCapacity();
		double distWind = (double) _advertiserInfo.getDistributionWindow();
		double dailyCap = distCap/distWind;
		
		for(Query q : _querySpace) {
			double queryBid;
			double queryBudget;
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO){
				queryBid = 1.2;
				queryBudget = dailyCap;
			}
			else if (q.getType() == QueryType.FOCUS_LEVEL_ONE){
				queryBid = 1.8;
				queryBudget = dailyCap;
			}
			else {
				queryBid = 2.4;
				queryBudget = dailyCap;
			}
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
