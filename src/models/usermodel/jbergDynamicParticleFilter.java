package models.usermodel;

/**
 * @author jberg
 *
 */

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.io.*;
import java.util.*;


public class jbergDynamicParticleFilter extends ParticleFilterAbstractUserModel {

   private HashMap<UserState, HashMap<UserState, Double>> _standardProbs;
   private HashMap<UserState, HashMap<UserState, Double>> _burstProbs;
   private HashMap<UserState, Double> _conversionProbs;
   private long _seed = 61686;
   private Random _R;
   private ArrayList<Product> _products;
   private Particle[] initParticle;
   private static final double _burstProb = .1;
   private static final double _successiveBurstProb = .2;
   private double _baseConvPr1, _baseConvPr2, _baseConvPr3;
   private double _convPrVar1, _convPrVar2, _convPrVar3; //multiply this by the baseConvPr
   private HashMap<Product, HashMap<UserState, Double>> _predictions, _currentEstimate;
   public HashMap<Product, TransactedProbUpdater> _tpu;

   private static final boolean _rules2009 = true;

   public jbergDynamicParticleFilter(double convPr1, double convPrVar1,
                                     double convPr2, double convPrVar2,
                                     double convPr3, double convPrVar3) {
      _baseConvPr1 = convPr1;
      _convPrVar1 = convPrVar1;
      _baseConvPr2 = convPr2;
      _convPrVar2 = convPrVar2;
      _baseConvPr3 = convPr3;
      _convPrVar3 = convPrVar3;
//		System.out.println(_baseConvPr1 + " " + _convPrVar1 + " " + 
//				_baseConvPr2 + " " + _convPrVar2 + " " + 
//				_baseConvPr3 + " " + _convPrVar3);
      _standardProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _burstProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _R = new Random(_seed);
      _particles = new HashMap<Product, Particle[]>();
      _conversionProbs = new HashMap<UserState, Double>();
      _tpu = new HashMap<Product, TransactedProbUpdater>();

      _conversionProbs.put(UserState.NS, 0.0);
      _conversionProbs.put(UserState.IS, 0.0);
      _conversionProbs.put(UserState.F0, _baseConvPr1);
      _conversionProbs.put(UserState.F1, _baseConvPr2);
      _conversionProbs.put(UserState.F2, _baseConvPr3);
      _conversionProbs.put(UserState.T, 0.0);

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

      _products = new ArrayList<Product>();
      _products.add(new Product("flat", "dvd"));
      _products.add(new Product("flat", "tv"));
      _products.add(new Product("flat", "audio"));
      _products.add(new Product("pg", "dvd"));
      _products.add(new Product("pg", "tv"));
      _products.add(new Product("pg", "audio"));
      _products.add(new Product("lioneer", "dvd"));
      _products.add(new Product("lioneer", "tv"));
      _products.add(new Product("lioneer", "audio"));

      for (Product product : _products) {
         _tpu.put(product, new TransactedProbUpdater());
      }

      initializeParticlesFromFile("/Users/jordanberg/Documents/workspace/Clients/initUserParticles");
//				initializeParticlesFromFile("/u/jberg/initUserParticles");
      updatePredictionMaps();
   }

   public void initializeParticlesFromFile(String filename) {
      int[][] allStates = new int[NUM_PARTICLES][UserState.values().length];

      /*
         * Parse Particle Log
         */
      BufferedReader input = null;
      int count = 0;
      try {
         input = new BufferedReader(new FileReader(filename));
         String line;
         while ((line = input.readLine()) != null && count < NUM_PARTICLES) {
            StringTokenizer st = new StringTokenizer(line, " ");
            if (st.countTokens() == UserState.values().length) {
               for (int i = 0; i < UserState.values().length; i++) {
                  allStates[count][i] = Integer.parseInt(st.nextToken());
               }
            } else {
               break;
            }
            count++;
         }
         if (count > NUM_PARTICLES) {
            throw new RuntimeException("Problem reading particle file");
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      boolean firstTime = true;
      for (Product prod : _products) {
         Particle[] particles = new Particle[NUM_PARTICLES];
         for (int i = 0; i < particles.length; i++) {
            Particle particle = new Particle(allStates[i % count]);
            particles[i] = particle;
         }
         _particles.put(prod, particles);

         if (firstTime) {
            initParticle = new Particle[NUM_PARTICLES];
            for (int i = 0; i < particles.length; i++) {
               Particle particle = new Particle(allStates[i % count]);
               initParticle[i] = particle;
            }
            firstTime = false;
         }
      }
   }

   public UserState transitionUserWithoutConversions(UserState currState, boolean burst) {
      HashMap<UserState, HashMap<UserState, Double>> transProbs;
      if (burst) {
         transProbs = _burstProbs;
      } else {
         transProbs = _standardProbs;
      }

      double rand = _R.nextDouble();
      double threshhold = transProbs.get(currState).get(UserState.NS);
      if (rand <= threshhold) {
         return UserState.NS;
      }
      threshhold += transProbs.get(currState).get(UserState.IS);
      if (rand <= threshhold) {
         return UserState.IS;
      }
      threshhold += transProbs.get(currState).get(UserState.F0);
      if (rand <= threshhold) {
         return UserState.F0;
      }
      threshhold += transProbs.get(currState).get(UserState.F1);
      if (rand <= threshhold) {
         return UserState.F1;
      }
      return UserState.F2;
   }

   public UserState transitionUserWithConversions(UserState currState, boolean burst, HashMap<UserState, Double> conversionProbs) {
      double convRand = _R.nextDouble();
      if (convRand <= conversionProbs.get(currState)) {
         return UserState.T;
      } else {
         return transitionUserWithoutConversions(currState, burst);
      }
   }

   public void updateParticles(int totalImpressions, Particle[] particles) {
      double totalWeight = 0.0;

      /*
         * Update weights based on observations
         */
      for (int i = 0; i < particles.length; i++) {
         Particle particle = particles[i];
         double weight = particle.getWeight();
         int[] state = particle.getState();
         double prob = getProbability(totalImpressions, state);
         double newWeight = weight * prob;
         totalWeight += newWeight;
         particle.setWeight(newWeight);
      }

      /*
         * Normalize Probabilities
         */
      if (totalWeight > 0) {
         for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            double newWeight = particle.getWeight() / totalWeight;
            particle.setWeight(newWeight);
         }
      } else {
         for (int i = 0; i < particles.length; i++) {
            Particle particle = new Particle(initParticle[i].getState(), particles[i].getBurstHistory());
            particles[i] = particle;
         }
//			System.out.println("We had to reinitialize the particles...");
      }
   }

   public double getProbability(int totalImpressions, int[] state) {
      int IS = state[UserState.IS.ordinal()];
      int F2 = state[UserState.F2.ordinal()];
      if (IS + F2 < totalImpressions) {
         return 0.0;
      } else {
         return getBinomialProbUsingGaussian(IS, totalImpressions - F2);
      }
   }

   public double getBinomialProbUsingGaussian(int n, int k) {
      double p = 1.0 / 3.0;
      double mean = n * p;
      double sigma2 = mean * (1.0 - p);
      double diff = k - mean;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));
   }

   public double getRandomBinomial(double n, double p) {
      if (p == 0) {
         return 0.0;
      }
      double k = _R.nextGaussian() * n * p * (1 - p) + n * p;
      return (k > 0 && k < n) ? k : getRandomBinomial(n, p);
   }

   public Particle[] resampleParticles(Particle[] particles) {
      List<Double> CDF = new LinkedList<Double>();
      double CDFval = 0.0;
      for (int i = 0; i < particles.length; i++) {
         CDFval += particles[i].getWeight();
         CDF.add(CDFval);
      }

      Particle[] newParticles = new Particle[particles.length];
      for (int i = 0; i < particles.length; i++) {
         Particle particle = null;
         double rand = _R.nextDouble();

         int index = Collections.binarySearch(CDF, rand);
         if (index < 0) {
            particle = particles[-index - 1];
         } else {
            particle = particles[index];
         }

         Particle newParticle = new Particle(particle.getState(), particle.getBurstHistory());
         newParticles[i] = newParticle;
      }
      return newParticles;
   }

   public void makeInitParticleLog() throws IOException {
      Particle[] particles = new Particle[NUM_PARTICLES];
      for (int i = 0; i < particles.length; i++) {
         Particle particle = new Particle();
         particles[i] = particle;
      }
      for (int day = 0; day < 5; day++) {
         for (int i = 0; i < particles.length; i++) {
            double burstRand = _R.nextDouble();
            boolean burst;
            if (burstRand <= _burstProb) {
               burst = true;
            } else {
               burst = false;
            }
            Particle particle = particles[i];
            int[] state = particle.getState();
            int[] newState = new int[state.length];
            for (int j = 0; j < state.length; j++) {
               for (int k = 0; k < state[j]; k++) {
                  UserState userState = transitionUserWithoutConversions(UserState.values()[j], burst);
                  newState[userState.ordinal()] += 1;
               }
            }
            particle.setState(newState);
         }
      }
      saveParticlesToFile(particles);
   }

   public void saveParticlesToFile(Particle[] particles) throws IOException {
      FileWriter fstream = new FileWriter("initParticles" + _R.nextLong());
      BufferedWriter out = new BufferedWriter(fstream);
      String output = "";
      for (int i = 0; i < particles.length; i++) {
         output += particles[i].stateString() + "\n";
      }
      out.write(output);
      out.close();
   }

   @Override
   public int getCurrentEstimate(Product product, UserState userState) {
      return ((int) ((double) _currentEstimate.get(product).get(userState)));
   }

   @Override
   public int getPrediction(Product product, UserState userState) {
      return ((int) ((double) _predictions.get(product).get(userState)));
   }

   @Override
   public boolean updateModel(HashMap<Query, Integer> totalImpressions) {
      HashMap<Product, HashMap<UserState, Double>> currentEstimateCopy = new HashMap<Product, HashMap<UserState, Double>>();

      for (Product prod : _products) {
         HashMap<UserState, Double> newMap = new HashMap<UserState, Double>();
         for (UserState state : _currentEstimate.get(prod).keySet()) {
            newMap.put(state, new Double(((double) _currentEstimate.get(prod).get(state))));
         }
         currentEstimateCopy.put(prod, newMap);
      }

      for (Query q : totalImpressions.keySet()) {
         Product prod = new Product(q.getManufacturer(), q.getComponent());
         if (_products.contains(prod)) {
            Particle[] particles = _particles.get(prod);
            updateParticles(totalImpressions.get(q), particles);
            particles = resampleParticles(particles);
            _particles.put(prod, particles);
         }
      }

      /*
         * Update Predictions
         */
      updatePredictionMaps();


      for (Query q : totalImpressions.keySet()) {
         Product prod = new Product(q.getManufacturer(), q.getComponent());
         if (_products.contains(prod)) {
            Particle[] particles = _particles.get(prod);
            pushParticlesForward(particles, prod);
            _particles.put(prod, particles);
         }
      }

      /*
         * Update Transacted Prob Models
         */
      for (Query q : totalImpressions.keySet()) {
         Product prod = new Product(q.getManufacturer(), q.getComponent());
         if (_products.contains(prod)) {
            _tpu.get(prod).updateProbs(currentEstimateCopy.get(prod), _currentEstimate.get(prod));
         }
      }

      return true;
   }

   public void pushParticlesForward(Particle[] particles, Product prod) {
      for (int i = 0; i < particles.length; i++) {
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
            ArrayList<Boolean> burstHistory = particles[i].getBurstHistory();
            if (burstHistory.size() > 0) {
               for (int j = 1; j <= 3 || j <= burstHistory.size(); j++) {
                  if (burstHistory.get(burstHistory.size() - j)) {
                     successiveBurst = true;
                     break;
                  }
               }
            }

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

         double[] convProbs = _tpu.get(prod).getSampleProbs();

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
         Particle particle = particles[i];

         particle.addToBurstHistory(burst);

         int[] state = particle.getState();
         int[] newState = new int[state.length];

         double[] stateFloat = new double[state.length];
         double[] newStateFloat = new double[newState.length];

         for (int j = 0; j < state.length; j++) {
            /*
                 * Transition users because of conversions
                 */
            double numConvs = conversionProbs.get(UserState.values()[j]) * state[j];
            stateFloat[j] = state[j] - numConvs;
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

         /*
             * Convert the particles that are represented by
             * double to ints
             */
         int totUsers = 0;
         for (int j = 0; j < state.length; j++) {
            int s = (int) newStateFloat[j];
            newState[j] = s;
            totUsers += s;
         }

         double diff = NUM_USERS_PER_PROD - totUsers;

         if (diff != 0) {
            for (int j = 0; j < Math.abs(diff); j++) {
               int idx = _R.nextInt(state.length);
               if (diff > 0) {
                  newState[idx] += 1;
               } else {
                  newState[idx] -= 1;
               }
            }
         }


         particle.setState(newState);
      }
   }

   public void pushParticlesForward(Particle[] particles, Product prod, boolean burst) {
      for (int i = 0; i < particles.length; i++) {

         HashMap<UserState, HashMap<UserState, Double>> transProbs = burst ? _burstProbs : _standardProbs;

         double[] convProbs = _tpu.get(prod).getSampleProbs();

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
         Particle particle = particles[i];

         particle.addToBurstHistory(burst);

         int[] state = particle.getState();
         int[] newState = new int[state.length];

         double[] stateFloat = new double[state.length];
         double[] newStateFloat = new double[newState.length];

         for (int j = 0; j < state.length; j++) {
            /*
                 * Transition users because of conversions
                 */
            double numConvs = conversionProbs.get(UserState.values()[j]) * state[j];
            stateFloat[j] = state[j] - numConvs;
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

         /*
             * Convert the particles that are represented by
             * double to ints
             */
         int totUsers = 0;
         for (int j = 0; j < state.length; j++) {
            int s = (int) newStateFloat[j];
            newState[j] = s;
            totUsers += s;
         }

         double diff = NUM_USERS_PER_PROD - totUsers;

         if (diff != 0) {
            for (int j = 0; j < Math.abs(diff); j++) {
               int idx = _R.nextInt(state.length);
               if (diff > 0) {
                  newState[idx] += 1;
               } else {
                  newState[idx] -= 1;
               }
            }
         }


         particle.setState(newState);
      }
   }

   private void updatePredictionMaps() {
      HashMap<Product, Particle[]> particlesCopy = new HashMap<Product, Particle[]>();
      _predictions = new HashMap<Product, HashMap<UserState, Double>>();
      _currentEstimate = new HashMap<Product, HashMap<UserState, Double>>();
      for (Product prod : _products) {
         Particle[] particles = _particles.get(prod);
         Particle[] particleCopy = new Particle[particles.length];

         HashMap<UserState, Double> estimates = new HashMap<UserState, Double>();
         double[] estimate = new double[UserState.values().length];

         for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            for (UserState state : UserState.values()) {
               estimate[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
            }
            particleCopy[i] = new Particle(particle.getState(), particle.getWeight(), particle.getBurstHistory());
         }

         for (int i = 0; i < estimate.length; i++) {
            estimates.put(UserState.values()[i], estimate[i]);
         }

         _currentEstimate.put(prod, estimates);
         particlesCopy.put(prod, particleCopy);
      }

      for (Product prod : _products) {
         Particle[] particles = particlesCopy.get(prod);
         pushParticlesForward(particles, prod, false);
         pushParticlesForward(particles, prod, false);
         particlesCopy.put(prod, particles);
      }

      for (Product prod : _products) {
         Particle[] particles = particlesCopy.get(prod);

         HashMap<UserState, Double> estimates = new HashMap<UserState, Double>();
         double[] estimate = new double[UserState.values().length];

         for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            for (UserState state : UserState.values()) {
               estimate[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
            }
         }

         for (int i = 0; i < estimate.length; i++) {
            estimates.put(UserState.values()[i], estimate[i]);
         }

         _predictions.put(prod, estimates);
      }
   }

   public String toString() {
      return "jbergFilter(" + _baseConvPr1 + ", " + _convPrVar1 + ", " + _baseConvPr2 + ", " + _convPrVar2 + ", " + _baseConvPr3 + ", " + _convPrVar3 + ")";
   }

   @Override
   public AbstractModel getCopy() {
      return new jbergDynamicParticleFilter(_baseConvPr1, _convPrVar1, _baseConvPr2, _convPrVar2, _baseConvPr3, _convPrVar3);
   }

}


/*


9.13E-04	0.322757859	0.03073908	0.191880567	0.187458945	0.366898498	186.5540936
0.004932699	0.263532334	0.045700011	0.174371757	0.188113883	0.220140091	186.7697856
0.00137587	0.033243062	0.052249038	0.436104127	0.169024453	0.341843764	187.2162768
0.001004541	0.363593202	0.029031876	0.166947116	0.189067606	0.15234732	188.0296134
0.005197206	0.362167098	0.027407045	0.150127531	0.204464851	0.059186922	188.3200374
0.004470606	0.152708964	0.019707097	0.345928109	0.19809	0.207009812	188.8046702
0.005977345	0.003484737	0.054007691	0.265453547	0.172152857	0.276010655	188.9478476
0.003556267	0.111575966	0.065888996	0.130363111	0.188318986	0.46448095	189.3403428
1.47E-04	0.393714485	0.041534959	0.228842583	0.156192607	0.172643107	189.5679581
0.00394025	0.272837768	0.003960436	0.20564184	0.206756387	0.05615008	190.6669672
0.001068156	0.069795641	0.009629839	0.409295789	0.251571306	0.370058668	190.7993827
0.009169542	0.462383102	0.020392133	0.203126828	0.178788577	0.015930587	190.8358756
0.016361356	0.342006098	0.027478581	0.277848929	0.189073116	0.258146324	191.4164068
0.009047789	0.063400987	0.061828757	0.433036314	0.161090511	0.222698552	191.4242609
0.016530646	0.02055777	0.038830311	0.058805199	0.187553857	0.192670387	191.5670809
0.003178379	0.478498104	0.009151328	0.024975585	0.254086582	0.353953349	191.7099659
0.015307621	0.365407217	0.034933288	0.009679673	0.205192522	0.322534298	191.9689084
9.17E-04	0.207878307	0.072797593	0.296011688	0.131309477	0.01807845	192.0322775


*/