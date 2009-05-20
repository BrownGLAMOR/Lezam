package agents;

import java.util.ArrayList;

import modelers.bidtoposition.ilke.BucketBidToPositionModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class CapricaAgent extends AbstractAgent {
	GenericBidStrategy _bidStrategy;
	BucketBidToPositionModel _pgbm;
	ArrayList<BidBundle>  _bidBundles;
	double _slice;
	int _day;
	
	@Override
	protected void initBidder() {
		_day = 0;
		_bidBundles = new ArrayList<BidBundle>();
		_bidStrategy = new GenericBidStrategy(_querySpace);
		
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		
		_slice = distributionCapacity / (_querySpace.size() * distributionWindow);
		
		int slots = _slotInfo.getPromotedSlots() + _slotInfo.getRegularSlots();
		_pgbm = new BucketBidToPositionModel(_querySpace, slots);
	}
	
	@Override
	protected void handleQueryReport(QueryReport queryReport) {
		super.handleQueryReport(queryReport);
		if(queryReport.size() > 0) {
			BidBundle bidBundle = _bidBundles.remove(0);
			if(_day > 10) {
				_pgbm.train();
				
				for(Query q: queryReport) {
					double bid = bidBundle.getBid(q);
					double actualpos = queryReport.getPosition(q);
					double predictedPos = _pgbm.getPosition(q, bid);
					System.out.println(q + "\tBid: " + bid + "\tBucket: " + _pgbm.getBucket(bid) + "\tActualPos: " + actualpos + "\tPredictedPos: " + predictedPos);
				}
			}
			_pgbm.updateQueryReport(queryReport);
		}
		_day++;
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
		
		BidBundle bidBundle = _bidStrategy.buildBidBundle();
		_bidBundles.add(bidBundle);
		_pgbm.updateBidBundle(bidBundle);
		
		
		
		return bidBundle;
	}

}
