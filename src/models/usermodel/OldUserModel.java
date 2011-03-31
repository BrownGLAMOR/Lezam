package models.usermodel;

/**
 * @author jberg
 *
 */

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.Map;

public class OldUserModel extends ParticleFilterAbstractUserModel {

   private double F0users;
   private double F2users;
   private double F1users;
   private double ISusers;
   private double Tusers;
   private double NSusers;

   public OldUserModel() {
      F0users = 570;
      F1users = 412;
      F2users = 444;
      ISusers = 301;
      Tusers = 18;
      NSusers = 8255;
   }

   @Override
   public AbstractModel getCopy() {
      return new OldUserModel();
   }

   @Override
   public int getCurrentEstimate(Product product, UserState userState) {
      if (userState == UserState.F0) {
         return (int) F0users;
      } else if (userState == UserState.F1) {
         return (int) F1users;
      } else if (userState == UserState.F2) {
         return (int) F2users;
      } else if (userState == UserState.IS) {
         return (int) ISusers;
      } else if (userState == UserState.T) {
         return (int) Tusers;
      } else if (userState == UserState.NS) {
         return (int) NSusers;
      } else {
         throw new RuntimeException("");
      }
   }

   @Override
   public int getPrediction(Product product, UserState userState) {
      return getCurrentEstimate(product, userState);
   }

   @Override
   public boolean updateModel(Map<Query, Integer> totalImpressions) {
      return true;
   }

}
