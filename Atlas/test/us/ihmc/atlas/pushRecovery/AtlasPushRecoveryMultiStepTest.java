package us.ihmc.atlas.pushRecovery;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.DRCPushRecoveryMultiStepTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.IN_DEVELOPMENT})
public class AtlasPushRecoveryMultiStepTest extends DRCPushRecoveryMultiStepTest
{
   @Override
   public DRCRobotModel getRobotModel()
   {
      return new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false);
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }

   @Override
   protected void setForwardPushParameters()
   {
      forceMagnitude = 750.0;
      forceDuration = 0.2;
   }

   @Override
   protected void setBackwardPushParameters()
   {
      forceMagnitude = -700.0;
      forceDuration = 0.2;
   }
}
