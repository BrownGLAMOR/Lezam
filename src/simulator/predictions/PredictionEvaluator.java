package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.avgpostoposdist.AvgPosToPosDist;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.ConstantBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoposdist.AbstractBidToPosDistModel;
import newmodels.bidtoposdist.BidToPosDist;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.EnsembleBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.postoprclick.AbstractPosToPrClick;
import newmodels.postoprclick.BasicPosToPrClick;
import newmodels.postoprclick.RegressionPosToPrClick;
import newmodels.targeting.BasicTargetModel;


import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PredictionEvaluator {

	private boolean _ignoreNan = false;
	private double _outOfAuction = 6.0;

	public ArrayList<String> getGameStrings() {
		//		String baseFile = "/Users/jordanberg/Desktop/mckpgames/localhost_sim";
		//		String baseFile = "/home/jberg/mckpgames/localhost_sim";
		//		int min = 454;
		//		int max = 455;
		//		int max = 496;

		String baseFile = "/Users/jordanberg/Desktop/games/game-";
		int min = 1;
		int max = 2;
		//		int max = 9;
		ArrayList<String> filenames = new ArrayList<String>();
		//		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void posToClickPrPredictionChallenge(AbstractPosToPrClick baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractPosToPrClick model = (AbstractPosToPrClick) baseModel.getCopy();
				model.setSpecialty(advInfo.getManufacturerSpecialty(),advInfo.getComponentSpecialty());

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

				//				System.out.println(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					model.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 5) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double error = model.getPrediction(q, otherQueryReport.getPosition(q), otherBidBundle.getAd(q));
								double clicks = otherQueryReport.getClicks(q);
								double imps = otherQueryReport.getImpressions(q);
								double clickPr = 0;
								if(!(clicks == 0 || imps == 0)) {
									clickPr = clicks/imps;
								}
								else {
									if(_ignoreNan ) {
										continue;
									}
								}
								error -= clickPr;
								error = error*error;
								ourTotActual += clickPr;
								ourTotError += error;
								ourTotErrorCounter++;
							}
						}
						model.updatePredictions(otherQueryReport);
					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		double avgRMSE = 0.0;
		int RMSECounter = 0;
		int dataPointCounter = 0;
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				dataPointCounter += totErrorCounter;
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		System.out.println(baseModel + ", " + (avgRMSE/RMSECounter));
	}

	public void bidToClickPrPredictionChallenge(AbstractBidToPrClick baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractBidToPrClick model = (AbstractBidToPrClick) baseModel.getCopy();
				model.setSpecialty(advInfo.getManufacturerSpecialty(),advInfo.getComponentSpecialty());

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

				//				System.out.println(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					model.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 5) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double error = model.getPrediction(q, otherBidBundle.getBid(q), otherBidBundle.getAd(q));
								double clicks = otherQueryReport.getClicks(q);
								double imps = otherQueryReport.getImpressions(q);
								double clickPr = 0;
								if(!(clicks == 0 || imps == 0)) {
									clickPr = clicks/imps;
								}
								else {
									if(_ignoreNan ) {
										continue;
									}
								}
								error -= clickPr;
								error = error*error;
								ourTotActual += clickPr;
								ourTotError += error;
								ourTotErrorCounter++;
							}
						}
						model.updatePredictions(otherBidBundle);
					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		double avgRMSE = 0.0;
		int RMSECounter = 0;
		int dataPointCounter = 0;
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				dataPointCounter += totErrorCounter;
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		System.out.println(baseModel + ", " + (avgRMSE/RMSECounter));
	}

	public void bidToCPCPredictionChallenge(AbstractBidToCPC baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractBidToCPC model = (AbstractBidToCPC) baseModel.getCopy();

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

				//				System.out.println(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					model.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 5) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double error = model.getPrediction(q, otherBidBundle.getBid(q));
								double CPC = otherQueryReport.getCPC(q);
								if(Double.isNaN(CPC)) {
									if(_ignoreNan ) {
										continue;
									}
									CPC = 0.0;
								}
								error -= CPC;
								error = error*error;
								ourTotActual += CPC;
								ourTotError += error;
								ourTotErrorCounter++;
							}
						}
					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		double avgRMSE = 0.0;
		int RMSECounter = 0;
		int dataPointCounter = 0;
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				dataPointCounter += totErrorCounter;
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		System.out.println(baseModel + ", " + (avgRMSE/RMSECounter));
	}

	public void posToCPCPredictionChallenge(AbstractBidToCPC baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractBidToCPC model = (AbstractBidToCPC) baseModel.getCopy();

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

				//				System.out.println(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					model.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 5) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double pos = otherQueryReport.getPosition(q);
							if(Double.isNaN(pos)) {
								pos = _outOfAuction;
							}
							double error = model.getPrediction(q, pos);
							double CPC = otherQueryReport.getCPC(q);
							if(Double.isNaN(CPC)) {
								if(_ignoreNan ) {
									continue;
								}
								CPC = 0.0;
							}
							error -= CPC;
							error = error*error;
							ourTotActual += CPC;
							ourTotError += error;
							ourTotErrorCounter++;
						}
					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		double avgRMSE = 0.0;
		int RMSECounter = 0;
		int dataPointCounter = 0;
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				dataPointCounter += totErrorCounter;
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		System.out.println(baseModel + ", " + (avgRMSE/RMSECounter));
	}

	public void bidToPosDistPredictionChallenge(AbstractBidToPosDistModel baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,HashMap<String,Double>> ourTotErrorMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Double>> ourTotActualMegaMap = new HashMap<String,HashMap<String,Double>>();
		HashMap<String,HashMap<String,Integer>> ourTotErrorCounterMegaMap = new HashMap<String,HashMap<String,Integer>>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			int numPromSlots = status.getSlotInfo().getPromotedSlots();
			BasicPosToPrClick posToPrClickModel = new BasicPosToPrClick(numPromSlots);
			AvgPosToPosDist avgPosToDistModel = new AvgPosToPosDist(40, numPromSlots, posToPrClickModel);

			/*
			 * One map for each advertiser
			 */
			HashMap<String,Double> ourTotErrorMap = new HashMap<String, Double>();
			HashMap<String,Double> ourTotActualMap = new HashMap<String, Double>();
			HashMap<String,Integer> ourTotErrorCounterMap = new HashMap<String, Integer>();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			for(int agent = 0; agent < agents.length; agent++) {
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractBidToPosDistModel model = (AbstractBidToPosDistModel) baseModel.getCopy();

				double ourTotError = 0;
				double ourTotActual = 0;
				int ourTotErrorCounter = 0;

				HashMap<String, LinkedList<SalesReport>> allSalesReports = status.getSalesReports();
				HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();
				HashMap<String, LinkedList<BidBundle>> allBidBundles = status.getBidBundles();

				LinkedList<SalesReport> ourSalesReports = allSalesReports.get(agents[agent]);
				LinkedList<QueryReport> ourQueryReports = allQueryReports.get(agents[agent]);
				LinkedList<BidBundle> ourBidBundles = allBidBundles.get(agents[agent]);

				//				System.out.println(agents[agent]);
				for(int i = 0; i < 57; i++) {
					SalesReport salesReport = ourSalesReports.get(i);
					QueryReport queryReport = ourQueryReports.get(i);
					BidBundle bidBundle = ourBidBundles.get(i);

					HashMap<Query,double[]> posDists = new HashMap<Query, double[]>();
					for(Query query : querySpace) {
						double[] posDist = avgPosToDistModel.getPrediction(query, queryReport.getRegularImpressions(query), queryReport.getPromotedImpressions(query), queryReport.getPosition(query), queryReport.getClicks(query));
						posDists.put(query, posDist);
					}

					model.updateModel(queryReport, salesReport, bidBundle, posDists);
					if(i >= 5) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double[] posDist = model.getPrediction(q, otherBidBundle.getBid(q));
								double avgPos = 0.0;
								double posTot = 0.0;
								for(int j = 0; j < posDist.length-1; j++) {
									avgPos += posDist[j] * (j+1);
									posTot += posDist[j];
								}
								if(posTot == 0) {
									avgPos = _outOfAuction;
								}
								else {
									avgPos /= posTot;
								}
								double pos = otherQueryReport.getPosition(q);
								if(Double.isNaN(pos)) {
									if(_ignoreNan ) {
										continue;
									}
									pos = _outOfAuction;
								}
								double error = avgPos-pos;
								if(pos == _outOfAuction && avgPos > 5.0) {
									error = 0.0;
								}
								error = error*error;
								ourTotActual += pos;
								ourTotError += error;
								ourTotErrorCounter++;
							}
						}
					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		double avgRMSE = 0.0;
		int RMSECounter = 0;
		int dataPointCounter = 0;
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				//				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				dataPointCounter += totErrorCounter;
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		System.out.println(baseModel + ", " + (avgRMSE/RMSECounter));
	}

	public static boolean intToBin(int x) {
		if(x == 0) {
			return false;
		}
		else if(x == 1) {
			return true;
		}
		else {
			throw new RuntimeException("intToBin can only be called with 0 or 1");
		}
	}

	public static void main(String[] args) throws RserveException {
		PredictionEvaluator evaluator = new PredictionEvaluator();
		Set<Query> _querySpace = new LinkedHashSet<Query>();
		_querySpace.add(new Query(null, null));
		_querySpace.add(new Query("lioneer", null));
		_querySpace.add(new Query(null, "tv"));
		_querySpace.add(new Query("lioneer", "tv"));
		_querySpace.add(new Query(null, "audio"));
		_querySpace.add(new Query("lioneer", "audio"));
		_querySpace.add(new Query(null, "dvd"));
		_querySpace.add(new Query("lioneer", "dvd"));
		_querySpace.add(new Query("pg", null));
		_querySpace.add(new Query("pg", "tv"));
		_querySpace.add(new Query("pg", "audio"));
		_querySpace.add(new Query("pg", "dvd"));
		_querySpace.add(new Query("flat", null));
		_querySpace.add(new Query("flat", "tv"));
		_querySpace.add(new Query("flat", "audio"));
		_querySpace.add(new Query("flat", "dvd"));

		AbstractBidToPrClick model;
		//		AbstractBidToCPC model;
		try {
			double start = System.currentTimeMillis();
			//			model = new RegressionBidToPrClick(new RConnection(),_querySpace,false,2,20,new BasicTargetModel("flat", "tv"),true,false,false,false,false);
			//			model = new EnsembleBidToPrClick(_querySpace, 30, 3, new BasicTargetModel("flat", "tv"), false, true);
			//			evaluator.clickPrPredictionChallenge(model);

			//			for(int perQuery = 0; perQuery < 2; perQuery++) {
			//				for(int IDVar = 1; IDVar < 6; IDVar++) {
			//					for(int numPrevDays = 10; numPrevDays <= 60; numPrevDays += 10) {
			//						for(int weighted = 0; weighted < 2; weighted++) {
			//							for(int robust = 0; robust < 1; robust++) {
			//								for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//									for(int queryTypeIndicators = 0; queryTypeIndicators < 2; queryTypeIndicators++) {
			//										for(int powers = 0; powers < 2; powers++) {
			//											if(!(IDVar == 2) && !(robust == 1 && (queryIndicators == 1 || queryTypeIndicators == 1 || powers == 1)) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//													&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1))) {
			//												model = new RegressionBidToPrClick(new RConnection(), _querySpace, intToBin(perQuery), IDVar, numPrevDays, new BasicTargetModel(null, null), intToBin(weighted), intToBin(robust), intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers));
			//												evaluator.clickPrPredictionChallenge(model);
			//											}
			//										}
			//									}
			//								}
			//							}
			//						}
			//					}
			//				}
			//			}

			//			model = new RegressionBidToCPC(new RConnection(),_querySpace,true,2,30,false,false,false,false,false,false,false);
			//			model = new ConstantBidToCPC(0.1);
			//			evaluator.CPCPredictionChallenge(model);

			//			ArrayList<AbstractPosToPrClick> modelList = new ArrayList<AbstractPosToPrClick>();
			//			RConnection rConnection = new RConnection();
			//			BasicTargetModel _targModel = new BasicTargetModel(null, null);
			//			modelList.add(new BasicPosToPrClick(0));
			//			modelList.add(new BasicPosToPrClick(1));
			//			modelList.add(new BasicPosToPrClick(2));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 25, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 20, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 30, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 35, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 15, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 40, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 10, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 10, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 60, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 55, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 45, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 50, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 45, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 40, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 35, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 30, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 5, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 25, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 15, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 1, 50, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 30, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 35, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 40, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 25, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, true, 1, 20, _targModel, true, false, false, false, false));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 45, _targModel, false, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 35, _targModel, true, false, true, false, true));
			//			modelList.add(new RegressionPosToPrClick(rConnection, _querySpace, false, 3, 40, _targModel, true, false, true, false, true));

			//			for(AbstractPosToPrClick tempModel : modelList) {
			//				evaluator.posToClickPrPredictionChallenge(tempModel);
			//			}

			ArrayList<AbstractBidToPosDistModel> modelList = new ArrayList<AbstractBidToPosDistModel>();
			RConnection rConnection = new RConnection();
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .85));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .75));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .65));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .55));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .45));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 40, true, .35));
			modelList.add(new BidToPosDist(rConnection, _querySpace, false, 2, 20, true, .25));
			modelList.add(new BidToPosDist(rConnection, _querySpace, false, 2, 20, true, .15));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .85));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .75));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .65));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .55));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .45));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .35));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .25));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, true, .15));
			modelList.add(new BidToPosDist(rConnection, _querySpace, false, 2, 20, false, 1.0));
			modelList.add(new BidToPosDist(rConnection, _querySpace, true, 2, 20, false, 1.0));

			for(AbstractBidToPosDistModel tempModel : modelList) {
				evaluator.bidToPosDistPredictionChallenge(tempModel);
			}

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			System.out.println("This took " + (elapsed / 1000) + " seconds");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}