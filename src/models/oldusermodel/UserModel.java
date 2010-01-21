package models.oldusermodel;

import java.util.HashMap;
import java.util.Set;

import models.oldusermodel.UserStateTransitionModel.UserState;

import oldagentsSSB.agents.rules.Constants;


import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;

public class UserModel {
        // --------------
        // Output stuff
        // Note: NS users are both NS and T
        private int _userCount[];
        boolean _valid;
        // --------------
        
        // --------------
        // Stuff we use to make output stuff
        private UserStateTransitionModel _transitions;
        
        private int _clicks[];   // Per focus level
        private int _conversions[];
        private int _totalImpressions[];
        private int _ourImpressions[];
        private double _baselineConversion[];
        
        private final int TOTAL_USERS = 90000;

        private HashMap<Query,Integer> m_clicks;
        private HashMap<Query,Integer> m_conversions;
        private HashMap<Query,Integer> m_ourImpressions;
        private HashMap<Query,Integer> m_totalImpressions;
        private HashMap<UserState,Integer> m_populations;
        private static double AVERAGE_CLICK_THROUGH_RATE = .2;
        private Set<Query> m_querySpace;
        private int m_populationSize; 
  
        
        public UserModel(int populationSize, Set<Query> querySpace) {
        	
        	UserStateTransitionModel.init();
        	
        	m_populationSize = populationSize;
        	
        	m_clicks = new HashMap<Query, Integer>();
        	m_conversions = new HashMap<Query, Integer>();
        	m_ourImpressions = new HashMap<Query, Integer>();
        	m_totalImpressions = new HashMap<Query, Integer>();
        	m_querySpace = querySpace;
        	
        }
        
        public void updateReport(QueryReport queryReport, HashMap<Query, Integer> totalImpressions) {
    
        	for(Query query:m_querySpace) {
        		m_clicks.put(query, queryReport.getClicks(query));
        		m_conversions.put(query, queryReport.getClicks(query));
        		m_ourImpressions.put(query,queryReport.getImpressions(query));
        	}
        	
        	
        	m_totalImpressions = totalImpressions;
        	
        	updateModel();
        }
        
		private double getBaselineConversion(Query query) {
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				return Constants.CONVERSION_F0;
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				return Constants.CONVERSION_F1;
			else
				return Constants.CONVERSION_F2;
		}
	
		private UserState getUserState(Query query) {
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				return UserState.F0;
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				return UserState.F1;
			else
				return UserState.F2;
		}
		
		private void updateModel() {
			
			m_populations.put(UserState.IS, 0);
			
			int totalFocusedSearchers = 0;
			for(Query query:m_querySpace) {
				
				int informationalSearchers = (int)Math.floor(m_clicks.get(query) - (m_conversions.get(query) / getBaselineConversion(query)));
		
                                // If there are no impressions for us, we can't
                                // make any judgement
                                if(m_ourImpressions.get(query) == 0) continue;

				// Extrapolate the total number of informational searchers based on how many we've seen
				double extrapolation = m_totalImpressions.get(query) / m_ourImpressions.get(query);
				informationalSearchers *= extrapolation;
				
				m_populations.put(UserState.IS, m_populations.get(UserState.IS) + informationalSearchers);
				m_populations.put(getUserState(query), m_populations.get(getUserState(query)) + m_totalImpressions.get(query) - informationalSearchers);
				totalFocusedSearchers += m_totalImpressions.get(query) - informationalSearchers;
			}
			
			m_populations.put(UserState.TandNS, m_populationSize - totalFocusedSearchers - m_populations.get(UserState.IS));
			
			// Step forward two days
			simulate();
			simulate();
		}
		
		private void simulate() {
			HashMap<UserState,Integer> delta = new HashMap<UserState, Integer>();
			for(UserState state:UserState.values())
				delta.put(state, 0);
			
			// Transition the users
 			for(UserState start:UserState.values()) {
 				for(UserState end:UserState.values()) {
 					delta.put(start, (int) ( delta.get(start) - m_populations.get(start)*UserStateTransitionModel.probability(start, end) ));
 					delta.put(end, (int) ( delta.get(end) + m_populations.get(start)*UserStateTransitionModel.probability(start, end) ));
 				}	
			}
 			
 			// Purchase transitions
 			delta.put(UserState.F0, (int) (delta.get(UserState.F0) - m_populations.get(UserState.F0)*(Constants.CONVERSION_F0)*AVERAGE_CLICK_THROUGH_RATE));
 			delta.put(UserState.TandNS, (int) (delta.get(UserState.TandNS) + m_populations.get(UserState.F0)*(Constants.CONVERSION_F0)*AVERAGE_CLICK_THROUGH_RATE));
 			
 			delta.put(UserState.F1, (int) (delta.get(UserState.F1) - m_populations.get(UserState.F1)*(Constants.CONVERSION_F1)*AVERAGE_CLICK_THROUGH_RATE));
 			delta.put(UserState.TandNS, (int) (delta.get(UserState.TandNS) + m_populations.get(UserState.F1)*(Constants.CONVERSION_F1)*AVERAGE_CLICK_THROUGH_RATE));
 			
 			delta.put(UserState.F2, (int) (delta.get(UserState.F2) - m_populations.get(UserState.F2)*(Constants.CONVERSION_F2)*AVERAGE_CLICK_THROUGH_RATE));
 			delta.put(UserState.TandNS, (int) (delta.get(UserState.TandNS) + m_populations.get(UserState.F2)*(Constants.CONVERSION_F2)*AVERAGE_CLICK_THROUGH_RATE));
 			
 			// Update the values
 			for(UserState state:UserState.values()) {
 				m_populations.put(state,m_populations.get(state) + delta.get(state));
 			}
		}
		
		public int getUserCount(UserState s) {
			return m_populations.get(s);
		}
		
		public double getSearchingRatio() {
			int searching = 0;
			int infoSearching = 0;
			
			searching+=m_populations.get(UserState.F0);
			searching+=m_populations.get(UserState.F1);
			searching+=m_populations.get(UserState.F2);
			
			infoSearching+=m_populations.get(UserState.IS);
			
			return searching/(searching*infoSearching);
		}
		
		// Not used anymore
		/*
        public boolean calculate() {
           int totalIS = 0;
           int totalS = 0;
           
           for(int i = 0; i < QueryType.values().length; i++) {
              int ISUsers = (int)Math.floor(_clicks[i] - (_conversions[i] / _baselineConversion[i]));
              totalIS += ISUsers;
              _userCount[i] = _totalImpressions[i] - ISUsers;
              totalS += _userCount[i];
           }
           
           _userCount[UserStateTransitionModel.UserState.IS.ordinal()] = totalIS;
           _userCount[UserStateTransitionModel.UserState.TandNS.ordinal()] = TOTAL_USERS - totalS - totalIS;
           
           _valid = true;
           return _valid;
        }
        
        public Integer GetUserCount(UserStateTransitionModel.UserState s) {
           if(_valid)
              return _userCount[s.ordinal()];
           else
              return null;
        }
	*/
	
}
