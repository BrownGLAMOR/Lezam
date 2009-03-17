package agents;

import java.util.Hashtable;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class SSBBidStrategy {
	protected Set<Query> _querySpace;
	
	protected double _campaignSpendLimit;
	
	protected double _defaultQuerySpendLimit;
	protected Ad _defaultQueryAd;
	protected double _defaultQueryConversion;
	protected double _defaultQueryReinvestFactor;
	protected double _defaultQueryConversionRevenue;
	
	protected Hashtable<Query, Double> _querySpendLimit;
	protected Hashtable<Query, Ad> _queryAd;
	protected Hashtable<Query, Double> _queryConversion;
	protected Hashtable<Query, Double> _queryReinvestFactor;
	protected Hashtable<Query, Double> _queryConversionRevenue;
	
	
	//F0_Query_Bid = Revenue * F0_Conversion * ACC
	
	public SSBBidStrategy(Set<Query> querySpace){
		_querySpace = querySpace;
		_campaignSpendLimit = Double.NaN;
		_defaultQuerySpendLimit = Double.NaN;
		_defaultQueryAd = null;//?
		
		
		_defaultQueryConversion = 0.1;
		_defaultQueryReinvestFactor = 0.5;
		_defaultQueryConversionRevenue = 10;
		
		_querySpendLimit = new Hashtable<Query, Double>();
		_queryAd = new Hashtable<Query, Ad>();
		_queryConversion = new Hashtable<Query, Double>();
		_queryReinvestFactor = new Hashtable<Query, Double>();
		_queryConversionRevenue = new Hashtable<Query, Double>();
	}

	public Set<Query> getQuerySpace(){return _querySpace;}
	public double getCampaignSpendLimit(){return _campaignSpendLimit;}
	public double getQuerySpendLimit(Query q){
		if(_querySpendLimit.containsKey(q)){
			return _querySpendLimit.get(q);
		}
		else {
			return _defaultQuerySpendLimit;
		}
	}
	
	public Ad getQueryAd(Query q){
		if(_queryAd.containsKey(q)){
			return _queryAd.get(q);
		}
		else {
			return _defaultQueryAd;
		}
	}
	
	public double getQueryConversion(Query q){
		if(_queryConversion.containsKey(q)){
			return _queryConversion.get(q);
		}
		else {
			return _defaultQueryConversion;
		}
	}
	
	public double getQueryReinvestFactor(Query q){
		if(_queryReinvestFactor.containsKey(q)){
			return _queryReinvestFactor.get(q);
		}
		else {
			return _defaultQueryReinvestFactor;
		}
	}
	
	public double getQueryConversionRevenue(Query q){
		if(_queryConversionRevenue.containsKey(q)){
			return _queryConversionRevenue.get(q);
		}
		else {
			return _defaultQueryConversionRevenue;
		}
	}
	
	public void setQuerySpendLimit(Query q, double d){_querySpendLimit.put(q, d);}
	public void setQueryAd(Query q, Ad d){_queryAd.put(q, d);}
	public void setQueryConversion(Query q, double d){_queryConversion.put(q, d);}
	public void setQueryReinvestFactor(Query q, double d){_queryReinvestFactor.put(q, d);}
	public void setQueryConversionRevenue(Query q, double d){_queryConversionRevenue.put(q, d);}
	public void setQueryCampaignSpendLimit(double campaignSpendLimit){_campaignSpendLimit = campaignSpendLimit;}
	
	
	public void setDefaultQuerySpendLimit(double d){_defaultQuerySpendLimit = d;}
	public void setDefaultQueryAd(Ad d){_defaultQueryAd = d;}
	public void setDefaultQueryConversion(double d){_defaultQueryConversion = d;}
	public void setDefaultReinvestFactor(double d){_defaultQueryReinvestFactor = d;}
	public void setDefaultConversionRevenue(double d){_defaultQueryConversionRevenue = d;}

	
	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		
		for(Query query : _querySpace) {
			Ad ad = _defaultQueryAd;
			if(_queryAd.containsKey(query)){
				ad = _queryAd.get(query);
			}
			double spendLimit = _defaultQuerySpendLimit;
			if(_querySpendLimit.containsKey(query)){
				spendLimit = _querySpendLimit.get(query);
			}
			
			double queryConversion = _defaultQueryConversion;
			double queryAdvertisingCost = _defaultQueryReinvestFactor;
			double queryConversionRevenue = _defaultQueryConversionRevenue;
			
			
			
			if(_queryConversion.containsKey(query)){
				queryConversion = _queryConversion.get(query);
			}
			if(_queryReinvestFactor.containsKey(query)){
				queryAdvertisingCost = _queryReinvestFactor.get(query);
			}
			if(_queryConversionRevenue.containsKey(query)){
				queryConversionRevenue = _queryConversionRevenue.get(query);
			}
			
			double queryBid = queryConversionRevenue*queryConversion*queryAdvertisingCost;

			bidBundle.addQuery(query, queryBid, ad);
			bidBundle.setDailyLimit(query, spendLimit);
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;
	}
}
