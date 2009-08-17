package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import newmodels.bidtoprclick.AbstractBidToPrClick;


import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PredictionEvaluator {


	private static LinkedHashSet<Query> _querySpace;

	public ArrayList<String> getGameStrings() {
		//		String baseFile = "/Users/jordan/Desktop/mckpgames/localhost_sim";
		String baseFile = "/Users/jordan/Desktop/JavaWorkSpaces/aa-server-0.9.6.1/logs/sims/localhost_sim";
		int min = 40;
		int max = 54;
		ArrayList<String> filenames = new ArrayList<String>();
		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void clickPrPredictionChallenge(AbstractBidToPrClick baseModel, double increment, double min, double max) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> totErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> totActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> totErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		HashMap<String,HashMap<String,Double>> fullRangeTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> fullRangeTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> fullRangeTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> totErrorMap = new HashMap<String,Double>();
			HashMap<String,Double> totActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> totErrorCounterMap = new HashMap<String, Integer>();
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();
			HashMap<String,Double> fullRangeTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> fullRangeTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> fullRangeTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			_querySpace = new LinkedHashSet<Query>();
			_querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				_querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				_querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				_querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				
				AbstractBidToPrClick model = (AbstractBidToPrClick) baseModel.getCopy();

				double totError = 0;
				double totActual = 0;
				int totErrorCounter = 0;
				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;
				double fullRangeTotError = 0;
				double fullRangeTotActual = 0;
				int fullRangeTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					model.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 5) {
						if(i >= 7) {
							/*
							 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
							 */
							for(int j = 0; j < agents.length; j++) {
								String agentName = agents[j];
								LinkedList<SalesReport> salesReports = allSalesReports.get(agentName);
								LinkedList<QueryReport> queryReports = allQueryReports.get(agentName);
								LinkedList<BidBundle> bidBundles = allBidBundles.get(agentName);
								SalesReport otherSalesReport = salesReports.get(i+2);
								QueryReport otherQueryReport = queryReports.get(i+2);
								BidBundle otherBidBundle = bidBundles.get(i+2);
								for(Query q : _querySpace) {
									double bid = otherBidBundle.getBid(q);
									if(bid != 0) {
										double error = model.getPrediction(q, otherBidBundle.getBid(q), new Ad());
										double clicks = otherQueryReport.getClicks(q);
										double imps = otherQueryReport.getImpressions(q);
										double clickPr = 0;
										if(!(clicks == 0 || imps == 0)) {
											clickPr = clicks/imps;
										}
										totActual += clickPr;
										error -= clickPr;
										error = error*error;
										totError += error;
										totErrorCounter++;
										if(agentName.equals(agents[agent])) {
											ourTotActual += clickPr;
											ourTotError += error;
											ourTotErrorCounter++;
										}
									}
								}
							}
						}
					}
				}
				totErrorMap.put(agents[agent],totError);
				totActualMap.put(agents[agent],totActual);
				totErrorCounterMap.put(agents[agent],totErrorCounter);
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);
				fullRangeTotErrorMap.put(agents[agent],fullRangeTotError);
				fullRangeTotActualMap.put(agents[agent],fullRangeTotActual);
				fullRangeTotErrorCounterMap.put(agents[agent],fullRangeTotErrorCounter);
				
			}
			
			totErrorMegaMap.put(filename,totErrorMap);
			totActualMegaMap.put(filename,totActualMap);
			totErrorCounterMegaMap.put(filename,totErrorCounterMap);
			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
			fullRangeTotErrorMegaMap.put(filename,fullRangeTotErrorMap);
			fullRangeTotActualMegaMap.put(filename,fullRangeTotActualMap);
			fullRangeTotErrorCounterMegaMap.put(filename,fullRangeTotErrorCounterMap);
		}
	}
}