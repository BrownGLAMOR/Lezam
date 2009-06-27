package agents.old;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import agents.GenericBidStrategy;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

public class OldILPBidStrategy extends GenericBidStrategy{

	public static final String DAILYCAPACITY = "MaximumDailyCapacity";
	public static final String NUMUSERS = "NumberOfUsers";
	public static final String DISCOUNTER = "DistributionCapacityDiscounter";
	public static int NUMOFQUERIES;
	public static Map <Product , Double> _productRevenue;
	protected Hashtable<Query, Double> _bid;
	private Hashtable<Integer, Query> _queryIndexing;
	
	//public static Hashtable <Double, ILPmodelsCalc> _modelsBundle ;
	
	public Vector<Double> _bidSet;
	public Vector<Integer> _QSet;
	
	protected int _day;
	
	public OldILPBidStrategy(Set<Query> querySpace) {
		super(querySpace);
		_day = 0;
		_day = 1 - 1;
		NUMOFQUERIES = querySpace.size();
		_queryIndexing = new Hashtable<Integer, Query>();
		_productRevenue = new Hashtable<Product, Double>();
		_bid = new Hashtable<Query, Double>();
		_bidSet = new Vector<Double>();
		_QSet = new Vector<Integer>();
		int i=0;
		for (Query q : querySpace) {
			_queryIndexing.put(i , q);
			i++;
		}
	}
	
	public void setProductsRevenue (Product product, double price) {
		_productRevenue.put(product,price);
	}
	
	public Vector<Double> setPossibleBids (double minBid, double maxBid, double interval) {
		Vector<Double> bids = new Vector<Double>();
		if ((minBid < 0) || (maxBid <= minBid)) return bids;
		double bid = minBid;
		while (bid < maxBid) {
			_bidSet.add(bid);
			bids.add(bid);
			bid += interval;
		}
		return bids;
	}

	public Vector<Integer> setPossibleQuantities(int minQ, int maxQ, int interval) {
		Vector<Integer> quantity = new Vector<Integer>();
		if ((minQ < 0) || (maxQ <= minQ)) return quantity;
		int q = minQ;
		while (q < maxQ) {
			_QSet.add(q);
			quantity.add(q);
			q += interval;
		}
		return quantity;
	}

	public void refreshData(OldILPmodelsCalc ilp) {
		//TODO calculate the new bids according to new results coming from the models
		
		Double[] bids = new Double[_bidSet.size()];
		//bids = (Double[])_bidSet.toArray().clone();
		_bidSet.copyInto(bids);
		Integer[] quantities = new Integer[_QSet.size()];
		//quantities = (Integer[])_QSet.toArray().clone();
		_QSet.copyInto(quantities);

		try {
			IloCplex cplex = new IloCplex();
			IloIntVar[] overQuantVar = cplex.boolVarArray(quantities.length);
//			IloIntVar[] overQuantVar = cplex.intVarArray(quantities.length, 0, 1);
			IloIntVar[][] bidsVar = new IloIntVar[NUMOFQUERIES][bids.length];
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				bidsVar[query] = cplex.boolVarArray(bids.length);
//				bidsVar[query] = cplex.intVarArray(bids.length, 0, 1);
			}
			IloLinearNumExpr expr = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int bid=0 ; bid<bids.length ; bid++) {
					expr.addTerm(bidsVar[query][bid], ilp.getBidCoefficient(bids[bid] , _queryIndexing.get(query)));
				}
			}
			for (int quantity=0 ; quantity <quantities.length ; quantity++) {
				expr.addTerm(overQuantVar[quantity], -ilp.getQuantityCoefficient(quantities[quantity]));				
			}
			
			cplex.addMaximize(expr);
			expr.clear();
			IloLinearNumExpr exprOverQ = cplex.linearNumExpr();
			IloLinearNumExpr exprBid = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int bid=0 ; bid<bids.length ; bid++) {
					expr.addTerm(bidsVar[query][bid] , ilp.getQuantityBoundCoef(bids[bid] , _queryIndexing.get(query)));
					exprBid.addTerm(1.0, bidsVar[query][bid]);
				}
				cplex.addLe(exprBid, 1);
			}
			for (int quantity=0 ; quantity<quantities.length ; quantity++) {
				expr.addTerm(overQuantVar[quantity] , -quantities[quantity]);				
				exprOverQ.addTerm(1.0, overQuantVar[quantity]);
			}
			cplex.addLe(exprOverQ, 1);
			cplex.addLe(expr, getProperty(null, DAILYCAPACITY));
			
			if (cplex.solve()) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				
				//double[] bidsPerQuery = new double[NUMOFQUERIES];
				for (int query=0 ; query<NUMOFQUERIES ; query++) {
					double[] queryResults = cplex.getValues(bidsVar[query]);
					for (int i=0 ; i<queryResults.length ; i++) {
						//if (queryResults[i] == 1.0) bidsPerQuery[query] = bids[i];
						if (queryResults[i] == 1.0) _bid.put(_queryIndexing.get(query) , bids[i]);
						System.out.print(queryResults[i] + " - ");
					}
					System.out.println();
				}
			}
			cplex.end();
		}
		catch (IloException e) {
			System.err.println ("Concert Exception" + e + "' caught");
		}
	}
	
	public double getQueryBid(Query q) {
		return _bid.get(q);
	}

}
