package us.ihmc.commonWalkingControlModules.controlModules.spine;

import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.SpineControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.SpineJointName;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.SpineTorques;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.Wrench;

import com.mathworks.jama.Matrix;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.AxisAngleOrientationController;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class SpineOrientationInHeadingFrameControlModule implements SpineControlModule
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final ProcessedSensorsInterface processedSensors;
   private final ReferenceFrame bodyFrame;
   private final FrameVector desiredChestAngularVelocity;
   private final FrameVector chestAngularVelocity;
   private final YoFrameVector tauChest;
   private final Wrench chestWrench;

   private final AxisAngleOrientationController axisAngleOrientationController;
   private final DesiredHeadingControlModule desiredHeadingControlModule;
   private final GeometricJacobian spineJacobian;

   private final DoubleYoVariable desiredPitch = new DoubleYoVariable("desiredChestPitch", registry);
   private final DoubleYoVariable desiredRoll = new DoubleYoVariable("desiredChestRoll", registry);
   

   public SpineOrientationInHeadingFrameControlModule(ProcessedSensorsInterface processedSensors, DesiredHeadingControlModule desiredHeadingControlModule,
           ReferenceFrame chestFrame, GeometricJacobian spineJacobian, YoVariableRegistry parentRegistry)
   {
      this.processedSensors = processedSensors;
      this.bodyFrame = chestFrame;
      this.desiredChestAngularVelocity = new FrameVector(bodyFrame);
      this.chestAngularVelocity = new FrameVector(bodyFrame);

      this.tauChest = new YoFrameVector("tauChest", "", bodyFrame, registry);
      this.chestWrench = new Wrench(bodyFrame, bodyFrame);

      this.axisAngleOrientationController = new AxisAngleOrientationController("chest", bodyFrame, parentRegistry);
      this.desiredHeadingControlModule = desiredHeadingControlModule;
      
      this.spineJacobian = spineJacobian;
      
      setGains();
      parentRegistry.addChild(registry);
   }

   public void doSpineControl(SpineTorques spineTorquesToPack)
   {
      spineTorquesToPack.setTorquesToZero();
     
      ReferenceFrame desiredHeadingFrame = desiredHeadingControlModule.getDesiredHeadingFrame();
      Orientation desiredChestOrientation = new Orientation(desiredHeadingFrame);
      desiredChestOrientation.setYawPitchRoll(0.0, desiredPitch.getDoubleValue(), desiredRoll.getDoubleValue());
      desiredChestOrientation.changeFrame(bodyFrame);
      chestAngularVelocity.set(processedSensors.getChestAngularVelocityInChestFrame());
      
      FrameVector chestTorque = new FrameVector(bodyFrame);
      axisAngleOrientationController.compute(chestTorque, desiredChestOrientation, desiredChestAngularVelocity, chestAngularVelocity);
      
      tauChest.set(chestTorque);
      chestWrench.setAngularPart(chestTorque.getVector());

      spineJacobian.compute();
      Matrix spineJointTorques = spineJacobian.computeJointTorques(chestWrench);
      // FIXME: magic numbers for indices: 
      spineTorquesToPack.setTorque(SpineJointName.SPINE_PITCH, spineJointTorques.get(2, 0));
      spineTorquesToPack.setTorque(SpineJointName.SPINE_YAW, spineJointTorques.get(1, 0));
      spineTorquesToPack.setTorque(SpineJointName.SPINE_ROLL, spineJointTorques.get(0, 0));
   }

   private void setGains()
   {
      axisAngleOrientationController.setProportionalGains(1500.0, 1500.0, 1500.0);
      axisAngleOrientationController.setDerivativeGains(200.0, 150.0, 50.0);
   }
}
