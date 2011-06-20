package models.usermodel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static simulator.parser.GameStatusHandler.UserState;

public class TransactedProbUpdater {

   public static final int NUM_USERS_PER_PROD = 10000;
   public static final int NUM_STATES = 100;
   public static final int NUM_SAMPLES = 25;
   public static final double INIT_PROB = 1.0 / NUM_STATES;
   private static final double _burstProb = .1;
   private static final double _successiveBurstProb = .2;
   public double[][] _states;
   public double[] _weights;
   public ArrayList<Double> _CDF;

   public Random _R;
   private HashMap<UserState, HashMap<UserState, Double>> _standardProbs;
   private HashMap<UserState, HashMap<UserState, Double>> _burstProbs;
   private boolean _rules2009 = false;


   public TransactedProbUpdater() {
      _standardProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _burstProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _R = new Random(61686);

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


      initializeParticlesFromFile("/Users/jordanberg/Documents/workspace/Clients/initTransactedProbs");
   }

   public void initializeParticlesFromFile(String filename) {
      _states = new double[NUM_STATES][6];
      _weights = new double[NUM_STATES];
      _CDF = new ArrayList<Double>();

      /*
         * Parse Particle Log
         */
      BufferedReader input = null;
      int count = 0;
      try {
         input = new BufferedReader(new FileReader(filename));
         String line;
         double total = 0.0;
         while ((line = input.readLine()) != null && count < NUM_STATES) {
            StringTokenizer st = new StringTokenizer(line, "	");
            if (st.countTokens() == 6) {
               for (int i = 0; i < 6; i++) {
                  _states[count][i] = Double.parseDouble(st.nextToken());
               }
               _weights[count] = INIT_PROB / (count + 1);
               total += _weights[count];
            } else {
               break;
            }
            count++;
         }
         if (count > NUM_STATES) {
            throw new RuntimeException("Problem reading particle file");
         }
         for (int i = 0; i < count; i++) {
            _weights[i] /= total;
            if (i == 0) {
               _CDF.add(_weights[i]);
            } else {
               _CDF.add(_weights[i] + _CDF.get(_CDF.size() - 1));
            }
         }

      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void updateProbs(HashMap<UserState, Double> state1, HashMap<UserState, Double> state2) {
      double total = 0.0;
      for (int i = 0; i < NUM_STATES; i++) {
         _weights[i] *= calculateProbability(state1, state2, _states[i]);
         total += _weights[i];
      }


      /*
         * Normalize
         */
      double cdf = 0.0;
      for (int i = 0; i < NUM_STATES; i++) {
         _weights[i] /= total;
         cdf += _weights[i];
         _CDF.set(i, cdf);
      }
   }

   public double[] getSampleProbs() {
      double rand = _R.nextDouble();

      int index = Collections.binarySearch(_CDF, rand);
      if (index < 0) {
         index = -index - 1;
      }

      return _states[index];
   }

   public double calculateProbability(HashMap<UserState, Double> state1, HashMap<UserState, Double> state2, double[] convProbs) {
      double prob = 0.0;
      for (int i = 0; i < NUM_SAMPLES; i++) {
         double burstRand = _R.nextDouble();
         boolean burst;

         if (_rules2009) {
            if (burstRand <= _burstProb) {
               burst = true;
            } else {
               burst = false;
            }
         } else {
            boolean successiveBurst = false;
//				ArrayList<Boolean> burstHistory = particles[i].getBurstHistory();
//				if(burstHistory.size() > 0) {
//					for(int j = 1; j <= 3 || j <= burstHistory.size(); j++) {
//						if(burstHistory.get(burstHistory.size() - j)) {
//							successiveBurst = true;
//							break;
//						}
//					}
//				}

            if (successiveBurst) {
               if (burstRand <= _successiveBurstProb) {
                  burst = true;
               } else {
                  burst = false;
               }
            } else {
               if (burstRand <= _burstProb) {
                  burst = true;
               } else {
                  burst = false;
               }
            }
         }

         HashMap<UserState, HashMap<UserState, Double>> transProbs = burst ? _burstProbs : _standardProbs;

         HashMap<UserState, Double> conversionProbs = new HashMap<UserState, Double>();
         conversionProbs.put(UserState.NS, 0.0);
         conversionProbs.put(UserState.F0, convProbs[0] * (1 + convProbs[1] * _R.nextGaussian()));
         conversionProbs.put(UserState.F1, convProbs[2] * (1 + convProbs[3] * _R.nextGaussian()));
         conversionProbs.put(UserState.F2, convProbs[4] * (1 + convProbs[5] * _R.nextGaussian()));
         if (_rules2009) {
            conversionProbs.put(UserState.IS, (conversionProbs.get(UserState.F0) + conversionProbs.get(UserState.F1) + conversionProbs.get(UserState.F2)) / 3.0);
         } else {
            conversionProbs.put(UserState.IS, 0.0);
         }
         conversionProbs.put(UserState.T, 0.0);

         double[] stateFloat = new double[UserState.values().length];
         double[] newStateFloat = new double[UserState.values().length];

         stateFloat[0] = state1.get(UserState.NS);
         stateFloat[1] = state1.get(UserState.IS);
         stateFloat[2] = state1.get(UserState.F0);
         stateFloat[3] = state1.get(UserState.F1);
         stateFloat[4] = state1.get(UserState.F2);
         stateFloat[5] = state1.get(UserState.T);

         for (int j = 0; j < stateFloat.length; j++) {
            /*
                 * Transition users because of conversions
                 */
            double numConvs = conversionProbs.get(UserState.values()[j]) * stateFloat[j];
            stateFloat[j] -= numConvs;
            newStateFloat[UserState.T.ordinal()] += numConvs;

            /*
                 * Generate perturbed markov chain
                 */
            HashMap<UserState, Double> probs = transProbs.get(UserState.values()[j]);
            double[] probsArr = new double[UserState.values().length];
            double totalWeight = 0.0;
            for (int k = 0; k < probsArr.length; k++) {
               probsArr[k] = getRandomBinomial(NUM_USERS_PER_PROD, probs.get(UserState.values()[k])) / NUM_USERS_PER_PROD;
               totalWeight += probsArr[k];
            }

            /*
                 * Normalize
                 */
            for (int k = 0; k < probsArr.length; k++) {
               probsArr[k] /= totalWeight;
            }

            /*
                 * Now transition users
                 */
            for (int k = 0; k < probsArr.length; k++) {
               newStateFloat[k] += stateFloat[j] * probsArr[k];
            }
         }

         double[] state2Float = new double[UserState.values().length];

         state2Float[0] = state2.get(UserState.NS);
         state2Float[1] = state2.get(UserState.IS);
         state2Float[2] = state2.get(UserState.F0);
         state2Float[3] = state2.get(UserState.F1);
         state2Float[4] = state2.get(UserState.F2);
         state2Float[5] = state2.get(UserState.T);

         prob += getLikelihood(newStateFloat, state2Float);
      }

      prob /= NUM_SAMPLES;

      return prob;
   }

   	/*
	 * Kullback Leibler Divergence Test
	 */
	public static double KLDivergence(double[] P, double[] Q) {
		if(P.length != Q.length) {
			throw new RuntimeException("KL Divergence requires arrays of equal length");
		}

		double total1 = 0.0;
		double total2 = 0.0;
		for(int i = 0; i < P.length; i++) {
			total1 += P[i];
			total2 += Q[i];
		}
		for(int i = 0; i < P.length; i++) {
			P[i] /= total1;
			Q[i] /= total2;
		}

		double divergence = 0.0;

		for(int i = 0; i < P.length; i++) {
			if(P[i] > 0 && Q[i] > 0) {
				divergence += P[i] * (Math.log(P[i])-Math.log(Q[i]));
			}
		}

		return divergence;
	}

	/*
	 * When P and Q are discrete, we can get the likelihood of Q from
	 * the KL divergence.  We want to minimize the KL divergence, which will
	 * in turn maximize the likelihood
	 */
	public static double KLLikelihood(double[] P, double[] Q) {
		double divergence = KLDivergence(P, Q);
		double likelihood = Math.exp(-1*divergence*P.length);
		return likelihood;
	}

   public double getLikelihood(double[] newStateFloat, double[] state2Float) {
      double likelihood = KLLikelihood(state2Float, newStateFloat);
      return likelihood;
   }

   public double getRandomBinomial(double n, double p) {
      if (p == 0) {
         return 0.0;
      }
      double k = _R.nextGaussian() * n * p * (1 - p) + n * p;
      return (k > 0 && k < n) ? k : getRandomBinomial(n, p);
   }

}
