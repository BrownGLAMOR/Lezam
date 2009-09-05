package newmodels.postoprclick;

/*
 * TODO
 * 
 * Build this into basic deterministic slot to pr click model
 * 
 * allow setting our adv effect and cont prob per query all independently!
 * 
 * build in defaults
 */


import java.util.Random;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class DetBasicSlotToPrClick extends AbstractPosToPrClick{

	private Query _query;
	private int F0 = 0;
	private int F1 = 1;
	private int F2 = 2;
	private static final double[] adveffect = {.2,.3,.4};
	private static final double[] contprob = {.2,.3,.4};
	private static final double[] conv = {.1,.2,.3};
	private double[] clickprob;
	private int focuslevel;
	private static final int NUMOFREGSLOTS = 5;
	Random _R = new Random();

	public DetBasicSlotToPrClick(Query q) {
		super(q);
		// TODO Auto-generated constructor stub
		focuslevel = q.getType().ordinal();
		clickprob = new double[NUMOFREGSLOTS];
		
		clickprob[0] = adveffect[focuslevel];
		for (int slot=1 ; slot<NUMOFREGSLOTS ; slot++) {
			clickprob[slot] = ((1-clickprob[slot-1]) + (clickprob[slot-1]*(1-conv[focuslevel])))*contprob[focuslevel]*adveffect[focuslevel];
		}
	}
	
	protected double eta (double p, double x) {
		return ((p*x) / (p*x + (1-p)));
	}
	
	//Returns a random double rand such that a <= r < b
	protected double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	@Override
	public double getPrediction(double slot) {
		slot -= 1;
		int min = (int) Math.floor(slot);
		int max = (int) Math.ceil(slot);
		if(min == max) {
			return clickprob[min];
		}
		else {
			double avg = (slot - min) * clickprob[min] + (max - slot) * clickprob[max];
			return avg;
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

}
