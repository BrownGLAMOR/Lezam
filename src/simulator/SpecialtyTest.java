package simulator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.LinkedList;

import models.advertiserspecialties.SimpleSpecialtyModel;

import se.sics.isl.transport.Transportable;
import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.props.SimulationStatus;
import simulator.parser.GameLogParser;
import simulator.parser.SimParserMessage;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BankStatus;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.ReserveInfo;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import edu.umich.eecs.tac.props.UserClickModel;
import edu.umich.eecs.tac.props.UserPopulationState;
/*
 * 
 * 
 * Bank Status: 488
 * Sim Status: 488
 * Slot Info: 10
 * Reserver Info: 2
 * Pub Info: 8
 * Advertiser Info: 8
 * Bid Bundle: 480
 * Query Report: 480
 * Retail Catalog: 10
 * Sales Report: 488
 * User Click Model: 2
 */
public class SpecialtyTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 * @throws IllegalConfigurationException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, IllegalConfigurationException, ParseException {
		
		SimpleSpecialtyModel model = new SimpleSpecialtyModel();
		
		
		
		
//		String filename = "/Users/Jordan/Desktop/Class/CS2955/Server/ver0.9.5/logs/sims/localhost_sim51.slg";
		String filename = "/Users/sodomka/Desktop/game.slg";
		InputStream inputStream = new FileInputStream(filename);
    	GameLogParser parser = new GameLogParser(new LogReader(inputStream));
        parser.start();
        parser.stop();
	    LinkedList<SimParserMessage> messages = parser.getMessages();
	    System.out.println(messages.size());
	    
	    int bankstatus = 0;
	    int simstatus = 0;
	    int slotinfo = 0;
	    int reserveinfo = 0;
	    int pubinfo = 0;
	    int advinfo = 0;
	    int bidbundle = 0;
	    int queryrep = 0;
	    int retailcatalog = 0;
	    int salesrep = 0;
	    int userclickmodel = 0;
	    int userPop = 0;
	    int other = 0;
	    for(int i = 0; i < messages.size(); i++) {
	    	SimParserMessage mes = messages.get(i);
	    	int from = mes.getSender();
	    	int to = mes.getReceiver();
	    	Transportable content = mes.getContent();
	    	/*
	    	 * BANKSTATUS
	    	 * 
	    	 * getAccountBalance()
	    	 */
	    	if (content instanceof BankStatus) {
	    		//System.out.println(i+": from "+from+" to "+to+"  BankStatus");
	    		bankstatus++;
	    		BankStatus salesreport = (BankStatus) content;
	    		//System.out.println(salesreport);
	    	}
	    	/*
	    	 * SIMULATIONSTATUS
	    	 * 
	    	 * isSimulationEnded()
	    	 * getCurrentDate()
	    	 * getConsumedMillis()
	    	 */
	    	else if (content instanceof SimulationStatus) {
	    		//System.out.println(i+": from "+from+" to "+to+"  SimulationStatus"+ " day:"+((SimulationStatus)content).getCurrentDate());
	    		simstatus++;
	    	}
	    	/*
	    	 * SLOTINFO
	    	 * 
	    	 * getPromotedSlots()
	    	 * getRegularSlots()
	    	 * getPromotedSlotBonus()
	    	 */
	    	else if (content instanceof SlotInfo) {
	    		//System.out.println(i+": from "+from+" to "+to+"  SlotInfo");
	    		slotinfo++;
	    	}
	    	/*
	    	 * RESERVEINFO
	    	 * 
	    	 * getPromotedReserve()
	    	 * getRegularReserve()
	    	 */
	    	else if (content instanceof ReserveInfo) {
	    		//System.out.println(i+": from "+from+" to "+to+"  ReserveInfo");
	    		reserveinfo++;
	    	}
	    	/*
	    	 * PUBLISHERINFO
	    	 * 
	    	 * getSquashingParameter()
	    	 */
	    	else if (content instanceof PublisherInfo) {
	    		//System.out.println(i+": from "+from+" to "+to+"  PublisherInfo");
	    		pubinfo++;
	    	}
	    	/*
	    	 * SALESREPORT
	    	 * 
	    	 * getConversions(Query query)
	    	 * getConversions(int index)
	    	 * getRevenue(Query queue)
	    	 * getRevenue(int index)
	    	 * 
	    	 */
	    	else if (content instanceof SalesReport) {
	    		//System.out.println(i+": from "+from+" to "+to+"  SalesReport");
	    		salesrep++;
	    	}
	    	/*
	    	 * QUERYREPORT
	    	 * 
	    	 * getAd(Query query)
	    	 * getAd(int index)
	    	 * getAd(Query query, String advertiser)
	    	 * getAd(int index, String advertiser)
	    	 * getClicks(Query query)
	    	 * getClicks(int index)
	    	 * getCost(Query query)
	    	 * getCost(int index)
	    	 * getCPC(Query query)
	    	 * getCPC(int index)
	    	 * getPosition(Query query)
	    	 * getPosition(int index)
	    	 * getPosition(Query query, String advertiser)
	    	 * getPosition(int index, String advertiser)
	    	 * getImpressions(Query query)
	    	 * getImpressions(int index)
	    	 * getRegularImpressions(Query query)
	    	 * getRegularImpressions(int index)
	    	 * getPromotedImpressions(Query query)
	    	 * getPromotedImpressions(int index)
	    	 */
	    	else if (content instanceof QueryReport) {
	    		//System.out.println(i+": from "+from+" to "+to+"  QueryReport");
	    		queryrep++;
	    		
	    		
	    		
	    		QueryReport queryReport = (QueryReport) content;
	    		if (queryReport==null) continue;

	    		
	    		int OUR_AGENT_IDX = 3;
	    		
//	    		String OTHER_AGENT = "adv2";
//	    		if (to==OUR_AGENT_IDX)
//	    		for (int queryIdx=0; queryIdx<queryReport.size(); queryIdx++) {
//	    			Ad ad = queryReport.getAd(queryIdx,OTHER_AGENT);
//	    			System.out.println("query="+queryReport.getQuery(queryIdx) + " ad=" + ad);
//	    			if (ad != null && !ad.isGeneric()) System.out.println(" productForAd=" + ad.getProduct());
//	    		}	    		
	    		

	    		if (to==OUR_AGENT_IDX) {
	    			model.updateModel((QueryReport)content);
	    			model.printAllPredictions();
	    		}
	    		
	    	}
	    	/*
	    	 * RETAILCATALOG
	    	 * 
	    	 * getComponents()
	    	 * getManufacturers()
	    	 * getSalesProfit(Product product)
	    	 * getSalesProfit(int index)
	    	 */
	    	else if (content instanceof RetailCatalog) {
	    		//System.out.println(i+": from "+from+" to "+to+"  RetailCatalog");
	    		retailcatalog++;
	    	}
	    	/*
	    	 * BIDBUNDLE
	    	 * 
	    	 * getAd(Query query)
	    	 * getAd(int index)
	    	 * getBid(Query query)
	    	 * getBid(int index)
	    	 * getCampaignDailySpendLimit()
	    	 * getDailyLimit(Query query)
	    	 * getDailyLimit(int index)
	    	 */
	    	else if (content instanceof BidBundle) {
	    		//System.out.println(i+": from "+from+" to "+to+"  BidBundle");
	    		bidbundle++;
	    	}
	    	/*
	    	 * USERCLICKMODEL
	    	 * 
	    	 * advertiser(int index)
	    	 * advertiserCount()
	    	 * advertiserIndex(String advertiser)
	    	 * getAdvertiserEffect(int queryIndex, int advertiserIndex)
	    	 * getContinutationProbablitity(int queryIndex)
	    	 * query(int index)
	    	 * queryCount()
	    	 * queryIndex(Query query)
	    	 */
	    	else if (content instanceof UserClickModel) {
	    		//System.out.println(i+": from "+from+" to "+to+"  UserClickModel");
	    		userclickmodel++;
	    	}
	    	/*
	    	 * ADVERTISERINFO
	    	 * 
	    	 * getAdvertiserID()
	    	 * getComponentBonus()
	    	 * getComponentSpecialty()
	    	 * getDistirbutionCapacity()
	    	 * getDistributionCapacityDiscounter()
	    	 * getDistributionWindow()
	    	 * getFocusEffect(QueryType queryType)
	    	 * getManufacturerBonus()
	    	 * getManufacturerSpecialty()
	    	 * getPublisherId()
	    	 * getTargetEffect()
	    	 */
	    	else if (content instanceof AdvertiserInfo) {
	    		//System.out.println(i+": from "+from+" to "+to+"  AdvertiserInfo");
	    		AdvertiserInfo info = (AdvertiserInfo) content;
	    		System.out.println(" id=" + info.getAdvertiserId() + " comp=" + info.getComponentSpecialty() + " man=" + info.getManufacturerSpecialty());
	    		advinfo++;
	    	}
	    	else if (content instanceof UserPopulationState) {
	    		UserPopulationState userPopState = (UserPopulationState) content;
//	    		System.out.println(i+": from "+from+" to "+to+"  UserPopulationState");
//	    		for(Product p : userPop) {
//	    			System.out.println("\t"+p);
//	    			System.out.println("\t\t NS: "+userPop.getDistribution(p)[0]);
//	    			System.out.println("\t\t IS: "+userPop.getDistribution(p)[1]);
//	    			System.out.println("\t\t F0: "+userPop.getDistribution(p)[2]);
//	    			System.out.println("\t\t F1: "+userPop.getDistribution(p)[3]);
//	    			System.out.println("\t\t F2: "+userPop.getDistribution(p)[4]);
//	    			System.out.println("\t\t T: "+userPop.getDistribution(p)[5]);
//	    		}
	    		userPop++;
	    	}
	    	else {
	    		other++;
	    	}
	    }
//	    System.out.println("Bank Status: " + bankstatus);
//	    System.out.println("Sim Status: " + simstatus);
//	    System.out.println("Slot Info: " + slotinfo);
//	    System.out.println("Reserver Info: " + reserveinfo);
//	    System.out.println("Pub Info: " + pubinfo);
//	    System.out.println("Advertiser Info: " + advinfo);
//	    System.out.println("Bid Bundle: " + bidbundle);
//	    System.out.println("Query Report: " + queryrep);
//	    System.out.println("Retail Catalog: " + retailcatalog);
//	    System.out.println("Sales Report: " + salesrep);
//	    System.out.println("User Click Model: " + userclickmodel);
//	    System.out.println("User Population State: " + userPop);
//	    System.out.println("Other: " + other);
	}

}
