	package simulator.parser;

import java.util.HashMap;
import java.util.LinkedList;

import usermodel.UserState;

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

/**
*
* @author jberg
* 
*/
public class GameStatus {

	private String[] _advertisers;
	private HashMap<String, LinkedList<BankStatus>> _bankStatuses;
	private HashMap<String, LinkedList<BidBundle>> _bidBundles;
	private HashMap<String, LinkedList<QueryReport>> _queryReports;
	private HashMap<String, LinkedList<SalesReport>> _salesReports;
	private HashMap<String, AdvertiserInfo> _advertiserInfos;
	private LinkedList<HashMap<UserState, Double>> _userDistributions;
	private SlotInfo _slotInfo;
	private ReserveInfo _reserveInfo;
	private PublisherInfo _pubInfo;
	private RetailCatalog _retailCatalog;
	private UserClickModel _userClickModel;

	public GameStatus(String[] advertisers,
					HashMap<String, LinkedList<BankStatus>> bankStatuses,
					HashMap<String, LinkedList<BidBundle>> bidBundles,
					HashMap<String, LinkedList<QueryReport>> queryReports,
					HashMap<String, LinkedList<SalesReport>> salesReports,
					HashMap<String, AdvertiserInfo> advertiserInfos,
					LinkedList<HashMap<UserState,Double>> userDistributions,
					SlotInfo slotInfo,
					ReserveInfo reserveInfo,
					PublisherInfo pubInfo,
					RetailCatalog retailCatalog,
					UserClickModel userClickModel) {
		_advertisers = advertisers;
		_bankStatuses = bankStatuses;
		_bidBundles = bidBundles;
		_queryReports = queryReports;
		_salesReports = salesReports;
		_advertiserInfos = advertiserInfos;
		_userDistributions = userDistributions;
		_slotInfo = slotInfo;
		_reserveInfo = reserveInfo;
		_pubInfo = pubInfo;
		_retailCatalog = retailCatalog;
		_userClickModel = userClickModel;
	}

	public String[] getAdvertisers() {
		return _advertisers;
	}
	
	public LinkedList<HashMap<UserState, Double>> getUserDistributions() {
		return _userDistributions;
	}
	
	public HashMap<String, AdvertiserInfo> getAdvertiserInfos() {
		return _advertiserInfos;
	}


	public HashMap<String, LinkedList<BankStatus>> getBankStatuses() {
		return _bankStatuses;
	}

	public HashMap<String, LinkedList<BidBundle>> getBidBundles() {
		return _bidBundles;
	}

	public PublisherInfo getPubInfo() {
		return _pubInfo;
	}

	public HashMap<String, LinkedList<QueryReport>> getQueryReports() {
		return _queryReports;
	}

	public ReserveInfo getReserveInfo() {
		return _reserveInfo;
	}

	public RetailCatalog getRetailCatalog() {
		return _retailCatalog;
	}

	public HashMap<String, LinkedList<SalesReport>> getSalesReports() {
		return _salesReports;
	}

	public SlotInfo getSlotInfo() {
		return _slotInfo;
	}

	public UserClickModel getUserClickModel() {
		return _userClickModel;
	}
	
}
