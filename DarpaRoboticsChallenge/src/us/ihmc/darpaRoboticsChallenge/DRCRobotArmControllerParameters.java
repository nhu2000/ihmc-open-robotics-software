package us.ihmc.darpaRoboticsChallenge;

import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.*;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.robotSide.RobotSide;

public class DRCRobotArmControllerParameters implements ArmControllerParameters
{
   public double getArmJointspaceKp()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 80.0;
      return 80.0; 
   }

   public double getArmJointspaceZeta()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 0.6;
      return 0.2;  // Lots of natural damping in the arms. Don't need to damp the controllers.
   }

   public double getArmJointspaceKi()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 0.0;
      return 0.0; 
   }

   public double getArmJointspaceMaxIntegralError()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 0.0;
      return 0.0; 
   }

   public double getArmJointspaceMaxAcceleration()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return Double.POSITIVE_INFINITY;
      return 10.0;
   }

   public double getArmJointspaceMaxJerk()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return Double.POSITIVE_INFINITY;
      return 100.0;
   }

   public double getArmTaskspaceKp()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 100.0;
      return 100.0; 
   }

   public double getArmTaskspaceZeta()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 1.0;
      return 1.0; 
   }

   public double getArmTaskspaceKi()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 0.0;
      return 0.0; 
   }

   public double getArmTaskspaceMaxIntegralError()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return 0.0;
      return 0.0; 
   }

   public double getArmTaskspaceMaxAcceleration()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return Double.POSITIVE_INFINITY;
      return 10.0; 
   }

   public double getArmTaskspaceMaxJerk()
   {
      if (!DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT)  return Double.POSITIVE_INFINITY;
      return 100.0; 
   }

   public boolean useDecoupledTaskspaceControl()
   {
      return DRCConfigParameters.USE_MANIPULATION_DECOUPLED_TASKSPACE_CONTROL;
   }
   
   public String[] getDefaultHandOrientationJointNames(RobotSide robotSide)
   {
      if (robotSide == RobotSide.LEFT)
         return new String[]{jointNames[l_arm_ely], jointNames[l_arm_wry], jointNames[l_arm_wrx]};
      else
         return new String[]{jointNames[r_arm_ely], jointNames[r_arm_wry], jointNames[r_arm_wrx]};
   }

   public String[] getDefaultHandPositionJointNames(RobotSide robotSide)
   {
      if (robotSide == RobotSide.LEFT)
         return new String[]{jointNames[l_arm_shy], jointNames[l_arm_shx], jointNames[l_arm_elx]};
      else
         return new String[]{jointNames[r_arm_shy], jointNames[r_arm_shx], jointNames[r_arm_elx]};
   }

}
