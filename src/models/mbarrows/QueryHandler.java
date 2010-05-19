package models.mbarrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math.complex.Complex;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class QueryHandler extends ConstantsAndFunctions {
	final Query _query;
	final QueryType _queryType;
	
	final static double EPSILON = .0001;

	// 1st - promoted
	// 2nd - targeted - 1 is targeted, 2 is targeted incorrectly
	// 3rd - numerator, denominator
	double targetedPromoted[][][];
	double fTargetfPro[][];

	public QueryHandler(Query q) {
		
		
		_query = q;
		_queryType = q.getType();

		targetedPromoted = new double[3][2][2];
		fTargetfPro = new double[3][2];
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				fTargetfPro[targeted][promoted] = 1;
				if (targeted == 1) {
					fTargetfPro[targeted][promoted] *= 1 + _TE;
				} else if (targeted == 2) {
					fTargetfPro[targeted][promoted] /= 1 + _TE;
				}
				if (promoted == 1) {
					fTargetfPro[targeted][promoted] *= 1 + _PSB;
				}
				targetedPromoted[targeted][promoted][0] = forwardClickProbability(
						_advertiserEffectBoundsAvg[2],
						fTargetfPro[promoted][targeted]);
				targetedPromoted[targeted][promoted][1] = 1;
			}
		}
	}

	// Returns advertiser effect and continuation probability
	public double[] getPredictions() {
		double tempAdvertiserEffect = 0;
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				tempAdvertiserEffect += inverseClickProbability(
						targetedPromoted[targeted][promoted][0]
								/ targetedPromoted[targeted][promoted][1],
						fTargetfPro[promoted][targeted]);
			}
		}
		// For now! TODO: later test results for this compared to weighted average
		tempAdvertiserEffect /= 6;

		double tempContinuationProb = 0;
		// TODO
		double[] tempArr = { tempAdvertiserEffect, tempContinuationProb };
		return tempArr;
	}
	
	public boolean update(QueryReport queryReport,
			SalesReport salesReport, LinkedList<Integer> impressionsPerSlot,
			LinkedList<LinkedList<String>> advertisersAbovePerSlot,
			HashMap<String, Ad> ads,
			HashMap<Product, HashMap<UserState, Integer>> userStates){
		return false;
	}
	
	public static final double[] getQuarticCoefficients() {
		Random R = new Random();
		double[] coeff = new double[5];
		for(int i = 0; i < coeff.length; i++) {
			coeff[i] = R.nextDouble();
		}
		return coeff;
	}
	
	public static final double[][] getQuarticRoots(double[] coeff) {
		return getQuarticRoots(coeff[0],coeff[1],coeff[2],coeff[3],coeff[4]);
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
		R1 = T3.multiply(T3).subtract(T2.multiply(T2).multiply(T2));
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
	
	public static void main(String[] args) {
		//		double topStart = System.nanoTime();
		//		double numTrials = 500000.0;
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
		//			//			System.out.println(checkCubicSolution(coeff, roots) + ", x1 = " + roots[0][0] + " i*" + roots[0][1] + ", x2 = " + roots[1][0] + " i*" + roots[1][1] + ", x3 = " + roots[2][0] + " i*" + roots[2][1]);
		//		}
		//		double end = System.nanoTime();
		//		System.out.println(time/numTrials + " ms per equation");
		//		System.out.println("Total Time: " + (end-topStart)/1000000000.0);
		//		System.out.println("% below threshold: " + (badCounter/numTrials)*100);

		double topStart = System.nanoTime();
		double numTrials = 1;//100000.0;
		double time = 0;
		int badCounter = 0;
		for(int i = 0; i < numTrials; i++) {
			double[] coeff = getQuarticCoefficients();
			double start = System.nanoTime();
			double[][] roots = getQuarticRoots(coeff);
			
			System.out.println("The roots are:");
			
			System.out.println("The roots.length is:"+roots.length);
			
			for (int j = 0; j < roots.length; j++)
			{
				System.out.println(roots[j][0]);
				
			}
			double end = System.nanoTime();
			time += (end-start)/1000000.0;
			//boolean bool = checkQuarticSolution(coeff, roots);
			//if(!bool) {badCounter++;}
			//System.out.println(bool + ", x1 = " + roots[0][0] + " i*" + roots[0][1] + ", x2 = " + roots[1][0] + " i*" + roots[1][1] + ", x3 = " + roots[2][0] + " i*" + roots[2][1] + ", x4 = " + roots[3][0] + " i*" + roots[3][1]);
		}
		double end = System.nanoTime();
		System.out.println(time/numTrials + " ms per equation");
		System.out.println("Total Time: " + (end-topStart)/1000000000.0);
		System.out.println("% below threshold: " + (badCounter/numTrials)*100);
		
		
		
	}

//	@Override
//	public double[] getPrediction(Query q) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean updateModel(
//			QueryReport queryReport,
//			SalesReport salesReport,
//			HashMap<Query, LinkedList<Integer>> impressionsPerSlot,
//			HashMap<Query, LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
//			HashMap<String, HashMap<Query, Ad>> ads,
//			HashMap<Product, HashMap<UserState, Integer>> userStates) {
//		// TODO Auto-generated method stub
//		return false;
//	}



	
	
	
	
	

}
