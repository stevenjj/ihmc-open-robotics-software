package us.ihmc.footstepPlanning.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.tools.FootstepPlannerDataExporter;
import us.ihmc.footstepPlanning.ui.components.*;
import us.ihmc.footstepPlanning.ui.controllers.*;
import us.ihmc.footstepPlanning.ui.viewers.*;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.javaFXToolkit.scenes.View3DFactory;
import us.ihmc.pathPlanning.visibilityGraphs.ui.StartGoalPositionEditor;
import us.ihmc.pathPlanning.visibilityGraphs.ui.viewers.PlanarRegionViewer;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.*;

/**
 * This class is the visualization element of the footstep planner. It also contains a graphical interface for
 * setting planner parameters to be used by the footstep planner itself.
 */
public class FootstepPlannerUI
{
   private static final boolean VERBOSE = true;

   private final JavaFXMessager messager;
   private final Stage primaryStage;
   private final BorderPane mainPane;

   private final PlanarRegionViewer planarRegionViewer;
   private final StartGoalPositionEditor startGoalEditor;
   private final FootPositionEditor footPositionEditor;
   private final StartGoalPositionViewer startGoalPositionViewer;
   private final StartGoalOrientationViewer startGoalOrientationViewer;
   private final FootstepPathRenderer pathRenderer;
   private final StartGoalOrientationEditor orientationEditor;
   private final NodeCheckerRenderer nodeCheckerRenderer;
   private final FootstepPlannerDataExporter dataExporter;
   private final BodyPathMeshViewer bodyPathMeshViewer;
   private final VisibilityGraphsRenderer visibilityGraphsRenderer;

   @FXML
   private FootstepPlannerMenuUIController footstepPlannerMenuUIController;
   @FXML
   private StatusTabController statusTabController;
   @FXML
   private StartGoalTabController startGoalTabController;
   @FXML
   private FootstepNodeCheckingUIController footstepNodeCheckingUIController;
   @FXML
   private FootstepPlannerParametersUIController footstepPlannerParametersUIController;
   @FXML
   private FootstepPlannerCostsUIController footstepPlannerCostsUIController;
   @FXML
   private FootstepPlannerDataExporterAnchorPaneController dataExporterAnchorPaneController;

   @FXML
   private VisualizationController visibilityGraphsUIController;

   public FootstepPlannerUI(Stage primaryStage) throws Exception
   {
      this(primaryStage, new SharedMemoryJavaFXMessager(FootstepPlannerMessagerAPI.API));
      messager.startMessager();
   }

   public FootstepPlannerUI(Stage primaryStage, JavaFXMessager messager) throws Exception
   {
      this.primaryStage = primaryStage;
      this.messager = messager;

      FXMLLoader loader = new FXMLLoader();
      loader.setController(this);
      loader.setLocation(getClass().getResource(getClass().getSimpleName() + ".fxml"));

      mainPane = loader.load();

      footstepPlannerMenuUIController.attachMessager(messager);
      statusTabController.attachMessager(messager);
      startGoalTabController.attachMessager(messager);
      footstepPlannerParametersUIController.attachMessager(messager);
      footstepPlannerCostsUIController.attachMessager(messager);
      footstepNodeCheckingUIController.attachMessager(messager);
      visibilityGraphsUIController.attachMessager(messager);
      dataExporterAnchorPaneController.attachMessager(messager);

      footstepPlannerMenuUIController.setMainWindow(primaryStage);

      statusTabController.bindControls();
      startGoalTabController.bindControls();
      footstepPlannerParametersUIController.bindControls();
      footstepPlannerCostsUIController.bindControls();
      footstepNodeCheckingUIController.bindControls();
      visibilityGraphsUIController.bindControls();

      View3DFactory view3dFactory = View3DFactory.createSubscene();
      view3dFactory.addCameraController(true);
      view3dFactory.addWorldCoordinateSystem(0.3);
      Pane subScene = view3dFactory.getSubSceneWrappedInsidePane();

      this.planarRegionViewer = new PlanarRegionViewer(messager, PlanarRegionDataTopic, ShowPlanarRegionsTopic);
      this.startGoalPositionViewer = new StartGoalPositionViewer(messager, StartPositionEditModeEnabledTopic, GoalPositionEditModeEnabledTopic,
                                                                 StartPositionTopic, LowLevelGoalPositionTopic, GoalPositionTopic);
      this.startGoalOrientationViewer = new StartGoalOrientationViewer(messager);
      this.startGoalEditor = new StartGoalPositionEditor(messager, subScene, StartPositionEditModeEnabledTopic, GoalPositionEditModeEnabledTopic,
                                                         StartPositionTopic, GoalPositionTopic);
      this.footPositionEditor = new FootPositionEditor(messager, subScene);
      this.orientationEditor = new StartGoalOrientationEditor(messager, view3dFactory.getSubScene());
      this.pathRenderer = new FootstepPathRenderer(messager);
      this.nodeCheckerRenderer = new NodeCheckerRenderer(messager);
      this.dataExporter = new FootstepPlannerDataExporter(messager);
      this.bodyPathMeshViewer = new BodyPathMeshViewer(messager);
      this.visibilityGraphsRenderer = new VisibilityGraphsRenderer(messager);

      view3dFactory.addNodeToView(planarRegionViewer.getRoot());
      view3dFactory.addNodeToView(startGoalPositionViewer.getRoot());
      view3dFactory.addNodeToView(startGoalOrientationViewer.getRoot());
      view3dFactory.addNodeToView(pathRenderer.getRoot());
      view3dFactory.addNodeToView(nodeCheckerRenderer.getRoot());
      view3dFactory.addNodeToView(bodyPathMeshViewer.getRoot());
      view3dFactory.addNodeToView(visibilityGraphsRenderer.getRoot());

      planarRegionViewer.start();
      startGoalPositionViewer.start();
      startGoalOrientationViewer.start();
      startGoalEditor.start();
      orientationEditor.start();
      pathRenderer.start();
      nodeCheckerRenderer.start();
      footPositionEditor.start();
      bodyPathMeshViewer.start();
      visibilityGraphsRenderer.start();

      mainPane.setCenter(subScene);
      primaryStage.setTitle(getClass().getSimpleName());
      primaryStage.setMaximized(true);
      Scene mainScene = new Scene(mainPane, 600, 400);

      primaryStage.setScene(mainScene);
      primaryStage.setOnCloseRequest(event -> stop());
   }

   public JavaFXMessager getMessager()
   {
      return messager;
   }

   public void show()
   {
      primaryStage.show();
   }

   public void stop()
   {
      planarRegionViewer.stop();
      startGoalPositionViewer.stop();
      startGoalOrientationViewer.stop();
      startGoalEditor.stop();
      orientationEditor.stop();
      pathRenderer.stop();
      nodeCheckerRenderer.stop();
      dataExporter.stop();
      bodyPathMeshViewer.stop();
      visibilityGraphsRenderer.stop();
   }

   public static FootstepPlannerUI createMessagerUI(Stage primaryStage, JavaFXMessager messager) throws Exception
   {
      return new FootstepPlannerUI(primaryStage, messager);
   }
}
