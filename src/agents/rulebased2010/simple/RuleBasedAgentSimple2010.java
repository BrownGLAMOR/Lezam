package agents.rulebased2010.simple;


import agents.AbstractAgent;
import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.prconv.AbstractConversionModel;
import models.prconv.HistoricPrConversionModel;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldLambdaModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public abstract class RuleBasedAgentSimple2010 extends AbstractAgent {

   protected double _dailyCapacity;
   protected BidBundle _bidBundle;
   protected final int HIGH_CAPACITY = 600;
   protected final int MEDIUM_CAPACITY = 450;
   protected final int LOW_CAPACITY = 300;
   protected final int MAX_TIME_HORIZON = 5;
   protected AbstractUnitsSoldModel _unitsSoldModel;
   protected AbstractConversionModel _conversionPrModel;
   protected Random _R;
   protected long seed = 61686;
   protected boolean SEEDED = false;
   protected boolean TOTBUDGET = false;

   protected HashMap<Query, Double> _baselineConversion;
   protected HashMap<Query, Double> _baseClickProbs;
   protected HashMap<Query, Double> _salesPrices;
   protected HashMap<Query, Double> _salesDistribution;

   protected double _lambdaCap;

   @Override
   public void initBidder() {

      _R = new Random();

      if (SEEDED) {
         _R.setSeed(seed);
      }

      _baselineConversion = new HashMap<Query, Double>();
      _baseClickProbs = new HashMap<Query, Double>();
      _salesDistribution = new HashMap<Query, Double>();

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
            _baselineConversion.put(q, _piF0);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
            _baselineConversion.put(q, _piF1);
         } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
            _baselineConversion.put(q, _piF2);
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
            _baselineConversion.put(q, eta(_baselineConversion.get(q), 1 + _CSB));
         } else if (component == null) {
            _baselineConversion.put(q, eta(_baselineConversion.get(q), 1 + _CSB) * (1 / 3.0) + _baselineConversion.get(q) * (2 / 3.0));
         }

         _salesDistribution.put(q, 1.0 / _querySpace.size());
      }

      setDailyQueryCapacity();
   }

   @Override
   public final Set<AbstractModel> initModels() {
      HashSet<AbstractModel> models = new HashSet<AbstractModel>();
      _unitsSoldModel = new BasicUnitsSoldLambdaModel(_querySpace, _capacity, _capWindow, 1.0);
      _conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty, _compSpecialty));

      models.add(_unitsSoldModel);
      models.add(_conversionPrModel);
      return models;
   }

   @Override
   public final void updateModels(SalesReport salesReport, QueryReport queryReport) {
      if (_conversionPrModel instanceof HistoricPrConversionModel) {
         int timeHorizon = (int) Math.min(Math.max(1, _day - 1), MAX_TIME_HORIZON);
         ((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(timeHorizon);
      }

      if (_bidBundles.size() > 1 && salesReport != null && queryReport != null) {
         _unitsSoldModel.update(salesReport);
         setDailyQueryCapacity();
         _conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
      }
   }

   protected final void buildMaps(Set<AbstractModel> models) {
      for (AbstractModel model : models) {
         if (model instanceof AbstractUnitsSoldModel) {
            AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
            _unitsSoldModel = unitsSold;
         } else if (model instanceof AbstractConversionModel) {
            AbstractConversionModel convPrModel = (AbstractConversionModel) model;
            _conversionPrModel = convPrModel;
         }
      }
   }

   protected final void setDailyQueryCapacity() {
      if (_day < 5) {
         _dailyCapacity = (_capacity / ((double) _capWindow));
      } else {
         _dailyCapacity = _capacity * _lambdaCap - _unitsSoldModel.getWindowSold();
      }
   }

   protected final double getDailySpendingLimit(Query q, double targetCPC) {
      if (_day >= 6 && _conversionPrModel != null) {
         return ((targetCPC * _salesDistribution.get(q) * _dailyCapacity) / _conversionPrModel.getPrediction(q));
      } else {
         return ((targetCPC * _salesDistribution.get(q) * _dailyCapacity) / _baselineConversion.get(q));
      }
   }

   protected final double getTotalSpendingLimit(BidBundle bundle) {
      if (_dailyCapacity > 0) {
         double targetCPC = 0;
         double convPr = 0;
         int numQueries = 0;
         for (Query q : _querySpace) {
            if (!Double.isNaN(bundle.getBid(q)) && bundle.getBid(q) > 0) {
               targetCPC += bundle.getBid(q);
               if (_day >= 6 && _conversionPrModel != null) {
                  convPr += _conversionPrModel.getPrediction(q);
               } else {
                  convPr += _baselineConversion.get(q);
               }
               numQueries++;
            }
         }
         targetCPC /= numQueries;
         convPr /= numQueries;
         return (targetCPC * (_dailyCapacity / convPr));
      } else {
         return 0.0;
      }
   }

   protected final double getBidFromCPC(Query query, double cpc) {
      return cpc + .01;
   }

   protected final double getRandomBid(Query q) {
      return randDouble(.04, _salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9);
   }

   private final double randDouble(double a, double b) {
      double rand = _R.nextDouble();
      return rand * (b - a) + a;
   }

}
