package models.bidmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

import models.AbstractModel;

public class IndependentBidModel extends AbstractBidModel{
 
	
	/**************************************************************************************************************************************
	WHAT DOES IT GIVE US IF WE NEVER GOT A CLICK for cpc??? (currently assumed 0.0 is returned)
	IS CPC in squashed bid form? is our bid also? are neither (should be in same form)
	HOW DO YOU SELECT Y advertiser (equation 8) currently random and not the same as the current advertiser
	MAKE SURE THEY GIVE US EVERYONE AT SAME RANK WHEN THEY HAVE NOT YETGOTTEN RANK INFO
	GET BETTER STARTING DISTRIBUTIONS
	***************************************************************************************************************************************/
	private static final double aStep = Math.pow(2, (1.0/25.0));
	private HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> bidDist;
	private HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> predDist;
	private HashMap<Query, HashMap<String, ArrayList<Double>>> curValue;
	private HashMap<Query, HashMap<String, ArrayList<Double>>> predValue;
	private double[] transProbs;
	private int numBidValues;
	private ArrayList<Query> _query;
	private Random _r;
	private static final double randomJumpProb = 0.0;
	private static final double yesterdayProb = 0.7;
	private static final double nDaysAgoProb = 0.3;
	private static final double normVar = 6; 
	private static final int numIterations = 2;
	private String ourAgent;
	Set<String> _advertisers;
	private static final boolean printlns = false;
	
	public static void main(String[] args){
		HashSet<String> ads = new HashSet<String>();
		ads.add("A");
		ads.add("B");
		ads.add("C");
		IndependentBidModel myIBM = new IndependentBidModel(ads, "A");
	}
	
	public IndependentBidModel(Set<String> advertisers, String me){
		
		_advertisers = advertisers;
		
		if(printlns)
			System.out.println("Reinitializing");
		_query = new ArrayList<Query>();
		_query.add(new Query("flat", "dvd"));
		_query.add(new Query("flat", "tv"));
		_query.add(new Query("flat", "audio"));
		_query.add(new Query("pg", "dvd"));
		_query.add(new Query("pg", "tv"));
		_query.add(new Query("pg", "audio"));
		_query.add(new Query("lioneer", "dvd"));
		_query.add(new Query("lioneer", "tv"));
		_query.add(new Query("lioneer", "audio"));
		_query.add(new Query());
		_query.add(new Query("flat", null));
		_query.add(new Query("pg", null));
		_query.add(new Query("lioneer", null));
		_query.add(new Query(null, "dvd"));
		_query.add(new Query(null, "tv"));
		_query.add(new Query(null, "audio"));
		
		_r = new Random();
		
		ourAgent = me;
		
		bidDist = new HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>>();
		predDist = new HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>>();
		predValue = new HashMap<Query, HashMap<String, ArrayList<Double>>>();
		curValue = new HashMap<Query, HashMap<String, ArrayList<Double>>>();
		Double startVal = Math.pow(2, (1.0/25.0-2.0))-0.25;
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curStringMap = new HashMap<String, ArrayList<ArrayList<Double>>>();
			HashMap<String, ArrayList<ArrayList<Double>>> curStringMapTwo = new HashMap<String, ArrayList<ArrayList<Double>>>();
			HashMap<String, ArrayList<Double>> curStringMapThree = new HashMap<String, ArrayList<Double>>();
			HashMap<String, ArrayList<Double>> curStringMapFour = new HashMap<String, ArrayList<Double>>();
			bidDist.put(q, curStringMap);
			predDist.put(q, curStringMapTwo);
			predValue.put(q, curStringMapThree);
			curValue.put(q, curStringMapFour);
			//System.out.print("Initializing with agents: ");
			for(String s:advertisers){
				//System.out.print(s+", ");
				ArrayList<ArrayList<Double>> curDoubleMap = new ArrayList<ArrayList<Double>>();
				ArrayList<ArrayList<Double>> curDoubleMapTwo = new ArrayList<ArrayList<Double>>();
				ArrayList<Double> curDoubleALTwo = new ArrayList<Double>();
				ArrayList<Double> curDoubleALThree = new ArrayList<Double>();
				curStringMap.put(s, curDoubleMap);
				curStringMapTwo.put(s, curDoubleMapTwo);
				curStringMapThree.put(s, curDoubleALTwo);
				curStringMapFour.put(s, curDoubleALThree);
				numBidValues = 0;
				ArrayList<Double> curDoubleAL = new ArrayList<Double>();
				int index = 0;
				for(Double curKey = startVal; curKey <= maxBid+0.001; curKey = (curKey+0.25)*aStep-0.25){
					if(q.getType()==QueryType.FOCUS_LEVEL_ZERO){
						curDoubleAL.add(InitDistributions.initDistF0[index]);
					}
					else if(q.getType()==QueryType.FOCUS_LEVEL_ONE){
						curDoubleAL.add(InitDistributions.initDistF1[index]);
					}
					else if(q.getType()==QueryType.FOCUS_LEVEL_TWO){
						curDoubleAL.add(InitDistributions.initDistF2[index]);
					}
					
					//curDoubleAL.add(1.0);//TODO: initialize to more intelligent values
					//System.out.print(""+curKey+", ");
					numBidValues++;
					index++;
				}
				curDoubleMap.add(curDoubleAL);
				//System.out.println();
			}
			//System.out.println();
		}
		normalizeLastDay(bidDist);
		transProbs = new double[numBidValues];
		for(int i = 0; i<numBidValues; i++){
			transProbs[i] = normalDensFn(i);
		}
		normalize(transProbs);
		genCurEst();
		pushForwardsPrediction();
	}
	
	private void genCurEst() {
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curStrHM = bidDist.get(q);
			Set<String> curStrKey = curStrHM.keySet();
			for(String s:curStrKey){
				ArrayList<ArrayList<Double>> curHist = bidDist.get(q).get(s);
				ArrayList<Double> newPred = curHist.get(curHist.size()-1);
				curValue.get(q).get(s).add(averageAL(newPred));
			}
		}
	}

	private void normalize(double[] transProbs2) {
		double sum = 0.0;
		for(int i = 0; i <transProbs2.length; i++){
			sum+= transProbs2[i];
		}
		for(int i = 0; i <transProbs2.length; i++){
			transProbs2[i] /= sum;
		}
	}
	
	private void normalizeAL(ArrayList<Double> transProbs2) {
		double sum = 0.0;
		for(int i = 0; i <transProbs2.size(); i++){
			sum+= transProbs2.get(i);
		}
		for(int i = 0; i <transProbs2.size(); i++){
			transProbs2.set(i, transProbs2.get(i)/sum);
		}
	}

	private double normalDensFn(double d){
		return Math.exp((-(d*d))/(2.0*normVar))/(Math.sqrt(2.0*Math.PI*normVar));
	}
	
	private void pushForwardsPrediction() {
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curStrHM = bidDist.get(q);
			Set<String> curStrKey = curStrHM.keySet();
			for(String s:curStrKey){
				ArrayList<ArrayList<Double>> curHist = bidDist.get(q).get(s);
				ArrayList<Double> newPred = pushForward(curHist, 2, q);
				predDist.get(q).get(s).add(newPred);
				predValue.get(q).get(s).add(averageAL(newPred));
			}
		}
	}

	private Double averageAL(ArrayList<Double> newPred) {
		double sum = 0.0;
		for(int i =0; i<newPred.size(); i++){
			sum += (newPred.get(i)*indexToBidValue(i));
		}
		return sum;
	}

	private ArrayList<Double> pushForward(ArrayList<ArrayList<Double>> arrayList, int iterations, Query q) {
		ArrayList<Double> startingList = arrayList.get(arrayList.size()-1);
		ArrayList<Double> toRet;
		for(int i=0; i<iterations; i++){
			toRet = new ArrayList<Double>();
			for(int j = 0; j < numBidValues; j++){
				double toAdd = 0.0;//randomJumpProb/startingList.size();
				if(q.getType()==QueryType.FOCUS_LEVEL_ZERO){
					toAdd += randomJumpProb*(InitDistributions.initDistF0[j]);
				}
				else if(q.getType()==QueryType.FOCUS_LEVEL_ONE){
					toAdd +=  randomJumpProb*(InitDistributions.initDistF1[j]);
				}
				else if(q.getType()==QueryType.FOCUS_LEVEL_TWO){
					toAdd += randomJumpProb*(InitDistributions.initDistF2[j]);
				}
				for(int k = 0; k < numBidValues; k++){
					toAdd += yesterdayProb*transProbs[Math.abs(k-j)]*startingList.get(k);
				}
				for(int k = 0; k < numBidValues; k++){
					if(arrayList.size()>=5){
						toAdd += nDaysAgoProb*transProbs[Math.abs(k-j)]*arrayList.get(arrayList.size()-5+i).get(k);
					}
					else
					{
						toAdd += nDaysAgoProb*transProbs[Math.abs(k-j)]*arrayList.get(0).get(k);
					}
				}
				toRet.add(toAdd);
			}
			startingList = toRet;
		}
		return startingList;
	}

	private void normalizeLastDay(HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> toNorm) {
//		Collection<HashMap<String, ArrayList<ArrayList<Double>>>> hms = toNorm.values();
//		for(HashMap<String, ArrayList<ArrayList<Double>>> curHM:hms){
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curHM = toNorm.get(q);
			Collection<ArrayList<ArrayList<Double>>> dHMs = curHM.values();
			for(ArrayList<ArrayList<Double>> curDHM:dHMs){
				ArrayList<Double> curAL = curDHM.get(curDHM.size()-1);
				double sum = 0;
				for(int i = 0; i<curAL.size(); i++){
					sum += curAL.get(i);
				}
				if(sum>0&&sum<100000){
					for(int i = 0; i<curAL.size(); i++){
						curAL.set(i, curAL.get(i)/sum);
						//System.out.print(""+curAL.get(curAL.size()-1)+", ");
					}
				}
				else
				{
					for(int i = 0; i<curAL.size(); i++){
						if(q.getType()==QueryType.FOCUS_LEVEL_ZERO){
							curAL.set(i,InitDistributions.initDistF0[i]);
						}
						else if(q.getType()==QueryType.FOCUS_LEVEL_ONE){
							curAL.set(i,InitDistributions.initDistF1[i]);
						}
						else if(q.getType()==QueryType.FOCUS_LEVEL_TWO){
							curAL.set(i,InitDistributions.initDistF2[i]);
						}
						//System.out.print(""+curAL.get(curAL.size()-1)+", ");
					}
				}
			}
		}
		//System.out.println();
	}
	
	private double indexToBidValue(int index){
		return Math.pow(2.0, (((double)(index))+1.0)/25.0-2.0)-0.25;
	}

	@Override
	public double getPrediction(String player, Query q) {
		/*ArrayList<Double> toRet = predValue.get(q).get(player);
		return toRet.get(toRet.size()-1);*/
		return getCurrentEstimate(player, q);
	}
	
	public double getCurrentEstimate(String player, Query q){
		ArrayList<Double> toRet = curValue.get(q).get(player);
		return toRet.get(toRet.size()-1);
	}
	
	@Override
	public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid,
			HashMap<Query, HashMap<String, Integer>> ranks) {
		pushForwardCurEst(cpc, ourBid, ranks);
		updateProbs(cpc, ourBid, ranks);
		genCurEst();
		pushForwardsPrediction();
		return true;
	}

	private void updateProbs(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid,
			HashMap<Query, HashMap<String, Integer>> ranks) {
		for(Query q:_query){
			if(printlns)
			System.out.println("Query: "+q.getComponent()+", "+q.getManufacturer()+" -- ");
			HashMap<String, ArrayList<ArrayList<Double>>> curStrHM = bidDist.get(q);
			Set<String> curStrKey = curStrHM.keySet();
			for(int n = 0; n<numIterations; n++){
				if(printlns)
				System.out.print("Iteration: " + n);
				HashMap<String, ArrayList<Double>> os = new HashMap<String, ArrayList<Double>>();
				for(String s:curStrKey){
					for(int i = 0; i<numBidValues; i++){
						if(i==0){
							os.put(s, new ArrayList<Double>());
						}
						os.get(s).add(1.0);
					}
				}
				//System.out.println("Running with agents: ");
				for(String s:curStrKey){
					if(!s.equals(ourAgent)){
						//System.out.print(s+", ");
						int numAds = curStrKey.size();
						
						/*String curAd = null;
						while(curAd==null||curAd.equals(s)){
							int toSelect = _r.nextInt(numAds);
							for(java.util.Iterator<String> it = curStrKey.iterator(); it.hasNext()&&toSelect>=0; toSelect--){
								curAd = it.next(); //TODO: change how this y advertiser is chosen
							}
						}*/
						/*if(n!=0&&firstAd){
							os.get(s).set(i, 1.0);
						}*/
						for(String curAd:curStrKey){
							if(printlns)
							System.out.print("Updating: " + s + "("+ranks.get(q).get(s)+") with advertiser: " + curAd + "("+ranks.get(q).get(curAd)+") [");
							if(curAd == null){
								System.out.println("PROBLEM 1 ******************************************************************************************");
							}
							for(int i = 0; i<numBidValues; i++){
								if(printlns)
								System.out.print(curStrHM.get(curAd).get(curStrHM.get(curAd).size()-1).get(i) +", ");
							}
							if(printlns)
							System.out.println("]");
							if(printlns)
							System.out.print("Updated probs: [");
							for(int i = 0; i<numBidValues; i++){
								/*if(n==0){
									if(i==0){
										os.put(s, new ArrayList<Double>());
									}
									os.get(s).add(1.0);
								}
								else
								{*/
									double toSet = 0.0;
									if(!curAd.equals(s)){
										if(ranks.get(q).get(s).intValue()>ranks.get(q).get(curAd).intValue()){
											ArrayList<Double> yDist = curStrHM.get(curAd).get(curStrHM.get(curAd).size()-1);
											for(int j = i; j<numBidValues; j++){
												toSet += yDist.get(j);
											}
										}
										else if(ranks.get(q).get(s).intValue()<ranks.get(q).get(curAd).intValue()){
											ArrayList<Double> yDist = curStrHM.get(curAd).get(curStrHM.get(curAd).size()-1);
											for(int j = i; j>=0; j--){
												toSet += yDist.get(j);
											}
										}
										else
										{
											toSet = 1.0;
										}
									}
									else
									{
										toSet = 1.0;
									}
									os.get(s).set(i, os.get(s).get(i)*toSet);
									if(printlns)
									System.out.print(os.get(s).get(i) +", ");
	//							}
							}
							if(printlns)
							System.out.println("]");
							normalizeAL(os.get(s));
						}
					//System.out.println();
					}
				}
				for(String s:curStrKey){
					if(!s.equals(ourAgent)){
						ArrayList<ArrayList<Double>> curDistHist = bidDist.get(q).get(s);
						ArrayList<Double> lastDist = curDistHist.get(curDistHist.size()-1);
						if(printlns){
						System.out.println();
						System.out.println();
						System.out.print("Updating "+s+"From: [");
						for(int i = 0; i<numBidValues; i++){
							System.out.print(lastDist.get(i)+", ");
						}
						System.out.println("]");
						System.out.print("Updating to: ");
						}
						for(int i = 0; i<numBidValues; i++){
							if(printlns)
							System.out.print(lastDist.get(i)*os.get(s).get(i)+", ");
							lastDist.set(i, lastDist.get(i)*os.get(s).get(i)); 
						}
						if(printlns){
						System.out.println("]");
						System.out.println();
						}
					}
				}
				normalizeLastDay(bidDist);
			}
		}
		
	}

	private void pushForwardCurEst(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks) {
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curStrHM = bidDist.get(q);
			Set<String> curStrKey = curStrHM.keySet();
			int nextSpot = 100;
			for(String s:curStrKey){
				if(s.equals(ourAgent)){
					nextSpot = ranks.get(q).get(s).intValue()+1;
				}
			}
			for(String s:curStrKey){
				ArrayList<ArrayList<Double>> curDHM = curStrHM.get(s);
				if(s.equals(ourAgent)){
					ArrayList<Double> myALD = new ArrayList<Double>();
					for(int i = 0; i<numBidValues;i++){
						myALD.add(0.0);
					}
					double myBid = ourBid.get(q);
					double theInd = ((((Math.log(myBid+0.25)/Math.log(2.0))+2)*25.0)-1.0);
					boolean onEdge = false;
					int theIndex = (int)(theInd);
					double firstProp = theInd-(double)theIndex;
					if(theIndex <= 0){
						theIndex = 0;
					}
					if(theIndex >= numBidValues-1){
						theIndex = numBidValues-1;
						onEdge = true;
					}
					if(!onEdge){
						myALD.set(theIndex, 1.0-firstProp);
						myALD.set(theIndex+1, firstProp);
					}
					else
					{
						myALD.set(theIndex, 1.0);
					}
					curDHM.add(myALD);
					if(printlns)
					System.out.println("MY RANK: " + ranks.get(q).get(s).intValue()+"MY BID DISC = " + theIndex + ",  MY BID = "+myBid);
				}
				else if(cpc.get(q).doubleValue()!=Double.NaN&&(nextSpot)==(ranks.get(q).get(s).intValue())){//TODO: does this ever get called
					ArrayList<Double> myALD = new ArrayList<Double>();
					for(int i = 0; i<numBidValues;i++){
						myALD.add(0.0);
					}
					double myBid = cpc.get(q).doubleValue();//TODO: make sure this isn't squashed (otherwise get appropriate input for adjustment)
					double theInd = ((((Math.log(myBid+0.25)/Math.log(2.0))+2)*25.0)-1.0);
					boolean onEdge = false;
					int theIndex = (int)(theInd);
					double firstProp = theInd-(double)theIndex;
					if(theIndex <= 0){
						theIndex = 0;
					}
					if(theIndex >= numBidValues-1){
						theIndex = numBidValues-1;
						onEdge = true;
					}
					if(!onEdge){
						myALD.set(theIndex, 1.0-firstProp);
						myALD.set(theIndex+1, firstProp);
					}
					else
					{
						myALD.set(theIndex, 1.0);
					}
					curDHM.add(myALD);
					if(printlns)
					System.out.println("BELOW RANK: " +ranks.get(q).get(s).intValue()+ "BELOW BID DISC = " + theIndex + ",  BELOW BID = "+myBid);
				}
				else
				{
					curDHM.add(pushForward(curDHM, 1, q));
				}
			}
		}
	}
	
	/*public void setAdvertisers(Set<String> advertisers) {
		_advertisers = advertisers;
	}*/
	
	@Override
	public AbstractModel getCopy() {
		return new IndependentBidModel(_advertisers, ourAgent);
	}

	@Override
	public void setAdvertiser(String ourAdvertiser) {
		ourAgent = ourAdvertiser;
	}

}
