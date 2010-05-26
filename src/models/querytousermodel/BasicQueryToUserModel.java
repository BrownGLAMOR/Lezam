package models.querytousermodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.AbstractModel;
import models.usermodel.AbstractUserModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicQueryToUserModel extends AbstractQueryToUserModel {
	
	private AbstractUserModel _userModel;
	private Set<Product> _products;
	private HashMap<Query,HashMap<UserState,Integer>> _numUsers;
	private Set<Query> _querySpace;
	
	public BasicQueryToUserModel(AbstractUserModel userModel) {
		_userModel = userModel;
		_products = new HashSet<Product>();
		_querySpace = new HashSet<Query>();
		_numUsers = new HashMap<Query,HashMap<UserState,Integer>>();
		
		//Initialize products
		_products.add(new Product("pg","tv"));
		_products.add(new Product("pg","dvd"));
		_products.add(new Product("pg","audio"));
		_products.add(new Product("lioneer","tv"));
		_products.add(new Product("lioneer","dvd"));
		_products.add(new Product("lioneer","audio"));
		_products.add(new Product("flat","tv"));
		_products.add(new Product("flat","dvd"));
		_products.add(new Product("flat","audio"));
		
		//Initialize Query Space
        _querySpace.add(new Query(null, null));
        for(Product product : _products) {
            // The F1 query classes
            // F1 Manufacturer only
            _querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            _querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
        }
	}

	@Override
	public int getPrediction(Query q, UserState userState, int day) {
        //Set num impressions per query
        for(Query query : _querySpace) {
        	int numIS = 0;
        	int numF0 = 0;
        	int numF1 = 0;
        	int numF2 = 0;
        	for(Product product : _products) {
        		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
        			numF0 += _userModel.getPrediction(product, UserState.F0,day);
        			numIS += _userModel.getPrediction(product, UserState.IS,day) / 3;
        		}
        		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
        			if(product.getComponent().equals(query.getComponent()) || product.getManufacturer().equals(query.getManufacturer())) {
        				numF1 += _userModel.getPrediction(product, UserState.F1,day) / 2;
        				numIS += _userModel.getPrediction(product, UserState.IS,day) / 6;
        			}
        		}
        		else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
        			if(product.getComponent().equals(query.getComponent()) && product.getManufacturer().equals(query.getManufacturer())) {
        				numF2 += _userModel.getPrediction(product, UserState.F2,day);
        				numIS += _userModel.getPrediction(product, UserState.IS,day)/3;
        			}
        		}
        	}
        	HashMap<UserState,Integer> users = new HashMap<UserState,Integer>();
        	users.put(UserState.IS, numIS);
        	users.put(UserState.F0, numF0);
        	users.put(UserState.F1, numF1);
        	users.put(UserState.F2, numF2);
        	_numUsers.put(query, users);
        }
		return _numUsers.get(q).get(userState);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new BasicQueryToUserModel(_userModel);
	}

}
