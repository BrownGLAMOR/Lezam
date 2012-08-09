package models.queryanalyzer.riep.search;


import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;

public class LDSearchHybrid {
	private AbstractQAInstance _inst;
	private LDSearchIESmart _avgPosLDS;
	private LDSearchIESmart _carletonLDS;
	private int _iterations;
	private IEResult _best;
	
	public LDSearchHybrid(int maxIterAvgPos, int maxIterCarleton, AbstractImpressionEstimator ie){
		_avgPosLDS = new LDSearchIESmart(maxIterAvgPos, ie);
		_carletonLDS = new LDSearchIESmart(maxIterCarleton, ie);
		_inst = ie.getInstance();
	}
	
	public void search(){
		_avgPosLDS.search(QAInstanceAll.getAvgPosOrder(_inst.getAvgPos()), _inst.getAvgPos());
		_carletonLDS.search(QAInstanceAll.getCarletonOrder(_inst.getAvgPos(), _inst.getNumSlots()), _inst.getAvgPos());
		
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
