package agents.modelbased;

import agents.AbstractAgent;
import agents.modelbased.mckputil.DrIncItem;
import agents.modelbased.mckputil.DrItem;
import agents.modelbased.mckputil.Item;
import agents.modelbased.mckputil.ItemComparatorByWeight;
import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.bidtocpc.AbstractBidToCPC;
import models.bidtocpc.WEKAEnsembleBidToCPC;
import models.bidtoprclick.AbstractBidToPrClick;
import models.bidtoprclick.WEKAEnsembleBidToPrClick;
import models.prconv.AbstractConversionModel;
import models.prconv.GoodConversionPrModel;
import models.prconv.HistoricPrConversionModel;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.BasicQueryToNumImp;
import models.sales.SalesDistributionModel;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.usermodel.AbstractUserModel;
import models.usermodel.BasicUserModel;

import java.util.*;

/**
 * @author jberg, spucci, vnarodit
 */
public class DrMCKPBid extends AbstractAgent {

   private static final int MAX_TIME_HORIZON = 5;
   private static final boolean TARGET = false;
   private static final boolean BUDGET = false;
   private static final boolean SAFETYBUDGET = true;

   private double _safetyBudget = 800;

   private Random _R = new Random();
   private boolean DEBUG = false;
   private HashMap<Query, Double> _salesPrices;
   private HashMap<Query, Double> _baseConvProbs;
   private HashMap<Query, Double> _baseClickProbs;
   private AbstractUserModel _userModel;
   private AbstractQueryToNumImp _queryToNumImpModel;
   private AbstractBidToCPC _bidToCPC;
   private AbstractBidToPrClick _bidToPrClick;
   private AbstractUnitsSoldModel _unitsSold;
   private AbstractConversionModel _convPrModel;
   private SalesDistributionModel _salesDist;
   private BasicTargetModel _targModel;
   private Hashtable<Query, Integer> _queryId;
   private LinkedList<Double> bidList;
   private int lagDays = 5;
   private boolean salesDistFlag;
   private int _numMultiday;

   public DrMCKPBid(int numMultiday) {
      _R.setSeed(124962748);
      bidList = new LinkedList<Double>();
      //		double increment = .25;
      double increment = .04;
      double min = .04;
      double max = 1.65;
      int tot = (int) Math.ceil((max - min) / increment);
      for (int i = 0; i < tot; i++) {
         bidList.add(min + (i * increment));
      }
      salesDistFlag = false;
      _numMultiday = numMultiday;
   }


   @Override
   public Set<AbstractModel> initModels() {
      /*
         * Order is important because some of our models use other models
         * so we use a LinkedHashSet
         */
      Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
      AbstractUserModel userModel = new BasicUserModel();
      AbstractQueryToNumImp queryToNumImp = new BasicQueryToNumImp(userModel);
      AbstractUnitsSoldModel unitsSold = new BasicUnitsSoldModel(_querySpace, _capacity, _capWindow);
      BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty, _compSpecialty);
      AbstractBidToCPC bidToCPC = new WEKAEnsembleBidToCPC(_querySpace, 10, 10, true, false);
      AbstractBidToPrClick bidToPrClick = new WEKAEnsembleBidToPrClick(_querySpace, 10, 10, basicTargModel, true, true);
      GoodConversionPrModel convPrModel = new GoodConversionPrModel(_querySpace, basicTargModel);
      models.add(userModel);
      models.add(queryToNumImp);
      models.add(bidToCPC);
      models.add(bidToPrClick);
      models.add(unitsSold);
      models.add(convPrModel);
      models.add(basicTargModel);
      return models;
   }

   protected void buildMaps(Set<AbstractModel> models) {
      for (AbstractModel model : models) {
         if (model instanceof AbstractUserModel) {
            AbstractUserModel userModel = (AbstractUserModel) model;
            _userModel = userModel;
         } else if (model instanceof AbstractQueryToNumImp) {
            AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
            _queryToNumImpModel = queryToNumImp;
         } else if (model instanceof AbstractUnitsSoldModel) {
            AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
            _unitsSold = unitsSold;
         } else if (model instanceof AbstractBidToCPC) {
            AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
            _bidToCPC = bidToCPC;
         } else if (model instanceof AbstractBidToPrClick) {
            AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
            _bidToPrClick = bidToPrClick;
         } else if (model instanceof AbstractConversionModel) {
            AbstractConversionModel convPrModel = (AbstractConversionModel) model;
            _convPrModel = convPrModel;
         } else if (model instanceof BasicTargetModel) {
            BasicTargetModel targModel = (BasicTargetModel) model;
            _targModel = targModel;
         } else {
            //				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)"+model);
         }
      }
   }

   @Override
   public void initBidder() {

      _baseConvProbs = new HashMap<Query, Double>();
      _baseClickProbs = new HashMap<Query, Double>();

      // set revenue prices
      _salesPrices = new HashMap<Query, Double>();
      for (Query q : _querySpace) {

         String manufacturer = q.getManufacturer();
         if (_manSpecialty.equals(manufacturer)) {
            _salesPrices.put(q, 10 * (_MSB + 1));
         } else if (manufacturer == null) {
            _salesPrices.put(q, (10 * (_MSB + 1)) * (1 / 3.0) + (10) * (2 / 3.0));
         } else {
            _salesPrices.put(q, 10.0);
         }

         if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
            _baseConvProbs.put(q, _piF0);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
            _baseConvProbs.put(q, _piF1);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
            _baseConvProbs.put(q, _piF2);
         } else {
            throw new RuntimeException("Malformed query");
         }

         /*
             * These are the MAX e_q^a (they are randomly generated), which is our clickPr for being in slot 1!
             *
             * Taken from the spec
             */

         if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
            _baseClickProbs.put(q, .3);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
            _baseClickProbs.put(q, .4);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
            _baseClickProbs.put(q, .5);
         } else {
            throw new RuntimeException("Malformed query");
         }

         String component = q.getComponent();
         if (_compSpecialty.equals(component)) {
            _baseConvProbs.put(q, eta(_baseConvProbs.get(q), 1 + _CSB));
         } else if (component == null) {
            _baseConvProbs.put(q, eta(_baseConvProbs.get(q), 1 + _CSB) * (1 / 3.0) + _baseConvProbs.get(q) * (2 / 3.0));
         }
      }

      _queryId = new Hashtable<Query, Integer>();
      int i = 0;
      for (Query q : _querySpace) {
         i++;
         _queryId.put(q, i);
      }
   }


   @Override
   public void updateModels(SalesReport salesReport, QueryReport queryReport) {

      for (AbstractModel model : _models) {
         if (model instanceof AbstractUserModel) {
            AbstractUserModel userModel = (AbstractUserModel) model;
            userModel.updateModel(queryReport, salesReport);
         } else if (model instanceof AbstractQueryToNumImp) {
            AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
            queryToNumImp.updateModel(queryReport, salesReport);
         } else if (model instanceof AbstractUnitsSoldModel) {
            AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
            unitsSold.update(salesReport);
         } else if (model instanceof AbstractBidToCPC) {
            AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
            bidToCPC.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
         } else if (model instanceof AbstractBidToPrClick) {
            AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
            bidToPrClick.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
         } else if (model instanceof AbstractConversionModel) {
            AbstractConversionModel convPrModel = (AbstractConversionModel) model;
            int timeHorizon = (int) Math.min(Math.max(1, _day - 1), MAX_TIME_HORIZON);
            if (model instanceof GoodConversionPrModel) {
               GoodConversionPrModel adMaxModel = (GoodConversionPrModel) convPrModel;
               adMaxModel.setTimeHorizon(timeHorizon);
            }
            if (model instanceof HistoricPrConversionModel) {
               HistoricPrConversionModel adMaxModel = (HistoricPrConversionModel) convPrModel;
               adMaxModel.setTimeHorizon(timeHorizon);
            }
            convPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
         } else if (model instanceof BasicTargetModel) {
            //Do nothing
         } else {
            //				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)");
         }
      }
   }


   @Override
   public BidBundle getBidBundle(Set<AbstractModel> models) {
      double start = System.currentTimeMillis();
      BidBundle bidBundle = new BidBundle();

      if (SAFETYBUDGET) {
         bidBundle.setCampaignDailySpendLimit(_safetyBudget);
      }

      if (_day > 1) {
         if (!salesDistFlag) {
            SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
            _salesDist = salesDist;
            salesDistFlag = true;
         }
         _salesDist.updateModel(_salesReport);
      }

      if (_day > lagDays) {
         buildMaps(models);
         double budget = _capacity / _capWindow;
         if (_day < 4) {
            //do nothing
         } else {
            //				budget = Math.max(20,_capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4);
            budget = _capacity - _unitsSold.getWindowSold();
            debug("Unit Sold Model Budget " + budget);
         }

         int realNumDays = (int) Math.max(1, Math.min(59 - _day, _numMultiday));

         ArrayList<Double> clickPrTrend = new ArrayList<Double>();
         ArrayList<Double> numImpsTrend = new ArrayList<Double>();
         ArrayList<Double> CPCTrend = new ArrayList<Double>();
         ArrayList<Double> convPrTrend = new ArrayList<Double>();
         for (int i = 0; i < realNumDays; i++) {
            /*
                 * This trend says the future is always worse
                 */
            clickPrTrend.add(1.0 - i * .01);
            numImpsTrend.add(1.0 - i * .01);
            CPCTrend.add(1.0 + i * .01);
            convPrTrend.add(1.0 - i * .01);
         }

         //MULTIDAY
         LinkedList<DrIncItem> allIncItems = new LinkedList<DrIncItem>();
         ArrayList<Integer> salesArray = new ArrayList<Integer>(((BasicUnitsSoldModel) _unitsSold).getSalesArray());
         Integer missingConvs = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
         if (missingConvs == null) {
            salesArray.add((int) (_unitsSold.getWindowSold() - _unitsSold.getThreeDaysSold()));
         } else {
            salesArray.add(missingConvs);
         }
         double[] budgets = new double[realNumDays];
         for (int multiDay = 1; multiDay <= budgets.length; multiDay++) {
            double multiDayBudget = _capacity;
            for (int i = 0; i < _capWindow - 1; i++) {
               multiDayBudget -= salesArray.get(salesArray.size() - 1 - i);
            }
            multiDayBudget = _capacity - _unitsSold.getWindowSold();

            budgets[multiDay - 1] = multiDayBudget;

            double penalty = 1.0;
            if (multiDayBudget < 0) {
               penalty = Math.pow(_lambda, Math.abs(multiDayBudget));
            }
            for (Query q : _querySpace) {
               LinkedList<Item> itemList = new LinkedList<Item>();
               debug("Query: " + q);
               for (int i = 0; i < bidList.size(); i++) {
                  double salesPrice = _salesPrices.get(q);
                  double bid = bidList.get(i);
                  double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad()) * clickPrTrend.get(multiDay - 1);
                  double numImps = _queryToNumImpModel.getPrediction(q, (int) (_day + 1)) * numImpsTrend.get(multiDay - 1);
                  int numClicks = (int) (clickPr * numImps);
                  double CPC = _bidToCPC.getPrediction(q, bid) * CPCTrend.get(multiDay - 1);
                  double convProb = _convPrModel.getPrediction(q) * penalty * convPrTrend.get(multiDay - 1);

                  if (Double.isNaN(CPC)) {
                     CPC = 0.0;
                  }

                  if (Double.isNaN(clickPr)) {
                     clickPr = 0.0;
                  }

                  if (Double.isNaN(convProb)) {
                     convProb = 0.0;
                  }

                  int isID = _queryId.get(q);
                  double w = numClicks * convProb;            //weight = numClciks * convProv
                  double v = numClicks * convProb * salesPrice - numClicks * CPC;   //value = revenue - cost	[profit]
                  itemList.add(new DrItem(q, w, v, bid, false, isID, i, multiDay));

                  if (TARGET) {
                     /*
                             * add a targeted version of our bid as well
                             */
                     if (clickPr != 0) {
                        numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
                        if (convProb != 0) {
                           convProb *= _targModel.getConvPrPredictionMultiplier(q, clickPr, convProb, false);
                        }
                        salesPrice = _targModel.getUSPPrediction(q, clickPr, false);
                     }

                     w = numClicks * convProb;            //weight = numClciks * convProv
                     v = numClicks * convProb * salesPrice - numClicks * CPC;   //value = revenue - cost	[profit]

                     itemList.add(new DrItem(q, w, v, bid, true, isID, i, multiDay));
                  }
               }
               DrItem[] items = itemList.toArray(new DrItem[itemList.size()]);
               DrIncItem[] iItems = getIncremental(items);
               allIncItems.addAll(Arrays.asList(iItems));
            }
            salesArray.add((int) (_capacity / ((double) _capWindow)));
         }

         for (int multiDay = 1; multiDay <= budgets.length; multiDay++) {
            salesArray.remove((int) (salesArray.size() - 1));
         }

         Collections.sort(allIncItems);

         HashMap<Integer, Item> solution = fillMultiDayKnapsack(allIncItems, budgets);

         //set bids
         for (Query q : _querySpace) {

            Integer isID = _queryId.get(q);
            double bid;

            if (solution.containsKey(isID)) {
               bid = solution.get(isID).b();
               //					bid *= randDouble(.97,1.03);  //Mult by rand to avoid users learning patterns.
               //					System.out.println("Bidding " + bid + "   for query: " + q);
               double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
               double numImps = _queryToNumImpModel.getPrediction(q, (int) (_day + 1));
               int numClicks = (int) (clickPr * numImps);
               double CPC = _bidToCPC.getPrediction(q, bid);

               if (solution.get(isID).targ()) {

                  if (clickPr != 0) {
                     numClicks *= _targModel.getClickPrPredictionMultiplier(q, clickPr, false);
                  }

                  bidBundle.setBid(q, bid);

                  if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                     bidBundle.setAd(q, new Ad(new Product(_manSpecialty, _compSpecialty)));
                  }
                  if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getComponent() == null) {
                     bidBundle.setAd(q, new Ad(new Product(q.getManufacturer(), _compSpecialty)));
                  }
                  if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && q.getManufacturer() == null) {
                     bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
                  }
                  if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && q.getManufacturer().equals(_manSpecialty)) {
                     bidBundle.setAd(q, new Ad(new Product(_manSpecialty, q.getComponent())));
                  }
               } else {
                  bidBundle.addQuery(q, bid, new Ad());
               }

               if (BUDGET) {
                  bidBundle.setDailyLimit(q, numClicks * CPC * 1.3);
               }
            } else {
               /*
                     * We decided that we did not want to be in this query, so we will use it to explore the space
                     */
               //					bid = 0.0;
               //					bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
               //					System.out.println("Bidding " + bid + "   for query: " + q);

               if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
                  bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
               } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
                  bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
               } else {
                  bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
               }

               //					System.out.println("Exploring " + q + "   bid: " + bid);
               bidBundle.addQuery(q, bid, new Ad(), bid * 10);
            }
         }

         /*
             * Pass expected conversions to unit sales model
             */
         double threshold = 2;
         double lastSolWeight = 0.0;
         double solutionWeight = threshold + 1;
         int numIters = 0;
         while (Math.abs(lastSolWeight - solutionWeight) > threshold) {
            numIters++;
            lastSolWeight = solutionWeight;
            solutionWeight = 0;
            double newPenalty;
            double numOverCap = lastSolWeight - budget;
            if (budget < 0) {
               newPenalty = 0.0;
               int num = 0;
               for (double j = Math.abs(budget) + 1; j <= numOverCap; j++) {
                  newPenalty += Math.pow(_lambda, j);
                  num++;
               }
               newPenalty /= (num);
               double oldPenalty = Math.pow(_lambda, Math.abs(budget));
               newPenalty = newPenalty / oldPenalty;
            } else {
               if (numOverCap <= 0) {
                  newPenalty = 1.0;
               } else {
                  newPenalty = budget;
                  for (int j = 1; j <= numOverCap; j++) {
                     newPenalty += Math.pow(_lambda, j);
                  }
                  newPenalty /= (budget + numOverCap);
               }
            }
            if (Double.isNaN(newPenalty)) {
               newPenalty = 1.0;
            }
            for (Query q : _querySpace) {
               double bid = bidBundle.getBid(q);
               double dailyLimit = bidBundle.getDailyLimit(q);
               double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
               double numImps = _queryToNumImpModel.getPrediction(q, (int) (_day + 1));
               int numClicks = (int) (clickPr * numImps);
               double CPC = _bidToCPC.getPrediction(q, bid);
               double convProb = _convPrModel.getPrediction(q) * newPenalty;

               if (Double.isNaN(CPC)) {
                  CPC = 0.0;
               }

               if (Double.isNaN(clickPr)) {
                  clickPr = 0.0;
               }

               if (Double.isNaN(convProb)) {
                  convProb = 0.0;
               }

               if (!Double.isNaN(dailyLimit)) {
                  if (numClicks * CPC > dailyLimit) {
                     numClicks = (int) (dailyLimit / CPC);
                  }
               }

               solutionWeight += numClicks * convProb;
            }
         }
         //			System.out.println(numIters);
         ((BasicUnitsSoldModel) _unitsSold).expectedConvsTomorrow((int) solutionWeight);
      } else {
         for (Query q : _querySpace) {
            double bid = 0.0;
            if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
               bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
            } else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
               bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
            } else {
               bid = randDouble(.04, _salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
            }
            bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
         }
      }
      double stop = System.currentTimeMillis();
      double elapsed = stop - start;
      System.out.println("This took " + (elapsed / 1000) + " seconds");
      return bidBundle;
   }

   private HashMap<Integer, Item> fillMultiDayKnapsack(LinkedList<DrIncItem> incItems, double[] budgets) {
      HashMap<Integer, Item> solution = new HashMap<Integer, Item>();

      int[] expectedConvs = new int[budgets.length];

      for (int i = 0; i < incItems.size(); i++) {
         DrIncItem ii = incItems.get(i);
         int itemDay = ii.day();
         boolean capacityViolated = false;

         for (int d = 1; d <= budgets.length; d++) {
            double budget = budgets[d - 1];
            int expectedConv = expectedConvs[d - 1];
            double overCap = expectedConv - budget;
            if (overCap > 0) {
               capacityViolated = true;
               break;
            }
            if (itemDay >= d && itemDay < d + 5) {
               if (expectedConv + ii.w() > budget) {
                  capacityViolated = true;
                  break;
               }
            } else {
               if (expectedConv > budget) {
                  capacityViolated = true;
                  break;
               }
            }
         }

         if (!capacityViolated) {
            if (itemDay == 1) {
               solution.put(ii.item().isID(), ii.item());
            }
            expectedConvs[itemDay - 1] += ii.w();
         } else {
            double avgConvProb = 0; //the average probability of conversion;
            for (Query q : _querySpace) {
               if (_day < 2) {
                  avgConvProb += _baseConvProbs.get(q) / 16.0;
               } else {
                  avgConvProb += _baseConvProbs.get(q) * _salesDist.getPrediction(q);
               }
            }

            double avgUSP = 0;
            for (Query q : _querySpace) {
               if (_day < 2) {
                  avgUSP += _salesPrices.get(q) / 16.0;
               } else {
                  avgUSP += _salesPrices.get(q) * _salesDist.getPrediction(q);
               }
            }

            double valueLost = 0;

            /*
                 * The item cannot change the value on days before it occurs, or more
                 * than 4 days later
                 */
            for (int d = itemDay; d < itemDay + _capWindow && d <= budgets.length; d++) {
               if (d >= itemDay) {
                  double valueLostWindow = Math.max(1, Math.min(_capWindow - (budgets.length - d), Math.min(_capWindow, 59 - (_day + d - 1))));
                  double budget = budgets[d - 1];
                  int expectedConv = expectedConvs[d - 1];
                  double min = expectedConv - budget;
                  double max = min + ii.w();
                  min = Math.max(min, 0);
                  if (max > 0) {
                     for (double j = min + 1; j <= max; j++) {
                        double iD = Math.pow(_lambda, j);
                        double worseConvProb = avgConvProb * iD; //this is a gross average that lacks detail
                        valueLost += (avgConvProb - worseConvProb) * avgUSP * valueLostWindow; //You also lose conversions in the future (for 5 days)
                     }
                  }
               }
            }

            if (ii.v() > valueLost) {
               if (itemDay == 1) {
                  solution.put(ii.item().isID(), ii.item());
               }
               expectedConvs[itemDay - 1] += ii.w();
            } else {
               if (itemDay != 1 && itemDay > _capWindow + 1) {
                  for (int j = i; j < incItems.size(); j++) {
                     DrIncItem itemToRemove = incItems.get(j);
                     if (itemToRemove.day() <= itemDay &&
                             itemToRemove.day() > itemDay - _capWindow) {
                        incItems.remove(j);
                        j--;
                     }
                  }
               } else {
                  break;
               }
            }
         }
      }
      return solution;
   }

   /**
    * Get undominated items
    *
    * @param items
    * @return
    */
   public static DrItem[] getUndominated(DrItem[] items) {
      Arrays.sort(items, new ItemComparatorByWeight());
      //remove dominated items (higher weight, lower value)
      LinkedList<Item> temp = new LinkedList<Item>();
      temp.add(items[0]);
      for (int i = 1; i < items.length; i++) {
         Item lastUndominated = temp.get(temp.size() - 1);
         if (lastUndominated.v() < items[i].v()) {
            temp.add(items[i]);
         }
      }


      LinkedList<Item> betterTemp = new LinkedList<Item>();
      betterTemp.addAll(temp);
      for (int i = 0; i < temp.size(); i++) {
         LinkedList<Item> duplicates = new LinkedList<Item>();
         Item item = temp.get(i);
         duplicates.add(item);
         for (int j = i + 1; j < temp.size(); j++) {
            Item otherItem = temp.get(j);
            if (item.v() == otherItem.v() && item.w() == otherItem.w()) {
               duplicates.add(otherItem);
            }
         }
         if (duplicates.size() > 1) {
            betterTemp.removeAll(duplicates);
            double minBid = 10;
            double maxBid = -10;
            for (int j = 0; j < duplicates.size(); j++) {
               double bid = duplicates.get(j).b();
               if (bid > maxBid) {
                  maxBid = bid;
               }
               if (bid < minBid) {
                  minBid = bid;
               }
            }
            Item newItem = new Item(item.q(), item.w(), item.v(), (maxBid + minBid) / 2.0, item.targ(), item.isID(), item.idx());
            betterTemp.add(newItem);
         }
      }

      //items now contain only undominated items
      items = betterTemp.toArray(new DrItem[0]);
      Arrays.sort(items, new ItemComparatorByWeight());

      //remove lp-dominated items
      LinkedList<Item> q = new LinkedList<Item>();
      q.add(new Item(new Query(), 0, 0, -1, false, 1, 0));//add item with zero weight and value

      for (int i = 0; i < items.length; i++) {
         q.add(items[i]);//has at least 2 items now
         int l = q.size() - 1;
         Item li = q.get(l);//last item
         Item nli = q.get(l - 1);//next to last
         if (li.w() == nli.w()) {
            if (li.v() > nli.v()) {
               q.remove(l - 1);
            } else {
               q.remove(l);
            }
         }
         l = q.size() - 1; //reset in case an item was removed
         //while there are at least three elements and ...
         while (l > 1 && (q.get(l - 1).v() - q.get(l - 2).v()) / (q.get(l - 1).w() - q.get(l - 2).w())
                 <= (q.get(l).v() - q.get(l - 1).v()) / (q.get(l).w() - q.get(l - 1).w())) {
            q.remove(l - 1);
            l--;
         }
      }

      //remove the (0,0) item
      if (q.get(0).w() == 0 && q.get(0).v() == 0) {
         q.remove(0);
      }

      DrItem[] uItems = (DrItem[]) q.toArray(new DrItem[0]);
      return uItems;
   }


   /**
    * Get incremental items
    *
    * @param items
    * @return
    */
   public DrIncItem[] getIncremental(DrItem[] items) {
      debug("PRE INCREMENTAL");
      for (int i = 0; i < items.length; i++) {
         debug("\t" + items[i]);
      }

      DrItem[] uItems = getUndominated(items);

      debug("UNDOMINATED");
      for (int i = 0; i < uItems.length; i++) {
         debug("\t" + uItems[i]);
      }

      DrIncItem[] ii = new DrIncItem[uItems.length];

      if (uItems.length != 0) { //getUndominated can return an empty array
         ii[0] = new DrIncItem(uItems[0].w(), uItems[0].v(), uItems[0], null, uItems[0].day());
         for (int item = 1; item < uItems.length; item++) {
            DrItem prev = uItems[item - 1];
            DrItem cur = uItems[item];
            ii[item] = new DrIncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur, prev, cur.day());
         }
      }
      debug("INCREMENTAL");
      for (int i = 0; i < ii.length; i++) {
         debug("\t" + ii[i]);
      }
      return ii;
   }

   private double randDouble(double a, double b) {
      double rand = _R.nextDouble();
      return rand * (b - a) + a;
   }

   public void debug(Object str) {
      if (DEBUG) {
         System.out.println(str);
      }
   }

   @Override
   public String toString() {
      return "DrMCKPBid(" + _numMultiday + ")";
   }

   @Override
   public AbstractAgent getCopy() {
      return new DrMCKPBid(_numMultiday);
   }

}
