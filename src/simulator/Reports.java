package simulator;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class Reports {

	private QueryReport _queryReport;
	private SalesReport _salesReport;
	private int _numReports;
	
	public Reports(Reports reports) {
		
		QueryReport copyQueryReport = new QueryReport();
		SalesReport copySalesReport = new SalesReport();
		QueryReport queryReport = reports.getQueryReport();
		SalesReport salesReport = reports.getSalesReport();
		for(Query query : queryReport) {
			copyQueryReport.setClicks(query, queryReport.getClicks(query));
			copyQueryReport.setCost(query, queryReport.getCost(query));
			copyQueryReport.setImpressions(query, queryReport.getRegularImpressions(query), queryReport.getPromotedImpressions(query));
			copyQueryReport.setPositionSum(query, queryReport.getPosition(query) * (queryReport.getRegularImpressions(query) + queryReport.getPromotedImpressions(query)));
			copySalesReport.setConversions(query, salesReport.getConversions(query));
			copySalesReport.setRevenue(query, salesReport.getRevenue(query));
		}
		_queryReport = copyQueryReport;
		_salesReport = copySalesReport;
		_numReports = reports.getNumReports();
		
	}

	public Reports(QueryReport queryReport, SalesReport salesReport) {
		_queryReport = queryReport;
		_salesReport = salesReport;
		_numReports = 1;
	}

	public QueryReport getQueryReport() {
		return _queryReport;
	}

	public SalesReport getSalesReport() {
		return _salesReport;
	}

	public int getNumReports() {
		return _numReports;
	}
	
	public void addReport(Reports report) {
		_numReports += report.getNumReports();
		QueryReport queryReport = report.getQueryReport();
		SalesReport salesReport = report.getSalesReport();
		for(Query query : queryReport) {
			double weight = (((_numReports-report.getNumReports())*1.0)/_numReports);
			int clicks = (int) (_queryReport.getClicks(query)*weight + queryReport.getClicks(query)*(1-weight));
			double cost = _queryReport.getCost(query)*weight + queryReport.getCost(query)*(1-weight);
			int regImps1 = _queryReport.getRegularImpressions(query);
			int promImps1 = _queryReport.getPromotedImpressions(query);
			int regImps2 = queryReport.getRegularImpressions(query);
			int promImps2 = queryReport.getPromotedImpressions(query);
			int regImps = (int) (regImps1 * weight + regImps2 * (1-weight));
			int promImps = (int) (promImps1 * weight + promImps2 * (1-weight));
			int positionSum = (int) ((_queryReport.getPosition(query) * (regImps1+promImps1)) * weight +
								(queryReport.getPosition(query) * (regImps2+promImps2)) * (1-weight));
			int conversions = (int) (_salesReport.getConversions(query)*weight + salesReport.getConversions(query)*(1-weight));
			double revenue = _salesReport.getRevenue(query)*weight + _salesReport.getRevenue(query)*(1-weight);
			_queryReport.setClicks(query, clicks);
			_queryReport.setCost(query, cost);
			_queryReport.setImpressions(query, regImps, promImps);
			_queryReport.setPositionSum(query, positionSum);
			_salesReport.setConversions(query, conversions);
			_salesReport.setRevenue(query, revenue);
		}
	}
}
