package models.queryanalyzer.iep;

public class IEResult {
	private int[] _order;
	private int[] _sol;
	private int _obj;
	
	public IEResult(int obj, int[] sol, int[] order){
		_obj = obj;
		_sol = sol;
		_order = order;
	}
	
	public int getObj() {return _obj;}
	public int[] getSol() {return _sol;}
	public int[] getOrder() {return _order;}
}
