package modelers;

import java.util.Random;

public class UserStateTransitionModel {
	
        public enum UserState {F0, F1, F2, TandNS, IS};
        
        public enum QueryType {F0, F1, F2};
	
	public double probability(UserState start, UserState end) {
		// TODO: is this correct?
		return (new Random()).nextDouble();
	}
}
