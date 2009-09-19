package simulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.postoprclick.AbstractPosToPrClick;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectPosToPrClick extends AbstractPosToPrClick {

	private HashMap<Query, HashMap<Double, Reports>> _allReportsMap;
	private HashMap<Query, double[]> _potentialBidsMap;
	private HashMap<Query, HashMap<Double, Double>> _posToBidMap;

	public PerfectPosToPrClick(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap, HashMap<Query,HashMap<Double,Double>> posToBidMap) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
		_posToBidMap = posToBidMap;
	}

	@Override
	public double getPrediction(Query query, double pos, Ad currentAd) {
		double clickPr;
		HashMap<Double, Double> posToBid = _posToBidMap.get(query);
		Set<Double> posToBidSet = posToBid.keySet();
		ArrayList<Double> posToBidArrList = new ArrayList<Double>(posToBidSet);
		Collections.sort(posToBidArrList);
		double[] posToBidArr = new double[posToBidArrList.size()];
		for(int i = 0; i < posToBidArr.length; i++) {
			posToBidArr[i] = posToBidArrList.get(i);
		}
		double bid = getClosestElement(posToBidArr,pos);
		HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
		Reports reports = queryReportMaps.get(bid);
		QueryReport queryReport;
		if(reports == null) {
			double closestBid = getClosestElement(_potentialBidsMap.get(query),bid);
			Reports closestReports = queryReportMaps.get(closestBid);
			queryReport = closestReports.getQueryReport();
		}
		else {
			queryReport = reports.getQueryReport();
		}
		int clicks = queryReport.getClicks(query);
		int impressions = queryReport.getImpressions(query);
		if(clicks == 0 || impressions == 0) {
			clickPr = 0.0;
		}
		else {
			clickPr = clicks/(impressions*1.0);
		}
		return clickPr;
	}

	private double getClosestElement(double[] arr, double bid) {
		double lastDiff = Double.MAX_VALUE;
		for(int i = 0; i < arr.length; i++) {
			double elem = arr[i];
			if(elem == bid) {
				return elem;
			}
			double diff = bid - elem;
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
		return new PerfectPosToPrClick(_allReportsMap,_potentialBidsMap, _posToBidMap);
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
