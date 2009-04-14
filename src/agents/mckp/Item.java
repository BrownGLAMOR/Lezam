package agents.mckp;

public class Item {
	double _w;
	double _v;
	double _b;//bid
	int _isID;//item set id (i.e., id for the query)
	
	public Item(double w, double v, double b, int isID) {
		_w = w;
		_v = v;
		_b = b;
		_isID = isID;
	}
	
	public double w() {
		return _w;
	}

	public double v() {
		return _v;
	}
	
	public int isID() {
		return _isID;
	}

}