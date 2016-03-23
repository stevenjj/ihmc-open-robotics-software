package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.controllerCore.command.SolverWeightLevels;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointAccelerationIntegrationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelJointControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderReadOnly;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlMode;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.TaskspaceToJointspaceCalculator;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.trajectories.PoseTrajectoryGenerator;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SpatialMotionVector;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicReferenceFrame;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.tools.FormattingTools;

/**
 * @author twan
 *         Date: 5/9/13
 */
public class TaskspaceHandPositionControlState extends TrajectoryBasedTaskspaceHandControlState
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name;
   private final YoVariableRegistry registry;

   private final LowLevelOneDoFJointDesiredDataHolder jointDesiredDataHolder;
   private final JointAccelerationIntegrationCommand jointAccelerationIntegrationCommand;
   private final SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();
   private final DenseMatrix64F selectionMatrix = new DenseMatrix64F(SpatialMotionVector.SIZE, SpatialMotionVector.SIZE);
   private final PrivilegedConfigurationCommand privilegedConfigurationCommand = new PrivilegedConfigurationCommand();
   private final InverseDynamicsCommandList inverseDynamicsCommandList = new InverseDynamicsCommandList();

   // viz stuff:
   private final YoGraphicReferenceFrame dynamicGraphicReferenceFrame;
   private final PoseReferenceFrame desiredPositionFrame;

   // temp stuff:
   private final FramePose desiredPose = new FramePose();
   private final FramePoint desiredPosition = new FramePoint(worldFrame);
   private final FrameVector desiredVelocity = new FrameVector(worldFrame);
   private final FrameVector desiredAcceleration = new FrameVector(worldFrame);

   private final FrameOrientation desiredOrientation = new FrameOrientation(worldFrame);
   private final FrameVector desiredAngularVelocity = new FrameVector(worldFrame);
   private final FrameVector desiredAngularAcceleration = new FrameVector(worldFrame);

   private PoseTrajectoryGenerator poseTrajectoryGenerator;

   private final FramePose controlFramePose = new FramePose();
   private final PoseReferenceFrame trackingFrame;
   private final ReferenceFrame endEffectorFrame;

   private final DoubleYoVariable doneTrajectoryTime;
   private final DoubleYoVariable holdPositionDuration;

   private final YoSE3PIDGainsInterface gains;
   private final DoubleYoVariable weight;

   public TaskspaceHandPositionControlState(String namePrefix, HandControlMode stateEnum, boolean doPositionControl, OneDoFJoint[] armJoints, RigidBody base, RigidBody endEffector,
         RigidBody primaryBase, YoSE3PIDGainsInterface gains, YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      super(stateEnum);
      this.gains = gains;

      name = namePrefix + FormattingTools.underscoredToCamelCase(this.getStateEnum().toString(), true) + "State";
      registry = new YoVariableRegistry(name);

      endEffectorFrame = endEffector.getBodyFixedFrame();

      weight = new DoubleYoVariable(namePrefix + "TaskspaceWeight", registry);
      weight.set(SolverWeightLevels.HAND_TASKSPACE_WEIGHT);

      spatialFeedbackControlCommand.set(base, endEffector);
      spatialFeedbackControlCommand.setPrimaryBase(primaryBase);

      parentRegistry.addChild(registry);

      trackingFrame = new PoseReferenceFrame("trackingFrame", endEffectorFrame);
      desiredPositionFrame = new PoseReferenceFrame(name + "DesiredFrame", worldFrame);

      if (yoGraphicsListRegistry != null)
      {
         YoGraphicsList list = new YoGraphicsList(name);

         dynamicGraphicReferenceFrame = new YoGraphicReferenceFrame(desiredPositionFrame, registry, 0.3);
         list.add(dynamicGraphicReferenceFrame);

         yoGraphicsListRegistry.registerYoGraphicsList(list);
         list.hideYoGraphics();
      }
      else
      {
         dynamicGraphicReferenceFrame = null;
      }

      doneTrajectoryTime = new DoubleYoVariable(namePrefix + "DoneTrajectoryTime", registry);
      holdPositionDuration = new DoubleYoVariable(namePrefix + "HoldPositionDuration", registry);

      privilegedConfigurationCommand.applyPrivilegedConfigurationToSubChain(primaryBase, endEffector);
      inverseDynamicsCommandList.addCommand(privilegedConfigurationCommand);

      if (doPositionControl)
      {
         jointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();
         jointDesiredDataHolder.registerJointsWithEmptyData(armJoints);
         jointDesiredDataHolder.setJointsControlMode(armJoints, LowLevelJointControlMode.POSITION_CONTROL);
         jointAccelerationIntegrationCommand = new JointAccelerationIntegrationCommand();
         for (int i = 0; i < armJoints.length; i++)
            jointAccelerationIntegrationCommand.addJointToComputeDesiredPositionFor(armJoints[i]);
         inverseDynamicsCommandList.addCommand(jointAccelerationIntegrationCommand);
      }
      else
      {
         jointDesiredDataHolder = null;
         jointAccelerationIntegrationCommand = null;
      }
   }

   public void setWeight(double weight)
   {
      this.weight.set(weight);
   }

   @Override
   public void doAction()
   {
      if (poseTrajectoryGenerator.isDone())
         recordDoneTrajectoryTime();

      poseTrajectoryGenerator.compute(getTimeInCurrentState());

      poseTrajectoryGenerator.getPose(desiredPose);
      poseTrajectoryGenerator.getLinearData(desiredPosition, desiredVelocity, desiredAcceleration);
      poseTrajectoryGenerator.getAngularData(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);

      spatialFeedbackControlCommand.changeFrameAndSet(desiredPosition, desiredVelocity, desiredVelocity);
      spatialFeedbackControlCommand.changeFrameAndSet(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);
      spatialFeedbackControlCommand.setGains(gains);
      spatialFeedbackControlCommand.setWeightForSolver(weight.getDoubleValue());

      updateVisualizers();

   }

   @Override
   public void doTransitionIntoAction()
   {
      poseTrajectoryGenerator.showVisualization();
      poseTrajectoryGenerator.initialize();
      doneTrajectoryTime.set(Double.NaN);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      holdPositionDuration.set(0.0);
   }

   @Override
   public boolean isDone()
   {
      if (Double.isNaN(doneTrajectoryTime.getDoubleValue()))
         return false;

      return getTimeInCurrentState() > doneTrajectoryTime.getDoubleValue() + holdPositionDuration.getDoubleValue();
   }

   private void recordDoneTrajectoryTime()
   {
      if (Double.isNaN(doneTrajectoryTime.getDoubleValue()))
      {
         doneTrajectoryTime.set(getTimeInCurrentState());
      }
   }

   private void updateVisualizers()
   {
      if (dynamicGraphicReferenceFrame != null)
      {
         desiredPosition.changeFrame(worldFrame);
         desiredOrientation.changeFrame(worldFrame);
         desiredPositionFrame.setPoseAndUpdate(desiredPosition, desiredOrientation);
         desiredPositionFrame.update();

         dynamicGraphicReferenceFrame.update();
      }

      if (poseTrajectoryGenerator.isDone())
         poseTrajectoryGenerator.hideVisualization();
   }

   @Override
   public void setHoldPositionDuration(double time)
   {
      holdPositionDuration.set(time);
   }

   @Override
   public void setTrajectory(PoseTrajectoryGenerator poseTrajectoryGenerator)
   {
      this.poseTrajectoryGenerator = poseTrajectoryGenerator;
   }

   @Override
   public void setControlModuleForPositionControl(TaskspaceToJointspaceCalculator taskspaceToJointspaceCalculator)
   {
   }

   @Override
   public void setSelectionMatrix(DenseMatrix64F selectionMatrix)
   {
      this.selectionMatrix.reshape(selectionMatrix.getNumRows(), selectionMatrix.getNumCols());
      this.selectionMatrix.set(selectionMatrix);
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return desiredPose.getReferenceFrame();
   }

   @Override
   public ReferenceFrame getTrackingFrame()
   {
      return trackingFrame;
   }

   @Override
   public void setControlFrameFixedInEndEffector(ReferenceFrame controlFrame)
   {
      controlFramePose.setToZero(controlFrame);
      controlFramePose.changeFrame(endEffectorFrame);
      spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(controlFramePose);
      trackingFrame.setPoseAndUpdate(controlFramePose);
   }

   @Override
   public void getDesiredPose(FramePose desiredPoseToPack)
   {
      desiredPoseToPack.setIncludingFrame(desiredPose);
   }

   @Override
   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return inverseDynamicsCommandList;
   }

   @Override
   public SpatialFeedbackControlCommand getFeedbackControlCommand()
   {
      return spatialFeedbackControlCommand;
   }

   @Override
   public LowLevelOneDoFJointDesiredDataHolderReadOnly getLowLevelJointDesiredData()
   {
      return jointDesiredDataHolder;
   }
}
