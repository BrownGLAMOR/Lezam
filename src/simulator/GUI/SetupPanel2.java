package simulator.GUI;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import simulator.GUI.SetupPanel.LogButtonListener;
import simulator.parser.GameStatus;

public class SetupPanel2 extends JPanel {

	private SimulatorGUI _simulatorGUI;

	SetupPanel2(SimulatorGUI simulatorGUI, File file) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		_simulatorGUI = simulatorGUI;
		JLabel label = new JLabel("Selected " + file.getName() + " to parse.\n This may take a few seconds");
		label.setAlignmentX(CENTER_ALIGNMENT);
		this.add(label);
		JButton gameLogButton = new JButton("Parse");
		gameLogButton.addActionListener(new ParseButtonListener());
		gameLogButton.setAlignmentX(CENTER_ALIGNMENT);
		this.add(gameLogButton);
		JButton resetFile = new JButton("Choose file again");
		resetFile.addActionListener(new RechooseFileButtonListener());
		resetFile.setAlignmentX(CENTER_ALIGNMENT);
		this.add(resetFile);
	}

	class ParseButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			/*
			 * Parse game log here
			 */
			GameStatus status = null;
			_simulatorGUI.setGameStatus(status);
		}
	}
	
	class RechooseFileButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			/*
			 * Parse game log here
			 */
			_simulatorGUI.resetFile();
		}
	}
}

