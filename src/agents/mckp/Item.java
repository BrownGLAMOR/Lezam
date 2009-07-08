package agents.mckp;

import edu.umich.eecs.tac.props.Query;

public class Item {
	Query _q;
	double _w;
	double _v;
	double _b;//bid
	int _isID;//item set id (i.e., id for the query)
	private boolean _targ;
	public static int UNDEFINED = -1;
	
	public Item(Query q, double w, double v, double b, boolean targ, int isID) {
		_q = q;
		_w = w;
		_v = v;
		_b = b;
		_targ = targ;
		_isID = isID;
	}
	
	public Item(Query q, double w, double v, double b, int isID) {
		this(q,w,v,b,false,isID);
	}

	public Item(double w, double v) {
		_w = w;
		_v = v;
		_b = UNDEFINED;
		_targ = false;
		_isID = UNDEFINED;
	}

	
	public double w() {
		return _w;
	}

	public double v() {
		return _v;
	}

	public double b() {
		return _b;
	}
	
	public boolean targ() {
		return _targ;
	}
	
	public int isID() {
		return _isID;
	}
	
	public Query q() {
		return _q;
	}
	
	public String toString() {
		return _q+" [W: " + _w + ", V: " + _v + ", B: " + _b + ", T: " + _targ + "]";
	}

}