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
   private int maxSols = 1000;

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

      int lastIdx = queue.size();

      while (!queue.isEmpty()) {
         int val = queue.poll();
         impsBeforeDropout.add(val);

         if (lastIdx < impsPerAgent.length) {
            queue.add(impsPerAgent[order[lastIdx]] + val);
            lastIdx++;
         }
      }

      debug("BEFORE DUPES: " + impsBeforeDropout);
      impsBeforeDropout = removeDupes(impsBeforeDropout);
      debug("AFTER DUPES: " + impsBeforeDropout);

      return convertListToArr(impsBeforeDropout);
   }

   public ArrayList<Integer> removeDupes(ArrayList<Integer> list) {
      ArrayList<Integer> arrNoDupes = new ArrayList<Integer>();
      for (Integer val : list) {
         if (!arrNoDupes.contains(val)) {
            arrNoDupes.add(val);
         }
      }
      return arrNoDupes;
   }

   public static int[] convertListToArr(List<Integer> integers) {
      int[] ret = new int[integers.size()];
      for (int i = 0; i < ret.length; i++) {
         ret[i] = integers.get(i);
      }
      return ret;
   }


   public void getProbabilityofSample(double[] avgPosArr, int[] order, int[] impsPerAgent, int impressionUpperBound, int numSlots, int numSamples) throws IloException {

      int[] impsBeforeDropout = getDropoutPoints(impsPerAgent, order, numSlots);
      int[][] impsPerSlot = greedyAssign(numSlots, impsPerAgent.length, order, impsPerAgent);

      int numDropoutPoints = impsBeforeDropout.length;

      if (numDropoutPoints == 1 || order.length == 0) {
//         debug("\n\nONLY ONE PERSON IN THE AUCTION, NO PROBLEM!\n\n");
         return;
      }

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
      samplesPerBin = _cplex.intVarArray(numDropoutPoints + 1, lowerBound, upperBound);

      //Setup constraints
      for (int i = 0; i < order.length; i++) {
         int currAgent = order[i];
         double avgPos = avgPosArr[currAgent];
         int[] ourImpsPerSlot = impsPerSlot[currAgent];
         int ourTotImps = impsPerAgent[currAgent];

         if (Double.isNaN(avgPos)) {
            avgPos = 0; //people not in the auctions have a position of zero in this model
         }

         IloLinearNumExpr expr = _cplex.linearNumExpr();

         ArrayList<Boolean> inAuction = new ArrayList<Boolean>();

         //Add the position we are currently in or avgPos if we are out of the auction
         if (i < numSlots) {
            expr.addTerm(i + 1, samplesPerBin[0]);
            inAuction.add(true);
         } else {
            expr.addTerm(avgPos, samplesPerBin[0]);
            inAuction.add(false);
         }

         //Add positions for all future dropout points
         for (int j = 1; j < numDropoutPoints; j++) {
            int totalImpsSeen = impsBeforeDropout[j - 1];

            if (i < numSlots) {
               //Started in the auction
               if (ourTotImps <= totalImpsSeen) {
                  //We are out of the auction so set position to average
                  expr.addTerm(avgPos, samplesPerBin[j]);
                  inAuction.add(false);
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
                     throw new RuntimeException();
                  }

                  expr.addTerm(currPos + 1, samplesPerBin[j]);
                  inAuction.add(true);
               }
            } else {
               //Started out of the auction
               int numDropsBeforeEntry = i - (numSlots - 1);
               int impsBeforeEntry = impsBeforeDropout[numDropsBeforeEntry - 1];

               if (impsBeforeEntry > totalImpsSeen) {
                  //We aren't in the auction yet
                  expr.addTerm(avgPos, samplesPerBin[j]);
                  inAuction.add(false);
               } else if ((impsBeforeEntry + ourTotImps) <= totalImpsSeen) {
                  //We are out of the auction
                  expr.addTerm(avgPos, samplesPerBin[j]);
                  inAuction.add(false);
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
                     throw new RuntimeException();
                  }

                  expr.addTerm(currPos + 1, samplesPerBin[j]);
                  inAuction.add(true);
               }
            }
         }
         expr.addTerm(avgPos, samplesPerBin[numDropoutPoints]);
         _cplex.addEq(expr, avgPos * numSamples);
         debug(expr.toString() + "  =  " + (avgPos * numSamples));

         if (avgPos != 0) {
            IloLinearIntExpr expr2 = _cplex.linearIntExpr();
            for (int j = 0; j < numDropoutPoints; j++) {
               boolean currInAuction = inAuction.get(j);
               if (currInAuction) {
                  expr2.addTerm(1, samplesPerBin[j]);
               }
            }
            _cplex.addGe(expr2, 1);
//            debug(expr2.toString() + " >= " + 1);
         }
      }

      _cplex.addLe(samplesPerBin[numDropoutPoints], numSamples - 1);

      //Add constraint on number of samples
      IloLinearIntExpr expr = _cplex.linearIntExpr();
      for (int i = 0; i < samplesPerBin.length; i++) {
         expr.addTerm(1, samplesPerBin[i]);
      }
      _cplex.addEq(expr, numSamples);

      _cplex.populate();

      int[] impsBetweenDropout = new int[numDropoutPoints + 1];
      impsBetweenDropout[0] = impsBeforeDropout[0];
      for (int i = 1; i < impsBeforeDropout.length; i++) {
         impsBetweenDropout[i] = impsBeforeDropout[i] - impsBeforeDropout[i - 1];
      }
      impsBetweenDropout[numDropoutPoints] = impressionUpperBound - impsBeforeDropout[numDropoutPoints - 1];

      double probability = 0.0;

      System.out.println("" + _cplex.getSolnPoolNsolns());
      for (int i = 0; i < _cplex.getSolnPoolNsolns(); i++) {
         double[] val = _cplex.getValues(samplesPerBin, i);
         String vals = "";
         double innerProb = 1.0;
         for (int j = 0; j < val.length; ++j) {
            double curVal = val[j];
            if (curVal > 0) {
               innerProb *= Math.pow(impsBetweenDropout[j], curVal);
            }
            vals += curVal + ", ";
         }
         probability += innerProb;
         debug(vals);
      }
      probability /= Math.pow(impressionUpperBound, numSamples);
      System.out.println("Probability: " + probability);
      debug("\n\n");

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
