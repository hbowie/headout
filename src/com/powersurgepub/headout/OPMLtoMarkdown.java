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
  private             TextLineWriter      lineWriter;
  
  private             XMLReader           parser;
  private             MarkupWriter        writer = null;
  
  private             boolean             ok = true;
  
  private             String              message = "";
  
  private             File                xmlSourceAsFile;
  
  private             int                 pass = 0;
  private             boolean             tocGenerated = false;
  private             boolean             tocInsertionPointFound = false;
  
  private             int                 startHeadingLevel = 1;
  private             int                 endHeadingLevel = 6;
  
  private             int                 firstHeadingLevel = 0;
  private             int                 lastHeadingLevel = 1;
  
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
    
    ok = true;
    message = "";
    xmlSourceAsFile = new File (reader.toString());
    
    startHeadingLevel = headingLevelStartSlider.getValue();
    endHeadingLevel = headingLevelEndSlider.getValue();
    
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
      pass = 0;
      tocGenerated = false;
      // Let's do an initial pass
      parseOPML();
      // If we generated a Table of Contents on the first pass, 
      // then let's do a second pass to finish writing the outline. 
      if (tocGenerated) {
        parseOPML();
      }
    }
    
    if (! ok) {
      throw new TransformException(message);
    }

    writer.close();
    
    savePrefs();
  }
  
  private void parseOPML() {
    
    pass++;
    tocInsertionPointFound = false;
    
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
  
  public void startElement (
      String namespaceURI,
      String localName,
      String qualifiedName,
      Attributes attributes) {

    if (localName.equalsIgnoreCase(OUTLINE)) {
      headingLevel++;
      boolean generatingContent = false;
      for (int i = 0; i < attributes.getLength(); i++) {
        String name = attributes.getLocalName (i);
        String value = attributes.getValue (i);
        if (name.equalsIgnoreCase(TEXT)) {
          
          String id = MarkdownLine.makeID(value, 0, value.length() - 1);
          // System.out.println(" ");
          // System.out.println("Pass # = " + String.valueOf(pass));
          // System.out.println("Heading Level = " + String.valueOf(headingLevel));
          // System.out.println("Heading Text  = " + value);
          // System.out.println("Heading ID    = " + id);
          boolean tocEligibleHeading = 
              (headingLevel >= startHeadingLevel
              && headingLevel <= endHeadingLevel
              && id.length() > 0
              && (! id.equalsIgnoreCase("tableofcontents"))
              && (! id.equalsIgnoreCase("contents")));
          // System.out.println("Eligible for TOC? " + String.valueOf(tocEligibleHeading));
          if (tocEligibleHeading
              && (! tocInsertionPointFound)) {
            tocInsertionPointFound = true;
            // System.out.println("TOC Insertion Point Found!");
          }
          
          if (pass == 1
              && tocInsertionPointFound) {
            tocGenerated = true;
          }
          
          if (pass == 1 && tocGenerated) {
            if (tocEligibleHeading) {
              // System.out.println("Writing out a toc line");
              if (firstHeadingLevel < 1) {
                firstHeadingLevel = headingLevel;
                lastHeadingLevel = headingLevel;
              }
              String link = "#" + id;
              StringBuilder tocLine = new StringBuilder();
              int h = firstHeadingLevel;
              while (h < headingLevel) {
                tocLine.append("    ");
                h++;
              }
              tocLine.append("* ");
              tocLine.append("[");
              tocLine.append(value);
              tocLine.append("](");
              tocLine.append(link);
              tocLine.append(")");
              writer.writeLine(tocLine.toString());
              lastHeadingLevel = headingLevel;
            } else {
              // If first pass and we're generating a table of contents,
              // but this heading isn't eligible, then just skip it for now. 
            }
          } // End of toc generation condition
          
          if ((pass == 1 && (! tocGenerated))
              || (pass == 2 && (tocInsertionPointFound))) {
            writer.writeHeading(headingLevel, value, "");
            generatingContent = true;
            // System.out.println("Writing out a heading line");
          } // End of content generation condition
        }
        else
        if (name.equalsIgnoreCase(NOTE)) {
          if (generatingContent) {
            writer.writeLine(value);
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
  } // end method
  
  /**
   Save user options as preferences. 
  */
  public void savePrefs() {

    prefs.setPref(HEADING_LEVEL_START, headingLevelStartSlider.getValue());
    prefs.setPref(HEADING_LEVEL_END, headingLevelEndSlider.getValue());

  }

}
