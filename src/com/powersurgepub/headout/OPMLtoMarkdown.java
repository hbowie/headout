/*
 * Copyright 2014 - 2015 by Herb Bowie
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
  import java.io.*;
  import java.util.*;
  import org.xml.sax.*;
  import org.xml.sax.helpers.*;

/**
 Generate a Table of Contents from Markdown source. 

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
  
  public OPMLtoMarkdown (
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
    
    startHeadingLevel = headingLevelStartSlider.getValue();
    endHeadingLevel = headingLevelEndSlider.getValue();
    
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
