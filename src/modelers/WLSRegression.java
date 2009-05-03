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
		int N = X.length;         	 // N = Number of linear terms

		V = new double[N][N];
		C = new double[N];
		double[][] B = new double[N][1];   // Vector for LSQ

		// Clear the matrices to start out
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				V[i][j] = 0;
			}
		}

		// Form Least Squares Matrix
		for (int i = 0; i < N; i++)	{
			for (int j = 0; j < N; j++)	{
				V[i][j] = 0;
				for (int k = 0; k < M; k++) {
					V[i][j] += W[k] * X[i][k] * X[j][k];
				}
			}
			B[i][0] = 0;
			for (int k = 0; k < M; k++) {
				B[i][0] += W[k] * X[i][k] * Y[k];
			}
		}

		Matrix Vmat = new Matrix(V);
		Matrix Bmat = new Matrix(B);
		LUDecomposition LU = new LUDecomposition(Vmat);
		if(LU.isNonsingular()) {
			Matrix sol = LU.solve(Bmat);
			C[0] = sol.getArrayCopy()[0][0];
			C[1] = sol.getArrayCopy()[0][0];
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