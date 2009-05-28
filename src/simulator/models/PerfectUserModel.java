package simulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import modelers.AbstractModel;

public class PerfectUserModel extends AbstractModel {
	
	private int _numUsers;

	private HashMap<UserState, Double> _users;

	public PerfectUserModel(int numUsers, HashMap<UserState,Double> users) {
		_numUsers = numUsers;
		_users = users;
	}

	@Override
	public Object getPrediction(Object info) {
		/*
		 * The incoming info for a User model should always be a UserState and
		 * we will return the ratio of users in that state
		 */
		UserState state = (UserState) info;
		return _users.get(state)/_numUsers;
	}

	@Override
	public void updateModel(QueryReport queryReport, SalesReport salesReport, Object otherInfo) {
		//Nothing needs to be updated
	}
	
	public int getNumUsers() {
		return _numUsers;
	}

	public void setNumUsers(int numUsers) {
		_numUsers = numUsers;
	}

}
