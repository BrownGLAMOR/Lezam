package newmodels.bidtoslot;

import java.util.ArrayList;
import java.util.HashMap;

import newmodels.bidtoslot.AbstractBidToSlotModel;

import regressions.WLSRegression;


import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicBidToClick {
	protected Query _query;

	private double m = .7;
	private ArrayList<double[]> _dataPoints;
	private boolean _isLogLinear = false;
	private int _weight;
	
	//The position function is psi*e^{zeta*bid}
	// or psi - zeta*bid
	double PSI, ZETA;
	
	//PositionGivenBid is a separate calculation for each query, so we instantiate it
	//with a specific query
	public BasicBidToClick(Query q, boolean isLogLinear) {
		_query = q;
		_isLogLinear = isLogLinear;
		_dataPoints = new ArrayList<double[]>();
		_weight = 1;
	}

	public Query getQuery() {
		return _query;
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
		_dataPoints.add(arr);
	}
	
	public double getPrediction(double bid) {
		if(_isLogLinear) {
			return PSI * Math.exp(ZETA*bid);  //Log-linear version
		}
		else {
			return PSI + ZETA*bid;
		}
	}

	public boolean updateModel(SalesReport salesReport, QueryReport queryReport) {
		//double pos = queryReport.getPosition(_query);
		double click = queryReport.getClicks(_query);
		double cpc = queryReport.getCPC(_query);
		//Make sure that we actually got a position
		if(!Double.isNaN(cpc)) {
			addDataPoint(_weight, cpc, click);
		}
		_weight++;
		
		if(_dataPoints.size() < 2) {
			return false;
		}
		//prepare matrices for regression
		double[] Y = new double[_dataPoints.size()];
		double[][] X = new double[_dataPoints.size()][2];
		double[] W = new double[_dataPoints.size()];
		
		for(int i = 0; i < Y.length; i++) {
			X[i][0] = 1;   //Linear Coefficient
			double[] pt = _dataPoints.get(i);
			W[i] = Math.pow(m, _weight - pt[0]);
			X[i][1] = pt[1];
			if(_isLogLinear) {
				Y[i] = Math.log(pt[2]);  //Log-linear version
			}
			else {
				Y[i] = pt[2];
			}
		}

		//do regressions stuff here;
		WLSRegression linReg = new WLSRegression();
		double psi,zeta;
		if(linReg.Regress(Y, X, W)) {
			if(_isLogLinear) {
				psi = Math.exp(linReg.Coefficients()[0]);  //Log-linear version
			}
			else {
				psi = linReg.Coefficients()[0];
			}
			zeta = linReg.Coefficients()[1];
			if(psi < 0) {
				return false;
			}
			PSI = psi;
			ZETA = zeta;
			System.out.println("ZETA: "+ZETA+"  PSI: "+PSI);
			return true;	
		}

		return false;
	}

}
