package simulator.GUI;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

	public MainPanel(SimulatorGUI simulatorGUI, GameStatus status, String agentIn, String agentOut, int numSims, Dimension prefSize) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		_simulatorGUI = simulatorGUI;
		_gameStatus = status;
		_agentIn = agentIn;
		_agentOut = agentOut;
		_numSims = numSims;
		_prefSize = prefSize;
		
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
		
		JPanel newParsePanel = new JPanel(new FlowLayout());
		JButton newParseButton = new JButton("Select a new file to parse");
		newParseButton.addActionListener(new RechooseFileButtonListener());
		newParsePanel.add(newParseButton);
		
		JPanel tabbedPanePanel = new JPanel(new FlowLayout());
		tabbedPanePanel.setSize(prefSize);
		tabbedPanePanel.setPreferredSize(prefSize);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setSize(prefSize);
		tabbedPane.setPreferredSize(prefSize);
		
		JPanel pane1 = new JPanel(new FlowLayout());
		JLabel pane1Label1 = new JLabel("Pane 1");
		pane1.add(pane1Label1);
		pane1.setSize(prefSize);
		pane1.setPreferredSize(prefSize);
		tabbedPane.add("One",pane1);
		
		JPanel pane2 = new JPanel(new FlowLayout());
		JLabel pane2Label1 = new JLabel("Pane 2");
		pane2.add(pane2Label1);
		pane2.setSize(prefSize);
		pane2.setPreferredSize(prefSize);
		tabbedPane.add("Two",pane2);
		
		JPanel pane3 = new JPanel(new FlowLayout());
		JLabel pane3Label1 = new JLabel("Pane 3");
		pane3.add(pane3Label1);
		pane3.setSize(prefSize);
		pane2.setPreferredSize(prefSize);
		tabbedPane.add("Three",pane3);
		
		tabbedPanePanel.add(tabbedPane);
		
		this.add(simsPanel);
		this.add(newParsePanel);
		this.add(tabbedPanePanel);
	}
	
	class RechooseFileButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			/*
			 * Parse game log here
			 */
			_simulatorGUI.resetFile();
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
