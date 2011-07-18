package models.paramest;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;

public class NaiveParameterEstimation extends AbstractParameterEstimation {


   @Override
   public double getAdvEffectPrediction(Query q) {
      if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return .25;
      }
      else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
         return .35;
      }
      else {
         return .45;
      }
   }

   @Override
   public double getContProbPrediction(Query q) {
      if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return .35;
      }
      else if(q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
         return .45;
      }
      else {
         return .55;
      }
   }

   @Override
   public double getRegReservePrediction(QueryType qt) {
      if(qt.equals(QueryType.FOCUS_LEVEL_ZERO)) {
         return (0.08 + 0.29) / 2.0;
      }
      else if(qt.equals(QueryType.FOCUS_LEVEL_TWO)) {
         return (0.29 + 0.46) / 2.0;
      }
      else {
         return (0.46 + 0.6) / 2.0;
      }
   }

   @Override
   public double getPromReservePrediction(QueryType qt) {
      return (getRegReservePrediction(qt) + 0.25);
   }

   @Override
   public boolean updateModel(QueryReport queryReport, BidBundle bidBundle, HashMap<Query, int[]> impressions, HashMap<Query, int[][]> allWaterfalls, HashMap<Product, HashMap<GameStatusHandler.UserState, Double>> userStates) {
      return false;
   }

   @Override
   public AbstractModel getCopy() {
      return new NaiveParameterEstimation();
   }
}
