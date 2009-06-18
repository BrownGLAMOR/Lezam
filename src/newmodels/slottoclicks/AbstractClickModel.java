package newmodels.slottoclicks;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.AbstractModel;

public abstract class AbstractClickModel extends AbstractModel{
	protected Query _query;

	public AbstractClickModel(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport, BidBundle bidBundle);
	
	public abstract double getPrediction(double slot);

	public Query getQuery() {
		return _query;
	}
}
