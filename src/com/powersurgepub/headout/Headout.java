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

  import com.powersurgepub.psutils2.basic.*;
  import com.powersurgepub.psutils2.env.*;
  import com.powersurgepub.psutils2.logging.*;
  import com.powersurgepub.psutils2.strings.*;
  import com.powersurgepub.psutils2.textio.*;
  import com.powersurgepub.psutils2.ui.*;

  import java.io.*;
  import java.net.*;
  import java.text.*;

  import javafx.application.*;
  import javafx.collections.*;
  import javafx.scene.*;
  import javafx.scene.control.*;
  import javafx.scene.control.Alert.*;
  import javafx.scene.layout.*;
  import javafx.stage.*;

/**
 A small app that generates a table of contents for a markdown document. 

 @author Herb Bowie. 
 */
public class Headout 
    extends Application {
  
  public static final String PROGRAM_NAME = "HeadOut";
  public static final String PROGRAM_VERSION = "2.00";
  
  public static final boolean PREFS_AVAILABLE = false;
  
  private     static  final int   DEFAULT_WIDTH = 780;
  private     static  final int   DEFAULT_HEIGHT = 360;
  
  private     static  final String TRANSFORM_TYPE = "transform-type";

  private             File                appFolder;
  private             String              userName;
  private             String              userDirString;
  
  private             Appster appster;

  private             String  country = "  ";
  private             String  language = "  ";

  private             Home                home;
  private             ProgramVersion      programVersion;
  private             Trouble             trouble = Trouble.getShared();
  private             UserPrefs           prefs;
  private             WindowMenuManager   windowMenuManager;
  
  // About window
  private             AboutWindow         aboutWindow;
  
  private             LogWindow           logWindow;
  private             Logger              logger     = Logger.getShared();
  private             LogOutput           logOutput;

  private             Stage               primaryStage;
  private             VBox                primaryLayout;
  private             Scene               primaryScene;
  private             FXUtils             fxUtils;
  
  private             Menu                toolsMenu;
  private             MenuItem            toolsOptions;
  
  private             Menu                windowMenu;
  
  private			        Menu								helpMenu;

  private             MenuItem						helpAbout;
  
  private             MenuItem						helpPSPubWebSite;
  private             MenuItem            helpReduceWindowSize;
  
  // private             Component           refComponent         = null;
  private             int                 locationStyle        
      = WindowMenuManager.LEAVE_LOCATION_AS_IS;

  private             Menu                fileMenu;
  private             MenuBar             menuBar;

  private             GridPane            mainGrid;

  private             GridPane            centerGrid;
  private             Button              transformNowButton;
  private             ComboBox<String>    transformTypeComboBox;

  private             int                 transformTypeIndex = 0;
  private             String              transformTypeStr = "";

  private             GridPane            parmsGrid = null;
  
  private             TextFileSelector    inputSelector;
  private             TextFileSelector    outputSelector;
  
  private             HeadOutTransformer  transformer = null;

  @Override
  public void start(Stage primaryStage) {

    this.primaryStage = primaryStage;
    primaryStage.setTitle("Headout");
    primaryLayout = new VBox();
    fxUtils = FXUtils.getShared();

    appster = new Appster
        (this, "powersurgepub", "com",
            PROGRAM_NAME, PROGRAM_VERSION,
            primaryStage);

    home = Home.getShared ();
    prefs = UserPrefs.getShared();

    Trouble.getShared().setParent(primaryStage);

    programVersion = ProgramVersion.getShared ();

    // Build most of the UI elements and init the Window Menu Manager
    buildMenuBar();

    windowMenuManager = WindowMenuManager.getShared(windowMenu);

    buildContent();

    primaryScene = new Scene(primaryLayout, 600, 400);

    // Let's set up Logging
    logger = Logger.getShared();
    logWindow = new LogWindow (primaryStage);
    logger.setLogOutput (logWindow);
    logger.setLogAllData (false);
    logger.setLogThreshold (LogEvent.NORMAL);
    windowMenuManager.add(logWindow);

    // About Window
    aboutWindow = new AboutWindow(
        primaryStage,
        false, // Load about window from disk?
        false, // Using JXL?
        false, // Using PegDown?
        false, // Using Xerces?
        false, // Using SAXON?
        "2013" // Starting Copyright Year
    );
    int aboutIndex = windowMenuManager.add(aboutWindow);

    logger.recordEvent(LogEvent.NORMAL,
        PROGRAM_NAME + " " + PROGRAM_VERSION + " starting up",
        false);

    windowMenuManager.makeVisible(logWindow);

    aboutWindow = new AboutWindow(
        primaryStage,
        false,   // loadFromDisk,
        true,    // jxlUsed,
        true,    // Markdown converter Used,
        true,    // xerces used
        true,    // saxon used
        "2009"); // copyRightYearFrom

    home.setHelpMenu(primaryStage, helpMenu, aboutWindow);

    // Get App Folder
    appFolder = home.getAppFolder();
    if (appFolder == null) {
      trouble.report ("The " + home.getProgramName()
              + " Folder could not be found",
          "App Folder Missing");
    } else {
      Logger.getShared().recordEvent (LogEvent.NORMAL,
          "App Folder = " + appFolder.toString(),
          false);
    }

    // Get System Properties
    userName = System.getProperty ("user.name");
    userDirString = System.getProperty (GlobalConstants.USER_DIR);
    Logger.getShared().recordEvent (LogEvent.NORMAL,
        "User Directory = " + userDirString,
        false);

    // Write some basic data about the run-time environment to the log
    Logger.getShared().recordEvent (LogEvent.NORMAL,
        "Java Virtual Machine = " + System.getProperty("java.vm.name") +
            " version " + System.getProperty("java.vm.version") +
            " from " + StringUtils.removeQuotes(System.getProperty("java.vm.vendor")),
        false);
    if (Home.runningOnMac()) {
      Logger.getShared().recordEvent (LogEvent.NORMAL,
          "Mac Runtime for Java = " + System.getProperty("mrj.version"),
          false);
    }
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    NumberFormat numberFormat = NumberFormat.getInstance();
    Logger.getShared().recordEvent (LogEvent.NORMAL,
        "Available Memory = " + numberFormat.format (Runtime.getRuntime().freeMemory()),
        false);

    // Now let's bring the curtains up
    primaryStage.setScene(primaryScene);
    primaryStage.setWidth
        (prefs.getPrefAsDouble (UserPrefs.WIDTH, 620));
    primaryStage.setHeight
        (prefs.getPrefAsDouble (UserPrefs.HEIGHT, 620));
    primaryStage.setX
        (prefs.getPrefAsDouble (UserPrefs.LEFT, 100));
    primaryStage.setY
        (prefs.getPrefAsDouble (UserPrefs.TOP,  100));
    primaryStage.show();

    WindowMenuManager.getShared().hide(logWindow);
  }

  /**
   Build the menu bar for the app.
   */
  private void buildMenuBar() {

    menuBar = new MenuBar();
    menuBar.setUseSystemMenuBar(true);

    fileMenu = new Menu("File");

    toolsMenu = new Menu("Tools");
    toolsOptions = new MenuItem ("Options");
    toolsMenu.getItems().add(toolsOptions);
    toolsOptions.setOnAction( e -> handlePreferences());

    windowMenu = new Menu("Window");

    helpMenu = new Menu("Help");

    menuBar.getMenus().addAll(fileMenu, toolsMenu, windowMenu, helpMenu);
    menuBar.prefWidthProperty().bind(primaryStage.widthProperty());
    primaryLayout.getChildren().add(menuBar);
  }

  /**
   Build the controls for the window.
   */
  private void buildContent() {

    mainGrid = new GridPane();
    centerGrid = new GridPane();
    fxUtils.applyStyle(mainGrid);
    fxUtils.applyStyle(centerGrid);

    inputSelector = new TextFileSelector (primaryStage, TextFileSelector.INPUT);
    GridPane leftGrid = inputSelector.getGrid();
    mainGrid.add(leftGrid, 0, 0, 1, 5);
    GridPane.setHgrow(leftGrid, Priority.ALWAYS);
    GridPane.setVgrow(leftGrid, Priority.ALWAYS);

    outputSelector = new TextFileSelector (primaryStage, TextFileSelector.OUTPUT);
    GridPane rightGrid = outputSelector.getGrid();
    mainGrid.add(rightGrid, 2, 0, 1, 5);
    GridPane.setHgrow(rightGrid, Priority.ALWAYS);
    GridPane.setVgrow(rightGrid, Priority.ALWAYS);

    Label typeLabel = new Label("Specify type of Transformation");
    centerGrid.add(typeLabel, 0, 0, 1, 1);
    ObservableList<String> typeList = FXCollections.<String>observableArrayList(
    "Create Markdown ToC from Markdown",
      "Create HTML ToC from Markdown",
      "Add ToC to Markdown",
      "Create Markdown from OPML");
    transformTypeComboBox = new ComboBox<>(typeList);
    transformTypeComboBox.getSelectionModel().select(prefs.getPrefAsInt(TRANSFORM_TYPE, 0));
    transformTypeComboBox.setOnAction( e -> transformTypeSelected());
    transformTypeComboBox.setMinWidth(150);
    centerGrid.add(transformTypeComboBox, 0, 1, 1, 1);
    GridPane.setHgrow(transformTypeComboBox, Priority.ALWAYS);

    transformTypeSelected();

    transformNowButton = new Button("Transform Now");
    transformNowButton.setOnAction(e -> transformNow());
    centerGrid.add(transformNowButton, 0, 4, 1, 1);
    GridPane.setHgrow(transformNowButton, Priority.ALWAYS);

    mainGrid.add(centerGrid, 1, 0, 1, 5);
    GridPane.setHgrow(centerGrid, Priority.ALWAYS);
    GridPane.setVgrow(centerGrid, Priority.ALWAYS);

    primaryLayout.getChildren().add(mainGrid);

  }
  
  /**
     Standard way to respond to an About Menu Item Selection on a Mac.
   */
  public void handleAbout() {
    displayAuxiliaryWindow(aboutWindow);
  }
  
  public void displayAuxiliaryWindow(WindowToManage window) {
    windowMenuManager.locateUpperLeftAndMakeVisible(primaryStage, window);
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
    prefs.setPref(TRANSFORM_TYPE, transformTypeComboBox.getSelectionModel().getSelectedIndex());
    prefs.savePrefs();
  }
  
  public void setLocationStyle(int locationStyle) {
    this.locationStyle = locationStyle;
  }
  
  public int getLocationStyle() {
    return locationStyle;
  }

  /**
   Adjust the transformation parms based on the transform type selected.
   */
  private void transformTypeSelected() {
    
    if (parmsGrid != null) {
      centerGrid.getChildren().remove(parmsGrid);
    }

    transformTypeIndex = transformTypeComboBox.getSelectionModel().getSelectedIndex();
    transformTypeStr = (String)transformTypeComboBox.getSelectionModel().getSelectedItem();

    switch (transformTypeIndex) {

      case 0:
        transformer = new GenTocFromMarkdown (
            transformTypeIndex,
            transformTypeStr);
        parmsGrid = transformer.getGrid();
        break;
        
      case 1:
        transformer = new GenTocFromMarkdown (
            transformTypeIndex,
            transformTypeStr);
        parmsGrid = transformer.getGrid();
        break;
        
      case 2:
        transformer = new GenTocFromMarkdown (
            transformTypeIndex,
            transformTypeStr);
        parmsGrid = transformer.getGrid();
        break;
        
      case 3:
        transformer = new OPMLtoMarkdown (
            transformTypeIndex,
            transformTypeStr);
        parmsGrid = transformer.getGrid();
        break;
            
    }

    centerGrid.add(parmsGrid, 0, 2, 1, 2);
    GridPane.setHgrow(parmsGrid, Priority.ALWAYS);
    GridPane.setVgrow(parmsGrid, Priority.ALWAYS);
    
  }

  /**
   Transform the input to the output using the specified Transform method.
   */
  private void transformNow() {
    logger.recordEvent(LogEvent.NORMAL, "Starting Text Transformation", false);
    logger.recordEvent(LogEvent.NORMAL, "Transform Type = " + transformTypeStr, false);
    try {
      transformer.transformNow(inputSelector.getReader(), outputSelector.getWriter());
    } catch (TransformException e) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.initOwner(primaryStage);
      alert.setTitle("Transform error");
      alert.setHeaderText(null);
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }

  /**
   The main method.

   @param args the command line arguments
   */
  public static void main(String[] args) {
    launch(args);
  }
}
