package simulator.GUI;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import simulator.BasicSimulator;
import simulator.Reports;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;

public class ChartUtils {

   private BasicSimulator _simulator;
   private String[] _agentsIn;


   public ChartUtils(BasicSimulator simulator, String[] agentsIn) {
      _simulator = simulator;
      _agentsIn = agentsIn;
   }


   public JFreeChart fullSimProfitsChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minProfit[j] = Double.MAX_VALUE;
            maxProfit[j] = -Double.MAX_VALUE;
            avgProfit[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
               SalesReport salesReport = reports.getSalesReport();
               QueryReport queryReport = reports.getQueryReport();
               double totRevenue = 0.0;
               double totCost = 0.0;
               for (Query query : _simulator.getQuerySpace()) {
                  totRevenue += salesReport.getRevenue(query);
                  totCost += queryReport.getCost(query);
               }
               double totProfit = totRevenue - totCost;
               avgProfit[day] += totProfit;
               if (totProfit > maxProfit[day]) {
                  maxProfit[day] = totProfit;
               } else if (totProfit < minProfit[day]) {
                  minProfit[day] = totProfit;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgProfit[day] = avgProfit[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         double totAvgProfit = 0.0;
         double totMinProfit = 0.0;
         double totMaxProfit = 0.0;
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            totMinProfit = totAvgProfit + minProfit[day];
            totMaxProfit = totAvgProfit + maxProfit[day];
            totAvgProfit += avgProfit[day];
            series.add(day, totAvgProfit, totMinProfit, totMaxProfit);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Profit over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Profit",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);

      return chart;
   }

   public JFreeChart dailyProfitsChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minProfit[j] = Double.MAX_VALUE;
            maxProfit[j] = -Double.MAX_VALUE;
            avgProfit[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
               SalesReport salesReport = reports.getSalesReport();
               QueryReport queryReport = reports.getQueryReport();
               double totRevenue = 0.0;
               double totCost = 0.0;
               for (Query query : _simulator.getQuerySpace()) {
                  totRevenue += salesReport.getRevenue(query);
                  totCost += queryReport.getCost(query);
               }
               double totProfit = totRevenue - totCost;
               avgProfit[day] += totProfit;
               if (totProfit > maxProfit[day]) {
                  maxProfit[day] = totProfit;
               } else if (totProfit < minProfit[day]) {
                  minProfit[day] = totProfit;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgProfit[day] = avgProfit[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            series.add(day, avgProfit[day], minProfit[day], maxProfit[day]);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Daily Profit over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Profit",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);

      return chart;
   }

   public JFreeChart dailyClicksChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minClicks[j] = Double.MAX_VALUE;
            maxClicks[j] = -Double.MAX_VALUE;
            avgClicks[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
               QueryReport queryReport = reports.getQueryReport();
               double totClicks = 0.0;
               for (Query query : _simulator.getQuerySpace()) {
                  totClicks += queryReport.getClicks(query);
               }
               avgClicks[day] += totClicks;
               if (totClicks > maxClicks[day]) {
                  maxClicks[day] = totClicks;
               } else if (totClicks < minClicks[day]) {
                  minClicks[day] = totClicks;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgClicks[day] = avgClicks[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            series.add(day, avgClicks[day], minClicks[day], maxClicks[day]);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Clicks over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Clicks",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);


      return chart;
   }


   public JFreeChart dailyImpsChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minImps[j] = Double.MAX_VALUE;
            maxImps[j] = -Double.MAX_VALUE;
            avgImps[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
               QueryReport queryReport = reports.getQueryReport();
               double totClicks = 0.0;
               for (Query query : _simulator.getQuerySpace()) {
                  totClicks += queryReport.getImpressions(query);
               }
               avgImps[day] += totClicks;
               if (totClicks > maxImps[day]) {
                  maxImps[day] = totClicks;
               } else if (totClicks < minImps[day]) {
                  minImps[day] = totClicks;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgImps[day] = avgImps[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            series.add(day, avgImps[day], minImps[day], maxImps[day]);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Impressions over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Impressions",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);

      return chart;
   }

   public JFreeChart dailyConvsChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minConvs[j] = Double.MAX_VALUE;
            maxConvs[j] = -Double.MAX_VALUE;
            avgConvs[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
               SalesReport salesReport = reports.getSalesReport();
               double totClicks = 0.0;
               for (Query query : _simulator.getQuerySpace()) {
                  totClicks += salesReport.getConversions(query);
               }
               avgConvs[day] += totClicks;
               if (totClicks > maxConvs[day]) {
                  maxConvs[day] = totClicks;
               } else if (totClicks < minConvs[day]) {
                  minConvs[day] = totClicks;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgConvs[day] = avgConvs[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            series.add(day, avgConvs[day], minConvs[day], maxConvs[day]);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Conversions over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Conversions",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);

      return chart;
   }

   public JFreeChart dailyWindowChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
      YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
      for (int i = 0; i < agentsIn.length; i++) {
         double[] minConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] maxConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
         double[] avgConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

         for (int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
            minConvs[j] = Double.MAX_VALUE;
            maxConvs[j] = -Double.MAX_VALUE;
            avgConvs[j] = 0.0;
         }

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            for (int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
               LinkedList<Reports> windowedConvsList = new LinkedList<Reports>();
               for (int day2 = 0; day2 < 5; day2++) {
                  if (day - day2 >= 0) {
                     Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day - day2);
                     windowedConvsList.add(reports);
                  }
               }
               double totConvs = 0.0;
               for (Reports reports : windowedConvsList) {
                  SalesReport salesReport = reports.getSalesReport();
                  for (Query query : _simulator.getQuerySpace()) {
                     totConvs += salesReport.getConversions(query);
                  }
               }
               avgConvs[day] += totConvs;
               if (totConvs > maxConvs[day]) {
                  maxConvs[day] = totConvs;
               } else if (totConvs < minConvs[day]) {
                  minConvs[day] = totConvs;
               }
            }
         }
         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            avgConvs[day] = avgConvs[day] / reportsListMap.get(agentsIn[i]).size();
         }

         YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

         for (int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
            series.add(day, avgConvs[day], minConvs[day], maxConvs[day]);
         }
         yIntervalSeriesColl.addSeries(series);
      }
      XYDataset xyDataset = yIntervalSeriesColl;
      JFreeChart chart = ChartFactory.createXYLineChart
              ("Avg Conversions over 5 day window over " + reportsListMap.get(agentsIn[0]).size() + " sims",  // Title
               "Day",           // X-Axis label
               "Conversions",           // Y-Axis label
               xyDataset,          // Dataset
               PlotOrientation.VERTICAL,
               true,
               true,
               false);

      return chart;
   }


   public JFreeChart addDeviation(JFreeChart chart) {
      chart.setBackgroundPaint(Color.white);

      // get a reference to the plot for further customisation...
      XYPlot plot = (XYPlot) chart.getPlot();
      plot.setBackgroundPaint(Color.lightGray);
      plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
      plot.setDomainGridlinePaint(Color.white);
      plot.setRangeGridlinePaint(Color.white);

      DeviationRenderer renderer = new DeviationRenderer(true, false);
      //		renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
      //				BasicStroke.JOIN_ROUND));
      //		renderer.setSeriesStroke(0, new BasicStroke(3.0f));
      //		renderer.setSeriesStroke(1, new BasicStroke(3.0f));
      //		renderer.setSeriesFillPaint(0, new Color(0, 0, 255));
      //		renderer.setSeriesFillPaint(1, new Color(255, 0, 0));
      plot.setRenderer(renderer);

      // change the auto tick unit selection to integer units only...
      NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
      yAxis.setAutoRangeIncludesZero(false);
      yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      return chart;
   }

}
