package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.graphSearch.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.robotics.geometry.AngleTools;

public class QuadraticDistanceAndYawCost implements FootstepCost
{
   private final FootstepPlannerParameters parameters;

   private static final double xCost = 1.0;
   private static final double yCost = 2.0;
   private static final double yawCostScaler = 10.0;

   public QuadraticDistanceAndYawCost(FootstepPlannerParameters parameters)
   {
      this.parameters = parameters;
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      // TODO is this in the correct frame? No, no it's not.
      Point2D startPoint = startNode.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      Point2D endPoint = endNode.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth());
      double euclideanDistance = startPoint.distance(endPoint);
      double cost = xCost * Math.pow(endPoint.getX() - startPoint.getX(), 2.0);
      cost += yCost * Math.pow(endPoint.getY() - startPoint.getY(), 2.0);

      double yaw = AngleTools.computeAngleDifferenceMinusPiToPi(startNode.getYaw(), endNode.getYaw());

      cost += yawCostScaler * euclideanDistance * parameters.getYawWeight() * Math.abs(yaw);

      return cost;
   }
}
