package newmodels.slottonumimp;

import usermodel.UserState;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicSlotToNumImp extends AbstractSlotToNumImp {
	
	private AbstractUserModel _userModel;
	private int numUsers = 90000;

	public BasicSlotToNumImp(Query query, AbstractUserModel userModel) {
		super(query);
		_userModel = userModel;
	}

	@Override
	public int getPrediction(double slot) {
		double numFocusUsers;
		double numISUsers;
		int numImp;
		UserState userstate;
		if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			userstate = UserState.F0;
			/*
			 * All F0 users regardless of product preference make the same query
			 * 1/3 of all IS users can make a F0 query
			 */
			numFocusUsers = _userModel.getPrediction(userstate)*numUsers;
			numISUsers = _userModel.getPrediction(UserState.IS)*numUsers;
			numImp = (int) (numFocusUsers + numISUsers/3.0);
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			userstate = UserState.F1;
			/*
			 * F1 users make one of 6 queries, but for each query there are
			 * 3 different underlying product preferences that could have made
			 * the same query
			 * 
			 * 1/3 of all IS users make F1 queries and then do the same as
			 * noted just above here
			 */
			numFocusUsers = _userModel.getPrediction(userstate)*numUsers;
			numISUsers = _userModel.getPrediction(UserState.IS)*numUsers;
			numImp = (int) ((numFocusUsers/6.0)*3.0 + ((numISUsers/3.0)/6.0)*3.0);
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			/*
			 * F2 users make one of 9 queries
			 * 
			 * 1/3 of all IS users make F1 queries and then do the as
			 * noted just above here
			 */
			userstate = UserState.F2;
			numFocusUsers = _userModel.getPrediction(userstate)*numUsers;
			numISUsers = _userModel.getPrediction(UserState.IS)*numUsers;
			numImp = (int) (numFocusUsers/9.0 + (numISUsers/3.0)/9.0);
		}
		else {
			throw new RuntimeException("Malformed query");
		}
		return numImp;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing to do
		return true;
	}

}
