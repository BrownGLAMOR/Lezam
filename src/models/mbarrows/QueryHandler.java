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

public class QueryHandler implements ConstantsAndFunctions {
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
						_advertiserEffectsBoundsAvg[2],
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



	
	
	
	
	

}
