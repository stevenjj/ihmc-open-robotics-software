package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerCostParameters;

public class LinearHeightCost implements FootstepCost
{
   private final FootstepPlannerCostParameters parameters;

   public LinearHeightCost(FootstepPlannerCostParameters parameters)
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
         return parameters.getStepUpWeight() * heightChange;
      else
         return -parameters.getStepDownWeight() * heightChange;
   }
}
