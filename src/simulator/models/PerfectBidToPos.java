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

import newmodels.AbstractModel;
import newmodels.bidtopos.AbstractBidToPos;
import newmodels.bidtopos.SpatialBidToPos;
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
		HashMap<Double, Double> bidToPos = _bidToPosMap.get(query);
		Set<Double> bidToPosSet = bidToPos.keySet();
		ArrayList<Double> bidToPosArrList = new ArrayList<Double>(bidToPosSet);
		Collections.sort(bidToPosArrList);
		double[] bidToPosArr = new double[bidToPosArrList.size()];
		for(int i = 0; i < bidToPosArr.length; i++) {
			bidToPosArr[i] = bidToPosArrList.get(i);
		}
		bid = getClosestElement(bidToPosArr,bid);
		double pos = bidToPos.get(bid);
		return pos;
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
