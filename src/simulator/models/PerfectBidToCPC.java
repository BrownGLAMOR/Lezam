package simulator.models;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.omg.CORBA._PolicyStub;

import models.AbstractModel;
import models.bidtocpc.AbstractBidToCPC;
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
			double closestBid = getClosestBid(_potentialBidsMap.get(query),bid);
			Reports closestReports = queryReportMaps.get(closestBid);
			avgCPC = closestReports.getCPC(query);
		}
		else {
			avgCPC = reports.getCPC(query);
		}
		return avgCPC;
	}

	/*
	 * Need to get closest that ISN'T larger
	 */
	private double getClosestBid(double[] arr, double bid) {
		double lastDiff = Double.MAX_VALUE;
		int idx = -1;
		for(int i = 0; i < arr.length; i++) {
			double elem = arr[i];
			if(elem == bid) {
				idx = i;
				break;
			}
			double diff = bid - elem;
			diff = Math.abs(diff);
			if(diff < lastDiff) { 
				lastDiff = diff;
			}
			else {
				idx = i-1;
				break;
			}
		}
		if(idx == -1) {
			idx = arr.length-1;
		}
		if(arr[idx] > bid) {
			if(idx == 0) {
				return arr[0];
			}
			else {
				return arr[idx-1];
			}
		}
		else {
			return arr[idx];
		}
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
