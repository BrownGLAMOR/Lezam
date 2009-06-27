package newmodels.bidtoprclick;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class RegressionBidToPrClick{
	protected LinkedList<Double> _bids, _prClicks;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int numQueries = 16;
	private LinkedList<QueryReport> _queryReports;
	private LinkedList<BidBundle> _bidBundles;


	public RegressionBidToPrClick (Set<Query> queryspace) throws RserveException{
		c = new RConnection();
		_bids = new LinkedList<Double>();
		_prClicks = new LinkedList<Double>();
		_queryReports = new LinkedList<QueryReport>();
		_bidBundles = new LinkedList<BidBundle>();
		_querySpace = queryspace;
	}

	
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid, BidBundle bidbundle){
		double prediction = 0.00;
		double oneDayOldBid = bidbundle.getBid(query);
		double twoDayOldBid = _bidBundles.getLast().getBid(query);
		double twoDayOldPrClicks = _queryReports.getLast().getClicks(query)/(_queryReports.getLast().getImpressions(query));
		double currentBidSq = currentBid * currentBid;
		double currentBidCube = currentBidSq * currentBid;
		double oneDayOldBidSq = oneDayOldBid * oneDayOldBid;
		
		prediction = coeff[0] + coeff[1]*twoDayOldPrClicks + coeff[2]*currentBid + coeff[3]*oneDayOldBid + coeff[4]*twoDayOldBid + coeff[5]*currentBidSq + coeff[6]*oneDayOldBidSq + coeff[7]*currentBidCube ; 
		
		/**
		 * c.voidEval("for (j in 1:16){" +
		 *
		 *		"pred[j] = coefficients[1] + coefficients[2]*cpc3[i*16 + j] + " +
		 * 		"coefficients[3]*bid5[j] + coefficients[4]*bid3[(i+1)*16+j] " +
		 *		"+ coefficients[5]*bid2[(i+1)*16+j] + coefficients[6]*((bid5)^2)[j] " +
		 *		"+ coefficients[7]*((bid3)^2)[(i+1)*16+j] + coefficients[8]*((bid5)^3)[j]" +
		 * 	"}");
		 * 
		 */
		
		return prediction;
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
			double prClicks = queryreport.getClicks(query)/queryreport.getImpressions(query);
			_bids.add(bid);
			_prClicks.add(prClicks);
			
		}

		if(_bids.size() >= 3*numQueries) {

			List<Double> bidsM1 = _bids.subList(0, _bids.size()-2*numQueries);
			List<Double> bidsM2 = _bids.subList(1*numQueries, _bids.size()-1*numQueries);
			List<Double> bidsM3 = _bids.subList(2*numQueries, _bids.size());


			List<Double> PrClicksM1 = _prClicks.subList(0, _prClicks.size()-2*numQueries);
			List<Double> PrClicksM2 = _prClicks.subList(1*numQueries, _prClicks.size()-1*numQueries);
			List<Double> PrClicksM3 = _prClicks.subList(2*numQueries, _prClicks.size());

			Double[] bidsM1Arr = bidsM1.toArray(new Double[0]);
			Double[] bidsM2Arr = bidsM2.toArray(new Double[0]);
			Double[] bidsM3Arr = bidsM3.toArray(new Double[0]);

			Double[] PrClicksM1Arr = PrClicksM1.toArray(new Double[0]);
			Double[] PrClicksM2Arr = PrClicksM2.toArray(new Double[0]);
			Double[] PrClicksM3Arr = PrClicksM3.toArray(new Double[0]);

			double[] bidsM2Sqarr = new double[bidsM2Arr.length];
			double[] bidsM3Sqarr = new double[bidsM3Arr.length];
			double[] bidsM3Cubearr = new double[bidsM3Arr.length];

			/*
			 * TODO
			 * There must be a faster way to convert Double[] to double[]
			 */

			double[] bidsM1arr = new double[bidsM1Arr.length];
			double[] bidsM2arr = new double[bidsM2Arr.length];
			double[] bidsM3arr = new double[bidsM3Arr.length];
			double[] PrClicksM1arr = new double[PrClicksM1Arr.length];
			double[] PrClicksM2arr = new double[PrClicksM2Arr.length];
			double[] PrClicksM3arr = new double[PrClicksM3Arr.length];

			for(int i = 0; i < bidsM1Arr.length; i++) {
				bidsM1arr[i] = bidsM1Arr[i].doubleValue();
				bidsM2arr[i] = bidsM2Arr[i].doubleValue();
				bidsM3arr[i] = bidsM3Arr[i].doubleValue();

				PrClicksM1arr[i] = PrClicksM1Arr[i].doubleValue();
				PrClicksM2arr[i] = PrClicksM2Arr[i].doubleValue();
				PrClicksM3arr[i] = PrClicksM3Arr[i].doubleValue();
			}

			for(int i = 0; i < bidsM2Sqarr.length; i++) {
				bidsM2Sqarr[i] = bidsM2arr[i] * bidsM2arr[i];
				bidsM3Sqarr[i] = bidsM3arr[i] * bidsM3arr[i];
				bidsM3Cubearr[i] = bidsM3arr[i] * bidsM3Sqarr[i];
			}

			//c.assign("i", _predCounter);
			try {
				c.assign("bid3", bidsM3arr);
				c.assign("bid3sq", bidsM3Sqarr);
				c.assign("bid3cube", bidsM3Cubearr);
				c.assign("bid2", bidsM2arr);
				c.assign("bid2sq", bidsM2Sqarr);
				c.assign("bid1", bidsM1arr);
				c.assign("prClick1", PrClicksM1arr);
				c.assign("prClick2", PrClicksM2arr);
				c.assign("prClick3", PrClicksM3arr);
				c.voidEval("pred = 1:16");

				c.voidEval("model = lm(prClick3 ~ prClick1 + bid3 + bid2 + bid1 + bid3sq + bid2sq + bid3cube)");
				c.voidEval("coefficients = model$coefficients");
				coeff = c.eval("coefficients(model)").asDoubles();
			} catch (REngineException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
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
