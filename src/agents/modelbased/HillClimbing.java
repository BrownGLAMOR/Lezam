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
import agents.modelbased.MCKP.MultiDay;
import agents.modelbased.mckputil.Item;
import edu.umich.eecs.tac.props.Query;



public abstract class HillClimbing extends MCKP {

	/**
	 * 
	 */

	int daysToLook;

	public HillClimbing() {
		super();
		// TODO Auto-generated constructor stub
	}


	public HillClimbing(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL,
				budgetM, budgetH,  bidMultLow,
				bidMultHigh, multiDay, multiDayDiscretization);
	}

	public HillClimbing(int daysToLook, double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL,
				budgetM, budgetH,  bidMultLow,
				bidMultHigh, multiDay, multiDayDiscretization);
		this.daysToLook = daysToLook;
		System.out.println("SETTING DTL");
	}


	public HillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
	}

	public HillClimbing(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization, String filename) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization, filename);
	}

	public HillClimbing(int daysToLook,long timeConstraint, PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization, String filename) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization, filename);
		this.daysToLook = daysToLook;
		this._timeConstraint = timeConstraint;
		System.out.println("SETTING DTL");
	}

	public HashMap<Query,Item> fillKnapsackHillClimbing(HashMap<Query, ArrayList<Double>> bidLists, 
			HashMap<Query, ArrayList<Double>> budgetLists, Map<Query, ArrayList<Predictions>> allPredictionsMap, 
			int[] initialSales, int accountForProbing){

		int[] preDaySales = new int[_capWindow-1];
		if(!hasPerfectModels()) {
			ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP);

			Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			soldArray.add(expectedConvsYesterday);

			for(int i = 0; i < (_capWindow-1); i++) {
				int idx = soldArray.size()-1-i;//HC num
				if(idx >= 0) {
					preDaySales[_capWindow-2-i] = soldArray.get(idx);//HC num
				}
				else {
					preDaySales[_capWindow-2-i] = (int)(_capacity / ((double) _capWindow));//HC num
				}
			}
		}
		else {
			for(int i = 0; i < (_capWindow-1); i++) {
				int idx = _perfectStartSales.length-1-i;
				if(idx >= 0) {
					preDaySales[_capWindow-2-i] = _perfectStartSales[idx];//HC num
				}
				else {
					preDaySales[_capWindow-2-i] = (int)(_capacity / ((double) _capWindow));//HC num
				}
			}
		}
		//	      System.out.println("day " + _day + ": " + "preDaySales=" + Arrays.toString(preDaySales));

		int startRemCap = (int)(_capacity*_capMod.get(_capacity));
		for (int preDaySale : preDaySales) {
			startRemCap -= preDaySale;
		}

		System.out.println("Jordan's remaining capacity:"+startRemCap);//Debug

		int daysAhead = Math.max(0,58-(int)_day)+1;//HC num
		int daysRemain = Math.max(0,58-(int)_day)+1;
		daysAhead = Math.min(daysRemain, daysToLook);

		System.out.println("DA: "+daysAhead+" DR: "+daysRemain+" DTL: "+daysToLook);
		int capacityIncrement = 10; //_multiDayDiscretization; //10; //50; //10;
		int[] salesOnDay = new int[daysAhead];
		for(int i = 0; i < salesOnDay.length; i++) {
			salesOnDay[i] = initialSales[i];
		}



		System.out.println("Begin Jordan's changes (day, increment)");
		Map<Integer,Map<Integer, Double>> profitMemoizeMap;
		long time = 0;
		long timeNow = 0;
		long timeStart = System.currentTimeMillis();

		if(!THREADING) {
			profitMemoizeMap = new HashMap<Integer, Map<Integer, Double>>(daysAhead);
			double currProfit;
			double bestProfit = findProfitForDays(preDaySales,salesOnDay,bidLists,budgetLists,allPredictionsMap,profitMemoizeMap);
			//COMMENTING OUT HILLCLIMBING
			int count = 0;
			do {
				currProfit = bestProfit;
				int bestIdx = -1;
				int bestIncrement = 0;
				for(int i = 0; i < salesOnDay.length; i++) {
					for(int j = 0; j < 2; j++) {
						if(!(j == 1 && salesOnDay[i] < capacityIncrement)) { //capacity cannot be negative
							int increment = capacityIncrement * (j == 0 ? 1 : -1);//HC num
							salesOnDay[i] += increment;

							double profit = findProfitForDays(preDaySales,salesOnDay,bidLists,budgetLists,allPredictionsMap,profitMemoizeMap);
							if(profit > bestProfit) {
								bestProfit = profit;
								bestIdx = i;
								bestIncrement = increment;
							}

							salesOnDay[i] -= increment;
						}
					}
				}

				if(bestIdx > -1) {
					salesOnDay[bestIdx] += bestIncrement;
					System.out.print(bestIdx+","+bestIncrement+" ");
				}

				//			if(bestProfit <= currProfit){
				//				count+=1;
				//				capacityIncrement+=20;
				//			}
				timeNow = System.currentTimeMillis();
				time = timeNow - timeStart;
			}while(bestProfit > currProfit && time<_timeConstraint);
			//while(bestProfit > currProfit && count<20);

		}
		else {
			ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
			profitMemoizeMap = new ConcurrentHashMap<Integer, Map<Integer, Double>>(daysAhead);
			double currProfit;
			double bestProfit = findProfitForDays(preDaySales,salesOnDay,bidLists,budgetLists,allPredictionsMap,profitMemoizeMap);
			do {
				currProfit = bestProfit;

				ArrayList<Future<HillClimbingResult>> results = new ArrayList<Future<HillClimbingResult>>();
				for(int i = 0; i < salesOnDay.length; i++) {
					HillClimbingCreator hcc = new HillClimbingCreator(i,capacityIncrement,preDaySales,salesOnDay,bidLists,budgetLists,allPredictionsMap,profitMemoizeMap);
					Future<HillClimbingResult> result = executor.submit(hcc);
					results.add(result);
				}

				int bestIdx = -1;//HC num
				int bestIncrement = 0;//HC num
				for(Future<HillClimbingResult> result : results) {
					try {
						HillClimbingResult hcr = result.get();
						if(hcr.getProfit() > bestProfit) {
							bestProfit = hcr.getProfit();
							bestIdx = hcr.getIdx();
							bestIncrement = hcr.getInc();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						throw new RuntimeException();
					} catch (ExecutionException e) {
						e.printStackTrace();
						throw new RuntimeException();
					}
				}

				if(bestIdx > -1) {
					salesOnDay[bestIdx] += bestIncrement;
				}
			}
			while(bestProfit > currProfit);

			executor.shutdown(); //execute all threads
		}
		System.out.println();
		// System.out.println("End Jordan's changes");
		System.out.println("Choosing plan for day " + _day + " : " + Arrays.toString(salesOnDay));
		//	      

		//Testing purposes; delete later
		//	      salesOnDay[salesOnDay.length-1] *= 2;
		//	      if(salesOnDay.length>1){
		//	    	  salesOnDay[salesOnDay.length-2] *= 2;
		//	      }

		//	      System.out.println("Jordan's Array: ");
		//	      for (int i = 0; i<salesOnDay.length; i++){
		//	    	  System.out.print(salesOnDay[i]+" ");//debug
		//	      }
		//	      System.out.println();



		int salesToday = salesOnDay[0];
		//HC num
		targetArray[(int) _day]= salesToday;
		int knapsackSales = salesToday-accountForProbing;




		//     System.out.println(profitMemoizeMap.toString());//Debug
		int sum = 0;
		for(int d=0;d<2;d++){
			if(d<salesOnDay.length){
				sum+=salesOnDay[d];
			}
		}
		System.out.println("Jordan's settled on capacity: "+salesToday+ " start remaining Cap: "+startRemCap+" sum:" +sum);//debug

		return fillKnapsack(getIncItemsForOverCapLevel(startRemCap,salesOnDay[0],bidLists,budgetLists,allPredictionsMap),knapsackSales);
		//return fillKnapsack(getIncItemsForOverCapLevelMultiDay(2,startRemCap,preDaySales,salesOnDay,bidLists,budgetLists,allPredictionsMap),sum);
	}

	public class HillClimbingCreator implements Callable<HillClimbingResult> {

		int _idx;
		int _capInc;
		int[] _preDaySales;
		int[] _salesOnDay;
		HashMap<Query,ArrayList<Double>> _bidLists;
		HashMap<Query,ArrayList<Double>> _budgetLists;
		Map<Query,ArrayList<Predictions>> _allPredictionsMap;
		Map<Integer,Map<Integer, Double>> _profitMemoizeMap;

		public HillClimbingCreator(int idx, int capacityIncrement, int[] preDaySales, int[] salesOnDay, HashMap<Query,ArrayList<Double>> bidLists, HashMap<Query,ArrayList<Double>> budgetLists, Map<Query,ArrayList<Predictions>> allPredictionsMap, Map<Integer,Map<Integer, Double>> profitMemoizeMap) {
			_idx = idx;
			_capInc = capacityIncrement;
			_preDaySales = preDaySales;
			_salesOnDay = salesOnDay.clone();
			_bidLists = bidLists;
			_budgetLists = budgetLists;
			_allPredictionsMap = allPredictionsMap;
			_profitMemoizeMap = profitMemoizeMap;
		}

		public HillClimbingResult call() throws Exception {
			double bestProfit = -Double.MAX_VALUE;
			int bestInc = 0;
			for(int j = 0; j < 2; j++) {
				if(!(j == 1 && _salesOnDay[_idx] < _capInc)) { //capacity cannot be negative
					int increment = _capInc * (j == 0 ? 1 : -1); //HC num
					_salesOnDay[_idx] += increment;

					double profit = findProfitForDays(_preDaySales,_salesOnDay,_bidLists,_budgetLists,_allPredictionsMap,_profitMemoizeMap);
					if(profit > bestProfit) {
						bestProfit = profit;
						bestInc = increment;
					}

					_salesOnDay[_idx] -= increment;
				}
			}
			return new HillClimbingResult(_idx,bestInc,bestProfit);
		}
	}

	public class HillClimbingResult {

		int _idx;
		int _increment;
		double _profit;

		public HillClimbingResult(int idx, int increment, double profit) {
			_idx = idx;
			_increment = increment;
			_profit = profit;
		}

		public int getIdx() {
			return _idx;
		}

		public int getInc() {
			return _increment;
		}

		public double getProfit() {
			return _profit;
		}

	}

	private double findProfitForDays(int[] preDaySales, int[] salesOnDay, HashMap<Query,ArrayList<Double>> bidLists, HashMap<Query,ArrayList<Double>> budgetLists, Map<Query,ArrayList<Predictions>> allPredictionsMap, Map<Integer,Map<Integer, Double>> profitMemoizeMap) {
		double totalProfit = 0.0;
		for(int i = 0; i < salesOnDay.length; i++) {
			int dayStartSales = (int)(_capacity*_capMod.get(_capacity));
			for(int j = 1; j <= (_capWindow-1); j++) {
				int idx = i-j;
				if(idx >= 0) {
					dayStartSales -= salesOnDay[idx];
				}
				else {
					dayStartSales -= preDaySales[preDaySales.length+idx];
				}
			}

			double profit;
			if(profitMemoizeMap.get(dayStartSales) != null &&
					profitMemoizeMap.get(dayStartSales).get(salesOnDay[i]) != null) {
				profit = profitMemoizeMap.get(dayStartSales).get(salesOnDay[i]);
			}
			else {
				HashMap<Query, Item> solution = fillKnapsack(getIncItemsForOverCapLevel(dayStartSales,salesOnDay[i],bidLists,budgetLists,allPredictionsMap),salesOnDay[i]);

				int sum = 0;
				for (int k=0;k<2;k++){
					if(i+k<salesOnDay.length){
						sum+=salesOnDay[i+k];
					}
				}
				//HashMap<Query, Item> solution = fillKnapsack(getIncItemsForOverCapLevelMultiDay(2,dayStartSales,preDaySales,salesOnDay, bidLists,budgetLists,allPredictionsMap),sum);


				profit = 0.0;
				for(Query q : solution.keySet()) {
					profit += solution.get(q).v();
				}

				if(profitMemoizeMap.get(dayStartSales) == null) {
					Map<Integer,Double> profitMap;
					if(!THREADING) {
						profitMap = new HashMap<Integer, Double>(salesOnDay.length);
					}
					else {
						profitMap = new ConcurrentHashMap<Integer, Double>(salesOnDay.length);
					}
					profitMap.put(salesOnDay[i],profit);
					profitMemoizeMap.put(dayStartSales,profitMap);
				}
				else {
					profitMemoizeMap.get(dayStartSales).put(salesOnDay[i],profit);
				}
			}

			totalProfit += profit;
		}
		return totalProfit;
	}



}
