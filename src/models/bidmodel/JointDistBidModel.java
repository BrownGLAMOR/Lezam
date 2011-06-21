package models.bidmodel;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import models.AbstractModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class JointDistBidModel extends AbstractBidModel {

   ArrayList<HashMap<String, Integer>> rankhistory;
   HashMap<Query, JointDistFilter> filters;
   ArrayList<Query> _queries;
   Set<String> _advertisers;
   String _ourAdvertiser;
   HashMap<Query, Double> lastOurBid;
   public int _numIters;
   public double _gapFrac;
   public int _numParticles;

   public JointDistBidModel(Set<String> advertisers, String ourAdvertiser, int numIters, double gapFrac, int numParticles) {

      _advertisers = advertisers;
      _ourAdvertiser = ourAdvertiser;

      _numIters = numIters;
      _gapFrac = gapFrac;
      _numParticles = numParticles;

      //rankhistory = new ArrayList<HashMap<String, Integer>>();
      filters = new HashMap<Query, JointDistFilter>();

      _queries = new ArrayList<Query>();

      lastOurBid = new HashMap<Query, Double>();

      _queries.add(new Query("flat", "dvd"));
      _queries.add(new Query("flat", "tv"));
      _queries.add(new Query("flat", "audio"));
      _queries.add(new Query("pg", "dvd"));
      _queries.add(new Query("pg", "tv"));
      _queries.add(new Query("pg", "audio"));
      _queries.add(new Query("lioneer", "dvd"));
      _queries.add(new Query("lioneer", "tv"));
      _queries.add(new Query("lioneer", "audio"));
      Query q = new Query();
      q.setComponent("tv");
      _queries.add(q);
      q = new Query();
      q.setComponent("dvd");
      _queries.add(q);
      q = new Query();
      q.setComponent("audio");
      _queries.add(q);
      q = new Query();
      q.setManufacturer("pg");
      _queries.add(q);
      q = new Query();
      q.setManufacturer("flat");
      _queries.add(q);
      q = new Query();
      q.setManufacturer("lioneer");
      _queries.add(q);
      _queries.add(new Query());


      for (Query qr : _queries) {
         double curMaxBid = 0;

         if (qr.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
            curMaxBid = maxReasonableBidF0;
         } else if (qr.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
            curMaxBid = maxReasonableBidF1;
         } else if (qr.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
            curMaxBid = maxReasonableBidF2;
         }

//			curMaxBid = maxBid;

         filters.put(qr, new JointDistFilter(advertisers, qr.getType(), curMaxBid, ourAdvertiser, numIters, gapFrac, numParticles));
         lastOurBid.put(qr, 0.0);
      }

   }

   public void setAdvertiser(String ourAdvertiser) {
      _ourAdvertiser = ourAdvertiser;
      System.out.println("updating advertiser: " + ourAdvertiser);
   }


   @Override
   public double getPrediction(String player, Query q) {
      if (player.equals(_ourAdvertiser)) {
         return lastOurBid.get(q);
      } else {
         return filters.get(q).getBid(player);
      }
   }

   @Override
   public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks, HashMap<Query, HashMap<String, Boolean>> allRankable) {
      for (Query q : _queries) {
         if (Double.isNaN(ourBid.get(q)) || ourBid.get(q) < 0) {
            ourBid.put(q, 0.0);
         }
      }

      lastOurBid = ourBid;
      for (Query q : filters.keySet()) {
         filters.get(q).simulateDay(ourBid.get(q), cpc.get(q), ranks.get(q));
      }

      return true;
   }

   @Override
   public String toString() {
      return "JointDistBidModel(" + _numIters + ", " + _gapFrac + ", " + _numParticles + ")";
   }

   @Override
   public AbstractModel getCopy() {
      return new JointDistBidModel(_advertisers, _ourAdvertiser, _numIters, _gapFrac, _numParticles);
   }

}
