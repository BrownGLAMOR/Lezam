package modelers.bidtoposition.ilke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class LinearPositionToBidModel1_4 extends PositionToBidModel {
	static double POS_DISTANCE = (1.0/4);
	protected HashMap<Query, Double[]> _data;
	protected ArrayList<BidBundle> _bids;
	protected HashMap<Query, Double> _lastbids;
	protected HashMap<Query, Double> _lastpositions;
	protected QueryReport _queryReport;
	protected int _slots;
	protected double _noposition;
	
	public LinearPositionToBidModel1_4(Set<Query> space, int slots) {
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
	public double getBid(Query q, double position) {
		// * bid = lastBid + 1/6*lastBid*pos - 1/6*lastBid*lastPos
		double lastBid = _lastbids.get(q);
		double lastPos = _lastpositions.get(q);
		
		double bid = lastBid + (POS_DISTANCE * lastBid * (lastPos - position));
		
		return bid;
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
				Double[] postobid = new Double[2];
				postobid[0] = position;
				postobid[1] = bid;
				_data.put(q, postobid);
			}
			else {
				_data.get(q)[0] = position;
				_data.get(q)[1] = bid;
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
		double position = (Double) given[1];
		
		double lastPos = _data.get(q)[0];
		double lastBid = _data.get(q)[1];
		
		double bid = lastBid + (POS_DISTANCE * lastBid * (lastPos - position));
		
		return bid;
	}

	@Override
	public void insertPoint(ModelDataPoint mp) {
		Object[] given = mp.getGiven();
		Query q = (Query) given[0];
		double position = (Double) given[1];
		
		double bid = mp.getToBePredicted();
		if(_data.get(q) == null) {
			Double[] postobid = new Double[2];
			postobid[0] = position;
			postobid[1] = bid;
			_data.put(q, postobid);
		}
		else {
			_data.get(q)[0] = position;
			_data.get(q)[1] = bid;
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
		return "LinearBidToPositionModel1_4";
	}
}
