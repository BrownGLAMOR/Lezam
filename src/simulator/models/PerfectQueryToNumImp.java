package simulator.models;

import java.util.HashMap;

import simulator.Reports;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.AbstractModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;

public class PerfectQueryToNumImp extends AbstractQueryToNumImp {

	private HashMap<Query, HashMap<Double, Reports>> _allReportsMap;
	private HashMap<Query, double[]> _potentialBidsMap;

	public PerfectQueryToNumImp(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
	}

	@Override
	public int getPrediction(Query query) {
		double avgNumImps = 0;
		int totNumImps = 0;
		HashMap<Double, Reports> queryReportsMap = _allReportsMap.get(query);
		for(Double bid : queryReportsMap.keySet()) {
			Reports reports = queryReportsMap.get(bid);
			double numImps = reports.getRegularImpressions(query) + reports.getPromotedImpressions(query);
			if(numImps > 0) {
				avgNumImps += numImps;
				totNumImps++;
			}
		}
		if(totNumImps > 0) {
			avgNumImps /= totNumImps;	
		}
		else {
			avgNumImps = 0.0;
		}

		return (int) avgNumImps;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectQueryToNumImp(_allReportsMap, _potentialBidsMap);
	}

}
