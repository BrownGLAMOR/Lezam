package models.bidmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.List;

public class JointDistFilter {


	int ADJUST_BID_ITERATIONS = 5;
	private ArrayList<String> names;
	private Particle[] particles;
	private double[] probDist;
	Random r = new Random();
	double maxReasonableBid;
	
	private class Particle {
				
		HashMap<String, Double> bids;		
		
		//Return the ranking represented by this particle for use in recomputeDistribution
		public HashMap<String, Integer> getRanksHash() {
			HashMap<String, Integer> ret = new HashMap<String, Integer>();
			ArrayList<Pair<Double, String>> sorted = new ArrayList<Pair<Double, String>>();
			for(String s : bids.keySet()) {
				sorted.add( new Pair<Double, String>(bids.get(s), s));
			}
			Collections.sort(sorted);		
			int i = bids.keySet().size();
			for(Pair<Double, String> p : sorted) {			
				ret.put(p.getSecond(), new Integer(i));				
				i--;
			}
			return ret;
		}
		
		public ArrayList<Pair<Double, String>> getRanks() {		
			ArrayList<Pair<Double, String>> sorted = new ArrayList<Pair<Double, String>>();
			for(String s : bids.keySet()) {
				sorted.add( new Pair<Double, String>(bids.get(s), s));
			}
			Collections.sort(sorted);				
			return sorted;
		}
		
		public Particle() {
			bids = new HashMap<String, Double>();
			for(int i = 0; i < names.size(); i++) {
				bids.put(names.get(i), r.nextDouble() * maxReasonableBid);
			}
		}
		
	}
	
	public JointDistFilter(Set<String> snames, double maxbid) {
		maxReasonableBid = maxbid;
		names = new ArrayList<String>(snames);
		particles = new Particle[1000];
		probDist = new double[1000];
		
		for(int i = 0; i < probDist.length; i++) {			
			probDist[i] = .001;
		}
		
		for(int i = 0; i < particles.length; i++) {			
			particles[i] = new Particle();
		}
	}
	
	
	private double recomputeParticleLikelihood(Particle p, HashMap<String, Integer> ranks) {
		double ret = 1;
		HashMap<String, Integer> particleRanks = p.getRanksHash();
		for(String s : names) {			
			double d = Math.abs(particleRanks.get(s) - ranks.get(s));
			d = d / 7;
			ret = ret * Math.exp(- (d * d) / 4.9);
		}
		return ret;
	}
	
	private void recomputeDistribution(HashMap<String, Integer> ranks) {
		int i = 0;
		double sum = 0;
		for(Particle p : particles) {
			double t = recomputeParticleLikelihood(p, ranks);
			probDist[i] = t;
			sum += t;
		}
		for(int j = 0; j < probDist.length; j++) {
			probDist[j] = probDist[j] / sum;
		}
				
	}
	
	//generate a new generation of particles by choosing random old ones weighted by the probability distribution
	private void resample() {
		
		Particle[] p = new Particle[1000];
		for(int i = 0; i < 1000; i++) {
			double d = r.nextDouble() * 1000;
			double sum = -1;
			int index;			
			for(index = 0; sum < d; index++) {
				if(sum < 0) sum = 0;				
				sum += probDist[index];
			}			
			p[i] = particles[index - 1]; 
		}
		particles = p;
	}
	
	private void adjustParticle(Particle p, double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		double low_bid = 0;
		int ourRank = ranks.get("Us"); //How to obtain actual agent name?
		for(int i = 0; i < ADJUST_BID_ITERATIONS; i++) {
			for(String s : ranks.keySet()) {
				int curplayerActualRank = ranks.get(s);
				if(curplayerActualRank == ourRank + 1) {
					p.bids.put(s, cpc);
				} else if (curplayerActualRank == -1) { //When does this happen?
					p.bids.put(s, (r.nextDouble() + low_bid) * (maxReasonableBid - low_bid));
				} else if (curplayerActualRank > ourBid + 1 && curplayerActualRank > cpc ||
						curplayerActualRank < ourBid && p.bids.get(s) <= ourBid) {
					ArrayList<Pair<Double, String>> particleBids = p.getRanks();
					p.bids.put(s, (r.nextDouble() + p.bids.get(particleBids.get(curplayerActualRank + 1).getFirst())) *
							(p.bids.get(particleBids.get(curplayerActualRank - 1).getSecond()) - 
									p.bids.get(particleBids.get(curplayerActualRank + 1).getSecond())));					
				} 
			}
		}
	}
	
	private void adjustBids(double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		for(Particle p : particles) {
			adjustParticle(p, ourBid, cpc, ranks);
		}
		
	}
	
	public void simulateDay(double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		resample();
		adjustBids(ourBid, cpc, ranks);
		recomputeDistribution(ranks);
		
	}
	
	public double getBid(String player) {
		return getBid(player, false);
	}
	
	private int max(double[] arr) {
		int ret = 0;
		double cur = 0;
		for(int i = 0; i < arr.length; i++) {
			if (arr[i] > cur) {
				ret = i;
				cur = arr[i];
			}
		}
		return ret;
	}
	
	//returns the current prediction for the given player's bid. If max is true, uses the most likely particle. Otherwise, does a weighted average of particles.
	public double getBid(String player, boolean max) {
		
		if(max) {
			return particles[max(probDist)].bids.get(player);
		}

		double ret = 0;
		
		for(int i = 0; i < 1000; i++) {
			ret += probDist[i] * particles[i].bids.get(player);
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		
		List name = (List) Arrays.asList("One", "Two", "Three");
		JointDistFilter jdf = new JointDistFilter(new HashSet((Collection) name), 3.75);
		for(double d : jdf.probDist)
			System.out.print(d + "   ");
		HashMap<String, Integer> update = new HashMap<String, Integer>();
		update.put("One", 1);
		update.put("Two", 3);
		update.put("Three", 2);
		jdf.recomputeDistribution(update);
		for(double d : jdf.probDist)
			System.out.print(d + "   ");
		
	}
	
	
	
}
