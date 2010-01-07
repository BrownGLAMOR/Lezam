package simulator;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import agents.AbstractAgent;
import agents.AdjustPM;
import agents.AdjustPPS;
import agents.AdjustPR;
import agents.EquatePM;
import agents.EquatePPS;
import agents.EquatePR;

public class AgentTuner {

	String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
	int _min = 1425;
	int _max = 1430;
	private AbstractAgent _agent;
	private ArrayList<ArrayList<Double>> _parameters;
	private Random _random;
	double mutationRate = .04;
	double includeBadRate = .5;

	public AgentTuner(AbstractAgent agent, ArrayList<ArrayList<Double>> parameters) {
		_agent = agent;
		_parameters = parameters;
		_random = new Random();
	}


	public ArrayList<Integer> solveWithGA(int popSize, int numKeep, int maxIterations, int numGames) {
		ArrayList<ArrayList<Integer>> population = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < popSize; i++) {
			ArrayList<Integer> randState = new ArrayList<Integer>();
			for(int j = 0; j < _parameters.size(); j++) {
				ArrayList<Double> params = _parameters.get(j);
				randState.add(_random.nextInt(params.size()));
			}
			population.add(randState);
		}
		HashMap<ArrayList<Integer>,Double> paramScore = new HashMap<ArrayList<Integer>, Double>();
		_max = _min + numGames;
		ArrayList<Integer> bestParams = new ArrayList<Integer>();
		double bestScore = 0;
		for(int iter = 0; iter < maxIterations; iter++) {
			HashMap<ArrayList<Integer>,Double> currentParamScore = new HashMap<ArrayList<Integer>, Double>();
			ArrayList<ArrayList<Integer>> newPopulation = new ArrayList<ArrayList<Integer>>();
			/*
			 * Update population scores and keep some of the best chromosomes
			 */
			double threshold = 0;
			for(int i = 0; i < population.size(); i++) {
				ArrayList<Integer> chromosome = population.get(i);
				Double score = paramScore.get(chromosome);
				if(score == null) {
					score = evaluateAgent(createCopy(chromosome));
				}
				paramScore.put(chromosome, score);
				currentParamScore.put(chromosome, score);
				if(score > bestScore) {
					bestScore = score;
					bestParams = chromosome;
				}

				if(score > threshold) {
					if(newPopulation.size() == 0) {
						newPopulation.add(chromosome);
					}
					else {
						boolean added = false;
						for(int j = 0; j < newPopulation.size(); j++) {
							if(score > paramScore.get(newPopulation.get(j))) {
								added = true;
								newPopulation.add(j, chromosome);
								break;
							}
						}
						if(!added && newPopulation.size() < numKeep) {
							newPopulation.add(chromosome);
							added = true;
						}
						if(added) {
							while(newPopulation.size() > numKeep) {
								newPopulation.remove(newPopulation.size()-1);
							}
							if(newPopulation.size() == numKeep) {
								threshold = paramScore.get(newPopulation.get(newPopulation.size()-1));
							}
						}
					}
				}
			}

			/*
			 * Randomly include old chromosomes below the threshold
			 */
			if(includeBadRate > _random.nextDouble()) {
				Collections.shuffle(population);
				for(ArrayList<Integer> chromosome : population) {
					if(currentParamScore.get(chromosome) < threshold) {
						newPopulation.add(chromosome);
						break;
					}
				}
			}

			/*
			 * Create rest of population through reproduction
			 */
			double totScore = 0;
			for(ArrayList<Integer> chromosome : population) {
				if(currentParamScore.get(chromosome) == null) {
					System.out.println("?");
					System.out.println(chromosome);
					System.out.println(population);
					System.out.println(currentParamScore);
				}
				totScore += currentParamScore.get(chromosome);
			}
			while(newPopulation.size() < popSize) {
				ArrayList<Integer> parent1 = getRandomParent(population,currentParamScore,totScore);
				ArrayList<Integer> parent2 = getRandomParent(population,currentParamScore,totScore);
				while(parent1.equals(parent2)) {
					parent2 = getRandomParent(population,currentParamScore,totScore);
				}
				ArrayList<ArrayList<Integer>> children = reproduce(parent1, parent2);

				for(ArrayList<Integer> child : children) {
					/*
					 * Mutate?
					 */
					for(int idx = 0; idx < child.size(); idx++) {
						if(mutationRate > _random.nextDouble()) {
							child.remove(idx);
							child.add(idx, _random.nextInt(_parameters.get(idx).size()));
						}
					}
					newPopulation.add(child);
				}
			}

			population = newPopulation;
			System.out.print(_agent + "(");
			for(int i = 0; i < bestParams.size(); i++) {
				System.out.print(_parameters.get(i).get(bestParams.get(i)) + ",");
			}
			System.out.println("), " + "Iter " + iter +", Top Score: " + paramScore.get(bestParams));
		}


		/*
		 * Pick best chromosome in resulting population
		 */
		for(int i = 0; i < population.size(); i++) {
			ArrayList<Integer> chromosome = population.get(i);
			Double score = paramScore.get(chromosome);
			if(score == null) {
				score = evaluateAgent(createCopy(chromosome));
			}
			if(score > bestScore) {
				bestScore = score;
				bestParams = chromosome;
			}
		}
		System.out.print(_agent + "(");
		for(int i = 0; i < bestParams.size(); i++) {
			System.out.print(_parameters.get(i).get(bestParams.get(i)) + ",");
		}
		System.out.println("), " + "FINAL, Top Score: " + paramScore.get(bestParams));
		return bestParams;
	}

	public ArrayList<Integer> getRandomParent(ArrayList<ArrayList<Integer>> population, HashMap<ArrayList<Integer>,Double> currentParamScore, double totScore) {
		double rand = _random.nextDouble();
		for(int i = 0; i < population.size(); i++) {
			ArrayList<Integer> chromosome = population.get(i);
			double prob = currentParamScore.get(chromosome)/totScore;
			if(prob > rand) {
				return chromosome;
			}
			else {
				rand -= prob;
			}
		}
		System.out.println("I prob shouldn't happen");
		return population.get(_random.nextInt(population.size()));
	}

	public ArrayList<ArrayList<Integer>> reproduce(ArrayList<Integer> parent1, ArrayList<Integer> parent2) {
		/*
		 * Uniform Crossover(UX)
		 */
		double uniformProb = .5;
		ArrayList<Integer> child1 = new ArrayList<Integer>();
		ArrayList<Integer> child2 = new ArrayList<Integer>();
		for(int i = 0; i < parent1.size(); i++) {
			double rand = _random.nextDouble();
			if(uniformProb > rand) {
				child1.add(parent1.get(i));
				child2.add(parent2.get(i));
			}
			else {
				child2.add(parent1.get(i));
				child1.add(parent2.get(i));
			}
		}
		ArrayList<ArrayList<Integer>> children = new ArrayList<ArrayList<Integer>>();
		children.add(child1);
		children.add(child2);
		return children;
	}

	public double evaluateAgent(AbstractAgent agent) {
		BasicSimulator sim = new BasicSimulator();
		double val = 0;
		try {
			val = sim.runSimulations(baseFile,_min,_max,0,0, agent);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return val;
	}

	public AbstractAgent createCopy(ArrayList<Integer> paramsIndices) {
		Class<? extends AbstractAgent> c = _agent.getClass();
		Constructor[] constr = c.getConstructors();
		Object[] args = new Object[_parameters.size()];
		for(int i = 0; i < args.length; i++) {
			args[i] = _parameters.get(i).get(paramsIndices.get(i));
		}

		AbstractAgent agentCopy = null;

		try {
			agentCopy = (AbstractAgent)(constr[0].newInstance(args));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return agentCopy;
	}

	public static void main(String[] args) {
		//		AbstractAgent adjustPM = new AdjustPR(1.1, .9, .4, .9, 1.1, .1, .9, 1.0);
		//		ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//
		//		ArrayList<Double> incTS = new ArrayList<Double>();
		//		for(double val = 1.05; val < 1.3; val += .05) {
		//			incTS.add(val);
		//		}
		//		parameters.add(incTS);
		//
		//		ArrayList<Double> decTS = new ArrayList<Double>();
		//		for(double val = .95; val > .7; val -= .05) {
		//			decTS.add(val);
		//		}
		//		parameters.add(decTS);
		//
		//		ArrayList<Double> initPM = new ArrayList<Double>();
		//		for(double val = .4; val < .71; val += .1) {
		//			initPM.add(val);
		//		}
		//		parameters.add(initPM);
		//
		//		ArrayList<Double> decPM = new ArrayList<Double>();
		//		for(double val = .95; val > .7; val -= .05) {
		//			decPM.add(val);
		//		}
		//		parameters.add(decPM);
		//
		//		ArrayList<Double> incPM = new ArrayList<Double>();
		//		for(double val = 1.05; val < 1.3; val += .05) {
		//			incPM.add(val);
		//		}
		//		parameters.add(incPM);
		//
		//		ArrayList<Double> minPM = new ArrayList<Double>();
		//		for(double val = .1; val < .41; val += .1) {
		//			minPM.add(val);
		//		}
		//		parameters.add(minPM);
		//
		//		ArrayList<Double> maxPM = new ArrayList<Double>();
		//		for(double val = .7; val < .91; val += .1) {
		//			maxPM.add(val);
		//		}
		//		parameters.add(maxPM);
		//
		//		ArrayList<Double> budgetModifier = new ArrayList<Double>();
		//		for(double val = .9; val < 1.5; val += .1) {
		//			budgetModifier.add(val);
		//		}
		//		parameters.add(budgetModifier);
		//
		//		AgentTuner tuner = new AgentTuner(adjustPM,parameters);
		//		tuner.solveWithGA(8, 2, 25, 8);

		//		AbstractAgent equatePM = new EquatePM(.4, .9, 1.1, .1, .9, 1.0);
		//		ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//
		//		ArrayList<Double> initPM = new ArrayList<Double>();
		//		for(double val = .4; val < .71; val += .1) {
		//			initPM.add(val);
		//		}
		//		parameters.add(initPM);
		//
		//		ArrayList<Double> decPM = new ArrayList<Double>();
		//		for(double val = .95; val > .7; val -= .05) {
		//			decPM.add(val);
		//		}
		//		parameters.add(decPM);
		//
		//		ArrayList<Double> incPM = new ArrayList<Double>();
		//		for(double val = 1.05; val < 1.3; val += .05) {
		//			incPM.add(val);
		//		}
		//		parameters.add(incPM);
		//
		//		ArrayList<Double> minPM = new ArrayList<Double>();
		//		for(double val = .1; val < .41; val += .1) {
		//			minPM.add(val);
		//		}
		//		parameters.add(minPM);
		//
		//		ArrayList<Double> maxPM = new ArrayList<Double>();
		//		for(double val = .7; val < .91; val += .1) {
		//			maxPM.add(val);
		//		}
		//		parameters.add(maxPM);
		//
		//		ArrayList<Double> budgetModifier = new ArrayList<Double>();
		//		for(double val = .9; val < 1.5; val += .1) {
		//			budgetModifier.add(val);
		//		}
		//		parameters.add(budgetModifier);
		//
		//		AgentTuner tuner = new AgentTuner(equatePM,parameters);
		//		tuner.solveWithGA(8, 2, 25, 8);

		//		AbstractAgent equatePR = new EquatePR(1.5, .9, 1.1, 1.0, 5.0, 1.0);
		//		ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//
		//		ArrayList<Double> initPR = new ArrayList<Double>();
		//		for(double val = 1.2; val < 2.8; val += .2) {
		//			initPR.add(val);
		//		}
		//		parameters.add(initPR);
		//
		//		ArrayList<Double> decPR = new ArrayList<Double>();
		//		for(double val = .95; val > .7; val -= .05) {
		//			decPR.add(val);
		//		}
		//		parameters.add(decPR);
		//
		//		ArrayList<Double> incPR = new ArrayList<Double>();
		//		for(double val = 1.05; val < 1.3; val += .05) {
		//			incPR.add(val);
		//		}
		//		parameters.add(incPR);
		//
		//		ArrayList<Double> minPR = new ArrayList<Double>();
		//		for(double val = 1.0; val < 1.4; val += .1) {
		//			minPR.add(val);
		//		}
		//		parameters.add(minPR);
		//
		//		ArrayList<Double> maxPR = new ArrayList<Double>();
		//		for(double val = 3.0; val < 5.0; val += .25) {
		//			maxPR.add(val);
		//		}
		//		parameters.add(maxPR);
		//
		//		ArrayList<Double> budgetModifier = new ArrayList<Double>();
		//		for(double val = .9; val < 1.5; val += .1) {
		//			budgetModifier.add(val);
		//		}
		//		parameters.add(budgetModifier);
		//
		//		AgentTuner tuner = new AgentTuner(equatePR,parameters);
		//		tuner.solveWithGA(8, 2, 25, 8);


		AbstractAgent equatePPS = new EquatePPS(7.5, .9, 1.1, 4, 15, 1.0);
		ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();

		ArrayList<Double> initPPS = new ArrayList<Double>();
		for(double val = 4; val <= 7; val++) {
			initPPS.add(val);
		}
		parameters.add(initPPS);

		ArrayList<Double> decPPS = new ArrayList<Double>();
		for(double val = .95; val > .7; val -= .05) {
			decPPS.add(val);
		}
		parameters.add(decPPS);

		ArrayList<Double> incPPS = new ArrayList<Double>();
		for(double val = 1.05; val < 1.3; val += .05) {
			incPPS.add(val);
		}
		parameters.add(incPPS);

		ArrayList<Double> minPPS = new ArrayList<Double>();
		for(double val = 3; val <= 8; val++) {
			minPPS.add(val);
		}
		parameters.add(minPPS);

		ArrayList<Double> maxPPS = new ArrayList<Double>();
		for(double val = 11; val <= 15; val ++) {
			maxPPS.add(val);
		}
		parameters.add(maxPPS);

		ArrayList<Double> budgetModifier = new ArrayList<Double>();
		for(double val = .9; val < 1.5; val += .1) {
			budgetModifier.add(val);
		}
		parameters.add(budgetModifier);

		AgentTuner tuner = new AgentTuner(equatePPS,parameters);
		tuner.solveWithGA(1, 1, 25, 1);
	}

}
