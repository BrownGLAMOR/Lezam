package modelers.bidtoposition.ilke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class BucketPositionToBidModel extends PositionToBidModel {
	static double BUCKET_SIZE = 1.0;
	protected int _slots;
	protected double _noposition;
	protected ArrayList<ModelDataPoint> _data;
	protected HashMap<Query, HashMap<Integer, Double>> _buckets;
	protected HashMap<Query, HashMap<Integer, Integer>> _bucketAppearances;
	protected ArrayList<BidBundle> _bids;
	protected QueryReport _queryReport;
	protected Set<Query> _queries;
	
	
	public BucketPositionToBidModel(Set<Query> queries, int slots) {
		_slots = slots;
		_noposition = _slots + 1;		
		_bids = new ArrayList<BidBundle>();
		_data = new ArrayList<ModelDataPoint>();
		_queries = queries;
		
		initBuckets();
	}
	
	protected void initBuckets() {
		_buckets = new HashMap<Query, HashMap<Integer,Double>>();
		_bucketAppearances = new HashMap<Query, HashMap<Integer,Integer>>();
		
		for(Query q: _queries) {
			_buckets.put(q, new HashMap<Integer, Double>());
			_bucketAppearances.put(q, new HashMap<Integer, Integer>());
			
		}
	}
	public void updateBidBundle(BidBundle bb) {
		_bids.add(bb);
	}
	
	protected void updateBucketAppearance(Query q, double position) {
		int bucketnum = getBucket(position);

		Integer appearances = _bucketAppearances.get(q).get(bucketnum);
		if(appearances == null) {
			_bucketAppearances.get(q).put(bucketnum, 1);
		}
		else {
			_bucketAppearances.get(q).put(bucketnum, appearances.intValue() + 1);
		}

	}
	
	/**
	 * Get the average position of the given query and bid.
	 * @param q Query
	 * @param bid Bid
	 * @return Average position; Double.NaN if no position
	 */
	public double getBid(Query q, double position) {
		double bid =  getMeanBid(q, position);
		
		return bid;
	}
	
	protected void updateBuckets(Query q, double position, double bid) {
		int bucket = getBucket(position);
		Double bucketSum = _buckets.get(q).get(bucket);
		
		if(bucketSum == null) {
			_buckets.get(q).put(bucket, bid);
		}
		else {
			_buckets.get(q).put(bucket, bucketSum.doubleValue() + bid);
		}
	}
	
	protected void mapBidsToReportedPositions() {
		BidBundle bids = _bids.remove(0);
		
		for(Query q: _queryReport) {
			double pos = _queryReport.getPosition(q);
			if(pos == Double.NaN) {
				pos = _noposition;
			}
			Object[] given = new Object[2];
			given[0] = q;
			given[1] = pos;
			
			ModelDataPoint mp = new ModelDataPoint(given, bids.getBid(q));
			
			insertPoint(mp);
		}	
	}
	public void updateQueryReport(QueryReport queryReport) {
		_queryReport = queryReport;
		
		mapBidsToReportedPositions();	
	}
	
	public int getBucket(double position) {
		int bucket = (int) (position / BUCKET_SIZE);
		return bucket;
	}
	
	protected boolean greaterBucketExists(Query q, int bucket) {
		for(double b: _buckets.get(q).keySet()) {
			if(b > bucket)
				return true;
		}
		return false;
	}
	
	protected double getMeanBid(Query q, double position) {
	
		int bucket = getBucket(position);
		Double sum = _buckets.get(q).get(bucket);
		Integer appearance = _bucketAppearances.get(q).get(bucket);
		if(sum == null) {
			// no info gathered for the bucket of this bid yet, use the info from other buckets
			int left = bucket-1;
			int right = bucket+1;
			
			while(_buckets.get(q).get(left) == null && left >= 0) {
				left--;
			}
			
			while(_buckets.get(q).get(right) == null && greaterBucketExists(q, right)) {
				right++;
			}
			
			if(_buckets.get(q).get(left) != null && _buckets.get(q).get(right) != null) {
				double leftpos = (_buckets.get(q).get(left) /_bucketAppearances.get(q).get(left) );
				double rightpos = (_buckets.get(q).get(right) /_bucketAppearances.get(q).get(right) );

				double rightdist = right - bucket;
				double leftdist = bucket - left;
				return (( rightdist * leftpos + leftdist * rightpos) / (leftdist + rightdist));
			}
			else {
				if(_buckets.get(q).get(left) == null) {
					double distance = right - bucket;
					// smaller bucket => larger position
					double rightpos = (_buckets.get(q).get(right) /_bucketAppearances.get(q).get(right) );
					double increment = rightpos / right;

					return (rightpos + (distance * increment));
				}
				else {
					double distance = bucket - left;
					// larger bucket => smaller position
					double leftpos = (_buckets.get(q).get(left) /_bucketAppearances.get(q).get(left) );
					double decrement = leftpos / (_buckets.get(q).size() - left);

					return (leftpos - (distance * decrement));
				}
			}
		}
		else {
			return (sum / appearance);
		}
	}

	@Override
	public double getPrediction(Object[] given) {
		Query q = (Query) given[0];
		Double position = (Double) given[1];
		
		double bid =  getMeanBid(q, position);
		
		return bid;
	}

	@Override
	public void insertPoint(ModelDataPoint mp) {
		_data.add(mp);
	}

	@Override
	public void reset() {
		_buckets.clear();
		_bucketAppearances.clear();
		_data.clear();
		_queryReport = null;
		_bids.clear();
	}

	@Override
	public void train() {
		initBuckets();
		
		for(ModelDataPoint point: _data) {
			Object[] given = point.getGiven();
			Query q = (Query) given[0];
			double position = (Double) given[1];
			double bid = point.getToBePredicted();
			
			updateBuckets(q, position, bid);
			updateBucketAppearance(q, position);
		}
		
	}
	
	public String toString() {
		return "BucketPositionToBidModel";
	}
}
