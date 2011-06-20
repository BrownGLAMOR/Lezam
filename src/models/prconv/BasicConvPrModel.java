package models.prconv;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.usermodel.AbstractUserModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;
import java.util.Set;

public class BasicConvPrModel extends AbstractConversionModel {

   private AbstractUserModel _userModel;
   private Set<Query> _querySpace;
   private HashMap<Query, Double> _baselineConvPr;
   int day = 0;

   public BasicConvPrModel(AbstractUserModel userModel, Set<Query> querySpace, HashMap<Query, Double> baselineConvPr) {
      _userModel = userModel;
      _querySpace = querySpace;
      _baselineConvPr = baselineConvPr;
   }

   @Override
   public double getPrediction(Query q) {
      double ISeffect;
      Product prod = new Product("pg", "tv");
      if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
         double F0Users = _userModel.getPrediction(prod, GameStatusHandler.UserState.F0, day);
         double ISUsers = _userModel.getPrediction(prod, GameStatusHandler.UserState.IS, day);
         ISeffect = F0Users / ((1 / 3.0) * ISUsers + F0Users);
      } else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
         double F1Users = _userModel.getPrediction(prod, GameStatusHandler.UserState.F1, day);
         double ISUsers = _userModel.getPrediction(prod, GameStatusHandler.UserState.IS, day);
         ISeffect = F1Users / ((1 / 3.0) * ISUsers + F1Users);
      } else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
         double F2Users = _userModel.getPrediction(prod, GameStatusHandler.UserState.F2, day);
         double ISUsers = _userModel.getPrediction(prod, GameStatusHandler.UserState.IS, day);
         ISeffect = F2Users / ((1 / 3.0) * ISUsers + F2Users);
      } else {
         throw new RuntimeException("Malformed query");
      }
      return _baselineConvPr.get(q) * ISeffect;
   }

   @Override
   public double getPredictionWithBid(Query query, double bid) {
      return getPrediction(query);
   }

   @Override
   public double getPredictionWithPos(Query query, double pos) {
      return getPrediction(query);
   }

   @Override
   public void setSpecialty(String manufacturerSpecialty,
                            String componentSpecialty) {
   }

   @Override
   public void setTimeHorizon(int min) {
   }

   @Override
   public boolean updateModel(QueryReport queryReport,
                              SalesReport salesReport, BidBundle bundle) {
      day++;
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new BasicConvPrModel(_userModel, _querySpace, _baselineConvPr);
   }

}
