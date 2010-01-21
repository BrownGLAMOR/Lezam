package carleton.oldmodels.unitssold;

import java.util.Iterator;

public class UnitsSoldModelMeanWindow extends UnitsSoldModelBasic {

	public UnitsSoldModelMeanWindow(int distributionWindow) {
		super(distributionWindow);
	}

	@Override
	protected int updateLastDaySold() {
		int sum = 0;

		Iterator<Integer> iter = buildWindowIterator();
		while(iter.hasNext()){
			int val = iter.next();
			sum += val;
		}
		
		return sum/(_distributionWindow-1);
	}
	
}
