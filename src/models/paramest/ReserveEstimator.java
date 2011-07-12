package models.paramest;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;

import java.util.Set;

import static models.paramest.ConstantsAndFunctions.*;

public class ReserveEstimator {

   QueryType _queryType;
   Set<Query> _querySpace;
   int _qTypeIdx;
   double _currentRegEstimate;
   double _minRegBidWithImps;
   double _currentPromEstimate;
   double _minPromBidWithImps;
   int _numPromSlots;

   boolean AVG_ESTS = false;
   boolean EST_PROM = false;

   public ReserveEstimator(int numPromSlots, QueryType qt, Set<Query> querySpace) {
      _numPromSlots = numPromSlots;
      _queryType = qt;
      _querySpace = querySpace;

      _qTypeIdx = queryTypeToInt(_queryType);
      _currentRegEstimate = _regReserveLow[_qTypeIdx];
      _minRegBidWithImps = _regReserveHigh[_qTypeIdx];

      _currentRegEstimate = _regReserveLow[_qTypeIdx];
      _minRegBidWithImps = _regReserveHigh[_qTypeIdx] + 0.5;
   }

   public boolean updateModel(BidBundle bundle, QueryReport queryReport) {

      for(Query q : _querySpace) {
         if(q.getType().equals(_queryType)) {
            double bid = bundle.getBid(q);
            /*
            * Check if there are any times that our bid was above what we believe
            * the reserve to be, and we weren't in the auction when we could have been
            */
            if(queryReport.getImpressions(q) == 0 && bid > _currentRegEstimate) {
               boolean avgPos5 = false;
               for(int i = 0; i < 8; i++) {
                  double avgPos = queryReport.getPosition(q,"adv" + (i+1));
                  if(!Double.isNaN(avgPos) && avgPos == 5.0) {
                     avgPos5 = true;
                     break;
                  }
               }

               if(!avgPos5) {
                  //This means that there was no one in slot 5 the whole time
                  //and we weren't in the auction, so our bid is below the reserve
                  _currentRegEstimate = bid;
               }
            }


            /*
            * Check if there were any times that we were in a promoted slot, but
            * got no promoted impressions
            */
            if(queryReport.getImpressions(q) > 0 &&
                    queryReport.getPromotedImpressions(q) == 0 &&
                    !Double.isNaN(queryReport.getPosition(q)) &&
                    queryReport.getPosition(q) <= _numPromSlots &&
                    bid > _currentPromEstimate) {
               _currentPromEstimate = bid;
            }
         }
      }

      /*
      * Find the minimum bid that we received imps with
      *
      */
      for(Query q : _querySpace) {
         if(q.getType().equals(_queryType)) {
            double bid = bundle.getBid(q);
            if(queryReport.getImpressions(q) > 0 && bid < _minRegBidWithImps) {
               _minRegBidWithImps = bid;
            }
         }
      }

      /*
      * Find the minimum bid that we received prom imps with
      */
      for(Query q : _querySpace) {
         if(q.getTransportName().equals(_queryType)) {
            double bid  = bundle.getBid(q);
            if(queryReport.getPromotedImpressions(q) > 0 && bid < _minPromBidWithImps) {
               _minPromBidWithImps = bid;
            }
         }
      }

      _currentRegEstimate = Math.min(_currentRegEstimate, _minRegBidWithImps);
      _currentRegEstimate = Math.min(_regReserveHigh[_qTypeIdx],_currentRegEstimate);
      _currentRegEstimate = Math.max(_regReserveLow[_qTypeIdx],_currentRegEstimate);

      _currentPromEstimate = Math.min(_currentPromEstimate,_minPromBidWithImps);
      _currentPromEstimate = Math.min(_currentRegEstimate + 0.5,_currentPromEstimate);
      _currentPromEstimate = Math.max(_currentRegEstimate,_currentPromEstimate);

      return true;
   }

   public double getRegPrediction() {
      if(AVG_ESTS) {
         return ((_currentRegEstimate+_minRegBidWithImps)/2.0);
      }
      else {
         return _currentRegEstimate;
      }
   }

   public double getPromPrediction() {
      if(EST_PROM) {
         if(AVG_ESTS) {
            return ((_currentPromEstimate+_minPromBidWithImps)/2.0);
         }
         else {
            return _currentPromEstimate;

         }
      }
      else {
         return getRegPrediction()+.25;
      }
   }
}
