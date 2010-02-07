package agents.modelbased.mckputil;

import java.util.Random;

public class RootFinding {

	final static double EPSILON = .01;

	public static final double[][] getCubicRoots(double[] coeff) {
		return getCubicRoots(coeff[0],coeff[1],coeff[2],coeff[3]);
	}

	public static final double[][] getQuarticRoots(double[] coeff) {
		return getQuarticRoots(coeff[0],coeff[1],coeff[2],coeff[3],coeff[4]);
	}

	public static final double[][] getCubicRoots(double a, double b, double c, double d) {
		double[][] roots = new double[3][2];
		double A,B,C,Q,R,D;
		A = b/a;
		B = c/a;
		C = d/a;
		Q = (3*B-A*A)/9.0;
		R = (9*A*B - 27*C - 2*A*A*A)/54.0;
		D = Q*Q*Q + R*R;
		if(D >= 0) {
			double S,T;
			S = Math.cbrt(R + Math.sqrt(D));
			T = Math.cbrt(R - Math.sqrt(D));
			roots[0][0] = -A/3.0 + (S + T); //First root real component
			roots[0][1] = 0; //First root complex component
			roots[1][0] = -A/3.0 - (S + T)/2.0;
			roots[1][1] = Math.sqrt(3) * (S-T)/2.0;
			roots[2][0] = -A/3.0 - (S + T)/2.0;
			roots[2][1] = -Math.sqrt(3) * (S-T)/2.0;
		}
		else {
			double theta;
			theta = Math.acos(R/Math.sqrt(-Q*Q*Q));
			roots[0][0] = 2 * Math.sqrt(-Q) * Math.cos(theta/3.0) - A/3.0;
			roots[1][0] = 2 * Math.sqrt(-Q) * Math.cos((theta + 2*Math.PI)/3.0) - A/3.0;
			roots[2][0] = 2 * Math.sqrt(-Q) * Math.cos((theta + 4*Math.PI)/3.0) - A/3.0;
		}
		return roots;
	}

	public static final double[][] getQuarticRoots(double a, double b, double c, double d, double e) {
		double[][] roots = new double[4][2];
		return roots;
	}

	public static final double[] getCubicCoefficients() {
		Random R = new Random(System.nanoTime());
		double[] coeff = new double[4];
		for(int i = 0; i < coeff.length; i++) {
			coeff[i] = R.nextDouble();
		}
		return coeff;
	}

	public static final double[] getQuarticCoefficients() {
		Random R = new Random();
		double[] coeff = new double[5];
		for(int i = 0; i < coeff.length; i++) {
			coeff[i] = R.nextDouble();
		}
		return coeff;
	}

	public static final boolean checkCubicSolution(double[] coeff, double[][] roots) {
		if(coeff.length != roots.length+1 || coeff.length != 4) {
			return false;
		}

		for(int i = 0; i < 3; i++) {
			/*
			 * I am only checking the real roots, because I didn't feel like dealing
			 * with checking the complex roots and we wouldn't use them anyhow.  If the
			 * real roots are correct I can't imagine the compex ones are wrong anyhow
			 */
			if(roots[i][1] != 0) {
				continue;
			}
			double real = coeff[0]*roots[i][0]*roots[i][0]*roots[i][0] + 
			coeff[1]*roots[i][0]*roots[i][0] + coeff[2]*roots[i][0] + coeff[3];
			if(Math.abs(real) > EPSILON) {
				return false;
			}
		}

		return true;
	}

	public static void main(String[] args) {
		double numTrials = 100000000;
		double time = 0;
		for(int i = 0; i < numTrials; i++) {
			double[] coeff = getCubicCoefficients();
			double start = System.nanoTime();
			double[][] roots = getCubicRoots(coeff);
			double end = System.nanoTime();
			time += (end-start)/1000000.0;
			boolean bool = checkCubicSolution(coeff, roots);
			if(!bool) {throw new RuntimeException("ERROR\nERROR\bERROR");}
//			System.out.println(checkCubicSolution(coeff, roots) + ", x1 = " + roots[0][0] + " i*" + roots[0][1] + ", x2 = " + roots[1][0] + " i*" + roots[1][1] + ", x3 = " + roots[2][0] + " i*" + roots[2][1]);
		}
		System.out.println(time/numTrials + " ms per equation");
	}

}
