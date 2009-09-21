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
			double convPr = reports.getConvPr(query);
			if(convPr > 0) {
				avgConvProb += convPr;
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
