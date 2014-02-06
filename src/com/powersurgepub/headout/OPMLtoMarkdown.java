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
  
  private             XMLReader           parser;
  private             MarkupWriter        writer = null;
  
  private             int                 headingLevel = 0;
  
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
   @param lineWriter The line writer to be used to create the output. 
   @throws TransformException If an error occurs. 
  */
  public void transformNow(TextLineReader reader, TextLineWriter lineWriter) 
      throws TransformException {
    
    this.reader = reader;
    this.lineWriter = lineWriter;
    
    boolean ok = true;
    String message = "";
    File xmlSourceAsFile = new File (reader.toString());
    
    // Open Output File
    int markupFormat = MarkupWriter.MARKDOWN_FORMAT;
    writer = new MarkupWriter(lineWriter, markupFormat);
    writer.setIndenting(true);
    writer.setIndentPerLevel(2);
    writer.openForOutput();
    
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
    
    if (! ok) {
      throw new TransformException(message);
    }

    writer.close();
    
    savePrefs();
  }
  
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
          writer.writeHeading(headingLevel, value, "");
        }
        else
        if (name.equalsIgnoreCase(NOTE)) {
          writer.writeLine(value);
        }
      }
    }
  } // end method
  
  public void endElement (
      String namespaceURI,
      String localName,
      String qualifiedName) {
    
    if (localName.equalsIgnoreCase(OUTLINE)) {
      headingLevel--;
    }
  } // end method
  
  /**
   Save user options as preferences. 
  */
  public void savePrefs() {

    prefs.setPref(HEADING_LEVEL_START, headingLevelStartSlider.getValue());
    prefs.setPref(HEADING_LEVEL_END, headingLevelEndSlider.getValue());

  }

}
