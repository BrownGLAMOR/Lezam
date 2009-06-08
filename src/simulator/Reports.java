package simulator;

import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class Reports {

	private QueryReport _queryReport;
	private SalesReport _salesReport;

	public Reports(QueryReport queryReport, SalesReport salesReport) {
		_queryReport = queryReport;
		_salesReport = salesReport;
	}

	public QueryReport getQueryReport() {
		return _queryReport;
	}

	public SalesReport getSalesReport() {
		return _salesReport;
	}

	
	
}
