package newmodels.bidtocpc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXP;
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
	private int IDVar = 5;  //THIS NEEDS TO BE 4 OR MORE!!
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;


	public RegressionBidToCPC (Set<Query> queryspace) throws RserveException{
		c = new RConnection();
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
		double prediction = 0.00;
		/*
		 * oldest - > newest
		 */
		List<Double> bids = new ArrayList<Double>();
		for(int i = IDVar-2; i >= 0; i --) {
			bids.add(_bidBundles.get(_bidBundles.size()-1-i).getBid(query));
		}
		bids.add(currentBid);

		List<Double> CPCs = new ArrayList<Double>();
		for(int i = IDVar-3; i >= 0; i --) {
			double cpc = _queryReports.get(_queryReports.size()-1-i).getCPC(query);
			if(Double.isNaN(cpc)) {
				CPCs.add(0.0);
			}
			else {
				CPCs.add(cpc);
			}
		}

		int predCounter = 0;
		prediction += coeff[0];
		predCounter++;
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
			if(!(Double.isNaN(CPC) || CPC == 0)) {
				_bids.add(bid);
				_CPCs.add(CPC);
			}
			else {
				_bids.add(0.0);
				_CPCs.add(0.0);
			}
		}

		if(_bids.size() >= IDVar*numQueries) {
			List<List<Double>> bigBidList = new ArrayList<List<Double>>();
			List<List<Double>> bigCPCList = new ArrayList<List<Double>>();

			for(int i = 0 ; i < IDVar; i++) {
				List<Double> bids = _bids.subList(i*numQueries, _bids.size() - (IDVar-1-i)*numQueries);
				List<Double> CPC = _CPCs.subList(i*numQueries, _bids.size() - (IDVar-1-i)*numQueries);
				bigBidList.add(bids);
				bigCPCList.add(CPC);
			}

			List<double[]> bidArrList = new LinkedList<double[]>();
			List<double[]> CPCArrList = new LinkedList<double[]>();

			for(int i = 0; i < bigBidList.size(); i++) {
				List<Double> bidList = bigBidList.get(i);
				List<Double> CPCList = bigCPCList.get(i);
				double[] bidArr = new double[bidList.size()];
				double[] CPCArr = new double[CPCList.size()];
				for(int j = 0; j < bidList.size(); j++) {
					bidArr[j] = bidList.get(j);
					CPCArr[j] = CPCList.get(j);
				}
				bidArrList.add(bidArr);
				CPCArrList.add(CPCArr);
			}

			double[] mostRecentBidArr = bidArrList.get(bidArrList.size()-1);
			double[] secondRecentBidArr = bidArrList.get(bidArrList.size()-2);

			double[] mostRecentCubeBidArr = new double[mostRecentBidArr.length];
			double[] mostRecentSqBidArr = new double[mostRecentBidArr.length];
			double[] secondRecentSqBidArr = new double[secondRecentBidArr.length];


			double[] mostRecentCPCArr = CPCArrList.get(CPCArrList.size()-3);
			double[] secondRecentCPCArr = CPCArrList.get(CPCArrList.size()-4);

			double[] mostRecentCubeCPCArr = new double[mostRecentCPCArr.length];
			double[] mostRecentSqCPCArr = new double[mostRecentCPCArr.length];
			double[] secondRecentSqCPCArr = new double[secondRecentCPCArr.length];


			for(int i = 0; i < mostRecentBidArr.length; i++) {
				mostRecentSqBidArr[i] = mostRecentBidArr[i] * mostRecentBidArr[i];
				mostRecentCubeBidArr[i] = mostRecentBidArr[i] * mostRecentSqBidArr[i];
				secondRecentSqBidArr[i] = secondRecentBidArr[i] * secondRecentBidArr[i];

				mostRecentSqCPCArr[i] = mostRecentCPCArr[i] * mostRecentCPCArr[i];
				mostRecentCubeCPCArr[i] = mostRecentCPCArr[i] * mostRecentSqCPCArr[i];
				secondRecentSqCPCArr[i] = secondRecentCPCArr[i] * secondRecentCPCArr[i];
			}

			//c.assign("i", _predCounter);
			try {
				for(int i = 0; i < IDVar; i++) {
					c.assign("bid" + i, bidArrList.get(i));
					c.assign("cpc" + i, CPCArrList.get(i));
				}
				c.assign("bid" + (IDVar-1) + "cube", mostRecentCubeBidArr);
				c.assign("bid" + (IDVar-1) + "sq",mostRecentSqBidArr);
				c.assign("bid" + (IDVar-2) + "sq",secondRecentSqBidArr);

				c.assign("cpc" + (IDVar-3) + "cube", mostRecentCubeCPCArr);
				c.assign("cpc" + (IDVar-3) + "sq",mostRecentSqCPCArr);
				c.assign("cpc" + (IDVar-4) + "sq",secondRecentSqCPCArr);

				String model = "model = lm(cpc" + (IDVar-1) + " ~ ";
				for(int i = 0; i < IDVar; i++) {
					model += "bid" + i + " + ";
					if(i == IDVar -1) {
						model += "bid" + (IDVar - 1) + "sq + ";
						model += "bid" + (IDVar - 1) + "cube + ";
					}
					else if(i == IDVar - 2) {
						model += "bid" + (IDVar - 2) + "sq + ";
					}
				}

				for(int i = 0; i < IDVar-1; i++) {
					if(i == IDVar - 2) {
						model = model.substring(0, model.length()-3);
						model += ")";
					}
					else {
						model += "cpc" + i + " + ";
					}

					if(i == IDVar - 3) {
						model += "cpc" + (IDVar - 3) + "sq + ";
						model += "cpc" + (IDVar - 3) + "cube + ";
					}
					else if(i == IDVar - 4) {
						model += "cpc" + (IDVar - 4) + "sq + ";
					}
				}

				System.out.println(model);				
				c.voidEval(model);
				coeff = c.eval("coefficients(model)").asDoubles();
				for(int i = 0 ; i < coeff.length; i++)
					System.out.println(coeff[i]);
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
			System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

			return true;
		}
		else {
			return false;
		}
	}
}