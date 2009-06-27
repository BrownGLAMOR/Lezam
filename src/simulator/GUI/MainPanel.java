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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import simulator.BasicSimulator;
import simulator.Reports;
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
	private ChartUtils _chartingUtils;

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
		
		_chartingUtils = new ChartUtils(_simulator, _agentsIn);

		simsPanel = new JPanel(new FlowLayout());
		String[] numSimsStrings = { "Total Profits", "Daily Profits" , "Daily Impressions" , "Daily Clicks" , "Daily Conversions", "Windowed Conversions"};
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
				chart = _chartingUtils.fullSimProfitsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Profits")) {
				chart = _chartingUtils.dailyProfitsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Impressions")) {
				chart = _chartingUtils.dailyImpsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Clicks")) {
				chart = _chartingUtils.dailyClicksChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Daily Conversions")) {
				chart = _chartingUtils.dailyConvsChart(_reportsListMap,_agentsIn);
			}
			else if (simSelection.equals("Windowed Conversions")) {
				chart = _chartingUtils.dailyWindowChart(_reportsListMap,_agentsIn);
			}
			else {
				throw new RuntimeException("Bad Selection");
			}
			if(_deviation) {
				chart = _chartingUtils.addDeviation(chart);
			}
			BufferedImage image = chart.createBufferedImage((_prefSize.width*4)/5,(_prefSize.height*4)/5);
			lblChart.setIcon(new ImageIcon(image));
		}
	}


}
