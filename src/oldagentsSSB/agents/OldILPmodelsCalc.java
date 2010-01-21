package oldagentsSSB.agents;

import java.util.Hashtable;
import java.util.Queue;
import java.util.Vector;

import newmodels.oldusermodel.UserState;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.RetailCatalog;

public class OldILPmodelsCalc {
	private static final int NUMOFUSERFOCUS = 4;

	private static final int NUMOFUSERS = 9000;
	
	private Hashtable <Pair<Product,UserState> , Double> p_x_Pr;
	private Hashtable <Pair<Pair<Product,UserState>,Pair<Query,Integer>>,Double> click_Pr_q_p_x_s;
	private Hashtable <Double,Integer> slotOfBid;
	private Hashtable <Pair<Product,UserState> , Double> p_x_convPr;
	private Hashtable <Pair<Pair<Product,UserState>,Integer>,Double> p_x_i_convPr;
	private Hashtable <Double,Double> cpcOfBid;
	private Hashtable <Product,Integer> revenueP;
	RetailCatalog _retailCatalog;
	
	public OldILPmodelsCalc (Vector<Double> bids , Vector<Integer> quantities, RetailCatalog retailCatalog , Queue<QueryReport> queryReport, int numOfSlots) {
		
		_retailCatalog = retailCatalog;
		
//		p_x_Pr = new double[retailCatalog.size()][NUMOFUSERFOCUS]; // TODO find a model
//		click_Pr_q_p_x_s = new double[queryReport.size()][retailCatalog.size()][NUMOFUSERFOCUS][numOfSlots];
//		slotOfBid = new int[numOfSlots];
//		p_x_convPr = new double[retailCatalog.size()][NUMOFUSERFOCUS];
//		p_x_i_convPr = new double[retailCatalog.size()][NUMOFUSERFOCUS][quantities.size()];
//		cpcOfBid = new double[bids.size()];

		p_x_Pr = new Hashtable <Pair<Product,UserState> , Double>();
		click_Pr_q_p_x_s = new Hashtable <Pair<Pair<Product,UserState>,Pair<Query,Integer>>,Double>();
		slotOfBid = new Hashtable <Double,Integer>();
		p_x_convPr = new Hashtable <Pair<Product,UserState> , Double>();
		p_x_i_convPr = new Hashtable <Pair<Pair<Product,UserState>,Integer>,Double>();
		cpcOfBid = new Hashtable <Double,Double>();
		revenueP = new Hashtable <Product,Integer>();
		_retailCatalog = retailCatalog;
		
		// TODO find a model
		for (Product p : _retailCatalog) {
			for (UserState us : UserState.values()) {
				Pair<Product,UserState> p_x = new Pair<Product, UserState>(p , us);
				p_x_Pr.put(p_x, 0.5);
				p_x_convPr.put(p_x, 0.5);
				for (int i=0 ; i<5 ; i++) {
					Pair<Pair<Product,UserState>,Integer> p_x_i = new Pair<Pair<Product,UserState>, Integer>(p_x,i);
					p_x_i_convPr.put(p_x_i, 0.5);
				}
			}
		}
		
	}
	
	public double getBidCoefficient(double bid , Query query) {
		double result = 0;
		Pair<Query,Integer> q_s = new Pair<Query,Integer>(query,slotOfBid.get(bid));
		double cpcBid = cpcOfBid.get(bid);
		
		for (Product p : _retailCatalog) {
			for (int userFocus=0 ; userFocus<NUMOFUSERFOCUS ; userFocus++) {
				Pair<Product , Integer> p_x = new Pair<Product , Integer>(p,userFocus);
				Pair<Pair<Product,Integer>,Pair<Query,Integer>> q_p_x_s = new Pair<Pair<Product,Integer>,Pair<Query,Integer>>(p_x,q_s);
				result += NUMOFUSERS * p_x_Pr.get(p_x) * click_Pr_q_p_x_s.get(q_p_x_s) * (p_x_convPr.get(p_x)*revenueP.get(p) - cpcBid);
			}
		}
		return bid;
	}

	public double getQuantityBoundCoef(double bid, Query query) {
		// TODO Auto-generated method stub
		return bid;
	}

	public double getQuantityCoefficient(int quantity) {
		// TODO Auto-generated method stub
		return quantity;
	}

	/**
	 * ObjectPair stores two associated objects, that can be retrieved separately.
	 * It has similar functionality to Pair<Object, Object> class in c++ and
	 * javac Pair<Object, Object> class.
	 * Code was inspired from an example on the web.
	 *
	 * @param <T>
	 * @param <S>
	 */
	public class Pair<T, S>
	{
	  private T first;
	  private S second;
	  
	  public Pair(T f, S s)
	  { 
	    first = f;
	    second = s;   
	  }

	  public T getFirst()
	  {
	    return first;
	  }

	  public S getSecond() 
	  {
	    return second;
	  }

	  public String toString()
	  { 
	    return "(" + first.toString() + ", " + second.toString() + ")"; 
	  }
	}
	
}


