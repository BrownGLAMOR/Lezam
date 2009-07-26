package newmodels;

/**
 * @author jberg
 */

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AvgPosToPos extends AbstractModel {

	private IloCplex _cplex;
	private int _numSols;
	private double _promBonus= .5;

	public AvgPosToPos(int numSols) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setParam(IntParam.SolnPoolIntensity, 4);
			cplex.setParam(IntParam.SolnPoolReplace, 2);
			cplex.setParam(IntParam.SolnPoolCapacity, 1000000);
			_cplex = cplex;
			_numSols = numSols;
		} catch (IloException e) {
			e.printStackTrace();
		}
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
	public double[] getPrediction(Query query, int regImps, int promImps, double avgPos, int numClicks, int numPromSlots) {
		try {
			double start = System.currentTimeMillis();
			_cplex.clearModel();

			int numImps = regImps + promImps;

			int[] lb = {0, 0, 0, 0, 0};
			int[] ub = {numImps, numImps, numImps, numImps, numImps};
			IloIntVar[] x;
			x = _cplex.intVarArray(5, lb, ub);

			double[] clickPr = new double[5];
			clickPr = getClickPr(query,numPromSlots);

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

			if(numPromSlots == 0) {
				_cplex.addEq(_cplex.sum(x[0], x[1], x[2], x[3], x[4]), numImps);
			}
			else if(numPromSlots == 1) {
				_cplex.addEq(x[0], promImps);
				_cplex.addEq(_cplex.sum(x[1], x[2], x[3], x[4]), regImps);
				_cplex.addEq(_cplex.sum(x[0], x[1], x[2], x[3], x[4]), numImps);
			}
			else if(numPromSlots == 2) {
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
			
			System.out.println(_cplex.getSolnPoolNsolns());

			double[] solution = new double[5];

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

			_cplex.end();
			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			System.out.println("This took " + (elapsed / 1000) + " seconds");
			return solution;
		}
		catch (IloException e) {
			e.printStackTrace();
			return null;
		}
	}

	public double[] getClickPr(Query q, int numPromSlots) {
		double prConv, prClick, prCont;

		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			prConv = .1;
			prClick = .25;
			prCont = .35;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
			prConv = .2;
			prClick = .35;
			prCont = .45;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
			prConv = .3;
			prClick = .45;
			prCont = .55;
		}
		else {
			throw new RuntimeException("Malformed query");
		}

		double[] clickPrs = new double[5];
		if(numPromSlots > 0) {
			clickPrs[0] = eta(prClick,_promBonus);
		}

		for(int i = 1; i < 5; i++) {
			if(numPromSlots > i) {
				clickPrs[i] = eta(prClick,_promBonus) * (clickPrs[i-1] * (1-prConv) + (1 - clickPrs[i-1]) * (clickPrs[i-1] / prClick)) * prCont;
			}
		}

		return clickPrs;
	}

	public double eta(double p, double x) {
		return (p*x)/(p*x + (1-p));
	}
	
	public static void main(String[] args) {
		AvgPosToPos avgPosModel = new AvgPosToPos(40);
		double[] sols = avgPosModel.getPrediction(new Query("tv",null), 25, 100, 1.9, 25, 2);
		for(int i = 0; i < sols.length; i++) {
			System.out.println("Slot " + (i+1) + ": " + sols[i]);
		}
	}
}
