package newmodels.oldusermodel;

import java.util.HashMap;

public class UserStateTransitionModel {

    private static boolean _inited = false;
    private static HashMap<UserStatePair,Double> _transitions;

    private static class UserStatePair {

        public UserState from;
        public UserState to;

        public UserStatePair(UserState f, UserState t) {
            from = f;
            to = t;
        }

        public boolean equals(Object o) {
            UserStatePair other = (UserStatePair)o;
            return other.from == from && other.to == to;
        }
    }
    

    public static boolean init() {
        if(_inited) return true;

        
        _transitions = new HashMap<UserStatePair,Double>();
        _transitions.put(new UserStatePair(UserState.TandNS,UserState.IS),.01);
        _transitions.put(new UserStatePair(UserState.TandNS,UserState.TandNS),.99);
        
        _transitions.put(new UserStatePair(UserState.IS,UserState.F0),.6);
        _transitions.put(new UserStatePair(UserState.IS,UserState.F1),.2);
        _transitions.put(new UserStatePair(UserState.IS,UserState.F2),.05);
        _transitions.put(new UserStatePair(UserState.IS,UserState.TandNS),.05);
        _transitions.put(new UserStatePair(UserState.IS,UserState.IS),.2);

        _transitions.put(new UserStatePair(UserState.F0,UserState.F0),.7);
        _transitions.put(new UserStatePair(UserState.F0,UserState.F1),.2);
        _transitions.put(new UserStatePair(UserState.F0,UserState.TandNS),.1);

        _transitions.put(new UserStatePair(UserState.F1,UserState.F1),.7);
        _transitions.put(new UserStatePair(UserState.F1,UserState.F2),.2);
        _transitions.put(new UserStatePair(UserState.F1,UserState.TandNS),.1);
        
        _transitions.put(new UserStatePair(UserState.F2,UserState.F2),.9);
        _transitions.put(new UserStatePair(UserState.F2,UserState.TandNS),.1);

        _inited = true;
        return _inited;
    }
        public enum UserState {F0, F1, F2, TandNS, IS};
        
        public enum QueryType {F0, F1, F2};
	
	public static double probability(UserState start, UserState end) {

            init();

            Double prob = _transitions.get(new UserStatePair(start,end));
            if(prob != null) return prob;
            else return 0;

	}
}
