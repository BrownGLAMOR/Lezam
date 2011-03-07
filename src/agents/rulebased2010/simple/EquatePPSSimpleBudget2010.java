package agents.rulebased2010.simple;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquatePPSSimpleBudget2010 extends RuleBasedAgentSimple2010 {
   protected double _PPS;
   protected double _initPPS;
   protected double _incPPS;
   protected double _decPPS;

   public EquatePPSSimpleBudget2010() {
      this(7.5, 1.0225, 1.375);
//		this(9.9684,1.03,1.375);
   }

   public EquatePPSSimpleBudget2010(double initPPS,
                                    double incPPS,
                                    double lambdaCap) {
      _initPPS = initPPS;
      _incPPS = incPPS;
      _decPPS = 1.0 / incPPS;
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
            _bidBundle.setBid(q, bid);
         }
         _bidBundle.setCampaignDailySpendLimit(1000.0);
         return _bidBundle;
      }

      //if _dailyCapacity < 0, no need to update anything since we aren't really bidding
      if (_day > 1 && _salesReport != null && _queryReport != null && _dailyCapacity > 0) {
         /*
             * Equate PPS
             */
         double sum = 0.0;
         for (Query query : _querySpace) {
            sum += _salesReport.getConversions(query);
         }

         if (sum <= _dailyCapacity) {
            _PPS *= _decPPS;
         } else {
            _PPS *= _incPPS;
         }

         if (Double.isNaN(_PPS) || _PPS <= 0) {
            _PPS = _initPPS;
         }
         if (_PPS > 15.0) {
            _PPS = 15.0;
         }

         System.out.println("\n\n\nCURRENT PPSPoop2: " + _PPS + "\n\n\n");
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         if (TOTBUDGET) {
            _bidBundle.addQuery(query, getBidFromCPC(query, targetCPC), new Ad(), getDailySpendingLimit(query, targetCPC));
         } else {
            _bidBundle.addQuery(query, getBidFromCPC(query, targetCPC), new Ad(), Double.MAX_VALUE);
         }
      }

      if (TOTBUDGET) {
         if (_capacity == HIGH_CAPACITY) {
            _bidBundle.setCampaignDailySpendLimit(Math.min(1400, Math.max(400.0, getTotalSpendingLimit(_bidBundle))));
         } else if (_capacity == MEDIUM_CAPACITY) {
            _bidBundle.setCampaignDailySpendLimit(Math.min(1200, Math.max(300.0, getTotalSpendingLimit(_bidBundle))));
         } else if (_capacity == LOW_CAPACITY) {
            _bidBundle.setCampaignDailySpendLimit(Math.min(1000, Math.max(200.0, getTotalSpendingLimit(_bidBundle))));
         } else {
            throw new RuntimeException("BAD CAPACITY");
         }
      } else {
         _bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);
      }

      return _bidBundle;
   }

   @Override
   public void initBidder() {
      super.initBidder();
      setDailyQueryCapacity();
      _PPS = _initPPS;
   }

   protected double getTargetCPC(Query q) {

      double prConv;
      if (_day <= 6) {
         prConv = _baselineConversion.get(q);
      } else {
         prConv = _conversionPrModel.getPrediction(q);
      }

      double rev = _salesPrices.get(q);
      double CPC = (rev - _PPS) * prConv;
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q), CPC));

      return CPC;
   }

   @Override
   public String toString() {
      return "EquatePPS(" + _initPPS + ", " + _incPPS + ", " + _lambdaCap + ")";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquatePPSSimpleBudget2010(_initPPS, _incPPS, _lambdaCap);
   }

}