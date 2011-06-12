package models.prconv;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;

import java.util.HashMap;
import java.util.Set;

public class NewBasicConvPrModel extends AbstractConversionModel {

   private ParticleFilterAbstractUserModel _userModel;
   private Set<Query> _querySpace;
   private HashMap<Query, Double> _baselineConvPr;
   int day = 0;

   public NewBasicConvPrModel(ParticleFilterAbstractUserModel userModel, Set<Query> querySpace, HashMap<Query, Double> baselineConvPr) {
      _userModel = userModel;
      _querySpace = querySpace;
      _baselineConvPr = baselineConvPr;
   }

   @Override
   public double getPrediction(Query q) {
      /*
      * Return baseline probs including the IS effect
       */
      double ISeffect;
      if (q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
         double F0Users = 0.0;
         double ISUsers = 0.0;
         for(Query q2 : _querySpace) {
            if(q2.getManufacturer() != null &&
                    q2.getComponent() != null) {
               Product prod = new Product(q2.getManufacturer(), q2.getComponent());
               F0Users += _userModel.getPrediction(prod, UserState.F0);
               ISUsers += _userModel.getPrediction(prod, UserState.IS);
            }
         }
         ISeffect = F0Users / ((1 / 3.0) * ISUsers + F0Users);
      }
      else if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
         double F1Users = 0.0;
         double ISUsers = 0.0;
         for(Query q2 : _querySpace) {
            if((q.getManufacturer() != null &&
                        q.getManufacturer().equals(q2.getManufacturer()) &&
                        q2.getComponent() != null) ||
                    (q.getComponent() != null &&
                             q.getComponent().equals(q2.getComponent()) &&
                             q2.getManufacturer() != null)) {
               Product prod = new Product(q2.getManufacturer(), q2.getComponent());
               F1Users += .5 * _userModel.getPrediction(prod, UserState.F1);
               ISUsers += .5 * _userModel.getPrediction(prod, UserState.IS);
            }
         }
         ISeffect = F1Users / ((1 / 3.0) * ISUsers + F1Users);
      }
      else if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
         Product prod = new Product(q.getManufacturer(), q.getComponent());
         double F2Users = _userModel.getPrediction(prod, UserState.F2);
         double ISUsers = _userModel.getPrediction(prod, UserState.IS);
         ISeffect = F2Users / ((1 / 3.0) * ISUsers + F2Users);
      }
      else {
         throw new RuntimeException("Malformed query");
      }
      double convPr = _baselineConvPr.get(q) * ISeffect;
      return convPr;
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
      return new NewBasicConvPrModel(_userModel, _querySpace, _baselineConvPr);
   }

}
