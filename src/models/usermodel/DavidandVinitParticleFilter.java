package models.usermodel;

import Jama.Matrix;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math.random.RandomDataImpl;

import java.io.*;
import java.util.*;

//import org.apache.commons.math.*;
//import org.apache.commons.math.distribution.NormalDistributionImpl;


public class DavidandVinitParticleFilter extends ParticleFilterAbstractUserModel {

   private long _seed = 1263456;
   private Random _R;
   private ArrayList<Product> _products;
   final double X = 0.06145;
   final double Y = 0.35;

   //	private NormalDistributionImpl f0_to_t = new NormalDistributionImpl(X,Math.sqrt(Y*X));
   //	private NormalDistributionImpl f1_to_t = new NormalDistributionImpl(2*X,Math.sqrt(2*Y*X));
   //	private NormalDistributionImpl f2_to_t = new NormalDistributionImpl(3*X,Math.sqrt(3*Y*X));

   //										NS		IS		F0		F1		F2		T
   private double[][] base_transitions = {{0.99, 0.05, 0.1, 0.1, 0.1, 0.8},
           {0.01, 0.2, 0.0, 0.0, 0.0, 0.0},
           {0.0, 0.6, 0.7, 0.0, 0.0, 0.0},
           {0.0, 0.1, 0.2, 0.7, 0.0, 0.0},
           {0.0, 0.05, 0.0, 0.2, 0.9, 0.0},
           {0.0, 0.0, 0.0, 0.0, 0.0, 0.2}};

   private double[][] estimated_transitions = {{0.99, 0.05, 0.09, 0.09, 0.08, 0.8},
           {0.01, 0.2, 0.0, 0.0, 0.0, 0.0},
           {0.0, 0.6, 0.66, 0.0, 0.0, 0.0},
           {0.0, 0.1, 0.19, 0.62, 0.0, 0.0},
           {0.0, 0.05, 0.0, 0.18, 0.76, 0.0},
           {0.0, 0.0, 0.06, 0.11, 0.16, 0.2}};

//	private double[][] estimated_transitions = {	{0.99,	0.05,	0.94,	0.88,	0.82,	0.8},
//			{0.01,	0.2,	0.0,	0.0,	0.0,	0.0},
//			{0.0,	0.6,	0.7,	0.0,	0.0,	0.0},
//			{0.0,	0.1,	0.2,	0.7,	0.0,	0.0},
//			{0.0,	0.05,	0.0,	0.2,	0.9,	0.0},		
//			{0.0,	0.0,	0.06,	0.12,	0.18,	0.2}};


   public DavidandVinitParticleFilter() {
      _R = new Random(_seed);
      _particles = new HashMap<Product, Particle[]>();
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

      initializeParticlesFromFile("initUserParticles");
   }

   public void initializeParticlesFromFile(String filename) {
      int[][] allStates = new int[NUM_PARTICLES][UserState.values().length];

      /*
         * Parse Particle Log
         */
      BufferedReader input = null;
      try {
         input = new BufferedReader(new FileReader(filename));
         String line;
         int count = 0;
         while ((line = input.readLine()) != null) {
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
         if (count != NUM_PARTICLES) {
            System.out.println(count);
            throw new RuntimeException("Problem reading particle file");
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }


      for (Product prod : _products) {
         Particle[] particles = new Particle[NUM_PARTICLES];
         for (int i = 0; i < particles.length; i++) {
            Particle particle = new Particle(allStates[i]);
            particles[i] = particle;
         }
         _particles.put(prod, particles);
      }
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
   public int getPrediction(Product product, UserState userState) {
      int sum = 0;

      //Iterate over particles for that product, transition twice
      Particle[] particles = _particles.get(product);

      for (int i = 0; i < particles.length; i++) {
         int[] tempOldstates = particles[i].getState();

         double[] oldStates = new double[tempOldstates.length];

         for (int k = 0; k < tempOldstates.length; k++) {
            oldStates[k] = (double) tempOldstates[k];
         }

         Matrix oldStateMatrix = new Matrix(oldStates, oldStates.length);

         Matrix transition = new Matrix(estimated_transitions);
         Matrix newStateMatrix = transition.times(oldStateMatrix);

         //Need to check this
         sum += newStateMatrix.getRowPackedCopy()[userState.ordinal()];
      }

      sum /= particles.length;

      int toreturn = (int) sum;

      return toreturn;
   }

   public int getPrediction(Product product, UserState userState, int day) {
      int sum = 0;

      int times_to_use_matrix = day;

      //Iterate over particles for that product, transition twice
      Particle[] particles = _particles.get(product);

      for (int i = 0; i < particles.length; i++) {
         int[] tempOldstates = particles[i].getState();

         double[] oldStates = new double[tempOldstates.length];

         for (int k = 0; k < tempOldstates.length; k++) {
            oldStates[k] = (double) tempOldstates[k];
         }

         Matrix oldStateMatrix = new Matrix(oldStates, oldStates.length);

         Matrix transition = new Matrix(estimated_transitions);

         for (int j = 0; j < times_to_use_matrix; j++) {
            oldStateMatrix = transition.times(oldStateMatrix);
         }

         Matrix newStateMatrix = oldStateMatrix;

         //Need to check this
         sum += newStateMatrix.getRowPackedCopy()[userState.ordinal()];
      }

      sum /= particles.length;

      int toreturn = (int) sum;

      //System.out.println(toreturn);

      return toreturn;
   }

   @Override
   public boolean updateModel(Map<Query, Integer> totalImpressions) {

      //for each product
      for (Product product : _products) {
         //get particles
         Particle[] particles = _particles.get(product);

         //cumulative sum
         double[] cumulative_sum = new double[particles.length];

         RandomDataImpl randomData = new RandomDataImpl();

         Query query = new Query(product.getManufacturer(), product.getComponent());
         for (int i = 0; i < particles.length; i++) {
            //get particle
            Particle particle = particles[i];
            //weigh particles based on observation
            //How many F2 Queries were made by IS users? This is k
            int op = totalImpressions.get(query);
            int k = op - particle.getStateCount(UserState.F2);
            // Number of users in the IS state - this is n
            int n = particle.getStateCount(UserState.IS);
            double p = 1.0 / 3.0;
            if (k < 0.0) {
               particle.setWeight(0.0);
            } else {
               BinomialDistributionImpl binom = new BinomialDistributionImpl(n, p);
               double weight = binom.probability(k);
               particle.setWeight(weight);
            }

            if (i == 0) {
               cumulative_sum[i] = particle.getWeight();
            } else {
               cumulative_sum[i] = particle.getWeight() + cumulative_sum[i - 1];
            }
         }

         //resample (create new set of particles)
         Particle[] new_particles = new Particle[particles.length];

         Random random = new Random();

         //choose each new particle
         for (int i = 0; i < new_particles.length; i++) {
            //Get a random number between 0.0 and the cumulative sum
            double sample = random.nextDouble() * cumulative_sum[particles.length - 1];
            //Find the first value in cumulative sum that is greater than sample
            int index = 0;
            for (int j = 0; j < cumulative_sum.length; j++) {
               if (cumulative_sum[j] > sample) {
                  index = j;
                  break;
               }
            }
            //The particle we've chosen.
            Particle new_particle = particles[index];
            //These particles should be unweighted
            new_particle.setWeight(ParticleFilterAbstractUserModel.BASE_WEIGHT);

            //propagate

            //for each user state
            double[][] trans = new double[6][6];

            for (UserState from : UserState.values()) {

               //Number of users in that state
               int N = new_particle.getStateCount(from);

               //This is the sum of probabilities
               double sum = 0;

               //Determine probabilities
               for (UserState to : UserState.values()) {
                  double P = base_transitions[to.ordinal()][from.ordinal()];
                  //get probability of transition from "from" to "to"
                  //BinomialDistributionImpl bnd = new BinomialDistributionImpl(in_state,base_transitions[to.ordinal()][from.ordinal()]);

                  double probability = -1.0;


                  if (P > 0.0 && N > 0.0) {

                     if (from == UserState.F0 && to == UserState.T) {
                        while (probability > 1 || probability < 0) {
                           probability = randomData.nextGaussian(X, Math.sqrt(X * Y));
                        }

                     } else if (from == UserState.F1 && to == UserState.T) {
                        while (probability > 1 || probability < 0) {
                           probability = randomData.nextGaussian(2 * X, Math.sqrt(2 * X * Y));
                        }
                     } else if (from == UserState.F2 && to == UserState.T) {
                        while (probability > 1 || probability < 0) {
                           probability = randomData.nextGaussian(3 * X, Math.sqrt(3 * X * Y));
                        }
                     } else {
                        double mean = N * P;
                        double std = Math.sqrt(N * P * (1 - P));


                        while (probability > 1 || probability < 0) {
                           //System.out.println(P);
                           probability = randomData.nextGaussian(mean, std) / N;
                        }
                     }
                  } else {
                     probability = 0.0;
                  }
                  assert (probability >= 0 && probability <= 1);

                  sum += probability;

                  trans[to.ordinal()][from.ordinal()] = probability;

               }
               //Now we need to normalize
               for (UserState to : UserState.values()) {
                  if (sum != 0.0) {
                     trans[to.ordinal()][from.ordinal()] /= sum;
                  }
               }

            }
            Matrix transition = new Matrix(trans);

            //For debugging
            //System.out.println(transition.toString());

            // find new states
            int[] tempOldstates = new_particle.getState();

            double[] oldStates = new double[tempOldstates.length];

            for (int k = 0; k < tempOldstates.length; k++) {
               oldStates[k] = (double) tempOldstates[k];
            }


            Matrix oldStateMatrix = new Matrix(oldStates, oldStates.length);

            Matrix newStateMatrix = transition.times(oldStateMatrix);

            double[] tempNewStates = newStateMatrix.getRowPackedCopy();

            assert (tempNewStates.length == oldStates.length);

            int[] newStates = new int[tempNewStates.length];

            for (int k = 0; k < tempOldstates.length; k++) {
               newStates[k] = (int) tempNewStates[k];
            }

            int sum_of_users = 0;

            for (int h = 0; h < newStates.length; h++) {
               sum_of_users += newStates[h];
            }

            newStates[0] += (DavidandVinitParticleFilter.NUM_USERS_PER_PROD - sum_of_users);

            new_particle.setState(newStates);

            new_particles[i] = new_particle;
         }


         //store new particles
         _particles.put(product, new_particles);
      }

      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new DavidandVinitParticleFilter();
   }

   @Override
   public int getCurrentEstimate(Product product, UserState userState) {

      int toreturn = 0;

      //Iterate over particles for that product, return average value
      Particle[] particles = _particles.get(product);

      //System.out.println(particles.length);
      for (int i = 0; i < particles.length; i++) {
         toreturn += particles[i].getStateCount(userState);
      }

      toreturn /= particles.length;

      return toreturn;
   }

   public static void main(String[] args) {
      double[][] base_transitions = {{0.99, 0.05, 0.1, 0.1, 0.1, 0.8},
              {0.01, 0.2, 0.0, 0.0, 0.0, 0.0},
              {0.0, 0.6, 0.7, 0.0, 0.0, 0.0},
              {0.0, 0.1, 0.2, 0.7, 0.0, 0.0},
              {0.0, 0.05, 0.0, 0.2, 0.9, 0.0},
              {0.0, 0.0, 0.0, 0.0, 0.0, 0.2}};

      RandomDataImpl randomData = new RandomDataImpl();

      //Make an array of 1000 Particles.

      Particle[] particles = new Particle[NUM_PARTICLES];

      for (int p = 0; p < particles.length; p++) {
         particles[p] = new Particle();
      }

      //for each particle
      for (int p = 0; p < particles.length; p++) {
         //make transition matrix
         //for each user state
         double[][] trans = new double[6][6];

         for (UserState from : UserState.values()) {

            //This is the sum of probabilities
            double sum = 0;

            //Determine probabilities
            for (UserState to : UserState.values()) {
               double P = base_transitions[to.ordinal()][from.ordinal()];
               //get probability of transition from "from" to "to"
               //BinomialDistributionImpl bnd = new BinomialDistributionImpl(in_state,base_transitions[to.ordinal()][from.ordinal()]);

               double probability = -1.0;

               if (P > 0.0) {

                  double mean = P;
                  double std = Math.sqrt(P * (1 - P));


                  while (probability > 1 || probability < 0) {
                     probability = randomData.nextGaussian(mean, std);
                  }
               } else {
                  probability = 0.0;
               }
               assert (probability >= 0 && probability <= 1);

               sum += probability;

               trans[to.ordinal()][from.ordinal()] = probability;

            }
            //Now we need to normalize
            for (UserState to : UserState.values()) {
               trans[to.ordinal()][from.ordinal()] /= sum;
            }

         }
         Matrix transition = new Matrix(trans);

         int[] tempOldstates = particles[p].getState();

         double[] oldStates = new double[tempOldstates.length];

         for (int k = 0; k < tempOldstates.length; k++) {
            oldStates[k] = (double) tempOldstates[k];
         }

         Matrix oldStateMatrix = new Matrix(oldStates, oldStates.length);

         for (int j = 0; j < 5; j++) {
            oldStateMatrix = transition.times(oldStateMatrix);
            //oldStateMatrix.print(3, 5);
         }

         Matrix newStateMatrix = oldStateMatrix;

         double[] tempNewStates = newStateMatrix.getRowPackedCopy();

         int[] newStates = new int[tempNewStates.length];

         for (int k = 0; k < tempOldstates.length; k++) {
            newStates[k] = (int) tempNewStates[k];
         }

         int sum_of_users = 0;

         for (int h = 0; h < newStates.length; h++) {
            sum_of_users += newStates[h];
         }

         newStates[0] += (DavidandVinitParticleFilter.NUM_USERS_PER_PROD - sum_of_users);

         particles[p].setState(newStates);
      }

      DavidandVinitParticleFilter m = new DavidandVinitParticleFilter();
      try {
         m.saveParticlesToFile(particles);
      } catch (IOException e) {
         System.out.println("Couldn't write file.");
         e.printStackTrace();
      }

      //m.initializeParticlesFromFile("initParticles-5806165191807168463");
   }

}
