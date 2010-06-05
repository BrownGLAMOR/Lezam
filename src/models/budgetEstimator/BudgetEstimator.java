package models.budgetEstimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BudgetEstimator extends AbstractBudgetEstimator {

	public HashMap<String,HashMap<Query,Double>> _budgetPredictions;
	private Set<Query> _querySpace;

	private int numSlots = 5;
	private int numAdvertisers = 8;
	private double _proReserve = .4;
	private double _regReserve = .05;
	private double[] _c;
	
	// Target Effect
	final double _TE = 0.5;
	// Promoted Slot Bonus
	final double _PSB = 0.5;
	// Component Specialty Bonus
	final double _CSB = 0.5;
	// Advertiser effect lower bound <> upper bound
	final double[][] _advertiserEffectBounds = { { 0.2, 0.3 },
												 { 0.3, 0.4 }, 
												 { 0.4, 0.5 } };

	// Average advertiser effect
	final double[] _advertiserEffectBoundsAvg = {
			(_advertiserEffectBounds[0][0] + _advertiserEffectBounds[0][1]) / 2,
			(_advertiserEffectBounds[1][0] + _advertiserEffectBounds[1][1]) / 2,
			(_advertiserEffectBounds[2][0] + _advertiserEffectBounds[2][1]) / 2 };
	
	// first index:
	// 0 - untargeted
	// 1 - targeted correctly
	// 2 - targeted incorrectly
	// second index:
	// 0 - not promoted
	// 1 - promoted
	final double[][] fTargetfPro = { { (1.0), (1.0) * (1.0 + _PSB) },
									 { (1.0 + _TE), (1.0 + _TE) * (1.0 + _PSB) },
									 { (1.0) / (1.0 + _TE), ((1.0) / (1.0 + _TE)) * (1.0 + _PSB) } };

	public final static boolean BUDGETINDOLLARS = true;

	public BudgetEstimator(Set<Query> querySpace) {
		_querySpace = querySpace;
		_budgetPredictions = new HashMap<String,HashMap<Query,Double>>();
		for(int i = 0; i < numAdvertisers; i++) {
			HashMap<Query, Double> budgets = new HashMap<Query,Double>();
			for(Query q : _querySpace) {
				budgets.put(q,Double.MAX_VALUE);
			}
			_budgetPredictions.put("adv"+(i+1), budgets);
		}
	}

	@Override
	public double getBudgetEstimate(Query q, String advertiser) {
		return _budgetPredictions.get(advertiser).get(q);
	}

	@Override
	public void updateModel(QueryReport queryReport, 
							SalesReport salesReport,
							BidBundle bidBundle,
							int numberPromotedSlots,
							double[] convProbs,
							HashMap<Query, Double> contProbs,
							HashMap<Query, int[]> allOrders,
							HashMap<Query, int[]> allImpressions,
							HashMap<Query,double[]> allBids,
							HashMap<Product, HashMap<UserState, Integer>> userStates) {
		
		_c = convProbs;

		/*
		 * Start at index 1, because we don't care about ourselves
		 */
		for(int i = 1; i < numAdvertisers; i++) {
			HashMap<Query, Double> budgets = new HashMap<Query,Double>();
			for(Query q : _querySpace) {
				double budget;
				int[] impressions = allImpressions.get(q);
				int ourImpressions = impressions[i];
				if(ourImpressions > 0) {
					double[] bids = allBids.get(q);

					int[] order = allOrders.get(q);
					int ourOrder = order.length-1;
					for(int j = 0; j < order.length; j++) {
						if(i == order[j]) {
							ourOrder = j;
						}
					}

					if(ourOrder <= numSlots-1) {
						int maxImps = 0;
						for(int j = 0; j < order.length; j++) {
							if(impressions[j] > maxImps) {
								maxImps = impressions[j];
							}
						}

						if(ourImpressions == maxImps) {
							/*
							 * We didn't drop so we should assume there was no budget
							 */
							budget = Double.MAX_VALUE;
						}
						else {
							if(BUDGETINDOLLARS) {
								HashMap<String, Ad> query_ads = new HashMap<String,Ad>();
								query_ads.put("adv1",bidBundle.getAd(q));
								for(int j = 2; j <= 8; j++) {
									query_ads.put("adv" + j, queryReport.getAd(q, "adv" + j));
								}
								
								Ad ourAd = query_ads.get("adv" + (i+1));
								boolean ourAdTargeted = true;
								if (ourAd == null || ourAd.isGeneric()) {
									ourAdTargeted = false;
								}

								Product ourAdProduct = null;
								if (ourAdTargeted) {
									ourAdProduct = ourAd.getProduct();
								}

								LinkedList<LinkedList<String>> advertisersAbovePerSlot = new LinkedList<LinkedList<String>>();
								LinkedList<Integer> impressionsPerSlot = new LinkedList<Integer>();
								int[][] impressionMatrix = greedyAssign(numSlots, numAdvertisers, order, impressions);

								//where are we in bid pair matrix?
								ArrayList<ImprPair> higherthanus = new ArrayList<ImprPair>();
								for(int j = 0; j < order.length; j++){
									if(order[j] == i){
										break;
									}else{
										higherthanus.add(new ImprPair(order[j],impressions[order[j]]));
									}
								}

								Collections.sort(higherthanus);
								for(int j = 0; j < numSlots; j++) {
									int numImpressions = impressionMatrix[i][j];
									impressionsPerSlot.add(numImpressions);

									LinkedList<String> advsAbove = new LinkedList<String>();
									if(!(numImpressions == 0 || numSlots == 0)) {
										List<ImprPair> sublist = higherthanus.subList(0, j);
										for(ImprPair imp : sublist){
											advsAbove.add("adv" + (imp.getID()+1));
										}
									}

									advertisersAbovePerSlot.add(advsAbove);
								}

								double advEffect;
								if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
									advEffect = _advertiserEffectBoundsAvg[0];
								} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
									advEffect = _advertiserEffectBoundsAvg[1];
								} else {
									advEffect = _advertiserEffectBoundsAvg[2];
								}
								
								LinkedList<LinkedList<Ad>> adsAbovePerSlot = getAdsAbovePerSlot(advertisersAbovePerSlot, query_ads);
								HashMap<Product, Double> imprdist = getImpressionDist(q, userStates, ourImpressions);
								HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers = getStatesOfSearchingUsers(q, userStates, impressionsPerSlot, imprdist);
								LinkedList<Double> clicksPerSlot = getClicksPerSlot(q,numberPromotedSlots,impressionsPerSlot,advEffect,contProbs.get(q),
										adsAbovePerSlot,userStatesOfSearchingUsers,ourAdTargeted,ourAdProduct);

								/*
								 * We were in the auction and dropped out, so calculate budget
								 */
								if(ourImpressions < impressions[order[ourOrder+1]]) {
									/*
									 * We were beneath the same person for the whole game
									 * so just multiply their CPC by number of clicks
									 */

									/*
									 * Assuming everyone has the same advertiser effect, we
									 * get that CPC is just bid of the person below you
									 * plus 1 cent
									 */
									double totalClicks = 0;
									for(Double clicks : clicksPerSlot) {
										totalClicks += clicks;
									}

									double bidBelow = bids[order[ourOrder+1]];
									if(ourOrder < numberPromotedSlots) {
										if(bidBelow < _proReserve) {
											bidBelow = _proReserve;
										}
									}
									else {
										if(bidBelow < _regReserve) {
											bidBelow = _regReserve;
										}
									}

									double CPC = bidBelow + .01;
									budget = totalClicks*CPC;
								}
								else {
									budget = 0.0;
									int advBelowUsDrop = 0;
									for(int j = ourOrder; j >= 0; j--) {
										if(impressionsPerSlot.get(j) > 0) {
											if(ourOrder+1+advBelowUsDrop < order.length) {
												if(impressionsPerSlot.get(j) <= impressionMatrix[order[ourOrder+1+advBelowUsDrop]][j+1]) {
													/*
													 * All impressions in this slot were seen with the same person below us
													 */
													double bidBelow = bids[order[ourOrder+1+advBelowUsDrop]];
													if(ourOrder < numberPromotedSlots) {
														if(bidBelow < _proReserve) {
															bidBelow = _proReserve;
														}
													}
													else {
														if(bidBelow < _regReserve) {
															bidBelow = _regReserve;
														}
													}

													double CPC = bidBelow + .01;
													budget += clicksPerSlot.get(j)*CPC;
												}
												else {
													/*
													 * An advertiser dropped out in the middle of while we were in a given
													 * slot.  Add the clicks we saw while that advertiser was in, remove
													 * those impressions and clicks from the list, and rerun the current slot
													 * with a new advertiser below us
													 */
													int numImpSeen = impressionMatrix[order[ourOrder+1+advBelowUsDrop]][j+1];
													int impPercentage = numImpSeen / (impressionsPerSlot.get(j));
													double bidBelow = bids[order[ourOrder+1+advBelowUsDrop]];
													if(ourOrder < numberPromotedSlots) {
														if(bidBelow < _proReserve) {
															bidBelow = _proReserve;
														}
													}
													else {
														if(bidBelow < _regReserve) {
															bidBelow = _regReserve;
														}
													}

													double CPC = bidBelow + .01;
													double numClickSeen = clicksPerSlot.get(j)*impPercentage;
													budget += numClickSeen*CPC;
													
													/*
													 * Increase the number of advertisers below us who dropped,
													 * and reduce the number of impressions seen in the slot
													 */
													advBelowUsDrop++;
													impressionsPerSlot.set(j, impressionsPerSlot.get(j)-numImpSeen);
													clicksPerSlot.set(j, clicksPerSlot.get(j)-numClickSeen);
													j++; //we need to rerun this slot with a new advertiser and fewer impressions
												}
											}
											else {
												/*
												 * There is no one below us anymore, so we pay the reserve depending
												 * on the slot
												 */
												if(j < numberPromotedSlots) {
													budget += clicksPerSlot.get(j) * (_proReserve + .01);
												}
												else {
													budget += clicksPerSlot.get(j) * (_regReserve + .01);
												}
											}
										}
									}
								}
							}
							else {
								budget = impressions[i];
							}
						}
					}
					else {
						/*
						 * If they didn't start in the auction assume they had no budget
						 * 
						 * (this should really actually check if they entered and dropped
						 * with someone still left in the auction)
						 */
						budget = Double.MAX_VALUE;

						/*
						 * We may consider keeping the old value here...
						 */
						//budget = budgets.get(q);
					}
				}
				else {
					/*
					 * If we didn't see any impressions, we have no information about budgets
					 */
					budget = Double.MAX_VALUE;

					/*
					 * We may consider keeping the old value here...
					 */
					//budget = budgets.get(q);
				}
				budgets.put(q, budget);
			}
			_budgetPredictions.put("adv"+i, budgets);
		}
	}

	private double otherAdvertiserConvProb(Query q) {
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			return _c[0];
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			return _c[1];
		} else {
			return _c[2];
		}
	}
	
	private LinkedList<Double> getClicksPerSlot(Query q,
			int numberPromotedSlots,
			LinkedList<Integer> impressionsPerSlot,
			double ourAdvertiserEffect,
			double continuationProb,
			LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot, // <our slot < their slots <ad>>
			HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers,
			boolean targeted,
			Product target) {
		
		double otherAdvertiserEffects;
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[0];
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[1];
		} else {
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[2];
		}
		
		double[] coeff = new double[numSlots];
		for (int i = 0; i < numSlots; i++) {
			coeff[i] = 0;
		}
		for (Product p : userStatesOfSearchingUsers.keySet()) {
			int ft = getFTargetIndex(targeted, p, target);
			for (int ourSlot = 0; ourSlot < numSlots; ourSlot++) {
				if (impressionsPerSlot.get(ourSlot) > 0) {
					LinkedList<Ad> advertisersAboveUs = advertisersAdsAbovePerSlot.get(ourSlot);
					double ftfp = fTargetfPro[ft][bool2int(numberPromotedSlots >= ourSlot + 1)];
					double theoreticalClickProb = etaClickPr(ourAdvertiserEffect, ftfp);
					double IS = userStatesOfSearchingUsers.get(p).get(ourSlot)[0];
					double nonIS = userStatesOfSearchingUsers.get(p).get(ourSlot)[1];
					for (int prevSlot = 0; prevSlot < ourSlot; prevSlot++) {
						Ad otherAd = advertisersAboveUs.get(prevSlot);
						int ftOther = 0;
						if (otherAd != null) {
							ftOther = getFTargetIndex(!otherAd.isGeneric(), p,otherAd.getProduct());
						}
						double ftfpOther = fTargetfPro[ftOther][bool2int(numberPromotedSlots >= prevSlot + 1)];
						double otherAdvertiserClickProb = etaClickPr(otherAdvertiserEffects, ftfpOther);
						nonIS *= (1.0 - otherAdvertiserConvProb(q) * otherAdvertiserClickProb);
					}
					coeff[(numSlots-1) - ourSlot] += (theoreticalClickProb * (IS + nonIS));
				}
			}
		}
		
		LinkedList<Double> clicksPerSlot = new LinkedList<Double>();
		double contProb2 = continuationProb*continuationProb;
		double contProb3 = contProb2*continuationProb;
		double contProb4 = contProb3*continuationProb;
		clicksPerSlot.add(coeff[4]);
		clicksPerSlot.add(continuationProb*coeff[3]);
		clicksPerSlot.add(contProb2*coeff[2]);
		clicksPerSlot.add(contProb3*coeff[1]);
		clicksPerSlot.add(contProb4*coeff[0]);
		
		return clicksPerSlot;
	}

	// Calculate the forward click probability as defined on page 14 of the
	// spec.
	public double etaClickPr(double advertiserEffect, double fTargetfPro) {
		return (advertiserEffect * fTargetfPro) / ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
	}
	

	// Turns a boolean into binary
	int bool2int(boolean bool) {
		if (bool) {
			return 1;
		}
		return 0;
	}

	// returns the corresponding index for the targeting part of fTargetfPro
	int getFTargetIndex(boolean targeted, Product p, Product target) {
		if (!targeted || p == null || target == null) {
			return 0; //untargeted
		}
		else if(p.equals(target)) {
			return 1; //targeted correctly
		}
		else {
			return 2; //targeted incorrectly
		}
	}

	// Turns a query type into 0/1/2
	int queryTypeToInt(QueryType qt) {
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
	
	public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions){
		int[][] impressionsBySlot = new int[agents][slots];

		int[] slotStart= new int[slots];
		int a;

		for(int i = 0; i < agents; ++i){
			a = order[i];
			//System.out.println(a);
			int remainingImp = impressions[a];
			//System.out.println("remaining impressions "+ impressions[a]);
			for(int s = Math.min(i+1, slots)-1; s>=0; --s){
				if(s == 0){
					impressionsBySlot[a][0] = remainingImp;
					slotStart[0] += remainingImp;
				}else{
					int r = slotStart[s-1] - slotStart[s];
					//System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
					assert(r >= 0);
					if(r < remainingImp){
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
	
	// Get Impression Distribution
	public HashMap<Product, Double> getImpressionDist(Query q, HashMap<Product, HashMap<UserState, Integer>> userStates, int impressions) {
		HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
		// for each product
		int total = 0;
		HashMap<Product, Double> userDist = new HashMap<Product, Double>();
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// add up the number of searching users
			double searching = 0;
			if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && 
					q.getComponent().equals(p.getComponent()) &&
					q.getManufacturer().equals(p.getManufacturer())) {
				searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F2);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && 
					((q.getComponent() != null && q.getComponent().equals(p.getComponent())) || 
							(q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
				searching = 1.0 / 6.0 * states.get(UserState.IS) + 0.5 * states.get(UserState.F1);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				searching = 1.0 / 3.0 * states.get(UserState.IS) + states.get(UserState.F0);
			}
			total += searching;
			userDist.put(p, searching);
		}
		for (Product p : userStates.keySet()) {
			toreturn.put(p, 1.0 * userDist.get(p) * ((double) impressions) / ((double) total));
		}
		return toreturn;
	}

	public LinkedList<LinkedList<Ad>> getAdsAbovePerSlot(LinkedList<LinkedList<String>> advertisersAbovePerSlot,HashMap<String, Ad> ads) {
		LinkedList<LinkedList<Ad>> adsAbovePerSlot = new LinkedList<LinkedList<Ad>>();
		// for every slot
		for (LinkedList<String> thoseAbove : advertisersAbovePerSlot) {
			// make a linked list of Ads
			LinkedList<Ad> adsAbove = new LinkedList<Ad>();
			// for each advertiser above
			for (String advertiser : thoseAbove) {
				// add their add to the list
				adsAbove.add(ads.get(advertiser));
			}
			adsAbovePerSlot.add(adsAbove);
		}
		return adsAbovePerSlot;
	}
	
	public HashMap<Product, LinkedList<double[]>> getStatesOfSearchingUsers(Query q, HashMap<Product, HashMap<UserState, Integer>> userStates, LinkedList<Integer> impressionsPerSlot, HashMap<Product, Double> imprdist) {

		double impressions = 0;
		for(Product p : imprdist.keySet()) {
			impressions += imprdist.get(p);
		}

		HashMap<Product, LinkedList<double[]>> toreturn = new HashMap<Product, LinkedList<double[]>>();
		// for each product
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// count up how many searching users there were
			double ISusers = 0.0;
			double nonISusers = 0.0;
			if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO) && 
					q.getComponent().equals(p.getComponent()) &&
					q.getManufacturer().equals(p.getManufacturer())) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = states.get(UserState.F2);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE) && 
					((q.getComponent() != null && q.getComponent().equals(p.getComponent())) || 
							(q.getManufacturer() != null && q.getManufacturer().equals(p.getManufacturer())))) {
				ISusers = 1.0 / 6.0 * states.get(UserState.IS);
				nonISusers = 0.5 * states.get(UserState.F1);
			}
			if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = states.get(UserState.F0);
			}
			// double nonISusers =
			// states.get(UserState.F0)+states.get(UserState.F1)+states.get(UserState.F2);
			// make a list of Is, non Is users arrays, one for each slot
			LinkedList<double[]> userSlotDist = new LinkedList<double[]>();
			double sum = ISusers + nonISusers;
			for (Integer integer : impressionsPerSlot) {
				double ISusersPerSlot = 0.0;
				double nonISusersPerSlot = 0.0;
				if (sum > 0.0) {
					ISusersPerSlot = (((double) ISusers * ((double) integer)) / sum)*(imprdist.get(p)/impressions);
					nonISusersPerSlot = (((double) nonISusers * ((double) integer)) / sum)*(imprdist.get(p)/impressions);
				}
				double[] statesOfSearchingUsersPerSlot = { ISusersPerSlot, nonISusersPerSlot };
				userSlotDist.add(statesOfSearchingUsersPerSlot);
			}
			toreturn.put(p, userSlotDist);
		}
		return toreturn;
	}

	@Override
	public AbstractModel getCopy() {
		return new BudgetEstimator(_querySpace);
	}
	
	
	public static class ImprPair implements Comparable<ImprPair> {

		private int _advIdx;
		private int _impr;

		public ImprPair(int advIdx, int impr) {
			_advIdx = advIdx;
			_impr = impr;
		}

		public int getID() {
			return _advIdx;
		}

		public void setID(int advIdx) {
			_advIdx = advIdx;
		}

		public double getImpr() {
			return _impr;
		}

		public void setImpr(int impr) {
			_impr = impr;
		}

		public int compareTo(ImprPair agentImprPair) {
			double ourBid = this._impr;
			double otherBid = agentImprPair.getImpr();
			if(ourBid > otherBid) {
				return 1;
			}
			if(otherBid > ourBid) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
	
	
}
