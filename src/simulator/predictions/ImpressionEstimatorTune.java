package simulator.predictions;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class ImpressionEstimatorTune {

   private static final int NUM_PARAMS = 4;

   public static final double[] runImpressionEstimator(double avgposstddev, double ouravgposstddev, double imppriorstddev, double ourimppriorstddev,
                                                       double avgpospower, double ouravgpospower, double imppriorpower, double ourimppriorpower) {

      boolean sampleAvgPositions = true;
      boolean perfectImps = true; //NOTE: This is not passed at the command line right now (assume perfect imps)
      boolean useWaterfallPriors = false;
      double noiseFactor = 0;
      boolean useHistoricPriors = true;
      ImpressionEstimatorTest.HistoricalPriorsType historicPriorsType = ImpressionEstimatorTest.HistoricalPriorsType.EMA; //Naive, LastNonZero, SMA, EMA,
      boolean orderingKnown = true;
      ImpressionEstimatorTest.SolverType solverToUse = ImpressionEstimatorTest.SolverType.CP;

//      ImpressionEstimatorTest.GameSet GAMES_TO_TEST = ImpressionEstimatorTest.GameSet.test2010;
//      int START_GAME = 1;
//      int END_GAME = 2;
      ImpressionEstimatorTest.GameSet GAMES_TO_TEST = ImpressionEstimatorTest.GameSet.finals2010;
      int START_GAME = 15127;
      int END_GAME = 15130;
//      int END_GAME = 15136;
      int START_DAY = 0; //0
      int END_DAY = 57; //57
      int START_QUERY = 0; //0
      int END_QUERY = 15; //15
      String AGENT_NAME = "all"; //"all"; //(Schlemazl, crocodileagent, McCon, Nanda_AA, TacTex, tau, all)


      ImpressionEstimatorTest evaluator;

      evaluator = new ImpressionEstimatorTest(sampleAvgPositions, perfectImps, useWaterfallPriors, noiseFactor, useHistoricPriors, historicPriorsType, orderingKnown,1.2);

      double[] err = new double[4];
      try {
         err = evaluator.impressionEstimatorPredictionChallenge(solverToUse, GAMES_TO_TEST, START_GAME, END_GAME,
                                                                START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,100,0,.1);


//         err = evaluator.impressionEstimatorPredictionChallenge(solverToUse, GAMES_TO_TEST, START_GAME, END_GAME,
//                                                                START_DAY, END_DAY, START_QUERY, END_QUERY, AGENT_NAME,100,0,
//                                                                avgposstddev,ouravgposstddev,imppriorstddev,ourimppriorstddev);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return err;
   }

   //Comparator that compares based on first element of an array
   static class ChromosomeComparator implements Comparator<double[]> {
      public int compare(double[] o1, double[] o2) {
         double v1 = o1[0];
         double v2 = o2[0];
         if(v1 < v2) {
            return 1;
         }
         else if(v2 > v1) {
            return -1;
         }
         else {
            return 0;
         }
      }
   }

   private static final void createNewGeneration(String filename, int numChildren, Random rand) throws IOException {
      List<double[]> allChromosomes = new ArrayList<double[]>();

      if(!filename.equals("")) {

         /*
         * Parse Particle Log
         */
         BufferedReader input;
         try {
            input = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = input.readLine()) != null) {
               StringTokenizer st = new StringTokenizer(line, ",");
               if (st.countTokens() == NUM_PARAMS + 3) {
                  double imprErr = Double.parseDouble(st.nextToken());
                  double totImprErr = Double.parseDouble(st.nextToken());
                  double rankErr = Double.parseDouble(st.nextToken());
                  double[] params = new double[NUM_PARAMS + 1];
                  params[0] = 1.0 / (imprErr*totImprErr);
                  for (int i = 0; i < NUM_PARAMS; i++) {
                     params[1+i] = Double.parseDouble(st.nextToken());
                  }
                  allChromosomes.add(params);
               }
               else {
                  break;
               }
            }
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      //Sort Chromosomes
      Collections.sort(allChromosomes, new ChromosomeComparator());

      //Only keep best 1/2
      allChromosomes = allChromosomes.subList(0,(int)(allChromosomes.size()/2.0));

      double weightSum = 0.0;
      for(int i = 0; i < allChromosomes.size(); i++) {
         weightSum += allChromosomes.get(i)[0];
      }

      //Normalize fitness values
      for(int i = 0; i < allChromosomes.size(); i++) {
         double[] params = allChromosomes.get(i);
         params[0] = params[0] / weightSum;
         allChromosomes.set(i,params);
      }

      //Calculate accumulated normalized fitness
      double totalFit = 0.0;
      for(int i = 0; i < allChromosomes.size(); i++) {
         double[] params = allChromosomes.get(i);
         totalFit += params[0];
         params[0] = totalFit;
         allChromosomes.set(i,params);
      }

      //Create Children
      ArrayList<double[]> children = new ArrayList<double[]>(numChildren);
      int keepBest = (int)(numChildren*0.05); //keep best 5%
      for(int i = 0; i < keepBest; i++) {
         children.add(allChromosomes.get(i));
      }

      for(int i = keepBest; i < numChildren; i++) {
         double[] newChild;
         double[] c1 = selectChromosome(allChromosomes, rand.nextDouble());
         if(rand.nextDouble() < .2) {
            newChild = c1;
         }
         else {
            double[] c2 = selectChromosome(allChromosomes, rand.nextDouble());
            newChild = uniformCrossover(c1,c2,rand);
         }
         children.add(newChild);
      }

      //Write Children to File
      FileWriter fstream = new FileWriter("newImpGen" + rand.nextLong() + ".txt");
      BufferedWriter out = new BufferedWriter(fstream);
      String output = "";
      for (int i = 0; i < children.size(); i++) {
         double[] child = children.get(i);
         for(int j = 1; j < child.length; j++) {
            output += child[j] + ",";
         }
         output = output.substring(0, output.length()-1) + "\n";
      }
      out.write(output);
      out.close();
   }

   private static final double[] selectChromosome(List<double[]> chromosomes, double rnum) {
      for(int i = 0; i < chromosomes.size(); i++) {
         double[] c = chromosomes.get(i);
         rnum -= c[0];
         if(rnum < 0) {
            return c;
         }
      }
      return chromosomes.get(chromosomes.size()-1);
   }

   private static final void printChromosomes(List<double[]> chromosomes) {
      System.out.println("Printing Chromosomes: ");
      for(int i = 0; i < chromosomes.size(); i++) {
         System.out.println("\t" + Arrays.toString(chromosomes.get(i)));
      }
      System.out.println("Done Printing Chromosomes: ");
   }

   private static final double[] readParams(String filename, int paramIdx, Random rand) {
      double[] params = new double[NUM_PARAMS];

      boolean setParams = false;

      if(paramIdx >= 0) {

         /*
         * Parse Particle Log
         */
         BufferedReader input = null;
         int count = 0;
         try {
            input = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = input.readLine()) != null && count <= paramIdx) {
               if(count == paramIdx) {
                  StringTokenizer st = new StringTokenizer(line, ",");
                  if (st.countTokens() == NUM_PARAMS) {
                     for (int i = 0; i < NUM_PARAMS; i++) {
                        params[i] = Double.parseDouble(st.nextToken());
                     }
                     setParams = true;
                  } else {
                     break;
                  }
               }
               count++;
            }
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      if(!setParams) {
         params = generateRandomParams(rand);
      }

      return params;
   }

   private static final double[] generateRandomParams(Random rand) {
      double[] params = new double[NUM_PARAMS];
      for(int i = 0; i < NUM_PARAMS; i++) {
         params[i] = rand.nextDouble()*5;
      }
      return params;
   }

   private static final double[] uniformCrossover(double[] c1, double[] c2, Random rand) {
      double[] newc = new double[c1.length];
      for(int i = 0; i < c1.length; i++) {
         if(rand.nextDouble() < .5) {
            newc[i] = c1[i];
         }
         else {
            newc[i] = c2[i];
         }
      }
      return newc;
   }

   private static final double uniformMutate(double gene, Random rand) {
      return rand.nextDouble()*5;
   }

   private static final double gaussianMutate(double gene, Random rand) {
      return Math.max(Math.min(gene + rand.nextGaussian()*.1,5.0),0.0001);
   }

   private static final double[] mutateParams(double[] params, Random rand) {
      double[] newParams = new double[params.length];
      double threshhold = 2.0 / 3.0;
      for(int i = 0; i < params.length; i++) {
         if(rand.nextDouble() < threshhold) {
            newParams[i] = params[i];
         }
         else {
            if(rand.nextDouble() < threshhold) {
               newParams[i] = gaussianMutate(params[i],rand);
            }
            else {
               newParams[i] = uniformMutate(params[i],rand);
            }
         }
      }
      return newParams;
   }

   public static void main(String[] args) throws IOException, ParseException {
      int iters = 30;
      Random rand = new Random();
      String filename = "";
      int paramIdx = -1;
      if(args.length > 0) {
         filename = args[0];
         if(args.length > 1) {
            paramIdx = Integer.parseInt(args[1]);
         }
      }
      double[] params = readParams(filename,paramIdx,rand);
      for(int i = 0; i < iters; i++) {
         double[] newParams;
         if(paramIdx < 0 || (rand.nextDouble() < 0.33333)) {
            newParams = generateRandomParams(rand);
         }
         else {
            newParams = mutateParams(params,rand);
         }
         double p1 = newParams[0];
         double p2 = newParams[1];
         double p3 = newParams[2];
         double p4 = newParams[3];
         double[] err = ImpressionEstimatorTune.runImpressionEstimator(p1,p2,p3,p4,1,1,1,1);
         System.out.println(err[1] + ", " + err[2] + ", " + err[3] + ", " + p1 + "," + p2 + "," + p3 + "," + p4);
      }

//      createNewGeneration(filename,300,rand);
   }
}
