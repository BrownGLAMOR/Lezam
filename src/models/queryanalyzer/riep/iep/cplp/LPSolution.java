package models.queryanalyzer.riep.iep.cplp;

import java.util.Arrays;

public class LPSolution {
	public final double objectiveVal;
	public final double[][] I_a_s; //Impressions each agent sees in each slot
	public final double[] S_a;
	public final double[] T_a; //Total impressions for each agent

	public LPSolution(double objectiveVal, double[][] I_a_s, double[] S_a, double[] T_a) {
		this.objectiveVal = objectiveVal;
		this.I_a_s = I_a_s;
		this.S_a = S_a;
		this.T_a = T_a;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("obj: " + objectiveVal + "\n");
		sb.append("S_a: " + Arrays.toString(S_a) + "\n");
		sb.append("T_a: " + Arrays.toString(T_a) + "\n");
		for (int a=0; a<I_a_s.length; a++) {
			sb.append("I_" + a + "_s: " + Arrays.toString(I_a_s[a]) + "\n");				
		}
		return sb.toString();
	}
}

