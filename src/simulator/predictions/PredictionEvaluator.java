package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import models.AbstractModel;
import models.avgpostoposdist.AvgPosToPosDist;
import models.bidtocpc.AbstractBidToCPC;
import models.bidtocpc.ConstantBidToCPC;
import models.bidtocpc.EnsembleBidToCPC;
import models.bidtocpc.KitchenSinkEnsembleBidToCPC;
import models.bidtocpc.RegressionBidToCPC;
import models.bidtocpc.WEKABidToCPC;
import models.bidtocpc.WEKAClassifBidToCPC;
import models.bidtocpc.WEKAEnsembleBidToCPC;
import models.bidtopos.AbstractBidToPos;
import models.bidtopos.EnsembleBidToPos;
import models.bidtopos.RegressionBidToPos;
import models.bidtopos.SpatialBidToPos;
import models.bidtoprclick.AbstractBidToPrClick;
import models.bidtoprclick.EnsembleBidToPrClick;
import models.bidtoprclick.KitchenSinkEnsembleBidToPrClick;
import models.bidtoprclick.RegressionBidToPrClick;
import models.bidtoprclick.WEKABidToPrClick;
import models.bidtoprclick.WEKAClassifBidToPrClick;
import models.bidtoprclick.WEKAEnsembleBidToPrClick;
import models.cpctobid.AbstractCPCToBid;
import models.cpctobid.BidToCPCInverter;
import models.postobid.AbstractPosToBid;
import models.postobid.BidToPosInverter;
import models.postocpc.AbstractPosToCPC;
import models.postocpc.EnsemblePosToCPC;
import models.postocpc.KitchenSinkEnsemblePosToCPC;
import models.postocpc.RegressionPosToCPC;
import models.postocpc.WEKAClassifPosToCPC;
import models.postocpc.WEKAEnsemblePosToCPC;
import models.postocpc.WEKAPosToCPC;
import models.postoprclick.AbstractPosToPrClick;
import models.postoprclick.BasicPosToPrClick;
import models.postoprclick.EnsemblePosToPrClick;
import models.postoprclick.KitchenSinkEnsemblePosToPrClick;
import models.postoprclick.RegressionPosToPrClick;
import models.postoprclick.WEKAClassifPosToPrClick;
import models.postoprclick.WEKAEnsemblePosToPrClick;
import models.postoprclick.WEKAPosToPrClick;
import models.prclicktobid.AbstractPrClickToBid;
import models.prclicktobid.BidToPrClickInverter;
import models.prconv.AbstractConversionModel;
import models.prconv.GoodConversionPrModel;
import models.prconv.HistoricPrConversionModel;
import models.prconv.WEKAPrConv;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.BasicQueryToNumImp;
import models.querytousermodel.BasicQueryToUserModel;
import models.targeting.BasicTargetModel;
import models.usermodel.BasicUserModel;
import models.usermodel.HistoricalDailyAverageUserModel;


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

		//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		int min = 1425;
		//		int max = 1430;

		String baseFile = "/Users/jordanberg/Desktop/LATESTMCKP/localhost_sim";
		int min = 2;
		int max = 3;

		//						int max = 9;

		//				String baseFile = "/pro/aa/finals/day-2/server-1/game";
		//				int min = 1435;
		//				int max = 1445;
		//		int max = 1464;

		ArrayList<String> filenames = new ArrayList<String>();
		//		System.out.println("Min: " + min + "  Max: " + max);
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void queryToPrConv(AbstractConversionModel baseModel) throws IOException, ParseException {
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
				AbstractConversionModel model = (AbstractConversionModel) baseModel.getCopy();
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
					model.setTimeHorizon(5);
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
								double error = model.getPrediction(q);
								if(Double.isNaN(error)) {
									error = 0.0;
								}
								double clicks = otherQueryReport.getClicks(q);
								double conversions = otherSalesReport.getConversions(q);
								double convPr = 0;
								if(!(clicks == 0 || conversions == 0)) {
									convPr = conversions/clicks;
								}
								else {
									continue;
								}
								error -= convPr;
								error = error*error;
								ourTotActual += convPr;
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void queryToNumImpPredictionChallenge(AbstractQueryToNumImp baseModel) throws IOException, ParseException {
		/*
		 * All these maps they are like this: <fileName<agentName,error>>
		 */
		HashMap<String,Double> ourTotErrorMegaMap = new HashMap<String,Double>();
		HashMap<String,Double> ourTotActualMegaMap = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterMegaMap = new HashMap<String,Integer>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

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

			AbstractQueryToNumImp model = (AbstractQueryToNumImp) baseModel.getCopy();

			double ourTotError = 0;
			double ourTotActual = 0;
			int ourTotErrorCounter = 0;

			HashMap<String, LinkedList<QueryReport>> allQueryReports = status.getQueryReports();

			//				System.out.println(agents[agent]);
			for(int i = 0; i < 57; i++) {
				for(Query q : querySpace) {
					double imps = 0.0;
					for(int agent = 0; agent < agents.length; agent++) {
						LinkedList<QueryReport> queryReportsList = allQueryReports.get(agents[agent]);
						QueryReport queryReport = queryReportsList.get(i+2);
						imps = Math.max(imps, queryReport.getImpressions(q));
					}
					if(imps == 0.0) {
						/*
						 * We only care what the error is on days we actually got in the auction
						 */
						continue;
					}
					double imppred = model.getPrediction(q,i+1);
					if(Double.isNaN(imppred)) {
						imppred = 0.0;
					}
					imppred -= imps;
					imppred = imppred*imppred;
					ourTotActual += imps;
					ourTotError += imppred;
					ourTotErrorCounter++;
				}
			}

			ourTotErrorMegaMap.put(filename,ourTotError);
			ourTotActualMegaMap.put(filename,ourTotActual);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounter);
		}
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
		//		System.out.println("Model: " + baseModel);
		for(String file : filenames) {
			//			System.out.println("File: " + file);
			double totError = ourTotErrorMegaMap.get(file);
			double totActual = ourTotActualMegaMap.get(file);
			int totErrorCounter = ourTotErrorCounterMegaMap.get(file);
			//				System.out.println("\t Agent: " + agent);
			//				System.out.println("\t\t Predictions: " + totErrorCounter);
			double MSE = (totError/totErrorCounter);
			double RMSE = Math.sqrt(MSE);
			double actual = totActual/totErrorCounter;
			RMSEList.add(RMSE);
			actualList.add(actual);
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}


	public void bidToPosToClickPrPredictionChallenge(AbstractBidToPos bidToPosBaseModel, AbstractPosToPrClick baseModel) throws IOException, ParseException {
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
				AbstractBidToPos bidToPosModel = (AbstractBidToPos) bidToPosBaseModel.getCopy();
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
					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double pos = bidToPosModel.getPrediction(q, bid);
								double error = model.getPrediction(q, pos, otherBidBundle.getAd(q));
								if(Double.isNaN(error)) {
									error = 0.0;
								}
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void posToClickPrPredictionChallenge(AbstractPosToPrClick baseModel) throws IOException, ParseException {
		double start = System.currentTimeMillis();

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
								if(Double.isNaN(error)) {
									error = 0.0;
								}
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
					}
					//					if(i == 56) {
					//						((EnsemblePosToPrClick)model).printErrDiffAndBorda();
					//					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + elapsed);
		//		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void bidToClickPrPredictionChallenge(AbstractBidToPrClick baseModel) throws IOException, ParseException {
		double start = System.currentTimeMillis();

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
								if(Double.isNaN(error)) {
									error = 0.0;
								}
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
					}
					//					if(i == 56) {
					//						((EnsembleBidToPrClick)model).printErrDiffAndBorda();
					//					}
				}
				ourTotErrorMap.put(agents[agent],ourTotError);
				ourTotActualMap.put(agents[agent],ourTotActual);
				ourTotErrorCounterMap.put(agents[agent],ourTotErrorCounter);

			}

			ourTotErrorMegaMap.put(filename,ourTotErrorMap);
			ourTotActualMegaMap.put(filename,ourTotActualMap);
			ourTotErrorCounterMegaMap.put(filename,ourTotErrorCounterMap);
		}
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + elapsed);
		//		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void bidToCPCPredictionChallenge(AbstractBidToCPC baseModel) throws IOException, ParseException {
		double start = System.currentTimeMillis();
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
								if(Double.isNaN(error)) {
									error = bid;
									continue;
								}
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + elapsed);
		//		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void posToClickPrToBidPredictionChallenge(AbstractBidToPrClick bidToPrClickBaseModel, AbstractPosToPrClick baseModel) throws IOException, ParseException {
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
				AbstractBidToPrClick bidToClickPrModel = (AbstractBidToPrClick) bidToPrClickBaseModel.getCopy();
				AbstractPosToPrClick posToClickPrModel = (AbstractPosToPrClick) baseModel.getCopy();
				AbstractPrClickToBid clickPrToBid = null;
				//TODO
				try {
					clickPrToBid = new BidToPrClickInverter(new RConnection(), querySpace, bidToClickPrModel, .05, 0, 3.0);
				} catch (RserveException e) {
					e.printStackTrace();
				}

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

					bidToClickPrModel.updateModel(queryReport, salesReport, bidBundle);
					posToClickPrModel.updateModel(queryReport, salesReport, bidBundle);
					clickPrToBid.updateModel(queryReport, salesReport, bidBundle);

					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double pos = otherQueryReport.getPosition(q);
							double clickPr = posToClickPrModel.getPrediction(q, pos, new Ad());
							double error = clickPrToBid.getPrediction(q, clickPr);
							if(Double.isNaN(error)) {
								error = 0.0;
							}
							double bid = otherBidBundle.getBid(q);
							//							System.out.println("Pos: " + pos + "  CPC: " + CPC + "  BidEst: " + error + "   bid: " + bid);
							if(bid != 0) {
								error -= bid;
								error = error*error;
								ourTotActual += bid;
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
		//		System.out.println("Model: " + baseModel);
		int dataPointCounter = 0;
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
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void posToCPCToBidPredictionChallenge(AbstractBidToCPC bidToCPCBaseModel, AbstractPosToCPC baseModel) throws IOException, ParseException {
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
				AbstractBidToCPC bidToCPCModel = (AbstractBidToCPC) bidToCPCBaseModel.getCopy();
				AbstractPosToCPC posToCPCModel = (AbstractPosToCPC) baseModel.getCopy();
				AbstractCPCToBid CPCToBid = null;
				//TODO
				try {
					CPCToBid = new BidToCPCInverter(new RConnection(), querySpace, bidToCPCModel, .05, 0, 3.0);
				} catch (RserveException e) {
					e.printStackTrace();
				}

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

					bidToCPCModel.updateModel(queryReport, salesReport, bidBundle);
					posToCPCModel.updateModel(queryReport, salesReport, bidBundle);
					CPCToBid.updateModel(queryReport, salesReport, bidBundle);

					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double pos = otherQueryReport.getPosition(q);
							double CPC = posToCPCModel.getPrediction(q, pos);
							double error = CPCToBid.getPrediction(q, CPC);
							if(Double.isNaN(error)) {
								error = 0.0;
							}
							double bid = otherBidBundle.getBid(q);
							//							System.out.println("Pos: " + pos + "  CPC: " + CPC + "  BidEst: " + error + "   bid: " + bid);
							if(bid != 0) {
								error -= bid;
								error = error*error;
								ourTotActual += bid;
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
		//		System.out.println("Model: " + baseModel);
		int dataPointCounter = 0;
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
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}


	public void posToBidToCPCPredictionChallenge(AbstractBidToPos bidToPosBaseModel, AbstractBidToCPC baseModel) throws IOException, ParseException {
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
				AbstractBidToPos bidToPosModel = (AbstractBidToPos) bidToPosBaseModel.getCopy();
				AbstractBidToCPC model = (AbstractBidToCPC) baseModel.getCopy();
				AbstractPosToBid posToBid = null;
				try {
					posToBid = new BidToPosInverter(new RConnection(), querySpace, bidToPosModel, .05, 0, 3.0);
				} catch (RserveException e) {
					e.printStackTrace();
				}

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

					bidToPosModel.updateModel(queryReport, salesReport, bidBundle);
					model.updateModel(queryReport, salesReport, bidBundle);
					posToBid.updateModel(queryReport, salesReport, bidBundle);

					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double pos = otherQueryReport.getPosition(q);
							double bid = posToBid.getPrediction(q, pos);
							if(Double.isNaN(bid)) {
								bid = 0;
							}
							if(bid != 0) {
								double error = model.getPrediction(q, bid);
								if(Double.isNaN(error)) {
									error = bid;
									continue;
								}
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void posToCPCPredictionChallenge(AbstractPosToCPC baseModel) throws IOException, ParseException {

		double start = System.currentTimeMillis();

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
				AbstractPosToCPC model = (AbstractPosToCPC) baseModel.getCopy();

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
					if(i > 5) {
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
							if(Double.isNaN(error)) {
								error = 0.0;
							}
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		double stop = System.currentTimeMillis();
		double elapsed = (stop - start)/1000.0;
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + elapsed);
		//		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void bidToPosPredictionChallenge(AbstractBidToPos baseModel) throws IOException, ParseException {
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

			//			int numPromSlots = status.getSlotInfo().getPromotedSlots();
			//			BasicPosToPrClick posToPrClickModel = new BasicPosToPrClick(numPromSlots);
			//			AvgPosToPosDist avgPosToDistModel = new AvgPosToPosDist(40, numPromSlots, posToPrClickModel);

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
				if("MCKPBid".equals(agents[agent])) {
					continue;
				}
				HashMap<String, AdvertiserInfo> advertiserInfos = status.getAdvertiserInfos();
				AdvertiserInfo advInfo = advertiserInfos.get(agents[agent]);
				AbstractBidToPos model = (AbstractBidToPos) baseModel.getCopy();
				model.setNumPromSlots(status.getSlotInfo().getPromotedSlots());

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
					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double bid = otherBidBundle.getBid(q);
							if(bid != 0) {
								double avgPos = model.getPrediction(q, otherBidBundle.getBid(q));
								if(Double.isNaN(avgPos)) {
									avgPos = _outOfAuction;
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
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
				//				System.out.println("\t\t Predictions: " + totErrorCounter);
				double MSE = (totError/totErrorCounter);
				double RMSE = Math.sqrt(MSE);
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		//		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	public void posToBidPredictionChallenge(AbstractBidToPos baseModel) throws IOException, ParseException {
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

			//			int numPromSlots = status.getSlotInfo().getPromotedSlots();
			//			BasicPosToPrClick posToPrClickModel = new BasicPosToPrClick(numPromSlots);
			//			AvgPosToPosDist avgPosToDistModel = new AvgPosToPosDist(40, numPromSlots, posToPrClickModel);

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
				AbstractBidToPos model = (AbstractBidToPos) baseModel.getCopy();
				model.setNumPromSlots(status.getSlotInfo().getPromotedSlots());

				AbstractPosToBid posToBid = null;
				try {
					posToBid = new BidToPosInverter(new RConnection(), querySpace, model, .05, 0, 3.0);
				} catch (RserveException e) {
					e.printStackTrace();
				}

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
					posToBid.updateModel(queryReport, salesReport, bidBundle);
					if(i >= 6) {
						/*
						 * Make Predictions and Evaluate Error Remember to do this for i + 2 !!!
						 */
						SalesReport otherSalesReport = ourSalesReports.get(i+2);
						QueryReport otherQueryReport = ourQueryReports.get(i+2);
						BidBundle otherBidBundle = ourBidBundles.get(i+2);
						for(Query q : querySpace) {
							double pos = otherQueryReport.getPosition(q);
							double error = posToBid.getPrediction(q, pos);
							if(Double.isNaN(error)) {
								error = 0.0;
							}
							double bid = otherBidBundle.getBid(q);
							//							System.out.println(q + "  Pos: " + pos + "  BidEst: " + error + "  bid: " + bid);
							if(bid != 0) {
								error -= bid;
								error = error*error;
								ourTotActual += bid;
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
		ArrayList<Double> RMSEList = new ArrayList<Double>();
		ArrayList<Double> actualList = new ArrayList<Double>();
		//		System.out.println("Model: " + baseModel);
		int dataPointCounter = 0;
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
				double actual = totActual/totErrorCounter;
				RMSEList.add(RMSE);
				actualList.add(actual);
			}
		}
		System.out.println("Data Points: " + dataPointCounter);
		Collections.sort(RMSEList);
		double percentile5 = 0.0;
		double n = (5/100.0) * (RMSEList.size()-1) + 1;
		int k = (int) Math.floor(n);
		double d = n-k;
		if(n == 1) {
			percentile5 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile5 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile5 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile25 = 0.0;
		n = (25/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile25 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile25 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile25 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile75 = 0.0;
		n = (75/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile75 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile75 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile75 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double percentile95 = 0.0;
		n = (95/100.0) * (RMSEList.size()-1) + 1;
		k = (int) Math.floor(n);
		d = n-k;
		if(n == 1) {
			percentile95 = RMSEList.get(0);
		}
		else if(n == RMSEList.size()) {
			percentile95 = RMSEList.get(RMSEList.size()-1);
		}
		else {
			percentile95 = RMSEList.get(k-1) + d*(RMSEList.get(k)-RMSEList.get(k-1));
		}

		double[] rmseStd = getStdDevAndMean(RMSEList);
		double[] actualStd = getStdDevAndMean(actualList);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStd[1] + ", " + actualStd[0] + ", " + actualStd[1] + ", " + RMSEList.get(0) + ", " + percentile5 + ", " + percentile25 + ", " + percentile75 + ", " + percentile95 + ", " + RMSEList.get(RMSEList.size()-1));
	}

	private double[] getStdDevAndMean(ArrayList<Double> list) {
		double n = list.size();
		double sum = 0.0;
		for(Double data : list) {
			sum += data;
		}
		double mean = sum/n;

		double variance = 0.0;

		for(Double data : list) {
			variance += (data-mean)*(data-mean);
		}

		variance /= (n-1);

		double[] stdDev = new double[2];
		stdDev[0] = mean;
		stdDev[1] = Math.sqrt(variance);
		return stdDev;
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

		try {
			double start = System.currentTimeMillis();
			//			model = new RegressionBidToPrClick(new RConnection(),_querySpace,false,2,20,new BasicTargetModel("flat", "tv"),true,false,false,false,false);
			//			model = new EnsembleBidToPrClick(_querySpace, 30, 3, new BasicTargetModel("flat", "tv"), false, true);
			//			evaluator.clickPrPredictionChallenge(model);
			RConnection _rConnection = new RConnection();
			BasicTargetModel _targModel = new BasicTargetModel(null, null);

			evaluator.queryToPrConv(new GoodConversionPrModel(_querySpace, _targModel));
			evaluator.queryToPrConv(new HistoricPrConversionModel(_querySpace, _targModel));
			for(double weight = 1.0; weight > .89; weight -= .1) {
				for(int i = 1; i < 8; i++) {
					evaluator.queryToPrConv(new WEKAPrConv(i,weight));
				}
			}

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,60, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,60, true,0.915, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,40, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,40, true,0.915, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,60, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.99, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,60, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, false,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,20, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.99, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, false,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,20, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,60, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,20, true,0.99, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,60, true,0.99, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,20, false,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, true,0.99, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.915, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,40, true,0.99, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,60, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, false,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.99, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,40, false,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,60, false,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.99, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.915, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, false,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, true,1,40, false,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,1,60, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,1,40, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,20, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,1,60, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,60, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, true,0.84, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,1,40, true,0.915, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,1,20, true,0.84, false, false, true, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,40, true,0.915, false, false, false, false, false, false));
			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,4,60, true,0.915, false, false, false, false, false, false));
			//			//						evaluator.bidToCPCPredictionChallenge( new RegressionBidToCPC(_rConnection, _querySpace, false,3,20, true,0.915, false, false, false, false, false, false));
			//									for(double weight = 1.0; weight > .89; weight -= .1) {
			//										for(int i = 1; i < 8; i++) {
			//											evaluator.bidToCPCPredictionChallenge(new WEKABidToCPC(i,weight));
			//										}
			//									}
			//						
			//									for(int i = 1; i < 20; i++) {
			//										evaluator.bidToCPCPredictionChallenge(new WEKAClassifBidToCPC(i));
			//									}
			//						evaluator.bidToCPCPredictionChallenge(new WEKAEnsembleBidToCPC(_querySpace,10,12,true,false));
			//						evaluator.bidToCPCPredictionChallenge(new EnsembleBidToCPC(_querySpace,10,20,true,false));
			//						evaluator.bidToCPCPredictionChallenge(new KitchenSinkEnsembleBidToCPC(_querySpace,10,20,true,false));



			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,40, true,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,60, true,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.915, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,60, true,0.915, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,40, true,0.915, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.915, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.915, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.99, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, false,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.99, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,40, true,0.99, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.915, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.915, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.99, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, false,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,60, true,0.99, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,40, false,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.915, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, false,0.84, false, false, true, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, true,1,60, false,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.99, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, false,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.99, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.915, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, false,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.915, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.915, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.99, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, true,0.99, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,20, false,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, false,0.84, false, false, true, false, true, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, true,0.99, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.99, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,40, false,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, false,0.84, false, false, false, false, false, false));
			//						evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false,1,60, true,0.915, false, false, false, false, true, false));
			//						for(double weight = 1.0; weight > .89; weight -= .1) {
			//							for(int i = 1; i < 8; i++) {
			//								evaluator.posToCPCPredictionChallenge(new WEKAPosToCPC(i,weight));
			//							}
			//						}
			//			
			//						for(int i = 1; i < 20; i++) {
			//							evaluator.posToCPCPredictionChallenge(new WEKAClassifPosToCPC(i));
			//						}
			//						evaluator.posToCPCPredictionChallenge(new WEKAEnsemblePosToCPC(_querySpace,10,12,true,false));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,20,true,true));
			//						evaluator.posToCPCPredictionChallenge(new KitchenSinkEnsemblePosToCPC(_querySpace,10,20,true,false));


			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,60, _targModel, true,0.84, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,40, _targModel, true,0.84, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,40, _targModel, true,0.915, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,60, _targModel, true,0.915, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,20, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,20, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,40, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,40, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,3,60, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,60, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,60, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,40, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,40, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,20, _targModel, true,0.915, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,20, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,1,60, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,20, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,40, _targModel, true,0.99, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,40, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,20, _targModel, true,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,40, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,40, _targModel, false,0.84, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,60, _targModel, true,0.99, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,60, _targModel, true,0.99, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,60, _targModel, false,0.84, false, true, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, true,1,60, _targModel, false,0.84, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,40, _targModel, true,0.915, false, false, false, false));
			//						evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false,4,20, _targModel, true,0.99, false, false, false, false));
			//						for(double weight = 1.0; weight > .89; weight -= .1) {
			//							for(int i = 1; i < 8; i++) {
			//								evaluator.bidToClickPrPredictionChallenge(new WEKABidToPrClick(i,weight));
			//							}
			//						}
			//			
			//						for(int i = 1; i < 20; i++) {
			//							evaluator.bidToClickPrPredictionChallenge(new WEKAClassifBidToPrClick(i));
			//						}		
			//						evaluator.bidToClickPrPredictionChallenge(new WEKAEnsembleBidToPrClick(_querySpace,10,12,_targModel, true,false));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,true,true));
			//						evaluator.bidToClickPrPredictionChallenge(new KitchenSinkEnsembleBidToPrClick(_querySpace,10,20,_targModel, true,false));

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,60, _targModel, false,0.84, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,60, _targModel, true,0.915, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,40, _targModel, false,0.84, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,40, _targModel, true,0.915, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,60, _targModel, true,0.84, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.99, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,20, _targModel, false,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, true,1,40, _targModel, true,0.84, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.915, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.99, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.99, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.915, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, false,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, false,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.915, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,20, _targModel, true,0.84, false, true, false, true));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.99, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, false,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.915, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, true,0.99, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, true,0.99, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,40, _targModel, false,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,3,60, _targModel, false,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,60, _targModel, true,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,40, _targModel, true,0.84, false, true, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false,1,20, _targModel, true,0.84, false, true, false, false));
			//			for(double weight = 1.0; weight > .89; weight -= .1) {
			//				for(int i = 1; i < 8; i++) {
			//					evaluator.posToClickPrPredictionChallenge(new WEKAPosToPrClick(i,weight));
			//				}
			//			}
			//
			//			for(int i = 1; i < 20; i++) {
			//				evaluator.posToClickPrPredictionChallenge(new WEKAClassifPosToPrClick(i));
			//			}
			//			evaluator.posToClickPrPredictionChallenge(new WEKAEnsemblePosToPrClick(_querySpace,10,12,_targModel, true,false));
			//			evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,20,_targModel,true,true));
			//			evaluator.posToClickPrPredictionChallenge(new KitchenSinkEnsemblePosToPrClick(_querySpace,10,12,_targModel, true,false));

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


























			//			AbstractQueryToNumImp model = new BasicQueryToNumImp(new HistoricalDailyAverageUserModel());
			//			//			AbstractQueryToNumImp model = new BasicQueryToNumImp(new BasicUserModel());
			//			evaluator.queryToNumImpPredictionChallenge(model);



			/*
			 * Bid-CPC is true,false
			 */

			//			evaluator.bidToCPCPredictionChallenge(new EnsembleBidToCPC(_querySpace,10,20,true,false));
			//						evaluator.bidToCPCPredictionChallenge(new EnsembleBidToCPC(_querySpace,10,30,true,true));
			//						evaluator.bidToCPCPredictionChallenge(new EnsembleBidToCPC(_querySpace,5,20,true,true));
			//						evaluator.bidToCPCPredictionChallenge(new EnsembleBidToCPC(_querySpace,5,30,true,true));


			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,20,true,true));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,30,true,false));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,30,false,true));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,30,false,false));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,10,30,true,true));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,5,20,true,true));
			//						evaluator.posToCPCPredictionChallenge(new EnsemblePosToCPC(_querySpace,5,30,true,true));

			//			evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,true,true));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,true,false));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,false,true));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,false,false));
			//									evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,30,_targModel,true,true));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,5,20,_targModel,true,true));
			//						evaluator.bidToClickPrPredictionChallenge(new EnsembleBidToPrClick(_querySpace,5,30,_targModel,true,true));
			//
			//									evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,20,_targModel,true,true));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,20,_targModel,true,false));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,20,_targModel,false,true));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,20,_targModel,false,false));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,10,30,_targModel,true,true));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,5,20,_targModel,true,true));
			//						evaluator.posToClickPrPredictionChallenge(new EnsemblePosToPrClick(_querySpace,5,30,_targModel,true,true));

			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null, 10, 20,true,true));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null, 10, 20,true,false));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null, 10, 20,false,true));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null, 10, 20,false,false));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null,10,30,true,true));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null,5,20,true,true));
			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace, null,5,30,true,true));

			//						evaluator.bidToPosToClickPrPredictionChallenge(new EnsembleBidToPos(_querySpace, null,10,20,true,true), new EnsemblePosToPrClick(_querySpace,10,20,_targModel,true,true));

			//						evaluator.posToBidToCPCPredictionChallenge(new EnsembleBidToPos(_querySpace, null,10,20,true,true), new EnsembleBidToCPC(_querySpace,10,20,true,true));


			//			evaluator.posToBidPredictionChallenge(new EnsembleBidToPos(_querySpace, null,10,20,true,true));
			//			double stop = System.currentTimeMillis();
			//			double elapsed = stop - start;
			//			System.out.println("This took " + (elapsed / 1000) + " seconds");

			//						evaluator.posToCPCToBidPredictionChallenge(new EnsembleBidToCPC(_querySpace,10,20,true,false), new EnsemblePosToCPC(_querySpace,10,20,true,true));
			//			evaluator.posToClickPrToBidPredictionChallenge(new EnsembleBidToPrClick(_querySpace,10,20,_targModel,true,true), new EnsemblePosToPrClick(_querySpace,10,20,_targModel,true,true));


			//			evaluator.bidToPosPredictionChallenge(new EnsembleBidToPos(_querySpace,null, 10,20,true,true));

			//			evaluator.bidToCPCPredictionChallenge(new RegressionBidToCPC(_rConnection, _querySpace, false, 3, 30, true, .85, false, false, false, false,false,false));
			//			evaluator.posToCPCPredictionChallenge(new RegressionPosToCPC(_rConnection, _querySpace, false, 1, 30, true, .85, false, false, false, false, false, false));
			//			evaluator.bidToClickPrPredictionChallenge(new RegressionBidToPrClick(_rConnection, _querySpace, false, 1, 30, _targModel, true, .85, false, false, false, false));
			//			evaluator.posToClickPrPredictionChallenge(new RegressionPosToPrClick(_rConnection, _querySpace, false, 1, 30, _targModel, true, .85, false, false, false, false));
			//			evaluator.bidToPosPredictionChallenge(new RegressionBidToPos(_rConnection, _querySpace, false, 1, 30, true, .85, false, false, false, false));


			//			evaluator.bidToPosPredictionChallenge(new SpatialBidToPos(_rConnection, _querySpace, new AvgPosToPosDist(40,1,new BasicPosToPrClick(1)), false, 1, 30,true, .85));

			//						/*
			//						 * Test all BID-POS models!
			//						 */
			//						for(int perQuery = 0; perQuery < 2; perQuery++) {
			//							for(int IDVar = 1; IDVar < 5; IDVar++) {
			//								for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//									for(int weighted = 0; weighted < 2; weighted++) {
			//										for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//											for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//												for(int queryTypeIndicators = 0; queryTypeIndicators < 1; queryTypeIndicators++) {
			//													for(int powers = 0; powers < 2; powers++) {
			//														for(int ignoreNaN = 0; ignoreNaN < 1; ignoreNaN++) {
			//															if(!(IDVar == 2) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//																	&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1))
			//																	&& !(intToBin(perQuery) && numPrevDays < 30) && !(!intToBin(weighted) && mWeight > .84)) {
			//																AbstractBidToPos model = new RegressionBidToPos(_rConnection, _querySpace, intToBin(perQuery), IDVar, numPrevDays, intToBin(weighted), mWeight, intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers), intToBin(ignoreNaN));
			//																evaluator.bidToPosPredictionChallenge(model);
			//															}
			//														}
			//													}
			//												}
			//											}
			//										}
			//									}
			//								}
			//							}
			//						}



			//			/*
			//			 * Test all BID-POS SPATIAL models!
			//			 */
			//			for(int IDVar = 1; IDVar < 5; IDVar++) {
			//				for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//					for(int weighted = 0; weighted < 2; weighted++) {
			//						for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//							for(int ignoreNaN = 0; ignoreNaN < 1; ignoreNaN++) {
			//								if(!(!intToBin(weighted) && mWeight > .84)) {
			//									AbstractBidToPos model = new SpatialBidToPos(_rConnection, _querySpace, new AvgPosToPosDist(40,1,new BasicPosToPrClick(1)), intToBin(ignoreNaN), IDVar, numPrevDays, intToBin(weighted), mWeight);
			//									evaluator.bidToPosPredictionChallenge(model);
			//								}
			//							}
			//						}
			//					}
			//				}
			//			}

			//
			//			/*
			//			 * Test all BID-PRCLICK models!
			//			 */
			//			for(int perQuery = 0; perQuery < 2; perQuery++) {
			//				for(int IDVar = 1; IDVar < 5; IDVar++) {
			//					for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//						for(int weighted = 0; weighted < 2; weighted++) {
			//							for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//								for(int robust = 0; robust < 1; robust++) {
			//									for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//										for(int queryTypeIndicators = 0; queryTypeIndicators < 1; queryTypeIndicators++) {
			//											for(int powers = 0; powers < 2; powers++) {
			//												if(!(IDVar == 2) && !(robust == 1 && (queryIndicators == 1 || queryTypeIndicators == 1 || powers == 1)) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//														&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1)) && !(intToBin(perQuery) && numPrevDays < 30) && !(!intToBin(weighted) && mWeight > .84)) {
			//													RegressionBidToPrClick model = new RegressionBidToPrClick(_rConnection, _querySpace, intToBin(perQuery), IDVar, numPrevDays, _targModel, intToBin(weighted), mWeight, intToBin(robust), intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers));
			//													evaluator.bidToClickPrPredictionChallenge(model);
			//												}
			//											}
			//										}
			//									}
			//								}
			//							}
			//						}
			//					}
			//				}
			//			}


			//			/*
			//			 * Test all POS-PRCLICK models!
			//			 */
			//			for(int perQuery = 0; perQuery < 2; perQuery++) {
			//				for(int IDVar = 1; IDVar < 5; IDVar++) {
			//					for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//						for(int weighted = 0; weighted < 2; weighted++) {
			//							for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//								for(int robust = 0; robust < 1; robust++) {
			//									for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//										for(int queryTypeIndicators = 0; queryTypeIndicators < 1; queryTypeIndicators++) {
			//											for(int powers = 0; powers < 2; powers++) {
			//												if(!(IDVar == 2) && !(robust == 1 && (queryIndicators == 1 || queryTypeIndicators == 1 || powers == 1)) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//														&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1)) && !(intToBin(perQuery) && numPrevDays < 30) && !(!intToBin(weighted) && mWeight > .84)) {
			//													RegressionPosToPrClick model = new RegressionPosToPrClick(_rConnection, _querySpace, intToBin(perQuery), IDVar, numPrevDays, _targModel, intToBin(weighted), mWeight, intToBin(robust), intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers));
			//													evaluator.posToClickPrPredictionChallenge(model);
			//												}
			//											}
			//										}
			//									}
			//								}
			//							}
			//						}
			//					}
			//				}
			//			}


			//						/*
			//						 * Test all POS-CPC models!
			//						 */
			//						for(int perQuery = 0; perQuery < 2; perQuery++) {
			//							for(int IDVar = 1; IDVar < 5; IDVar++) {
			//								for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//									for(int weighted = 0; weighted < 2; weighted++) {
			//										for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//											for(int robust = 0; robust < 1; robust++) {
			//												for(int loglinear = 0; loglinear < 1; loglinear++) {
			//													for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//														for(int queryTypeIndicators = 0; queryTypeIndicators < 1; queryTypeIndicators++) {
			//															for(int powers = 0; powers < 2; powers++) {
			//																for(int ignoreNaN = 0; ignoreNaN < 1; ignoreNaN++) {
			//																	if(!(IDVar == 2) && !(robust == 1 && (queryIndicators == 1 || queryTypeIndicators == 1 || powers == 1)) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//																			&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1)) && !(intToBin(perQuery) && numPrevDays < 30) && !(!intToBin(weighted) && mWeight > .84)) {
			//																		RegressionPosToCPC model = new RegressionPosToCPC(_rConnection, _querySpace, intToBin(perQuery), IDVar, numPrevDays, intToBin(weighted), mWeight, intToBin(robust), intToBin(loglinear), intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers), intToBin(ignoreNaN));
			//																		evaluator.posToCPCPredictionChallenge(model);
			//																	}
			//																}
			//															}
			//														}
			//													}
			//												}
			//											}
			//										}
			//									}
			//								}
			//							}
			//						}



			//									/*
			//									 * Test all BID-CPC models!
			//									 */
			//									for(int perQuery = 0; perQuery < 2; perQuery++) {
			//										for(int IDVar = 1; IDVar < 5; IDVar++) {
			//											for(int numPrevDays = 20; numPrevDays <= 60; numPrevDays += 20) {
			//												for(int weighted = 0; weighted < 2; weighted++) {
			//													for(double mWeight = 0.84; mWeight < 1.0; mWeight += .075) {
			//														for(int robust = 0; robust < 1; robust++) {
			//															for(int loglinear = 0; loglinear < 1; loglinear++) {
			//																for(int queryIndicators = 0; queryIndicators < 2; queryIndicators++) {
			//																	for(int queryTypeIndicators = 0; queryTypeIndicators < 1; queryTypeIndicators++) {
			//																		for(int powers = 0; powers < 2; powers++) {
			//																			for(int ignoreNaN = 0; ignoreNaN < 1; ignoreNaN++) {
			//																				if(!(IDVar == 2) && !(robust == 1 && (queryIndicators == 1 || queryTypeIndicators == 1 || powers == 1)) && !(queryIndicators == 1 && queryTypeIndicators == 1)
			//																						&& !(perQuery == 1 && (queryIndicators == 1 || queryTypeIndicators == 1)) && !(intToBin(perQuery) && numPrevDays < 30) && !(!intToBin(weighted) && mWeight > .84)) {
			//																					AbstractBidToCPC model = new RegressionBidToCPC(_rConnection, _querySpace, intToBin(perQuery), IDVar, numPrevDays, intToBin(weighted), mWeight, intToBin(robust), intToBin(loglinear), intToBin(queryIndicators), intToBin(queryTypeIndicators), intToBin(powers), intToBin(ignoreNaN));
			//																					evaluator.bidToCPCPredictionChallenge(model);
			//																				}
			//																			}
			//																		}
			//																	}
			//																}
			//															}
			//														}
			//													}
			//												}
			//											}
			//										}
			//									}

			//			for(double c = 0.0; c <= 1.01; c += .01) {
			//				ConstantBidToCPC model = new ConstantBidToCPC(c);
			//				evaluator.bidToCPCPredictionChallenge(model);
			//				//															System.out.println(model);
			//				double stop = System.currentTimeMillis();
			//				double elapsed = stop - start;
			//				System.out.println("This took " + (elapsed / 1000) + " seconds");
			//				start = System.currentTimeMillis();
			//			}

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