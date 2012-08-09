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
	
	public int[][] getImpressions() {
        int[][] cascade = new int[I_a_s.length][];
        for(int i = 0; i < cascade.length; i++) {
        	cascade[i] = new int[I_a_s[i].length];
           for(int j = 0; j < cascade[i].length; j++) {
        	   cascade[i][j] = (int)I_a_s[i][j];
           }
        }
        return cascade;
	}

	public LPSolution unorder(int[] order) {
		assert(order.length == T_a.length);
		int[] orderLookup = new int[order.length];
		for(int i=0; i < order.length; i++){
			orderLookup[order[i]] = i;
		}
		double[][] I_a_sCopy = new double[I_a_s.length][];
		double[] S_aCopy = Arrays.copyOf(S_a, S_a.length);
		double[] T_aCopy = new double[T_a.length];
		
		for(int i=0; i < order.length; i++){
			I_a_sCopy[i] = Arrays.copyOf(I_a_s[orderLookup[i]],I_a_s[orderLookup[i]].length);
			T_aCopy[i] = T_a[orderLookup[i]];
		}
		return new LPSolution(objectiveVal, I_a_sCopy, S_aCopy, T_aCopy);
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

