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
import newmodels.postobid.AbstractPosToBid;
import simulator.BasicSimulator;
import simulator.Reports;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectPosToBid extends AbstractPosToBid {

	private HashMap<Query,HashMap<Double,Double>> _posToBidMap;

	public PerfectPosToBid(HashMap<Query,HashMap<Double,Double>> posToBidMap) {
		_posToBidMap = posToBidMap;
	}

	@Override
	public double getPrediction(Query query, double pos) {
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
		pos = getClosestElement(posToBidArr,pos);
		double bid = posToBid.get(pos);
		return bid;
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
	public AbstractModel getCopy() {
		return new PerfectPosToBid(_posToBidMap);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		return true;
	}

}
