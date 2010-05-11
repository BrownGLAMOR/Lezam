package models.bidmodel;

import java.util.HashMap;

import models.AbstractModel;

public class JointDistBidModel extends AbstractBidModel{

	@Override
	public double getPrediction(String player) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(double cpc, double ourBid,
			HashMap<String, Integer> ranks) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
