package simulator.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

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
	private HashMap<Query, double[]> _potentialBidsMap;
	private HashMap<Query, HashMap<Double, Double>> _posToBidMap;

	public PerfectQueryToPrConv(HashMap<Query,HashMap<Double,Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap,
			HashMap<Query,HashMap<Double,Double>> posToBidMap) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
		_posToBidMap = posToBidMap;
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
	public double getPredictionWithBid(Query query, double bid) {
		if(bid == 0) {
			return 0.0;
		}
		double prClick;
		HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
		Reports reports = queryReportMaps.get(bid);
		if(reports == null) {
			double closestBid = getClosestBid(_potentialBidsMap.get(query),bid);
			Reports closestReports = queryReportMaps.get(closestBid);
			prClick = closestReports.getClickPr(query);
		}
		else {
			prClick = reports.getClickPr(query);
		}
		return prClick;
	}


	@Override
	public double getPredictionWithPos(Query query, double pos) {
		if(Double.isNaN(pos)) {
			return 0.0;
		}
		HashMap<Double, Double> posToBid = _posToBidMap.get(query);
		Set<Double> posToBidSet = posToBid.keySet();
		ArrayList<Double> posToBidArrList = new ArrayList<Double>(posToBidSet);
		Collections.sort(posToBidArrList);
		double[] posToBidArr = new double[posToBidArrList.size()];
		for(int i = 0; i < posToBidArr.length; i++) {
			posToBidArr[i] = posToBidArrList.get(i);
		}
		pos = getClosestPos(posToBidArr,pos);
		double bid = posToBid.get(pos);
		HashMap<Double, Reports> queryReportMaps = _allReportsMap.get(query);
		Reports reports = queryReportMaps.get(bid);
		double avgCPC = reports.getClickPr(query);
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
	
	/*
	 * Need to get closest that IS larger
	 */
	private double getClosestPos(double[] arr, double pos) {
		double lastDiff = Double.MAX_VALUE;
		int idx = -1;
		for(int i = 0; i < arr.length; i++) {
			double elem = arr[i];
			if(elem == pos) {
				idx = i;
				break;
			}
			double diff = pos - elem;
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
			throw new RuntimeException("");
		}
		if(arr[idx] > pos) {
				return arr[idx];
		}
		else {
			if(idx == arr.length-1 || arr[idx] == pos) {
				return arr[idx];
			}
			else {
				return arr[idx+1];
			}
		}
	}
	
	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		return true;
	}


	@Override
	public AbstractModel getCopy() {
		return new PerfectQueryToPrConv(_allReportsMap, _potentialBidsMap, _posToBidMap);
	}

}
