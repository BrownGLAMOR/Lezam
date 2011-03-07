package agents.rulebased;


import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquatePM extends RuleBasedAgent {
   protected BidBundle _bidBundle;
   protected boolean BUDGET = true;
   protected double _PM;
   protected double _alphaIncPM;
   protected double _betaIncPM;
   protected double _gammaIncPM;
   protected double _deltaIncPM;
   protected double _alphaDecPM;
   protected double _betaDecPM;
   protected double _gammaDecPM;
   protected double _deltaDecPM;
   protected double _initPM;
   protected double _threshTS;


   public EquatePM(double initPM,
                   double alphaIncPM,
                   double betaIncPM,
                   double gammaIncPM,
                   double deltaIncPM,
                   double alphaDecPM,
                   double betaDecPM,
                   double gammaDecPM,
                   double deltaDecPM,
                   double threshTS,
                   double lambdaCapLow,
                   double lambdaCapMed,
                   double lambdaCapHigh,
                   double lambdaBudgetLow,
                   double lambdaBudgetMed,
                   double lambdaBudgetHigh,
                   double dailyCapMin,
                   double dailyCapMax) {
      _initPM = initPM;
      _alphaIncPM = alphaIncPM;
      _betaIncPM = betaIncPM;
      _gammaIncPM = gammaIncPM;
      _deltaIncPM = deltaIncPM;
      _alphaDecPM = alphaDecPM;
      _betaDecPM = betaDecPM;
      _gammaDecPM = gammaDecPM;
      _deltaDecPM = deltaDecPM;
      _threshTS = threshTS;
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
   public BidBundle getBidBundle(Set<AbstractModel> models) {
      buildMaps(models);
      _bidBundle = new BidBundle();
      if (_day < 2) {
         for (Query q : _querySpace) {
            double bid = getRandomBid(q);
            _bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
         }
         return _bidBundle;
      }

      if (_day > 1 && _salesReport != null && _queryReport != null) {
         /*
             * Equate PMs
             */
         double sum = 0.0;
         for (Query query : _querySpace) {
            sum += _salesReport.getConversions(query);
         }

         if (Math.abs(sum - _dailyCapacity) <= _threshTS) {
            //Do Nothing
         } else if (sum <= _dailyCapacity) {
            _PM *= (1 - (_alphaDecPM * Math.abs(sum - _dailyCapacity) + _betaDecPM) * Math.pow(_gammaDecPM, _day * _deltaDecPM));
         } else {
            _PM *= (1 + (_alphaIncPM * Math.abs(sum - _dailyCapacity) + _betaIncPM) * Math.pow(_gammaIncPM, _day * _deltaIncPM));
         }

         if (Double.isNaN(_PM) || _PM <= 0) {
            _PM = _initPM;
         }
         if (_PM > 1.0) {
            _PM = 1.0;
         }
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         _bidBundle.setBid(query, getBidFromCPC(query, targetCPC));
      }

      if (BUDGET) {
         _bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
      }

      System.out.println(_PM);
      System.out.println(_bidBundle);

      return _bidBundle;
   }

   @Override
   public void initBidder() {
      super.initBidder();
      setDailyQueryCapacity();
      _PM = _initPM;
   }

   protected double getTargetCPC(Query q) {

      double prConv;
      if (_day <= 6) {
         prConv = _baselineConversion.get(q);
      } else {
         prConv = _conversionPrModel.getPrediction(q);
      }

      double rev = _salesPrices.get(q);
      double CPC = (1 - _PM) * rev * prConv;
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));

      return CPC;
   }

   @Override
   public String toString() {
      return "EquatePM";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquatePM(_initPM, _alphaIncPM, _betaIncPM, _gammaIncPM, _deltaIncPM, _alphaDecPM, _betaDecPM, _gammaDecPM, _deltaDecPM, _threshTS, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed, _lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
   }

}