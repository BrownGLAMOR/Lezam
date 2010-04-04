package agents.rulebased.simple;

import java.util.Set;

import models.AbstractModel;
import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class EquatePMSimple extends RuleBasedAgentSimple {
	protected BidBundle _bidBundle;
	protected double _PM;
	protected double _initPM;
	protected double _incPM;
	protected double _decPM;
	
	public EquatePMSimple() {
		this(0.797475, 1.08, 1.35);
	}

	public EquatePMSimple(double initPM,
			double incPM,
			double lambdaCap) {
		_initPM = initPM;
		_incPM = incPM;
		_decPM = 1.0/incPM;
		_lambdaCap = lambdaCap;
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();
		if(_day < 2) { 
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}

		if (_day > 1 && _salesReport != null && _queryReport != null) {
			/*
			 * Equate PMs
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(sum <= _dailyCapacity) {
				_PM *=  _decPM;
			}
			else {
				_PM *=  _incPM;
			}

			if(Double.isNaN(_PM) || _PM <= 0 || _PM >= 1.0) {
				_PM = _initPM;
			}
		}

		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, getBidFromCPC(query, targetCPC));
		}

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();
		_PM = _initPM;
	}

	protected double getTargetCPC(Query q){		

		double prConv;
		if(_day <= 6) {
			prConv = _baselineConversion.get(q);
		}
		else {
			prConv = _conversionPrModel.getPrediction(q);
		}

		double rev = _salesPrices.get(q);
		double CPC = (1 - _PM)*rev* prConv;
		CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));

		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePM(" + _initPM + ", " + _incPM + ", " + _lambdaCap + ")";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePMSimple(_initPM, _incPM, _lambdaCap);
	}

}