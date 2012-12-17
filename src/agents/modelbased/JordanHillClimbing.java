/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import clojure.lang.PersistentHashMap;

import models.unitssold.BasicUnitsSoldModel;

//import agents.AbstractAgent.Predictions;
//import agents.modelbased.MCKP.HillClimbingCreator;
//import agents.modelbased.MCKP.HillClimbingResult;
import agents.AbstractAgent;
import agents.modelbased.MCKP.MultiDay;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import edu.umich.eecs.tac.props.Query;



public class JordanHillClimbing extends HillClimbing {

	/**
	 * 
	 */
	int accountForProbing = 0;
	boolean changeWandV = true;
	boolean adjustBudget = false;
	
	public JordanHillClimbing() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public JordanHillClimbing(int accountForProbing, boolean changeWandV, boolean adjustBudget) {
		super();
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
	}

	public JordanHillClimbing(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization, 
			int accountForProbing, boolean changeWandV, boolean adjustBudget) {
		super( c1, c2,c3, budgetL,budgetM, budgetH, bidMultLow, bidMultHigh, 
				multiDay, multiDayDiscretization);
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
	}

	public JordanHillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,	int multiDayDiscretization,
			int accountForProbing, boolean changeWandV, boolean adjustBudget) {
		super(cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
	}
	
	public JordanHillClimbing(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super( c1, c2,c3, budgetL, budgetM, budgetH,  bidMultLow,
			bidMultHigh, multiDay, multiDayDiscretization);
	}

	public JordanHillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization) {
		super(cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		
	}

	
	/**
	    * By default, don't take any initial sales as input. Initial sales will be a default constant.
	    * @param bidLists
	    * @param budgetLists
	    * @param allPredictionsMap
	    * @return
	    */
	@Override
	   protected HashMap<Query,Item> getSolution(ArrayList<IncItem> incItems, double budget, Map<Query, 
				ArrayList<Predictions>> allPredictionsMap, HashMap<Query, ArrayList<Double>> bidLists,
				HashMap<Query, ArrayList<Double>> budgetLists){
	      int windowSize = 59; //TODO: don't hard code this  //HC num
	      int initSales = (int)(_capacity*_capMod.get(_capacity) / ((double) _capWindow));
	      int[] initialSales = new int[windowSize];
	      Arrays.fill(initialSales, initSales);
	      return fillKnapsackHillClimbing(bidLists, budgetLists, allPredictionsMap, initialSales, accountForProbing);
	   }

	
	   
		@Override
		public AbstractAgent getCopy() {
			JordanHillClimbing copy = new JordanHillClimbing(accountForProbing,changeWandV,adjustBudget);
			return copy;
		}

}
