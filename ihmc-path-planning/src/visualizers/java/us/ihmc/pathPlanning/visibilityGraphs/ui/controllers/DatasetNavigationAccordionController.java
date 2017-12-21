package us.ihmc.pathPlanning.visibilityGraphs.ui.controllers;

import java.io.File;

import com.sun.javafx.scene.control.skin.LabeledText;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityGraphsIOTools;
import us.ihmc.pathPlanning.visibilityGraphs.ui.messager.SimpleUIMessager;
import us.ihmc.pathPlanning.visibilityGraphs.ui.messager.UIVisibilityGraphsTopics;
import us.ihmc.robotics.geometry.PlanarRegionsList;

public class DatasetNavigationAccordionController
{
   private final File visualizerDataFolder, testDataFolder;
   private File customDataFolder = null;

   @FXML
   private Accordion datasetNavigationAccordion;

   @FXML
   private ListView<String> visualizerDataListView, testDataListView, customDataListView;

   private SimpleUIMessager messager;
   private Window ownerWindow;

   public DatasetNavigationAccordionController()
   {
      visualizerDataFolder = new File("..\\visualizers\\resources\\Data");
      if (!visualizerDataFolder.exists())
         throw new RuntimeException("Wrong path to the visualizer data folder, please update me.");
      testDataFolder = new File("..\\test\\resources\\" + VisibilityGraphsIOTools.DATA_FOLDER_NAME);
      if (!testDataFolder.exists())
         throw new RuntimeException("Wrong path to the test data folder, please update me.");
   }

   public void attachMessager(SimpleUIMessager messager)
   {
      this.messager = messager;
   }

   public void setMainWindow(Window ownerWindow)
   {
      this.ownerWindow = ownerWindow;
   }

   public void bindControls()
   {
   }

   @FXML
   public void load()
   {
      visualizerDataListView.getItems().clear();
      visualizerDataListView.getItems().addAll(VisibilityGraphsIOTools.getPlanarRegionAndVizGraphsFilenames(visualizerDataFolder));

      testDataListView.getItems().clear();
      testDataListView.getItems().addAll(VisibilityGraphsIOTools.getPlanarRegionAndVizGraphsFilenames(testDataFolder));

      customDataListView.getItems().clear();
      if (customDataFolder != null && customDataFolder.exists() && customDataFolder.isDirectory())
         customDataListView.getItems().addAll(VisibilityGraphsIOTools.getPlanarRegionAndVizGraphsFilenames(customDataFolder));
   }

   @FXML
   private void loadCustomDataFolder()
   {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      File result = directoryChooser.showDialog(ownerWindow);
      if (result == null)
         return;

      customDataFolder = result;
      load();
   }

   @FXML
   private void requestNewVisualizerData(MouseEvent event)
   {
      requestNewData(visualizerDataListView, visualizerDataFolder, event);
   }

   @FXML
   private void requestNewTestData(MouseEvent event)
   {
      requestNewData(testDataListView, testDataFolder, event);
   }

   @FXML
   private void requestNewCustomData(MouseEvent event)
   {
      requestNewData(customDataListView, customDataFolder, event);
   }

   private void requestNewData(ListView<String> listViewOwner, File dataFolder, MouseEvent event)
   {
      if (dataFolder == null)
         return;
      if (!hasListViewCellBeenDoubleClicked(event))
         return;

      String filename = listViewOwner.getSelectionModel().getSelectedItem();
      File dataFile = findChildFile(dataFolder, filename);
      PlanarRegionsList loadedPlanarRegions = VisibilityGraphsIOTools.importPlanarRegionData(dataFile);
      messager.submitMessage(UIVisibilityGraphsTopics.PlanarRegionData, loadedPlanarRegions);
   }

   private static File findChildFile(File folder, String childFilename)
   {
      return folder.listFiles((dir, name) -> name.equals(childFilename))[0];
   }

   private static boolean hasListViewCellBeenDoubleClicked(MouseEvent event)
   {
      return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && event.getTarget() instanceof LabeledText;
   }
}