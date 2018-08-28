package us.ihmc.footstepPlanning.graphSearch.parameters;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class YoFootstepPlannerCostParameters implements FootstepPlannerCostParameters
{
   private final YoDouble yawWeight;
   private final YoDouble forwardWeight;
   private final YoDouble lateralWeight;
   private final YoDouble costPerStep;

   public YoFootstepPlannerCostParameters(YoVariableRegistry registry, FootstepPlannerCostParameters defaults)
   {
      yawWeight = new YoDouble("yawWeight", registry);
      forwardWeight = new YoDouble("forwardWeight", registry);
      lateralWeight = new YoDouble("lateralWeight", registry);
      costPerStep = new YoDouble("costPerStep", registry);

      set(defaults);
   }

   public void set(FootstepPlannerCostParameters defaults)
   {
      yawWeight.set(defaults.getYawWeight());
      forwardWeight.set(defaults.getForwardWeight());
      lateralWeight.set(defaults.getLateralWeight());
      costPerStep.set(defaults.getCostPerStep());
   }

   @Override
   public double getYawWeight()
   {
      return yawWeight.getDoubleValue();
   }

   @Override
   public double getCostPerStep()
   {
      return costPerStep.getDoubleValue();
   }

   @Override
   public double getForwardWeight()
   {
      return forwardWeight.getDoubleValue();
   }

   @Override
   public double getLateralWeight()
   {
      return lateralWeight.getDoubleValue();
   }
}
