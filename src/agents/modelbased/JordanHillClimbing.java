/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
	boolean adjustBudget = true;
	boolean goOver = true;
	
	public JordanHillClimbing() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public JordanHillClimbing(int accountForProbing, boolean changeWandV, boolean adjustBudget, boolean goOver) {
		super();
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
		this.goOver = goOver;
	}

	public JordanHillClimbing(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization, 
			int accountForProbing, boolean changeWandV, boolean adjustBudget, boolean goOver) {
		super( c1, c2,c3, budgetL,budgetM, budgetH, bidMultLow, bidMultHigh, 
				multiDay, multiDayDiscretization);
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
		this.goOver = goOver;
	}
	
	public JordanHillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,	int multiDayDiscretization,
			int accountForProbing, boolean changeWandV, boolean adjustBudget, boolean goOver) {
		super(cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		this.accountForProbing = accountForProbing;
		this.changeWandV = changeWandV;
		this.adjustBudget = adjustBudget;
		this.goOver = goOver;
	}
	
	public JordanHillClimbing(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super( c1, c2,c3, budgetL, budgetM, budgetH,  bidMultLow,
			bidMultHigh, multiDay, multiDayDiscretization);
	}
	
	public JordanHillClimbing(int daysToLook, double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(daysToLook, c1, c2,c3, budgetL, budgetM, budgetH,  bidMultLow,
			bidMultHigh, multiDay, multiDayDiscretization);
		System.out.println("HERE");
	}

	public JordanHillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization) {
		super(cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		
	}

	public JordanHillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization, String filename) {
		super(cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization, filename);
		
	}
	
	public JordanHillClimbing(int daysToLook, long timeConstraint, PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization, String filename) {
		super(daysToLook, timeConstraint, cljSim, agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization, filename);
		
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
	      HashMap<Query,Item> solution = fillKnapsackHillClimbing(bidLists, budgetLists, allPredictionsMap, initialSales, accountForProbing);

			Set<Query> queries= solution.keySet();
			//for(Query qry : queries){
				//System.out.println("Query: "+qry.toString()+" bid: "+solution.get(qry).b()+" budget: "+solution.get(qry).budget());
				
			//}
	      return solution;
	   }

	protected Item makeNewItem(IncItem ii, double budget, double lowW,
			double newValue, double newBudget, boolean changeWV, boolean changeB) {
		Item itemHigh = ii.itemHigh();
		if(changeWandV){
			if (adjustBudget){
				
				//System.out.println("Jordan: 1; budget before: "+itemHigh.budget()+" budget after:"+ newBudget);
			return new Item(ii.item().q(),budget+lowW,newValue,itemHigh.b(),
				newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
			}else{
				//System.out.println("2");
				return new Item(ii.item().q(),budget+lowW,newValue,itemHigh.b(),
						itemHigh.budget(),itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
			}
		}else{
			if(adjustBudget){
				//System.out.println("3; budget before: "+itemHigh.budget()+" budget after:"+ newBudget);
				return new Item(ii.item().q(),ii.w(),ii.v(),itemHigh.b(),newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
			}else{
				//System.out.println("4");
				return new Item(ii.item().q(),ii.w(),ii.v(),itemHigh.b(),itemHigh.budget(),itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
			}
		}
		
	}
	   
		@Override
		public AbstractAgent getCopy() {
			JordanHillClimbing copy = new JordanHillClimbing(accountForProbing,changeWandV,adjustBudget, goOver);
			return copy;
		}

		@Override
		protected boolean getGoOver() {
			
			return goOver;
		}
		

}
