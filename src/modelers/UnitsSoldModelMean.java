package modelers;

public class UnitsSoldModelMean extends UnitsSoldModel {

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
