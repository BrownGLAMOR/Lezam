/**
 * This is an abstract class for all versions of TacTex like user models
 */
package models.usermodel;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jberg
 */
public abstract class ParticleFilterAbstractUserModel extends AbstractModel {
   /**
    * Number of users per product type
    */
   public static final int NUM_USERS_PER_PROD = 10000;
   int DAY = -1;
   /**
    * Number of particles in the filter
    */
   public static final int NUM_PARTICLES = 1000;

   public static final double BASE_WEIGHT = 1.0 / NUM_PARTICLES;

   /**
    * These particles represent the current estimate of each of the user population
    */
   /*
     * Feel free to ignore this and the Particle class and use whatever
     * representation you want.  You could just as easily store all the particle
     * states in a multidimensional array and the weights in an array.
     */
   public HashMap<Product, Particle[]> _particles;

   /**
    * This will be called everyday and pass you the correct number of
    * total impressions for every query.  Specifically you should use
    * this information to update your current estimation in particles
    */
   public abstract boolean updateModel(Map<Query, Integer> totalImpressions);

   /**
    * When this method is called you should return you prediction for the user
    * distribution for the (product,state) combination, but it needs to be for
    * two days from the current day, because we are bidding for tomorrow and there
    * is one day of lag
    */
   public abstract int getPrediction(Product product, GameStatusHandler.UserState userState);

   /**
    * This method should return the current estimate for this
    * (product,state) combination given the particles
    */
   public abstract int getCurrentEstimate(Product product, GameStatusHandler.UserState userState);


   /**
    * This class represents a particle (i.e. state and weight)
    */
   public static class Particle {


      /**
       * This array represents the state of the particle.
       * The state vector has as many indices as
       * there are values in UserState
       */
      int[] _state;

      /**
       * This represents the weight of the particle
       */
      double _weight;

      /**
       * This maintains the history of whether or not
       * this particle bursted
       */
      ArrayList<Boolean> _burstHistory;

      /**
       * The default constructor initializes the particle to
       * have all users in the first state and the weight
       * to the base weight (i.e. the number that makes all
       * particles weight the same amount)
       */
      public Particle() {
         _state = new int[GameStatusHandler.UserState.values().length];
         _state[0] = NUM_USERS_PER_PROD;
         _weight = BASE_WEIGHT;
         _burstHistory = new ArrayList<Boolean>();
      }

      public Particle(int[] particle) {
         this(particle, BASE_WEIGHT);
      }

      public Particle(int[] particle, ArrayList<Boolean> burstHistory) {
         this(particle, BASE_WEIGHT, burstHistory);
      }

      public Particle(int[] particle, double weight) {
         if (particle.length != GameStatusHandler.UserState.values().length) {
            if (particle.length > GameStatusHandler.UserState.values().length) {
               throw new RuntimeException("Why are you passing me a particle if too many states");
            } else {
               throw new RuntimeException("Why are you passing me a particle if too few states");
            }
         }

         _state = new int[GameStatusHandler.UserState.values().length];
         int totalUsers = 0;
         for (int i = 0; i < _state.length; i++) {
            _state[i] = particle[i];
            totalUsers += particle[i];
         }
         _weight = weight;

         if (totalUsers != NUM_USERS_PER_PROD) {
            if (totalUsers > NUM_USERS_PER_PROD) {
               throw new RuntimeException("Why are you passing me a particle if too many users");
            } else {
               throw new RuntimeException("Why are you passing me a particle if too few users");
            }
         }

         _burstHistory = new ArrayList<Boolean>();
      }

      public Particle(int[] particle, double weight, ArrayList<Boolean> burstHistory) {
         if (particle.length != GameStatusHandler.UserState.values().length) {
            if (particle.length > GameStatusHandler.UserState.values().length) {
               throw new RuntimeException("Why are you passing me a particle if too many states");
            } else {
               throw new RuntimeException("Why are you passing me a particle if too few states");
            }
         }

         _state = new int[GameStatusHandler.UserState.values().length];
         int totalUsers = 0;
         for (int i = 0; i < _state.length; i++) {
            _state[i] = particle[i];
            totalUsers += particle[i];
         }
         _weight = weight;

         if (totalUsers != NUM_USERS_PER_PROD) {
            if (totalUsers > NUM_USERS_PER_PROD) {
               throw new RuntimeException("Why are you passing me a particle if too many users");
            } else {
               throw new RuntimeException("Why are you passing me a particle if too few users");
            }
         }

         _burstHistory = new ArrayList<Boolean>(burstHistory);
      }

      public String stateString() {
         String output = "";
         for (int i = 0; i < _state.length; i++) {
            output += _state[i] + " ";
         }
         output = output.substring(0, output.length() - 1);
         return output;
      }

      @Override
      public String toString() {
         String output = "" + _weight;
         for (int i = 0; i < _state.length; i++) {
            output += " " + _state[i];
         }
         return output;
      }

      public int[] getState() {
         return _state;
      }

      public void setState(int[] state) {
         if (state.length != _state.length) {
            throw new RuntimeException("Too many states");
         } else {
            for (int i = 0; i < state.length; i++) {
               _state[i] = state[i];
            }
         }
      }

      public int getStateCount(GameStatusHandler.UserState state) {
         return _state[state.ordinal()];
      }

      public double getWeight() {
         return _weight;
      }

      public void setWeight(double weight) {
         _weight = weight;
      }

      public ArrayList<Boolean> getBurstHistory() {
         return _burstHistory;
      }

      public void setBurstHistory(ArrayList<Boolean> burstHistory) {
         _burstHistory = new ArrayList<Boolean>(burstHistory);
      }

      public void addToBurstHistory(boolean burst) {
         _burstHistory.add(burst);
      }

   }


public void updateDay(int d) {
	DAY = d;
	System.out.println("DAY UPDATED: "+DAY+"_________________________________");
	
}
}