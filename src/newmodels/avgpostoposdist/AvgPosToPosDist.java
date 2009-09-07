package newmodels.avgpostoposdist;

/**
 * @author jberg
 */

import newmodels.AbstractModel;
import newmodels.postoprclick.AbstractPosToPrClick;
import newmodels.postoprclick.BasicPosToPrClick;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AvgPosToPosDist extends AbstractModel {

	private static final boolean forceToPos = true;
	private IloCplex _cplex;
	private int _numSols = 50;
	/*
	 * A note on the click pr model:
	 * 
	 * Most clickPr models take avgPos to something, but for this
	 * model we only care about integer values.  Therefore an appropriate
	 * model should be supplied.
	 */
	private AbstractPosToPrClick _clickPrModel;
	private int _numPromSlots;

	public AvgPosToPosDist(int numSols, int numPromSlots, AbstractPosToPrClick model) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setOut(null);
			cplex.setParam(IntParam.SolnPoolIntensity, 4);
			cplex.setParam(IntParam.SolnPoolReplace, 2);
			cplex.setParam(IntParam.SolnPoolCapacity, 1000000);
			_cplex = cplex;
			_numSols = numSols;
		} catch (IloException e) {
			e.printStackTrace();
		}
		_clickPrModel = model;
		_numPromSlots = numPromSlots;
	}

	private boolean doubleEquals(double position, double d) {
		double epsilon = .01;
		double diff = Math.abs(position - d);
		if(diff <= epsilon) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param query The query we are analyzing
	 * @param numImps The number of impressions we observed
	 * @param avgPos The average position we observed
	 * @param numClicks The number of clicks we observed
	 * @param numPromSlots The number of promoted slots
	 * @return
	 */
	public double[] getPrediction(Query query, int regImps, int promImps, double avgPos, int numClicks) {
		try {
			
			if(Double.isNaN(avgPos)) {
				double[] ans = {0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
				return ans;
			}
			
			/*
			 * If the average position returned is an integer, it is likely that a person was in that position
			 * the entire time.  If the forceToPos flag is on then we want to return an array with impressions
			 * only in the position specified
			 */
			if(forceToPos && doubleEquals(avgPos,Math.floor(avgPos))) {
				double[] ans = new double[6];
				for(int i = 0; i < 5; i++) {
					if((i+1) == avgPos) {
						ans[i] = regImps+promImps;
					}
					else {
						ans[i] = 0;
					}
				}
				ans[5] = 0.0;
				return ans;
			}
			double start = System.currentTimeMillis();

			_cplex.clearModel();

			int numImps = regImps + promImps;

			int[] lb = {0, 0, 0, 0, 0};
			int[] ub = {numImps, numImps, numImps, numImps, numImps};
			IloIntVar[] x;
			x = _cplex.intVarArray(5, lb, ub);

			double[] clickPr = new double[5];
			for(int i = 1; i <= clickPr.length; i++) {
				clickPr[i-1] = _clickPrModel.getPrediction(query,i, new Ad());
			}

			_cplex.addMinimize(_cplex.abs(_cplex.diff(_cplex.sum(_cplex.prod(clickPr[0],x[0]),
					_cplex.prod(clickPr[1],x[1]),
					_cplex.prod(clickPr[2],x[2]),
					_cplex.prod(clickPr[3],x[3]),
					_cplex.prod(clickPr[4],x[4])), numClicks)));


			_cplex.addEq(_cplex.sum(_cplex.prod( 1.0, x[0]),
					_cplex.prod( 2.0, x[1]),
					_cplex.prod( 3.0, x[2]),
					_cplex.prod( 4.0, x[3]),
					_cplex.prod( 5.0, x[4])), Math.ceil(numImps * avgPos));

			/*
			 * There is a problem when we don't get the promoted reserve price and we expect promoted impressions
			 * but don't get any.  This should fix most problems but is a hack
			 */
			if(_numPromSlots == 0 || (_numPromSlots == 1.0 && avgPos < 2.0 && promImps == 0) || (_numPromSlots == 2.0 && avgPos < 3.0 && promImps == 0)) {
				_cplex.addEq(_cplex.sum(x[0], x[1], x[2], x[3], x[4]), numImps);
			}
			else if(_numPromSlots == 1) {
				_cplex.addEq(x[0], promImps);
				_cplex.addEq(_cplex.sum(x[1], x[2], x[3], x[4]), regImps);
				_cplex.addEq(_cplex.sum(x[0], x[1], x[2], x[3], x[4]), numImps);
			}
			else if(_numPromSlots == 2) {
				_cplex.addEq(_cplex.sum(x[0], x[1]), promImps);
				_cplex.addEq(_cplex.sum(x[2], x[3], x[4]), regImps);
				_cplex.addEq(_cplex.sum(x[0], x[1], x[2], x[3], x[4]), numImps);
			}
			else {
				throw new RuntimeException("Model currently doesn't support more than 2 prom slots");
			}

			int lastNumSols = 0;
			while(_cplex.getSolnPoolNsolns() < _numSols) {
				_cplex.populate();
				if(lastNumSols == _cplex.getSolnPoolNsolns()) {
					break;
				}
				lastNumSols = _cplex.getSolnPoolNsolns();
			}

			//			System.out.println(_cplex.getSolnPoolNsolns());

			double[] solution = new double[6];

			for(int i = 0; i < 5; i++) {
				solution[i] = 0;
			}

			for(int i = 0; i < _cplex.getSolnPoolNsolns(); i++) {
				//				_cplex.output().println("Solution value  = " + _cplex.getObjValue(i));
				double[] val = _cplex.getValues(x,i);
				for (int j = 0; j < 5; ++j) {
					//					_cplex.output().println("Column: " + j + " Value = " + val[j]);
					solution[j] = solution[j] + val[j];
				}
			}

			for(int i = 0; i < 5; i++) {
				solution[i] = solution[i] / ((double)_cplex.getSolnPoolNsolns());
			}
			
			solution[5] = 0.0;

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			//			System.out.println("This took " + (elapsed / 1000) + " seconds");
			return solution;
		}
		catch (IloException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public AbstractModel getCopy() {
		return new AvgPosToPosDist(_numSols,_numPromSlots,_clickPrModel);
	}

	public static void main(String[] args) {
		int numPromSlots = 2;
		AvgPosToPosDist avgPosModel = new AvgPosToPosDist(40,numPromSlots,new BasicPosToPrClick(numPromSlots));
		double[] sols = avgPosModel.getPrediction(new Query("tv",null), 25, 100, 1.9, 25);
		for(int i = 0; i < sols.length; i++) {
			System.out.println("Slot " + (i+1) + ": " + sols[i]);
		}
	}
}
