package models.usermodel;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import models.AbstractModel;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;


public class JakeParticleFilter extends ParticleFilterAbstractUserModel {

   private long _seed = 1119956;
   private Random _R;
   private ArrayList<Product> _products;
   private HashMap<UserState, HashMap<UserState, Double>> _transitions;
   private HashMap<UserState, HashMap<UserState, Double>> _burst_transitions;


   public JakeParticleFilter() {
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

      _transitions = new HashMap<UserState, HashMap<UserState, Double>>();

      _transitions.put(UserState.NS, new HashMap<UserState, Double>());
      _transitions.get(UserState.NS).put(UserState.NS, new Double(.99));
      _transitions.get(UserState.NS).put(UserState.IS, new Double(.01));

      _transitions.put(UserState.IS, new HashMap<UserState, Double>());
      _transitions.get(UserState.IS).put(UserState.NS, new Double(.05));
      _transitions.get(UserState.IS).put(UserState.IS, new Double(.2));
      _transitions.get(UserState.IS).put(UserState.F0, new Double(.6));
      _transitions.get(UserState.IS).put(UserState.F1, new Double(.1));
      _transitions.get(UserState.IS).put(UserState.F2, new Double(.05));

      _transitions.put(UserState.F0, new HashMap<UserState, Double>());
      _transitions.get(UserState.F0).put(UserState.NS, new Double(.1));
      _transitions.get(UserState.F0).put(UserState.F0, new Double(.7));
      _transitions.get(UserState.F0).put(UserState.F1, new Double(.2));

      _transitions.put(UserState.F1, new HashMap<UserState, Double>());
      _transitions.get(UserState.F1).put(UserState.NS, new Double(.1));
      _transitions.get(UserState.F1).put(UserState.F1, new Double(.7));
      _transitions.get(UserState.F1).put(UserState.F2, new Double(.2));

      _transitions.put(UserState.F2, new HashMap<UserState, Double>());
      _transitions.get(UserState.F2).put(UserState.NS, new Double(.1));
      _transitions.get(UserState.F2).put(UserState.F2, new Double(.9));

      _transitions.put(UserState.T, new HashMap<UserState, Double>());
      _transitions.get(UserState.T).put(UserState.NS, new Double(.8));
      _transitions.get(UserState.T).put(UserState.T, new Double(.2));

      _burst_transitions = new HashMap<UserState, HashMap<UserState, Double>>();

      _burst_transitions.put(UserState.NS, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.NS).put(UserState.NS, new Double(.80));
      _burst_transitions.get(UserState.NS).put(UserState.IS, new Double(.20));

      _burst_transitions.put(UserState.IS, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.IS).put(UserState.NS, new Double(.05));
      _burst_transitions.get(UserState.IS).put(UserState.IS, new Double(.2));
      _burst_transitions.get(UserState.IS).put(UserState.F0, new Double(.6));
      _burst_transitions.get(UserState.IS).put(UserState.F1, new Double(.1));
      _burst_transitions.get(UserState.IS).put(UserState.F2, new Double(.05));

      _burst_transitions.put(UserState.F0, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.F0).put(UserState.NS, new Double(.1));
      _burst_transitions.get(UserState.F0).put(UserState.F0, new Double(.7));
      _burst_transitions.get(UserState.F0).put(UserState.F1, new Double(.2));

      _burst_transitions.put(UserState.F1, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.F1).put(UserState.NS, new Double(.1));
      _burst_transitions.get(UserState.F1).put(UserState.F1, new Double(.7));
      _burst_transitions.get(UserState.F1).put(UserState.F2, new Double(.2));

      _burst_transitions.put(UserState.F2, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.F2).put(UserState.NS, new Double(.1));
      _burst_transitions.get(UserState.F2).put(UserState.F2, new Double(.9));

      _burst_transitions.put(UserState.T, new HashMap<UserState, Double>());
      _burst_transitions.get(UserState.T).put(UserState.NS, new Double(.8));
      _burst_transitions.get(UserState.T).put(UserState.T, new Double(.2));

      //comment these when generating particles
      initializeParticlesFromFile("initUserParticles");
      startGame();

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

      Particle[] tomorrow = new Particle[_particles.get(product).length];
      System.arraycopy(_particles.get(product), 0, tomorrow, 0, tomorrow.length);
      for (int i = 0; i < tomorrow.length; i++) {

         tomorrow[i] = transition(tomorrow[i]);

      }

      Particle[] nextday = new Particle[tomorrow.length];
      System.arraycopy(tomorrow, 0, nextday, 0, nextday.length);
      for (int i = 0; i < nextday.length; i++) {

         nextday[i] = transition(nextday[i]);

      }

      double sum = 0;
      for (int i = 0; i < nextday.length; i++) {

         //System.out.println(parts[i]);//.getStateCount(userState));
         sum += (double) nextday[i].getStateCount(userState);

      }
      return (int) (sum / (nextday.length));

   }

   public void makeNewParticles() {

      for (Product prod : _products) {
         double weightsum = 0;
         for (Particle p : _particles.get(prod)) {

            weightsum += p.getWeight();
         }
         //System.out.println("w " +weightsum);


         Particle[] newparticles = new Particle[_particles.get(prod).length];
         double[] weights = new double[newparticles.length];
         int i = 0;
         for (Particle p : _particles.get(prod)) {
            p.setWeight(p.getWeight() / weightsum);
            weights[i] = (i == 0 ? 0 : weights[i - 1]) + (p.getWeight() / weightsum);
            i++;
         }

//			for(int j = 0; j < newparticles.length; j++) {
//				System.out.println(weights[j]);
//			}


         for (int j = 0; j < 1000; j++) {
            double d = _R.nextDouble();
            for (int k = 0; k < weights.length; k++) {
               if (d < weights[k]) {
                  newparticles[j] = transition(_particles.get(prod)[k]);
                  break;
               }
            }
            if (newparticles[j] == null) {
               j--;
            }
         }

         /*	for(int j = 0; j < _particles.get(prod).length; j++) {

               double d = _R.nextDouble();
               for(int k = 0; k < weights.length; k++) {
                  if(d < weights[k]) {
                     newparticles[k] = transition(_particles.get(prod)[k]);
                     if(newparticles[k] == null) {
                        System.out.println("Whoa. " + _particles.get(prod)[k].toString() + ", at " + k);
                     }
                  } else {
                     newparticles[]
                  }
               }

            }	*/


         //System.out.println("weights: " + weights.length + "P: " + newparticles[0]);
         _particles.put(prod, newparticles);


      }

   }

   @Override
   public boolean updateModel(HashMap<Query, Integer> totalImpressions) {
      /*
         * Don't worry about this until week 2
         */
      for (Query q : totalImpressions.keySet()) {
         if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
            for (Particle p : _particles.get(new Product(q.getManufacturer(), q.getComponent()))) {
               double k = totalImpressions.get(q) - p.getStateCount(UserState.F2);

               //the probability that k is in fact the number of IS searchers, given than
               //totalImpressions.get(q) searches were made
               p.setWeight(1 / (Math.sqrt(p.getStateCount(UserState.IS) * .3333333) * .66666666)
                                   * 1 / Math.sqrt(2 * Math.PI)
                                   * Math.pow(Math.E, -.5 * Math.pow(
                       (k - p.getStateCount(UserState.IS) * .3333333) / (Math.sqrt(p.getStateCount(UserState.IS) * .3333333) * .66666666),
                       2)));
               /*String pr = "";
                    for (UserState s : UserState.values()) {
                       pr = pr + " " + s.toString() + ": " + p.getStateCount(s);
                    }
                    System.out.println(pr);*/

               //System.out.println("o: " + totalImpressions.get(q) + " p.F2: " + p.getStateCount(UserState.F2));
               //System.out.println("k: " + k + " IS/3: " + p.getStateCount(UserState.IS) * .333333333);
               //System.out.println("---");
               //p.setWeight()
               if (Double.isNaN(p.getWeight())) {
                  p.setWeight(0);
               }

               //System.out.println(p.getWeight());

            }
         }
      }

      makeNewParticles();

      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new JakeParticleFilter();
   }

   @Override
   public int getCurrentEstimate(Product product, UserState userState) {
      // TODO Auto-generated method stub

      Particle[] parts = _particles.get(product);
      //System.out.println(parts.length);

      double sum = 0;
      for (int i = 0; i < parts.length; i++) {

         //System.out.println(parts[i]);//.getStateCount(userState));
         sum += (double) parts[i].getStateCount(userState);

      }
      return (int) (sum / (parts.length));

   }


   //takes a number of users and a transition probability
   //returns a number that's kind of like how many users make that transition
   //but since they aren't independent it's an approximation, and then we normalize
   public int binomialTransition(int num, double pr) {

      int ret = Math.max(Math.min(10000, (int) Math.floor(_R.nextGaussian() * ((double) num * pr * (1 - pr)) + (double) num * pr)), 0);
      //System.out.println(ret);
      return ret;

   }

   //Takes the result of all the binomialTransitions for a state and how many users there are to apply it to
   //and divides those users up accordingly
   public void normalize(HashMap<UserState, Integer> h, int num) {

      int sum = 0;
      int distributedusers = 0;
      for (UserState key : h.keySet()) {
         sum += h.get(key);
      }
      for (UserState key : h.keySet()) {
         int amt = (int) ((double) h.get(key) / (double) sum * (double) num);
         h.put(key, amt);
         distributedusers += amt; //to keep track of rounding errors
      }

      h.put(UserState.NS, h.get(UserState.NS) + num - distributedusers); //put any excess in NS

   }

   public Particle transition(Particle p) {

      int[] newstate = new int[UserState.values().length];

      boolean burst = _R.nextDouble() > .9;

      for (UserState from : UserState.values()) {
         HashMap<UserState, Integer> t = new HashMap<UserState, Integer>();

         for (UserState to : UserState.values()) {
            try {
               t.put(to, binomialTransition(p.getStateCount(from), (burst ? _burst_transitions : _transitions).get(from).get(to)));
               //System.out.println(t.get(us).ge)
            } catch (NullPointerException e) {
               t.put(to, 0);
            }
         }

         normalize(t, p.getStateCount(from));
         for (int i = 0; i < UserState.values().length; i++) {
            newstate[i] += t.get(UserState.values()[i]);
            //System.out.println("new: " + newstate[i]);
         }
      }

      return new Particle(newstate);
   }

   //Run this after setup
   //Just adds transition to transacted prob.
   public void startGame() {
      _transitions.get(UserState.F0).put(UserState.T, .35 * 0.06145);
      _transitions.get(UserState.F1).put(UserState.T, .7 * 0.06145);
      _transitions.get(UserState.F2).put(UserState.T, 1.05 * 0.06145);

   }

   public static void main(String[] args) {


      JakeParticleFilter mpf = new JakeParticleFilter();
      Particle[] particles = new Particle[1000];
      for (int i = 0; i < 1000; i++) {
         particles[i] = new Particle();
         for (int j = 0; j < 5; j++) {
            particles[i] = mpf.transition(particles[i]);

         }
      }

      try {
         mpf.saveParticlesToFile(particles);
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      mpf.initializeParticlesFromFile("initParticles-6576212678036567411");
      //System.out.println(mpf.getCurrentEstimate(mpf._products.get(0), UserState.NS));
      //System.out.println(Double.isNaN(0.0/0.0));
      for (double i = 1; i < 5; i += .5) {
         System.out.println(1 / Math.sqrt(2)
                                    * 1 / Math.sqrt(2 * Math.PI)
                                    * Math.pow(Math.E, -.5 * Math.pow((i - 3) / 2, 2)));
      }
   }

}