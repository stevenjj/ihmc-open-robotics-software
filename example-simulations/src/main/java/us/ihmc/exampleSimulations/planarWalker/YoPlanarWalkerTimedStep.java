package us.ihmc.exampleSimulations.planarWalker;

import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.quadrupedRobotics.util.YoTimeInterval;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.robotics.robotSide.RobotSide;

public class YoPlanarWalkerTimedStep extends PlanarWalkerTimedStep
{
   private final YoEnum<RobotSide> robotSide;
   private final YoTimeInterval timeInterval;
   
   YoPlanarWalkerTimedStep(String prefix, YoVariableRegistry registry)
   {
      super();
      this.robotSide = new YoEnum<>(prefix + "RobotSide", registry, RobotSide.class);
      this.timeInterval = new YoTimeInterval(prefix + "TimeInterval", registry);
   }

   @Override
   public RobotSide getRobotSide()
   {
      return this.robotSide.getEnumValue();
   }

   @Override
   public void setRobotSide(RobotSide robotSide)
   {
      this.robotSide.set(robotSide);
   }

   @Override
   public TimeInterval getTimeInterval()
   {
      return this.timeInterval;
   }

   @Override
   public void getTimeInterval(TimeInterval timeInterval)
   {
      this.timeInterval.get(timeInterval);
   }

   @Override
   public void setTimeInterval(TimeInterval timeInterval)
   {
      this.timeInterval.set(timeInterval);
   }
}
