package newmodels.postoprclick;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import newmodels.AbstractModel;
import newmodels.targeting.BasicTargetModel;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicPosToPrClick extends AbstractPosToPrClick {

	private BasicTargetModel _targModel;
	private int _numPromSlots;
	private double _promBonus;
	private double _outOfAuction = 6.0;

	public BasicPosToPrClick(int numPromSlots) {
		_numPromSlots = numPromSlots;
		_promBonus = .5;
	}

	@Override
	public double getPrediction(Query q, double position, Ad ad) {
		double prConv, prClick, prCont;

		if(Double.isNaN(position) || position == _outOfAuction ) {
			return 0.0;
		}

		if(position < 1.0 || position > _outOfAuction) {
			throw new RuntimeException("Position cannot be less than 1.0 or more than " + _outOfAuction);
		}

		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			prConv = .1;
			prClick = .25;
			prCont = .35;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
			prConv = .2;
			prClick = .35;
			prCont = .45;
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
			prConv = .3;
			prClick = .45;
			prCont = .55;
		}
		else {
			throw new RuntimeException("Malformed query");
		}

		double[] clickPrs = new double[6];
		clickPrs[0] = prClick;
		if(_numPromSlots > 0) {
			clickPrs[0] = eta(prClick,1 + _promBonus);
		}

		for(int i = 1; i < 5; i++) {
			if(_numPromSlots > i) {
				clickPrs[i] = eta(prClick,1 + _promBonus) * (clickPrs[i-1] * (1-prConv) + (1 - clickPrs[i-1]) * (clickPrs[i-1] / prClick)) * prCont;
			}
			else {
				clickPrs[i] = prClick * (clickPrs[i-1] * (1-prConv) + (1 - clickPrs[i-1]) * (clickPrs[i-1] / prClick)) * prCont;
			}
		}

		clickPrs[5] = 0.0;

		double clickPr = 0.0;
		int posfloor = (int) Math.floor(position);
		int posceil = (int) Math.ceil(position);
		if(posfloor == posceil) {
			clickPr = clickPrs[posceil-1];
		}
		else {
			clickPr = (position-posfloor) * clickPrs[posfloor-1] + (posceil-position) * clickPrs[posceil-1];
		}
		return clickPr;
	}


	public double eta(double p, double x) {
		return (p*x)/(p*x + (1-p));
	}


	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		//Updating this model is not necessary
		return true;
	}

	@Override
	public void setSpecialty(String manufacturer, String component) {
		_targModel = new BasicTargetModel(manufacturer, component);
	}

	@Override
	public String toString() {
		return "BasicPosToPrClick";
	}

	@Override
	public AbstractModel getCopy() {
		return new BasicPosToPrClick(_numPromSlots);
	}

	public static void main(String[] args) {
		BasicPosToPrClick model = new BasicPosToPrClick(2);
		ArrayList<Query> miniQuerySet = new ArrayList<Query>();
		miniQuerySet.add(new Query("flat","tv"));
		miniQuerySet.add(new Query(null,"tv"));
		miniQuerySet.add(new Query("flat",null));
		miniQuerySet.add(new Query(null,null));
		for(Query query : miniQuerySet) {
			System.out.println(query);
			for(int i = 0; i < 6; i++) {
				System.out.println("\tSlot " + (i+1) + ": " + model.getPrediction(query, i+1, new Ad()));
			}
		}
	}

}
