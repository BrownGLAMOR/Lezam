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
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectUserModel extends AbstractUserModel {
	
	private int _numUsers;

	private HashMap<UserState, Double> _users;

	public PerfectUserModel(int numUsers, HashMap<UserState,Double> users) {
		_numUsers = numUsers;
		_users = users;
	}

	@Override
	public double getPrediction(UserState info) {
		/*
		 * The incoming info for a User model should always be a UserState and
		 * we will return the ratio of users in that state
		 */
		UserState state = (UserState) info;
		return _users.get(state)/_numUsers;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
		return true;
	}
	
	public int getNumUsers() {
		return _numUsers;
	}

	public void setNumUsers(int numUsers) {
		_numUsers = numUsers;
	}

}
