package simulator.predictions;

import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;
import java.util.Comparator;


public class CompareTimeResults implements Comparator<QAInstanceResults> {

	SolverType solver;
	
	public CompareTimeResults(SolverType solver){
		this.solver = solver;
	}
	
	@Override
	public int compare(QAInstanceResults result1, QAInstanceResults result2) {
		if(result1.getResults().get(solver).getTime()>result2.getResults().get(solver).getTime()){
			return 1;
		}else if(result1.getResults().get(solver).getTime()<result2.getResults().get(solver).getTime()){
			return -1;
		}else{
			return 0;
		}
	}

	
}
