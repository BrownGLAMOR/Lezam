package modelers;

public class UnitsSoldModelMax extends UnitsSoldModel {

	public UnitsSoldModelMax(int distributionWindow) {
		super(distributionWindow);
	}

	@Override
	protected int updateLastDaySold() {
		int max = 0;

		for(int val : _sold){
			if(val > max){
				max = val;
			}
		}
		
		return max;
	}
	
}
