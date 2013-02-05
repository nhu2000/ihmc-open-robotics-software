package us.ihmc.darpaRoboticsChallenge;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFPerfectSimulatedSensorReaderAndWriter;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonAvatarInterfaces.CommonAvatarEnvironmentInterface;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.CenterOfMassJacobianUpdater;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.sensors.TwistUpdater;
import us.ihmc.commonWalkingControlModules.visualizer.CommonInertiaElipsoidsVisualizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.sensors.PerfectFootswitch;
import us.ihmc.projectM.R2Sim02.initialSetup.GuiInitialSetup;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.projectM.R2Sim02.initialSetup.ScsInitialSetup;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.gui.GUISetterUpperRegistry;
import com.yobotics.simulationconstructionset.robotController.ModularRobotController;
import com.yobotics.simulationconstructionset.robotController.ModularSensorProcessor;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.robotController.SensorProcessor;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class DRCSimulationFactory
{
   public static boolean SHOW_INERTIA_ELLIPSOIDS = false;
   
   public static HumanoidRobotSimulation<SDFRobot> createSimulation(DRCRobotJointMap jointMap, ControllerFactory controllerFactory,
           CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface, RobotInitialSetup<SDFRobot> robotInitialSetup, ScsInitialSetup scsInitialSetup,
           GuiInitialSetup guiInitialSetup)
   {
      GUISetterUpperRegistry guiSetterUpperRegistry = new GUISetterUpperRegistry();
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      double simulateDT = scsInitialSetup.getDT();
      int simulationTicksPerControlTick = controllerFactory.getSimulationTicksPerControlTick();
      double controlDT = simulateDT * simulationTicksPerControlTick;

      DRCRobotSDFLoader drcRobotSDFLoader = new DRCRobotSDFLoader();
      JaxbSDFLoader jaxbSDFLoader = drcRobotSDFLoader.loadDRCRobot(jointMap);
      SDFRobot simulatedRobot = jaxbSDFLoader.getRobot();
      FullRobotModel fullRobotModelForSimulation = jaxbSDFLoader.getFullRobotModel();
     
//      drcRobotSDFLoader = new DRCRobotSDFLoader(robotModel);
//      jaxbSDFLoader = drcRobotSDFLoader.loadDRCRobot();
//      FullRobotModel fullRobotModelForController = new FullRobotModelWithUncertainty(jaxbSDFLoader.getFullRobotModel());
//      CommonWalkingReferenceFrames referenceFramesForController = jaxbSDFLoader.getReferenceFrames();

      FullRobotModel fullRobotModelForController = fullRobotModelForSimulation; 
      CommonWalkingReferenceFrames referenceFramesForController = jaxbSDFLoader.getReferenceFrames();

      SideDependentList<FootSwitchInterface> footSwitches = new SideDependentList<FootSwitchInterface>();
      for (RobotSide robotSide : RobotSide.values())
      {
         footSwitches.put(robotSide, new PerfectFootswitch(simulatedRobot, robotSide));
      }

      TwistCalculator twistCalculator = new TwistCalculator(ReferenceFrame.getWorldFrame(), fullRobotModelForController.getElevator());
      CenterOfMassJacobian centerOfMassJacobian = new CenterOfMassJacobian(fullRobotModelForController.getElevator());

      SDFPerfectSimulatedSensorReaderAndWriter sensorReaderAndOutputWriter = new SDFPerfectSimulatedSensorReaderAndWriter(simulatedRobot, fullRobotModelForController,
            referenceFramesForController);

      RobotController robotController = controllerFactory.getController(fullRobotModelForController, referenceFramesForController, controlDT, simulatedRobot.getYoTime(),
                                           dynamicGraphicObjectsListRegistry, guiSetterUpperRegistry, twistCalculator, centerOfMassJacobian, footSwitches);

      ModularRobotController modularRobotController = new ModularRobotController("ModularRobotController");
      modularRobotController.setRawSensorReader(sensorReaderAndOutputWriter);
      modularRobotController.setSensorProcessor(createSensorProcessor(twistCalculator, centerOfMassJacobian));
      modularRobotController.addRobotController(robotController);
      
      if (SHOW_INERTIA_ELLIPSOIDS)
      {
         modularRobotController.addRobotController(new CommonInertiaElipsoidsVisualizer(fullRobotModelForSimulation.getElevator(), dynamicGraphicObjectsListRegistry));
      }
      modularRobotController.setRawOutputWriter(sensorReaderAndOutputWriter);

      return new HumanoidRobotSimulation<SDFRobot>(simulatedRobot, modularRobotController, simulationTicksPerControlTick, fullRobotModelForSimulation, commonAvatarEnvironmentInterface,
                                         robotInitialSetup, scsInitialSetup, guiInitialSetup, guiSetterUpperRegistry, dynamicGraphicObjectsListRegistry);
   }

   private static SensorProcessor createSensorProcessor(TwistCalculator twistCalculator, CenterOfMassJacobian centerOfMassJacobian)
   {
      ModularSensorProcessor modularSensorProcessor = new ModularSensorProcessor("ModularSensorProcessor", "");
      modularSensorProcessor.addSensorProcessor(new TwistUpdater(twistCalculator));
      modularSensorProcessor.addSensorProcessor(new CenterOfMassJacobianUpdater(centerOfMassJacobian));

      return modularSensorProcessor;
   }
}
