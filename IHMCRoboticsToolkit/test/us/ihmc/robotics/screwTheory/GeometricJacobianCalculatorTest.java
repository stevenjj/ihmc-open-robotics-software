package us.ihmc.robotics.screwTheory;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.ScrewTestTools.RandomFloatingChain;

public class GeometricJacobianCalculatorTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testBasicFeatures() throws Exception
   {
      Random random = new Random(435435L);
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      verifyThatHasBeenCleared(jacobianCalculator);

      // Setting random information to make sure it is being saved in the calculator.
      int numberOfJoints = 10;
      List<RevoluteJoint> joints = ScrewTestTools.createRandomChainRobot(numberOfJoints, random);
      ScrewTestTools.setRandomPositions(joints, random);
      ScrewTestTools.setRandomVelocities(joints, random);

      jacobianCalculator.setKinematicChain(joints.get(0).getPredecessor(), joints.get(numberOfJoints - 1).getSuccessor());

      jacobianCalculator.computeJacobianMatrix();
      jacobianCalculator.computeConvectiveTerm();

      { // Just checking the matrix sizing is correct
         DenseMatrix64F jacobianMatrix = new DenseMatrix64F(1, 1);
         jacobianCalculator.getJacobianMatrix(jacobianMatrix);
         assertEquals(6, jacobianMatrix.getNumRows());
         assertEquals(numberOfJoints, jacobianMatrix.getNumCols());

         DenseMatrix64F convectiveTerm = new DenseMatrix64F(1, 1);
         jacobianCalculator.getConvectiveTerm(convectiveTerm);
         assertEquals(6, convectiveTerm.getNumRows());
         assertEquals(1, convectiveTerm.getNumCols());

         DenseMatrix64F selectionMatrix = CommonOps.identity(6);
         MatrixTools.removeRow(selectionMatrix, random.nextInt(6));

         jacobianCalculator.getJacobianMatrix(selectionMatrix, jacobianMatrix);
         assertEquals(5, jacobianMatrix.getNumRows());
         assertEquals(numberOfJoints, jacobianMatrix.getNumCols());

         jacobianCalculator.getConvectiveTerm(selectionMatrix, convectiveTerm);
         assertEquals(5, convectiveTerm.getNumRows());
         assertEquals(1, convectiveTerm.getNumCols());
      }

      jacobianCalculator.clear();

      verifyThatHasBeenCleared(jacobianCalculator);
   }

   public void verifyThatHasBeenCleared(GeometricJacobianCalculator jacobianCalculator)
   {
      {// Simply checking that it is empty
         assertNull(jacobianCalculator.getBase());
         assertNull(jacobianCalculator.getEndEffector());
         assertNull(jacobianCalculator.getJacobianFrame());
         assertTrue(jacobianCalculator.getJointsFromBaseToEndEffector().isEmpty());
         assertEquals(-1, jacobianCalculator.getNumberOfDegreesOfFreedom());
      }

      // Try some exceptions
      try
      {
         jacobianCalculator.computeJacobianMatrix();
         fail("Should have thrown a " + RuntimeException.class.getSimpleName());
      }
      catch (RuntimeException e)
      {
         // good
      }
      try
      {
         jacobianCalculator.computeConvectiveTerm();
         fail("Should have thrown a " + RuntimeException.class.getSimpleName());
      }
      catch (RuntimeException e)
      {
         // good
      }
      try
      {
         jacobianCalculator.getJacobianMatrix(new DenseMatrix64F(10, 10));
         fail("Should have thrown a " + RuntimeException.class.getSimpleName());
      }
      catch (RuntimeException e)
      {
         // good
      }
      try
      {
         jacobianCalculator.getConvectiveTerm(new DenseMatrix64F(10, 10));
         fail("Should have thrown a " + RuntimeException.class.getSimpleName());
      }
      catch (RuntimeException e)
      {
         // good
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstTwistCalculatorChainRobot() throws Exception
   {
      Random random = new Random(4324342L);

      int numberOfJoints = random.nextInt(100);

      List<OneDoFJoint> joints = ScrewTestTools.createRandomChainRobotWithOneDoFJoints(numberOfJoints, random);
      RigidBody rootBody = ScrewTools.getRootBody(joints.get(0).getSuccessor());
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());

      Twist expectedTwist = new Twist();
      Twist actualTwist = new Twist();
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      for (int i = 0; i < 1000; i++)
      {
         ScrewTestTools.setRandomPositions(joints, random);
         ScrewTestTools.setRandomVelocities(joints, random, -10.0, 10.0);
         twistCalculator.compute();

         int randomEndEffectorIndex = random.nextInt(numberOfJoints);
         RigidBody randomEndEffector = joints.get(randomEndEffectorIndex).getSuccessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(rootBody, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();

         DenseMatrix64F jointVelocitiesMatrix = new DenseMatrix64F(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getJointVelocitiesMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointVelocitiesMatrix);

         twistCalculator.getRelativeTwist(rootBody, randomEndEffector, expectedTwist);
         jacobianCalculator.getEndEffectorTwist(jointVelocitiesMatrix, actualTwist);

         TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-12);

         RigidBody randomBase = joints.get(random.nextInt(randomEndEffectorIndex + 1)).getPredecessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(randomBase, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();

         jointVelocitiesMatrix.reshape(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getJointVelocitiesMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointVelocitiesMatrix);

         twistCalculator.getRelativeTwist(randomBase, randomEndEffector, expectedTwist);
         jacobianCalculator.getEndEffectorTwist(jointVelocitiesMatrix, actualTwist);

         TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-12);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.1)
   @Test(timeout = 30000)
   public void testConvectiveTerm() throws Exception
   {
      Random random = new Random(345345L);

      int numberOfRevoluteJoints = 100;
      RandomFloatingChain floatingChain = new RandomFloatingChain(random, numberOfRevoluteJoints);
      SixDoFJoint floatingJoint = floatingChain.getRootJoint();
      List<RevoluteJoint> revoluteJoints = floatingChain.getRevoluteJoints();
      List<InverseDynamicsJoint> joints = floatingChain.getInverseDynamicsJoints();
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      for (int i = 0; i < 1000; i++)
      {
         ScrewTestTools.setRandomPositionAndOrientation(floatingJoint, random);
         ScrewTestTools.setRandomVelocity(floatingJoint, random);
         ScrewTestTools.setRandomAcceleration(floatingJoint, random);

         ScrewTestTools.setRandomPositions(revoluteJoints, random, -10.0, 10.0);
         ScrewTestTools.setRandomVelocities(revoluteJoints, random, -1.0, 1.0);
         ScrewTestTools.setRandomAccelerations(revoluteJoints, random, -10.0, 10.0);

         RigidBody body = joints.get(0).getPredecessor();
         RigidBody rootBody = ScrewTools.getRootBody(body);
         SpatialAccelerationVector rootAcceleration = new SpatialAccelerationVector(rootBody.getBodyFixedFrame(), worldFrame, rootBody.getBodyFixedFrame());
         TwistCalculator twistCalculator = new TwistCalculator(worldFrame, body);
         SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(body, worldFrame, rootAcceleration, twistCalculator,
                                                                                                         true, false, false);

         twistCalculator.compute();
         spatialAccelerationCalculator.compute();

         int randomEndEffectorIndex = random.nextInt(numberOfRevoluteJoints + 1);
         RigidBody randomEndEffector = joints.get(randomEndEffectorIndex).getSuccessor();
         RigidBody randomBase = joints.get(random.nextInt(randomEndEffectorIndex + 1)).getPredecessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(randomBase, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeConvectiveTerm();
         SpatialAccelerationVector actualConvectiveTerm = new SpatialAccelerationVector();
         jacobianCalculator.getConvectiveTerm(actualConvectiveTerm);

         SpatialAccelerationVector expectedConvectiveTerm = new SpatialAccelerationVector();
         spatialAccelerationCalculator.getRelativeAcceleration(randomBase, randomEndEffector, expectedConvectiveTerm);

         SpatialMotionVectorTest.assertSpatialMotionVectorEquals(expectedConvectiveTerm, actualConvectiveTerm, 1.0e-12);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.2)
   @Test(timeout = 30000)
   public void testAgainstSpatialAccelerationCalculatorChainRobot() throws Exception
   {
      Random random = new Random(4324342L);

      int numberOfJoints = random.nextInt(100);

      List<OneDoFJoint> joints = ScrewTestTools.createRandomChainRobotWithOneDoFJoints(numberOfJoints, random);
      RigidBody rootBody = ScrewTools.getRootBody(joints.get(0).getSuccessor());
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());
      SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(rootBody, twistCalculator, 0.0, true);

      SpatialAccelerationVector expectedAcceleration = new SpatialAccelerationVector();
      SpatialAccelerationVector actualAcceleration = new SpatialAccelerationVector();
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      for (int i = 0; i < 1000; i++)
      {
         ScrewTestTools.setRandomPositions(joints, random);
         ScrewTestTools.setRandomVelocities(joints, random, -10.0, 10.0);
         ScrewTestTools.setRandomDesiredAccelerations(joints, random, -10.0, 10.0);
         twistCalculator.compute();
         spatialAccelerationCalculator.compute();

         int randomEndEffectorIndex = random.nextInt(numberOfJoints);
         RigidBody randomEndEffector = joints.get(randomEndEffectorIndex).getSuccessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(rootBody, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();
         jacobianCalculator.computeConvectiveTerm();

         DenseMatrix64F jointDesiredAccelerationsMatrix = new DenseMatrix64F(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getDesiredJointAccelerationsMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointDesiredAccelerationsMatrix);

         spatialAccelerationCalculator.getRelativeAcceleration(rootBody, randomEndEffector, expectedAcceleration);
         jacobianCalculator.getEndEffectorAcceleration(jointDesiredAccelerationsMatrix, actualAcceleration);

         SpatialAccelerationCalculatorTest.assertSpatialAccelerationVectorEquals(expectedAcceleration, actualAcceleration, 1.0e-10);

         RigidBody randomBase = joints.get(random.nextInt(randomEndEffectorIndex + 1)).getPredecessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(randomBase, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();
         jacobianCalculator.computeConvectiveTerm();

         jointDesiredAccelerationsMatrix.reshape(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getDesiredJointAccelerationsMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointDesiredAccelerationsMatrix);

         spatialAccelerationCalculator.getRelativeAcceleration(randomBase, randomEndEffector, expectedAcceleration);
         jacobianCalculator.getEndEffectorAcceleration(jointDesiredAccelerationsMatrix, actualAcceleration);

         SpatialAccelerationCalculatorTest.assertSpatialAccelerationVectorEquals(expectedAcceleration, actualAcceleration, 1.0e-10);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstTwistCalculatorFloatingJointRobot() throws Exception
   {
      Random random = new Random(4324342L);

      int numberOfJoints = random.nextInt(100);

      RandomFloatingChain floatingChain = new RandomFloatingChain(random, numberOfJoints);
      SixDoFJoint floatingJoint = floatingChain.getRootJoint();
      List<RevoluteJoint> revoluteJoints = floatingChain.getRevoluteJoints();
      List<InverseDynamicsJoint> joints = floatingChain.getInverseDynamicsJoints();

      RigidBody rootBody = ScrewTools.getRootBody(joints.get(0).getSuccessor());
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());

      Twist expectedTwist = new Twist();
      Twist actualTwist = new Twist();
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      for (int i = 0; i < 1000; i++)
      {
         ScrewTestTools.setRandomPositionAndOrientation(floatingJoint, random);
         ScrewTestTools.setRandomVelocity(floatingJoint, random);
         ScrewTestTools.setRandomPositions(revoluteJoints, random);
         ScrewTestTools.setRandomVelocities(revoluteJoints, random, -10.0, 10.0);
         twistCalculator.compute();

         int randomEndEffectorIndex = random.nextInt(numberOfJoints);
         RigidBody randomEndEffector = joints.get(randomEndEffectorIndex).getSuccessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(rootBody, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();

         DenseMatrix64F jointVelocitiesMatrix = new DenseMatrix64F(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getJointVelocitiesMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointVelocitiesMatrix);

         twistCalculator.getRelativeTwist(rootBody, randomEndEffector, expectedTwist);
         jacobianCalculator.getEndEffectorTwist(jointVelocitiesMatrix, actualTwist);

         TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-12);

         RigidBody randomBase = joints.get(random.nextInt(randomEndEffectorIndex + 1)).getPredecessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(randomBase, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();

         jointVelocitiesMatrix.reshape(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getJointVelocitiesMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointVelocitiesMatrix);

         twistCalculator.getRelativeTwist(randomBase, randomEndEffector, expectedTwist);
         jacobianCalculator.getEndEffectorTwist(jointVelocitiesMatrix, actualTwist);

         TwistCalculatorTest.assertTwistEquals(expectedTwist, actualTwist, 1.0e-12);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAgainstSpatialAccelerationCalculatorFloatingJointRobot() throws Exception
   {
      Random random = new Random(4324342L);

      int numberOfJoints = random.nextInt(100);

      RandomFloatingChain floatingChain = new RandomFloatingChain(random, numberOfJoints);
      SixDoFJoint floatingJoint = floatingChain.getRootJoint();
      List<RevoluteJoint> revoluteJoints = floatingChain.getRevoluteJoints();
      List<InverseDynamicsJoint> joints = floatingChain.getInverseDynamicsJoints();

      RigidBody rootBody = ScrewTools.getRootBody(joints.get(0).getSuccessor());
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, joints.get(0).getPredecessor());
      SpatialAccelerationCalculator spatialAccelerationCalculator = new SpatialAccelerationCalculator(rootBody, twistCalculator, 0.0, true);

      SpatialAccelerationVector expectedAcceleration = new SpatialAccelerationVector();
      SpatialAccelerationVector actualAcceleration = new SpatialAccelerationVector();
      GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();

      for (int i = 0; i < 1000; i++)
      {
         ScrewTestTools.setRandomPositionAndOrientation(floatingJoint, random);
         ScrewTestTools.setRandomVelocity(floatingJoint, random);
         ScrewTestTools.setRandomPositions(revoluteJoints, random);
         ScrewTestTools.setRandomVelocities(revoluteJoints, random, -10.0, 10.0);
         twistCalculator.compute();
         spatialAccelerationCalculator.compute();

         int randomEndEffectorIndex = random.nextInt(numberOfJoints);
         RigidBody randomEndEffector = joints.get(randomEndEffectorIndex).getSuccessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(rootBody, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();
         jacobianCalculator.computeConvectiveTerm();

         DenseMatrix64F jointDesiredAccelerationsMatrix = new DenseMatrix64F(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getDesiredJointAccelerationsMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointDesiredAccelerationsMatrix);

         spatialAccelerationCalculator.getRelativeAcceleration(rootBody, randomEndEffector, expectedAcceleration);
         jacobianCalculator.getEndEffectorAcceleration(jointDesiredAccelerationsMatrix, actualAcceleration);

         SpatialAccelerationCalculatorTest.assertSpatialAccelerationVectorEquals(expectedAcceleration, actualAcceleration, 1.0e-10);

         RigidBody randomBase = joints.get(random.nextInt(randomEndEffectorIndex + 1)).getPredecessor();
         jacobianCalculator.clear();
         jacobianCalculator.setKinematicChain(randomBase, randomEndEffector);
         jacobianCalculator.setJacobianFrame(randomEndEffector.getBodyFixedFrame());
         jacobianCalculator.computeJacobianMatrix();
         jacobianCalculator.computeConvectiveTerm();

         jointDesiredAccelerationsMatrix.reshape(jacobianCalculator.getNumberOfDegreesOfFreedom(), 1);
         ScrewTools.getDesiredJointAccelerationsMatrix(jacobianCalculator.getJointsFromBaseToEndEffector(), jointDesiredAccelerationsMatrix);

         spatialAccelerationCalculator.getRelativeAcceleration(randomBase, randomEndEffector, expectedAcceleration);
         jacobianCalculator.getEndEffectorAcceleration(jointDesiredAccelerationsMatrix, actualAcceleration);

         SpatialAccelerationCalculatorTest.assertSpatialAccelerationVectorEquals(expectedAcceleration, actualAcceleration, 1.0e-10);
      }
   }
}
