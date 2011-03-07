package simulator.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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
            _simulatorGUI.setGameToParse(file);
         }
      }
   }

}