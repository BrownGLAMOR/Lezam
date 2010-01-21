package models.oldusermodel;



/*
 * How This is all going to work:
 *
 * First:  Analyze the virtualization
 * What does the analysis entail:
 * 		Mean, std deviation, etc.
 * 		How different are the different possibilities of bursting and not
 * 			-i.e. maybe a burst on day one or two in the virtualization don't really make a difference?
 * 		Use confidence intervals to determine what our best guess for the start of the game is.... 
 * 
 * Analyze the game with transactions:
 * 		Remember that we need to do a separate analysis depending on how the virtualization went
 * 		mean, std deviation, etc for all the different bursting probs
 * 		Remember now we need to do a bunch of different simulations depending on the sales to see how this effects things
 * 
 * 
 * Use probabilities and confidence intervals to try to narrow it down to less than 1000? (100?) possible user states depending on when bursts happened and whatnot!
 * 
 * This is going to take forever to run....
 * 
 * Difficulties::
 * 		-Remember to predict how long this will take to run (make sure it won't take like years hahahah)
 * 		-This is going to take a while to run, need to think of best method to aggregate data
 * 		-So I am going to have like 10-20 different types of virtualizations that can all happen, all with different means, std devs,
 * 		and probabilities of occurrence.  How do I go from these to inferring things about game dynamics.  Clearly I need to consider what could happen starting from any
 * 		starting configuration, but how do I make sure to consider the variance in each one of these options?
 * 
 * 
 * 	IDEA:
 * 
 *  Build a binary string recursively and keep a count of how many ones it has and make sure that it does not go voer the total...
 * 
 */

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import edu.umich.eecs.tac.props.Product;

public class UserSteadyStateDist {

	Random _R = new Random();

	protected double epsilon =  10;
	protected double burstprobability =  0.1;

	private HashMap<UserState, HashMap<UserState, Double>> _standardProbs;

	private HashMap<UserState, HashMap<UserState, Double>> _burstProbs;

	private HashMap<Product, HashMap<UserState, Integer>> _users;

	private HashSet<Product> _products;

	public UserSteadyStateDist() {
		//Probability of transitioning from the first state to the second state
		_standardProbs = new HashMap<UserState,HashMap<UserState,Double>>();
		_burstProbs = new HashMap<UserState,HashMap<UserState,Double>>();

		HashMap<UserState,Double> standardFromNSProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromISProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF0Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF1Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF2Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromTProbs = new HashMap<UserState, Double>();

		HashMap<UserState,Double> burstFromNSProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromISProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF0Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF1Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF2Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromTProbs = new HashMap<UserState, Double>();

		standardFromNSProbs.put(UserState.NS,0.99);
		standardFromNSProbs.put(UserState.IS,0.01);
		standardFromNSProbs.put(UserState.F0,0.0);
		standardFromNSProbs.put(UserState.F1,0.0);
		standardFromNSProbs.put(UserState.F2,0.0);
		standardFromNSProbs.put(UserState.T,0.0);

		standardFromISProbs.put(UserState.NS,0.05);
		standardFromISProbs.put(UserState.IS,0.2);
		standardFromISProbs.put(UserState.F0,0.6);
		standardFromISProbs.put(UserState.F1,0.1);
		standardFromISProbs.put(UserState.F2,0.05);
		standardFromISProbs.put(UserState.T,0.0);

		standardFromF0Probs.put(UserState.NS,0.1);
		standardFromF0Probs.put(UserState.IS,0.0);
		standardFromF0Probs.put(UserState.F0,0.7);
		standardFromF0Probs.put(UserState.F1,0.2);
		standardFromF0Probs.put(UserState.F2,0.0);
		standardFromF0Probs.put(UserState.T,0.0);

		standardFromF1Probs.put(UserState.NS,0.1);
		standardFromF1Probs.put(UserState.IS,0.0);
		standardFromF1Probs.put(UserState.F0,0.0);
		standardFromF1Probs.put(UserState.F1,0.7);
		standardFromF1Probs.put(UserState.F2,0.2);
		standardFromF1Probs.put(UserState.T,0.0);

		standardFromF2Probs.put(UserState.NS,0.1);
		standardFromF2Probs.put(UserState.IS,0.0);
		standardFromF2Probs.put(UserState.F0,0.0);
		standardFromF2Probs.put(UserState.F1,0.0);
		standardFromF2Probs.put(UserState.F2,0.9);
		standardFromF2Probs.put(UserState.T,0.0);

		standardFromTProbs.put(UserState.NS,0.8);
		standardFromTProbs.put(UserState.IS,0.0);
		standardFromTProbs.put(UserState.F0,0.0);
		standardFromTProbs.put(UserState.F1,0.0);
		standardFromTProbs.put(UserState.F2,0.0);
		standardFromTProbs.put(UserState.T,0.2);

		burstFromNSProbs.put(UserState.NS,0.8);
		burstFromNSProbs.put(UserState.IS,0.2);
		burstFromNSProbs.put(UserState.F0,0.0);
		burstFromNSProbs.put(UserState.F1,0.0);
		burstFromNSProbs.put(UserState.F2,0.0);
		burstFromNSProbs.put(UserState.T,0.0);

		burstFromISProbs.put(UserState.NS,0.05);
		burstFromISProbs.put(UserState.IS,0.2);
		burstFromISProbs.put(UserState.F0,0.6);
		burstFromISProbs.put(UserState.F1,0.1);
		burstFromISProbs.put(UserState.F2,0.05);
		burstFromISProbs.put(UserState.T,0.0);

		burstFromF0Probs.put(UserState.NS,0.1);
		burstFromF0Probs.put(UserState.IS,0.0);
		burstFromF0Probs.put(UserState.F0,0.7);
		burstFromF0Probs.put(UserState.F1,0.2);
		burstFromF0Probs.put(UserState.F2,0.0);
		burstFromF0Probs.put(UserState.T,0.0);

		burstFromF1Probs.put(UserState.NS,0.1);
		burstFromF1Probs.put(UserState.IS,0.0);
		burstFromF1Probs.put(UserState.F0,0.0);
		burstFromF1Probs.put(UserState.F1,0.7);
		burstFromF1Probs.put(UserState.F2,0.2);
		burstFromF1Probs.put(UserState.T,0.0);

		burstFromF2Probs.put(UserState.NS,0.1);
		burstFromF2Probs.put(UserState.IS,0.0);
		burstFromF2Probs.put(UserState.F0,0.0);
		burstFromF2Probs.put(UserState.F1,0.0);
		burstFromF2Probs.put(UserState.F2,0.9);
		burstFromF2Probs.put(UserState.T,0.0);

		burstFromTProbs.put(UserState.NS,0.8);
		burstFromTProbs.put(UserState.IS,0.0);
		burstFromTProbs.put(UserState.F0,0.0);
		burstFromTProbs.put(UserState.F1,0.0);
		burstFromTProbs.put(UserState.F2,0.0);
		burstFromTProbs.put(UserState.T,0.2);

		_standardProbs.put(UserState.NS,standardFromNSProbs);
		_standardProbs.put(UserState.IS,standardFromISProbs);
		_standardProbs.put(UserState.F0,standardFromF0Probs);
		_standardProbs.put(UserState.F1,standardFromF1Probs);
		_standardProbs.put(UserState.F2,standardFromF2Probs);
		_standardProbs.put(UserState.T,standardFromTProbs);

		_burstProbs.put(UserState.NS,burstFromNSProbs);
		_burstProbs.put(UserState.IS,burstFromISProbs);
		_burstProbs.put(UserState.F0,burstFromF0Probs);
		_burstProbs.put(UserState.F1,burstFromF1Probs);
		_burstProbs.put(UserState.F2,burstFromF2Probs);
		_burstProbs.put(UserState.T,burstFromTProbs);

		//Sanity Check!
		for(UserState fromState : UserState.values()) {
			double standardTot = 0.0;
			double burstTot = 0.0;
			for(UserState toState : UserState.values()) {
				standardTot += _standardProbs.get(fromState).get(toState);
				burstTot += _burstProbs.get(fromState).get(toState);
			}
			if(standardTot != 1.0 || burstTot != 1.0) {
				throw new RuntimeException("Make sure the probs sum to one!");
			}
		}

		_users = new HashMap<Product,HashMap<UserState,Integer>>();

		_products = new HashSet<Product>();

		_products.add(new Product("pg","tv"));
		_products.add(new Product("pg","dvd"));
		_products.add(new Product("pg","audio"));

		_products.add(new Product("flat","tv"));
		_products.add(new Product("flat","dvd"));
		_products.add(new Product("flat","audio"));

		_products.add(new Product("lioneer","tv"));
		_products.add(new Product("lioneer","dvd"));
		_products.add(new Product("lioneer","audio"));

		initializeUsers();
	}

	private void transitionUsers(boolean burst) {
		HashMap<UserState, HashMap<UserState, Double>> transProbs;
		if(burst) {
			transProbs = _burstProbs;
		}
		else {
			transProbs = _standardProbs;
		}
		HashMap<Product,HashMap<UserState,Integer>> oldUsers = copyUsers(_users);
		zeroOutUsers();
		for(Product prod : _products) {
			for(UserState state : UserState.values()) {
				HashMap<UserState, Integer> users = oldUsers.get(prod);
				for(int i = 0; i < users.get(state); i++) {
					double rand = _R.nextDouble();
					double threshhold = transProbs.get(state).get(UserState.NS);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.NS, tempUsers.get(UserState.NS)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					threshhold += transProbs.get(state).get(UserState.IS);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.IS, tempUsers.get(UserState.IS)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					threshhold += transProbs.get(state).get(UserState.F0);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.F0, tempUsers.get(UserState.F0)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					threshhold += transProbs.get(state).get(UserState.F1);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.F1, tempUsers.get(UserState.F1)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					threshhold += transProbs.get(state).get(UserState.F2);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.F2, tempUsers.get(UserState.F2)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					threshhold += transProbs.get(state).get(UserState.T);
					if(rand <= threshhold) {
						HashMap<UserState, Integer> tempUsers = _users.get(prod);
						tempUsers.put(UserState.T, tempUsers.get(UserState.T)+1);
						_users.put(prod,tempUsers);
						continue;
					}
					else {
						throw new RuntimeException("Transition Probs don't sum to 1");
					}
				}
			}
		}
	}

	private void initializeUsers() {
		for(Product prod : _products) {
			HashMap<UserState,Integer> tempUsers = new HashMap<UserState, Integer>();
			for(UserState state : UserState.values()) {
				if(state == UserState.NS) {
					tempUsers.put(state, 10000);
				}
				else {
					tempUsers.put(state, 0);
				}
			}
			_users.put(prod, tempUsers);
		}
	}

	private void zeroOutUsers() {
		for(Product prod : _products) {
			HashMap<UserState,Integer> tempUsers = new HashMap<UserState, Integer>();
			for(UserState state : UserState.values()) {
				tempUsers.put(state, 0);
			}
			_users.put(prod, tempUsers);
		}
	}

	private HashMap<Product, HashMap<UserState, Integer>> copyUsers(HashMap<Product, HashMap<UserState, Integer>> users) {
		HashMap<Product, HashMap<UserState, Integer>> usersCopy = new HashMap<Product, HashMap<UserState,Integer>>();
		for(Product prod : _products) {
			HashMap<UserState,Integer> tempUsers = new HashMap<UserState, Integer>();
			for(UserState state : UserState.values()) {
				tempUsers.put(state,users.get(prod).get(state));
			}
			usersCopy.put(prod, tempUsers);
		}
		return usersCopy;
	}

	/**
	 * Simulates one virtual day with no impressions, clicks or conversions
	 */
	private void simulateVirtualDay() {
		double rand = _R.nextDouble();
		if(rand <= burstprobability) {
			transitionUsers(false);
		}
		else {
			transitionUsers(true);
		}
	}

	/**
	 * Simulates one normal game day with the specified transitions (burst or not)
	 * @param transProbs State to state transition prob matrix
	 */
	private void simulateDayWithTransactions(boolean burst) {
		int numAdvertisers = 8;
		//TODO
		int avgCapacity = 400;
		int totalCapacity = numAdvertisers*avgCapacity;
		int dailyTotCap = totalCapacity/5;
		int perProdDailyTotCap = dailyTotCap/9;
		for(Product prod : _products) {
			int dailyCap = (int) (perProdDailyTotCap * randDouble(.9, 1.1));
			HashMap<UserState, Integer> users = _users.get(prod);
			int numF0Users = users.get(UserState.F0);
			int numF1Users = users.get(UserState.F1)*2; //We multiply by 2 because F1 is twice as likely to convert as F0
			int numF2Users = users.get(UserState.F2)*3;
			int convertingUsers = numF0Users + numF1Users + numF2Users;
			for(int i = 0; i < dailyCap; i++) {
				double rand = _R.nextDouble();
				double threshhold = numF0Users/((double)convertingUsers);
				if(rand <= threshhold) {
					users.put(UserState.F0, users.get(UserState.F0)-1);
					users.put(UserState.T, users.get(UserState.T)+1);
					continue;
				}
				threshhold += numF1Users/((double)convertingUsers);
				if(rand <= threshhold) {
					users.put(UserState.F1, users.get(UserState.F1)-1);
					users.put(UserState.T, users.get(UserState.T)+1);
					continue;
				}
				threshhold += numF2Users/((double)convertingUsers);
				if(rand <= threshhold) {
					users.put(UserState.F2, users.get(UserState.F2)-1);
					users.put(UserState.T, users.get(UserState.T)+1);
					continue;
				}
			}
			_users.put(prod, users);
		}
		transitionUsers(burst);
	}

	private void printUsers() {
		for(Product prod : _products) {
			System.out.println("Product: " + prod);
			printMap(_users.get(prod));
		}
	}

	private void printMap(HashMap<UserState,Integer> map) {
		for(UserState state : UserState.values()) {
			System.out.println("\t Users " + state + ": " + map.get(state));
		}
	}

	private Set<String> generateStringList(int numDays, int numBurstDays, Set<String> strings) {
		if(strings == null && numDays > 0) {
			strings = new HashSet<String>();
			if(numDays != numBurstDays) {
				strings.add("0");
			}
			if(numBurstDays != 0) {
				strings.add("1");
			}
			return generateStringList(numDays-1,numBurstDays,strings);
		}
		if(numDays > 0) {
			HashSet<String> newStrings = new HashSet<String>();
			for(String string : strings) {
				if(numDays-1 + sumOnes(string) >= numBurstDays) {
					newStrings.add(string+"0");
				}
				if(sumOnes(string) < numBurstDays) {
					newStrings.add(string+"1");
				}
			}
			return generateStringList(numDays-1,numBurstDays,newStrings);
		}
		return strings;
	}

	private int sumOnes(String string) {
		int tot = 0;
		for(int i = 0; i < string.length(); i++) {
			if(string.charAt(i) == '1') {
				tot++;
			}
		}
		return tot;
	}

	//Returns a random double rand such that a <= r < b
	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}


	private void analyzeVirutalization() {
		int totalIters = 0;
		int numDays = 5;
		Set<String> strings = new HashSet<String>();
		double epsilon = .0005;
		int numOnesNeeded = 0;
		double totalProb = 0;
		for(int i = 0; i <= numDays; i++) {
			numOnesNeeded = i;
			totalProb += power(.1,i)*power(.9,(numDays-i))*(factorial(numDays)/(factorial(i)*factorial(numDays-i)));
			if(totalProb + epsilon > 1.0) {
				break;
			}
		}
		System.out.println(numOnesNeeded);
//		for(int i = 0; i <= numOnesNeeded; i++) {
		for(int i = 0; i <= numDays; i++) {
			Set<String> tempStrings = generateStringList(numDays, i, null);
			strings.addAll(tempStrings);
		}
		System.out.println(strings.size());
		int numSims = 5;
		HashMap<UserState,Double> estimateMap = new HashMap<UserState, Double>();
		for(UserState state : UserState.values()) {
			estimateMap.put(state, 0.0);
		}
		double totprob = 0;
		for(String string : strings) {
			for(int i = 0; i < numSims; i++) {
				initializeUsers();
				for(int j = 0; j < string.length(); j++) {
					totalIters++;
					if(string.charAt(j) == '0') {
						transitionUsers(false);
					}
					else if(string.charAt(j) == '1') {
						transitionUsers(true);
					}
					else {
						throw new RuntimeException("Malformed string");
					}
					if(totalIters % 1000 == 0) {
						System.out.println(totalIters);
					}
				}
				for(Product product : _products) {
					for(UserState state : UserState.values()) {
						double estimate = _users.get(product).get(state);
						int numOnes = sumOnes(string);
						int numZeros = numDays - numOnes;
						double probability = power(.1,numOnes) * power(.9, numZeros);
						totprob += probability;
						estimate *= probability;
						estimate /= (_products.size()*numSims);
						estimateMap.put(state,estimateMap.get(state) + estimate);
					}
				}
			}
		}
		System.out.println(totprob/(UserState.values().length*_products.size()*numSims));
		for(UserState state : UserState.values()) {
			System.out.println(state + ": " + estimateMap.get(state));
		}
	}

	private void virtualExpectedUsers() {
		for(Product prod : _products) {
			HashMap<UserState,Integer> tempUsers = new HashMap<UserState, Integer>();
			tempUsers.put(UserState.NS,8403);
			tempUsers.put(UserState.IS,306);
			tempUsers.put(UserState.F0,541);
			tempUsers.put(UserState.F1,348);
			tempUsers.put(UserState.F2,297);
			tempUsers.put(UserState.T,103);
			_users.put(prod, tempUsers);
		}
	}

	public void analyzeFirstNDays(int n) {
		int totalIters = 0;
		Set<String> strings = new HashSet<String>();
		double epsilon = .0005;
		int numOnesNeeded = 0;
		double totalProb = 0;
		for(int i = 0; i <= n; i++) {
			numOnesNeeded = i;
			totalProb += power(.1,i)*power(.9,(n-i))*(factorial(n)/(factorial(i)*factorial(n-i)));
			if(totalProb + epsilon > 1.0) {
				break;
			}
		}
		for(int i = 0; i <= numOnesNeeded; i++) {
			Set<String> tempStrings = generateStringList(n, i, null);
			strings.addAll(tempStrings);
		}
		int numSims = 5;
		HashMap<UserState,Double> estimateMap = new HashMap<UserState, Double>();
		for(UserState state : UserState.values()) {
			estimateMap.put(state, 0.0);
		}
		double totprob = 0;
		for(String string : strings) {
			for(int i = 0; i < numSims; i++) {
				virtualExpectedUsers();
				for(int j = 0; j < string.length(); j++) {
					totalIters++;
					if(string.charAt(j) == '0') {
						simulateDayWithTransactions(false);
					}
					else if(string.charAt(j) == '1') {
						simulateDayWithTransactions(true);
					}
					else {
						throw new RuntimeException("Malformed string");
					}
					if(totalIters % 1000 == 0) {
						System.out.println(totalIters);
					}
				}
				for(Product product : _products) {
					for(UserState state : UserState.values()) {
						double estimate = _users.get(product).get(state);
						int numOnes = sumOnes(string);
						int numZeros = n - numOnes;
						double probability = power(.1,numOnes) * power(.9, numZeros);
						totprob += probability;
						estimate *= probability;
						estimate /= (_products.size()*numSims);
						estimateMap.put(state,estimateMap.get(state) + estimate);
					}
				}
			}
		}
		System.out.println(totprob/(UserState.values().length*_products.size()*numSims));
		for(UserState state : UserState.values()) {
			System.out.println(state + ": " + estimateMap.get(state));
		}
	}

	public double power(double a, int b) {
		if (b == 0) {
			return 1;
		}
		double pow = a;
		for(int i = 1; i < b; i++) {
			pow *= a;
		}
		return pow;
	}

	public double factorial(int x) {
		switch (x) {
		case 0:
			return 1;
		case 1:
			return 1;
		case 2:
			return 2;
		case 3:
			return 6;
		case 4:
			return 24;
		case 5:
			return 120;
		case 6:
			return 720;
		case 7:
			return 5040;
		case 8:
			return 40320;
		case 9:
			return 362800;
		case 10:
			return 3628800;
		case 11:
			return 39916800;
		case 12:
			return 479001600;
		case 13:
			return 6227020800.0;
		case 14:
			return 87178291200.0;
		case 15:
			return 1307674368000.0;
		case 16:
			return 20922789888000.0;
		case 17:
			return 355687428096000.0;
		case 18:
			return 6402373705728000.0;
		case 19:
			return 121645100408832000.0;
		case 20:
			return 2432902008176640000.0;
		default:
			return -1;
		}
	}

	public static void main(String[] args) {
		System.out.println("5 SIMS and SYSTEM 400");
		UserSteadyStateDist steadyState = new UserSteadyStateDist();
		double start = System.currentTimeMillis();

		for(int i = 1; i <= 15; i++) {
			System.out.println(i + " days: ");
			steadyState.analyzeFirstNDays(i);
		}
		
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}
}