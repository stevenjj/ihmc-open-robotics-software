package us.ihmc.simulationConstructionSetTools.util.environments.planarRegionEnvironments;

import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.robotics.geometry.PlanarRegionsListGenerator;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class CinderBlockFieldPlanarRegionEnvironment extends PlanarRegionEnvironmentInterface
{
   private enum BlockOrientation {NORTH, SOUTH, EAST, WEST}

   private static final double cinderBlockTiltDegrees = 15;
   private static final double cinderBlockTiltRadians = Math.toRadians(cinderBlockTiltDegrees);

   private static final double cinderBlockLength = 0.40; // 40 cm (approx 16 in, just less than 16in)
   private static final double flatCinderBlockHeight = 0.08; // 15 cm (approx 6 in, less than 6 in, but consistent with other cm measurements)
   private static final double cinderBlockHeight = 0.15; // 15 cm (approx 6 in, less than 6 in, but consistent with other cm measurements)

   private static final double distanceToFirstCinderBlock = 0.75;
   private static final double gapBetweenCinderBlockSets = 1.0;

   public static void main(String[] args)
   {
      SimulationConstructionSet scs = new SimulationConstructionSet();
      scs.setGroundVisible(false);
      scs.addStaticLinkGraphics(new CinderBlockFieldPlanarRegionEnvironment().getTerrainObject3D().getLinkGraphics());
      scs.startOnAThread();
   }

   public CinderBlockFieldPlanarRegionEnvironment()
   {
      generator.translate(0.0, 0.0, -0.01);
      generator.addCubeReferencedAtBottomMiddle(2.0 * distanceToFirstCinderBlock, 4 * cinderBlockLength, 0.01);
      generator.identity();

      double xPosition = distanceToFirstCinderBlock + 0.5 * cinderBlockLength;

      setUpFlatBlock(generator, xPosition, 0.2, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.6, 0.0, flatCinderBlockHeight);

      xPosition += cinderBlockLength;

      setUpFlatBlock(generator, xPosition, -0.6, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.2, -flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, flatCinderBlockHeight, flatCinderBlockHeight);

      xPosition += cinderBlockLength;

      setUpFlatBlock(generator, xPosition, -0.6, 2.0 * flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.2, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, flatCinderBlockHeight, flatCinderBlockHeight);

      xPosition += cinderBlockLength;

      setUpFlatBlock(generator, xPosition, -0.6, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, 2.0 * flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.2, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, 0.0, flatCinderBlockHeight);

      xPosition += cinderBlockLength;

      setUpFlatBlock(generator, xPosition, -0.6, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.2, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, -flatCinderBlockHeight, flatCinderBlockHeight);

      xPosition += cinderBlockLength;

      setUpFlatBlock(generator, xPosition, -0.6, -flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, -0.2, 0.0, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.2, -flatCinderBlockHeight, flatCinderBlockHeight);
      setUpFlatBlock(generator, xPosition, 0.6, -flatCinderBlockHeight, flatCinderBlockHeight);



      xPosition += 0.5 * (gapBetweenCinderBlockSets + cinderBlockLength);

      generator.translate(xPosition, 0.0, -0.01);
      generator.addCubeReferencedAtBottomMiddle(gapBetweenCinderBlockSets, 4.0 * cinderBlockLength, 0.01);
      generator.identity();

      xPosition += 0.5 * (gapBetweenCinderBlockSets + cinderBlockLength) + 0.04;

      setUpRampBlock(generator, 4.39, -0.6, 0.0, BlockOrientation.NORTH);
      setUpRampBlock(generator, 4.39, -0.2, 0.0, BlockOrientation.NORTH);
      setUpRampBlock(generator, 4.39, 0.2, 0.0, BlockOrientation.NORTH);
      setUpRampBlock(generator, 4.39, 0.6, 0.0, BlockOrientation.NORTH);

      xPosition += cinderBlockLength;

      setUpRampBlock(generator, 4.79, -0.6, 0.0, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 4.79, -0.2, 0.0, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 4.79, 0.2, 0.0, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 4.79, 0.6, 0.0, BlockOrientation.SOUTH);

      xPosition += cinderBlockLength + 0.04;

      setUpRampBlock(generator, 5.23, -0.665, 0.0, BlockOrientation.EAST);
      setUpRampBlock(generator, 5.23, -0.195, 0.0, BlockOrientation.WEST);
      setUpRampBlock(generator, 5.23, 0.195, 0.0, BlockOrientation.EAST);
      setUpRampBlock(generator, 5.23, 0.665, 0.0, BlockOrientation.WEST);

      xPosition += cinderBlockLength + 0.01;

      setUpRampBlock(generator, 5.64, -0.635, 0.0, BlockOrientation.WEST);
      setUpRampBlock(generator, 5.64, -0.235, 0.0, BlockOrientation.EAST);
      setUpRampBlock(generator, 5.64, 0.235, 0.0, BlockOrientation.WEST);
      setUpRampBlock(generator, 5.64, 0.635, 0.0, BlockOrientation.EAST);


      xPosition += cinderBlockLength + 0.01;

      setUpFlatBlock(generator, 6.05, -0.6, cinderBlockHeight);
      setUpFlatBlock(generator, 6.05, -0.2, cinderBlockHeight);
      setUpFlatBlock(generator, 6.05, 0.2, cinderBlockHeight);
      setUpFlatBlock(generator, 6.05, 0.6, cinderBlockHeight);

      xPosition += cinderBlockLength + 0.025;

      setUpRampBlock(generator, 6.475 + 0.02, -0.6 - 0.04, 0.0, BlockOrientation.NORTH);
      setUpRampBlock(generator, 6.475, -0.2 - 0.04, 0.0, BlockOrientation.EAST);
      setUpRampBlock(generator, 6.475 - 0.01, 0.2, 0.0, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 6.475, 0.6 + 0.04, 0.0, BlockOrientation.WEST);

      xPosition += cinderBlockLength + 0.04;

      setUpRampBlock(generator, 6.915 - 0.015, -0.64, cinderBlockHeight, BlockOrientation.EAST);
      setUpRampBlock(generator, 6.915 - 0.03, -0.2, cinderBlockHeight, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 6.915 - 0.015, 0.24, cinderBlockHeight, BlockOrientation.WEST);
      setUpRampBlock(generator, 6.915, 0.64, cinderBlockHeight, BlockOrientation.NORTH);

      xPosition += cinderBlockLength + 0.015;

      setUpRampBlock(generator, 7.33 -0.015, -0.6 - 0.04, 0.0, BlockOrientation.SOUTH);
      setUpRampBlock(generator, 7.33, -0.2, 0.0, BlockOrientation.WEST);
      setUpRampBlock(generator, 7.33 + 0.015, 0.2, 0.0, BlockOrientation.NORTH);
      setUpRampBlock(generator, 7.33, 0.6, 0.0, BlockOrientation.EAST);

      xPosition += 0.75 + 0.5 * cinderBlockLength + 0.02;
      generator.translate(xPosition, 0.0, -0.01);
      generator.addCubeReferencedAtBottomMiddle(1.5, 4.0 * cinderBlockLength, 0.01);
      generator.identity();

      addPlanarRegionsToTerrain(YoAppearance.Grey());
   }

   private static void setUpFlatBlock(PlanarRegionsListGenerator generator, double xInWorld, double yInWorld, double bottomSurfaceHeightLowestPoint)
   {
      setUpFlatBlock(generator, xInWorld, yInWorld, bottomSurfaceHeightLowestPoint, cinderBlockHeight);
   }

   private static void setUpFlatBlock(PlanarRegionsListGenerator generator, double xInWorld, double yInWorld, double bottomSurfaceHeightLowestPoint, double blockHeight)
   {
      generator.translate(xInWorld, yInWorld, bottomSurfaceHeightLowestPoint);

      generator.addCubeReferencedAtBottomMiddle(cinderBlockLength, cinderBlockLength, blockHeight);

      generator.identity();
   }

   private static void setUpRampBlock(PlanarRegionsListGenerator generator, double xInWorld, double yInWorld, double surfaceHeightLowestPoint,
                                      BlockOrientation orientation)
   {
      double zHeightToCenter = 0.5 * cinderBlockLength * Math.sin(cinderBlockTiltRadians);

      generator.translate(xInWorld, yInWorld, surfaceHeightLowestPoint + zHeightToCenter);

      Quaternion quaternion = new Quaternion();
      switch (orientation)
      {
      case NORTH:
         quaternion.appendPitchRotation(-cinderBlockTiltRadians);
         break;
      case EAST:
         quaternion.appendRollRotation(-cinderBlockTiltRadians);
         break;
      case WEST:
         quaternion.appendRollRotation(cinderBlockTiltRadians);
         break;
      case SOUTH:
         quaternion.appendPitchRotation(cinderBlockTiltRadians);
         break;
      }
      generator.rotate(quaternion);

      generator.addCubeReferencedAtBottomMiddle(cinderBlockLength, cinderBlockLength, cinderBlockHeight);

      switch (orientation)
      {
      case NORTH:
         quaternion.appendPitchRotation(cinderBlockTiltRadians);
         break;
      case EAST:
         quaternion.appendRollRotation(cinderBlockTiltRadians);
         break;
      case WEST:
         quaternion.appendRollRotation(-cinderBlockTiltRadians);
         break;
      case SOUTH:
         quaternion.appendPitchRotation(-cinderBlockTiltRadians);
         break;
      }
      generator.identity();
   }

}
