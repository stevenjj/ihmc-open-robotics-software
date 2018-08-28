package us.ihmc.footstepPlanning.graphSearch.stepCost;

import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerCostParameters;

public class HeightCost implements FootstepCost
{
   private final FootstepPlannerCostParameters parameters;

   private final LinearHeightCost linearHeightCost;
   private final QuadraticHeightCost quadraticHeightCost;

   public HeightCost(FootstepPlannerCostParameters parameters)
   {
      this.parameters = parameters;

      linearHeightCost = new LinearHeightCost(parameters);
      quadraticHeightCost = new QuadraticHeightCost(parameters);
   }

   @Override
   public double compute(FootstepNode startNode, FootstepNode endNode)
   {
      if (parameters.useQuadraticHeightCost())
         return quadraticHeightCost.compute(startNode, endNode);
      else
         return linearHeightCost.compute(startNode, endNode);
   }
}
