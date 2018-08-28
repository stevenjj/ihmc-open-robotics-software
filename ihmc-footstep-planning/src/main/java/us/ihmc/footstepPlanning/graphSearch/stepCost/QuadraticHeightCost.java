package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerCostParameters;

public class QuadraticHeightCost implements FootstepCost
{
   private static final double stepHeightScalar = 10.0;

   private final FootstepPlannerCostParameters parameters;

   public QuadraticHeightCost(FootstepPlannerCostParameters parameters)
   {
      this.parameters = parameters;
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      if (!startNode.hasZ() || !endNode.hasZ())
         return 0.0;

      double heightChange = endNode.getZ() - startNode.getZ();

      if (heightChange > 0.0)
         return parameters.getStepUpWeight() * Math.pow(stepHeightScalar * heightChange, 2.0);
      else
         return parameters.getStepDownWeight() * Math.pow(stepHeightScalar * heightChange, 2.0);
   }
}
