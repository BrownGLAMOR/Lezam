/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import clojure.lang.PersistentHashMap;

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
public class DP extends MCKP {

	/**
	 * 
	 */
	int _daysToLook = 59;
	public DP() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DP(double c1, double c2, double c3, double budgetL, double budgetM,
			double budgetH, double bidMultLow, double bidMultHigh,
			MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL,
				budgetM, budgetH,  bidMultLow,
				bidMultHigh, multiDay, multiDayDiscretization);
	}

	public DP(int daysToLook, PersistentHashMap cljSim, String agentToReplace, double c1,
			double c2, double c3, MultiDay multiDay, int multiDayDiscretization) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
		_daysToLook =daysToLook;
	}

	/* (non-Javadoc)
	 * @see agents.modelbased.AbstractSolver#getSolution()
	 */
	@Override
	protected HashMap<Query,Item> getSolution(ArrayList<IncItem> incItems, double budget, Map<Query, 
			ArrayList<Predictions>> allPredictionsMap, HashMap<Query, ArrayList<Double>> bidLists,
			HashMap<Query, ArrayList<Double>> budgetLists){
	      System.out.println("Running DP.");

	      //CONFIG FOR DP
	      int PLANNING_HORIZON = _daysToLook;//HC num
	      int capacityWindow = _capWindow-1; //Excluding the current day
	      int totalCapacityMax = 2*_capacity; //The most capacity we'll ever consider using (across all days)//HC num
	      int dailyCapacityUsedMin = 0;//HC num
	      int dailyCapacityUsedMax = totalCapacityMax; //The most capacity we'll ever consider using on a single day
	      int dailyCapacityUsedStep = 15;//HC num
	      int dayStart = (int) _day;
	      int dayEnd = Math.min(58, dayStart + PLANNING_HORIZON); //FIXME: _numDays starts out as 0???  //HC num


	      int[] preDaySales = getPreDaySales();


	      //-------------------------
	      //Create list of single-day profits for any given (startCapacity, salesForToday) pairs
	      //-------------------------
	      long startTimerMap = System.currentTimeMillis();

//		   HashMap<Integer,HashMap<Integer, Double>> profitMemoizeMap = getHashedProfits(capacityWindow,
//				   totalCapacityMax,dailyCapacityUsedMin,dailyCapacityUsedMax,dailyCapacityUsedStep,
//				   bidLists,budgetLists,allPredictionsMap);

	      HashMap<Integer,HashMap<Integer, Double>> profitMemoizeMap = getSpeedyHashedProfits(capacityWindow,
	                                                                                          totalCapacityMax,dailyCapacityUsedMin,dailyCapacityUsedMax,dailyCapacityUsedStep,
	                                                                                          bidLists,budgetLists,allPredictionsMap);


	      long endTimerMap = System.currentTimeMillis();


//		   //--------------------
//		   //Print out daily profit data
//		   StringBuffer sb = new StringBuffer();
//		   sb.append("\t");
//			for (int salesForToday=0; salesForToday<=dailyCapacityUsedMax && salesForToday<=totalCapacityMax; salesForToday+=dailyCapacityUsedStep) {
//				sb.append(salesForToday + "\t");
//			}
//			sb.append("\n");
//		   for (int dayStartSales=dailyCapacityUsedMin; dayStartSales<= maxStartingSales; dayStartSales+=dailyCapacityUsedStep) {
//			   sb.append(dayStartSales + "\t");
//			   for (int salesForToday=0; salesForToday<=dailyCapacityUsedMax && dayStartSales+salesForToday<=totalCapacityMax; salesForToday+=dailyCapacityUsedStep) {
////			   for (int salesForToday=0; salesForToday<=dailyCapacityUsedMax; salesForToday+=dailyCapacityUsedStep) {
//			   		double profit = profitMemoizeMap.get(dayStartSales).get(salesForToday);
//					sb.append(profit + "\t");
//				}
//				sb.append("\n");
//		   }
//		   System.out.println("Profit cache for day " + _day + ":\n" + sb);
//		   //--------------------

//		   //-------------------
//		   //Save daily profit data to log
//		   StringBuffer sb1 = new StringBuffer();
//		   for (int dayStartSales=dailyCapacityUsedMin; dayStartSales<= maxStartingSales; dayStartSales+=dailyCapacityUsedStep) {
//			   for (int salesForToday=0; salesForToday<=dailyCapacityUsedMax && dayStartSales+salesForToday<=totalCapacityMax; salesForToday+=dailyCapacityUsedStep) {
//			   		double profit = profitMemoizeMap.get(dayStartSales).get(salesForToday);
//			   		sb1.append("MODEL\t" + _day + "\t" + dayStartSales + "\t" + salesForToday + "\t" + profit + "\n");
//			   }
//		   }
//		   System.out.println(sb1);
//		   //------------------


	      //-------------------------
	      //Create the multiday DP, and solve to get the number of conversions we want for the current day
	      //-------------------------
	      long startTimerDP = System.currentTimeMillis();
	      DPMultiday dp = new DPMultiday(capacityWindow, totalCapacityMax, dailyCapacityUsedMin, dailyCapacityUsedMax, dailyCapacityUsedStep, dayStart, dayEnd, preDaySales.clone(), _capacity);
	      dp.profitGivenCapacities = profitMemoizeMap;
	      double salesForToday = dp.solve(); //TODO: Call the DP to get this value
	      long endTimerDP = System.currentTimeMillis();


	      System.out.println("MapTime=" + (endTimerMap-startTimerMap)/1000.0 + ", DPTime=" + (endTimerDP-startTimerDP)/1000.0 ) ;//HC num


	      //-------------------------
	      //Get the MCKP solution for this number of targeted conversions
	      //-------------------------

	      //Get today's remaining capacity
	      int startRemCap = (int)(_capacity*_capMod.get(_capacity));
	      for(int i = 0; i < preDaySales.length; i++) {
	         startRemCap -= preDaySales[i];
	      }

	      //Get the actual MCKP solution, given today's remaining capacity
	      return fillKnapsack(getIncItemsForOverCapLevel(startRemCap,salesForToday,bidLists,budgetLists,allPredictionsMap),salesForToday);

	   }

	@Override
	public AbstractAgent getCopy() {
		DP copy = new DP();
		return copy;
	}

	@Override
	protected Item makeNewItem(IncItem ii, double budget, double lowW,
			double newValue, double newBudget, boolean changeWandV, boolean changeBudget) {
		Item itemHigh = ii.itemHigh();
		if(changeWandV && !changeBudget){
			return new Item(ii.item().q(),budget+lowW,newValue,itemHigh.b(),
				itemHigh.budget(),itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
		}else if (changeWandV && changeBudget){
			
			return new Item(ii.item().q(),budget+lowW,newValue,itemHigh.b(),newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx());
		}else{
			return new Item(ii.item().q(),ii.w(),ii.v(),itemHigh.b(),newBudget,itemHigh.targ(),itemHigh.isID(),itemHigh.idx());

		}
		
	}

	@Override
	protected boolean getGoOver() {
		// TODO Auto-generated method stub
		return true;
	}
}
