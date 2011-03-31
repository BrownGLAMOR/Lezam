package models.usermodel;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math.random.RandomDataImpl;

import java.io.*;
import java.util.*;

public class GregFilter extends ParticleFilterAbstractUserModel {

   private long _seed = 1263456;
   private Random _R;
   private ArrayList<Product> _products;


   public GregFilter() {
      _R = new Random(_seed);


      //Creates an array of partles
      Particle[] particles = new Particle[NUM_PARTICLES];

      //instantiates the array setting all particles in the NS state
      for (int i = 0; i < NUM_PARTICLES; ++i) {
         int[] start = {NUM_USERS_PER_PROD, 0, 0, 0, 0, 0};
         particles[i] = new Particle(start);
      }

      //Pushes particles for 5 days (without tranacting)
      particles = pushParticles(particles, 5, false, true);


      try {
         saveParticlesToFile(particles);
      } catch (IOException e) {
         System.out.println("Couldn't write file.");
         e.printStackTrace();
      }


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

      initializeParticlesFromFile("initParticles-5806165191807168463");
   }

   public void initializeParticlesFromFile(String filename) {
      //	System.out.println("I AM HERE NOW BHAOU BALKDJFALK");
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
      //System.out.println("I am called");
      FileWriter fstream = new FileWriter("initParticles" + _R.nextLong());
      BufferedWriter out = new BufferedWriter(fstream);
      String output = "";
      for (int i = 0; i < particles.length; i++) {
         output += particles[i].stateString() + "\n";
      }
      out.write(output);
      out.close();
   }

   public double factorial(double n) {
      if (n == 1 || n == 0) {
         return 1;

      } else {
         int total = 1;
         for (int i = 2; i < n + 1; ++n) {
            total *= i;

         }
         return total;
      }
   }

   public Particle[] pushParticles(Particle[] particles, int days, boolean transacting, boolean bursting) {

      //Creates a new set of particles
      Particle[] newParts = new Particle[particles.length];

      //sets x to proper value for transactions
      Random r = new Random();
      //if not transacting then x = 0
      double X = .06145;
      double Y = .35;

      //Creates a gaussian random number generator
      RandomDataImpl randMaker = new RandomDataImpl();

      //The number of users states
      int sizeOfUsers = UserState.values().length;

      //Instatiates the matrix of transitions
      double[][] transMatrix = {
              {.99, .05, .10, .10, .10, .80},
              {.01, .20, 0.0, 0.0, 0.0, 0.0},
              {0.0, .60, .70, 0.0, 0.0, 0.0},
              {0.0, .10, .20, .70, 0.0, 0.0},
              {0.0, .05, 0.0, .20, .90, 0.0},
              {0.0, 0.0, 0.0, 0.0, 0.0, .20}};


      double[][] tempMatrix = new double[sizeOfUsers][sizeOfUsers];
      double[][] markovMatrix = new double[sizeOfUsers][sizeOfUsers];
      double N;
      Particle curParticle;
      double curSum;
      double tempProb;
      for (int p = 0; p < particles.length; p++) {
         //For each particle
         newParts[p] = new Particle();


         for (int i = 0; i < days; ++i) {
            //for each day creates a new matrix to multiply

            if (i == 0) {
               curParticle = particles[p];
            } else {
               curParticle = newParts[p];
            }

            //refreshes the matrix
            for (int a = 0; a < sizeOfUsers; ++a) {
               for (int b = 0; b < sizeOfUsers; ++b) {
                  tempMatrix[a][b] = transMatrix[a][b];
               }
            }

            if (r.nextDouble() < .1 && bursting == true) {
               tempMatrix[0][0] = .8;
               tempMatrix[1][0] = .2;
            } else {
               tempMatrix[0][0] = .99;
               tempMatrix[1][0] = .01;
            }

            //System.out.println("early trans value is " + transMatrix[0][2]);

            if (transacting == true) {
               tempProb = -1;
               while (tempProb <= 0 || tempProb >= 1) {
                  tempProb = randMaker.nextGaussian(X, X * Y);
               }
               tempMatrix[5][2] = tempProb;
               tempProb = 1 - tempProb;
               //System.out.println(tempProb);
               //System.out.println("old trans value is " + transMatrix[0][2]);
               tempMatrix[0][2] = tempProb * transMatrix[0][2];
               //System.out.println("trans value is " + transMatrix[0][2]);
               tempMatrix[2][2] = tempProb * transMatrix[2][2];
               tempMatrix[3][2] = tempProb * transMatrix[3][2];

               tempProb = -1;

               while (tempProb <= 0 || tempProb >= 1) {
                  tempProb = randMaker.nextGaussian(2 * X, 2 * X * Y);
               }
               tempMatrix[5][3] = tempProb;
               tempProb = 1 - tempProb;
               //System.out.println(tempProb);
               tempMatrix[0][3] = tempProb * transMatrix[0][3];
               tempMatrix[3][3] = tempProb * transMatrix[3][3];
               tempMatrix[4][3] = tempProb * transMatrix[4][3];
               tempProb = -1;

               while (tempProb <= 0 || tempProb >= 1) {
                  tempProb = randMaker.nextGaussian(3 * X, 3 * X * Y);
               }
               tempMatrix[5][4] = tempProb;
               tempProb = 1 - tempProb;
               //System.out.println(tempProb);
               tempMatrix[0][4] = tempProb * transMatrix[0][4];
               tempMatrix[4][4] = tempProb * transMatrix[4][4];

            }

            /*for(int a = 0; a < sizeOfUsers; ++a){
                   for(int b = 0; b < sizeOfUsers; ++b){
                      System.out.print(tempMatrix[a][b] + "\t");

                   }
                   System.out.println();

                }
                System.out.println();*/


            for (UserState coming : UserState.values()) {
               //number of particles are in the state
               N = curParticle.getStateCount(coming);
               curSum = 0;
               for (UserState going : UserState.values()) {
                  tempProb = -1;
                  if (tempMatrix[going.ordinal()][coming.ordinal()] == 0 || N == 0) {
                     //sets it to 0 if has to be 0
                     markovMatrix[going.ordinal()][coming.ordinal()] = 0;
                  } else {
                     while (tempProb <= 0 || tempProb >= 1) {
                        //keep looking for a new value until it is in the range
                        tempProb = randMaker.nextGaussian(N * tempMatrix[going.ordinal()][coming.ordinal()], Math.sqrt(N * tempMatrix[going.ordinal()][coming.ordinal()] * (1.0 - tempMatrix[going.ordinal()][coming.ordinal()]))) / N;
                     }
                     //sums up the values
                     curSum += tempProb;
                     markovMatrix[going.ordinal()][coming.ordinal()] = tempProb;
                  }
               }

               //now need to renormalize
               for (UserState going : UserState.values()) {
                  if (markovMatrix[going.ordinal()][coming.ordinal()] == 0) {
                     markovMatrix[going.ordinal()][coming.ordinal()] = 0;
                  } else {
                     markovMatrix[going.ordinal()][coming.ordinal()] /= curSum;
                  }
               }
            }

            //multiplies out the matrix after recomputation of the markov matrix re-normalization
            //markovMatrix = matrixMultiplication(tempMatrix,markovMatrix);
            if (i == 0) {
               //setting the uninitialized particles from the previous ones
               newParts[p].setState(matrixInitify(matrixMultiplication(markovMatrix, curParticle.getState()), NUM_USERS_PER_PROD));
            } else {
               //setting newer particles withi new ones
               newParts[p].setState(matrixInitify(matrixMultiplication(markovMatrix, curParticle.getState()), NUM_USERS_PER_PROD));
            }
         }
         //while(true);
      }
      return newParts;
   }

   public Particle[] reSample(Particle[] particles) {
      Arrays.sort(particles, new particleComp());

      Random rand = new Random();

      Particle[] resampledParticles = new Particle[particles.length];

      //sums up total weight
      double totalSum = 0;
      for (int i = 0; i < particles.length; ++i) {
         totalSum += particles[i].getWeight();
      }

      //normalizes the weights
      double[] cumDensity = new double[particles.length];
      double cumSum = 0;
      double normalizedVal;
      for (int i = 0; i < particles.length; ++i) {
         normalizedVal = particles[i].getWeight() / totalSum;
         cumDensity[i] = cumSum;
         cumSum += normalizedVal;

      }


      double find;
      int a;
      //goes through and picks a particle randomly 1000 times
      for (int i = 0; i < particles.length; ++i) {
         find = rand.nextDouble();
         a = 0;
         while (find > cumDensity[a] && a < particles.length) {
            ++a;
         }
         //new unweighted particle
         resampledParticles[i] = new Particle(particles[a].getState());
      }


      return resampledParticles;
   }


   public double[] matrixMultiplication(double[][] M, int[] v) {
      double[] returnedVec = new double[M.length];
      assert (M[0].length == v.length);

      for (int i = 0; i < M.length; ++i) {
         double curSum = 0;
         for (int j = 0; j < M[0].length; ++j) {
            curSum += M[i][j] * (double) v[j];
         }
         returnedVec[i] = (int) curSum;
      }
      return returnedVec;

   }

   public int[] matrixInitify(double[] v, int total) {
      //transfer all data to a vector of ints that has excatly total in total
      int[] intVec = new int[v.length];
      int sum = 0;
      for (int i = 0; i < v.length; ++i) {
         intVec[i] = (int) Math.round(v[i]);
         sum += intVec[i];
      }

      intVec[0] = intVec[0] + total - sum;
      return intVec;
   }

   public class particleComp implements Comparator<Particle> {


      public int compare(Particle o1, Particle o2) {
         if (o2.getWeight() - o1.getWeight() > 0) {
            return 1;
         } else {
            return -1;
         }
      }

   }

   public double[][] matrixMultiplication(double[][] M1, double[][] M2) {
      //matrix multiplication doubles
      double[][] returnedMat = new double[M1.length][M2[0].length];
      int M = M1.length;
      int N = M2[0].length;

      assert (M1.length == M2[0].length && M1[0].length == M2.length); // see the comment above
      for (int j = 0; j < M; ++j) {
         for (int i = 0; i < N; ++i) {
            for (int k = 0; k < M1[0].length; ++k) {
               returnedMat[i][j] += M1[i][k] * M2[k][j];
            }

         }
      }
      return returnedMat;


   }

   @Override
   public int getPrediction(Product product, UserState userState) {
      /*
         * Don't worry about this until week 2
         */
      Particle[] transitionedParticle = pushParticles(_particles.get(product), 2, true, false);
      double totalSum = 0;
      //sums up the weighted average to get an estimate at a current position
      for (int i = 0; i < transitionedParticle.length; ++i) {
         totalSum += transitionedParticle[i].getState()[userState.ordinal()] * transitionedParticle[i].getWeight();
      }

      //System.out.println("Guess is sum " + totalSum + " for prodcut " + product + " in user state" + userState);
      return (int) Math.round(totalSum);
   }

   @Override
   public boolean updateModel(Map<Query, Integer> totalImpressions) {
      /*
         * Don't worry about this until week 2
         */

      Iterator<Query> qIterator = totalImpressions.keySet().iterator();

      //totalImpressions.get(new Query("",""));

      Product curProd;
      int n;
      double k;
      Iterator<Product> iterator = _particles.keySet().iterator();
      Particle particle;
      double op;
      double p = 1.0 / 3.0;
      while (iterator.hasNext()) {
         //updating all products
         curProd = iterator.next();
         Particle[] particles = _particles.get(curProd);

         //then number of REAL impressions for a given Manufactuer/component
         op = totalImpressions.get(new Query(curProd.getManufacturer(), curProd.getComponent()));

         //particles are reweighted
         for (int i = 0; i < particles.length; ++i) {
            particle = particles[i];

            //calculated number of F2 Users using particle
            k = op - particle.getStateCount(UserState.F2);

            //number of calculated IS users
            n = particle.getStateCount(UserState.IS);

            //if k < 0 we have a problem
            if (k < 0 || k > n) {
               particle.setWeight(0.0);
            } else {
               BinomialDistributionImpl binom = new BinomialDistributionImpl(n, p);
               double weight = binom.probability(k);
               particle.setWeight(weight);
            }
         }

         //resample particles
         Particle[] newParticles = reSample(particles);

         //push particles through the chain
         newParticles = pushParticles(newParticles, 1, true, true);

         _particles.put(new Product(curProd.getManufacturer(), curProd.getComponent()), newParticles);


      }
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new GregFilter();
   }

   @Override
   public int getCurrentEstimate(Product product, UserState userState) {

      //gets the particles for a corresponding product
      Particle[] myParticles = _particles.get(product);

      double totalSum = 0;
      //sums up the weighted average to get an estimate at a current position
      for (int i = 0; i < myParticles.length; ++i) {
         totalSum += myParticles[i].getState()[userState.ordinal()] * myParticles[i].getWeight();
      }

      //System.out.println("Estimation of sum is " + totalSum + " for prodcut " + product + " in user state" + userState);
      return (int) Math.round(totalSum);
   }


   public static void main(String[] args) {
      //Instantiates a particle filter
      new GregFilter();
   }

}
