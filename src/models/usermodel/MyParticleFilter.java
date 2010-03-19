package models.usermodel;

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


public class MyParticleFilter extends TacTexAbstractUserModel {

	private long _seed = 1263456;
	private Random _R;
	private ArrayList<Product> _products;


	public MyParticleFilter() {
		_R = new Random(_seed);
		_particles = new HashMap<Product,Particle[]>();
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
			if(count != NUM_PARTICLES-1) {
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

	@Override
	public int getPrediction(Product product, UserState userState) {
		/*
		 * Don't worry about this until week 2
		 */
		return 0;
	}
	
	@Override
	public int getCurrentEstimate(Product product, UserState userState) {
		return 0;
	}

	@Override
	public boolean updateModel(HashMap<Query, Integer> totalImpressions) {
		/*
		 * Don't worry about this until week 2
		 */
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new MyParticleFilter();
	}

}
