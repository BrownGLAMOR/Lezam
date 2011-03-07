package oldagentsSSB.strategies;

import edu.umich.eecs.tac.props.Query;

import java.util.Set;

public class SSBBidStrategy extends GenericBidStrategy {
   public SSBBidStrategy(Set<Query> querySpace) {
      super(querySpace);
      setDefaultProperty(CONVERSION_PR, 0.1); // constant set by game info
      setDefaultProperty(REINVEST_FACTOR, 0.3); // constant set by user
      setDefaultProperty(CONVERSION_REVENUE, 10); // constant set by game
   }

   @Override
   public double getQueryBid(Query q) {
      return getProperty(q, CONVERSION_PR) * getProperty(q, CONVERSION_REVENUE) * getProperty(q, REINVEST_FACTOR); // Carlton's bid strategy
   }

   public void propertiesToString(StringBuffer buff, Query q) {
      buff.append("\t").append("Conversion: ").append(getProperty(q, CONVERSION_PR)).append("\n");
      buff.append("\t").append("ReinvestFactor: ").append(getProperty(q, REINVEST_FACTOR)).append("\n");
      buff.append("\t").append("ConversionRevenue: ").append(getProperty(q, CONVERSION_REVENUE)).append("\n");
   }


   @Override
   public void setProperty(Query q, String key, double value) {
      if (key == REINVEST_FACTOR && value < 0.0) {
         System.out.println();
         System.out.println("*!@#$* " + q + " " + key + " <- " + value);
         System.out.println();
         GenericBidStrategy stg = null;
         stg.getCampaignSpendLimit();
      }

      super.setProperty(q, key, value);
   }

}
