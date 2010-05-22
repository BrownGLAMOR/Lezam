package models.usermodel;

import java.io.*;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import java.util.*;

import jsc.distributions.Binomial;
import jsc.distributions.Normal;


public class BgleibParticleFilter extends ParticleFilterAbstractUserModel {

    private static final int USERS_TO_TRANSFER = 200;
    private static final double USERS_TO_TRANSFER_STDEV = 20.0;
    private static final double BURST_PROB = 0.10;
    private static final int BURST_THRESHOLD = 900;
    
    private static final boolean USE_MATRIX = true;

    private long _seed = 1263456;
    private Random _R;
    private ArrayList<Product> _products;

    private double[][] _standardTransitionProbs;
    private double[][] _burstTransitionProbs;
    
    private BgleibParticleFilter _prediction;
    private BgleibParticleFilter _previous;
    private Map<Product, Double> _burstProbs;

    public BgleibParticleFilter()
    {
            _R = new Random(_seed);
            _particles = new HashMap<Product,Particle[]>();
            _products = new ArrayList<Product>();
            _burstProbs = new HashMap<Product, Double>();

            _products.add(new Product("flat", "dvd"));
            _products.add(new Product("flat", "tv"));
            _products.add(new Product("flat", "audio"));
            _products.add(new Product("pg", "dvd"));
            _products.add(new Product("pg", "tv"));
            _products.add(new Product("pg", "audio"));
            _products.add(new Product("lioneer", "dvd"));
            _products.add(new Product("lioneer", "tv"));
            _products.add(new Product("lioneer", "audio"));

            for (Product product : _products)
            {
            	_burstProbs.put(product, 0.0);
                Particle[] particles = new Particle[NUM_PARTICLES];
                for (int i = 0; i < NUM_PARTICLES; i++)
                    particles[i] = new Particle();
                _particles.put(product, particles);
            }
            initializeStandardTransitionProbs();
            initializeBurstTransitionProbs();
            
            simulateInitialDays(5);
            
            _prediction = makeCopy();
            _prediction.simulateDay(true);
            _prediction.simulateDay(true);
            
            _previous = null;
    }

    public BgleibParticleFilter(HashMap<Product, Particle[]> oldParticles, BgleibParticleFilter prediction, BgleibParticleFilter previous)
    {
            _R = new Random(_seed);
            _particles = new HashMap<Product,Particle[]>();
            _products = new ArrayList<Product>();
            _burstProbs = new HashMap<Product, Double>();

            _products.add(new Product("flat", "dvd"));
            _products.add(new Product("flat", "tv"));
            _products.add(new Product("flat", "audio"));
            _products.add(new Product("pg", "dvd"));
            _products.add(new Product("pg", "tv"));
            _products.add(new Product("pg", "audio"));
            _products.add(new Product("lioneer", "dvd"));
            _products.add(new Product("lioneer", "tv"));
            _products.add(new Product("lioneer", "audio"));

            for (Product product : _products)
            {
            	_burstProbs.put(product, 0.0);
                Particle[] oldParticleArr = oldParticles.get(product);
                Particle[] particles = new Particle[NUM_PARTICLES];
                for (int i = 0; i < NUM_PARTICLES; i++)
                    particles[i] = new Particle(oldParticleArr[i].getState());
                _particles.put(product, particles);
            }
            _prediction = prediction;	
            _previous = previous;
            
            initializeStandardTransitionProbs();
            initializeBurstTransitionProbs();
    }

    private void initializeStandardTransitionProbs()
    {
        _standardTransitionProbs = new double[UserState.values().length][UserState.values().length];

        for (int i = 0; i < UserState.values().length; i++)
            for (int j = 0; j < UserState.values().length; j++)
                _standardTransitionProbs[i][j] = 0.0;

        _standardTransitionProbs[UserState.NS.ordinal()][UserState.NS.ordinal()] = 0.99;
        _standardTransitionProbs[UserState.NS.ordinal()][UserState.IS.ordinal()] = 0.01;

        _standardTransitionProbs[UserState.IS.ordinal()][UserState.NS.ordinal()] = 0.05;
        _standardTransitionProbs[UserState.IS.ordinal()][UserState.IS.ordinal()] = 0.20;
        _standardTransitionProbs[UserState.IS.ordinal()][UserState.F0.ordinal()] = 0.60;
        _standardTransitionProbs[UserState.IS.ordinal()][UserState.F1.ordinal()] = 0.10;
        _standardTransitionProbs[UserState.IS.ordinal()][UserState.F2.ordinal()] = 0.05;

        _standardTransitionProbs[UserState.F0.ordinal()][UserState.NS.ordinal()] = 0.10;
        _standardTransitionProbs[UserState.F0.ordinal()][UserState.F0.ordinal()] = 0.70;
        _standardTransitionProbs[UserState.F0.ordinal()][UserState.F1.ordinal()] = 0.20;

        _standardTransitionProbs[UserState.F1.ordinal()][UserState.NS.ordinal()] = 0.10;
        _standardTransitionProbs[UserState.F1.ordinal()][UserState.F1.ordinal()] = 0.70;
        _standardTransitionProbs[UserState.F1.ordinal()][UserState.F2.ordinal()] = 0.20;

        _standardTransitionProbs[UserState.F2.ordinal()][UserState.NS.ordinal()] = 0.10;
        _standardTransitionProbs[UserState.F2.ordinal()][UserState.F2.ordinal()] = 0.90;

        _standardTransitionProbs[UserState.T.ordinal()][UserState.NS.ordinal()]  = 0.80;
        _standardTransitionProbs[UserState.T.ordinal()][UserState.T.ordinal()]   = 0.20;
    }

    private void initializeBurstTransitionProbs()
    {
        _burstTransitionProbs = new double[UserState.values().length][UserState.values().length];

        for (int i = 0; i < UserState.values().length; i++)
            for (int j = 0; j < UserState.values().length; j++)
                _burstTransitionProbs[i][j] = 0.0;

        _burstTransitionProbs[UserState.NS.ordinal()][UserState.NS.ordinal()] = 0.80;
        _burstTransitionProbs[UserState.NS.ordinal()][UserState.IS.ordinal()] = 0.20;

        _burstTransitionProbs[UserState.IS.ordinal()][UserState.NS.ordinal()] = 0.05;
        _burstTransitionProbs[UserState.IS.ordinal()][UserState.IS.ordinal()] = 0.20;
        _burstTransitionProbs[UserState.IS.ordinal()][UserState.F0.ordinal()] = 0.60;
        _burstTransitionProbs[UserState.IS.ordinal()][UserState.F1.ordinal()] = 0.10;
        _burstTransitionProbs[UserState.IS.ordinal()][UserState.F2.ordinal()] = 0.05;

        _burstTransitionProbs[UserState.F0.ordinal()][UserState.NS.ordinal()] = 0.10;
        _burstTransitionProbs[UserState.F0.ordinal()][UserState.F0.ordinal()] = 0.70;
        _burstTransitionProbs[UserState.F0.ordinal()][UserState.F1.ordinal()] = 0.20;

        _burstTransitionProbs[UserState.F1.ordinal()][UserState.NS.ordinal()] = 0.10;
        _burstTransitionProbs[UserState.F1.ordinal()][UserState.F1.ordinal()] = 0.70;
        _burstTransitionProbs[UserState.F1.ordinal()][UserState.F2.ordinal()] = 0.20;

        _burstTransitionProbs[UserState.F2.ordinal()][UserState.NS.ordinal()] = 0.10;
        _burstTransitionProbs[UserState.F2.ordinal()][UserState.F2.ordinal()] = 0.90;

        _burstTransitionProbs[UserState.T.ordinal()][UserState.NS.ordinal()]  = 0.80;
        _burstTransitionProbs[UserState.T.ordinal()][UserState.T.ordinal()]   = 0.20;
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
                    if(count != NUM_PARTICLES - 1) {
                            throw new RuntimeException(" Problem reading particle file");
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

    public void saveParticlesToFile(Product product) throws IOException {
            Particle[] particles = _particles.get(product);
            FileWriter fstream = new FileWriter("initParticles.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            String output = "";
            for(int i = 0; i < particles.length; i++) {
                    output += particles[i].stateString() + "\n";
            }
            out.write(output);
            out.close();
    }

    private void simulateInitialDays(int daysToSim)
    {
        for (int i = 0; i < daysToSim; i++)
        {
        	System.out.println("Initial day");
            simulateDay(false);
        }
//        for (Product p : _particles.keySet())
//        {
//            try {
//                saveParticlesToFile(p);
//            } catch (IOException ex) {
//                System.err.println("Error printing file");
//            }
//        }
    }

    private void simulateDay(boolean doTransactions)
    {
        List<WorkerThread> threads = new LinkedList<WorkerThread>();
        WorkerThread w;
        for (Product product : _particles.keySet())
        {
            w = new WorkerThread(_particles.get(product), doTransactions);
            threads.add(w);
            w.start();
        }
        for (WorkerThread t : threads)
        {
            try {
                t.join();
            } catch (InterruptedException ex) {
                System.err.println("Huh?");
            }
        }
//        for (Product product : _particles.keySet())
//        {
//            for (Particle particle : _particles.get(product))
//            {
//            	if (USE_MATRIX)
//            		simulateParticleDayMatrix(particle, doTransactions);
//            	else
//            		simulateParticleDayIterative(particle, doTransactions);
//            }
//        }
    }

    private class WorkerThread extends Thread
    {
        private Particle[] _particles;
        private boolean _doTransactions;

        public WorkerThread(Particle[] particles, boolean doTransactions)
        {
            _particles = particles;
            _doTransactions = doTransactions;
        }

        @Override
        public void run()
        {
            for (Particle particle : _particles)
            {
            	if (USE_MATRIX)
            		simulateParticleDayMatrix(particle, _doTransactions);
            	else
            		simulateParticleDayIterative(particle, _doTransactions);
            }
        }
    }

    private void simulateParticleDayIterative(Particle p, boolean doTransactions)
    {
    	int numTransacted = 0;
        if (doTransactions)
        	numTransacted = doTransactions(p);
        double[][] transitionProbs = _R.nextDouble() < BURST_PROB ? _burstTransitionProbs : _standardTransitionProbs;
        int[] newState = new int[UserState.values().length];
        for (UserState fromState : UserState.values())
        {
            int numInState = p.getStateCount(fromState);
            if (fromState == UserState.T)
            	numInState -= numTransacted;
            while(numInState > 0)
            {
//                double numToTransfer = numInState >= USERS_TO_TRANSFER ? USERS_TO_TRANSFER : numInState;
                int numToTransfer = (int)(_R.nextGaussian() * USERS_TO_TRANSFER_STDEV + USERS_TO_TRANSFER + 0.5);
                if (numToTransfer > numInState)
                    numToTransfer = numInState;
                else if (numToTransfer <= 0)
                    numToTransfer = 1;
                double rand = _R.nextDouble();
                for (UserState toState : UserState.values())
                {
                    if (rand < transitionProbs[fromState.ordinal()][toState.ordinal()])
                    {
                        newState[toState.ordinal()] += numToTransfer;
                        numInState -= numToTransfer;
                        break;
                    }
                    rand -= transitionProbs[fromState.ordinal()][toState.ordinal()];
                }
            }
        }
        newState[UserState.T.ordinal()] += numTransacted;
        p._state = newState;
    }

    private void simulateParticleDayMatrix(Particle particle, boolean doTransactions)
    {
    	int numTransacted = 0;
        if (doTransactions)
            numTransacted = doTransactions(particle);
        double[][] transitionProbs = _R.nextDouble() < BURST_PROB ? _burstTransitionProbs : _standardTransitionProbs;
        double[][] newTransProbs = new double[UserState.values().length][UserState.values().length];
        int[] newState = new int[UserState.values().length];
        double p, val, sum;
        int n, numToMove;
        Binomial binom;
        int numMoved = numTransacted;
        for (UserState fromState : UserState.values())
        {
            n = particle.getStateCount(fromState);
            if (fromState == UserState.T)
            	n -= numTransacted;
            if (n == 0)
                continue;
            sum = 0;
            for (UserState toState : UserState.values())
            {
                p = transitionProbs[fromState.ordinal()][toState.ordinal()];
                if (p == 0)
                    continue;
                binom = new Binomial(n, p);
                val = (double)binom.random() / n;
//                val = Phi()
                newTransProbs[fromState.ordinal()][toState.ordinal()] = val;
                sum += val;
            }
            if (sum == 0)
                continue;
            for (UserState toState : UserState.values())
            {
                newTransProbs[fromState.ordinal()][toState.ordinal()] /= sum;
                numToMove = (int)(n * newTransProbs[fromState.ordinal()][toState.ordinal()] + 0.5);
                numMoved += numToMove;
                newState[toState.ordinal()] += numToMove;
            }
        }
        newState[UserState.T.ordinal()] += numTransacted;
        newState[0] -= numMoved - 10000;
        particle._state = newState;
    }

    private double getParticleWeight(int numQueries, Particle particle)
    {
    	if (particle.getStateCount(UserState.IS) == 0)
    	{
    		if (particle.getStateCount(UserState.F2) != numQueries)
    			return 0;
    		return 1;
    	}
        Binomial binom = new Binomial(particle.getStateCount(UserState.IS), 1.0 / 3.0);
        return binom.pdf(numQueries - particle.getStateCount(UserState.F2));
//        double n = particle.getStateCount(UserState.IS);
//        double p = 1.0 / 3.0;
//        double x = numQueries - particle.getStateCount(UserState.F2);
//        return Phi((x - n*p) / (n*p * (1 - n*p)) + 0.5) - Phi((x - n*p) / (n*p * (1 - n*p)) - 0.5);
    }

    private int doTransactions(Particle p)
    {
    	int numMoved = 0;
        double x = 0.06145;
        double y = 0.35;
        double prob = new Normal(x, x * y).random();
//        double prob = _R.nextGaussian() * x * y + x;
        prob = prob < 0 ? 0 : prob;
        prob = prob > 1 ? 1 : prob;
        int numTrans = (int)(prob * p._state[UserState.F0.ordinal()] + 0.5);
        p._state[UserState.F0.ordinal()] -= numTrans;
        p._state[UserState.T.ordinal()] += numTrans;
        numMoved += numTrans;
        
        x += x;
        y += y;
        prob = new Normal(x, x * y).random();
//        prob = _R.nextGaussian() * x * y + x;
        prob = prob < 0 ? 0 : prob;
        prob = prob > 1 ? 1 : prob;
        numTrans = (int)(prob * p._state[UserState.F1.ordinal()] + 0.5);
        p._state[UserState.F1.ordinal()] -= numTrans;
        p._state[UserState.T.ordinal()] += numTrans;
        numMoved += numTrans;
        
        x += x;
        y += y;
        prob = new Normal(x, x * y).random();
//        prob = _R.nextGaussian() * x * y + x;
        prob = prob < 0 ? 0 : prob;
        prob = prob > 1 ? 1 : prob;
        numTrans = (int)(prob * p._state[UserState.F2.ordinal()] + 0.5);
        p._state[UserState.F2.ordinal()] -= numTrans;
        p._state[UserState.T.ordinal()] += numTrans;
        numMoved += numTrans;
        
        return numMoved;
    }

    @Override
    public boolean updateModel(HashMap<Query, Integer> totalImpressions) 
    {
        double sum, val;
        for (Product product : _products)
        {
        	Query q = new Query(product.getManufacturer(), product.getComponent());
            sum = 0;
            for (Particle p : _particles.get(product))
            {
                val = getParticleWeight(totalImpressions.get(q), p);
                p.setWeight(val);
                sum += val;
            }
            for (Particle p : _particles.get(product))
            {
                p.setWeight(p.getWeight() / sum);
            }
        }
        for (Product p : _particles.keySet())
            resample(p);
        setCurrentBurstProbs();
//        simulateDay(true);
        _previous = makeCopy();
        simulateDay(true);
        _prediction = makeCopy();
        _prediction.simulateDay(true);
        return true;
    }

    private void resample(Product product)
    {
        Particle[] newParticles = new Particle[NUM_PARTICLES];
        double rand;
        for (int i = 0; i < NUM_PARTICLES; i++)
        {
            rand = _R.nextDouble();
            for (Particle p : _particles.get(product))
            {
                if (rand < p.getWeight())
                {
                    newParticles[i] = new Particle(p.getState());
                    break;
                }
                else
                    rand -= p.getWeight();
            }
        }
        _particles.put(product, newParticles);
    }


    public AbstractModel getCopy()
    {
        return new BgleibParticleFilter();
    }
    
    public BgleibParticleFilter makeCopy()
    {
    	return new BgleibParticleFilter(_particles, _prediction, _previous);
    }
    
    public int getThisFiltersEstimate(Product product, UserState userState)
    {
    	Particle[] particles = _particles.get(product);
        int sum = 0;
        int burst = 0;
        int burstSum = 0;
        for (Particle p : particles)
        {
        	if (isBurst(p)) {
        		burst++;
        		burstSum += p.getStateCount(userState);
        	}
            sum += p.getStateCount(userState);
        }
        int count;
        double burstProb = (double)burst / NUM_PARTICLES;
        if (burstProb > 0.5)
        {
        	sum = burstSum;
        	count = burst;
        }
        else 
        {
        	sum = sum - burstSum;
        	count = NUM_PARTICLES - burst;
        }
        return (int)((double)sum / count + 0.5);
    }

    @Override
    public int getCurrentEstimate(Product product, UserState userState)
    {
        return _previous.getThisFiltersEstimate(product, userState);
    }
    
    private void setCurrentBurstProbs()
    {
    	for (Product product : _products)
    	{
	        int burst = 0;
	        for (Particle p : _particles.get(product))
	        {
	        	if (isBurst(p)) burst++;
	        }
	        _burstProbs.put(product, (double)burst / NUM_PARTICLES);
    	}
    }

    private boolean isBurst(Particle p)
    {
            return p._state[UserState.IS.ordinal()] >= BURST_THRESHOLD;
    }

    public double getCurrentBurstProb(Product product)
    {
    	return _burstProbs.get(product);
    }

    @Override
    public int getPrediction(Product product, UserState userState)
    {
        return _prediction.getThisFiltersEstimate(product, userState);
    }
    
    public String toString()
    {
    	return "le particle filter de chez leib";
    }
}

