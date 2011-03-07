package agents.rulebased;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.HashMap;
import java.util.Set;

public class AdjustPM extends RuleBasedAgent {

   protected BidBundle _bidBundle;
   protected final boolean BUDGET = true;
   protected final boolean DAILYBUDGET = true;
   protected double _initPM;
   protected double _alphaIncTS;
   protected double _betaIncTS;
   protected double _gammaIncTS;
   protected double _deltaIncTS;
   protected double _alphaDecTS;
   protected double _betaDecTS;
   protected double _gammaDecTS;
   protected double _deltaDecTS;
   protected double _alphaIncPM;
   protected double _betaIncPM;
   protected double _gammaIncPM;
   protected double _deltaIncPM;
   protected double _alphaDecPM;
   protected double _betaDecPM;
   protected double _gammaDecPM;
   protected double _deltaDecPM;
   protected double _threshTS;
   protected double _threshPM;
   protected HashMap<Query, Double> _PM;


   public AdjustPM(double initPM,
                   double alphaIncTS,
                   double betaIncTS,
                   double gammaIncTS,
                   double deltaIncTS,
                   double alphaDecTS,
                   double betaDecTS,
                   double gammaDecTS,
                   double deltaDecTS,
                   double alphaIncPM,
                   double betaIncPM,
                   double gammaIncPM,
                   double deltaIncPM,
                   double alphaDecPM,
                   double betaDecPM,
                   double gammaDecPM,
                   double deltaDecPM,
                   double threshTS,
                   double threshPM,
                   double lambdaCapLow,
                   double lambdaCapMed,
                   double lambdaCapHigh,
                   double lambdaBudgetLow,
                   double lambdaBudgetMed,
                   double lambdaBudgetHigh,
                   double dailyCapMin,
                   double dailyCapMax) {
      _initPM = initPM;
      _alphaIncTS = alphaIncTS;
      _betaIncTS = betaIncTS;
      _gammaIncTS = gammaIncTS;
      _deltaIncTS = deltaIncTS;
      _alphaDecTS = alphaDecTS;
      _betaDecTS = betaDecTS;
      _gammaDecTS = gammaDecTS;
      _deltaDecTS = deltaDecTS;
      _alphaIncPM = alphaIncPM;
      _betaIncPM = betaIncPM;
      _gammaIncPM = gammaIncPM;
      _deltaIncPM = deltaIncPM;
      _alphaDecPM = alphaDecPM;
      _betaDecPM = betaDecPM;
      _gammaDecPM = gammaDecPM;
      _deltaDecPM = deltaDecPM;
      _threshTS = threshTS;
      _threshPM = threshPM;
      _lambdaCapLow = lambdaCapLow;
      _lambdaCapMed = lambdaCapMed;
      _lambdaCapHigh = lambdaCapHigh;
      _lambdaBudgetLow = lambdaBudgetLow;
      _lambdaBudgetMed = lambdaBudgetMed;
      _lambdaBudgetHigh = lambdaBudgetHigh;
      _dailyCapMin = dailyCapMin;
      _dailyCapMax = dailyCapMax;
   }

   @Override
   public void initBidder() {
      super.initBidder();

      _PM = new HashMap<Query, Double>();
      for (Query q : _querySpace) {
         _PM.put(q, _initPM);
      }
   }

   @Override
   public BidBundle getBidBundle(Set<AbstractModel> models) {
      buildMaps(models);
      _bidBundle = new BidBundle();

      if (_day < 2) {
         _bidBundle = new BidBundle();
         for (Query q : _querySpace) {
            double bid = getRandomBid(q);
            _bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
         }
         return _bidBundle;
      }

      /*
         * Calculate Average PM
         */
      double avgPM = 0.0;
      double totWeight = 0;
      for (Query q : _querySpace) {
         if (_queryReport.getCost(q) != 0 &&
                 _salesReport.getRevenue(q) != 0) {
            double weight = _salesDistribution.get(q);
            avgPM += ((_salesReport.getRevenue(q) - _queryReport.getCost(q)) / _salesReport.getRevenue(q)) * weight;
            totWeight += weight;
         }
      }
      avgPM /= totWeight;
      if (Double.isNaN(avgPM)) {
         avgPM = _initPM;
      }

      /*
         * Adjust Target Sales
         */
      double totDesiredSales = 0;
      for (Query q : _querySpace) {
         if (_queryReport.getCost(q) != 0 &&
                 _salesReport.getRevenue(q) != 0) {
            double PMq = (_salesReport.getRevenue(q) - _queryReport.getCost(q)) / _salesReport.getRevenue(q);
            if (Math.abs(PMq - avgPM) <= _threshPM) {
               //Do Nothing
            } else if (PMq <= avgPM) {
               _salesDistribution.put(q, _salesDistribution.get(q) * (1 - (_alphaDecTS * Math.abs(PMq - avgPM) + _betaDecTS) * Math.pow(_gammaDecTS, _day * _deltaDecTS)));
            } else {
               _salesDistribution.put(q, _salesDistribution.get(q) * (1 + (_alphaIncTS * Math.abs(PMq - avgPM) + _betaIncTS) * Math.pow(_gammaIncTS, _day * _deltaIncTS)));
            }
         }
         totDesiredSales += _salesDistribution.get(q);
      }

      /*
         * Normalize
         */
      double normFactor = 1.0 / totDesiredSales;
      for (Query q : _querySpace) {
         _salesDistribution.put(q, _salesDistribution.get(q) * normFactor);
      }

      /*
         * Adjust PM
         */
      if (_day > 1) {
         adjustPM();
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         _bidBundle.setBid(query, getBidFromCPC(query, targetCPC));

         if (DAILYBUDGET) {
            _bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));
         }

      }
      if (BUDGET) {
         _bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
      }
      return _bidBundle;
   }

   protected double getTargetCPC(Query q) {
      double prConv;
      if (_day <= 6) {
         prConv = _baselineConversion.get(q);
      } else {
         prConv = _conversionPrModel.getPrediction(q);
      }
      double CPC = _salesPrices.get(q) * (1 - _PM.get(q)) * prConv;
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
      return CPC;
   }

   protected void adjustPM() {
      for (Query q : _querySpace) {
         double tmp = _PM.get(q);
         double sales = _salesReport.getConversions(q);
         double dailyCap = _salesDistribution.get(q) * _dailyCapacity;
         if (Math.abs(sales - dailyCap) <= _threshTS) {
            //Do Nothing
         } else if (sales >= dailyCap) {
            tmp *= (1 + (_alphaIncPM * Math.abs(sales - dailyCap) + _betaIncPM) * Math.pow(_gammaIncPM, _day * _deltaIncPM));
         } else {
            tmp *= (1 - (_alphaDecPM * Math.abs(sales - dailyCap) + _betaDecPM) * Math.pow(_gammaDecPM, _day * _deltaDecPM));
         }
         if (Double.isNaN(tmp) || tmp <= 0) {
            tmp = _initPM;
         }
         if (tmp > 1.0) {
            tmp = 1.0;
         }
         _PM.put(q, tmp);
      }
   }

   @Override
   public String toString() {
      return "AdjustPM";
   }

   @Override
   public AbstractAgent getCopy() {
      return new AdjustPM(_initPM, _alphaIncTS, _betaIncTS, _gammaIncTS, _deltaIncTS, _alphaDecTS, _betaDecTS, _gammaDecTS, _deltaDecTS, _alphaIncPM, _betaIncPM, _gammaIncPM, _deltaIncPM, _alphaDecPM, _betaDecPM, _gammaDecPM, _deltaDecPM, _threshTS, _threshPM, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed, _lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
   }
}
