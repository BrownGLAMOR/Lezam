package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import models.AbstractModel;
import models.bidtopos.AbstractBidToPos;
import models.bidtopos.SpatialBidToPos;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectBidToPos extends AbstractBidToPos {

	private HashMap<Query,HashMap<Double,Double>> _bidToPosMap;

	public PerfectBidToPos(HashMap<Query,HashMap<Double,Double>> bidToPosMap) {
		_bidToPosMap = bidToPosMap;
	}

	@Override
	public double getPrediction(Query query, double bid) {
		if(bid == 0) {
			return 6.0;
		}
		HashMap<Double, Double> bidToPos = _bidToPosMap.get(query);
		Set<Double> bidToPosSet = bidToPos.keySet();
		ArrayList<Double> bidToPosArrList = new ArrayList<Double>(bidToPosSet);
		Collections.sort(bidToPosArrList);
		double[] bidToPosArr = new double[bidToPosArrList.size()];
//		System.out.println(query + ", bid: " + bid);
		for(int i = 0; i < bidToPosArr.length; i++) {
			bidToPosArr[i] = bidToPosArrList.get(i);
//			System.out.println(i + "  " + bidToPosArr[i] + "   " + bidToPos.get(bidToPosArr[i]));
		}
		bid = getClosestBid(bidToPosArr,bid);
//		System.out.println("Closest bid: " + bid);
		double pos = bidToPos.get(bid);
//		System.out.println("Resulting Pos: " + pos);
		return pos;
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
	public AbstractModel getCopy() {
		return new PerfectBidToPos(_bidToPosMap);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

	@Override
	public void setNumPromSlots(int numPromSlots) {
		//not used in this class
	}

}
