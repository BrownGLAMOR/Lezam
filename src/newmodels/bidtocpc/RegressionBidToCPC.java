package newmodels.bidtocpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/**
 * @author afoo & jberg
 *
 */

/*
 * TODO
 * Keep track of how many points come in every day
 * 
 * Allow for bids of 0
 */

public class RegressionBidToCPC extends AbstractBidToCPC {

	protected ArrayList<Double> _bids , _CPCs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int numQueries = 16;
	private int IDVar = 5;  //THIS NEEDS TO BE MORE THAN 4, LESS THAN 10
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;


	public RegressionBidToCPC (Set<Query> queryspace) {
		try {
			c = new RConnection();
		} catch (RserveException e) {
			e.printStackTrace();
		}
		_bids = new ArrayList<Double>();
		_CPCs = new ArrayList<Double>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		_querySpace = queryspace;
		if(IDVar < 4) {
			throw new RuntimeException("Don't set IDVar below 4");
		}
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid, BidBundle bidbundle){
		double prediction = 0.0;
		/*
		 * oldest - > newest
		 */
		List<Double> bids = new ArrayList<Double>();
		for(int i = 0; i < IDVar - 2; i++) {
			bids.add(_bidBundles.get(_bidBundles.size() - 1 - (IDVar - 3 -i)).getBid(query));
		}
		bids.add(bidbundle.getBid(query));
		bids.add(currentBid);

		List<Double> CPCs = new ArrayList<Double>();
		for(int i = IDVar-3; i >= 0; i --) {
			double cpc = _queryReports.get(_queryReports.size()-1-i).getCPC(query);
			CPCs.add(cpc);
		}

		int queryInd1 = 0;
		int queryInd2 = 0;
		int queryInd3 = 0;
		int queryInd4 = 0;
		int queryInd5 = 0;
		int queryInd6 = 0;

		String man = query.getManufacturer();
		String comp = query.getComponent();
		if("pg".equals(man)) {
			queryInd1 = 1;
		}
		else if("lioneer".equals(man)) {
			queryInd2= 1;
		}
		else if("flat".equals(man)) {
			queryInd3 = 1;
		}

		if("tv".equals(comp)) {
			queryInd4 = 1;
		}
		else if("dvd".equals(comp)) {
			queryInd5 = 1;
		}
		else if("audio".equals(comp)) {
			queryInd6 = 1;
		}

		int predCounter = 0;
		prediction += coeff[0];
		predCounter++;

		prediction += coeff[1] * queryInd1;
		prediction += coeff[2] * queryInd2;
		prediction += coeff[3] * queryInd3;
		prediction += coeff[4] * queryInd4;
		prediction += coeff[5] * queryInd5;
		prediction += coeff[6] * queryInd6;
		predCounter += 6;
		for(int i = 0; i < bids.size(); i++) {
			prediction += coeff[i+predCounter] * bids.get(i);
			if(i == bids.size() - 2) {
				predCounter++;
				prediction += coeff[i+predCounter] * bids.get(i) * bids.get(i);
			}
			else if(i == bids.size() - 1) {
				predCounter++;
				prediction += coeff[i+predCounter] * bids.get(i) * bids.get(i);
				predCounter++;
				prediction += coeff[i+predCounter] * bids.get(i) * bids.get(i) * bids.get(i);
			}
		}
		predCounter += bids.size();
		for(int i = 0; i < CPCs.size(); i++) {
			prediction += coeff[i+predCounter] * CPCs.get(i);
			if(i == CPCs.size() - 2) {
				predCounter++;
				prediction += coeff[i+predCounter] * CPCs.get(i) * CPCs.get(i);
			}
			else if(i == CPCs.size() - 1) {
				predCounter++;
				prediction += coeff[i+predCounter] * CPCs.get(i) * CPCs.get(i);
				predCounter++;
				prediction += coeff[i+predCounter] * CPCs.get(i) * CPCs.get(i) * CPCs.get(i);
			}
		}
		predCounter += CPCs.size();

		/*
		 * Our CPC can never be higher than our bid
		 */
		if(prediction < currentBid) {
			return prediction;

		}
		else {
			return currentBid;
		}
	}

	/*
	 * MAKE SURE THAT THE BIDBUNDLE CORRESPONDS TO THE QUERY REPORT
	 */
	public boolean updateModel(QueryReport queryreport, BidBundle bidbundle) {

		double start = System.currentTimeMillis();

		_queryReports.add(queryreport);
		_bidBundles.add(bidbundle);

		for(Query query : _querySpace) {
			double bid = bidbundle.getBid(query);
			double CPC = queryreport.getCPC(query);
			if(!(Double.isNaN(CPC) || bid == 0)) {
				_bids.add(bid);
				_CPCs.add(CPC);
			}
			else {
				_bids.add(0.0);
				_CPCs.add(0.0);
			}
		}

		if(_bids.size() > IDVar*numQueries) {

			double[] bids = new double[_bids.size()];
			double[] cpcs = new double[_bids.size()];

			for(int i = 0; i < _bids.size(); i++) {
				bids[i] = _bids.get(i);
				cpcs[i] = _CPCs.get(i);
			}

			int arrLen = _bids.size() - (IDVar-1)*numQueries ;

			int[] queryInd1 = new int[arrLen];
			int[] queryInd2 = new int[arrLen];
			int[] queryInd3 = new int[arrLen];
			int[] queryInd4 = new int[arrLen];
			int[] queryInd5 = new int[arrLen];
			int[] queryInd6 = new int[arrLen];
			int numIters = queryInd1.length/16;
			for(int i = 0; i < numIters; i++) {
				int j = 0;
				for(Query query : _querySpace) {
					queryInd1[i*16 + j] = 0;
					queryInd2[i*16 + j] = 0;
					queryInd3[i*16 + j] = 0;
					queryInd4[i*16 + j] = 0;
					queryInd5[i*16 + j] = 0;
					queryInd6[i*16 + j] = 0;
					String man = query.getManufacturer();
					String comp = query.getComponent();
					if("pg".equals(man)) {
						queryInd1[i*16 + j] = 1;
					}
					else if("lioneer".equals(man)) {
						queryInd2[i*16 + j] = 1;
					}
					else if("flat".equals(man)) {
						queryInd3[i*16 + j] = 1;
					}

					if("tv".equals(comp)) {
						queryInd4[i*16 + j] = 1;
					}
					else if("dvd".equals(comp)) {
						queryInd5[i*16 + j] = 1;
					}
					else if("audio".equals(comp)) {
						queryInd6[i*16 + j] = 1;
					}
					j++;
				}
			}

			try {
				c.assign("queryInd1",queryInd1);
				c.assign("queryInd2",queryInd2);
				c.assign("queryInd3",queryInd3);
				c.assign("queryInd4",queryInd4);
				c.assign("queryInd5",queryInd5);
				c.assign("queryInd6",queryInd6);
				c.assign("bids", bids);
				c.assign("cpcs", cpcs);

				String model = "model = lm(cpcs[" + ((IDVar - 1)*numQueries+1) + ":" + _bids.size() +  "] ~ queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + ";
				for(int i = 0; i < IDVar; i++) {
					int min = i * numQueries + 1;
					int max = _bids.size() - (IDVar - 1 - i) * numQueries;
					model += "bids[" + min +":" + max + "] + ";
					if(i >= IDVar - 2) {
						model += "I(bids[" + min +":" + max + "]^2) + ";
					}
					if(i == IDVar -1) {
						model += "I(bids[" + min +":" + max + "]^3) + ";
					}
				}

				for(int i = 0; i < IDVar-2; i++) {
					int min = i * numQueries + 1;
					int max = _bids.size() - (IDVar - 1 - i) * numQueries;

					model += "cpcs[" + min +":" + max + "] + ";
					if(i >= IDVar - 4) {
						model += "I(cpcs[" + min +":" + max + "]^2) + ";
					}
					if(i == IDVar -3) {
						model += "I(cpcs[" + min +":" + max + "]^3) + ";
					}
				}

				model = model.substring(0, model.length()-3);
				model += ")";

//				System.out.println(model);				
				c.voidEval(model);
				coeff = c.eval("coefficients(model)").asDoubles();
//				for(int i = 0 ; i < coeff.length; i++)
//					System.out.println(coeff[i]);
			}
			catch (REngineException e) {
				e.printStackTrace();
				return false;
			}
			catch (REXPMismatchException e) {
				e.printStackTrace();
				return false;
			}

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
//			System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

			return true;
		}
		else {
			return false;
		}
	}
}