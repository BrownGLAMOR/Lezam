package models.querytousermodel;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

public abstract class AbstractQueryToUserModel extends AbstractModel {

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport);

   public abstract int getPrediction(Query query, GameStatusHandler.UserState userState, int day);

}
