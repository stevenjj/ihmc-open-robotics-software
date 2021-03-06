package us.ihmc.footstepPlanning.graphSearch.heuristics;

import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.yoVariables.variable.YoDouble;

public class ZeroHeuristics extends CostToGoHeuristics
{
   public ZeroHeuristics(YoDouble weight)
   {
      super(weight);
   }

   @Override
   protected double computeHeuristics(FootstepNode node, FootstepNode goalNode)
   {
      return 0.0;
   }
}
