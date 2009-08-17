package newmodels.bidtoprclick;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import newmodels.AbstractModel;

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

	protected ArrayList<Double> _bids , _clickPrs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int _numQueries = 16;
	private int _IDVar;  //THIS NEEDS TO BE MORE THAN 4, LESS THAN 10
	private int _numPrevDays;	//How many days worth of data to include in the regression
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private boolean _queryIndicators;
	private boolean _queryTypeIndicators;
	private boolean _powers;


	public RegressionBidToPrClick (Set<Query> queryspace, int IDVar, int numPrevDays, boolean queryIndicators, boolean queryTypeIndicators, boolean powers) {
		try {
			c = new RConnection();
		} catch (RserveException e) {
			e.printStackTrace();
		}
		_bids = new ArrayList<Double>();
		_clickPrs = new ArrayList<Double>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		_querySpace = queryspace;
		_IDVar = IDVar;
		_numPrevDays = numPrevDays;
		_queryIndicators = queryIndicators;
		_queryTypeIndicators = queryTypeIndicators;
		_powers = powers;
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid, Ad currentAd){
		double prediction = 0.0;
		/*
		 * oldest - > newest
		 */
		List<Double> bids = new ArrayList<Double>();
		for(int i = 0; i < _IDVar - 2; i++) {
			bids.add(_bidBundles.get(_bidBundles.size() - 1 - (_IDVar - 3 -i)).getBid(query));
		}
		bids.add(currentBid);

		List<Double> clickPrs = new ArrayList<Double>();
		for(int i = _IDVar-3; i >= 0; i --) {
			double clickPr = 0;
			double imps = _queryReports.get(_queryReports.size()-1-i).getImpressions(query);
			double clicks = _queryReports.get(_queryReports.size()-1-i).getClicks(query);
			if(imps != 0) {
				clickPr = clicks/imps;
			}
			clickPrs.add(clickPr);
		}


		int predCounter = 0;
		prediction += coeff[0];
		predCounter++;

		if(_queryIndicators) {
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
			prediction += coeff[1] * queryInd1;
			prediction += coeff[2] * queryInd2;
			prediction += coeff[3] * queryInd3;
			prediction += coeff[4] * queryInd4;
			prediction += coeff[5] * queryInd5;
			prediction += coeff[6] * queryInd6;
			predCounter += 6;
		}
		else if(_queryTypeIndicators) {
			int queryIndF1 = 0;
			int queryIndF2 = 0;
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				//do nothing
			}
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
				queryIndF1 = 1;
			}
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
				queryIndF2 = 1;
			}
			else {
				throw new RuntimeException("Malformed Query");
			}
			prediction += coeff[1]* queryIndF1;
			prediction += coeff[2] * queryIndF2;
			predCounter += 2;
		}

		for(int i = 0; i < bids.size(); i++) {
			double bid = bids.get(i);
			prediction += coeff[i+predCounter] * bid;
			if(_powers) {
				if(i == bids.size() - 1) {
					predCounter++;
					prediction += coeff[i+predCounter] * bid * bid;
					predCounter++;
					prediction += coeff[i+predCounter] * bid * bid * bid;
				}
			}
		}
		predCounter += bids.size();
		for(int i = 0; i < clickPrs.size(); i++) {
			double clickPr = clickPrs.get(i);
			prediction += coeff[i+predCounter] * clickPr;
			if(_powers) {
				if(i == clickPrs.size() - 1) {
					predCounter++;
					prediction += coeff[i+predCounter] * clickPr * clickPr;
					predCounter++;
					prediction += coeff[i+predCounter] * clickPr * clickPr * clickPr;
				}
			}
		}
		predCounter += clickPrs.size();

		double clickpr = 1/(1+Math.exp(-prediction));

		return clickpr;
	}

	/*
	 * MAKE SURE THAT THE BIDBUNDLE CORRESPONDS TO THE QUERY REPORT
	 */
	public boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle) {

		double start = System.currentTimeMillis();

		_queryReports.add(queryReport);
		_bidBundles.add(bidBundle);

		if(_bidBundles.size() != _queryReports.size()) {
			throw new RuntimeException("Uneven number of bidbundles and query reports");
		}

		/*
		 * Remove the oldest points from the model
		 */
		while(_bidBundles.size() > _numPrevDays) {
			_bidBundles.remove(0);
			_queryReports.remove(0);
			for(int i = 0; i < _numQueries; i++) {
				_bids.remove(0);
				_clickPrs.remove(0);
			}
		}

		for(Query query : _querySpace) {
			double bid = bidBundle.getBid(query);
			double imps = queryReport.getImpressions(query);
			double clicks = queryReport.getClicks(query);
			if(!(clicks == 0 || imps == 0)) {
				_bids.add(bid);
				_clickPrs.add(clicks/imps);
			}
			else {
				_bids.add(bid);
				_clickPrs.add(0.0);
			}
		}

		if(_bids.size() > _IDVar*_numQueries) {

			double[] bids = new double[_bids.size()];
			double[] prclicks = new double[_bids.size()];

			for(int i = 0; i < _bids.size(); i++) {
				bids[i] = _bids.get(i);
				prclicks[i] = _clickPrs.get(i);
			}


			int arrLen = _bids.size() - (_IDVar-1)*_numQueries ;

			int[] queryInd1 = new int[arrLen];
			int[] queryInd2 = new int[arrLen];
			int[] queryInd3 = new int[arrLen];
			int[] queryInd4 = new int[arrLen];
			int[] queryInd5 = new int[arrLen];
			int[] queryInd6 = new int[arrLen];

			int[] queryIndF1 = new int[arrLen];
			int[] queryIndF2 = new int[arrLen];

			if(_queryIndicators) {
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
			}
			else if(_queryTypeIndicators) {
				int numIters = queryIndF1.length/16;
				for(int i = 0; i < numIters; i++) {
					int j = 0;
					for(Query query : _querySpace) {
						queryIndF1[i*16 + j] = 0;
						queryIndF2[i*16 + j] = 0;
						if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
							//do nothing
						}
						else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
							queryIndF1[i*16 + j] = 1;
						}
						else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
							queryIndF2[i*16 + j] = 1;
						}
						else {
							throw new RuntimeException("Malformed Query");
						}
						j++;
					}
				}
			}

			try {
				if(_queryIndicators) {
					c.assign("queryInd1",queryInd1);
					c.assign("queryInd2",queryInd2);
					c.assign("queryInd3",queryInd3);
					c.assign("queryInd4",queryInd4);
					c.assign("queryInd5",queryInd5);
					c.assign("queryInd6",queryInd6);
				}
				else if(_queryTypeIndicators) {
					c.assign("queryIndF1",queryIndF1);
					c.assign("queryIndF2",queryIndF2);
				}
				c.assign("bids", bids);
				c.assign("prclicks", prclicks);

				String model = "model = glm(prclicks[" + ((_IDVar - 1)*_numQueries+1) + ":" + _bids.size() +  "] ~ ";
				if(_queryIndicators) {
					model += "queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + ";
				}
				else if(_queryTypeIndicators) {
					model += "queryIndF1 + queryIndF2 + ";
				}

				for(int i = 0; i < _IDVar; i++) {
					int min = i * _numQueries + 1;
					int max = _bids.size() - (_IDVar - 1 - i) * _numQueries;
					if(i != _IDVar - 2) {
						model += "bids[" + min +":" + max + "] + ";
					}
					if(_powers) {
						if(i == _IDVar - 1) {
							model += "I(bids[" + min +":" + max + "]^2) + ";
							model += "I(bids[" + min +":" + max + "]^3) + ";
						}
					}
				}

				for(int i = 0; i < _IDVar-2; i++) {
					int min = i * _numQueries + 1;
					int max = _bids.size() - (_IDVar - 1 - i) * _numQueries;

					model += "prclicks[" + min +":" + max + "] + ";
					if(_powers) {
						if(i == _IDVar -3) {
							model += "I(prclicks[" + min +":" + max + "]^2) + ";
							model += "I(prclicks[" + min +":" + max + "]^3) + ";
						}
					}
				}

				model = model.substring(0, model.length()-3);
				model += ", family = quasibinomial(link = \"logit\"))";


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
			//		System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public AbstractModel getCopy() {
		return new RegressionBidToPrClick(_querySpace, _IDVar, _numPrevDays, _queryIndicators, _queryTypeIndicators, _powers);
	}
}