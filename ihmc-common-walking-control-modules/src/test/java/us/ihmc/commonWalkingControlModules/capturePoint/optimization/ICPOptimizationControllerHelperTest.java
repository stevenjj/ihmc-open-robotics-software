package us.ihmc.commonWalkingControlModules.capturePoint.optimization;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST})
public class ICPOptimizationControllerHelperTest
{
   @After
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testTransformFromDynamicsFrame()
   {
      ICPOptimizationControllerHelper helper = new ICPOptimizationControllerHelper();

      FrameVector2D icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.1, 0.1);
      FrameVector2D gainsToPack = new FrameVector2D(ReferenceFrame.getWorldFrame());
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      double expectedX = 2.0;
      double expectedY = 2.0;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);

      icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.1, 0.2);
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      expectedX = 2.3;
      expectedY = 1.7;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);

      icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.2, 0.1);
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      expectedX = 1.7;
      expectedY = 2.3;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);

      icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.1, 0.0);
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      expectedX = 1.5;
      expectedY = 2.5;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);

      icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.0, 0.1);
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      expectedX = 2.5;
      expectedY = 1.5;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);

      icpVelocity = new FrameVector2D(ReferenceFrame.getWorldFrame(), 0.0, 0.0);
      helper.transformFromDynamicsFrame(gainsToPack, icpVelocity, 1.5, 2.5);
      expectedX = 2.5;
      expectedY = 2.5;

      Assert.assertEquals(expectedX, gainsToPack.getX(), 1e-7);
      Assert.assertEquals(expectedY, gainsToPack.getY(), 1e-7);
   }

}
