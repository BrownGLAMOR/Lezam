package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.forecast.AbstractImpressionForecaster;
import models.queryanalyzer.forecast.EMAImpressionForecaster;
import models.queryanalyzer.riep.ImpressionAndRankEstimator;
import models.queryanalyzer.riep.LDSImpressionAndRankEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorSample;

import java.util.*;


public class CarletonQueryAnalyzer extends AbstractQueryAnalyzer {

   private HashMap<Query, ArrayList<IEResult>> _allResults;
   private HashMap<Query, ArrayList<int[]>> _allAgentIDs;
   private HashMap<Query, ArrayList<String[]>> _allAgentNames;
   private HashMap<Query, ArrayList<int[][]>> _allImpRanges;
   private AbstractImpressionForecaster _impressionForecaster;

   private Set<Query> _querySpace;
   private HashMap<String, Integer> _advToIdx;
   private ArrayList<String> _advertisers;
   private String _ourAdvertiser;
   public final static int NUM_SLOTS = 5;
   private boolean REPORT_FULLPOS_FORSELF = true;
   private boolean _isSampled;
   private boolean usePriors = true;

   /**
    * For each query/agent, this gives the sequence of squashed bid positions that the agent was in.
    * Note: Any handling of default agent positions should not be included here. 
    */
   private HashMap<Query, HashMap<String, ArrayList<Integer>>> _allAgentPositionsPREADJUST; 
   private HashMap<Query, HashMap<String, ArrayList<Integer>>> _allAgentImpressionsPREADJUST;
      
   
   public CarletonQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser, boolean selfAvgPosFlag, boolean isSampled) {
      _querySpace = querySpace;

      REPORT_FULLPOS_FORSELF = selfAvgPosFlag;
      _isSampled = isSampled;

      int ourIdx = -1;
      for(int i = 0; i < advertisers.size(); i++) {
         if(ourAdvertiser.equals(advertisers.get(i))) {
            ourIdx = i;
            break;
         }
      }

      if(ourIdx == -1) {
         System.out.println("PASSED QUERY ANALYZER BAD AGENT ASSUMING IDX 0");
         ourIdx = 0;
      }

      String[] agents = new String[advertisers.size()];
      for(int i = 0; i < agents.length; i++) {
         agents[i] = advertisers.get(i);
      }

      _impressionForecaster = new EMAImpressionForecaster(.1, querySpace, agents, ourIdx);

      _allResults = new HashMap<Query, ArrayList<IEResult>>();
      _allImpRanges = new HashMap<Query, ArrayList<int[][]>>();
      _allAgentIDs = new HashMap<Query, ArrayList<int[]>>();
      _allAgentNames = new HashMap<Query, ArrayList<String[]>>();
      for (Query q : _querySpace) {
         ArrayList<IEResult> resultsList = new ArrayList<IEResult>();
         _allResults.put(q, resultsList);

         ArrayList<int[][]> impRanges = new ArrayList<int[][]>();
         _allImpRanges.put(q, impRanges);

         ArrayList<int[]> agentIDs = new ArrayList<int[]>();
         _allAgentIDs.put(q, agentIDs);

         ArrayList<String[]> agentNames = new ArrayList<String[]>();
         _allAgentNames.put(q, agentNames);
      }

      _advertisers = advertisers;

      _ourAdvertiser = ourAdvertiser;

      
      _allAgentPositionsPREADJUST = new HashMap<Query, HashMap<String, ArrayList<Integer>>>();
      _allAgentImpressionsPREADJUST = new HashMap<Query, HashMap<String, ArrayList<Integer>>>();
      for (Query q : _querySpace) {
    	  _allAgentPositionsPREADJUST.put(q, new HashMap<String, ArrayList<Integer>>());
    	  _allAgentImpressionsPREADJUST.put(q, new HashMap<String, ArrayList<Integer>>());
    	  for (String adv : _advertisers) {
        	  _allAgentPositionsPREADJUST.get(q).put(adv, new ArrayList<Integer>());
        	  _allAgentImpressionsPREADJUST.get(q).put(adv, new ArrayList<Integer>());
          }
      }
      
      
      
      _advToIdx = new HashMap<String, Integer>();
      for (int i = 0; i < advertisers.size(); i++) {
         _advToIdx.put(advertisers.get(i), i);
      }
   }

   @Override
   public int getTotImps(Query query) {
      IEResult result = _allResults.get(query).get(_allResults.get(query).size()-1);
     
      if(result != null) {
         return result.getSlotImpressions()[0];
      }
      else {
         return -1;
      }
   }

   @Override
   public int[] getImpressionsPrediction(Query q) {
      int[][] waterfallPred = getImpressionRangePrediction(q);
      if(waterfallPred != null) {
         int[] impPred = new int[_advertisers.size()];
         for(int i = 0; i < impPred.length; i++) {
            for(int slot = 0; slot < NUM_SLOTS; slot++) {
               impPred[i] += waterfallPred[i][slot];
            }
         }
         return impPred;
      }
      else {
         return null;
      }
   }


   
   /**
    * Updates the list of daily agent squashed bid rankings (obtained from the IEResult).
    * @param q
    * @return
    */
   private void updateObservedAgentPositions(Query q, int[] orderPredictionsReduced, String[] agentNamesReduced) {
	   //For each advertiser, get the order predictions.
	   //If an advertiser doesn't appear, their order prediction is -1.
	   //Create the array that we'll return.
	   for (int a = 0; a < _advertisers.size(); a++) {
		   String adv = _advertisers.get(a);
		   for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
			   if (adv.equals(agentNamesReduced[aReduced])) {
				   for(int i = 0; i < orderPredictionsReduced.length; i++) {
					   if(orderPredictionsReduced[i] == aReduced) {
						   _allAgentPositionsPREADJUST.get(q).get(adv).add(i);
						   break;
					   }
				   }
				   break;
			   }
		   }
	   }
	   return;
   }
   
   
   
   private void updateObservedAgentImpressions() {
	   for (Query q : _querySpace) {
		   updateObservedAgentImpressions(q);
	   }
   }
   private void updateObservedAgentImpressions(Query q) {
	   int[] impsPerAdvertiser = getImpressionsPrediction(q);
	   if (impsPerAdvertiser == null) return;
	   
	   for (int i=0; i<_advertisers.size(); i++) {
		   String adv = _advertisers.get(i);
		   int imps = impsPerAdvertiser[i];
		   _allAgentImpressionsPREADJUST.get(q).get(adv).add(imps);
	   }
   }
   
   
   public double computeAveragePosition(Query q, String advertiser) {
	   double averagePosition = Double.POSITIVE_INFINITY;

	   ArrayList<Integer> positions = _allAgentPositionsPREADJUST.get(q).get(advertiser);
	   if (positions != null && positions.size() > 0) {
		   double sumPosition = 0;
		   for (Integer position : positions) {
			   sumPosition += position;
		   }
		   averagePosition = sumPosition / positions.size();
	   }
	   return averagePosition;
   }
   
   
   
   @Override
   /**
    * Gives a ranking over all agents.
    * Output is array orderPredictions[rank], where the array index
    * is the ranking (index=0 means top slot), and the value is the 
    * agent index that was predicted to be in that "position".
    */
   public int[] getOrderPrediction(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         //Get most recent impressions predictions (IEResult)
         int[] orderPredictionsReduced = result.getOrder();

         //Get names of agents that appear in the IEResult
         String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

         ArrayList<String> advsLeft = new ArrayList<String>(_advertisers.size());
         for(int i = 0; i < _advertisers.size(); i++) {
            advsLeft.add("adv" + (i + 1));
         }

         //For each advertiser, get the order predictions.
         //If an advertiser doesn't appear, their order prediction is -1.
         //Create the array that we'll return.
         int[] orderPredictions = new int[_advertisers.size()];
         Arrays.fill(orderPredictions, -1);
         for (int a = 0; a < _advertisers.size(); a++) {
            String adv = _advertisers.get(a);
            for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
               if (adv.equals(agentNamesReduced[aReduced])) {
                  for(int i = 0; i < orderPredictionsReduced.length; i++) {
                     if(orderPredictionsReduced[i] == aReduced) {
                        orderPredictions[i] = a;
                        advsLeft.remove("adv"+(a+1));
                        break;
                     }
                  }
                  break;
               }
            }
         }
         
         
         //-----------
         // Fill in those who weren't in the auction with some default order
         //  (default: in order of historical average ranking)
         //----------
         
//         System.out.println("(Before adjusting) order predictions: " + Arrays.toString(orderPredictions));
         
         //FIXME New method
         //Compute historical average positions of remaining advertisers
         ArrayList<Double> avgPositionsOfAdvsLeft = new ArrayList<Double>();
         for (String adv : advsLeft) {
        	 double avgPos = computeAveragePosition(q, adv);
        	 avgPositionsOfAdvsLeft.add(avgPos);
         }
         //System.out.println(" advertisers left: " + advsLeft);
         //System.out.println(" avgPos of advertisers left: " + avgPositionsOfAdvsLeft);
         //Assign these remaining advertisers to remaining slots based on average position.
         for (int a = 0; a < _advertisers.size(); a++) {
             if(orderPredictions[a] == -1) {
            	 double lowestPosition = Double.POSITIVE_INFINITY;
            	 int lowestIdx = 0;
            	 //Get the remaining advertiser with the lowest average position
            	 for (int i=0; i<avgPositionsOfAdvsLeft.size(); i++) {
            		 double avgPos = avgPositionsOfAdvsLeft.get(i);
            		 if (avgPos < lowestPosition) {
            			 lowestPosition = avgPos;
            			 lowestIdx = i;
            		 }
            	 }
            	 String lowestAdvertiser = advsLeft.get(lowestIdx);
            	 orderPredictions[a] = _advToIdx.get(lowestAdvertiser);

            	 advsLeft.remove(lowestIdx);
            	 avgPositionsOfAdvsLeft.remove(lowestIdx);
            	 
             }
          }         
         //System.out.println("order predictions: " + Arrays.toString(orderPredictions));
         
         //OLD METHOD: Fill in those who weren't in the auction in order of agentID
//         for (int a = 0; a < _advertisers.size(); a++) {
//            if(orderPredictions[a] == -1) {
//               String adv = advsLeft.remove(0);
//               orderPredictions[a] = _advToIdx.get(adv);
//            }
//         }

         return orderPredictions;
      }
      else {
         return null;
      }
   }

   @Override
   public int[][] getImpressionRangePrediction(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         int[][] waterfallPredReduced = _allImpRanges.get(q).get(latestResultIdx);

         //Get names of agents that appear in the IEResult
         String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

         ArrayList<String> advsLeft = new ArrayList<String>(_advertisers.size());
         for(int i = 0; i < _advertisers.size(); i++) {
            advsLeft.add("adv"+(i+1));
         }

         //For each advertiser, get the impressions predictions.
         //If an advertiser doesn't appear, their impressions prediction is 0.
         //Create the array that we'll return.
         int[][] waterfallPred = new int[_advertisers.size()][NUM_SLOTS];
         for(int a = 0; a < _advertisers.size(); a++) {
            String adv = _advertisers.get(a);
            for(int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
               if (adv.equals(agentNamesReduced[aReduced])) {
                  waterfallPred[a] = waterfallPredReduced[aReduced];
                  advsLeft.remove(adv);
                  break;
               }
            }
         }
         
         return waterfallPred;
      }
      else {
         return null;
      }
   }

   
   
   
	private static double getMedian(ArrayList<Integer> values)
	{
		if (values == null || values.size() == 0) return Double.NaN;
		
	    Collections.sort(values);
	 
	    if (values.size() % 2 == 1)
		return values.get((values.size()+1)/2-1);
	    else
	    {
		double lower = values.get(values.size()/2-1);
		double upper = values.get(values.size()/2);
	 
		return (lower + upper) / 2.0;
	    }	
	}
   
   
   
   @Override
   public HashMap<String,Boolean> getRankableMap(Query q) {
      int latestResultIdx = _allResults.get(q).size() - 1;
      IEResult result = _allResults.get(q).get(latestResultIdx);
      if(result != null) {
         HashMap<String,Boolean> rankable = new HashMap<String, Boolean>();

         //Get most recent impressions predictions (IEResult)
         int[] orderPredictionsReduced = result.getOrder();

         //Get names of agents that appear in the IEResult
         String[] agentNamesReduced = _allAgentNames.get(q).get(latestResultIdx);

         ArrayList<String> advsLeft = new ArrayList<String>(_advertisers.size());
         for(int i = 0; i < _advertisers.size(); i++) {
            advsLeft.add("adv"+(i+1));
         }

         for (int a = 0; a < _advertisers.size(); a++) {
            String adv = _advertisers.get(a);
            for (int aReduced = 0; aReduced < agentNamesReduced.length; aReduced++) {
               if (adv.equals(agentNamesReduced[aReduced])) {
                  for(int i = 0; i < orderPredictionsReduced.length; i++) {
                     if(orderPredictionsReduced[i] == aReduced) {
                        rankable.put("adv"+(a+1),true);
                        advsLeft.remove("adv"+(a+1));
                        break;
                     }
                  }
                  break;
               }
            }
         }

         for(String adv : advsLeft) {
            rankable.put(adv,false);
         }

         return rankable;
      }
      else {
         return null;
      }
   }

   @Override
   public boolean updateModel(QueryReport queryReport, BidBundle bidBundle, HashMap<Query, Integer> maxImps) {

      List<Map<Query, Integer>> impPredMapList = new ArrayList<Map<Query, Integer>>(_advertisers.size());
      for (int j = 0; j < _advertisers.size(); j++) {
         Map<Query, Integer> impPredMap = new HashMap<Query, Integer>(_querySpace.size());
         impPredMapList.add(impPredMap);
      }

      for (Query q : _querySpace) {
    	 
         /*
          * For now we can only solve the auctions we were in
          */
         if(!Double.isNaN(queryReport.getPosition(q))) {
    	 // if(true) {
            // Load data from the query report (specifically, the average position for each advertiser)
            ArrayList<Double> allAvgPos = new ArrayList<Double>();
            ArrayList<Integer> agentIds = new ArrayList<Integer>();
            ArrayList<String> agentNames = new ArrayList<String>();
            int ourIdx = -1;
            for (int i = 0; i < _advertisers.size(); i++) {
               if (_advertisers.get(i).equals(_ourAdvertiser)) {
                  ourIdx = i;
               }

               double avgPos = queryReport.getPosition(q, "adv" + (i + 1));
               if (!Double.isNaN(avgPos) || i == ourIdx) {
                  agentIds.add(i);
                  agentNames.add(_advertisers.get(i));
                  allAvgPos.add(avgPos);
               }
            }

            // Create arrays with query report data (instead of arrayLists)
            double[] sampledAvgPos = new double[allAvgPos.size()];
            int[] agentIdsArr = new int[agentIds.size()];
            String[] agentNamesArr = new String[agentNames.size()];
            int ourNewIdx = -1;
            for (int i = 0; i < sampledAvgPos.length; i++) {
               sampledAvgPos[i] = allAvgPos.get(i);
               agentIdsArr[i] = agentIds.get(i);
               agentNamesArr[i] = agentNames.get(i);
               if (agentIds.get(i) == ourIdx) {
                  ourNewIdx = i;
               }
            }

            //Add our true average position in
            double[] trueAvgPos = new double[agentIds.size()];
            Arrays.fill(trueAvgPos, -1);
            trueAvgPos[ourNewIdx] = queryReport.getPosition(q);

            //No distinguishing between promoted and unpromoted slots
            int numPromotedSlots = -1;

            //Not considering whether our bid is high enough to be in a promoted slot
            boolean promotionEligibiltyVerified = false;

            //Not considering whether we hit our budget
            boolean hitOurBudget = false;

            //Not using any prior knowledge about agent impressions
            double[] agentImpressionDistributionMean = new double[_advertisers.size()];
            double[] agentImpressionDistributionStdev = new double[_advertisers.size()];
            if(usePriors) {
               for (int i = 0; i < _advertisers.size(); i++) {
                  agentImpressionDistributionMean[i] = (int) _impressionForecaster.getPrediction(i, q);
                  if(agentImpressionDistributionMean[i] > 0) {
                     agentImpressionDistributionStdev[i] = agentImpressionDistributionMean[i] * 0.3;
                  }
                  else {
                     agentImpressionDistributionStdev[i] = -1;
                  }
               }
            }
            else {
               Arrays.fill(agentImpressionDistributionMean, -1);
               Arrays.fill(agentImpressionDistributionStdev, -1);
            }

            double[] reducedImpMean = new double[agentIds.size()];
            double[] reducedImpStdDev = new double[agentIds.size()];
            for(int i = 0; i < agentIds.size(); i++) {
               reducedImpMean[i] = agentImpressionDistributionMean[agentIds.get(i)];
               reducedImpStdDev[i] = agentImpressionDistributionStdev[agentIds.get(i)];
            }

            //We don't know which agentIdx was in the ith position, for any i.
            int[] ordering = new int[agentIds.size()];
            Arrays.fill(ordering, -1);
            //--------------

            boolean padAgents = true;

            
            QAInstanceAll inst = new QAInstanceAll(NUM_SLOTS, numPromotedSlots, allAvgPos.size(),
                                             trueAvgPos, sampledAvgPos, agentIdsArr, ourNewIdx,
                                             queryReport.getImpressions(q), queryReport.getPromotedImpressions(q), maxImps.get(q),
                                             padAgents, promotionEligibiltyVerified, hitOurBudget,
                                             reducedImpMean, reducedImpStdDev, _isSampled, ordering, agentNamesArr);

            //TODO: Pull out the data format
            ImpressionEstimatorSample ie = new ImpressionEstimatorSample(inst);
            ImpressionAndRankEstimator estimator = new LDSImpressionAndRankEstimator(ie);
            IEResult bestSol = estimator.getBestSolution();

            if(bestSol != null ) {
            	
            	//FIXME: I noticed that occasionally, there will be a padded agent, but its identity in the "getOrder()" matrix
            	//is 1 instead of -1. I've only seen this happen with 2 agents total, 1 padded and 1 unpadded (the unpadded one being us)
            	//After identifying the agent, we are going to assign it a value of 1 in the order matrix anyway, so this isn't hurting anything.
            	//But it is suspicious and should be investigated.
            	//(check for instances where # of -1 values in orderArray != numPadded)
            	            	
            	//System.out.println("BEFORE: AGENT NAMES ARR: " + Arrays.toString(agentNamesArr));
            	//System.out.println("BEFORE: ORDER: " + Arrays.toString(bestSol.getOrder()));

            	//Keep track of which agent is in which initial position (before any padded agents or other adjustments)
                updateObservedAgentPositions(q, bestSol.getOrder(), agentNamesArr); 
                
                //We can either update now, before padded agents have been assigned, or afterwards.
                //updateObservedAgentImpressions(); //Keep track of how many impressions we think each agent saw (pre-adjustment)
            	
            	//Get number of padded agents
            	int numPadded = bestSol.getWaterfall().length - agentNamesArr.length;
            	ArrayList<String> paddedAgentNames = getPaddedAgentPredictions(q, numPadded, agentNames);
            	
            	//Need to update: 
            	//  agentNamesArr: add padded agents to the end of the list
            	//  order[i] array: which agent was at each index i. (agents identified by their position in the agentNamesArr)
            	//  agentIdsArr????

            	//Update order array
            	int[] order = bestSol.getOrder();
            	int paddedIdx=0;
            	for (int i=0; i<order.length; i++) {
            		if (order[i] == -1) {
            			order[i] = agentNamesArr.length + paddedIdx;
            			paddedIdx++;
            		}            		
            	}
            	bestSol.setOrder(order);
            	
            	//Update agentNamesArr
            	String[] newAgentNamesArr = new String[agentNamesArr.length + numPadded];
            	for (int a=0; a<agentNamesArr.length; a++) {
            		newAgentNamesArr[a] = agentNamesArr[a];
            	}
            	for (int a=0; a<numPadded; a++) {
            		newAgentNamesArr[agentNamesArr.length + a] = paddedAgentNames.get(a);
            	}
            	agentNamesArr = newAgentNamesArr;
            	
            	//FIXME: Update agentIdsArr (doesn't seem to be used anywhere right now...)
            	            	
            	//System.out.println("AFTER: AGENT NAMES ARR: " + Arrays.toString(agentNamesArr));
            	//System.out.println("AFTER: ORDER: " + Arrays.toString(bestSol.getOrder()));
            	
//            	//DEBUG
//            	System.out.println("QUERY=: " + q);
//            	System.out.println("agents: " + Arrays.toString(agentNamesArr));
//            	System.out.println("trueAvgPos: " + Arrays.toString(trueAvgPos));
//            	System.out.println("sampledAvgP: " + Arrays.toString(sampledAvgPos));
//            	System.out.println("order: " + Arrays.toString(bestSol.getOrder()));
//            	System.out.println("slotImps: " + Arrays.toString(bestSol.getSlotImpressions()));
//            	System.out.println("sol: " +  Arrays.toString(bestSol.getSol()));
//            	int[][] w = bestSol.getWaterfall();
//            	System.out.println("waterfall:");
//            	for (int x=0; x<w.length; x++) {
//            		System.out.println(Arrays.toString(w[x]));
//            	}
//            	System.out.println("end waterfall");
//            	//END DEBUG
            	
            	
            	
               _allResults.get(q).add(bestSol);
               _allImpRanges.get(q).add(bestSol.getWaterfall());

               _allAgentNames.get(q).add(agentNamesArr);
               _allAgentIDs.get(q).add(agentIdsArr); //these are the IDs of each agent that was in the QAInstance for the given query. Corresponds to indices of IEResults

               int[] imps = getImpressionsPrediction(q);
               int[] ranks = getOrderPrediction(q);

               for (int i = 0; i < _advertisers.size(); i++) {
                  Map<Query, Integer> impPredMap = impPredMapList.get(i);
                  int agentIdx = -1;
                  for (int j = 0; j < imps.length; j++) {
                     if (i == ranks[j]) {
                        if(!Double.isNaN(queryReport.getPosition(q, "adv" + (j + 1)))) {
                           agentIdx = j;
                        }
                        else {
                           break;
                        }
                     }
                  }

                  if (agentIdx > -1) {
                     impPredMap.put(q, imps[agentIdx]);
                  } else {
                     impPredMap.put(q, -1);
                  }
               }

//               System.out.println("Done solving. " + bestSol + "\t agentNames:" + Arrays.toString(agentNamesArr));
            }
            else {
            	
               _allResults.get(q).add(null);
               _allImpRanges.get(q).add(null);
               _allAgentNames.get(q).add(null);
               _allAgentIDs.get(q).add(null);

               for (int i = 0; i < _advertisers.size(); i++) {
                  Map<Query, Integer> impPredMap = impPredMapList.get(i);
                  impPredMap.put(q, -1);
               }

               System.out.println("********Query Analyzer failed to find a solution************");
            }
         }
         else {
        	 
            _allResults.get(q).add(null);
            _allImpRanges.get(q).add(null);
            _allAgentNames.get(q).add(null);
            _allAgentIDs.get(q).add(null);

            for (int i = 0; i < _advertisers.size(); i++) {
               Map<Query, Integer> impPredMap = impPredMapList.get(i);
               impPredMap.put(q, -1);
            }
         }
      }

      
      //TODO: Move this above? (before changes made to padded agents)
      updateObservedAgentImpressions(); //Keep track of how many impressions we think each agent saw (pre-adjustment)
      
      
      _impressionForecaster.updateModel(impPredMapList);

      return true;
   }

   

   private ArrayList<String> getPaddedAgentPredictions(Query q, int numPadded,
		   ArrayList<String> agentNamesUsed) {

	   //The "padded agent" is likely someone who was in for a very short amount of time.
	   //Of the remaining advertisers, look at the one w/ lowest median impressions
	   //(for the instances in which it appears)
	   //Fill padded agents in this order.

	   //The list of agents that we determine are padded
	   ArrayList<String> paddedAgentAssignments = new ArrayList<String>();

	   //The list of candidate padded agents 
	   ArrayList<String> advsLeft = new ArrayList<String>(_advertisers.size());
	   for(int i = 0; i < _advertisers.size(); i++) {
		   advsLeft.add("adv"+(i+1));
	   }
	   advsLeft.remove(_ourAdvertiser); //We know we weren't a padded agent.
	   advsLeft.removeAll(agentNamesUsed); //Nobody already in the auction can be a padded agent.

	   //Get the median amount of impressions each of these candidates has received in this query
	   ArrayList<Double> medianImpsOfAdvsLeft = new ArrayList<Double>();
	   ArrayList<Integer> zero = new ArrayList<Integer>();
	   zero.add(0);
	   for (String adv : advsLeft) {
		   //Only get instances where the agent saw some impressions
		   ArrayList<Integer> imps = new ArrayList<Integer>(_allAgentImpressionsPREADJUST.get(q).get(adv));        		 
		   imps.removeAll(zero);
		   double medianImps = getMedian(imps);
		   if (medianImps == Double.NaN) medianImps = Double.POSITIVE_INFINITY;
		   medianImpsOfAdvsLeft.add(medianImps);
	   }
	   
	   //System.out.println("advsLeft: " + advsLeft);
	   //System.out.println("medianImps: " + medianImpsOfAdvsLeft);
	   
	   //Assign padded agents in order of fewers median impressions
	   for(int i = 0; i < numPadded; i++) {
		   //Get advertiser w/ smallest median imps
		   double lowestMedianImps = Double.POSITIVE_INFINITY;
		   int lowestIdx = 0;
		   //Get the remaining advertiser with the lowest average position
		   for (int j=0; j<medianImpsOfAdvsLeft.size(); j++) {
			   double medianImps = medianImpsOfAdvsLeft.get(j);
			   if (medianImps < lowestMedianImps) {
				   lowestMedianImps = medianImps;
				   lowestIdx = j;
			   }
		   }
		   String adv = advsLeft.get(lowestIdx);
		   advsLeft.remove(lowestIdx);
		   medianImpsOfAdvsLeft.remove(lowestIdx);

		   paddedAgentAssignments.add(adv);
		   //waterfallPred[_advToIdx.get(adv)] = waterfallPredReduced[agentNamesReduced.length+i];
	   }
	   
	   
//  	 //Old method: This is arbitrarily choosing agents to take the place of padded agents.
//	   	 //(code will need to be updated if used; it was moved from a different method)
//  	 int numPadded = waterfallPredReduced.length-agentNamesReduced.length;
//  	 for(int i = 0; i < numPadded; i++) {
//  		 String adv = advsLeft.remove(0);
//  		 waterfallPred[_advToIdx.get(adv)] = waterfallPredReduced[agentNamesReduced.length+i];
//  	 }
	   
	return paddedAgentAssignments;
}

   
@Override
   public String toString() {
      return "CarletonQueryAnalyzer";
   }

   @Override
   public AbstractModel getCopy() {
      return new CarletonQueryAnalyzer(_querySpace, _advertisers, _ourAdvertiser, REPORT_FULLPOS_FORSELF, _isSampled);
   }

   @Override
   public void setAdvertiser(String ourAdv) {
      _ourAdvertiser = ourAdv;
   }

}
