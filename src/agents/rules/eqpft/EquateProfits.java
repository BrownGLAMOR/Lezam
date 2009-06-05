package agents.rules.eqpft;

import java.util.HashMap;
import java.util.Set;

import newmodels.profits.AbstractProfitsModel;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import agents.rules.AbstractRule;

public class EquateProfits extends AbstractRule {
	HashMap<Query, Double> _desiredSales;
	HashMap<Query, AbstractProfitsModel> _profitsModels;
	
	
	public EquateProfits(Set<Query> querySpace, HashMap<Query, Double> desiredSales, HashMap<Query, AbstractProfitsModel> profitsModels) {
		super(querySpace);
		this._desiredSales = desiredSales;
		this._profitsModels = profitsModels;
	}
	
	protected double getAvgProfit() {
		double result = 0;
		for (Query query : _querySpace) 
			result += _profitsModels.get(query).getProfit();
		result /=
	}

	@Override
	public void apply(Query q, BidBundle bidBundle) {
		// TODO Auto-generated method stub
		
	}

}
