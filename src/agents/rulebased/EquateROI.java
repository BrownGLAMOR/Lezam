package agents.rulebased;


import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquateROI extends RuleBasedAgent {
   protected BidBundle _bidBundle;
   protected boolean BUDGET = false;
   protected double _ROI;
   protected double _alphaIncROI;
   protected double _betaIncROI;
   protected double _gammaIncROI;
   protected double _deltaIncROI;
   protected double _alphaDecROI;
   protected double _betaDecROI;
   protected double _gammaDecROI;
   protected double _deltaDecROI;
   protected double _initROI;
   protected double _threshTS;

   public EquateROI(double initROI,
                    double alphaIncROI,
                    double betaIncROI,
                    double gammaIncROI,
                    double deltaIncROI,
                    double alphaDecROI,
                    double betaDecROI,
                    double gammaDecROI,
                    double deltaDecROI,
                    double threshTS,
                    double lambdaCapLow,
                    double lambdaCapMed,
                    double lambdaCapHigh,
                    double lambdaBudgetLow,
                    double lambdaBudgetMed,
                    double lambdaBudgetHigh,
                    double dailyCapMin,
                    double dailyCapMax) {
      _initROI = initROI;
      _alphaIncROI = alphaIncROI;
      _betaIncROI = betaIncROI;
      _gammaIncROI = gammaIncROI;
      _deltaIncROI = deltaIncROI;
      _alphaDecROI = alphaDecROI;
      _betaDecROI = betaDecROI;
      _gammaDecROI = gammaDecROI;
      _deltaDecROI = deltaDecROI;
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
            _ROI *= (1 - (_alphaDecROI * Math.abs(sum - _dailyCapacity) + _betaDecROI) * Math.pow(_gammaDecROI, _day * _deltaDecROI));
         } else {
            _ROI *= (1 + (_alphaIncROI * Math.abs(sum - _dailyCapacity) + _betaIncROI) * Math.pow(_gammaIncROI, _day * _deltaIncROI));
         }

         if (Double.isNaN(_ROI) || _ROI <= 0.0) {
            _ROI = _initROI;
         }
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         _bidBundle.setBid(query, targetCPC + .01);
      }

      if (BUDGET) {
         _bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
      }

      System.out.println(_ROI);

      return _bidBundle;
   }

   @Override
   public void initBidder() {
      super.initBidder();
      setDailyQueryCapacity();
      _ROI = _initROI;
   }

   protected double getTargetCPC(Query q) {

      double prConv;
      if (_day <= 6) {
         prConv = _baselineConversion.get(q);
      } else {
         prConv = _conversionPrModel.getPrediction(q);
      }

      double rev = _salesPrices.get(q);
      double CPC = (rev * prConv) / (_ROI + 1.0);
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));

      return CPC;
   }

   @Override
   public String toString() {
      return "EquateROI";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquateROI(_initROI, _alphaIncROI, _betaIncROI, _gammaIncROI, _deltaIncROI, _alphaDecROI, _betaDecROI, _gammaDecROI, _deltaDecROI, _threshTS, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed, _lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
   }

}