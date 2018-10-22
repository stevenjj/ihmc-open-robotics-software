package us.ihmc.footstepPlanning.ui.components;

import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.graphSearch.parameters.DefaultFootstepPlanningParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapAndWiggler;
import us.ihmc.footstepPlanning.graphSearch.nodeChecking.SnapAndWiggleBasedNodeChecker;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.nodeExpansion.ParameterBasedNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.planners.AStarFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.BodyPathBasedFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.DepthFirstFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.planners.VisibilityGraphWithAStarPlanner;
import us.ihmc.footstepPlanning.graphSearch.stepCost.ConstantFootstepCost;
import us.ihmc.footstepPlanning.simplePlanners.PlanThenSnapPlanner;
import us.ihmc.footstepPlanning.simplePlanners.TurnWalkTurnPlanner;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.javaFXToolkit.messager.Messager;
import us.ihmc.javaFXToolkit.messager.SharedMemoryMessager;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI.*;

public class FootstepPathCalculatorModule
{
   private static final boolean VERBOSE = true;

   private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));

   private final AtomicReference<PlanarRegionsList> planarRegionsReference;
   private final AtomicReference<Point3D> startPositionReference;
   private final AtomicReference<Quaternion> startOrientationReference;
   private final AtomicReference<RobotSide> initialStanceSideReference;
   private final AtomicReference<Point3D> goalPositionReference;
   private final AtomicReference<Quaternion> goalOrientationReference;
   private final AtomicReference<FootstepPlannerType> footstepPlannerTypeReference;

   private final AtomicReference<Double> plannerTimeoutReference;
   private final AtomicReference<Double> plannerHorizonLengthReference;

   private final AtomicReference<FootstepPlannerParameters> parameters;

   private final Messager messager;

   public FootstepPathCalculatorModule(Messager messager)
   {
      this.messager = messager;

      planarRegionsReference = messager.createInput(PlanarRegionDataTopic);
      startPositionReference = messager.createInput(StartPositionTopic);
      startOrientationReference = messager.createInput(StartOrientationTopic, new Quaternion());
      initialStanceSideReference = messager.createInput(InitialSupportSideTopic, RobotSide.LEFT);
      goalPositionReference = messager.createInput(GoalPositionTopic);
      goalOrientationReference = messager.createInput(GoalOrientationTopic, new Quaternion());
      parameters = messager.createInput(PlannerParametersTopic, new DefaultFootstepPlanningParameters());
      footstepPlannerTypeReference = messager.createInput(PlannerTypeTopic, FootstepPlannerType.A_STAR);
      plannerTimeoutReference = messager.createInput(PlannerTimeoutTopic, 5.0);
      plannerHorizonLengthReference = messager.createInput(PlannerHorizonLengthTopic, 1.0);

      messager.registerTopicListener(ComputePathTopic, request -> computePathOnThread());
   }

   public void clear()
   {
      planarRegionsReference.set(null);
      startPositionReference.set(null);
      startOrientationReference.set(null);
      initialStanceSideReference.set(null);
      goalPositionReference.set(null);
      goalOrientationReference.set(null);
      plannerTimeoutReference.set(null);
      plannerHorizonLengthReference.set(null);
   }

   public void start()
   {
   }

   public void stop()
   {
      executorService.shutdownNow();
   }

   private void computePathOnThread()
   {
      executorService.submit(this::computePath);
   }

   private void computePath()
   {
      if (VERBOSE)
      {
         PrintTools.info(this, "Starting to compute path...");
      }

      PlanarRegionsList planarRegionsList = planarRegionsReference.get();

      if (planarRegionsList == null)
         return;

      Point3D start = startPositionReference.get();

      if (start == null)
         return;

      Point3D goal = goalPositionReference.get();

      if (goal == null)
         return;

      if (VERBOSE)
         PrintTools.info(this, "Computing footstep path.");

      try
      {
         FootstepPlanner planner = createPlanner();

         planner.setPlanarRegions(planarRegionsList);
         planner.setTimeout(plannerTimeoutReference.get());
         planner.setPlanningHorizonLength(plannerHorizonLengthReference.get());

         planner.setInitialStanceFoot(new FramePose3D(ReferenceFrame.getWorldFrame(), start, startOrientationReference.get()), initialStanceSideReference.get());

         FootstepPlannerGoal plannerGoal = new FootstepPlannerGoal();
         plannerGoal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
         plannerGoal.setGoalPoseBetweenFeet(new FramePose3D(ReferenceFrame.getWorldFrame(), goal, goalOrientationReference.get()));
         planner.setGoal(plannerGoal);

         messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.PLANNING_PATH);

         FootstepPlanningResult planningResult = planner.planPath();
         BodyPathPlan bodyPathPlan = null;
         if (planningResult.validForExecution())
         {
            bodyPathPlan = planner.getPathPlan();
            messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.PLANNING_STEPS);

            List<Point3DReadOnly> bodyPath = new ArrayList<>();
            for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
               bodyPath.add(bodyPathPlan.getWaypoint(i));
            messager.submitMessage(BodyPathDataTopic, bodyPath);
            messager.submitMessage(PlanningResultTopic, planningResult);

            planningResult = planner.plan();
         }

         FootstepPlan footstepPlan  = planner.getPlan();

         if (VERBOSE)
         {
            PrintTools.info(this, "Planner result: " + planningResult);
            if (planningResult.validForExecution())
               PrintTools.info(this, "Planner result: " + planner.getPlan().getNumberOfSteps() + " steps, taking " + planner.getPlanningDuration() + " s.");
         }

         messager.submitMessage(PlanningResultTopic, planningResult);
         messager.submitMessage(PlannerTimeTakenTopic, planner.getPlanningDuration());
         messager.submitMessage(PlannerStatusTopic, FootstepPlannerStatus.IDLE);


         if (planningResult.validForExecution())
         {
            messager.submitMessage(FootstepPlanTopic, footstepPlan);
            messager.submitMessage(LowLevelGoalPositionTopic, new Point3D(footstepPlan.getLowLevelPlanGoal().getPosition()));
            messager.submitMessage(LowLevelGoalOrientationTopic, new Quaternion(footstepPlan.getLowLevelPlanGoal().getOrientation()));
         }
      }
      catch (Exception e)
      {
         PrintTools.error(this, e.getMessage());
         e.printStackTrace();
      }

   }

   private FootstepPlanner createPlanner()
   {
      SideDependentList<ConvexPolygon2D> contactPointsInSoleFrame = PlannerTools.createDefaultFootPolygons();
      YoVariableRegistry registry = new YoVariableRegistry("visualizerRegistry");

      switch (footstepPlannerTypeReference.get())
      {
      case PLANAR_REGION_BIPEDAL:
         return createPlanarRegionBipedalPlanner(contactPointsInSoleFrame, registry);
      case PLAN_THEN_SNAP:
         return new PlanThenSnapPlanner(new TurnWalkTurnPlanner(), contactPointsInSoleFrame);
      case A_STAR:
         return createAStarPlanner(contactPointsInSoleFrame, registry);
      case SIMPLE_BODY_PATH:
         return new BodyPathBasedFootstepPlanner(parameters.get(), contactPointsInSoleFrame, registry);
      case VIS_GRAPH_WITH_A_STAR:
         return new VisibilityGraphWithAStarPlanner(parameters.get(), contactPointsInSoleFrame, null, registry);
      default:
         throw new RuntimeException("Planner type " + footstepPlannerTypeReference.get() + " is not valid!");
      }
   }

   private FootstepPlanner createAStarPlanner(SideDependentList<ConvexPolygon2D> footPolygons, YoVariableRegistry registry)
   {
      FootstepNodeExpansion expansion = new ParameterBasedNodeExpansion(parameters.get());
      return AStarFootstepPlanner.createRoughTerrainPlanner(parameters.get(), null, footPolygons, expansion, registry);
   }

   private FootstepPlanner createPlanarRegionBipedalPlanner(SideDependentList<ConvexPolygon2D> footPolygonsInSoleFrame, YoVariableRegistry registry)
   {
      FootstepNodeSnapAndWiggler snapper = new FootstepNodeSnapAndWiggler(footPolygonsInSoleFrame, parameters.get(), null);
      SnapAndWiggleBasedNodeChecker nodeChecker = new SnapAndWiggleBasedNodeChecker(footPolygonsInSoleFrame, null, parameters.get());
      ConstantFootstepCost stepCostCalculator = new ConstantFootstepCost(1.0);

      DepthFirstFootstepPlanner footstepPlanner = new DepthFirstFootstepPlanner(parameters.get(), snapper, nodeChecker, stepCostCalculator, registry);
      footstepPlanner.setFeetPolygons(footPolygonsInSoleFrame, footPolygonsInSoleFrame);
      footstepPlanner.setMaximumNumberOfNodesToExpand(Integer.MAX_VALUE);
      footstepPlanner.setExitAfterInitialSolution(false);

      return footstepPlanner;
   }

   public static FootstepPathCalculatorModule createMessagerModule(SharedMemoryMessager messager)
   {
      return new FootstepPathCalculatorModule(messager);
   }
}