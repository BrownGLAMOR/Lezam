package agents.mckpmkii;

public class IncItem implements Comparable<IncItem>{
	Item _item;
	double _w;
	double _v;
	private int _numConv;
	
	
	public IncItem(double w, double v, int numConv, Item item) {
		_item = item;
		_w = w;
		_v = v;
		_numConv = numConv;
	}
	
	public IncItem(double w, double v, Item item) {
		_item = item;
		_w = w;
		_v = v;
	}
	
	public Item item() {
		return _item;
	}
	
	public double w() {
		return _w;
	}

	public double v() {
		return _v;
	}
	
	public double numConv() {
		return _numConv;
	}

	public double eff() {
		return _v/_w;
	}
	
	/*
	 * sorts in DESCREASING order of EFFICIENCY
	 */
	public int compareTo(IncItem  item) {
		int result;
		if(eff() < item.eff()) {
			result = 1;
		}else if(eff() > item.eff()) {
			result = -1;
		}else{
			result = 0;
		}
		//Misc.println(this + " <?> " + item + " result " + result);
		return result;
	}
	
	public String toString() {
		return _item.q()+" [W: " + _w + ", V: " + _v + ", B: " + _item.b() + "]";
	}

}