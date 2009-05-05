package modelers.bidtoposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import modelers.ModelPoint;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class BucketBidToPositionModel extends BidToPositionModel {
	static double BUCKET_SIZE = 0.40;
	protected int _slots;
	protected double _noposition;
	protected ArrayList<ModelPoint> _data;
	protected HashMap<Query, HashMap<Integer, Double>> _buckets;
	protected HashMap<Query, HashMap<Integer, Integer>> _bucketAppearances;
	protected ArrayList<BidBundle> _bids;
	protected QueryReport _queryReport;
	protected Set<Query> _queries;
	
	
	public BucketBidToPositionModel(Set<Query> queries, int slots) {
		_slots = slots;
		_noposition = _slots + 1;		
		_bids = new ArrayList<BidBundle>();
		_data = new ArrayList<ModelPoint>();
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
	
	protected void updateBucketAppearance(Query q, double bid) {
		int bucketnum = getBucket(bid);

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
	public double getPosition(Query q, double bid) {
		double position =  getMeanPosition(q, bid);
		if(position < 1) {
			position = 1.0;
		}
		else if(position > _slots){
			position = Double.NaN;
		}
		return position;
	}
	
	protected void updateBuckets(Query q, double bid, double position) {
		int bucket = getBucket(bid);
		Double bucketSum = _buckets.get(q).get(bucket);
		
		if(bucketSum == null) {
			_buckets.get(q).put(bucket, position);
		}
		else {
			_buckets.get(q).put(bucket, bucketSum.doubleValue() + position);
		}
	}
	
	protected void mapBidsToReportedPositions() {
		BidBundle bids = _bids.remove(0);
		
		for(Query q: _queryReport) {
			double reportedPos = _queryReport.getPosition(q);
			if(reportedPos == Double.NaN) {
				reportedPos = _noposition;
			}
			Object[] given = new Object[2];
			given[0] = q;
			given[1] = bids.getBid(q);
			
			ModelPoint mp = new ModelPoint(given, reportedPos);
			
			insertPoint(mp);
		}	
	}
	public void updateQueryReport(QueryReport queryReport) {
		_queryReport = queryReport;
		
		mapBidsToReportedPositions();	
	}
	
	public int getBucket(double bid) {
		int bucket = (int) (bid / BUCKET_SIZE);
		return bucket;
	}
	
	protected boolean greaterBucketExists(Query q, int bucket) {
		for(double b: _buckets.get(q).keySet()) {
			if(b > bucket)
				return true;
		}
		return false;
	}
	
	protected double getMeanPosition(Query q, double bid) {
		int bucket = getBucket(bid);
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
		Double bid = (Double) given[1];
		
		double position =  getMeanPosition(q, bid);
		
		return position;
	}

	@Override
	public void insertPoint(ModelPoint mp) {
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
		
		for(ModelPoint point: _data) {
			Object[] given = point.getGiven();
			Query q = (Query) given[0];
			double bid = (Double) given[1];
			double position = point.getToBePredicted();
			
			updateBuckets(q, bid, position);
			updateBucketAppearance(q, bid);
		}
		
	}
}
