package models.paramest.maxold;

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
public abstract class AbstractParameterEstimation extends AbstractModel {
	
	/*
	 * Assume you get passed this in the constructor
	 */
	
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
	public abstract boolean updateModel(String ourAgent,
										QueryReport queryReport, 
										SalesReport salesReport,
										int numberPromotedSlots,
										HashMap<Query,LinkedList<Integer>> impressionsPerSlot,
										HashMap<Query,LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
										HashMap<String,HashMap<Query,Ad>> ads,
										HashMap<Product,HashMap<UserState,Integer>> userStates);
	
}