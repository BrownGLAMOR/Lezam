package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.AvgPosToPos;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoposdist.BidToPosDist;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BidPosModelTestAgent extends SimAbstractAgent {

	private AvgPosToPos _avgPosToPosDist;
	private BidToPosDist _bidToPosDist;
	Random _R = new Random();					//Random number generator
	private ArrayList<HashMap<Query, Double>> avgPosPredictions;
	private double sumVar;

	public BidPosModelTestAgent() {
		avgPosPredictions = new ArrayList<HashMap<Query,Double>>();
		sumVar = 0.0;
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bundle = new BidBundle();
		HashMap<Query,Double> dailyPosPredictions = new HashMap<Query, Double>();
		for(Query query : _querySpace) {
			double bid = randDouble(.25,1.5);
			bundle.addQuery(query, bid, new Ad());
			if(_day >= 10) {
				dailyPosPredictions.put(query, getAvg(_bidToPosDist.getPrediction(query, bid)));
			}
		}
		if(_day >= 12) {
			BidBundle tempBundle = _bidBundles.get(_bidBundles.size()-2);
			HashMap<Query, Double> pospredictions = avgPosPredictions.get(avgPosPredictions.size()-2);
			QueryReport queryReport = _queryReports.getLast();
			for(Query query : _querySpace) {
				sumVar += (pospredictions.get(query) - queryReport.getPosition(query))*(pospredictions.get(query) - queryReport.getPosition(query));
			}
			double stddev = Math.sqrt(sumVar/((_day - 12)*16));
			System.out.println("Standard Deviation: " + stddev);
		}
		
		avgPosPredictions.add(dailyPosPredictions);
		return bundle;
	}

	private Double getAvg(double[] prediction) {
		double avg = 0.0;
		double total = 0.0;
		for(int i = 0; i < prediction.length; i++) {
			avg += prediction[i] * (i+1);
			total += prediction[i];
		}
		return avg/total;
	}


	@Override
	public void initBidder() {
	}

	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		_avgPosToPosDist = new AvgPosToPos(80);
		try {
			_bidToPosDist = new BidToPosDist(new RConnection(), _querySpace);
		} catch (RserveException e) {
			e.printStackTrace();
		}
		models.add(_avgPosToPosDist);
		models.add(_bidToPosDist);
		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		HashMap<Query,double[]> posDists = new HashMap<Query, double[]>();
		for(Query query : _querySpace) {
			int regImps = queryReport.getRegularImpressions(query);
			int promImps = queryReport.getPromotedImpressions(query);
			double avgPos = queryReport.getPosition(query);
			int numClicks = queryReport.getClicks(query);
			double[] posDist;
			if(Double.isNaN(avgPos)) {
				posDist = new double[5];
				for(int i = 0; i < 5; i++) {
					posDist[i] = 0;
				}
			}
			else {
				posDist = _avgPosToPosDist.getPrediction(query, regImps, promImps, avgPos, numClicks, _numPS);
			}
			posDists.put(query, normalizeArr(posDist));
		}
		_bidToPosDist.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2),posDists);
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
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

}
