package newmodels;


import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;

public class AvgPosToPos {


	public static void main(String[] args) {
		try {
			IloCplex cplex = new IloCplex();
			cplex.setParam(IntParam.SolnPoolIntensity, 4);
			cplex.setParam(IntParam.SolnPoolReplace, 2);
			cplex.setParam(IntParam.SolnPoolCapacity, 1000000);

			int numImps = 50;
			double avgPos = 3.27;
			int numClicks = 5;
			Query q = new Query(null,null);
			int[]    lb = {0, 0, 0, 0, 0};
			int[]    ub = {numImps, numImps, numImps, numImps, numImps};
			IloIntVar[] x  = cplex.intVarArray(5, lb, ub);

			double[] clickPr = new double[5];
			clickPr = getClickPr(q);

			cplex.addMinimize(cplex.abs(cplex.diff(cplex.sum(cplex.prod(clickPr[0],x[0]),
					cplex.prod(clickPr[1],x[1]),
					cplex.prod(clickPr[2],x[2]),
					cplex.prod(clickPr[3],x[3]),
					cplex.prod(clickPr[4],x[4])), numClicks)));
			
			
			cplex.addEq(cplex.sum(cplex.prod( 1.0, x[0]),
					cplex.prod( 2.0, x[1]),
					cplex.prod( 3.0, x[2]),
					cplex.prod( 4.0, x[3]),
					cplex.prod( 5.0, x[4])), Math.ceil(numImps * avgPos));

			cplex.addEq(cplex.sum(x[0],	x[1], x[2], x[3], x[4]), numImps);

			cplex.setOut(null);
			boolean flag = true;
			int lastVal = 0;
			while(flag) {
				cplex.populate();
				System.out.println(cplex.getSolnPoolNsolns());
				if(lastVal == cplex.getSolnPoolNsolns()) {
					flag = false;
				}
				lastVal = cplex.getSolnPoolNsolns();
			}

			//				if (cplex.populate()) {
			//					for(int k = 0; k < cplex.getSolnPoolNsolns(); k++) {
			//						cplex.output().println("Solution status = " + cplex.getStatus());
			//						cplex.output().println("Solution value  = " + cplex.getObjValue());
			//
			//						double[] val = cplex.getValues(x,k);
			//						int ncols = cplex.getNcols();
			//						for (int j = 0; j < ncols; ++j)
			//							cplex.output().println("Column: " + j + " Value = " + val[j]);
			//						cplex.delSolnPoolSoln(0);
			//					}
			//				}
			cplex.end();
		}
		catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
	}

	public static double[] getClickPr(Query q) {
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
		clickPrs[0] = prClick;
		for(int i = 1; i < 5; i++) {
			clickPrs[i] = prClick * (clickPrs[i-1] * (1-prConv) + (1 - clickPrs[i-1]) * (clickPrs[i-1] / prClick)) * prCont;
		}

		return clickPrs;
	}
}
