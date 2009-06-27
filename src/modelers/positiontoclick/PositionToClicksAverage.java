package modelers.positiontoclick;

import java.util.Hashtable;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class PositionToClicksAverage implements PositionClicksModel {
	private int _slots;
	private Hashtable<Query,Hashtable<Integer,Double>> _positionClicks;
	private Set<Query> _querySpace;
	
	private double continuation;
	private double avgBaseClick;
	private double fTARfPRO;
	private double avgNoConv;
	
	public PositionToClicksAverage(int slots, Set<Query> queries, int searchingUsers){
		//These are average value assumptions that could be very wrong
		continuation = .5;
		avgBaseClick = .5;
		fTARfPRO = 1.2;
		avgNoConv = .8;
		double prClick = eta(avgBaseClick, fTARfPRO);
		
		_slots = slots;
		_positionClicks = new Hashtable<Query,Hashtable<Integer,Double>>();
		_querySpace = queries;
		
		Hashtable<Integer,Double> slotClicks;
		for (Query q : queries){
			slotClicks = new Hashtable<Integer,Double>();
			for (int i = 1; i <= _slots; i++){
				double clicks = clickSlot(i)*((double) searchingUsers)/16.;
				if (clicks<1) clicks = 1; //numClicks has to be at least 1
				slotClicks.put(i, clicks);
			}
			_positionClicks.put(q, slotClicks);
		}
	}
	
	private double eta(double e, double ff){
		return e*ff/(e*ff + (1-e));
	}
	
	private double clickSlot(int slot){
		double prClick = eta(avgBaseClick, fTARfPRO);
		return prClick * Math.pow(continuation*(avgNoConv*prClick + 1 - prClick), slot-1);
	}
	
	public void updateReport(QueryReport queryReport){
		if(queryReport == null){
			return;
		}
		for(Query q : queryReport){
			Hashtable<Integer,Double> slotClicks = _positionClicks.get(q);
			//narsty update method just updates ours or the slot above us's numClicks to be what we saw
			if (((Integer) queryReport.getClicks(q)).doubleValue() >= 1){ //numClicks has to be at least 1
				slotClicks.put( ((Double) queryReport.getPosition(q)).intValue(), 
						((Integer) queryReport.getClicks(q)).doubleValue() );
			}
		}
	}
	
	public int getClicks(Query q, int slot) {
		Hashtable<Integer,Double> slotClicks = _positionClicks.get(q);
		return slotClicks.get(slot).intValue();
	}
	
	public String toString(){
		String toReturn = "";
		for (Query q : _querySpace){
			toReturn += q + "\n";
			for (int i = 1; i <= _slots; i++){
				toReturn += "\tSlot: " + i + ", Clicks: " + getClicks(q,i) + "\n";
			}
		}
		return toReturn;
	}

}
