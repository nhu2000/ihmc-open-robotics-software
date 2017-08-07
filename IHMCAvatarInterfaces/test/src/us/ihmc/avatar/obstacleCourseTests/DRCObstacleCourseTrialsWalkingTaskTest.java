package us.ihmc.avatar.obstacleCourseTests;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.BoundingBox3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.yoVariables.dataBuffer.DataProcessingFunction;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

public abstract class DRCObstacleCourseTrialsWalkingTaskTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private static final boolean MOVE_ROBOT_FOR_VIZ = false;

   private DRCSimulationTestHelper drcSimulationTestHelper;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }


	@ContinuousIntegrationTest(estimatedDuration = 38.6)
	@Test(timeout = 190000)
   public void testStepOnCinderBlocks() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      String scriptName = "scripts/ExerciseAndJUnitScripts/TwoCinderBlocksStepOn_LeftFootTest.xml";

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.IN_FRONT_OF_TWO_HIGH_CINDERBLOCKS;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCObstacleCourseTrialsCinderBlocksTest", selectedLocation, simulationTestingParameters, getRobotModel());
      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.01);
      FramePoint pelvisHeight = new FramePoint(fullRobotModel.getRootJoint().getFrameAfterJoint());
      pelvisHeight.changeFrame(ReferenceFrame.getWorldFrame());
      PelvisHeightTrajectoryMessage message = new PelvisHeightTrajectoryMessage(0.5, pelvisHeight.getZ() + 0.05);
      drcSimulationTestHelper.send(message);
      InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream(scriptName);
      drcSimulationTestHelper.loadScriptFile(scriptInputStream, fullRobotModel.getSoleFrame(RobotSide.LEFT));

      setupCameraForWalkingOverCinderBlocks();

      ThreadTools.sleep(0);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(9.5);

      drcSimulationTestHelper.createVideo(getSimpleRobotName(), 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      Point3D center = new Point3D(13.10268850797296, 14.090724695197087, 1.146368436759061);
      Vector3D plusMinusVector = new Vector3D(0.2, 0.2, 0.5);
      BoundingBox3D boundingBox = BoundingBox3D.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }


	@ContinuousIntegrationTest(estimatedDuration = 47.3)
   @Test(timeout = 240000)
   public void testStepOnCinderBlocksSlowlyWithDisturbance() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      String scriptName = "scripts/ExerciseAndJUnitScripts/TwoCinderBlocksStepOn_LeftFootTest_slow.xml";

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.IN_FRONT_OF_TWO_HIGH_CINDERBLOCKS;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCObstacleCourseTrialsCinderBlocksTest", selectedLocation, simulationTestingParameters, getRobotModel());
      setupCameraForWalkingOverCinderBlocks();

      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      FramePoint pelvisHeight = new FramePoint(fullRobotModel.getRootJoint().getFrameAfterJoint());
      pelvisHeight.changeFrame(ReferenceFrame.getWorldFrame());
      PelvisHeightTrajectoryMessage message = new PelvisHeightTrajectoryMessage(0.5, pelvisHeight.getZ() + 0.1);
      drcSimulationTestHelper.send(message);
      InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream(scriptName);
      drcSimulationTestHelper.loadScriptFile(scriptInputStream, fullRobotModel.getSoleFrame(RobotSide.LEFT));

      ThreadTools.sleep(0);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.5);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(14.5);

      drcSimulationTestHelper.createVideo(getSimpleRobotName(), 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      Point3D center = new Point3D(13.10268850797296, 14.090724695197087, 1.146368436759061);
      Vector3D plusMinusVector = new Vector3D(0.2, 0.2, 0.5);
      BoundingBox3D boundingBox = BoundingBox3D.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      if (MOVE_ROBOT_FOR_VIZ) moveRobotOutOfWayForViz();

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   private void moveRobotOutOfWayForViz()
   {
      final SimulationConstructionSet scs = drcSimulationTestHelper.getSimulationConstructionSet();
      DataProcessingFunction dataProcessingFunction = new DataProcessingFunction()
      {
         private final YoDouble q_y = (YoDouble) scs.getVariable("q_y");

         @Override
         public void initializeProcessing()
         {
         }

         @Override
         public void processData()
         {
            q_y.sub(4.0);
         }};

      scs.applyDataProcessingFunction(dataProcessingFunction);
   }

   // @Test(timeout=300000), we don't need step on/off two layer CinderBlocks anymore
   //Note: this test will fail because of bounding box that needs to be "tuned"
   public void testStepOnAndOffCinderBlocks() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      String scriptName = "scripts/ExerciseAndJUnitScripts/TwoCinderBlocksStepOver_LeftFootTest.xml";

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.IN_FRONT_OF_TWO_HIGH_CINDERBLOCKS;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCObstacleCourseTrialsCinderBlocksTest", selectedLocation, simulationTestingParameters, getRobotModel());
      FullHumanoidRobotModel fullRobotModel = drcSimulationTestHelper.getControllerFullRobotModel();
      drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.001);
      InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream(scriptName);
      drcSimulationTestHelper.loadScriptFile(scriptInputStream, fullRobotModel.getSoleFrame(RobotSide.LEFT));

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();

      setupCameraForWalkingOverCinderBlocks();

      ThreadTools.sleep(0);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.1);

      YoBoolean doToeTouchdownIfPossible = (YoBoolean) simulationConstructionSet.getVariable("doToeTouchdownIfPossible");
      doToeTouchdownIfPossible.set(true);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(13.0);

      drcSimulationTestHelper.createVideo(getSimpleRobotName(), 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      Point3D center = new Point3D();
      Vector3D plusMinusVector = new Vector3D(0.2, 0.2, 0.5);
      BoundingBox3D boundingBox = BoundingBox3D.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
      drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   private void setupCameraForWalkingOverCinderBlocks()
   {
      Point3D cameraFix = new Point3D(13.5, 13.0, 0.75);
      Point3D cameraPosition = new Point3D(7.0, 17.0, 2.0);

      drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }
}