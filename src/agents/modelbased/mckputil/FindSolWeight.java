package agents.modelbased.mckputil;

import java.util.Random;

public class FindSolWeight {

   static double cubicFitA = -5.283245E-09;
   static double cubicFitB = 8.027939E-06;
   static double cubicFitC = -0.00449243;
   static double cubicFitD = 0.9857410;
   static double LAMBDA = .995;

   public static double getSolutionWeightPolyFit(double remCap, double baseSol) {
      if (baseSol == 0) {
         return 0;
      }
      if (baseSol < remCap) {
         return baseSol;
      }
      double a, b, c, d;
      double lambdaRatio = Math.pow(LAMBDA, -remCap) / Math.log(LAMBDA);
      if (remCap < 0) {
         a = cubicFitA * lambdaRatio;
         b = cubicFitB * lambdaRatio - 1.0 / baseSol;
         c = cubicFitC * lambdaRatio;
         d = (cubicFitD - 1) * lambdaRatio;
      } else {
         a = cubicFitA * lambdaRatio;
         b = cubicFitB * lambdaRatio - 1.0 / baseSol;
         c = cubicFitC * lambdaRatio;
         d = remCap + cubicFitD * lambdaRatio - 1.0 / Math.log(LAMBDA);
      }
      double[][] roots = RootFinding.getCubicRoots(a, b, c, d);
      int numGoodRoots = 0;
      double sales = 0;
      for (int i = 0; i < roots.length; i++) {
         if (roots[i][1] == 0) {
//				if(roots[i][0] > 0 && roots[i][0] < baseSol) {
            if (roots[i][0] > 0 && roots[i][0] < baseSol * 1.05) {
               numGoodRoots++;
               sales = roots[i][0];
               System.out.println("Sales: " + sales);
            }
         }
      }
      System.out.println("x1 = " + roots[0][0] + " " + roots[0][1] + " * i");
      System.out.println("x2 = " + roots[1][0] + " " + roots[1][1] + " * i");
      System.out.println("x3 = " + roots[2][0] + " " + roots[2][1] + " * i");
      System.out.println("Good Solutions Found: " + numGoodRoots);

      return sales;
   }

   private static double solutionWeightIter(double remCap, double baseSol) {
      double threshold = .5;
      int maxIters = 40;
      double lastSolWeight = Double.MAX_VALUE;
      double solutionWeight = baseSol;
      double originalSolWeight = solutionWeight;

      Random _R = new Random();

      int numIters = 0;
      while (Math.abs(lastSolWeight - solutionWeight) > threshold) {
         numIters++;
         if (numIters > maxIters) {
            numIters = 0;
            solutionWeight = (_R.nextDouble() + .5) * originalSolWeight; //restart the search
            threshold *= 1.5; //increase the threshold
            maxIters *= 1.25;
         }
         lastSolWeight = solutionWeight;
         double penalty = getPenalty(remCap, lastSolWeight);
         solutionWeight = baseSol * penalty;
      }
      return solutionWeight;
   }

   private static double getPenalty(double remainingCap, double solutionWeight) {
      double penalty;
      solutionWeight = Math.max(0, solutionWeight);
      if (remainingCap < 0) {
         if (solutionWeight <= 0) {
            penalty = Math.pow(LAMBDA, Math.abs(remainingCap));
         } else {
            penalty = 0.0;
            int num = 0;
            for (double j = Math.abs(remainingCap) + 1; j <= Math.abs(remainingCap) + solutionWeight; j++) {
               penalty += Math.pow(LAMBDA, j);
               num++;
            }
            penalty /= (num);
         }
      } else {
         if (solutionWeight <= 0) {
            penalty = 1.0;
         } else {
            if (solutionWeight > remainingCap) {
               penalty = remainingCap;
               for (int j = 1; j <= solutionWeight - remainingCap; j++) {
                  penalty += Math.pow(LAMBDA, j);
               }
               penalty /= (solutionWeight);
            } else {
               penalty = 1.0;
            }
         }
      }
      if (Double.isNaN(penalty)) {
         penalty = 1.0;
      }
      return penalty;
   }


   public static void main(String[] args) {
      System.out.println(solutionWeightIter(-50, 200));
   }

}
