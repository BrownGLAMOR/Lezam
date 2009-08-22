package newmodels.bidtoposdist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtoslot.AbstractBidToSlotModel;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BidToPosDist extends AbstractBidToPosDistModel {

	private RConnection _rConnection;
	private Set<Query> _querySpace;
	private HashMap<Query, ArrayList<Double>> _bids;
	private HashMap<Query, ArrayList<double[]>> _posDists;
	private HashMap<Query, double[]> _coefficients;
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private int _degree;

	public BidToPosDist(RConnection rConnection, Set<Query> querySpace, int degree) {
		_rConnection = rConnection;
		_querySpace = querySpace;
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
		_degree = degree;
	}

	@Override
	public double[] getPrediction(Query query, double bid) {
		double[] coeff = _coefficients.get(query);
		if(coeff == null) {
			return null;
		}
		double predictions[] = new double[5];
		for(int i = 0; i < predictions.length; i++) {
			int posDist = i+1;
			double prediction = 0.0;
			if(_degree == 1) {
				prediction = coeff[0] + coeff[1] * posDist + coeff[2] * bid;
			}
			else if(_degree == 2) {
				prediction = coeff[0] + coeff[1] * posDist + coeff[2] * posDist * posDist + coeff[3] * bid + coeff[4] * bid * posDist + coeff[5] * bid * bid;
			}
			else if(_degree == 3) {
				prediction = coeff[0] + coeff[1] * posDist + coeff[2] * posDist * posDist + coeff[3] * posDist * posDist * posDist + coeff[4] * bid + coeff[5] * bid * posDist + coeff[6] * bid * posDist * posDist + coeff[7] * bid * bid + coeff[8] * bid * bid * posDist + coeff[9] * bid * bid * bid;
			}
			else if(_degree == 4) {
				prediction = coeff[0] + coeff[1] * posDist + coeff[2] * posDist * posDist + coeff[3] * posDist * posDist * posDist + coeff[4] * posDist * posDist * posDist * posDist + coeff[5] * bid + coeff[6] * bid * posDist + coeff[7] * bid * posDist * posDist + coeff[8] * bid * posDist * posDist * posDist + coeff[9] * bid * bid + coeff[10] * bid * bid * posDist + coeff[11] * bid * bid * posDist * posDist + coeff[12] * bid * bid * bid + coeff[13] * bid * bid * bid * posDist + coeff[14] * bid * bid * bid * bid;
			}
			else if(_degree == 5) {
				prediction = coeff[0] + coeff[1] * posDist + coeff[2] * posDist * posDist + coeff[3] * posDist * posDist * posDist + coeff[4] * posDist * posDist * posDist * posDist + coeff[5] * posDist * posDist * posDist * posDist * posDist + coeff[6] * bid + coeff[7] * bid * posDist + coeff[8] * bid * posDist * posDist + coeff[9] * bid * posDist * posDist * posDist + coeff[10] * bid * posDist * posDist * posDist * posDist + coeff[11] * bid * bid + coeff[12] * bid * bid * posDist + coeff[13] * bid * bid * posDist * posDist + coeff[14] * bid * bid * posDist * posDist * posDist + coeff[15] * bid * bid * bid + coeff[16] * bid * bid * bid * posDist + coeff[17] * bid * bid * bid * posDist * posDist + coeff[18] * bid * bid * bid * bid + coeff[19] * bid * bid * bid * bid * posDist + coeff[20] * bid * bid * bid * bid * bid;
			}
			//				System.out.println("Pred: " + prediction);
			predictions[i] = prediction;
		}
		return predictions;
	}

	private double[] normalizeArr(double[] predictions) {
		double total = 0.0;
		for(int i = 0 ; i < predictions.length; i++) {
			total += predictions[i];
		}
		if(total == 1.0 || total == 0.0) {
			return predictions;
		}
		else {
			double[] newpredictions = new double[predictions.length];
			for(int i = 0 ; i < predictions.length; i++) {
				newpredictions[i] = predictions[i]/total;
			}
			return newpredictions;
		}
	}

	@Override
	/*
	 * All of these reports should correspond to the same auction.
	 * 
	 * posDists must be normalized first!
	 */
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport,
			BidBundle bidBundle, HashMap<Query,double[]> posDists) {

		_queryReports.add(queryReport);
		_bidBundles.add(bidBundle);

		if(_bidBundles.size() != _queryReports.size()) {
			throw new RuntimeException("Uneven number of bidbundles and query reports");
		}

		for(Query query : _querySpace) {
			double bid = bidBundle.getBid(query);
			double[] posDist = posDists.get(query);

			ArrayList<Double> bids = _bids.get(query);
			ArrayList<double[]> pDists = _posDists.get(query);

			bids.add(bid);
			pDists.add(posDist);

			_bids.put(query, bids);
			_posDists.put(query, pDists);
		}

		if(_bids.get(new Query(null,null)).size() > 3) {
			for(Query query : _querySpace) {
				ArrayList<Double> bidsArr = _bids.get(query);
				ArrayList<double[]> pDistsArr = _posDists.get(query);
				int len = bidsArr.size() * pDistsArr.get(0).length;
				double[] bids = new double[len];
				double[] pos = new double[len];
				double[] posDist = new double[len];
				for(int i = 0; i < bidsArr.size(); i++) {
					double bid = bidsArr.get(i);
					double[] pDists = pDistsArr.get(i);
					for(int j = 0; j < pDists.length; j++) {
						int index = pDists.length*i + j;
						bids[index] = bid;
						pos[index] = j+1;
						posDist[index] = pDists[j];
					}
				}
				try {
					_rConnection.assign("bids", bids);
					_rConnection.assign("pos", pos);
					_rConnection.assign("posDist", posDist);
					String model = "lm(";
					if(_degree == 1) {
						model += "pos ~ posDist + bid";
					}
					else if(_degree == 2) {
						model += "pos ~ posDist + posDist^2 + bid + bid * posDist + bid^2";
					}
					else if(_degree == 3) {
						model += "pos ~ posDist + posDist^2 + posDist^3 + bid + bid * posDist + bid * posDist^2 + bid^2 + bid^2 * posDist + bid^3";
					}
					else if(_degree == 4) {
						model += "pos ~ posDist + posDist^2 + posDist^3 + posDist^4 + bid + bid * posDist + bid * posDist^2 + bid * posDist^3 + bid^2 + bid^2 * posDist + bid^2 * posDist^2 + bid^3 + bid^3 * posDist + bid^4";
					}
					else if(_degree == 5) {
						model += "pos ~ posDist + posDist^2 + posDist^3 + posDist^4 + posDist^5 + bid + bid * posDist + bid * posDist^2 + bid * posDist^3 + bid * posDist^4 + bid^2 + bid^2 * posDist + bid^2 * posDist^2 + bid^2 * posDist^3 + bid^3 + bid^3 * posDist + bid^3 * posDist^2 + bid^4 + bid^4 * posDist + bid^5";
					}
					
					model += ")";
					_rConnection.voidEval(model);
					double[] coeff = _rConnection.eval("coefficients(model)").asDoubles();
					_coefficients.put(query, coeff);
				} catch (REngineException e) {
					_coefficients.put(query, null);
					e.printStackTrace();
				} catch (REXPMismatchException e) {
					_coefficients.put(query, null);
					e.printStackTrace();
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public AbstractModel getCopy() {
		return new BidToPosDist(_rConnection, _querySpace, _degree);
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