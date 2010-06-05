package models.paramest.maxold;

import java.util.HashMap;
import java.util.LinkedList;


import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class MBarrowsQueryHandler extends ConstantsAndFunctions {
	final Query _query;
	final QueryType _queryType;

	LinkedList<MBarrowsDayHandler> _dayHandlers;

	// 1st - targeted - 0 is untargeted, 1 is targeted, 2 is targeted incorrectly
	// 2nd - promoted
	// 3rd - numerator, denominator
	double targetedPromoted[][][];

	public MBarrowsQueryHandler(Query q) {

		_query = q;
		_queryType = q.getType();

		_dayHandlers = new LinkedList<MBarrowsDayHandler>();

		targetedPromoted = new double[3][2][2];
		
		/*
		 * Initialize the estimates to be the means with only
		 * one view (as to not actually affect the estimate after
		 * the first day)
		 */
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				
				targetedPromoted[targeted][promoted][0] = etaClickPr(_advertiserEffectBoundsAvg[queryTypeToInt(_queryType)],
																	 fTargetfPro[targeted][promoted]);
				
				targetedPromoted[targeted][promoted][1] = 1;
			}
		}
	}

	// Returns advertiser effect and continuation probability
	public double[] getPredictions() {
		// Sum up denominators
		double denominatorsum = 0;
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				denominatorsum += targetedPromoted[targeted][promoted][1];
			}
		}

		
		/*
		 * Take a weighted average
		 */
		double advEffect = 0;
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				advEffect += (targetedPromoted[targeted][promoted][1] / denominatorsum) * 
										  clickPrtoE(targetedPromoted[targeted][promoted][0] / targetedPromoted[targeted][promoted][1],
												     fTargetfPro[targeted][promoted]);
			}
		}

		
		double contProb = 0;
		double numberOfDayHandlers = 0;
		for (MBarrowsDayHandler dh : _dayHandlers) {
			if (dh.getContinuationProbability() > 0) { // TODO: not average,
				// throw out bad or
				// something
				// note: only takes >0 because Solve returns -1 if imaginary or
				// negative
				contProb += dh.getContinuationProbability();
				numberOfDayHandlers += 1;
			}
		}

		contProb = contProb / numberOfDayHandlers;

		double[] predictions = { advEffect, contProb };
		return predictions;
	}

	public boolean update(String ourAgent, QueryReport queryReport,
			SalesReport salesReport, int numberPromotedSlots,
			LinkedList<Integer> impressionsPerSlot,
			LinkedList<LinkedList<String>> advertisersAbovePerSlot,
			HashMap<String, Ad> ads,
			HashMap<Product, HashMap<UserState, Integer>> userStates) {

		assert impressionsPerSlot.size() == advertisersAbovePerSlot.size();

		double[] predictions = getPredictions();
		double advertiserEffect = predictions[0];

		// Were we ever not in a top slot
		boolean notinslot1 = false;
		for (int i = 1; i < 5; i++) {
			if (impressionsPerSlot.get(i) > 0) {
				notinslot1 = true;
				break;
			}
		}
		
		// Were we ever in the top slot
		boolean inslot1 = false;
		if (impressionsPerSlot.get(0) > 0) {
			inslot1 = true;
		}
		
		
		Ad ourAd = ads.get(ourAgent);
		boolean ourAdTargeted = true;
		if (ourAd == null || ourAd.isGeneric()) {
			ourAdTargeted = false;
		}
		
		Product ourAdProduct = null;
		if (ourAdTargeted) {
			ourAdProduct = ourAd.getProduct();
		}
		
		// If we were not in top slot for any part of the day, make day handler (to estimate cont prob)
		if (notinslot1) {

			int totalClicks = queryReport.getClicks(_query);

			// Make list of Ads above
			LinkedList<LinkedList<Ad>> adsAbovePerSlot = getAdsAbovePerSlot(
					advertisersAbovePerSlot, ads);

			// get states of searching users
			HashMap<Product, LinkedList<double[]>> statesSearchingUsers = getStatesOfSearchingUsers(userStates, impressionsPerSlot);

			MBarrowsDayHandler latestday = new MBarrowsDayHandler(_query, totalClicks,
					numberPromotedSlots, impressionsPerSlot, advertiserEffect,
					adsAbovePerSlot, statesSearchingUsers, (ourAdTargeted),
					ourAdProduct);

			_dayHandlers.add(latestday);

			// Calculate new value of continuation probability
			// Calculate new average

		} else if (inslot1) {
			// Update advertiser effect
			HashMap<Product, Double> clickdist = getClickDist(userStates,queryReport.getClicks(_query), ourAdTargeted, ourAdProduct);
			HashMap<Product, Double> imprdist = getImpressionDist(userStates,queryReport.getImpressions(_query));

			int promoted = 0;
			if (queryReport.getPromotedImpressions(_query) > 0) {
				promoted = 1;
			}

			for (Product p : userStates.keySet()) {
				int targeted = getFTargetIndex(ourAdTargeted, p, ourAdProduct);

				targetedPromoted[targeted][promoted][0] += clickdist.get(p);
				targetedPromoted[targeted][promoted][1] += imprdist.get(p);
			}

			double newAdvertiserEffect = getPredictions()[0];

			// Update all previous continuation probability estimates
			for (MBarrowsDayHandler dh : _dayHandlers) {
				dh.updateEstimate(newAdvertiserEffect);
			}
			
		} else {
			// do nothing if we saw no impressions
		}

		return true;
	}

	public LinkedList<LinkedList<Ad>> getAdsAbovePerSlot(LinkedList<LinkedList<String>> advertisersAbovePerSlot,
														 HashMap<String, Ad> ads) {
		
		LinkedList<LinkedList<Ad>> adsAbovePerSlot = new LinkedList<LinkedList<Ad>>();
		// for every slot
		for (LinkedList<String> thoseAbove : advertisersAbovePerSlot) {
			// make a linked list of Ads
			LinkedList<Ad> adsAbove = new LinkedList<Ad>();
			// for each advertiser above
			for (String advertiser : thoseAbove) {
				// add their add to the list
				adsAbove.add(ads.get(advertiser));
				/*
				 * if (ads.get(advertiser) == null) {
				 * System.out.println(advertiser); }
				 */
			}
			adsAbovePerSlot.add(adsAbove);
		}
		return adsAbovePerSlot;
	}

	// Get Click Distribution
	public HashMap<Product, Double> getClickDist(
			HashMap<Product, HashMap<UserState, Integer>> userStates,
			int clicks, boolean ourAdTargeted, Product ourAdProduct) {
		HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
		// numprefs should be 9
		double numprefs = userStates.keySet().size();
		// for each product
		int total = 0;
		int totalMatching = 0;
		int totalNotMatching = 0;
		HashMap<Product, Double> userDist = new HashMap<Product, Double>();
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// add up the number of searching users
			double searching = 0;
			if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO)
					&& (_query.getComponent().equals(p.getComponent()))
					&& (_query.getManufacturer().equals(p.getManufacturer()))) {
				searching = 1.0 / 3.0 * states.get(UserState.IS)
						+ states.get(UserState.F2);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE)
					&& ((_query.getComponent() != null && _query.getComponent()
							.equals(p.getComponent())) || (_query
							.getManufacturer() != null && _query
							.getManufacturer().equals(p.getManufacturer())))) {
				searching = 1.0 / 6.0 * states.get(UserState.IS) + 0.5
						* states.get(UserState.F1);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
				searching = 1.0 / 3.0 * states.get(UserState.IS) + 1.0
						/ (numprefs) * states.get(UserState.F0);
			}
			total += searching;
			userDist.put(p, searching);
			if (ourAdTargeted) {
				if (p.equals(ourAdProduct)) {
					totalMatching += searching;
				} else {
					totalNotMatching += searching;
				}
			}
		}
		// TODO split up clicks intelligently if it's targeted, else do for loop
		// note: use totalMatching and totalNotMatching to determine how to
		// weight the clicks. I tried to do this on paper, but the eta equation
		// screws it up and makes it not easy. good luck?
		for (Product p : userStates.keySet()) {
			toreturn.put(p, 1.0 * userDist.get(p) * ((double) clicks)
					/ ((double) total));
		}
		return toreturn;
	}

	// Get Impression Distribution
	public HashMap<Product, Double> getImpressionDist(
			HashMap<Product, HashMap<UserState, Integer>> userStates,
			int Impressions) {
		HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
		// numprefs should be 9
		double numprefs = userStates.keySet().size();
		// for each product
		int total = 0;
		HashMap<Product, Double> userDist = new HashMap<Product, Double>();
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// add up the number of searching users
			double searching = 0;
			if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO)
					&& (_query.getComponent().equals(p.getComponent()))
					&& (_query.getManufacturer().equals(p.getManufacturer()))) {
				searching = 1.0 / 3.0 * states.get(UserState.IS)
						+ states.get(UserState.F2);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE)
					&& ((_query.getComponent() != null && _query.getComponent()
							.equals(p.getComponent())) || (_query
							.getManufacturer() != null && _query
							.getManufacturer().equals(p.getManufacturer())))) {
				searching = 1.0 / 6.0 * states.get(UserState.IS) + 0.5
						* states.get(UserState.F1);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
				searching = 1.0 / 3.0 * states.get(UserState.IS) + 1.0
						/ (numprefs) * states.get(UserState.F0);
			}
			total += searching;
			userDist.put(p, searching);
		}
		for (Product p : userStates.keySet()) {
			toreturn.put(p, 1.0 * userDist.get(p) * ((double) Impressions)
					/ ((double) total));
		}
		return toreturn;
	}

	public HashMap<Product, LinkedList<double[]>> getStatesOfSearchingUsers(
			HashMap<Product, HashMap<UserState, Integer>> userStates,
			LinkedList<Integer> impressionsPerSlot) {
		HashMap<Product, LinkedList<double[]>> toreturn = new HashMap<Product, LinkedList<double[]>>();
		// numprefs should be 9
		double numprefs = userStates.keySet().size();
		// for each product
		for (Product p : userStates.keySet()) {
			HashMap<UserState, Integer> states = userStates.get(p);
			// count up how many searching users there were
			double ISusers = 0.0;
			double nonISusers = 0.0;
			if (_queryType.equals(QueryType.FOCUS_LEVEL_TWO)
					&& (_query.getComponent().equals(p.getComponent()))
					&& (_query.getManufacturer().equals(p.getManufacturer()))) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = states.get(UserState.F2);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ONE)
					&& ((_query.getComponent() != null && _query.getComponent()
							.equals(p.getComponent())) || (_query
							.getManufacturer() != null && _query
							.getManufacturer().equals(p.getManufacturer())))) {
				ISusers = 1.0 / 6.0 * states.get(UserState.IS);
				nonISusers = 0.5 * states.get(UserState.F1);
			}
			if (_queryType.equals(QueryType.FOCUS_LEVEL_ZERO)) {
				ISusers = 1.0 / 3.0 * states.get(UserState.IS);
				nonISusers = 1.0 / (numprefs) * states.get(UserState.F0);
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
					ISusersPerSlot = (double) ISusers * ((double) integer)
							/ sum;
					nonISusersPerSlot = (double) nonISusers
							* ((double) integer) / sum;
				}
				double[] statesOfSearchingUsersPerSlot = { ISusersPerSlot,
						nonISusersPerSlot };
				userSlotDist.add(statesOfSearchingUsersPerSlot);
			}
			toreturn.put(p, userSlotDist);
		}
		return toreturn;
	}

}
