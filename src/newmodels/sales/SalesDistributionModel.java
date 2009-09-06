package newmodels.sales;

import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;

import newmodels.AbstractModel;

public class SalesDistributionModel extends AbstractModel {
	
	Set<Query> _querySpace;
	HashMap<Query,Integer> _conversions;
	int _totConversions;
	
	public SalesDistributionModel(Set<Query> querySpace) {
		_querySpace = querySpace;
		_conversions = new HashMap<Query, Integer>();
		for(Query query : _querySpace) {
			_conversions.put(query, 0);
		}
		_totConversions = 0;
	}
	
	public double getPrediction(Query q) {
		return _conversions.get(q)/((double) _totConversions);
	}
	
	public boolean updateModel(SalesReport salesReport) {
		for(Query query : _querySpace) {
			int convs = salesReport.getConversions(query);
			_conversions.put(query, _conversions.get(query) + convs);
			_totConversions += convs;
		}
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return null;
	}
	
}
