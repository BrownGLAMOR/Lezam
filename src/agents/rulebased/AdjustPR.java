package agents.rulebased;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.HashMap;
import java.util.Set;

public class AdjustPR extends RuleBasedAgent {

   protected BidBundle _bidBundle;
   protected final boolean BUDGET = false;
   protected final boolean DAILYBUDGET = false;
   protected double _initPR;
   protected double _alphaIncTS;
   protected double _betaIncTS;
   protected double _gammaIncTS;
   protected double _deltaIncTS;
   protected double _alphaDecTS;
   protected double _betaDecTS;
   protected double _gammaDecTS;
   protected double _deltaDecTS;
   protected double _alphaIncPR;
   protected double _betaIncPR;
   protected double _gammaIncPR;
   protected double _deltaIncPR;
   protected double _alphaDecPR;
   protected double _betaDecPR;
   protected double _gammaDecPR;
   protected double _deltaDecPR;
   protected double _threshTS;
   protected double _threshPR;
   protected HashMap<Query, Double> _PR;

   public AdjustPR(double initPR,
                   double alphaIncTS,
                   double betaIncTS,
                   double gammaIncTS,
                   double deltaIncTS,
                   double alphaDecTS,
                   double betaDecTS,
                   double gammaDecTS,
                   double deltaDecTS,
                   double alphaIncPR,
                   double betaIncPR,
                   double gammaIncPR,
                   double deltaIncPR,
                   double alphaDecPR,
                   double betaDecPR,
                   double gammaDecPR,
                   double deltaDecPR,
                   double threshTS,
                   double threshPR,
                   double lambdaCapLow,
                   double lambdaCapMed,
                   double lambdaCapHigh,
                   double lambdaBudgetLow,
                   double lambdaBudgetMed,
                   double lambdaBudgetHigh,
                   double dailyCapMin,
                   double dailyCapMax) {
      _initPR = initPR;
      _alphaIncTS = alphaIncTS;
      _betaIncTS = betaIncTS;
      _gammaIncTS = gammaIncTS;
      _deltaIncTS = deltaIncTS;
      _alphaDecTS = alphaDecTS;
      _betaDecTS = betaDecTS;
      _gammaDecTS = gammaDecTS;
      _deltaDecTS = deltaDecTS;
      _alphaIncPR = alphaIncPR;
      _betaIncPR = betaIncPR;
      _gammaIncPR = gammaIncPR;
      _deltaIncPR = deltaIncPR;
      _alphaDecPR = alphaDecPR;
      _betaDecPR = betaDecPR;
      _gammaDecPR = gammaDecPR;
      _deltaDecPR = deltaDecPR;
      _threshTS = threshTS;
      _threshPR = threshPR;
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

      _PR = new HashMap<Query, Double>();
      for (Query q : _querySpace) {
         _PR.put(q, _initPR);
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
         * Calculate Average PR
         */
      double avgPR = 0.0;
      double totWeight = 0;
      for (Query q : _querySpace) {
         if (_queryReport.getCost(q) != 0 &&
                 _salesReport.getRevenue(q) != 0) {
            double weight = _salesDistribution.get(q);
            avgPR += (_salesReport.getRevenue(q) / _queryReport.getCost(q)) * weight;
            totWeight += weight;
         }
      }
      avgPR /= totWeight;
      if (Double.isNaN(avgPR)) {
         avgPR = _initPR;
      }

      /*
         * Adjust Target Sales
         */
      double totDesiredSales = 0;
      for (Query q : _querySpace) {
         if (_queryReport.getCost(q) != 0 &&
                 _salesReport.getRevenue(q) != 0) {
            double PRq = _salesReport.getRevenue(q) / _queryReport.getCost(q);
            if (Math.abs(PRq - avgPR) <= _threshPR) {
               //Do Nothing
            } else if (PRq < avgPR) {
               _salesDistribution.put(q, _salesDistribution.get(q) * (1 - (_alphaDecTS * Math.abs(PRq - avgPR) + _betaDecTS) * Math.pow(_gammaDecTS, _day * _deltaDecTS)));
            } else {
               _salesDistribution.put(q, _salesDistribution.get(q) * (1 + (_alphaIncTS * Math.abs(PRq - avgPR) + _betaIncTS) * Math.pow(_gammaIncTS, _day * _deltaIncTS)));
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
         adjustPR();
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
      double rev = _salesPrices.get(q);
      double CPC = (rev * prConv) / _PR.get(q);
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
      return CPC;
   }

   protected void adjustPR() {
      for (Query q : _querySpace) {
         double tmp = _PR.get(q);
         double sales = _salesReport.getConversions(q);
         double dailyCap = _salesDistribution.get(q) * _dailyCapacity;
         if (Math.abs(sales - dailyCap) <= _threshTS) {
            //Do Nothing
         } else if (sales >= dailyCap) {
            tmp *= (1 + (_alphaIncPR * Math.abs(sales - dailyCap) + _betaIncPR) * Math.pow(_gammaIncPR, _day * _deltaIncPR));
         } else {
            tmp *= (1 - (_alphaDecPR * Math.abs(sales - dailyCap) + _betaDecPR) * Math.pow(_gammaDecPR, _day * _deltaDecPR));
         }
         if (Double.isNaN(tmp)) {
            tmp = _initPR;
         }
         if (tmp <= 1.0) {
            tmp = 1.0;
         }
         _PR.put(q, tmp);
      }
   }

   @Override
   public String toString() {
      return "AdjustPR";
   }

   @Override
   public AbstractAgent getCopy() {
      return new AdjustPR(_initPR, _alphaIncTS, _betaIncTS, _gammaIncTS, _deltaIncTS, _alphaDecTS, _betaDecTS, _gammaDecTS, _deltaDecTS, _alphaIncPR, _betaIncPR, _gammaIncPR, _deltaIncPR, _alphaDecPR, _betaDecPR, _gammaDecPR, _deltaDecPR, _threshTS, _threshPR, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed, _lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
   }
}
