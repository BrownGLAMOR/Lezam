package models.parameters;

import java.util.Random;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class ParamParticle {
	
	public Query query;
	public double continuationProbability;
	public double[] advertiserEffects;
	
	public ParamParticle(Query q, double contProb, double[] aEs){
		query = q;
		continuationProbability = contProb;
		advertiserEffects = aEs;
	}
	
	public ParamParticle(Query q){
		advertiserEffects = new double[8];
		QueryType qt = q.getType();
		if(qt==QueryType.FOCUS_LEVEL_TWO){
			Random random = new Random();
			continuationProbability = 0.4 + 0.3*random.nextDouble();
			for(int i = 0; i < advertiserEffects.length; i++){
				advertiserEffects[i] = 0.4+0.1*random.nextDouble();
			}
		}
		if(qt==QueryType.FOCUS_LEVEL_ONE){
			Random random = new Random();
			continuationProbability = 0.3 + 0.3*random.nextDouble();
			for(int i = 0; i < advertiserEffects.length; i++){
				advertiserEffects[i] = 0.3+0.1*random.nextDouble();
			}
		}
		if(qt==QueryType.FOCUS_LEVEL_ZERO){
			Random random = new Random();
			continuationProbability = 0.2 + 0.3*random.nextDouble();
			for(int i = 0; i < advertiserEffects.length; i++){
				advertiserEffects[i] = 0.2+0.1*random.nextDouble();
			}
		}
	}

}
