package agents.rulebased.simple;

import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Set;

public class EquatePPSSimple extends RuleBasedAgentSimple {
   protected BidBundle _bidBundle;
   protected double _PPS;
   protected double _initPPS;
   protected double _incPPS;
   protected double _decPPS;

   public EquatePPSSimple(double lambdaCap) {
      this(9.9684, 1.03, lambdaCap);
   }

   public EquatePPSSimple() {
      this(9.9684, 1.03, 1.375);
   }

   public EquatePPSSimple(double initPPS,
                          double incPPS,
                          double lambdaCap) {
      _initPPS = initPPS;
      _incPPS = incPPS;
      _decPPS = 1.0 / incPPS;
      _lambdaCap = lambdaCap;
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
      }

      for (Query query : _querySpace) {
         double targetCPC = getTargetCPC(query);
         _bidBundle.addQuery(query, getBidFromCPC(query, targetCPC), new Ad(), Double.MAX_VALUE);
      }

      _bidBundle.setCampaignDailySpendLimit(Integer.MAX_VALUE);

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
      CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));

      return CPC;
   }

   @Override
   public String toString() {
      return "EquatePPS(" + _initPPS + ", " + _incPPS + ", " + _lambdaCap + ")";
   }

   @Override
   public AbstractAgent getCopy() {
      return new EquatePPSSimple(_initPPS, _incPPS, _lambdaCap);
   }

}