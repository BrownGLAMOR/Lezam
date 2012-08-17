package simulator.predictions;

import simulator.predictions.ImpressionEstimatorTest.SolverType;
import java.util.Comparator;


public class CompareSolverResults implements Comparator<QAInstanceResults> {

	SolverType solver;
	String value;
	
	public CompareSolverResults(SolverType solver, String value){
		this.solver = solver;
		this.value = value;
	}
	
	@Override
	public int compare(QAInstanceResults result1, QAInstanceResults result2) {
		if(value.compareToIgnoreCase("stat")==0){
			if(result1.getResults().get(solver).getStat()>result2.getResults().get(solver).getStat()){
				return 1;
			}else if(result1.getResults().get(solver).getStat()<result2.getResults().get(solver).getStat()){
				return -1;
			}else{
				return 0;
			}
		}else{
			if(result1.getResults().get(solver).getAbsError()>result2.getResults().get(solver).getAbsError()){
				return 1;
			}else if(result1.getResults().get(solver).getAbsError()<result2.getResults().get(solver).getAbsError()){
				return -1;
			}else{
				return 0;
			}
		}
	}
	
}
