package newmodels.postoprclick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.targeting.BasicTargetModel;

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

public class RegressionPosToPrClick extends AbstractPosToPrClick {

	private int DEBUG = 0;
	protected HashMap<Query,ArrayList<Double>> _pos;
	HashMap<Query,ArrayList<Double>> _clickPrs;
	protected Set<Query> _querySpace;
	protected int	_counter;
	protected int[] _predCounter;
	private RConnection c;
	private int _numQueries;
	private int _IDVar;  //THIS NEEDS TO BE MORE THAN 4, LESS THAN 10
	private int _numPrevDays;	//How many days worth of data to include in the regression
	private ArrayList<QueryReport> _queryReports;
	private boolean _queryIndicators;
	private boolean _queryTypeIndicators;
	private boolean _powers;
	private BasicTargetModel _targModel;
	private boolean _weighted;
	private double _mWeight = 0.85;
	private boolean _robust;
	private HashMap<Query, double[]> _coefficients;
	private boolean _perQuery;
	private double _outOfAuctionPos = 6.0;
	private boolean _targetModification = false;


	public RegressionPosToPrClick(RConnection rConnection, Set<Query> queryspace, boolean perQuery, int IDVar, int numPrevDays, BasicTargetModel targModel, boolean weighted, double mWeight, boolean robust, boolean queryIndicators, boolean queryTypeIndicators, boolean powers) {
		c = rConnection;
		_pos = new HashMap<Query,ArrayList<Double>>();
		_clickPrs = new HashMap<Query,ArrayList<Double>>();
		_coefficients = new HashMap<Query, double[]>();
		_queryReports = new ArrayList<QueryReport>();
		_querySpace = queryspace;
		_numQueries = _querySpace.size();
		_perQuery = perQuery;
		_IDVar = IDVar;
		_numPrevDays = numPrevDays;
		_weighted = weighted;
		_mWeight = mWeight;
		_robust = robust;
		_queryIndicators = queryIndicators;
		_queryTypeIndicators = queryTypeIndicators;
		_powers = powers;
		_targModel = targModel;

		for(Query query : _querySpace) {
			ArrayList<Double> pos = new ArrayList<Double>();
			ArrayList<Double> clickPrs = new ArrayList<Double>();
			_pos.put(query, pos);
			_clickPrs.put(query, clickPrs);
			_coefficients.put(query, null);
		}

		if(_robust) {
			try {
				c.voidEval("library(robust)");
			} catch (RserveException e) {
				throw new RuntimeException("Could not load the R robust library");
			}
		}
	}

	@Override
	/*
	 * The bid bundle is from the day before, the bid is for tomorrow
	 */
	public double getPrediction(Query query, double currentPos, Ad currentAd){
		double[] coeff = _coefficients.get(query);
		if(coeff == null) {
			return Double.NaN;
		}

		if(currentPos == _outOfAuctionPos) {
			return 0.0;
		}
		
		double prediction = 0.0;
		
		/*
		 * oldest - > newest
		 */
		List<Double> pos = new ArrayList<Double>();
		for(int i = 0; i < _IDVar - 2; i++) {
			double tempPos = _queryReports.get(_queryReports.size() - 1 - (_IDVar - 3 -i)).getPosition(query);
			if(Double.isNaN(tempPos)) {
				tempPos = _outOfAuctionPos ;
			}
			pos.add(tempPos);
		}
		pos.add(currentPos);

		List<Double> clickPrs = new ArrayList<Double>();
		for(int i = _IDVar-3; i >= 0; i --) {
			double clickPr = 0.0;
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

		for(int i = 0; i < pos.size(); i++) {
			double tempPos = pos.get(i);
			prediction += coeff[i+predCounter] * tempPos;
			if(_powers) {
				if(i == pos.size() - 1) {
					predCounter++;
					prediction += coeff[i+predCounter] * tempPos * tempPos;
					predCounter++;
					prediction += coeff[i+predCounter] * tempPos * tempPos * tempPos;
				}
			}
		}
		predCounter += pos.size();
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
		
		if(Double.isNaN(clickpr)) {
			return Double.NaN;
		}

		if(_targetModification && currentAd != null && !currentAd.isGeneric()) {
			clickpr = _targModel.getClickPrPrediction(query,clickpr,false);
		}

		if(clickpr < 0) {
			return 0.0;
		}

		double bound;
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			bound = .4;
		}
		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			bound = .5;
		}
		else {
			bound = .6;
		}

		if(clickpr > bound) {
			return bound;
		}
		
		return clickpr;
	}

	/*
	 * MAKE SURE THAT THE BIDBUNDLE CORRESPONDS TO THE QUERY REPORT
	 */
	public boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle) {

		double start = System.currentTimeMillis();
		_queryReports.add(queryReport);

		/*
		 * Remove the oldest points from the model
		 */
		while(_queryReports.size() > _numPrevDays) {
			_queryReports.remove(0);
			for(Query query : _querySpace) {
				ArrayList<Double> pos = _pos.get(query);
				ArrayList<Double> clickPrs = _clickPrs.get(query);
				pos.remove(0);
				clickPrs.remove(0);
				_pos.put(query,pos);
				_clickPrs.put(query,clickPrs);
			}
		}

		for(Query query : _querySpace) {
			ArrayList<Double> pos = _pos.get(query);
			ArrayList<Double> clickPrs = _clickPrs.get(query);
			double tempPos = queryReport.getPosition(query);
			if(Double.isNaN(tempPos)) {
				tempPos = _outOfAuctionPos;
			}
			double imps = queryReport.getImpressions(query);
			double clicks = queryReport.getClicks(query);
			double conversions = salesReport.getConversions(query);
			if(!(clicks == 0 || imps == 0)) {
				if(_targetModification && bidBundle.getAd(query) != null && !bidBundle.getAd(query).isGeneric()) {
					double[] multipliers = _targModel.getInversePredictions(query, (clicks/((double) imps)), (conversions/((double) clicks)), false);
					clicks = (int) (imps * multipliers[0]);
				}
				pos.add(tempPos);
				clickPrs.add(clicks/imps);
			}
			else {
				pos.add(tempPos);
				clickPrs.add(0.0);
			}
			_pos.put(query,pos);
			_clickPrs.put(query,clickPrs);
		}

		if(_queryReports.size() > _IDVar+1) {
			if(_perQuery) {
				for(Query query : _querySpace) {
					ArrayList<Double> posArr = _pos.get(query);
					ArrayList<Double> clickPrArr = _clickPrs.get(query);

					int len = posArr.size();

					double[] pos = new double[len];
					double[] prclicks = new double[len];
					double[] weights = new double[len];

					for(int i = 0; i < len; i++) {
						pos[i] = posArr.get(i);
						prclicks[i] = clickPrArr.get(i);
						if(_weighted) {
							/*
							 * For our WLS we weight the points by $m^{t-t_i}$ where 
							 * $0 < m < 1$ and $t - t_i$ is the difference between the
							 * day we are predicting and the day we observed the data
							 */
							weights[i] = Math.pow(_mWeight,_queryReports.size() + 2 - i);
						}
					}


					try {
						c.assign("pos", pos);
						c.assign("prclicks", prclicks);

						String model;

						if(_robust) {
							model = "model = glmRob(prclicks[" + ((_IDVar - 1)+1) + ":" + _queryReports.size() +  "] ~ ";
						}
						else {
							model = "model = glm(prclicks[" + ((_IDVar - 1)+1) + ":" + _queryReports.size() +  "] ~ ";
						}

						for(int i = 0; i < _IDVar; i++) {
							int min = i + 1;
							int max = _queryReports.size() - (_IDVar - 1 - i);
							if(i != _IDVar - 2) {
								model += "pos[" + min +":" + max + "] + ";
							}
							if(_powers) {
								if(i == _IDVar - 1) {
									model += "I(pos[" + min +":" + max + "]^2) + ";
									model += "I(pos[" + min +":" + max + "]^3) + ";
								}
							}
						}

						for(int i = 0; i < _IDVar-2; i++) {
							int min = i + 1;
							int max = _queryReports.size() - (_IDVar - 1 - i);
							model += "prclicks[" + min +":" + max + "] + ";
							if(_powers) {
								if(i == _IDVar -3) {
									model += "I(prclicks[" + min +":" + max + "]^2) + ";
									model += "I(prclicks[" + min +":" + max + "]^3) + ";
								}
							}
						}

						model = model.substring(0, model.length()-3);

						if(_robust) {
							model += ", family = binomial(link = \"logit\")";
						}
						else {
							model += ", family = quasibinomial(link = \"logit\")";
						}

						if(_weighted == true) {
							c.assign("regweights", weights);
							model += ", weights = regweights[" + ((_IDVar - 1)+1) + ":" + _queryReports.size() +  "]";
						}

						model += ")";

						//						System.out.println(model);				
						c.voidEval(model);
						double[] coeff = c.eval("coefficients(model)").asDoubles();
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
						if(DEBUG > 1) {
							e.printStackTrace();
						}
						_coefficients.put(query, null);
					}
					catch (REXPMismatchException e) {
						if(DEBUG > 1) {
							e.printStackTrace();
						}
						_coefficients.put(query, null);
					}
					
					if(_coefficients.get(query) != null) {
						if(!monotonicCheck(query)) {
							System.out.println(toString() + " FAILED MONOTONIC CHECK");
							_coefficients.put(query, null);
						}
					}

					double stop = System.currentTimeMillis();
					double elapsed = stop - start;
					//		System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");
				}
				boolean nonNullQuery = false;
				for(Query query : _querySpace) {
					if(_coefficients.get(query) != null) {
						nonNullQuery = true;
					}
				}
				return nonNullQuery;
			}
			else {
				int len = _querySpace.size() * _queryReports.size();
				double[] pos = new double[len];
				double[] prclicks = new double[len];
				double[] weights = new double[len];

				int idx = 0;
				for(int i = 0; i < _queryReports.size(); i++) {
					for(Query query : _querySpace) {
						ArrayList<Double> posVec = _pos.get(query);
						ArrayList<Double> clickPrVec = _clickPrs.get(query);
						pos[idx] = posVec.get(i);
						prclicks[idx] = clickPrVec.get(i);
						if(_weighted) {
							/*
							 * For our WLS we weight the points by $m^{t-t_i}$ where 
							 * $0 < m < 1$ and $t - t_i$ is the difference between the
							 * day we are predicting and the day we observed the data
							 */
							weights[idx] = Math.pow(_mWeight,_queryReports.size() + 2 - i);
						}
						idx++;
					}
				}


				int arrLen = (_queryReports.size() - (_IDVar-1))*_querySpace.size();

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
					c.assign("pos", pos);
					c.assign("prclicks", prclicks);

					String model;

					if(_robust) {
						model = "model = glmRob(prclicks[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_queryReports.size() * _querySpace.size()) +  "] ~ ";
					}
					else {
						model = "model = glm(prclicks[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_queryReports.size() * _querySpace.size()) +  "] ~ ";
					}

					if(_queryIndicators) {
						model += "queryInd1 + queryInd2 + queryInd3 + queryInd4 + queryInd5 + queryInd6 + ";
					}
					else if(_queryTypeIndicators) {
						model += "queryIndF1 + queryIndF2 + ";
					}

					for(int i = 0; i < _IDVar; i++) {
						int min = i * _numQueries + 1;
						int max = (_queryReports.size() - (_IDVar - 1 - i)) * _querySpace.size();
						if(i != _IDVar - 2) {
							model += "pos[" + min +":" + max + "] + ";
						}
						if(_powers) {
							if(i == _IDVar - 1) {
								model += "I(pos[" + min +":" + max + "]^2) + ";
								model += "I(pos[" + min +":" + max + "]^3) + ";
							}
						}
					}

					for(int i = 0; i < _IDVar-2; i++) {
						int min = i * _numQueries + 1;
						int max = (_queryReports.size() - (_IDVar - 1 - i)) * _querySpace.size();

						model += "prclicks[" + min +":" + max + "] + ";
						if(_powers) {
							if(i == _IDVar -3) {
								model += "I(prclicks[" + min +":" + max + "]^2) + ";
								model += "I(prclicks[" + min +":" + max + "]^3) + ";
							}
						}
					}

					model = model.substring(0, model.length()-3);

					if(_robust) {
						model += ", family = binomial(link = \"logit\")";
					}
					else {
						model += ", family = quasibinomial(link = \"logit\")";
					}

					if(_weighted == true) {
						c.assign("regweights", weights);
						model += ", weights = regweights[" + ((_IDVar - 1)*_numQueries+1) + ":" + (_queryReports.size() * _querySpace.size()) +  "]";
					}

					model += ")";

//					System.out.println(model);				
					c.voidEval(model);
					double[] coeff = c.eval("coefficients(model)").asDoubles();
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
					if(DEBUG > 1) {
						e.printStackTrace();
					}
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					return false;
				}
				catch (REXPMismatchException e) {
					if(DEBUG > 1) {
						e.printStackTrace();
					}
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					return false;
				}

				Query query = _querySpace.iterator().next();
				if(_coefficients.get(query) != null) {
					if(!monotonicCheck(query)) {
						System.out.println(toString() + " FAILED MONOTONIC CHECK");
						for(Query q : _querySpace) {
							_coefficients.put(q, null);
						}
					}
				}
				
				double stop = System.currentTimeMillis();
				double elapsed = stop - start;
				//		System.out.println("\n\n\n\n\nThis took " + (elapsed / 1000) + " seconds\n\n\n\n\n");

				return true;
			}
		}
		else {
			return false;
		}
	}
	
	private boolean monotonicCheck(Query query) {
		double lastPrClick = 0;
		for(double pos = 6.0; pos >= 1.0; pos -= .25) {
			double prClick = getPrediction(query, pos, new Ad());
			if(!(prClick >= lastPrClick)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new RegressionPosToPrClick(c, _querySpace, _perQuery, _IDVar, _numPrevDays, _targModel, _weighted, _mWeight, _robust, _queryIndicators, _queryTypeIndicators, _powers);
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
		_targModel = new BasicTargetModel(manufacturer,component);
	}

	@Override
	public String toString() {
		return "RegressionPosToPrClick(_rConnection, _querySpace, " + _perQuery + ", " +  _IDVar + ", " + _numPrevDays + ", targModel, " + _weighted  + ", " + _mWeight + ", " + _robust + ", " + _queryIndicators + ", " + _queryTypeIndicators + ", " + _powers + ")";
//		return "RegressionPosToPrClick(perQuery: " + _perQuery + ", IDVar: " + _IDVar + ", numPrevDays: " + _numPrevDays + ", weighted: " + _weighted + ", robust: " +  _robust + ", queryInd: " + _queryIndicators + ", queryTypeInd: " + _queryTypeIndicators + ", powers: " +  _powers;
	}

}