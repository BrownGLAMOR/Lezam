package models.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import models.usermodel.ParticleFilterAbstractUserModel.Particle;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

public class ParamParticleFilter {
	
	public static final int NUM_PARTICLES = 1000;
	private ArrayList<Query> _queries;
	public HashMap<Query,ParamParticle[]> _particles;
	
	public ParamParticleFilter(){
		 _queries.add(new Query(null, null));
		 _queries.add(new Query("lioneer", null));
		 _queries.add(new Query(null, "tv"));
		 _queries.add(new Query("lioneer", "tv"));
		 _queries.add(new Query(null, "audio"));
		 _queries.add(new Query("lioneer", "audio"));
		 _queries.add(new Query(null, "dvd"));
		 _queries.add(new Query("lioneer", "dvd"));
		 _queries.add(new Query("pg", null));
		 _queries.add(new Query("pg", "tv"));
		 _queries.add(new Query("pg", "audio"));
		 _queries.add(new Query("pg", "dvd"));
		 _queries.add(new Query("flat", null));
		 _queries.add(new Query("flat", "tv"));
		 _queries.add(new Query("flat", "audio"));
		 _queries.add(new Query("flat", "dvd"));
		 
		 InitializeParticles();
	}
	
	public void InitializeParticles(){
		//For each query type
		for(Query q : _queries) {
			//Make an array of ~1000 ParamParticles
			ParamParticle[] particles = new ParamParticle[NUM_PARTICLES];
			for(int i = 0; i < particles.length; i++) {
				ParamParticle particle = new ParamParticle(q);
				particles[i] = particle;
			}
			//Push this into HashMap _particles
			// q = key
			// particles = value
			_particles.put(q, particles);
		}
	}
	
	public void updateParticles(){
		//using some input
		//reweight particles
		//resample and propogate particles
	}
	
	public double estimateContinuation(Query q){
		//take a weighted average of particles for q
		return 0.0;
	}
	
	public double[] advertiserEffect(Query q){
		//take a weighted average of advertiser Effects for Query q
		return new double[8];
	}

}
