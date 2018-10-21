package us.ihmc.footstepPlanning;

import controller_msgs.msg.dds.VisibilityClusterMessage;
import controller_msgs.msg.dds.VisibilityMapMessage;
import org.junit.Test;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.footstepPlanning.ui.VisibilityGraphMessagesConverter;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.*;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VisibilityGraphMessagesConverterTest
{
   private static final int iters = 1;
   private static final double epsilon = 1e-9;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @Test(timeout = 30000)
   public void testConvertToCluster()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         byte typeByte = (byte) RandomNumbers.nextInt(random, 0, Cluster.Type.values.length - 1);
         byte extrusionSideByte = (byte) RandomNumbers.nextInt(random, 0, Cluster.ExtrusionSide.values.length - 1);

         int numberOfRawPoints = RandomNumbers.nextInt(random, 1, 1000);
         int numberOfNavigableExtrusions = RandomNumbers.nextInt(random, 1, 1000);
         int numberOfNonNavigableExtrusions = RandomNumbers.nextInt(random, 1, 1000);
         RigidBodyTransform transformToWorld = EuclidCoreRandomTools.nextRigidBodyTransform(random);

         List<Point3D> rawPointsInLocalExpected = new ArrayList<>();
         List<Point2D> navigableExtrusionsInLocalExpected = new ArrayList<>();
         List<Point2D> nonNavigableExtrusionsInLocalExpected = new ArrayList<>();

         for (int i = 0; i < numberOfRawPoints; i++)
            rawPointsInLocalExpected.add(EuclidCoreRandomTools.nextPoint3D(random, 100.0));
         for (int i = 0; i < numberOfNavigableExtrusions; i++)
            navigableExtrusionsInLocalExpected.add(EuclidCoreRandomTools.nextPoint2D(random, 100.0));
         for (int i = 0; i < numberOfNonNavigableExtrusions; i++)
            nonNavigableExtrusionsInLocalExpected.add(EuclidCoreRandomTools.nextPoint2D(random, 100.0));

         Cluster clusterToConvert = new Cluster();
         clusterToConvert.setTransformToWorld(transformToWorld);
         clusterToConvert.setType(Cluster.Type.fromByte(typeByte));
         clusterToConvert.setExtrusionSide(Cluster.ExtrusionSide.fromByte(extrusionSideByte));

         for (int i = 0; i < numberOfRawPoints; i++)
            clusterToConvert.addRawPointInLocal(rawPointsInLocalExpected.get(i));
         for (int i = 0; i < numberOfNavigableExtrusions; i++)
            clusterToConvert.addNavigableExtrusionInLocal(navigableExtrusionsInLocalExpected.get(i));
         for (int i = 0; i < numberOfNonNavigableExtrusions; i++)
            clusterToConvert.addNonNavigableExtrusionInLocal(nonNavigableExtrusionsInLocalExpected.get(i));

         VisibilityClusterMessage message = VisibilityGraphMessagesConverter.convertToVisibilityClusterMessage(clusterToConvert);
         Cluster convertedCluster = VisibilityGraphMessagesConverter.convertToCluster(message);

         EuclidCoreTestTools.assertRigidBodyTransformGeometricallyEquals(transformToWorld, convertedCluster.getTransformToWorld(), epsilon);
         EuclidCoreTestTools.assertRigidBodyTransformGeometricallyEquals(transformToWorld, clusterToConvert.getTransformToWorld(), epsilon);
         assertEquals(numberOfRawPoints, convertedCluster.getNumberOfRawPoints());
         assertEquals(numberOfRawPoints, clusterToConvert.getNumberOfRawPoints());
         assertEquals(numberOfNavigableExtrusions, convertedCluster.getNumberOfNavigableExtrusions());
         assertEquals(numberOfNavigableExtrusions, clusterToConvert.getNumberOfNavigableExtrusions());
         assertEquals(numberOfNonNavigableExtrusions, convertedCluster.getNumberOfNonNavigableExtrusions());
         assertEquals(numberOfNonNavigableExtrusions, clusterToConvert.getNumberOfNonNavigableExtrusions());

         ReferenceFrame localFrame = new ReferenceFrame("localFrame", worldFrame)
         {
            @Override
            protected void updateTransformToParent(RigidBodyTransform transformToParent)
            {
               transformToParent.set(transformToWorld);
            }
         };
         localFrame.update();

         for (int i = 0; i < numberOfRawPoints; i++)
         {
            Point3D expectedPoint = new Point3D(rawPointsInLocalExpected.get(i));
            FramePoint3D expectedFramePoint = new FramePoint3D(localFrame, rawPointsInLocalExpected.get(i));
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedPoint, clusterToConvert.getRawPointInLocal(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedPoint, convertedCluster.getRawPointInLocal(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedFramePoint, clusterToConvert.getRawPointInLocal(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedFramePoint, convertedCluster.getRawPointInLocal(i), epsilon);
            transformToWorld.transform(expectedPoint);
            expectedFramePoint.changeFrame(worldFrame);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedPoint, clusterToConvert.getRawPointInWorld(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedPoint, clusterToConvert.getRawPointInWorld(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedFramePoint, clusterToConvert.getRawPointInWorld(i), epsilon);
            EuclidCoreTestTools.assertPoint3DGeometricallyEquals(expectedFramePoint, convertedCluster.getRawPointInWorld(i), epsilon);
         }
         for (int i = 0; i < numberOfNavigableExtrusions; i++)
         {
            EuclidCoreTestTools
                  .assertPoint2DGeometricallyEquals(navigableExtrusionsInLocalExpected.get(i), clusterToConvert.getNavigableExtrusionInLocal(i), epsilon);
            EuclidCoreTestTools
                  .assertPoint2DGeometricallyEquals(navigableExtrusionsInLocalExpected.get(i), convertedCluster.getNavigableExtrusionInLocal(i), epsilon);
         }
         for (int i = 0; i < numberOfNonNavigableExtrusions; i++)
         {
            EuclidCoreTestTools
                  .assertPoint2DGeometricallyEquals(nonNavigableExtrusionsInLocalExpected.get(i), clusterToConvert.getNonNavigableExtrusionInLocal(i), epsilon);
            EuclidCoreTestTools
                  .assertPoint2DGeometricallyEquals(nonNavigableExtrusionsInLocalExpected.get(i), convertedCluster.getNonNavigableExtrusionInLocal(i), epsilon);
         }

         assertClustersEqual(clusterToConvert, convertedCluster, epsilon);
      }
   }

   @Test(timeout = 30000)
   public void testConvertInterRegionsVisibilityMap()
   {
      Random random = new Random(1738L);
      for (int iter = 0; iter < iters; iter++)
      {
         VisibilityMapHolder mapToConvert = new InterRegionVisibilityMap();

         int numberOfConnections = RandomNumbers.nextInt(random, 2, 10000);

         for (int i = 0; i < numberOfConnections; i++)
            ((InterRegionVisibilityMap) mapToConvert).addConnection(getRandomConnection(random));

         VisibilityMapMessage visibilityMapMessage = VisibilityGraphMessagesConverter.convertToVisibilityMapMessage(mapToConvert);
         VisibilityMapHolder convertedMap = VisibilityGraphMessagesConverter.convertToInterRegionsVisibilityMap(visibilityMapMessage);

         assertVisibilityMapHoldersEqual(mapToConvert, convertedMap, epsilon);
      }
   }

   private static VisibilityMap getRandomVisibilityMap(Random random)
   {
      VisibilityMap visibilityMap = new VisibilityMap();
      int numberOfConnections = RandomNumbers.nextInt(random, 2, 10000);

      for (int i = 0; i < numberOfConnections; i++)
         visibilityMap.addConnection(getRandomConnection(random));

      visibilityMap.computeVertices();

      return visibilityMap;
   }

   private static Connection getRandomConnection(Random random)
   {
      int startId = RandomNumbers.nextInt(random, 0, 1000);
      int targetId = RandomNumbers.nextInt(random, 0, 1000);
      Point3DReadOnly startPoint = EuclidCoreRandomTools.nextPoint3D(random, 100);
      Point3DReadOnly targetPoint = EuclidCoreRandomTools.nextPoint3D(random, 100);

      return new Connection(startPoint, startId, targetPoint, targetId);
   }

   private static void assertVisibilityMapHoldersEqual(VisibilityMapHolder holderExpected, VisibilityMapHolder holderActual, double epsilon)
   {
      assertEquals(holderExpected.getMapId(), holderActual.getMapId());
      assertVisibilityMapsEqual(holderExpected.getVisibilityMapInWorld(), holderActual.getVisibilityMapInWorld(), epsilon);
      assertVisibilityMapsEqual(holderExpected.getVisibilityMapInLocal(), holderActual.getVisibilityMapInLocal(), epsilon);

      for (Connection connection : holderExpected.getVisibilityMapInWorld().getConnections())
      {
         assertEquals(holderExpected.getConnectionWeight(connection), holderActual.getConnectionWeight(connection), epsilon);
      }

      for (Connection connection : holderExpected.getVisibilityMapInLocal().getConnections())
      {
         assertEquals(holderExpected.getConnectionWeight(connection), holderActual.getConnectionWeight(connection), epsilon);
      }
   }

   private static void assertVisibilityMapsEqual(VisibilityMap mapExpected, VisibilityMap mapActual, double epsilon)
   {
      assertEquals(mapExpected.getConnections().size(), mapActual.getConnections().size());
      assertEquals(mapExpected.getVertices().size(), mapActual.getVertices().size());
      for (int i = 0; i < mapExpected.getConnections().size(); i++)
         assertConnectionsEqual(mapExpected.getConnections().get(i), mapActual.getConnections().get(i), epsilon);
      for (int i = 0; i < mapExpected.getVertices().size(); i++)
         assertConnectionPointsEqual(mapExpected.getVertices().get(i), mapActual.getVertices().get(i), epsilon);

   }

   private static void assertConnectionsEqual(Connection connectionExpected, Connection connectionActual, double epsilon)
   {
      assertConnectionPointsEqual(connectionExpected.getSourcePoint(), connectionActual.getSourcePoint(), epsilon);
      assertConnectionPointsEqual(connectionExpected.getTargetPoint(), connectionActual.getTargetPoint(), epsilon);
   }

   private static void assertConnectionPointsEqual(ConnectionPoint3D pointExpected, ConnectionPoint3D pointActual, double epsilon)
   {
      assertEquals(pointExpected.getRegionId(), pointActual.getRegionId());
      EuclidCoreTestTools.assertPoint3DGeometricallyEquals(pointExpected, pointActual, epsilon);
   }

   private static void assertClustersEqual(Cluster clusterExpected, Cluster clusterActual, double epsilon)
   {
      assertEquals(clusterExpected.getExtrusionSide(), clusterActual.getExtrusionSide());
      assertEquals(clusterExpected.getType(), clusterActual.getType());

      RigidBodyTransform transformExpected = clusterExpected.getTransformToWorld();
      ReferenceFrame localFrame = new ReferenceFrame("localFrame", worldFrame)
      {
         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
            transformToParent.set(transformExpected);
         }
      };
      localFrame.update();
      EuclidCoreTestTools.assertRigidBodyTransformGeometricallyEquals(transformExpected, clusterActual.getTransformToWorld(), epsilon);

      int numberOfRawPoints = clusterExpected.getRawPointsInLocal2D().size();
      assertEquals(numberOfRawPoints, clusterActual.getNumberOfRawPoints());
      assertEquals(numberOfRawPoints, clusterExpected.getNumberOfRawPoints());
      assertEquals(numberOfRawPoints, clusterExpected.getRawPointsInLocal2D().size());
      assertEquals(numberOfRawPoints, clusterExpected.getRawPointsInWorld().size());
      assertEquals(numberOfRawPoints, clusterExpected.getRawPointsInLocal3D().size());
      assertEquals(numberOfRawPoints, clusterActual.getRawPointsInLocal2D().size());
      assertEquals(numberOfRawPoints, clusterActual.getRawPointsInWorld().size());
      assertEquals(numberOfRawPoints, clusterActual.getRawPointsInLocal3D().size());

      for (int i = 0; i < numberOfRawPoints; i++)
      {
         EuclidCoreTestTools
               .assertPoint3DGeometricallyEquals("Point " + i + " failed.", clusterExpected.getRawPointInLocal(i), clusterActual.getRawPointInLocal(i),
                                                 epsilon);
         EuclidCoreTestTools
               .assertPoint3DGeometricallyEquals("Point " + i + " failed.", clusterExpected.getRawPointInWorld(i), clusterActual.getRawPointInWorld(i),
                                                 epsilon);

         FramePoint3D point = new FramePoint3D(localFrame, clusterExpected.getRawPointInLocal(i));
         point.changeFrame(worldFrame);

         EuclidCoreTestTools.assertPoint3DGeometricallyEquals("Point " + i + " failed.", point, clusterExpected.getRawPointInWorld(i), epsilon);
         EuclidCoreTestTools.assertPoint3DGeometricallyEquals("Point " + i + " failed.", point, clusterActual.getRawPointInWorld(i), epsilon);
      }

      int numberOfNavigableExtrusions = clusterExpected.getNumberOfNavigableExtrusions();
      assertEquals(numberOfNavigableExtrusions, clusterActual.getNumberOfNavigableExtrusions());
      assertEquals(numberOfNavigableExtrusions, clusterExpected.getNavigableExtrusionsInLocal().size());
      assertEquals(numberOfNavigableExtrusions, clusterExpected.getNavigableExtrusionsInWorld().size());
      assertEquals(numberOfNavigableExtrusions, clusterActual.getNavigableExtrusionsInLocal().size());
      assertEquals(numberOfNavigableExtrusions, clusterActual.getNavigableExtrusionsInWorld().size());

      for (int i = 0; i < numberOfNavigableExtrusions; i++)
      {
         EuclidCoreTestTools
               .assertPoint2DGeometricallyEquals(clusterExpected.getNavigableExtrusionInLocal(i), clusterActual.getNavigableExtrusionInLocal(i), epsilon);
         EuclidCoreTestTools
               .assertPoint3DGeometricallyEquals(clusterExpected.getNavigableExtrusionInWorld(i), clusterActual.getNavigableExtrusionInWorld(i), epsilon);

         FramePoint3D framePoint = new FramePoint3D(localFrame, clusterExpected.getNavigableExtrusionInLocal(i));
         framePoint.changeFrame(worldFrame);

         EuclidCoreTestTools.assertPoint3DGeometricallyEquals(framePoint, clusterExpected.getNavigableExtrusionInWorld(i), epsilon);
         EuclidCoreTestTools.assertPoint3DGeometricallyEquals(framePoint, clusterActual.getNavigableExtrusionInWorld(i), epsilon);
      }

      int numberOfNonNavigableExtrusions = clusterExpected.getNumberOfNonNavigableExtrusions();
      assertEquals(numberOfNonNavigableExtrusions, clusterActual.getNumberOfNonNavigableExtrusions());
      assertEquals(numberOfNonNavigableExtrusions, clusterExpected.getNonNavigableExtrusionsInWorld().size());
      assertEquals(numberOfNonNavigableExtrusions, clusterExpected.getNonNavigableExtrusionsInLocal().size());
      assertEquals(numberOfNonNavigableExtrusions, clusterActual.getNonNavigableExtrusionsInWorld().size());
      assertEquals(numberOfNonNavigableExtrusions, clusterActual.getNonNavigableExtrusionsInLocal().size());

      for (int i = 0; i < numberOfNonNavigableExtrusions; i++)
      {
         EuclidCoreTestTools
               .assertPoint2DGeometricallyEquals(clusterExpected.getNonNavigableExtrusionInLocal(i), clusterActual.getNonNavigableExtrusionInLocal(i), epsilon);
         EuclidCoreTestTools
               .assertPoint3DGeometricallyEquals(clusterExpected.getNonNavigableExtrusionInWorld(i), clusterActual.getNonNavigableExtrusionInWorld(i), epsilon);

         FramePoint3D framePoint = new FramePoint3D(localFrame, clusterExpected.getNonNavigableExtrusionInLocal(i));
         framePoint.changeFrame(worldFrame);

         EuclidCoreTestTools.assertPoint3DGeometricallyEquals(framePoint, clusterExpected.getNonNavigableExtrusionInWorld(i), epsilon);
         EuclidCoreTestTools.assertPoint3DGeometricallyEquals(framePoint, clusterActual.getNonNavigableExtrusionInWorld(i), epsilon);

      }
   }

}
