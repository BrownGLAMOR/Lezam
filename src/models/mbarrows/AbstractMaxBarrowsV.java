package models.mbarrows;

import java.util.HashMap;
import java.util.LinkedList;

import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractMaxBarrowsV extends AbstractModel {
	
	/*
	 * Assume you get passed this in the constructor
	 */
	int _numPromSlots;
	
	// Target Effect
	final double _TE = 0.5;
	
	// Promoted Slot Bonus
	final double _PSB =  0.5;
	
	// Component Specialty Bonus
	final double _CSB = 0.6;
	
	// Advertiser effect  lower bound <> upper bound
	final double[][] _advertiserEffectsBounds = { {0.2, 0.3},
											      {0.3, 0.4},
											      {0.4, 0.5}
										  };
	
	// Average advertiser effect  
	final double[] _advertiserEffectsBoundsAvg = {0.25,0.35,0.45};

	
	// Continuation Probability  lower bound <> upper bound
	final double[][] _continuationProbBounds = { {0.2, 0.5},
										         {0.3, 0.6},
										         {0.4, 0.7}
		  								 };
	
	final double[] _continuationProbBoundsAvg = {0.35,0.45,0.55};
	
	// Calculate the forward click probability as defined on page 14 of the spec.
	public double forwardClickProbability(double advertiserEffect, double fTargetfPro)
	{	
		double temp = (advertiserEffect * fTargetfPro) / 
			   ((advertiserEffect * fTargetfPro) + (1 - advertiserEffect));
		
		return temp;
	}
	
	// Calculate the inverse of the forward click probability
	public double inverseClickProbability(double ProbClick, double fTargetfPro)
	{
		double temp = ProbClick / (ProbClick + fTargetfPro - ProbClick * fTargetfPro); 
		
		return temp;
		
	}

	
	/*
	 * Return the advertiser effect and continuation probabilities in the array
	 */
	public abstract double[] getPrediction(Query q);

	/*
	 * QueryReport/SalesReport report contain information about the overall number of
	 * impressions and clicks that we saw
	 * 
	 * impressionsPerSlot contains the number of impressions we saw in each slot with 
	 * index 0 being the highest slot and the size being the last slot
	 * 
	 * advertisersAbovePerSlot is a list that containts the advertisers that were above
	 * us when we were in any given slot, the first index corresponds to the same slot as
	 * the impressionsPerSlot varaible
	 * 
	 * UserStates contains the actual number of users in ever product in every state
	 * 
	 * ads containts the ad that each advertiser placed.  The strings in this hashmap are
	 * the same as in the advertisersAbovePerSlot
	 */
	public abstract boolean updateModel(QueryReport queryReport, 
										SalesReport salesReport,
										LinkedList<Integer> impressionsPerSlot,
										LinkedList<LinkedList<String>> advertisersAbovePerSlot,
										HashMap<String,Ad> ads,
										HashMap<Product,HashMap<UserState,Double>> userStates);
	
}