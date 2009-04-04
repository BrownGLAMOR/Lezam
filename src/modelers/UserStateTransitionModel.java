package modelers;

import java.util.Random;

public class UserStateTransitionModel {
	
	public enum UserState {NS, IS, F0, F1, F2, T};
	
	public double probability(UserState start, UserState end) {
		// TODO: is this correct?
		return (new Random()).nextDouble();
	}
}
