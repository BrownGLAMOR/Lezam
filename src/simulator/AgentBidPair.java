package simulator;

/**
 * This class will be used for ranking agents in auctions
 */

public class AgentBidPair implements Comparable<AgentBidPair> {

   private SimAgent _agent;
   private double _bid;

   public AgentBidPair(SimAgent agent, double bid) {
      _agent = agent;
      _bid = bid;
   }

   public SimAgent getAgent() {
      return _agent;
   }

   public void setAgent(SimAgent agent) {
      _agent = agent;
   }

   public double getSquashedBid() {
      return _bid;
   }

   public void setBid(double bid) {
      _bid = bid;
   }

   /*
     *
     * TODO take Double.NaN into account.. :(
     *
     */
   public int compareTo(AgentBidPair agentBidPair) {
      double ourBid = this._bid;
      double otherBid = agentBidPair.getSquashedBid();
      if (ourBid < otherBid) {
         return 1;
      }
      if (otherBid < ourBid) {
         return -1;
      } else {
         return 0;
      }
   }
}
