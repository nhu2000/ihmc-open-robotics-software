package us.ihmc.avatar.behaviorTests.rrtPlannerTests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxModule;
import us.ihmc.avatar.networkProcessor.rrtToolboxModule.ConstrainedWholeBodyPlanningToolboxController;
import us.ihmc.avatar.networkProcessor.rrtToolboxModule.ConstrainedWholeBodyPlanningToolboxModule;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.KinematicsToolboxOutputStatus;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.SetBooleanParameterPacket;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.graphicsDescription.Graphics3DObject;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.CuttingWallBehaviorStateMachine;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.CuttingWallBehaviorStateMachine.CuttingWallBehaviorState;
import us.ihmc.humanoidBehaviors.behaviors.primitives.PlanConstrainedWholeBodyTrajectoryBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.WholeBodyInverseKinematicsBehavior;
import us.ihmc.humanoidRobotics.communication.packets.ConstrainedWholeBodyPlanningToolboxOutputConverter;
import us.ihmc.humanoidRobotics.communication.packets.KinematicsToolboxOutputConverter;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.WallPosePacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.constrainedWholeBodyPlanning.ConstrainedEndEffectorTrajectory;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.constrainedWholeBodyPlanning.ConstrainedWholeBodyPlanningToolboxOutputStatus;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.manipulation.planning.rrt.constrainedplanning.configurationAndTimeSpace.DrawingTrajectory;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationConstructionSetTools.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationConstructionSetTools.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

public abstract class AvatarCuttingWallBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private static final boolean visualize = !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer();

   private static boolean visulaizerOn = true;

   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   private ConstrainedWholeBodyPlanningToolboxModule cwbPlanningToolboxModule;
   private KinematicsToolboxModule kinematicsToolboxModule;
   private PacketCommunicator kinematicsToolboxCommunicator;
   private PacketCommunicator cwbToolboxCommunicator;

   private void setupToolboxModule() throws IOException
   {
      DRCRobotModel robotModel = getRobotModel();
      FullHumanoidRobotModel fullRobotModel = robotModel.createFullRobotModel();
      kinematicsToolboxModule = new KinematicsToolboxModule(robotModel, false);
      cwbPlanningToolboxModule = new ConstrainedWholeBodyPlanningToolboxModule(robotModel, fullRobotModel, null, visulaizerOn);
      kinematicsToolboxCommunicator = drcBehaviorTestHelper.createAndStartPacketCommunicator(NetworkPorts.KINEMATICS_TOOLBOX_MODULE_PORT,
                                                                                   PacketDestination.KINEMATICS_TOOLBOX_MODULE);
      cwbToolboxCommunicator = drcBehaviorTestHelper.createAndStartPacketCommunicator(NetworkPorts.CONSTRAINED_WHOLE_BODY_PLANNING_TOOLBOX_MODULE_PORT,
                                                                                   PacketDestination.CONSTRAINED_WHOLE_BODY_PLANNING_TOOLBOX_MODULE);
   }

   private void showUpFullRobotModelWithConfiguration(FullHumanoidRobotModel createdFullRobotModel) throws SimulationExceededMaximumTimeException
   {
      for (int i = 0; i < createdFullRobotModel.getOneDoFJoints().length; i++)
      {
         double jointPosition = createdFullRobotModel.getOneDoFJoints()[i].getQ();
         Joint scsJoint = drcBehaviorTestHelper.getRobot().getJoint(createdFullRobotModel.getOneDoFJoints()[i].getName());

         if (scsJoint instanceof PinJoint)
         {
            PinJoint pinJoint = (PinJoint) scsJoint;
            pinJoint.setQ(jointPosition);
         }
         else
         {
            PrintTools.info(createdFullRobotModel.getOneDoFJoints()[i].getName() + " was not a PinJoint.");
         }
      }

      FloatingJoint scsRootJoint = drcBehaviorTestHelper.getRobot().getRootJoint();
      scsRootJoint.setQuaternion(new Quaternion(createdFullRobotModel.getRootJoint().getRotationForReading()));
      scsRootJoint.setPosition(new Point3D(createdFullRobotModel.getRootJoint().getTranslationForReading()));

      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(0.005);
   }

   public ArrayList<Graphics3DObject> getXYZAxis(Pose3D pose)
   {
      double axisHeight = 0.05;
      double axisRadius = 0.005;
      ArrayList<Graphics3DObject> ret = new ArrayList<Graphics3DObject>();

      Graphics3DObject retX = new Graphics3DObject();
      Graphics3DObject retY = new Graphics3DObject();
      Graphics3DObject retZ = new Graphics3DObject();

      Point3D centerPoint = new Point3D(pose.getPosition());

      retX.translate(centerPoint);
      retY.translate(centerPoint);
      retZ.translate(centerPoint);

      RotationMatrix axisOrientation = new RotationMatrix(pose.getOrientation());

      RotationMatrix axisX = new RotationMatrix(axisOrientation);
      RotationMatrix axisY = new RotationMatrix(axisOrientation);
      RotationMatrix axisZ = new RotationMatrix(axisOrientation);

      retZ.rotate(axisZ);
      retZ.addCylinder(axisHeight, axisRadius, YoAppearance.Blue());

      axisX.appendPitchRotation(Math.PI * 0.5);
      retX.rotate(axisX);
      retX.addCylinder(axisHeight, axisRadius, YoAppearance.Red());

      axisY.appendRollRotation(-Math.PI * 0.5);
      retY.rotate(axisY);
      retY.addCylinder(axisHeight, axisRadius, YoAppearance.Green());

      ret.add(retX);
      ret.add(retY);
      ret.add(retZ);

      return ret;
   }

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      // if (visualize)
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      if (cwbPlanningToolboxModule != null)
      {
         cwbPlanningToolboxModule.destroy();
         cwbPlanningToolboxModule = null;
      }

      if (kinematicsToolboxModule != null)
      {
         kinematicsToolboxModule.destroy();
         kinematicsToolboxModule = null;
      }

      if (kinematicsToolboxCommunicator != null)
      {
         kinematicsToolboxCommunicator.closeConnection();
         kinematicsToolboxCommunicator = null;
      }
      
      if (cwbToolboxCommunicator != null)
      {
         cwbToolboxCommunicator.closeConnection();
         cwbToolboxCommunicator = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @Before
   public void setUp() throws IOException
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      CommonAvatarEnvironmentInterface environment = new FlatGroundEnvironment();

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(environment, getSimpleRobotName(), DRCObstacleCourseStartingLocation.DEFAULT,
                                                        simulationTestingParameters, getRobotModel());

      setupToolboxModule();
   }

   public void testForReachability() throws SimulationExceededMaximumTimeException, IOException
   {
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();
      drcBehaviorTestHelper.getControllerFullRobotModel().updateFrames();

      FullHumanoidRobotModel sdfFullRobotModel = drcBehaviorTestHelper.getControllerFullRobotModel();
      sdfFullRobotModel.updateFrames();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(sdfFullRobotModel);
      referenceFrames.updateFrames();

      Point3D centerPosition = new Point3D(0.52, -0.01, 1.0164);
      Quaternion centerOrientation = new Quaternion();
      centerOrientation.appendPitchRotation(-Math.PI * 0.5);

      Point3D desiredPosition = new Point3D(centerPosition);
      desiredPosition.addY(-0.35);

      Quaternion desiredOrientation = new Quaternion();
      desiredOrientation.appendRollRotation(Math.PI * 0.5);
      desiredOrientation.appendYawRotation(Math.PI * 0.5);
      // desiredOrientation.appendPitchRotation(-Math.PI * 0.4);
      desiredOrientation.appendPitchRotation(-Math.PI * 0.5);

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      scs.addStaticLinkGraphics(getXYZAxis(new Pose3D(desiredPosition, desiredOrientation)));

      WholeBodyInverseKinematicsBehavior wholebodyBehavior = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                                    drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                                    drcBehaviorTestHelper.getSDFFullRobotModel());

      FramePose handFramePose = new FramePose(referenceFrames.getMidFootZUpGroundFrame(), desiredPosition, desiredOrientation);

      wholebodyBehavior.setTrajectoryTime(3.0);
      wholebodyBehavior.setDesiredHandPose(RobotSide.LEFT, handFramePose);

      drcBehaviorTestHelper.dispatchBehavior(wholebodyBehavior);

      desiredOrientation = new Quaternion();
      desiredOrientation.appendPitchRotation(Math.PI * 0.4);
      HandTrajectoryMessage rhandTrajectoryMessage = new HandTrajectoryMessage(RobotSide.RIGHT, 2.0, new Point3D(-0.1, -0.5, 0.7), desiredOrientation,
                                                                               referenceFrames.getMidFootZUpGroundFrame());
      drcBehaviorTestHelper.send(rhandTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(4.0);

      System.out.println("End");

   }

   public void testForCuttingWallBehavior() throws SimulationExceededMaximumTimeException, IOException
   {
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();
      drcBehaviorTestHelper.getControllerFullRobotModel().updateFrames();

      FullHumanoidRobotModel sdfFullRobotModel = drcBehaviorTestHelper.getControllerFullRobotModel();
      sdfFullRobotModel.updateFrames();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(sdfFullRobotModel);
      referenceFrames.updateFrames();

      /*
       * Behavior create.
       */
      CuttingWallBehaviorStateMachine cuttingWallBehaviorStateMachine = new CuttingWallBehaviorStateMachine(getRobotModel(),
                                                                                                            drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                                            drcBehaviorTestHelper.getYoTime(),
                                                                                                            sdfFullRobotModel, referenceFrames);

      referenceFrames.updateFrames();

      System.out.println("Behavior Dispatch");
      drcBehaviorTestHelper.dispatchBehavior(cuttingWallBehaviorStateMachine);

      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      Point3D centerPosition = new Point3D(0.52, -0.01, 1.0164);
      Quaternion centerOrientation = new Quaternion();
      centerOrientation.appendPitchRotation(-Math.PI * 0.5);
      WallPosePacket wallPosePacket = new WallPosePacket(0.35, centerPosition, centerOrientation);

      System.out.println("wallPosePacket Dispatch");
      drcBehaviorTestHelper.getBehaviorCommunicationBridge().sendPacketToBehavior(wallPosePacket);
      System.out.println("wallPosePacket Dispatch done");

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      double yoTime = 0.0;
      while (yoTime < 10000)
      {
         assertTrue(drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(0.001));
         yoTime = drcBehaviorTestHelper.getYoTime().getDoubleValue();
         if (cuttingWallBehaviorStateMachine.getStateMachine().getCurrentStateEnum() == CuttingWallBehaviorState.WAITING_CONFIRM)
         {
            PrintTools.info("Motion START!");

            SetBooleanParameterPacket confirmPacket = new SetBooleanParameterPacket("", true);

            System.out.println("confirmPacket Dispatch");
            drcBehaviorTestHelper.getBehaviorCommunicationBridge().sendPacketToBehavior(confirmPacket);

            int numberOfDiplayedWayPoints = 10;
            ConstrainedWholeBodyPlanningToolboxOutputConverter converter = new ConstrainedWholeBodyPlanningToolboxOutputConverter(getRobotModel());
            ConstrainedWholeBodyPlanningToolboxOutputStatus constrainedWholeBodyPlanningToolboxOutputStatus = cuttingWallBehaviorStateMachine.getPlanConstrainedWholeBodyTrajectoryBehavior()
                                                                                                                                             .getConstrainedWholeBodyPlanningToolboxOutputStatus();

            for (int i = 0; i < numberOfDiplayedWayPoints; i++)
            {
               int length = constrainedWholeBodyPlanningToolboxOutputStatus.getTrajectoryTimes().length;
               double trajectoryTime = constrainedWholeBodyPlanningToolboxOutputStatus.getTrajectoryTimes()[length - 1];
               double time = trajectoryTime * (double) (i) / (double) (numberOfDiplayedWayPoints);

               KinematicsToolboxOutputStatus robotConfiguration = converter.getRobotConfiguration(constrainedWholeBodyPlanningToolboxOutputStatus, time);
               KinematicsToolboxOutputConverter kinematicConverter = new KinematicsToolboxOutputConverter(getRobotModel());
               kinematicConverter.updateFullRobotModel(robotConfiguration);

               FullHumanoidRobotModel robotModel = kinematicConverter.getFullRobotModel();
               Pose3D lHandPose = new Pose3D(robotModel.getHand(RobotSide.LEFT).getBodyFixedFrame().getTransformToWorldFrame());
               lHandPose.appendTranslation(0.0, -ConstrainedWholeBodyPlanningToolboxController.handCoordinateOffsetX, 0.0);
               scs.addStaticLinkGraphics(getXYZAxis(lHandPose));
               
               Pose3D rHandPose = new Pose3D(robotModel.getHand(RobotSide.RIGHT).getBodyFixedFrame().getTransformToWorldFrame());
               rHandPose.appendTranslation(0.0, ConstrainedWholeBodyPlanningToolboxController.handCoordinateOffsetX, 0.0);
               scs.addStaticLinkGraphics(getXYZAxis(rHandPose));
            }

            break;
         }
      }

      assertTrue(drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(30.0));

      System.out.println("End");
   }

   public void testForCWBPlanningBehavior() throws SimulationExceededMaximumTimeException, IOException
   {
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();
      drcBehaviorTestHelper.getControllerFullRobotModel().updateFrames();

      FullHumanoidRobotModel sdfFullRobotModel = drcBehaviorTestHelper.getControllerFullRobotModel();
      sdfFullRobotModel.updateFrames();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(sdfFullRobotModel);
      referenceFrames.updateFrames();

      /*
       * reaching initial configuration
       */
      Quaternion initialOrientation = new Quaternion();
      initialOrientation.appendRollRotation(Math.PI * 0.5);
      initialOrientation.appendYawRotation(Math.PI * 0.5);
      initialOrientation.appendPitchRotation(-Math.PI * 0.4);
      HandTrajectoryMessage lhandTrajectoryMessage = new HandTrajectoryMessage(RobotSide.LEFT, 2.0, new Point3D(0.6, 0.35, 1.0), initialOrientation,
                                                                               referenceFrames.getMidFootZUpGroundFrame());
      drcBehaviorTestHelper.send(lhandTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(getRobotModel().getControllerDT());

      initialOrientation = new Quaternion();
      initialOrientation.appendPitchRotation(Math.PI * 0.4);
      HandTrajectoryMessage rhandTrajectoryMessage = new HandTrajectoryMessage(RobotSide.RIGHT, 2.0, new Point3D(-0.1, -0.5, 0.7), initialOrientation,
                                                                               referenceFrames.getMidFootZUpGroundFrame());
      drcBehaviorTestHelper.send(rhandTrajectoryMessage);
      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(4.0);

      sdfFullRobotModel.updateFrames();
      /*
       * Behavior create.
       */
      PlanConstrainedWholeBodyTrajectoryBehavior planningBehavior = new PlanConstrainedWholeBodyTrajectoryBehavior("PlanningBehavior", getRobotModel(),
                                                                                                                   drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                                                   sdfFullRobotModel,
                                                                                                                   drcBehaviorTestHelper.getYoTime());

      ConstrainedEndEffectorTrajectory endeffectorTrajectory = new DrawingTrajectory(20.0);
      PrintTools.info("" + endeffectorTrajectory.getTrajectoryTime());

      planningBehavior.setInputs(endeffectorTrajectory, sdfFullRobotModel);
      PlanConstrainedWholeBodyTrajectoryBehavior.constrainedEndEffectorTrajectory = endeffectorTrajectory;

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();

      System.out.println("Behavior Dispatch");
      drcBehaviorTestHelper.dispatchBehavior(planningBehavior);

      drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(15.0);
      
      System.out.println("Go Motion " + drcBehaviorTestHelper.getYoTime());

      PrintTools.info("planningResult " + planningBehavior.getConstrainedWholeBodyPlanningToolboxOutputStatus().getPlanningResult());

      System.out.println("End " + drcBehaviorTestHelper.getYoTime());

   }

}