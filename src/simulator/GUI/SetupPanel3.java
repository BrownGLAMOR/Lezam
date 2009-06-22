package simulator.GUI;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

import simulator.BasicSimulator;
import simulator.parser.GameStatus;

public class SetupPanel3 extends JPanel {

	private SimulatorGUI _simulatorGUI;
	private JList _agentList;
	private JComboBox _agentList2;
	private JFormattedTextField _numSims;
	private GameStatus _gameStatus;
	private BasicSimulator _simulator;

	public SetupPanel3(SimulatorGUI simulatorGUI, GameStatus gameStatus) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		
		_simulatorGUI = simulatorGUI;
		_gameStatus = gameStatus;
		
		_simulator = new BasicSimulator();
		
		JPanel panel1 = new JPanel(new FlowLayout());
		JLabel label1 = new JLabel("Which agent do you want to simulate: ");
		panel1.add(label1);
		String[] agentStrings = _simulator.getUsableAgents();
		_agentList = new JList(agentStrings);
		_agentList.setVisibleRowCount(-1);
		panel1.add(_agentList);

		
		JPanel panel2 = new JPanel(new FlowLayout());
		JLabel label2 = new JLabel("Which agent do you want to remove: ");
		panel2.add(label2);
		String[] agentStrings2 = _gameStatus.getAdvertisers();
		_agentList2 = new JComboBox(agentStrings2);
		_agentList2.setSelectedIndex(0);
		panel2.add(_agentList2);

		
		JPanel panel3 = new JPanel(new FlowLayout());
		JLabel label3 = new JLabel("How many simulations would you like to run: ");
		panel3.add(label3);
		_numSims = new JFormattedTextField(new NumberFormatter());
		_numSims.setValue(20);
		panel3.add(_numSims);
		
		JPanel panel4 = new JPanel(new FlowLayout());
		JLabel label4 = new JLabel("This may take a few minutes... (I clocked it at roughly 20 seconds per simulation)");
		panel4.add(label4);
		JButton simulateButton = new JButton("Simulate");
		simulateButton.addActionListener(new SimButtonListener());
		panel4.add(simulateButton);
		
		this.add(panel1);
		this.add(panel2);
		this.add(panel3);
		this.add(panel4);
	}
	
	class SimButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			int[] selectedIndices = _agentList.getSelectedIndices();
			String[] agentsIn = new String[selectedIndices.length];
			for(int i = 0; i < selectedIndices.length; i++) {
				agentsIn[i] = (String) _agentList.getModel().getElementAt(selectedIndices[i]);
			}
			String agentOut = (String) _agentList2.getSelectedItem();
			int numSims = Integer.parseInt(_numSims.getText());
			try {
				_simulatorGUI.setNumSims(_simulator,agentsIn, agentOut, numSims);
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Encountered an error parsing the logs: " + e1);
			} catch (ParseException e1) {
				e1.printStackTrace();
				throw new RuntimeException("Encountered an error parsing the logs: " + e1);
			}
		}
	}
}
