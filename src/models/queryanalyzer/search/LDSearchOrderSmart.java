package models.queryanalyzer.search;

public class LDSearchOrderSmart extends LDSearchSmart {
	int[] _goalPerm;
	
	public LDSearchOrderSmart(int[] goalPerm, int slots){
		super(slots);
		_goalPerm = goalPerm;
	}
	
	@Override
	protected boolean evalPerm(int[] perm) {
		assert(perm.length == _goalPerm.length);
		for(int i=0; i < perm.length; i++){
			if(perm[i] != _goalPerm[i]){
				return false;
			}
		}
		return true;
	}

}
