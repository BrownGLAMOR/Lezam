package agents.rulebased2010.simple;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquateROISimpleBudget2010 extends RuleBasedAgentSimple2010 {
   protected double _ROI;
   protected double _initROI;
   protected double _incROI;
   protected double _decROI;

   public EquateROISimpleBudget2010() {
      this(3.9376620170349352, 1.02, 1.35);
   }

   public EquateROISimpleBudget2010(double initROI,
                                    double incROI,
                                    double lambdaCap) {
      _initROI = initROI;
      _incROI = incROI;
      _decROI = 1.0 / incROI;
      _lambdaCap = lambdaCap;
      TOTBUDGET = true;
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

         if (sum <= _dailyCapacity) {
            _ROI *= _decROI;
         } else {
            _ROI *= _incROI;
         }

         if (Double.isNaN(_ROI) || _ROI <= 0.0) {
            _ROI = _initROI;
         }
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         _bidBundle.addQuery(query, getBidFromCPC(query, targetCPC), new Ad(), Double.MAX_VALUE);
      }

      if (TOTBUDGET) {
         _bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
      } else {
         _bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);
      }

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
      return "EquateROI(" + _initROI + ", " + _incROI + ", " + _lambdaCap + ")";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquateROISimpleBudget2010(_initROI, _incROI, _lambdaCap);
   }

}