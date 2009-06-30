package newmodels.bidtoprclick;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */


public class RegressionBidToPrClick extends AbstractRegressionPrClick {

	protected ArrayList<Double> _bids , _clickPrs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int numQueries = 16;
	private int IDVar = 8;  //THIS NEEDS TO BE MORE THAN 4, LESS THAN 10
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private ArrayList<Ad> _ads;


	public RegressionBidToPrClick (Set<Query> queryspace) throws RserveException {
		c = new RConnection();
		_bids = new ArrayList<Double>();
		_clickPrs = new ArrayList<Double>();
		_ads = new ArrayList<Ad>();
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
	public double getPrediction(Query query, double currentBid, Ad currentAd, BidBundle bidbundle){
		double prediction = 0.00;
		/*
		 * oldest - > newest
		 */
		List<Double> bids = new ArrayList<Double>();
		for(int i = IDVar-2; i >= 0; i --) {
			bids.add(_bidBundles.get(_bidBundles.size()-1-i).getBid(query));
		}
		bids.add(currentBid);

		List<Double> clickPrs = new ArrayList<Double>();
		for(int i = IDVar-3; i >= 0; i --) {
			double imps = _queryReports.get(_queryReports.size()-1-i).getImpressions(query);
			double clicks = _queryReports.get(_queryReports.size()-1-i).getClicks(query);
			double clickPr;
			if(imps == 0 || clicks == 0) {
				clickPr = 0;
			}
			else {
				clickPr = clicks/imps;
			}
			clickPrs.add(clickPr);
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
		for(int i = 0; i < clickPrs.size(); i++) {
			prediction += coeff[i+predCounter] * clickPrs.get(i);
			if(i == clickPrs.size() - 2) {
				predCounter++;
				prediction += coeff[i+predCounter] * clickPrs.get(i) * clickPrs.get(i);
			}
			else if(i == clickPrs.size() - 1) {
				predCounter++;
				prediction += coeff[i+predCounter] * clickPrs.get(i) * clickPrs.get(i);
				predCounter++;
				prediction += coeff[i+predCounter] * clickPrs.get(i) * clickPrs.get(i) * clickPrs.get(i);
			}
		}
		predCounter += clickPrs.size();

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
			double clicks = queryreport.getClicks(query);
			double imps = queryreport.getImpressions(query);
			double clickPr;
			if(imps == 0 || clicks == 0) {
				clickPr = 0;
			}
			else {
				clickPr= clicks/imps;
			}
			double bid = bidbundle.getBid(query);
			Ad ad = bidbundle.getAd(query);
			_bids.add(bid);
			_clickPrs.add(clickPr);
			_ads.add(ad);
		}

		if(_bids.size() >= IDVar*numQueries) {
			List<List<Double>> bigBidList = new ArrayList<List<Double>>();
			List<List<Double>> bigClickPrList = new ArrayList<List<Double>>();
			List<List<Ad>> bigAdsList = new ArrayList<List<Ad>>();

			for(int i = 0 ; i < IDVar; i++) {
				List<Double> bids = _bids.subList(i*numQueries, _bids.size() - (IDVar-1-i)*numQueries);
				List<Double> ClickPr = _clickPrs.subList(i*numQueries, _bids.size() - (IDVar-1-i)*numQueries);
				List<Ad> ads = _ads.subList(i*numQueries, _bids.size() - (IDVar-1-i)*numQueries);
				bigBidList.add(bids);
				bigClickPrList.add(ClickPr);
				bigAdsList.add(ads);
			}

			List<double[]> bidArrList = new LinkedList<double[]>();
			List<double[]> clickPrArrList = new LinkedList<double[]>();
			List<Ad[]> adsArrList = new LinkedList<Ad[]>();

			for(int i = 0; i < bigBidList.size(); i++) {
				List<Double> bidList = bigBidList.get(i);
				List<Double> clickPrList = bigClickPrList.get(i);
				List<Ad> adList = bigAdsList.get(i);
				double[] bidArr = new double[bidList.size()];
				double[] clickPrArr = new double[clickPrList.size()];
				Ad[] adArr = new Ad[adList.size()];
				for(int j = 0; j < bidList.size(); j++) {
					bidArr[j] = bidList.get(j);
					clickPrArr[j] = clickPrList.get(j);
					adArr[j] = adList.get(j);
				}
				bidArrList.add(bidArr);
				clickPrArrList.add(clickPrArr);
				adsArrList.add(adArr);
			}

			double[] mostRecentBidArr = bidArrList.get(bidArrList.size()-1);
			double[] secondRecentBidArr = bidArrList.get(bidArrList.size()-2);

			double[] mostRecentCubeBidArr = new double[mostRecentBidArr.length];
			double[] mostRecentSqBidArr = new double[mostRecentBidArr.length];
			double[] secondRecentSqBidArr = new double[secondRecentBidArr.length];


			double[] mostRecentClickPrArr = clickPrArrList.get(clickPrArrList.size()-3);
			double[] secondRecentClickPrArr = clickPrArrList.get(clickPrArrList.size()-4);

			double[] mostRecentCubeClickPrArr = new double[mostRecentClickPrArr.length];
			double[] mostRecentSqClickPrArr = new double[mostRecentClickPrArr.length];
			double[] secondRecentSqClickPrArr = new double[secondRecentClickPrArr.length];


			for(int i = 0; i < mostRecentBidArr.length; i++) {
				mostRecentSqBidArr[i] = mostRecentBidArr[i] * mostRecentBidArr[i];
				mostRecentCubeBidArr[i] = mostRecentBidArr[i] * mostRecentSqBidArr[i];
				secondRecentSqBidArr[i] = secondRecentBidArr[i] * secondRecentBidArr[i];

				mostRecentSqClickPrArr[i] = mostRecentClickPrArr[i] * mostRecentClickPrArr[i];
				mostRecentCubeClickPrArr[i] = mostRecentClickPrArr[i] * mostRecentSqClickPrArr[i];
				secondRecentSqClickPrArr[i] = secondRecentClickPrArr[i] * secondRecentClickPrArr[i];
			}

			int[] queryInd1 = new int[mostRecentBidArr.length];
			int[] queryInd2 = new int[mostRecentBidArr.length];
			int[] queryInd3 = new int[mostRecentBidArr.length];
			int[] queryInd4 = new int[mostRecentBidArr.length];
			int[] queryInd5 = new int[mostRecentBidArr.length];
			int[] queryInd6 = new int[mostRecentBidArr.length];
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
				for(int i = 0; i < IDVar; i++) {
					c.assign("bid" + i, bidArrList.get(i));
					c.assign("clickPr" + i, clickPrArrList.get(i));
				}
				c.assign("bid" + (IDVar-1) + "cube", mostRecentCubeBidArr);
				c.assign("bid" + (IDVar-1) + "sq",mostRecentSqBidArr);
				c.assign("bid" + (IDVar-2) + "sq",secondRecentSqBidArr);

				c.assign("clickPr" + (IDVar-3) + "cube", mostRecentCubeClickPrArr);
				c.assign("clickPr" + (IDVar-3) + "sq",mostRecentSqClickPrArr);
				c.assign("clickPr" + (IDVar-4) + "sq",secondRecentSqClickPrArr);

				String model = "model = lm(clickPr" + (IDVar-1) + " ~ queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + ";
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
						model += "clickPr" + i + " + ";
					}

					if(i == IDVar - 3) {
						model += "clickPr" + (IDVar - 3) + "sq + ";
						model += "clickPr" + (IDVar - 3) + "cube + ";
					}
					else if(i == IDVar - 4) {
						model += "clickPr" + (IDVar - 4) + "sq + ";
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