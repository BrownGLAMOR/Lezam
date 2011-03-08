package models.queryanalyzer.iep;

import java.util.Arrays;

public class IEResult {
	private int[] _order;
	private int[] _sol; //numImps each agent saw
	private int[] _slotImpr; //numImps in each slot
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		//sb.append("Result:\n\tobj=" + _obj + "\n\tsol=" + Arrays.toString(_sol) + "\n\torder=" + Arrays.toString(_order) + "\n\tslotImpr=" + Arrays.toString(_slotImpr));
		sb.append("Result:\tobj=" + _obj + "\tsol=" + Arrays.toString(_sol));
		return sb.toString();
		
	}
}
