package agents.rulebased2010;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquatePRSimple2010 extends RuleBasedAgentSimple2010 {
   protected double _PR;
   protected double _initPR;
   protected double _incPR;
   protected double _decPR;

   public EquatePRSimple2010() {
      this(4.9376620170349348, 1.06, 1.35);
   }

   public EquatePRSimple2010(double initPR,
                             double incPR,
                             double lambdaCap) {
      _initPR = initPR;
      _incPR = incPR;
      _decPR = 1.0 / incPR;
      _lambdaCap = lambdaCap;
      TOTBUDGET = false;
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
            _PR *= _decPR;
         } else {
            _PR *= _incPR;
         }

         if (Double.isNaN(_PR)) {
            _PR = _initPR;
         }
         if (_PR <= 1.0) {
            _PR = 1.0;
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
      _PR = _initPR;
   }

   protected double getTargetCPC(Query q) {

      double prConv;
      if (_day <= 6) {
         prConv = _baselineConversion.get(q);
      } else {
         prConv = _conversionPrModel.getPrediction(q);
      }

      double rev = _salesPrices.get(q);
      double CPC = (rev * prConv) / _PR;
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));

      return CPC;
   }

   @Override
   public String toString() {
      return "EquatePR(" + _initPR + ", " + _incPR + ", " + _lambdaCap + ")";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquatePRSimple2010(_initPR, _incPR, _lambdaCap);
   }

}