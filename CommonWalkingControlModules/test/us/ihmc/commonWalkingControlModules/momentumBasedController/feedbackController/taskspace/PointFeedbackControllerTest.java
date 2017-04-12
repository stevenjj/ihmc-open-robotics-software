package us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.taskspace;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.NormOps;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.inverseKinematics.RobotJointVelocityAccelerationIntegrator;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MotionQPInput;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MotionQPInputCalculator;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.convexOptimization.quadraticProgram.OASESConstrainedQPSolver;
import us.ihmc.convexOptimization.quadraticProgram.SimpleEfficientActiveSetQPSolver;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.controllers.PositionPIDGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RevoluteJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTestTools;
import us.ihmc.robotics.screwTheory.ScrewTestTools.RandomFloatingChain;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.TwistCalculator;

public final class PointFeedbackControllerTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testConvergence() throws Exception
   {
      Random random = new Random(5641654L);

      int numberOfJoints = 10;
      Vector3D[] jointAxes = new Vector3D[numberOfJoints];
      for (int i = 0; i < numberOfJoints; i++)
         jointAxes[i] = RandomGeometry.nextVector3D(random, 1.0);

      YoVariableRegistry registry = new YoVariableRegistry("Dummy");
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      List<RevoluteJoint> joints = randomFloatingChain.getRevoluteJoints();
      RigidBody elevator = randomFloatingChain.getElevator();
      RigidBody endEffector = joints.get(joints.size() - 1).getSuccessor();
      FramePoint bodyFixedPointToControl = FramePoint.generateRandomFramePoint(random, endEffector.getBodyFixedFrame(), 1.0, 1.0, 1.0);

      ScrewTestTools.setRandomPositions(joints, random);
      ScrewTestTools.setRandomVelocities(joints, random);
      joints.get(0).getPredecessor().updateFramesRecursively();
      FramePoint desiredPosition = new FramePoint();
      desiredPosition.setIncludingFrame(bodyFixedPointToControl);
      desiredPosition.changeFrame(worldFrame);
      ScrewTestTools.setRandomPositions(joints, random);
      ScrewTestTools.setRandomVelocities(joints, random);
      joints.get(0).getPredecessor().updateFramesRecursively();

      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, elevator);
      twistCalculator.compute();
      InverseDynamicsJoint[] jointsToOptimizeFor = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      double controlDT = 0.004;

      WholeBodyControlCoreToolbox toolbox = new WholeBodyControlCoreToolbox(controlDT, 0.0, null, jointsToOptimizeFor, centerOfMassFrame, twistCalculator, null,
                                                                            null, registry);
      toolbox.setupForInverseDynamicsSolver(null);
      FeedbackControllerToolbox feedbackControllerToolbox = new FeedbackControllerToolbox(registry);
      PointFeedbackController pointFeedbackController = new PointFeedbackController(endEffector, toolbox, feedbackControllerToolbox, registry);

      PointFeedbackControlCommand pointFeedbackControlCommand = new PointFeedbackControlCommand();
      pointFeedbackControlCommand.set(elevator, endEffector);
      PositionPIDGains gains = new PositionPIDGains();
      gains.setGains(100.0, 50.0);
      pointFeedbackControlCommand.setGains(gains);
      pointFeedbackControlCommand.setBodyFixedPointToControl(bodyFixedPointToControl);
      pointFeedbackControlCommand.set(desiredPosition, new FrameVector(worldFrame), new FrameVector(worldFrame));
      pointFeedbackController.submitFeedbackControlCommand(pointFeedbackControlCommand);
      pointFeedbackController.setEnabled(true);

      int numberOfDoFs = ScrewTools.computeDegreesOfFreedom(jointsToOptimizeFor);
      MotionQPInput motionQPInput = new MotionQPInput(numberOfDoFs);
      LinearSolver<DenseMatrix64F> pseudoInverseSolver = LinearSolverFactory.pseudoInverse(true);
      DenseMatrix64F jInverse = new DenseMatrix64F(numberOfDoFs, 6);
      MotionQPInputCalculator motionQPInputCalculator = toolbox.getMotionQPInputCalculator();
      DenseMatrix64F jointAccelerations = new DenseMatrix64F(numberOfDoFs, 1);
      RobotJointVelocityAccelerationIntegrator integrator = new RobotJointVelocityAccelerationIntegrator(controlDT);

      FramePoint currentPosition = new FramePoint();
      FrameVector errorVector = new FrameVector();

      double previousErrorMagnitude = Double.POSITIVE_INFINITY;
      double errorMagnitude = previousErrorMagnitude;

      for (int i = 0; i < 100; i++)
      {
         twistCalculator.compute();

         pointFeedbackController.computeInverseDynamics();
         SpatialAccelerationCommand output = pointFeedbackController.getInverseDynamicsOutput();

         motionQPInputCalculator.convertSpatialAccelerationCommand(output, motionQPInput);
         pseudoInverseSolver.setA(motionQPInput.taskJacobian);
         pseudoInverseSolver.invert(jInverse);
         CommonOps.mult(jInverse, motionQPInput.taskObjective, jointAccelerations);

         integrator.integrateJointAccelerations(jointsToOptimizeFor, jointAccelerations);
         integrator.integrateJointVelocities(jointsToOptimizeFor, integrator.getJointVelocities());
         ScrewTools.setVelocities(jointsToOptimizeFor, integrator.getJointVelocities());
         ScrewTools.setJointPositions(jointsToOptimizeFor, integrator.getJointConfigurations());
         elevator.updateFramesRecursively();

         currentPosition.setIncludingFrame(bodyFixedPointToControl);
         currentPosition.changeFrame(worldFrame);
         errorVector.sub(desiredPosition, currentPosition);
         errorMagnitude = errorVector.length();
         boolean isErrorReducing = errorMagnitude < previousErrorMagnitude;
         assertTrue(isErrorReducing);
         previousErrorMagnitude = errorMagnitude;
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testConvergenceWithJerryQP() throws Exception
   {
      Random random = new Random(5641654L);

      int numberOfJoints = 10;
      Vector3D[] jointAxes = new Vector3D[numberOfJoints];
      for (int i = 0; i < numberOfJoints; i++)
         jointAxes[i] = RandomGeometry.nextVector3D(random, 1.0);

      YoVariableRegistry registry = new YoVariableRegistry("Dummy");
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      List<RevoluteJoint> joints = randomFloatingChain.getRevoluteJoints();
      RigidBody elevator = randomFloatingChain.getElevator();
      RigidBody endEffector = joints.get(joints.size() - 1).getSuccessor();
      FramePoint bodyFixedPointToControl = FramePoint.generateRandomFramePoint(random, endEffector.getBodyFixedFrame(), 1.0, 1.0, 1.0);

      ScrewTestTools.setRandomPositions(joints, random);
      ScrewTestTools.setRandomVelocities(joints, random);
      joints.get(0).getPredecessor().updateFramesRecursively();
      FramePoint desiredPosition = new FramePoint();
      desiredPosition.setIncludingFrame(bodyFixedPointToControl);
      desiredPosition.changeFrame(worldFrame);
      ScrewTestTools.setRandomPositions(joints, random);
      ScrewTestTools.setRandomVelocities(joints, random);
      joints.get(0).getPredecessor().updateFramesRecursively();

      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, elevator);
      twistCalculator.compute();
      InverseDynamicsJoint[] jointsToOptimizeFor = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      double controlDT = 0.004;

      WholeBodyControlCoreToolbox toolbox = new WholeBodyControlCoreToolbox(controlDT, 0.0, null, jointsToOptimizeFor, centerOfMassFrame, twistCalculator, null,
                                                                            null, registry);
      toolbox.setupForInverseDynamicsSolver(null);
      FeedbackControllerToolbox feedbackControllerToolbox = new FeedbackControllerToolbox(registry);
      PointFeedbackController pointFeedbackController = new PointFeedbackController(endEffector, toolbox, feedbackControllerToolbox, registry);

      PointFeedbackControlCommand pointFeedbackControlCommand = new PointFeedbackControlCommand();
      pointFeedbackControlCommand.set(elevator, endEffector);
      PositionPIDGains gains = new PositionPIDGains();
      gains.setGains(10.0, 5.0);
      pointFeedbackControlCommand.setGains(gains);
      pointFeedbackControlCommand.setBodyFixedPointToControl(bodyFixedPointToControl);
      pointFeedbackControlCommand.set(desiredPosition, new FrameVector(worldFrame), new FrameVector(worldFrame));
      pointFeedbackController.submitFeedbackControlCommand(pointFeedbackControlCommand);
      pointFeedbackController.setEnabled(true);

      int numberOfDoFs = ScrewTools.computeDegreesOfFreedom(jointsToOptimizeFor);
      MotionQPInput motionQPInput = new MotionQPInput(numberOfDoFs);
      LinearSolver<DenseMatrix64F> pseudoInverseSolver = LinearSolverFactory.pseudoInverse(true);
      DenseMatrix64F jInverse = new DenseMatrix64F(numberOfDoFs, 6);
      MotionQPInputCalculator motionQPInputCalculator = toolbox.getMotionQPInputCalculator();
      DenseMatrix64F jointAccelerations = new DenseMatrix64F(numberOfDoFs, 1);
      DenseMatrix64F jointAccelerationsFromJerryQP = new DenseMatrix64F(numberOfDoFs, 1);
      DenseMatrix64F jointAccelerationsFromQPOASES = new DenseMatrix64F(numberOfDoFs, 1);
      RobotJointVelocityAccelerationIntegrator integrator = new RobotJointVelocityAccelerationIntegrator(controlDT);

      SimpleEfficientActiveSetQPSolver jerryQPSolver = new SimpleEfficientActiveSetQPSolver();
      OASESConstrainedQPSolver oasesQPSolver = new OASESConstrainedQPSolver(registry);

      DenseMatrix64F solverInput_H = new DenseMatrix64F(numberOfDoFs, numberOfDoFs);
      DenseMatrix64F solverInput_f = new DenseMatrix64F(numberOfDoFs, 1);
      DenseMatrix64F solverInput_Aeq = new DenseMatrix64F(0, numberOfDoFs);
      DenseMatrix64F solverInput_beq = new DenseMatrix64F(0, 1);
      DenseMatrix64F solverInput_Ain = new DenseMatrix64F(0, numberOfDoFs);
      DenseMatrix64F solverInput_bin = new DenseMatrix64F(0, 1);
      DenseMatrix64F solverInput_lb = new DenseMatrix64F(numberOfDoFs, 1);
      DenseMatrix64F solverInput_ub = new DenseMatrix64F(numberOfDoFs, 1);
      CommonOps.fill(solverInput_lb, Double.NEGATIVE_INFINITY);
      CommonOps.fill(solverInput_ub, Double.POSITIVE_INFINITY);

      DenseMatrix64F tempJtW = new DenseMatrix64F(numberOfDoFs, 3);

      FramePoint currentPosition = new FramePoint();
      FrameVector errorVector = new FrameVector();

      double previousErrorMagnitude = Double.POSITIVE_INFINITY;
      double errorMagnitude = previousErrorMagnitude;

      for (int i = 0; i < 100; i++)
      {
         twistCalculator.compute();

         pointFeedbackController.computeInverseDynamics();
         SpatialAccelerationCommand output = pointFeedbackController.getInverseDynamicsOutput();
         motionQPInputCalculator.convertSpatialAccelerationCommand(output, motionQPInput);

         MatrixTools.scaleTranspose(1.0, motionQPInput.taskJacobian, tempJtW); // J^T W
         CommonOps.mult(tempJtW, motionQPInput.taskJacobian, solverInput_H); // H = J^T W J
         CommonOps.mult(tempJtW, motionQPInput.taskObjective, solverInput_f); // f = - J^T W xDDot
         CommonOps.scale(-1.0, solverInput_f);

         for (int diag = 0; diag < numberOfDoFs; diag++)
            solverInput_H.add(diag, diag, 1e-8);

         jerryQPSolver.clear();
         jerryQPSolver.setQuadraticCostFunction(solverInput_H, solverInput_f, 0.0);
         jerryQPSolver.solve(jointAccelerationsFromJerryQP, new DenseMatrix64F(1, 1), new DenseMatrix64F(1, 1));
         oasesQPSolver.solve(solverInput_H, solverInput_f, solverInput_Aeq, solverInput_beq, solverInput_Ain, solverInput_bin, solverInput_lb, solverInput_ub,
                             jointAccelerationsFromQPOASES, true);

         pseudoInverseSolver.setA(motionQPInput.taskJacobian);
         pseudoInverseSolver.invert(jInverse);
         CommonOps.mult(jInverse, motionQPInput.taskObjective, jointAccelerations);

         integrator.integrateJointAccelerations(jointsToOptimizeFor, jointAccelerationsFromJerryQP);
         integrator.integrateJointVelocities(jointsToOptimizeFor, integrator.getJointVelocities());
         ScrewTools.setVelocities(jointsToOptimizeFor, integrator.getJointVelocities());
         ScrewTools.setJointPositions(jointsToOptimizeFor, integrator.getJointConfigurations());
         elevator.updateFramesRecursively();

         assertArrayEquals(jointAccelerations.data, jointAccelerationsFromJerryQP.data, 1.0e-5);
         assertArrayEquals(jointAccelerations.data, jointAccelerationsFromQPOASES.data, 2.0e-1);

         currentPosition.setIncludingFrame(bodyFixedPointToControl);
         currentPosition.changeFrame(worldFrame);
         errorVector.sub(desiredPosition, currentPosition);
         errorMagnitude = errorVector.length();
         boolean isErrorReducing = errorMagnitude < previousErrorMagnitude;
         assertTrue(isErrorReducing);
         previousErrorMagnitude = errorMagnitude;
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.5)
   @Test(timeout = 30000)
   public void testCompareAgainstSpatialController() throws Exception
   {
      Random random = new Random(5641654L);

      YoVariableRegistry registry = new YoVariableRegistry("Dummy");
      int numberOfRevoluteJoints = 10;
      RandomFloatingChain randomFloatingChain = new RandomFloatingChain(random, numberOfRevoluteJoints);
      List<RevoluteJoint> joints = randomFloatingChain.getRevoluteJoints();
      RigidBody elevator = randomFloatingChain.getElevator();
      RigidBody endEffector = joints.get(joints.size() - 1).getSuccessor();

      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, elevator);
      InverseDynamicsJoint[] jointsToOptimizeFor = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      double controlDT = 0.004;

      WholeBodyControlCoreToolbox toolbox = new WholeBodyControlCoreToolbox(controlDT, 0.0, null, jointsToOptimizeFor, centerOfMassFrame, twistCalculator, null,
                                                                            null, registry);
      toolbox.setupForInverseDynamicsSolver(null);
      // Making the controllers to run with different instances of the toolbox so they don't share variables.
      PointFeedbackController pointFeedbackController = new PointFeedbackController(endEffector, toolbox, new FeedbackControllerToolbox(new YoVariableRegistry("Dummy")), registry);
      SpatialFeedbackController spatialFeedbackController = new SpatialFeedbackController(endEffector, toolbox, new FeedbackControllerToolbox(new YoVariableRegistry("Dummy")), registry);
      pointFeedbackController.setEnabled(true);
      spatialFeedbackController.setEnabled(true);

      PointFeedbackControlCommand pointFeedbackControlCommand = new PointFeedbackControlCommand();
      pointFeedbackControlCommand.set(elevator, endEffector);
      PositionPIDGains positionGains = new PositionPIDGains();


      SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();
      spatialFeedbackControlCommand.set(elevator, endEffector);
      spatialFeedbackControlCommand.getSpatialAccelerationCommand().setSelectionMatrixForLinearControl();

      MotionQPInputCalculator motionQPInputCalculator = new MotionQPInputCalculator(centerOfMassFrame, twistCalculator, toolbox.getJointIndexHandler(), null,
                                                                                    registry);
      MotionQPInput pointMotionQPInput = new MotionQPInput(toolbox.getJointIndexHandler().getNumberOfDoFs());
      MotionQPInput spatialMotionQPInput = new MotionQPInput(toolbox.getJointIndexHandler().getNumberOfDoFs());

      SpatialAccelerationCommand pointControllerOutput = pointFeedbackController.getInverseDynamicsOutput();
      SpatialAccelerationCommand spatialControllerOutput = spatialFeedbackController.getInverseDynamicsOutput();

      for (int i = 0; i < 300; i++)
      {
         ScrewTestTools.setRandomPositions(joints, random);
         ScrewTestTools.setRandomVelocities(joints, random);
         joints.get(0).getPredecessor().updateFramesRecursively();
         centerOfMassFrame.update();
         twistCalculator.compute();

         double proportionalGain = RandomNumbers.nextDouble(random, 10.0, 200.0);
         double derivativeGain = RandomNumbers.nextDouble(random, 0.0, 100.0);
         double integralGain = RandomNumbers.nextDouble(random, 0.0, 100.0);
         double maxIntegralError = RandomNumbers.nextDouble(random, 0.0, 10.0);
         positionGains.setGains(proportionalGain, derivativeGain, integralGain, maxIntegralError);
         positionGains.setMaximumError(RandomNumbers.nextDouble(random, 0.0, 10.0));
         positionGains.setMaximumVelocityError(RandomNumbers.nextDouble(random, 0.0, 10.0));
         positionGains.setMaximumFeedbackAndFeedbackRate(RandomNumbers.nextDouble(random, 0.1, 10.0), RandomNumbers.nextDouble(random, 0.1, 10.0)); 
         pointFeedbackControlCommand.setGains(positionGains);
         spatialFeedbackControlCommand.setGains(positionGains);

         FramePoint bodyFixedPointToControl = FramePoint.generateRandomFramePoint(random, endEffector.getBodyFixedFrame(), 1.0, 1.0, 1.0);
         FramePoint desiredPosition = new FramePoint(worldFrame, EuclidCoreRandomTools.generateRandomVector3D(random, -10.0, 10.0));
         FrameVector desiredLinearVelocity = new FrameVector(worldFrame, EuclidCoreRandomTools.generateRandomVector3D(random, -10.0, 10.0));
         FrameVector feedForwardLinearAcceleration = new FrameVector(worldFrame, EuclidCoreRandomTools.generateRandomVector3D(random, -10.0, 10.0));

         pointFeedbackControlCommand.setBodyFixedPointToControl(bodyFixedPointToControl);
         spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(bodyFixedPointToControl);

         pointFeedbackControlCommand.set(desiredPosition, desiredLinearVelocity, feedForwardLinearAcceleration);
         spatialFeedbackControlCommand.set(desiredPosition, desiredLinearVelocity, feedForwardLinearAcceleration);

         spatialFeedbackController.submitFeedbackControlCommand(spatialFeedbackControlCommand);
         pointFeedbackController.submitFeedbackControlCommand(pointFeedbackControlCommand);

         spatialFeedbackController.computeInverseDynamics();
         pointFeedbackController.computeInverseDynamics();

         motionQPInputCalculator.convertSpatialAccelerationCommand(pointControllerOutput, pointMotionQPInput);
         motionQPInputCalculator.convertSpatialAccelerationCommand(spatialControllerOutput, spatialMotionQPInput);

         DenseMatrix64F pointDesiredAcceleration = new DenseMatrix64F(3, 1);
         DenseMatrix64F spatialDesiredAcceleration = new DenseMatrix64F(3, 1);
         pointControllerOutput.getDesiredSpatialAcceleration(pointDesiredAcceleration);
         spatialControllerOutput.getDesiredSpatialAcceleration(spatialDesiredAcceleration);

         assertEquals(spatialDesiredAcceleration, pointDesiredAcceleration, 1.0e-12);

         assertEquals(spatialMotionQPInput.taskJacobian, pointMotionQPInput.taskJacobian, 1.0e-12);
         assertEquals(spatialMotionQPInput.taskObjective, pointMotionQPInput.taskObjective, 1.0e-12);
         assertEquals(spatialMotionQPInput.taskWeightMatrix, pointMotionQPInput.taskWeightMatrix, 1.0e-12);
      }
   }

   private static void assertEquals(DenseMatrix64F expected, DenseMatrix64F actual, double epsilon)
   {
      assertTrue(assertErrorMessage(expected, actual), MatrixFeatures.isEquals(expected, actual, epsilon));
   }

   private static String assertErrorMessage(DenseMatrix64F expected, DenseMatrix64F actual)
   {
      DenseMatrix64F diff = new DenseMatrix64F(expected.getNumRows(), expected.getNumCols());
      CommonOps.subtract(expected, actual, diff);
      return "Expected:\n" + expected + "\nActual:\n" + actual + ", difference: " + NormOps.normP2(diff);
   }
}
