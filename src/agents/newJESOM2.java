package agents;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class newJESOM2 extends SimAbstractAgent{
	protected AbstractUnitsSoldModel _unitsSoldModel;
	protected HashMap<Query, AbstractPrConversionModel> _conversionPrModel;
	protected HashMap<Query, Double> _baselineConversion;
    protected HashMap<Query, Double> _reinvestment;
    protected HashMap<Query, Double> _revenue;
    protected HashMap<Query, Double> _wantedSales;
    protected BidBundle _bidBundle;
    protected int counter = 1;
    
    protected double magicDivisor = 8;
    
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initBidder() {
		// TODO Auto-generated method stub
		_baselineConversion = new HashMap<Query, Double>();
        for(Query q: _querySpace){
        	if(query>)
        }
		
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ZERO)) {_baseLineConversion.put(q, 0.1);}  // constant set by game server info
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_ONE)) {_baseLineConversion.put(q, 0.2);}  // constant set by game server info
		for(Query q : _queryFocus.get(QueryType.FOCUS_LEVEL_TWO)) {_baseLineConversion.put(q, 0.3);}  // 
		
	}

	@Override
	public Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
	
	}