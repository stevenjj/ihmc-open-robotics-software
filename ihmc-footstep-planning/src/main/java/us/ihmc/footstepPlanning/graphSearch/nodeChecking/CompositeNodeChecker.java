package us.ihmc.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.robotics.geometry.PlanarRegionsList;

import java.util.ArrayList;

public class CompositeNodeChecker implements FootstepNodeChecker
{
   private final ArrayList<FootstepNodeChecker> footstepNodeCheckers = new ArrayList<>();

   public void addFootstepNodeChecker(FootstepNodeChecker footstepNodeChecker)
   {
      footstepNodeCheckers.add(footstepNodeChecker);
   }

   @Override
   public void setPlanarRegions(PlanarRegionsList planarRegions)
   {
      for (int i = 0; i < footstepNodeCheckers.size(); i++)
         footstepNodeCheckers.get(i).setPlanarRegions(planarRegions);
   }

   @Override
   public boolean isNodeValid(FootstepNode node, FootstepNode previousNode)
   {
      for (int i = 0; i < footstepNodeCheckers.size(); i++)
      {
         if (!footstepNodeCheckers.get(i).isNodeValid(node, previousNode))
            return false;
      }

      return true;
   }

   @Override
   public void addStartNode(FootstepNode startNode, RigidBodyTransform startNodeTransform)
   {
      for (int i = 0; i < footstepNodeCheckers.size(); i++)
         footstepNodeCheckers.get(i).addStartNode(startNode, startNodeTransform);
   }
}
