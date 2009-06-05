package agents.rules.eqpft;

import java.util.HashMap;
import java.util.Set;

import newmodels.prconv.AbstractPrConversionModel;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import agents.rules.AbstractRule;

public class CapacityCap extends AbstractRule {
	HashMap<Query, Double> _desiredSales;
	HashMap<Query, AbstractPrConversionModel> _prConversionModels;

	public CapacityCap(Set<Query> querySpace, HashMap<Query, Double> desiredSales, HashMap<Query, AbstractPrConversionModel> prConversionModels) {
		super(querySpace);
		this._desiredSales = desiredSales;
		this._prConversionModels = prConversionModels;
	}

	@Override
	public void apply(Query query, BidBundle bidBundle) {
		double dailyLimit = bidBundle.getBid(query)*_desiredSales.get(query)/_prConversionModels.get(query).getPrediction();
		bidBundle.setDailyLimit(query, dailyLimit);
	}

}
