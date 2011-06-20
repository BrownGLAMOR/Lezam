package models.queryanalyzer.probsample;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

import static models.paramest.ConstantsAndFunctions.getDropoutPoints;
import static models.paramest.ConstantsAndFunctions.greedyAssign;


public class SampleProbability {

   private IloCplex _cplex;
   private int maxSols = 1000;

   private boolean DEBUG = false;

   public SampleProbability() {
      try {
         _cplex = new IloCplex(); //establish initial connection with CPLEX
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public double getProbabilityofSample(double[] avgPosArr, int[] order, int[] impsPerAgent, int impressionUpperBound, int numSlots, int numSamples) throws IloException {

      _cplex.clearModel(); //clear last model

      int[] impsBeforeDropout = getDropoutPoints(impsPerAgent, order, numSlots);
      int[][] impsPerSlot = greedyAssign(numSlots, impsPerAgent.length, order, impsPerAgent);

      int numDropoutPoints = impsBeforeDropout.length;

      if (numDropoutPoints == 1 || order.length == 0) {
//         debug("\n\nONLY ONE PERSON IN THE AUCTION, NO PROBLEM!\n\n");
         return 0;
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
      _cplex.setParam(IloCplex.DoubleParam.WorkMem, 1000); //Give it 1GB of RAM

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

      debug("" + _cplex.getSolnPoolNsolns());
      for (int i = 0; i < _cplex.getSolnPoolNsolns(); i++) {
         double[] val = _cplex.getValues(samplesPerBin, i);
         String vals = "";
         double innerProb = 1.0;
         for (int j = 0; j < val.length; ++j) {
            int curVal = (int) (val[j] + .5);
            if (curVal > 0) {
               innerProb *= Math.pow(impsBetweenDropout[j], curVal);
            }
            vals += curVal + ", ";
         }
         probability += innerProb;
         debug(vals);
      }

      if (probability != 0) {
         probability /= Math.pow(impressionUpperBound, numSamples);
      }

      debug("Probability: " + probability);
      debug("\n\n");
//      _cplex.end();
      return probability;
   }

   /*
    * Returns a double since ints and longs are too small
    *
    * this is *much* slower than using powers and assuming sampling
    * with replacement
    *
    */
   private static double binomCoeff(final int n, final int m) {
      if (n < m) {
         return 0;
      } else if (n == m) {
         return 1;
      }

      double[] binom = new double[n + 1];
      binom[0] = 1;
      for (int i = 1; i <= n; i++) {
         binom[i] = 1;
         for (int j = i - 1; j > 0; j--) {
            binom[j] += binom[j - 1];
         }
      }
      return binom[m];
   }

   private void debug(String s) {
      if (DEBUG) {
         System.out.println(s);
      }
   }

}
