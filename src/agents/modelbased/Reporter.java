package agents.modelbased;
import java.io.*;

public class Reporter {
	//bid = first index, budget = second index
	Double[] bids;
	Double[] budgets;
	Double[][] cost;
	Double[][] numClicks;
	Double[][] weights;
	Double[][] profits;
	
	public Reporter(Double[] bids, Double[] budgets, Double[][] cost, Double[][] numClicks, Double[][] weights, Double[][] profits) {
		this.bids = bids;
		this.budgets = budgets;
		this.cost = cost;
		this.numClicks = numClicks;
		this.weights = weights;
		this.profits = profits;
	}
	
	//dumps the data into a single csv file for the data set
	//spec: first line is bids, second line is budgets, then an empty line, then everything after that is the matrix
	public void dump(String fileHeader, String dataset) throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(fileHeader+"_"+dataset+".dat"));
		dumpArr(bids,writer);
		dumpArr(budgets,writer);
		writer.write("\r\n");
		switch (dataset) {
			case "cost":  dumpMat(cost,writer);
			case "numClicks": dumpMat(numClicks,writer);
			case "weights": dumpMat(weights,writer);
			case "profits": dumpMat(profits,writer);
		}
		writer.flush();
		writer.close();
	}
	
	private void dumpArr(Double[] arr, Writer writer) throws IOException {
		StringBuffer strBuff = new StringBuffer();
		for (Double d : arr) {
			strBuff.append(d.toString() + ",");
		}
		String res = strBuff.toString();
		writer.write(res.substring(0,res.length()-2)+"\r\n");
	}
	
	private void dumpMat(Double[][] mat, Writer writer) throws IOException {
		StringBuffer rowBuff = new StringBuffer();
		for (int i = 0; i < mat.length; i++) {
			rowBuff.setLength(0);
			for (int j = 0; j < mat[i].length; j++) {
				Double dat = mat[i][j];
				String toAppend = "NA";
				if (dat != null) {
					toAppend = dat.toString();
				}
				rowBuff.append(toAppend + ",");
			}
			String row = rowBuff.toString();
			writer.write(row.substring(0,row.length()-1) + "\r\n");
		}
	}
}