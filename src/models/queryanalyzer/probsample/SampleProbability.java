package models.queryanalyzer.probsample;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;


public class SampleProbability {

   private IloCplex _cplex;
   private int maxSols = 100;

   private boolean DEBUG = true;

   public SampleProbability() {
      try {
         _cplex = new IloCplex(); //establish initial connection with CPLEX
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public int[] getDropoutPoints(int[] impsPerAgent, int[] order, int numSlots) {
      ArrayList<Integer> impsBeforeDropout = new ArrayList<Integer>();
      PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
      for (int i = 0; i < numSlots && i < impsPerAgent.length; i++) {
         queue.add(impsPerAgent[order[i]]);
      }

      int lastIdx = queue.size() - 1;

      while (!queue.isEmpty()) {
         int val = queue.poll();
         impsBeforeDropout.add(val);

         if (lastIdx < impsPerAgent.length) {
            queue.add(impsPerAgent[order[lastIdx]] + val);
            lastIdx++;
         }
      }

      //remove all duplicates from end of array
      while (impsBeforeDropout.size() > 1 &&
              impsBeforeDropout.get(impsBeforeDropout.size() - 1) ==
                      impsBeforeDropout.get(impsBeforeDropout.size() - 2)) {
         impsBeforeDropout.remove(impsBeforeDropout.size() - 1);
      }

      return convertListToArr(impsBeforeDropout);
   }

   public static int[] convertListToArr(List<Integer> integers) {
      int[] ret = new int[integers.size()];
      for (int i = 0; i < ret.length; i++) {
         ret[i] = integers.get(i);
      }
      return ret;
   }


   public void getAllSolutions(double[] avgPosArr, int[] order, int[] impsPerAgent, int numSlots, int numSamples) throws IloException {

      int[] impsBeforeDropout = getDropoutPoints(impsPerAgent, order, numSlots);
      int[][] impsPerSlot = greedyAssign(numSlots, impsPerAgent.length, order, impsPerAgent);

      //TODO: For now we are assuming that samples must take place while agents were still in the auction
      int numDropoutPoints = impsBeforeDropout.length;

//      if(!DEBUG) {
      _cplex.setOut(null);
//      }

      //Setup solution pool to enumerate all solutions
      _cplex.setParam(IloCplex.DoubleParam.SolnPoolAGap, 0); //Set the absolute gap in objective value for solutions in pool
      _cplex.setParam(IloCplex.IntParam.SolnPoolIntensity, 4); //This will enumerate all solutions
      _cplex.setParam(IloCplex.IntParam.SolnPoolReplace, 1); //Keep all optimal solutions
      _cplex.setParam(IloCplex.IntParam.SolnPoolCapacity, maxSols);  //Set size of solution pool
      _cplex.setParam(IloCplex.IntParam.PopulateLim, maxSols); //Set number of solution to get in each populate step

      //Create variables
      IloIntVar[] samplesPerBin;
      int lowerBound = 0;
      int upperBound = numSamples;
      samplesPerBin = _cplex.intVarArray(numDropoutPoints, lowerBound, upperBound);

      //Setup constraints
      for (int i = 0; i < order.length; i++) {
         int currAgent = order[i];
         double avgPos = avgPosArr[currAgent];
         int[] ourImpsPerSlot = impsPerSlot[currAgent];
         int totalImps = impsPerAgent[currAgent];

         if (Double.isNaN(avgPos)) {
            avgPos = 0; //people not in the auctions have a position of zero in this model
         }

         IloLinearNumExpr expr = _cplex.linearNumExpr();

         //Add the position we are currently in or avgPos if we are out of the auction
         if (i < numSlots) {
            expr.addTerm(i + 1, samplesPerBin[0]);
         } else {
            expr.addTerm(avgPos, samplesPerBin[0]);
         }

         //Add positions for all future dropout points
         for (int j = 1; j < numDropoutPoints; j++) {
            int totalImpsSeen = 0;
            for (int k = 0; k < j; k++) {
               totalImpsSeen += impsBeforeDropout[k];
            }

            if (i < numSlots) {
               //Started in the auction
               if (totalImps <= totalImpsSeen) {
                  //We are out of the auction so set position to average
                  expr.addTerm(avgPos, samplesPerBin[j]);
               } else {
                  //Still in auction, determine position
                  double currPos = -1;
                  int innerImpsSeen = 0;
                  for (int k = 0; k <= i; k++) {
                     int idx = i - k;
                     innerImpsSeen += ourImpsPerSlot[idx];
                     if (totalImpsSeen < innerImpsSeen) {
                        currPos = idx;
                        break;
                     }
                  }

                  if (currPos < 0) {
                     //We are no longer in the auction so set positon to average
                     currPos = avgPos;
                     System.out.println("\n\n\nI SHOULD NEVER GET CALLED!\n\n\n");
                  }

                  expr.addTerm(currPos + 1, samplesPerBin[j]);
               }
            } else {
               //Started out of the auction
               int impsBeforeEntry = 0;
               int numDropsBeforeEntry = i - (numSlots - 1);
               for (int k = 0; k < numDropsBeforeEntry; k++) {
                  impsBeforeEntry += impsBeforeDropout[k];
               }

               if (impsBeforeEntry < totalImpsSeen) {
                  //We aren't in the auction yet
                  expr.addTerm(avgPos, samplesPerBin[j]);
               } else if (impsBeforeEntry + totalImps <= totalImpsSeen) {
                  //We are out of the auction
                  expr.addTerm(avgPos, samplesPerBin[j]);
               } else {
                  //Still in auction, determine position
                  double currPos = -1;
                  int innerImpsSeen = impsBeforeEntry;
                  for (int k = 0; k <= i; k++) {
                     int idx = i - k;
                     if (idx < numSlots) {
                        innerImpsSeen += ourImpsPerSlot[idx];
                        if (totalImpsSeen < innerImpsSeen) {
                           currPos = idx;
                           break;
                        }
                     }
                  }

                  if (currPos < 0) {
                     //We are no longer in the auction so set position to average
                     currPos = avgPos;
                     System.out.println("\n\n\nI SHOULD NEVER GET CALLED!\n\n\n");
                  }

                  expr.addTerm(currPos + 1, samplesPerBin[j]);
               }
            }
         }
         _cplex.addEq(expr, avgPos * numSamples);
      }

      //Add constraint on number of samples
      IloLinearIntExpr expr = _cplex.linearIntExpr();
      for (int i = 0; i < numDropoutPoints; i++) {
         expr.addTerm(1, samplesPerBin[i]);
      }
      _cplex.addEq(expr, numSamples);

      _cplex.populate();

      debug("Num Vals = " + _cplex.getSolnPoolNsolns());
      for (int i = 0; i < _cplex.getSolnPoolNsolns(); i++) {
         double[] val = _cplex.getValues(samplesPerBin, i);
         String vals = "";
         for (int j = 0; j < val.length; ++j) {
            vals += val[j] + ", ";
         }
         debug(vals);
      }

   }

   private void debug(String s) {
      if (DEBUG) {
         System.out.println(s);
      }
   }


   /*
  Function input
  number of slots: int slots

  number of agents: int agents

  order of agents: int[]
  example: order = {1, 6, 0, 4, 3, 5, 2} means agent 1 was 1st, agent 6 2nd, 0 3rd, 4 4th, 3 5th, 5 6th, 2 7th
  NOTE: these agents are zero numbered 0 is first... other note agents that are not in the "auction" are
  ommitted so there might be less than 8 agents but that means the numbering must go up to the last agents
  number -1 so if there are 6 agents in the auction the ordering numbers are 0...5

  impressions: int[] impressions
  example: impressions  = {294,22, 8, 294,294,272,286} agent 0 (not the highest slot) has 294 impressions agent 1 22... agent 6 286 impressions
  NOTE: same as order of agents they only reflect the agents in the auction

  Function output
  This is a matrix where one direction is for each agent and the other direction is for the slot.
  The matrix represents is the number of impressions observed at that slot for each of the agents.
  *
  * -gnthomps
   */
   public int[][] greedyAssign(int slots, int agents, int[] order, int[] impressions) {
      int[][] impressionsBySlot = new int[agents][slots];

      int[] slotStart = new int[slots];
      int a;

      for (int i = 0; i < agents; ++i) {
         a = order[i];
         //System.out.println(a);
         int remainingImp = impressions[a];
         //System.out.println("remaining impressions "+ impressions[a]);
         for (int s = Math.min(i + 1, slots) - 1; s >= 0; --s) {
            if (s == 0) {
               impressionsBySlot[a][0] = remainingImp;
               slotStart[0] += remainingImp;
            } else {
               int r = slotStart[s - 1] - slotStart[s];
               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
               assert (r >= 0);
               if (r < remainingImp) {
                  remainingImp -= r;
                  impressionsBySlot[a][s] = r;
                  slotStart[s] += r;
               } else {
                  impressionsBySlot[a][s] = remainingImp;
                  slotStart[s] += remainingImp;
                  break;
               }
            }
         }
      }

      return impressionsBySlot;
   }
}
