package agents;

import java.util.Hashtable;
import java.util.Random;

import edu.umich.eecs.tac.props.Query;

public class WalkingAgent extends JESOMAgent {

	protected static final double FIRST_ADJUSTMENT = 0.75;
	protected static final double ADJUSTMENT = 0.05;
	
	protected Hashtable<Query, Double> _maxBids;
	protected Random _random;
	
	protected static final double LOWER_CUTOFF = 0.30;
	protected static final double UPPER_CUTOFF = 0.60;
	
	@Override
	protected void initBidder(){
		super.initBidder();
		_random = new Random();
		_maxBids = new Hashtable<Query, Double>();
		for(Query q : _querySpace){
			_maxBids.put(q, _bidStrategy.getQueryBid(q));
			_bidStrategy.setQueryBid(q, _bidStrategy.getQueryBid(q) * FIRST_ADJUSTMENT);
		}
	}
	
	@Override
	protected void updateBidStrategy() {
		for(Query q : _querySpace){
			double prob = _random.nextDouble();
			double oldBid = _bidStrategy.getQueryBid(q);
			if (prob <= LOWER_CUTOFF){
				_bidStrategy.setQueryBid(q, oldBid * (1.0 - ADJUSTMENT));
			}
			else if (prob <= UPPER_CUTOFF){
				// Do nothing!
			}
			else { // Move up
				double newBid = _bidStrategy.getQueryBid(q) * (1.0 + ADJUSTMENT);
				double maxBid = _maxBids.get(q); 
				if(newBid >= maxBid){
					newBid = maxBid;
				}
				_bidStrategy.setQueryBid(q, newBid);
			}
		}
	}

}
