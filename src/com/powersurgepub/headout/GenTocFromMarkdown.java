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

  import com.powersurgepub.psutils2.env.*;
  import com.powersurgepub.psutils2.mkdown.*;
  import com.powersurgepub.psutils2.textio.*;
  import com.powersurgepub.psutils2.txbio.*;
  import com.powersurgepub.psutils2.ui.*;

  import javafx.scene.control.*;
  import javafx.scene.layout.*;

/**
 Generate a Table of Contents from Markdown source. 

 @author Herb Bowie
 */
public class GenTocFromMarkdown 
     implements 
        HeadOutTransformer,
        MarkdownLineReader {
  
  private     static  final String HEADING_LEVEL_START = "heading-level-start";
  private     static  final String HEADING_LEVEL_END   = "heading-level-end";
  
  private             UserPrefs           prefs;
  
  private             int                 transformTypeIndex = 0;
  private             String              transformTypeString = "";

  private             FXUtils             fxUtils;

  private             GridPane            grid;
  
  private             Menu                fileMenu;
  private             Button              genTOCButton;
  private             Label               headingLevelEndLabel;
  private             Slider              headingLevelEndSlider;
  private             Label               headingLevelStartLabel;
  private             Slider              headingLevelStartSlider;
  private             MenuBar             menuBar;
  private             Label               messageLabel;
  private             ComboBox            outputFormatComboBox;
  private             Label               outputFormatLabel;
  
  private             TextLineReader      reader;
  private             TextLineWriter      lineWriter;
  
  private             int                 startHeadingLevel = 2;
  private             int                 endHeadingLevel = 4;
  
  public GenTocFromMarkdown (
      int transformTypeIndex, 
      String transformTypeString) {

    this.transformTypeIndex = transformTypeIndex;
    this.transformTypeString = transformTypeString;
    
    prefs = UserPrefs.getShared();

    fxUtils = FXUtils.getShared();
    grid = new GridPane();
    fxUtils.applyStyle(grid);
    
    headingLevelStartLabel = new Label();
    headingLevelStartLabel.setText("Lowest Heading Level");
    grid.add(headingLevelStartLabel, 0, 0, 1, 1);

    headingLevelStartSlider = new Slider(1, 6, 1);
    headingLevelStartSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_START, 1));
    headingLevelStartSlider.setSnapToTicks(true);
    headingLevelStartSlider.setShowTickLabels(true);
    headingLevelStartSlider.setShowTickMarks(true);
    headingLevelStartSlider.setMajorTickUnit(1.0);
    headingLevelStartSlider.setMinorTickCount(0);
    grid.add(headingLevelStartSlider, 0, 1, 1, 1);
    GridPane.setHgrow(headingLevelStartSlider, Priority.ALWAYS);
    
    headingLevelEndLabel = new Label();
    headingLevelEndLabel.setText("Highest Heading Level");
    grid.add(headingLevelEndLabel, 0, 2, 1, 1);

    headingLevelEndSlider = new Slider(1, 6, 6);
    headingLevelEndSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_END, 6));
    headingLevelEndSlider.setSnapToTicks(true);
    headingLevelEndSlider.setShowTickLabels(true);
    headingLevelEndSlider.setShowTickMarks(true);
    headingLevelEndSlider.setMajorTickUnit(1.0);
    headingLevelEndSlider.setMinorTickCount(0);
    grid.add(headingLevelEndSlider, 0, 3, 1, 1);
    GridPane.setHgrow(headingLevelEndSlider, Priority.ALWAYS);
    
  }

  /**
   Get the GridPane containing the controls for this transformer.

   @return the grid pane containing the controls for this type of transformation.
   */
  public GridPane getGrid() {
    return grid;
  }
  
  /**
   Generate a Table of Contents from the headings found in the Markdown source. 
  
   @param reader The line reader to be used to access the input.
   @param lineWriter The line writer to be used to create the output. 
   @throws TransformException If an error occurs. 
  */
  public void transformNow(TextLineReader reader, TextLineWriter lineWriter) 
      throws TransformException {

    Double startHeadingLevelDouble = new Double(headingLevelStartSlider.getValue());
    startHeadingLevel = startHeadingLevelDouble.intValue();
    Double endHeadingLevelDouble = new Double(headingLevelEndSlider.getValue());
    endHeadingLevel = endHeadingLevelDouble.intValue();
    
    if (transformTypeString.contains("Add ToC to Markdown")) {
      AddToCtoMarkdown addToC = new AddToCtoMarkdown();
      addToC.transformNow(reader, lineWriter, startHeadingLevel, endHeadingLevel);
    } else {
      genToC (reader, lineWriter);
    }
    
    savePrefs();
  }
  
  private void genToC (TextLineReader reader, TextLineWriter lineWriter)
      throws TransformException {
    
    this.reader = reader;
    this.lineWriter = lineWriter;
    reader.open();
    MarkdownInitialParser mdParser = new MarkdownInitialParser (this);
    
    // Open Output File
    int markupFormat = MarkupWriter.MARKDOWN_FORMAT;
    if (transformTypeString.contains("Create HTML")) {
      markupFormat = MarkupWriter.HTML_FRAGMENT_FORMAT;
    }
    MarkupWriter writer = new MarkupWriter(lineWriter, markupFormat);
    writer.setIndenting(true);
    writer.setIndentPerLevel(2);
    writer.openForOutput();
    
    
    int firstHeadingLevel = 0;
    
    boolean listItemOpen[] = new boolean[7];
    for (int i = 0; i < 7; i++) {
      listItemOpen[i] = false;
    }
    
    if (markupFormat == MarkupWriter.HTML_FRAGMENT_FORMAT) {
      writer.startDiv("", "toc");
      writer.startUnorderedList("");
    }
    
    int lastHeadingLevel = 1;

    MarkdownLine mdLine = mdParser.getNextLine();
    while (mdLine != null) {    
      if (mdLine.getHeadingLevel() > 0
        && mdLine.getHeadingLevel() >= startHeadingLevel 
        && mdLine.getHeadingLevel() <= endHeadingLevel) {
        if (mdLine.getID().length() > 0
            && (! mdLine.getID().equals("tableofcontents"))
            && (! mdLine.getID().equals("contents"))) {
          if (firstHeadingLevel < 1) {
            firstHeadingLevel = mdLine.getHeadingLevel();
            lastHeadingLevel = mdLine.getHeadingLevel();
          }

          String link = "#" + mdLine.getID();
          String text = mdLine.getLineContent();
          if (markupFormat == MarkupWriter.HTML_FRAGMENT_FORMAT) {
            // Write HTML
            if (mdLine.getHeadingLevel() > lastHeadingLevel) {
              writer.startUnorderedList("");
            } else {
              if (mdLine.getHeadingLevel() < lastHeadingLevel) {
                int l = lastHeadingLevel;
                while (l > mdLine.getHeadingLevel()) {
                  if (listItemOpen[l]) {
                    writer.endListItem();
                    writer.endUnorderedList();
                    listItemOpen[l] = false;
                  }
                  l--;
                } // end while higher (more deeply indented) lists still open
              } else {
                // No change in heading level
                if (listItemOpen[mdLine.getHeadingLevel()]) {
                  writer.endListItem();
                  listItemOpen[mdLine.getHeadingLevel()] = false;
                }
              }
            } // end if new heading level less than or equal to last

            if (listItemOpen[mdLine.getHeadingLevel()]) {
              writer.endListItem();
              listItemOpen[mdLine.getHeadingLevel()] = false;
            }
            writer.startListItem("");
            writer.startLink(link);
            writer.write(text);
            writer.endLink(link);
            listItemOpen[mdLine.getHeadingLevel()] = true;
          } else {
            // Write Markdown
            StringBuilder tocLine = new StringBuilder();
            int h = firstHeadingLevel;
            while (h < mdLine.getHeadingLevel()) {
              tocLine.append("    ");
              h++;
            }
            tocLine.append("* ");
            tocLine.append("[");
            tocLine.append(text);
            tocLine.append("](");
            tocLine.append(link);
            tocLine.append(")");
            writer.writeLine(tocLine.toString());
          } // end if markdown format
          lastHeadingLevel = mdLine.getHeadingLevel();

        } // end if we have a heading string
      } // end if we have a heading identifier
      
      mdLine = mdParser.getNextLine();
    } // end while more markdown lines to process
    
    if (markupFormat == MarkupWriter.HTML_FRAGMENT_FORMAT) {
      int l = lastHeadingLevel;
      while (l >= firstHeadingLevel) {
        if (listItemOpen[l]) {
          writer.endListItem();
          writer.endUnorderedList();
          listItemOpen[l] = false;
        }
        l--;
      } // end while higher (more deeply indented) lists still open
      writer.endDiv();
    }
    
    reader.close();
    writer.close();
    
  }
  
  /**
   Obtains the next line of raw markdown source. 
  
   @return The next markdown input line, or null when no more input is available.
   */
  public String getMarkdownInputLine() {
    if (reader == null
        || reader.isAtEnd()
        || (! reader.isOK())) {
      return null;
    } else {
      return reader.readLine();
    }
  }
  
  /**
   Save user options as preferences. 
  */
  public void savePrefs() {

    prefs.setPref(HEADING_LEVEL_START, headingLevelStartSlider.getValue());
    prefs.setPref(HEADING_LEVEL_END, headingLevelEndSlider.getValue());

  }

}
