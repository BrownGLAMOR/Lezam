package agents;

import java.util.Hashtable;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class JESOMBidStrategy {
	protected Set<Query> _querySpace;
	
	protected double _defaultQuerySpendLimit;
	protected Ad _defaultQueryAd;
	protected double _defaultQueryConversion;
	protected double _defaultQueryReinvestFactor;
	protected double _defaultQueryConversionRevenue;
	
	protected Hashtable<Query, Pair<Double, Double>> _queryBidBudget;
	protected Hashtable<Query, Ad> _queryAd;

	
	// Added a second parameter which means Query->(Bid, Budget)
	public JESOMBidStrategy(Set<Query> querySpace, Hashtable<Query, Pair<Double, Double>> bids){

		_querySpace = querySpace;
		_defaultQuerySpendLimit = Double.NaN;
		_defaultQueryAd = new Ad();
			
		_queryAd = new Hashtable<Query, Ad>();		
		_queryBidBudget = new Hashtable<Query, Pair<Double, Double>>();
		
		// Set the budget limit and the bid for each query.
		for (Query q : querySpace)
		{
			_queryBidBudget.put(q, bids.get(q));
		}
	}

	public Set<Query> getQuerySpace(){return _querySpace;}

	public double getQuerySpendLimit(Query q){
		return _queryBidBudget.get(q).getSecond();
	}
	
	public Ad getQueryAd(Query q){
		if(_queryAd.containsKey(q)){
			return _queryAd.get(q);
		}
		else {
			return _defaultQueryAd;
		}
	}
	
	public double getQueryBid(Query q) {
		return _queryBidBudget.get(q).getFirst();		
	}
	
	/**
	 * Set the bid for a query.
	 * @param q The query to set the bid for.
	 * @param bid The bid to set.
	 * @return The previous value of the specified key. <code>null</code> if the value was not set.
	 */
	public Pair<Double, Double> setQueryBid(Query q, double bid) {
		if(_queryBidBudget == null || _queryBidBudget.isEmpty() || _queryBidBudget.get(q) == null){
			return null;
		}
		else {
			return _queryBidBudget.put(q, new Pair<Double,Double>(bid, _queryBidBudget.get(q).getSecond()));
		}
	}
	
	/**
	 * Set the budget for a query.
	 * @param q The query to set the bid for.
	 * @param budget The budget to set.
	 * @return The previous value of the specified key. <code>null</code> if the value was not set.
	 */
	public Pair<Double, Double> setQueryBudget(Query q, double budget) {
		if(_queryBidBudget == null || _queryBidBudget.isEmpty() || _queryBidBudget.get(q) == null){
			return null;
		}
		else {
			return _queryBidBudget.put(q, new Pair<Double,Double>(_queryBidBudget.get(q).getFirst(), budget));
		}
	}
	
	public void setQueryAd(Query q, Ad d){_queryAd.put(q, d);}
	
	public void setDefaultQuerySpendLimit(double d){_defaultQuerySpendLimit = d;}
	public void setDefaultQueryAd(Ad d){_defaultQueryAd = d;}

	// Need to add what product we are selling. For now just null/
	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		
		for(Query q : _querySpace) {
			double queryBid = getQueryBid(q);
			double queryBudget = getQuerySpendLimit(q);

			bidBundle.addQuery(q, queryBid, getQueryAd(q));
			bidBundle.setDailyLimit(q, queryBudget);
		}
		
		return bidBundle;
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(getQueryBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
		}
		return buff.toString();
	}
	
}