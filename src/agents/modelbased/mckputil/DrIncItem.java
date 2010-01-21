package agents.modelbased.mckputil;

public class DrIncItem extends IncItem{
	
	private int _day;

	public DrIncItem(double w, double v, Item itemHigh, Item itemLow, int day) {
		super(w,v,itemHigh,itemLow);
		_day = day;
	}
	
	public int day() {
		return _day;
	}
	
	public String toString() {
		return _itemHigh.q()+"day: " + _day +", [W: " + _w + ", V: " + _v + ", E: " + (_v/_w) + ", B: " + _itemHigh.b() + "]";
	}

}