package modelers;

import Jama.LUDecomposition;
import Jama.Matrix;


public class WLSRegression {

	private double[][] V;           // Least squares matrix
	private double[] C;      		// Coefficients

	public boolean Regress(double[] Y, double[][] X, double[] W) {
		//Y[j] = j-th observed data point
		//X[i,j] j-th value of the i-th independent variable
		//W[j] = j-th weight value
		int M = Y.length;             // M = Number of data points
		int N = X[0].length;         	 // N = Number of linear terms
	
		
		double[][] Wdiag = new double[W.length][W.length];
		
		//Form diagonal matrix with weights
		for(int i = 0; i < W.length; i++) {
			for(int j = 0; j < W.length; j++) {
				if(i == j) {
					Wdiag[i][i] = W[i];
				}
				else{
					Wdiag[i][j] = 0;
				}
			}
		}
		
		Matrix Xmat = new Matrix(X);
		Matrix Ymat = new Matrix(Y,1).transpose();
		Matrix Wmat = new Matrix(Wdiag);
		Matrix XmatT = new Matrix(Xmat.transpose().getArrayCopy());
		
		C = new double[N];

		//Form Weighted Least Squares Matrices
		Matrix Vmat = XmatT.times(Wmat).times(Xmat);
		Matrix Bmat = XmatT.times(Wmat).times(Ymat);

		LUDecomposition LU = new LUDecomposition(Vmat);
		if(LU.isNonsingular()) {
			Matrix sol = LU.solve(Bmat);
			for(int i = 0; i < N; i++) {
				C[i] = sol.getArrayCopy()[i][0];
			}
			return true;
		}
		else {
			return false;
		}
	}

	public double[] Coefficients() {
		return C;
	}

}