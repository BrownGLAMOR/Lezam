package models.queryanalyzer.search;


import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;

public class LDSearchHybrid {
	private QAInstance _inst;
	private LDSearchIESmart _avgPosLDS;
	private LDSearchIESmart _carletonLDS;
	private int _iterations;
	private IEResult _best;
	
	public LDSearchHybrid(int maxIterAvgPos, int maxIterCarleton, QAInstance inst){
		_avgPosLDS = new LDSearchIESmart(maxIterAvgPos, inst);
		_carletonLDS = new LDSearchIESmart(maxIterCarleton, inst);
		_inst = inst;
	}
	
	public void search(){
		_avgPosLDS.search(_inst.getAvgPosOrder(), _inst.getAvgPos());
		_carletonLDS.search(_inst.getCarletonOrder(), _inst.getAvgPos());
		
		_iterations = _avgPosLDS.getIterations() + _carletonLDS.getIterations();
		IEResult avgPosResult = _avgPosLDS.getBestSolution();
		IEResult avgPosCarleton = _carletonLDS.getBestSolution();
		
		if(avgPosResult == null && avgPosCarleton == null){
			_best = null;
		} else if(avgPosResult != null && avgPosCarleton == null) {
			_best = avgPosResult;
		} else if(avgPosResult == null && avgPosCarleton != null){
			_best = avgPosCarleton;
		} else {
			if(avgPosResult.getObj() < avgPosCarleton.getObj()){
				_best = avgPosResult;
			} else {
				_best = avgPosCarleton;
			}
		}
		
	}
	
	public int getIterations(){return _iterations;}
	
	public IEResult getBestSolution() {
		//assert(_best != null) : "possibly called before search was run";
		return _best;
	}
}
