package newmodels;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedList;

import newmodels.bidtocpc.EnsembleBidToCPC;

import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class EnsembleTest {


	private static Set<Query> _querySpace;

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
//		String baseFile = "/Users/jordan/Desktop/mckpgames/localhost_sim";
		String baseFile = "/u/jberg/Desktop/mckpgames/localhost_sim";
		int min = 454;
		int max = 455;
		String[] filenames = new String[max-min];
		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames[i-min] = baseFile + i + ".slg";
		}
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
			EnsembleBidToCPC bidToCPC = new EnsembleBidToCPC(_querySpace);
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
						bidToCPC.updateError(ourQueryReports.get(i), ourBidBundles.get(i));
						bidToCPC.createEnsemble();
					}
				}
			}
			bidToCPC.printEnsembleMemberSummary();
		}
	}
}