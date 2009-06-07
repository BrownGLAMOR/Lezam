package simulator.GUI;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

public class SetupPanel extends JPanel {

	private JFileChooser fc;
	private SimulatorGUI _simulatorGUI;

	SetupPanel(SimulatorGUI simulatorGUI) {
		super(new FlowLayout());
		_simulatorGUI = simulatorGUI;
		fc = new JFileChooser();
		JButton gameLogButton = new JButton("Choose Log To Work From");
		gameLogButton.addActionListener(new LogButtonListener());
		this.add(gameLogButton);
	}


	class LogButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int returnVal = fc.showOpenDialog((Component) e.getSource());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				System.out.println("Selected File: " + file.getAbsolutePath());
				_simulatorGUI.setGameToParse(file);
			}
		}
	}

}