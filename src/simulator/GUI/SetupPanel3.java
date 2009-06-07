package simulator.GUI;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

import simulator.parser.GameStatus;

public class SetupPanel3 extends JPanel {

	private SimulatorGUI _simulatorGUI;
	private JComboBox _agentList;
	private JComboBox _agentList2;
	private JFormattedTextField _numSims;
	private GameStatus _gameStatus;

	public SetupPanel3(SimulatorGUI simulatorGUI, GameStatus gameStatus) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		
		_simulatorGUI = simulatorGUI;
		_gameStatus = gameStatus;
		
		JPanel panel1 = new JPanel(new FlowLayout());
		JLabel label1 = new JLabel("Which agent do you want to simulate: ");
		panel1.add(label1);
		String[] agentStrings = { "MCKP", "Cheap" };
		_agentList = new JComboBox(agentStrings);
		_agentList.setSelectedIndex(0);
		panel1.add(_agentList);

		
		JPanel panel2 = new JPanel(new FlowLayout());
		JLabel label2 = new JLabel("Which agent do you want to remove: ");
		panel2.add(label2);
		/*
		 * Actually parse these from game logs
		 */
		String[] agentStrings2 = { "Dummy", "Dummy1" , "Dummy2" , "Dummy3" , "Dummy4" , "Dummy5" , "Dummy6" , "Dummy7" };
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
//		BoxLayout layout2 =  new BoxLayout(panel4,BoxLayout.PAGE_AXIS);
//		panel4.setLayout(layout2);
		JLabel label4 = new JLabel("This may take a few minutes...");
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
			String agentIn = (String) _agentList.getSelectedItem();
			String agentOut = (String) _agentList2.getSelectedItem();
			int numSims = (Integer) _numSims.getValue();
			_simulatorGUI.setNumSims(agentIn, agentOut, numSims);
		}
	}
}
