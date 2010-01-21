package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.HashMap;

import newmodels.AbstractModel;
import newmodels.oldusermodel.UserState;
import newmodels.usermodel.AbstractUserModel;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectUserModel extends AbstractUserModel {
	

	private HashMap<Product, HashMap<UserState, Integer>> _users;
	private int _numUsers;

	public PerfectUserModel(int numUsers, HashMap<Product, HashMap<UserState, Integer>> users) {
		_users = users;
		_numUsers = numUsers;
	}

	@Override
	public int getPrediction(Product product, UserState state, int day) {
		/*
		 * The incoming info for a User model should always be a UserState and
		 * we will return the ratio of users in that state
		 */
		return _users.get(product).get(state);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectUserModel(_numUsers, _users);
	}
	
}
