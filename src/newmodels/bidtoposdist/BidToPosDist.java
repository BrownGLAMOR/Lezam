package newmodels.bidtoposdist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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
	private ArrayList<QueryReport> _queryReports;
	private ArrayList<BidBundle> _bidBundles;
	private HashMap<Query, REXP> _surfaces;

	public BidToPosDist(RConnection rConnection, Set<Query> querySpace) {
		_rConnection = rConnection;
		try {
			_rConnection.voidEval("library(spatial)");
		} catch (RserveException e) {
			throw new RuntimeException("Could not load the R spatial library");
		}
		_querySpace = querySpace;
		_surfaces = new HashMap<Query,REXP>();
		_bids = new HashMap<Query,ArrayList<Double>>();
		_posDists = new HashMap<Query,ArrayList<double[]>>();
		_queryReports = new ArrayList<QueryReport>();
		_bidBundles = new ArrayList<BidBundle>();
		for(Query query : _querySpace) {
			ArrayList<Double> bids = new ArrayList<Double>();
			ArrayList<double[]> posDists = new ArrayList<double[]>();
			_bids.put(query, bids);
			_posDists.put(query, posDists);
			_surfaces.put(query, null);
		}
	}

	@Override
	public double[] getPrediction(Query query, double bid) {
		double predictions[] = new double[5];
		for(int i = 0; i < predictions.length; i++) {
			predictions[i] = 0.0;
		}
		REXP surface = _surfaces.get(query);
		try {
			_rConnection.assign("surface", surface);
			for(int i = 0; i < predictions.length; i++) {
				double prediction = _rConnection.eval("predict.trls(surface," + bid + "," + (i+1) + ")").asDouble();
				predictions[i] = prediction;
			}
			predictions = normalizeArr(predictions);
			return predictions;
		} catch (RserveException e) {
			e.printStackTrace();
			return null;
		} catch (REXPMismatchException e) {
			e.printStackTrace();
			return null;
		}
	}

	private double[] normalizeArr(double[] predictions) {
		double total = 0.0;
		for(int i = 0 ; i < predictions.length; i++) {
			total += predictions[i];
		}
		if(total == 1.0) {
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
					for(int j = 0; j < bidsArr.size(); j++) {
						bids[i*bidsArr.size() + j] = bid;
						pos[i*bidsArr.size() + j] = j+1;
						posDist[i*bidsArr.size() + j] = pDists[j];
					}
				}
				try {
					_rConnection.assign("bids", bids);
					_rConnection.assign("pos", pos);
					_rConnection.assign("posDist", posDist);
					REXP surface = _rConnection.eval("surf.ls(3,bids,pos,posDists");
					_surfaces.put(query, surface);

				} catch (REngineException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
}
