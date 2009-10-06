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
	private HashMap<Query, Double> _noise;

	public PerfectPosToPrClick(HashMap<Query, HashMap<Double, Reports>> allReportsMap, HashMap<Query, double[]> potentialBidsMap, HashMap<Query,HashMap<Double,Double>> posToBidMap,HashMap<Query, Double> noise) {
		_allReportsMap = allReportsMap;
		_potentialBidsMap = potentialBidsMap;
		_posToBidMap = posToBidMap;
		_noise = noise;
	}

	@Override
	public double getPrediction(Query query, double pos, Ad currentAd) {
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
		double clickPr = reports.getClickPr(query);
		
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
		return new PerfectPosToPrClick(_allReportsMap,_potentialBidsMap, _posToBidMap, _noise);
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
