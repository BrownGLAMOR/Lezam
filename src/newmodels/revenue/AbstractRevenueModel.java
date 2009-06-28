package newmodels.revenue;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.AbstractModel;

public abstract class AbstractRevenueModel extends AbstractModel{
	protected Query _query;
	public abstract void update(SalesReport salesReport, QueryReport queryReport);
	
	public abstract double getRevenue();
	
	public Query getQuery() {
		return _query;
	}
}
