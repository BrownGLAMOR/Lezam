package models.usermodel;

// My Particle Filter
// David Lapayowker
// March 12, 2010

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;


public class DavidLParticleFilter extends TacTexAbstractUserModel {

	private long _seed = 1263456;
	private Random _R;
	private ArrayList<Product> _products;
	private HashMap<Product, models.usermodel.TacTexAbstractUserModel.Particle[]> _next;
	private HashMap<Product, models.usermodel.TacTexAbstractUserModel.Particle[]> _burst;


	public DavidLParticleFilter() {
		_R = new Random(_seed);
		_particles = new HashMap<Product,models.usermodel.TacTexAbstractUserModel.Particle[]>();
		_products = new ArrayList<Product>();
		_next = new HashMap<Product,models.usermodel.TacTexAbstractUserModel.Particle[]>();
		_burst = new HashMap<Product, models.usermodel.TacTexAbstractUserModel.Particle[]>();
		
		_products.add(new Product("flat", "dvd"));
		_products.add(new Product("flat", "tv"));
		_products.add(new Product("flat", "audio"));
		_products.add(new Product("pg", "dvd"));
		_products.add(new Product("pg", "tv"));
		_products.add(new Product("pg", "audio"));
		_products.add(new Product("lioneer", "dvd"));
		_products.add(new Product("lioneer", "tv"));
		_products.add(new Product("lioneer", "audio"));
		
		initializeParticlesFromFile("/Users/jordanberg/Documents/workspace/Clients/initUserParticles");
//		initializeParticlesDefault();
	}
	
	/*
	 * Fills in the Hash Map with default particles for each map, and 
	 * propagates the particles over 5 days with no conversions
	 * 
	 */
	public void initializeParticlesDefault() {
		int s = _products.size();
		for(int i = 0; i < s; ++i) {
			Particle[] temp = new Particle[1000];
			for(int j = 0; j < 1000; ++j) {
				temp[j] = new Particle();
			}
			_particles.put(_products.get(i),temp);
		}
		int states = UserState.values().length;
		int NS = 0;
		int IS = 1;
		int F0 = 2;
		int F1 = 3;
		int F2 = 4;
		int T = 5;

		// now, for 5 days, iterate through all of the products, and then through
		// all the particles, moving a random number of users between each position
		// where the probability of each transition is uniform
		// for simplicity's sake, and because it doesn't hit time too hard, we're
		// going to keep this as simulation
		for(int day = 0; day < 5; ++day) {
			for(int i = 0; i < s; ++i) {
				Product current = _products.get(i);
				Particle[] parts = _particles.get(current);
				for(int j = 0; j < 1000; ++j) {
					Particle curr = parts[j];
					int[] users = curr.getState();
					int[] nextStep = new int[states];
					// initialize to 0, just in case
					for(int a = 0; a < states; ++a)
						nextStep[a] = 0;
					// NS state
					int custs = users[NS];
					/*
					  Strategy: for each state, determine the probabilities of moving
					  into the other possible states, p1 through pk.  Generate a 
					  uniform RV X, and find the min k such that \sum_{i=1}^k p_i \geq X,
					  and move it into the corresponding state
					 */
					float r = 0;
					for(int k = 0; k < custs; ++k) {
						r = _R.nextFloat();
						if(r <=0.1) { // burst; hopefully this will improve things
							r = _R.nextFloat();
						    if(r <= 0.8)
						    	nextStep[NS] += 1;
						    else
						    	nextStep[IS] += 1;
						}
						else {
						    r = _R.nextFloat();
						    if(r <= 0.99)
						    	nextStep[NS] += 1;
						    else
						    	nextStep[IS] += 1;
						}
					}
					custs = users[IS];
					for(int k = 0; k < custs; ++k) {
					    r = _R.nextFloat();
					    if(r <= 0.05)
					    	nextStep[NS] += 1;
					    else if(r <= .25)
					    	nextStep[IS] += 1;
					    else if(r <= .85)
					    	nextStep[F0] += 1;
					    else if(r <= .95)
					    	nextStep[F1] += 1;
					    else
					    	nextStep[F2] += 1;
					}
					custs = users[F0];
					for(int k = 0; k < custs; ++k) {
					    r = _R.nextFloat();
					    if(r <= .1)
					    	nextStep[NS] += 1;
					    else if(r<=0.8)
					    	nextStep[F0] += 1;
					    else
					    	nextStep[F1] += 1;
					}
					custs = users[F1];
					for(int k = 0; k < custs; ++k) {
					    r = _R.nextFloat();
					    if(r <= .1)
					    	nextStep[NS] += 1;
					    else if(r <= .8)
					    	nextStep[F1] += 1;
					    else
					    	nextStep[F2] += 1;
					    }
					custs = users[F2];
					for(int k = 0; k < custs; ++k) {
					    r = _R.nextFloat();
					    if(r <= .1)
					    	nextStep[NS] += 1;
					    else
					    	nextStep[F2] += 1;
					}
					curr.setState(nextStep);
					parts[j] = curr;
				}
				_particles.put(current,parts);

			}
		}
		
		// save them to a file
		for(int i = 0; i < _products.size(); ++i) {
			Product p = _products.get(i);
			Particle[] toWrite = _particles.get(p);
			try {
				saveParticlesToFile(toWrite);
			}
			catch(IOException e) {
				System.err.println("Error saving the data!");
			}
		}
		// now we need to make the prediction particles for the next day
		for(int i = 0; i < s; ++i) {
			Product current = _products.get(i);
			Particle[] parts = _particles.get(current);
			int numParts = parts.length;
			Particle[] predParts = new Particle[numParts];
			for(int j = 0; j < numParts; ++j)
				predParts[j] = new Particle();
			// iterating over all the particles in that product
			for(int j = 0; j < numParts; ++j) {
				Particle curr = parts[j];
				int[] users = curr.getState();
				double[][] transition = new double[states][states];
				for(int l = 0; l < states; ++l) {
					for(int k = 0; k < states; ++k)
						transition[l][k] = 0;
				}
				transition[NS][NS] = genRV(0.99);
				transition[NS][IS] = genRV(0.01);
				normalizeVector(transition[NS]);
				transition[IS][NS] = genRV(0.05);
				transition[IS][IS] = genRV(0.2);
				transition[IS][F0] = genRV(0.6);
				transition[IS][F1] = genRV(0.1);
				transition[IS][F2] = genRV(0.05);
				normalizeVector(transition[IS]);
				transition[F0][NS] = genRV(0.1);
				transition[F0][F0] = genRV(0.7);
				transition[F0][F1] = genRV(0.2);
				normalizeVector(transition[F0]);
				transition[F1][NS] = genRV(0.1);
				transition[F1][F1] = genRV(0.7);
				transition[F1][NS] = genRV(0.2);
				normalizeVector(transition[F1]);
				transition[F2][NS] = genRV(0.1);
				transition[F2][F2] = genRV(0.9);
				normalizeVector(transition[F2]);
				transition[T][NS] = genRV(0.8);
				transition[T][T] = genRV(0.2);
				normalizeVector(transition[T]);
				// first, transition to transacted
				int transactors = 0;
				double x = 0.06145;
				double y = 0.35;
				double prob = (_R.nextGaussian()*x*y)+x;
				int move = (int)(users[F0]*prob);
				users[F0] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*2*x*y)+2*x;
				move = (int)(users[F1]*prob);
				users[F1] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*3*x*y)+3*x;
				move = (int)(users[F2]*prob);
				users[F1] -= move;
				transactors += move;
				// now transition the rest
				int[] nextState = new int[states];
				nextState[T] = 0;
				int sum = 0;
				for(int k = 0; k < states; ++k) {
					sum = 0;
					for(int l = 0; l < states; ++l) {
						sum += (int)(users[l]*transition[l][k]);
					}
					nextState[k] = sum;
				}
				nextState[T] += transactors;
				// we're going to hack our way around the issue of the number of users, by
				// constructing a CDF of the distribution of users, and adding/removing people one at
				// a time in a manner consistent with the CDF
				sum = 0;
				for(int a = 0; a < nextState.length; ++a)
					sum += nextState[a];
				double r1 = nextState[NS]/10000.0;
				double r2 = r1 + nextState[IS]/10000.0;
				double r3 = r2 + nextState[F0]/10000.0;
				double r4 = r3 + nextState[F1]/10000.0;
				double r5 = r4 + nextState[F2]/10000.0;
				while(sum < 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]++;
					else if(r < r2)
						nextState[IS]++;
					else if(r < r3)
						nextState[F0]++;
					else if(r<r4)
						nextState[F1]++;
					else if(r < r5)
						nextState[F2]++;
					else
						nextState[T]++;
					++sum;
				}
				while(sum > 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]--;
					else if(r < r2)
						nextState[IS]--;
					else if(r < r3)
						nextState[F0]--;
					else if(r<r4)
						nextState[F1]--;
					else if(r < r5)
						nextState[F2]--;
					else
						nextState[T]--;
					--sum;
				}
				predParts[j].setState(nextState);
			}
			_next.put(current, predParts);
		}
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
				StringTokenizer st = new StringTokenizer(line," ");
				if(st.countTokens() == UserState.values().length) {
					for(int i = 0; i < UserState.values().length; i++) {
						allStates[count][i] = Integer.parseInt(st.nextToken());
					}
				}
				else {
					break;
				}
				count++;
			}
			if(count != NUM_PARTICLES) {
				throw new RuntimeException("Problem reading particle file");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		for(Product prod : _products) {
			Particle[] particles = new Particle[NUM_PARTICLES];
			for(int i = 0; i < particles.length; i++) {
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
		for(int i = 0; i < particles.length; i++) {
			output += particles[i].stateString() + "\n";
		}
		out.write(output);
		out.close();
	}
	
	public void normalizeVector(double[] vals) {
		double sum = 0;
		int s = vals.length;
		for(int i = 0; i < s; ++i) {
			sum += vals[i];
		}
		for(int i = 0; i < s; ++i) {
			vals[i] /= sum;
		}
	}

	@Override
	public int getPrediction(Product product, UserState userState) {
		Particle[] parts = _next.get(product);
		int numParts = parts.length;
		int index = 0;
		int total = 0;
		switch(userState) {
			case NS: index = 0; break;
			case IS: index = 1; break;
			case F0: index = 2; break;
			case F1: index = 3; break;
			case F2: index = 4; break;
			case T: index = 5; break;
		}
		for(int i = 0; i < numParts; ++i) {
			int[] users = parts[i].getState();
			total += users[index];
		}
		return total/numParts;
		
	}
	
	// just go over all of the particles for this product, average the numbers
	// in each state, return that average (since they should be unweighted)
	public int getCurrentEstimate(Product product, UserState userState) {
		Particle[] parts = _particles.get(product);
		int total = 0;
		int numParts = parts.length;
		int index = 0;
		switch(userState) {
			case NS: index = 0; break;
			case IS: index = 1; break;
			case F0: index = 2; break;
			case F1: index = 3; break;
			case F2: index = 4; break;
			case T: index = 5; break;
		}
		for(int i = 0; i < numParts; ++i) {
			int[] users = parts[i].getState();
			total += users[index];
		}
		return total/numParts;
	}
	
	/*
	 * A wrapper around generating normal RVs for the Markov matrix,
	 * making sure that the values generated are between zero and one
	 */
	public double genRV(double p) {
		double r = (_R.nextGaussian()*(10000*p*(1-p))+10000*p)/10000;
		while(r >= 1 || r < 0) {
			r = (_R.nextGaussian()*(10000*p*(1-p))+10000*p)/10000;
		}
		return r;
	}

	@Override
	/*
	 * We have the total number of impressions for each query type, so we 
	 * can use this to update our model.  Check the order of whether an
	 * update or a probability adjustment should happen first (I think it's
	 * the update)
	 */
	public boolean updateModel(HashMap<Query, Integer> totalImpressions) {
		int s = _products.size();
		int states = UserState.values().length;
		int NS = 0;
		int IS = 1;
		int F0 = 2;
		int F1 = 3;
		int F2 = 4;
		int T = 5;
		// first, the bursting particles
		for(int i = 0; i < s; ++i) {
			Product current = _products.get(i);
			Particle[] parts = _particles.get(current);
			int numParts = parts.length;
			Particle[] burstParts = new Particle[numParts];
			for(int j = 0; j < numParts; ++j)
				burstParts[j] = new Particle();
			// iterating over all the particles in that product
			for(int j = 0; j < numParts; ++j) {
				Particle curr = parts[j];
				int[] users = curr.getState();
				double[][] transition = new double[states][states];
				for(int l = 0; l < states; ++l) {
					for(int k = 0; k < states; ++k)
						transition[l][k] = 0;
				}
				transition[NS][NS] = genRV(0.8);
				transition[NS][IS] = genRV(0.2);
				normalizeVector(transition[NS]);
				transition[IS][NS] = genRV(0.05);
				transition[IS][IS] = genRV(0.2);
				transition[IS][F0] = genRV(0.6);
				transition[IS][F1] = genRV(0.1);
				transition[IS][F2] = genRV(0.05);
				normalizeVector(transition[IS]);
				transition[F0][NS] = genRV(0.1);
				transition[F0][F0] = genRV(0.7);
				transition[F0][F1] = genRV(0.2);
				normalizeVector(transition[F0]);
				transition[F1][NS] = genRV(0.1);
				transition[F1][F1] = genRV(0.7);
				transition[F1][NS] = genRV(0.2);
				normalizeVector(transition[F1]);
				transition[F2][NS] = genRV(0.1);
				transition[F2][F2] = genRV(0.9);
				normalizeVector(transition[F2]);
				transition[T][NS] = genRV(0.8);
				transition[T][T] = genRV(0.2);
				normalizeVector(transition[T]);
				// first, transition to transacted
				int transactors = 0;
				double x = 0.06145;
				double y = 0.35;
				double prob = (_R.nextGaussian()*x*y)+x;
				int move = (int)(users[F0]*prob);
				users[F0] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*2*x*y)+2*x;
				move = (int)(users[F1]*prob);
				users[F1] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*3*x*y)+3*x;
				move = (int)(users[F2]*prob);
				users[F1] -= move;
				transactors += move;
				// now transition the rest
				int[] nextState = new int[states];
				nextState[T] = 0;
				int sum = 0;
				for(int k = 0; k < states; ++k) {
					sum = 0;
					for(int l = 0; l < states; ++l) {
						sum += (int)(users[l]*transition[l][k]);
					}
					nextState[k] = sum;
				}
				nextState[T] += transactors;
				// we're going to hack our way around the issue of the number of users, by
				// constructing a CDF of the distribution of users, and adding/removing people one at
				// a time in a manner consistent with the CDF
				sum = 0;
				for(int a = 0; a < nextState.length; ++a)
					sum += nextState[a];
				double r1 = nextState[NS]/10000.0;
				double r2 = r1 + nextState[IS]/10000.0;
				double r3 = r2 + nextState[F0]/10000.0;
				double r4 = r3 + nextState[F1]/10000.0;
				double r5 = r4 + nextState[F2]/10000.0;
				while(sum < 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]++;
					else if(r < r2)
						nextState[IS]++;
					else if(r < r3)
						nextState[F0]++;
					else if(r<r4)
						nextState[F1]++;
					else if(r < r5)
						nextState[F2]++;
					else
						nextState[T]++;
					++sum;
				}
				while(sum > 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]--;
					else if(r < r2)
						nextState[IS]--;
					else if(r < r3)
						nextState[F0]--;
					else if(r<r4)
						nextState[F1]--;
					else if(r < r5)
						nextState[F2]--;
					else
						nextState[T]--;
					--sum;
				}
				burstParts[j].setState(nextState);
				String comp = current.getComponent();
				String man = current.getManufacturer();
				int op = totalImpressions.get(new Query(man,comp));
				
				int spf2 = nextState[F2];
				int k = op - spf2;
				int n = nextState[IS];
				double m = 1.0/3*n;
				double sig = 2.0/9*n;
				double p = Phi((k-m)/sig+0.5) - Phi((k-0.5-m)/sig-0.5);
				if(p < 0.00001) {
					p = 0.00001;
				}
				burstParts[j].setWeight(p);
			}
			_burst.put(current, burstParts);
		}
		
		// now, the normal particles
		for(int i = 0; i < s; ++i) {
			Product current = _products.get(i);
			Particle[] parts = _particles.get(current);
			int numParts = parts.length;
			Particle[] nextParts = new Particle[numParts];
			for(int j = 0; j < numParts; ++j)
				nextParts[j] = new Particle();
			// iterating over all the particles in that product
			for(int j = 0; j < numParts; ++j) {
				Particle curr = parts[j];
				int[] users = curr.getState();
				double[][] transition = new double[states][states];
				for(int l = 0; l < states; ++l) {
					for(int k = 0; k < states; ++k)
						transition[l][k] = 0;
				}
				transition[NS][NS] = genRV(0.99);
				transition[NS][IS] = genRV(0.01);
				normalizeVector(transition[NS]);
				transition[IS][NS] = genRV(0.05);
				transition[IS][IS] = genRV(0.2);
				transition[IS][F0] = genRV(0.6);
				transition[IS][F1] = genRV(0.1);
				transition[IS][F2] = genRV(0.05);
				normalizeVector(transition[IS]);
				transition[F0][NS] = genRV(0.1);
				transition[F0][F0] = genRV(0.7);
				transition[F0][F1] = genRV(0.2);
				normalizeVector(transition[F0]);
				transition[F1][NS] = genRV(0.1);
				transition[F1][F1] = genRV(0.7);
				transition[F1][NS] = genRV(0.2);
				normalizeVector(transition[F1]);
				transition[F2][NS] = genRV(0.1);
				transition[F2][F2] = genRV(0.9);
				normalizeVector(transition[F2]);
				transition[T][NS] = genRV(0.8);
				transition[T][T] = genRV(0.2);
				normalizeVector(transition[T]);
				// first, transition to transacted
				int transactors = 0;
				double x = 0.06145;
				double y = 0.35;
				double prob = (_R.nextGaussian()*x*y)+x;
				int move = (int)(users[F0]*prob);
				users[F0] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*2*x*y)+2*x;
				move = (int)(users[F1]*prob);
				users[F1] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*3*x*y)+3*x;
				move = (int)(users[F2]*prob);
				users[F1] -= move;
				transactors += move;
				// now transition the rest
				int[] nextState = new int[states];
				nextState[T] = 0;
				int sum = 0;
				for(int k = 0; k < states; ++k) {
					sum = 0;
					for(int l = 0; l < states; ++l) {
						sum += (int)(users[l]*transition[l][k]);
					}
					nextState[k] = sum;
				}
				nextState[T] += transactors;
				// we're going to hack our way around the issue of the number of users, by
				// constructing a CDF of the distribution of users, and adding/removing people one at
				// a time in a manner consistent with the CDF
				sum = 0;
				for(int a = 0; a < nextState.length; ++a)
					sum += nextState[a];
				double r1 = nextState[NS]/10000.0;
				double r2 = r1 + nextState[IS]/10000.0;
				double r3 = r2 + nextState[F0]/10000.0;
				double r4 = r3 + nextState[F1]/10000.0;
				double r5 = r4 + nextState[F2]/10000.0;
				while(sum < 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]+=1;
					else if(r < r2)
						nextState[IS]+=1;
					else if(r < r3)
						nextState[F0]+=1;
					else if(r<r4)
						nextState[F1]+=1;
					else if(r < r5)
						nextState[F2]+=1;
					else
						nextState[T]+=1;
					++sum;
				}
				while(sum > 10000) {
					double r = _R.nextDouble();
					if(r < r1)
						nextState[NS]-=1;
					else if(r < r2)
						nextState[IS]-=1;
					else if(r < r3)
						nextState[F0]-=1;
					else if(r<r4)
						nextState[F1]-=1;
					else if(r < r5)
						nextState[F2]-=1;
					else
						nextState[T]-=1;
					--sum;
				}
				nextParts[j].setState(nextState);
				String comp = current.getComponent();
				String man = current.getManufacturer();
				int op = totalImpressions.get(new Query(man,comp));
				
				int spf2 = nextState[F2];
				int k = op - spf2;
				int n = nextState[IS];
				double m = 1.0/3*n;
				double sig = 2.0/9*n;
				double p = Phi((k-m)/sig+0.5) - Phi((k-0.5-m)/sig-0.5);
				if(p < 0.0001) {
					p = 0.0001;
				}
				nextParts[j].setWeight(p);
			}
			_particles.put(current, nextParts);
			// We will now resample with either this set or the burst
			// particles, depending upon whether or not we have over 750 results
			String comp = current.getComponent();
			String man = current.getManufacturer();
			int op = totalImpressions.get(new Query(man,comp));
			if(op > 750) {
				// use the burst particles instead
				_particles.put(current,_burst.get(current));
			}
			nextParts = _particles.get(current);
			// need this for normalization!
			double totalProb = 0;
			for(int a = 0; a < 1000; ++a)
				totalProb += nextParts[a].getWeight();
			double[] cumProb = new double[numParts];
			double curr = 0;
			for(int a = 0; a < numParts;++a) {
				curr += (nextParts[a].getWeight())/totalProb;
				cumProb[a] = curr;
			}
			double r = 0;
			Particle[] resampled = new Particle[numParts];
			for(int a = 0; a < 1000; ++a)
				resampled[a] = new Particle();
			for(int a = 0; a < 1000; ++a) {
				r = _R.nextDouble();
				boolean found = false;
				// need to search manually, since binary search failed badly
				for(int b = 0; b < 999; ++b) {
					if(cumProb[b] <= r && r < cumProb[b+1]) {
						resampled[a].setState(nextParts[b].getState());
						found = true;
						break;
					}
				}
				if(!found) {
					resampled[a].setState(nextParts[999].getState());
				}
			}
			_particles.put(current, resampled);
		}
		
		// next we're going to build the prediction particles
		for(int i = 0; i < s; ++i) {
			Product current = _products.get(i);
			Particle[] parts = _particles.get(current);
			int numParts = parts.length;
			Particle[] predParts = new Particle[numParts];
			for(int j = 0; j < numParts; ++j)
				predParts[j] = new Particle();
			// iterating over all the particles in that product
			for(int j = 0; j < numParts; ++j) {
				Particle curr = parts[j];
				int[] users = curr.getState();
				double[][] transition = new double[states][states];
				for(int l = 0; l < states; ++l) {
					for(int k = 0; k < states; ++k)
						transition[l][k] = 0;
				}
				double r = _R.nextDouble();
				if(r < 0.05) { // dropping the prob of bursting a bit
					transition[NS][NS] = genRV(0.8);
					transition[NS][IS] = genRV(0.2);
				}
				else {
					transition[NS][NS] = genRV(0.99);
					transition[NS][IS] = genRV(0.01);
				}
				normalizeVector(transition[NS]);
				transition[IS][NS] = genRV(0.05);
				transition[IS][IS] = genRV(0.2);
				transition[IS][F0] = genRV(0.6);
				transition[IS][F1] = genRV(0.1);
				transition[IS][F2] = genRV(0.05);
				normalizeVector(transition[IS]);
				transition[F0][NS] = genRV(0.1);
				transition[F0][F0] = genRV(0.7);
				transition[F0][F1] = genRV(0.2);
				normalizeVector(transition[F0]);
				transition[F1][NS] = genRV(0.1);
				transition[F1][F1] = genRV(0.7);
				transition[F1][NS] = genRV(0.2);
				normalizeVector(transition[F1]);
				transition[F2][NS] = genRV(0.1);
				transition[F2][F2] = genRV(0.9);
				normalizeVector(transition[F2]);
				transition[T][NS] = genRV(0.8);
				transition[T][T] = genRV(0.2);
				normalizeVector(transition[T]);
				// first, transition to transacted
				int transactors = 0;
				double x = 0.06145;
				double y = 0.35;
				double prob = (_R.nextGaussian()*x*y)+x;
				int move = (int)(users[F0]*prob);
				users[F0] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*2*x*y)+2*x;
				move = (int)(users[F1]*prob);
				users[F1] -= move;
				transactors += move;
				prob = (_R.nextGaussian()*3*x*y)+3*x;
				move = (int)(users[F2]*prob);
				users[F1] -= move;
				transactors += move;
				// now transition the rest
				int[] nextState = new int[states];
				nextState[T] = 0;
				int sum = 0;
				for(int k = 0; k < states; ++k) {
					sum = 0;
					for(int l = 0; l < states; ++l) {
						sum += (int)(users[l]*transition[l][k]);
					}
					nextState[k] = sum;
				}
				nextState[T] += transactors;
				// we're going to hack our way around the issue of the number of users, by
				// constructing a CDF of the distribution of users, and adding/removing people one at
				// a time in a manner consistent with the CDF
				sum = 0;
				for(int a = 0; a < nextState.length; ++a)
					sum += nextState[a];
				double r1 = nextState[NS]/10000.0;
				double r2 = r1 + nextState[IS]/10000.0;
				double r3 = r2 + nextState[F0]/10000.0;
				double r4 = r3 + nextState[F1]/10000.0;
				double r5 = r4 + nextState[F2]/10000.0;
				while(sum < 10000) {
					r = _R.nextDouble();
					if(r < r1)
						nextState[NS]++;
					else if(r < r2)
						nextState[IS]++;
					else if(r < r3)
						nextState[F0]++;
					else if(r<r4)
						nextState[F1]++;
					else if(r < r5)
						nextState[F2]++;
					else
						nextState[T]++;
					++sum;
				}
				while(sum > 10000) {
					r = _R.nextDouble();
					if(r < r1)
						nextState[NS]--;
					else if(r < r2)
						nextState[IS]--;
					else if(r < r3)
						nextState[F0]--;
					else if(r<r4)
						nextState[F1]--;
					else if(r < r5)
						nextState[F2]--;
					else
						nextState[T]--;
					--sum;
				}
				predParts[j].setState(nextState);
			}
			_next.put(current, predParts);
		}
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new DavidLParticleFilter();
	}
	 
	 /*
	  * These functions were taken from 
	  * http://www.cs.princeton.edu/introcs/21function/ErrorFunction.java.html
	  * 
	  */
    // fractional error in math formula less than 1.2 * 10 ^ -7.
    // although subject to catastrophic cancellation when z in very close to 0
    // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
    double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                            t * ( 1.00002368 +
                                            t * ( 0.37409196 + 
                                            t * ( 0.09678418 + 
                                            t * (-0.18628806 + 
                                            t * ( 0.27886807 + 
                                            t * (-1.13520398 + 
                                            t * ( 1.48851587 + 
                                            t * (-0.82215223 + 
                                            t * ( 0.17087277))))))))));
        if (z >= 0) return  ans;
        else        return -ans;
    }

    // fractional error less than x.xx * 10 ^ -4.
    // Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical.
    double erf2(double z) {
        double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
        double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        double ans = 1.0 - poly * Math.exp(-z*z);
        if (z >= 0) return  ans;
        else        return -ans;
    }

    // cumulative normal distribution
    // See Gaussia.java for a better way to compute Phi(z)
    double Phi(double z) {
        return 0.5 * (1.0 + erf(z / (Math.sqrt(2.0))));
    }
    
    /*public String toString() {
    	return "This is my Particle filter.  There are many others like it, but this one is mine";
    }*/

}
