package simulator.GUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;

public class SetupPanel2 extends JPanel {

	private SimulatorGUI _simulatorGUI;
	private File _file;

	SetupPanel2(SimulatorGUI simulatorGUI, File file) {
		super();
		BoxLayout layout = new BoxLayout(this,BoxLayout.PAGE_AXIS);
		this.setLayout(layout);
		
		_simulatorGUI = simulatorGUI;
		_file = file;
		
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
			String filename = _file.getAbsolutePath();
			GameStatusHandler statusHandler;
			try {
				statusHandler = new GameStatusHandler(filename);
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new RuntimeException("IOException upon opening game log: " + e1);
			} catch (ParseException e1) {
				e1.printStackTrace();
				throw new RuntimeException("ParseException upon opening game log: " + e1);
			}
			GameStatus status = statusHandler.getGameStatus();
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

