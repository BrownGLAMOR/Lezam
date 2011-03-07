package models.bidtocpc;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

public class ConstantBidToCPC extends AbstractBidToCPC {

   private double _constant;

   public ConstantBidToCPC(double constant) {
      if ((constant < 0) || (constant > 1)) {
         throw new RuntimeException("Constant for ConstantBidToCPC must be 0 <= c <= 1");
      }
      _constant = constant;
   }

   @Override
   public double getPrediction(Query query, double bid) {
      return bid * _constant;
   }

   @Override
   public String toString() {
      return "ConstantBidToCPC, c = " + _constant;
   }

   @Override
   public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new ConstantBidToCPC(_constant);
   }

}
