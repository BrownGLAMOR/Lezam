package agents.modelbased;

import java.util.Arrays;
import java.util.HashMap;


public class DPMultiday {

	//======================================= FIELDS =======================================

	/**
	 * For a given starting capacity and ending capacity, this stores the profit of the
	 * best solution. 
	 * (startCap, endCap) -> profit
	 */
	public HashMap<Integer, HashMap<Integer, Double>> profitGivenCapacities;

	
	//profit for state can keep track of profits/actions for each day, or just the current day being considered
	public HashMap profitForState;
	public HashMap actionForState;
	
	
	int capacityWindow; //How many past days are considered when computing total capacity used
	int totalCapacityMax; //The maximum amount of capacity we can ever use (across all days in the window)
	int dailyCapacityUsedMin;
	int dailyCapacityUsedMax;
	int dailyCapacityUsedStep;
	int dayStart;
	int dayEnd;
	int[] capacityHistory; //How much you've sold on each of the last [capacityWindow] days
	int[] roundedCapacityHistory; //Same as capacityHistory, but rounded to fit the discretization of the DP
	
	
	/**
	 * If this is true, the entire working solution will not be stored, but instead
	 * only the information that is necessary to output the current day's action.
	 */
	boolean STORE_COMPACT_SOLUTION = true; 
	

	//======================================= CONSTRUCTORS =======================================


	public DPMultiday(int capacityWindow, int totalCapacityMax, int dailyCapacityUsedMin, int dailyCapacityUsedMax, int dailyCapacityUsedStep, int dayStart, int dayEnd, int[] capacityHistory) {

		this.capacityWindow = capacityWindow;
		this.totalCapacityMax = totalCapacityMax;
		this.dailyCapacityUsedMin = dailyCapacityUsedMin;
		this.dailyCapacityUsedMax = dailyCapacityUsedMax;
		this.dailyCapacityUsedStep = dailyCapacityUsedStep;
		this.dayStart = dayStart;
		this.dayEnd = dayEnd;
		this.capacityHistory = capacityHistory;

		profitGivenCapacities = new HashMap<Integer, HashMap<Integer, Double>>();		
		profitForState = new HashMap(); //new TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Double>>>>>();
		actionForState = new HashMap(); //new TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Double>>>>>();
		
		
		//Get the rounded capacity history for today. TODO: Should we setup the DP such that we will consider the exact actual capacity history? 
		roundedCapacityHistory = capacityHistory.clone();
		for (int i=0; i<capacityWindow; i++) {
			int amountOverMin = roundedCapacityHistory[i] - dailyCapacityUsedMin;
			int stepsOverMin = (int) Math.round(amountOverMin / (double) dailyCapacityUsedStep);
			roundedCapacityHistory[i] = dailyCapacityUsedMin + stepsOverMin*dailyCapacityUsedStep;
		}
		
	}



	/**
	 * Given some capacity state (that is, a history of amounts sold within the capacity window) that was just considered,
	 * get the next capacity state to consider.
	 * If no states have been considered yet, null can be passed as input to get the first state.
	 * If all states have been considered, returns null.
	 * @param capacityState
	 * @return
	 */
	private int[] getNextCapacityState(int[] capacityState, int daysIntoFuture) {

		//If we are not very far into the future (e.g. we're getting states for the current day or d+1),
		//we don't actually have to consider EVERY possible state. We're constrained by the agent's actual 
		//history of conversions. Here we compute how many elements are forced to be fixed values 
		//(and which indices we can safely change).
		int numFixedElements = Math.max(0, capacityWindow - daysIntoFuture);
		int lowestIndexToConsider = numFixedElements;
		
		
		//Return a default starting capacity state if none is given
		if (capacityState == null) {
			capacityState = roundedCapacityHistory.clone();
			//If there are some indices that can be filled with min values, do so now.
			//(The only reason there wouldn't be is if we're considering possible states for today. Our sales history completely 
			//constrains in this case. 
			if (lowestIndexToConsider < capacityWindow) {
				Arrays.fill(capacityState, lowestIndexToConsider, capacityWindow, dailyCapacityUsedMin);
			}
			return capacityState;
		}


		//Get the total capacity used so far
		//TODO: We're running through this method *a lot*. Can we speed this method up by passing the totalCapacityUsed value?
		int totalCapacityUsed = 0;
		for (int i=0; i<capacityState.length; i++) totalCapacityUsed += capacityState[i];


		//Increment the previous capacity state [c1, c2, c3, c4]
		//Method: increment c4. If already maxed out, set it to the min and increment c3. If already maxed out...
		// If everything maxed out, return null
		boolean incrementMade = false;
//		for (int daysBack=0; daysBack<capacityWindow && !incrementMade; daysBack++) {
		for (int daysBack=capacityWindow-1; daysBack>=lowestIndexToConsider && !incrementMade; daysBack--) {
			if (capacityState[daysBack] + dailyCapacityUsedStep <= dailyCapacityUsedMax && 
					totalCapacityUsed + dailyCapacityUsedStep <= totalCapacityMax) { 
				capacityState[daysBack] += dailyCapacityUsedStep;
				incrementMade = true;
			} else {
				totalCapacityUsed -= (capacityState[daysBack] - dailyCapacityUsedMin); //reduce the totalCapacityUsed for this adjustment
				capacityState[daysBack] = dailyCapacityUsedMin;
			}
		}

		//Return the updated capacity state if an increment was made; otherwise return null
		if (incrementMade) return capacityState;
		else return null;
	}


	//======================================= MAIN SOLVER =======================================
	public double solve() {
		for (int d=dayEnd; d>=dayStart; d--) {
			int daysIntoFuture = d-dayStart;
			
			//TODO: Make this more flexible so we can easily handle different window sizes
			//TODO: We don't need to consider states that are impossible to reach 
			//  (e.g. states on the current day with different (c1-c4) values than what actually happened)
			int[] capacityHistory = getNextCapacityState(null, daysIntoFuture);
			while(capacityHistory != null) {

				//Compute initial capacity for this state (will be used for computing profit of different decisions)
				int initialCapacity = 0;
				for (int i=0; i<capacityWindow; i++) initialCapacity += capacityHistory[i];

				//Compute new capacity window and new initial capacity (will be used for computing what state we transition to)
				//Note: We'll have to adjust these for whatever action we take!
				int[] newCapacityHistory = new int[capacityWindow];
				for (int i=1; i<capacityWindow; i++) newCapacityHistory[i-1] = capacityHistory[i];
				//int newInitialCapacity = initialCapacity - capacityHistory[0]; 

				//For each possible amount to sell (TODO: this could possibly be a function of initial capacity)
				double bestValue = Double.NEGATIVE_INFINITY;
				int bestC5 = 0;

				//TODO: Is there a speedup where we don't have to check every possible c5 action?
				for (int c5=dailyCapacityUsedMin; c5<=dailyCapacityUsedMax && initialCapacity+c5<=totalCapacityMax; c5+= dailyCapacityUsedStep) {
					//OldState = (d  , c1, c2, c3, c4)
					//NewState = (d+1, c2, c3, c4, c5)
					double profitForAction = getProfitGivenCapacities(initialCapacity, c5);

					newCapacityHistory[capacityWindow-1] = c5;
					double newStateValue = getValueOfState(d+1, newCapacityHistory);

					if (profitForAction + newStateValue > bestValue) {
						bestValue = profitForAction + newStateValue;
						bestC5 = c5;
					}
				}

				//We have now found the best action and value for this state. Store result and continue to the next state.
				setValueOfState(d, capacityHistory, bestValue);
				
				//Only set action of state if it is for the starting day? 
				//(Or only if its for the state we're actually at?)
				if (STORE_COMPACT_SOLUTION && d==dayStart) setActionOfState(d, capacityHistory, bestC5);


				//System.out.println("State=(" + d + ", " + Arrays.toString(capacityHistory) + "), bestValue=" + bestValue + ", bestAction=" + bestC5);

				//Shift the values
				//Get next capacityHistory state to consider
				capacityHistory = getNextCapacityState(capacityHistory, daysIntoFuture);
			}
		}

		
		
		//Return the c5 to use today
		double action = getActionOfState(dayStart, roundedCapacityHistory);
		System.out.println("day=" + dayStart + ", capHistory=" + Arrays.toString(capacityHistory) + ", roundedCapHistory=" + Arrays.toString(roundedCapacityHistory)   + ", action=" + action);
		return action;
	}







	//======================================= SETTERS AND GETTERS =======================================



	public void setProfitGivenCapacities(int startCap, int amountSold, double profit) {
		if (!profitGivenCapacities.containsKey(startCap)) {
			profitGivenCapacities.put(startCap, new HashMap<Integer, Double>());
		}

		if (profitGivenCapacities.get(startCap).containsKey(amountSold)) {
			System.err.println("Capacities (" + startCap + ", " + amountSold + ") already in HashMap; being overridden!");
		}

		profitGivenCapacities.get(startCap).put(amountSold, profit);
	}


	public double getProfitGivenCapacities(int startCap, int amountSold) {
		return profitGivenCapacities.get(startCap).get(amountSold);
	}



	public void setValueOfState(int day, int[] caps, double profit) {
		//System.out.println("Setting value of state (" + day + ", " + Arrays.toString(caps) + ") to " + profit);
		if(STORE_COMPACT_SOLUTION) day = day % 2;
		setMapValue(profitForState, day, caps, profit);
	}

	public void setActionOfState(int day, int[] caps, double profit) {
		if(STORE_COMPACT_SOLUTION) day = day % 2;
		setMapValue(actionForState, day, caps, profit);
		 
	}

	private void setMapValue(HashMap initialMap, int day, int[] caps, double profit) {
		if(!initialMap.containsKey(day)) initialMap.put(day, new HashMap());
		HashMap map = (HashMap) initialMap.get(day);

		//Add all the appropriate capacity keys
		for (int daysBack=0; daysBack<caps.length; daysBack++) {
			int cap = caps[daysBack];
			if (daysBack==caps.length-1) { 
				//If we're on the last dimension of the capacity window, add the profit value
				
				//Print an error message if this key already exists.
				//(Unless we're storing a compact solution)
				if(!STORE_COMPACT_SOLUTION && map.containsKey(cap)) System.err.println("Assigning profit for state that already existed. Overriding."); 
				map.put(cap, profit);
			} else {
				//If we're not on the last dimension, add the new treeMap (if it doesn't yet exist)
				if(!map.containsKey(cap)) map.put(cap, new HashMap());
				map = (HashMap) map.get(cap);				
			}
		}		
	}

	public double getValueOfState(int day, int[] caps) {
		try {
		//If the state being considered is beyond the number of days (as is the case for terminal states), return 0
		if (day > dayEnd) return 0;

		if(STORE_COMPACT_SOLUTION) day = day % 2;
		return getMapValue(profitForState, day, caps);
		} catch (Exception e) {
			System.err.println("Could not find value for day=" + day + ", caps=" + Arrays.toString(caps));
			e.printStackTrace();
			System.exit(-1);
		}
		return -1;
	}

	public double getActionOfState(int day, int[] caps) {
		if(STORE_COMPACT_SOLUTION) day = day % 2;
		return getMapValue(actionForState, day, caps);
	}

	private double getMapValue(HashMap initialMap, int day, int[] caps) {
		HashMap map = (HashMap) initialMap.get(day);

		for (int daysBack=0; daysBack<capacityWindow-1; daysBack++) {
			map = (HashMap) map.get(caps[daysBack]);
		}
		double profit = (Double) map.get(caps[capacityWindow-1]);
		return profit;		
	}





	//======================================= STRING OUTPUT =======================================

	public String getProfitGivenCapacitiesString() {
		StringBuffer sb = new StringBuffer();
		for (int startCap : profitGivenCapacities.keySet()) {
			for (int endCap : profitGivenCapacities.get(startCap).keySet()) {
				double profit = getProfitGivenCapacities(startCap, endCap);
				sb.append(startCap + " " + endCap + " " + profit + "\n");
			}
		}
		return sb.toString();
	}


	public void getSolutionString() {
		int[] caps = capacityHistory.clone();
		
		for (int d=dayStart; d<=dayEnd; d++) {

			//Get best action and the value for this state.
			double c5 = getActionOfState(d, caps);
			double v = getValueOfState(d, caps);

			System.out.println("(" + d + " " + Arrays.toString(caps) + ")\t bestAction=" + c5 + "\tbestValue=" + v);
			//Get capacities for the next state.
			for (int i=1; i<capacityWindow; i++) caps[i-1] = caps[i];
			caps[capacityWindow-1] = (int) c5;
			
			//If we didn't store all solutions, don't output anymore.
			if(STORE_COMPACT_SOLUTION) break;
		}
	}




	//======================================= MAIN METHOD =======================================


	public static void main(String[] args) {
		System.out.println("Starting.");

		int capacityWindow = 2;
		int totalCapacityMax = 4;
		int dailyCapacityUsedMin = 0;
		int dailyCapacityUsedMax = 2;
		int dailyCapacityUsedStep = 1;
		int dayStart = 1;
		int dayEnd = 3;
		int[] capacityHistory = new int[capacityWindow];
		int maxCapacity = 1;
		Arrays.fill(capacityHistory, maxCapacity/capacityWindow);

		DPMultiday dp = new DPMultiday(capacityWindow, totalCapacityMax, dailyCapacityUsedMin, dailyCapacityUsedMax, dailyCapacityUsedStep, dayStart, dayEnd, capacityHistory);



		dp.setProfitGivenCapacities(0, 0, 0);
		dp.setProfitGivenCapacities(0, 1, 10);
		dp.setProfitGivenCapacities(0, 2, 15);
		dp.setProfitGivenCapacities(1, 0, 0);
		dp.setProfitGivenCapacities(1, 1, 8);
		dp.setProfitGivenCapacities(1, 2, 10);
		dp.setProfitGivenCapacities(2, 0, 0);
		dp.setProfitGivenCapacities(2, 1, 6);
		dp.setProfitGivenCapacities(2, 2, 5);
		dp.setProfitGivenCapacities(3, 0, 0);
		dp.setProfitGivenCapacities(3, 1, 4);
		dp.setProfitGivenCapacities(3, 2, 0);
		dp.setProfitGivenCapacities(4, 0, 0);
		dp.setProfitGivenCapacities(4, 1, 2);
		dp.setProfitGivenCapacities(4, 2, -5);

//		//Come up w/ some synthetic profits for (startCapacity, endCapacity)
//		for (int startCap=0; startCap<=capacityWindow*dailyCapacityUsedMax; startCap+=dailyCapacityUsedStep) {
//		for (int c5=0; c5<=dailyCapacityUsedMax; c5+=dailyCapacityUsedStep) {
//		int endCap = startCap + c5;
//		int amountOverCap = Math.max(0, (startCap+c5) - maxCapacity); 
//		double profit = 0;
//		if (c5>0) profit = 10 + 8 * (c5-1) - c5*amountOverCap*3; 
//		dp.setProfitGivenCapacities(startCap, endCap, profit);
//		}
//		}
		System.out.println("Profits given capacities: \n" + dp.getProfitGivenCapacitiesString());		



		//Solve for best action/value at each state
		double c5 = dp.solve();
		System.out.println("Amount to sell today = " + c5);

		//Output values for each state
		//Output actions for each state
		//Output solution path
		dp.getSolutionString();



		System.out.println("Done.");
	}






}
