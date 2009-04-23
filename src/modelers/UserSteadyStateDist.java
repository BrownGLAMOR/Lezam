package modelers;

import java.math.BigDecimal;

public class UserSteadyStateDist {
	
	public enum UserState {NS, IS, F0, F1, F2, T};

	protected final static BigDecimal epsilon = new BigDecimal(".0000000001");
	protected final static BigDecimal burstprobability = new BigDecimal("0.1");
	//All these probabilities should sum to 1
	// From NS:
	protected final static BigDecimal standard_NON_SEARCHING_NON_SEARCHING=new BigDecimal("0.99");
	protected final static BigDecimal standard_NON_SEARCHING_INFORMATIONAL_SEARCH=new BigDecimal("0.01");
	protected final static BigDecimal burst_NON_SEARCHING_NON_SEARCHING=new BigDecimal("0.95");
	protected final static BigDecimal burst_NON_SEARCHING_INFORMATIONAL_SEARCH=new BigDecimal("0.05");
	// From IS:
	protected final static BigDecimal standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ZERO=new BigDecimal("0.5");
	protected final static BigDecimal standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ONE=new BigDecimal("0.20");
	protected final static BigDecimal standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_TWO=new BigDecimal("0.05");
	protected final static BigDecimal standard_INFORMATIONAL_SEARCH_NON_SEARCHING=new BigDecimal("0.05");
	protected final static BigDecimal standard_INFORMATIONAL_SEARCH_INFORMATIONAL_SEARCH=new BigDecimal("0.2");
	protected final static BigDecimal burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ZERO=new BigDecimal("0.5");
	protected final static BigDecimal burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ONE=new BigDecimal("0.2");
	protected final static BigDecimal burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_TWO=new BigDecimal("0.05");
	protected final static BigDecimal burst_INFORMATIONAL_SEARCH_NON_SEARCHING=new BigDecimal("0.05");
	protected final static BigDecimal burst_INFORMATIONAL_SEARCH_INFORMATIONAL_SEARCH=new BigDecimal("0.2");
	// From F0
	protected final static BigDecimal standard_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ZERO=new BigDecimal("0.70");
	protected final static BigDecimal standard_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ONE=new BigDecimal("0.20");
	protected final static BigDecimal standard_FOCUS_LEVEL_ZERO_NON_SEARCHING=new BigDecimal("0.10");
	protected final static BigDecimal burst_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ZERO=new BigDecimal("0.70");
	protected final static BigDecimal burst_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ONE=new BigDecimal("0.20");
	protected final static BigDecimal burst_FOCUS_LEVEL_ZERO_NON_SEARCHING=new BigDecimal("0.10");
	// From F1
	protected final static BigDecimal standard_FOCUS_LEVEL_ONE_FOCUS_LEVEL_ONE=new BigDecimal("0.70");
	protected final static BigDecimal standard_FOCUS_LEVEL_ONE_FOCUS_LEVEL_TWO=new BigDecimal("0.20");
	protected final static BigDecimal standard_FOCUS_LEVEL_ONE_NON_SEARCHING=new BigDecimal("0.10");
	protected final static BigDecimal burst_FOCUS_LEVEL_ONE_FOCUS_LEVEL_ONE=new BigDecimal("0.7");
	protected final static BigDecimal burst_FOCUS_LEVEL_ONE_FOCUS_LEVEL_TWO=new BigDecimal("0.2");
	protected final static BigDecimal burst_FOCUS_LEVEL_ONE_NON_SEARCHING=new BigDecimal("0.10");
	// From F2
	protected final static BigDecimal standard_FOCUS_LEVEL_TWO_FOCUS_LEVEL_TWO=new BigDecimal("0.90");
	protected final static BigDecimal standard_FOCUS_LEVEL_TWO_NON_SEARCHING=new BigDecimal("0.10");
	protected final static BigDecimal burst_FOCUS_LEVEL_TWO_FOCUS_LEVEL_TWO=new BigDecimal("0.90");
	protected final static BigDecimal burst_FOCUS_LEVEL_TWO_NON_SEARCHING=new BigDecimal("0.10");
	// From T
	protected final static BigDecimal standard_TRANSACTED_TRANSACTED=new BigDecimal("0.20");
	protected final static BigDecimal standard_TRANSACTED_NON_SEARCHING=new BigDecimal("0.80");
	protected final static BigDecimal burst_TRANSACTED_TRANSACTED=new BigDecimal("0.20");
	protected final static BigDecimal burst_TRANSACTED_NON_SEARCHING=new BigDecimal("0.80");
	
	public UserSteadyStateDist() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BigDecimal[][] steadyStateM = new BigDecimal[UserState.values().length][UserState.values().length];
		steadyStateM = simulateVirtualization();
		for(int i = 0; i < UserState.values().length; i++) {
			for(int j = 0; j < UserState.values().length; j++) {
				System.out.println(UserState.values()[i]+" to "+UserState.values()[j]+":  "+steadyStateM[i][j]+"");
			}
		}
	}


	protected static BigDecimal[][] simulateVirtualization() {
		BigDecimal[][] m = new BigDecimal[UserState.values().length][UserState.values().length];
		BigDecimal[][] standardMatrix = makeStandardMatrix();
		BigDecimal[][] burstMatrix = makeBurstMatrix();
		if(Math.random() < burstprobability.doubleValue()) {
			System.out.println("***");
			m = burstMatrix;
		}
		else {
			m = standardMatrix;
		}
		for(int i = 0; i < 9; i++) {
			if(Math.random() < burstprobability.doubleValue()) {
				System.out.println("***");
				m = matrixMultiplication(m, burstMatrix);
			}
			else {
				m = matrixMultiplication(m, standardMatrix);
			}
		}
		return m;
	}

	protected static BigDecimal[][] findSteadyState() {
		BigDecimal[][] probMatrix = new BigDecimal[UserState.values().length][UserState.values().length];
		probMatrix = makeProbMatrix();
		int count = 0;
		do {
			for(int i = 0; i < 1; i++) {
				probMatrix = matrixMultiplication(probMatrix, probMatrix);
				count++;
			}
		}
		while(!convergenceTest(probMatrix));
		System.out.println(count+" Iterations");
		return probMatrix;
	}
	

	protected static boolean convergenceTest(BigDecimal[][] m) {
		BigDecimal[][] m2 = matrixMultiplication(m,m);
		for (int i = 0; i < UserState.values().length; i++) {                                  
			for (int j = 0; j < UserState.values().length; j++) {
					if(m[i][j].subtract(m2[i][j]).abs().compareTo(epsilon) > 0) {
						return false;
					}
			}
		}
		return true;
	}

	protected static BigDecimal[][] makeProbMatrix() {
		BigDecimal[][] standardMatrix = makeStandardMatrix();
		BigDecimal[][] burstMatrix = makeBurstMatrix();
		BigDecimal[][] probMatrix = combineMarkovChains(standardMatrix, burstMatrix, burstprobability); 
		return probMatrix;
	}
	
	//NS=0, IS=1, F0=2, F1=3, F2=4, NS=5
	protected static BigDecimal[][] makeStandardMatrix() {
		BigDecimal[][] standardMatrix = new BigDecimal[UserState.values().length][UserState.values().length];
		//transitions from non-searching to other states
		standardMatrix[0][0] = standard_NON_SEARCHING_NON_SEARCHING;
		standardMatrix[0][1] = standard_NON_SEARCHING_INFORMATIONAL_SEARCH;
		standardMatrix[0][2] = new BigDecimal("0");
		standardMatrix[0][3] = new BigDecimal("0");
		standardMatrix[0][4] = new BigDecimal("0");
		standardMatrix[0][5] = new BigDecimal("0");
		//transitions from informational-searching to other states
		standardMatrix[1][0] = standard_INFORMATIONAL_SEARCH_NON_SEARCHING;
		standardMatrix[1][1] = standard_INFORMATIONAL_SEARCH_INFORMATIONAL_SEARCH;
		standardMatrix[1][2] = standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ZERO;
		standardMatrix[1][3] = standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ONE;
		standardMatrix[1][4] = standard_INFORMATIONAL_SEARCH_FOCUS_LEVEL_TWO;
		standardMatrix[1][5] = new BigDecimal("0");
		//transitions from F0 to other states
		standardMatrix[2][0] = standard_FOCUS_LEVEL_ZERO_NON_SEARCHING;
		standardMatrix[2][1] = new BigDecimal("0");
		standardMatrix[2][2] = standard_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ZERO;
		standardMatrix[2][3] = standard_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ONE;
		standardMatrix[2][4] = new BigDecimal("0");
		standardMatrix[2][5] = new BigDecimal("0");
		//transitions from F1 to other states
		standardMatrix[3][0] = standard_FOCUS_LEVEL_ONE_NON_SEARCHING;
		standardMatrix[3][1] = new BigDecimal("0");
		standardMatrix[3][2] = new BigDecimal("0");
		standardMatrix[3][3] = standard_FOCUS_LEVEL_ONE_FOCUS_LEVEL_ONE;
		standardMatrix[3][4] = standard_FOCUS_LEVEL_ONE_FOCUS_LEVEL_TWO;
		standardMatrix[3][5] = new BigDecimal("0");
		//transitions from F2 to other states
		standardMatrix[4][0] = standard_FOCUS_LEVEL_TWO_NON_SEARCHING;
		standardMatrix[4][1] = new BigDecimal("0");
		standardMatrix[4][2] = new BigDecimal("0");
		standardMatrix[4][3] = new BigDecimal("0");
		standardMatrix[4][4] = standard_FOCUS_LEVEL_TWO_FOCUS_LEVEL_TWO;
		standardMatrix[4][5] = new BigDecimal("0");
		//transitions from T to other states
		standardMatrix[5][0] = standard_TRANSACTED_NON_SEARCHING;
		standardMatrix[5][1] = new BigDecimal("0");
		standardMatrix[5][2] = new BigDecimal("0");
		standardMatrix[5][3] = new BigDecimal("0");
		standardMatrix[5][4] = new BigDecimal("0");
		standardMatrix[5][5] = standard_TRANSACTED_TRANSACTED;
		return standardMatrix;
	}
	
	protected static BigDecimal[][] makeBurstMatrix() {
		BigDecimal[][] burstMatrix = new BigDecimal[UserState.values().length][UserState.values().length];
		//transitions from non-searching to other states
		burstMatrix[0][0] = burst_NON_SEARCHING_NON_SEARCHING;
		burstMatrix[0][1] = burst_NON_SEARCHING_INFORMATIONAL_SEARCH;
		burstMatrix[0][2] = new BigDecimal("0");
		burstMatrix[0][3] = new BigDecimal("0");
		burstMatrix[0][4] = new BigDecimal("0");
		burstMatrix[0][5] = new BigDecimal("0");
		//transitions from informational-searching to other states
		burstMatrix[1][0] = burst_INFORMATIONAL_SEARCH_NON_SEARCHING;
		burstMatrix[1][1] = burst_INFORMATIONAL_SEARCH_INFORMATIONAL_SEARCH;
		burstMatrix[1][2] = burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ZERO;
		burstMatrix[1][3] = burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_ONE;
		burstMatrix[1][4] = burst_INFORMATIONAL_SEARCH_FOCUS_LEVEL_TWO;
		burstMatrix[1][5] = new BigDecimal("0");
		//transitions from F0 to other states
		burstMatrix[2][0] = burst_FOCUS_LEVEL_ZERO_NON_SEARCHING;
		burstMatrix[2][1] = new BigDecimal("0");
		burstMatrix[2][2] = burst_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ZERO;
		burstMatrix[2][3] = burst_FOCUS_LEVEL_ZERO_FOCUS_LEVEL_ONE;
		burstMatrix[2][4] = new BigDecimal("0");
		burstMatrix[2][5] = new BigDecimal("0");
		//transitions from F1 to other states
		burstMatrix[3][0] = burst_FOCUS_LEVEL_ONE_NON_SEARCHING;
		burstMatrix[3][1] = new BigDecimal("0");
		burstMatrix[3][2] = new BigDecimal("0");
		burstMatrix[3][3] = burst_FOCUS_LEVEL_ONE_FOCUS_LEVEL_ONE;
		burstMatrix[3][4] = burst_FOCUS_LEVEL_ONE_FOCUS_LEVEL_TWO;
		burstMatrix[3][5] = new BigDecimal("0");
		//transitions from F2 to other states
		burstMatrix[4][0] = burst_FOCUS_LEVEL_TWO_NON_SEARCHING;
		burstMatrix[4][1] = new BigDecimal("0");
		burstMatrix[4][2] = new BigDecimal("0");
		burstMatrix[4][3] = new BigDecimal("0");
		burstMatrix[4][4] = burst_FOCUS_LEVEL_TWO_FOCUS_LEVEL_TWO;
		burstMatrix[4][5] = new BigDecimal("0");
		//transitions from T to other states
		burstMatrix[5][0] = burst_TRANSACTED_NON_SEARCHING;
		burstMatrix[5][1] = new BigDecimal("0");
		burstMatrix[5][2] = new BigDecimal("0");
		burstMatrix[5][3] = new BigDecimal("0");
		burstMatrix[5][4] = new BigDecimal("0");
		burstMatrix[5][5] = burst_TRANSACTED_TRANSACTED;
		return burstMatrix;
	}
	
	protected static BigDecimal[][] combineMarkovChains(BigDecimal[][] standard, BigDecimal[][] burst, BigDecimal burstprob) {
		BigDecimal[][] newMatrix = new BigDecimal[UserState.values().length][UserState.values().length];
		for(int i=0; i < UserState.values().length; i++){
			for(int j=0; j < UserState.values().length; j++){
				newMatrix[i][j] = standard[i][j].multiply(new BigDecimal("1").subtract(burstprob)).add(burst[i][j].multiply(burstprob));
			}
		}
		return newMatrix;
	}
	
	protected static BigDecimal[][] matrixMultiplication(BigDecimal[][] m1, BigDecimal[][] m2) {
		BigDecimal[][] newMatrix = new BigDecimal[UserState.values().length][UserState.values().length];                                                   
		for (int i = 0; i < UserState.values().length; i++) {                                  
			for (int j = 0; j < UserState.values().length; j++) {               
				newMatrix[i][j] = new BigDecimal("0");;
				for(int k = 0; k < UserState.values().length; k++) {
					newMatrix[i][j] = newMatrix[i][j].add(m1[i][k].multiply(m2[k][j]));
				}
			}
		}
		return newMatrix;
	}
	
}