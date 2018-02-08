package us.ihmc.humanoidBehaviors.behaviors.roughTerrain;

import us.ihmc.commons.PrintTools;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage.RequestType;
import us.ihmc.communication.packets.TextToSpeechPacket;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridge;
import us.ihmc.humanoidRobotics.communication.packets.walking.*;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.*;
import us.ihmc.robotics.time.YoStopwatch;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.concurrent.atomic.AtomicReference;

public class WalkOverTerrainStateMachineBehavior extends AbstractBehavior
{
   enum WalkOverTerrainState
   {
      WAIT, PLAN_FOOTSTEPS, WALKING
   }

   private final StateMachine<WalkOverTerrainState> stateMachine;

   private final WaitState waitState;
   private final PlanFootstepsState planPathState;
   private final WalkingState walkingState;

   private final ReferenceFrame chestFrame, midFeetZUpFrame;
   private final AtomicReference<FramePose3D> goalPose = new AtomicReference<>();

   private final YoDouble swingTime = new YoDouble("swingTime", registry);
   private final YoDouble transferTime = new YoDouble("transferTime", registry);

   public WalkOverTerrainStateMachineBehavior(CommunicationBridge communicationBridge, YoDouble yoTime, HumanoidReferenceFrames referenceFrames)
   {
      super(communicationBridge);

      stateMachine = new StateMachine<>(getName() + "StateMachine", getName() + "StateMachineSwitchTime", WalkOverTerrainState.class, yoTime, registry);

      waitState = new WaitState(yoTime);
      planPathState = new PlanFootstepsState(communicationBridge, referenceFrames.getSoleFrames(), swingTime, registry);
      walkingState = new WalkingState(communicationBridge);

      this.chestFrame = referenceFrames.getChestFrame();
      this.midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();

      swingTime.set(1.5);
      transferTime.set(0.3);

      setupStateMachine();
   }

   private void setupStateMachine()
   {
      stateMachine.addState(waitState);
      stateMachine.addState(planPathState);
      stateMachine.addState(walkingState);

      StateTransitionAction planningToWalkingAction = () -> { waitState.hasWalkedBetweenWaiting.set(true); sendFootstepPlan(planPathState.getPlannerOutput()); };
      StateTransitionCondition planningToWalkingCondition = () -> planPathState.getPlannerOutput() != null && planPathState.getPlannerOutput().planningResult.validForExecution();
      StateTransitionCondition planningToWaitingCondition = () -> planPathState.getPlannerOutput() != null && !planPathState.getPlannerOutput().planningResult.validForExecution();

      planPathState.addStateTransition(new StateTransition<>(WalkOverTerrainState.WALKING, planningToWalkingCondition, planningToWalkingAction));
      planPathState.addStateTransition(WalkOverTerrainState.WAIT, planningToWaitingCondition);
      waitState.addStateTransition(WalkOverTerrainState.PLAN_FOOTSTEPS, waitState::isDoneWaiting);
      walkingState.addStateTransition(WalkOverTerrainState.PLAN_FOOTSTEPS, walkingState::stepHasCompleted);

      stateMachine.setCurrentState(WalkOverTerrainState.PLAN_FOOTSTEPS);
   }

   @Override
   public void onBehaviorEntered()
   {
      PrintTools.info("Starting walk over terrain behavior");
      sendTextToSpeechPacket("Starting walk over terrain behavior");
   }

   @Override
   public void onBehaviorExited()
   {
      stateMachine.setCurrentState(WalkOverTerrainState.PLAN_FOOTSTEPS);
   }

   @Override
   public void doControl()
   {
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   @Override
   public void onBehaviorAborted()
   {

   }

   @Override
   public void onBehaviorPaused()
   {

   }

   @Override
   public void onBehaviorResumed()
   {

   }

   @Override
   public boolean isDone()
   {
      FramePose3D goalPoseInMidFeetZUpFrame = new FramePose3D(goalPose.get());
      goalPoseInMidFeetZUpFrame.changeFrame(midFeetZUpFrame);
      double goalXYDistance = EuclidGeometryTools.pythagorasGetHypotenuse(goalPoseInMidFeetZUpFrame.getX(), goalPoseInMidFeetZUpFrame.getY());
      double yawFromGoal = Math.abs(EuclidCoreTools.trimAngleMinusPiToPi(goalPoseInMidFeetZUpFrame.getYaw()));
      return goalXYDistance < 0.2 && yawFromGoal < Math.toRadians(25.0);
   }

   class WaitState extends State<WalkOverTerrainState>
   {
      private static final double initialWaitTime = 5.0;

      private final YoDouble waitTime = new YoDouble("waitTime", registry);
      private final YoBoolean hasWalkedBetweenWaiting = new YoBoolean("hasWalkedBetweenWaiting", registry);
      private final YoStopwatch stopwatch;

      WaitState(YoDouble yoTime)
      {
         super(WalkOverTerrainState.WAIT);

         stopwatch = new YoStopwatch("waitStopWatch", yoTime, registry);
         stopwatch.start();
         waitTime.set(initialWaitTime);
      }

      @Override
      public void doAction()
      {

      }

      @Override
      public void doTransitionIntoAction()
      {
         lookDown();
         clearPlanarRegionsList();

         stopwatch.reset();

         if(hasWalkedBetweenWaiting.getBooleanValue())
         {
            waitTime.set(initialWaitTime);
            hasWalkedBetweenWaiting.set(false);
         }
         else
         {
            waitTime.set(2.0 * waitTime.getDoubleValue());
         }

         sendTextToSpeechPacket("Waiting for " + waitTime.getDoubleValue() + " seconds");
      }

      private void lookDown()
      {
         AxisAngle orientationAxisAngle = new AxisAngle(0.0, 1.0, 0.0, Math.PI / 2.0);
         Quaternion headOrientation = new Quaternion();
         headOrientation.set(orientationAxisAngle);
         HeadTrajectoryMessage headTrajectoryMessage = new HeadTrajectoryMessage(1.0, headOrientation, ReferenceFrame.getWorldFrame(), chestFrame);
         headTrajectoryMessage.setDestination(PacketDestination.CONTROLLER);
         sendPacket(headTrajectoryMessage);
      }

      private void clearPlanarRegionsList()
      {
         RequestPlanarRegionsListMessage requestPlanarRegionsListMessage = new RequestPlanarRegionsListMessage(RequestType.CLEAR);
         requestPlanarRegionsListMessage.setDestination(PacketDestination.REA_MODULE);
         sendPacket(requestPlanarRegionsListMessage);
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }

      boolean isDoneWaiting()
      {
         return stopwatch.totalElapsed() >= waitTime.getDoubleValue();
      }
   }

   class WalkingState extends State<WalkOverTerrainState>
   {
      private final AtomicReference<FootstepStatus> footstepStatusMessage = new AtomicReference<>();

      WalkingState(CommunicationBridge communicationBridge)
      {
         super(WalkOverTerrainState.WALKING);
         communicationBridge.attachListener(FootstepStatus.class, footstepStatusMessage::set);
      }

      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
         sendTextToSpeechPacket("Walking");
         // TODO adjust com based on upcoming footsteps
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }

      boolean stepHasCompleted()
      {
         FootstepStatus footstepStatus = this.footstepStatusMessage.getAndSet(null);
         return (footstepStatus != null) && (footstepStatus.status == FootstepStatus.Status.COMPLETED);
      }
   }

   private void sendFootstepPlan(FootstepPlanningToolboxOutputStatus outputStatus)
   {
      FootstepDataListMessage footstepDataListMessage = outputStatus.footstepDataList;
      footstepDataListMessage.setDefaultSwingDuration(swingTime.getValue());
      footstepDataListMessage.setDefaultTransferDuration(transferTime.getDoubleValue());

      footstepDataListMessage.setDestination(PacketDestination.CONTROLLER);
      communicationBridge.sendPacket(footstepDataListMessage);
   }

   private void sendTextToSpeechPacket(String text)
   {
      sendPacketToUI(new TextToSpeechPacket(text));
   }
}
