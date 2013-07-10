package models.queryanalyzer;

public class SolverStats {
	
	private double AIPSMAE;
	private double AIMAE;
	private double TIMAE;
	private double Time;
	private int sum_AIPSMAE;
	private double sum_AIMAE;
	private double sum_TIMAE; 
	private double sum_Time; 
	private double denom_AIPSMAE;
	private double denom_AIMAE;
	private double denom_TIMAE; 
	private double denom_Time;
	
	
	public SolverStats(){
		AIPSMAE = 0.0;
		AIMAE = 0.0;
		TIMAE = 0.0;
		Time =0;
		 sum_AIPSMAE = 0;
		 sum_AIMAE = 0;
		 sum_TIMAE = 0; 
		 sum_Time = 0; 
		 denom_AIPSMAE = 0;
		 denom_AIMAE = 0;
		 denom_TIMAE = 0; 
		 denom_Time = 0;
	}
	
	public void addDenominatorAIPS(double toAdd){
		denom_AIPSMAE+=toAdd;
	}

	public void addDenominatorAI(int toAdd){
		denom_AIMAE+=toAdd;
	}
	public void addDenominatorTI(int toAdd){
		denom_TIMAE+=toAdd;
	}
	public void addDenominatorTime(int toAdd){
		denom_TIMAE+=toAdd;
	}
	
	public void addSumAIPS(int toAdd){
		sum_AIPSMAE=sum_AIPSMAE+toAdd;
		System.out.println("SUM AIPS: "+sum_AIPSMAE+" toAdd: "+toAdd);
	}
	
	public void addSumAI(double toAdd){
		sum_AIMAE+=toAdd;
	}
	public void addSumTI(double toAdd){
		sum_TIMAE+=toAdd;
	}
	
	public void addSumTime(double toAdd){
		sum_AIPSMAE+=toAdd;
	}
	
	public void calcStats(){
		AIPSMAE = sum_AIPSMAE/denom_AIPSMAE;
		System.out.println("AIPSEMAE: "+sum_AIPSMAE+" denom: "+denom_AIPSMAE);
		AIMAE = sum_AIMAE/denom_AIMAE;
		TIMAE = sum_TIMAE/denom_TIMAE;
		Time = sum_Time/denom_Time;
	}
	
	public double getAIPSMAE(){
		return AIPSMAE;
	}
	
	public double getAIMAE(){
		return AIMAE;
	}
	
	public double getTIMAE(){
		return TIMAE;
	}
	
	public double getTime(){
		return Time;
	}

	public void updateMAES(int[] AIPS, double[] AI, double[] TI, double time) {
		
		
		addDenominatorAIPS(AIPS[1]);
		addDenominatorAI((int)AI[1]);
		addDenominatorTI((int)TI[1]);
		addSumAIPS(AIPS[0]);
		addSumAI(AI[0]);
		addSumTI(TI[0]);
		addSumTime(time);
		addDenominatorTime(1);
		
	}
}
