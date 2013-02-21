package models.usermodel;

public class UserModelInput {
	
	double[] _convProbs;
	int _numSlots;
	int _promSlots;
	
	public UserModelInput() {
		
	}
	
	public UserModelInput(double[] convProbs, int numSlots, int promSlots) {
		_convProbs = convProbs;
		_numSlots = numSlots;
		_promSlots = promSlots;
	}
	
	public double[] getConversionProbs() {
		return _convProbs;
	}
	
	public int getNumSlots() {
		return _numSlots;
	}
	
	public int getPromSlots() {
		return _promSlots;
	}

}
