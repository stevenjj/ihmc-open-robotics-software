package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerCostParameters;

public class PitchAndRollBasedCost implements FootstepCost
{
   private final FootstepPlannerCostParameters parameters;

   public PitchAndRollBasedCost(FootstepPlannerCostParameters parameters)
   {
      this.parameters = parameters;
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      if (!endNode.hasPitch() || !endNode.hasRoll())
         return 0.0;

      return parameters.getPitchWeight() * Math.abs(endNode.getPitch()) + parameters.getRollWeight() * Math.abs(endNode.getRoll());
   }
}
