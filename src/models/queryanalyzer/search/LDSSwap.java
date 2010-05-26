package models.queryanalyzer.search;

public class LDSSwap implements Comparable<LDSSwap> {
	int _a1;
	int _a2;
	
	public LDSSwap(int a1, int a2){
		_a1 = a1;
		_a2 = a2;
	}

	public int compareTo(LDSSwap o) {
		if((_a1 == o._a1 && _a2 == o._a2)){
			return 0;
		}
		return 1;
	}
	
}
