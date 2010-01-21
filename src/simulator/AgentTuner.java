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
import agents.rulebased.AdjustPM;
import agents.rulebased.AdjustPPS;
import agents.rulebased.AdjustPR;
import agents.rulebased.AdjustROI;
import agents.rulebased.EquatePM;
import agents.rulebased.EquatePPS;
import agents.rulebased.EquatePR;
import agents.rulebased.EquateROI;

public class AgentTuner {

	String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
	//	String baseFile = "/pro/aa/finals/day-2/server-1/game";
	int _min = 1430;
	int _max = 172;
	private AbstractAgent _agent;
	private ArrayList<ArrayList<Double>> _parameters;
	private Random _random;
	double mutationRate = .1;
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
				System.out.println("chromosome: " + chromosome);
				System.out.println("score: " + score);
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
		/*
		 * ENSURE GARBAGE COLLECTOR IS RUN BETWEEN ITERATIONS
		 */
		System.gc(); sim.emptyFunction(); sim.emptyFunction(); sim.emptyFunction(); sim.emptyFunction();


		try {
			val = sim.runSimulations(baseFile,_min,_max,0,0, agent);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if(Double.isNaN(val) || val < 0) {
			return 0.0;
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
		//		AbstractAgent adjustPM = new AdjustPM(0, 0, 0, 0, 0, 0, 0, 0, 0);
		//		ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//
		//		ArrayList<Double> alphaIncTS = new ArrayList<Double>();
		//		for(double val = -.01; val <= .011; val += .001) {
		//			alphaIncTS.add(val);
		//		}
		//		parameters.add(alphaIncTS);
		//
		//		ArrayList<Double> betaIncTS = new ArrayList<Double>();
		//		for(double val = -.3; val <= .31; val += .033333) {
		//			betaIncTS.add(val);
		//		}
		//		parameters.add(betaIncTS);
		//
		//		ArrayList<Double> alphaDecTS = new ArrayList<Double>();
		//		for(double val = -.01; val <= .011; val += .001) {
		//			alphaDecTS.add(val);
		//		}
		//		parameters.add(alphaDecTS);
		//
		//		ArrayList<Double> betaDecTS = new ArrayList<Double>();
		//		for(double val = -.3; val <= .31; val += .033333) {
		//			betaDecTS.add(val);
		//		}
		//		parameters.add(betaDecTS);
		//
		//		ArrayList<Double> initPM = new ArrayList<Double>();
		//		for(double val = .1; val < .91; val += .05) {
		//			initPM.add(val);
		//		}
		//		parameters.add(initPM);
		//
		//		ArrayList<Double> alphaIncPM = new ArrayList<Double>();
		//		for(double val = -.01; val <= .011; val += .001) {
		//			alphaIncPM.add(val);
		//		}
		//		parameters.add(alphaIncPM);
		//
		//		ArrayList<Double> betaIncPM = new ArrayList<Double>();
		//		for(double val = -.3; val <= .31; val += .033333) {
		//			betaIncPM.add(val);
		//		}
		//		parameters.add(betaIncPM);
		//
		//		ArrayList<Double> alphaDecPM = new ArrayList<Double>();
		//		for(double val = -.01; val <= .011; val += .001) {
		//			alphaDecPM.add(val);
		//		}
		//		parameters.add(alphaDecPM);
		//
		//		ArrayList<Double> betaDecPM = new ArrayList<Double>();
		//		for(double val = -.3; val <= .31; val += .033333) {
		//			betaDecPM.add(val);
		//		}
		//		parameters.add(betaDecPM);
		//
		//		AgentTuner tuner = new AgentTuner(adjustPM,parameters);
		//		tuner.solveWithGA(25, 10, 100, 12);



		//										AbstractAgent adjustPR = new AdjustPR(0, 0, 0, 0, 0, 0, 0, 0, 0);
		//										ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//								
		//										ArrayList<Double> alphaIncTS = new ArrayList<Double>();
		//										for(double val = -.01; val <= .011; val += .001) {
		//											alphaIncTS.add(val);
		//										}
		//										parameters.add(alphaIncTS);
		//								
		//										ArrayList<Double> betaIncTS = new ArrayList<Double>();
		//										for(double val = -.3; val <= .31; val += .033333) {
		//											betaIncTS.add(val);
		//										}
		//										parameters.add(betaIncTS);
		//								
		//										ArrayList<Double> alphaDecTS = new ArrayList<Double>();
		//										for(double val = -.01; val <= .011; val += .001) {
		//											alphaDecTS.add(val);
		//										}
		//										parameters.add(alphaDecTS);
		//								
		//										ArrayList<Double> betaDecTS = new ArrayList<Double>();
		//										for(double val = -.3; val <= .31; val += .033333) {
		//											betaDecTS.add(val);
		//										}
		//										parameters.add(betaDecTS);
		//								
		//										ArrayList<Double> initPR = new ArrayList<Double>();
		//										for(double val = 1.5; val < 3.5; val += .2) {
		//											initPR.add(val);
		//										}
		//										parameters.add(initPR);
		//								
		//										ArrayList<Double> alphaIncPR = new ArrayList<Double>();
		//										for(double val = -.01; val <= .011; val += .001) {
		//											alphaIncPR.add(val);
		//										}
		//										parameters.add(alphaIncPR);
		//								
		//										ArrayList<Double> betaIncPR = new ArrayList<Double>();
		//										for(double val = -.3; val <= .31; val += .033333) {
		//											betaIncPR.add(val);
		//										}
		//										parameters.add(betaIncPR);
		//								
		//										ArrayList<Double> alphaDecPR = new ArrayList<Double>();
		//										for(double val = -.01; val <= .011; val += .001) {
		//											alphaDecPR.add(val);
		//										}
		//										parameters.add(alphaDecPR);
		//								
		//										ArrayList<Double> betaDecPR = new ArrayList<Double>();
		//										for(double val = -.3; val <= .31; val += .033333) {
		//											betaDecPR.add(val);
		//										}
		//										parameters.add(betaDecPR);
		//								
		//										AgentTuner tuner = new AgentTuner(adjustPR,parameters);
		//										tuner.solveWithGA(25, 10, 100, 12);




		//								AbstractAgent adjustPPS = new AdjustPPS(0, 0, 0, 0, 0, 0, 0, 0, 0);
		//								ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//						
		//								ArrayList<Double> alphaIncTS = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaIncTS.add(val);
		//								}
		//								parameters.add(alphaIncTS);
		//						
		//								ArrayList<Double> betaIncTS = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaIncTS.add(val);
		//								}
		//								parameters.add(betaIncTS);
		//						
		//								ArrayList<Double> alphaDecTS = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaDecTS.add(val);
		//								}
		//								parameters.add(alphaDecTS);
		//						
		//								ArrayList<Double> betaDecTS = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaDecTS.add(val);
		//								}
		//								parameters.add(betaDecTS);
		//						
		//								ArrayList<Double> initPPS = new ArrayList<Double>();
		//								for(double val = 5; val <= 12; val += .5) {
		//									initPPS.add(val);
		//								}
		//								parameters.add(initPPS);
		//						
		//								ArrayList<Double> alphaIncPPS = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaIncPPS.add(val);
		//								}
		//								parameters.add(alphaIncPPS);
		//						
		//								ArrayList<Double> betaIncPPS = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaIncPPS.add(val);
		//								}
		//								parameters.add(betaIncPPS);
		//						
		//								ArrayList<Double> alphaDecPPS = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaDecPPS.add(val);
		//								}
		//								parameters.add(alphaDecPPS);
		//						
		//								ArrayList<Double> betaDecPPS = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaDecPPS.add(val);
		//								}
		//								parameters.add(betaDecPPS);
		//						
		//								AgentTuner tuner = new AgentTuner(adjustPPS,parameters);
		//								tuner.solveWithGA(25, 10, 100, 12);	





		//						AbstractAgent adjustROI = new AdjustROI(0, 0, 0, 0, 0, 0, 0, 0, 0);
		//						ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//				
		//						ArrayList<Double> alphaIncTS = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaIncTS.add(val);
		//						}
		//						parameters.add(alphaIncTS);
		//				
		//						ArrayList<Double> betaIncTS = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaIncTS.add(val);
		//						}
		//						parameters.add(betaIncTS);
		//				
		//						ArrayList<Double> alphaDecTS = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaDecTS.add(val);
		//						}
		//						parameters.add(alphaDecTS);
		//				
		//						ArrayList<Double> betaDecTS = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaDecTS.add(val);
		//						}
		//						parameters.add(betaDecTS);
		//				
		//						ArrayList<Double> initROI = new ArrayList<Double>();
		//						for(double val = 1.5; val < 3.5; val += .2) {
		//							initROI.add(val);
		//						}
		//						parameters.add(initROI);
		//				
		//						ArrayList<Double> alphaIncROI = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaIncROI.add(val);
		//						}
		//						parameters.add(alphaIncROI);
		//				
		//						ArrayList<Double> betaIncROI = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaIncROI.add(val);
		//						}
		//						parameters.add(betaIncROI);
		//				
		//						ArrayList<Double> alphaDecROI = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaDecROI.add(val);
		//						}
		//						parameters.add(alphaDecROI);
		//				
		//						ArrayList<Double> betaDecROI = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaDecROI.add(val);
		//						}
		//						parameters.add(betaDecROI);
		//				
		//						AgentTuner tuner = new AgentTuner(adjustROI,parameters);
		//						tuner.solveWithGA(25, 10, 100, 12);


		//								AbstractAgent equatePM = new EquatePM(0,0,0,0,0);
		//								ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//						
		//								ArrayList<Double> initPM = new ArrayList<Double>();
		//								for(double val = .1; val < .91; val += .05) {
		//									initPM.add(val);
		//								}
		//								parameters.add(initPM);
		//						
		//								ArrayList<Double> alphaIncPM = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaIncPM.add(val);
		//								}
		//								parameters.add(alphaIncPM);
		//						
		//								ArrayList<Double> betaIncPM = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaIncPM.add(val);
		//								}
		//								parameters.add(betaIncPM);
		//						
		//								ArrayList<Double> alphaDecPM = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaDecPM.add(val);
		//								}
		//								parameters.add(alphaDecPM);
		//						
		//								ArrayList<Double> betaDecPM = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaDecPM.add(val);
		//								}
		//								parameters.add(betaDecPM);
		//						
		//								AgentTuner tuner = new AgentTuner(equatePM,parameters);
		//								tuner.solveWithGA(25, 10, 100, 12);



		//		
		//								AbstractAgent equatePR = new EquatePR(0,0,0,0,0);
		//								ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//						
		//								ArrayList<Double> initPR = new ArrayList<Double>();
		//								for(double val = 1.5; val < 3.5; val += .2) {
		//									initPR.add(val);
		//								}
		//								parameters.add(initPR);
		//						
		//								ArrayList<Double> alphaIncPR = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaIncPR.add(val);
		//								}
		//								parameters.add(alphaIncPR);
		//						
		//								ArrayList<Double> betaIncPR = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaIncPR.add(val);
		//								}
		//								parameters.add(betaIncPR);
		//						
		//								ArrayList<Double> alphaDecPR = new ArrayList<Double>();
		//								for(double val = -.01; val <= .011; val += .001) {
		//									alphaDecPR.add(val);
		//								}
		//								parameters.add(alphaDecPR);
		//						
		//								ArrayList<Double> betaDecPR = new ArrayList<Double>();
		//								for(double val = -.3; val <= .31; val += .033333) {
		//									betaDecPR.add(val);
		//								}
		//								parameters.add(betaDecPR);
		//						
		//								AgentTuner tuner = new AgentTuner(equatePR,parameters);
		//								tuner.solveWithGA(25, 10, 100, 12);






		//						AbstractAgent equatePPS = new EquatePPS(0,0,0,0,0);
		//						ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
		//				
		//						ArrayList<Double> initPPS = new ArrayList<Double>();
		//						for(double val = 5; val <= 12; val += .5) {
		//							initPPS.add(val);
		//						}
		//						parameters.add(initPPS);
		//				
		//						ArrayList<Double> alphaIncPPS = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaIncPPS.add(val);
		//						}
		//						parameters.add(alphaIncPPS);
		//				
		//						ArrayList<Double> betaIncPPS = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaIncPPS.add(val);
		//						}
		//						parameters.add(betaIncPPS);
		//				
		//						ArrayList<Double> alphaDecPPS = new ArrayList<Double>();
		//						for(double val = -.01; val <= .011; val += .001) {
		//							alphaDecPPS.add(val);
		//						}
		//						parameters.add(alphaDecPPS);
		//				
		//						ArrayList<Double> betaDecPPS = new ArrayList<Double>();
		//						for(double val = -.3; val <= .31; val += .033333) {
		//							betaDecPPS.add(val);
		//						}
		//						parameters.add(betaDecPPS);
		//				
		//						AgentTuner tuner = new AgentTuner(equatePPS,parameters);
		//						tuner.solveWithGA(25, 10, 100, 12);


//						AbstractAgent equateROI = new EquateROI(0,0,0,0,0);
//						ArrayList<ArrayList<Double>> parameters = new ArrayList<ArrayList<Double>>();
//				
//						ArrayList<Double> initROI = new ArrayList<Double>();
//						for(double val = 1.5; val < 3.5; val += .2) {
//							initROI.add(val);
//						}
//						parameters.add(initROI);
//				
//						ArrayList<Double> alphaIncROI = new ArrayList<Double>();
//						for(double val = -.01; val <= .011; val += .001) {
//							alphaIncROI.add(val);
//						}
//						parameters.add(alphaIncROI);
//				
//						ArrayList<Double> betaIncROI = new ArrayList<Double>();
//						for(double val = -.3; val <= .31; val += .033333) {
//							betaIncROI.add(val);
//						}
//						parameters.add(betaIncROI);
//				
//						ArrayList<Double> alphaDecROI = new ArrayList<Double>();
//						for(double val = -.01; val <= .011; val += .001) {
//							alphaDecROI.add(val);
//						}
//						parameters.add(alphaDecROI);
//				
//						ArrayList<Double> betaDecROI = new ArrayList<Double>();
//						for(double val = -.3; val <= .31; val += .033333) {
//							betaDecROI.add(val);
//						}
//						parameters.add(betaDecROI);
//				
//						AgentTuner tuner = new AgentTuner(equateROI,parameters);
//						tuner.solveWithGA(25, 10, 100, 12);
	}

}
