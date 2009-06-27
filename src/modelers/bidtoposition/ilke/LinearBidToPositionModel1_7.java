package modelers.bidtoposition.ilke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/**
 * bid = lastBid + 1/6*lastBid*pos - 1/6*lastBid*lastPos
 *pos =  ((1/6 * lastBid * lastPod) + bid - lastBid) / (1/6 * lastBid)
 */
public class LinearBidToPositionModel1_7 extends BidToPositionModel {
	static double POS_DISTANCE = (1.0/7);
	protected HashMap<Query, Double[]> _data;
	protected ArrayList<BidBundle> _bids;
	protected HashMap<Query, Double> _lastbids;
	protected HashMap<Query, Double> _lastpositions;
	protected QueryReport _queryReport;
	protected int _slots;
	protected double _noposition;
	
	public LinearBidToPositionModel1_7(Set<Query> space, int slots) {
		_data = new HashMap<Query, Double[]>();
		_slots = slots;
		_noposition = slots+1;
		_bids = new ArrayList<BidBundle>();
		
		_lastbids = new HashMap<Query, Double>();
		_lastpositions = new HashMap<Query, Double>();
		for(Query q: space) {
			_lastbids.put(q, Double.NaN);
			_lastpositions.put(q, Double.NaN);
		}
	}

	@Override
	public double getPosition(Query q, double bid) {
		//pos =  ((1/6 * lastBid * lastPos) + lastBid - bid) / (1/6 * lastBid)
		double lastBid = _lastbids.get(q);
		double lastPos = _lastpositions.get(q);
		
		double nom = (POS_DISTANCE * lastBid * lastPos) + lastBid - bid;
		double denom = POS_DISTANCE * lastBid;
		
		double position = (nom / denom);
		
		if(position < 1) {
			position = 1;
		}
		else if(position >= _noposition) {
			position = Double.NaN;
		}
		
		return position;
	}

	@Override
	public void updateBidBundle(BidBundle bidBundle) {
		_bids.add(bidBundle);
	}

	protected void mapBidsToReportedPositions() {
		BidBundle bidBundle = _bids.remove(0);
		
		for(Query q: _queryReport) {
			double bid = bidBundle.getBid(q);
			double position = _queryReport.getPosition(q);
			
			if(_data.get(q) == null) {
				Double[] bidtopos = new Double[2];
				bidtopos[0] = bid;
				bidtopos[1] = position;
				_data.put(q, bidtopos);
			}
			else {
				_data.get(q)[0] = bid;
				_data.get(q)[1] = position;
			}
		}
	}
	
	@Override
	public void updateQueryReport(QueryReport queryReport) {
		_queryReport = queryReport;
		
		mapBidsToReportedPositions();
	}

	@Override
	public double getPrediction(Object[] given) {
		Query q = (Query) given[0];
		double bid = (Double) given[1];
		
		double lastBid = _data.get(q)[0];
		double lastPos = _data.get(q)[1];
		
		double nom = (POS_DISTANCE * lastBid * lastPos) + lastBid - bid;
		double denom = POS_DISTANCE * lastBid;
		
		double position = (nom / denom);
		
		if(position < 1) {
			position = 1;
		}
	
		return position;
	}

	@Override
	public void insertPoint(ModelDataPoint mp) {
		Object[] given = mp.getGiven();
		Query q = (Query) given[0];
		double bid = (Double) given[1];
		
		double position = mp.getToBePredicted();
		if(_data.get(q) == null) {
			Double[] bidtopos = new Double[2];
			bidtopos[0] = bid;
			bidtopos[1] = position;
			_data.put(q, bidtopos);
		}
		else {
			_data.get(q)[0] = bid;
			_data.get(q)[1] = position;
		}
	}

	@Override
	public void reset() {
		_bids.clear();
		_lastbids.clear();
		_lastpositions.clear();
		_data.clear();
	}

	@Override
	public void train() {
		// this model requires no training
	}

	public String toString() {
		return "LinearBidToPositionModel1_7";
	}
}
