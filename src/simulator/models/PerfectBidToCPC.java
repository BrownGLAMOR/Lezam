package simulator.models;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.omg.CORBA._PolicyStub;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToCPC extends AbstractBidToCPC {

	private HashMap<Query, HashMap<Double, Reports>> _allReportsMap;
	private HashMap<Query, double[]> _potentialBidsMap;

	public PerfectBidToCPC(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
	}

	@Override
	public double getPrediction(Query query, double bid) {
		if(bid == 0) {
			return 0.0;
		}
		double avgCPC;
		HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
		Reports reports = queryReportMaps.get(bid);
		if(reports == null) {
			double closestBid = getClosestElement(_potentialBidsMap.get(query),bid);
			Reports closestReports = queryReportMaps.get(closestBid);
			avgCPC = closestReports.getCPC(query);
		}
		else {
			avgCPC = reports.getCPC(query);
		}
		return avgCPC;
	}

	private double getClosestElement(double[] arr, double bid) {
		double lastDiff = Double.MAX_VALUE;
		for(int i = 0; i < arr.length; i++) {
			double elem = arr[i];
			if(elem == bid) {
				return elem;
			}
			double diff = bid - elem;
			diff = Math.abs(diff);
			if(diff < lastDiff) { 
				lastDiff = diff;
			}
			else {
				return arr[i-1];
			}
		}
		return arr[arr.length-1];
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new PerfectBidToCPC(_allReportsMap,_potentialBidsMap);
	}

	@Override
	public String toString() {
		return "PerfectBidToCPC";
	}

}
