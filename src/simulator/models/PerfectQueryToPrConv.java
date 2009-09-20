package simulator.models;

import java.util.HashMap;
import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectQueryToPrConv extends AbstractConversionModel {

	private HashMap<Query,HashMap<Double,Reports>> _allReportsMap;

	public PerfectQueryToPrConv(HashMap<Query,HashMap<Double,Reports>> allReportsMap) {
		_allReportsMap = allReportsMap;
	}


	@Override
	public double getPrediction(Query query) {
		double avgConvProb = 0;
		int totConvProbs = 0;
		HashMap<Double, Reports> queryReportsMap = _allReportsMap.get(query);
		for(Double bid : queryReportsMap.keySet()) {
			Reports reports = queryReportsMap.get(bid);
			QueryReport queryReport = reports.getQueryReport();
			SalesReport salesReport = reports.getSalesReport();
			int clicks = queryReport.getClicks(query);
			int conversions = salesReport.getConversions(query);
			if(clicks == 0 || conversions == 0) {
				continue;
			}
			else {
				avgConvProb += conversions/(clicks*1.0);
				totConvProbs++;
			}
		}
		if(totConvProbs > 0) {
			avgConvProb /= totConvProbs;	
		}
		else {
			avgConvProb = 0.0;
		}
		
		return avgConvProb;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}


	@Override
	public AbstractModel getCopy() {
		return new PerfectQueryToPrConv(_allReportsMap);
	}

}
