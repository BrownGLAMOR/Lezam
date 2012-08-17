package simulator.predictions;

public class ResultValues {
	int[] guessedImps; 
	int[] trueImps; 
	double stat; 
	double time;
	double absError;
	
	public ResultValues(int[] guessedImps, int[] trueImps, double statVal, double absError, double timeVal){
		this.guessedImps = guessedImps;
		this.trueImps = trueImps;
		this.stat = statVal;
		this.absError = absError;
		this.time = timeVal;
		
	}

	public double getStat() {
		
		return stat;
	}
	
	public double getAbsError() {
		
		return absError;
	}

	public double getTime() {
		// TODO Auto-generated method stub
		return time;
	}

}
