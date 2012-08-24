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

import static simulator.parser.GameStatusHandler.UserState;


public class jbergParticleFilter extends ParticleFilterAbstractUserModel {

   private HashMap<UserState, HashMap<UserState, Double>> _standardProbs;
   private HashMap<UserState, HashMap<UserState, Double>> _burstProbs;
   private Random _R;
   private ArrayList<Product> _products;
   private HashSet<Query> _querySpace;
   private Particle[] initParticle;
   private static final double _burstProb = .1; //HC num
   private static final double _successiveBurstProb = .2; //HC num
   private double _convPrMult;
   private double _baseConvPr1, _baseConvPr2, _baseConvPr3;
   private double _convPrVar1, _convPrVar2, _convPrVar3; //multiply this by the baseConvPr
   private HashMap<Product, HashMap<UserState, Double>> _predictions, _currentEstimate;
   private HashMap<Query,double[]> _avgImps; 
   
   int _numSlots;
   int _promSlots;
   int rand2 = 4321;

   private static final boolean _rules2009 = false;

   public jbergParticleFilter(double[] convPr, int numSlots, int promSlots) {
      _convPrMult  = .2;//HC num
      _baseConvPr1 = convPr[0];
      _convPrVar1 = _baseConvPr1*_convPrMult;
      _baseConvPr2 = convPr[1];
      _convPrVar2 = _baseConvPr2*_convPrMult;
      _baseConvPr3 = convPr[2];
      _convPrVar3 = _baseConvPr3*_convPrMult;
      _numSlots = numSlots;
      _promSlots = promSlots;
      _standardProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _burstProbs = new HashMap<UserState, HashMap<UserState, Double>>();
      _R = new Random(rand2);
      _particles = new HashMap<Product, Particle[]>();

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

      standardFromNSProbs.put(UserState.NS, 0.99); //HC num  all of these are hard coded
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

      _querySpace = new HashSet<Query>();
      _querySpace.add(new Query(null, null));
      for (Product product : _products) {
         // The F1 query classes
         // F1 Manufacturer only
         _querySpace.add(new Query(product.getManufacturer(), null));
         // F1 Component only
         _querySpace.add(new Query(null, product.getComponent()));

         // The F2 query class
         _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
      }

//      initializeParticlesFromFile("/Users/jordanberg/Documents/workspace/Clients/src/resources/initUserParticles.txt");
      initializeParticlesFromFile("initUserParticles.txt");
      initAvgImps("avgImps.txt");
      updatePredictionMaps();
   }

   public void initializeParticlesFromFile(String filename) {
      int[][] allStates = new int[NUM_PARTICLES][UserState.values().length];

      /*
         * Parse Particle Log
         */
      int count = 0;
      try {
         BufferedReader input = new BufferedReader(new FileReader(filename));
         String line;

//         InputStream inputStream = this.getClass().getResourceAsStream(filename);
//         final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//         final BufferedReader input = new BufferedReader(inputStreamReader);
//         String line;
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
   
   //creates an _avgImps map from query to average number of imps
   //this is a hack to try to catch when the QA outputs bad/no information
   //
   //FIXME: This should probably be a map from day to average numImps
   public void initAvgImps(String filename)
   {
	   int numQueries = 16;
	   int numDays = 59;
	   _avgImps = new HashMap<Query,double[]>();
	   double [][] avgImps = new double[numDays][numQueries];
	   
	   int count = 0;
	   
	   try{
		   BufferedReader input = new BufferedReader(new FileReader(filename));
		   String line;
		   
		   
		   
		   while ((line = input.readLine()) != null && count < 59){
			   StringTokenizer st = new StringTokenizer(line, " ");
			   if (st.countTokens() == numQueries) {
				   //double[] avgImps = new double[numDays];
				   for(int i = 0; i < numQueries; ++i){
					   avgImps[count][i] = Double.parseDouble(st.nextToken());
				   }//end i iteraion
				   //_avgImps.put(queryList[count],avgImps);
			   }//end check line is right length
			   else{
				   System.out.println("-----WARNING-----");
				   System.out.println("In jbergParticleFilter::initAvgImps(.)");
				   System.out.println("Line is not the correct length");
				   break;
			   }//end else check line is right length
			   count++;
		   }//end while((line = input.readLine()....
	   }catch (FileNotFoundException e) {
		   e.printStackTrace();
	   } catch (IOException e) {
		   e.printStackTrace();
	   }
	   
	   Query[] queryList = new Query[numQueries];
	   
	   queryList[0] = new Query(null,null);
	   queryList[1] = new Query(null,"audio");
	   queryList[2] = new Query(null,"dvd");
	   queryList[3] = new Query ("lioneer","dvd");
	   queryList[4] = new Query ("flat","tv");
	   queryList[5] = new Query ("lioneer",null);
	   queryList[6] = new Query ("pg","dvd");
	   queryList[7] = new Query ("flat","dvd");
	   queryList[8] = new Query ("flat","audio");
	   queryList[9] = new Query ("lioneer","tv");
	   queryList[10] = new Query ("flat",null);
	   queryList[11] = new Query (null,"tv");
	   queryList[12] = new Query ("lioneer","audio");
	   queryList[13] = new Query ("pg","tv");
	   queryList[14] = new Query ("pg","audio");
	   queryList[15] = new Query ("pg",null);
	   
	   
	   for(int i = 0; i < numQueries; i++){
		   double[] a = new double[numDays];
		   for(int j = 0; j < numDays; j++){
			   a[j] = avgImps[j][i];
		   }
		   for(Query q : _querySpace){
			   if(q.getComponent() == queryList[i].getComponent() &&
				  q.getManufacturer() == queryList[i].getManufacturer()){
				   System.out.println("Q array: "+q+" "+Arrays.toString(a));
				   _avgImps.put(q,a);
			   }
		   }
		   
	   }
		   
   }//end initAvgImps

   private Object Query(Object object, Object object2) {
	// TODO Auto-generated method stub
	return null;
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
      ArrayList<Double> newWeights = new ArrayList<Double>();
      for (int i = 0; i < particles.length; i++) {
         Particle particle = particles[i];
         double weight = particle.getWeight();
         int[] state = particle.getState();
         double prob = getProbability(totalImpressions, state);
         double newWeight = weight * prob;
         totalWeight += newWeight;
         newWeights.add(newWeight);
         //			particle.setWeight(newWeight);
      }

      /*
         * Normalize Probabilities
         */
      if (totalWeight > 0) {
         for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            //				double newWeight = particle.getWeight()/totalWeight;
            particle.setWeight(newWeights.get(i) / totalWeight);
         }
      } else {
         for(int i = 0; i < particles.length; i++) {
            Particle particle = new Particle(initParticle[i].getState(),particles[i].getBurstHistory());
            particles[i] = particle;
         }
//         System.out.println("We had to reinitialize the particles...");
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
      double mean = n / 3.0; //HC num
      double sigma2 = mean * 2.0 / 3.0;//HC num
      double diff = k - mean;
      return 1.0 / Math.sqrt(2.0 * Math.PI * sigma2) * Math.exp(-(diff * diff) / (2.0 * sigma2));//HC num
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
   public boolean updateModel(Map<Query, Integer> totalImpressions) {
	   updateDay(DAY+1);
      for (Query q : _querySpace) {
         String man = q.getManufacturer();
         String comp = q.getComponent();
         if (man != null && comp != null) {
            Product prod = new Product(man, comp);
            Integer totalImps = totalImpressions.get(q);
            if (totalImps != null && totalImps > 0) {
            	//System.out.println("TOTAL IMPS:"+totalImps);
               Particle[] particles = _particles.get(prod);
               updateParticles(totalImps, particles);
               particles = resampleParticles(particles);
               _particles.put(prod, particles);
               updatePredictionMaps(prod, false);
            } else {
//               totalImps = 0;
//               totalImps += getCurrentEstimate(prod,UserState.F2);
//               totalImps += (1.0/3.0)*getCurrentEstimate(prod,UserState.IS);
//               Particle[] particles = _particles.get(prod);
//               updateParticles(totalImps, particles);
//               particles = resampleParticles(particles);
//               _particles.put(prod, particles);
                //updatePredictionMaps(prod, true);
            	
            	//only take average b/c particle filter 
            	//is agnostic to day. If it knew the day, we could condition on
            	//the row of the table.
            	double [] avgImps = _avgImps.get(q);
            	double avg =0.0;
            	if(DAY>58){
            		System.out.println("ERROR_____________________");
            		avg = avgImps[58];
            	}else{
            		avg = avgImps[DAY];
            	}
            	System.out.println("AVG: "+avg);
//            	double avg = 0.0;
//            	for(int k = 0; k < avgImps.length; ++k){
//            		avg+=avgImps[k];
//            	}
//            	avg/=avgImps.length;
            	
            	Particle[] particles = _particles.get(prod);
                //updateParticles(totalImps, particles);
            	updateParticles((int)Math.floor(avg), particles);
                particles = resampleParticles(particles);
                _particles.put(prod, particles);
                updatePredictionMaps(prod, false);
            }

            Particle[] particles = _particles.get(prod);
            pushParticlesForward(particles, prod);
            _particles.put(prod, particles);
         }
      }

      //Update c's
//      double[] c = new double[3];
//      for(Query q : _querySpace) {
//         if(q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) {
//            double convPr = 0.0;
//            double cf0 = 0.11;
//            double[] prViews = getPrView(q,_numSlots, _promSlots, _advertiserEffectBoundsAvg[0], _continuationProbBoundsAvg[0], cf0, _predictions);
//            for(Double prView : prViews) {
//               convPr += prView * _advertiserEffectBoundsAvg[0] * cf0;
//            }
//            c[0] += convPr;
//         }
//         else if(q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
//            double convPr = 0.0;
//            double cf1 = 0.23;
//            double[] prViews = getPrView(q,_numSlots, _promSlots, _advertiserEffectBoundsAvg[1], _continuationProbBoundsAvg[1], cf1, _predictions);
//            for(Double prView : prViews) {
//               convPr += prView * _advertiserEffectBoundsAvg[1] * cf1;
//            }
//            c[1] += convPr;
//         }
//         else {
//            double convPr = 0.0;
//            double cf2 = 0.36;
//            double[] prViews = getPrView(q,_numSlots, _promSlots, _advertiserEffectBoundsAvg[2], _continuationProbBoundsAvg[2], cf2, _predictions);
//            for(Double prView : prViews) {
//               convPr += prView * _advertiserEffectBoundsAvg[2] * cf2;
//            }
//            c[2] += convPr;
//         }
//      }
//
//      c[1] /= 6;
//      c[2] /= 9;
//
//      _baseConvPr1 = c[0];
//      _convPrVar1 = _baseConvPr1*_convPrMult;
//      _baseConvPr2 = c[1];
//      _convPrVar2 = _baseConvPr2*_convPrMult;
//      _baseConvPr3 = c[2];
//      _convPrVar3 = _baseConvPr3*_convPrMult;

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
               for (int j = 1; j <= 3 || j <= burstHistory.size(); j++) { //HC num
                  if (burstHistory.size() - j > 0) {
                     if (burstHistory.get(burstHistory.size() - j)) {
                        successiveBurst = true;
                        break;
                     }
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

         HashMap<UserState, Double> conversionProbs = new HashMap<UserState, Double>();
         conversionProbs.put(UserState.NS, 0.0);
         conversionProbs.put(UserState.F0, _baseConvPr1 * (1 + _convPrVar1 * _R.nextGaussian()));
         conversionProbs.put(UserState.F1, _baseConvPr2 * (1 + _convPrVar2 * _R.nextGaussian()));
         conversionProbs.put(UserState.F2, _baseConvPr3 * (1 + _convPrVar3 * _R.nextGaussian()));
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

         HashMap<UserState, Double> conversionProbs = new HashMap<UserState, Double>();
         conversionProbs.put(UserState.NS, 0.0);
         conversionProbs.put(UserState.F0, _baseConvPr1 * (1 + _convPrVar1 * _R.nextGaussian()));
         conversionProbs.put(UserState.F1, _baseConvPr2 * (1 + _convPrVar2 * _R.nextGaussian()));
         conversionProbs.put(UserState.F2, _baseConvPr3 * (1 + _convPrVar3 * _R.nextGaussian()));
         if (_rules2009) {
            conversionProbs.put(UserState.IS, (conversionProbs.get(UserState.F0) + conversionProbs.get(UserState.F1) + conversionProbs.get(UserState.F2)) / 3.0); //HC num
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

   private void updatePredictionMaps(Product prod, boolean onlyNonBurst) {
      Particle[] particles = _particles.get(prod);
      Particle[] particleCopy = new Particle[particles.length];

      HashMap<UserState, Double> estimates = new HashMap<UserState, Double>();
      double[] estimate = new double[UserState.values().length];

      double totalWeight = 0.0;
      for (int i = 0; i < particles.length; i++) {
         Particle particle = particles[i];
         if (!(onlyNonBurst &&
                       particle.getBurstHistory().size() > 0 &&
                       particle.getBurstHistory().get(particle.getBurstHistory().size() - 1) == true)) {
            totalWeight += particle.getWeight();
            for (UserState state : UserState.values()) {
               estimate[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
            }
         }
         particleCopy[i] = new Particle(particle.getState(), particle.getWeight(), particle.getBurstHistory());
      }

      for (UserState state : UserState.values()) {
         estimate[state.ordinal()] /= totalWeight;
      }

      for (int i = 0; i < estimate.length; i++) {
         estimates.put(UserState.values()[i], estimate[i]);
      }

      _currentEstimate.put(prod, estimates);

      pushParticlesForward(particleCopy, prod, false);
      pushParticlesForward(particleCopy, prod, false);

      HashMap<UserState, Double> estimatesPred = new HashMap<UserState, Double>();
      double[] estimatePred = new double[UserState.values().length];

      for (int i = 0; i < particleCopy.length; i++) {
         Particle particle = particleCopy[i];
         for (UserState state : UserState.values()) {
            estimatePred[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
         }
      }

      for (int i = 0; i < estimatePred.length; i++) {
         estimatesPred.put(UserState.values()[i], estimatePred[i]);
      }

      _predictions.put(prod, estimatesPred);
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
      return new jbergParticleFilter(new double[] {_baseConvPr1, _baseConvPr2, _baseConvPr3}, _numSlots, _promSlots);
   }

}
