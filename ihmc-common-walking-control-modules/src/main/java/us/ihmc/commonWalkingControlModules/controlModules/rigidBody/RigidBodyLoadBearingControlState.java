package us.ihmc.commonWalkingControlModules.controlModules.rigidBody;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.PlaneContactStateCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicVector;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.AbstractLoadBearingCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.JointspaceTrajectoryCommand;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.robotics.screwTheory.SpatialAccelerationVector;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class RigidBodyLoadBearingControlState extends RigidBodyControlState
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final long NO_CONTACT_ID = 0L;
   private static final long IN_CONTACT_ID = 1L;
   private static final int dofs = Twist.SIZE;
   private static final FrameVector3D zeroInWorld = new FrameVector3D(worldFrame, 0.0, 0.0, 0.0);

   private final InverseDynamicsCommandList inverseDynamicsCommandList = new InverseDynamicsCommandList();
   private final FeedbackControlCommandList feedbackControlCommandList = new FeedbackControlCommandList();
   private final SpatialAccelerationCommand spatialAccelerationCommand = new SpatialAccelerationCommand();
   private final SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();
   private final PlaneContactStateCommand planeContactStateCommand = new PlaneContactStateCommand();

   private final SelectionMatrix6D accelerationSelectionMatrix = new SelectionMatrix6D();
   private final SelectionMatrix6D feedbackSelectionMatrix = new SelectionMatrix6D();
   private final boolean[] isDirectionFeedbackControlled = new boolean[dofs];

   private final FramePose bodyFixedControlledPose = new FramePose();
   private final SpatialAccelerationVector bodyAcceleration;

   // TODO: allow multiple contact points
   private final YoFramePoint contactPoint;
   private final YoFramePoint contactPointInWorld;

   private final ReferenceFrame bodyFrame;
   private final ReferenceFrame elevatorFrame;
   private final PoseReferenceFrame desiredContactFrame;

   private final ContactablePlaneBody contactableBody;
   private final YoDouble coefficientOfFriction;
   private final YoFrameVector contactNormal;
   private final ReferenceFrame contactFrame;

   private final RigidBodyTransform bodyToJointTransform = new RigidBodyTransform();
   private final RigidBodyTransform contactToJointTransform = new RigidBodyTransform();

   private final FramePoint3D desiredContactPosition = new FramePoint3D(worldFrame);
   private final FrameOrientation desiredContactOrientation = new FrameOrientation(worldFrame);
   private final FramePoint3D currentContactPosition = new FramePoint3D(worldFrame);
   private final FrameOrientation currentContactOrientation = new FrameOrientation(worldFrame);

   private final YoBoolean hybridModeActive;
   private final RigidBodyJointControlHelper jointControlHelper;

   public RigidBodyLoadBearingControlState(RigidBody bodyToControl, ContactablePlaneBody contactableBody, RigidBody elevator, YoDouble yoTime,
         RigidBodyJointControlHelper jointControlHelper, YoGraphicsListRegistry graphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      super(RigidBodyControlMode.LOADBEARING, bodyToControl.getName(), yoTime, parentRegistry);
      this.bodyFrame = bodyToControl.getBodyFixedFrame();
      this.elevatorFrame = elevator.getBodyFixedFrame();
      this.contactFrame = contactableBody.getSoleFrame();
      this.contactableBody = contactableBody;

      bodyFrame.getTransformToDesiredFrame(bodyToJointTransform, bodyToControl.getParentJoint().getFrameAfterJoint());

      bodyAcceleration = new SpatialAccelerationVector(contactFrame, elevatorFrame, contactFrame);
      spatialAccelerationCommand.set(elevator, bodyToControl);
      spatialFeedbackControlCommand.set(elevator, bodyToControl);

      String bodyName = bodyToControl.getName();
      coefficientOfFriction = new YoDouble(bodyName + "CoefficientOfFriction", registry);
      contactNormal = new YoFrameVector(bodyName + "ContactNormal", worldFrame, parentRegistry);
      contactPoint = new YoFramePoint(bodyName + "ContactPoint", contactFrame, parentRegistry);
      contactPointInWorld = new YoFramePoint(bodyName + "ContactPointInWorld", worldFrame, parentRegistry);
      desiredContactFrame = new PoseReferenceFrame(bodyName + "DesiredContactFrame", worldFrame);

      planeContactStateCommand.setContactingRigidBody(bodyToControl);
      planeContactStateCommand.setId(NO_CONTACT_ID);

      this.jointControlHelper = jointControlHelper;
      String prefix = bodyName + "Loadbearing";
      hybridModeActive = new YoBoolean(prefix + "HybridModeActive", registry);

      setupViz(graphicsListRegistry, bodyName);
   }

   private void setupViz(YoGraphicsListRegistry graphicsListRegistry, String bodyName)
   {
      if (graphicsListRegistry == null)
         return;

      String listName = getClass().getSimpleName();

      YoGraphicVector surfaceNormal = new YoGraphicVector(bodyName + "ContactNormal", contactPointInWorld, contactNormal, 0.1, YoAppearance.Black());
      graphicsListRegistry.registerYoGraphic(listName, surfaceNormal);
      graphics.add(surfaceNormal);

      YoGraphicPosition contactPoint = new YoGraphicPosition(bodyName + "ContactPoint", contactPointInWorld, 0.01, YoAppearance.Black());
      graphicsListRegistry.registerYoGraphic(listName, contactPoint);
      graphics.add(contactPoint);

      hideGraphics();
   }

   public void setWeights(Vector3D taskspaceAngularWeight, Vector3D taskspaceLinearWeight)
   {
      spatialFeedbackControlCommand.setWeightsForSolver(taskspaceAngularWeight, taskspaceLinearWeight);
   }

   public void setGains(YoPID3DGains taskspaceOrientationGains, YoPID3DGains taskspacePositionGains)
   {
      spatialFeedbackControlCommand.setOrientationGains(taskspaceOrientationGains);
      spatialFeedbackControlCommand.setPositionGains(taskspacePositionGains);
   }

   public void setCoefficientOfFriction(double coefficientOfFriction)
   {
      this.coefficientOfFriction.set(coefficientOfFriction);
   }

   public void setContactNormalInWorldFrame(Vector3D contactNormalInWorldFrame)
   {
      contactNormal.set(contactNormalInWorldFrame);
   }

   public void setAndUpdateContactFrame(RigidBodyTransform bodyToContactFrame)
   {
      contactToJointTransform.set(bodyToJointTransform);
      contactToJointTransform.multiply(bodyToContactFrame);
      contactableBody.setSoleFrameTransformFromParentJoint(contactToJointTransform);
      contactPoint.setToZero();
   }

   @Override
   public void doAction()
   {
      updateInternal();

      // assemble contact command
      planeContactStateCommand.clearContactPoints();
      planeContactStateCommand.setCoefficientOfFriction(coefficientOfFriction.getDoubleValue());
      planeContactStateCommand.setContactNormal(contactNormal.getFrameTuple());
      planeContactStateCommand.addPointInContact(contactPoint.getFrameTuple());
      planeContactStateCommand.setId(IN_CONTACT_ID);

      // assemble zero acceleration command
      bodyAcceleration.setToZero(contactFrame, elevatorFrame, contactFrame);
      bodyAcceleration.changeBodyFrameNoRelativeAcceleration(bodyFrame);
      spatialAccelerationCommand.setSpatialAcceleration(contactFrame, bodyAcceleration);
      spatialAccelerationCommand.setSelectionMatrix(accelerationSelectionMatrix);

      // assemble feedback control command
      bodyFixedControlledPose.setToZero(contactFrame);
      bodyFixedControlledPose.changeFrame(bodyFrame);
      spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(bodyFixedControlledPose);
      spatialFeedbackControlCommand.set(desiredContactPosition, zeroInWorld, zeroInWorld);
      spatialFeedbackControlCommand.set(desiredContactOrientation, zeroInWorld, zeroInWorld);
      spatialFeedbackControlCommand.setSelectionMatrix(feedbackSelectionMatrix);

      if (hybridModeActive.getBooleanValue())
      {
         double timeInTrajectory = getTimeInTrajectory();
         jointControlHelper.doAction(timeInTrajectory);
      }

      updateGraphics();
   }

   private void updateInternal()
   {
      // update current contact information
      currentContactPosition.setToZero(contactFrame);
      currentContactOrientation.setToZero(contactFrame);
      currentContactPosition.changeFrame(desiredContactPosition.getReferenceFrame());
      currentContactOrientation.changeFrame(desiredContactOrientation.getReferenceFrame());

      // TODO: figure out which directions to control based on support area
      // This requires the selection matrix in the command to be in contact frame.
      // For now we just hold the orientation and not the position of the contact point in world frame
      for (int i = 0; i < dofs; i++)
         isDirectionFeedbackControlled[i] = false;
      isDirectionFeedbackControlled[0] = true; // control x orientation
      isDirectionFeedbackControlled[1] = true; // control y orientation
      isDirectionFeedbackControlled[2] = true; // control z orientation
      desiredContactPosition.setX(currentContactPosition.getX()); // do not control x position
      desiredContactPosition.setY(currentContactPosition.getY()); // do not control y position
      desiredContactPosition.setZ(currentContactPosition.getZ()); // do not control z position

      // update things for visualization
      desiredContactPosition.checkReferenceFrameMatch(desiredContactFrame.getParent());
      desiredContactOrientation.checkReferenceFrameMatch(desiredContactFrame.getParent());
      desiredContactFrame.setPoseAndUpdate(desiredContactPosition, desiredContactOrientation);
      contactPointInWorld.setAndMatchFrame(contactPoint);

      // assemble the selection matrices for the controller core commands
      accelerationSelectionMatrix.resetSelection();
      feedbackSelectionMatrix.resetSelection();

      for (int i = dofs-1; i >= 0; i--)
      {
         if (isDirectionFeedbackControlled[i])
            accelerationSelectionMatrix.selectAxis(i, false);
         else
            feedbackSelectionMatrix.selectAxis(i, false);
      }
   }

   public boolean handleLoadbearingCommand(AbstractLoadBearingCommand<?, ?> command)
   {
      setCoefficientOfFriction(command.getCoefficientOfFriction());
      setContactNormalInWorldFrame(command.getContactNormalInWorldFrame());
      setAndUpdateContactFrame(command.getBodyFrameToContactFrame());
      return true;
   }

   public boolean handleJointTrajectoryCommand(JointspaceTrajectoryCommand<?, ?> command, double[] initialJointPositions)
   {
      if (jointControlHelper == null)
      {
         PrintTools.warn(warningPrefix + "Can not use hybrid mode. Was not created with a jointspace helper.");
         return false;
      }

      if (!handleCommandInternal(command))
      {
         return false;
      }

      if (!jointControlHelper.handleTrajectoryCommand(command, initialJointPositions))
      {
         return false;
      }

      hybridModeActive.set(true);
      return true;
   }

   @Override
   public void doTransitionIntoAction()
   {
      desiredContactPosition.setToZero(contactFrame);
      desiredContactOrientation.setToZero(contactFrame);

      desiredContactPosition.changeFrame(worldFrame);
      desiredContactOrientation.changeFrame(worldFrame);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      hideGraphics();
   }

   @Override
   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      inverseDynamicsCommandList.clear();
      inverseDynamicsCommandList.addCommand(spatialAccelerationCommand);
      inverseDynamicsCommandList.addCommand(planeContactStateCommand);
      return inverseDynamicsCommandList;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      if (hybridModeActive.getBooleanValue())
      {
         feedbackControlCommandList.clear();
         feedbackControlCommandList.addCommand(spatialFeedbackControlCommand);
         feedbackControlCommandList.addCommand(jointControlHelper.getJointspaceCommand());
         return feedbackControlCommandList;
      }

      return spatialFeedbackControlCommand;
   }

   @Override
   public FeedbackControlCommand<?> createFeedbackControlTemplate()
   {
      feedbackControlCommandList.clear();
      feedbackControlCommandList.addCommand(spatialFeedbackControlCommand);
      feedbackControlCommandList.addCommand(jointControlHelper.getJointspaceCommand());
      return feedbackControlCommandList;
   }

   @Override
   public InverseDynamicsCommand<?> getTransitionOutOfStateCommand()
   {
      planeContactStateCommand.clearContactPoints();
      planeContactStateCommand.setId(NO_CONTACT_ID);
      return planeContactStateCommand;
   }

   @Override
   public boolean isEmpty()
   {
      // this control mode does not support command queuing
      return false;
   }

   @Override
   public double getLastTrajectoryPointTime()
   {
      // this control mode does not support command queuing
      return 0.0;
   }

   @Override
   public void clear()
   {
      hybridModeActive.set(false);
   }
}