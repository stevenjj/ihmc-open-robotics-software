package us.ihmc.commonWalkingControlModules.controllerCore.command;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommandList;
import us.ihmc.outputProcessing.outputData.JointDesiredOutputListReadOnly;

public interface ControllerCoreCommandInterface
{
   InverseDynamicsCommandList getInverseDynamicsCommandList();
   VirtualModelControlCommandList getVirtualModelControlCommandList();
   FeedbackControlCommandList getFeedbackControlCommandList();
   InverseKinematicsCommandList getInverseKinematicsCommandList();
   JointDesiredOutputListReadOnly getLowLevelOneDoFJointDesiredDataHolder();
   WholeBodyControllerCoreMode getControllerCoreMode();
}