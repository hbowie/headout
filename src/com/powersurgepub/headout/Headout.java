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

  import com.powersurgepub.pstextio.*;
  import com.powersurgepub.psdatalib.ui.*;
  import com.powersurgepub.psutils.*;
  import com.powersurgepub.xos2.*;
  import java.awt.*;
  import java.awt.event.*;
  import java.io.*;
  import java.net.*;
  import javax.swing.*;
  import javax.swing.event.*;

/**
 A small app that generates a table of contents for a markdown document. 

 @author Herb Bowie. 
 */
public class Headout 
    extends javax.swing.JFrame 
        implements
            WindowToManage,
            XHandler{
  
  public static final String PROGRAM_NAME = "HeadOut";
  public static final String PROGRAM_VERSION = "1.10";
  
  public static final boolean PREFS_AVAILABLE = false;
  
  private     static  final int   DEFAULT_WIDTH = 780;
  private     static  final int   DEFAULT_HEIGHT = 360;
  
  private     static  final String TRANSFORM_TYPE = "transform-type";
  
  private                   int   defaultX = 0;
  private                   int   defaultY = 0;
  
  private             Appster appster;

  private             String  country = "  ";
  private             String  language = "  ";

  private             Home                home;
  private             ProgramVersion      programVersion;
  private             XOS                 xos;
  private             Trouble             trouble = Trouble.getShared();
  private             UserPrefs           prefs;
  private             WindowMenuManager   windowMenuManager;
  
  // About window
  private             AboutWindow         aboutWindow;
  
  private             LogWindow           logWindow;
  private             Logger              logger     = Logger.getShared();
  private             LogOutput           logOutput;
  
  private             JMenu               toolsMenu;
  private             JMenuItem           toolsOptions;
  
  private             JMenu               windowMenu;
  
  private			        JMenu								helpMenu;
  
  private             JSeparator          helpSeparator2;
  private             JMenuItem						helpAbout;
  
  private             JMenuItem						helpPSPubWebSite;
  private             JSeparator          helpSeparator3;
  private             JMenuItem           helpReduceWindowSize;
  
  private             Component           refComponent         = null;
  private             int                 locationStyle        
      = WindowMenuManager.LEAVE_LOCATION_AS_IS;
  
  private             TextLineReader      reader = null;
  
  private             TextFileSelector    inputSelector;
  private             TextFileSelector    outputSelector;
  
  private             HeadOutTransformer  transformer = null;
  
  private             JPanel              parmsPanel = null;

  /**
   Creates new form Headout
   */
  public Headout() {
    
    appster = new Appster
        ("powersurgepub", "com",
          PROGRAM_NAME, PROGRAM_VERSION,
          language, country,
          this, this);
    
    home = Home.getShared ();
    xos = XOS.getShared();
    prefs = UserPrefs.getShared();
    
    Trouble.getShared().setParent(this);
    if (PREFS_AVAILABLE) {
      xos.enablePreferences();
    }
    
    programVersion = ProgramVersion.getShared ();
    
    initComponents();
    
    transformTypeComboBox.setSelectedIndex(prefs.getPrefAsInt(TRANSFORM_TYPE, 0));
    
    inputSelector = new TextFileSelector (this, TextFileSelector.INPUT, inputPanel);
    outputSelector = new TextFileSelector (this, TextFileSelector.OUTPUT, outputPanel);
    
    transformTypeSelected();
    
    setBounds (100, 100, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    try {
      javax.swing.UIManager.setLookAndFeel
        (javax.swing.UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // Let's hope this doesn't happen!!
    }
    SwingUtilities.updateComponentTreeUI (this);
    
    xos.setXHandler (this);
    xos.setMainWindow (this);
    
    // Create Menus for the app
    
    // File menu
    // fileMenu = new JMenu("File");
    // if (! xos.isRunningOnMacOS()) {
    //   menuBar.add (fileMenu);
      /*
      fileExit = new JMenuItem ("Exit/Quit");
      fileExit.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q,
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
      fileMenu.add (fileExit);
      fileExit.addActionListener (new ActionListener ()
        {
          public void actionPerformed (ActionEvent event) {
            handleQuit();
          } // end actionPerformed method
        } // end action listener
      );
      */
    // }
    xos.setFileMenu (fileMenu);
    
    // Tools menu
    if (! xos.isRunningOnMacOS()) {
      toolsMenu = new JMenu("Tools");
      menuBar.add (toolsMenu);
      toolsOptions = new JMenuItem ("Options");
      toolsMenu.add (toolsOptions);
      toolsOptions.addActionListener (new ActionListener ()
        {
          public void actionPerformed (ActionEvent event) {
            handlePreferences();
          } // end actionPerformed method
        } // end action listener
      );
    }
    
    // Window menu
    windowMenu = new JMenu("Window");
    menuBar.add (windowMenu);
    windowMenuManager = WindowMenuManager.getShared(windowMenu);
    
    // Log Window
    logWindow = new LogWindow ();
    logOutput = new LogOutputText(logWindow.getTextArea());
    Logger.getShared().setLog (logOutput);
    Logger.getShared().setLogAllData (false);
    Logger.getShared().setLogThreshold (LogEvent.NORMAL);
    int logIndex = windowMenuManager.add(logWindow);
    System.out.println("Log Window added at " + String.valueOf(logIndex));
    
    // About Window
    aboutWindow = new AboutWindow(
        false, // Load about window from disk?
        false, // Using JXL?
        false, // Using PegDown? 
        false, // Using Xerces?
        false, // Using SAXON?
        "2013" // Starting Copyright Year
        );
    int aboutIndex = windowMenuManager.add(aboutWindow);
    System.out.println("About Window added at " + String.valueOf(aboutIndex));
    
    // Help Menu 
    helpMenu = new JMenu("Help");
    menuBar.add (helpMenu);
    
    // Help Menu 
    xos.setHelpMenu (helpMenu);
    home.setHelpMenu(this, helpMenu);
    xos.setHelpMenuItem (home.getHelpMenuItem());
    /*
    if (! xos.isRunningOnMacOS()) {
      helpAbout = new JMenuItem ("About " + PROGRAM_NAME);
      helpMenu.add (helpAbout);
      helpAbout.addActionListener (new ActionListener ()
        {
          public void actionPerformed (ActionEvent event) {
            handleAbout();
          } // end actionPerformed method
        } // end action listener
      );
    } 
    helpSeparator3 = new JSeparator();
    helpMenu.add (helpSeparator3);
    */
    
    helpReduceWindowSize = new JMenuItem ("Reduce Window Size");
    helpReduceWindowSize.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W,
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    helpMenu.add (helpReduceWindowSize);
    helpReduceWindowSize.addActionListener(new ActionListener()
      {
        public void actionPerformed (ActionEvent event) {
          setDefaultScreenSizeAndLocation();
        }
      });
    
    WindowMenuManager.getShared().addWindowMenu(windowMenu);
    /* 
    try {
      userGuideURL = new URL (pageURL, USER_GUIDE);
    } catch (MalformedURLException e) {
    } 
    */
    /* 
    try {
      programHistoryURL = new URL(pageURL, PROGRAM_HISTORY);
    } catch (MalformedURLException e) {
      // shouldn't happen
    } 
    */
    // xos.setHelpMenuItem (helpUserGuideMenuItem);
    
  }
  
  public void setDefaultScreenSizeAndLocation() {

		this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    this.setResizable (true);
		calcDefaultScreenLocation();
		this.setLocation (defaultX, defaultY);
  }
  
  private void calcDefaultScreenLocation() {
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    defaultX = (d.width - this.getSize().width) / 2;
    defaultY = (d.height - this.getSize().height) / 2;
  }
  
  /**
     Standard way to respond to an About Menu Item Selection on a Mac.
   */
  public void handleAbout() {
    displayAuxiliaryWindow(aboutWindow);
  }
  
  public void displayAuxiliaryWindow(WindowToManage window) {
    windowMenuManager.locateUpperLeftAndMakeVisible(this, window);
  }
  
  /**      
    Standard way to respond to a document being passed to this application on a Mac.
   
    @param inFile File to be processed by this application, generally
                  as a result of a file or directory being dragged
                  onto the application icon.
   */
  public void handleOpenFile (File inFile) {
    // Not supported
  }
  
  /**
   Open the passed URI. 
   
   @param inURI The URI to open. 
  */
  public void handleOpenURI(URI inURI) {
    // Not supported
  }
  
  public boolean preferencesAvailable() {
    return PREFS_AVAILABLE;
  }
  
  /**
     Standard way to respond to a Preferences Item Selection on a Mac.
   */
  public void handlePreferences() {
    displayPrefs ();
  }

  public void displayPrefs () {
    // displayAuxiliaryWindow(prefsWindow);
  }
  
  /**
   Standard way to respond to a print request.
   */
  public void handlePrintFile (File printFile) {
    // not supported
  }
  
  private void windowClose() {
    System.out.println("window close...");
    handleQuit();
  }
  
  /**
     We're out of here!
   */
  public void handleQuit() {
    savePrefs();
    System.exit(0);
  }

  private void savePrefs() {
    prefs.setPref(TRANSFORM_TYPE, transformTypeComboBox.getSelectedIndex());
    prefs.savePrefs();
  }
  
  public void setRefComponent (Component refComponent) {
    this.refComponent = refComponent;
  }
  
  public Component getRefComponent() {
    return refComponent;
  }
  
  public void setLocationStyle(int locationStyle) {
    this.locationStyle = locationStyle;
  }
  
  public int getLocationStyle() {
    return locationStyle;
  }
  
  private void transformTypeSelected() {
    
    if (parmsPanel != null) {
      getContentPane().remove(parmsPanel);
    }
    
    parmsPanel = new JPanel();
    
    switch (transformTypeComboBox.getSelectedIndex()) {
      case 0:
        transformer = new GenTocFromMarkdown (
            this,
            transformTypeComboBox.getSelectedIndex(),
            (String)transformTypeComboBox.getSelectedItem(),
            parmsPanel);
        break;
        
      case 1:
        transformer = new GenTocFromMarkdown (
            this,
            transformTypeComboBox.getSelectedIndex(),
            (String)transformTypeComboBox.getSelectedItem(),
            parmsPanel);
        break;
        
      case 2:
        transformer = new GenTocFromMarkdown (
            this,
            transformTypeComboBox.getSelectedIndex(),
            (String)transformTypeComboBox.getSelectedItem(),
            parmsPanel);
        break;
        
      case 3:
        transformer = new OPMLtoMarkdown (
            this,
            transformTypeComboBox.getSelectedIndex(),
            (String)transformTypeComboBox.getSelectedItem(),
            parmsPanel);
        break;
            
    }
    
    java.awt.GridBagConstraints gridBagConstraints;
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    getContentPane().add(parmsPanel, gridBagConstraints);
    pack();
    
  }

  /**
   This method is called from within the constructor to initialize the form.
   WARNING: Do NOT modify this code. The content of this method is always
   regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    outputFormatGroup = new javax.swing.ButtonGroup();
    transformTypeComboBox = new javax.swing.JComboBox();
    inputPanel = new javax.swing.JPanel();
    outputPanel = new javax.swing.JPanel();
    transformNowButton = new javax.swing.JButton();
    menuBar = new javax.swing.JMenuBar();
    fileMenu = new javax.swing.JMenu();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    transformTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Create Markdown ToC from Markdown", "Create HTML ToC from Markdown", "Add ToC to Markdown", "Create Markdown from OPML" }));
    transformTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        transformTypeComboBoxActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    getContentPane().add(transformTypeComboBox, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridheight = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    getContentPane().add(inputPanel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridheight = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    getContentPane().add(outputPanel, gridBagConstraints);

    transformNowButton.setText("Transform Now");
    transformNowButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        transformNowButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    getContentPane().add(transformNowButton, gridBagConstraints);

    fileMenu.setMnemonic('f');
    fileMenu.setText("File");
    menuBar.add(fileMenu);

    setJMenuBar(menuBar);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void transformTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transformTypeComboBoxActionPerformed
    transformTypeSelected();
  }//GEN-LAST:event_transformTypeComboBoxActionPerformed

  private void transformNowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transformNowButtonActionPerformed
    try {
      transformer.transformNow(inputSelector.getReader(), outputSelector.getWriter());
    } catch (TransformException e) {
      JOptionPane.showMessageDialog(this,
        e.getMessage(),
      "Transform error",
      JOptionPane.ERROR_MESSAGE);
    }
  }//GEN-LAST:event_transformNowButtonActionPerformed

  /**
   @param args the command line arguments
   */
  public static void main(String args[]) {
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
     */
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(Headout.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(Headout.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(Headout.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(Headout.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new Headout().setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenu fileMenu;
  private javax.swing.JPanel inputPanel;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.ButtonGroup outputFormatGroup;
  private javax.swing.JPanel outputPanel;
  private javax.swing.JButton transformNowButton;
  private javax.swing.JComboBox transformTypeComboBox;
  // End of variables declaration//GEN-END:variables
}
