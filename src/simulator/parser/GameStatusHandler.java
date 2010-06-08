package simulator.parser;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import se.sics.isl.transport.Transportable;
import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import simulator.AgentBidPair;
import simulator.SimAgent;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;
import edu.umich.eecs.tac.props.UserPopulationState;

/**
 *
 * @author jberg
 * 
 */
public class GameStatusHandler {


	private GameStatus _gameStatus;


	public GameStatusHandler(String filename) throws IOException, ParseException {
		_gameStatus = parseGameLog(filename);
	}

	public GameStatus parseGameLog(String filename) throws IOException, ParseException {
		InputStream inputStream = new FileInputStream(filename);
		GameLogParser parser = new GameLogParser(new LogReader(inputStream));
		parser.start();
		parser.stop();
		LinkedList<SimParserMessage> messages = parser.getMessages();

		ParticipantInfo[] participants = parser.getParticipants();
		String[] participantNames = parser.getParticipantNames();
		boolean[] isAdvertiser = parser.getIsAdvertiser();
		int numadvertisers = 0;
		ArrayList<String> advertiserslist = new ArrayList<String>();
		for(int i = 0; i < participants.length; i++) { 
			if(isAdvertiser[i]) {
				numadvertisers++;
				advertiserslist.add(participantNames[i]);
			}
		}
		String[] advertisers = new String[numadvertisers];
		advertisers = advertiserslist.toArray(advertisers);

		/*
		 * These variables will be used save the gamestate 
		 */
		HashMap<String,LinkedList<BankStatus>> bankStatuses = new HashMap<String,LinkedList<BankStatus>>();
		HashMap<String,LinkedList<BidBundle>> bidBundles = new HashMap<String, LinkedList<BidBundle>>();
		HashMap<String,LinkedList<QueryReport>> queryReports = new HashMap<String, LinkedList<QueryReport>>();
		HashMap<String,LinkedList<SalesReport>> salesReports = new HashMap<String, LinkedList<SalesReport>>();
		HashMap<String,AdvertiserInfo> advertiserInfos = new HashMap<String, AdvertiserInfo>();
		LinkedList<HashMap<Product,HashMap<UserState,Integer>>> userDists = new LinkedList<HashMap<Product,HashMap<UserState,Integer>>>();

		SlotInfo slotInfo = null;
		ReserveInfo reserveInfo = null;
		PublisherInfo pubInfo = null;
		AdvertiserInfo advInfo = null;
		RetailCatalog retailCatalog = null;
		UserClickModel userClickModel = null;

		for(int i = 0; i < advertisers.length; i++) {
			LinkedList<BankStatus> bankStatuslist = new LinkedList<BankStatus>();
			LinkedList<BidBundle> bidBundlelist = new LinkedList<BidBundle>();
			LinkedList<QueryReport> queryReportlist = new LinkedList<QueryReport>();
			LinkedList<SalesReport> salesReportlist = new LinkedList<SalesReport>();
			LinkedList<SimulationStatus> simulationStatusList = new LinkedList<SimulationStatus>();
			bankStatuses.put(advertisers[i], bankStatuslist);
			bidBundles.put(advertisers[i], bidBundlelist);
			queryReports.put(advertisers[i], queryReportlist);
			salesReports.put(advertisers[i], salesReportlist);
		}

		boolean slotinfoflag = false;
		boolean reserveinfoflag = false;
		boolean pubinfoflag = false;
		boolean retailcatalogflag = false;
		boolean userclickmodelflag = false;

		for(int i = 0; i < messages.size(); i++) {
			SimParserMessage mes = messages.get(i);
			int from = mes.getSender();
			int to = mes.getReceiver();
			int messageDay = mes.getDay();
			Transportable content = mes.getContent();
			if (content instanceof BankStatus) {
				if(messageDay >= 1) {
					BankStatus bankstatustemp = (BankStatus) content;
					String name = participantNames[to];
					LinkedList<BankStatus> bankstatuslist = bankStatuses.get(name);
					bankstatuslist.addLast(bankstatustemp);
					bankStatuses.put(name, bankstatuslist);
				}
			}
			else if (content instanceof SlotInfo && !slotinfoflag) {
				SlotInfo slotinfotemp = (SlotInfo) content;
				slotInfo = slotinfotemp;
				slotinfoflag = true;
			}
			else if (content instanceof ReserveInfo && !reserveinfoflag) {
				ReserveInfo reserveinfotemp = (ReserveInfo) content;
				reserveInfo = reserveinfotemp;
				reserveinfoflag = true;
			}
			else if (content instanceof PublisherInfo && !pubinfoflag) {
				PublisherInfo publisherinfotemp = (PublisherInfo) content;
				pubInfo = publisherinfotemp;
				pubinfoflag = true;
			}
			else if (content instanceof SalesReport) {
				/*
				 * The first three salesreports messages we get are all zeros.
				 * The first one is sent pregame, and the next two are sent on
				 * days one and two before we actually get bank information back
				 * because of the two day lag.
				 */
				if(messageDay >= 2) {
					SalesReport salesreporttemp = (SalesReport) content;
					String name = participantNames[to];
					LinkedList<SalesReport> salesreportlist = salesReports.get(name);
					salesreportlist.addLast(salesreporttemp);
					salesReports.put(name, salesreportlist);
				}
			}
			else if (content instanceof QueryReport) {
				/*
				 * The first three queryreports messages we get are all zeros.
				 * The first one is sent pregame, and the next two are sent on
				 * days one and two before we actually get bank information back
				 * because of the two day lag.
				 */
				if(messageDay >= 2) {
					QueryReport queryreporttemp = (QueryReport) content;
					String name = participantNames[to];
					LinkedList<QueryReport> queryreportlist = queryReports.get(name);
					queryreportlist.addLast(queryreporttemp);
					queryReports.put(name, queryreportlist);
				}
			}
			else if (content instanceof RetailCatalog && !retailcatalogflag) {
				RetailCatalog retailcatalogtemp = (RetailCatalog) content;
				retailCatalog = retailcatalogtemp;
				retailcatalogflag = true;
			}
			else if (content instanceof BidBundle) {
				if(messageDay <= 58) {
					BidBundle bidbundletemp = (BidBundle) content;
					String name = participantNames[from];
					LinkedList<BidBundle> bidbundlelist = bidBundles.get(name);
					/*
					 * If a bid bundle is missing, it means the person uses the same
					 * bundle as the last time
					 */
					int listsize = bidbundlelist.size();
					if(messageDay > listsize) {
						for(int j = 0; j < (messageDay-listsize); j++) {
							if(bidbundlelist.size() > 0) {
							BidBundle bundleTemp = bidbundlelist.getLast();
							BidBundle newBundle = copyBundle(bundleTemp);
							bidbundlelist.add(newBundle);
							}
							else {
								bidbundlelist.add(new BidBundle());
							}
						}
					}
					/*
					 * This ensures we don't get too many
					 */
					if(messageDay == bidbundlelist.size()) {
						/*
						 * Check for NaN's and Negative bids which
						 * are replaced by our bid from last round
						 */
						for(Query query : bidbundletemp.keys()) {
							double bid = bidbundletemp.getBid(query);
							if(Double.isNaN(bid) || bid <  0) {
								bidbundletemp.addQuery(query, bidbundlelist.get(bidbundlelist.size()-1).getBid(query), bidbundletemp.getAd(query), bidbundletemp.getDailyLimit(query));
							}
						}
						bidbundlelist.addLast(bidbundletemp);
					}
					/*
					 * This means they sent a second bid bundle, so write over the last entry
					 */
					else {
						bidbundlelist.removeLast();
						/*
						 * Check for NaN's and Negative bids which
						 * are replaced by our bid from last round
						 */
						for(Query query : bidbundletemp.keys()) {
							double bid = bidbundletemp.getBid(query);
							if(Double.isNaN(bid) || bid <  0) {
								bidbundletemp.addQuery(query, bidbundlelist.get(bidbundlelist.size()-1).getBid(query), bidbundletemp.getAd(query), bidbundletemp.getDailyLimit(query));
							}
						}
						bidbundlelist.add(bidbundletemp);
					}
					bidBundles.put(name, bidbundlelist);
				}
			}
			else if (content instanceof UserClickModel && !userclickmodelflag) {
				UserClickModel userclickmodeltemp = (UserClickModel) content;
				userClickModel = userclickmodeltemp;
				userclickmodelflag = true;
			}
			else if (content instanceof AdvertiserInfo) {
				AdvertiserInfo advertiserinfotemp = (AdvertiserInfo) content;
				advInfo = advertiserinfotemp;
				String name = participantNames[to];
				advertiserInfos.put(name, advInfo);
			}
			else if (content instanceof UserPopulationState) {
				if(messageDay >= 1) {
					UserPopulationState userPopState = (UserPopulationState) content;
					HashMap<Product, HashMap<UserState,Integer>> userDist = new HashMap<Product, HashMap<UserState,Integer>>();
					for(Product p : retailCatalog) {
						int[] users = userPopState.getDistribution(p);
						HashMap<UserState,Integer> userDistperProd = new HashMap<UserState, Integer>();
						userDistperProd.put(UserState.NS, users[0]);
						userDistperProd.put(UserState.IS, users[1]);
						userDistperProd.put(UserState.F0, users[2]);
						userDistperProd.put(UserState.F1, users[3]);
						userDistperProd.put(UserState.F2, users[4]);
						userDistperProd.put(UserState.T, users[5]);
						userDist.put(p, userDistperProd);
					}
					userDists.add(userDist);
				}
			}
			else {
				//	    		throw new RuntimeException("Unexpected parse token");
			}
		}


		//Ensure everyone has the right number of bid bundles! (59)
		for(int i = 0; i < participants.length; i++) { 
			if(isAdvertiser[i]) {
				String name = participantNames[i];
				LinkedList<BidBundle> bidbundlelist = bidBundles.get(name);
				int bundleListSize = bidbundlelist.size();
				if(bundleListSize < 59) {
					if(bundleListSize == 0) {
						for(int j = 0; j < 59; j++) {
							bidbundlelist.add(new BidBundle());
						}
					}
					else {
						for(int j = 0; j < (59 - bundleListSize); j++) {
							BidBundle bundleTemp = bidbundlelist.getLast();
							BidBundle newBundle = copyBundle(bundleTemp);
							bidbundlelist.add(newBundle);
						}
					}
				}
			}
		}


		GameStatus gameStatus = new GameStatus(advertisers, bankStatuses, bidBundles, queryReports, salesReports, advertiserInfos,
				userDists, slotInfo, reserveInfo, pubInfo, retailCatalog, userClickModel);
		return gameStatus;
	}


	private BidBundle copyBundle(BidBundle bundleTemp) {
		BidBundle newBundle = new BidBundle();
		for(Query query : bundleTemp) {
			newBundle.addQuery(query, bundleTemp.getBid(query), bundleTemp.getAd(query), bundleTemp.getDailyLimit(query));
		}
		newBundle.setCampaignDailySpendLimit(bundleTemp.getCampaignDailySpendLimit());
		return newBundle;
	}

	public GameStatus getGameStatus() {
		return _gameStatus;
	}

	@SuppressWarnings("unused")
	private static double[] getStdDevAndMean(ArrayList<Double> list) {
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

	public static String generateWEKADataSet() throws IOException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		String output = "";
		int min = 1425;
		int max = 1426;
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			output += "%" + "\n";
			output += "%" + "\n";
			output += "@RELATION TACAA" + "\n";
			output += "" + "\n";
			output += "@ATTRIBUTE bid NUMERIC" + "\n";
			output += "@ATTRIBUTE position NUMERIC" + "\n";
			output += "@ATTRIBUTE cpc NUMERIC" + "\n";
			output += "@ATTRIBUTE prclick NUMERIC" + "\n";
			output += "@ATTRIBUTE prconv NUMERIC" + "\n";
			output += "@ATTRIBUTE query {null-null,lioneer-null,null-tv,lioneer-tv,null-audio,lioneer-audio,null-dvd,lioneer-dvd,pg-null,pg-tv,pg-audio,pg-dvd,flat-null,flat-tv,flat-audio,flat-dvd}" + "\n";
			output += "" + "\n";
			output += "@DATA" + "\n";
			for(int j = 0; j < 59; j++) {
				Iterator<Query> iter = bidBundles.get("Schlemazl").get(j).iterator();
				while(iter.hasNext()) {
					Query query = (Query) iter.next();
					String adv = "Schlemazl";
					BidBundle bundle = bidBundles.get(adv).get(j);
					QueryReport qreport = queryReports.get(adv).get(j);
					SalesReport sreport = salesReports.get(adv).get(j);
					//bid
					output += bundle.getBid(query);
					//position
					if(Double.isNaN(qreport.getPosition(query))) {
						output += ",6.0";
					}
					else {
						output += "," + qreport.getPosition(query);
					}
					//CPC
					if(Double.isNaN(qreport.getCPC(query))) {
						output += ",0.0";
					}
					else {
						output += "," + qreport.getCPC(query);
					}
					//clickpr
					if(qreport.getClicks(query) == 0 || qreport.getImpressions(query) == 0) {
						output += ",0.0";
					}
					else {
						output += "," + (qreport.getClicks(query)/((double) qreport.getImpressions(query)));
					}
					//convPr
					if(qreport.getClicks(query) == 0 || sreport.getConversions(query) == 0) {
						output += ",0.0";
					}
					else {
						output += "," + (sreport.getConversions(query)/((double) qreport.getClicks(query)));
					}
					output += "," + query.getManufacturer() +"-" + query.getComponent() + "\n";
				}
			}
		}
		return output;
	}

	public static HashMap<Query,String> generateRDataSetOnlyBids(int min, int max, String adv) throws IOException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		//		int min = 1425;
		//		int max = 1426;
		//		String adv = "AstonTAC";
		//		String adv = "MetroClick";
		//		String adv = "Schlemazl";
		//		String adv = "epflagent";
		//		String adv = "QuakTAC";
		//		String adv = "UMTac09";
		//		String adv = "munsey";
		//		String adv = "TacTex";

		HashMap<Query,String> outputs = new HashMap<Query,String>();

		Set<Query> querySpace = new LinkedHashSet<Query>();
		querySpace.add(new Query(null, null));
		querySpace.add(new Query("lioneer", null));
		querySpace.add(new Query(null, "tv"));
		querySpace.add(new Query("lioneer", "tv"));
		querySpace.add(new Query(null, "audio"));
		querySpace.add(new Query("lioneer", "audio"));
		querySpace.add(new Query(null, "dvd"));
		querySpace.add(new Query("lioneer", "dvd"));
		querySpace.add(new Query("pg", null));
		querySpace.add(new Query("pg", "tv"));
		querySpace.add(new Query("pg", "audio"));
		querySpace.add(new Query("pg", "dvd"));
		querySpace.add(new Query("flat", null));
		querySpace.add(new Query("flat", "tv"));
		querySpace.add(new Query("flat", "audio"));
		querySpace.add(new Query("flat", "dvd"));

		for(Query query : querySpace) {
			outputs.put(query,"bid\n");
		}

		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			for(int j = 0; j < 59; j++) {
				Iterator<Query> iter = bidBundles.get(adv).get(j).iterator();
				while(iter.hasNext()) {
					Query query = (Query) iter.next();
					BidBundle bundle = bidBundles.get(adv).get(j);
					String output = outputs.get(query);
					output += bundle.getBid(query) + "\n";
					outputs.put(query,output);
				}
			}
		}

		for(Query query : querySpace) {
			String output = outputs.get(query);
			outputs.put(query,output.substring(0, output.length()-1));
		}

		return outputs;
	}

	public static String generateRDataSet() throws IOException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		String output = "";
		int min = 1425;
		int max = 1426;
		String adv = "Schlemazl";
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			output += "bid pos cpc prclick prconv query \n";
			for(int j = 0; j < 59; j++) {
				Iterator<Query> iter = bidBundles.get(adv).get(j).iterator();
				while(iter.hasNext()) {
					Query query = (Query) iter.next();
					BidBundle bundle = bidBundles.get(adv).get(j);
					QueryReport qreport = queryReports.get(adv).get(j);
					SalesReport sreport = salesReports.get(adv).get(j);
					//bid
					output += bundle.getBid(query);
					//position
					if(Double.isNaN(qreport.getPosition(query))) {
						output += " 6.0";
					}
					else {
						output += " " + qreport.getPosition(query);
					}
					//CPC
					if(Double.isNaN(qreport.getCPC(query))) {
						output += " 0.0";
					}
					else {
						output += " " + qreport.getCPC(query);
					}
					//clickpr
					if(qreport.getClicks(query) == 0 || qreport.getImpressions(query) == 0) {
						output += " 0.0";
					}
					else {
						output += " " + (qreport.getClicks(query)/((double) qreport.getImpressions(query)));
					}
					//convPr
					if(qreport.getClicks(query) == 0 || sreport.getConversions(query) == 0) {
						output += " 0.0";
					}
					else {
						output += " " + (sreport.getConversions(query)/((double) qreport.getClicks(query)));
					}
					output += " " + query.getManufacturer() +"-" + query.getComponent() + "\n";
				}
			}
		}
		output = output.substring(0, output.length()-1);
		return output;
	}

	public static void generateCarletonDataSet2() throws IOException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		int min = 1425;
		int max = 1430;
		int numSlots = 5;
		System.out.println("Num Advertisers\tNum slots");
		System.out.println("AgentID\tAvgPos\t#Imps\tBids\tBudgets\n");
		Random R = new Random();
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			String[] advertisers = gameStatus.getAdvertisers();
			int numAdvs = advertisers.length;
			ReserveInfo reserveInfo = gameStatus.getReserveInfo();
			double reserve = reserveInfo.getRegularReserve();
			PublisherInfo pubInfo = gameStatus.getPubInfo();
			double squashing = pubInfo.getSquashingParameter();
			UserClickModel clickModel = gameStatus.getUserClickModel();
			for(int j = 0; j < 59; j++) {
				Iterator<Query> iter = bidBundles.get(advertisers[0]).get(j).iterator();
				while(iter.hasNext()) {
					Query query = (Query) iter.next();
					String output = "";
					output += numAdvs + " " + numSlots + "\n";
					ArrayList<AdvBidPair> bidPairListAdvSort = new ArrayList<AdvBidPair>();
					ArrayList<AdvBidPair> bidPairListBidSort = new ArrayList<AdvBidPair>();
					for(int k = 0; k < advertisers.length; k++) {
						String adv = advertisers[k];
						BidBundle bundle = bidBundles.get(adv).get(j);
						QueryReport qreport = queryReports.get(adv).get(j);
						double avgPos = Double.isNaN(qreport.getPosition(query)) ? -1 : qreport.getPosition(query);
						double squashedBid = bundle.getBid(query) * Math.pow(clickModel.getAdvertiserEffect(clickModel.queryIndex(query), k), squashing);
						double budget = Double.isNaN(bundle.getDailyLimit(query)) ? -1 : bundle.getDailyLimit(query);
						if(Double.isNaN(squashedBid)) {
							throw new RuntimeException();
						}
						int numImps = qreport.getImpressions(query);
						output += (k + 1) + " " + avgPos + " " + numImps + " " + squashedBid + " " + budget + "\n";
					}
					output = output.substring(0, output.length()-1);
					// Stream to write file
					FileOutputStream fout;		

					try
					{
						// Open an output stream
						fout = new FileOutputStream ("carleton" + R .nextDouble() + ".o");

						// Print a line of text
						new PrintStream(fout).print(output);

						// Close our output stream
						fout.close();		
					}
					// Catches any error conditions
					catch (IOException e)
					{
						System.err.println ("Unable to write to file");
						System.exit(-1);
					}
				}
			}
		}
	}

	public static void generateCarletonDataSet() throws IOException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		int min = 1425;
		int max = 1426;
		int numSlots = 5;
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			String[] advertisers = gameStatus.getAdvertisers();
			int numAdvs = advertisers.length;
			ReserveInfo reserveInfo = gameStatus.getReserveInfo();
			double reserve = reserveInfo.getRegularReserve();
			PublisherInfo pubInfo = gameStatus.getPubInfo();
			double squashing = pubInfo.getSquashingParameter();
			UserClickModel clickModel = gameStatus.getUserClickModel();
			for(int j = 0; j < 59; j++) {
				for(int ourAdvId = 0; ourAdvId < advertisers.length; ourAdvId++) {
					Iterator<Query> iter = bidBundles.get(advertisers[0]).get(j).iterator();
					while(iter.hasNext()) {
						Query query = (Query) iter.next();
						String output = "";
						output += numAdvs + " " + numSlots + "\n";
						output += (ourAdvId + 1) + " " + queryReports.get(advertisers[ourAdvId]).get(j).getImpressions(query) + "\n";
						ArrayList<AdvBidPair> bidPairListAdvSort = new ArrayList<AdvBidPair>();
						ArrayList<AdvBidPair> bidPairListBidSort = new ArrayList<AdvBidPair>();
						for(int k = 0; k < advertisers.length; k++) {
							String adv = advertisers[k];
							BidBundle bundle = bidBundles.get(adv).get(j);
							QueryReport qreport = queryReports.get(adv).get(j);
							double avgPos = Double.isNaN(qreport.getPosition(query)) ? -1 : qreport.getPosition(query);
							double squashedBid = bundle.getBid(query) * Math.pow(clickModel.getAdvertiserEffect(clickModel.queryIndex(query), k), squashing);
							if(Double.isNaN(squashedBid)) {
								throw new RuntimeException();
							}
							int numImps = qreport.getImpressions(query);
							output += avgPos + " ";

							AdvBidPair bidPair = new AdvBidPair(k, squashedBid, numImps,avgPos);
							bidPairListBidSort.add(bidPair);
							bidPairListAdvSort.add(bidPair);
						}
						output = output.substring(0, output.length()-1);
						output += "\n\n";

						Collections.sort(bidPairListBidSort);
						int[][][] startEndImpArr = new int[numAdvs][numSlots][2];

						/*
						 * Determine Time Spent in each position
						 */
						int nextSlot = 0;
						for(int k = 0; k < advertisers.length; k++) {
							AdvBidPair bidPair = bidPairListAdvSort.get(k);
							int numImps = bidPair.getNumImps();
							if(numImps > 0) {
								if(nextSlot == 0) {
									startEndImpArr[k][nextSlot][0] = 1;
									startEndImpArr[k][nextSlot++][1] = numImps+1;
								}
								else {
									/*
									 * We need to check 2 things, one if the person
									 * above us moved up in position, or if we
									 * had more impressions than them.  If either of these
									 * are true the impressions will be spread over multiple
									 * slots, otherwise all impressions were in the current
									 * start slot
									 */
									int numSkips = 0;
									boolean moveUp = false;
									int currSlot = nextSlot < numSlots ? nextSlot : numSlots;
									int currImp = 1;
									int impsUsed = 0;
									for(int l = 0; l < nextSlot; l++) {
										int idx = k - (l + numSkips + 1);
										boolean spread = false;
										int numImpsOther = 0;
										for(int m = 0; m < startEndImpArr[idx].length; m++) {
											int newImps = startEndImpArr[idx][m][1]-startEndImpArr[idx][m][0];
											if(numImpsOther > 0 && newImps > 0) {
												spread = true;
											}
											numImpsOther += newImps;
										}

										if(numImpsOther == 0) {
											numSkips++; //we need to skip this advertiser
										}
										else {
											if(spread) {
												moveUp = true;


												if(numImps > numImpsOther) {

												}
												else {
													break;
												}
											}
											else if(numImps > numImpsOther) {

											}
											else if(impsUsed >= numImps) {
												break;
											}
											else {
												startEndImpArr[k][currSlot][0] = currImp + 1;
												startEndImpArr[k][currSlot][1] = (numImps - impsUsed) + currImp + 1;
												break;
											}
										}
									}

									if(moveUp) {
										if(nextSlot >= numSlots) {

										}
										else {
											int idx = k - (numSkips + 1);
											startEndImpArr[k][nextSlot][0] = 1;
											startEndImpArr[k][nextSlot][1] = startEndImpArr[idx][nextSlot][1];
											for(int l = 0; l < nextSlot; l++) {
												idx = k - (l + numSkips + 1);
												startEndImpArr[k][nextSlot-l][0] = startEndImpArr[k][nextSlot][1] + 1;
												startEndImpArr[k][nextSlot][1] = startEndImpArr[idx][nextSlot][1];
											}
											nextSlot++;
										}
									}
									else {
										startEndImpArr[k][nextSlot][0] = 1;
										startEndImpArr[k][nextSlot++][1] = numImps+1;
									}
								}
							}
						}


						for(int x = 0; x < startEndImpArr.length; x++) {
							for(int y = 0; y < startEndImpArr[x].length; y++) {
								for(int z = 0; z < startEndImpArr[x][y].length; z++) {
									output += startEndImpArr[x][y][z] + " ";
								}
								output = output.substring(0, output.length()-1);
								output += "\n";
							}
							output += "\n";
						}
						output = output.substring(0, output.length()-2);
						System.out.println(output);

						//check validity
						for(int x = 0; x < startEndImpArr.length; x++) {
							double posSum = 0;
							for(int y = 0; y < startEndImpArr[x].length; y++) {
								posSum += (y+1) * (startEndImpArr[x][y][1]-startEndImpArr[x][y][0]);
							}

							double avgPos = posSum / numSlots;
							if(avgPos == 0) {
								avgPos = -1;
							}

							if(Math.abs(avgPos - bidPairListAdvSort.get(x).getAvgPos()) < .0001) {
								System.out.println("TRUE");
							}
							else {
								System.out.println("FALSE");
							}
						}
					}
				}
			}
		}
	}

	public static class AdvBidPair implements Comparable<AdvBidPair> {

		private int _advIdx;
		private double _bid;
		private int _numImps;
		private double _avgPos;

		public AdvBidPair(int advIdx, double bid, int numImps, double avgPos) {
			_advIdx = advIdx;
			_bid = bid;
			_numImps = numImps;
			_avgPos = avgPos;
		}

		public int getID() {
			return _advIdx;
		}

		public void setID(int advIdx) {
			_advIdx = advIdx;
		}

		public double getBid() {
			return _bid;
		}

		public void setBid(double bid) {
			_bid = bid;
		}

		public int getNumImps() {
			return _numImps;
		}

		public void setNumImps(int numImps) {
			_numImps = numImps;
		}

		public double getAvgPos() {
			return _avgPos;
		}

		public void setAvgPos(double avgPos) {
			_avgPos = avgPos;
		}

		public int compareTo(AdvBidPair agentBidPair) {
			double ourBid = this._bid;
			double otherBid = agentBidPair.getBid();
			if(ourBid < otherBid) {
				return 1;
			}
			if(otherBid < ourBid) {
				return -1;
			}
			else {
				return 0;
			}
		}

		public String toString() {
			return _bid + ", " + _avgPos + ", " + _advIdx + ", " + _numImps + "\n";
		}

	}

	public static void main(String[] args) throws FileNotFoundException, IOException, IllegalConfigurationException, ParseException {
		int min = 1442;
		int max = 1443;
				String adv = "AstonTAC";
//				String adv = "MetroClick";
//				String adv = "Schlemazl";
//				String adv = "epflagent";
//				String adv = "QuakTAC";
//				String adv = "UMTac09";
//				String adv = "munsey";
//		String adv = "TacTex";
		HashMap<Query, String> outputs = generateRDataSetOnlyBids(min,max,adv);
		for(Query query : outputs.keySet()) {
			FileOutputStream fout;		

			try
			{
				// Open an output stream
				fout = new FileOutputStream ("Rdata" + min + adv + query.getComponent() + query.getManufacturer() + ".data");

				// Print a line of text
				new PrintStream(fout).println(outputs.get(query));

				// Close our output stream
				fout.close();		
			}
			// Catches any error conditions
			catch (IOException e)
			{
				System.err.println ("Unable to write to file");
				System.exit(-1);
			}
		}

		//		generateCarletonDataSet2();


		//		// Stream to write file
		//		FileOutputStream fout;		
		//
		//		try
		//		{
		//			// Open an output stream
		//			fout = new FileOutputStream ("Rdata.data");
		//
		//			// Print a line of text
		//			new PrintStream(fout).println(Rdata);
		//
		//			// Close our output stream
		//			fout.close();		
		//		}
		//		// Catches any error conditions
		//		catch (IOException e)
		//		{
		//			System.err.println ("Unable to write to file");
		//			System.exit(-1);
		//		}
	}


}
