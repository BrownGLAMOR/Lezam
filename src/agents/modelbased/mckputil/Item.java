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
	
	int _dayFromNow;
	int numTimesTaken = 0;
	
	
	boolean _hasBeenTaken = false;
	boolean _currentlyTaken = false;
	boolean inOpt = false;
	
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
	
	public Item(Query q, double weight, double value, double bid, double budget, boolean targ) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
	
	}
	public Item(Query q, int day, double weight, double value, double bid, double budget, boolean targ) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
		_dayFromNow = day;
	
	}
	public Item(Query q, double weight, double value, double bid, double budget, boolean targ, boolean taken) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
		_currentlyTaken = taken;
		_hasBeenTaken = taken;
	
	}
	
	public Item(Query q, int day, double weight, double value, double bid, double budget, boolean targ, boolean taken) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
		_currentlyTaken = taken;
		_hasBeenTaken = taken;
		_dayFromNow = day;
	
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
	
	public Item(Query q, double weight, double value, double bid, double budget, boolean targ, int isID, int idx, int dayFromNow) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = budget;
		_targ = targ;
		_isID = isID;
		_idx = idx;
		_dayFromNow = dayFromNow;
	}
	
	/*
	 * TODO:
	 * 
	 * We should get rid of this and allow all agents to set budgets in items, maybe?
	 */
	public Item(Query q, double weight, double value, double bid, boolean targ, int isID, int idx, int dayFromNow) {
		_q = q;
		_weight = weight;
		_value = value;
		_bid = bid;
		_budget = Double.MAX_VALUE;
		_targ = targ;
		_isID = isID;
		_idx = idx;
		_dayFromNow = dayFromNow;
	}
	
	public int dayFromNow(){
		return _dayFromNow;
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
		return _q+" Day "+_dayFromNow+" [W: " + _weight + ", V: " + _value + ", B: " + _bid + " Budget: " + _budget + ", T: " + _targ + " taken?: "+_currentlyTaken+"]";
	}

	public boolean compareTo(Item item) {
		boolean equal = true;
		if(_dayFromNow==UNDEFINED){
		if (_q!= item.q()||_weight!= item.w()||_value!= item.v()||_bid!= item.b()||_budget!= item.budget()||_targ!= item.targ()){
			equal = false;
		}
		}else{
			if (_q!= item.q()||_weight!= item.w()||_value!= item.v()||_bid!= item.b()||_budget!= item.budget()||_targ!= item.targ()||_dayFromNow!=item.dayFromNow()){
				equal = false;
			}
		}
		
		return equal;
	}
	
	//The following methods were written to handle the PMCKP algorithms
	
	public boolean hasBeenTaken(){
		return _hasBeenTaken;
	}

	public boolean isCurrentlyTaken(){
		return _currentlyTaken;
	}
	
	public void setCurrentlyTaken(boolean toSet){
		_currentlyTaken = toSet;
	}
	
	public void setHasBeenTaken(){
		numTimesTaken++;
		if(numTimesTaken>0){
		_hasBeenTaken = true;
		}
	}
	
	public void setHasBeenTaken(boolean value){
		
		_hasBeenTaken = value;
		
	}
	
	public int getDaysFromNow(){
		return _dayFromNow;
	}

	public void setInOpt() {
		inOpt = true;
		
	}
	

}