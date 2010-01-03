package simulator.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.omg.CORBA._PolicyStub;

import se.sics.isl.transport.Transportable;
import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import usermodel.UserState;
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
							BidBundle bundleTemp = bidbundlelist.getLast();
							BidBundle newBundle = copyBundle(bundleTemp);
							bidbundlelist.add(newBundle);
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

	public static void main(String[] args) throws FileNotFoundException, IOException, IllegalConfigurationException, ParseException {
		String filename = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		int min = 1425;
		int max = 1426;
		HashMap<String,Double> results = new HashMap<String, Double>();
		for(int i = min; i < max; i++) {
			String file = filename + i + ".slg";
			GameStatusHandler gameStatusHandler = new GameStatusHandler(file);
			GameStatus gameStatus = gameStatusHandler.getGameStatus();
			String[] advertisers = gameStatus.getAdvertisers();
			HashMap<String, LinkedList<BankStatus>> bankStatuses = gameStatus.getBankStatuses();
			HashMap<String, LinkedList<BidBundle>> bidBundles = gameStatus.getBidBundles();
			HashMap<String, LinkedList<QueryReport>> queryReports = gameStatus.getQueryReports();
			HashMap<String, LinkedList<SalesReport>> salesReports = gameStatus.getSalesReports();
			HashMap<String, AdvertiserInfo> advertiserInfos = gameStatus.getAdvertiserInfos();
			//			LinkedList<HashMap<Product, HashMap<UserState, Integer>>> usersDists = gameStatus.getUserDistributions();
			//			SlotInfo slotInfo = gameStatus.getSlotInfo();
			//			ReserveInfo reserveInfo = gameStatus.getReserveInfo();
			PublisherInfo pubInfo = gameStatus.getPubInfo();
			//			RetailCatalog retailCatalog = gameStatus.getRetailCatalog();
			System.out.println("%");
			System.out.println("%  TAC AA Dataset");
			System.out.println("%");
			System.out.println("@RELATION TACAA");
			System.out.println("");
			System.out.println("@ATTRIBUTE bid NUMERIC");
			//			System.out.println("@ATTRIBUTE position NUMERIC");
			System.out.println("@ATTRIBUTE cpc NUMERIC");
			//			System.out.println("@ATTRIBUTE prclick NUMERIC");
			//			System.out.println("@ATTRIBUTE bid NUMERIC");
			//			System.out.println("@ATTRIBUTE prconv NUMERIC");
			System.out.println("@ATTRIBUTE query {null-null,lioneer-null,null-tv,lioneer-tv,null-audio,lioneer-audio,null-dvd,lioneer-dvd,pg-null,pg-tv,pg-audio,pg-dvd,flat-null,flat-tv,flat-audio,flat-dvd}");
			System.out.println("");
			System.out.println("@DATA");
			for(int j = 0; j < 59; j++) {
				Iterator iter = bidBundles.get("Schlemazl").get(j).iterator();
				while(iter.hasNext()) {
					Query query = (Query) iter.next();
					String adv = "Schlemazl";
					BidBundle bundle = bidBundles.get(adv).get(j);
					QueryReport qreport = queryReports.get(adv).get(j);
					SalesReport sreport = salesReports.get(adv).get(j);
					//bid
					System.out.print(bundle.getBid(query));
					//					//position
					//					if(Double.isNaN(qreport.getPosition(query))) {
					//						System.out.print(",6.0");
					//					}
					//					else {
					//						System.out.print("," + qreport.getPosition(query));
					//					}
					//CPC
					if(Double.isNaN(qreport.getCPC(query))) {
						System.out.print(",0.0");
					}
					else {
						System.out.print("," + qreport.getCPC(query));
					}
					//					//clickpr
					//					if(qreport.getClicks(query) == 0 || qreport.getImpressions(query) == 0) {
					//						System.out.print(",0.0");
					//					}
					//					else {
					//						System.out.print("," + (qreport.getClicks(query)/((double) qreport.getImpressions(query))));
					//					}
					//					//convPr
					//					if(qreport.getClicks(query) == 0 || sreport.getConversions(query) == 0) {
					//						System.out.print(",0.0");
					//					}
					//					else {
					//						System.out.print("," + (sreport.getConversions(query)/((double) qreport.getClicks(query))));
					//					}
					System.out.println("," + query.getManufacturer() +"-" + query.getComponent());
				}
			}
		}
	}


}
