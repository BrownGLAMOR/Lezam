package simulator;

import java.util.Random;

import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

public class SimUser {

	private Product _prod;
	private UserState _state;
	private Query _query;
	private boolean forceQuery;
	private long _seed;

	public SimUser(Product prod, UserState state) {
		_prod = prod;
		_state = state;
		forceQuery = false;
	}
	
	public SimUser(Product prod, UserState state, long seed) {
		this(prod,state);
		forceQuery = true;
		_seed = seed;
		_query = generateQuery();
	}

	public Product getProduct() {
		return _prod;
	}

	public UserState getUserState() {
		return _state;
	}

	public Query getUserQuery() {
		if(forceQuery) {
			return _query;
		}
		else {
			return generateQuery();
		}
	}

	public Query generateQuery() {
		Random random;
		if(forceQuery) {
			random = new Random(_seed);
		}
		else {
			random = new Random();
		}
		double rand = random.nextDouble();
		if(_state == UserState.NS || _state == UserState.T) {
			return null;
		}
		else if(_state == UserState.F0) {
			return new Query(null,null);
		}
		else if(_state == UserState.F1) {
			if(rand <= .5) {
				return new Query(null,_prod.getComponent());
			}
			else {
				return new Query(_prod.getManufacturer(),null);
			}
		}
		else if(_state == UserState.F2) {
			return new Query(_prod.getManufacturer(),_prod.getComponent());
		}
		else if(_state == UserState.IS) {
			if(rand <= (1.0/3.0)) {
				return new Query(null,null);
			}
			else if(rand <= (2.0/3.0)) {
				double rand2 = random.nextDouble();
				if(rand2 <= .5) {
					return new Query(null,_prod.getComponent());
				}
				else {
					return new Query(_prod.getManufacturer(),null);
				}
			}
			else {
				return new Query(_prod.getManufacturer(),_prod.getComponent());
			}
		}
		else {
			throw new RuntimeException("Malformed Query");
		}
	}
}
