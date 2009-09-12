package carleton.strategies;

import java.util.Hashtable;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class GenericBidStrategy {
	public static String BID = "Bid";
	
	/* General Constants */
	public static String CONVERSION_PR= "ConversionPr";
	public static String CONVERSION_REVENUE= "ConversionRevenue";
	
	/* SSB Constants */
	public static String REINVEST_FACTOR = "ReinvestFactor";
	
	/* JESOM Constants */
	public static String WANTED_SALES = "WantedSales";
	public static String HONESTY_FACTOR = "HonestyFactor";
	
	
	protected Set<Query> _querySpace;
	
	protected double _campaignSpendLimit;
	protected double _defaultQuerySpendLimit;
	protected Ad _defaultQueryAd;
	
	protected Hashtable<Query, Double> _querySpendLimit;
	protected Hashtable<Query, Ad> _queryAd;

	protected Hashtable<String, Double> _defaultProperties;
	protected Hashtable<String, Hashtable<Query, Double>> _queryProperties;
	
	public GenericBidStrategy(Set<Query> querySpace){
		_querySpace = querySpace;
		_campaignSpendLimit = Double.NaN;
		_defaultQuerySpendLimit = Double.NaN;
		_defaultQueryAd = new Ad();
		
		_querySpendLimit = new Hashtable<Query, Double>();
		_queryAd = new Hashtable<Query, Ad>();
		
		_defaultProperties = new Hashtable<String, Double>();
		_queryProperties = new Hashtable<String, Hashtable<Query, Double>>();
		
		setDefaultProperty(BID, Double.NaN);
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
	
	public double getProperty(Query q, String key){
		if(_queryProperties.containsKey(key) && _queryProperties.get(key).containsKey(q)){
			return _queryProperties.get(key).get(q);
		}
		if(_defaultProperties.containsKey(key)){
			return _defaultProperties.get(key);
		}
		return Double.NaN;
	}
	
	public void setProperty(Query q, String key, double value){
		if(_queryProperties.containsKey(key)){
			_queryProperties.get(key).put(q,value);
		} else {
			Hashtable<Query, Double> tbl = new Hashtable<Query, Double>();
			tbl.put(q, value);
			_queryProperties.put(key, tbl);
		}
	}
	
	public void setDefaultProperty(String key, double value){
		_defaultProperties.put(key, value);
	}
	
	public void setQuerySpendLimit(Query q, double d){_querySpendLimit.put(q, d);}
	public void setQueryAd(Query q, Ad d){_queryAd.put(q, d);}
	
	public void setCampaignSpendLimit(double campaignSpendLimit){_campaignSpendLimit = campaignSpendLimit;}
	public void setDefaultQuerySpendLimit(double d){_defaultQuerySpendLimit = d;}
	public void setDefaultQueryAd(Ad d){_defaultQueryAd = d;}

	/**
	 * extend this method if you are planning to use a diffrent bid calulation
	 * @param q
	 * @return
	 */
	public double getQueryBid(Query q){return getProperty(q, BID);}
	
	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		
		for(Query q : _querySpace) {
			double queryBid = getQueryBid(q);

			bidBundle.addQuery(q, queryBid, getQueryAd(q));
			bidBundle.setDailyLimit(q, getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;
	}

	public void propertiesToString(StringBuffer buff, Query q){}
	
	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(getQueryBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
			propertiesToString(buff,q);
		}
		return buff.toString();
	}
	
}
