package newmodels.bidtopos;

import java.util.HashMap;

public class BidToPositionHistogram {
	protected HashMap<Integer, Integer> _buckets;
	protected int _totalTimesSeen;
	protected static double BUCKET_SIZE = 1;
	protected double alpha = 0; // for smoothing
	
	public BidToPositionHistogram() {
		_buckets = new HashMap<Integer, Integer>();
		_totalTimesSeen = 0;
	}
	
	public void addReportedPosition(double position) {
		int bucket = getBucket(position);
		
		if(_buckets.get(bucket) == null) {
			_buckets.put(bucket, 1);
		}
		else {
			_buckets.put(bucket, _buckets.get(bucket) + 1);
		}
 		
		_totalTimesSeen++;
	}
	
	public double getProbability(double position) {
		int bucket = getBucket(position);
		Integer timesSeen = _buckets.get(bucket);
		
		if(timesSeen == null) {
			timesSeen = 0;
		}
		
		double nominator = timesSeen + alpha;
		double denominator = _totalTimesSeen + ((_buckets.keySet().size() + 1) * alpha);
		System.out.println("Times: " + timesSeen + "\tTotal:" + _totalTimesSeen);
		return (nominator / denominator);
	}
	
	protected int getBucket(double position) {
		return (int) (position / BUCKET_SIZE);
	}
}
