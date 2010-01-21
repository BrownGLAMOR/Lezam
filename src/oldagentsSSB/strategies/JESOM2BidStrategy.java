package oldagentsSSB.strategies;

import java.util.Set;


import edu.umich.eecs.tac.props.Query;

public class JESOM2BidStrategy extends GenericBidStrategy {
	
	public JESOM2BidStrategy(Set<Query> querySpace){
		super(querySpace);
		setDefaultProperty(CONVERSION_PR, 0.1);
		setDefaultProperty(WANTED_SALES, 4);
		setDefaultProperty(CONVERSION_REVENUE, 10);
		setDefaultProperty(HONESTY_FACTOR, 0.75);
	}
	


	@Override
	public double getQueryBid(Query q){
		return getProperty(q,HONESTY_FACTOR)*getProperty(q,CONVERSION_REVENUE)*getProperty(q,CONVERSION_PR);
	}

	public void propertiesToString(StringBuffer buff, Query q){
		buff.append("\t").append("Conversion: ").append(getProperty(q,CONVERSION_PR)).append("\n");
		buff.append("\t").append("HonestyFactor: ").append(getProperty(q,HONESTY_FACTOR)).append("\n");
		buff.append("\t").append("ConversionRevenue: ").append(getProperty(q,CONVERSION_REVENUE)).append("\n");
		buff.append("\t").append("WantedSales: ").append(getProperty(q,WANTED_SALES)).append("\n");
		buff.append("\t").append("clicks: ").append(getProperty(q,WANTED_SALES) / getProperty(q,CONVERSION_PR)).append("\n");
	}
	
	/*public String toString(){
		String toReturn = "";
		StringBuffer buff;
		for (Query q: _querySpace){
			buff = new StringBuffer();
			propertiesToString(buff, q);
			toReturn += buff;
		}
		return toReturn;
	}*/
	
}
