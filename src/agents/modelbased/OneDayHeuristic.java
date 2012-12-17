/**
 * 
 */
package agents.modelbased;

import java.util.ArrayList;
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
public class OneDayHeuristic extends MCKP {

	private static boolean FORWARDUPDATING = true;
	private static boolean PRICELINES = false;
	private static boolean UPDATE_WITH_ITEM = false;

	/**
	 * 
	 */
	public OneDayHeuristic() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public OneDayHeuristic(boolean ForwardUpdating, boolean Pricelines, boolean updateWithItem) {
		super();
		FORWARDUPDATING = true;
		PRICELINES = false;
		UPDATE_WITH_ITEM = false;

	}
	

	public OneDayHeuristic(double c1, double c2, double c3, double budgetL,
			double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		super(c1, c2,c3, budgetL,
				budgetM, budgetH,  bidMultLow,
				bidMultHigh, multiDay, multiDayDiscretization);
	}

	public OneDayHeuristic(PersistentHashMap cljSim, String agentToReplace,
			double c1, double c2, double c3, MultiDay multiDay,
			int multiDayDiscretization) {
		super(cljSim,agentToReplace, c1, c2, c3, multiDay, multiDayDiscretization);
	}

	/* (non-Javadoc)
	 * @see agents.modelbased.AbstractSolver#getSolution()
	 */
	@Override
	public HashMap<Query, Item> getSolution(ArrayList<IncItem> incItems, double budget, Map<Query, 
			ArrayList<Predictions>> allPredictionsMap, HashMap<Query, ArrayList<Double>> bidLists,
			HashMap<Query, ArrayList<Double>> budgetLists){
			      HashMap<Query,Item> solution = new HashMap<Query, Item>();
			      int expectedConvs = 0;//HC num
			      double[] lastSolVal = null;
			      for(int i = 0; i < incItems.size(); i++) {
			         IncItem ii = incItems.get(i);
			         double itemWeight = ii.w();
			         //			double itemValue = ii.v();
			         if(budget >= (expectedConvs + itemWeight)) {
			            solution.put(ii.item().q(), ii.item());
			            expectedConvs += itemWeight;
			         }
			         else {
			            double[] currSolVal;
			            if(lastSolVal != null) {
			               currSolVal = lastSolVal;
			            }
			            else {
			               currSolVal = solutionValueMultiDay(solution, budget, allPredictionsMap, 15);//HC num
			            }

			            HashMap<Query, Item> solutionCopy = (HashMap<Query, Item>)solution.clone();
			            solutionCopy.put(ii.item().q(), ii.item());
			            double[] newSolVal = solutionValueMultiDay(solutionCopy, budget, allPredictionsMap, 15);//HC num

			            //				System.out.println("[" + _day +"] CurrSolVal: " + currSolVal[0] + ", NewSolVal: " + newSolVal[0]);

			            if(newSolVal[0] > currSolVal[0]) {
			               solution.put(ii.item().q(), ii.item());
			               expectedConvs += ii.w();
			               lastSolVal = newSolVal;

			               if(i != incItems.size() - 1) {
			                  /*
			                         * Discount the item based on the current penalty level
			                         */
			                  double penalty = getPenalty(budget, newSolVal[1]);

			                  if(FORWARDUPDATING && !PRICELINES) {//Hybrid greedy; doesn't sort but instead updates next item as capacity changes
			                     //Update next item
			                     IncItem nextItem  = incItems.get(i+1);
			                     double v,w;
			                     if(nextItem.itemLow() != null) {
			                        Predictions prediction1 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemLow().idx());
			                        Predictions prediction2 = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
			                        v = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction2.getISRatio())*_salesPrices.get(nextItem.item().q()) - prediction2.getCPC()) -
			                                (prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction1.getISRatio())*_salesPrices.get(nextItem.item().q()) - prediction1.getCPC()));
			                        w = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction2.getISRatio()) -
			                                (prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction1.getISRatio()));
			                     }
			                     else {
			                        Predictions prediction = allPredictionsMap.get(nextItem.item().q()).get(nextItem.itemHigh().idx());
			                        v = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction.getISRatio())*_salesPrices.get(nextItem.item().q()) - prediction.getCPC());
			                        w = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(nextItem.item().q(), penalty,prediction.getISRatio());
			                     }
			                     IncItem newNextItem = new IncItem(w, v, nextItem.itemHigh(), nextItem.itemLow());
			                     incItems.remove(i+1);
			                     incItems.add(i+1, newNextItem);
			                  }
			                  else if(PRICELINES) {//sorts; dynamic greedy mckp
			                     ArrayList<IncItem> updatedItems = new ArrayList<IncItem>();
			                     for(int j = i+1; j < incItems.size(); j++) {
			                        IncItem incItem = incItems.get(j);
			                        Item itemLow = incItem.itemLow();
			                        Item itemHigh = incItem.itemHigh();

			                        double newPenalty;
			                        if(UPDATE_WITH_ITEM) {
			                           HashMap<Query, Item> solutionInnerCopy = (HashMap<Query, Item>)solutionCopy.clone();
			                           solutionInnerCopy.put(incItem.item().q(), incItem.item());
			                           double solWeight = solutionWeight(budget, solutionInnerCopy, allPredictionsMap);
			                           newPenalty = getPenalty(budget, solWeight);
			                        }
			                        else {
			                           newPenalty = penalty;
			                        }

			                        double newWeight,newValue;

			                        if(itemLow != null) {
			                           Predictions prediction1 = allPredictionsMap.get(itemHigh.q()).get(itemLow.idx());
			                           Predictions prediction2 = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
			                           newValue = prediction2.getClickPr()*prediction2.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction2.getISRatio())*_salesPrices.get(itemHigh.q()) - prediction2.getCPC()) -
			                                   (prediction1.getClickPr()*prediction1.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction1.getISRatio())*_salesPrices.get(itemHigh.q()) - prediction1.getCPC())) ;
			                           newWeight = prediction2.getClickPr()*prediction2.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction2.getISRatio()) -
			                                   (prediction1.getClickPr()*prediction1.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction1.getISRatio()));
			                        }
			                        else {
			                           Predictions prediction = allPredictionsMap.get(itemHigh.q()).get(itemHigh.idx());
			                           newValue = prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction.getISRatio())*_salesPrices.get(itemHigh.q()) - prediction.getCPC());
			                           newWeight = prediction.getClickPr()*prediction.getNumImp()*getConversionPrWithPenalty(incItem.item().q(), newPenalty,prediction.getISRatio());
			                        }
			                        IncItem newItem = new IncItem(newWeight,newValue,itemHigh,itemLow);
			                        updatedItems.add(newItem);
			                     }

			                     Collections.sort(updatedItems);

			                     while(incItems.size() > i+1) {
			                        incItems.remove(incItems.size()-1);
			                     }
			                     for(IncItem priceLineItem : updatedItems) {
			                        incItems.add(incItems.size(),priceLineItem);
			                     }
			                  }
			               }
			            }
			            else {
			               break;
			            }
			         }
			      }
			      return solution;
			   }
	 private double[] solutionValueMultiDay(HashMap<Query, Item> solution, double remainingCap, Map<Query, ArrayList<Predictions>> allPredictionsMap, int numDays) {

	      double totalWeight;
	      double weightMult;
//	      if(DAY_SIM_WEIGHT_OPT) {
//	         double[] results = simulateDay(daySim,mkBundleFromKnapsack(solution));
//	         totalWeight = results[3];
//	         weightMult = totalWeight / solutionWeight(remainingCap, solution, allPredictionsMap);
//	      }
//	      else {
	      totalWeight = solutionWeight(remainingCap, solution, allPredictionsMap);
	      weightMult = 1.0;//HC num
//	      }

	      double penalty = getPenalty(remainingCap, totalWeight);

	      double totalValue = 0;
	      for(Query q : _querySpace) {
	         if(solution.containsKey(q)) {
	            Item item = solution.get(q);
	            Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
	            totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penalty,prediction.getISRatio())*_salesPrices.get(item.q()) - prediction.getCPC());
	         }
	      }

	      double daysLookahead = Math.max(0, Math.min(numDays, 58 - _day));//HC num
	      if(daysLookahead > 0 && totalWeight > 0) {
	         ArrayList<Integer> soldArray;
	         if(!hasPerfectModels()) {
	            ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
	            soldArray = new ArrayList<Integer>(soldArrayTMP);

	            Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
	            if(expectedConvsYesterday == null) {
	               expectedConvsYesterday = 0;
	               int counter2 = 0;
	               for(int j = 0; j < 5 && j < soldArray.size(); j++) {
	                  expectedConvsYesterday += soldArray.get(soldArray.size()-1-j);
	                  counter2++;
	               }
	               expectedConvsYesterday = (int) (expectedConvsYesterday / (double) counter2);
	            }
	            soldArray.add(expectedConvsYesterday);
	         }
	         else {
	            soldArray = new ArrayList<Integer>(_perfectStartSales.length);

	            if(_perfectStartSales.length < (_capWindow-1)) {
	               for(int i = 0; i < (_capWindow - 1 - _perfectStartSales.length); i++) {
	                  soldArray.add((int)(_capacity / ((double) _capWindow)));
	               }
	            }

	            for(Integer numConvs : _perfectStartSales) {
	               soldArray.add(numConvs);
	            }
	         }
	         soldArray.add((int) totalWeight);

	         for(int i = 0; i < daysLookahead; i++) {
	            //Compute amount that can be sold on this day.
	            //Start budget at capacity limit, and subtract off sales
	            //from each of the past days within the window excluding today (4 days w/ current game settings)
	            double expectedBudget = _capacity*_capMod.get(_capacity);
	            for(int j = 0; j < _capWindow-1; j++) {
	               int idx = soldArray.size() - 1 - j;
	               double defaultSales = _capacity/(double)_capWindow; //TODO: The other alternative is to pad soldArray. This might be cleaner.
	               if (idx<0) expectedBudget -= defaultSales;
	               else expectedBudget -= soldArray.get(idx);
	            }

	            double numSales = solutionWeight(expectedBudget, solution, allPredictionsMap)*weightMult;
	            soldArray.add((int) numSales);

	            double penaltyNew = getPenalty(expectedBudget, numSales);
	            for(Query q : _querySpace) {
	               if(solution.containsKey(q)) {
	                  Item item = solution.get(q);
	                  Predictions prediction = allPredictionsMap.get(item.q()).get(item.idx());
	                  totalValue += prediction.getClickPr()*prediction.getNumImp()*(getConversionPrWithPenalty(q, penaltyNew,prediction.getISRatio())*_salesPrices.get(item.q()) - prediction.getCPC());
	               }
	            }
	         }
	      }
	      double[] output = new double[2];
	      output[0] = totalValue;
	      output[1] = totalWeight;
	      return output;
	   }

	@Override
	public AbstractAgent getCopy() {
		OneDayHeuristic copy = new OneDayHeuristic(FORWARDUPDATING,PRICELINES,UPDATE_WITH_ITEM);
		return copy;
	}
	
}
