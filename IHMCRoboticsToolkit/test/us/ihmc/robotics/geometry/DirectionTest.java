package us.ihmc.robotics.geometry;

import org.junit.Test;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectionTest
{

	@EstimatedDuration(duration = 0.0)
	@Test(timeout = 30000)
   public void testDirection()
   {
      Direction xDirection = Direction.X;
      Direction yDirection = Direction.Y;
      Direction zDirection = Direction.Z;

      assertEquals(3, Direction.values().length);
      assertEquals(2, Direction.values2D().length);

      boolean touchedX = false;
      boolean touchedY = false;
      boolean touchedZ = false;

      for (Direction direction : Direction.values())
      {
         switch (direction)
         {
            case X :
               touchedX = true;

               break;

            case Y :
               touchedY = true;

               break;

            case Z :
               touchedZ = true;

               break;
         }
      }

      assertTrue(touchedX);
      assertTrue(touchedY);
      assertTrue(touchedZ);

      touchedX = false;
      touchedY = false;
      touchedZ = false;

      for (Direction direction : Direction.values2D())
      {
         switch (direction)
         {
            case X :
               touchedX = true;

               break;

            case Y :
               touchedY = true;

               break;

            case Z :
               touchedZ = true;

               break;
         }
      }

      assertTrue(touchedX);
      assertTrue(touchedY);
      assertFalse(touchedZ);

   }

}
