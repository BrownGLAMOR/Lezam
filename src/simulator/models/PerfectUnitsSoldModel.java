package simulator.models;

import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.unitssold.AbstractUnitsSoldModel;


public class PerfectUnitsSoldModel extends AbstractUnitsSoldModel {

   private Integer[] _salesOverWindow;
   private int totOver4Days;
   protected int _capacity;
   protected int _window;

   public PerfectUnitsSoldModel(Integer[] salesOverWindow, int capacity, int window) {
      _salesOverWindow = salesOverWindow;
      totOver4Days = 0;
      for (int i = 0; i < _salesOverWindow.length - 1; i++) {
         totOver4Days += _salesOverWindow[i];
//			System.out.println("Sales " + (i+1) + " days ago: " + _salesOverWindow[i]);
      }
      _capacity = capacity;
      _window = window;
//		System.out.println("Total over 4 days: " + totOver4Days);
//		System.out.println("Total over 5 days: " + (totOver4Days+_salesOverWindow[_salesOverWindow.length-1]));
   }

   @Override
   public double getEstimate() {
      return totOver4Days;
   }

   @Override
   public double getLatestSample() {
      return totOver4Days;
   }

   @Override
   public double getWindowSold() {
      return totOver4Days;
   }

   @Override
   public void update(SalesReport salesReport) {
   }


   public double getThreeDaysSold() {
      double total = 0;
      for (int i = 0; i < _window - 2; i++) {
         int index = _salesOverWindow.length - i - 1;
         if (index >= 0) {
            total += _salesOverWindow[index];
         } else {
            total += _capacity / ((double) _window);
         }
      }
      return total;
   }

   @Override
   public AbstractModel getCopy() {
      return new PerfectUnitsSoldModel(_salesOverWindow, _capacity, _window);
   }

}
