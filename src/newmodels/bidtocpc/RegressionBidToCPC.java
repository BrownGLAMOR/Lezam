package newmodels.bidtocpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import newmodels.AbstractModel;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author afoo & jberg
 *
 */

public class RegressionBidToCPC extends AbstractBidToCPC {

	protected HashMap<Query, ArrayList<Double>> _bids;
	protected HashMap<Query,ArrayList<Double>> _CPCs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection _rConnection;
	private int _numQueries = 16;
	private int _IDVar;
	private boolean _perQuery;
	private int _numPrevDays;
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private int predictErrors = 0;
	private boolean _queryIndicators;
	private boolean _queryTypeIndicators;
	private boolean _powers;
	private boolean _weighted;
	private double _mWeight;
	private boolean _robust;
	private boolean _loglinear;
	/*
	 * When we don't get a position, we get NaN as our CPC
	 */
	private boolean _ignoreNaN;
	private HashMap<Query, double[]> _coefficients;

	public RegressionBidToCPC(RConnection rConnection, Set<Query> queryspace, boolean perQuery, int IDVar, int numPrevDays, boolean weighted, double mWeight, boolean robust, boolean loglinear, boolean queryIndicators, boolean queryTypeIndicators, boolean powers, boolean ignoreNaN) {
		_rConnection = rConnection;
		_bids = new HashMap<Query,ArrayList<Double>>();
		_CPCs = new HashMap<Query,ArrayList<Double>>();
		_coefficients = new HashMap<Query, double[]>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		_querySpace = queryspace;
		_perQuery = perQuery;
		_IDVar = IDVar;
		_numPrevDays = numPrevDays;
		_weighted = weighted;
		_mWeight = mWeight;
		_robust = robust;
		_loglinear = loglinear;
		_queryIndicators = queryIndicators;
		_queryTypeIndicators = queryTypeIndicators;
		_ignoreNaN = ignoreNaN;
		_powers = powers;

		for(Query query : _querySpace) {
			ArrayList<Double> bids = new ArrayList<Double>();
			ArrayList<Double> CPCs = new ArrayList<Double>();
			_bids.put(query, bids);
			_CPCs.put(query, CPCs);
			_coefficients.put(query, null);
		}

		if(_robust) {
			try {
				_rConnection.voidEval("library(MASS)");
			} catch (RserveException e) {
				throw new RuntimeException("Could not load the R MASS library");
			}
		}
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentBid){
		double[] coeff = _coefficients.get(query);
		if(coeff == null) {
			return Double.NaN;
		}

		double prediction = 0.0;
		/*
		 * oldest - > newest
		 */
		List<Double> bids = new ArrayList<Double>();
		for(int i = 0; i < _IDVar - 2; i++) {
			bids.add(_bidBundles.get(_bidBundles.size() - 1 - (_IDVar - 3 -i)).getBid(query));
		}
		bids.add(currentBid);

		List<Double> CPCs = new ArrayList<Double>();
		for(int i = _IDVar-3; i >= 0; i --) {
			double cpc = _queryReports.get(_queryReports.size()-1-i).getCPC(query);
			CPCs.add(cpc);
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
		for(int i = 0; i < CPCs.size(); i++) {
			double CPC = CPCs.get(i);
			if(Double.isNaN(CPC)) {
				CPC = 0;
			}
			prediction += coeff[i+predCounter] * CPC;
			if(_powers) {
				if(i == CPCs.size() - 1) {
					predCounter++;
					prediction += coeff[i+predCounter] * CPC * CPC;
					predCounter++;
					prediction += coeff[i+predCounter] * CPC * CPC * CPC;
				}
			}
		}
		predCounter += CPCs.size();

		if(_loglinear) {
			prediction = Math.exp(prediction);
		}

		if(Double.isNaN(prediction)) {
			return Double.NaN;
		}

		/*
		 * Our CPC can never be higher than our bid
		 */
		if(prediction > currentBid) {
			return currentBid;
		}

		if(prediction < 0.0) {
			return 0.0;
		}

		return currentBid;
	}

	/*
	 * MAKE SURE THAT THE BIDBUNDLE CORRESPONDS TO THE QUERY REPORT
	 */
	public boolean updateModel(QueryReport queryreport, SalesReport salesReport, BidBundle bidbundle) {

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
			for(Query query : _querySpace) {
				ArrayList<Double> bids = _bids.get(query);
				ArrayList<Double> CPCs = _CPCs.get(query);
				bids.remove(0);
				CPCs.remove(0);
				_bids.put(query,bids);
				_CPCs.put(query,CPCs);
			}
		}


		for(Query query : _querySpace) {
			ArrayList<Double> bids = _bids.get(query);
			ArrayList<Double> CPCs = _CPCs.get(query);
			double bid = bidbundle.getBid(query);
			double CPC = queryreport.getCPC(query);
			if(!(Double.isNaN(CPC))) {
				bids.add(bid);
				if(_loglinear) {
					CPCs.add(Math.log(CPC));
				}
				else {
					CPCs.add(CPC);
				}
			}
			else {
				if(_ignoreNaN) {
					bids.add(0.0);
					if(_loglinear) {
						CPCs.add(Math.log(0.01));
					}
					else {
						CPCs.add(0.0);
					}
				}
				else {
					bids.add(bid);
					if(_loglinear) {
						CPCs.add(Math.log(0.01));
					}
					else {
						CPCs.add(0.01);
					}
				}
			}
			_bids.put(query,bids);
			_CPCs.put(query,CPCs);
		}

		if(_bidBundles.size() > _IDVar + 1) {
			if(_perQuery) {
				for(Query query: _querySpace) {
					ArrayList<Double> bidsArr = _bids.get(query);
					ArrayList<Double> CPCArr = _CPCs.get(query);

					int len = bidsArr.size();

					double[] bids = new double[len];
					double[] cpcs = new double[len];
					double[] weights = new double[len];

					for(int i = 0; i < len; i++) {
						bids[i] = bidsArr.get(i);
						cpcs[i] = CPCArr.get(i);
						if(_weighted) {
							/*
							 * For our WLS we weight the points by $m^{t-t_i}$ where 
							 * $0 < m < 1$ and $t - t_i$ is the difference between the
							 * day we are predicting and the day we observed the data
							 */
							weights[i] = Math.pow(_mWeight, _bidBundles.size()  + 2 - i);
						}
					}

					try {
						_rConnection.assign("bids", bids);
						_rConnection.assign("cpcs", cpcs);

						String model;


						if(_robust) {
							model = "model = rlm(cpcs[" + ((_IDVar - 1)+1) + ":" + _bidBundles.size() +  "] ~ ";
						}
						else {
							model = "model = lm(cpcs[" + ((_IDVar - 1)+1) + ":" + _bidBundles.size() +  "] ~ ";
						}

						for(int i = 0; i < _IDVar; i++) {
							int min = i + 1;
							int max = _bidBundles.size() - (_IDVar - 1 - i);
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
							int min = i + 1;
							int max = _bidBundles.size() - (_IDVar - 1 - i);
							model += "cpcs[" + min +":" + max + "] + ";
							if(_powers) {
								if(i == _IDVar -3) {
									model += "I(cpcs[" + min +":" + max + "]^2) + ";
									model += "I(cpcs[" + min +":" + max + "]^3) + ";
								}
							}
						}

						model = model.substring(0, model.length()-3);

						if(_weighted == true) {
							_rConnection.assign("regweights", weights);
							model += ", weights = regweights[" + ((_IDVar - 1)+1) + ":" + _bidBundles.size() +  "]";
						}

						model += ")";

						//				System.out.println(model);				
						_rConnection.voidEval(model);
						double[] coeff = _rConnection.eval("coefficients(model)").asDoubles();
						//				for(int i = 0 ; i < coeff.length; i++)
						//					System.out.println(coeff[i]);
						_coefficients.put(query, coeff);
						for(int i = 0; i < coeff.length; i++) {
							if(Double.isNaN(coeff[i])) {
								_coefficients.put(query, null);
							}
						}
					}
					catch (REngineException e) {
						e.printStackTrace();
						_coefficients.put(query, null);
					}
					catch (REXPMismatchException e) {
						e.printStackTrace();
						_coefficients.put(query, null);
					}

					double stop = System.currentTimeMillis();
					double elapsed = stop - start;
					//			System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

				}
				return true;
			}
			else {
				int len = _querySpace.size() * _bidBundles.size();
				double[] bids = new double[len];
				double[] cpcs = new double[len];
				double[] weights = new double[len];

				int idx = 0;
				for(int i = 0; i < _bidBundles.size(); i++) {
					for(Query query : _querySpace) {
						ArrayList<Double> bidVec = _bids.get(query);
						ArrayList<Double> CPCVec = _CPCs.get(query);
						bids[idx] = bidVec.get(i);
						cpcs[idx] = CPCVec.get(i);
						if(_weighted) {
							/*
							 * For our WLS we weight the points by $m^{t-t_i}$ where 
							 * $0 < m < 1$ and $t - t_i$ is the difference between the
							 * day we are predicting and the day we observed the data
							 */
							weights[idx] = Math.pow(_mWeight, _bidBundles.size()  + 2 - i);
						}
						idx++;
					}
				}

				int arrLen = (_bidBundles.size() - (_IDVar-1))*_querySpace.size();

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
						_rConnection.assign("queryInd1",queryInd1);
						_rConnection.assign("queryInd2",queryInd2);
						_rConnection.assign("queryInd3",queryInd3);
						_rConnection.assign("queryInd4",queryInd4);
						_rConnection.assign("queryInd5",queryInd5);
						_rConnection.assign("queryInd6",queryInd6);
					}
					else if(_queryTypeIndicators) {
						_rConnection.assign("queryIndF1",queryIndF1);
						_rConnection.assign("queryIndF2",queryIndF2);
					}
					_rConnection.assign("bids", bids);
					_rConnection.assign("cpcs", cpcs);

					String model;


					if(_robust) {
						model = "model = rlm(cpcs[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_bidBundles.size() * _querySpace.size()) +  "] ~ ";
					}
					else {
						model = "model = lm(cpcs[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_bidBundles.size() * _querySpace.size()) +  "] ~ ";
					}

					if(_queryIndicators) {
						model += "queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + ";
					}
					else if(_queryTypeIndicators) {
						model += "queryIndF1 + queryIndF2 + ";
					}

					for(int i = 0; i < _IDVar; i++) {
						int min = i * _numQueries + 1;
						int max = (_bidBundles.size() - (_IDVar - 1 - i)) * _querySpace.size();
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
						int max = (_bidBundles.size() - (_IDVar - 1 - i)) * _querySpace.size();
						model += "cpcs[" + min +":" + max + "] + ";
						if(_powers) {
							if(i == _IDVar -3) {
								model += "I(cpcs[" + min +":" + max + "]^2) + ";
								model += "I(cpcs[" + min +":" + max + "]^3) + ";
							}
						}
					}

					model = model.substring(0, model.length()-3);

					if(_weighted == true) {
						_rConnection.assign("regweights", weights);
						model += ", weights = regweights[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_bidBundles.size() * _querySpace.size()) +  "]";
					}

					model += ")";

					//				System.out.println(model);				
					_rConnection.voidEval(model);
					double[] coeff = _rConnection.eval("coefficients(model)").asDoubles();
					//				for(int i = 0 ; i < coeff.length; i++)
					//					System.out.println(coeff[i]);
					for(Query query : _querySpace) {
						_coefficients.put(query, coeff);
					}
					for(int i = 0; i < coeff.length; i++) {
						if(Double.isNaN(coeff[i])) {
							for(Query query : _querySpace) {
								_coefficients.put(query, null);
							}
							return false;
						}
					}
				}
				catch (REngineException e) {
					e.printStackTrace();
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					return false;
				}
				catch (REXPMismatchException e) {
					e.printStackTrace();
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					return false;
				}

				double stop = System.currentTimeMillis();
				double elapsed = stop - start;
				//			System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

				return true;
			}
		}
		else {
			return false;
		}
	}

	@Override
	public AbstractModel getCopy() {
		return new RegressionBidToCPC(_rConnection, _querySpace, _perQuery, _IDVar, _numPrevDays, _weighted, _mWeight, _robust,_loglinear,_queryIndicators, _queryTypeIndicators, _powers,_ignoreNaN);
	}

	@Override
	public String toString() {
//		return "RegressionBidToCPC(perQuery: " + _perQuery + ", IDVar: " + _IDVar + ", numPrevDays: " + _numPrevDays + ", weighted: " + _weighted + ", mWeight: " + _mWeight + ", robust: " +  _robust + ", loglinear: " + _loglinear + ", queryInd: " + _queryIndicators + ", queryTypeInd: " + _queryTypeIndicators + ", powers: " +  _powers + ", ignoreNan: " + _ignoreNaN;
		return "RegressionBidToCPC(_rConnection, _querySpace, " + _perQuery + ", " +  _IDVar + ", " + _numPrevDays + ", " + _weighted  + ", " + _mWeight + ", " + _robust + ", " + _loglinear + ", " + _queryIndicators + ", " + _queryTypeIndicators + ", " + _powers + ", " + _ignoreNaN + ")";
	}
}