/*
 * Copyright 2014 - 2018 by Herb Bowie
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

  import com.powersurgepub.psutils2.basic.*;
  import com.powersurgepub.psutils2.env.*;
  import com.powersurgepub.psutils2.logging.*;
  import com.powersurgepub.psutils2.textio.*;
  import com.powersurgepub.psutils2.txbio.*;
  import com.powersurgepub.psutils2.ui.*;

  import javafx.scene.control.*;
  import javafx.scene.layout.*;

  import java.io.*;

  import org.xml.sax.*;
  import org.xml.sax.helpers.*;

/**
 Generate a Markdown document from OPML source. 

 @author Herb Bowie
 */
public class OPMLtoMarkdown 
      extends DefaultHandler 
      implements 
        HeadOutTransformer {
  
  private     static  final String OUTLINE = "outline";
  private     static  final String TEXT = "text";
  private     static  final String NOTE = "_note";
  
  private     static  final String HEADING_LEVEL_START = "opml-heading-level-start";
  private     static  final String HEADING_LEVEL_END   = "opml-heading-level-end";
  private     static  final String INPUT_FILE          = "opml-input-file";
  private     static  final String OUTPUT_FILE         = "opml-output-file";
  
  private             UserPrefs           prefs;
  
  private             FXUtils             fxUtils;
  private             GridPane            grid;
  
  private             int                 transformTypeIndex = 0;
  private             String              transformTypeString = "";
  
  private             Menu fileMenu;
  private             Button genTOCButton;
  private             Label headingLevelEndLabel;
  private             Slider headingLevelEndSlider;
  private             Label headingLevelStartLabel;
  private             Slider headingLevelStartSlider;
  private             MenuBar menuBar;
  private             Label messageLabel;
  private             ComboBox outputFormatComboBox;
  private             javax.swing.ButtonGroup outputFormatGroup;
  private             Label outputFormatLabel;
  
  private             TextLineReader      reader;
  private             TextLineWriter      interimLineWriter;
  private             TextLineReader      interimLineReader;
  private             TextLineWriter      finalLineWriter;
  
  private             XMLReader           parser;
  private             MarkupWriter        mdWriter = null;
  
  private             boolean             ok = true;
  
  private             String              message = "";
  
  private             File                xmlSourceAsFile;
  
  private             int                 startHeadingLevel = 1;
  private             int                 endHeadingLevel = 6;
  
  private             int                 firstHeadingLevel = 0;
  private             int                 lastHeadingLevel = 1;
  
  private             int                 headingLevel = 0;
  private             int                 listLevel = -1;
  private             int                 indents = 0;
  
  /**
   Construct a new OPML to MD transformer. 
   
   @param transformTypeIndex WHich transformation was requested?
   @param transformTypeString What did we call it? 
  */
  public OPMLtoMarkdown (
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
    headingLevelStartSlider.setSnapToTicks(true);
    headingLevelStartSlider.setShowTickLabels(true);
    headingLevelStartSlider.setShowTickMarks(true);
    headingLevelStartSlider.setMajorTickUnit(1.0);
    headingLevelStartSlider.setMinorTickCount(0);
    headingLevelStartSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_START, 1));
    grid.add(headingLevelStartSlider, 0, 1, 1, 1);
    GridPane.setHgrow(headingLevelStartSlider, Priority.ALWAYS);
    
    headingLevelEndLabel = new Label();
    headingLevelEndLabel.setText("Highest Heading Level");
    grid.add(headingLevelEndLabel, 0, 2, 1, 1);

    headingLevelEndSlider = new Slider(1, 6, 6);
    headingLevelEndSlider.setSnapToTicks(true);
    headingLevelEndSlider.setSnapToTicks(true);
    headingLevelEndSlider.setShowTickLabels(true);
    headingLevelEndSlider.setMajorTickUnit(1.0);
    headingLevelEndSlider.setMinorTickCount(0);
    headingLevelEndSlider.setValue(prefs.getPrefAsInt(HEADING_LEVEL_END, 6));
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
   Reads an outline defined in OPML and generates a Markdown file using 
   headings to represent each outline level. 
  
   @param reader The line reader to be used to access the input.
   @param lineWriter The line mdWriter to be used to create the output. 
   @throws TransformException If an error occurs. 
  */
  public void transformNow(TextLineReader reader, TextLineWriter lineWriter) 
      throws TransformException {
    
    this.reader = reader;
    finalLineWriter = lineWriter;
    interimLineWriter = new StringMaker();
    
    // First transform the OPML input to an interim string containing Markdown
    
    ok = true;
    message = "";
    xmlSourceAsFile = new File (reader.toString());

    Double startHeadingLevelDouble = new Double(headingLevelStartSlider.getValue());
    startHeadingLevel = startHeadingLevelDouble.intValue();
    Double endHeadingLevelDouble = new Double(headingLevelEndSlider.getValue());
    endHeadingLevel = endHeadingLevelDouble.intValue();
    
    // Open Output File
    int markupFormat = MarkupWriter.MARKDOWN_FORMAT;
    mdWriter = new MarkupWriter(interimLineWriter, markupFormat);
    mdWriter.setIndenting(true);
    mdWriter.setIndentPerLevel(4);
    mdWriter.openForOutput();
    
    // Set up XML Parser to read the OPML input
    try {
      parser = XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      Logger.getShared().recordEvent (LogEvent.MINOR, 
          "Generic SAX Parser Not Found",
          false);
      try {
        parser = XMLReaderFactory.createXMLReader
            ("org.apache.xerces.parsers.SAXParser");
      } catch (SAXException eex) {
        Logger.getShared().recordEvent (LogEvent.MEDIUM, 
            "Xerces SAX Parser Not Found",
            false);
        ok = false;
        message = "SAX Parser Not Found";
      }
    }
    if (ok) {
      parser.setContentHandler (this);
      if (! xmlSourceAsFile.exists()) {
        ok = false;
        Logger.getShared().recordEvent (LogEvent.MEDIUM, 
            "XML File or Directory " + reader.toString() + " cannot be found",
            false);
        message = "Input file not found";
      }
    }
    if (ok
        && ! xmlSourceAsFile.isFile()) {
      ok = false;
      Logger.getShared().recordEvent (LogEvent.MEDIUM, 
            "XML File or Directory " + reader.toString() + " is not a file",
            false); 
        message = "Input source is not a file";
    }
    if (ok) {
      if (! xmlSourceAsFile.canRead()) {
        ok = false;
        Logger.getShared().recordEvent (LogEvent.MEDIUM, 
            "XML File or Directory " + reader.toString() + " cannot be read",
            false); 
        message = "Input file cannot be read";
      }
    }
    
    if (ok) {
      parseOPML();
    }
    
    if (! ok) {
      throw new TransformException(message);
    }
    
    mdWriter.close();
    
    // Now let's add a Table of Contents to the Markdown file
    interimLineReader = new StringLineReader(interimLineWriter.toString());
    interimLineWriter = null;
    AddToCtoMarkdown addToC = new AddToCtoMarkdown();
    addToC.transformNow(interimLineReader, finalLineWriter, 
        startHeadingLevel, endHeadingLevel);
    
    // Let's save our user's preferences before exiting
    savePrefs();
  }
  
  private void parseOPML() {
    
    headingLevel = 0;
    listLevel = -1;
    indents = 0;
    
    try {
      parser.parse (xmlSourceAsFile.toURI().toString());
    } 
    catch (SAXException saxe) {
      Logger.getShared().recordEvent (LogEvent.MEDIUM, 
          "Encountered SAX error while reading XML file " + reader.toString() 
          + saxe.toString(),
          false);  
      ok = false;
      message = "SAX error while reading OPML file";
    } 
    catch (java.io.IOException ioe) {
      Logger.getShared().recordEvent (LogEvent.MEDIUM, 
          "Encountered I/O error while reading XML file " + reader.toString() 
          + ioe.toString(),
          false);   
      ok = false;
      message = "I/O Error reading OPML file";
    }
      
  }
  
  /**
   This is where all the content is processed. 
  
   @param namespaceURI
   @param localName This is the name identifier we will use. 
   @param qualifiedName
   @param attributes Where all the content resides. 
  */
  public void startElement (
      String namespaceURI,
      String localName,
      String qualifiedName,
      Attributes attributes) {

    if (localName.equalsIgnoreCase(OUTLINE)) {
      headingLevel++;
      for (int i = 0; i < attributes.getLength(); i++) {
        String name = attributes.getLocalName (i);
        String value = attributes.getValue (i);
        if (name.equalsIgnoreCase(TEXT)) {
          if (headingLevel <= endHeadingLevel) {
            endOpenLists();
            mdWriter.writeHeading(headingLevel, value, "");
          } else {
            if (listLevel < endHeadingLevel) {
              listLevel = endHeadingLevel;
            }
            endOpenLists();
            while (listLevel < headingLevel) {
              mdWriter.startUnorderedList("");
              listLevel++;
            }
            mdWriter.startListItem("");
            mdWriter.write("* ");
            mdWriter.write(value);
            mdWriter.endListItem();
            moreIndent();
          } // End of content generation condition
        }
        else
        if (name.equalsIgnoreCase(NOTE)) {
          int j = 0;
          int k = 0;
          int l = 0;
          while (j < value.length()) {
            int lfs = 0;
            k = value.indexOf(GlobalConstants.LINE_FEED, j);
            if (k < 0) {
              k = value.length();
            }
            l = k;
            while (l < value.length()
                && (value.charAt(l) == GlobalConstants.LINE_FEED
                  || value.charAt(l) == GlobalConstants.CARRIAGE_RETURN)) {
              if (value.charAt(l) == GlobalConstants.LINE_FEED) {
                lfs++;
              }
              l++;
            }
            mdWriter.writeLine(value.substring(j, k));
            if (lfs > 1) {
              mdWriter.newLine();
            }
            j = l;
          }
        } // end if it was a note
      } // End for each attribute of the Outline element
    } // End if it is an Outline element
  } // end method
  
  public void endElement (
      String namespaceURI,
      String localName,
      String qualifiedName) {
    
    if (localName.equalsIgnoreCase(OUTLINE)) {
      headingLevel--;
    }
    endOpenLists();
  } // end method
  
  private void endOpenLists() {
    while (listLevel > headingLevel && listLevel > endHeadingLevel) {
      mdWriter.endUnorderedList();
      listLevel--;
    }
    adjustIndent();
  }
  
  private void moreIndent() {
    mdWriter.moreIndent();
    indents++;
  }
  
  private void adjustIndent() {
    while (indents > (listLevel - endHeadingLevel)
        && indents > 0) {
      mdWriter.lessIndent();
      indents--;
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
