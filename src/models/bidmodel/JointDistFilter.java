package models.bidmodel;

import edu.umich.eecs.tac.props.QueryType;

import java.util.*;

public class JointDistFilter {

   private ArrayList<String> names;
   private Particle[] particles;
   private double[] probDist;
   Random r;
   double maxReasonableBid;
   String _ourAdvertiser;
   double[] dist;
   public int ADJUST_BID_ITERATIONS = 15;
   public double JUMP_GAP_FRACTION = 0.8;
   public int NUMPARTICLES = 1000;


   public JointDistFilter(Set<String> snames, QueryType q, double maxbid, String ourAdvertiser, int numIters, double gapFrac, int numParticles) {
      r = new Random();
      r.setSeed(5);
      switch (q) {
         case FOCUS_LEVEL_ZERO:
            dist = InitDistributions.initDistF0;
            break;
         case FOCUS_LEVEL_ONE:
            dist = InitDistributions.initDistF1;
            break;
         case FOCUS_LEVEL_TWO:
            dist = InitDistributions.initDistF2;
            break;
      }

      ADJUST_BID_ITERATIONS = numIters;
      JUMP_GAP_FRACTION = gapFrac;
      NUMPARTICLES = numParticles;

      _ourAdvertiser = ourAdvertiser;
      maxReasonableBid = maxbid;
      names = new ArrayList<String>(snames);
      particles = new Particle[NUMPARTICLES];
      probDist = new double[NUMPARTICLES];

      for (int i = 0; i < probDist.length; i++) {
         probDist[i] = 1.0 / NUMPARTICLES;
      }

      for (int i = 0; i < particles.length; i++) {
         particles[i] = new Particle(q);
      }
   }


   private double recomputeParticleLikelihood(Particle p, HashMap<String, Integer> ranks) {
      HashMap<String, Integer> particleRanks = p.getRanksHash();
      double diffSum = 0.0;
      for (String s : names) {
         double d = Math.abs(particleRanks.get(s) - ranks.get(s));
         d = d / 7;
         diffSum += -(d * d) / 4.9;
      }
      double likelihood = Math.exp(diffSum);
      return likelihood;
   }

   private void recomputeDistribution(HashMap<String, Integer> ranks) {
      int i = 0;
      double sum = 0;
      for (Particle p : particles) {
         double t = recomputeParticleLikelihood(p, ranks);
         probDist[i] = t;
         sum += t;
         i++;
      }

      for (int j = 0; j < probDist.length; j++) {
         probDist[j] = probDist[j] / sum;
      }
   }

   //generate a new generation of particles by choosing random old ones weighted by the probability distribution
   private void resample() {
      Particle[] p = new Particle[NUMPARTICLES];
      int index;
      for (int i = 0; i < NUMPARTICLES; i++) {
         double sum = 0;
         double d = r.nextDouble();

         for (index = 0; sum <= d; index++) {
            sum += probDist[index];
         }
         p[i] = particles[index - 1];
      }
      particles = p;
   }


   private double randomInRange(double low, double high) {
      double maxBid = 3.75;
      if (high < low) {
         System.out.println("bad range: " + low + ", " + high);
         return high;
      } else if (high == low) {
         return high;
      } else if (low > maxBid) {
         return maxBid - .0001;
      }

      ArrayList<Double> bidDist = new ArrayList<Double>();
      double startVal = Math.pow(2, (1.0 / 25.0 - 2.0)) - 0.25;
      double aStep = Math.pow(2, (1.0 / 25.0));
      int count = 0;
      for (double curKey = startVal; curKey <= maxBid + 0.001; curKey = (curKey + 0.25) * aStep - 0.25) {
         bidDist.add(curKey);
         count++;
      }

      int low_index = Collections.binarySearch(bidDist, low);
      if (low_index < 0) {
         low_index = -low_index - 1;
      }

      if (high > maxBid) {
         high = maxBid - .0001;
      }

      int high_index = Collections.binarySearch(bidDist, high);
      if (high_index < 0) {
         high_index = -high_index - 1;
      }


      double[] normalized;
      int minIdx;
      if (low_index == 0 && high_index == 0) {
         return rand(0, bidDist.get(0));
      } else if (low_index == 0) {
         minIdx = low_index;
      } else {
         minIdx = low_index - 1;
      }

      normalized = new double[1 + high_index - (minIdx)];
      for (int i = minIdx; i <= high_index; i++) {
         normalized[i - (minIdx)] = dist[i];
      }

      double sum = 0;
      for (int i = 0; i < normalized.length; i++) {
         sum += normalized[i];
      }
      for (int i = 0; i < normalized.length; i++) {
         normalized[i] /= sum;
      }

      double d = r.nextDouble();
      sum = 0;
      int lastIndex = 0;
      for (int j = 0; sum < d && j < normalized.length; j++) {
         sum += normalized[j];
         lastIndex = j;
      }

      double newBid;
      if (low_index == 0 && lastIndex == 0) {
         newBid = rand(0.0, bidDist.get(0));
      } else {
         if (lastIndex == 0) {
            newBid = rand(bidDist.get(minIdx), bidDist.get(minIdx + 1));
         } else {
            newBid = rand(bidDist.get(lastIndex + minIdx - 1), bidDist.get(lastIndex + minIdx));
         }
      }

      return newBid;
   }

   private void adjustParticle(Particle p, double ourBid, double cpc, HashMap<String, Integer> ranks) {
      /*
         * Set the our bid in every particle to our actual bid
         */
      p.bids.put(_ourAdvertiser, ourBid);

      int ourRank = ranks.get(_ourAdvertiser);

      /*
         * Set the person under us bid
         */
      for (String s : ranks.keySet()) {
         int curplayerActualRank = ranks.get(s);
         if (curplayerActualRank == ourRank + 1) {
            if (!Double.isNaN(cpc)) {
               p.bids.put(s, cpc);
            } else {
               p.bids.put(s, randomInRange(0, ourBid));
            }
         }
      }

      for (int i = 0; i < ADJUST_BID_ITERATIONS; i++) {
         boolean bidChanged = false;
         for (String s : ranks.keySet()) {
            if (s != _ourAdvertiser) {
               int curplayerActualRank = ranks.get(s);
               if ((curplayerActualRank > ourRank + 1 && p.bids.get(s) >= cpc) ||
                       (curplayerActualRank < ourRank && p.bids.get(s) <= ourBid)) {

                  ArrayList<Pair<Double, String>> particleBids = p.getSortedBids();
                  if (curplayerActualRank == 0) {
                     if (particleBids.get(curplayerActualRank + 1).getFirst() > maxReasonableBid) { //this guy bid higher than someone who bid higher than max(probable)bid
                        p.bids.put(s, particleBids.get(curplayerActualRank + 1).getFirst() + .01);  //so give this guy an even slightly higher bid
                     } else {
                        /*
                                 * Get bid more than the person below us
                                 */
                        p.bids.put(s, randomInRange(particleBids.get(curplayerActualRank + 1).getFirst(), (1 - JUMP_GAP_FRACTION) * maxReasonableBid + JUMP_GAP_FRACTION * particleBids.get(curplayerActualRank + 1).getFirst()));
                     }
                  } else if (curplayerActualRank == ranks.size() - 1) {
                     /*
                             * Get bid less than person above us
                             */
                     p.bids.put(s, randomInRange(JUMP_GAP_FRACTION * particleBids.get(curplayerActualRank - 1).getFirst(), particleBids.get(curplayerActualRank - 1).getFirst()));
                  } else {
                     /*
                             * Get a bid between the person above and below us
                             */
                     p.bids.put(s, randomInRange(particleBids.get(curplayerActualRank + 1).getFirst(), particleBids.get(curplayerActualRank - 1).getFirst()));
                  }

                  bidChanged = true;
               }
            }
         }
         if (!bidChanged) {
            break;
         }
      }
   }

   private void adjustBids(double ourBid, double cpc, HashMap<String, Integer> ranks) {
      for (Particle p : particles) {
         adjustParticle(p, ourBid, cpc, ranks);
      }
   }

   public void simulateDay(double ourBid, double cpc, HashMap<String, Integer> ranks) {
      resample();
      if(ranks != null) {
         adjustBids(ourBid, cpc, ranks);
         recomputeDistribution(ranks);
      }
   }

   public double getBid(String player) {
      return getBid(player, false);
   }

   private int max(double[] arr) {
      int ret = 0;
      double cur = 0;
      for (int i = 0; i < arr.length; i++) {
         if (arr[i] > cur) {
            ret = i;
            cur = arr[i];
         }
      }
      return ret;
   }

   private double rand(double a, double b) {
      return (b - a) * r.nextDouble() + a;
   }

   //returns the current prediction for the given player's bid. If max is true, uses the most likely particle. Otherwise, does a weighted average of particles.
   public double getBid(String player, boolean max) {
      if (max) {
         return particles[max(probDist)].bids.get(player);
      }

      double ret = 0;
      for (int i = 0; i < NUMPARTICLES; i++) {
         ret += probDist[i] * particles[i].bids.get(player);
      }

      return ret;
   }

   private class Particle {

      HashMap<String, Double> bids;

      //Return the ranking represented by this particle for use in recomputeDistribution
      public HashMap<String, Integer> getRanksHash() {
         HashMap<String, Integer> ret = new HashMap<String, Integer>();

         ArrayList<Pair<Double, String>> sorted = getSortedBids();

         int i = bids.keySet().size();
         for (Pair<Double, String> p : sorted) {
            ret.put(p.getSecond(), new Integer(i));
            i--;
         }
         return ret;
      }

      @SuppressWarnings("unchecked")
      public ArrayList<Pair<Double, String>> getSortedBids() {
         ArrayList<Pair<Double, String>> sorted = new ArrayList<Pair<Double, String>>();
         for (String s : bids.keySet()) {
            sorted.add(new Pair<Double, String>(bids.get(s), s));
         }
         Collections.sort(sorted);

         return sorted;
      }


      public Particle(QueryType q) {
         bids = new HashMap<String, Double>();

         for (int i = 0; i < names.size(); i++) {

            double d = r.nextDouble();
            double sum = 0;
            double lastIndex = 0;
            for (int j = 0; sum < d; j++) {
               sum += dist[j];
               lastIndex = j;
            }
            bids.put(names.get(i), Math.pow(2, (lastIndex / 25.0 - 2.0)) - 0.25);
         }
      }

      /*
         * Copy Constructor
         */
      @SuppressWarnings("unused")
      public Particle(Particle p) {
         bids = new HashMap<String, Double>();
         for (String adv : p.bids.keySet()) {
            bids.put(adv, new Double(((double) p.bids.get(adv))));
         }
      }

   }

}
