package models.bidmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

import models.AbstractModel;

public class IndependentBidModel extends AbstractBidModel{

	/*
	 * 
	 IndependentBidModel(1	0.001920206	0.93158911	0.066490685	 2.6411142720749314)	0.040435407	886.512
IndependentBidModel(1	0.002158675	0.809201528	0.188639797	 1.3215652774956843)	0.041970943	770.647
IndependentBidModel(1	0.004189202	0.664774628	0.33103617	 1.501443206055253)	0.044563284	813.014
IndependentBidModel(1	0.017824885	0.972635234	0.009539881	 4.872773921681622)	0.045360217	877.801
IndependentBidModel(1	0.007471113	0.599089585	0.393439302	 0.6798428107025789)	0.045946334	411.19
IndependentBidModel(1	0.023205428	0.708323137	0.268471434	 2.0289534202487136)	0.046080396	858.065
IndependentBidModel(1	0.010000105	0.613800136	0.376199759	 2.2272145571380255)	0.046412113	719.28
IndependentBidModel(1	0.032718437	0.894801643	0.07247992	 1.1098144471903948)	0.046511306	1054.171
IndependentBidModel(1	0.024666645	0.839955651	0.135377703	 5.43534932015163)	0.046646183	679.441
IndependentBidModel(1	0.023450539	0.966875203	0.009674258	 8.270653559072693)	0.04699118	730.738
IndependentBidModel(1	0.006803242	0.554342302	0.438854456	 1.9629875579250566)	0.047007562	875.083
IndependentBidModel(1	0.012511157	0.578897468	0.408591375	 2.6184790012523718)	0.047544342	889.126
IndependentBidModel(1	0.043598151	0.760688128	0.195713721	 1.9539736844933442)	0.047632926	736.003
IndependentBidModel(1	0.009236423	0.496808305	0.493955272	 0.21927469833677105)	0.047799383	899.582
IndependentBidModel(1	0.044168383	0.884857875	0.070973742	 3.296999671119546)	0.047931406	907.069
IndependentBidModel(1	0.036903518	0.878067728	0.085028754	 5.406437066335405)	0.0479414	377.253
IndependentBidModel(1	0.029993772	0.683216401	0.286789827	 3.354864491649676)	0.048015084	920.56
IndependentBidModel(1	0.010967657	0.677299865	0.311732478	 5.985499964801065)	0.048373207	778.826
IndependentBidModel(1	0.034865069	0.696782142	0.268352788	 3.5795923554762465)	0.04847707	858.345
IndependentBidModel(1	0.055675383	0.776354193	0.167970424	 1.0216991458279123)	0.0490047	816.604
IndependentBidModel(1	0.040246328	0.786737255	0.173016416	 5.108495082759328)	0.049018642	872.999
IndependentBidModel(1	0.052066641	0.757058472	0.190874887	 2.9448745108207253)	0.049078867	237.75
IndependentBidModel(1	0.037765459	0.57623767	0.38599687	 0.7416173844210072)	0.049239585	776.944
IndependentBidModel(1	0.004181492	0.536173184	0.459645324	 5.080979266545219)	0.049339255	844.513
IndependentBidModel(1	0.016589003	0.483429222	0.499981775	 2.418015214683784)	0.049572269	774.336
IndependentBidModel(1	0.015391524	0.468115675	0.516492801	 2.4491271723405497)	0.049766376	1064.232
	 */

	/**************************************************************************************************************************************
	HOW DO YOU SELECT Y advertiser (equation 8) currently random and not the same as the current advertiser
	MAKE SURE THEY GIVE US EVERYONE AT SAME RANK WHEN THEY HAVE NOT YETGOTTEN RANK INFO
	 ***************************************************************************************************************************************/
	private static final double aStep = Math.pow(2, (1.0/25.0));
	private HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> bidDist;
	//	private HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>> predDist;
	private HashMap<Query, HashMap<String, ArrayList<Double>>> curValue;
	//	private HashMap<Query, HashMap<String, ArrayList<Double>>> predValue;
	private double[] transProbs;
	private int numBidValues;
	private ArrayList<Query> _query;
	private double randomJumpProb = 0.0;
	private double yesterdayProb = 0.8;
	private double nDaysAgoProb = 0.2;
	private double normVar = 2.1; 
	private int numIterations = 1;
	private String ourAgent;
	Set<String> _advertisers;
	private static final boolean printlns = false;

	public IndependentBidModel(Set<String> advertisers, String me, int iterations, double randJump, double yesterday, double nDaysAgo, double var){

		numIterations = iterations;
		randomJumpProb = randJump;
		yesterdayProb = yesterday;
		nDaysAgoProb = nDaysAgo;
		normVar = var;

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

		ourAgent = me;

		bidDist = new HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>>();
		//		predDist = new HashMap<Query, HashMap<String, ArrayList<ArrayList<Double>>>>();
		//		predValue = new HashMap<Query, HashMap<String, ArrayList<Double>>>();
		curValue = new HashMap<Query, HashMap<String, ArrayList<Double>>>();
		Double startVal = Math.pow(2, (1.0/25.0-2.0))-0.25;
		for(Query q:_query){
			HashMap<String, ArrayList<ArrayList<Double>>> curStringMap = new HashMap<String, ArrayList<ArrayList<Double>>>();
			//			HashMap<String, ArrayList<ArrayList<Double>>> curStringMapTwo = new HashMap<String, ArrayList<ArrayList<Double>>>();
			//			HashMap<String, ArrayList<Double>> curStringMapThree = new HashMap<String, ArrayList<Double>>();
			HashMap<String, ArrayList<Double>> curStringMapFour = new HashMap<String, ArrayList<Double>>();
			bidDist.put(q, curStringMap);
			//			predDist.put(q, curStringMapTwo);
			//			predValue.put(q, curStringMapThree);
			curValue.put(q, curStringMapFour);
			//System.out.print("Initializing with agents: ");
			for(String s:advertisers){
				//System.out.print(s+", ");
				ArrayList<ArrayList<Double>> curDoubleMap = new ArrayList<ArrayList<Double>>();
				//				ArrayList<ArrayList<Double>> curDoubleMapTwo = new ArrayList<ArrayList<Double>>();
				//				ArrayList<Double> curDoubleALTwo = new ArrayList<Double>();
				ArrayList<Double> curDoubleALThree = new ArrayList<Double>();
				curStringMap.put(s, curDoubleMap);
				//				curStringMapTwo.put(s, curDoubleMapTwo);
				//				curStringMapThree.put(s, curDoubleALTwo);
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

					numBidValues++;
					index++;
				}
				curDoubleMap.add(curDoubleAL);
			}
		}
		normalizeLastDay(bidDist);
		transProbs = new double[numBidValues];
		for(int i = 0; i<numBidValues; i++){
			transProbs[i] = normalDensFn(i);
		}
		normalize(transProbs);
		genCurEst();
		//		pushPredictionsForward();
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

	private void normalize(double[] arr) {
		double sum = 0.0;
		for(int i = 0; i <arr.length; i++){
			sum+= arr[i];
		}
		for(int i = 0; i <arr.length; i++){
			arr[i] /= sum;
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

	private double normalDensFn(double d, double var){
		return Math.exp((-(d*d))/(2.0*var))/(Math.sqrt(2.0*Math.PI*var));
	}

	//	private void pushPredictionsForward() {
	//		for(Query q:_query){
	//			HashMap<String, ArrayList<ArrayList<Double>>> curStrHM = bidDist.get(q);
	//			Set<String> curStrKey = curStrHM.keySet();
	//			for(String s:curStrKey){
	//				ArrayList<ArrayList<Double>> curHist = bidDist.get(q).get(s);
	//				ArrayList<Double> newPred = pushForward(curHist, 2, q);
	//				predDist.get(q).get(s).add(newPred);
	//				predValue.get(q).get(s).add(averageAL(newPred));
	//			}
	//		}
	//	}

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
					if(arrayList.size() + i>= 5){
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
					}
				}
				else
				{
					/*
					 * Re-initializing Distribution
					 */
					System.out.println("\n\n REINITIALIZING DIST (" + sum + ") \n\n");
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
					}
				}
			}
		}
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
	public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks) {
		pushForwardCurEst(cpc, ourBid, ranks);
		updateProbs(cpc, ourBid, ranks);
		genCurEst();
		//		pushPredictionsForward();
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
						for(String curAdv:curStrKey){
							if(printlns)
								System.out.print("Updating: " + s + "("+ranks.get(q).get(s)+") with advertiser: " + curAdv + "("+ranks.get(q).get(curAdv)+") [");

							for(int i = 0; i<numBidValues; i++){
								if(printlns)
									System.out.print(curStrHM.get(curAdv).get(curStrHM.get(curAdv).size()-1).get(i) +", ");
							}
							if(printlns)
								System.out.println("]");
							if(printlns)
								System.out.print("Updated probs: [");
							for(int i = 0; i<numBidValues; i++){
								double toSet = 0.0;
								if(!curAdv.equals(s)){
									if(ranks.get(q).get(s).intValue()>ranks.get(q).get(curAdv).intValue()){
										ArrayList<Double> yDist = curStrHM.get(curAdv).get(curStrHM.get(curAdv).size()-1);
										for(int j = i; j<numBidValues; j++){
											toSet += yDist.get(j);
										}
									}
									else if(ranks.get(q).get(s).intValue()<ranks.get(q).get(curAdv).intValue()){
										ArrayList<Double> yDist = curStrHM.get(curAdv).get(curStrHM.get(curAdv).size()-1);
										for(int j = i; j>=0; j--){
											toSet += yDist.get(j);
										}
									}
									else
									{
										toSet = 1.0;
										throw new RuntimeException();
									}
								}
								else
								{
									toSet = 1.0;
								}

								if(Double.isNaN(toSet)) {
									throw new RuntimeException();
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
			int nextSpot = -1;
			for(String s:curStrKey){
				if(s.equals(ourAgent)){
					nextSpot = ranks.get(q).get(s).intValue()+1;
				}
			}
			for(String s:curStrKey){
				ArrayList<ArrayList<Double>> curDHM = curStrHM.get(s);
				if(s.equals(ourAgent)) {
					ArrayList<Double> myALD = new ArrayList<Double>();
					for(int i = 0; i<numBidValues;i++){
						myALD.add(0.0);
					}
					double myBid = ourBid.get(q);
					double theInd = ((((Math.log(myBid+0.25)/Math.log(2.0))+2)*25.0)-1.0);
					boolean onEdge = false;
					int theIndex = (int)(theInd);
					double firstProp = theInd-(double)theIndex;
					if(Double.isNaN(firstProp)) {
						throw new RuntimeException();
					}
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
				else if(!Double.isNaN(cpc.get(q)) && (nextSpot)==(ranks.get(q).get(s).intValue())){
					ArrayList<Double> myALD = new ArrayList<Double>();
					for(int i = 0; i<numBidValues;i++){
						myALD.add(0.0);
					}
					double ourCPC = cpc.get(q).doubleValue();
					double theInd = ((((Math.log(ourCPC+0.25)/Math.log(2.0))+2)*25.0)-1.0);
					boolean onEdge = false;
					int theIndex = (int)(theInd);
					double firstProp = theInd-(double)theIndex;
					if(Double.isNaN(firstProp)) {
						throw new RuntimeException();
					}
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
						System.out.println("BELOW RANK: " +ranks.get(q).get(s).intValue()+ "BELOW BID DISC = " + theIndex + ",  BELOW BID (ourCPC)= "+ourCPC);
				}
				else
				{
					curDHM.add(pushForward(curDHM, 1, q));
				}
			}
		}
		normalizeLastDay(bidDist);
	}

	@Override
	public AbstractModel getCopy() {
		return new IndependentBidModel(_advertisers, ourAgent,numIterations,randomJumpProb,yesterdayProb,nDaysAgoProb, normVar);
	}

	@Override
	public String toString() {
		return "IndependentBidModel(" + numIterations + ", " + randomJumpProb + ", " + yesterdayProb + ", " + nDaysAgoProb + ", " + normVar + ")";
	}

	@Override
	public void setAdvertiser(String ourAdvertiser) {
		ourAgent = ourAdvertiser;
	}

}
