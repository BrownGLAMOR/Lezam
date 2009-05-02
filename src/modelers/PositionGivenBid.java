package modelers;

import java.util.LinkedList;

import edu.umich.eecs.tac.props.Query;

public class PositionGivenBid {
	
	private double m = .5;
	private Query Q;
	private LinkedList<double[]> dataPoints;
	
	//The position function is psi*e^{zeta*bid}
	double PSI, ZETA;
	
	//PositionGivenBid is a separate calculation for each query, so we instantiate it
	//with a specific query
	public PositionGivenBid(Query q) {
		Q = q;
		dataPoints = new LinkedList<double[]>();
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
	
	public void updateModel() {
		//do regressions stuff here;
		
		double fitPsi  = .1;
		double fitZeta = .3;
		PSI = fitPsi;
		ZETA = fitZeta;
	}
		
}
