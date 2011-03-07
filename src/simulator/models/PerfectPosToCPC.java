package simulator.models;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.postocpc.AbstractPosToCPC;
import simulator.Reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class PerfectPosToCPC extends AbstractPosToCPC {

   private HashMap<Query, HashMap<Double, Reports>> _allReportsMap;
   private HashMap<Query, double[]> _potentialBidsMap;
   private HashMap<Query, HashMap<Double, Double>> _posToBidMap;

   public PerfectPosToCPC(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap, HashMap<Query, HashMap<Double, Double>> posToBidMap) {
      _allReportsMap = allReportsMap;
      _potentialBidsMap = potentialBidsMap;
      _posToBidMap = posToBidMap;
   }

   @Override
   public double getPrediction(Query query, double pos) {
      if (Double.isNaN(pos)) {
         return 0.0;
      }
      HashMap<Double, Double> posToBid = _posToBidMap.get(query);
      Set<Double> posToBidSet = posToBid.keySet();
      ArrayList<Double> posToBidArrList = new ArrayList<Double>(posToBidSet);
      Collections.sort(posToBidArrList);
      double[] posToBidArr = new double[posToBidArrList.size()];
      for (int i = 0; i < posToBidArr.length; i++) {
         posToBidArr[i] = posToBidArrList.get(i);
      }
      pos = getClosestPos(posToBidArr, pos);
      double bid = posToBid.get(pos);
      HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
      Reports reports = queryReportMaps.get(bid);
      double avgCPC = reports.getCPC(query);
      return avgCPC;
   }

   /*
     * Need to get closest that IS larger
     */
   private double getClosestPos(double[] arr, double pos) {
      double lastDiff = Double.MAX_VALUE;
      int idx = -1;
      for (int i = 0; i < arr.length; i++) {
         double elem = arr[i];
         if (elem == pos) {
            idx = i;
            break;
         }
         double diff = pos - elem;
         diff = Math.abs(diff);
         if (diff < lastDiff) {
            lastDiff = diff;
         } else {
            idx = i - 1;
            break;
         }
      }
      if (idx == -1) {
         idx = arr.length - 1;
      }
      if (arr[idx] > pos) {
         return arr[idx];
      } else {
         if (idx == arr.length - 1 || arr[idx] == pos) {
            return arr[idx];
         } else {
            return arr[idx + 1];
         }
      }
   }

   @Override
   public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new PerfectPosToCPC(_allReportsMap, _potentialBidsMap, _posToBidMap);
   }

   @Override
   public String toString() {
      return "PerfectBidToCPC";
   }

}
