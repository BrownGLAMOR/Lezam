package agents;

import java.util.Set;

import edu.umich.eecs.tac.props.Query;

public class SSBBidStrategy extends GenericBidStrategy {
	public static String CONVERSION_PR= "ConversionPr";
	public static String REINVEST_FACTOR = "ReinvestFactor";
	public static String CONVERSION_REVENUE= "ConversionRevenue";
	
	public SSBBidStrategy(Set<Query> querySpace){
		super(querySpace);
		setDefaultProperty(CONVERSION_PR, 0.1);
		setDefaultProperty(REINVEST_FACTOR, 0.3);
		setDefaultProperty(CONVERSION_REVENUE, 10);
	}

	@Override
	public double getQueryBid(Query q){
		return getProperty(q,CONVERSION_PR)*getProperty(q,CONVERSION_REVENUE)*getProperty(q,REINVEST_FACTOR);
	}

	public void propertiesToString(StringBuffer buff, Query q){
		buff.append("\t").append("Conversion: ").append(getProperty(q,CONVERSION_PR)).append("\n");
		buff.append("\t").append("ReinvestFactor: ").append(getProperty(q,REINVEST_FACTOR)).append("\n");
		buff.append("\t").append("ConversionRevenue: ").append(getProperty(q,CONVERSION_REVENUE)).append("\n");
	}
	
}
