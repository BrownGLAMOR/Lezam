package simulator;

import java.util.ArrayList;

public class SimMisc {

	//Floyd's cycle-finding algorithm
	private boolean cycleDetect(ArrayList<double[]> bidVectors) {
		int len = bidVectors.size();
		int tortoise= 1;
		int hare = 2;
		while(hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			tortoise += 1;
			hare += 2;
		}

		if(hare > len) {
			return false;
		}

		System.out.println("Tortoise: " + tortoise);
		System.out.println("Hare: " + hare);

		int mu = 0;
		hare = tortoise;
		tortoise = 0; 
		while(tortoise < len && hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			tortoise += 1;
			hare += 1;
			mu += 1;
		}

		System.out.println("Mu: " + mu);

		int lam = 1;
		hare = tortoise + 1;
		while(tortoise < len && hare < len && !bidVectorequal(bidVectors.get(tortoise),bidVectors.get(hare))) {
			hare += 1;
			lam += 1;
		}
		System.out.println("Lam: " + lam);
		return true;
	}

	private boolean bidVectorequal(double[] vector1, double[] vector2) {
		if(vector1.length != vector2.length) {
			throw new RuntimeException("Vectors unequal length");
		}
		double epsilon = .005;
		for(int i = 0; i < vector1.length; i++) {
			if(vector1[i] - vector2[i] > epsilon) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Bin-by-bin comparisons
	 * 
	 * ref: http://www.cs.cmu.edu/~efros/courses/AP06/Papers/rubner-jcviu-00.pdf
	 */

	/*
	 * Kullback Leibler Divergence Test
	 */
	public double KLDivergence(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("KL Divergence requires arrays of equal length");
		}

		double divergence = 0.0;

		for(int i = 0; i < P.length; i++) {
			divergence += P[i] * (Math.log(P[i])-Math.log(Q[i]));
		}

		return divergence;
	}

	/*
	 * When P and Q are discrete, we can get the likelihood of Q from
	 * the KL divergence.  We want to minimize the KL divergence, which will
	 * in turn maximize the likelihood
	 */
	public double KLLikelihood(double[] P, double[] Q) {
		double divergence = KLDivergence(P, Q);
		double likelihood = Math.exp(-1*divergence*P.length);
		return likelihood;
	}

	/*
	 * The Minkowski Distance uses the L_p norm, so we define a p
	 * 
	 * Probably want to use 2.....
	 */
	public double minkowskiDistance(double[] P, double[] Q, int p) {
		if(P.length != Q.length) {
			throw new RuntimeException("Minkowski Distance requires arrays of equal length");
		}

		double distance = 0.0;

		for(int i = 0; i < P.length; i++) {
			distance += Math.pow(Math.abs(P[i] - Q[i]),p);
		}

		distance = Math.pow(distance,1.0/p);

		return distance;
	}

	/*
	 * When the areas are equal 
	 */
	public double histogramIntersection(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("Histogram Intersection requires arrays of equal length");
		}

		double distance = 1.0;

		double temp1 = 0.0;
		double temp2 = 0.0;

		for(int i = 0; i < P.length; i++) {
			temp1 += Math.min(P[i], Q[i]);
			temp2 += Q[i];
		}

		distance -= temp1/temp2;

		return distance;
	}

	public double jeffreyDivergence(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("Jeffrey Divergence requires arrays of equal length");
		}

		double divergence = 0.0;

		for(int i = 0; i < P.length; i++) {
			if(P[i] == 0 && Q[i] == 0) {
				continue;
			}
			double m = (P[i] + Q[i])/2.0;
			divergence += P[i] * Math.log(P[i] / m) + Q[i] * Math.log(Q[i] / m);
		}

		return divergence;
	}

	/*
	 * This is equivalent to the Earth Mover's Distance in 1 dimensional
	 * histograms with equal area, and with equal bin sizes.
	 */
	public double matchDistance(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("Match Distance requires arrays of equal length");
		}

		double distance = 0.0;
		double pCum = 0.0;
		double qCum = 0.0;

		for(int i = 0; i < P.length; i++) {
			pCum += P[i];
			qCum += Q[i];
			distance += Math.abs(pCum-qCum);
		}

		return distance;
	}

	/*
	 * Kolmogorov-Smirnov
	 */
	public double KSdistance(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("KS Distance requires arrays of equal length");
		}

		double distance = 0.0;
		double pCum = 0.0;
		double qCum = 0.0;

		for(int i = 0; i < P.length; i++) {
			pCum += P[i];
			qCum += Q[i];
			distance = Math.max(distance,Math.abs(pCum-qCum));
		}

		return distance;
	}

}
