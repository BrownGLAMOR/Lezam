package simulator;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import simulator.predictions.BidPredModelTest.BidPair;

import java.util.*;

public class SimAgent {

   public boolean _is2009 = false;

   private HashMap<Query, Double> _bids;
   private HashMap<Query, Double> _budgets;
   private double _totBudget;
   private HashMap<Query, Double> _advEffect;
   private HashMap<Query, Ad> _adType;
   private Integer[] _salesOverWindow;
   private String _manSpecialty;
   private String _compSpecialty;
   private double _squashing;
   private Set<Query> _querySpace;

   private HashMap<Query, Double> _CPC;
   private HashMap<Query, Double> _cost;
   private HashMap<Query, Double> _revenue;
   private HashMap<Query, Integer> _unitsSold;
   private HashMap<Query, Integer> _numClicks;
   private HashMap<Query, Integer> _regImps;
   private HashMap<Query, Integer> _promImps;
   private HashMap<Query, Double> _posSum;
   private HashMap<Query, double[]> _perQPosSum;
   private int _totUnitsSold;
   private int _totClicks;
   private double _totCost;
   private double _totRevenue;
   private String _advId;
   private int _capacity;
   private int _prevConvs;

   public SimAgent(HashMap<Query, Double> bids,
                   HashMap<Query, Double> budgets,
                   double totBudget,
                   HashMap<Query, Double> advEffect,
                   HashMap<Query, Ad> adType,
                   Integer[] salesOverWindow,
                   int capacity,
                   String manSpecialty,
                   String compSpecialty,
                   String advId,
                   double squashing,
                   Set<Query> querySpace) {
      _bids = bids;
      _budgets = budgets;
      _totBudget = totBudget;
      _advEffect = advEffect;
      _adType = adType;
      _salesOverWindow = salesOverWindow;
      _capacity = capacity;
      _manSpecialty = manSpecialty;
      _compSpecialty = compSpecialty;
      _advId = advId;
      _squashing = squashing;
      _querySpace = querySpace;

      _CPC = new HashMap<Query, Double>();
      _cost = new HashMap<Query, Double>();
      _revenue = new HashMap<Query, Double>();
      _unitsSold = new HashMap<Query, Integer>();
      _numClicks = new HashMap<Query, Integer>();
      _regImps = new HashMap<Query, Integer>();
      _promImps = new HashMap<Query, Integer>();
      _posSum = new HashMap<Query, Double>();
      _perQPosSum = new HashMap<Query, double[]>();
      _totUnitsSold = 0;
      _totClicks = 0;
      _totCost = 0.0;
      _totRevenue = 0.0;
      for (Query query : _querySpace) {
         _CPC.put(query, 0.0);
         _cost.put(query, 0.0);
         _revenue.put(query, 0.0);
         _unitsSold.put(query, 0);
         _numClicks.put(query, 0);
         _regImps.put(query, 0);
         _promImps.put(query, 0);
         _posSum.put(query, 0.0);
         double[] perQPosSum = new double[5];
         for (int i = 0; i < 5; i++) {
            perQPosSum[i] = 0;
         }
         _perQPosSum.put(query, perQPosSum);
      }
      _prevConvs = 0;
      for (int i = 0; i < _salesOverWindow.length - 1; i++) {
         _prevConvs += _salesOverWindow[i];
      }

   }

   public double getSquashedBid(Query query) {
      return Math.pow(_advEffect.get(query), _squashing) * _bids.get(query);
   }

   public double getBid(Query query) {
      return _bids.get(query);
   }

   public double getAdvEffect(Query query) {
      return _advEffect.get(query);
   }

   public String getManSpecialty() {
      return _manSpecialty;
   }

   public String getCompSpecialty() {
      return _compSpecialty;
   }

   public double getBudget(Query query) {
      return _budgets.get(query);
   }

   public double getTotBudget() {
      return _totBudget;
   }

   public Ad getAd(Query query) {
      return _adType.get(query);
   }

   public double getCPC(Query query) {
      return _CPC.get(query);
   }

   public void setCPC(Query query, double cpc) {
      _CPC.put(query, cpc);
   }

   public double getCost(Query query) {
      return _cost.get(query);
   }

   public void setCost(Query query, double cost) {
      _cost.put(query, cost);
   }

   public void addCost(Query query, double cost) {
      _cost.put(query, _cost.get(query) + cost);
      _totCost += cost;
      _totClicks++;
      _numClicks.put(query, _numClicks.get(query) + 1);
   }

   public double getRevenue(Query query) {
      return _revenue.get(query);
   }

   public void setRevenue(Query query, double revenue) {
      _revenue.put(query, revenue);
   }

   public void addRevenue(Query query, double revenue) {
      _revenue.put(query, _revenue.get(query) + revenue);
      _totRevenue += revenue;
      addUnitSold(query);
   }

   public double getUnitsSold(Query query) {
      return _unitsSold.get(query);
   }

   public void setUnitsSold(Query query, int unitsSold) {
      _unitsSold.put(query, unitsSold);
   }

   public void addUnitSold(Query query) {
      _unitsSold.put(query, _unitsSold.get(query) + 1);
      _totUnitsSold++;
   }

   public void addImpressions(Query query, int regImps, int promImps, int pos) {
      _regImps.put(query, _regImps.get(query) + regImps);
      _promImps.put(query, _promImps.get(query) + promImps);
      _posSum.put(query, _posSum.get(query) + pos);
      double[] perQPosSum = _perQPosSum.get(query);
      perQPosSum[pos - 1] = perQPosSum[pos - 1] + 1;
      _perQPosSum.put(query, perQPosSum);
   }

   public int getOverCap() {
      int overCap = _prevConvs + _totUnitsSold - _capacity;
      return Math.max(overCap, 0);
   }

   public double getTotCost() {
      return _totCost;
   }

   public double getTotRevenue() {
      return _totRevenue;
   }

   public double getTotUnitsSold() {
      return _totUnitsSold;
   }

   public String getAdvId() {
      return _advId;
   }

   public int getNumClicks(Query query) {
      return _numClicks.get(query);
   }

   public int getNumPromImps(Query query) {
      return _promImps.get(query);
   }

   public int getNumRegImps(Query query) {
      return _regImps.get(query);
   }

   public double getPosSum(Query query) {
      return _posSum.get(query);
   }

   public double[] getPerQPosSum(Query query) {
      return _perQPosSum.get(query);
   }

   public QueryReport buildQueryReport(ArrayList<SimAgent> agents) {
      QueryReport queryReport = new QueryReport();
      if (_is2009) {
         for (Query query : _querySpace) {
            queryReport.addQuery(query, _regImps.get(query), _promImps.get(query), _numClicks.get(query), _cost.get(query), _posSum.get(query));
            queryReport.setAd(query, _adType.get(query));

            for (int i = 0; i < agents.size(); i++) {
               queryReport.setAd(query, "adv" + (i + 1), agents.get(i)._adType.get(query));
               queryReport.setPosition(query, "adv" + (i + 1), (agents.get(i)._posSum.get(query)) / (agents.get(i)._regImps.get(query) + agents.get(i)._promImps.get(query)));
            }
         }
      } else {
         /*
             * set our own true position
             */
         for (Query query : _querySpace) {
            queryReport.addQuery(query, _regImps.get(query), _promImps.get(query), _numClicks.get(query), _cost.get(query), _posSum.get(query));
            queryReport.setAd(query, _adType.get(query));
         }

         /*
             * set sample avg positions for everyone else
             */
         for (Query query : _querySpace) {
            ArrayList<BidPair> bidPairs = new ArrayList<BidPair>();
            for (int agentInner = 0; agentInner < agents.size(); agentInner++) {
               double squashedBid = agents.get(agentInner).getSquashedBid(query);
               bidPairs.add(new BidPair(agentInner, squashedBid));
            }

            Collections.sort(bidPairs);

            int[] order = new int[bidPairs.size()];
            for (int i = 0; i < bidPairs.size(); i++) {
               order[i] = bidPairs.get(i).getID();
            }

            int[] impressions = new int[bidPairs.size()];
            for (int agentInner = 0; agentInner < agents.size(); agentInner++) {
               impressions[agentInner] = agents.get(agentInner)._promImps.get(query) + agents.get(agentInner)._regImps.get(query);
            }


            int[][] allImpressions = greedyAssign(5, bidPairs.size(), order, impressions);
            int maxImps = getMaxImps(5, bidPairs.size(), order, impressions);

            /*
                 * TODO
                 * these samples should be drawn using seeing from the main simulator
                 */
            Random r = new Random();
            ArrayList<Integer> timeSlices = new ArrayList<Integer>();
            for (int i = 0; i < 10; i++) {
               timeSlices.add(r.nextInt(maxImps));
            }

            Collections.sort(timeSlices);

            ArrayList<ArrayList<Integer>> impTimeSlices = new ArrayList<ArrayList<Integer>>();
            for (int i = 0; i < agents.size(); i++) {
               ArrayList<Integer> impsSeen = new ArrayList<Integer>();
               int[] impSums = new int[5];
               impSums[0] = allImpressions[i][0];
               for (int j = 1; j < 5; j++) {
                  impSums[j] = impSums[j - 1] + allImpressions[i][j];
               }

               for (int j = 0; j < timeSlices.size(); j++) {
                  int impNum = timeSlices.get(j);
                  boolean addedImp = false;
                  for (int k = 0; k < 5; k++) {
                     if (impNum <= impSums[k]) {
                        impsSeen.add(k + 1);
                        addedImp = true;
                        break;
                     }
                  }
                  if (!addedImp) {
                     break; //if we get here it means we are out of the auction
                  }
               }

               impTimeSlices.add(impsSeen);
            }

            for (int i = 0; i < agents.size(); i++) {
               queryReport.setAd(query, "adv" + (i + 1), agents.get(i)._adType.get(query));

               ArrayList<Integer> ourTimeSlices = impTimeSlices.get(i);
               int impSum = 0;
               for (int j = 0; j < ourTimeSlices.size(); j++) {
                  impSum += ourTimeSlices.get(j);
               }

               if (impSum == 0) {
                  queryReport.setPosition(query, "adv" + (i + 1), Double.NaN);
               } else {
                  queryReport.setPosition(query, "adv" + (i + 1), ((double) impSum) / ourTimeSlices.size());
               }
            }
         }
      }
      return queryReport;
   }

   public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
      int[][] impressionsBySlot = new int[agents][slots];

      int[] slotStart = new int[slots];
      int a;

      for (int i = 0; i < agents; ++i) {
         a = order[i];
         //System.out.println(a);
         int remainingImp = impressions[a];
         //System.out.println("remaining impressions "+ impressions[a]);
         for (int s = Math.min(i + 1, slots) - 1; s >= 0; --s) {
            if (s == 0) {
               impressionsBySlot[a][0] = remainingImp;
               slotStart[0] += remainingImp;
            } else {

               int r = slotStart[s - 1] - slotStart[s];
               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
               assert (r >= 0);
               if (r < remainingImp) {
                  remainingImp -= r;
                  impressionsBySlot[a][s] = r;
                  slotStart[s] += r;
               } else {
                  impressionsBySlot[a][s] = remainingImp;
                  slotStart[s] += remainingImp;
                  break;
               }
            }
         }
      }
      return impressionsBySlot;
   }

   public int getMaxImps(int slots, int agents, int[] order, int[] impressions) {
      int[][] impressionsBySlot = new int[agents][slots];

      int[] slotStart = new int[slots];
      int a;

      for (int i = 0; i < agents; ++i) {
         a = order[i];
         //System.out.println(a);
         int remainingImp = impressions[a];
         //System.out.println("remaining impressions "+ impressions[a]);
         for (int s = Math.min(i + 1, slots) - 1; s >= 0; --s) {
            if (s == 0) {
               impressionsBySlot[a][0] = remainingImp;
               slotStart[0] += remainingImp;
            } else {

               int r = slotStart[s - 1] - slotStart[s];
               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
               assert (r >= 0);
               if (r < remainingImp) {
                  remainingImp -= r;
                  impressionsBySlot[a][s] = r;
                  slotStart[s] += r;
               } else {
                  impressionsBySlot[a][s] = remainingImp;
                  slotStart[s] += remainingImp;
                  break;
               }
            }
         }
      }
      return slotStart[0];
   }

   public SalesReport buildSalesReport() {
      SalesReport salesReport = new SalesReport();
      for (Query query : _querySpace) {
         salesReport.addQuery(query);
         salesReport.addConversions(query, _unitsSold.get(query));
         salesReport.addRevenue(query, _revenue.get(query));
      }
      return salesReport;
   }

}
