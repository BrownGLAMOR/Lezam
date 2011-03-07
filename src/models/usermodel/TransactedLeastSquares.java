package models.usermodel;

/**
 * @author jberg
 *
 */

import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.QRDecomposition;
import org.apache.commons.math.linear.QRDecompositionImpl;

import java.util.HashMap;
import java.util.Random;

public class TransactedLeastSquares {

   public HashMap<UserState, HashMap<UserState, Double>> _standardProbs;
   public HashMap<UserState, HashMap<UserState, Double>> _burstProbs;
   final static double weight1 = 1.0;
   final static double weight2 = 1.0;
   final static double weight3 = 1.0;


   public TransactedLeastSquares() {
      _standardProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _burstProbs = new HashMap<UserState, HashMap<UserState, Double>>();

      HashMap<UserState, Double> standardFromNSProbs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> standardFromISProbs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> standardFromF0Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> standardFromF1Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> standardFromF2Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> standardFromTProbs = new HashMap<UserState, Double>();

      HashMap<UserState, Double> burstFromNSProbs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> burstFromISProbs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> burstFromF0Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> burstFromF1Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> burstFromF2Probs = new HashMap<UserState, Double>();
      HashMap<UserState, Double> burstFromTProbs = new HashMap<UserState, Double>();

      standardFromNSProbs.put(UserState.NS, 0.99);
      standardFromNSProbs.put(UserState.IS, 0.01);
      standardFromNSProbs.put(UserState.F0, 0.0);
      standardFromNSProbs.put(UserState.F1, 0.0);
      standardFromNSProbs.put(UserState.F2, 0.0);
      standardFromNSProbs.put(UserState.T, 0.0);

      standardFromISProbs.put(UserState.NS, 0.05);
      standardFromISProbs.put(UserState.IS, 0.2);
      standardFromISProbs.put(UserState.F0, 0.6);
      standardFromISProbs.put(UserState.F1, 0.1);
      standardFromISProbs.put(UserState.F2, 0.05);
      standardFromISProbs.put(UserState.T, 0.0);

      standardFromF0Probs.put(UserState.NS, 0.1);
      standardFromF0Probs.put(UserState.IS, 0.0);
      standardFromF0Probs.put(UserState.F0, 0.7);
      standardFromF0Probs.put(UserState.F1, 0.2);
      standardFromF0Probs.put(UserState.F2, 0.0);
      standardFromF0Probs.put(UserState.T, 0.0);

      standardFromF1Probs.put(UserState.NS, 0.1);
      standardFromF1Probs.put(UserState.IS, 0.0);
      standardFromF1Probs.put(UserState.F0, 0.0);
      standardFromF1Probs.put(UserState.F1, 0.7);
      standardFromF1Probs.put(UserState.F2, 0.2);
      standardFromF1Probs.put(UserState.T, 0.0);

      standardFromF2Probs.put(UserState.NS, 0.1);
      standardFromF2Probs.put(UserState.IS, 0.0);
      standardFromF2Probs.put(UserState.F0, 0.0);
      standardFromF2Probs.put(UserState.F1, 0.0);
      standardFromF2Probs.put(UserState.F2, 0.9);
      standardFromF2Probs.put(UserState.T, 0.0);

      standardFromTProbs.put(UserState.NS, 0.8);
      standardFromTProbs.put(UserState.IS, 0.0);
      standardFromTProbs.put(UserState.F0, 0.0);
      standardFromTProbs.put(UserState.F1, 0.0);
      standardFromTProbs.put(UserState.F2, 0.0);
      standardFromTProbs.put(UserState.T, 0.2);

      burstFromNSProbs.put(UserState.NS, 0.8);
      burstFromNSProbs.put(UserState.IS, 0.2);
      burstFromNSProbs.put(UserState.F0, 0.0);
      burstFromNSProbs.put(UserState.F1, 0.0);
      burstFromNSProbs.put(UserState.F2, 0.0);
      burstFromNSProbs.put(UserState.T, 0.0);

      burstFromISProbs.put(UserState.NS, 0.05);
      burstFromISProbs.put(UserState.IS, 0.2);
      burstFromISProbs.put(UserState.F0, 0.6);
      burstFromISProbs.put(UserState.F1, 0.1);
      burstFromISProbs.put(UserState.F2, 0.05);
      burstFromISProbs.put(UserState.T, 0.0);

      burstFromF0Probs.put(UserState.NS, 0.1);
      burstFromF0Probs.put(UserState.IS, 0.0);
      burstFromF0Probs.put(UserState.F0, 0.7);
      burstFromF0Probs.put(UserState.F1, 0.2);
      burstFromF0Probs.put(UserState.F2, 0.0);
      burstFromF0Probs.put(UserState.T, 0.0);

      burstFromF1Probs.put(UserState.NS, 0.1);
      burstFromF1Probs.put(UserState.IS, 0.0);
      burstFromF1Probs.put(UserState.F0, 0.0);
      burstFromF1Probs.put(UserState.F1, 0.7);
      burstFromF1Probs.put(UserState.F2, 0.2);
      burstFromF1Probs.put(UserState.T, 0.0);

      burstFromF2Probs.put(UserState.NS, 0.1);
      burstFromF2Probs.put(UserState.IS, 0.0);
      burstFromF2Probs.put(UserState.F0, 0.0);
      burstFromF2Probs.put(UserState.F1, 0.0);
      burstFromF2Probs.put(UserState.F2, 0.9);
      burstFromF2Probs.put(UserState.T, 0.0);

      burstFromTProbs.put(UserState.NS, 0.8);
      burstFromTProbs.put(UserState.IS, 0.0);
      burstFromTProbs.put(UserState.F0, 0.0);
      burstFromTProbs.put(UserState.F1, 0.0);
      burstFromTProbs.put(UserState.F2, 0.0);
      burstFromTProbs.put(UserState.T, 0.2);

      _standardProbs.put(UserState.NS, standardFromNSProbs);
      _standardProbs.put(UserState.IS, standardFromISProbs);
      _standardProbs.put(UserState.F0, standardFromF0Probs);
      _standardProbs.put(UserState.F1, standardFromF1Probs);
      _standardProbs.put(UserState.F2, standardFromF2Probs);
      _standardProbs.put(UserState.T, standardFromTProbs);

      _burstProbs.put(UserState.NS, burstFromNSProbs);
      _burstProbs.put(UserState.IS, burstFromISProbs);
      _burstProbs.put(UserState.F0, burstFromF0Probs);
      _burstProbs.put(UserState.F1, burstFromF1Probs);
      _burstProbs.put(UserState.F2, burstFromF2Probs);
      _burstProbs.put(UserState.T, burstFromTProbs);
   }

   public void computeTransactedProbs(HashMap<UserState, Integer> state1, HashMap<UserState, Integer> state2) {
      /*
         * Setting up the system of equations
         *
         * Ax = b
         */

      double[][] A = new double[13][11];

      A[0][0] = weight1 * state1.get(UserState.F0);
      A[0][4] = weight1 * state1.get(UserState.F1);
      A[0][8] = weight1 * state1.get(UserState.F2);

      A[1][1] = weight1 * state1.get(UserState.F0);

      A[2][2] = weight1 * state1.get(UserState.F0);
      A[2][5] = weight1 * state1.get(UserState.F1);

      A[3][6] = weight1 * state1.get(UserState.F1);
      A[3][9] = weight1 * state1.get(UserState.F2);

      A[4][3] = weight1 * state1.get(UserState.F0);
      A[4][7] = weight1 * state1.get(UserState.F1);
      A[4][10] = weight1 * state1.get(UserState.F2);

      A[5][0] = weight2 * 1;
      A[5][1] = weight2 * 1;
      A[5][2] = weight2 * 1;
      A[5][3] = weight2 * 1;

      A[6][4] = weight2 * 1;
      A[6][5] = weight2 * 1;
      A[6][6] = weight2 * 1;
      A[6][7] = weight2 * 1;

      A[7][8] = weight2 * 1;
      A[7][9] = weight2 * 1;
      A[7][10] = weight2 * 1;

      A[8][0] = weight3 * -7;
      A[8][1] = weight3 * 1;

      A[9][0] = weight3 * -2;
      A[9][2] = weight3 * 1;

      A[10][4] = weight3 * -7;
      A[10][5] = weight3 * 1;

      A[11][4] = weight3 * -2;
      A[11][6] = weight3 * 1;

      A[12][8] = weight3 * -9;
      A[12][9] = weight3 * 1;

      Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(A);

      double[] b = new double[13];
      b[0] = weight1 * (state2.get(UserState.NS) - _standardProbs.get(UserState.NS).get(UserState.NS) * state1.get(UserState.NS)
                                - _standardProbs.get(UserState.IS).get(UserState.NS) * state1.get(UserState.IS)
                                - _standardProbs.get(UserState.T).get(UserState.NS) * state1.get(UserState.T));
      b[1] = weight1 * (state2.get(UserState.F0) - _standardProbs.get(UserState.IS).get(UserState.F0) * state1.get(UserState.IS));
      b[2] = weight1 * (state2.get(UserState.F1) - _standardProbs.get(UserState.IS).get(UserState.F1) * state1.get(UserState.IS));
      b[3] = weight1 * (state2.get(UserState.F2) - _standardProbs.get(UserState.IS).get(UserState.F2) * state1.get(UserState.IS));
      b[4] = weight1 * (state2.get(UserState.T) - _standardProbs.get(UserState.T).get(UserState.T) * state1.get(UserState.T));
      b[5] = weight2 * 1;
      b[6] = weight2 * 1;
      b[7] = weight2 * 1;
      b[8] = weight3 * 0;
      b[9] = weight3 * 0;
      b[10] = weight3 * 0;
      b[11] = weight3 * 0;
      b[12] = weight3 * 0;

      QRDecomposition solver = new QRDecompositionImpl(matrix);
      double[] answers = solver.getSolver().solve(b);

      /*
         * Extracting the transition model from the
         * system solution
         */
      double[][] transMat = new double[6][6];

      transMat[0][0] = _standardProbs.get(UserState.NS).get(UserState.NS);
      transMat[0][1] = _standardProbs.get(UserState.IS).get(UserState.NS);
      transMat[0][2] = answers[0];
      transMat[0][3] = answers[4];
      transMat[0][4] = answers[8];
      transMat[0][5] = _standardProbs.get(UserState.T).get(UserState.NS);

      transMat[1][0] = _standardProbs.get(UserState.NS).get(UserState.IS);
      transMat[1][1] = _standardProbs.get(UserState.IS).get(UserState.IS);
      transMat[1][2] = _standardProbs.get(UserState.F0).get(UserState.IS);
      transMat[1][3] = _standardProbs.get(UserState.F1).get(UserState.IS);
      transMat[1][4] = _standardProbs.get(UserState.F2).get(UserState.IS);
      transMat[1][5] = _standardProbs.get(UserState.T).get(UserState.IS);

      transMat[2][0] = _standardProbs.get(UserState.NS).get(UserState.F0);
      transMat[2][1] = _standardProbs.get(UserState.IS).get(UserState.F0);
      transMat[2][2] = answers[1];
      transMat[2][3] = _standardProbs.get(UserState.F1).get(UserState.F0);
      transMat[2][4] = _standardProbs.get(UserState.F2).get(UserState.F0);
      transMat[2][5] = _standardProbs.get(UserState.T).get(UserState.F0);

      transMat[3][0] = _standardProbs.get(UserState.NS).get(UserState.F1);
      transMat[3][1] = _standardProbs.get(UserState.IS).get(UserState.F1);
      transMat[3][2] = answers[2];
      transMat[3][3] = answers[5];
      transMat[3][4] = _standardProbs.get(UserState.F2).get(UserState.F1);
      transMat[3][5] = _standardProbs.get(UserState.T).get(UserState.F1);

      transMat[4][0] = _standardProbs.get(UserState.NS).get(UserState.F2);
      transMat[4][1] = _standardProbs.get(UserState.IS).get(UserState.F2);
      transMat[4][2] = _standardProbs.get(UserState.F0).get(UserState.F2);
      transMat[4][3] = answers[6];
      transMat[4][4] = answers[9];
      transMat[4][5] = _standardProbs.get(UserState.T).get(UserState.F2);

      transMat[5][0] = _standardProbs.get(UserState.NS).get(UserState.T);
      transMat[5][1] = _standardProbs.get(UserState.IS).get(UserState.T);
      transMat[5][2] = answers[3];
      transMat[5][3] = answers[7];
      transMat[5][4] = answers[10];
      transMat[5][5] = _standardProbs.get(UserState.T).get(UserState.T);

      Array2DRowRealMatrix T = new Array2DRowRealMatrix(transMat);

      /*
         * Check the answer
         */

      double[] oldVec = new double[UserState.values().length];
      double[] newVec = new double[UserState.values().length];

      oldVec[0] = state1.get(UserState.NS);
      oldVec[1] = state1.get(UserState.IS);
      oldVec[2] = state1.get(UserState.F0);
      oldVec[3] = state1.get(UserState.F1);
      oldVec[4] = state1.get(UserState.F2);
      oldVec[5] = state1.get(UserState.T);

      newVec[0] = state2.get(UserState.NS);
      newVec[1] = state2.get(UserState.IS);
      newVec[2] = state2.get(UserState.F0);
      newVec[3] = state2.get(UserState.F1);
      newVec[4] = state2.get(UserState.F2);
      newVec[5] = state2.get(UserState.T);

      Array2DRowRealMatrix oldArr = new Array2DRowRealMatrix(oldVec);
      Array2DRowRealMatrix newArr = new Array2DRowRealMatrix(newVec);
      Array2DRowRealMatrix estVar = T.multiply(oldArr);

      double[][] data = estVar.getData();
      double total = 0.0;
      for (int i = 0; i < data.length; i++) {
         total += data[i][0];
      }

      System.out.println("T: " + T);
      System.out.println("V: " + oldArr);
      System.out.println("TV: " + estVar + ", tot: " + total);
      System.out.println("V': " + newArr);
      System.out.println("Answers: " + new ArrayRealVector(answers) + "\n");
   }

   public void computeTransactedProbs2(HashMap<UserState, Integer> state1, HashMap<UserState, Integer> state2) {
      /*
         * Setting up the system of equations
         *
         * Ax = b
         */

      double[][] A = new double[6][3];

      A[0][0] = state1.get(UserState.F0);
      A[0][1] = state1.get(UserState.F1);
      A[0][2] = state1.get(UserState.F2);

      A[1][0] = -.7 * state1.get(UserState.F0);

      A[2][0] = -.2 * state1.get(UserState.F0);
      A[2][1] = -.7 * state1.get(UserState.F1);

      A[3][1] = -.2 * state1.get(UserState.F1);
      A[3][2] = -.9 * state1.get(UserState.F2);

      A[4][0] = -2 * state1.get(UserState.F0);
      A[4][1] = 1 * state1.get(UserState.F0);

      A[5][1] = -1.5 * state1.get(UserState.F0);
      A[5][2] = 1 * state1.get(UserState.F0);

      Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(A);

      double[] b = new double[6];
      b[0] = state2.get(UserState.T) - _standardProbs.get(UserState.T).get(UserState.T) * state1.get(UserState.T);
      b[1] = state2.get(UserState.F0) - _standardProbs.get(UserState.IS).get(UserState.F0) * state1.get(UserState.IS) - .7 * state1.get(UserState.F0);
      b[2] = state2.get(UserState.F1) - _standardProbs.get(UserState.IS).get(UserState.F1) * state1.get(UserState.IS) - .2 * state1.get(UserState.F0) - .7 * state1.get(UserState.F1);
      b[3] = state2.get(UserState.F2) - _standardProbs.get(UserState.IS).get(UserState.F2) * state1.get(UserState.IS) - .2 * state1.get(UserState.F1) - .9 * state1.get(UserState.F2);
      b[4] = 0;
      b[5] = 0;
      QRDecomposition solver = new QRDecompositionImpl(matrix);
      double[] answers = solver.getSolver().solve(b);

      /*
         * Extracting the transition model from the
         * system solution
         */
      double[][] transMat = new double[6][6];

      transMat[0][0] = _standardProbs.get(UserState.NS).get(UserState.NS);
      transMat[0][1] = _standardProbs.get(UserState.IS).get(UserState.NS);
      transMat[0][2] = _standardProbs.get(UserState.F0).get(UserState.NS) * (1 - answers[0]);
      transMat[0][3] = _standardProbs.get(UserState.F1).get(UserState.NS) * (1 - answers[1]);
      transMat[0][4] = _standardProbs.get(UserState.F2).get(UserState.NS) * (1 - answers[2]);
      transMat[0][5] = _standardProbs.get(UserState.T).get(UserState.NS);

      transMat[1][0] = _standardProbs.get(UserState.NS).get(UserState.IS);
      transMat[1][1] = _standardProbs.get(UserState.IS).get(UserState.IS);
      transMat[1][2] = _standardProbs.get(UserState.F0).get(UserState.IS);
      transMat[1][3] = _standardProbs.get(UserState.F1).get(UserState.IS);
      transMat[1][4] = _standardProbs.get(UserState.F2).get(UserState.IS);
      transMat[1][5] = _standardProbs.get(UserState.T).get(UserState.IS);

      transMat[2][0] = _standardProbs.get(UserState.NS).get(UserState.F0);
      transMat[2][1] = _standardProbs.get(UserState.IS).get(UserState.F0);
      transMat[2][2] = _standardProbs.get(UserState.F0).get(UserState.F0) * (1 - answers[0]);
      transMat[2][3] = _standardProbs.get(UserState.F1).get(UserState.F0);
      transMat[2][4] = _standardProbs.get(UserState.F2).get(UserState.F0);
      transMat[2][5] = _standardProbs.get(UserState.T).get(UserState.F0);

      transMat[3][0] = _standardProbs.get(UserState.NS).get(UserState.F1);
      transMat[3][1] = _standardProbs.get(UserState.IS).get(UserState.F1);
      transMat[3][2] = _standardProbs.get(UserState.F0).get(UserState.F1) * (1 - answers[0]);
      transMat[3][3] = _standardProbs.get(UserState.F1).get(UserState.F1) * (1 - answers[1]);
      transMat[3][4] = _standardProbs.get(UserState.F2).get(UserState.F1);
      transMat[3][5] = _standardProbs.get(UserState.T).get(UserState.F1);

      transMat[4][0] = _standardProbs.get(UserState.NS).get(UserState.F2);
      transMat[4][1] = _standardProbs.get(UserState.IS).get(UserState.F2);
      transMat[4][2] = _standardProbs.get(UserState.F0).get(UserState.F2);
      transMat[4][3] = _standardProbs.get(UserState.F1).get(UserState.F2) * (1 - answers[1]);
      transMat[4][4] = _standardProbs.get(UserState.F2).get(UserState.F2) * (1 - answers[2]);
      transMat[4][5] = _standardProbs.get(UserState.T).get(UserState.F2);

      transMat[5][0] = _standardProbs.get(UserState.NS).get(UserState.T);
      transMat[5][1] = _standardProbs.get(UserState.IS).get(UserState.T);
      transMat[5][2] = answers[0];
      transMat[5][3] = answers[1];
      transMat[5][4] = answers[2];
      transMat[5][5] = _standardProbs.get(UserState.T).get(UserState.T);

      Array2DRowRealMatrix T = new Array2DRowRealMatrix(transMat);

      /*
         * Check the answer
         */

      double[] oldVec = new double[UserState.values().length];
      double[] newVec = new double[UserState.values().length];

      oldVec[0] = state1.get(UserState.NS);
      oldVec[1] = state1.get(UserState.IS);
      oldVec[2] = state1.get(UserState.F0);
      oldVec[3] = state1.get(UserState.F1);
      oldVec[4] = state1.get(UserState.F2);
      oldVec[5] = state1.get(UserState.T);

      newVec[0] = state2.get(UserState.NS);
      newVec[1] = state2.get(UserState.IS);
      newVec[2] = state2.get(UserState.F0);
      newVec[3] = state2.get(UserState.F1);
      newVec[4] = state2.get(UserState.F2);
      newVec[5] = state2.get(UserState.T);

      Array2DRowRealMatrix oldArr = new Array2DRowRealMatrix(oldVec);
      Array2DRowRealMatrix newArr = new Array2DRowRealMatrix(newVec);
      Array2DRowRealMatrix estVar = T.multiply(oldArr);

      double[][] data = estVar.getData();
      double total = 0.0;
      for (int i = 0; i < data.length; i++) {
         total += data[i][0];
      }

      System.out.println("T: " + T);
      System.out.println("V: " + oldArr);
      System.out.println("TV: " + estVar + ", tot: " + total);
      System.out.println("V': " + newArr);
      System.out.println("Answers: " + new ArrayRealVector(answers) + "\n");
   }


   public static void main(String[] args) {
      TransactedLeastSquares tls = new TransactedLeastSquares();

      Random rand = new Random();

      double[] oldVec = new double[UserState.values().length];
      oldVec[0] = (int) (rand.nextDouble() * 10000);
      oldVec[1] = (int) (rand.nextDouble() * 2000);
      oldVec[2] = (int) (rand.nextDouble() * 1000);
      oldVec[3] = (int) (rand.nextDouble() * 1000);
      oldVec[4] = (int) (rand.nextDouble() * 1000);
      oldVec[5] = (int) (rand.nextDouble() * 400);

      int total = 0;
      for (int i = 0; i < oldVec.length; i++) {
         total += oldVec[i];
      }
      for (int i = 0; i < oldVec.length; i++) {
         oldVec[i] /= total;
         oldVec[i] *= 10000;
      }

      total = 0;
      for (int i = 0; i < oldVec.length; i++) {
         total += oldVec[i];
      }

      if (total < 10000) {
         oldVec[0] += (10000 - total);
      }

      double[][] transMat = new double[6][6];

      double convPr1, convPr2, convPr3;

      do {
         convPr1 = rand.nextDouble() * .1;
         convPr2 = rand.nextDouble() * .2;
         convPr3 = rand.nextDouble() * .3;
      } while (!(convPr1 < convPr2 && convPr2 < convPr3));

      System.out.println(convPr1 + " " + convPr2 + " " + convPr3);

      transMat[0][0] = tls._standardProbs.get(UserState.NS).get(UserState.NS);
      transMat[0][1] = tls._standardProbs.get(UserState.IS).get(UserState.NS);
      transMat[0][2] = (1.0 - convPr1) * .1;
      transMat[0][3] = (1.0 - convPr2) * .1;
      transMat[0][4] = (1.0 - convPr3) * .1;
      transMat[0][5] = tls._standardProbs.get(UserState.T).get(UserState.NS);

      transMat[1][0] = tls._standardProbs.get(UserState.NS).get(UserState.IS);
      transMat[1][1] = tls._standardProbs.get(UserState.IS).get(UserState.IS);
      transMat[1][2] = tls._standardProbs.get(UserState.F0).get(UserState.IS);
      transMat[1][3] = tls._standardProbs.get(UserState.F1).get(UserState.IS);
      transMat[1][4] = tls._standardProbs.get(UserState.F2).get(UserState.IS);
      transMat[1][5] = tls._standardProbs.get(UserState.T).get(UserState.IS);

      transMat[2][0] = tls._standardProbs.get(UserState.NS).get(UserState.F0);
      transMat[2][1] = tls._standardProbs.get(UserState.IS).get(UserState.F0);
      transMat[2][2] = (1.0 - convPr1) * .7;
      transMat[2][3] = 0;
      transMat[2][4] = 0;
      transMat[2][5] = tls._standardProbs.get(UserState.T).get(UserState.F0);

      transMat[3][0] = tls._standardProbs.get(UserState.NS).get(UserState.F1);
      transMat[3][1] = tls._standardProbs.get(UserState.IS).get(UserState.F1);
      transMat[3][2] = (1.0 - convPr1) * .2;
      transMat[3][3] = (1.0 - convPr2) * .7;
      transMat[3][4] = 0;
      transMat[3][5] = tls._standardProbs.get(UserState.T).get(UserState.F1);

      transMat[4][0] = tls._standardProbs.get(UserState.NS).get(UserState.F2);
      transMat[4][1] = tls._standardProbs.get(UserState.IS).get(UserState.F2);
      transMat[4][2] = 0;
      transMat[4][3] = (1.0 - convPr2) * .2;
      transMat[4][4] = (1.0 - convPr2) * .9;
      transMat[4][5] = tls._standardProbs.get(UserState.T).get(UserState.F2);

      transMat[5][0] = tls._standardProbs.get(UserState.NS).get(UserState.T);
      transMat[5][1] = tls._standardProbs.get(UserState.IS).get(UserState.T);
      transMat[5][2] = convPr1;
      transMat[5][3] = convPr2;
      transMat[5][4] = convPr3;
      transMat[5][5] = tls._standardProbs.get(UserState.T).get(UserState.T);

      Array2DRowRealMatrix T = new Array2DRowRealMatrix(transMat);
      Array2DRowRealMatrix oldArr = new Array2DRowRealMatrix(oldVec);
      Array2DRowRealMatrix estVar = T.multiply(oldArr);

      HashMap<UserState, Integer> oldDist = new HashMap<UserState, Integer>();
      oldDist.put(UserState.NS, (int) oldVec[0]);
      oldDist.put(UserState.IS, (int) oldVec[1]);
      oldDist.put(UserState.F0, (int) oldVec[2]);
      oldDist.put(UserState.F1, (int) oldVec[3]);
      oldDist.put(UserState.F2, (int) oldVec[4]);
      oldDist.put(UserState.T, (int) oldVec[5]);

      HashMap<UserState, Integer> newDist = new HashMap<UserState, Integer>();
      newDist.put(UserState.NS, (int) estVar.getData()[0][0]);
      newDist.put(UserState.IS, (int) estVar.getData()[1][0]);
      newDist.put(UserState.F0, (int) estVar.getData()[2][0]);
      newDist.put(UserState.F1, (int) estVar.getData()[3][0]);
      newDist.put(UserState.F2, (int) estVar.getData()[4][0]);
      newDist.put(UserState.T, (int) estVar.getData()[5][0]);

      System.out.println(oldDist);
      System.out.println(newDist);

      tls.computeTransactedProbs2(oldDist, newDist);
   }
}
