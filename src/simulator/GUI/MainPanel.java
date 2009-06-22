package simulator.GUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;


import simulator.BasicSimulator;
import simulator.Reports;
import simulator.GUI.SetupPanel2.RechooseFileButtonListener;
import simulator.parser.GameStatus;

public class MainPanel  extends JPanel {

	private SimulatorGUI _simulatorGUI;
	private GameStatus _gameStatus;
	private String _agentOut;
	private String[] _agentsIn;
	private int _numSims;
	private JComboBox _numSimsList;
	private JPanel simsPanel;
	private Dimension _prefSize;
	private HashMap<String,LinkedList<LinkedList<Reports>>> _reportsListMap;
	private BasicSimulator _simulator;
	private JLabel lblChart;
	private boolean _deviation;
	private JCheckBox deviationButton;
	
	public MainPanel(SimulatorGUI simulatorGUI, BasicSimulator simulator, GameStatus status, String[] agentsIn, String agentOut, int numSims, Dimension prefSize, HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		_simulatorGUI = simulatorGUI;
		_simulator = simulator;
		_gameStatus = status;
		_agentsIn = agentsIn;
		_agentOut = agentOut;
		_numSims = numSims;
		_prefSize = prefSize;
		_reportsListMap = reportsListMap;

		simsPanel = new JPanel(new FlowLayout());
		String[] numSimsStrings = { "Total Profits", "Daily Profits" , "Daily Impressions" , "Daily Clicks" , "Daily Conversions"};
		_numSimsList = new JComboBox(numSimsStrings);
		_numSimsList.setSelectedIndex(0);
		simsPanel.add(_numSimsList);
		
		JPanel deviationPanel = new JPanel(new FlowLayout());
		deviationButton = new JCheckBox("Min/Max Deviations",true);
		deviationPanel.add(deviationButton);
		
		simsPanel.add(deviationPanel);

		JPanel rechartPanel = new JPanel(new FlowLayout());
		JButton rechartButton = new JButton("Draw Chart");
		rechartButton.addActionListener(new RechartButtonListener());
		rechartPanel.add(rechartButton);

		JPanel chartPanel = new JPanel();
		BoxLayout chartLayout = new BoxLayout(chartPanel,BoxLayout.PAGE_AXIS);
		chartPanel.setLayout(chartLayout);
		chartPanel.setSize((_prefSize.width*4)/5,(_prefSize.height*4)/5);
		chartPanel.setPreferredSize(new Dimension((_prefSize.width*4)/5,(_prefSize.height*4)/5));

		lblChart = new JLabel();
		chartPanel.add(lblChart);

		JPanel newParsePanel = new JPanel(new FlowLayout());
		JButton newParseButton = new JButton("Select a new file to parse");
		newParseButton.addActionListener(new RechooseFileButtonListener());
		newParsePanel.add(newParseButton);

		this.add(simsPanel);
		this.add(rechartPanel);
		this.add(chartPanel);
		this.add(newParsePanel);
	}

	public JFreeChart fullSimProfitsChart(HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
		for(int i = 0; i < agentsIn.length; i++) {
			double[] minProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] maxProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] avgProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

			for(int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
				minProfit[j] = Double.MAX_VALUE;
				maxProfit[j] = -Double.MAX_VALUE;
				avgProfit[j] = 0.0;
			}

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				for(int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
					Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
					SalesReport salesReport = reports.getSalesReport();
					QueryReport queryReport = reports.getQueryReport();
					double totRevenue = 0.0;
					double totCost = 0.0;
					for(Query query : _simulator.getQuerySpace()) {
						totRevenue += salesReport.getRevenue(query);
						totCost += queryReport.getCost(query);
					}
					double totProfit = totRevenue - totCost;
					avgProfit[day] += totProfit;
					if(totProfit > maxProfit[day]) {
						maxProfit[day] = totProfit;
					}
					else if(totProfit < minProfit[day]) {
						minProfit[day] = totProfit;
					}
				}
			}
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				avgProfit[day] = avgProfit[day] / reportsListMap.get(agentsIn[i]).size();
			}

			YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

			double totAvgProfit = 0.0;
			double totMinProfit = 0.0;
			double totMaxProfit = 0.0;
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
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

	public JFreeChart dailyProfitsChart(HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
		for(int i = 0; i < agentsIn.length; i++) {
			double[] minProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] maxProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] avgProfit = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

			for(int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
				minProfit[j] = Double.MAX_VALUE;
				maxProfit[j] = -Double.MAX_VALUE;
				avgProfit[j] = 0.0;
			}

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				for(int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
					Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
					SalesReport salesReport = reports.getSalesReport();
					QueryReport queryReport = reports.getQueryReport();
					double totRevenue = 0.0;
					double totCost = 0.0;
					for(Query query : _simulator.getQuerySpace()) {
						totRevenue += salesReport.getRevenue(query);
						totCost += queryReport.getCost(query);
					}
					double totProfit = totRevenue - totCost;
					avgProfit[day] += totProfit;
					if(totProfit > maxProfit[day]) {
						maxProfit[day] = totProfit;
					}
					else if(totProfit < minProfit[day]) {
						minProfit[day] = totProfit;
					}
				}
			}
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				avgProfit[day] = avgProfit[day] / reportsListMap.get(agentsIn[i]).size();
			}

			YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
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
	
	public JFreeChart dailyClicksChart(HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
		for(int i = 0; i < agentsIn.length; i++) {
			double[] minClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] maxClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] avgClicks = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

			for(int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
				minClicks[j] = Double.MAX_VALUE;
				maxClicks[j] = -Double.MAX_VALUE;
				avgClicks[j] = 0.0;
			}

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				for(int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
					Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
					QueryReport queryReport = reports.getQueryReport();
					double totClicks = 0.0;
					for(Query query : _simulator.getQuerySpace()) {
						totClicks += queryReport.getClicks(query);
					}
					avgClicks[day] += totClicks;
					if(totClicks > maxClicks[day]) {
						maxClicks[day] = totClicks;
					}
					else if(totClicks < minClicks[day]) {
						minClicks[day] = totClicks;
					}
				}
			}
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				avgClicks[day] = avgClicks[day] / reportsListMap.get(agentsIn[i]).size();
			}

			YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
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
	
	
	public JFreeChart dailyImpsChart(HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
		for(int i = 0; i < agentsIn.length; i++) {
			double[] minImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] maxImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] avgImps = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

			for(int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
				minImps[j] = Double.MAX_VALUE;
				maxImps[j] = -Double.MAX_VALUE;
				avgImps[j] = 0.0;
			}

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				for(int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
					Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
					QueryReport queryReport = reports.getQueryReport();
					double totClicks = 0.0;
					for(Query query : _simulator.getQuerySpace()) {
						totClicks += queryReport.getImpressions(query);
					}
					avgImps[day] += totClicks;
					if(totClicks > maxImps[day]) {
						maxImps[day] = totClicks;
					}
					else if(totClicks < minImps[day]) {
						minImps[day] = totClicks;
					}
				}
			}
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				avgImps[day] = avgImps[day] / reportsListMap.get(agentsIn[i]).size();
			}

			YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
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
	
	public JFreeChart dailyConvsChart(HashMap<String,LinkedList<LinkedList<Reports>>> reportsListMap, String[] agentsIn) {
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();
		for(int i = 0; i < agentsIn.length; i++) {
			double[] minConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] maxConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];
			double[] avgConvs = new double[reportsListMap.get(agentsIn[i]).get(0).size()];

			for(int j = 0; j < reportsListMap.get(agentsIn[i]).get(0).size(); j++) {
				minConvs[j] = Double.MAX_VALUE;
				maxConvs[j] = -Double.MAX_VALUE;
				avgConvs[j] = 0.0;
			}

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				for(int sim = 0; sim < reportsListMap.get(agentsIn[i]).size(); sim++) {
					Reports reports = reportsListMap.get(agentsIn[i]).get(sim).get(day);
					SalesReport salesReport = reports.getSalesReport();
					double totClicks = 0.0;
					for(Query query : _simulator.getQuerySpace()) {
						totClicks += salesReport.getConversions(query);
					}
					avgConvs[day] += totClicks;
					if(totClicks > maxConvs[day]) {
						maxConvs[day] = totClicks;
					}
					else if(totClicks < minConvs[day]) {
						minConvs[day] = totClicks;
					}
				}
			}
			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
				avgConvs[day] = avgConvs[day] / reportsListMap.get(agentsIn[i]).size();
			}

			YIntervalSeries series = new YIntervalSeries(_agentsIn[i]);

			for(int day = 0; day < reportsListMap.get(agentsIn[i]).get(0).size(); day++) {
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


	private JFreeChart addDeviation(JFreeChart chart) {
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

	class RechooseFileButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_simulatorGUI.resetFile();
		}
	}

	class RechartButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			_deviation = deviationButton.isSelected();
			String simSelection = (String)_numSimsList.getSelectedItem();
			int sim = 0;
			JFreeChart chart;
			if(simSelection.equals("Total Profits")) {
				chart = fullSimProfitsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Profits")) {
				chart = dailyProfitsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Impressions")) {
				chart = dailyImpsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Clicks")) {
				chart = dailyClicksChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Conversions")) {
				chart = dailyConvsChart(_reportsListMap,_agentsIn);
			}
			else {
				throw new RuntimeException("Bad Selection");
			}
			if(_deviation) {
				chart = addDeviation(chart);
			}
			BufferedImage image = chart.createBufferedImage((_prefSize.width*4)/5,(_prefSize.height*4)/5);
			lblChart.setIcon(new ImageIcon(image));
		}
	}


}
