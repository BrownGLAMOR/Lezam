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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	private String _agentIn;
	private int _numSims;
	private JComboBox _numSimsList;
	private JPanel simsPanel;
	private Dimension _prefSize;
	private LinkedList<LinkedList<Reports>> _reportsList;
	private BasicSimulator _simulator;
	private JLabel lblChart;

	public MainPanel(SimulatorGUI simulatorGUI, BasicSimulator simulator, GameStatus status, String agentIn, String agentOut, int numSims, Dimension prefSize, LinkedList<LinkedList<Reports>> reportsList) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		_simulatorGUI = simulatorGUI;
		_simulator = simulator;
		_gameStatus = status;
		_agentIn = agentIn;
		_agentOut = agentOut;
		_numSims = numSims;
		_prefSize = prefSize;
		_reportsList = reportsList;
		
		simsPanel = new JPanel(new FlowLayout());
		String[] numSimsStrings = { "Total Profits", "Daily Profits" };
		_numSimsList = new JComboBox(numSimsStrings);
		_numSimsList.setSelectedIndex(0);
		_numSimsList.addActionListener(new NumSimsListener());
		simsPanel.add(_numSimsList);
		
		JPanel rechartPanel = new JPanel(new FlowLayout());
		JButton rechartButton = new JButton("Draw Chart");
		rechartButton.addActionListener(new RechartButtonListener());
		rechartPanel.add(rechartButton);
		
		JPanel chartPanel = new JPanel();
		BoxLayout chartLayout = new BoxLayout(chartPanel,BoxLayout.PAGE_AXIS);
		chartPanel.setLayout(chartLayout);
		chartPanel.setSize(prefSize);
		chartPanel.setPreferredSize(prefSize);

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

	public JFreeChart fullSimProfitsChart(LinkedList<LinkedList<Reports>> list) {
		double[] minProfit = new double[list.get(0).size()];
		double[] maxProfit = new double[list.get(0).size()];
		double[] avgProfit = new double[list.get(0).size()];
		
		for(int i = 0; i < list.get(0).size(); i++) {
			minProfit[i] = Double.MAX_VALUE;
			maxProfit[i] = -Double.MAX_VALUE;
			avgProfit[i] = 0.0;
		}
		
		for(int day = 0; day < list.get(0).size(); day++) {
			for(int sim = 0; sim < list.size(); sim++) {
				Reports reports = list.get(sim).get(day);
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
		for(int day = 0; day < list.get(0).size(); day++) {
			avgProfit[day] = avgProfit[day] / list.size();
		}
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();

		YIntervalSeries series = new YIntervalSeries(_agentIn);

		double totAvgProfit = 0.0;
		double totMinProfit = 0.0;
		double totMaxProfit = 0.0;
		for(int day = 0; day < list.get(0).size(); day++) {
			totMinProfit = totAvgProfit + minProfit[day];
			totMaxProfit = totAvgProfit + maxProfit[day];
			totAvgProfit += avgProfit[day];
			series.add(day, totAvgProfit, totMinProfit, totMaxProfit);
		}
		yIntervalSeriesColl.addSeries(series);
		XYDataset xyDataset = yIntervalSeriesColl;
		JFreeChart chart = ChartFactory.createXYLineChart
		("Simulation ",  // Title
				"Day",           // X-Axis label
				"Profit",           // Y-Axis label
				xyDataset,          // Dataset
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		

        chart.setBackgroundPaint(Color.white);
        
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesStroke(1, new BasicStroke(3.0f));
        renderer.setSeriesFillPaint(0, new Color(0, 0, 255));
        renderer.setSeriesFillPaint(1, new Color(255, 0, 0));
        plot.setRenderer(renderer);

        // change the auto tick unit selection to integer units only...
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		return chart;
	}

	public JFreeChart dailyProfitsChart(LinkedList<LinkedList<Reports>> list) {
		double[] minProfit = new double[list.get(0).size()];
		double[] maxProfit = new double[list.get(0).size()];
		double[] avgProfit = new double[list.get(0).size()];
		
		for(int i = 0; i < list.get(0).size(); i++) {
			minProfit[i] = Double.MAX_VALUE;
			maxProfit[i] = -Double.MAX_VALUE;
			avgProfit[i] = 0.0;
		}
		
		for(int day = 0; day < list.get(0).size(); day++) {
			for(int sim = 0; sim < list.size(); sim++) {
				Reports reports = list.get(sim).get(day);
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
		for(int day = 0; day < list.get(0).size(); day++) {
			avgProfit[day] = avgProfit[day] / list.size();
		}
		YIntervalSeriesCollection yIntervalSeriesColl = new YIntervalSeriesCollection();

		YIntervalSeries series = new YIntervalSeries(_agentIn);

		for(int day = 0; day < list.get(0).size(); day++) {
			series.add(day, avgProfit[day], minProfit[day], maxProfit[day]);
		}
		yIntervalSeriesColl.addSeries(series);
		XYDataset xyDataset = yIntervalSeriesColl;
		JFreeChart chart = ChartFactory.createXYLineChart
		("Simulation ",  // Title
				"Day",           // X-Axis label
				"Profit",           // Y-Axis label
				xyDataset,          // Dataset
				PlotOrientation.VERTICAL,
				true,
				true,
				false);
		

        chart.setBackgroundPaint(Color.white);
        
        // get a reference to the plot for further customisation...
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        
        DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesStroke(1, new BasicStroke(3.0f));
        renderer.setSeriesFillPaint(0, new Color(0, 0, 255));
        renderer.setSeriesFillPaint(1, new Color(255, 0, 0));
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
		    String simSelection = (String)_numSimsList.getSelectedItem();
		    int sim = 0;
		    JFreeChart chart;
		    if(simSelection.equals("Total Profits")) {
		    	chart = fullSimProfitsChart(_reportsList);
		    }
		    else if (simSelection.equals("Daily Profits")) {
		    	chart = dailyProfitsChart(_reportsList);
		    }
		    else {
		    	throw new RuntimeException("Bad Selection");
		    }
			BufferedImage image = chart.createBufferedImage((_prefSize.width*4)/5,(_prefSize.height*4)/5);
			lblChart.setIcon(new ImageIcon(image));
		}
	}

	class NumSimsListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox)e.getSource();
		    String simSelection = (String)cb.getSelectedItem();
		    if(simSelection == "Average Simulation") {
		    }
		    else if (simSelection == "Specific Simulation") {
		    }
		}
	}
	

}
