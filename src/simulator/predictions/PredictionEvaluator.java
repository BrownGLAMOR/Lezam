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

import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.ConstantBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
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

	public ArrayList<String> getGameStrings() {
		String baseFile = "/Users/jordanberg/Desktop/mckpgames/localhost_sim";
		//		String baseFile = "/home/jberg/mckpgames/localhost_sim";
		int min = 454;
		int max = 460;
		//		int max = 496;
		ArrayList<String> filenames = new ArrayList<String>();
		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void clickPrPredictionChallenge(AbstractBidToPrClick baseModel) throws IOException, ParseException {
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
								if(agentName.equals(agents[agent])) {
									LinkedList<SalesReport> salesReports = allSalesReports.get(agentName);
									LinkedList<QueryReport> queryReports = allQueryReports.get(agentName);
									LinkedList<BidBundle> bidBundles = allBidBundles.get(agentName);
									SalesReport otherSalesReport = salesReports.get(i+2);
									QueryReport otherQueryReport = queryReports.get(i+2);
									BidBundle otherBidBundle = bidBundles.get(i+2);
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
											error -= clickPr;
											error = error*error;
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
		for(String file : filenames) {
			System.out.println("File: " + file);
			HashMap<String, Double> totErrorMap = ourTotErrorMegaMap.get(file);
			HashMap<String, Double> totActualMap = ourTotActualMegaMap.get(file);
			HashMap<String, Integer> totErrorCounterMap = ourTotErrorCounterMegaMap.get(file);
			for(String agent : totErrorCounterMap.keySet()) {
				System.out.println("\t Agent: " + agent);
				double totError = totErrorMap.get(agent);
				double totActual = totActualMap.get(agent);
				double totErrorCounter = totErrorCounterMap.get(agent);
				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		System.out.println("Average RMSE: " + (avgRMSE/RMSECounter));
	}

	public void CPCPredictionChallenge(AbstractBidToCPC baseModel) throws IOException, ParseException {
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
								if(agentName.equals(agents[agent])) {
									LinkedList<SalesReport> salesReports = allSalesReports.get(agentName);
									LinkedList<QueryReport> queryReports = allQueryReports.get(agentName);
									LinkedList<BidBundle> bidBundles = allBidBundles.get(agentName);
									SalesReport otherSalesReport = salesReports.get(i+2);
									QueryReport otherQueryReport = queryReports.get(i+2);
									BidBundle otherBidBundle = bidBundles.get(i+2);
									for(Query q : querySpace) {
										double bid = otherBidBundle.getBid(q);
										if(bid != 0) {
											double error = model.getPrediction(q, otherBidBundle.getBid(q));
											double cpc = otherQueryReport.getCPC(q);
											if(!Double.isNaN(cpc)) {
												error -= cpc;
											}
											error = error*error;
											ourTotActual += cpc;
											ourTotError += error;
											ourTotErrorCounter++;
										}
									}
								}
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
		System.out.println("Model: " + baseModel);
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				avgRMSE += RMSE;
				//				System.out.println("\t\t RMSE: " + RMSE);
				RMSECounter++;
			}
		}
		System.out.println("Data Points: " + RMSECounter);
		System.out.println("Average RMSE: " + (avgRMSE/RMSECounter));
	}

	public static void main(String[] args) {
		PredictionEvaluator evaluator = new PredictionEvaluator();
		Set<Query> _querySpace = new LinkedHashSet<Query>();
		_querySpace.add(new Query(null, null));
		_querySpace.add(new Query("flat", null));
		_querySpace.add(new Query("flat", "tv"));
		_querySpace.add(new Query("flat", "dvd"));
		_querySpace.add(new Query("flat", "audio"));
		_querySpace.add(new Query("pg", null));
		_querySpace.add(new Query("pg", "tv"));
		_querySpace.add(new Query("pg", "dvd"));
		_querySpace.add(new Query("pg", "audio"));
		_querySpace.add(new Query("lioneer", null));
		_querySpace.add(new Query("lioneer", "tv"));
		_querySpace.add(new Query("lioneer", "dvd"));
		_querySpace.add(new Query("lioneer", "audio"));
		_querySpace.add(new Query(null, "tv"));
		_querySpace.add(new Query(null, "dvd"));
		_querySpace.add(new Query(null, "audio"));

		//		AbstractBidToPrClick model;
		AbstractBidToCPC model;
		try {
			double start = System.currentTimeMillis();
			//			model = new RegressionBidToPrClick(new RConnection(),_querySpace,false, 2,20,new BasicTargetModel("flat", "tv"),true,false,false,false,false);
			//			evaluator.clickPrPredictionChallenge(model);

			model = new RegressionBidToCPC(new RConnection(),_querySpace,true,2,30,false,false,false,false,false,false,true);
			//			model = new ConstantBidToCPC(0.1);
			evaluator.CPCPredictionChallenge(model);

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			System.out.println("This took " + (elapsed / 1000) + " seconds");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}