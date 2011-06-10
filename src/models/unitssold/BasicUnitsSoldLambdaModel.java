package models.unitssold;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BasicUnitsSoldLambdaModel extends AbstractUnitsSoldModel {

   protected Set<Query> _querySpace;
   protected int _capacity;
   protected int _window;
   protected List<Integer> _sold;
   private double _dailyCapacityLambda;

   public BasicUnitsSoldLambdaModel(Set<Query> querySpace, int distributionCapacity, int distributionWindow, double dailyCapacityLambda) {
      _querySpace = querySpace;
      _capacity = distributionCapacity;
      _window = distributionWindow;
      _dailyCapacityLambda = dailyCapacityLambda;
      _sold = new ArrayList<Integer>();
   }

   @Override
   public double getWindowSold() {
      double total = 0;
      for (int i = 0; i < _window - 2; i++) {
         int index = _sold.size() - i - 1;
         if (index >= 0) {
            total += _sold.get(index);
         } else {
            total += _capacity / ((double) _window);
         }
      }
      total = (total + _capacity / ((double) _window) * _dailyCapacityLambda) / 4.0 + total;
      return total;
   }

   @Override
   public void update(SalesReport salesReport) {
      int conversions = 0;
      for (Query q : _querySpace) {
         conversions += salesReport.getConversions(q);
      }
      _sold.add(conversions);
   }

   @Override
   public AbstractModel getCopy() {
      return new BasicUnitsSoldLambdaModel(_querySpace, _capacity, _window, _dailyCapacityLambda);
   }

}
