package newmodels.slottonumclicks;


import com.sun.org.apache.bcel.internal.generic.RETURN;

import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;

public class SimpleClick extends AbstractSlotToNumClicks{
	double[] history;
	double[] estimate;
	
	double prClick;
	double prConversion;
	double prContinue;
	int promotedSlots;
	int slots;
	double targetEffect;
	double promotedSlotBonus;
	
	final double alpha = .75;
	final double beta = .5;
	final double error = 20;
	
	public SimpleClick(Query query, AdvertiserInfo advertiserInfo, SlotInfo slotInfo) {
		super(query);
		promotedSlots = slotInfo.getPromotedSlots(); 
		slots = promotedSlots + slotInfo.getRegularSlots();
		history = new double[slots];
		estimate = new double[slots];
		for (int i = 0; i < slots; i++) {
			history[i] = Double.NaN;
			estimate[i] = Double.NaN;
		}
		targetEffect = advertiserInfo.getTargetEffect();
		promotedSlotBonus = slotInfo.getPromotedSlotBonus();
		if (_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			prClick = 0.25;
			prContinue = .35;
			prConversion = .1;
		}
		else if (_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			prClick = 0.35;
			prContinue = .45;
			prConversion = .2;
		}
		else if (_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			prClick = 0.45;
			prContinue = .55;
			prConversion = .3;
		}
	}
	
	@Override
	public int getPrediction(double targetSlot) {
		if (targetSlot > 5 || targetSlot < 1) return -1; 
		return (int)estimate[(int)targetSlot];
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		
		// if did not get any clicks, no update can be made
		if (queryReport.getClicks(_query) == Double.NaN) return true; 
		
		// read in new sample
		int position = (int)Math.ceil(queryReport.getPosition(_query));
		double sample = queryReport.getClicks(_query);
		boolean limit = Math.abs(bidBundle.getDailyLimit(_query) - queryReport.getCPC(_query) * queryReport.getClicks(_query)) < error;
		
		// update accurate history
		if (!limit) {
			if (history[position] == Double.NaN) history[position] = sample;
			else history[position] = alpha * sample + (1 - alpha) * history[position];
			estimate[position] = history[position];
		}
		else {
			if (history[position] == Double.NaN)
				history[position] = sample;
			else if (history[position] < sample + error) 
				history[position] = alpha * sample + (1 - alpha) * history[position];
			estimate[position] = history[position];
		}
		
		
		// estimate the position above
		if (position > 1) {
			
			double newEstimate = history[position] / (1 - prClick*prConversion) / prContinue;
			if (history[position - 1] == Double.NaN)
				estimate[position - 1] = newEstimate;
			else estimate[position - 1] = beta * newEstimate + (1 - beta) * history[position - 1];
		}
		
		// estimate the position below
		
		if (position < 5) {
			
			double newEstimate = history[position] * (1 - prClick*prConversion) * prContinue;
			if (history[position + 1] == Double.NaN)
				estimate[position + 1] = newEstimate;
			else estimate[position + 1] = beta * newEstimate + (1 - beta) * history[position + 1];
		}
		
		return true;
	}

	protected double solve(double y, double x) {
		double p = y/(x + y - x*y);
		return p;
	}
	
	protected double calculate(double p, double x) {
		double y = p*x/(p*x + 1 - p);
		return y;
	}

}

