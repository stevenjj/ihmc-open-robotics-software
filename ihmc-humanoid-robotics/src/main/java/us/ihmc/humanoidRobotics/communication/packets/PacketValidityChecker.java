package us.ihmc.humanoidRobotics.communication.packets;

import java.util.concurrent.atomic.AtomicInteger;

import us.ihmc.communication.packets.ObjectValidityChecker;
import us.ihmc.communication.packets.ObjectValidityChecker.ObjectErrorType;
import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.utils.NameBasedHashCodeTools;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmDesiredAccelerationsMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.DesiredSteeringAnglePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.OneDoFJointTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.SteeringWheelInformationPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.AdjustFootstepMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootLoadBearingMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepStatus;
import us.ihmc.humanoidRobotics.communication.packets.walking.GoHomeMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.NeckTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisOrientationTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.trajectories.TrajectoryType;

public abstract class PacketValidityChecker
{
   /**
    * Checks the validity of a {@link FootstepDataMessage}.
    *
    * @param packetToCheck
    * @return null if the packet is valid, or the error message.
    */
   public static String validateFootstepDataMessage(FootstepDataMessage packetToCheck)
   {
      ObjectErrorType packetFieldErrorType;

      packetFieldErrorType = ObjectValidityChecker.validateEnum(packetToCheck.getRobotSide());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s robotSide field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateTuple3d(packetToCheck.getLocation());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s location field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateQuat4d(packetToCheck.getOrientation());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s orientation field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (packetToCheck.getPredictedContactPoints() != null)
      {
         for (int arrayListIndex = 0; arrayListIndex < packetToCheck.getPredictedContactPoints().size(); arrayListIndex++)
         {
            packetFieldErrorType = ObjectValidityChecker.validateTuple2d(packetToCheck.getPredictedContactPoints().get(arrayListIndex));

            if (packetFieldErrorType != null)
            {
               String messageClassName = packetToCheck.getClass().getSimpleName();
               String errorMessage = messageClassName + "'s predictedContactPoints field " + packetFieldErrorType.getMessage();
               return errorMessage;
            }
         }
      }

      TrajectoryType trajectoryType = packetToCheck.getTrajectoryType();
      packetFieldErrorType = ObjectValidityChecker.validateEnum(trajectoryType);
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s trajectoryType field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      //TODO Check if thats supposed to be checked
      packetFieldErrorType = ObjectValidityChecker.validateDouble(packetToCheck.getSwingHeight());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s swingHeight field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (trajectoryType == TrajectoryType.WAYPOINTS)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         SE3TrajectoryPointMessage[] swingTrajectory = packetToCheck.getSwingTrajectory();

         if (swingTrajectory == null)
         {
            String errorMessage = messageClassName + " has no swing trajectory but trajectory type was set to " + TrajectoryType.WAYPOINTS.toString() + ".";
            return errorMessage;
         }

         if (swingTrajectory.length > Footstep.maxNumberOfSwingWaypoints)
         {
            String errorMessage = messageClassName + " has " + swingTrajectory.length + " waypoints. Up to " + Footstep.maxNumberOfSwingWaypoints + " are allowed.";
            return errorMessage;
         }

         double lastTime = 0.0;
         for (int waypointIdx = 0; waypointIdx < swingTrajectory.length; waypointIdx++)
         {
            double waypointTime = swingTrajectory[waypointIdx].getTime();
            if (waypointTime <= lastTime)
            {
               String errorMessage = messageClassName + "'s swing trajectory has non-increasing waypoint times.";
               return errorMessage;
            }
            lastTime = waypointTime;
         }

         if (packetToCheck.getSwingDuration() > 0.0 && lastTime >= packetToCheck.getSwingDuration())
         {
            String errorMessage = messageClassName + "'s swing trajectory has waypoints with time larger then the swing time.";
            return errorMessage;
         }

         if (packetToCheck.getSwingTrajectoryBlendDuration() < 0.0)
         {
            String errorMessage = messageClassName + "'s swing trajectory blend duration is less than zero.";
            return errorMessage;
         }

         if (packetToCheck.getSwingTrajectoryBlendDuration() > 0.0 && packetToCheck.getSwingTrajectory()[0].getTime() > 1.0e-5)
         {
            String errorMessage = messageClassName + "'s swing trajectory blend duration is greater than zero, initial waypoint at t = 0.0 is missing.";
            return errorMessage;
         }
      }

      if (trajectoryType == TrajectoryType.CUSTOM)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         Point3D[] positionWaypoints = packetToCheck.getCustomPositionWaypoints();
         if (positionWaypoints == null)
         {
            String errorMessage = messageClassName + "'s type is custom but no position waypoints were specified.";
            return errorMessage;
         }
      }

      return null;
   }

   /**
    * Checks the validity of a {@link FootstepDataListMessage}.
    *
    * @param packetToCheck
    * @return null if the packet is valid, or the error message.
    */
   public static String validateFootstepDataListMessage(FootstepDataListMessage packetToCheck)
   {
      ObjectErrorType packetFieldErrorType;

      packetFieldErrorType = ObjectValidityChecker.validateDouble(packetToCheck.defaultSwingDuration);
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s swingTime field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateDouble(packetToCheck.defaultTransferDuration);
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s transferTime field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (packetToCheck.getDataList() != null)
      {
         for (int arrayListIndex = 0; arrayListIndex < packetToCheck.getDataList().size(); arrayListIndex++)
         {
            FootstepDataMessage footstepData = packetToCheck.getDataList().get(arrayListIndex);
            String footstepDataListErrorMessage = validateFootstepDataMessage(footstepData);

            if (footstepDataListErrorMessage != null)
            {
               String messageClassName = packetToCheck.getClass().getSimpleName();
               String errorMessage = messageClassName + " field contains a FootstepData in which " + footstepDataListErrorMessage;
               return errorMessage;
            }
         }
      }

      return null;
   }

   /**
    * Checks the validity of a {@link FootstepDataMessage}.
    *
    * @param packetToCheck
    * @return null if the packet is valid, or the error message.
    */
   public static String validateFootstepDataMessage(AdjustFootstepMessage packetToCheck)
   {
      ObjectErrorType packetFieldErrorType;

      packetFieldErrorType = ObjectValidityChecker.validateEnum(packetToCheck.getRobotSide());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s robotSide field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateTuple3d(packetToCheck.getLocation());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s location field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateQuat4d(packetToCheck.getOrientation());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s orientation field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (packetToCheck.getPredictedContactPoints() != null)
      {
         for (int arrayListIndex = 0; arrayListIndex < packetToCheck.getPredictedContactPoints().size(); arrayListIndex++)
         {
            packetFieldErrorType = ObjectValidityChecker.validateTuple2d(packetToCheck.getPredictedContactPoints().get(arrayListIndex));

            if (packetFieldErrorType != null)
            {
               String messageClassName = packetToCheck.getClass().getSimpleName();
               String errorMessage = messageClassName + "'s predictedContactPoints field " + packetFieldErrorType.getMessage();
               return errorMessage;
            }
         }
      }

      return null;
   }

   /**
    * Checks the validity of a {@link FootstepStatus}.
    *
    * @param packetToCheck
    * @return null if the packet is valid, or the error message.
    */
   public static String validateFootstepStatus(FootstepStatus packetToCheck)
   {
      ObjectErrorType packetFieldErrorType = ObjectValidityChecker.validateTuple3d(packetToCheck.getActualFootPositionInWorld());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s actualFootPositionInWorld field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateQuat4d(packetToCheck.getActualFootOrientationInWorld());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s actualFootOrientationInWorld field " + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      packetFieldErrorType = ObjectValidityChecker.validateEnum(packetToCheck.getStatus());
      if (packetFieldErrorType != null)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         String errorMessage = messageClassName + "'s status field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (packetToCheck.getFootstepIndex() < 0)
      {
         String messageClassName = packetToCheck.getClass().getSimpleName();
         return messageClassName + ": footstepIndex field should be non-negative";
      }

      return null;
   }

   public static String validateSteeringWheelInformationPacket(SteeringWheelInformationPacket packet, SideDependentList<AtomicInteger> steeringWheelIdAtomic)
   {
      if (packet.getRobotSide() == null)
      {
         String errorMessage = "Steering hand side missing";
         return errorMessage;
      }

      if (packet.getSteeringWheelCenter() == null)
      {
         String errorMessage = "Steering wheel center missing";
         return errorMessage;
      }

      if (packet.getSteeringWheelRotationAxis() == null)
      {
         String errorMessage = "Steering wheel rotation axis missing";
         return errorMessage;
      }

      if (packet.getSteeringWheelZeroAxis() == null)
      {
         String errorMessage = "Steering wheel zero axis missing";
         return errorMessage;
      }

      if (Double.isNaN(packet.getSteeringWheelRadius()))
      {
         String errorMessage = "Steering wheel radius missing";
         return errorMessage;
      }

      if (packet.getSteeringWheelId() <= 0)
      {
         String errorMessage = "Invalid steering wheel ID, must be greater than or equal to 1";
         return errorMessage;
      }

      if (packet.getSteeringWheelId() == steeringWheelIdAtomic.get(packet.getRobotSide()).get())
      {
         String errorMessage = "Invalid steering wheel ID, must be different than the previous ID";
         return errorMessage;
      }

      ObjectErrorType errorType = ObjectValidityChecker.validateTuple3d(packet.getSteeringWheelCenter());

      if (errorType != null)
      {
         String errorMessage = "Steering wheel center " + errorType.getMessage();
         return errorMessage;
      }

      errorType = ObjectValidityChecker.validateTuple3d(packet.getSteeringWheelRotationAxis());

      if (errorType != null)
      {
         String errorMessage = "Steering wheel rotation axis " + errorType.getMessage();
         return errorMessage;
      }

      errorType = ObjectValidityChecker.validateTuple3d(packet.getSteeringWheelCenter());

      if (errorType != null)
      {
         String errorMessage = "Steering wheel zero axis " + errorType.getMessage();
         return errorMessage;
      }

      return null;
   }

   public static String validateDesiredSteeringAnglePacket(DesiredSteeringAnglePacket packet, SideDependentList<AtomicInteger> steeringWheelIdAtomic)
   {
      if (Double.isNaN(packet.getDesiredAbsoluteSteeringAngle()))
      {
         String errorMessage = "Desired steering angle missing";
         return errorMessage;
      }

      if (steeringWheelIdAtomic.get(packet.getRobotSide()).get() == -1)
      {
         String errorMessage = "Never received SteeringWheelInformationPacket";
         return errorMessage;
      }

      if (packet.getSteeringWheelId() <= 0)
      {
         String errorMessage = "Invalid steering wheel ID, must be greater than or equal to 1";
         return errorMessage;
      }

      if (packet.getSteeringWheelId() != steeringWheelIdAtomic.get(packet.getRobotSide()).get())
      {
         String errorMessage = "Unexpected steering wheel ID, probably dropped the last SteeringWheelInformationPacket";
         return errorMessage;
      }

      return null;
   }

   public static String validateHandTrajectoryMessage(HandTrajectoryMessage handTrajectoryMessage)
   {
      String errorMessage = validatePacket(handTrajectoryMessage, true);
      if (errorMessage != null)
         return HandTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if(handTrajectoryMessage.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(handTrajectoryMessage.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      ObjectErrorType errorType;
      SE3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (handTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = handTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < handTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         SE3TrajectoryPointMessage waypoint = handTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateSE3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = handTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      errorType = ObjectValidityChecker.validateEnum(handTrajectoryMessage.getRobotSide());
      if (errorType != null)
      {
         errorMessage = "robotSide field " + errorType.getMessage();
         return errorMessage;
      }

      if (handTrajectoryMessage.useCustomControlFrame() && handTrajectoryMessage.controlFramePose == null)
      {
         String messageClassName = handTrajectoryMessage.getClass().getSimpleName();
         return "The control frame pose for " + messageClassName + " has to be set to be able to use it.";
      }

      return null;
   }

   public static String validateArmTrajectoryMessage(ArmTrajectoryMessage armTrajectoryMessage)
   {
      String errorMessage = validatePacket(armTrajectoryMessage, true);
      if (errorMessage != null)
         return ArmTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      ObjectErrorType packetFieldErrorType = ObjectValidityChecker.validateEnum(armTrajectoryMessage.robotSide);
      if (packetFieldErrorType != null)
      {
         String messageClassName = armTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s robotSide field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (armTrajectoryMessage.jointTrajectoryMessages == null)
      {
         String messageClassName = armTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s trajectory points are empty.";
         return errorMessage;
      }

      int numberOfJoints = armTrajectoryMessage.getNumberOfJoints();
      if (numberOfJoints == 0)
      {
         String messageClassName = armTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + " is empty.";
         return errorMessage;
      }

      for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
      {
         OneDoFJointTrajectoryMessage jointTrajectory1DMessage = armTrajectoryMessage.getJointTrajectoryPointList(jointIndex);
         errorMessage = validateOneJointTrajectoryMessage(jointTrajectory1DMessage, false);
         if (errorMessage != null)
         {
            String messageClassName = armTrajectoryMessage.getClass().getSimpleName();
            errorMessage = messageClassName + ": Error with the " + jointIndex + " " + OneDoFJointTrajectoryMessage.class.getSimpleName() + " : "
                  + errorMessage;
            return errorMessage;
         }
      }

      return null;
   }

   public static String validateHeadTrajectoryMessage(HeadTrajectoryMessage headTrajectoryMessage)
   {
      String errorMessage = validatePacket(headTrajectoryMessage, true);
      if (errorMessage != null)
         return HeadTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if(headTrajectoryMessage.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(headTrajectoryMessage.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      SO3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (headTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = headTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < headTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         SO3TrajectoryPointMessage waypoint = headTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateSO3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = headTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      return null;
   }

   public static String validateNeckTrajectoryMessage(NeckTrajectoryMessage neckTrajectoryMessage)
   {
      String errorMessage = validatePacket(neckTrajectoryMessage, true);
      if (errorMessage != null)
         return NeckTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if (neckTrajectoryMessage.jointTrajectoryMessages == null)
      {
         String messageClassName = neckTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s trajectory points are empty.";
         return errorMessage;
      }

      int numberOfJoints = neckTrajectoryMessage.getNumberOfJoints();
      if (numberOfJoints == 0)
      {
         String messageClassName = neckTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + " is empty.";
         return errorMessage;
      }

      for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
      {
         OneDoFJointTrajectoryMessage oneJointTrajectoryMessage = neckTrajectoryMessage.getJointTrajectoryPointList(jointIndex);
         errorMessage = validateOneJointTrajectoryMessage(oneJointTrajectoryMessage, false);
         if (errorMessage != null)
         {
            String messageClassName = neckTrajectoryMessage.getClass().getSimpleName();
            errorMessage = messageClassName + " Error with the " + jointIndex + " " + OneDoFJointTrajectoryMessage.class.getSimpleName() + " : " + errorMessage;
            return errorMessage;
         }
      }

      return null;
   }

   public static String validateJointspaceTrajectoryMessage(AbstractJointspaceTrajectoryMessage<?> message)
   {
      String errorMessage = validatePacket(message, true);
      if (errorMessage != null)
         return message.getClass().getSimpleName() + " " + errorMessage;

      if (message.jointTrajectoryMessages == null)
      {
         String messageClassName = message.getClass().getSimpleName();
         errorMessage = messageClassName + "'s trajectory points are empty.";
         return errorMessage;
      }

      int numberOfJoints = message.getNumberOfJoints();
      if (numberOfJoints == 0)
      {
         String messageClassName = message.getClass().getSimpleName();
         errorMessage = messageClassName + " is empty.";
         return errorMessage;
      }

      for (int jointIndex = 0; jointIndex < numberOfJoints; jointIndex++)
      {
         OneDoFJointTrajectoryMessage oneJointTrajectoryMessage = message.getTrajectoryPointLists()[jointIndex];
         if(oneJointTrajectoryMessage != null)
         {
            errorMessage = validateOneJointTrajectoryMessage(oneJointTrajectoryMessage, false);
         }
         if (errorMessage != null)
         {
            String messageClassName = message.getClass().getSimpleName();
            errorMessage = messageClassName + " Error with the " + jointIndex + " " + OneDoFJointTrajectoryMessage.class.getSimpleName() + " : " + errorMessage;
            return errorMessage;
         }
      }

      return null;
   }

   public static String validateSO3TrajectoryMessage(AbstractSO3TrajectoryMessage<?> message)
   {
      String errorMessage = validatePacket(message, true);
      if (errorMessage != null)
         return ChestTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      SO3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (message.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = message.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      if(message.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(message.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      for (int i = 0; i < message.getNumberOfTrajectoryPoints(); i++)
      {
         SO3TrajectoryPointMessage waypoint = message.getTrajectoryPoint(i);
         errorMessage = validateSO3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = message.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      if (message.useCustomControlFrame() && message.controlFramePose == null)
      {
         String messageClassName = message.getClass().getSimpleName();
         return "The control frame pose for " + messageClassName + " has to be set to be able to use it.";
      }

      return null;
   }

   public static String validatePelvisOrientationTrajectoryMessage(PelvisOrientationTrajectoryMessage pelvisOrientationTrajectoryMessage)
   {
      String errorMessage = validatePacket(pelvisOrientationTrajectoryMessage, true);
      if (errorMessage != null)
         return PelvisOrientationTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if(pelvisOrientationTrajectoryMessage.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return PelvisOrientationTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(pelvisOrientationTrajectoryMessage.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return PelvisOrientationTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      SO3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (pelvisOrientationTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = pelvisOrientationTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < pelvisOrientationTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         SO3TrajectoryPointMessage waypoint = pelvisOrientationTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateSO3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = pelvisOrientationTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      return null;
   }

   public static String validatePelvisTrajectoryMessage(PelvisTrajectoryMessage pelvisTrajectoryMessage)
   {
      String errorMessage = validatePacket(pelvisTrajectoryMessage, true);
      if (errorMessage != null)
         return PelvisTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if(pelvisTrajectoryMessage.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(pelvisTrajectoryMessage.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      SE3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (pelvisTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = pelvisTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < pelvisTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         SE3TrajectoryPointMessage waypoint = pelvisTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateSE3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = pelvisTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      if (pelvisTrajectoryMessage.useCustomControlFrame() && pelvisTrajectoryMessage.controlFramePose == null)
      {
         String messageClassName = pelvisTrajectoryMessage.getClass().getSimpleName();
         return "The control frame pose for " + messageClassName + " has to be set to be able to use it.";
      }

      return null;
   }

   public static String validateFootTrajectoryMessage(FootTrajectoryMessage footTrajectoryMessage)
   {
      String errorMessage = validatePacket(footTrajectoryMessage, true);
      if (errorMessage != null)
         return FootTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      if(footTrajectoryMessage.getFrameInformation().getDataReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Expressed In Reference Frame Id Not Set";
      }

      if(footTrajectoryMessage.getFrameInformation().getTrajectoryReferenceFrameId() == NameBasedHashCodeTools.NULL_HASHCODE)
      {
         return ChestTrajectoryMessage.class.getSimpleName() + " Trajectory Reference Frame Id Not Set";
      }

      ObjectErrorType errorType;
      SE3TrajectoryPointMessage previousTrajectoryPoint = null;

      if (footTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = footTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < footTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         SE3TrajectoryPointMessage waypoint = footTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateSE3TrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = footTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      errorType = ObjectValidityChecker.validateEnum(footTrajectoryMessage.getRobotSide());
      if (errorType != null)
      {
         String messageClassName = footTrajectoryMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s robotSide field " + errorType.getMessage();
         return errorMessage;
      }

      if (footTrajectoryMessage.useCustomControlFrame() && footTrajectoryMessage.controlFramePose == null)
      {
         String messageClassName = footTrajectoryMessage.getClass().getSimpleName();
         return "The control frame pose for " + messageClassName + " has to be set to be able to use it.";
      }

      return null;
   }

   public static String validateFootLoadBearingMessage(FootLoadBearingMessage message)
   {
      String errorMessage = validatePacket(message, true);
      if (errorMessage != null)
         return FootLoadBearingMessage.class.getSimpleName() + " " + errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateEnum(message.getRequest());
      if (errorType != null)
      {
         String messageClassName = message.getClass().getSimpleName();
         errorMessage = messageClassName + "'s request field " + errorType.getMessage();
         return errorMessage;
      }

      return null;
   }

   public static String validateGoHomeMessage(GoHomeMessage goHomeMessage)
   {
      String errorMessage = validatePacket(goHomeMessage, true);
      if (errorMessage != null)
         return GoHomeMessage.class.getSimpleName() + " " + errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateEnum(goHomeMessage.getBodyPart());
      if (errorType != null)
      {
         String messageClassName = goHomeMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s endEffector field " + errorType.getMessage();
         return errorMessage;
      }

      if (goHomeMessage.getBodyPart().isRobotSideNeeded())
      {
         errorType = ObjectValidityChecker.validateEnum(goHomeMessage.getRobotSide());
         if (goHomeMessage.getRobotSide() == null)
         {
            String messageClassName = goHomeMessage.getClass().getSimpleName();
            errorMessage = messageClassName + "'s robotSide field is null. It is required for the bodyPart " + goHomeMessage.getBodyPart();
            return errorMessage;
         }
      }

      return null;
   }

   public static String validatePelvisHeightTrajectoryMessage(PelvisHeightTrajectoryMessage pelvisHeightTrajectoryMessage)
   {
      String errorMessage = validatePacket(pelvisHeightTrajectoryMessage, true);
      if (errorMessage != null)
         return PelvisHeightTrajectoryMessage.class.getSimpleName() + " " + errorMessage;

      EuclideanTrajectoryPointMessage previousTrajectoryPoint = null;

      if (pelvisHeightTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
      {
         String messageClassName = pelvisHeightTrajectoryMessage.getClass().getSimpleName();
         errorMessage = "Received " + messageClassName + " with no waypoint.";
         return errorMessage;
      }

      for (int i = 0; i < pelvisHeightTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         EuclideanTrajectoryPointMessage waypoint = pelvisHeightTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateEuclideanTrajectoryPointMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
         {
            String messageClassName = pelvisHeightTrajectoryMessage.getClass().getSimpleName();
            errorMessage = "The " + messageClassName + "'s " + i + "th waypoint " + errorMessage;
            return errorMessage;
         }
         previousTrajectoryPoint = waypoint;
      }

      return null;
   }

   public static String validateArmDesiredAccelerationsMessage(ArmDesiredAccelerationsMessage armDesiredAccelerationsMessage)
   {
      String errorMessage = validatePacket(armDesiredAccelerationsMessage, true);
      if (errorMessage != null)
         return ArmDesiredAccelerationsMessage.class.getSimpleName() + " " + errorMessage;

      ObjectErrorType packetFieldErrorType = ObjectValidityChecker.validateEnum(armDesiredAccelerationsMessage.robotSide);
      if (packetFieldErrorType != null)
      {
         String messageClassName = armDesiredAccelerationsMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s robotSide field" + packetFieldErrorType.getMessage();
         return errorMessage;
      }

      if (armDesiredAccelerationsMessage.desiredJointAccelerations == null)
      {
         String messageClassName = armDesiredAccelerationsMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s field with desired joint acceleration is empty.";
         return errorMessage;
      }

      if (armDesiredAccelerationsMessage.getNumberOfJoints() == 0)
      {
         String messageClassName = armDesiredAccelerationsMessage.getClass().getSimpleName();
         errorMessage = messageClassName + "'s field with desired joint acceleration is empty.";
         return errorMessage;
      }

      return null;
   }

   private static String validateSE3TrajectoryPointMessage(SE3TrajectoryPointMessage se3TrajectoryPoint, SE3TrajectoryPointMessage previousSE3TrajectoryPoint,
                                                           boolean checkId)
   {
      String errorMessage = validatePacket(se3TrajectoryPoint, checkId);
      if (errorMessage != null)
         return errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateTuple3d(se3TrajectoryPoint.position);
      if (errorType != null)
         return "SE3 waypoint position field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateQuat4d(se3TrajectoryPoint.orientation);
      if (errorType != null)
         return "SE3 waypoint orientation field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateTuple3d(se3TrajectoryPoint.linearVelocity);
      if (errorType != null)
         return "SE3 waypoint linear velocity field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateTuple3d(se3TrajectoryPoint.angularVelocity);
      if (errorType != null)
         return "SE3 waypoint angular velocity field " + errorType.getMessage();
      ;

      double subTrajectoryTime = se3TrajectoryPoint.getTime();
      if (previousSE3TrajectoryPoint != null)
         subTrajectoryTime -= previousSE3TrajectoryPoint.getTime();

      errorType = ObjectValidityChecker.validateTrajectoryTime(subTrajectoryTime);
      if (errorType != null)
         return "SE3 waypoint time (relative to previous waypoint) " + errorType.getMessage();

      return null;
   }

   private static String validateEuclideanTrajectoryPointMessage(EuclideanTrajectoryPointMessage se3TrajectoryPoint,
         EuclideanTrajectoryPointMessage previousTrajectoryPoint, boolean checkId)
   {
      String errorMessage = validatePacket(se3TrajectoryPoint, checkId);
      if (errorMessage != null)
         return errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateTuple3d(se3TrajectoryPoint.position);
      if (errorType != null)
         return "SE3 waypoint position field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateTuple3d(se3TrajectoryPoint.linearVelocity);
      if (errorType != null)
         return "SE3 waypoint linear velocity field " + errorType.getMessage();

      double subTrajectoryTime = se3TrajectoryPoint.getTime();
      if (previousTrajectoryPoint != null)
         subTrajectoryTime -= previousTrajectoryPoint.getTime();

      errorType = ObjectValidityChecker.validateTrajectoryTime(subTrajectoryTime);
      if (errorType != null)
         return "SE3 waypoint time (relative to previous waypoint) " + errorType.getMessage();

      return null;
   }

   private static String validateSO3TrajectoryPointMessage(SO3TrajectoryPointMessage so3TrajectoryPoint, SO3TrajectoryPointMessage previousSO3TrajectoryPoint,
                                                           boolean checkId)
   {
      String errorMessage = validatePacket(so3TrajectoryPoint, checkId);
      if (errorMessage != null)
         return errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateQuat4d(so3TrajectoryPoint.orientation);
      if (errorType != null)
         return "SO3 waypoint orientation field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateTuple3d(so3TrajectoryPoint.angularVelocity);
      if (errorType != null)
         return "SO3 waypoint angular velocity field " + errorType.getMessage();

      double subTrajectoryTime = so3TrajectoryPoint.getTime();
      if (previousSO3TrajectoryPoint != null)
         subTrajectoryTime -= previousSO3TrajectoryPoint.getTime();

      errorType = ObjectValidityChecker.validateTrajectoryTime(subTrajectoryTime);
      if (errorType != null)
         return "SO3 waypoint time (relative to previous waypoint) " + errorType.getMessage();

      return null;
   }

   private static String validateTrajectoryPoint1DMessage(TrajectoryPoint1DMessage waypoint1D, TrajectoryPoint1DMessage previousTrajectoryPoint1D,
                                                          boolean checkId)
   {
      String errorMessage = validatePacket(waypoint1D, checkId);
      if (errorMessage != null)
         return errorMessage;

      ObjectErrorType errorType;

      errorType = ObjectValidityChecker.validateDouble(waypoint1D.getPosition());
      if (errorType != null)
         return "1D waypoint orientation field " + errorType.getMessage();

      errorType = ObjectValidityChecker.validateDouble(waypoint1D.getVelocity());
      if (errorType != null)
         return "1D waypoint angular velocity field " + errorType.getMessage();

      double subTrajectoryTime = waypoint1D.getTime();
      if (previousTrajectoryPoint1D != null)
         subTrajectoryTime -= previousTrajectoryPoint1D.getTime();

      errorType = ObjectValidityChecker.validateTrajectoryTime(subTrajectoryTime);
      if (errorType != null)
         return "1D waypoint time (relative to previous waypoint) " + errorType.getMessage();

      return null;
   }

   private final static double MAX_ACCEPTED_JOINT_VELOCITY = 100.0;

   public static String validateOneJointTrajectoryMessage(OneDoFJointTrajectoryMessage oneJointTrajectoryMessage, boolean checkId)
   {
      String errorMessage = validatePacket(oneJointTrajectoryMessage, checkId);
      if (errorMessage != null)
         return errorMessage;

      TrajectoryPoint1DMessage previousTrajectoryPoint = null;

      if (oneJointTrajectoryMessage.getNumberOfTrajectoryPoints() == 0)
         return "The joint trajectory message has no waypoint.";

      for (int i = 0; i < oneJointTrajectoryMessage.getNumberOfTrajectoryPoints(); i++)
      {
         TrajectoryPoint1DMessage waypoint = oneJointTrajectoryMessage.getTrajectoryPoint(i);
         errorMessage = validateTrajectoryPoint1DMessage(waypoint, previousTrajectoryPoint, false);
         if (errorMessage != null)
            return "The " + i + "th " + errorMessage;
         previousTrajectoryPoint = waypoint;
      }

      for (int waypointIndex = 0; waypointIndex < oneJointTrajectoryMessage.getNumberOfTrajectoryPoints(); waypointIndex++)
      {
         TrajectoryPoint1DMessage waypoint = oneJointTrajectoryMessage.getTrajectoryPoint(waypointIndex);
         double waypointPosition = waypoint.getPosition();

         if (Math.abs(waypointPosition) > Math.PI)
            return "The " + waypointIndex + "th waypoint position is unreasonable: " + waypointPosition;

         double waypointVelocity = waypoint.getVelocity();

         if (Math.abs(waypointVelocity) > MAX_ACCEPTED_JOINT_VELOCITY)
            return "The " + waypointIndex + "th waypoint velocity is unreasonable: " + waypointVelocity;
      }

      return null;
   }

   public static String validatePacket(Packet<?> packet, boolean checkId)
   {
      if (packet == null)
         return "is null.";
      if (checkId && packet.getUniqueId() == Packet.INVALID_MESSAGE_ID)
         return "invalid id.";
      return null;
   }

   public static String validateDesiredAccelerationsMessage(AbstractDesiredAccelerationsMessage<?> packet, boolean checkId)
   {
      if (packet == null)
         return "is null.";
      if (checkId && packet.getUniqueId() == Packet.INVALID_MESSAGE_ID)
         return "invalid id.";
      if(packet.getDesiredJointAccelerations() == null)
      {
         return "desired acceleration buffer null";
      }
      if(packet.getDesiredJointAccelerations().length  == 0)
      {
         return "desired acceleration buffer empty";
      }
      return null;
   }
}