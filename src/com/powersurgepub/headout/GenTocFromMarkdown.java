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

  import com.powersurgepub.psdatalib.txbio.*;
  import com.powersurgepub.psdatalib.ui.*;
  import com.powersurgepub.psmkdown.*;
  import com.powersurgepub.pstextio.*;
  import com.powersurgepub.psutils.*;
  import javax.swing.*;

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
  
  private             JFrame              frame = null;
  
  private             JPanel              panel = null;
  
  private             int                 transformTypeIndex = 0;
  private             String              transformTypeString = "";
  
  private             GridBagger          gb = new GridBagger();
  
  private javax.swing.JMenu fileMenu;
  private javax.swing.JButton genTOCButton;
  private javax.swing.JLabel headingLevelEndLabel;
  private javax.swing.JSlider headingLevelEndSlider;
  private javax.swing.JLabel headingLevelStartLabel;
  private javax.swing.JSlider headingLevelStartSlider;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JLabel messageLabel;
  private javax.swing.JComboBox outputFormatComboBox;
  private javax.swing.ButtonGroup outputFormatGroup;
  private javax.swing.JLabel outputFormatLabel;
  
  private             TextLineReader      reader;
  private             TextLineWriter      lineWriter;
  
  private             int startHeadingLevel = 2;
  private             int endHeadingLevel = 4;
  
  public GenTocFromMarkdown (
      JFrame frame, 
      int transformTypeIndex, 
      String transformTypeString,
      JPanel panel) {
    
    this.frame = frame;
    this.panel = panel;
    this.transformTypeIndex = transformTypeIndex;
    this.transformTypeString = transformTypeString;
    
    prefs = UserPrefs.getShared();
    
    gb.startLayout(panel, 1, 4);
    
		gb.setByRows (false);
		gb.setAllInsets (4);
		gb.setDefaultRowWeight (0.0);
    
    headingLevelStartLabel = new javax.swing.JLabel();
    headingLevelStartLabel.setText("Lowest Heading Level");
    gb.add(headingLevelStartLabel);

    headingLevelStartSlider = new javax.swing.JSlider();
    headingLevelStartSlider.setMajorTickSpacing(1);
    headingLevelStartSlider.setMaximum(6);
    headingLevelStartSlider.setMinimum(1);
    headingLevelStartSlider.setPaintLabels(true);
    headingLevelStartSlider.setPaintTicks(true);
    headingLevelStartSlider.setValue(1);
    headingLevelStartSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_START, 1));
    gb.add(headingLevelStartSlider);
    
    headingLevelEndLabel = new javax.swing.JLabel();
    headingLevelEndLabel.setText("Highest Heading Level");
    gb.add(headingLevelEndLabel);

    headingLevelEndSlider = new javax.swing.JSlider();
    headingLevelEndSlider.setMajorTickSpacing(1);
    headingLevelEndSlider.setMaximum(6);
    headingLevelEndSlider.setMinimum(1);
    headingLevelEndSlider.setPaintLabels(true);
    headingLevelEndSlider.setPaintTicks(true);
    headingLevelEndSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_END, 6));
    gb.add(headingLevelEndSlider);
    
  }
  
  /**
   Generate a Table of Contents from the headings found in the Markdown source. 
  
   @param reader The line reader to be used to access the input.
   @param lineWriter The line writer to be used to create the output. 
   @throws TransformException If an error occurs. 
  */
  public void transformNow(TextLineReader reader, TextLineWriter lineWriter) 
      throws TransformException {
    
    startHeadingLevel = headingLevelStartSlider.getValue();
    endHeadingLevel = headingLevelEndSlider.getValue();
    
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
