package models.queryanalyzer.search;

public class LDSearchOrder extends LDSearch {
	int[] _goalPerm;
	
	public LDSearchOrder(int[] goalPerm){
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
