package models.queryanalyzer.forecast;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.*;

public class TimeSeriesImpressionForecaster extends AbstractImpressionForecaster {

   RConnection _rConnection;
   int _rConnID;
   Set<Query> _querySpace;
   String[] _agents;
   int _ourIdx;
   List<Map<Query, List<Integer>>> _impressions;
   List<Map<Query, Integer>> _predictions;
   private boolean ARIMA = false;
   private boolean CROSTON = false;
   private boolean ONLY_NON_ZERO = true;


   public TimeSeriesImpressionForecaster(RConnection rConnection, int rConnID, Set<Query> querySpace, String[] agents, int ourIdx) {
      _rConnection = rConnection;
      _rConnID = rConnID;
      loadRLibraries();
      _querySpace = querySpace;
      _agents = agents;
      _ourIdx = ourIdx;
      _impressions = new ArrayList<Map<Query, List<Integer>>>();
      _predictions = new ArrayList<Map<Query, Integer>>();
      for (int i = 0; i < agents.length; i++) {
         HashMap<Query, List<Integer>> impressionMap = new HashMap<Query, List<Integer>>();
         HashMap<Query, Integer> predMap = new HashMap<Query, Integer>();
         int qIdx = 0;
         for (Query q : _querySpace) {
            ArrayList<Integer> impressions = new ArrayList<Integer>();
            impressionMap.put(q, impressions);
            predMap.put(q, -1);
            //Initialize arrays in R
            try {
               _rConnection.voidEval("x" + rConnID + "_" + i + "_" + qIdx + " <- array()");
            } catch (RserveException e) {
               e.printStackTrace();
               throw new RuntimeException();
            }
            qIdx++;
         }
         _impressions.add(impressionMap);
         _predictions.add(predMap);
      }
   }

   public void cleanR() {
      for (int i = 0; i < _agents.length; i++) {
         int qIdx = 0;
         for (Query q : _querySpace) {
            try {
               String ids = _rConnID + "_" + i + "_" + qIdx;
               _rConnection.voidEval("rm(x" + ids + ")");
               _rConnection.voidEval("rm(f" + ids + ")");
               if (!CROSTON) {
                  _rConnection.voidEval("rm(xTS" + ids + ")");
                  if (!ARIMA) {
                     _rConnection.voidEval("rm(etsfit" + ids + ")");
                  } else {
                     _rConnection.voidEval("rm(arimafit" + ids + ")");
                  }
               } else {

               }
            } catch (RserveException e) {
               e.printStackTrace();
               throw new RuntimeException();
            }
            qIdx++;
         }
      }
   }

   public void loadRLibraries() {
      try {
         _rConnection.voidEval("library(forecast)");
      } catch (RserveException e) {
         e.printStackTrace();
         throw new RuntimeException();
      }
   }

   @Override
   public double getPrediction(int agent, Query q) {
      return _predictions.get(agent).get(q);
   }


   @Override
   public boolean updateModel(List<Map<Query, Integer>> allImpressions) {
      for (int i = 0; i < _impressions.size(); i++) {
         if (i != _ourIdx) {
            Map<Query, List<Integer>> impressionHistMap = _impressions.get(i);
            Map<Query, Integer> impressionMap = allImpressions.get(i);
            Map<Query, Integer> predMap = _predictions.get(i);
            int qIdx = 0;
            for (Query q : _querySpace) {
               List<Integer> impHist = impressionHistMap.get(q);
               int imps;
               if (impressionMap.get(q) == null || impressionMap.get(q) == -1) {
                  imps = 0;
               } else {
                  imps = impressionMap.get(q);
               }
               impHist.add(imps);
               impressionHistMap.put(q, impHist);
               int impSize = impHist.size();

               String ids = _rConnID + "_" + i + "_" + qIdx;

               try {
                  _rConnection.voidEval("x" + ids + "[" + impSize + "] <- " + imps);
               } catch (RserveException e) {
                  e.printStackTrace();
                  throw new RuntimeException();
               }

               if (impSize > 15) {
                  try {
                     if (!CROSTON) {
                        _rConnection.voidEval("xTS" + ids + " <- ts(x" + ids + ",f=5)");
                        if (!ARIMA) {
                           _rConnection.voidEval("etsfit" + ids + " <- ets(xTS" + ids + ",model=\"ANN\",additive.only=TRUE)");
                           _rConnection.voidEval("f" + ids + " <- forecast(etsfit" + ids + ",h=2)");
                        } else {
                           _rConnection.voidEval("arimafit" + ids + " <- auto.arima(xTS" + ids + ")");
                           _rConnection.voidEval("f" + ids + " <- forecast(arimafit" + ids + ",h=2)");
                        }
                     } else {
                        _rConnection.voidEval("f" + ids + " <- croston(x" + ids + ",h=2)");
                     }

                     int imp;
                     if (!CROSTON) {
                        imp = _rConnection.eval("f" + ids + "$mean[1]").asInteger();
                     } else {
                        imp = _rConnection.eval("f" + ids + "$mean[1]").asInteger();
                     }
                     imp = (imp < 0) ? 0 : imp;

                     if (!ONLY_NON_ZERO || imp > 0) {
                        predMap.put(q, imp);
                     }

                  } catch (Exception e) {
                     if (!ONLY_NON_ZERO || imps > 0) {
                        predMap.put(q, imps);
                     }
                  }
               } else {
                  //Naive prediction (same as last time)
                  if (!ONLY_NON_ZERO || imps > 0) {
                     predMap.put(q, imps);
                  }
               }
               qIdx++;
            }
         }
      }
      return true;
   }


   @Override
   public AbstractModel getCopy() {
      return new TimeSeriesImpressionForecaster(_rConnection, _rConnID, _querySpace, _agents, _ourIdx);
   }
}
