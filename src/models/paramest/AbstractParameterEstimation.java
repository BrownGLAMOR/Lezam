package models.paramest;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;

/**
 * @author jberg
 */
public abstract class AbstractParameterEstimation extends AbstractModel {

   /*
     * Assume you get passed this in the constructor
     */

   /*
     * Return the advertiser effect and continuation probabilities in the array
     */
   public abstract double getAdvEffectPrediction(Query q);
   public abstract double getContProbPrediction(Query q);
   public abstract double getReservePrediction(QueryType qt);

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
                                       BidBundle bidBundle,
                                       HashMap<Query, int[]> impressions,
                                       HashMap<Query, int[][]> allWaterfalls,
                                       HashMap<Product, HashMap<GameStatusHandler.UserState, Integer>> userStates,
                                       double[] c);

}