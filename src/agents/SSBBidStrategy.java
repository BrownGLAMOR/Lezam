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
	
	
	public SSBBidStrategy(Set<Query> querySpace){
		_querySpace = querySpace;
		_campaignSpendLimit = Double.NaN;
		_defaultQuerySpendLimit = Double.NaN;
		_defaultQueryAd = new Ad();
		
		_defaultQueryConversion = 0.1;
		_defaultQueryReinvestFactor = 0.3;
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
	
	public double getQueryBid(Query q) {
		return getQueryConversionRevenue(q)*getQueryConversion(q)*getQueryReinvestFactor(q);
	}
	
	public void setQuerySpendLimit(Query q, double d){_querySpendLimit.put(q, d);}
	public void setQueryAd(Query q, Ad d){_queryAd.put(q, d);}
	public void setQueryConversion(Query q, double d){_queryConversion.put(q, d);}
	public void setQueryReinvestFactor(Query q, double d){_queryReinvestFactor.put(q, d);}
	public void setQueryConversionRevenue(Query q, double d){_queryConversionRevenue.put(q, d);}
	
	
	public void setCampaignSpendLimit(double campaignSpendLimit){_campaignSpendLimit = campaignSpendLimit;}
	public void setDefaultQuerySpendLimit(double d){_defaultQuerySpendLimit = d;}
	public void setDefaultQueryAd(Ad d){_defaultQueryAd = d;}
	public void setDefaultQueryConversion(double d){_defaultQueryConversion = d;}
	public void setDefaultReinvestFactor(double d){_defaultQueryReinvestFactor = d;}
	public void setDefaultConversionRevenue(double d){_defaultQueryConversionRevenue = d;}

	public double getBid(Query q){
		return getQueryConversionRevenue(q)*getQueryConversion(q)*getQueryReinvestFactor(q);
	}
	
	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		
		for(Query q : _querySpace) {
			double queryBid = getBid(q);

			bidBundle.addQuery(q, queryBid, getQueryAd(q));
			bidBundle.setDailyLimit(q, getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(getQueryBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
			buff.append("\t").append("Conversion: ").append(getQueryConversion(q)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(getQueryReinvestFactor(q)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(getQueryConversionRevenue(q)).append("\n");
		}
		return buff.toString();
	}
	
}
