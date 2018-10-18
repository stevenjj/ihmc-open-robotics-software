package us.ihmc.avatar.roughTerrainWalking;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import controller_msgs.msg.dds.FootstepDataListMessage;
import controller_msgs.msg.dds.FootstepPlanningRequestPacket;
import controller_msgs.msg.dds.FootstepPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.RobotConfigurationData;
import controller_msgs.msg.dds.ToolboxStateMessage;
import controller_msgs.msg.dds.WalkingStatusMessage;
import us.ihmc.avatar.DRCStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.initialSetup.OffsetAndYawRobotInitialSetup;
import us.ihmc.avatar.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.packets.ToolboxState;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.robotics.geometry.PlanarRegionsListGenerator;
import us.ihmc.simulationConstructionSetTools.util.environments.planarRegionEnvironments.TwoBollardEnvironment;
import us.ihmc.simulationConstructionSetTools.util.planarRegions.PlanarRegionsListExamples;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicCoordinateSystem;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingStatus;
import us.ihmc.humanoidRobotics.communication.subscribers.HumanoidRobotDataReceiver;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationConstructionSetTools.util.environments.PlanarRegionsListDefinedEnvironment;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.util.ControllerFailureException;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.yoVariables.variable.YoFramePoseUsingYawPitchRoll;

@ContinuousIntegrationAnnotations.ContinuousIntegrationPlan(categories = {IntegrationCategory.EXCLUDE})
public abstract class AvatarBipedalFootstepPlannerEndToEndTest implements MultiRobotTestInterface
{
   private SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromSystemProperties();
   private static final boolean keepSCSUp = false;
   private static final int timeout = 120000; // to easily keep scs up. unfortunately can't be set programmatically, has to be a constant

   private DRCSimulationTestHelper drcSimulationTestHelper;
   private DRCNetworkModuleParameters networkModuleParameters;
   private HumanoidRobotDataReceiver humanoidRobotDataReceiver;

   private PacketCommunicator toolboxCommunicator;
   private PlanarRegionsList cinderBlockField;
   private PlanarRegionsList steppingStoneField;
   private PlanarRegionsList bollardEnvironment;

   public static final double CINDER_BLOCK_START_X = 0.0;
   public static final double CINDER_BLOCK_START_Y = 0.0;
   public static final double CINDER_BLOCK_HEIGHT = 0.1;
   public static final double CINDER_BLOCK_SIZE = 0.4;
   public static final int CINDER_BLOCK_COURSE_WIDTH_X_IN_NUMBER_OF_BLOCKS = 5;
   public static final int CINDER_BLOCK_COURSE_LENGTH_Y_IN_NUMBER_OF_BLOCKS = 6;
   public static final double CINDER_BLOCK_HEIGHT_VARIATION = 0.0;
   public static final double CINDER_BLOCK_FIELD_PLATFORM_LENGTH = 0.6;

   public static final double STEPPING_STONE_PATH_RADIUS = 3.5;

   private volatile boolean planCompleted = false;
   private AtomicReference<FootstepPlanningToolboxOutputStatus> outputStatus;
   private BlockingSimulationRunner blockingSimulationRunner;

   @Before
   public void setup()
   {
      PlanarRegionsListGenerator generator = new PlanarRegionsListGenerator();
      generator.translate(CINDER_BLOCK_START_X, CINDER_BLOCK_START_Y, 0.001);
      PlanarRegionsListExamples.generateCinderBlockField(generator, CINDER_BLOCK_SIZE, CINDER_BLOCK_HEIGHT,
                                                                            CINDER_BLOCK_COURSE_WIDTH_X_IN_NUMBER_OF_BLOCKS,
                                                                            CINDER_BLOCK_COURSE_LENGTH_Y_IN_NUMBER_OF_BLOCKS, CINDER_BLOCK_HEIGHT_VARIATION);
      cinderBlockField = generator.getPlanarRegionsList();
      steppingStoneField = PlanarRegionsListExamples.generateSteppingStonesEnvironment(STEPPING_STONE_PATH_RADIUS);
      bollardEnvironment = new TwoBollardEnvironment(0.65).getPlanarRegionsList();

      networkModuleParameters = new DRCNetworkModuleParameters();
      networkModuleParameters.enableFootstepPlanningToolbox(true);
      networkModuleParameters.enableLocalControllerCommunicator(true);
      networkModuleParameters.enableNetworkProcessor(true);

      toolboxCommunicator = PacketCommunicator.createIntraprocessPacketCommunicator(NetworkPorts.FOOTSTEP_PLANNING_TOOLBOX_MODULE_PORT,
                                                                                    new IHMCCommunicationKryoNetClassList());

      FullHumanoidRobotModel fullHumanoidRobotModel = getRobotModel().createFullRobotModel();
      ForceSensorDataHolder forceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(fullHumanoidRobotModel.getForceSensorDefinitions()));
      humanoidRobotDataReceiver = new HumanoidRobotDataReceiver(fullHumanoidRobotModel, forceSensorDataHolder);
      planCompleted = false;

      simulationTestingParameters.setKeepSCSUp(keepSCSUp && !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer());
   }

   @After
   public void tearDown()
   {
      cinderBlockField = null;
      steppingStoneField = null;
      networkModuleParameters = null;

      toolboxCommunicator.closeConnection();
      toolboxCommunicator.disconnect();
      toolboxCommunicator = null;
      planCompleted = false;

      humanoidRobotDataReceiver = null;
      blockingSimulationRunner = null;

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testShortCinderBlockFieldWithAStar()
   {
      double courseLength = CINDER_BLOCK_COURSE_WIDTH_X_IN_NUMBER_OF_BLOCKS * CINDER_BLOCK_SIZE + CINDER_BLOCK_FIELD_PLATFORM_LENGTH;
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(0.0, 0.0, 0.007);
      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(), new Pose3D(courseLength, 0.0, 0.0, 0.0, 0.0, 0.0));

      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.A_STAR, cinderBlockField, startingLocation, goalPose);
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testShortCinderBlockFieldWithVisibilityGraph()
   {
      double courseLength = CINDER_BLOCK_COURSE_WIDTH_X_IN_NUMBER_OF_BLOCKS * CINDER_BLOCK_SIZE + CINDER_BLOCK_FIELD_PLATFORM_LENGTH;
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(0.0, 0.0, 0.007);
      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(), new Pose3D(courseLength, 0.0, 0.0, 0.0, 0.0, 0.0));

      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.VIS_GRAPH_WITH_A_STAR, cinderBlockField, startingLocation, goalPose);
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testShortCinderBlockFieldWithPlanarRegionBipedalPlanner()
   {
      double courseLength = CINDER_BLOCK_COURSE_WIDTH_X_IN_NUMBER_OF_BLOCKS * CINDER_BLOCK_SIZE + CINDER_BLOCK_FIELD_PLATFORM_LENGTH;
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(0.0, 0.0, 0.007);
      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(), new Pose3D(courseLength, 0.0, 0.0, 0.0, 0.0, 0.0));

      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.PLANAR_REGION_BIPEDAL, cinderBlockField, startingLocation, goalPose);
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testSteppingStonesWithAStar()
   {
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(0.0, -0.75, 0.007, 0.5 * Math.PI);
      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(),
                                             new Pose3D(STEPPING_STONE_PATH_RADIUS + 0.5, STEPPING_STONE_PATH_RADIUS, 0.0, 0.0, 0.0, 0.0));

      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.A_STAR, steppingStoneField, startingLocation, goalPose);
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testSteppingStonesWithPlanarRegionBipedalPlanner()
   {
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(0.0, -0.75, 0.007, 0.5 * Math.PI);
      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame(),
                                             new Pose3D(STEPPING_STONE_PATH_RADIUS + 0.5, STEPPING_STONE_PATH_RADIUS, 0.0, 0.0, 0.0, 0.0));

      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.PLANAR_REGION_BIPEDAL, steppingStoneField, startingLocation, goalPose);
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = timeout)
   public void testWalkingBetweenBollardsAStarPlanner()
   {
      DRCStartingLocation startingLocation = () -> new OffsetAndYawRobotInitialSetup(-1.5, 0.0, 0.007, 0.0);
      FramePose3D goalPose = new FramePose3D();
      goalPose.setX(1.5);
      runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType.A_STAR, bollardEnvironment, startingLocation, goalPose);
   }

   private void runEndToEndTestAndKeepSCSUpIfRequested(FootstepPlannerType plannerType, PlanarRegionsList planarRegionsList,
                                                       DRCStartingLocation startingLocation, FramePose3D goalPose)
   {
      try
      {
         runEndToEndTest(plannerType, planarRegionsList, startingLocation, goalPose);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         if (simulationTestingParameters.getKeepSCSUp())
         {
            ThreadTools.sleepForever();
         }
         else
         {
            fail(e.getMessage());
         }
      }
   }

   private void runEndToEndTest(FootstepPlannerType plannerType, PlanarRegionsList planarRegionsList, DRCStartingLocation startingLocation,
                                FramePose3D goalPose)
         throws Exception
   {
      outputStatus = new AtomicReference<>();
      outputStatus.set(null);

      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
      }

      CommonAvatarEnvironmentInterface steppingStonesEnvironment = createCommonAvatarInterface(planarRegionsList);
      DRCRobotModel robotModel = getRobotModel();
      drcSimulationTestHelper = new DRCSimulationTestHelper(simulationTestingParameters, robotModel);
      drcSimulationTestHelper.setTestEnvironment(steppingStonesEnvironment);
      drcSimulationTestHelper.setNetworkProcessorParameters(networkModuleParameters);
      drcSimulationTestHelper.setStartingLocation(startingLocation);
      drcSimulationTestHelper.createSimulation("steppingStonesTestHelper");

      toolboxCommunicator.connect();
      toolboxCommunicator.attachListener(FootstepPlanningToolboxOutputStatus.class, this::setOutputStatus);

      drcSimulationTestHelper.createSubscriberFromController(RobotConfigurationData.class, humanoidRobotDataReceiver::receivedPacket);
      drcSimulationTestHelper.createSubscriberFromController(WalkingStatusMessage.class, this::listenForWalkingComplete);

      blockingSimulationRunner = drcSimulationTestHelper.getBlockingSimulationRunner();
      ToolboxStateMessage wakeUpMessage = MessageTools.createToolboxStateMessage(ToolboxState.WAKE_UP);
      toolboxCommunicator.send(wakeUpMessage);

      while (!humanoidRobotDataReceiver.framesHaveBeenSetUp())
      {
         simulate(1.0);
         humanoidRobotDataReceiver.updateRobotModel();
      }

      ReferenceFrame soleFrame = humanoidRobotDataReceiver.getReferenceFrames().getSoleFrame(RobotSide.LEFT);
      FramePose3D initialStancePose = new FramePose3D(soleFrame, new Point3D(0.0, 0.0, 0.001), new AxisAngle());
      initialStancePose.changeFrame(ReferenceFrame.getWorldFrame());
      RobotSide initialStanceSide = RobotSide.LEFT;

      YoGraphicsListRegistry graphicsListRegistry = createStartAndGoalGraphics(initialStancePose, goalPose);
      drcSimulationTestHelper.getSimulationConstructionSet().addYoGraphicsListRegistry(graphicsListRegistry);

      FootstepPlanningRequestPacket requestPacket = HumanoidMessageTools.createFootstepPlanningRequestPacket(initialStancePose, initialStanceSide, goalPose,
                                                                                                             plannerType);
      PlanarRegionsListMessage planarRegionsListMessage = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionsList);
      requestPacket.getPlanarRegionsListMessage().set(planarRegionsListMessage);
      toolboxCommunicator.send(requestPacket);

      while (outputStatus.get() == null)
      {
         simulate(1.0);
      }

      FootstepPlanningToolboxOutputStatus outputStatus = this.outputStatus.get();
      if (!FootstepPlanningResult.fromByte(outputStatus.getFootstepPlanningResult()).validForExecution())
      {
         throw new RuntimeException("Footstep plan not valid for execution: " + outputStatus.getFootstepPlanningResult());
      }

      IHMCROS2Publisher<FootstepDataListMessage> publisher = drcSimulationTestHelper.createPublisherForController(FootstepDataListMessage.class);

      planCompleted = false;
      if (outputStatus.getFootstepDataList().getFootstepDataList().size() > 0)
      {
         publisher.publish(outputStatus.getFootstepDataList());

         while (!planCompleted)
         {
            simulate(1.0);
         }
      }

      FloatingJoint rootJoint = drcSimulationTestHelper.getRobot().getRootJoint();
      Point3D rootJointPosition = new Point3D();
      rootJoint.getPosition(rootJointPosition);

      double errorThreshold = 0.3;
      double xPositionErrorMagnitude = Math.abs(rootJointPosition.getX() - goalPose.getX());
      double yPositionErrorMagnitude = Math.abs(rootJointPosition.getY() - goalPose.getY());
      assertTrue(xPositionErrorMagnitude < errorThreshold);
      assertTrue(yPositionErrorMagnitude < errorThreshold);
   }

   private YoGraphicsListRegistry createStartAndGoalGraphics(FramePose3D initialStancePose, FramePose3D goalPose)
   {
      YoGraphicsListRegistry graphicsListRegistry = new YoGraphicsListRegistry();
      YoGraphicsList graphicsList = new YoGraphicsList("testViz");

      YoFramePoseUsingYawPitchRoll yoInitialStancePose = new YoFramePoseUsingYawPitchRoll("initialStancePose", initialStancePose.getReferenceFrame(),
                                                                                          drcSimulationTestHelper.getYoVariableRegistry());
      yoInitialStancePose.set(initialStancePose);

      YoFramePoseUsingYawPitchRoll yoGoalPose = new YoFramePoseUsingYawPitchRoll("goalStancePose", goalPose.getReferenceFrame(),
                                                                                 drcSimulationTestHelper.getYoVariableRegistry());
      yoGoalPose.set(goalPose);

      YoGraphicCoordinateSystem startPoseGraphics = new YoGraphicCoordinateSystem("startPose", yoInitialStancePose, 13.0);
      YoGraphicCoordinateSystem goalPoseGraphics = new YoGraphicCoordinateSystem("goalPose", yoGoalPose, 13.0);

      graphicsList.add(startPoseGraphics);
      graphicsList.add(goalPoseGraphics);
      return graphicsListRegistry;
   }

   private void simulate(double time) throws BlockingSimulationRunner.SimulationExceededMaximumTimeException, ControllerFailureException
   {
      if (keepSCSUp)
         blockingSimulationRunner.simulateAndBlock(1.0);
      else
         blockingSimulationRunner.simulateAndBlock(1.0);
   }

   private static CommonAvatarEnvironmentInterface createCommonAvatarInterface(PlanarRegionsList planarRegionsList)
   {
      double allowablePenetrationThickness = 0.05;
      boolean generateGroundPlane = false;
      return new PlanarRegionsListDefinedEnvironment("testEnvironment", planarRegionsList, allowablePenetrationThickness, generateGroundPlane);
   }

   private void listenForWalkingComplete(WalkingStatusMessage walkingStatusMessage)
   {
      if (walkingStatusMessage.getWalkingStatus() == WalkingStatus.COMPLETED.toByte())
      {
         planCompleted = true;
      }
   }

   private void setOutputStatus(FootstepPlanningToolboxOutputStatus packet)
   {
      outputStatus.set(packet);
   }
}
