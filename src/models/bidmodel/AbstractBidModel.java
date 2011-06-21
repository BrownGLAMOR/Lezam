package models.bidmodel;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.HashMap;

public abstract class AbstractBidModel extends AbstractModel {
   protected static final double maxReasonableBidF0 = 1.25;
   protected static final double maxReasonableBidF1 = 2.25;
   protected static final double maxReasonableBidF2 = 3.00;

//	protected static final double maxReasonableBidF0 = 1.35;
//	protected static final double maxReasonableBidF1 = 2.75;
//	protected static final double maxReasonableBidF2 = 3.5;


   protected static final double maxBid = 3.75;

   public abstract boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks, HashMap<Query, HashMap<String, Boolean>> allRankable);

   public abstract double getPrediction(String player, Query q);

   public abstract void setAdvertiser(String ourAdvertiser);


}
