package simulator.GUI;

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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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
	private JSlider simSlider;
	private Dimension _prefSize;
	private HashMap<String, LinkedList<LinkedList<Reports>>> _allReportsLists;
	private BasicSimulator _simulator;
	private JLabel lblChart;

	public MainPanel(SimulatorGUI simulatorGUI, BasicSimulator simulator, GameStatus status, String agentIn, String agentOut, int numSims, Dimension prefSize, HashMap<String,LinkedList<LinkedList<Reports>>> allReportsLists) {
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
		_allReportsLists = allReportsLists;
		
		simsPanel = new JPanel(new FlowLayout());
		String[] numSimsStrings = { "Average Simulation", "Specific Simulation" };
		_numSimsList = new JComboBox(numSimsStrings);
		_numSimsList.setSelectedIndex(0);
		_numSimsList.addActionListener(new NumSimsListener());
		simsPanel.add(_numSimsList);
		
		int sliderStart = 1;
		simSlider = new JSlider(JSlider.HORIZONTAL, sliderStart, _numSims, 1);
		simSlider.setMajorTickSpacing(10);
		simSlider.setMinorTickSpacing(1);
		simSlider.setPaintTicks(true);
		simSlider.setEnabled(false);
		JLabel sliderLabel = new JLabel(""+sliderStart);
		simSlider.addChangeListener(new SimSliderListener(sliderLabel));
		simsPanel.add(simSlider);
		simsPanel.add(sliderLabel);

		JPanel chartPanel = new JPanel();
		BoxLayout chartLayout = new BoxLayout(chartPanel,BoxLayout.PAGE_AXIS);
		chartPanel.setLayout(chartLayout);
		chartPanel.setSize(prefSize);
		chartPanel.setPreferredSize(prefSize);
		JButton reChartButton = new JButton("Draw Chart");
		reChartButton.addActionListener(new ReChartButtonListener());
		chartPanel.add(reChartButton);

		lblChart = new JLabel();
		chartPanel.add(lblChart);

		JPanel newParsePanel = new JPanel(new FlowLayout());
		JButton newParseButton = new JButton("Select a new file to parse");
		newParseButton.addActionListener(new RechooseFileButtonListener());
		newParsePanel.add(newParseButton);

		this.add(simsPanel);
		this.add(chartPanel);
		this.add(newParsePanel);
	}
	
	public JFreeChart fullSimProfitsChart(HashMap<String, LinkedList<LinkedList<Reports>>> reportsLists, int simNum) {
		XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
		String[] advertisers = _gameStatus.getAdvertisers();
		for(int i = 0; i < advertisers.length; i++) {
			LinkedList<LinkedList<Reports>> reportsList = reportsLists.get(advertisers[i]);
			LinkedList<Reports> reports = reportsList.get(simNum);
			
			/*
			 * TODO
			 * make our name display properly
			 */
			XYSeries series = new XYSeries(advertisers[i]);
			double totProfit = 0.0;
			for(int day = 0; day < reports.size(); day++) {
				Reports report = reports.get(day);
				SalesReport salesReport = report.getSalesReport();
				QueryReport queryReport = report.getQueryReport();
				double totRevenue = 0.0;
				double totCost = 0.0;
				for(Query query : _simulator.getQuerySpace()) {
					totRevenue += salesReport.getRevenue(query);
					totCost += queryReport.getCost(query);
				}
				totProfit = totProfit + totRevenue - totCost;
				series.add(day, totProfit);
			}
			xySeriesCollection.addSeries(series);
		}
		XYDataset xyDataset = xySeriesCollection;
		JFreeChart chart = ChartFactory.createXYLineChart
		                     ("Simulation " + (simNum+1),  // Title
		                      "Day",           // X-Axis label
		                      "Profit",           // Y-Axis label
		                      xyDataset,          // Dataset
		                      PlotOrientation.VERTICAL,
		                      true,
		                      true,
		                      true);
		return chart;
	}
	
	class RechooseFileButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_simulatorGUI.resetFile();
		}
	}
	
	class ReChartButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		    String simSelection = (String)_numSimsList.getSelectedItem();
		    int sim = 0;
		    if(simSelection == "Average Simulation") {
		    	//TODO
		    }
		    else if (simSelection == "Specific Simulation") {
		    		sim = simSlider.getValue() - 1;
		    }
			JFreeChart chart = fullSimProfitsChart(_allReportsLists,sim);
			BufferedImage image = chart.createBufferedImage((_prefSize.width*4)/5,(_prefSize.height*4)/5);
			lblChart.setIcon(new ImageIcon(image));
		}
	}

	class NumSimsListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox)e.getSource();
		    String simSelection = (String)cb.getSelectedItem();
		    if(simSelection == "Average Simulation") {
		    	if(simSlider.isEnabled()) {
		    		simSlider.setEnabled(false);
		    	}
		    }
		    else if (simSelection == "Specific Simulation") {
		    	if(!simSlider.isEnabled()) {
		    		simSlider.setEnabled(true);
		    	}
		    }
		}
	}
	
	class SimSliderListener implements ChangeListener {
		
		JLabel _sliderLabel;
		
		public SimSliderListener(JLabel sliderLabel) {
			_sliderLabel = sliderLabel;
		}
		
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider)e.getSource();
			_sliderLabel.setText(""+slider.getValue());
		}
		
	}

}
