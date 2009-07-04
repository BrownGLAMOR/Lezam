package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoprclick.RegressionBidToPrClick;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class ClickPrModelTestAgent extends SimAbstractAgent {

	private RegressionBidToPrClick _bidToClickPr;
	Random _R = new Random();					//Random number generator
	private LinkedList<HashMap<Query, Double>> clickPrPredictions;
	private double sumVar;
	private int nancounter;

	public ClickPrModelTestAgent() {
		clickPrPredictions = new LinkedList<HashMap<Query,Double>>();
		sumVar = 0.0;
		nancounter = 0;	
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		if(_day >= 15) {
			BidBundle tempBundle = _bidBundles.get(_bidBundles.size()-2);
			HashMap<Query, Double> clickPrpredictions = clickPrPredictions.get(clickPrPredictions.size()-2);
			QueryReport queryReport = _queryReports.getLast();


			System.out.println("This is day: " + _day);
			for(Query query : _querySpace) {
				double clicks = queryReport.getClicks(query);
				double imps = queryReport.getImpressions(query);
				System.out.println("Bid: " + tempBundle.getBid(query) + "  click pr: " + (clicks/imps) + "  click pr predict: " + clickPrpredictions.get(query) + "Diff: " + (clicks/imps - clickPrpredictions.get(query)));

				if ((clicks == 0 || imps == 0) && _day > 15){
					nancounter++;
				}else if (_day > 15){
					sumVar += (clicks/imps - clickPrpredictions.get(query))*(clicks/imps- clickPrpredictions.get(query));
				}
			}
			double stddev = Math.sqrt(sumVar/((_day - 15)*16 - nancounter));
			System.out.println("Standard Deviation: " + stddev);
		}

		BidBundle bundle = new BidBundle();
		HashMap<Query,Double> dailyClickPrredictions = new HashMap<Query, Double>();
		for(Query query : _querySpace) {
			double bid = randDouble(.25,1.5);
			bundle.addQuery(query, bid, new Ad());
			if(_day >= 10) {
				dailyClickPrredictions.put(query, _bidToClickPr.getPrediction(query, bid, new Ad()));
			}
		}
		clickPrPredictions.add(dailyClickPrredictions);
		return bundle;
	}

	@Override
	public void initBidder() {
	}

	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		_bidToClickPr = new RegressionBidToPrClick(_querySpace);
		models.add(_bidToClickPr);
		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		_bidToClickPr.updateModel(queryReport, _bidBundles.get(_bidBundles.size()-2));
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
