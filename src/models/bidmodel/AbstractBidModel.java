package models.bidmodel;

import java.util.HashMap;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class AbstractBidModel extends AbstractModel {
	private static final double maxReasonableBid = 3.75;//TODO: CHANGE THIS TO THE APPROPRIATE NEW VALUE
	
	public abstract boolean updateModel(double cpc, double ourBid, HashMap<String, Integer> ranks);
	public abstract double getPrediction(String player);
}
