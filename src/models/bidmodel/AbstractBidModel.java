package models.bidmodel;

import java.util.HashMap;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class AbstractBidModel extends AbstractModel {
	protected static final double maxReasonableBidF0 = .8;
	protected static final double maxReasonableBidF1 = 1.1;
	protected static final double maxReasonableBidF2 = 1.6;
	protected static final double maxBid = 3.75;
	
	public abstract boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks);
	public abstract double getPrediction(String player, Query q);
	public abstract void setAdvertiser(String ourAdvertiser);
	
	
}
