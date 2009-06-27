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
	private double sumVar;
	private int nancounter;

	public ModelTestAgent() {
		CPCPredictions = new LinkedList<HashMap<Query,Double>>();
		sumVar = 0.0;
		nancounter = 0;	
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		if(_day >= 10) {
			BidBundle tempBundle = _bidBundles.get(_bidBundles.size()-2);
			HashMap<Query, Double> cpcpredictions = CPCPredictions.get(CPCPredictions.size()-2);
			QueryReport queryReport = _queryReports.getLast();


			System.out.println("This is day: " + _day);
			for(Query query : _querySpace) {
				System.out.println("Bid: " + tempBundle.getBid(query) + "  CPC: " + queryReport.getCPC(query) + "  CPC predict: " + cpcpredictions.get(query) + "Diff: " + (queryReport.getCPC(query) - cpcpredictions.get(query)));

				if (Double.isNaN(queryReport.getCPC(query)) && _day > 15){
					nancounter++;
				}else if (_day > 15){
					sumVar += (queryReport.getCPC(query) - cpcpredictions.get(query))*(queryReport.getCPC(query) - cpcpredictions.get(query));
				}
			}
		}

		if (_day > 15){
			double stddev = Math.sqrt(sumVar/((_day - 15)*16 - nancounter));
			System.out.println("Standard Deviation: " + stddev);
		}
		
		BidBundle bundle = new BidBundle();
		HashMap<Query,Double> dailyCPCPredictions = new HashMap<Query, Double>();
		for(Query query : _querySpace) {
			double bid = randDouble(.25,1.5);
			bundle.addQuery(query, bid, new Ad());
			if(_day >= 10) {
				dailyCPCPredictions.put(query, _bidToCPC.getPrediction(query, bid, _bidBundles.getLast()));
			}
		}
		CPCPredictions.add(dailyCPCPredictions);
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
		_bidToCPC.updateModel(queryReport, _bidBundles.get(_bidBundles.size()-2));
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
