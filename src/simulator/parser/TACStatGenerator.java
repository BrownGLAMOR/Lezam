package simulator.parser;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import simulator.Reports;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class TACStatGenerator {

	public static double[] stdDeviation(Double[] revenueErrorArr) {
		double[] meanAndStdDev = new double[2];
		meanAndStdDev[0] = 0.0;
		meanAndStdDev[1] = 0.0;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[0] += revenueErrorArr[i];
		}
		meanAndStdDev[0] /= revenueErrorArr.length;
		for(int i = 0; i < revenueErrorArr.length; i++) {
			meanAndStdDev[1] +=  (revenueErrorArr[i] - meanAndStdDev[0])*(revenueErrorArr[i] - meanAndStdDev[0]);
		}
		meanAndStdDev[1] /= revenueErrorArr.length;
		meanAndStdDev[1] = Math.sqrt(meanAndStdDev[1]);
		return meanAndStdDev;
	}
	
	public static double[] meanAndVar(ArrayList<Double> revenue) {
		double[] meanAndVar = new double[2];
		meanAndVar[0] = 0.0;
		meanAndVar[1] = 0.0;
		for(int i = 0; i < revenue.size(); i++) {
			meanAndVar[0] += revenue.get(i);
		}
		meanAndVar[0] /= revenue.size();
		
		for(int i = 0; i < revenue.size(); i++) {
			meanAndVar[1] +=  (revenue.get(i) - meanAndVar[0])*(revenue.get(i) - meanAndVar[0]);
		}
		meanAndVar[1] /= revenue.size();
		meanAndVar[1] = Math.sqrt(meanAndVar[1]);
		return meanAndVar;
	}
	
	public static void generateExtendedStats(String filename, int min, int max, int advId) throws IOException, ParseException {
		LinkedHashSet<Query> _querySpace = null;
		HashMap<String,HashMap<String, LinkedList<Reports>>> reportsListMegaMap = new HashMap<String, HashMap<String,LinkedList<Reports>>>();
		HashMap<String,Integer> capacities = new HashMap<String,Integer>();
		HashMap<String,String> compSpecialties = new HashMap<String,String>();
		HashMap<String,String> manSpecialties = new HashMap<String,String>();
		ArrayList<Double> lowVals = new ArrayList<Double>();
		ArrayList<Double> medVals = new ArrayList<Double>();
		ArrayList<Double> highVals = new ArrayList<Double>();
		ArrayList<Double> allVals = new ArrayList<Double>();
		String ourAgent = null;
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			System.out.println(file);
			GameStatusHandler statusHandler = new GameStatusHandler(file);
			GameStatus status = statusHandler.getGameStatus();
			String[] agents = status.getAdvertisers();

			if(ourAgent == null) {
				ourAgent = agents[advId];
				System.out.println(ourAgent);
			}

			if(_querySpace == null) {
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
			}

			HashMap<String,LinkedList<Reports>> reportsListMap = new HashMap<String,LinkedList<Reports>>();

			capacities.put(file,status.getAdvertiserInfos().get(ourAgent).getDistributionCapacity());
			compSpecialties.put(file,status.getAdvertiserInfos().get(ourAgent).getComponentSpecialty());
			manSpecialties.put(file,status.getAdvertiserInfos().get(ourAgent).getManufacturerSpecialty());

			HashMap<String, LinkedList<Reports>> maps = new HashMap<String, LinkedList<Reports>>();
			HashMap<String, LinkedList<QueryReport>> queryReports = status.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = status.getSalesReports();

			for(String agent : queryReports.keySet()) {
				LinkedList<Reports> reports = new LinkedList<Reports>();
				LinkedList<QueryReport> queryReportList = queryReports.get(agent);
				LinkedList<SalesReport> salesReportList = salesReports.get(agent);
				for(int j = 0; j < queryReportList.size(); j++) {
					reports.add(new Reports(queryReportList.get(j), salesReportList.get(j)));
				}
				maps.put(agent, reports);
			}

			for(int j = 0; j < agents.length; j++) {
				reportsListMap.put(agents[j],maps.get(agents[j]));
			}

			reportsListMegaMap.put(file, reportsListMap);
		}

		double totalRevenue = 0;
		double totalCost = 0;
		double totalImp = 0;
		double totalClick = 0;
		double totalConv = 0;
		double totalNoneConv = 0.0;
		double totalCompConv = 0.0;
		double totalManConv = 0.0;
		double totalPerfConv = 0.0;
		double totalPos = 0;
		double percInAuctions = 0;
		double totDays = 0;
		double totOverCap = 0;
		double percOverCap = 0;
		double percOverCapStdDev = 0;
		System.out.println("Generating Reports");
		String profits = "";
		for(String file : reportsListMegaMap.keySet()) {
			System.out.println(file);
			HashMap<String, LinkedList<Reports>> map = reportsListMegaMap.get(file);
			LinkedList<Reports> reports = map.get(ourAgent);
			totDays = reportsListMegaMap.size() * reports.size();
			double profitTot = 0;
			int[] sales = new int[reports.size()];
			int i = 0;
			int capacity = capacities.get(file);
			String ourManSpecialty = manSpecialties.get(file);
			String ourCompSpecialty = compSpecialties.get(file);
			for(Reports report : reports) {
				QueryReport queryReport = report.getQueryReport();
				SalesReport salesReport = report.getSalesReport();
				int totSales = 0;
				for(Query query : _querySpace) {
					profitTot += salesReport.getRevenue(query)-queryReport.getCost(query);
					totalRevenue += salesReport.getRevenue(query);
					totalCost += queryReport.getCost(query);
					totalImp += queryReport.getImpressions(query);
					totalClick += queryReport.getClicks(query);
					totalConv += salesReport.getConversions(query);
					totSales += salesReport.getConversions(query);
					if(!Double.isNaN(queryReport.getPosition(query))) {
						totalPos += queryReport.getPosition(query);
						percInAuctions++;
					}
					if(ourManSpecialty.equals(query.getManufacturer())) {
						totalManConv += salesReport.getConversions(query);
					}
					if(ourCompSpecialty.equals(query.getComponent())) {
						totalCompConv += salesReport.getConversions(query);
					}
					if(ourManSpecialty.equals(query.getManufacturer()) && ourCompSpecialty.equals(query.getComponent())) {
						totalPerfConv += salesReport.getConversions(query);
					}
					if(!(ourManSpecialty.equals(query.getManufacturer()) || ourCompSpecialty.equals(query.getComponent()))) {
						totalNoneConv += salesReport.getConversions(query);
					}
				}
				sales[i] = totSales;
				i++;
			}
			profits += profitTot + ", ";
			
			System.out.println(capacity);
			Double[] percOverCapArr = new Double[sales.length];
			for(int j = 0; j < sales.length; j++) {
				double convs = 0;
				for(int idx = j-4; idx < j; idx++) {
					if(idx < 0) {
						convs += capacity/5.0;
					}
					else {
						convs += sales[idx];
					}
				}
				convs += sales[j];
				totOverCap += convs-capacity;
				percOverCap += convs/capacity;
				percOverCapArr[j] = convs/capacity;
				System.out.println(percOverCapArr[j] + ", " + sales[j]/(capacity/5.0));
			}
			
			percOverCapStdDev += stdDeviation(percOverCapArr)[1];
			
			if(capacity == 300) {
				lowVals.add(profitTot);
			}
			else if(capacity == 400) {
				medVals.add(profitTot);
			}
			else {
				highVals.add(profitTot);
			}
			
		}
		
		allVals.addAll(lowVals);
		allVals.addAll(medVals);
		allVals.addAll(highVals);

		double[] stdDevLow = stdDeviation(lowVals.toArray(new Double[lowVals.size()]));
		double[] stdDevMed = stdDeviation(medVals.toArray(new Double[medVals.size()]));
		double[] stdDevHigh = stdDeviation(highVals.toArray(new Double[highVals.size()]));
		double[] stdDevAll = stdDeviation(allVals.toArray(new Double[allVals.size()]));
		
		System.out.println("Profit per game: , " + profits);
		String header = "Agent,Avg Profit(All), StdDev Profit (All),Avg Profit(High), StdDev Profit (High),Avg Profit(Med), StdDev Profit (Med),Avg Profit(Low), StdDev Profit (Low), Avg Revenue,Avg Cost,Avg Impressions,Avg Clicks,Avg Conversions,Avg Position,% in auction,CPC,ClickPr,ConvPr,Sales in neither,Sales in comp" +
		",Sales in man,Sales in both, # Overcap, % Overcap";
		String output = ourAgent + ",";

		output += stdDevAll[0] + ", " + stdDevAll[1] + ", ";
		output += stdDevHigh[0] + ", " + stdDevHigh[1] + ", ";
		output += stdDevMed[0] + ", " + stdDevMed[1] + ", ";
		output += stdDevLow[0] + ", " + stdDevLow[1] + ", ";
		output +=  (totalRevenue/reportsListMegaMap.size()) + ",";
		output +=  (totalCost/reportsListMegaMap.size()) + ",";
		output +=  (totalImp/reportsListMegaMap.size()) + ",";
		output +=  (totalClick/reportsListMegaMap.size()) + ",";
		output +=  (totalConv/reportsListMegaMap.size()) + ",";
		output +=  (totalPos/(percInAuctions)) + ",";
		output +=  (percInAuctions/(totDays*16)) + ",";
		output += (totalCost/totalClick) + ",";
		output += (totalClick/totalImp) + ",";
		output += (totalConv/totalClick) + ",";
		output += (totalNoneConv/totalConv) + ",";
		output += (totalCompConv/totalConv) + ",";
		output += (totalManConv/totalConv) + ",";
		output += (totalPerfConv/totalConv) + ",";
		output += (totOverCap/(totDays)) + ",";
		output += (percOverCap/(totDays)) + ",";
		output += (percOverCapStdDev/reportsListMegaMap.size()) + ", ";
		System.out.println(header + "\n" + output);
	}
	
	public static void generateAllAgentProfit(String filename, int min, int max) throws IOException, ParseException {
		boolean firstSim = true;
		HashMap<String,ArrayList<Double>> results = new HashMap<String, ArrayList<Double>>();
		HashMap<String,ArrayList<Double>> resultsLow = new HashMap<String, ArrayList<Double>>();
		HashMap<String,ArrayList<Double>> resultsMed = new HashMap<String, ArrayList<Double>>();
		HashMap<String,ArrayList<Double>> resultsHigh = new HashMap<String, ArrayList<Double>>();
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			String[] advertisers = gameStatus.getAdvertisers();
			if(firstSim) {
				for(int j = 0; j < advertisers.length; j++) {
					results.put(advertisers[j], new ArrayList<Double>());
					resultsLow.put(advertisers[j], new ArrayList<Double>());
					resultsMed.put(advertisers[j], new ArrayList<Double>());
					resultsHigh.put(advertisers[j], new ArrayList<Double>());
				}
				firstSim = false;
			}
			HashMap<String, LinkedList<BankStatus>> bankStatuses = gameStatus.getBankStatuses();
			HashMap<String, AdvertiserInfo> advInfos = gameStatus.getAdvertiserInfos();
			for(int j = 0; j < advertisers.length; j++)	{
				LinkedList<BankStatus> bankStatus = bankStatuses.get(advertisers[j]);
				AdvertiserInfo advInfo = advInfos.get(advertisers[j]);
				BankStatus status = bankStatus.get(bankStatus.size()-1);
				
				results.get(advertisers[j]).add(status.getAccountBalance());

				int capacity = advInfo.getDistributionCapacity();
				if(capacity == 300) {
					resultsLow.get(advertisers[j]).add(status.getAccountBalance());
				}
				else if(capacity == 400) {
					resultsMed.get(advertisers[j]).add(status.getAccountBalance());
				}
				else {
					resultsHigh.get(advertisers[j]).add(status.getAccountBalance());
				}
			}
		}
		
		System.out.println("Agent,All Mean,All StdDev,Low Mean,Low StdDev,Med Mean,Med StdDev,High Mean, High StdDev");
		for(String name : results.keySet()) {
			double[] meanAndVar = meanAndVar(results.get(name));
			double[] meanAndVarLow = meanAndVar(resultsLow.get(name));
			double[] meanAndVarMed = meanAndVar(resultsMed.get(name));
			double[] meanAndVarHigh = meanAndVar(resultsHigh.get(name));

			System.out.println(name + ", " + meanAndVar[0] + ", " + meanAndVar[1] + ", " + 
							   meanAndVarLow[0] + ", " + meanAndVarLow[1] + ", " + 
							   meanAndVarMed[0] + ", " + meanAndVarMed[1] + ", " + 
							   meanAndVarHigh[0] + ", " + meanAndVarHigh[1]);
		}
	}

	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		String filename = "/Users/jordanberg/TADAGAMES/MCKP/cslab1a_sim";
		int min = 3;
		int max = 63;
		int advId = 0;
		
//		String filename = "/Users/jordanberg/TADAGAMES/EquatePM/cslab2a_sim";
//		int min = 8;
//		int max = 68;
//		int advId = 0;
		
//		String filename = "/Users/jordanberg/TADAGAMES/EquatePPS/cslab3a_sim";
//		int min = 3;
//		int max = 63;
//		int advId = 0;
		
//		generateExtendedStats(filename,min,max,advId);
		generateAllAgentProfit(filename,min,max);
	}
}
