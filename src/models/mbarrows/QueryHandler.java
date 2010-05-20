package models.mbarrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.math.complex.Complex;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class QueryHandler extends ConstantsAndFunctions {
	final Query _query;
	final QueryType _queryType;
	
	final static double EPSILON = .0001;
	
	LinkedList<DayHandler> _dayHandlers;

	// 1st - promoted
	// 2nd - targeted - 1 is targeted, 2 is targeted incorrectly
	// 3rd - numerator, denominator
	double targetedPromoted[][][];

	public QueryHandler(Query q) {
		
		
		_query = q;
		_queryType = q.getType();
		
		_dayHandlers = new LinkedList<DayHandler>();

		targetedPromoted = new double[3][2][2];
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				targetedPromoted[targeted][promoted][0] = forwardClickProbability(
						_advertiserEffectBoundsAvg[2],
						fTargetfPro[promoted][targeted]);
				targetedPromoted[targeted][promoted][1] = 1;
			}
		}
	}

	// Returns advertiser effect and continuation probability
	public double[] getPredictions() {
		double tempAdvertiserEffect = 0;
		for (int targeted = 0; targeted < 3; targeted++) {
			for (int promoted = 0; promoted < 2; promoted++) {
				tempAdvertiserEffect += inverseClickProbability(
						targetedPromoted[targeted][promoted][0]
								/ targetedPromoted[targeted][promoted][1],
						fTargetfPro[promoted][targeted]);
			}
		}
		// For now! TODO: later test results for this compared to weighted average
		tempAdvertiserEffect /= 6;

		double tempContinuationProb = 0;
		// TODO
		double[] tempArr = { tempAdvertiserEffect, tempContinuationProb };
		return tempArr;
	}
	
	public boolean update(QueryReport queryReport,
			SalesReport salesReport, LinkedList<Integer> impressionsPerSlot,
			LinkedList<LinkedList<String>> advertisersAbovePerSlot,
			HashMap<String, Ad> ads,
			HashMap<Product, HashMap<UserState, Integer>> userStates){
		
		//getCurrent predictions
		
		double[] predictions = getPredictions();
		double advertiserEffect = predictions[0];
		
		//Were we ever not in a top slot
		boolean notinslot1 = false;
		for(LinkedList<String> advertisersabove : advertisersAbovePerSlot){
			if(advertisersabove.size()>0){
				notinslot1 = true;
				break;
			}
		}
		//If we were ever not in top slot, make day handler
		if(notinslot1){
			
			int totalClicks = queryReport.getClicks(_query);
			
			//Make list of Ads above
			LinkedList<LinkedList<Ad>> adsAbovePerSlot = getAdsAbovePerSlot(advertisersAbovePerSlot,ads);
			
			//get states of searching users
			 HashMap<Product,int[]> statesSearchingUsers = getStatesOfSearchingUsers(userStates);
			
			DayHandler latestday = new DayHandler(_query, totalClicks, impressionsPerSlot, advertiserEffect, adsAbovePerSlot, statesSearchingUsers);
			
			_dayHandlers.add(latestday);
			
		}else{
			_dayHandlers.add(null);
		}
		
		return false;
	}
	
	public LinkedList<LinkedList<Ad>> getAdsAbovePerSlot(LinkedList<LinkedList<String>> advertisersAbovePerSlot, HashMap<String, Ad> ads){
		LinkedList<LinkedList<Ad>> adsAbovePerSlot = new LinkedList<LinkedList<Ad>>();
		//for every slot
		for(LinkedList<String> thoseAbove : advertisersAbovePerSlot){
			//make a linked list of Ads
			LinkedList<Ad> adsAbove = new LinkedList<Ad>();
			//for each advertiser above
			for(String advertiser : thoseAbove){
				//add their add to the list
				adsAbove.add(ads.get(advertiser));
			}
			adsAbovePerSlot.add(adsAbove);
		}
		return adsAbovePerSlot;
	}
	
	public HashMap<Product,int[]> getStatesOfSearchingUsers(HashMap<Product, HashMap<UserState, Integer>> userStates){
		HashMap<Product, int[]> toreturn = new HashMap<Product, int[]>();
		//for each product
		for (Product p : userStates.keySet()){
			HashMap<UserState, Integer> states = userStates.get(p);
			//count up how many searching users there were
			int ISusers = states.get(UserState.IS);
			int nonISusers = states.get(UserState.F0)+states.get(UserState.F1)+states.get(UserState.F2);
			int [] statesOfSearchingUsers = {ISusers,nonISusers};
			toreturn.put(p,statesOfSearchingUsers);
		}
		return toreturn;
	}


//	@Override
//	public double[] getPrediction(Query q) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean updateModel(
//			QueryReport queryReport,
//			SalesReport salesReport,
//			HashMap<Query, LinkedList<Integer>> impressionsPerSlot,
//			HashMap<Query, LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
//			HashMap<String, HashMap<Query, Ad>> ads,
//			HashMap<Product, HashMap<UserState, Integer>> userStates) {
//		// TODO Auto-generated method stub
//		return false;
//	}



	
	
	
	
	

}
