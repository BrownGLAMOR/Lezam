package modelers.unitssold;

public class UnitsSoldModelMean extends UnitsSoldModelBasic {

	public UnitsSoldModelMean(int distributionWindow) {
		super(distributionWindow);
	}

	@Override
	protected int updateLastDaySold() {
		int sum = 0;

		for(int val : _sold){
			sum += val;
		}
		
		return sum/_sold.size();
	}
	
}
