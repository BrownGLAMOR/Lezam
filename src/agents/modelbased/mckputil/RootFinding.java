package agents.modelbased.mckputil;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math.complex.Complex;

public class RootFinding {

	final static double EPSILON = .0001;

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
		double A,B,C,D;
		A = b/a;
		B = c/a;
		C = d/a;
		D = e/a;
		Complex T1,T2,T3,T4,T5;
		T1 = new Complex(-A/4.0,0);
		T2 = new Complex(B*B - 3*A*C + 12*D,0);
		T3 = new Complex((2*B*B*B - 9*A*B*C + 27*C*C + 27*A*A*D - 72*B*D)/2.0,0);
		T4 = new Complex((-A*A*A + 4*A*B - 8*C)/32.0,0);
		T5 = new Complex((3*A*A*A - 8*B)/48.0,0);
		Complex R1,R2,R3,R4,R5,R6;
		R1 = T3.multiply(T3).subtract(T2.multiply(T2));
		R1 = R1.sqrt();
		R2 = T3.add(R1);
		R2 = R2.pow(new Complex(1.0/3.0,0));
		R3 = T2.divide(R2).add(R2).divide(new Complex(12,0));
		R4 = T5.add(R3);
		R4 = R4.sqrt();
		R5 = T5.multiply(new Complex(2,0)).subtract(R3);
		R6 = T4.divide(R4);
		Complex R5subR6sqrt, R5plusR6sqrt;
		R5subR6sqrt = R5.subtract(R6);
		R5subR6sqrt = R5subR6sqrt.sqrt();
		R5plusR6sqrt = R5.add(R6);
		R5plusR6sqrt = R5plusR6sqrt.sqrt();
		Complex r1,r2,r3,r4;
		r1 = T1.subtract(R4).subtract(R5subR6sqrt);
		r2 = T1.subtract(R4).add(R5subR6sqrt);
		r3 = T1.add(R4).subtract(R5plusR6sqrt);
		r4 = T1.add(R4).add(R5plusR6sqrt);
		roots[0][0] = r1.getReal();
		roots[0][1] = r1.getImaginary();
		roots[1][0] = r2.getReal();
		roots[1][1] = r2.getImaginary();
		roots[2][0] = r3.getReal();
		roots[2][1] = r3.getImaginary();
		roots[3][0] = r4.getReal();
		roots[3][1] = r4.getImaginary();
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

	public static final boolean checkQuarticSolution(double[] coeff, double[][] roots) {
		if(coeff.length != roots.length+1 || coeff.length != 5) {
			return false;
		}

		for(int i = 0; i < 4; i++) {
			/*
			 * I am only checking the real roots, because I didn't feel like dealing
			 * with checking the complex roots and we wouldn't use them anyhow.  If the
			 * real roots are correct I can't imagine the compex ones are wrong anyhow
			 */
			if(roots[i][1] != 0) {
				continue;
			}
			double real = coeff[0]*roots[i][0]*roots[i][0]*roots[i][0]*roots[i][0] + 
			coeff[1]*roots[i][0]*roots[i][0]*roots[i][0] + coeff[2]*roots[i][0]*roots[i][0] + 
			coeff[3]*roots[i][0] + coeff[4];
			if(Math.abs(real) > EPSILON) {
				return false;
			}
		}

		return true;
	}

	public static void main(String[] args) {
		//		double topStart = System.nanoTime();
		//		double numTrials = 50000000000.0;
		//		double time = 0;
		//		int badCounter = 0;
		//		for(int i = 0; i < numTrials; i++) {
		//			double[] coeff = getCubicCoefficients();
		//			double start = System.nanoTime();
		//			double[][] roots = getCubicRoots(coeff);
		//			double end = System.nanoTime();
		//			time += (end-start)/1000000.0;
		//			boolean bool = checkCubicSolution(coeff, roots);
		//			if(!bool) {badCounter++;}
		////			System.out.println(checkCubicSolution(coeff, roots) + ", x1 = " + roots[0][0] + " i*" + roots[0][1] + ", x2 = " + roots[1][0] + " i*" + roots[1][1] + ", x3 = " + roots[2][0] + " i*" + roots[2][1]);
		//		}
		//		double end = System.nanoTime();
		//		System.out.println(time/numTrials + " ms per equation");
		//		System.out.println("Total Time: " + (end-topStart)/1000000000.0);
		//		System.out.println("% below threshold: " + (badCounter/numTrials)*100);


		double topStart = System.nanoTime();
		double numTrials = 10000.0;
		double time = 0;
		int badCounter = 0;
		for(int i = 0; i < numTrials; i++) {
			double[] coeff = getQuarticCoefficients();
			double start = System.nanoTime();
			double[][] roots = getQuarticRoots(coeff);
			double end = System.nanoTime();
			time += (end-start)/1000000.0;
			boolean bool = checkQuarticSolution(coeff, roots);
			if(!bool) {badCounter++;}
			//System.out.println(bool + ", x1 = " + roots[0][0] + " i*" + roots[0][1] + ", x2 = " + roots[1][0] + " i*" + roots[1][1] + ", x3 = " + roots[2][0] + " i*" + roots[2][1] + ", x4 = " + roots[3][0] + " i*" + roots[3][1]);
		}
		double end = System.nanoTime();
		System.out.println(time/numTrials + " ms per equation");
		System.out.println("Total Time: " + (end-topStart)/1000000000.0);
		System.out.println("% below threshold: " + (badCounter/numTrials)*100);
	}

}
