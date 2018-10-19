package us.ihmc.valkyrie.parameters;

import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;

public class ValkyrieFootstepPlannerParameters implements FootstepPlannerParameters
{
   @Override
   public boolean checkForBodyBoxCollisions()
   {
      return true;
   }

   @Override
   public double getBodyBoxDepth()
   {
      return 0.35;
   }

   @Override
   public double getBodyBoxWidth()
   {
      return 0.6;
   }

   @Override
   public double getBodyBoxCenterHeight()
   {
      return 0.6;
   }

   @Override
   public double getBodyBoxHeight()
   {
      return 1.0;
   }

   @Override
   public double getIdealFootstepWidth()
   {
      return 0.2;
   }

   @Override
   public double getIdealFootstepLength()
   {
      return 0.2;
   }

   @Override
   public double getMaximumStepReach()
   {
      return 0.55;
   }

   @Override
   public double getMaximumStepYaw()
   {
      return 1.0;
   }

   @Override
   public double getMinimumStepYaw()
   {
      return -0.15;
   }

   @Override
   public double getMinimumStepWidth()
   {
      return 0.15;
   }

   @Override
   public double getMaximumStepZ()
   {
      return 0.1;
   }

   @Override
   public double getMaximumStepWidth()
   {
      return 0.35;
   }
   
   @Override
   public boolean getReturnBestEffortPlan()
   {
      return true;
   }
   
   @Override
   public int getMinimumStepsForBestEffortPlan()
   {
      return 3;
   }

   @Override
   public double getMinXClearanceFromStance()
   {
      return 0.2;
   }

   @Override
   public double getMinYClearanceFromStance()
   {
      return 0.15;
   }
}
