package models.bidtopos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import models.AbstractModel;
import models.avgpostoposdist.AvgPosToPosDist;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class SpatialBidToPos extends AbstractBidToPos {

	private RConnection _rConnection;
	private Set<Query> _querySpace;
	private HashMap<Query, ArrayList<Double>> _bids;
	private HashMap<Query, ArrayList<double[]>> _posDists;
	private HashMap<Query, double[]> _coefficients;
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private int _degree;
	private boolean _weighted;
	private double _m = .85;
	private int _numPrevDays;
	private boolean _perQuery;
	private double _outOfAuction = 6.0;
	private AvgPosToPosDist _avgPosToDistModel;

	public SpatialBidToPos(RConnection rConnection, Set<Query> querySpace, AvgPosToPosDist avgPosToDistModel, boolean perQuery, int degree, int numPrevDays, boolean weighted, double mWeight) {
		_rConnection = rConnection;
		_querySpace = querySpace;
		_avgPosToDistModel = avgPosToDistModel;
		_bids = new HashMap<Query,ArrayList<Double>>();
		_posDists = new HashMap<Query,ArrayList<double[]>>();
		_coefficients = new HashMap<Query, double[]>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		for(Query query : _querySpace) {
			ArrayList<Double> bids = new ArrayList<Double>();
			ArrayList<double[]> posDists = new ArrayList<double[]>();
			_bids.put(query, bids);
			_posDists.put(query, posDists);
			_coefficients.put(query, null);
		}
		_perQuery = perQuery;
		_degree = degree;
		_numPrevDays = numPrevDays;
		_weighted = weighted;
		_m = mWeight;
	}

	@Override
	public double getPrediction(Query query, double bid) {
		double[] posDist = getDistPrediction(query,bid);
		if(posDist == null) {
			return Double.NaN;
		}

		double avgPos = 0.0;
		double posTot = 0.0;

		for(int j = 0; j < posDist.length; j++) {
			avgPos += posDist[j] * (j+1);
			posTot += posDist[j];
		}

		if(posTot == 0.0 || Double.isNaN(posTot)) {
			avgPos = Double.NaN;
		}
		else {
			avgPos /= posTot;
		}

		if(avgPos < 1.0) {
			return 1.0;
		}

		if(avgPos > _outOfAuction) {
			return _outOfAuction;
		}

		return avgPos;
	}


	public double[] getDistPrediction(Query query, double bid) {
		double[] coeff = _coefficients.get(query);
		if(coeff == null) {
			return null;
		}
		double predictions[] = new double[6];
		for(int i = 0; i < predictions.length; i++) {
			int pos = i+1;
			double prediction = 0.0;
			if(_degree == 1) {
				prediction = coeff[0] + coeff[1] * pos + coeff[2] * bid;
			}
			else if(_degree == 2) {
				prediction = coeff[0] + coeff[1] * pos + coeff[2] * pos * pos + coeff[3] * bid + coeff[4] * bid * pos + coeff[5] * bid * bid;
			}
			else if(_degree == 3) {
				prediction = coeff[0] + coeff[1] * pos + coeff[2] * pos * pos + coeff[3] * pos * pos * pos + coeff[4] * bid + coeff[5] * bid * pos + coeff[6] * bid * pos * pos + coeff[7] * bid * bid + coeff[8] * bid * bid * pos + coeff[9] * bid * bid * bid;
			}
			else if(_degree == 4) {
				prediction = coeff[0] + coeff[1] * pos + coeff[2] * pos * pos + coeff[3] * pos * pos * pos + coeff[4] * pos * pos * pos * pos + coeff[5] * bid + coeff[6] * bid * pos + coeff[7] * bid * pos * pos + coeff[8] * bid * pos * pos * pos + coeff[9] * bid * bid + coeff[10] * bid * bid * pos + coeff[11] * bid * bid * pos * pos + coeff[12] * bid * bid * bid + coeff[13] * bid * bid * bid * pos + coeff[14] * bid * bid * bid * bid;
			}
			else if(_degree == 5) {
				prediction = coeff[0] + coeff[1] * pos + coeff[2] * pos * pos + coeff[3] * pos * pos * pos + coeff[4] * pos * pos * pos * pos + coeff[5] * pos * pos * pos * pos * pos + coeff[6] * bid + coeff[7] * bid * pos + coeff[8] * bid * pos * pos + coeff[9] * bid * pos * pos * pos + coeff[10] * bid * pos * pos * pos * pos + coeff[11] * bid * bid + coeff[12] * bid * bid * pos + coeff[13] * bid * bid * pos * pos + coeff[14] * bid * bid * pos * pos * pos + coeff[15] * bid * bid * bid + coeff[16] * bid * bid * bid * pos + coeff[17] * bid * bid * bid * pos * pos + coeff[18] * bid * bid * bid * bid + coeff[19] * bid * bid * bid * bid * pos + coeff[20] * bid * bid * bid * bid * bid;
			}
			//				System.out.println("Query: " + query + ", Pred: " + prediction);
			predictions[i] = prediction;
		}
		double totPosDist = 0.0;
		for(int i = 0; i < predictions.length; i++) {
			if(predictions[i] < 0) {
				predictions[i] = 0.0;
			}
			totPosDist += predictions[i];
		}

		if(totPosDist == 0 || Double.isNaN(totPosDist)) {
			return null;
		}

		for(int i = 0; i < predictions.length; i++) {
			predictions[i] = predictions[i]/totPosDist;
		}

		return predictions;
	}

	@Override
	/*
	 * All of these reports should correspond to the same auction.
	 * 
	 * posDists must be normalized first!
	 */
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		HashMap<Query,double[]> posDists = new HashMap<Query, double[]>();
		for(Query query : _querySpace) {
			double[] posDist = _avgPosToDistModel.getPrediction(query, queryReport.getRegularImpressions(query), queryReport.getPromotedImpressions(query), queryReport.getPosition(query), queryReport.getClicks(query));
			posDists.put(query, posDist);
		}

		//First normalize the arrs
		for(Query query : _querySpace) {
			double[] posDist = posDists.get(query);
			double posTotal = 0.0;
			for(int i = 0; i < posDist.length; i++) {
				posTotal += posDist[i];
			}
			for(int i = 0; i < posDist.length; i++) {
				posDist[i] = posDist[i]/posTotal;
			}
			if(posTotal == 0 || Double.isNaN(posTotal)) {
				for(int i = 0; i < posDist.length-1; i++) {
					posDist[i] = 0.0;
				}
				posDist[posDist.length-1]  = 1.0;
			}
			posDists.put(query, posDist);
		}


		_queryReports.add(queryReport);
		_bidBundles.add(bidBundle);
		int posLen = posDists.get(_querySpace.iterator().next()).length;

		if(_bidBundles.size() != _queryReports.size()) {
			throw new RuntimeException("Uneven number of bidbundles and query reports");
		}

		while(_bidBundles.size() > _numPrevDays) {
			_bidBundles.remove(0);
			_queryReports.remove(0);
			for(Query query : _querySpace) {
				ArrayList<Double> bids = _bids.get(query);
				ArrayList<double[]> posDist = _posDists.get(query);
				bids.remove(0);
				posDist.remove(0);
				_bids.put(query, bids);
				_posDists.put(query, posDist);
			}
		}

		HashMap<Query,double[]> weightVecs = new HashMap<Query, double[]>();
		double[] weightVec = new double[_querySpace.size() * _bidBundles.size()*posLen];


		for(Query query : _querySpace) {
			double bid = bidBundle.getBid(query);
			double[] posDist = posDists.get(query);



			ArrayList<Double> bids = _bids.get(query);
			ArrayList<double[]> pDists = _posDists.get(query);

			bids.add(bid);
			pDists.add(posDist);

			_bids.put(query, bids);
			_posDists.put(query, pDists);

			if(_weighted && _perQuery) {
				double[] weights = new double[_bidBundles.size()*posLen];
				/*
				 * For our WLS we weight the points by $m^{t-t_i}$ where 
				 * $0 < m < 1$ and $t - t_i$ is the difference between the
				 * day we are predicting and the day we observed the data
				 */
				for(int i = 0; i < _bidBundles.size()*posLen; i++) {
					weights[i] = Math.pow(_m,_bidBundles.size() + 2 - (i/posLen));
				}
				weightVecs.put(query, weights);
			}
		}

		if(_weighted && !_perQuery) {
			for(int i = 0; i < weightVec.length; i++) {
				weightVec[i] = Math.pow(_m ,_bidBundles.size() + 2 - (i/(16*posLen)));
			}
		}

		if(_bidBundles.size() > 6) {
			if(_perQuery) {
				for(Query query : _querySpace) {
					ArrayList<Double> bidsArr = _bids.get(query);
					ArrayList<double[]> pDistsArr = _posDists.get(query);
					int len = bidsArr.size() * pDistsArr.get(0).length;
					double[] bids = new double[len];
					double[] pos = new double[len];
					double[] posDist = new double[len];
					int idx = 0;
					for(int i = 0; i < bidsArr.size(); i++) {
						double bid = bidsArr.get(i);
						double[] pDists = pDistsArr.get(i);
						for(int j = 0; j < pDists.length; j++) {
							bids[idx] = bid;
							pos[idx] = j+1;
							posDist[idx] = pDists[j];
							idx++;
						}
					}
					try {
						_rConnection.assign("bid", bids);
						_rConnection.assign("pos", pos);
						_rConnection.assign("posDist", posDist);
						String model = "model = lm(";
						if(_degree == 1) {
							model += "posDist ~ pos + bid";
						}
						else if(_degree == 2) {
							model += "posDist ~ pos + I(pos^2) + bid + I(bid * pos) + I(bid^2)";
						}
						else if(_degree == 3) {
							model += "posDist ~ pos + I(pos^2) + I(pos^3) + bid + I(bid * pos) + I(bid * pos^2) + I(bid^2) + I(bid^2 * pos) + I(bid^3)";
						}
						else if(_degree == 4) {
							model += "posDist ~ pos + I(pos^2) + I(pos^3) + I(pos^4) + bid + I(bid * pos) + I(bid * pos^2) + I(bid * pos^3) + I(bid^2) + I(bid^2 * pos) + I(bid^2 * pos^2) + I(bid^3) + I(bid^3 * pos) + I(bid^4)";
						}
						else if(_degree == 5) {
							model += "posDist ~ pos + I(pos^2) + I(pos^3) + I(pos^4) + I(pos^5) + bid + I(bid * pos) + I(bid * pos^2) + I(bid * pos^3) + I(bid * pos^4) + I(bid^2) + I(bid^2 * pos) + I(bid^2 * pos^2) + I(bid^2 * pos^3) + I(bid^3) + I(bid^3 * pos) + I(bid^3 * pos^2) + I(bid^4) + I(bid^4 * pos) + I(bid^5)";
						}

						if(_weighted) {
							double[] weights = weightVecs.get(query);
							_rConnection.assign("regWeights", weights);
							model += ", weights = regWeights";
						}

						model += ")";
						_rConnection.voidEval(model);
						double[] coeff = _rConnection.eval("coefficients(model)").asDoubles();
						_coefficients.put(query, coeff);
						for(int i = 0; i < coeff.length; i++) {
							if(Double.isNaN(coeff[i])) {
								_coefficients.put(query, null);
							}
						}
					} catch (REngineException e) {
						_coefficients.put(query, null);
						//						e.printStackTrace();
					} catch (REXPMismatchException e) {
						_coefficients.put(query, null);
						//						e.printStackTrace();
					}
					
					if(_coefficients.get(query) != null) {
						if(!monotonicCheck(query)) {
							_coefficients.put(query, null);
						}
					}
				}
				return true;
			}
			else {
				int len = _querySpace.size() * _bidBundles.size()*posLen;
				double[] bids = new double[len];
				double[] pos = new double[len];
				double[] posDist = new double[len];
				int idx = 0;
				for(int i = 0; i < _bidBundles.size(); i++) {
					for(Query query : _querySpace) {
						ArrayList<Double> bidsArr = _bids.get(query);
						ArrayList<double[]> pDistsArr = _posDists.get(query);
						double bid = bidsArr.get(i);
						double[] pDists = pDistsArr.get(i);
						for(int j = 0; j < pDists.length; j++) {
							bids[idx] = bid;
							pos[idx] = j+1;
							posDist[idx] = pDists[j];
							idx++;
						}
					}
				}
				try {
					_rConnection.assign("bid", bids);
					_rConnection.assign("pos", pos);
					_rConnection.assign("posDist", posDist);
					String model = "model = lm(";
					if(_degree == 1) {
						model += "posDist ~ pos + bid";
					}
					else if(_degree == 2) {
						model += "posDist ~ pos + I(pos^2) + bid + I(bid * pos) + I(bid^2)";
					}
					else if(_degree == 3) {
						model += "posDist ~ pos + I(pos^2) + I(pos^3) + bid + I(bid * pos) + I(bid * pos^2) + I(bid^2) + I(bid^2 * pos) + I(bid^3)";
					}
					else if(_degree == 4) {
						model += "posDist ~ pos + I(pos^2) + I(pos^3) + I(pos^4) + bid + I(bid * pos) + I(bid * pos^2) + I(bid * pos^3) + I(bid^2) + I(bid^2 * pos) + I(bid^2 * pos^2) + I(bid^3) + I(bid^3 * pos) + I(bid^4)";
					}
					else if(_degree == 5) {
						model += "posDist ~ pos + I(pos^2) + I(pos^3) + I(pos^4) + I(pos^5) + bid + I(bid * pos) + I(bid * pos^2) + I(bid * pos^3) + I(bid * pos^4) + I(bid^2) + I(bid^2 * pos) + I(bid^2 * pos^2) + I(bid^2 * pos^3) + I(bid^3) + I(bid^3 * pos) + I(bid^3 * pos^2) + I(bid^4) + I(bid^4 * pos) + I(bid^5)";
					}

					if(_weighted) {
						_rConnection.assign("regWeights", weightVec);
						model += ", weights = regWeights";
					}

					model += ")";
					_rConnection.voidEval(model);
					double[] coeff = _rConnection.eval("coefficients(model)").asDoubles();

					for(int i = 0; i < coeff.length; i++) {
						if(Double.isNaN(coeff[i])) {
							for(Query query : _querySpace) {
								_coefficients.put(query, null);
							}
							return false;
						}
					}

					for(Query query : _querySpace) {
						_coefficients.put(query, coeff);
					}
					
					Query query = _querySpace.iterator().next();
					if(_coefficients.get(query) != null) {
						if(!monotonicCheck(query)) {
							for(Query q : _querySpace) {
								_coefficients.put(q, null);
							}
						}
					}
					
					
					return true;
				} catch (REngineException e) {
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					//					e.printStackTrace();
					return false;
				} catch (REXPMismatchException e) {
					for(Query query : _querySpace) {
						_coefficients.put(query, null);
					}
					//					e.printStackTrace();
					return false;
				}
			}
		}
		else {
			return false;
		}
	}
	
	private boolean monotonicCheck(Query query) {
		double lastPos = 6.0;
		for(double bid = 0; bid < 3.0; bid += .1) {
			double pos = getPrediction(query, bid);
			if(!(pos <= lastPos)) {
				return false;
			}
			lastPos = pos;
		}
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new SpatialBidToPos(_rConnection, _querySpace, _avgPosToDistModel, _perQuery, _degree,_numPrevDays,_weighted, _m);
	}

	@Override
	public String toString() {
		return "BidToPosDist( perQuery: " + _perQuery + ", degree: " + _degree + ", numPrevDays: " + _numPrevDays + ", weighted: " + _weighted + ", m: " + _m;
	}

	@Override
	public void setNumPromSlots(int numPromSlots) {
		_avgPosToDistModel.setNumPromSlots(numPromSlots);
	}

}


/*
 * 
 * 
 * I used the following two python scripts to generate the different surface strings, will put in java later:
 * 
 * 
 *
degree = 5
model = 'pos ~ '
for x in range(0,degree+1):
	for y in range(0,degree+1):
		cdeg = x + y
		if(cdeg < degree + 1):
			if(x > 0):
				if(x == 1):
					model += "bid"
				if(x > 1):
					model += "bid^" + str(x)
				if(y > 0):
					model += " * "
			if(y > 0):
				if(y == 1):
					model += "posDist"
				if(y > 1):
					model += "posDist^" + str(y)
			if(x > 0 or y > 0):
				model += " + "

model = model[:-3]
model
 * 
 *
 * and
 * 
 * 
 * 
degree = 5
model = 'prediction = '
coeff = 0;
for x in range(0,degree+1):
	for y in range(0,degree+1):
		cdeg = x + y
		if(cdeg < degree + 1):
			if(x == 0 and y == 0):
				model += "coeff[" + str(coeff) + "] + "
				coeff += 1
			if(x > 0):
				model += "coeff[" + str(coeff) + "] * "
				coeff += 1
				if(x == 1):
					model += "bid"
				if(x > 1):
					model += "bid * " * x
					model = model[:-3]
				if(y > 0):
					model += " * "
			if(y > 0):
				if(x == 0):
					model += "coeff[" + str(coeff) + "] * "
					coeff += 1
				if(y == 1):
					model += "posDist"
				if(y > 1):
					model += "posDist * " * y
					model = model[:-3]
			if(x > 0 or y > 0):
				model += " + "

model = model[:-3] + ";"
model
 */