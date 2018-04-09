package us.ihmc.wholeBodyController.diagnostics;

import us.ihmc.robotics.stateMachine.core.State;
import us.ihmc.outputProcessing.outputData.JointDesiredOutputListReadOnly;

public interface CalibrationState extends State
{
   public abstract JointDesiredOutputListReadOnly getOutputForLowLevelController();
}
