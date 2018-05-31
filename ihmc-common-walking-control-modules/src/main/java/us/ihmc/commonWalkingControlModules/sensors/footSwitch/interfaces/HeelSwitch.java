package us.ihmc.commonWalkingControlModules.sensors.footSwitch.interfaces;

import us.ihmc.robotics.sensors.FootSwitchInterface;

public interface HeelSwitch extends FootSwitchInterface
{
   public abstract boolean hasHeelHitGround();
   
   public void resetHeelSwitch();
}
