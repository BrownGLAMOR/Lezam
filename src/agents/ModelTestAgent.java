package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class ModelTestAgent extends SimAbstractAgent {

	private RegressionBidToCPC _bidToCPC;
	Random _R = new Random();					//Random number generator
	private LinkedList<HashMap<Query, Double>> CPCPredictions;

	public ModelTestAgent() {
		CPCPredictions = new LinkedList<HashMap<Query,Double>>();
	}
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bundle = new BidBundle();
		HashMap<Query,Double> dailyCPCPredictions = new HashMap<Query, Double>();
		for(Query query : _querySpace) {
			double bid = randDouble(.25,3.5);
			bundle.addQuery(query, bid, new Ad());
			try {
				dailyCPCPredictions.put(query, _bidToCPC.getPrediction(query, bid, _bidBundles.getLast()));
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (REngineException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return bundle;
	}

	@Override
	public void initBidder() {
	}

	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		try {
			_bidToCPC = new RegressionBidToCPC(_querySpace);
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		models.add(_bidToCPC);
		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		try {
			_bidToCPC.updateModel(queryReport, _bidBundles.get(_bidBundles.size()-2));
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}
	
}
