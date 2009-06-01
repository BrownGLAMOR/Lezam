package newmodels.slottoprclick;

/**
 * @author jberg
 *
 */

import java.util.Random;

import modelers.clickprob.ClickRatioModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicSlotToPrClick extends AbstractSlotToPrClick {
	
	private Query _query;
	private int F0 = 0;
	private int F1 = 1;
	private int F2 = 2;
	private int LOW = 0;
	private int HIGH = 1;
	private double[][] adveffect;
	private double[][] contprob;
	private double[] conv;
	private double[] clickprob;
	private int focuslevel;
	private int numslots = 5;
	Random _R = new Random();
	
/*
 * All of the constants are based off of these lines taken out of the
 * server config files...they will need to be changed in the future
 * 
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_ZERO.low=0.2
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_ZERO.high=0.3
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_ONE.low=0.3
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_ONE.high=0.4
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_TWO.low=0.4
	users.clickbehavior.advertisereffect.FOCUS_LEVEL_TWO.high=0.5
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_ZERO.low=0.2
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_ZERO.high=0.5
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_ONE.low=0.3
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_ONE.high=0.6
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_TWO.low=0.4
	users.clickbehavior.continuationprobability.FOCUS_LEVEL_TWO.high=0.7
	advertiser.focuseffect.FOCUS_LEVEL_ZERO=0.10
	advertiser.focuseffect.FOCUS_LEVEL_ONE=0.20
	advertiser.focuseffect.FOCUS_LEVEL_TWO=0.30*/

	public BasicSlotToPrClick(Query q) {
		super(q);
		focuslevel = q.getType().ordinal();
		clickprob = new double[numslots];
		adveffect = new double[3][2];
		contprob = new double[3][2];
		conv = new double[3];
		adveffect[F0][LOW] = .2;
		adveffect[F0][HIGH] = .3;
		adveffect[F1][LOW] = .3;
		adveffect[F1][HIGH] = .4;
		adveffect[F2][LOW] = .4;
		adveffect[F2][HIGH] = .5;
		contprob[F0][LOW] = .2;
		contprob[F0][HIGH] = .5;
		contprob[F1][LOW] = .3;
		contprob[F1][HIGH] = .6;
		contprob[F2][LOW] = .4;
		contprob[F2][HIGH] = .7;
		conv[F0] = .1;
		conv[F1] = .2;
		conv[F2] = .3;
	}

	public void generateClickProb() {
		double[] clickprobtot = new double[clickprob.length];
		for(int i = 0; i < clickprob.length; i++) {
			clickprobtot[i] = 0;
		}
		int N = 2500;
		for(int i = 0; i < N; i ++) {
			double adveff = randDouble(adveffect[focuslevel][LOW],adveffect[focuslevel][HIGH]);
			double gamma = randDouble(contprob[focuslevel][LOW],contprob[focuslevel][HIGH]);
			clickprob[0] = adveff;
			for(int j = 1; j < clickprob.length; j++) {
				clickprob[j] = adveff*Math.pow(gamma,j)*(1-conv[focuslevel]*(clickprob[j-1]));
			}
			for(int j = 0; j < clickprob.length; j++) {
				clickprobtot[j] += clickprob[j];
			}
		}
		for(int i = 0; i < clickprob.length; i++) {
			clickprob[i] = clickprobtot[i] / N;
		}
	}

	public double[] getClickProb() {
		if(clickprob[0] == 0) {
			this.generateClickProb();
			return clickprob;
		}
		else {
			return clickprob;
		}
	}
	
	//Returns a random double rand such that a <= r < b
	protected double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	@Override
	public double getPrediction(double slot) {
		slot -= 1;
		double[] clickprobs = getClickProb();
		int min = (int) Math.floor(slot);
		int max = (int) Math.ceil(slot);
		if(min == max) {
			return clickprobs[min];
		}
		else {
			double avg = (slot - min) * clickprobs[min] + (max - slot) * clickprobs[max];
			return avg;
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing to do
		return true;
	}

	public static void main(String[] args) {
		Query q = new Query(null,"dvd");
		BasicSlotToPrClick crm = new BasicSlotToPrClick(q);
		double[] clickprob = crm.getClickProb();
		for(int i = 2; i < clickprob.length*2 + 1; i++) {
			System.out.println("Slot "+(i/2.0)+": "+ crm.getPrediction(i/2.0));
		}
	}
	
}
