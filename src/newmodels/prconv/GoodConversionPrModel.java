package newmodels.prconv;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class GoodConversionPrModel extends NewAbstractConversionModel {

	private Limits _limits;
	private Set<Query> _querySpace;

	private HashMap<Query, Double> _wR;
	private HashMap<Query, Double> _wh;

	private int _timeHorizon = 1;

	public GoodConversionPrModel(Set<Query> querySpace) {
		_limits = new Limits();
		_querySpace = querySpace;

		_wR = initHashMap(new HashMap<Query, Double>());
		_wh = initHashMap(new HashMap<Query, Double>());
	}

	public HashMap<Query, Double> initHashMap(HashMap<Query, Double> map) {
		for(Query q : _querySpace) {
//			System.out.println(q);
			map.put(q, (double)0);
		}

		return map;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		for(Query q : _querySpace) {
			double clicks = queryReport.getClicks(q);
			double conversions = salesReport.getConversions(q);
//			System.out.println("UpdateModel");
//			System.out.println("\tClicks:" + clicks + "\tConversions: " + conversions);

			double wr = (1.0 - (1.0 / _timeHorizon)) * _wR.get(q);
			double wh = (1.0 - (1.0 / _timeHorizon)) * _wh.get(q);
			if(Double.isNaN(wr))
				wr = 0.0;
			if(Double.isNaN(wh))
				wh = 0.0;

			double r = (1.0 / _timeHorizon) * clicks;
			double h = (1.0 / _timeHorizon) * conversions;

			_wR.put(q, wr + r);
			_wh.put(q, wh + h);
		}

		return true;
	}

	public void setTimeHorizon(int timeHorizon) {
		_timeHorizon = timeHorizon;
	}	

	@Override
	public double getPrediction(Query query) {
		Double[] lim = _limits.findLimits(_wR.get(query), _wh.get(query), 0);

		return lim[0];
	}

	private class Limits {

		// ----------------------------------------------------------
		// constructors
		// ----------------------------------------------------------

		public Limits() {
		}

		// ----------------------------------------------------------
		// public methods
		// ----------------------------------------------------------

		// R = weighted number of clicks
		// h = weighted number of conversions
		// s = confidence interval in (0,1)
		public Double[] findLimits(double R, double h, double s) {
			Double[] ret = new Double[2];

			if (R > 0) {
				double p = h / R;
				double pp = h + 1.0;
				double qq = R - h + 1.0;

				ret[0] = inverseBeta(0.5 * (1.0 - s), pp, qq, p);
				ret[1] = inverseBeta(0.5 * (1.0 + s), pp, qq, p);

				double A = Math.pow((1.0 / Math.log(2.0)), (-8.0 / (h + 1.0)));
				double B = A - 1.0;

				double multiplier = 0.0;

				if (h < 0.5)
					multiplier = 1.0 / (2.0 * h + 1.0 / (10.0 * h + 3));
				else
					multiplier = 1.0 / (2.0 * h);

				R = R * multiplier;

				double q = Math.pow((A - (B / R)), (1.0 / -8.0));

				ret[0] = q * ret[0];
				ret[1] = q * ret[1];
			} else {
				ret[0] = 0.0;
				ret[1] = 0.0;
			}

			return ret;
		}

		// ----------------------------------------------------------
		// protected/private methods
		// ----------------------------------------------------------

		/**
		 * Returns natural logarithm of gamma function.
		 *
		 * @param x the value
		 * @return natural logarithm of gamma function
		 */
		public double lnGamma(double x) {

			final double LOGPI = 1.14472988584940017414;

			double p, q, w, z;

			double A[] = { 8.11614167470508450300E-4,
					-5.95061904284301438324E-4, 7.93650340457716943945E-4,
					-2.77777777730099687205E-3, 8.33333333333331927722E-2 };
			double B[] = { -1.37825152569120859100E3,
					-3.88016315134637840924E4, -3.31612992738871184744E5,
					-1.16237097492762307383E6, -1.72173700820839662146E6,
					-8.53555664245765465627E5 };
			double C[] = {
					/* 1.00000000000000000000E0, */
					-3.51815701436523470549E2, -1.70642106651881159223E4,
					-2.20528590553854454839E5, -1.13933444367982507207E6,
					-2.53252307177582951285E6, -2.01889141433532773231E6 };

			if (x < -34.0) {
				q = -x;
				w = lnGamma(q);
				p = Math.floor(q);
				if (p == q)
					throw new ArithmeticException("lnGamma: Overflow");
				z = q - p;
				if (z > 0.5) {
					p += 1.0;
					z = p - q;
				}
				z = q * Math.sin(Math.PI * z);
				if (z == 0.0)
					throw new ArithmeticException("lnGamma: Overflow");
				z = LOGPI - Math.log(z) - w;
				return z;
			}

			if (x < 13.0) {
				z = 1.0;
				while (x >= 3.0) {
					x -= 1.0;
					z *= x;
				}
				while (x < 2.0) {
					if (x == 0.0)
						throw new ArithmeticException("lnGamma: Overflow");
					z /= x;
					x += 1.0;
				}
				if (z < 0.0)
					z = -z;
				if (x == 2.0)
					return Math.log(z);
				x -= 2.0;
				p = x * polevl(x, B, 5) / p1evl(x, C, 6);
				return (Math.log(z) + p);
			}

			if (x > 2.556348e305)
				throw new ArithmeticException("lnGamma: Overflow");

			q = (x - 0.5) * Math.log(x) - x + 0.91893853320467274178;

			if (x > 1.0e8)
				return (q);

			p = 1.0 / (x * x);
			if (x >= 1000.0)
				q += ((7.9365079365079365079365e-4 * p - 2.7777777777777777777778e-3)
						* p + 0.0833333333333333333333)
						/ x;
			else
				q += polevl(p, A, 4) / x;
			return q;
		}

		private double p1evl(double x, double coef[], int N) {

			double ans;
			ans = x + coef[0];

			for (int i = 1; i < N; i++)
				ans = ans * x + coef[i];

			return ans;
		}

		private double polevl(double x, double coef[], int N) {

			double ans;
			ans = coef[0];

			for (int i = 1; i <= N; i++)
				ans = ans * x + coef[i];

			return ans;
		}


		/*	        private double lnGamma(double xx) {
	            double ser = 1.000000000190015;
	            double x = xx;
	            double y = xx;
	            double tmp = x + 5.5;

	            tmp -= (x + 0.5) * Math.log(tmp);

	            for (int j = 0; j < LNGAMMA_COF.length; j++) {
	                y += 1;
	                ser += LNGAMMA_COF[j] / y;
	            }

	            return -tmp + Math.log(2.5066282746310005 * ser / x);
	        }
		 */
		private double betaPDF(double z, double a, double b) {
			if ((z == 0) || (z == 1))
				return EPS;

			double num = lnGamma(a + b) + (a - 1.0) * Math.log(z) + (b - 1.0)
			* Math.log(1.0 - z);
			double denom = lnGamma(a) + lnGamma(b);

			return Math.exp(num - denom);
		}

		private double betaCF(double x, double a, double b) {
			double qab = a + b;
			double qap = a + 1.0;
			double qam = a - 1.0;
			double c = 1.0;
			double d = 1.0 - qab * x / qap;

			if (Math.abs(d) < DBL_EPS)
				d = DBL_EPS;

			d = 1.0 / d;

			double hh = d;

			for (int m = 1; m <= ITMAX; m++) {
				double m2 = 2.0 * m;
				double aa = m * (b - m) * x / ((qam + m2) * (a + m2));

				d = 1.0 + aa * d;

				if (Math.abs(d) < DBL_EPS)
					d = DBL_EPS;

				c = 1.0 + aa / c;

				if (Math.abs(c) < DBL_EPS)
					c = DBL_EPS;

				d = 1.0 / d;
				hh = hh * (d * c);

				aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));

				d = 1.0 + aa * d;

				if (Math.abs(d) < DBL_EPS)
					d = DBL_EPS;

				c = 1.0 + aa / c;

				if (Math.abs(c) < DBL_EPS)
					c = DBL_EPS;

				d = 1.0 / d;
				double del2 = d * c;
				hh = hh * del2;

				if (Math.abs(del2 - 1.0) < EPS)
					break;
			}

			return hh;
		} 

		double betaCDF(double z, double a, double b) {
			double y = -7;

			if (z == 0 || z == 1)
				return z;

			double norm = Math.exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a
					* Math.log(z) + b * Math.log(1.0 - z));

			if (z < (a + 1.0) / (a + b + 2.0)) {
				double tmp = betaCF(z, a, b);
				if (tmp >= 0)
					y = norm * tmp / a;
			} else {
				double tmp = betaCF(1.0 - z, b, a);
				if (tmp >= 0)
					y = 1.0 - norm * tmp / b;
			}

			return y;
		} 

		double inverseBeta(double z, double a, double b, double p) {
			// by Newton-Raphson integration
			// select initial guess for x

			double x0 = p;
			double x = 0;

			if (p == 0)
				x0 = 1e-6;

			if (p == 1)
				x0 = 1.0 - (1e-6);

			for (int j = 0; j < ITMAX; j++) {
				double tmp = betaCDF(x0, a, b);

				if (tmp < 0)
					return tmp;

				tmp = (tmp - z) / betaPDF(x0, a, b);
				x = x0 - Math.min(0.5 * x0, tmp);

				if (x <= 0.0 || x >= 1.0)
					return -3;

				if (Math.abs((x - x0) / x) < EPS)
					break;
				x0 = x;
			}

			return x;
		} 

		// ----------------------------------------------------------
		// protected/private constants
		// ----------------------------------------------------------

		private static final double ITMAX = 100;

		private static final double EPS = 1e-12;

		private static final double DBL_EPS = 1e-16;
		// private static final double TOL = 1E-7;
	}
}
