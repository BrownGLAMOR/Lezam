package models.paramest;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryType;

/**
 * @author jberg
 */
public class ConstantsAndFunctions {

   // Target Effect
   public static final double _TE = 0.5;
   // Promoted Slot Bonus
   public static final double _PSB = 0.5;
   // Advertiser effect lower bound <> upper bound
   public static final double[][] _advertiserEffectBounds = {{0.2, 0.3},
           {0.3, 0.4},
           {0.4, 0.5}};

   // Average advertiser effect
   public static final double[] _advertiserEffectBoundsAvg = {
           (_advertiserEffectBounds[0][0] + _advertiserEffectBounds[0][1]) / 2,
           (_advertiserEffectBounds[1][0] + _advertiserEffectBounds[1][1]) / 2,
           (_advertiserEffectBounds[2][0] + _advertiserEffectBounds[2][1]) / 2};

   // Continuation Probability lower bound <> upper bound
   public static final double[][] _continuationProbBounds = {{0.2, 0.5},
           {0.3, 0.6},
           {0.4, 0.7}};

   // Average continuation probability
   public static final double[] _continuationProbBoundsAvg = {
           (_continuationProbBounds[0][0] + _continuationProbBounds[0][1]) / 2,
           (_continuationProbBounds[1][0] + _continuationProbBounds[1][1]) / 2,
           (_continuationProbBounds[2][0] + _continuationProbBounds[2][1]) / 2};


   public static final double[] _regReserveLow = {.08, .29, .46};
   public static final double[] _regReserveHigh = {.29, .46, .6};
   
   public static final double[] _regReserveAvg = {(_regReserveLow[0] + _regReserveHigh[0]) / 2.0,
           (_regReserveLow[1] + _regReserveHigh[1]) / 2.0,
           (_regReserveLow[2] + _regReserveHigh[2]) / 2.0};

   // first index:
   // 0 - untargeted
   // 1 - targeted correctly
   // 2 - targeted incorrectly
   // second index:
   // 0 - not promoted
   // 1 - promoted
   public static final double[][] fTargetfPro = {{(1.0), (1.0) * (1.0 + _PSB)},
           {(1.0 + _TE), (1.0 + _TE) * (1.0 + _PSB)},
           {(1.0) / (1.0 + _TE), ((1.0) / (1.0 + _TE)) * (1.0 + _PSB)}};

   // Turns a boolean into binary
   public static int bool2int(boolean bool) {
      if (bool) {
         return 1;
      }
      return 0;
   }

   // returns the corresponding index for the targeting part of fTargetfPro
   public static int getFTargetIndex(boolean targeted, Product p, Product target) {
      if (!targeted || p == null || target == null) {
         return 0; //untargeted
      } else if (p.equals(target)) {
         return 1; //targeted correctly
      } else {
         return 2; //targeted incorrectly
      }
   }

   // Turns a query type into 0/1/2
   public static int queryTypeToInt(QueryType qt) {
      if (qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return 0;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_ONE)) {
         return 1;
      }
      if (qt.equals(QueryType.FOCUS_LEVEL_TWO)) {
         return 2;
      }
      System.out.println("Error in queryTypeToInt");
      return 2;
   }

   // Calculate the forward click probability as defined on page 14 of the
   // spec.
   public static double etaClickPr(double advertiserEffect, double fTargetfPro) {
      return (advertiserEffect * fTargetfPro) / ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
   }

   // Calculate the inverse of the forward click probability
   public static double clickPrtoE(double probClick, double fTargetfPro) {
      return probClick / (probClick + fTargetfPro - probClick * fTargetfPro);
   }

}