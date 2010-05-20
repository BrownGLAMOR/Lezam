package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.complex.Complex;

import models.usermodel.TacTexAbstractUserModel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class DayHandler extends ConstantsAndFunctions {

	double otherAdvertiserEffects;
	double otherAdvertiserConvProb;

	double curProbClick;
	double a, b, c, d, e;
	// will solve ax^4+bx^3+cx^2+dx+e=0

	double currentEstimate;

	public DayHandler(Query q, int totalClicks,
			int numberPromotedSlots,
			LinkedList<Integer> impressionsPerSlot, double ourAdvertiserEffect,
			LinkedList<LinkedList<Ad>> advertisersAdsAbovePerSlot, // <our slot
			// < their
			// slots
			// <ad>>
			HashMap<Product,LinkedList<double[]>> userStatesOfSearchingUsers, boolean targeted, Product target) // [IS, non-IS]
	{
		double[] coeff = new double[5];

		boolean sawslot5 = impressionsPerSlot.get(4) > 0;
		boolean sawslot4 = impressionsPerSlot.get(3) > 0;
		boolean sawslot3 = impressionsPerSlot.get(2) > 0;
		boolean sawslot2 = impressionsPerSlot.get(1) > 0;
		boolean sawslot1 = impressionsPerSlot.get(0) > 0;

		/*
		 * //Which slots did we see? for(LinkedList<Ad> ads :
		 * advertisersAdsAbovePerSlot){ if(ads.size()==4){ sawslot5=true; }
		 * if(ads.size()==3){ sawslot4=true; } if(ads.size()==2){ sawslot3=true;
		 * } if(ads.size()==1){ sawslot2=true; } }
		 */
		double something = 1; // TODO
		a = 0;
		b = 0;
		c = 0;
		d = 0;
		e = -1.0 * totalClicks;
		for (Product p : userStatesOfSearchingUsers.keySet()) {
			int ft;
			if (targeted){
				ft = 2-bool2int(p.equals(target));
			} else {
				ft = 0;
			}
			double ftfp = fTargetfPro[ft][0];
			double theoreticalClickProb = (ourAdvertiserEffect*ftfp);
			if (sawslot5) {
				a += something;
			}
			if (sawslot4) {
				b += something;
			}
			if (sawslot3) {
				c += something;
			}
			if (sawslot2) {
				d += something;
			}
			if (sawslot1) {
				e += something;
			}
		}

		// TODO Auto-generated constructor stub
	}

	public void updateEstimate(double ourAdvertiserEffect) {
		// currentEstimate =
	}

	public double getContinuationProbability(double ourAdvertiserEffect) {
		updateEstimate(ourAdvertiserEffect);
		return getContinuationProbability();
	}

	public double getContinuationProbability() {
		return currentEstimate;
	}

	// @Override
	// public double[] getPrediction(Query q) {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public boolean updateModel(
	// QueryReport queryReport,
	// SalesReport salesReport,
	// HashMap<Query, LinkedList<Integer>> impressionsPerSlot,
	// HashMap<Query, LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
	// HashMap<String, HashMap<Query, Ad>> ads,
	// HashMap<Product, HashMap<UserState, Integer>> userStates) {
	// // TODO Auto-generated method stub
	// return false;
	// }

}
