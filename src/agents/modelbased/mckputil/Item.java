package agents.modelbased.mckputil;

import edu.umich.eecs.tac.props.Query;

public class Item {
	Query _q;
	double _weight;
	double _value;
	double _bid;//bid
	double _budget;//bid
	int _isID;//item set id (i.e., id for the query)
	boolean _targ;
	int _idx;
	public static int UNDEFINED = -1;
	
	public Item(Query q, double weight, double value, double bid, double budget, boolean targ, int isID, int idx) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
		_isID = isID;
		_idx = idx;
	}
	
	/*
	 * TODO:
	 * 
	 * We should get rid of this and allow all agents to set budgets in items, maybe?
	 */
	public Item(Query q, double weight, double value, double bid, boolean targ, int isID, int idx) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = Double.MAX_VALUE;
		_targ = targ;
		_isID = isID;
		_idx = idx;
	}
	
	public double w() {
		return _weight;
	}

	public double v() {
		return _value;
	}

	public double b() {
		return _bid;
	}
	
	public double budget() {
		return _budget;
	}
	
	public boolean targ() {
		return _targ;
	}
	
	public int isID() {
		return _isID;
	}
	
	public int idx() {
		return _idx;
	}
	
	public Query q() {
		return _q;
	}
	
	public String toString() {
		return _q+" [W: " + _weight + ", V: " + _value + ", B: " + _bid + "Budget: " + _budget + ", T: " + _targ + "]";
	}

}