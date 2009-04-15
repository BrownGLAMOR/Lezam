package agents;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class MelGibsonAgent extends AbstractAgent {
	GenericBidStrategy _bidStrategy;
	double _slice;
	
	@Override
	protected void initBidder() {
		_bidStrategy = new GenericBidStrategy(_querySpace);
		
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		
		_slice = distributionCapacity / (_querySpace.size() * distributionWindow);
	}

	@Override
	protected void updateBidStrategy() {
		for(Query q : _querySpace){
			_bidStrategy.setProperty(q, GenericBidStrategy.BID, Math.random() * 4);
			double bid = _bidStrategy.getProperty(q, GenericBidStrategy.BID);
			_bidStrategy.setQuerySpendLimit(q, bid*_slice);
		}
	}

	@Override
	protected BidBundle buildBidBudle() {
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

}
