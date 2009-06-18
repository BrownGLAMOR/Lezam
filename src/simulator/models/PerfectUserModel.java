package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import newmodels.AbstractModel;
import newmodels.usermodel.AbstractUserModel;

import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectUserModel extends AbstractUserModel {
	

	private HashMap<Product, HashMap<UserState, Integer>> _users;

	public PerfectUserModel(int numUsers, HashMap<Product, HashMap<UserState, Integer>> users) {
		_users = users;
	}

	@Override
	public int getPrediction(Product product, UserState state) {
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
	
}
