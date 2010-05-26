package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math.complex.Complex;

import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class ConstantsAndFunctions 
{
	
	// Target Effect
	final double _TE = 0.5;
	// Promoted Slot Bonus
	final double _PSB =  0.5;
	// Component Specialty Bonus
	final double _CSB = 0.6;
	// Advertiser effect  lower bound <> upper bound
	final double[][] _advertiserEffectBounds = { {0.2, 0.3},
											      {0.3, 0.4},
											      {0.4, 0.5}
										       };
	
	// Average advertiser effect  
	final double[] _advertiserEffectBoundsAvg = {0.25,0.35,0.45};

	
	// Continuation Probability  lower bound <> upper bound
	final double[][] _continuationProbBounds = { {0.2, 0.5},
										         {0.3, 0.6},
										         {0.4, 0.7}
	                   	  					   };
	
	final double[] _continuationProbBoundsAvg = {0.35,0.45,0.55};
	
	final double[][] fTargetfPro = { {(1.0), (1.0)*(1.0+_PSB)},
	         {(1.0 + _TE), (1.0 + _TE)*(1.0+_PSB)},
	         {(1.0)/(1.0 + _TE), (1.0)/(1.0 + _TE)*(1.0+_PSB)}
		 };
	
	int bool2int(boolean bool){
		if (bool){
			return 1;
		}
		return 0;
	}
	
	int getFTargetIndex(boolean targeted, Product p, Product target){
		if (targeted){
			return (2-bool2int(p.equals(target)));
		}
		return 0;
	}

	int queryTypeToInt(QueryType qt){
		if(qt.equals(QueryType.FOCUS_LEVEL_ZERO)){
			return 0;
		}else
			if(qt.equals(QueryType.FOCUS_LEVEL_ONE)){
				return 1;
			}else{
				return 2;
			}
	}
	
	// Double array to store the roots for the quartic equation
	static double[][] _quarticRoots;

	// Double array to store the roots for the cubic equation
	static double[][] _cubicRoots;
	
	// Calculate the forward click probability as defined on page 14 of the spec.
	public double forwardClickProbability(double advertiserEffect, double fTargetfPro)
	{	
		double temp = (advertiserEffect * fTargetfPro) / 
			   ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
		
		return temp;
	}
	
	// Calculate the inverse of the forward click probability
	public double inverseClickProbability(double ProbClick, double fTargetfPro)
	{
		double temp = ProbClick / (ProbClick + fTargetfPro - ProbClick * fTargetfPro); 
		
		return temp;
		
	}

	// This function to solves a quartic equation. It returns all the roots in an array 
	// Note: 
	// [1] This has been directly copied from agents.modelbased.mckputil.RootFinding
	// [2] This function is overloaded
	public static double[][] getQuarticRoots(double a, double b, double c, double d, double e) 
	{
		double[][] roots = new double[4][2];
		double A, B, C, D;
		A = b / a;
		B = c / a;
		C = d / a;
		D = e / a;
		Complex T1, T2, T3, T4, T5;
		T1 = new Complex(-A / 4.0, 0);
		T2 = new Complex(B * B - 3 * A * C + 12 * D, 0);
		T3 = new Complex((2 * B * B * B - 9 * A * B * C + 27 * C * C + 27 * A
				* A * D - 72 * B * D) / 2.0, 0);
		T4 = new Complex((-A * A * A + 4 * A * B - 8 * C) / 32.0, 0);
		T5 = new Complex((3 * A * A * A - 8 * B) / 48.0, 0);
		Complex R1, R2, R3, R4, R5, R6;
		R1 = T3.multiply(T3).subtract(T2.multiply(T2).multiply(T2));
		R1 = R1.sqrt();
		R2 = T3.add(R1);
		R2 = R2.pow(new Complex(1.0 / 3.0, 0));
		R3 = T2.divide(R2).add(R2).divide(new Complex(12, 0));
		R4 = T5.add(R3);
		R4 = R4.sqrt();
		R5 = T5.multiply(new Complex(2, 0)).subtract(R3);
		R6 = T4.divide(R4);
		Complex R5subR6sqrt, R5plusR6sqrt;
		R5subR6sqrt = R5.subtract(R6);
		R5subR6sqrt = R5subR6sqrt.sqrt();
		R5plusR6sqrt = R5.add(R6);
		R5plusR6sqrt = R5plusR6sqrt.sqrt();
		Complex r1, r2, r3, r4;
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
	
	// This function merely returns the quartic roots
	public static double[][] getQuarticRoots(double[] coeff) 
	{
		return getQuarticRoots(coeff[0], coeff[1], coeff[2], coeff[3], coeff[4]);
	}
	
	// This function randomly generates coefficients to be used
	// in the quartic solver
	public static final double[] getQuarticCoefficients() 
	{
		Random R = new Random();
		double[] coeff = new double[5];
		for(int i = 0; i < coeff.length; i++) 
		{
			coeff[i] = R.nextDouble();
		}
		return coeff;
	}
	
	// This function returns the real, quartic root only
	// if it is nonnegative; else this function returns -1
	public static double getRealQuarticRoot()
	{
		// Variable to index into the real and imaginary components
		int m = 0;
		
		double realRoot = -1.0;
		
		// For each of the four possible real roots, print them out (if they
		// exist)
		for (int k = 0; k < 4; k++)
		{
			//System.out.println("***** ROOT # "+(k+1)+"*****");
			//System.out.println("The real component is: "+_quarticRoots[k][m]);
			//System.out.println("The imaginary component is: "+_quarticRoots[k][m+1]);
			
			// If the real component of the root is nonzero and
			// the imaginary component is zero return it
			if ((_quarticRoots[k][m] >= 0) && (_quarticRoots[k][m+1]) == 0.0)
			{
				// Set the real root and then return it
				realRoot = _quarticRoots[k][m];
				
				// Debug statement
				//System.out.println("The real root is: "+realRoot);
				
				return realRoot;
			}
		}	
		// If no real root is found, return -1
		return realRoot;
		
	}
	
	// This function to solves a cubic equation. It returns all the roots in an array 
	// Note: 
	// [1] This has been directly copied from agents.modelbased.mckputil.RootFinding
	// [2] This function is overloaded
	public static final double[][] getCubicRoots(double a, double b, double c, double d) 
	{
		double[][] roots = new double[3][2];
		double A,B,C,Q,R,D;
		A = b/a;
		B = c/a;
		C = d/a;
		Q = (3*B-A*A)/9.0;
		R = (9*A*B - 27*C - 2*A*A*A)/54.0;
		D = Q*Q*Q + R*R;
		if(D >= 0) 
		{
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
		else 
		{
			double theta;
			theta = Math.acos(R/Math.sqrt(-Q*Q*Q));
			roots[0][0] = 2 * Math.sqrt(-Q) * Math.cos(theta/3.0) - A/3.0;
			roots[1][0] = 2 * Math.sqrt(-Q) * Math.cos((theta + 2*Math.PI)/3.0) - A/3.0;
			roots[2][0] = 2 * Math.sqrt(-Q) * Math.cos((theta + 4*Math.PI)/3.0) - A/3.0;
		}
		return roots;
	}
	
	// This function merely returns the quartic roots
	public static final double[][] getCubicRoots(double[] coeff) 
	{
		return getCubicRoots(coeff[0],coeff[1],coeff[2],coeff[3]);
	}
	
	// This function randomly generates coefficients to be used
	// in the cubic solver
	public static final double[] getCubicCoefficients() 
	{
		Random R = new Random(System.nanoTime());
		double[] coeff = new double[4];
		for(int i = 0; i < coeff.length; i++) 
		{
			coeff[i] = R.nextDouble();
		}
		return coeff;
	}
	
	// This function returns the real, quartic root only
	// if it is nonnegative; else this function returns -1
	public static double getRealCubicRoot()
	{
		// Variable to index into the real and imaginary components
		int n = 0;
		
		double realRoot = -1.0;
		
		// For each of the three possible real roots, print them out (if they
		// exist)
		for (int k = 0; k < 3; k++)
		{
			//System.out.println("***** ROOT # "+(k+1)+"*****");
			//System.out.println("The real component is: "+_cubicRoots[k][n]);
			//System.out.println("The imaginary component is: "+_cubicRoots[k][n+1]);
			
			// If the real component of the root is nonzero and
			// the imaginary component is zero return it
			if ((_cubicRoots[k][n] >= 0) && (_cubicRoots[k][n+1]) == 0.0)
			{
				// Set the real root and then return it
				realRoot = _cubicRoots[k][n];
				
				// Debug statement
				//System.out.println("The real root is: "+realRoot);
				
				return realRoot;
			}
		}	
		// If no real root is found, return -1
		return realRoot;
		
	}
	
	// Method that will solve a cubic or quartic equation - as needed
	public static double solve(double[] coefficientsArray)
	{
		double realRoot = -1;
		
		// Debug statement
		/*for (int i = 0; i < 5; i++)
		{
			System.out.println("The coefficients array is: "+coefficientsArray[i]);
		}*/
		
		// Check if the first component in the array is a 0. If
		// it is, then this a quartic equation
		if (coefficientsArray[0] != 0.0)
		{
			
			// Get all the quartic roots
			_quarticRoots = getQuarticRoots(coefficientsArray);
			
			// Extract the real root
			realRoot = getRealQuarticRoot();
		}
		else
		{
			// Array to store cubic coefficients
			double[] cubicCoefficientsArray = new double[4];
			
			// Index to keep track of the cubic coefficients array
			int j = 0;
			
			// Copy all but the first coefficients
			for (int i = 1; i < 5; i++)
			{
				cubicCoefficientsArray[j] = coefficientsArray[i];
				j++;
			}
			
			// Get all the cubic roots
			_cubicRoots = getCubicRoots(cubicCoefficientsArray);
			
			// Extract the real root
			realRoot = getRealCubicRoot();
			
		}
		return realRoot;
	}
	
	
	
	// Main to test the functionality of the cubic and quartic equation solvers
	public static void main(String[] args) 
	{
		// Number of times we want to run the equation solvers 
		int numberOfTrials = 100;
		
		// ***** DEBUG INFO FOR QUARTIC SOLVER*****
//		// Loop to run the equation solver
//		for(int i = 0; i < numberOfTrials; i++) 
//		{
//			double[] quarticCoefficientsArray = getQuarticCoefficients();
//			
//			// ***** DEBUG INFO *****
//			// Statement to print out the coefficients
//			/*for (int j = 0; j < 5; j++)
//			{
//				System.out.println("The coefficients are: "+quarticCoefficientsArray[j]);
//			}*/
//			
//			_quarticRoots = getQuarticRoots(quarticCoefficientsArray);
//			
//			// ***** DEBUG INFO *****
//			// Variable to index into the real and imaginary components
//			//int m = 0;
//			
//			// For each of the four possible real roots, print them out (if they
//			// exist)
//			/*for (int k = 0; k < 4; k++)
//			{
//				System.out.println("***** ROOT # "+(k+1)+"*****");
//				System.out.println("The real component is: "+roots[k][m]);
//				System.out.println("The imaginary component is: "+roots[k][m+1]);	
//			}*/		
//			// Get the only real root
//			double realQuarticRoot = getRealQuarticRoot();
//			
//			// If the realQuarticRoot is not -1, it must be real
//			if (realQuarticRoot != -1.0)
//			{
//				System.out.println("***** REAL QUARTIC ROOT FOUND! *****");
//				System.out.println("Real root is: "+realQuarticRoot);				
//				break;
//			}
//		}
//		
		
		// ***** DEBUG INFO FOR CUBIC SOLVER*****
		// Loop to run the equation solver
//		for(int i = 0; i < numberOfTrials; i++) 
//		{
//			double[] cubicCoefficientsArray = getCubicCoefficients();
//			
//			// ***** DEBUG INFO *****
//			// Statement to print out the coefficients
//			/*for (int j = 0; j < 5; j++)
//			{
//				System.out.println("The coefficients are: "+cubicCoefficientsArray[j]);
//			}*/
//			
//			_cubicRoots = getCubicRoots(cubicCoefficientsArray);
//			
//			// ***** DEBUG INFO *****
//			// Variable to index into the real and imaginary components
//			int n = 0;
//			
//			// For each of the four possible real roots, print them out (if they
//			// exist)
//			for (int k = 0; k < 3; k++)
//			{
//				System.out.println("***** ROOT # "+(k+1)+"*****");
//				System.out.println("The real component is: "+_cubicRoots[k][n]);
//				System.out.println("The imaginary component is: "+_cubicRoots[k][n+1]);	
//			}		
//			// Get the only real root
//			double realCubicRoot = getRealCubicRoot();
//			
//			// If the realCubicRoot is not -1, it must be real
//			if (realCubicRoot != -1)
//			{
//				System.out.println("***** REAL CUBIC ROOT FOUND! *****");
//				System.out.println("Real root is: "+realCubicRoot);
//						break;
//			}
//		}
//		
		// ***** DEBUG INFO *****
		//double[] testArray = {0.0, 1.2, 2.5, 6.5, 7.3};
		double[] testArray = getQuarticCoefficients();
		double test = solve(testArray);
						
		//System.out.println("The real root is:"+test);
						
		
		
	}
		
	
	
//	/*
//	 * Return the advertiser effect and continuation probabilities in the array
//	 */
//	public abstract double[] getPrediction(Query q);
//
//	/*
//	 * QueryReport/SalesReport report contain information about the overall number of
//	 * impressions and clicks that we saw
//	 * 
//	 * impressionsPerSlot contains the number of impressions we saw in each slot with 
//	 * index 0 being the highest slot and the size being the last slot
//	 * 
//	 * advertisersAbovePerSlot is a list that containts the advertisers that were above
//	 * us when we were in any given slot, the first index corresponds to the same slot as
//	 * the impressionsPerSlot varaible
//	 * 
//	 * UserStates contains the actual number of users in ever product in every state
//	 * 
//	 * ads containts the ad that each advertiser placed.  The strings in this hashmap are
//	 * the same as in the advertisersAbovePerSlot
//	 */
//	public abstract boolean updateModel(QueryReport queryReport, 
//										SalesReport salesReport,
//										HashMap<Query,LinkedList<Integer>> impressionsPerSlot,
//										HashMap<Query,LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
//										HashMap<String,HashMap<Query,Ad>> ads,
//										HashMap<Product,HashMap<UserState,Integer>> userStates);
	
}