package simulator;

import java.util.Random;

import usermodel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;

public class SimUser {

	Random _R = new Random();

	private Product _prod;
	private UserState _state;
	private static final boolean forceQuery = true;
	private Query _query;

	public SimUser(Product prod, UserState state) {
		_prod = prod;
		_state = state;
		_query = null;
		if(forceQuery) {
			_query = generateQuery();
		}
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
		if(_state == UserState.NS || _state == UserState.T) {
			return null;
		}
		else if(_state == UserState.F0) {
			return new Query(null,null);
		}
		else if(_state == UserState.F1) {
			double rand = _R.nextDouble();
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
			double rand = _R.nextDouble();
			if(rand <= (1.0/3.0)) {
				return new Query(null,null);
			}
			else if(rand <= (2.0/3.0)) {
				double rand2 = _R.nextDouble();
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
