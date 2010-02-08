package simulator;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class CPLEXKnapsack {

	public static int solveWithCPLEX(String filename) throws IOException {
		int profit = 0;
		try {
			/*
			 * Setup Maximization
			 */
			IloCplex _cplex = new IloCplex();
			_cplex.setOut(null);

			double start = System.nanoTime();
			BufferedReader input =  new BufferedReader(new FileReader(filename));

			String firstLine = input.readLine();
			StringTokenizer capacityTokenizer = new StringTokenizer(firstLine);
			String capacityString = capacityTokenizer.nextToken();
			int	capacity = Integer.parseInt(capacityString);

			String line;
			ArrayList<Integer> values = new ArrayList<Integer>();
			ArrayList<Integer> weights = new ArrayList<Integer>();
			while ((line = input.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				if(st.hasMoreTokens()) {
					values.add(Integer.parseInt(st.nextToken()));
					if(st.hasMoreTokens()) {
						weights.add(Integer.parseInt(st.nextToken()));
					}
					else {
						values.remove(values.size()-1);
					}
				}
			}
			
			IloIntVar[] binVars = _cplex.boolVarArray(weights.size());
			IloLinearIntExpr maximization = _cplex.linearIntExpr();
			for(int i = 0; i < values.size(); i++) {
				maximization.addTerm(values.get(i), binVars[i]);
			}

			_cplex.addMaximize(maximization);


			/*
			 * Add Constraints
			 */

			IloLinearIntExpr capacityConstraint = _cplex.linearIntExpr();
			for(int i = 0; i < weights.size(); i++) {
				capacityConstraint.addTerm(weights.get(i), binVars[i]);
			}

			_cplex.addLe(capacityConstraint, capacity);

			_cplex.solve();
			double stop = System.nanoTime();
			double elapsed = stop - start;
			System.out.println("Time excluding CPLEX init " + (elapsed / 1000000000.0) + " seconds");

			profit = (int) _cplex.getObjValue();
		} catch (IloException e) {
			System.err.println("CPLEX PROBLEM (prob need to run: export ILOG_LICENSE_FILE=/maytag/comm0/cplex/ilm/linux/access.ilm): " + e.getMessage());
		}
		return profit;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(solveWithCPLEX(args[0]));
	}
}