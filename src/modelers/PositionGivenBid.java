package modelers;


import java.util.ArrayList;

import edu.umich.eecs.tac.props.Query;

public class PositionGivenBid {
	
	private double m = .75;
	private Query Q;
	private ArrayList<double[]> dataPoints;
	
	//The position function is psi*e^{zeta*bid}
	double PSI, ZETA;
	
	//PositionGivenBid is a separate calculation for each query, so we instantiate it
	//with a specific query
	public PositionGivenBid(Query q) {
		Q = q;
		dataPoints = new ArrayList<double[]>();
	}

	public Query getQuery() {
		return Q;
	}
	
	public double getPsi() {
		return PSI;
	}

	public double getZeta() {
		return ZETA;
	}

	public void addDataPoint(int time, double bid, double position) {
		double[] arr = new double[3];
		arr[0] = (double)time;
		arr[1] = bid;
		arr[2] = position;
		dataPoints.add(arr);
	}
	
	public boolean updateModel(int time) {
		
		//prepare matrices for regression
		double[] Y = new double[dataPoints.size()];
		double[][] X = new double[2][dataPoints.size()];
		double[] W = new double[dataPoints.size()];
		
		for(int j = 0; j < Y.length; j++) {
			X[0][j] = 1;   //Linear Coefficient
			double[] pt = dataPoints.get(j);
			W[j] = Math.pow(m, time - pt[0]);
			X[1][j] = pt[1];
//			Y[j] = Math.log(pt[2]);  //Log-linear version
			Y[j] = Math.log(pt[2]);
		}
		
		//do regressions stuff here;
		WLSRegression linReg = new WLSRegression();
		if(linReg.Regress(Y, X, W)) {
//			PSI = Math.exp(linReg.Coefficients()[0]);  //Log-linear version
			PSI = linReg.Coefficients()[0];
			ZETA = linReg.Coefficients()[1];
			System.out.println("Zeta: "+ZETA+"   Psi: "+PSI);
			System.out.println(dataPoints.size());
			return true;
		}
		
		return false;
	}
	
	//Uses the current model to get the position given bid
	//Make sure to update the model successfully first!
	public double getPosition(double bid) {
//		return PSI * Math.exp(ZETA*bid);  //Log-linear version
		return PSI + ZETA*bid;
	}
	
	//Uses the current model to get the bid given position
	//Make sure to update the model successfully first!
	public double getBid(double position) {
//		return Math.log(position/PSI)/ZETA;  //Log-linear version
		return (position-PSI)/ZETA;
	}
	
}
