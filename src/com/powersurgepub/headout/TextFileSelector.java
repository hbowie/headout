/*
 * Copyright 2014 Herb Bowie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.powersurgepub.headout;

  import com.powersurgepub.psdatalib.ui.*;
  import com.powersurgepub.pstextio.*;
  import com.powersurgepub.xos2.*;
  import java.awt.event.*;
  import java.awt.*;
  import java.io.*;
  import javax.swing.*;

/**
 A class that facilitates the selection and use of a text file for either
 input or output. 

 @author Herb Bowie
 */
public class TextFileSelector {
  
  private int inOrOut = 0;
  public static final int INPUT = 0;
  public static final int OUTPUT = 1;
  
  private             JFrame frame = null;
  
  private             JPanel panel = null;
  
  private             GridBagger           gb = new GridBagger();
  
  private             JLabel               ioLabel;
  
  private             String[]             textTypeLabels = 
  {"System Clipboard", "Local File..."};
  private             JComboBox            textTypeComboBox;
  
  private             JTextArea            fileNameText;
  
  private             File                 file = null;
  
  private             TextLineReader       reader = null;
  
  private             TextLineWriter       writer = null;
  
  public TextFileSelector (JFrame frame, int inOrOut) {
    this.frame = frame;
    this.inOrOut = inOrOut;
    panel = new JPanel();
    commonConstruction();
  }
  
  public TextFileSelector (JFrame frame, int inOrOut, JPanel panel) {
    this.frame = frame;
    this.inOrOut = inOrOut;
    this.panel = panel;
    commonConstruction();
  }
  
  private void commonConstruction() {
    
    Dimension min = new Dimension (240, 120);
    panel.setMinimumSize(min);
    gb.startLayout(panel, 1, 3);
    
		gb.setByRows (false);
		gb.setAllInsets (4);
		gb.setDefaultRowWeight (0.0);
		
		// Column 0
    if (inOrOut == INPUT) {
      ioLabel = new JLabel ("Specify Source for Input Text", JLabel.LEFT);
    } else {
      ioLabel = new JLabel ("Specify Target for Output Text", JLabel.LEFT);
    }
    gb.add (ioLabel);
    
    textTypeComboBox = new JComboBox(textTypeLabels);
    gb.add (textTypeComboBox);
    textTypeComboBox.setEditable (false);
		textTypeComboBox.addActionListener (new ActionListener()
		  {
		    public void actionPerformed (ActionEvent event) {
          reader = null;
          writer = null;
		      JComboBox cb = (JComboBox)event.getSource();
		      String inType = (String)cb.getSelectedItem();
          file = null;
          if (cb.getSelectedIndex() == 1) {
            XFileChooser fileChooser = new XFileChooser();
            fileChooser.setFileSelectionMode(XFileChooser.FILES_ONLY);
            if (inOrOut == INPUT) {
              fileChooser.setDialogTitle("Select Input File");
              file = fileChooser.showOpenDialog(frame);
            } else {
              fileChooser.setDialogTitle("Specify Output File");
              file = fileChooser.showSaveDialog(frame);
            }
          } // end if user said he wanted to select a file
          if (file == null) {
            fileNameText.setText(" ");
          } else {
            try {
              fileNameText.setText(file.getCanonicalPath());
            } catch (IOException e) {
              fileNameText.setText(file.getPath());
            }
          } // end if file identified
		    } // end ActionPerformed method
		  } // end action listener for input type combo box
		); 
    
    fileNameText = new JTextArea("                    ");
    
    gb.setFill(GridBagConstraints.BOTH);
    gb.setColumnWeight(1.0);
    gb.setRowWeight(1.0);
    
    gb.add (fileNameText);
  }
  
  /**
   Return the panel to be used as a UI component to allow the user
   to specify the text source or destination. 
  
   @return The panel to be used as a UI component. 
  */
  public JPanel getPanel() {
    return panel;
  }
  
  /**
   Return a line reader, if one is available. 
  
   @return A line reader, or null if none is available. 
  */
  public TextLineReader getReader() {
    if (inOrOut == INPUT) {
      if (reader != null) {
        return reader;
      } else {
        if (file == null) {
          reader = new ClipboardReader();
        } else {
          reader = new FileLineReader(file);
        }
        return reader;
      }
    } else {
      // If we're not looking for input, then no reader
      return null;
    }
  } // end method getReader
  
  /**
   Return a line writer, if one is available. 
  
   @return A line writer, or null if none is available. 
  */
  public TextLineWriter getWriter() {
    if (inOrOut == OUTPUT) {
      if (writer != null) {
        return writer;
      } else {
        if (file == null) {
          writer = new ClipboardMaker();
        } else {
          writer = new FileMaker(file);
        }
        return writer;
      }
    } else {
      // If we're not looking for output, then no writer
      return null;
    }
  }

}
