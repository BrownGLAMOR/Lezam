/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import clojure.lang.PersistentHashMap;

import models.unitssold.BasicUnitsSoldModel;

import agents.AbstractAgent;
import agents.AbstractAgent.Predictions;
import agents.modelbased.MCKP.MultiDay;
import agents.modelbased.mckputil.IncItem;
import agents.modelbased.mckputil.Item;
import edu.umich.eecs.tac.props.Query;

/**
 * @author betsy
 *
 */
public class DPHill extends HillClimbing {

	/**
	 * 
	 */
	public DPHill() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DPHill(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL,
				budgetM, budgetH,  bidMultLow,
				bidMultHigh, multiDay, multiDayDiscretization);
	}

	public DPHill(PersistentHashMap cljSim, String agentToReplace, double c1,
			double c2, double c3, MultiDay multiDay, int multiDayDiscretization) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
	}

	/* (non-Javadoc)
	 * @see agents.modelbased.AbstractSolver#getSolution()
	 */
	@Override
	protected HashMap<Query,Item> getSolution(ArrayList<IncItem> incItems, double budget, Map<Query, 
			ArrayList<Predictions>> allPredictionsMap, HashMap<Query, ArrayList<Double>> bidLists,
			HashMap<Query, ArrayList<Double>> budgetLists){

		      //-------------------------
		      //CONFIG FOR DP
		      //-------------------------
		      int PLANNING_HORIZON = 59; //HC num
		      int capacityWindow = _capWindow-1; //Excluding the current day
		      int totalCapacityMax = 2*_capacity; //The most capacity we'll ever consider using (across all days)
		      int dailyCapacityUsedMin = 0;//HC num
		      int dailyCapacityUsedMax = totalCapacityMax; //The most capacity we'll ever consider using on a single day
		      int dailyCapacityUsedStep = 50;//HC num
		      int dayStart = (int) _day;
		      int dayEnd = Math.min(59, dayStart + PLANNING_HORIZON); //FIXME: _numDays starts out as 0???   //HC num

		      //-------------------------
		      //Get Pre-day sales
		      //-------------------------
		      int[] preDaySales = getPreDaySales();


		      //-------------------------
		      //Create list of single-day profits for any given (startCapacity, salesForToday) pairs
		      //-------------------------
		      HashMap<Integer,HashMap<Integer, Double>> profitMemoizeMap = getSpeedyHashedProfits(capacityWindow,
		                                                                                          totalCapacityMax,dailyCapacityUsedMin,dailyCapacityUsedMax,dailyCapacityUsedStep,
		                                                                                          bidLists,budgetLists,allPredictionsMap);

		      //-------------------------
		      //Create the multiday DP, and solve to get the number of conversions we want for the current day
		      //-------------------------
		      DPMultiday dp = new DPMultiday(capacityWindow, totalCapacityMax, dailyCapacityUsedMin, dailyCapacityUsedMax, dailyCapacityUsedStep, dayStart, dayEnd, preDaySales.clone(), _capacity);
		      dp.profitGivenCapacities = profitMemoizeMap;
		      int[] salesPerDay = dp.solveAllDays();

		      System.out.println("DP result for day " + _day + " : " + Arrays.toString(salesPerDay));
		      return fillKnapsackHillClimbing(bidLists, budgetLists, allPredictionsMap, salesPerDay, 0);
		   }

	@Override
	public AbstractAgent getCopy() {
		DPHill copy = new DPHill();
		return copy;
	}
	   
	   
	   
	   
	   
	  

}

