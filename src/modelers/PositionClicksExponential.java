package modelers;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class PositionClicksExponential implements PositionClicksModel {
	protected double _interpolation;
	protected int _slots;
	protected LinkedList<Hashtable<Query,Double>> _position;
	protected LinkedList<Hashtable<Query,Double>> _clicks;
	protected Hashtable<Query,Hashtable<Integer,Double>> _positionClicks;
	
	public PositionClicksExponential(int slots, double interpolation){
		_slots = slots;
		_position = new LinkedList<Hashtable<Query,Double>>();
		_clicks = new LinkedList<Hashtable<Query,Double>>();
		_positionClicks = new Hashtable<Query,Hashtable<Integer,Double>>();
		
		_interpolation = interpolation;
	}
	
	public void updateReport(QueryReport queryReport){
		Hashtable<Query,Double> positionsToday = new Hashtable<Query,Double>();
		Hashtable<Query,Double> clicksToday = new Hashtable<Query,Double>();
		_position.add(positionsToday);
		_clicks.add(clicksToday);
		if(queryReport == null){
			return;
		}
			
		
		
		for(Query q : queryReport){
			Hashtable<Integer,Double> queryPositionClicks = new Hashtable<Integer,Double>();
			double position = queryReport.getPosition(q);
			double clicks = queryReport.getClicks(q);
			
			positionsToday.put(q, position);
			clicksToday.put(q, clicks); 
			
			if(!Double.isNaN(position)){
				int pos = (int)position;
				queryPositionClicks.put(pos, clicks);
				for(int i = pos-1; i >= 1; i--){
					queryPositionClicks.put(i, clicks*Math.pow(_interpolation, pos-i));
				}
				for(int i = pos+1; i <= _slots; i++){
					queryPositionClicks.put(i, clicks*Math.pow(_interpolation, pos-i));
				}
				
				System.out.println();
				System.out.println(q+" Position Clicks Given : "+clicks+" in position "+pos);
				double[] slotclicks = new double[_slots];
				for(int i = 0; i < _slots; i++){
					slotclicks[i] = queryPositionClicks.get(i+1);
				}
				System.out.println(Arrays.toString(slotclicks));
				_positionClicks.put(q, queryPositionClicks);
			}
			
		}
		
	}
	
	public int getClicks(Query q, int slot) {
		if(_positionClicks.containsKey(q) && _positionClicks.get(q).containsKey(slot)){
			double clicks = _positionClicks.get(q).get(slot);
			return (int)clicks;
		}
				
		return 1;
	}

}
