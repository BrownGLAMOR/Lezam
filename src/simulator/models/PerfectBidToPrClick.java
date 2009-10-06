package simulator.models;

import java.util.HashMap;
import java.util.LinkedList;

import newmodels.AbstractModel;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPrClick extends AbstractBidToPrClick {

	private HashMap<Query, HashMap<Double, Reports>> _allReportsMap;
	private HashMap<Query, double[]> _potentialBidsMap;
	private HashMap<Query, Double> _noise;

	public PerfectBidToPrClick(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap, HashMap<Query, Double> noise) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
		_noise = noise;
	}

	@Override
	public double getPrediction(Query query, double bid, Ad currentAd) {
		if(bid == 0) {
			return 0.0;
		}
		HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
		Reports reports = queryReportMaps.get(bid);
		double clickPr;
		if(reports == null) {
			double closestBid = getClosestBid(_potentialBidsMap.get(query),bid);
			Reports closestReports = queryReportMaps.get(closestBid);
			clickPr = closestReports.getClickPr(query);
		}
		else {
			clickPr = reports.getClickPr(query);
		}
		
		clickPr += _noise.get(query);
		if(clickPr < 0.0) {
			return 0.0;
		}
		
		if(clickPr > .6) {
			return .6;
		}
		
		return clickPr;
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
		return new PerfectBidToPrClick(_allReportsMap,_potentialBidsMap,_noise);
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
		//not needed
	}

	@Override
	public String toString() {
		return "PerfectBidToPrClick";
	}

}
