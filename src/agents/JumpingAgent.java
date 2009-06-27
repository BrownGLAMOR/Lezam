package agents;

import java.util.Hashtable;
import java.util.Queue;
import java.util.Random;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/*

INCOMPLETE

need to:

increase high value (done)

*/

public class JumpingAgent extends JESOMAgent {

	protected static final double FIRST_ADJUSTMENT = 0.55;
	protected static final double HIGH_ADJUSTMENT = 0.70;
	protected static final double MAX_ADJUSTMENT = 0.95;
	protected static final double MIN_ADJUSTMENT = 0.05;
	protected static final double HIGH_MULTIPLE = 1.25;
	
	protected Random _random;
	
	public static final boolean DEBUG = false;
	protected static final double SLOT_FRACTION = 0.30;
	protected static final double UNDERCUT_CONSTANT = 0.98; // could change to be a random interval
	
	public static final double CONVERSION_F0 = .1;
	public static final double CONVERSION_F1 = .2;
	public static final double CONVERSION_F2 = .3;
	public static final double SALE_VALUE = 10;
	
	public static final double DECREASE_FACTOR = .95;
	public static final double INCREASE_FACTOR = 1.5;
	protected Hashtable<Query, Double> _previousBids;
	protected Hashtable<Query, Double> _maxBids;
	protected Hashtable<Query, Double> _highBids;
	protected Hashtable<Query, Double> _minBids;
	protected int _estimatedCapacity;
	protected Queue<Integer> _capacityWindow;
	
	@Override
	protected void initBidder() {
		
		super.initBidder();

		_previousBids = new Hashtable<Query, Double>();
		_highBids = new Hashtable<Query, Double>();
		_maxBids = new Hashtable<Query, Double>();
		_minBids = new Hashtable<Query, Double>();
		
		for(Query query:_querySpace) {
			_previousBids.put(query, _bidStrategy.getQueryBid(query) * FIRST_ADJUSTMENT);
			_highBids.put(query, _bidStrategy.getQueryBid(query) * HIGH_ADJUSTMENT);
			_maxBids.put(query, _bidStrategy.getQueryBid(query) * MAX_ADJUSTMENT);
			_minBids.put(query, _bidStrategy.getQueryBid(query) * MIN_ADJUSTMENT);
			_bidStrategy.setQueryBid(query, _bidStrategy.getQueryBid(query) * FIRST_ADJUSTMENT);
		}
	}

	protected double returnBidWithinLimits(double newBid, Query query){
		Double minBid = _minBids.get(query);
		Double maxBid = _maxBids.get(query);
		return Math.min(Math.max(minBid, newBid),maxBid);
	}
	
	@Override
	protected void updateBidStrategy() {

		QueryReport lastReport = _queryReports.poll();
		
		for(Query query:_querySpace) {
			
			//Double lastValue = _previousBids.get(query);
			Double newBid = returnBidWithinLimits(_highBids.get(query), query);
			Double previousPosition = lastReport.getPosition(query, _advertiserInfo.getAdvertiserId());
			Double previousCPC = lastReport.getCPC(query);
			
			// undercut and update high value (down)
			if(previousPosition <= 3 && previousPosition != 0){
				Double highBid = previousCPC * UNDERCUT_CONSTANT; 
				newBid = highBid;//returnBidWithinLimits(highBid, query);
				//_highBids.put(query, highBid);
			}
			// undercut
			else if(previousPosition > 3 && previousPosition < 4){
				newBid = previousCPC * UNDERCUT_CONSTANT;//returnBidWithinLimits(previousCPC * UNDERCUT_CONSTANT, query);
			}
			// else bid high and TODO: update high value (up)
			/*if (previousCPC > _highBids.get(query)){
				_highBids.put(query, _highBids.get(query)*HIGH_MULTIPLE);
			}*/

			_previousBids.put(query, newBid);
			_bidStrategy.setQueryBid(query, newBid);
			

		}

	}

	public static void dbg(String str) {
		if(DEBUG)
			System.out.println(str);
	}

}
