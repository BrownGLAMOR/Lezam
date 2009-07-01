package newmodels.bidtoprclick;

import java.util.ArrayList;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.Ad;
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

public class RegressionBidToPrClick extends AbstractBidToPrClick {

	protected ArrayList<Double> _bids , _prclicks;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int numQueries = 16;
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;


	public RegressionBidToPrClick (Set<Query> queryspace) {
		try {
			c = new RConnection();
		} catch (RserveException e) {
			e.printStackTrace();
		}
		_bids = new ArrayList<Double>();
		_prclicks = new ArrayList<Double>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		_querySpace = queryspace;
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid, Ad currentAd, BidBundle bidbundle){
		double prediction = 0.0;
		/*
		 * oldest - > newest
		 */

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

		prediction += coeff[0];
		prediction += coeff[1] * queryInd1;
		prediction += coeff[2] * queryInd2;
		prediction += coeff[3] * queryInd3;
		prediction += coeff[4] * queryInd4;
		prediction += coeff[5] * queryInd5;
		prediction += coeff[6] * queryInd6;
		prediction += coeff[7] * currentBid;

		double clickpr = 1/(1+Math.exp(-prediction));

		return clickpr;
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
			double imps = queryreport.getImpressions(query);
			double clicks = queryreport.getClicks(query);
			if(!(clicks == 0 || imps == 0)) {
				_bids.add(bid);
				_prclicks.add(clicks/imps);
			}
			else {
				_bids.add(bid);
				_prclicks.add(0.0);
			}
		}

		double[] bids = new double[_bids.size()];
		double[] prclicks = new double[_bids.size()];

		for(int i = 0; i < _bids.size(); i++) {
			bids[i] = _bids.get(i);
			prclicks[i] = _prclicks.get(i);
		}

		int[] queryInd1 = new int[_bids.size()];
		int[] queryInd2 = new int[_bids.size()];
		int[] queryInd3 = new int[_bids.size()];
		int[] queryInd4 = new int[_bids.size()];
		int[] queryInd5 = new int[_bids.size()];
		int[] queryInd6 = new int[_bids.size()];
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
			c.assign("prclicks", prclicks);

			String model = "model = glm(prclicks ~ queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + bids , family = quasibinomial(link = \"logit\"))";

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
}