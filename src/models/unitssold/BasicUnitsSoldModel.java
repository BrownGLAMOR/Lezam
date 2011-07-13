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
   public double getWindowSold() {
      double total = 0;
      for (int i = 0; i < _window - 2; i++) {
         int index = _sold.size() - i - 1;
         if (index >= 0) {
            total += _sold.get(index);
         } else {
        	 //If there are days in the window that don't have historical sales records
        	 //(i.e. days that occur before the start of the game),
        	 //fill in with default values specified by the TAC AA spec.
            total += _capacity / ((double) _window);
         }
      }
      if (expectedConvsForMissingDay == null) {
    	  //expectedConvsForMissingDay = average of each #conversions that occurred within window and the 
    	  //"expected share" of daily conversions (if evenly distributed across the window) 
    	  //FIXME: When is expectedConvsForMissingDay null? Is there a better default predictor we could use?
         total += (total + _capacity / ((double) _window)) / 4.0;
      } else {
         total += expectedConvsForMissingDay;
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
      if(expectedConvsForMissingDay == null) {
         double total = 0;
         for (int i = 0; i < _window - 2; i++) {
            int index = _sold.size() - i - 1;
            if (index >= 0) {
               total += _sold.get(index);
            } else {
               total += _capacity / ((double) _window);
            }
         }
         return (int)((total + _capacity / ((double) _window)) / 4.0);
      }
      else {
         return expectedConvsForMissingDay;
      }
   }

   @Override
   public AbstractModel getCopy() {
      return new BasicUnitsSoldModel(_querySpace, _capacity, _window);
   }

}
