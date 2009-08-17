package newmodels.bidtocpc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import newmodels.AbstractModel;

import org.rosuda.REngine.Rserve.RConnection;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BaselineBidToCPC extends AbstractBidToCPC {
	
	// db connection
	private static final String _dbURL = "jdbc:mysql://aalog:3306/qualification";
	private static final String _dbUser = "logger";
	private static final String _dbPass = "l0gger";
	private static Connection _db;
	
	// rserve connection
	private RConnection rcon;
	
	private double[] coeff;
	

	public BaselineBidToCPC() throws Exception{
		
	    // data points
	    List<Double> cpc1 = new LinkedList<Double>();
	    List<Double> cpc3 = new LinkedList<Double>();
	    List<Double> bid1 = new LinkedList<Double>();
	    List<Double> bid2 = new LinkedList<Double>();
	    List<Double> bid3 = new LinkedList<Double>();
		
	    // cpc3 = cpc of day k+3, cpc1 = cpc of day k+1, ... etc
		getDataPoint(cpc1, cpc3, bid1, bid2, bid3);
		
		// the regression is of the form 
		// cpc3= cpc1 + bid3 + bid2 + bid1 + (bid3)^2 + (bid3)^3 + (bid2)^2
		// the coefficient is thus ordered in this way
		coeff = new double[8];
		getCoefficient(cpc1, cpc3, bid1, bid2, bid3);
		
		for (int i = 0; i < 8; i++) {
			System.out.print(coeff[i]+" ");
		}
		System.out.println();
		
	}

	private void getCoefficient(List<Double> cpc1,
			List<Double> cpc3, List<Double> bid1, List<Double> bid2,
			List<Double> bid3) throws Exception {
		
		double[] cpc1arr = createArrayFromList(cpc1,1);
		double[] cpc3arr = createArrayFromList(cpc3,1);
		double[] bid1arr = createArrayFromList(bid1,1);
		double[] bid2arr = createArrayFromList(bid2,1);
		double[] bid3arr = createArrayFromList(bid3,1);
		double[] bid2sqarr = createArrayFromList(bid2,2);
		double[] bid3sqarr = createArrayFromList(bid3,2);
		double[] bid3cbarr = createArrayFromList(bid3,3);
		
		rcon = new RConnection();
		rcon.assign("bid3", bid3arr);
		rcon.assign("bid3sq", bid3sqarr);
		rcon.assign("bid3cube", bid3cbarr);
		rcon.assign("bid2", bid2arr);
		rcon.assign("bid2sq", bid2sqarr);
		rcon.assign("bid1", bid1arr);
		rcon.assign("cpc1", cpc1arr);
		rcon.assign("cpc3", cpc3arr);
		
		System.out.printf("The number of data points: %d.\n", bid3arr.length);
				
		rcon.voidEval("model <- lm(cpc3 ~ cpc1 + bid3 + bid2 + bid1 + bid3sq + bid2sq + bid3cube)");
		coeff = rcon.eval("coefficients(model)").asDoubles();
  
	     

/*		rcon.voidEval("ctl <- c(4.17,5.58,5.18,6.11,4.50,4.61,5.17,4.53,5.33,5.14)");
	    rcon.voidEval("trt <- c(4.81,4.17,4.41,3.59,5.87,3.83,6.03,4.89,4.32,4.69)");
	    rcon.voidEval("group <- gl(2,10,20)");
	    rcon.voidEval("weight <- c(ctl, trt)");
	    rcon.voidEval("model <- lm(weight ~ group)");
	    String s = rcon.eval("model$coefficients").asString();
	    System.out.println(s);*/
	    
		System.out.println("Regression done.");
	}

	private double[] createArrayFromList(List<Double> list, double power) {
		double[] array = new double[list.size()];
		int index = 0;
		for (double element : list) {
			array[index] = Math.pow(element,power);
			index++;
		}
		return array;
	}

	private void getDataPoint(List<Double> cpc1, List<Double> cpc3,
			List<Double> bid1, List<Double> bid2, List<Double> bid3)
			throws SQLException {
		// setup connection
        try {
        	_db = DriverManager.getConnection(_dbURL, _dbUser, _dbPass);
        } catch (SQLException e) {
        	System.out.println("Error connecting to database: " + e.getMessage());
        	System.out.println("Terminating...");
        	System.exit(-1);    	
        }
                
        // execute sql query
        PreparedStatement stmt = _db.prepareStatement("SELECT QueryID, Bid, CPC FROM dailyqueries  WHERE GameID > 150 AND Day < 15 ORDER BY GameID, AgentID, QueryID, Day");
	    ResultSet rset = stmt.executeQuery();  
	    System.out.println("Data retrieval done!");
	   
	    // stores data for generating points
	    List<Double> bids = new LinkedList<Double>();
	    List<Double> cpcs = new LinkedList<Double>();
	    
	    int lastQuery = -1;
	    while(rset.next()) {
	    	int thisQuery = rset.getInt("QueryID");
	    	if (lastQuery != thisQuery) {
	    		if (lastQuery != -1) {
	    			generateDataPoints(bid1, bid2, bid3, cpc1, cpc3, bids, cpcs);
	    		    bids = new LinkedList<Double>();
	    		    cpcs = new LinkedList<Double>();
	    		}
	    		lastQuery = thisQuery;
	    	}
	    	
	    	double bid = rset.getDouble("Bid");
	    	if (!(bid > 0)) bid = 0;
	    	double cpc = rset.getDouble("CPC");
	    	if (!(cpc > 0)) cpc = 0;
	    	
	    	if (cpc < bid) {
	    		bids.add(bid);
	    		cpcs.add(cpc);
	    	}
	    }
	    
	    if (!bids.isEmpty()) {
	    	generateDataPoints(bid1, bid2, bid3, cpc1, cpc3, bids, cpcs);
	    }
	    System.out.println("All data points generated.");
	}
	
	protected void generateDataPoints(List<Double> bid1, List<Double> bid2, List<Double> bid3, List<Double> cpc1, List<Double> cpc3, List<Double> bids, List<Double> cpcs) {
		Iterator bidit = bids.iterator();
		Iterator cpcit = cpcs.iterator();
		int counter = 0;
		int size = Math.min(bids.size(), cpcs.size());
		
		while (bidit.hasNext() && cpcit.hasNext()) {
			double bid = (Double)bidit.next();
			double cpc = (Double)cpcit.next();
			if (size - counter > 2) {
				bid1.add(bid);
				cpc1.add(cpc);
			}
			if (size - counter > 1 && counter > 0) {
				bid2.add(bid);
			}
			if (counter > 1) {
				bid3.add(bid);
				cpc3.add(cpc);
			}
			counter ++;
		}
		
		
		
	}
	
	@Override
	public double getPrediction(Query query, double bid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryreport, SalesReport salesReport, BidBundle bidbundle) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		BaselineBidToCPC model = new BaselineBidToCPC();

	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		try {
			return new BaselineBidToCPC();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}

/*
Some Results:

GameId > 210, 15 < Day < 55
-0.10536894890896588 0.207941616612222 1.0278237738843412 -0.07332309868233446 -0.06699522809461302 -0.23089069635624657 0.015149242535354907 0.016234063325583945 
GameId > 220
-0.09436575423506026 0.11673870215587208 0.9300918041228036 -0.061979813437687936 -0.00250571934624439 -0.19370810812020575 0.010303828553760412 0.012280524901173033
GameId > 200, Day < 10 
-0.13020396962391037 0.04060021059914824 1.0287396561939361 -0.0335857590894806 -6.849514646524597E-4 -0.2016993388323136 0.005987619567488385 0.011443897653105366 
GameId > 150, Day < 10
-0.12597986581615647 0.02349913375652978 1.031950397569653 -0.02611973168589479 -1.2069604428887054E-4 -0.2087012477792655 0.004666727729092785 0.01297912222938058
GameId > 150, Day < 15
-0.1195058513231961 0.039242143559012906 1.045235512020745 -0.03775648676403431 -5.188620375490851E-4 -0.22066649900216934 0.005906860037513644 0.0141938541634402 

 */