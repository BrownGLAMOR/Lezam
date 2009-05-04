package modelers;

import java.util.ArrayList;

import edu.umich.eecs.tac.props.Query;

public class PositionGivenBid {
	
	private double m = .8;
	private Query Q;
	private ArrayList<double[]> dataPoints;
	private boolean isLogLinear = true;
	
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
		double[][] X = new double[dataPoints.size()][2];
		double[] W = new double[dataPoints.size()];
		
		for(int i = 0; i < Y.length; i++) {
			X[i][0] = 1;   //Linear Coefficient
			double[] pt = dataPoints.get(i);
			W[i] = Math.pow(m, time - pt[0]);
			X[i][1] = pt[1];
			if(isLogLinear) {
				Y[i] = Math.log(pt[2]);  //Log-linear version
			}
			else {
				Y[i] = pt[2];
			}
		}

		//do regressions stuff here;
		WLSRegression linReg = new WLSRegression();
		if(linReg.Regress(Y, X, W)) {
			if(isLogLinear) {
				PSI = Math.exp(linReg.Coefficients()[0]);  //Log-linear version
			}
			else {
				PSI = linReg.Coefficients()[0];
			}
			ZETA = linReg.Coefficients()[1];
			return true;
		}

		return false;
	}

	//Uses the current model to get the position given bid
	//Make sure to update the model successfully first!
	public double getPosition(double bid) {
		if(isLogLinear) {
			return PSI * Math.exp(ZETA*bid);  //Log-linear version
		}
		else {
			return PSI + ZETA*bid;
		}
	}

	//Uses the current model to get the bid given position
	//Make sure to update the model successfully first!
	public double getBid(double position) {
		if(isLogLinear) {
			return Math.log(position/PSI)/ZETA;  //Log-linear version
		}
		else {
			return (position-PSI)/ZETA;
		}
	}
	
	public void intervalEstimation() {
		
	}
	
}
