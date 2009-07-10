package newmodels;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import newmodels.bidtocpc.EnsembleBidToCPC;

import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class CPCEnsembleTest {


	private static LinkedHashSet<Query> _querySpace;

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
//		String baseFile = "/Users/jordan/Desktop/mckpgames/localhost_sim";
		String baseFile = "/Users/jordan/Desktop/JavaWorkSpaces/aa-server-0.9.6.1/logs/sims/localhost_sim";
		int min = 40;
		int max = 52;
		String[] filenames = new String[max-min];
		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames[i-min] = baseFile + i + ".slg";
		}
		double totError = 0;
		double totActual = 0;
		int totErrorCounter = 0;
		double ourTotError = 0;
		double ourTotActual = 0;
		int ourTotErrorCounter = 0;
		
		int numPastDays = 5;
		int ensembleSize = 8;
		HashMap<Query,HashMap<String,Integer>> ensembleMembers = null;
		for(int fileIdx = 0; fileIdx < filenames.length; fileIdx++) {
			String filename = filenames[fileIdx];
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

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

			/*
			 * Initialize Ensemble
			 */
			EnsembleBidToCPC bidToCPC = new EnsembleBidToCPC(_querySpace,numPastDays,ensembleSize,ensembleMembers);
			bidToCPC.initializeEnsemble();

			HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
			HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
			HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

			LinkedList<SalesReport> ourSalesReports = allSalesReports.get("mckpmkii");
			LinkedList<QueryReport> ourQueryReports = allQueryReports.get("mckpmkii");
			LinkedList<BidBundle> ourBidBundles = allBidBundles.get("mckpmkii");
			for(int i = 0; i < 57; i++) {
				SalesReport salesReport = ourSalesReports.get(i);
				QueryReport queryReport = ourQueryReports.get(i);
				BidBundle bidBundle = ourBidBundles.get(i);

				bidToCPC.updateModel(queryReport, salesReport, bidBundle);
				if(i >= 5) {
					bidToCPC.updatePredictions(ourBidBundles.get(i+2));
					if(i >= 7) {
						bidToCPC.updateError(queryReport, bidBundle);
						bidToCPC.createEnsemble();

						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						for(int j = 0; j < agents.length; j++) {
							String agent = agents[j];
							LinkedList<SalesReport> salesReports = allSalesReports.get(agent);
							LinkedList<QueryReport> queryReports = allQueryReports.get(agent);
							LinkedList<BidBundle> bidBundles = allBidBundles.get(agent);
							SalesReport otherSalesReport = salesReports.get(i+2);
							QueryReport otherQueryReport = queryReports.get(i+2);
							BidBundle otherBidBundle = bidBundles.get(i+2);
							for(Query q : _querySpace) {
								double bid = otherBidBundle.getBid(q);
								if(bid != 0) {
									double error = bidToCPC.getPrediction(q, otherBidBundle.getBid(q));
									double CPC = otherQueryReport.getCPC(q);
									if(Double.isNaN(CPC)) {
										CPC = 0;
									}
									totActual += CPC;
									error -= CPC;
									error = error*error;
									totError += error;
									totErrorCounter++;
									if(agent.equals("mckpmkii")) {
										ourTotActual += CPC;
										ourTotError += error;
										ourTotErrorCounter++;
									}
								}
							}
						}
					}
				}
			}
			ensembleMembers = bidToCPC.getEnsembleMembers();
			if(fileIdx + 1 == filenames.length) { 
				bidToCPC.printEnsembleMemberSummary(max-min);
			}
		}
		System.out.println("Num Prev Days: " + numPastDays);
		System.out.println("Ensemble Size: " + ensembleSize);
		System.out.println("TOTAL ERROR");
		double avgCPC = totActual/totErrorCounter;
		double MSE = totError/totErrorCounter;
		double RMSE = Math.sqrt(MSE);
		System.out.println("Over " + totErrorCounter + " predictions");
		System.out.println("Root Mean Square Error: "  + RMSE);
		System.out.println("Avg CPC: "  + avgCPC);
		System.out.println("% Error: " + (RMSE/avgCPC));

		System.out.println("MCKP ERROR:");
		double ourAvgCPC = ourTotActual/ourTotErrorCounter;
		double ourMSE = ourTotError/ourTotErrorCounter;
		double ourRMSE = Math.sqrt(ourMSE);
		System.out.println("Over " + ourTotErrorCounter + " predictions");
		System.out.println("Root Mean Square Error: "  + ourRMSE);
		System.out.println("Avg CPC: "  + ourAvgCPC);
		System.out.println("% Error: " + (ourRMSE/ourAvgCPC));

	}
}