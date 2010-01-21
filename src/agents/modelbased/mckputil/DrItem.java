package agents.modelbased.mckputil;

import edu.umich.eecs.tac.props.Query;

public class DrItem extends Item {
	
	private int _day;

	public DrItem(Query q, double w, double v, double b, boolean targ, int isID, int idx, int day) {
		super(q,w,v,b,targ,isID,idx);
		_day = day;
	}
	
	public DrItem(Query q, double w, double v, double b, int isID, int idx, int day) {
		this(q,w,v,b,false,isID,idx,day);
	}
	
	public int day() {
		return _day;
	}
	
	public String toString() {
		return _q+"day: " + _day +", [W: " + _w + ", V: " + _v + ", B: " + _b + ", T: " + _targ + "]";
	}

}