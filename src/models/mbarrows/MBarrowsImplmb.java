package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/*
 * Assumptions:
 * 
 * promoted slot reserve score is not coming into effect: if there is a promoted slot, then that person is taking advantage of promoted slot bonuses
 * TODO: look into: slot 2 gets promoted bonus, but slot 1 doesn't, due to advertiser effect
 * 
 * current assumption: everyone above us is the same and untargeted
 */

public class MBarrowsImplmb extends AbstractMaxBarrows { // TODO: should extend
															// individual
															// abstract class
															// instead of this

	// TODO: this was written for a single query. needs a wrapper for all
	// queries

	// TODO: move these to abstract
	final double TE = 0.5;
	final double PSB = 0.5;
	// final double MSB = 0.4;
	final double CSB = 0.6;
	// final double USP = 10;
	final int numSlots = 5;
	final int days = 60;

	final Query _query;
	final int _numPromSlots;
	final int _advEffectMin;
	final int _advEffectMax;
	double[] _clicks;
	double[] _views;
	double _advertiserEffect;
	double _continuationProbability;

	public MBarrowsImplmb(Query query, int numPromSlots, int _advEffectMin, int _advEffectMax){
		_query = query;
		_numPromSlots = numPromSlots;
		_advEffectMin = advEffectMin;
		_advEffectMax = advEffectMax;
		_clicks = int[numSlots];
		_views = int[numSlots];
		int advEffectAvg = (advEffectMin + advEffectMax)/2;
		for (int i=0;i<numSlots;i++){
			_clicks[i]= advEffectAvg;
			_views[i] = 1;
		}
		_advertiserEffect = _clicks[0]/_views[0];
	}

	public getAdvertiserEffect() {
		return _advertiserEffect;
	}

	public getContinuationProbability() {
		return _continuationProbability;
	}

	@Override
	public double[] getPrediction(Query q) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, LinkedList<Integer> impressionsPerSlot,
			LinkedList<LinkedList<String>> advertisersAbovePerSlot,
			HashMap<String, Ad> ads,
			HashMap<Product, HashMap<UserState, Double>> userStates) {
		// TODO Auto-generated method stub
		return false;
	}

	public static double calculateAdvertiserEffect(
			HashMap<Product, Double> userDist,
			HashMap<Product, Double> probClick, double averageProbClick,
			boolean targeted, boolean promoted, Product target, Query q) {
		double fpro = 1.0;
		if (promoted) {
			fpro = 1.5;
		}
		double TE = 0.5;
		// If targeted
		if (targeted) {
			// iterate over the user preferences
			double eq = 0.0;
			for (Product userpref : probClick.keySet()) {
				// calculate the component of the sum for users of this
				// preference
				if (target.equals(userpref)) {
					double prClick = probClick.get(userpref);
					eq += userDist.get(userpref)
							* (prClick / (prClick + (1.0 + TE) * fpro - (1.0 + TE)
									* prClick * fpro));
				} else {
					double prClick = probClick.get(userpref);
					eq += userDist.get(userpref)
							* (prClick / (prClick + (1 / (1.0 + TE)) * fpro - (1 / (1.0 + TE))
									* prClick * fpro));
				}
				// sum to get estimate for advertiser effect
			}
			return eq;
		} else {
			// using probability of click for the entire user population,
			// calculate estimate for advertiser effect
			double eq = averageProbClick
					/ (averageProbClick + fpro + averageProbClick * fpro);
			return eq;
		}
	}

	public static double calculateProbClick(double views, double clicks) {
		return clicks / views;
	}

	public static HashMap<Product, Double> estimateImpressionDist(
			HashMap<Product, Double> userDist, double impressions) {
		HashMap<Product, Double> toreturn = new HashMap<Product, Double>();
		double sum = 0;
		for (Product userpref : userDist.keySet()) {
			sum += userDist.get(userpref);
		}
		for (Product prod : userDist.keySet()) {
			double percentage = userDist.get(prod) / sum;
			toreturn.put(prod, percentage * impressions);
		}
		return toreturn;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
