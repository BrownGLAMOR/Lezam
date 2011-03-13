package simulator.parser;

import edu.umich.eecs.tac.props.*;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author jberg
 */
public class GameStatus {

   private String[] _advertisers;
   private HashMap<String, LinkedList<BankStatus>> _bankStatuses;
   private HashMap<String, LinkedList<BidBundle>> _bidBundles;
   private HashMap<String, LinkedList<QueryReport>> _queryReports;
   private HashMap<String, LinkedList<SalesReport>> _salesReports;
   private HashMap<String, AdvertiserInfo> _advertiserInfos;
   private LinkedList<HashMap<Product, HashMap<UserState, Integer>>> _userDistributions;
   private SlotInfo _slotInfo;
   private ReserveInfo _reserveInfo;
   private PublisherInfo _pubInfo;
   private RetailCatalog _retailCatalog;
   private UserClickModel _userClickModel;
   private Set<Query> _querySpace;

   public GameStatus(String[] advertisers,
                     HashMap<String, LinkedList<BankStatus>> bankStatuses,
                     HashMap<String, LinkedList<BidBundle>> bidBundles,
                     HashMap<String, LinkedList<QueryReport>> queryReports,
                     HashMap<String, LinkedList<SalesReport>> salesReports,
                     HashMap<String, AdvertiserInfo> advertiserInfos,
                     LinkedList<HashMap<Product, HashMap<UserState, Integer>>> userDists,
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
      _userDistributions = userDists;
      _slotInfo = slotInfo;
      _reserveInfo = reserveInfo;
      _pubInfo = pubInfo;
      _retailCatalog = retailCatalog;
      _userClickModel = userClickModel;

      _querySpace = new HashSet<Query>();

      // The query space is all the F0, F1, and F2 queries for each product
      // The F0 query class
      if (_retailCatalog.size() > 0) {
         _querySpace.add(new Query(null, null));
      }

      for (Product product : _retailCatalog) {
         // The F1 query classes
         // F1 Manufacturer only
         _querySpace.add(new Query(product.getManufacturer(), null));
         // F1 Component only
         _querySpace.add(new Query(null, product.getComponent()));

         // The F2 query class
         _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
      }
   }

   public String[] getAdvertisers() {
      return _advertisers;
   }

   public LinkedList<HashMap<Product, HashMap<UserState, Integer>>> getUserDistributions() {
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

   public Set<Query> getQuerySpace() {
      return _querySpace;
   }

}
