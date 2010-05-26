package models.queryanalyzer.search;
import java.util.Set;

public class LDSPerm implements Comparable<LDSPerm>{
	int _value;
	int[] _perm;
	Set<LDSSwap> _swapped;
	
	public LDSPerm(int value, int[] perm, Set<LDSSwap> swapped){
		_value = value;
		_perm = perm;
		_swapped = swapped;
	}
	
	public LDSPerm(int[] perm, Set<LDSSwap> swapped){
		_value = swapped.size();
		_perm = perm;
		_swapped = swapped;
	}

	public int compareTo(LDSPerm o) {
		if(o._value < _value)
			return 1;
		if(o._value > _value)
			return -1;
		return 0;
	}

	public int getVal() {
		return _value;
	}
}

