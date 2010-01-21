package models.querytousermodel;

import models.AbstractModel;
import models.oldusermodel.UserState;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class AbstractQueryToUserModel extends AbstractModel {
	
	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport);
	
	public abstract int getPrediction(Query query, UserState userState, int day);

}
