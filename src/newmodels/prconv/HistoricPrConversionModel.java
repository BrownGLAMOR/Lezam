package newmodels.prconv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.AbstractModel;

public class HistoricPrConversionModel extends NewAbstractConversionModel {

	private PrMath _math;
	private Set<Query> _querySpace;
	
	private int _timeHorizon;
	private HashMap<Query, ArrayList<double[]>> _curves;

	public HistoricPrConversionModel(Set<Query> querySpace) {
		_math = new PrMath();
		_querySpace = querySpace;
		
		_curves = new HashMap<Query, ArrayList<double[]>>();
		for(Query q : querySpace) {
			ArrayList<double[]> c = new ArrayList<double[]>();
			_curves.put(q, c);
		}
		_timeHorizon = 1;
	}

	public HistoricPrConversionModel() {
		_math = new PrMath();
	}

	public void setTimeHorizon(int t) {
		_timeHorizon = t;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		for(Query q : _querySpace) {
			int clicks = queryReport.getClicks(q);
			int conversions = salesReport.getConversions(q);
			//System.out.println("UpdateModel");
			//System.out.println("\tClicks:" + clicks + "\tConversions: " + conversions);			

			ArrayList<double[]> curves = _curves.get(q);
			for(int i = 0; i < Math.min(_timeHorizon - 2, curves.size() - 1); i++) {
				double[] dist = curves.get(i+1);
				dist = _math.prGivenObs(clicks, conversions, dist);
				curves.set(i, dist);
			}
			
			double[] dist = _math.prGivenObs(clicks, conversions, null);
			if(curves.size() < _timeHorizon)
				curves.add(dist);
			else
				curves.set(_timeHorizon - 1, dist);
			
			//System.out.println("Updated " + q + " / " + curves.size());
		}
		
		return true;
	}

	@Override
	public double getPrediction(Query query) {
		double[] curve = _curves.get(query).get(0);
		
		return _math.getMostLikelyProb(0.5, curve);
	}	

	private class PrMath {
		private double[] A;
		
		static private final int DISTRIBUTION_SIZE = 100;
		
		public PrMath() {
			this.reset();			
		}
		
		public void reset() {
			A = new double[DISTRIBUTION_SIZE];
			A[0] = 0;
			for(int i = 1; i < DISTRIBUTION_SIZE; i++) {
				A[i] = (double)i / (double)DISTRIBUTION_SIZE;
			}			
		}
		
		public double getMostLikelyProb(double interval, double[] dist) {
			final double EPSILON = 0.005;
						
			// Since we're dealing with probability functions, the total area is (had better be) always 1.0
			// So, we're looking for the range that integrates to "inverval"
			
			// Do a binary search to find the best spot
			int rangeStart = 0;
			int rangeEnd = DISTRIBUTION_SIZE;
			
			int start = 0;
			int end = (int) Math.floor(0.5 * DISTRIBUTION_SIZE);
			double area = 0;
			boolean add = true;
			boolean done = false;
			do {
				if(add)
					area += integrate(dist, start, end);
				else
					area -= integrate(dist, start, end);
				/*System.out.print(start + " - " + end + " ");
				if(add)
					System.out.print("(add): ");
				else
					System.out.print("(sub): ");
				System.out.println(area); */
				
				if(Math.abs(area - interval) > EPSILON) {
					if(area > interval) {
						// too much area
						add = false;
						rangeEnd = rangeStart + (int) Math.floor(0.5 * (rangeEnd - rangeStart));
						
						start = rangeStart + (int) Math.floor(0.5 * (rangeEnd - rangeStart));
						end = rangeEnd;
					} else {
						// not enough
						add = true;
						rangeStart = rangeStart + (int) Math.floor(0.5 * (rangeEnd - rangeStart));
						
						start = rangeStart;
						end = rangeStart + (int) Math.floor(0.5 * (rangeEnd - rangeStart));
					}
					if(rangeStart == rangeEnd)
						done = true;
				} else
					done = true;
			} while(!done);
			
			//System.out.println(A[end]);
			return A[end];
		}
		
		
		public double integrate(double[] c, int start, int end) {
			if(start == end)
				return c[start];

			final double w = (1.0 / (double)DISTRIBUTION_SIZE);
			
			double a = 0;
/*			for(int i = start; i < end - 1; i++) {
				double f, s;
				if(c[i] < c[i+1]) {
					f = c[i];
					s = c[i+1];
				} else {
					f = c[i+1];
					s = c[i];
				}
				
				a += (w * f) + (0.5 * (w * (s-f)));
			} */
			for(int i = start; i < end; i++)
				a += c[i];
			
			return a;
		}
		
		// Return the probability distribution of n clicks and k conversions
		public double[] prGivenObs(int n, int k, double[] prA) {
			double[] ret = new double[DISTRIBUTION_SIZE];
			if(prA == null) {
				prA = new double[DISTRIBUTION_SIZE];
				for(int i = 0; i < prA.length; i++)
					prA[i] = 1.0 / DISTRIBUTION_SIZE;
			}
			double comb = numCombinations(n,k);
			
			double ttl = 0;
			for(int i = 0; i < DISTRIBUTION_SIZE; i++) {
				ret[i] = prA[i] * comb * Math.pow(A[i], k) * (Math.pow((1.0 - A[i]), (n-k)));
				ttl += ret[i];
			}
			
			// Normalize...
			if(ttl != 1)
				for(int i = 0; i < DISTRIBUTION_SIZE; i++)
					ret[i] /= ttl;
			
			return ret;
		}
				
		
		private double numCombinations(int n, int k) {
			if(k == 0)
				return 1.0;
			
			double prod = (double)n / (double)k;
			for(int i = 1; i < k; i++)				
				prod *= (((double)(n-i)) / ((double)(k-i)));
			
			return prod;
			
		}
	}
}
