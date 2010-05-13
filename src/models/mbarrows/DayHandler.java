package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.complex.Complex;

import models.usermodel.TacTexAbstractUserModel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

public class DayHandler {
	// Target Effect
	final double _TE = 0.5;
	
	// Promoted Slot Bonus
	final double _PSB =  0.5;
	
	// Component Specialty Bonus
	final double _CSB = 0.6;
	
	// Advertiser effect  lower bound <> upper bound
	final double[][] _advertiserEffectsBounds = { {0.2, 0.3},
											      {0.3, 0.4},
											      {0.4, 0.5}
										  };
	
	// Average advertiser effect  
	final double[] _advertiserEffectsBoundsAvg = {0.25,0.35,0.45};

	
	// Continuation Probability  lower bound <> upper bound
	final double[][] _continuationProbBounds = { {0.2, 0.5},
										         {0.3, 0.6},
										         {0.4, 0.7}
		  								 };
	
	final double[] _continuationProbBoundsAvg = {0.35,0.45,0.55};
	
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
	double otherAdvertiserEffects;
	double otherAdvertiserConvProb;

	double curProbClick;
	double a,b,c,d,e;
	
	double currentEstimate;

	public DayHandler(Query q, int totalClicks,
			LinkedList<Integer> impressionsPerSlot, double ourAdvertiserEffect,
			LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot, // <our slot < their slots <ad>>
			HashMap<Product,int[]> userStatesOfSearchingUsers) // [IS, non-IS]
	{
		double[] coeff = new double[5];
		if (saw slot 5)
		{
			a = something;
		}
		else 
		{
			a = 0;
		}
		
		if (saw slot 4)
		{
			b = something;
		}
		else 
		{
			b = 0;
		}
		if (saw slot 3)
		{
			c = something;
		}
		else 
		{
			c = 0;
		}
		if (saw slot 2)
		{
			d = something;
		}
		else 
		{
			d = 0;
		}
			
		e = -1.0 * totalClicks;

	
		// TODO Auto-generated constructor stub
	}

	public static final double[][] getQuarticRoots(double a, double b, double c, double d, double e) 
	{
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
	
	public static final double[][] getQuarticRoots(double[] coeff) {
		return getQuarticRoots(coeff[0],coeff[1],coeff[2],coeff[3],coeff[4]);
	}
	
	public void updateEstimate(double ourAdvertiserEffect) {
		// currentEstimate =
	}

	public double getContinuationProbability(double ourAdvertiserEffect) {
		updateEstimate(ourAdvertiserEffect);
		return getContinuationProbability();
	}

	public double getContinuationProbability() {
		return currentEstimate;
	}

}
