package models.bidmodel;

import java.util.ArrayList;
import java.util.HashMap;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

public class LinearComboBidModel extends AbstractBidModel{
 
	private ArrayList<AbstractBidModel> _models;
	private ArrayList<Double> _weights;
	private double _totalWeight;

	public LinearComboBidModel(ArrayList<AbstractBidModel> models, ArrayList<Double> weights){
		_models = models;
		_weights = weights;
		
		_totalWeight = 0.0;
		for(Double weight : weights) {
			_totalWeight += weight;
		}
	}
	
	public void setAdvertiser(String advertiser) {
		for(AbstractBidModel model : _models) {
			model.setAdvertiser(advertiser);
		}
	}
	
	@Override
	public AbstractModel getCopy() {
		ArrayList<AbstractBidModel> newModels = new ArrayList<AbstractBidModel>();
		for(AbstractBidModel model : _models) {
			newModels.add((AbstractBidModel) model.getCopy());
		}
		return new LinearComboBidModel(newModels,_weights);
	}

	@Override
	public double getPrediction(String player, Query q) {
		double prediction = 0.0;
		for(AbstractBidModel model : _models) {
			prediction += model.getPrediction(player, q);
		}
		return prediction/_totalWeight;
	}

	@Override
	public boolean updateModel(HashMap<Query, Double> cpc, HashMap<Query, Double> ourBid, HashMap<Query, HashMap<String, Integer>> ranks) {
		for(AbstractBidModel model : _models) {
			model.updateModel(cpc, ourBid, ranks);
		}
		return true;
	}

}
