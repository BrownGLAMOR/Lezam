package modelers;

public class OptimalCapacity {
	
	private int _maxCap;
	private int _capWindow;
	private double _USP;
	private double _lam;
	
	public OptimalCapacity(int cap, int capWindow, double lambda, double USP) {
		_maxCap = cap;
		_capWindow = capWindow;
		_lam = lambda;
		_USP = USP;
	}
	
	public int getDailyOptimal(int optCap, int[] conv) {
		assert _capWindow == conv.length : "capWindow = " + _capWindow + "  conv.length = " + conv.length;
		int totConv = 0;
		/*
		 * Index 0 is the oldest day of conversions that we have.  So in order to calculate
		 * how many conversions we want tomorrow, we total how many we have had in
		 * the last _capWindow - 1 days and then determine from our optimal capacity
		 * how many more conversions we need to get today
		 */
		for(int i = 1; i < _capWindow; i++) {
			totConv += conv[i];
		}
		return optCap-totConv;
	}
	
	public int getOptimalCap(double CPC, double convRate) {
		int i = 1;
		/*
		 * Since we have a big sum, we only need to check of when the
		 * terms start to become negative.  Once a term is negative
		 * we know that the maximum was the term before it
		 */
		while(true) {
			double test = _USP - CPC/(convRate * Math.pow(_lam, i));
			if(test < 0) {
				i--;
				break;
			}
		}
		return _maxCap + i;
	}

}
