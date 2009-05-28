package simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import edu.umich.eecs.tac.props.*;

import se.sics.isl.transport.Transportable;
import se.sics.isl.util.IllegalConfigurationException;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.props.*;
import simulator.parser.GameLogParser;
import simulator.parser.SimParserMessage;

public class test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParseException 
	 * @throws IllegalConfigurationException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, IllegalConfigurationException, ParseException {
		
		String filename = "/home/jordan/Desktop/localhost_sim6.slg";
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
	    int other = 0;
	    for(int i = 0; i < messages.size(); i++) {
	    	SimParserMessage mes = messages.get(i);
	    	Transportable content = mes.getContent();
	    	if (content instanceof BankStatus) {
	    		bankstatus++;
	    	}
	    	else if (content instanceof SimulationStatus) {
	    		simstatus++;
	    	}
	    	else if (content instanceof SlotInfo) {
	    		slotinfo++;
	    	}
	    	else if (content instanceof ReserveInfo) {
	    		reserveinfo++;
	    	}
	    	else if (content instanceof PublisherInfo) {
	    		pubinfo++;
	    	}
	    	else if (content instanceof SalesReport) {
	    		salesrep++;
	    	}
	    	else if (content instanceof QueryReport) {
	    		queryrep++;
	    	}
	    	else if (content instanceof RetailCatalog) {
	    		retailcatalog++;
	    	}
	    	else if (content instanceof BidBundle) {
	    		bidbundle++;
	    	}
	    	else if (content instanceof UserClickModel) {
	    		userclickmodel++;
	    	}
	    	else if (content instanceof AdvertiserInfo) {
	    		advinfo++;
	    	}
	    	else {
	    		other++;
	    	}
	    }
	    System.out.println("Bank Status: " + bankstatus);
	    System.out.println("Sim Status: " + simstatus);
	    System.out.println("Slot Info: " + slotinfo);
	    System.out.println("Reserver Info: " + reserveinfo);
	    System.out.println("Pub Info: " + pubinfo);
	    System.out.println("Advertiser Info: " + advinfo);
	    System.out.println("Bid Bundle: " + bidbundle);
	    System.out.println("Query Report: " + queryrep);
	    System.out.println("Retail Catalog: " + retailcatalog);
	    System.out.println("Sales Report: " + salesrep);
	    System.out.println("User Clikc Model: " + userclickmodel);
	    System.out.println("Other: " + other);
	}

}
