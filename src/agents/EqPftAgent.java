package agents;

import java.util.HashMap;
import java.util.Set;

import agents.rules.AbstractRule;
import agents.rules.eqpft.CapacityCap;
import agents.rules.eqpft.EquateProfits;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.profits.AbstractProfitsModel;
import newmodels.profits.ProfitsMovingAvg;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * 
 * @author czhang
 *
 */

public class EqPftAgent extends SimAbstractAgent {
	BidBundle _bidBundle;
	
	AbstractUnitsSoldModel _unitsSoldModel; 
	HashMap<Query, AbstractPrConversionModel> _prConversionModels;
	HashMap<Query, AbstractProfitsModel> _profitsModels;
	//HashMap<Query,AbstractModel> bidToClicks;
	
	AbstractRule _capacityCap;
	AbstractRule _equateProfits;
	
	HashMap<Query, Double> _desiredSales;
	HashMap<Query, Double> _profitMargins;
	
	@Override
	protected void initBidder() {
		
		// initialize constants
		
		double initProfitMargin = .2;
		_profitMargins = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_profitMargins.put(query, initProfitMargin);
		}
		
		_desiredSales = new HashMap<Query, Double>();
		int distributionCapacity = _advertiserInfo.getDistributionCapacity();
		for (Query query : _querySpace) {
			_desiredSales.put(query, (double)distributionCapacity/_querySpace.size());
		}
		
		// initialize models
		
		int distributionWindow = _advertiserInfo.getDistributionWindow();
		_unitsSoldModel = new UnitsSoldMovingAvg(distributionWindow);
		
		_prConversionModels = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			double baselineConv = 0;
			if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
				baselineConv = .1;
			else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
				baselineConv = .2;
			else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
				baselineConv = .3;
			
			double componentBonus = 1;
			if (query.getComponent().equals(_advertiserInfo.getComponentSpecialty()))
				componentBonus = _advertiserInfo.getComponentBonus();
			
			double distributionCapacityDiscounter = _advertiserInfo.getDistributionCapacityDiscounter();
			_prConversionModels.put(query, new SimplePrConversion(query, baselineConv, distributionCapacityDiscounter, componentBonus, _unitsSoldModel));
			
		}
		
		_profitsModels = new HashMap<Query, AbstractProfitsModel>();
		for (Query query: _querySpace) {
			double profit = _profitMargins.get(query);
			_profitsModels.put(query, new ProfitsMovingAvg(query, profit));
		}
		
		// initialize rules
		
		_capacityCap = new CapacityCap(_querySpace, _desiredSales, _prConversionModels);
		_equateProfits = new EquateProfits(_querySpace, _desiredSales, _profitsModels);  
		
		// initialize the bid bundle
		
		_bidBundle = new BidBundle();
		
	}

	@Override
	protected BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateModels(SalesReport salesReport,
			QueryReport queryReport, Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		
	}


}
