package models.mbarrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class ParameterEstimation extends AbstractMaxBarrows {
	
	private ArrayList<Query> m_queries;
	private HashMap<Query, QueryHandler> m_queryHandlers;
	
	double _probClick;
	
	public ParameterEstimation()
	{
		m_queries = new ArrayList<Query>();
		m_queryHandlers = new HashMap<Query, QueryHandler>();
		
		 // Get the 16 queries
		 m_queries.add(new Query(null, null));
		 m_queries.add(new Query("lioneer", null));
		 m_queries.add(new Query(null, "tv"));
		 m_queries.add(new Query("lioneer", "tv"));
		 m_queries.add(new Query(null, "audio"));
		 m_queries.add(new Query("lioneer", "audio"));
		 m_queries.add(new Query(null, "dvd"));
		 m_queries.add(new Query("lioneer", "dvd"));
		 m_queries.add(new Query("pg", null));
		 m_queries.add(new Query("pg", "tv"));
		 m_queries.add(new Query("pg", "audio"));
		 m_queries.add(new Query("pg", "dvd"));
		 m_queries.add(new Query("flat", null));
		 m_queries.add(new Query("flat", "tv"));
		 m_queries.add(new Query("flat", "audio"));
		 m_queries.add(new Query("flat", "dvd"));
		 
		 for(Query q: m_queries){
			 m_queryHandlers.put(q, new QueryHandler(q));
		 }
		 
//		 for (Query q : m_queries)
//		 {
//				QueryType qt = q.getType();
//				
//				Double [] advertiserEffects = new Double[8];
//				
//				if(qt == QueryType.FOCUS_LEVEL_TWO)
//				{
//					 //For each query, store the average continuation probabilities
//					m_continuationProbs.put(q, _continuationProbBoundsAvg[2]);
//					 //For each query, store the average advertiser effects
//					for(int i = 0; i < advertiserEffects.length; i++)
//					{
//						advertiserEffects[i] = _advertiserEffectsBoundsAvg[2];
//					}
//					m_advertiserEffects.put(q, advertiserEffects);
//				}
//				
//				if(qt == QueryType.FOCUS_LEVEL_ONE)
//				{
//					 //For each query, store the average continuation probabilities
//					m_continuationProbs.put(q, _continuationProbBoundsAvg[1]);
//					 //For each query, store the average advertiser effects
//					for(int i = 0; i < advertiserEffects.length; i++){
//						advertiserEffects[i] = _advertiserEffectsBoundsAvg[1];
//					}
//					m_advertiserEffects.put(q, advertiserEffects);
//				}
//				
//				if(qt == QueryType.FOCUS_LEVEL_ZERO)
//				{
//					 //For each query, store the average continuation probabilities
//					m_continuationProbs.put(q, _continuationProbBoundsAvg[0]);
//					 //For each query, store the average advertiser effects
//					for(int i = 0; i < advertiserEffects.length; i++){
//						advertiserEffects[i] = _advertiserEffectsBoundsAvg[0];
//					}
//					m_advertiserEffects.put(q, advertiserEffects);
//				}
//		 }
	}
	
	
	public void calculateAdvertiserEffect(boolean targted, boolean promoted)
	{
		
		
		
	}
	
	
	
	
	/*public void probabilityOfClick(int clicks, int views)
	{
		if (views != 0)
		{
			_probClick = clicks / views; 
		}
		else
		{
			_probClick = 0;
		}
		
	}*/
	
	/*probConv = 
	
	public void calculateProbCon(int impressions, int drops, double probConv, int prevConv, int slot)
	{
		int views = 7;
		
		//views = impressions * (Math.pow((1 - _probClick + _probClick * (1 - probConv)) * probCont, slot-1);
			
		
		probConv = Math.pow(e,);
		
	}*/
	
	
	
	
	
	
	

	@Override
	public double[] getPrediction(Query q) {
//		Double[] toconvert = m_advertiserEffects.get(q);
//		double[] toreturn = new double[toconvert.length];
//		for (int index = 0; index < toconvert.length; index++){
//			toreturn[index]=toconvert[index];
//		}
		return m_queryHandlers.get(q).getPredictions();
	}
	
	@Override
	public boolean updateModel(
			String ourAgent,
			QueryReport queryReport, 
			SalesReport salesReport,
			HashMap<Query,Integer> numberPromotedSlots,
			HashMap<Query,LinkedList<Integer>> impressionsPerSlot,
			HashMap<Query,LinkedList<LinkedList<String>>> advertisersAbovePerSlot,
			HashMap<String,HashMap<Query,Ad>> ads,
			HashMap<Product,HashMap<UserState,Integer>> userStates) {
		
		for(Query q: m_queries){
			HashMap<String, Ad> query_ads = new HashMap<String,Ad>();
			for(String s : ads.keySet()){
				query_ads.put(s, ads.get(s).get(q));
			}
			m_queryHandlers.get(q).update(ourAgent,queryReport,salesReport,numberPromotedSlots,impressionsPerSlot.get(q),advertisersAbovePerSlot.get(q),query_ads,userStates);
		}
		// TODO Auto-generated method stub
		return false;
	}

//	public boolean updateModel(QueryReport queryReport,
//			SalesReport salesReport, LinkedList<Integer> impressionsPerSlot,
//			LinkedList<LinkedList<String>> advertisersAbovePerSlot,
//			HashMap<String, Ad> ads,
//			HashMap<Product, HashMap<UserState, Double>> userStates) {
//		
//		for(Query q: m_queries){
//			m_queryHandlers.get(q).update(queryReport, salesReport, impressionsPerSlot, advertisersAbovePerSlot, ads, userStates);
//		}
//		
////		//For each query
////		for (Query q: m_queries){
////			//For each slot
////			for (LinkedList<String> aboveme : advertisersAbovePerSlot){
////				//If in top slot, estimate advertiser effect
////				if(aboveme.size()==0){
////					
////				}
////				//If not, estimate conversion probability
////				double convProb = 0.11;
////				if(q.getType()==QueryType.FOCUS_LEVEL_TWO){
////					convProb = 0.36;
////				}
////				if(q.getType()==QueryType.FOCUS_LEVEL_ONE){
////					convProb = 0.23;
////				}
////				//Pick a good value for continuation prob based on that
////				
////			}
////		}
//		
//		
//		return false;
//	}
	
	
	
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
	
	public static HashMap<Product, Double> estimateUserDist(HashMap<Product, HashMap<UserState, Double>> userStates){
		HashMap<Product,Double> toreturn = new HashMap<Product,Double>();
		double sum = 0;
		for(Product userpref:userStates.keySet()){
			for(UserState userstate:userStates.get(userpref).keySet()){
				sum += userStates.get(userpref).get(userstate);
			}
			toreturn.put(userpref, sum);
		}
		return toreturn;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}
}
