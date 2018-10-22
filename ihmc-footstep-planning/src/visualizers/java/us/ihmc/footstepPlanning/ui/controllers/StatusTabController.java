package us.ihmc.footstepPlanning.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.messager.TopicListener;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.Point3DProperty;
import us.ihmc.pathPlanning.visibilityGraphs.ui.properties.YawProperty;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.*;

public class StatusTabController
{
   private static final boolean verbose = false;

   @FXML
   private ComboBox<FootstepPlannerType> plannerTypeComboBox;
   @FXML
   private TextField requestID;
   @FXML
   private TextField sequenceID;
   @FXML
   private TextField timeTaken;
   @FXML
   private TextField plannerStatus;
   @FXML
   private TextField planningResult;


   @FXML
   public void computePath()
   {
      if (verbose)
         PrintTools.info(this, "Clicked compute path...");

      messager.submitMessage(ComputePathTopic, true);
   }

   private JavaFXMessager messager;


   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   private void setupControls()
   {
      ObservableList<FootstepPlannerType> plannerTypeOptions = FXCollections.observableArrayList(FootstepPlannerType.values);
      plannerTypeComboBox.setItems(plannerTypeOptions);
      plannerTypeComboBox.setValue(FootstepPlannerType.A_STAR);
   }

   private class TextViewerListener<T> implements TopicListener<T>
   {
      private final TextField textField;
      public TextViewerListener(TextField textField)
      {
         this.textField = textField;
      }

      public void receivedMessageForTopic(T messageContent)
      {
         if (messageContent != null)
            textField.promptTextProperty().setValue(messageContent.toString());
      }
   }

   public void bindControls()
   {
      setupControls();

      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerTypeTopic, plannerTypeComboBox.valueProperty(), true);
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerRequestIdTopic, new TextViewerListener<>(requestID));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.SequenceIdTopic, new TextViewerListener<>(sequenceID));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerTimeTakenTopic, new TextViewerListener<>(timeTaken));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlanningResultTopic, new TextViewerListener<>(planningResult));
      messager.registerJavaFXSyncedTopicListener(FootstepPlannerMessagerAPI.PlannerStatusTopic, new TextViewerListener<>(plannerStatus));
   }


}
