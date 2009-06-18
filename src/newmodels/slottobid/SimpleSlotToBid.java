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
	
	
	public SimpleSlotToBid(Query query, SlotInfo slotInfo) {
		super(query);
		promotedSlots = slotInfo.getPromotedSlots(); 
		slots = promotedSlots + slotInfo.getRegularSlots();
		history = new double[slots];
		estimate = new double[slots];
		for (int i = 0; i < slots; i++) {
			history[i] = Double.NaN;
			estimate[i] = Double.NaN;
		}
	}

	@Override
	public double getPrediction(double targetSlot) {
		return estimate[(int)targetSlot];
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		// if did not get position, update lower bound if necessary
		if (queryReport.getPosition(_query) == Double.NaN) {
			if (bidBundle.getBid(_query) < history[slots])
				history[slots] = alpha * bidBundle.getBid(_query) + (1 - alpha) * history[slots];
				estimate[slots] = history[slots];
			return true;
		}
		
		// read in the latest sample
		int position = (int)Math.ceil(queryReport.getPosition(_query));
		double sample = queryReport.getCPC(_query) + error;
		
		// update accurate history
		if (history[position] == Double.NaN) history[position] = sample;
		else history[position] = alpha * sample + (1 - alpha) * history[position];
		estimate[position] = history[position];
		
		// make estimation about the position above
		if (position > 1) {
			double diff = bidBundle.getBid(_query) - queryReport.getCPC(_query) + error;
			if (history[position - 1] == Double.NaN)
				estimate[position - 1] = history[position] + diff;
			else estimate[position - 1] = beta * (history[position] + diff) + (1 - beta) * history[position - 1]; 
		}
		
		//make estimation about the position below
		if (position < 5) {
			double diff = bidBundle.getBid(_query) - queryReport.getCPC(_query) - error;
			if (history[position + 1] == Double.NaN)
				estimate[position + 1] = history[position] - diff;
			else estimate[position + 1] = beta * (history[position] - diff) + (1 - beta) * history[position + 1]; 
		}
		
		return true;
	}

	
}
