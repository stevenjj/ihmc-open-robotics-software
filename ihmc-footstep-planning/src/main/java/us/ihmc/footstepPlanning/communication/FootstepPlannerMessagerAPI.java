package us.ihmc.footstepPlanning.communication;

import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlannerStatus;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.javaFXToolkit.messager.MessagerAPIFactory;
import us.ihmc.javaFXToolkit.messager.MessagerAPIFactory.*;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;

import java.util.List;

public class FootstepPlannerMessagerAPI
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();

   private static final CategoryTheme PlanarRegion = apiFactory.createCategoryTheme("PlanarRegion");
   private static final CategoryTheme Start = apiFactory.createCategoryTheme("Start");
   private static final CategoryTheme Goal = apiFactory.createCategoryTheme("Goal");
   private static final CategoryTheme PositionTheme = apiFactory.createCategoryTheme("PositionTheme");
   private static final CategoryTheme OrientationTheme = apiFactory.createCategoryTheme("OrientationTheme");
   private static final CategoryTheme EditMode = apiFactory.createCategoryTheme("EditMode");
   private static final CategoryTheme FootstepPlan = apiFactory.createCategoryTheme("FootstepPlan");
   private static final CategoryTheme BodyPath = apiFactory.createCategoryTheme("BodyPath");
   private static final CategoryTheme VisibilityGraphs = apiFactory.createCategoryTheme("VisibilityGraphs");
   private static final CategoryTheme NodeChecking = apiFactory.createCategoryTheme("NodeChecking");
   private static final CategoryTheme UnitTest = apiFactory.createCategoryTheme("UnitTest");
   private static final CategoryTheme Cluster = apiFactory.createCategoryTheme("Cluster");
   private static final CategoryTheme Map = apiFactory.createCategoryTheme("Map");
   private static final CategoryTheme InterRegion = apiFactory.createCategoryTheme("InterRegion");

   private static final CategoryTheme Parameters = apiFactory.createCategoryTheme("Parameters");

   private static final TypedTopicTheme<Boolean> Show = apiFactory.createTypedTopicTheme("Show");
   private static final TypedTopicTheme<Boolean> Enable = apiFactory.createTypedTopicTheme("Enable");
   private static final TypedTopicTheme<Boolean> Reset = apiFactory.createTypedTopicTheme("Reset");
   private static final TypedTopicTheme<Point3D> Position = apiFactory.createTypedTopicTheme("Position");
   private static final TypedTopicTheme<RobotSide> Side = apiFactory.createTypedTopicTheme("Side");
   private static final TypedTopicTheme<Quaternion> Orientation = apiFactory.createTypedTopicTheme("Orientation");
   private static final TypedTopicTheme<Boolean> ComputePath = apiFactory.createTypedTopicTheme("ComputePath");
   private static final TypedTopicTheme<Double> PlannerTimeout = apiFactory.createTypedTopicTheme("PlannerTimeout");
   private static final TypedTopicTheme<Double> PlannerTimeTaken = apiFactory.createTypedTopicTheme("PlannerTimeTaken");
   private static final TypedTopicTheme<Double> PlannerHorizonLength = apiFactory.createTypedTopicTheme("PlannerHorizonLength");
   private static final TypedTopicTheme<Integer> PlannerRequestId = apiFactory.createTypedTopicTheme("PlannerRequestId");
   private static final TypedTopicTheme<Integer> SequenceId = apiFactory.createTypedTopicTheme("SequenceId");
   private static final TypedTopicTheme<FootstepPlannerType> FootstepPlannerType = apiFactory.createTypedTopicTheme("FootstepPlannerType");
   private static final TypedTopicTheme<FootstepPlanningResult> FootstepPlannerResult = apiFactory.createTypedTopicTheme("FootstepPlannerResult");
   private static final TypedTopicTheme<FootstepPlannerStatus> FootstepPlannerStatus = apiFactory.createTypedTopicTheme("FootstepPlannerStatus");
   private static final TypedTopicTheme<FootstepPlannerParameters> FootstepPlannerParameters = apiFactory.createTypedTopicTheme("FootstepPlannerParameters");
   private static final TypedTopicTheme<Boolean> Export = apiFactory.createTypedTopicTheme("Export");
   private static final TypedTopicTheme<String> Path = apiFactory.createTypedTopicTheme("Path");


   private static final TopicTheme Data = apiFactory.createTopicTheme("Data");

   private static final Category Root = apiFactory.createRootCategory(apiFactory.createCategoryTheme("FootstepPlanning"));

   public static final Topic<Boolean> GlobalReset = Root.topic(Reset);

   public static final Topic<PlanarRegionsList> PlanarRegionDataTopic = Root.child(PlanarRegion).topic(Data);
   public static final Topic<Boolean> ShowPlanarRegionsTopic = Root.child(PlanarRegion).topic(Show);
   public static final Topic<Boolean> AcceptNewPlanarRegions = Root.child(PlanarRegion).topic(Enable);

   public static final Topic<FootstepPlan> FootstepPlanTopic = Root.child(FootstepPlan).topic(Data);

   public static final Topic<Boolean> ComputePathTopic = Root.child(FootstepPlan).topic(ComputePath);
   public static final Topic<FootstepPlannerParameters> PlannerParametersTopic = Root.child(Parameters).topic(FootstepPlannerParameters);
   public static final Topic<Double> PlannerTimeoutTopic = Root.child(FootstepPlan).topic(PlannerTimeout);
   public static final Topic<Double> PlannerTimeTakenTopic = Root.child(FootstepPlan).topic(PlannerTimeTaken);
   public static final Topic<Double> PlannerHorizonLengthTopic = Root.child(FootstepPlan).topic(PlannerHorizonLength);
   public static final Topic<FootstepPlannerType> PlannerTypeTopic = Root.child(FootstepPlan).topic(FootstepPlannerType);
   public static final Topic<FootstepPlanningResult> PlanningResultTopic = Root.child(FootstepPlan).topic(FootstepPlannerResult);
   public static final Topic<FootstepPlannerStatus> PlannerStatusTopic = Root.child(FootstepPlan).topic(FootstepPlannerStatus);
   public static final Topic<Integer> PlannerRequestIdTopic = Root.child(FootstepPlan).topic(PlannerRequestId);
   public static final Topic<Integer> SequenceIdTopic = Root.child(FootstepPlan).topic(SequenceId);

   public static final Topic<Boolean> StartPositionEditModeEnabledTopic = Root.child(Start).child(EditMode).child(PositionTheme).topic(Enable);
   public static final Topic<Boolean> GoalPositionEditModeEnabledTopic = Root.child(Goal).child(EditMode).child(PositionTheme).topic(Enable);

   public static final Topic<Boolean> StartOrientationEditModeEnabledTopic = Root.child(Start).child(EditMode).child(OrientationTheme).topic(Enable);
   public static final Topic<Boolean> GoalOrientationEditModeEnabledTopic = Root.child(Goal).child(EditMode).child(OrientationTheme).topic(Enable);

   public static final Topic<RobotSide> InitialSupportSideTopic = Root.child(Start).topic(Side);
   public static final Topic<Point3D> StartPositionTopic = Root.child(FootstepPlan).child(Start).topic(Position);
   public static final Topic<Point3D> GoalPositionTopic = Root.child(FootstepPlan).child(Goal).topic(Position);
   public static final Topic<Point3D> LowLevelGoalPositionTopic = Root.child(FootstepPlan).child(Goal).topic(Position);
   public static final Topic<Quaternion> LowLevelGoalOrientationTopic = Root.child(FootstepPlan).child(Goal).topic(Orientation);

   public static final Topic<Quaternion> StartOrientationTopic = Root.child(FootstepPlan).child(Start).topic(Orientation);
   public static final Topic<Quaternion> GoalOrientationTopic = Root.child(FootstepPlan).child(Goal).topic(Orientation);

   public static final Topic<Boolean> GlobalResetTopic = Root.topic(Reset);

   public static final Topic<Boolean> EnableNodeChecking = Root.child(NodeChecking).topic(Enable);
   public static final Topic<Point3D> NodeCheckingPosition = Root.child(NodeChecking).topic(Position);
   public static final Topic<Quaternion> NodeCheckingOrientation = Root.child(NodeChecking).topic(Orientation);

   public static final Topic<Boolean> exportUnitTestDataFile = Root.child(UnitTest).topic(Export);
   public static final Topic<String> exportUnitTestPath = Root.child(UnitTest).topic(Path);

   public static final Topic<List<? extends Point3DReadOnly>> BodyPathDataTopic = Root.child(BodyPath).topic(Data);

   public static final Topic<Boolean> ShowBodyPath = Root.child(BodyPath).topic(Show);

   public static final Topic<List<NavigableRegion>> NavigableRegionData = Root.child(VisibilityGraphs).topic(Data);

   public static final Topic<VisibilityMapHolder> StartVisibilityMap = Root.child(VisibilityGraphs).child(Start).child(Map).topic(Data);
   public static final Topic<VisibilityMapHolder> GoalVisibilityMap = Root.child(VisibilityGraphs).child(Goal).child(Map).topic(Data);
   public static final Topic<VisibilityMapHolder> InterRegionVisibilityMap = Root.child(VisibilityGraphs).child(InterRegion).child(Map).topic(Data);


   public static final Topic<Boolean> ShowClusterRawPoints = Root.child(VisibilityGraphs).child(Cluster).topic(Show);
   public static final Topic<Boolean> ShowClusterNavigableExtrusions = Root.child(VisibilityGraphs).child(Cluster).topic(Show);
   public static final Topic<Boolean> ShowClusterNonNavigableExtrusions = Root.child(VisibilityGraphs).child(Cluster).topic(Show);

   public static final Topic<Boolean> ShowStartVisibilityMap = Root.child(VisibilityGraphs).child(Start).child(Map).topic(Show);
   public static final Topic<Boolean> ShowGoalVisibilityMap = Root.child(VisibilityGraphs).child(Goal).child(Map).topic(Show);

   public static final Topic<Boolean> ShowInterRegionVisibilityMap = Root.child(VisibilityGraphs).child(InterRegion).child(Map).topic(Show);

   public static final Topic<Boolean> ShowNavigableRegionVisibilityMaps = Root.child(VisibilityGraphs).child(Map).topic(Show);



   private static final TypedTopicTheme<Boolean> ValidNode = apiFactory.createTypedTopicTheme("ValidNode");
   private static final TypedTopicTheme<Pose3D> FootstepPose = apiFactory.createTypedTopicTheme("FootstepPose");

   public static final Topic<Boolean> ValidNodeTopic = Root.child(NodeChecking).topic(ValidNode);
   public static final Topic<Pose3D> FootstepPoseTopic = Root.child(NodeChecking).topic(FootstepPose);

   public static final MessagerAPI API = apiFactory.getAPIAndCloseFactory();
}