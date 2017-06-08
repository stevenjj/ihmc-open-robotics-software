package us.ihmc.atlas.behaviorTests;

import static org.junit.Assert.*;

import org.junit.Test;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.behaviorTests.AvatarHumanCoManipulationBehaviorTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.DRCRobotModel.RobotTarget;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class AtlasHumanCoManipulationBehaviorTest extends AvatarHumanCoManipulationBehaviorTest{

	AtlasRobotVersion version = AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ;
	AtlasRobotModel robotModel = new AtlasRobotModel(version, RobotTarget.SCS, false);
	
	@Override
	@ContinuousIntegrationTest(estimatedDuration = 0.1, categoriesOverride = IntegrationCategory.EXCLUDE)
   @Test
	public void testBehavior() throws SimulationExceededMaximumTimeException
	{
		super.testBehavior();
	}

	@Override
	public DRCRobotModel getRobotModel() {
		return robotModel;
	}

	@Override
	public String getSimpleRobotName() {
		return robotModel.getSimpleRobotName();
	}
}