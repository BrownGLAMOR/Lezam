package agents.mckp;

public class IncItem implements Comparable<IncItem>{
	Item _itemHigh,_itemLow;
	double _w;
	double _v;
	
	public IncItem(double w, double v, Item itemHigh, Item itemLow) {
		_itemHigh = itemHigh;
		_itemLow = itemLow;
		_w = w;
		_v = v;
	}
	
	public Item item() {
		return _itemHigh;
	}
	
	public Item itemHigh() {
		return _itemHigh;
	}
	
	public Item itemLow() {
		return _itemLow;
	}
	
	public double w() {
		return _w;
	}

	public double v() {
		return _v;
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
		return _itemHigh.q()+" [W: " + _w + ", V: " + _v + ", E: " + (_v/_w) + ", B: " + _itemHigh.b() + "]";
	}

}