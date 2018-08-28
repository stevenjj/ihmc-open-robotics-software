package us.ihmc.footstepPlanning.graphSearch.parameters;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class YoFootstepPlannerCostParameters implements FootstepPlannerCostParameters
{
   private final YoDouble yawWeight;
   private final YoDouble forwardWeight;
   private final YoDouble lateralWeight;
   private final YoDouble stepUpWeight;
   private final YoDouble stepDownWeight;
   private final YoDouble rollWeight;
   private final YoDouble pitchWeight;
   private final YoDouble costPerStep;
   
   private final YoBoolean useQuadraticDistanceCost;

   public YoFootstepPlannerCostParameters(YoVariableRegistry registry, FootstepPlannerCostParameters defaults)
   {
      yawWeight = new YoDouble("yawWeight", registry);
      forwardWeight = new YoDouble("forwardWeight", registry);
      lateralWeight = new YoDouble("lateralWeight", registry);
      stepUpWeight = new YoDouble("stepUpWeight", registry);
      stepDownWeight = new YoDouble("stepDownWeight", registry);
      rollWeight = new YoDouble("rollWeight", registry);
      pitchWeight = new YoDouble("pitchWeight", registry);
      costPerStep = new YoDouble("costPerStep", registry);
      
      useQuadraticDistanceCost = new YoBoolean("useQuadraticDistanceCost", registry);

      set(defaults);
   }

   public void set(FootstepPlannerCostParameters defaults)
   {
      yawWeight.set(defaults.getYawWeight());
      forwardWeight.set(defaults.getForwardWeight());
      lateralWeight.set(defaults.getLateralWeight());
      stepUpWeight.set(defaults.getStepUpWeight());
      stepDownWeight.set(defaults.getStepDownWeight());
      rollWeight.set(defaults.getRollWeight());
      pitchWeight.set(defaults.getPitchWeight());
      costPerStep.set(defaults.getCostPerStep());
      
      useQuadraticDistanceCost.set(defaults.useQuadraticDistanceCost());;
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

   @Override
   public double getStepUpWeight()
   {
      return stepUpWeight.getDoubleValue();
   }

   @Override
   public double getStepDownWeight()
   {
      return stepDownWeight.getDoubleValue();
   }

   @Override
   public double getRollWeight()
   {
      return rollWeight.getDoubleValue();
   }

   @Override
   public double getPitchWeight()
   {
      return pitchWeight.getDoubleValue();
   }
   
   @Override
   public boolean useQuadraticDistanceCost()
   {
      return useQuadraticDistanceCost.getBooleanValue();
   }
}
