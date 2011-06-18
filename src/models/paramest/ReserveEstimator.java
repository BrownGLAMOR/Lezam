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
   double _currentEstimate;
   double _minBidWithImps;

   public ReserveEstimator(QueryType qt, Set<Query> querySpace) {
      _queryType = qt;
      _querySpace = querySpace;

      _qTypeIdx = queryTypeToInt(_queryType);
      _currentEstimate = _regReserveLow[_qTypeIdx];
      _minBidWithImps = _regReserveHigh[_qTypeIdx];
   }

   public boolean updateModel(BidBundle bundle, QueryReport queryReport) {
      /*
       * Check if there are any times that our bid was above what we believe
       * the reserve to be, and we weren't in the auction when we could have been
       */
      for(Query q : _querySpace) {
         if(q.getType().equals(_queryType)) {
            double bid = bundle.getBid(q);
            if(queryReport.getImpressions(q) == 0 && bid > _currentEstimate) {
               boolean avgPos5 = false;
               for(int i = 0; i < 8; i++) {
                  if(queryReport.getPosition(q,"adv" + (i+1)) == 5.0) {
                     avgPos5 = true;
                     break;
                  }
               }

               if(!avgPos5) {
                  //This means that there was no one in slot 5 the whole time
                  //and we weren't in the auction, so our bid is below the reserve
                  _currentEstimate = bid;
               }
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
            if(queryReport.getImpressions(q) > 0 && bid < _minBidWithImps) {
               _minBidWithImps = bid;
            }
         }
      }

      _currentEstimate = Math.min(_currentEstimate,_minBidWithImps);

      return true;
   }

   public double getPrediction() {
      return _currentEstimate;
   }
}
