package simulator.predictions;

import java.util.Arrays;

import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;

public class ResultValues {
	double time;
	SolverType solver;
	private int[][] predictedWaterfall;
	private int predictedTotImpr;
	private int[] predictedImpsPerAgent;
	double objective;
	


	public ResultValues(SolverType solver, int[] predictedImpsPerAgent,
			int[][] predictedWaterfall, int predictedTotImpr, double objective, double time) {
		
		this.objective = objective;
		this.predictedWaterfall = predictedWaterfall; 
		this.predictedTotImpr = predictedTotImpr;
		this.predictedImpsPerAgent = predictedImpsPerAgent;
		this.time = time;
		this.solver = solver;
		
	}

	

	public double getTime() {
		return time;
	}
	
	public SolverType getSolverType() {
		return solver;
	}

	public int[][] getPredictedWaterfall(){
		return predictedWaterfall;
	}

	public int[] getPredictedImpsPerAgent() {
		return predictedImpsPerAgent;
	}

	public double getObjective(){
		return objective;
	}


	public int getTotalImpressions() {
		// TODO Auto-generated method stub
		return predictedTotImpr;
	}



	public int getStat() {
		// TODO Auto-generated method stub
		return -1;
	}



	public int getAbsError() {
		// TODO Auto-generated method stub
		return -1;
	}
	
	public String toString() {
		return "predictedWaterfall=" + Arrays.deepToString(predictedWaterfall) + 
			", predictedTotImpr=" + predictedTotImpr + 
			", predictedImpsPerAgent=" + Arrays.toString(predictedImpsPerAgent);
	}
}
