package us.ihmc.commonWalkingControlModules.kinematics;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointVelocities;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.frames.CommonHumanoidReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.DampedLeastSquaresSolver;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.DesiredJointAccelerationCalculator;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class DesiredJointAccelerationCalculatorInWorldFrame
{
   private final YoVariableRegistry registry;
   private final RobotSide swingSide;
   private final SwingFullLegJacobian swingLegJacobian;
   private final FullRobotModel fullRobotModel;

   private final InverseDynamicsJoint rootJoint;
   private final ReferenceFrame footFrame;
   private final ReferenceFrame pelvisFrame;
   private final SpatialAccelerationVector accelerationOfFootWithRespectToPelvis = new SpatialAccelerationVector();
   private final DampedLeastSquaresSolver jacobianSolver;
   private final LegJointName[] legJointNames;
   private final LegJointVelocities jointVelocities;

   private final DesiredJointAccelerationCalculator desiredJointAccelerationCalculator;

   public DesiredJointAccelerationCalculatorInWorldFrame(LegJointName[] legJointNames, SwingFullLegJacobian swingLegJacobian, FullRobotModel fullRobotModel,
           CommonHumanoidReferenceFrames referenceFrames, RobotSide robotSide, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(robotSide.getCamelCaseNameForStartOfExpression() + getClass().getSimpleName());
      swingLegJacobian.getRobotSide().checkRobotSideMatch(robotSide);

      this.swingSide = swingLegJacobian.getRobotSide();

      this.swingLegJacobian = swingLegJacobian;
      this.fullRobotModel = fullRobotModel;

      this.rootJoint = fullRobotModel.getRootJoint();
      this.footFrame = fullRobotModel.getFoot(swingSide).getBodyFixedFrame();
      this.pelvisFrame = fullRobotModel.getPelvis().getBodyFixedFrame();
      this.jacobianSolver = new DampedLeastSquaresSolver(swingLegJacobian.getGeometricJacobian().getNumberOfColumns());
      this.desiredJointAccelerationCalculator = new DesiredJointAccelerationCalculator(swingLegJacobian.getGeometricJacobian(), jacobianSolver);
      parentRegistry.addChild(registry);
      
      this.legJointNames = legJointNames;
      jointVelocities = new LegJointVelocities(legJointNames, robotSide);
   }

   /**
    * Sets the accelerations for the RevoluteJoints in legJoints
    * Assumes that the swingLegJacobian is already updated
    * Assumes that the rootJoint's acceleration has already been set
    */
   public void compute(SpatialAccelerationVector desiredAccelerationOfFootWithRespectToWorld, double alpha)
   {
      computeDesiredAccelerationOfFootWithRespectToPelvis(accelerationOfFootWithRespectToPelvis, desiredAccelerationOfFootWithRespectToWorld);
      jacobianSolver.setAlpha(alpha);
//      jacobianSolver.setJacobian(swingLegJacobian.getJacobian());
      desiredJointAccelerationCalculator.compute(accelerationOfFootWithRespectToPelvis);
   }

   private final Twist twistOfPelvisWithRespectToElevator = new Twist();

   private void computeDesiredAccelerationOfFootWithRespectToPelvis(SpatialAccelerationVector accelerationOfFootWithRespectToPelvis, SpatialAccelerationVector desiredAccelerationOfFootWithRespectToElevator)
   {
      rootJoint.packDesiredJointAcceleration(accelerationOfFootWithRespectToPelvis);    // acceleration of pelvis after joint frame with respect to elevator
      accelerationOfFootWithRespectToPelvis.changeBodyFrameNoRelativeAcceleration(pelvisFrame);    // acceleration of pelvis body with respect to elevator
      accelerationOfFootWithRespectToPelvis.changeFrameNoRelativeMotion(pelvisFrame);

      Twist twistOfPelvisWithRespectToFoot = computeTwistOfPelvisWithRespectToFoot();

      rootJoint.packJointTwist(twistOfPelvisWithRespectToElevator);    // twist of pelvis after joint frame with respect to elevator
      twistOfPelvisWithRespectToElevator.changeBodyFrameNoRelativeTwist(pelvisFrame);    // twist of pelvis body with respect to elevator
      twistOfPelvisWithRespectToElevator.changeFrame(pelvisFrame);

      accelerationOfFootWithRespectToPelvis.changeFrame(footFrame, twistOfPelvisWithRespectToFoot, twistOfPelvisWithRespectToElevator);
      accelerationOfFootWithRespectToPelvis.invert();    // acceleration of elevator with respect to pelvis body
      accelerationOfFootWithRespectToPelvis.add(desiredAccelerationOfFootWithRespectToElevator);    // acceleration of foot with respect to pelvis body
   }

   private Twist computeTwistOfPelvisWithRespectToFoot()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         for (int i = 0; i < legJointNames.length; i++)
         {
            jointVelocities.setJointVelocity(legJointNames[i], fullRobotModel.getLegJoint(robotSide, legJointNames[i]).getQd());
         }
      }
//      LegJointVelocities jointVelocities = fullRobotModel.getLegJointVelocities(swingSide);
      Twist twistOfPelvisWithRespectToFoot = swingLegJacobian.getTwistOfFootWithRespectToPelvisInFootFrame(jointVelocities);    // twist of foot with respect to body
      twistOfPelvisWithRespectToFoot.invert();    // twist of body with respect to foot
      twistOfPelvisWithRespectToFoot.changeFrame(pelvisFrame);

      return twistOfPelvisWithRespectToFoot;
   }
}
