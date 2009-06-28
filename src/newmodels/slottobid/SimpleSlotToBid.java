package newmodels.slottobid;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;

public class SimpleSlotToBid extends AbstractSlotToBidModel {
	
	double[] history;
	double[] estimate;
	int promotedSlots;
	int slots;
	final double alpha = .75;
	final double beta = .5;
	final double error = .01;
	final double minBid = .25;
	
	
	public SimpleSlotToBid(Query query, SlotInfo slotInfo) {
		super(query);
		promotedSlots = slotInfo.getPromotedSlots(); 
		slots = promotedSlots + slotInfo.getRegularSlots();
		history = new double[slots + 1];
		estimate = new double[slots + 1];
		for (int i = 0; i < slots; i++) {
			history[i] = Double.NaN;
			estimate[i] = Double.NaN;
		}
	}

	@Override
	public double getPrediction(double targetSlot) {
		if (targetSlot > slots || targetSlot < 1 ) return Double.NaN;
		else if (Double.isNaN(estimate[(int)targetSlot])) return minBid+error;
		else return Math.max(minBid, estimate[(int)targetSlot]);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		// if did not get position, update lower bound if necessary
		if (Double.isNaN(queryReport.getPosition(_query))) {
			double lastBid = bidBundle.getBid(_query);
			if (lastBid > history[slots])
				history[slots] = alpha * lastBid + (1 - alpha) * history[slots];
			estimate[slots] = Math.max(minBid, lastBid)*2;
			estimate[slots - 1] = Math.max(minBid, lastBid)*3;
			return false;
		}
		
		// read in the latest sample
		int position = (int)Math.ceil(queryReport.getPosition(_query));
		double sample = queryReport.getCPC(_query) + error;
		
		// update accurate history
		if (Double.isNaN(history[position])) history[position] = sample;
		else history[position] = alpha * sample + (1 - alpha) * history[position];
		estimate[position] = history[position];
		
		// make estimation about the position above
		if (position >= 2) {
			double diff;
			diff = queryReport.getCPC(_query)*.1;
			if (Double.isNaN(history[position - 1]))
				estimate[position - 1] = history[position] + diff;
			else estimate[position - 1] = beta * (history[position] + diff) + (1 - beta) * history[position - 1]; 
		}
		
		//make estimation about the position below
		if (position <= slots - 1) {
			double diff;
			diff = queryReport.getCPC(_query)*.1;
			if (Double.isNaN(history[position + 1]))
				estimate[position + 1] = Math.max(minBid,history[position] - diff);
			else estimate[position + 1] = beta * (history[position] - diff) + (1 - beta) * history[position + 1]; 
		}
		
		return true;
	}
	
	public void updateByAgent(int position, QueryReport queryReport, BidBundle bidBundle) {
		double realPosition = queryReport.getPosition(_query);
		if (Double.isNaN(realPosition) || (int)Math.ceil(realPosition) <= position) return;
		else {
			if (estimate[position] < bidBundle.getBid(_query)) {
				estimate[position] = beta * bidBundle.getBid(_query)*1.2 + (1 - beta)*estimate[position];
			}
		
			if (history[position] < bidBundle.getBid(_query)) {
				history[position] = alpha * bidBundle.getBid(_query) + (1 - alpha)*history[position];
			}
		}
	}

	
}
