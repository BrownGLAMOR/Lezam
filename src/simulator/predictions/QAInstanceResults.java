package simulator.predictions;

import java.util.Comparator;
import java.util.HashMap;

import simulator.predictions.ImpressionEstimatorTest.SolverType;

import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.util.LoadData;

public class QAInstanceResults {
	
	private QAInstanceAll inst;
	private QADataAll data;
	private QAInstanceExact instExact;
	private HashMap<SolverType, ResultValues> results = new HashMap<SolverType, ResultValues>();
	private String filename;
	
	public QAInstanceResults(String filename){
		this.filename = filename;
		data = LoadData.LoadIt(filename);
		inst = data.buildInstances(data.getOurAgentNum());
		instExact = data.buildExactInstance(data.getOurAgentNum());
		
	}
	
	public void setResult(SolverType solver, int[] guessedImps, int[] trueImps, double stat, double absError, double time ){
		
		ResultValues res = new ResultValues(guessedImps, trueImps, stat, absError, time);
		getResults().put(solver, res);
		
		
	}
	
	public QAInstanceAll getQAInstAll(){
		return inst;
	}
	
	public QAInstanceExact getQAInstExact(){
		return instExact;
	}
	
	public QADataAll getQADataAll(){
		return data;
	}
	public int[] getGuessedImps(SolverType solvers){
		return getResults().get(solvers).guessedImps;
	}
	
	public int[] getTrueImps(SolverType solver){
		return getResults().get(solver).trueImps;
	}
	
	public double getStat(SolverType solver){
		return getResults().get(solver).stat;
	}
	
	public double getAbsError(SolverType solver){
		return getResults().get(solver).absError;
	}
	
	
	public double getTime(SolverType solver){
		return getResults().get(solver).time;
	}
	
	public String getFilename(){
		return filename;
	}
	
	public void setResults(HashMap<SolverType, ResultValues> results) {
		this.results = results;
	}

	public HashMap<SolverType, ResultValues> getResults() {
		return results;
	}

	public class CompareBySolver implements Comparator<QAInstanceResults>{
		
		SolverType solver;
		
		public CompareBySolver(SolverType solver){
			this.solver = solver;
		}
		
		@Override
		public int compare(QAInstanceResults result1, QAInstanceResults result2) {
			if(result1.getResults().get(solver).getStat()>result2.getResults().get(solver).getStat()){
				return -1;
			}else if(result1.getResults().get(solver).getStat()<result2.getResults().get(solver).getStat()){
				return 1;
			}else{
				return 0;
			}
		}

		
	}

}
