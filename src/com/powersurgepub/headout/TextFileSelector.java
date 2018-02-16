/*
 * Copyright 2014 - 2018 Herb Bowie
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

  import com.powersurgepub.psutils2.textio.*;
  import com.powersurgepub.psutils2.ui.*;

  import javafx.collections.*;
  import javafx.scene.control.*;
  import javafx.scene.layout.*;
  import javafx.stage.*;

  import java.io.*;


/**
 A class that facilitates the selection and use of a text file for either
 input or output. 

 @author Herb Bowie
 */
public class TextFileSelector {
  
  private             int inOrOut             = 0;
  public static final int   INPUT             = 0;
  public static final int   OUTPUT            = 1;

  private             Window                  mainWindow;
  
  private             FXUtils                 fxUtils;
  private             GridPane                grid;
  
  private             Label                   ioLabel;

  private             ObservableList<String>  textTypeLabels;
  private             ComboBox                textTypeComboBox;
  
  private             TextArea                fileNameText;
  
  private             File                    file = null;
  
  private             TextLineReader          reader = null;
  
  private             TextLineWriter          writer = null;

  /**
   Create a text file selector.

   @param inOrOut Either for input or output.
   */
  public TextFileSelector (Window mainWindow, int inOrOut) {
    this.mainWindow = mainWindow;
    this.inOrOut = inOrOut;
    commonConstruction();
  }

  /**
   Build the appropriate UI.
   */
  private void commonConstruction() {

    grid = new GridPane();
    fxUtils = FXUtils.getShared();
    fxUtils.applyStyle(grid);

    if (inOrOut == INPUT) {
      ioLabel = new Label ("Specify Source for Input Text");
    } else {
      ioLabel = new Label ("Specify Target for Output Text");
    }
    grid.add(ioLabel, 0, 0, 1, 1);

    textTypeLabels = FXCollections.<String>observableArrayList("System Clipboard", "Local File...");
    textTypeComboBox = new ComboBox<>(textTypeLabels);
    textTypeComboBox.getSelectionModel().select(0);
    grid.add(textTypeComboBox, 0, 1, 1, 1);
    textTypeComboBox.setEditable (false);
    textTypeComboBox.setOnAction(e -> textTypeSelected());
    
    fileNameText = new TextArea("                    ");
    fileNameText.setEditable(false);
    fileNameText.setWrapText(true);
    fileNameText.setPrefRowCount(40);
    fileNameText.setPrefColumnCount(20);
    grid.add(fileNameText, 0, 2, 1, 1);
    GridPane.setHgrow(fileNameText, Priority.ALWAYS);
    GridPane.setVgrow(fileNameText, Priority.ALWAYS);
  }
  
  /**
   Return the pane to be used as a UI component to allow the user
   to specify the text source or destination. 
  
   @return The pane to be used as a UI component.
  */
  public GridPane getGrid() {
    return grid;
  }

  private void textTypeSelected() {
    reader = null;
    writer = null;
    String inType = (String)textTypeComboBox.getSelectionModel().getSelectedItem();
    file = null;
    if (textTypeComboBox.getSelectionModel().getSelectedIndex() == 1) {
      FileChooser fileChooser = new FileChooser();
      if (inOrOut == INPUT) {
        fileChooser.setTitle("Select Input File");
        file = fileChooser.showOpenDialog(mainWindow);
      } else {
        fileChooser.setTitle("Specify Output File");
        file = fileChooser.showSaveDialog(mainWindow);
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
