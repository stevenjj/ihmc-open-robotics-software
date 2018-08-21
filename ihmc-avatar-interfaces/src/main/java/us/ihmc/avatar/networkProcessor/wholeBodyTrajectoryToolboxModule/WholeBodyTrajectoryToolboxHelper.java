package us.ihmc.avatar.networkProcessor.wholeBodyTrajectoryToolboxModule;

import java.util.Arrays;
import java.util.Collection;

import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;

public class WholeBodyTrajectoryToolboxHelper
{
   public static double kinematicsChainLimitScore(RigidBody start, RigidBody end)
   {
      return jointsLimitScore(ScrewTools.createOneDoFJointPath(start, end));
   }

   public static double jointsLimitScore(Collection<OneDoFJoint> jointsToScore)
   {
      return jointsToScore.stream().mapToDouble(j -> jointLimitScore(j)).sum();
   }

   public static double jointsLimitScore(OneDoFJoint... jointsToScore)
   {
      return Arrays.stream(jointsToScore).mapToDouble(j -> jointLimitScore(j)).sum();
   }

   /**
    * This joint limit score is defined in the following paper (proposed by Yoshikawa).
    * <p>
    * Nelson, Khosla, "Strategies for increasing the tracking region of an eye-in-hand system by
    * singularity and joint limit avoidance.", The International journal of robotics research 14.3
    * (1995): 255-269.
    * <p>
    * See {@link
    * <p>
    * http://repository.cmu.edu/cgi/viewcontent.cgi?article=1581&context=isr}
    * <p>
    * See equation 24, 26. Joint limit score is stated on the head of natural exponential.
    * 
    */
   public static double jointLimitScore(OneDoFJoint jointToScore)
   {
      double q = jointToScore.getQ();
      double upperLimit = jointToScore.getJointLimitUpper();
      double lowerLimit = jointToScore.getJointLimitLower();

      double motionRange = upperLimit - lowerLimit;

      double diffUpper = upperLimit - q;
      double diffLower = q - lowerLimit;

      // Yoshikawa's definition.
      return diffUpper * diffLower / (motionRange * motionRange);
   }
}