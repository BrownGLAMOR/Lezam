package models.paramest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class MBarrowsParameterEstimation extends AbstractParameterEstimation {
	
	private ArrayList<Query> m_queries;
	private HashMap<Query, MBarrowsQueryHandler> m_queryHandlers;
	
	double _probClick;
	
	public MBarrowsParameterEstimation()
	{
		m_queries = new ArrayList<Query>();
		m_queryHandlers = new HashMap<Query, MBarrowsQueryHandler>();
		
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
			 m_queryHandlers.put(q, new MBarrowsQueryHandler(q));
		 }
	}

	@Override
	public double[] getPrediction(Query q) {
		return m_queryHandlers.get(q).getPredictions();
	}
	
	@Override
	public boolean updateModel(String ourAgent,
							   QueryReport queryReport, 
							   SalesReport salesReport,
							   int numberPromotedSlots,
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
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new MBarrowsParameterEstimation();
	}
}
