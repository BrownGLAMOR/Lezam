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

public class MBarrowsImpl extends AbstractMaxBarrows {

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
	
	public static double calculateAdvertiserEffect(HashMap<Product, Double> userDist, HashMap<Product, Double> probClick, double averageProbClick, boolean targeted, boolean promoted, Product target, Query q){
		double fpro = 1.0;
		if(promoted){
			fpro = 1.5;
		}
		double TE = 0.5;
		//If targeted
		if(targeted){
			//iterate over the user preferences
			double eq = 0.0;
			for(Product userpref:probClick.keySet()){
				//calculate the component of the sum for users of this preference
				if(target.equals(userpref)){
					double prClick = probClick.get(userpref);
					eq+=userDist.get(userpref)*(prClick/(prClick+(1.0+TE)*fpro-(1.0+TE)*prClick*fpro));
				}else{
					double prClick = probClick.get(userpref);
					eq+=userDist.get(userpref)*(prClick/(prClick+(1/(1.0+TE))*fpro-(1/(1.0+TE))*prClick*fpro));
				}
				//sum to get estimate for advertiser effect
			}
			return eq;
		}else{
			//using probability of click for the entire user population,
			//calculate estimate for advertiser effect
			double eq = averageProbClick/(averageProbClick+fpro+averageProbClick*fpro);
			return eq;
		}
	}
	
	public static double calculateProbClick(double views, double clicks){
		return clicks/views;
	}
	
	public static HashMap<Product, Double> estimateImpressionDist(HashMap<Product, Double> userDist, double impressions){
		HashMap<Product,Double> toreturn = new HashMap<Product,Double>();
		double sum = 0;
		for(Product userpref:userDist.keySet()){
			sum += userDist.get(userpref);
		}
		for(Product prod:userDist.keySet()){
			double percentage = userDist.get(prod)/sum;
			toreturn.put(prod, percentage*impressions);
		}
		return toreturn;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		
		//Test
		return null;
	}

}
