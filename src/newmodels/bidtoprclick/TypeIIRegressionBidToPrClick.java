package newmodels.bidtoprclick;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;

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

public class TypeIIRegressionBidToPrClick extends AbstractBidToPrClick {

	protected ArrayList<Double> _bids , _clickPrs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private double[] coeff;
	private int _numQueries;
	private int _numF0Queries = 1;
	private int _numF1Queries = 6;
	private int _numF2Queries = 9;
	private QueryType _queryType;
	private int _IDVar;  //THIS NEEDS TO BE MORE THAN 4, LESS THAN 10
	private int _numPrevDays;	//How many days worth of data to include in the regression
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private boolean _powers;


	public TypeIIRegressionBidToPrClick(RConnection rConnection, Set<Query> queryspace, QueryType queryType, int IDVar, int numPrevDays, boolean powers) {
		c = rConnection;
		_bids = new ArrayList<Double>();
		_clickPrs = new ArrayList<Double>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		_querySpace = queryspace;
		_queryType = queryType;
		if(_queryType == QueryType.FOCUS_LEVEL_ZERO) {
			_numQueries = _numF0Queries;
		}
		else if(_queryType == QueryType.FOCUS_LEVEL_ONE) {
			_numQueries = _numF1Queries;
		}
		else if(_queryType == QueryType.FOCUS_LEVEL_TWO) {
			_numQueries = _numF2Queries;
		}
		else {
			throw new RuntimeException("Malformed QueryType");
		}
		_IDVar = IDVar;
		_numPrevDays = numPrevDays;
		_powers = powers;
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid, Ad currentAd){
		if(query.getType() == _queryType) {
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
				if(imps != 0 && clicks != 0) {
					clickPr = clicks/imps;
				}
				clickPrs.add(clickPr);
			}


			int predCounter = 0;
			prediction += coeff[0];
			predCounter++;

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
		else {
			return Double.NaN;
		}
	}

	/*
	 * MAKE SURE THAT THE BIDBUNDLE CORRESPONDS TO THE QUERY REPORT
	 */
	public boolean updateModel(QueryReport queryreport, BidBundle bidbundle) {

		double start = System.currentTimeMillis();

		_queryReports.add(queryreport);
		_bidBundles.add(bidbundle);

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
			if(_queryType == query.getType()) {
				double bid = bidbundle.getBid(query);
				double imps = queryreport.getImpressions(query);
				double clicks = queryreport.getClicks(query);
				if(!(clicks == 0 || imps == 0)) {
					_bids.add(bid);
					_clickPrs.add(clicks/imps);
				}
				else {
					_bids.add(bid);
					_clickPrs.add(0.0);
				}
			}
		}

		if(_bids.size() > _IDVar*_numQueries) {

			double[] bids = new double[_bids.size()];
			double[] prclicks = new double[_bids.size()];

			for(int i = 0; i < _bids.size(); i++) {
				bids[i] = _bids.get(i);
				prclicks[i] = _clickPrs.get(i);
			}



			try {
				c.assign("bids", bids);
				c.assign("prclicks", prclicks);

				String model = "model = glm(prclicks[" + ((_IDVar - 1)*_numQueries+1) + ":" + _bids.size() +  "] ~ ";

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
			
			for(int i = 0; i < coeff.length; i++) {
				if(Double.isNaN(coeff[i])) {
					return false;
				}
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
}