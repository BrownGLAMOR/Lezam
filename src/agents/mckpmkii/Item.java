package agents.mckpmkii;

import edu.umich.eecs.tac.props.Query;

public class Item {
	Query _q;
	double _w;
	double _v;
	double _b;//bid
	int _isID;//item set id (i.e., id for the query)
	private int _numConv;
	public static int UNDEFINED = -1;
	
	public Item(Query q, double w, double v, double b, int numConv, int isID) {
		_q = q;
		_w = w;
		_v = v;
		_b = b;
		_numConv = numConv;
		_isID = isID;
	}
	
	public Item(Query q, double w, double v, double b, int isID) {
		_q = q;
		_w = w;
		_v = v;
		_b = b;
		_isID = isID;
	}

	public Item(double w, double v) {
		_w = w;
		_v = v;
		_b = UNDEFINED;
		_isID = UNDEFINED;
	}

	
	public double w() {
		return _w;
	}

	public double v() {
		return _v;
	}
	
	public int numConv() {
		return _numConv;
	}

	public double b() {
		return _b;
	}
	
	public int isID() {
		return _isID;
	}
	
	public Query q() {
		return _q;
	}
	
	public String toString() {
		return _q+" [W: " + _w + ", V: " + _v + ", B: " + _b + "]";
	}

}