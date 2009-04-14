package agents;

import java.util.Hashtable;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class G3BidStrategy extends SSBBidStrategy{
	
	protected int _capacity;
	protected double _pp; //Product Price
	protected double _revenue;
	protected int _quantity;

	protected Hashtable<Query, Double> _CPS; //Cost Per Sale
	protected Hashtable<Query, Double> _CTR;
	protected Hashtable<Query, Double> _convProb;
	protected Hashtable<Query, Double> _bid;

	public G3BidStrategy(Set<Query> querySpace){
		super(querySpace);
		_CPS = new Hashtable<Query, Double>();
		_CTR = new Hashtable<Query, Double>();
		_convProb = new Hashtable<Query, Double>();
		_bid = new Hashtable<Query, Double>();
		for (Query q : _querySpace) {
			_bid.put(q, 1.0);
			_CPS.put(q, 5.0);
			System.out.println ("LoopBID = " + _bid.get(q));
		}
	}

	public boolean setData(Query q, double CTR, double convProb) {
		/*if (!(Double.isInfinite(CPS) || Double.isNaN(CPS))) {
			_CPS.put(q, 5.0);
			System.out.println ("CPS 5");
		} else {
			_CPS.put(q, CPS);
			System.out.println ("CPS real");
		}*/
		if (!(Double.isInfinite(CTR) || Double.isNaN(CTR))) _CTR.put(q, CTR);
		if (convProb > 0) _convProb.put(q, convProb);
		return true;
	}

	@Override
	public double getQueryBid(Query q) {
		return _bid.get(q);
	}
	
	public void setPP(double pp) {
		_pp = pp;
	}
	
	public void setDistributionCapacity (int capacity) {
		_capacity = capacity;
	}

	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		double distanceFrom = 1 - ((double)_quantity/(double)_capacity);
		double increment = _pp/100;
		System.out.println("capacity:" + _capacity);
		System.out.println("quantity:" + _quantity);
		System.out.println("distance:" + distanceFrom);
		System.out.println("increment:" + increment);
		for(Query q : _querySpace) {
			_CPS.put (q, _CPS.get(q) + distanceFrom*increment);
			double queryBid = _CPS.get(q) * _convProb.get(q);
			System.out.println("queryBid:" + queryBid);
			System.out.println("CPS:" + _CPS.get(q));
			//System.out.println("newCPS:" + newCPS);

			if (queryBid > 0.9*_pp) queryBid = 0.9*_pp; //??
			_bid.put (q, queryBid);
//			System.out.println ("##(" + q.getComponent() + "," + q.getManufacturer() + ")## Bid on " + queryBid);
			bidBundle.addQuery(q, queryBid, bidBundle.getAd(q));
//			bidBundle.setDailyLimit(q, bidBundle.getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;	
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bid.get(q)).append("\n");
			buff.append("\t").append("CPS: ").append(_CPS.get(q)).append("\n");
			buff.append("\t").append("CTR: ").append(_CTR.get(q)).append("\n");
			buff.append("\t").append("CP: ").append(_convProb.get(q)).append("\n");
/*			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
			buff.append("\t").append("Conversion: ").append(getQueryConversion(q)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(getQueryReinvestFactor(q)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(getQueryConversionRevenue(q)).append("\n");*/
		}
		return buff.toString();
	}

	public void setConvertions(int quantity) {
		_quantity = quantity;
	}	
}
