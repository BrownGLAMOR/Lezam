package models.queryanalyzer.iep;

public class IEResult {
	private int[] _order;
	private int[] _sol;
	private int[] _slotImpr;
	private int _obj;
	
	public IEResult(int obj, int[] sol, int[] order, int[] slotImpr){
		_obj = obj;
		_sol = sol;
		_order = order;
		_slotImpr = slotImpr;
	}
	
	public int getObj() {return _obj;}
	public int[] getSol() {return _sol;}
	public int[] getOrder() {return _order;}
	public int[] getSlotImpressions() {return _slotImpr;}
}
