package us.ihmc.footstepPlanning.ui.components;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.Point3DProperty;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.YawProperty;

public class FootstepNodeCheckingUIController
{
   @FXML
   private ToggleButton enableNodeChecking;
   @FXML
   private ToggleButton nodeCheckerPositionToggleButton;
   @FXML
   private ToggleButton nodeCheckerRotationToggleButton;

   @FXML
   private Spinner<Double> nodeCheckerFootXSpinner;
   @FXML
   private Spinner<Double> nodeCheckerFootYSpinner;
   @FXML
   private Spinner<Double> nodeCheckerFootYaw;

   private JavaFXMessager messager;
   private final Point3DProperty nodeCheckerFootPosition = new Point3DProperty(this, "nodeCheckerFootPosition", new Point3D());
   private final YawProperty nodeCheckerFootYawProperty = new YawProperty(this, "nodeCheckerFootYaw", 0.0);

   private void setupControls()
   {
      nodeCheckerFootXSpinner.setValueFactory(createPositionValueFactory());
      nodeCheckerFootYSpinner.setValueFactory(createPositionValueFactory());
      nodeCheckerFootYaw.setValueFactory(createStartGoalOrientationValueFactory());
   }

   public void bindControls()
   {
      setupControls();

      messager.bindBidirectional(FootstepPlannerMessagerAPI.EnableNodeChecking, enableNodeChecking.selectedProperty(), false);

      nodeCheckerFootPosition.bindBidirectionalX(nodeCheckerFootXSpinner.getValueFactory().valueProperty());
      nodeCheckerFootPosition.bindBidirectionalY(nodeCheckerFootYSpinner.getValueFactory().valueProperty());
      messager.bindBidirectional(FootstepPlannerMessagerAPI.NodeCheckingPosition, nodeCheckerFootPosition, false);

      nodeCheckerFootYawProperty.bindBidirectionalYaw(nodeCheckerFootYaw.getValueFactory().valueProperty());
      messager.bindBidirectional(FootstepPlannerMessagerAPI.NodeCheckingOrientation, nodeCheckerFootYawProperty, false);
   }


   private SpinnerValueFactory.DoubleSpinnerValueFactory createPositionValueFactory()
   {
      double min = -100.0;
      double max = 100.0;
      double amountToStepBy = 0.1;
      return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, 0.0, amountToStepBy);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createStartGoalOrientationValueFactory()
   {
      double min = -Math.PI;
      double max = Math.PI;
      double amountToStepBy = 0.1;
      return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, 0.0, amountToStepBy);
   }

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }
}
