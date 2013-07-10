package models.queryanalyzer;

public class SolverBehaviorStats {
		
		double AIPSMAE;
		double AIMAE;
		double TIMAE;
		double Time;
		double sum_AIPSMAE;
		double sum_AIMAE;
		double sum_TIMAE; 
		double sum_Time; 
		double denom_AIPSMAE;
		double denom_AIMAE;
		double denom_TIMAE; 
		double denom_Time;
		
		double N;
		double numF2;
		double numIntPosition;
		double numTopSlot;
		double percentSeen;
		double numProbing;
		double numLongTerm;
		
		
		public SolverBehaviorStats(){
			AIPSMAE = 0;
			AIMAE = 0;
			TIMAE = 0;
			Time =0;
			sum_AIPSMAE = 0;
			sum_AIMAE = 0;
			sum_TIMAE = 0; 
			sum_Time = 0; 
			denom_AIPSMAE = 0;
			denom_AIMAE = 0;
			denom_TIMAE = 0; 
			denom_Time = 0;

			N = 0;
			numF2 = 0;
			numIntPosition = 0;
			numTopSlot = 0;
			percentSeen = 0;
			numProbing = 0;
			numLongTerm = 0;
		}
		
		public void addDenominatorAIPS(int toAdd){
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
		
		public void addSumAIPS(double toAdd){
			sum_AIPSMAE+=toAdd;
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

		public void updateMAES(double[] AIPS, double[] AI, double[] TI, double time) {
			
			
			addDenominatorAIPS((int)AIPS[1]);
			addDenominatorAI((int)AI[1]);
			addDenominatorTI((int)TI[1]);
			addSumAIPS((int)AIPS[0]);
			addSumAI((int)AI[0]);
			addSumTI((int)TI[0]);
			addSumTime(time);
			addDenominatorTime(1);
			
		}

		public void updateMAES(int[] is, double[] AI, double[] TI) {
			
			addDenominatorAIPS((int)is[1]);
			addDenominatorAI((int)AI[1]);
			addDenominatorTI((int)TI[1]);
			addSumAIPS(is[0]);
			addSumAI((int)AI[0]);
			addSumTI((int)TI[0]);
		
			
		}

		public void incrementN() {
			N++;
			
		}

		public void incrementNumF2() {
			numF2++;
			
		}

		public void incrementNumIntPosition() {
			numIntPosition++;
			
		}

		public void incrementNumTopSlot() {
			numTopSlot++;
			
		}

		public void incrementNumLongTerm() {
			numLongTerm++;
		}

		public void incrementpercentSeen(double percentImpressionsSeen) {
			percentSeen+=percentImpressionsSeen;
			
		}

		public void incrementNumProbing() {
			numProbing++;
			
		}

		public int getNumTopSlot() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getN() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getPercentSeen() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getNumIntPosition() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getNumProbing() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getNumLongTerm() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getNumF2() {
			// TODO Auto-generated method stub
			return 0;
		}


}
