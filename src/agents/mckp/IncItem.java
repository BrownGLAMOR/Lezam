package agents.mckp;


public class IncItem implements Comparable<IncItem>{
	Item _item;
	double _w;
	double _v;
	
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

	double eff() {
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

}