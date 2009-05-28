/**
 * Abstract agent class for agents that can be run in the simulator
 */
package agents;

import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import modelers.AbstractModel;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.PublisherInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;

/**
 * @author jberg
 *
 */
public abstract class SimAbstractAgent extends Agent {

	
	
    /**
     * Basic simulation information. {@link StartInfo} contains
     * <ul>
     * <li>simulation ID</li>
     * <li>simulation start time</li>
     * <li>simulation length in simulation days</li>
     * <li>actual seconds per simulation day</li>
     * </ul>
     * An agent should receive the {@link StartInfo} at the beginning of the game or during recovery.
     */
    private StartInfo _startInfo;

    /**
     * Basic auction slot information. {@link SlotInfo} contains
     * <ul>
     * <li>the number of regular slots</li>
     * <li>the number of promoted slots</li>
     * <li>promoted slot bonus</li>
     * </ul>
     * An agent should receive the {@link SlotInfo} at the beginning of the game or during recovery.
     * This information is identical for all auctions over all query classes.
     */
    protected SlotInfo _slotInfo;

    /**
     * The retail catalog. {@link RetailCatalog} contains
     * <ul>
     * <li>the product set</li>
     * <li>the sales profit per product</li>
     * <li>the manufacturer set</li>
     * <li>the component set</li>
     * </ul>
     * An agent should receive the {@link RetailCatalog} at the beginning of the game or during recovery.
     */
    protected RetailCatalog _retailCatalog;

    /**
     * The basic advertiser specific information. {@link AdvertiserInfo} contains
     * <ul>
     * <li>the manufacturer specialty</li>
     * <li>the component specialty</li>
     * <li>the manufacturer bonus</li>
     * <li>the component bonus</li>
     * <li>the distribution capacity discounter</li>
     * <li>the address of the publisher agent</li>
     * <li>the distribution capacity</li>
     * <li>the address of the advertiser agent</li>
     * <li>the distribution window</li>
     * <li>the target effect</li>
     * <li>the focus effects</li>
     * </ul>
     * An agent should receive the {@link AdvertiserInfo} at the beginning of the game or during recovery.
     */
    protected AdvertiserInfo _advertiserInfo;

    /**
     * The basic publisher information. {@link PublisherInfo} contains
     * <ul>
     * <li>the squashing parameter</li>
     * </ul>
     * An agent should receive the {@link PublisherInfo} at the beginning of the game or during recovery.
     */
    protected PublisherInfo _publisherInfo;

    /**
     * The list contains all of the {@link SalesReport sales report} delivered to the agent.  Each
     * {@link SalesReport sales report} contains the conversions and sales revenue accrued by the agent for each query
     * class during the period.
     */
    protected Queue<SalesReport> _salesReports;

    /**
     * The list contains all of the {@link QueryReport query reports} delivered to the agent.  Each
     * {@link QueryReport query report} contains the impressions, clicks, cost, average position, and ad displayed
     * by the agent for each query class during the period as well as the positions and displayed ads of all advertisers
     * during the period for each query class.
     */
    protected Queue<QueryReport> _queryReports;

    /**
     * List of all the possible queries made available in the {@link RetailCatalog retail catalog}.
     */
    protected Set<Query> _querySpace;
    protected Set<AbstractModel> _models;
    protected Hashtable<QueryType,Set<Query>> _queryFocus;
    protected Hashtable<String,Set<Query>> _queryComponent;
    protected Hashtable<String,Set<Query>> _queryManufacturer;
    protected Hashtable<String,Hashtable<String,Set<Query>>> _querySingleton;
    protected double _day;
	
	/**
	 * 
	 */
	public SimAbstractAgent() {
		_salesReports = new LinkedList<SalesReport>();
		_queryReports = new LinkedList<QueryReport>();
		_querySpace = new LinkedHashSet<Query>();
		
		_queryFocus = new Hashtable<QueryType, Set<Query>>();
		_queryManufacturer = new Hashtable<String, Set<Query>>();
		_queryComponent = new Hashtable<String, Set<Query>>();
		_querySingleton = new Hashtable<String, Hashtable<String, Set<Query>>>();

		_day = 0;
	}

    /**
     * Processes the messages received the by agent from the server.
     *
     * @param message the message
     */
    protected void messageReceived(Message message) {
        Transportable content = message.getContent();

        if (content instanceof QueryReport) {
            handleQueryReport((QueryReport) content);
        } else if (content instanceof SalesReport) {
            handleSalesReport((SalesReport) content);
        } else if (content instanceof SimulationStatus) {
            handleSimulationStatus((SimulationStatus) content);
        } else if (content instanceof PublisherInfo) {
            handlePublisherInfo((PublisherInfo) content);
        } else if (content instanceof SlotInfo) {
            handleSlotInfo((SlotInfo) content);
        } else if (content instanceof RetailCatalog) {
            handleRetailCatalog((RetailCatalog) content);
        } else if (content instanceof AdvertiserInfo) {
            handleAdvertiserInfo((AdvertiserInfo) content);
        } else if (content instanceof StartInfo) {
            handleStartInfo((StartInfo) content);
        }
        else {
        	throw new RuntimeException("received unexpected message: "+content);
        }
    }

    /**
     * Sends a constructed {@link BidBundle} from any updated bids, ads, or spend limits.
     */
    protected void sendBidAndAds() {
    	
    	SalesReport salesReport = _salesReports.poll();
    	QueryReport queryReport = _queryReports.poll();
    	
    	updateModels(salesReport, queryReport, _models);
    	
        BidBundle bidBundle = getBidBundle(_models);
        
        String publisherAddress = _advertiserInfo.getPublisherId();

        // Send the bid bundle to the publisher
        if (publisherAddress != null) {
            sendMessage(publisherAddress, bidBundle);
        }
    }


    /**
     * Processes an incoming query report.
     *
     * @param queryReport the daily query report.
     */
    protected void handleQueryReport(QueryReport queryReport) {
        _queryReports.add(queryReport);
    }

    
    /**
     * Processes an incoming sales report.
     *
     * @param salesReport the daily sales report.
     */
    protected void handleSalesReport(SalesReport salesReport) {
        _salesReports.add(salesReport);
    }

    /**
     * Processes a simulation status notification.  Each simulation day the {@link SimulationStatus simulation status }
     * notification is sent after the other daily messages ({@link QueryReport} {@link SalesReport} have been sent.
     *
     * @param simulationStatus the daily simulation status.
     */
    protected void handleSimulationStatus(SimulationStatus simulationStatus) {
        sendBidAndAds();
        _day += 1;
    }

    /**
     * Processes the publisher information.
     * @param publisherInfo the publisher information.
     */
    protected void handlePublisherInfo(PublisherInfo publisherInfo) {
        this._publisherInfo = publisherInfo;
    }

    /**
     * Processrs the slot information.
     * @param slotInfo the slot information.
     */
    protected void handleSlotInfo(SlotInfo slotInfo) {
        this._slotInfo = slotInfo;
    }

    /**
     * Processes the retail catalog.
     * @param retailCatalog the retail catalog.
     */
    protected void handleRetailCatalog(RetailCatalog retailCatalog) {
        this._retailCatalog = retailCatalog;

        // The query space is all the F0, F1, and F2 queries for each product
        // The F0 query class
        if(retailCatalog.size() > 0) {
            _querySpace.add(new Query(null, null));
        }

        for(Product product : retailCatalog) {
            // The F1 query classes
            // F1 Manufacturer only
            _querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            _querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
        }
    }

    /**
     * Processes the advertiser information.
     * @param advertiserInfo the advertiser information.
     */
    protected void handleAdvertiserInfo(AdvertiserInfo advertiserInfo) {
        this._advertiserInfo = advertiserInfo;
    }

    /**
     * Processes the start information.
     * @param startInfo the start information.
     */
    protected void handleStartInfo(StartInfo startInfo) {
        this._startInfo = startInfo;
    }

    /**
     * Prepares the agent for a new simulation.
     */
    protected void simulationSetup() {
    	initBidder();
    }

    /**
     * Runs any post-processes required for the agent after a simulation ends.
     */
    protected void simulationFinished() {
        _salesReports.clear();
        _queryReports.clear();
        _querySpace.clear();
        _day = 0;
    }
    
    /*
     * This method will be run once at the beginning of each simulation
     */
    protected abstract void initBidder();
    
    /*
     * This will be called once each day before getBidBundle to update all the models
     * that the agent needs to make a bid bundle
     */
    protected abstract Set<AbstractModel> updateModels(SalesReport salesReport,
    													QueryReport queryReport,
    													Set<AbstractModel> models);
    
    /*
     * This will be called once each day to get the bid bundle for the day, i.e. the bids,
     * budgets, and ad types
     */
    protected abstract BidBundle getBidBundle(Set<AbstractModel> models);


}
