package modelers;

import java.util.Iterator;

public class UnitsSoldModelMaxWindow extends UnitsSoldModelBasic {

	public UnitsSoldModelMaxWindow(int distributionWindow) {
		super(distributionWindow);
	}

	@Override
	protected int updateLastDaySold() {
		int max = 0;
		Iterator<Integer> iter = buildWindowIterator();
		while(iter.hasNext()){
			int val = iter.next();
			if(val > max){
				max = val;
			}
		}
		
		return max;
	}
	
}
