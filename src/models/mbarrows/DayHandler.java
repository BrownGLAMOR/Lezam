package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class DayHandler extends ConstantsAndFunctions {

	double otherAdvertiserEffects;
	// double otherAdvertiserConvProb;

	double curProbClick;

	double currentEstimate;
	boolean[] saw;
	double[] coeff; // will solve ax^4+bx^3+cx^2+dx+e=0

	// inputs defined by constructor
	Query q;
	int totalClicks;
	int numberPromotedSlots;
	LinkedList<Integer> impressionsPerSlot;
	double ourAdvertiserEffect;
	LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot;
	HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers; // [IS,
	// non-IS]
	boolean targeted;
	Product target;

	public DayHandler(
			Query q_,
			int totalClicks_,
			int numberPromotedSlots_,
			LinkedList<Integer> impressionsPerSlot_,
			double ourAdvertiserEffect_,
			LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot_, // <our slot
			// < their
			// slots
			// <ad>>
			HashMap<Product, LinkedList<double[]>> userStatesOfSearchingUsers_,
			boolean targeted_, Product target_) {

		q = q_;
		totalClicks = totalClicks_;
		numberPromotedSlots = numberPromotedSlots_;
		impressionsPerSlot = impressionsPerSlot_;
		ourAdvertiserEffect = ourAdvertiserEffect_;
		advertisersAdsAbovePerSlot = advertisersAdsAbovePerSlot_;
		userStatesOfSearchingUsers = userStatesOfSearchingUsers_;
		targeted = targeted_;
		target = target_;

		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			// otherAdvertiserConvProb = .07;
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[0];
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			// otherAdvertiserConvProb = .15;
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[1];
		} else {
			// otherAdvertiserConvProb = .23;
			otherAdvertiserEffects = _advertiserEffectBoundsAvg[2];
		}

		saw = new boolean[5];
		coeff = new double[5];
		for (int i = 0; i < 5; i++) {
			saw[i] = (impressionsPerSlot.get(i) > 0);
		}
		updateEstimate(ourAdvertiserEffect);
	}

	// TODO: add in whatever else is required, such as this advertiser's
	// component specialty. note: the 'eta' equation is called 'etoClickPr' and
	// is in ConstantsAndFunctions. will probably want to take in the ad type
	// and use the eta equation, with some assumption of how much they've
	// oversold
	private double otherAdvertiserConvProb() {
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
			return .07;
		} else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			return .15;
		} else {
			return .23;
		}
	}

	public void updateEstimate(double ourAdvertiserEffect) {
		for (int i = 0; i < 5; i++) {
			coeff[i] = 0;
		}
		coeff[4] = -1.0 * totalClicks;
		for (Product p : userStatesOfSearchingUsers.keySet()) {
			int ft = getFTargetIndex(targeted, p, target);
			for (int ourSlot = 0; ourSlot < 5; ourSlot++) {
				if (saw[ourSlot]) {
					LinkedList<Ad> advertisersAboveUs = advertisersAdsAbovePerSlot
							.get(ourSlot);
					double ftfp = fTargetfPro[ft][bool2int(numberPromotedSlots >= ourSlot + 1)]; // TODO
					// this
					// should
					// technically
					// check
					// if
					// bid
					// is
					// above
					// promoted
					// reserve
					double theoreticalClickProb = etoClickPr(
							ourAdvertiserEffect, ftfp);
					double IS = userStatesOfSearchingUsers.get(p).get(ourSlot)[0];
					double nonIS = userStatesOfSearchingUsers.get(p).get(
							ourSlot)[1];
					for (int prevSlot = 0; prevSlot < ourSlot; prevSlot++) {
						Ad otherAd = advertisersAboveUs.get(prevSlot);
						// System.out.println("Our slot: "+ourSlot);
						// System.out.println("Other slot: "+prevSlot);
						// System.out.println(advertisersAboveUs.size());
						// System.out.println("Ad "+otherAd);
						int ftOther = 0;
						if (otherAd != null) {
							ftOther = getFTargetIndex(!otherAd.isGeneric(), p,
									otherAd.getProduct());
						}
						double ftfpOther = fTargetfPro[ftOther][bool2int(numberPromotedSlots >= prevSlot + 1)]; // TODO
						// this
						// should
						// technically
						// check
						// if
						// bid
						// is
						// above
						// promoted
						// reserve
						double otherAdvertiserClickProb = etoClickPr(
								otherAdvertiserEffects, ftfpOther);
						nonIS *= (1 - otherAdvertiserConvProb()
								* otherAdvertiserClickProb);
					}
					coeff[ourSlot] += (theoreticalClickProb * (IS + nonIS));
				}
			}
		}

		currentEstimate = solve(coeff);
		// TODO: instead of solve, use these coefficients more appropriately
		// (b-updating)
	}

	public double getContinuationProbability() {
		return currentEstimate;
	}

}
