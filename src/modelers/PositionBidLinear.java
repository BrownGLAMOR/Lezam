package modelers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class PositionBidLinear implements PositionBidModel{
	protected double _interpolation;
	protected int _slots;
	//protected LinkedList<Hashtable<Query,Double>> _position;
	//protected LinkedList<Hashtable<Query,Double>> _bid;
	protected Hashtable<Query,Hashtable<Integer,Double>> _positionBid;
	
	public PositionBidLinear(int slots, double interpolation){
		_slots = slots;
		_positionBid = new Hashtable<Query,Hashtable<Integer,Double>>();
		_interpolation = interpolation;
	}
	
	public void updateReport(QueryReport queryReport, HashMap<Query, Double> lastBids){
		if(queryReport == null){
			return;
		}
		
		for(Query q : queryReport){
			Hashtable<Integer,Double> queryPositionBid = new Hashtable<Integer,Double>();
			double position = queryReport.getPosition(q);
			double bid = lastBids.get(q);//this is a hack, should be our actual bid.
			if (!((Double) queryReport.getCPC(q)).isNaN()){
				_interpolation = bid - queryReport.getCPC(q); //make a smarter interpolation
			}
			
			if(!Double.isNaN(position)){
				int pos = (int)position;
				queryPositionBid.put(pos, bid);
				
				for(int i = pos-1; i >= 1; i--){
					queryPositionBid.put(i, bid+_interpolation*(pos-i));
				}
				for(int i = pos+1; i <= _slots; i++){
					queryPositionBid.put(i, bid-_interpolation*(i-pos));
				}
				
				for(int i = 1; i <= _slots; i++){
					if(queryPositionBid.get(i) < 0){
						queryPositionBid.put(i, 0.0);
					}
				}
				
				_positionBid.put(q, queryPositionBid);
				
				double[] slotbids = new double[_slots];
				for(int i = 0; i < _slots; i++){
					slotbids[i] = queryPositionBid.get(i+1);
				}
				System.out.println(Arrays.toString(slotbids));
				
				
			}
			
			//Keep the values the same if the last bid didn't get a position...
		}
		
	}
	
	public double getBid(Query q, int slot) {
		if(_positionBid.containsKey(q) && _positionBid.get(q).containsKey(slot)){
			return _positionBid.get(q).get(slot);
		}
				
		return 1;
	}
	
	public double getCPC(Query q, int slot) {
		if(_positionBid.containsKey(q) && _positionBid.get(q).containsKey(slot)){
			if (slot == _slots){
				double poss = _positionBid.get(q).get(slot) - _interpolation;
				if (poss < 0){
					return 0;
				}
				return poss;
			}
			return _positionBid.get(q).get(slot+1);
		}
				
		return .5;
	}

}
