package agents.modelbased;

import agents.AbstractAgent;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;
import clojure.lang.PersistentHashMap;
import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.bidmodel.AbstractBidModel;
import models.bidmodel.IndependentBidModel;
import models.budgetEstimator.AbstractBudgetEstimator;
import models.budgetEstimator.BudgetEstimator;
import models.paramest.AbstractParameterEstimation;
import models.paramest.BayesianParameterEstimation;
import models.prconv.NewBasicConvPrModel;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.CarletonQueryAnalyzer;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.NewBasicQueryToNumImp;
import models.sales.SalesDistributionModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import models.usermodel.jbergParticleFilter;
import tacaa.javasim;

import java.util.*;

public class MCKP extends AbstractAgent {

   /*
     * TODO:
     *
     * 1) Predict opponent MSB and CSB
     * 2) Predict opponent ad type
     * 3) Dynamic or at least different capacity numbers
     */
   double[] _c;

   private boolean DEBUG = false;
   private Random _R;
   private boolean SAFETYBUDGET = false;
   private boolean BUDGET = false;
   private boolean FORWARDUPDATING = true;
   private boolean PRICELINES = true;
   private boolean UPDATE_WITH_ITEM = false;

   private double _safetyBudget = 950;
   private int lagDays = 2;

   private double[] _regReserveLow = {.08, .29, .46};
   private double[] _regReserveHigh = {.29, .46, .6};
   /*
     * FIXME:
     *
     * We are just assuming that the reserve is half way between max and min
     */
   private double[] _regReserve = {(_regReserveLow[0] + _regReserveHigh[0]) / 2.0,
           (_regReserveLow[1] + _regReserveHigh[1]) / 2.0,
           (_regReserveLow[2] + _regReserveHigh[2]) / 2.0};

   /*
     * FIXME
     *
     * We are just assuming that the pro reserve is 1/2 of the promoted boost
     * more than the regular reserve
     */
   private double _proReserveBoost = .5;
   private double[] _proReserve = {_regReserve[0] + _proReserveBoost * (1.0/2.0),
           _regReserve[1] + _proReserveBoost * (1.0/2.0),
           _regReserve[2] + _proReserveBoost * (1.0/2.0)};

   private HashMap<Query, Double> _baseConvProbs;
   private HashMap<Query, Double> _baseClickProbs;
   private HashMap<Query, Double> _salesPrices;

   private AbstractQueryAnalyzer _queryAnalyzer;
   private ParticleFilterAbstractUserModel _userModel;
   private AbstractQueryToNumImp _queryToNumImp;
   private AbstractUnitsSoldModel _unitsSold;
   private NewBasicConvPrModel _convPrModel;
   private AbstractBidModel _bidModel;
   private AbstractParameterEstimation _paramEstimation;
   private AbstractBudgetEstimator _budgetEstimator;
   private SalesDistributionModel _salesDist;
   private PersistentHashMap _baseCljSim;
   private PersistentHashMap _perfectCljSim = null;
   private String _agentToReplace;

   double[][] _advertiserEffectBounds;

   // Average advertiser effect
   double[] _advertiserEffectBoundsAvg;

   // Continuation Probability lower bound <> upper bound
   double[][] _continuationProbBounds;

   // Average continuation probability
   double[] _continuationProbBoundsAvg;

   // first index:
   // 0 - untargeted
   // 1 - targeted correctly
   // 2 - targeted incorrectly
   // second index:
   // 0 - not promoted
   // 1 - promoted
   double[][] fTargetfPro;


   public MCKP(String agentToReplace) {
      this();
      _agentToReplace = agentToReplace;
   }

   public MCKP(PersistentHashMap perfectSim, String agentToReplace) {
      this();
      _perfectCljSim = perfectSim;
      _agentToReplace = agentToReplace;
   }

   public MCKP() {
      this(0.10753988514063796,0.187966273,0.339007416);
   }

   public MCKP(double c1, double c2, double c3) {
      _R = new Random();
//		_R.setSeed(616866);
      _c = new double[3];
      _c[0] = c1;
      _c[1] = c2;
      _c[2] = c3;
   }

   public PersistentHashMap initClojureSim() {
      return javasim.initClojureSim(_publisherInfo,_slotInfo,_advertiserInfo,_retailCatalog,_advertisers);
   }

   public PersistentHashMap setupSimForDay() {
      if(_perfectCljSim == null) {
         HashMap<String,HashMap<Query,Double>> squashedBids = new HashMap<String, HashMap<Query, Double>>();
         HashMap<String,HashMap<Query,Double>> budgets = new HashMap<String, HashMap<Query, Double>>();
         HashMap<Product,double[]> userPop = new HashMap<Product, double[]>();
         HashMap<String,HashMap<Query,Double>> advAffects = new HashMap<String, HashMap<Query, Double>>();
         HashMap<Query,Double> contProbs = new HashMap<Query, Double>();
         HashMap<Query,Double> regReserves = new HashMap<Query, Double>();
         HashMap<Query,Double> promReserves = new HashMap<Query, Double>();
         HashMap<String,Integer> capacities = new HashMap<String, Integer>();
         HashMap<String,Integer> startSales = new HashMap<String, Integer>();
         HashMap<String,String> manSpecialties = new HashMap<String, String>();
         HashMap<String,String> compSpecialties = new HashMap<String, String>();
         HashMap<String,HashMap<Query,Ad>> ads = new HashMap<String, HashMap<Query, Ad>>();

         for(Query q : _querySpace) {
            contProbs.put(q,_paramEstimation.getContProbPrediction(q));
            regReserves.put(q,_regReserve[queryTypeToInt(q.getType())]);
            promReserves.put(q,_proReserve[queryTypeToInt(q.getType())]);
         }

         for(int i = 0; i < _advertisers.size(); i++) {
            String agent = _advertisers.get(i);
            if(i != _advIdx) {
               HashMap<Query,Double> aSquashedBids = new HashMap<Query, Double>();
               HashMap<Query,Double> aBudgets = new HashMap<Query, Double>();
               HashMap<Query,Double> aAdvAffects = new HashMap<Query, Double>();
               int aCapacities = 450;  //TODO estimate opponent capacity
               int aStartSales = (int)((4.0*(aCapacities / ((double) _capWindow)) + aCapacities) / 2.0);  //TODO Estimate opponent start-sales
               Query maxQuery = null;
               double maxBid = 0.0;
               for(Query q : _querySpace) {
                  double bid = _bidModel.getPrediction("adv" + (i+1), q);
                  aSquashedBids.put(q, bid);
                  aBudgets.put(q, _budgetEstimator.getBudgetEstimate(q, "adv" + (i+1)));
                  aAdvAffects.put(q,_advertiserEffectBoundsAvg[queryTypeToInt(q.getType())]);  //TODO estimate opponent advEffect
                  if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
                     if(bid >= maxBid) {
                        maxBid = bid;
                        maxQuery = q;
                     }
                  }
               }

               //Assume specialty is the prod of F2 query they are bidding most in
               String aManSpecialties = maxQuery.getManufacturer();
               String aCompSpecialties = maxQuery.getComponent();
               HashMap<Query,Ad> aAds = new HashMap<Query, Ad>();
               for(Query q : _querySpace) {
                  aAds.put(q,getTargetedAd(q,aManSpecialties,aCompSpecialties));
               }

               squashedBids.put(agent,aSquashedBids);
               budgets.put(agent,aBudgets);
               advAffects.put(agent,aAdvAffects);
               capacities.put(agent,aCapacities);
               startSales.put(agent,aStartSales);
               manSpecialties.put(agent,aManSpecialties);
               compSpecialties.put(agent,aCompSpecialties);
               ads.put(agent,aAds);
            }
            else {
               HashMap<Query,Double> aAdvAffects = new HashMap<Query, Double>();
               int aCapacities = _capacity;
//               double remainingCap;
//               if(_day < 4) {
//                  remainingCap = _capacity/((double)_capWindow);
//               }
//               else {
//                  remainingCap = _capacity - _unitsSold.getWindowSold();
//               }
//               int aStartSales = _capacity - (int)remainingCap;
               int aStartSales = (int)(_capacity - _capacity / ((double) _capWindow));

               String aManSpecialties = _manSpecialty;
               String aCompSpecialties = _compSpecialty;

               for(Query q : _querySpace) {
                  aAdvAffects.put(q,_paramEstimation.getAdvEffectPrediction(q));
               }

               advAffects.put(agent,aAdvAffects);
               capacities.put(agent,aCapacities);
               startSales.put(agent,aStartSales);
               manSpecialties.put(agent,aManSpecialties);
               compSpecialties.put(agent,aCompSpecialties);
            }
         }

         for(Product p : _products) {
            double[] userState = new double[UserState.values().length];
            userState[0] = _userModel.getPrediction(p, UserState.NS);
            userState[1] = _userModel.getPrediction(p, UserState.IS);
            userState[2] = _userModel.getPrediction(p, UserState.F0);
            userState[3] = _userModel.getPrediction(p, UserState.F1);
            userState[4] = _userModel.getPrediction(p, UserState.F2);
            userState[5] = _userModel.getPrediction(p, UserState.T);
            userPop.put(p, userState);
         }

         return javasim.mkFullStatus(_baseCljSim, squashedBids, budgets, userPop, advAffects, contProbs, regReserves,
                                     promReserves, capacities, startSales, manSpecialties, compSpecialties, ads);
      }
      else {
         return javasim.mkPerfectFullStatus(_perfectCljSim, (int)_day, _agentToReplace,(int)(_capacity - _capacity / ((double) _capWindow)));
      }
   }

   public double[] simulateQuery(PersistentHashMap cljSim, Query query, double bid, double budget, Ad ad) {
      ArrayList<Double> result;
      if(_perfectCljSim != null) {
         result = javasim.simQuery(cljSim,query,_agentToReplace,(int)_day,bid,budget,ad,1,true);
      }
      else {
         result = javasim.simQuery(cljSim,query,_advId,(int)_day,bid,budget,ad,1,false);
      }
      double[] resultArr = new double[result.size()];
      for(int i = 0; i < result.size(); i++) {
         resultArr[i] = result.get(i);
      }
      return resultArr;
   }

   @Override
   public void initBidder() {

      _advertiserEffectBounds = new double[][]{ { 0.2, 0.3 },
              { 0.3, 0.4 },
              { 0.4, 0.5 } };

      // Average advertiser effect
      _advertiserEffectBoundsAvg = new double[]{
              (_advertiserEffectBounds[0][0] + _advertiserEffectBounds[0][1]) / 2,
              (_advertiserEffectBounds[1][0] + _advertiserEffectBounds[1][1]) / 2,
              (_advertiserEffectBounds[2][0] + _advertiserEffectBounds[2][1]) / 2 };

      // Continuation Probability lower bound <> upper bound
      _continuationProbBounds = new double[][]{ { 0.2, 0.5 },
              { 0.3, 0.6 },
              { 0.4, 0.7 } };

      // Average continuation probability
      _continuationProbBoundsAvg = new double[]{
              (_continuationProbBounds[0][0] + _continuationProbBounds[0][1]) / 2,
              (_continuationProbBounds[1][0] + _continuationProbBounds[1][1]) / 2,
              (_continuationProbBounds[2][0] + _continuationProbBounds[2][1]) / 2 };

      // first index:
      // 0 - untargeted
      // 1 - targeted correctly
      // 2 - targeted incorrectly
      // second index:
      // 0 - not promoted
      // 1 - promoted
      fTargetfPro = new double[][]{ { (1.0), (1.0) * (1.0 + _PSB) },
              { (1.0 + _targEffect), (1.0 + _targEffect) * (1.0 + _PSB) },
              { (1.0) / (1.0 + _targEffect), ((1.0) / (1.0 + _targEffect)) * (1.0 + _PSB) } };

      _baseConvProbs = new HashMap<Query, Double>();
      _baseClickProbs = new HashMap<Query, Double>();
      _salesPrices = new HashMap<Query,Double>();

      for(Query q : _querySpace) {

         String manufacturer = q.getManufacturer();
         if(_manSpecialty.equals(manufacturer)) {
            _salesPrices.put(q, 10*(_MSB+1));
         }
         else if(manufacturer == null) {
            _salesPrices.put(q, (10*(_MSB+1)) * (1/3.0) + (10)*(2/3.0));
         }
         else {
            _salesPrices.put(q, 10.0);
         }

         if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
            _baseConvProbs.put(q, _piF0);
         }
         else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
            _baseConvProbs.put(q, _piF1);
         }
         else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
            _baseConvProbs.put(q, _piF2);
         }
         else {
            throw new RuntimeException("Malformed query");
         }

         /*
             * These are the MAX e_q^a (they are randomly generated), which is our clickPr for being in slot 1!
             *
             * Taken from the spec
             */

         /*
             * TODO
             *
             * we can consider replacing these with our predicted clickPrs
             *
             */
         if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
            _baseClickProbs.put(q, .3);
         }
         else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
            _baseClickProbs.put(q, .4);
         }
         else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
            _baseClickProbs.put(q, .5);
         }
         else {
            throw new RuntimeException("Malformed query");
         }
      }

      /*
       * Initialize Simulator
       */
      _baseCljSim = initClojureSim();
   }

   @Override
   public Set<AbstractModel> initModels() {
      Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
      /*
         * TODO
         *
         * re-tune all parameters on new data sets
         */
      _queryAnalyzer = new CarletonQueryAnalyzer(_querySpace,_advertisers,_advId,10,true,true);
      _userModel = new jbergParticleFilter(0.004932699,0.263532334,0.045700011,0.174371757,0.188113883,0.220140091);
      _queryToNumImp = new NewBasicQueryToNumImp(_userModel);
      _unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
      _convPrModel = new NewBasicConvPrModel(_userModel, _querySpace, _baseConvProbs);
      _bidModel = new IndependentBidModel(_advertisersSet, _advId,1,0,.8,.2,2.0);
      _paramEstimation = new BayesianParameterEstimation(_c,_advIdx,_numSlots, _numPS, _querySpace);
      _budgetEstimator = new BudgetEstimator(_querySpace,_advIdx,_numSlots,_numPS);
      _salesDist = new SalesDistributionModel(_querySpace);

      models.add(_queryAnalyzer);
      models.add(_userModel);
      models.add(_queryToNumImp);
      models.add(_unitsSold);
      models.add(_convPrModel);
      models.add(_bidModel);
      models.add(_paramEstimation);
      models.add(_budgetEstimator);
      models.add(_salesDist);
      return models;
   }

   @Override
   public BidBundle getBidBundle(Set<AbstractModel> models) {
      BidBundle bidBundle = new BidBundle();

      if(SAFETYBUDGET) {
         bidBundle.setCampaignDailySpendLimit(_safetyBudget);
      }
      else {
         bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);
      }

      if(_day >= lagDays){

         double remainingCap;
         if(_perfectStartSales == null) {
            if(_day < lagDays) {
               remainingCap = _capacity/((double)_capWindow);
            }
            else {
               remainingCap = _capacity - _unitsSold.getWindowSold();
               debug("Unit Sold Model Budget "  +remainingCap);
            }
         }
         else {
            remainingCap = _capacity;

            int saleslen = _perfectStartSales.length;
            for(int i = saleslen-1; i >= 0 && i > saleslen-_capWindow; i--) {
               remainingCap -= _perfectStartSales[i];
            }

            if(saleslen < (_capWindow-1)) {
               remainingCap -= _capacity/((double)_capWindow) * (_capacity - 1 - saleslen);
            }
         }

         debug("Budget: "+ remainingCap);

         HashMap<Query,ArrayList<Double>> bidLists = new HashMap<Query,ArrayList<Double>>();
         HashMap<Query,ArrayList<Double>> budgetLists = new HashMap<Query,ArrayList<Double>>();
         for(Query q : _querySpace) {
            if(!q.equals(new Query())) {
//               ArrayList<Double> bids = new ArrayList<Double>();
//               double unSquash = 1.0 / Math.pow(_paramEstimation.getPrediction(q)[0],_squashing);
//
//               for(int i = 0; i < _advertisers.size(); i++) {
//                  /*
//                  * We need to unsquash opponent bids
//                  */
//                  if(i != _advIdx) { //only care about opponent bids
//                     bids.add(_bidModel.getPrediction("adv" + (i+1), q) * unSquash);
//                  }
//               }
//
//               /*
//               * This sorts low to high
//               */
//               Collections.sort(bids);
//
//               ArrayList<Double> noDupeBids = removeDupes(bids);

               ArrayList<Double> newBids = new ArrayList<Double>();
//               int NUM_SAMPLES = 0;
//               for(int i = 0; i < noDupeBids.size(); i++) {
//                  newBids.add(noDupeBids.get(i) - .01);
//                  //					newBids.add(noDupeBids.get(i)); //TODO may want to include this since we requash
////                  newBids.add(noDupeBids.get(i) + .01);
//
//                  if((i == 0 && noDupeBids.size() > 1) || (i > 0 && i != noDupeBids.size()-1)) {
//                     for(int j = 1; j < NUM_SAMPLES+1; j++) {
//                        newBids.add(noDupeBids.get(i) + (noDupeBids.get(i+1) - noDupeBids.get(i)) * (j / ((double)(NUM_SAMPLES+1))));
//                     }
//                  }
//               }
               if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                  double increment  = .4;
                  double min = .08;
                  double max = 1.0;
                  int tot = (int) Math.ceil((max-min) / increment);
                  for(int i = 0; i < tot; i++) {
                     newBids.add(min+(i*increment));
                  }
               }
               else {
                  double increment  = .1;
                  double min = _regReserveLow[queryTypeToInt(q.getType())];
                  double max = _salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q);
                  int tot = (int) Math.ceil((max-min) / increment);
                  for(int i = 0; i < tot; i++) {
                     newBids.add(min+(i*increment));
                  }
               }

               Collections.sort(newBids);

//               System.out.println("Bids for " + q + ": " + newBids);
               bidLists.put(q, newBids);


               ArrayList<Double> budgetList = new ArrayList<Double>();
               budgetList.add(25.0);
//               budgetList.add(50.0);
               budgetList.add(75.0);
               budgetList.add(100.0);
//               budgetList.add(150.0);
               budgetList.add(200.0);
//               budgetList.add(250.0);
               budgetList.add(300.0);
//               budgetList.add(350.0);
//               budgetList.add(400.0);
//               budgetList.add(450.0);
               budgetList.add(500.0);

               budgetLists.put(q,budgetList);
            }
            else {
               bidLists.put(q,new ArrayList<Double>());
               budgetLists.put(q,new ArrayList<Double>());
            }
         }

         ArrayList<IncItem> allIncItems = new ArrayList<IncItem>();

         //want the queries to be in a guaranteed order - put them in an array
         //index will be used as the id of the query
         double penalty = getPenalty(remainingCap, 0);
         HashMap<Query,ArrayList<Predictions>> allPredictionsMap = new HashMap<Query, ArrayList<Predictions>>();
         PersistentHashMap querySim = setupSimForDay();
         for(Query q : _querySpace) {
            if(!q.equals(new Query())) {
               ArrayList<Item> itemList = new ArrayList<Item>();
               ArrayList<Predictions> queryPredictions = new ArrayList<Predictions>();
               debug("Knapsack Building Query: " + q);
               double convProbWithPen = getConversionPrWithPenalty(q, penalty);
               double convProb = _convPrModel.getPrediction(q);
               double salesPrice = _salesPrices.get(q);
               int itemCount = 0;
               for(int k = 0; k < 2; k++) {
//                  for(int k = 0; k < 2; k++) {
                  for(int i = 0; i < bidLists.get(q).size(); i++) {
                     for(int j = 0; j < budgetLists.get(q).size(); j++) {
                        boolean targeting = (k == 0) ? false : true;
                        double bid = bidLists.get(q).get(i);
                        double budget = budgetLists.get(q).get(j);
                        Ad ad = (k == 0) ? new Ad() : getTargetedAd(q);


                        double[] impsClicksAndCost = simulateQuery(querySim,q,bid,budget,ad);
                        double numImps = impsClicksAndCost[0];
                        double numClicks = impsClicksAndCost[1];
                        double cost = impsClicksAndCost[2];
                        double CPC = cost / numClicks;
                        double clickPr = numClicks / numImps;

//                        System.out.println("Bid: " + bid);
//                        System.out.println("Budget: " + budget);
//                        System.out.println("Targetting: " + targeting);
//                        System.out.println("numImps: " + numImps);
//                        System.out.println("numClicks: " + numClicks);
//                        System.out.println("cost: " + cost);
//                        System.out.println("CPC: " + CPC);
//                        System.out.println("clickPr: " + clickPr);
//                        System.out.println();

                        if(Double.isNaN(CPC)) {
                           CPC = 0.0;
                        }

                        if(Double.isNaN(clickPr)) {
                           clickPr = 0.0;
                        }

                        if(Double.isNaN(convProb)) {
                           convProb = 0.0;
                        }

                        debug("\tBid: " + bid);
                        debug("\tCPC: " + CPC);
                        debug("\tNumImps: " + numImps);
                        debug("\tNumClicks: " + numClicks);
                        debug("\tClickPr: " + clickPr);
                        debug("\tConv Prob: " + convProb + "\n\n");

                        double w = numClicks*convProbWithPen;				//weight = numClciks * convProv
                        double v = numClicks*convProbWithPen*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]
                        itemList.add(new Item(q,w,v,bid,budget,targeting,0,itemCount));
                        queryPredictions.add(new Predictions(clickPr, CPC, convProb, numImps));
                        itemCount++;

                        if(cost + bid*2 < budget) {
                           //If we don't hit our budget, we do not need to consider
                           //higher budgets, since we will have the same result
                           //so we break out of the budget loop
                           break;
                        }
                     }
                  }
               }
               debug("Items for " + q);
               if(itemList.size() > 0) {
                  Item[] items = itemList.toArray(new Item[0]);
                  IncItem[] iItems = getIncremental(items);
                  allIncItems.addAll(Arrays.asList(iItems));
                  allPredictionsMap.put(q, queryPredictions);
               }
            }
         }

         Collections.sort(allIncItems);
         HashMap<Query,Item> solution = fillKnapsackWithCapExt(allIncItems, remainingCap, allPredictionsMap);

         //set bids
         for(Query q : _querySpace) {
            ArrayList<Predictions> queryPrediction = allPredictionsMap.get(q);

            if(solution.containsKey(q)) {
               Item item = solution.get(q);
               double bid = item.b();
               double budget = item.budget();
               int idx = solution.get(q).idx();
               Predictions predictions = queryPrediction.get(idx);
               double clickPr = predictions.getClickPr();
               double numImps = predictions.getNumImp();
               int numClicks = (int) (clickPr * numImps);
               double CPC = predictions.getCPC();

               if(solution.get(q).targ()) {
                  bidBundle.setBid(q, bid);
                  bidBundle.setAd(q, getTargetedAd(q,_manSpecialty,_compSpecialty));
               }
               else {
                  bidBundle.addQuery(q, bid, new Ad());
               }

               if(BUDGET && budget == Double.MAX_VALUE) {
                  /*
                         * Only override the budget if the flag is set
                         * and we didn't choose to set a budget
                         */
                  bidBundle.setDailyLimit(q, numClicks*CPC);
               }
               else {
                  bidBundle.setDailyLimit(q, budget);
               }
            }
            else {
               /*
                     * We decided that we did not want to be in this query, so we will use it to explore the space
                     */
               double bid = randDouble(_regReserveLow[queryTypeToInt(q.getType())],_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .9);
               if(!q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                  //					System.out.println("Exploring " + q + "   bid: " + bid);
                  if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                     bidBundle.addQuery(q, bid, new Ad(), bid*3);
                  }
                  else {
                     bidBundle.addQuery(q, bid, new Ad(), bid*3);
                  }
               }
            }
         }

         /*
             * Pass expected conversions to unit sales model
             */

         double solutionWeight = solutionWeight(remainingCap,solution,allPredictionsMap);
         ((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
      }
      else {
         /*
             * Bound these with the reseve scores
             */
         for(Query q : _querySpace){
            if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
               double bid = randDouble(_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .35, _salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .85);
               bidBundle.addQuery(q, bid, new Ad(), Double.MAX_VALUE);
            }
            else {
               if(!q.equals(new Query())) {
                  double bid = randDouble(_regReserveLow[queryTypeToInt(q.getType())],_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .85);
                  bidBundle.addQuery(q, bid, new Ad(), bid*20);
               }
            }
         }
         bidBundle.setCampaignDailySpendLimit(925);
      }
      /*
         * Just in case...
         */
      for(Query q : _querySpace) {
         if(Double.isNaN(bidBundle.getBid(q)) || bidBundle.getBid(q) < 0) {
            bidBundle.setBid(q, 0.0);
         }
      }

//      System.out.println(bidBundle);
      return bidBundle;
   }

   // Turns a query type into 0/1/2
   int queryTypeToInt(QueryType qt) {
      if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return 0;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_ONE)) {
         return 1;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_TWO)) {
         return 2;
      }
      System.out.println("Error in queryTypeToInt");
      return 2;
   }

   private ArrayList<Double> removeDupes(ArrayList<Double> bids) {
      ArrayList<Double> noDupeList = new ArrayList<Double>();
      for(int i = 0; i < bids.size()-1; i++) {
         noDupeList.add(bids.get(i));
         while((i+1 < bids.size()-1) && (bids.get(i) == bids.get(i+1))) {
            i++;
         }
      }
      return noDupeList;
   }

   private Ad getTargetedAd(Query q) {
      return getTargetedAd(q, _manSpecialty, _compSpecialty);
   }

   private Ad getTargetedAd(Query q, String manSpecialty, String compSpecialty) {
      Ad ad;
      if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         /*
             * F0 Query, target our specialty
             */
         ad = new Ad(new Product(manSpecialty, compSpecialty));
      }
      else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
         if(q.getComponent() == null) {
            /*
                 * F1 Query (comp = null), so target the subgroup that searches for this and our
                 * component specialty
                 */
            ad = new Ad(new Product(q.getManufacturer(), compSpecialty));
         }
         else {
            /*
                 * F1 Query (man = null), so target the subgroup that searches for this and our
                 * manufacturer specialty
                 */
            ad = new Ad(new Product(manSpecialty, q.getComponent()));
         }
      }
      else  {
         /*
             * F2 Query, so target the subgroup that searches for this
             */
         ad = new Ad(new Product(q.getManufacturer(), q.getComponent()));
      }
      return ad;
   }

   @Override
   public void updateModels(SalesReport salesReport, QueryReport queryReport) {

//      System.out.println("Updating models on day " + _day);
      if(_perfectCljSim == null) {
         BidBundle bidBundle = _bidBundles.get(_bidBundles.size()-2);

         //TODO better upper bounds
         _maxImps = new HashMap<Query,Integer>();
         for(Query q : _querySpace) {
            int numImps;
            if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
               numImps = MAX_F0_IMPS;
            }
            else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
               numImps = MAX_F1_IMPS;
            }
            else {
               numImps = MAX_F2_IMPS;
            }
            _maxImps.put(q, numImps);
         }

         _queryAnalyzer.updateModel(queryReport, bidBundle, _maxImps);

         HashMap<Query,Integer> totalImpressions = new HashMap<Query,Integer>();
         HashMap<Query, HashMap<String, Integer>> ranks = new HashMap<Query,HashMap<String,Integer>>();
         HashMap<Query,int[]> fullOrders = new HashMap<Query,int[]>();
         HashMap<Query,int[]> fullImpressions = new HashMap<Query,int[]>();
         HashMap<Query,int[][]> fullWaterfalls = new HashMap<Query, int[][]>();
         for(Query q : _querySpace) {
            System.out.println("Query Analyzer Results for " + q);
            int[] impsPred = _queryAnalyzer.getImpressionsPrediction(q);
            int[] ranksPred = _queryAnalyzer.getOrderPrediction(q);
            int[][] waterfallPred = _queryAnalyzer.getImpressionRangePrediction(q);
            int totalImps = _queryAnalyzer.getTotImps(q);

            System.out.println("impsPred: " + Arrays.toString(impsPred));
            System.out.println("ranksPred: " + Arrays.toString(ranksPred));
            if(waterfallPred != null) {
               System.out.println("waterfall: ");
               for(int i = 0; i < waterfallPred.length; i++) {
                  System.out.println("\t" + Arrays.toString(waterfallPred[i]));
               }
            }
            else {
               System.out.println("waterfall: null");
            }

            if(totalImps == 0) {
               //this means something bad happened
               totalImps = -1;
            }

            fullOrders.put(q, ranksPred);
            fullImpressions.put(q, impsPred);
            fullWaterfalls.put(q,waterfallPred);
            totalImpressions.put(q, totalImps);

            HashMap<String, Integer> perQRanks = null;
            if(waterfallPred != null) {
               perQRanks = new HashMap<String,Integer>();
               for(int i = 0; i < _advertisers.size(); i++) {
                  perQRanks.put("adv" + (ranksPred[i] + 1),i);
               }
            }
            ranks.put(q, perQRanks);
            System.out.println("perQRanks: " + perQRanks);
         }

         _userModel.updateModel(totalImpressions);

         HashMap<Product,HashMap<UserState,Integer>> userStates = new HashMap<Product,HashMap<UserState,Integer>>();
         for(Product p : _products) {
            HashMap<UserState,Integer> userState = new HashMap<UserState,Integer>();
            for(UserState s : UserState.values()) {
               userState.put(s, _userModel.getCurrentEstimate(p, s));
            }
            userStates.put(p, userState);
         }

         _queryToNumImp.updateModel(queryReport, salesReport);
         _convPrModel.updateModel(queryReport, salesReport,bidBundle);

         _paramEstimation.updateModel(queryReport, bidBundle, fullOrders, fullImpressions, fullWaterfalls, userStates, _c);

         HashMap<Query, Double> cpc = new HashMap<Query,Double>();
         HashMap<Query, Double> ourBid = new HashMap<Query,Double>();
         for(Query q : _querySpace) {
            cpc.put(q, queryReport.getCPC(q)* Math.pow(_paramEstimation.getAdvEffectPrediction(q), _squashing));
            ourBid.put(q, bidBundle.getBid(q) * Math.pow(_paramEstimation.getAdvEffectPrediction(q), _squashing));
         }
         _bidModel.updateModel(cpc, ourBid, ranks);

         HashMap<Query,Double> contProbs = new HashMap<Query,Double>();
         HashMap<Query, double[]> allbids = new HashMap<Query,double[]>();
         for(Query q : _querySpace) {
            contProbs.put(q, _paramEstimation.getContProbPrediction(q));
            double[] bids = new double[_advertisers.size()];
            for(int j = 0; j < bids.length; j++) {
               if(j == _advIdx) {
                  bids[j] = bidBundle.getBid(q) * Math.pow(_paramEstimation.getAdvEffectPrediction(q), _squashing);
               }
               else {
                  bids[j] = _bidModel.getPrediction("adv" + (j+1), q);
               }
            }
            allbids.put(q, bids);
         }

         _budgetEstimator.updateModel(queryReport, bidBundle, _c, contProbs, fullOrders, fullImpressions, fullWaterfalls, allbids, userStates);

         _salesDist.updateModel(salesReport);
         _unitsSold.update(salesReport);
      }
   }

   public double getPenalty(double remainingCap, double solutionWeight) {
      return getPenalty(remainingCap,solutionWeight,_lambda);
   }

   public static double getPenalty(double remainingCap, double solutionWeight, double lambda) {
      double penalty;
      solutionWeight = Math.max(0,solutionWeight);
      if(remainingCap < 0) {
         if(solutionWeight <= 0) {
            penalty = Math.pow(lambda, Math.abs(remainingCap));
         }
         else {
            penalty = 0.0;
            int num = 0;
            for(double j = Math.abs(remainingCap)+1; j <= Math.abs(remainingCap)+solutionWeight; j++) {
               penalty += Math.pow(lambda, j);
               num++;
            }
            penalty /= (num);
         }
      }
      else {
         if(solutionWeight <= 0) {
            penalty = 1.0;
         }
         else {
            if(solutionWeight > remainingCap) {
               penalty = remainingCap;
               for(int j = 1; j <= solutionWeight-remainingCap; j++) {
                  penalty += Math.pow(lambda, j);
               }
               penalty /= (solutionWeight);
            }
            else {
               penalty = 1.0;
            }
         }
      }
      if(Double.isNaN(penalty)) {
         penalty = 1.0;
      }
      return penalty;
   }

   private double[] solutionValueMultiDay(HashMap<Query, Item> solution, double remainingCap, HashMap<Query, ArrayList<Predictions>> allPredictionsMap, int numDays) {
      double totalWeight = solutionWeight(remainingCap, solution, allPredictionsMap);
      double penalty = getPenalty(remainingCap, totalWeight);

      double totalValue = 0;
      for(Query q : _querySpace) {
         if(solution.containsKey(q)) {
            Item item = solution.get(q);
            Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
            totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penalty)*_salesPrices.get(item.q()) - prediction.getCPC());
         }
      }

      double daysLookahead = Math.max(0, Math.min(numDays, 58 - _day));
      if(daysLookahead > 0 && totalWeight > 0) {
         ArrayList<Integer> soldArray;
         if(_perfectStartSales == null) {
            ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
            soldArray = getCopy(soldArrayTMP);

            Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
            if(expectedConvsYesterday == null) {
               expectedConvsYesterday = 0;
               int counter2 = 0;
               for(int j = 0; j < 5 && j < soldArray.size(); j++) {
                  expectedConvsYesterday += soldArray.get(soldArray.size()-1-j);
                  counter2++;
               }
               expectedConvsYesterday = (int) (expectedConvsYesterday / (double) counter2);
            }
            soldArray.add(expectedConvsYesterday);
         }
         else {
            soldArray = new ArrayList<Integer>(_perfectStartSales.length);

            if(_perfectStartSales.length < (_capWindow-1)) {
               for(int i = 0; i < (_capWindow - 1 - _perfectStartSales.length); i++) {
                  soldArray.add((int)(_capacity / ((double) _capWindow)));
               }
            }

            for(Integer numConvs : _perfectStartSales) {
               soldArray.add(numConvs);
            }
         }
         soldArray.add((int) totalWeight);

         for(int i = 0; i < daysLookahead; i++) {
            double expectedBudget = _capacity;
            for(int j = 0; j < _capWindow-1; j++) {
               expectedBudget -= soldArray.get(soldArray.size()-1-j);
            }

            double numSales = solutionWeight(expectedBudget, solution, allPredictionsMap);
            soldArray.add((int) numSales);

            double penaltyNew = getPenalty(expectedBudget, numSales);
            for(Query q : _querySpace) {
               if(solution.containsKey(q)) {
                  Item item = solution.get(q);
                  Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
                  totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penaltyNew)*_salesPrices.get(item.q()) - prediction.getCPC());
               }
            }
         }
      }
      double[] output = new double[2];
      output[0] = totalValue;
      output[1] = totalWeight;
      return output;
   }

   private ArrayList<Integer> getCopy(ArrayList<Integer> soldArrayTMP) {
      ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP.size());
      for(int i = 0; i < soldArrayTMP.size(); i++) {
         soldArray.add(soldArrayTMP.get(i));
      }
      return soldArray;
   }


   private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap, BidBundle bidBundle) {
      double threshold = .5;
      int maxIters = 40;
      double lastSolWeight = Double.MAX_VALUE;
      double solutionWeight = 0.0;

      /*
         * As a first estimate use the weight of the solution
         * with no penalty
         */
      for(Query q : _querySpace) {
         if(solution.get(q) == null) {
            continue;
         }
         Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
         double dailyLimit = Double.NaN;
         if(bidBundle != null) {
            dailyLimit  = bidBundle.getDailyLimit(q);
         }
         double clickPr = predictions.getClickPr();
         double numImps = predictions.getNumImp();
         int numClicks = (int) (clickPr * numImps);
         double CPC = predictions.getCPC();
         double convProb = getConversionPrWithPenalty(q, 1.0);

         if(Double.isNaN(CPC)) {
            CPC = 0.0;
         }

         if(Double.isNaN(clickPr)) {
            clickPr = 0.0;
         }

         if(Double.isNaN(convProb)) {
            convProb = 0.0;
         }

         if(!Double.isNaN(dailyLimit)) {
            if(numClicks*CPC > dailyLimit) {
               numClicks = (int) (dailyLimit/CPC);
            }
         }

         solutionWeight += numClicks*convProb;
      }

      double originalSolWeight = solutionWeight;

      int numIters = 0;
      while(Math.abs(lastSolWeight-solutionWeight) > threshold) {
         numIters++;
         if(numIters > maxIters) {
            numIters = 0;
            solutionWeight = (_R.nextDouble() + .5) * originalSolWeight; //restart the search
            threshold *= 1.5; //increase the threshold
            maxIters *= 1.25;
         }
         lastSolWeight = solutionWeight;
         solutionWeight = 0;
         double penalty = getPenalty(budget, lastSolWeight);
         for(Query q : _querySpace) {
            if(solution.get(q) == null) {
               continue;
            }
            Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
            double dailyLimit = Double.NaN;
            if(bidBundle != null) {
               dailyLimit  = bidBundle.getDailyLimit(q);
            }
            double clickPr = predictions.getClickPr();
            double numImps = predictions.getNumImp();
            int numClicks = (int) (clickPr * numImps);
            double CPC = predictions.getCPC();
            double convProb = getConversionPrWithPenalty(q, penalty);

            if(Double.isNaN(CPC)) {
               CPC = 0.0;
            }

            if(Double.isNaN(clickPr)) {
               clickPr = 0.0;
            }

            if(Double.isNaN(convProb)) {
               convProb = 0.0;
            }

            if(!Double.isNaN(dailyLimit)) {
               if(numClicks*CPC > dailyLimit) {
                  numClicks = (int) (dailyLimit/CPC);
               }
            }

            solutionWeight += numClicks*convProb;
         }
      }
      return solutionWeight;
   }

   private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) {
      return solutionWeight(budget, solution, allPredictionsMap, null);
   }


   private HashMap<Query,Item> fillKnapsackWithCapExt(ArrayList<IncItem> incItems, double budget, HashMap<Query,ArrayList<Predictions>> allPredictionsMap){
      HashMap<Query,Item> solution = new HashMap<Query, Item>();

      int expectedConvs = 0;

      for(int i = 0; i < incItems.size(); i++) {
         IncItem ii = incItems.get(i);
         double itemWeight = ii.w();
         //			double itemValue = ii.v();
         if(budget >= expectedConvs + itemWeight) {
            solution.put(ii.item().q(), ii.item());
            expectedConvs += itemWeight;
         }
         else {
            double[] currSolVal = solutionValueMultiDay(solution, budget, allPredictionsMap, 20);

            HashMap<Query, Item> solutionCopy = (HashMap<Query, Item>)solution.clone();
            solutionCopy.put(ii.item().q(), ii.item());
            double[] newSolVal = solutionValueMultiDay(solutionCopy, budget, allPredictionsMap, 20);

            //				System.out.println("[" + _day +"] CurrSolVal: " + currSolVal[0] + ", NewSolVal: " + newSolVal[0]);

            if(newSolVal[0] > currSolVal[0]) {
               solution.put(ii.item().q(), ii.item());
               expectedConvs = (int) newSolVal[1];

               if(i != incItems.size() - 1) {
                  /*
                         * Discount the item based on the current penalty level
                         */
                  double penalty = getPenalty(budget, newSolVal[1]);

                  if(FORWARDUPDATING && !PRICELINES) {
                     //Update next item
                     IncItem nextItem  = incItems.get(i+1);
                     double v,w;
                     if(nextItem.itemLow() != null) {
                        Predictions prediction1 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemLow().idx());
                        Predictions prediction2 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
                        v = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction2.getCPC()) -
                                (prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction1.getCPC())) ;
                        w = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty) -
                                (prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty));
                     }
                     else {
                        Predictions prediction = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
                        v = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty)*_salesPrices.get(nextItem.item().q()) - prediction.getCPC());
                        w = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty);
                     }
                     IncItem newNextItem = new IncItem(w, v, nextItem.itemHigh(), nextItem.itemLow());
                     incItems.remove(i+1);
                     incItems.add(i+1, newNextItem);
                  }
                  else if(PRICELINES) {
                     ArrayList<IncItem> updatedItems = new ArrayList<IncItem>();
                     for(int j = i+1; j < incItems.size(); j++) {
                        IncItem incItem = incItems.get(j);
                        Item itemLow = incItem.itemLow();
                        Item itemHigh = incItem.itemHigh();

                        double newPenalty;
                        if(UPDATE_WITH_ITEM) {
                           HashMap<Query, Item> solutionInnerCopy = (HashMap<Query, Item>)solutionCopy.clone();
                           solutionInnerCopy.put(incItem.item().q(), incItem.item());
                           double solWeight = solutionWeight(budget, solutionInnerCopy, allPredictionsMap);
                           newPenalty = getPenalty(budget, solWeight);
                        }
                        else {
                           newPenalty = penalty;
                        }

                        double newWeight,newValue;

                        if(itemLow != null) {
                           Predictions prediction1 = allPredictionsMap.get(itemHigh.q()).get(itemLow.idx());
                           Predictions prediction2 = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
                           newValue = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty)*_salesPrices.get(itemHigh.q()) - prediction2.getCPC()) -
                                   (prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty)*_salesPrices.get(itemHigh.q()) - prediction1.getCPC())) ;
                           newWeight = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty) -
                                   (prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty));
                        }
                        else {
                           Predictions prediction = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
                           newValue = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty)*_salesPrices.get(itemHigh.q()) - prediction.getCPC());
                           newWeight = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty);
                        }
                        IncItem newItem = new IncItem(newWeight,newValue,itemHigh,itemLow);
                        updatedItems.add(newItem);
                     }

                     Collections.sort(updatedItems);

                     while(incItems.size() > i+1) {
                        incItems.remove(incItems.size()-1);
                     }
                     for(IncItem priceLineItem : updatedItems) {
                        incItems.add(incItems.size(),priceLineItem);
                     }
                  }
               }
            }
            else {
               solution.put(ii.item().q(), ii.item());
               break;
            }
         }
      }
      return solution;
   }

   private HashMap<Query,Item> fillKnapsackTacTex(ArrayList<IncItem> incItems, double budget, HashMap<Query,ArrayList<Predictions>> allPredictionsMap){
      return null;
   }

   /**
    * Get undominated items
    * @param items
    * @return
    */
   public static Item[] getUndominated(Item[] items) {
      Arrays.sort(items,new ItemComparatorByWeight());
      //remove dominated items (higher weight, lower value)
      ArrayList<Item> temp = new ArrayList<Item>();
      temp.add(items[0]);
      for(int i=1; i<items.length; i++) {
         Item lastUndominated = temp.get(temp.size()-1);
         if(lastUndominated.v() < items[i].v()) {
            temp.add(items[i]);
         }
      }


      ArrayList<Item> betterTemp = new ArrayList<Item>();
      betterTemp.addAll(temp);
      for(int i = 0; i < temp.size(); i++) {
         ArrayList<Item> duplicates = new ArrayList<Item>();
         Item item = temp.get(i);
         duplicates.add(item);
         for(int j = i + 1; j < temp.size(); j++) {
            Item otherItem = temp.get(j);
            if(item.v() == otherItem.v() && item.w() == otherItem.w()) {
               duplicates.add(otherItem);
            }
         }
         if(duplicates.size() > 1) {
            betterTemp.removeAll(duplicates);
            double minBid = 10;
            double maxBid = -10;
            for(int j = 0; j < duplicates.size(); j++) {
               double bid = duplicates.get(j).b();
               if(bid > maxBid) {
                  maxBid = bid;
               }
               if(bid < minBid) {
                  minBid = bid;
               }
            }
            Item newItem = new Item(item.q(), item.w(), item.v(), (maxBid+minBid)/2.0, item.targ(), item.isID(),item.idx());
            betterTemp.add(newItem);
         }
      }

      //items now contain only undominated items
      items = betterTemp.toArray(new Item[0]);
      Arrays.sort(items,new ItemComparatorByWeight());

      //remove lp-dominated items
      ArrayList<Item> q = new ArrayList<Item>();
      q.add(new Item(new Query(),0,0,-1,false,1,0));//add item with zero weight and value

      for(int i=0; i<items.length; i++) {
         q.add(items[i]);//has at least 2 items now
         int l = q.size()-1;
         Item li = q.get(l);//last item
         Item nli = q.get(l-1);//next to last
         if(li.w() == nli.w()) {
            if(li.v() > nli.v()) {
               q.remove(l-1);
            }else{
               q.remove(l);
            }
         }
         l = q.size()-1; //reset in case an item was removed
         //while there are at least three elements and ...
         while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w())
                 <= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
            q.remove(l-1);
            l--;
         }
      }

      //remove the (0,0) item
      if(q.get(0).w() == 0 && q.get(0).v() == 0) {
         q.remove(0);
      }

      Item[] uItems = q.toArray(new Item[0]);
      return uItems;
   }


   /**
    * Get incremental items
    * @param items
    * @return
    */
   public IncItem[] getIncremental(Item[] items) {
      for(int i = 0; i < items.length; i++) {
         debug("\t" + items[i]);
      }

      Item[] uItems = getUndominated(items);

      debug("UNDOMINATED");
      for(int i = 0; i < uItems.length; i++) {
         debug("\t" + uItems[i]);
      }

      IncItem[] ii = new IncItem[uItems.length];

      if (uItems.length != 0){ //getUndominated can return an empty array
         ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0], null);
         for(int item=1; item<uItems.length; item++) {
            Item prev = uItems[item-1];
            Item cur = uItems[item];
            ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur, prev);
         }
      }
      debug("INCREMENTAL");
      for(int i = 0; i < ii.length; i++) {
         debug("\t" + ii[i]);
      }
      return ii;
   }

   public double getConversionPrWithPenalty(Query q, double penalty) {
      double convPr;
      String component = q.getComponent();
      double pred = _convPrModel.getPrediction(q);
      if(_compSpecialty.equals(component)) {
         convPr = eta(pred*penalty,1+_CSB);
      }
      else if(component == null) {
         convPr = eta(pred*penalty,1+_CSB) * (1/3.0) + pred*penalty*(2/3.0);
      }
      else {
         convPr = pred*penalty;
      }
      return convPr;
   }

   // returns the corresponding index for the targeting part of fTargetfPro
   private int getFTargetIndex(boolean targeted, Product p, Product target) {
      if (!targeted || p == null || target == null) {
         return 0; //untargeted
      }
      else if(p.equals(target)) {
         return 1; //targeted correctly
      }
      else {
         return 2; //targeted incorrectly
      }
   }

   private double randDouble(double a, double b) {
      double rand = _R.nextDouble();
      return rand * (b - a) + a;
   }

   public void debug(Object str) {
      if(DEBUG) {
         System.out.println(str);
      }
   }

   @Override
   public String toString() {
      return "MCKP";
   }

   @Override
   public AbstractAgent getCopy() {
      return new MCKP(_c[0],_c[1],_c[2]);
   }
}
