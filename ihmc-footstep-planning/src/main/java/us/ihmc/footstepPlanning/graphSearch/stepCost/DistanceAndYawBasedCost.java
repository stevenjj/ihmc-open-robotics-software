package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;

public class DistanceAndYawBasedCost implements FootstepCost
{
   private final FootstepPlannerParameters parameters;
   private final EuclideanDistanceAndYawBasedCost euclideanCost;
   private final QuadraticDistanceAndYawCost quadraticCost;

   public DistanceAndYawBasedCost(FootstepPlannerParameters parameters)
   {
      this.parameters = parameters;
      
      euclideanCost = new EuclideanDistanceAndYawBasedCost(parameters);
      quadraticCost = new QuadraticDistanceAndYawCost(parameters);
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      if (parameters.getCostParameters().useQuadraticDistanceCost())
         return quadraticCost.compute(startNode, endNode);
      else
         return euclideanCost.compute(startNode, endNode);
   }
}
