package newmodels;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
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
			int[]    lb = {0, 0, 0, 0, 0};
			int[]    ub = {numImps, numImps, numImps, numImps, numImps};
			IloNumVar[] x  = cplex.intVarArray(5, lb, ub);

			cplex.addMaximize(cplex.sum(x[0], x[1], x[2], x[3], x[4]));


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
}
