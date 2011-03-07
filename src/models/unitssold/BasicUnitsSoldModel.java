package models.unitssold;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

import java.util.ArrayList;
import java.util.Set;

public class BasicUnitsSoldModel extends AbstractUnitsSoldModel {

   protected Set<Query> _querySpace;
   protected int _capacity;
   protected int _window;
   protected ArrayList<Integer> _sold;
   protected Integer expectedConvsForMissingDay;

   public BasicUnitsSoldModel(Set<Query> querySpace, int distributionCapacity, int distributionWindow) {
      _querySpace = querySpace;
      _capacity = distributionCapacity;
      _window = distributionWindow;
      _sold = new ArrayList<Integer>();
      expectedConvsForMissingDay = null;
   }

   public ArrayList<Integer> getSalesArray() {
      return _sold;
   }

   @Override
   public double getEstimate() {
      throw new RuntimeException("Do not use this method");
   }

   @Override
   public double getLatestSample() {
      throw new RuntimeException("Do not use this method");
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
      if (expectedConvsForMissingDay == null) {
         total += (total + _capacity / ((double) _window)) / 4.0;
      } else {
         total += expectedConvsForMissingDay;
      }
      return total;
   }

   public double getThreeDaysSold() {
      double total = 0;
      for (int i = 0; i < _window - 2; i++) {
         int index = _sold.size() - i - 1;
         if (index >= 0) {
            total += _sold.get(index);
         } else {
            total += _capacity / ((double) _window);
         }
      }
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

   public void expectedConvsTomorrow(int expectedConvs) {
      expectedConvsForMissingDay = expectedConvs;
   }

   public Integer getExpectedConvsTomorrow() {
      return expectedConvsForMissingDay;
   }

   @Override
   public AbstractModel getCopy() {
      return new BasicUnitsSoldModel(_querySpace, _capacity, _window);
   }

}
