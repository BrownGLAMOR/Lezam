package models.usermodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import models.AbstractModel;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;


public class jbergParticleFilter extends ParticleFilterAbstractUserModel {

	private HashMap<UserState, HashMap<UserState, Double>> _standardProbs;
	private HashMap<UserState, HashMap<UserState, Double>> _burstProbs;
	private HashMap<UserState,Double> _conversionProbs;
	private long _seed = 37257359;
	private Random _R;
	private ArrayList<Product> _products;
	private Particle[] initParticle;
	private static final double _burstProb = .1;
	private double _baseConvPr1, _baseConvPr2, _baseConvPr3;
	private double _convPrVar1, _convPrVar2, _convPrVar3; //multiply this by the baseConvPr
	private HashMap<Product,HashMap<UserState,Double>> _predictions, _currentEstimate;

//	694.9886157096605: 0.0626119 0.0251838 0.158351 0.67921 0.141542 0.160939 
//	697.0156722415486: 0.00284918 0.172542 0.267033 0.440053 0.119098 0.893219 
//	697.8355964995897: 0.0119813 0.264209 0.130913 0.620421 0.106847 0.607943 
//	699.2415381348094: 0.00809258 0.163013 0.293061 0.957269 0.0923163 0.442596 
//	699.3549323041273: 0.0724231 0.528481 0.168045 0.437404 0.107272 0.420076 
//	699.5752877898738: 0.0144021 0.852746 0.187898 0.316331 0.133562 0.198476 
//	700.3748063613748: 0.0355996 0.2807 0.167058 0.0516611 0.145452 0.174416 
//	700.4081527010201: 0.0183964 0.403492 0.166033 0.992771 0.114652 0.291618 
//	700.6141728471948: 0.0318413 0.743306 0.176706 0.59684 0.149999 0.6693 
//	702.0728497070176: 0.0369804 0.366531 0.168774 0.766537 0.095683 0.136267 
//	702.1050041219073: 0.0325374 0.374001 0.12593 0.304142 0.126231 0.724372 
//	702.4207566133291: 0.0422767 0.34229 0.136049 0.932376 0.122228 0.494697 
//	702.930491815266: 0.0413901 0.0291355 0.254303 0.272301 0.145452 0.139441 
//	703.1527381330859: 0.0578801 0.353304 0.234287 0.375422 0.0848571 0.0844061 
//	703.3201852282576: 0.0193268 0.556333 0.198908 0.781435 0.0931492 0.561895 
//	703.4479184113078: 0.0870916 0.169205 0.113056 0.200854 0.136544 0.568712 
//	703.4707302014956: 0.0840949 0.353304 0.165047 0.0678862 0.144986 0.173212 
//	703.4842845284924: 0.0523127 0.972342 0.0620776 0.117437 0.15223 0.345201 
//	703.9139222113099: 0.0110238 0.534941 0.261626 0.228775 0.0782604 0.695914 
//	703.9170808939764: 0.0382358 0.788045 0.232357 0.975039 0.116294 0.261882 
//	704.1502301455878: 0.0215972 0.152349 0.196439 0.817407 0.170955 0.993204 
//	704.3572414860248: 0.0144021 0.852746 0.204914 0.12147 0.133562 0.139439 
//	705.0420748391351: 0.0110726 0.544719 0.190365 0.187729 0.128143 0.0824589 
//	705.2580201139046: 0.0552641 0.239727 0.215436 0.504392 0.109823 0.627374 
//	705.6685022172433: 0.0692245 0.760256 0.0413297 0.0682217 0.162431 0.335104 
//	705.8947946658368: 0.023155 0.259906 0.193046 0.837086 0.145458 0.716235 
//	706.1499932465545: 0.0352226 0.353304 0.136283 0.469287 0.144986 0.173212 
//	706.1611935442287: 0.0177863 0.403492 0.166033 0.992771 0.13875 0.291618 

	
	public jbergParticleFilter() {
//		this(0.073, 0.35, 0.073*2, 0.35, 0.073*3, 0.35);
//		this(0.0626119, 0.0251838, 0.158351, 0.67921, 0.141542, 0.160939);
		this(0.0603256, 0.919268, 0.119586, 0.947004, 0.126231, 0.171058);
	}

	public jbergParticleFilter(double convPr1, double convPrVar1,
			double convPr2, double convPrVar2,
			double convPr3, double convPrVar3) {
		_baseConvPr1 = convPr1;
		_convPrVar1 = convPrVar1;
		_baseConvPr2 = convPr2;
		_convPrVar2 = convPrVar2;
		_baseConvPr3 = convPr3;
		_convPrVar3 = convPrVar3;
		System.out.println(_baseConvPr1 + " " + _convPrVar1 + " " + 
				_baseConvPr2 + " " + _convPrVar2 + " " + 
				_baseConvPr3 + " " + _convPrVar3);
		_standardProbs = new HashMap<UserState,HashMap<UserState,Double>>();
		_burstProbs = new HashMap<UserState,HashMap<UserState,Double>>();
		_R = new Random(_seed);
		_particles = new HashMap<Product,Particle[]>();
		_conversionProbs = new HashMap<UserState,Double>();

		_conversionProbs.put(UserState.NS, 0.0);
		_conversionProbs.put(UserState.IS, 0.0);
		_conversionProbs.put(UserState.F0, _baseConvPr1);
		_conversionProbs.put(UserState.F1, _baseConvPr2);
		_conversionProbs.put(UserState.F2, _baseConvPr3);
		_conversionProbs.put(UserState.T, 0.0);

		HashMap<UserState,Double> standardFromNSProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromISProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF0Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF1Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromF2Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> standardFromTProbs = new HashMap<UserState, Double>();

		HashMap<UserState,Double> burstFromNSProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromISProbs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF0Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF1Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromF2Probs = new HashMap<UserState, Double>();
		HashMap<UserState,Double> burstFromTProbs = new HashMap<UserState, Double>();

		standardFromNSProbs.put(UserState.NS,0.99);
		standardFromNSProbs.put(UserState.IS,0.01);
		standardFromNSProbs.put(UserState.F0,0.0);
		standardFromNSProbs.put(UserState.F1,0.0);
		standardFromNSProbs.put(UserState.F2,0.0);
		standardFromNSProbs.put(UserState.T,0.0);

		standardFromISProbs.put(UserState.NS,0.05);
		standardFromISProbs.put(UserState.IS,0.2);
		standardFromISProbs.put(UserState.F0,0.6);
		standardFromISProbs.put(UserState.F1,0.1);
		standardFromISProbs.put(UserState.F2,0.05);
		standardFromISProbs.put(UserState.T,0.0);

		standardFromF0Probs.put(UserState.NS,0.1);
		standardFromF0Probs.put(UserState.IS,0.0);
		standardFromF0Probs.put(UserState.F0,0.7);
		standardFromF0Probs.put(UserState.F1,0.2);
		standardFromF0Probs.put(UserState.F2,0.0);
		standardFromF0Probs.put(UserState.T,0.0);

		standardFromF1Probs.put(UserState.NS,0.1);
		standardFromF1Probs.put(UserState.IS,0.0);
		standardFromF1Probs.put(UserState.F0,0.0);
		standardFromF1Probs.put(UserState.F1,0.7);
		standardFromF1Probs.put(UserState.F2,0.2);
		standardFromF1Probs.put(UserState.T,0.0);

		standardFromF2Probs.put(UserState.NS,0.1);
		standardFromF2Probs.put(UserState.IS,0.0);
		standardFromF2Probs.put(UserState.F0,0.0);
		standardFromF2Probs.put(UserState.F1,0.0);
		standardFromF2Probs.put(UserState.F2,0.9);
		standardFromF2Probs.put(UserState.T,0.0);

		standardFromTProbs.put(UserState.NS,0.8);
		standardFromTProbs.put(UserState.IS,0.0);
		standardFromTProbs.put(UserState.F0,0.0);
		standardFromTProbs.put(UserState.F1,0.0);
		standardFromTProbs.put(UserState.F2,0.0);
		standardFromTProbs.put(UserState.T,0.2);

		burstFromNSProbs.put(UserState.NS,0.8);
		burstFromNSProbs.put(UserState.IS,0.2);
		burstFromNSProbs.put(UserState.F0,0.0);
		burstFromNSProbs.put(UserState.F1,0.0);
		burstFromNSProbs.put(UserState.F2,0.0);
		burstFromNSProbs.put(UserState.T,0.0);

		burstFromISProbs.put(UserState.NS,0.05);
		burstFromISProbs.put(UserState.IS,0.2);
		burstFromISProbs.put(UserState.F0,0.6);
		burstFromISProbs.put(UserState.F1,0.1);
		burstFromISProbs.put(UserState.F2,0.05);
		burstFromISProbs.put(UserState.T,0.0);

		burstFromF0Probs.put(UserState.NS,0.1);
		burstFromF0Probs.put(UserState.IS,0.0);
		burstFromF0Probs.put(UserState.F0,0.7);
		burstFromF0Probs.put(UserState.F1,0.2);
		burstFromF0Probs.put(UserState.F2,0.0);
		burstFromF0Probs.put(UserState.T,0.0);

		burstFromF1Probs.put(UserState.NS,0.1);
		burstFromF1Probs.put(UserState.IS,0.0);
		burstFromF1Probs.put(UserState.F0,0.0);
		burstFromF1Probs.put(UserState.F1,0.7);
		burstFromF1Probs.put(UserState.F2,0.2);
		burstFromF1Probs.put(UserState.T,0.0);

		burstFromF2Probs.put(UserState.NS,0.1);
		burstFromF2Probs.put(UserState.IS,0.0);
		burstFromF2Probs.put(UserState.F0,0.0);
		burstFromF2Probs.put(UserState.F1,0.0);
		burstFromF2Probs.put(UserState.F2,0.9);
		burstFromF2Probs.put(UserState.T,0.0);

		burstFromTProbs.put(UserState.NS,0.8);
		burstFromTProbs.put(UserState.IS,0.0);
		burstFromTProbs.put(UserState.F0,0.0);
		burstFromTProbs.put(UserState.F1,0.0);
		burstFromTProbs.put(UserState.F2,0.0);
		burstFromTProbs.put(UserState.T,0.2);

		_standardProbs.put(UserState.NS,standardFromNSProbs);
		_standardProbs.put(UserState.IS,standardFromISProbs);
		_standardProbs.put(UserState.F0,standardFromF0Probs);
		_standardProbs.put(UserState.F1,standardFromF1Probs);
		_standardProbs.put(UserState.F2,standardFromF2Probs);
		_standardProbs.put(UserState.T,standardFromTProbs);

		_burstProbs.put(UserState.NS,burstFromNSProbs);
		_burstProbs.put(UserState.IS,burstFromISProbs);
		_burstProbs.put(UserState.F0,burstFromF0Probs);
		_burstProbs.put(UserState.F1,burstFromF1Probs);
		_burstProbs.put(UserState.F2,burstFromF2Probs);
		_burstProbs.put(UserState.T,burstFromTProbs);

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

		initializeParticlesFromFile("/Users/jordanberg/Documents/workspace/Clients/initUserParticles");
		//		initializeParticlesFromFile("/u/jberg/initUserParticles");
		updatePredictionMaps();
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

		boolean firstTime = true;
		for(Product prod : _products) {
			Particle[] particles = new Particle[NUM_PARTICLES];
			for(int i = 0; i < particles.length; i++) {
				Particle particle = new Particle(allStates[i]);
				particles[i] = particle;
			}
			_particles.put(prod, particles);

			if(firstTime) {
				initParticle = new Particle[NUM_PARTICLES];
				for(int i = 0; i < particles.length; i++) {
					Particle particle = new Particle(allStates[i]);
					initParticle[i] = particle;
				}
				firstTime = false;
			}
		}
	}

	public UserState transitionUserWithoutConversions(UserState currState, boolean burst) {
		HashMap<UserState, HashMap<UserState, Double>> transProbs;
		if(burst) {
			transProbs = _burstProbs;
		}
		else {
			transProbs = _standardProbs;
		}

		double rand = _R.nextDouble();
		double threshhold = transProbs.get(currState).get(UserState.NS);
		if(rand <= threshhold) {
			return UserState.NS;
		}
		threshhold += transProbs.get(currState).get(UserState.IS);
		if(rand <= threshhold) {
			return UserState.IS;
		}
		threshhold += transProbs.get(currState).get(UserState.F0);
		if(rand <= threshhold) {
			return UserState.F0;
		}
		threshhold += transProbs.get(currState).get(UserState.F1);
		if(rand <= threshhold) {
			return UserState.F1;
		}
		return UserState.F2;
	}

	public UserState transitionUserWithConversions(UserState currState, boolean burst, HashMap<UserState, Double> conversionProbs) {
		double convRand = _R.nextDouble();
		if(convRand <= conversionProbs.get(currState)) {
			return UserState.T;
		}
		else {
			return transitionUserWithoutConversions(currState,burst);
		}
	}

	public void updateParticles(int totalImpressions, Particle[] particles) {
		double totalWeight = 0.0;

		/*
		 * Update weights based on observations
		 */
		for(int i = 0; i < particles.length; i++) {
			Particle particle = particles[i];
			double weight = particle.getWeight();
			int[] state = particle.getState();
			double prob = getProbability(totalImpressions, state);
			double newWeight = weight*prob;
			totalWeight += newWeight;
			particle.setWeight(newWeight);
		}

		/*
		 * Normalize Probabilities
		 */
		if(totalWeight > 0) {
			for(int i = 0; i < particles.length; i++) {
				Particle particle = particles[i];
				double newWeight = particle.getWeight()/totalWeight;
				particle.setWeight(newWeight);
			}
		}
		else {
			for(int i = 0; i < particles.length; i++) {
				Particle particle = new Particle(initParticle[i].getState(),particles[i].getBurstHistory());
				particles[i] = particle;
			}
			System.out.println("We had to reinitialize the particles...");
		}
	}

	public double getProbability(int totalImpressions, int[] state) {
		int IS = state[UserState.IS.ordinal()];
		int F2 = state[UserState.F2.ordinal()];
		if(IS + F2 < totalImpressions) {
			return 0.0;
		}
		else {
			return getBinomialProbUsingGaussian(IS,totalImpressions-F2);
		}
	}

	public double getBinomialProbUsingGaussian(int n, int k) {
		double mean = n/3.0;
		double sigma2 = mean * 2.0/3.0;
		double diff = k - mean;
		return 1.0/Math.sqrt(2.0*Math.PI*sigma2) * Math.exp(-(diff*diff)/(2.0*sigma2));
	}

	public double getRandomBinomial(double n, double p) {
		if(p == 0) {
			return 0.0;
		}
		double k = _R.nextGaussian()*n*p*(1-p) + n*p;
		return (k > 0 && k < n) ? k : getRandomBinomial(n, p);
	}

	public Particle[] resampleParticles(Particle[] particles) {
		List<Double> CDF = new LinkedList<Double>();
		double CDFval = 0.0;
		for(int i = 0; i < particles.length; i++) {
			CDFval += particles[i].getWeight();
			CDF.add(CDFval);
		}

		Particle[] newParticles = new Particle[particles.length];
		for(int i = 0; i < particles.length; i++) {
			Particle particle = null;
			double rand = _R.nextDouble();

			int index = Collections.binarySearch(CDF, rand); 
			if (index < 0) {
				particle = particles[-index-1];
			}
			else {
				particle = particles[index];
			}

			Particle newParticle = new Particle(particle.getState(),particle.getBurstHistory());
			newParticles[i] = newParticle;
		}
		return newParticles;
	}

	public void makeInitParticleLog() throws IOException {
		Particle[] particles = new Particle[NUM_PARTICLES];
		for(int i = 0; i < particles.length; i++) {
			Particle particle = new Particle();
			particles[i] = particle;
		}
		for(int day = 0; day < 5; day++) {
			for(int i = 0; i < particles.length; i++) {
				double burstRand = _R.nextDouble();
				boolean burst;
				if(burstRand <= _burstProb) {
					burst = true;
				}
				else {
					burst = false;
				}
				Particle particle = particles[i];
				int[] state = particle.getState();
				int[] newState = new int[state.length];
				for(int j = 0; j < state.length; j++) {
					for(int k = 0; k < state[j]; k++) {
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
		for(int i = 0; i < particles.length; i++) {
			output += particles[i].stateString() + "\n";
		}
		out.write(output);
		out.close();
	}

	@Override
	public int getCurrentEstimate(Product product, UserState userState) {
		return ((int)((double)_currentEstimate.get(product).get(userState)));
	}

	@Override
	public int getPrediction(Product product, UserState userState) {
		return ((int)((double)_predictions.get(product).get(userState)));
	}

	@Override
	public boolean updateModel(HashMap<Query, Integer> totalImpressions) {
		for(Query q : totalImpressions.keySet()) {
			Product prod = new Product(q.getManufacturer(), q.getComponent());
			if(_products.contains(prod)) {
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
		
		
		for(Query q : totalImpressions.keySet()) {
			Product prod = new Product(q.getManufacturer(), q.getComponent());
			if(_products.contains(prod)) {
				Particle[] particles = _particles.get(prod);
				pushParticlesForward(particles);
				_particles.put(prod, particles);
			}
		}
		return true;
	}

	public void pushParticlesForward(Particle[] particles) {
		for(int i = 0; i < particles.length; i++) {
			double burstRand = _R.nextDouble();
			boolean burst;
			if(burstRand <= _burstProb) {
				burst = true;
			}
			else {
				burst = false;
			}

			HashMap<UserState, HashMap<UserState, Double>> transProbs = burst ? _burstProbs : _standardProbs;

			HashMap<UserState, Double> conversionProbs = new HashMap<UserState,Double>();
			conversionProbs.put(UserState.NS, 0.0);
			conversionProbs.put(UserState.IS, 0.0);
			conversionProbs.put(UserState.F0, _baseConvPr1*(1+_convPrVar1*_R.nextGaussian()));
			conversionProbs.put(UserState.F1, _baseConvPr2*(1+_convPrVar2*_R.nextGaussian()));
			conversionProbs.put(UserState.F2, _baseConvPr3*(1+_convPrVar3*_R.nextGaussian()));
			conversionProbs.put(UserState.T, 0.0);
			Particle particle = particles[i];
			
			particle.addToBurstHistory(burst);

			int[] state = particle.getState();
			int[] newState = new int[state.length];

			double[] stateFloat = new double[state.length];
			double[] newStateFloat = new double[newState.length];

			for(int j = 0; j < state.length; j++) {
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
				for(int k = 0; k < probsArr.length; k++) {
					probsArr[k] = getRandomBinomial(NUM_USERS_PER_PROD, probs.get(UserState.values()[k]))/NUM_USERS_PER_PROD;
					totalWeight += probsArr[k];
				}

				/*
				 * Normalize
				 */
				for(int k = 0; k < probsArr.length; k++) {
					probsArr[k] /= totalWeight;
				}

				/*
				 * Now transition users
				 */
				for(int k = 0; k < probsArr.length; k++) {
					newStateFloat[k] += stateFloat[j] * probsArr[k];
				}
			}

			/*
			 * Convert the particles that are represented by
			 * double to ints
			 */
			int totUsers = 0;
			for(int j = 0; j < state.length; j++) {
				int s = (int) newStateFloat[j];
				newState[j] = s;
				totUsers += s;
			}

			double diff = NUM_USERS_PER_PROD - totUsers;

			if(diff != 0) {
				for(int j = 0; j < Math.abs(diff); j++) {
					int idx = _R.nextInt(state.length);
					if(diff > 0) {
						newState[idx] += 1;
					}
					else {
						newState[idx] -= 1;
					}
				}
			}


			particle.setState(newState);
		}
	}
	
	public void pushParticlesForward(Particle[] particles, boolean burst) {
		for(int i = 0; i < particles.length; i++) {

			HashMap<UserState, HashMap<UserState, Double>> transProbs = burst ? _burstProbs : _standardProbs;

			HashMap<UserState, Double> conversionProbs = new HashMap<UserState,Double>();
			conversionProbs.put(UserState.NS, 0.0);
			conversionProbs.put(UserState.IS, 0.0);
			conversionProbs.put(UserState.F0, _baseConvPr1*(1+_convPrVar1*_R.nextGaussian()));
			conversionProbs.put(UserState.F1, _baseConvPr2*(1+_convPrVar2*_R.nextGaussian()));
			conversionProbs.put(UserState.F2, _baseConvPr3*(1+_convPrVar3*_R.nextGaussian()));
			conversionProbs.put(UserState.T, 0.0);
			Particle particle = particles[i];
			
			particle.addToBurstHistory(burst);

			int[] state = particle.getState();
			int[] newState = new int[state.length];

			double[] stateFloat = new double[state.length];
			double[] newStateFloat = new double[newState.length];

			for(int j = 0; j < state.length; j++) {
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
				for(int k = 0; k < probsArr.length; k++) {
					probsArr[k] = getRandomBinomial(NUM_USERS_PER_PROD, probs.get(UserState.values()[k]))/NUM_USERS_PER_PROD;
					totalWeight += probsArr[k];
				}

				/*
				 * Normalize
				 */
				for(int k = 0; k < probsArr.length; k++) {
					probsArr[k] /= totalWeight;
				}

				/*
				 * Now transition users
				 */
				for(int k = 0; k < probsArr.length; k++) {
					newStateFloat[k] += stateFloat[j] * probsArr[k];
				}
			}

			/*
			 * Convert the particles that are represented by
			 * double to ints
			 */
			int totUsers = 0;
			for(int j = 0; j < state.length; j++) {
				int s = (int) newStateFloat[j];
				newState[j] = s;
				totUsers += s;
			}

			double diff = NUM_USERS_PER_PROD - totUsers;

			if(diff != 0) {
				for(int j = 0; j < Math.abs(diff); j++) {
					int idx = _R.nextInt(state.length);
					if(diff > 0) {
						newState[idx] += 1;
					}
					else {
						newState[idx] -= 1;
					}
				}
			}


			particle.setState(newState);
		}
	}

	private void updatePredictionMaps() {
		HashMap<Product,Particle[]> particlesCopy = new HashMap<Product,Particle[]>();
		_predictions = new HashMap<Product,HashMap<UserState,Double>>();
		_currentEstimate = new HashMap<Product,HashMap<UserState,Double>>();
		for(Product prod : _products) {
			Particle[] particles = _particles.get(prod);
			Particle[] particleCopy = new Particle[particles.length];

			HashMap<UserState,Double> estimates = new HashMap<UserState,Double>();
			double[] estimate = new double[UserState.values().length];

			for(int i = 0; i < particles.length; i++) {
				Particle particle = particles[i];
				for(UserState state : UserState.values()) {
					estimate[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
				}
				particleCopy[i] = new Particle(particle.getState(), particle.getWeight(), particle.getBurstHistory());
			}

			for(int i = 0; i < estimate.length; i++) {
				estimates.put(UserState.values()[i], estimate[i]);
			}

			_currentEstimate.put(prod, estimates);
			particlesCopy.put(prod, particleCopy);
		}

		for(Product prod : _products) {
			Particle[] particles = particlesCopy.get(prod);
			pushParticlesForward(particles,false);
			pushParticlesForward(particles,false);
			particlesCopy.put(prod, particles);
		}

		for(Product prod : _products) {
			Particle[] particles = particlesCopy.get(prod);

			HashMap<UserState,Double> estimates = new HashMap<UserState,Double>();
			double[] estimate = new double[UserState.values().length];

			for(int i = 0; i < particles.length; i++) {
				Particle particle = particles[i];
				for(UserState state : UserState.values()) {
					estimate[state.ordinal()] += particle.getStateCount(state) * particle.getWeight();
				}
			}

			for(int i = 0; i < estimate.length; i++) {
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
		return new jbergParticleFilter(_baseConvPr1,_convPrVar1,_baseConvPr2,_convPrVar2,_baseConvPr3,_convPrVar3);
	}

}
