package models.postobid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.bidtopos.AbstractBidToPos;

public class BidToPosInverter extends AbstractPosToBid {

	private RConnection _rConnection;
	private Set<Query> _querySpace;
	private HashMap<Query, double[]> _coefficients;
	private double _increment;
	private double _min;
	private double _max;
	private AbstractBidToPos _model;

	public BidToPosInverter(RConnection rConnection, Set<Query> querySpace, AbstractBidToPos model, double increment, double min, double max) {
		_rConnection = rConnection;
		_querySpace = querySpace;
		_model = model;
		_increment = increment;
		_min = min;
		_max = max;


		_coefficients = new HashMap<Query, double[]>();
		for(Query query : _querySpace) {
			_coefficients.put(query, null);
		}
	}

	public void setIncrements(double increment, double min, double max) {
		_increment = increment;
		_min = min;
		_max = max;
	}

	public double[] getIncrementedArray() {
		return getIncrementedArray(_increment,_min,_max);
	}

	public double[] getIncrementedArray(double increment, double min, double max) {
		double diff = max-min;
		double len = Math.ceil(diff/increment)  + 1;
		if(len > 100) {
			throw new RuntimeException("More than 100 points is probably overkill for the inversion");
		}
		double[] incrementedArr = new double[(int) len];
		for(int i = 0; i < len; i++) {
			incrementedArr[i] = min + i*increment;
		}
		return incrementedArr;
	}

	public double getPrediction(Query query, double pos) {
		double[] coeff = _coefficients.get(query);
		if(coeff == null) {
			return Double.NaN;
		}

		double bid = coeff[0] + pos * coeff[1];
		
		if(bid < _min) {
			return _min;
		}

		if(bid > _max) {
			return _max;
		}

		return bid;
	}

	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		for(Query query : _querySpace) {
			double[] bids = getIncrementedArray();
			double[] positions = new double[bids.length];
			for(int i = 0; i < bids.length; i++) {
				positions[i] = _model.getPrediction(query, bids[i]);
			}
			try {
				_rConnection.assign("bids", bids);
				_rConnection.assign("positions", positions);
				String Rmodel = "model = lm(bids ~ positions)";
				_rConnection.voidEval(Rmodel);
				double[] coefficients = _rConnection.eval("coefficients(model)").asDoubles();
				_coefficients.put(query, coefficients);
				for(int i = 0; i < coefficients.length; i++) {
					if(Double.isNaN(coefficients[i])) {
						_coefficients.put(query, null);
					}
				}
			} catch (REngineException e) {
//				e.printStackTrace();
			} catch (REXPMismatchException e) {
//				e.printStackTrace();
			}
		}
		return true;
	}

	public String toString() {
		return "BidToPosInverter(model: " + _model + ",increment: "  + _increment + ", min: " + _min + ", max: " + _max + " )";
	}

	@Override
	public AbstractModel getCopy() {
		return new BidToPosInverter(_rConnection, _querySpace, _model, _increment, _min, _max);
	}

}
