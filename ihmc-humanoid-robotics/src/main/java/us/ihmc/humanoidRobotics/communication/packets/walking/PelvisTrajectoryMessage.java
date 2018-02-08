package us.ihmc.humanoidRobotics.communication.packets.walking;

import java.util.Random;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.interfaces.QuaternionReadOnly;
import us.ihmc.humanoidRobotics.communication.packets.AbstractSE3TrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.PacketValidityChecker;

@RosMessagePacket(documentation =
      "This message commands the controller to move in taskspace the pelvis to the desired pose (position & orientation) while going through the specified trajectory points."
      + " A third order polynomial function is used to interpolate positions and a hermite based curve (third order) is used to interpolate the orientations."
      + " To excute a single straight line trajectory to reach a desired pelvis pose, set only one trajectory point with zero velocity and its time to be equal to the desired trajectory time."
      + " Note that the pelvis position is limited keep the robot's balance (center of mass has to remain inside the support polygon)."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE,
                  topic = "/control/pelvis_trajectory")
public class PelvisTrajectoryMessage extends Packet<PelvisTrajectoryMessage> implements VisualizablePacket, EpsilonComparable<PelvisTrajectoryMessage>
{
   private static final long WORLD_FRAME_HASH_CODE = ReferenceFrame.getWorldFrame().getNameBasedHashCode();

   public boolean enableUserPelvisControl = false;
   public boolean enableUserPelvisControlDuringWalking = false;
   @RosExportedField(documentation = "The position/orientation trajectory information.")
   public AbstractSE3TrajectoryMessage se3Trajectory;

   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public PelvisTrajectoryMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public PelvisTrajectoryMessage(Random random)
   {
      se3Trajectory = new AbstractSE3TrajectoryMessage(random);
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   /**
    * Clone constructor.
    * @param pelvisTrajectoryMessage message to clone.
    */
   public PelvisTrajectoryMessage(PelvisTrajectoryMessage pelvisTrajectoryMessage)
   {
      se3Trajectory = new AbstractSE3TrajectoryMessage(pelvisTrajectoryMessage.se3Trajectory);
      setEnableUserPelvisControl(pelvisTrajectoryMessage.isEnableUserPelvisControl());
      setEnableUserPelvisControlDuringWalking(pelvisTrajectoryMessage.isEnableUserPelvisControlDuringWalking());
      setDestination(pelvisTrajectoryMessage.getDestination());
      setUniqueId(pelvisTrajectoryMessage.getUniqueId());
   }

   /**
    * Use this constructor to execute a straight line trajectory in taskspace.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    * @param trajectoryTime how long it takes to reach the desired pose.
    * @param desiredPosition desired pelvis position expressed in world frame.
    * @param desiredOrientation desired pelvis orientation expressed in world frame.
    */
   public PelvisTrajectoryMessage(double trajectoryTime, Point3DReadOnly desiredPosition, QuaternionReadOnly desiredOrientation)
   {
      se3Trajectory = new AbstractSE3TrajectoryMessage(trajectoryTime, desiredPosition, desiredOrientation, ReferenceFrame.getWorldFrame());
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   /**
    * Use this constructor to build a message with more than one trajectory point.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    * This constructor only allocates memory for the trajectory points, you need to call {@link #setTrajectoryPoint(int, double, Point3DReadOnly, QuaternionReadOnly, Vector3DReadOnly, Vector3DReadOnly)} for each trajectory point afterwards.
    * @param numberOfTrajectoryPoints number of trajectory points that will be sent to the controller.
    */
   public PelvisTrajectoryMessage(int numberOfTrajectoryPoints)
   {
      se3Trajectory = new AbstractSE3TrajectoryMessage(numberOfTrajectoryPoints);
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public boolean isEnableUserPelvisControlDuringWalking()
   {
      return enableUserPelvisControlDuringWalking;
   }

   public void setEnableUserPelvisControlDuringWalking(boolean enableUserPelvisControlDuringWalking)
   {
      this.enableUserPelvisControlDuringWalking = enableUserPelvisControlDuringWalking;
   }

   public boolean isEnableUserPelvisControl()
   {
      return enableUserPelvisControl;
   }

   public void setEnableUserPelvisControl(boolean enableUserPelvisControl)
   {
      this.enableUserPelvisControl = enableUserPelvisControl;
   }

   public AbstractSE3TrajectoryMessage getSE3Trajectory()
   {
      return se3Trajectory;
   }

   @Override
   public boolean epsilonEquals(PelvisTrajectoryMessage other, double epsilon)
   {
      return se3Trajectory.epsilonEquals(other.se3Trajectory, epsilon);
   }

   @Override
   public String toString()
   {
      if (se3Trajectory.taskspaceTrajectoryPoints != null)
         return "Pelvis SE3 trajectory: number of SE3 trajectory points = " + se3Trajectory.getNumberOfTrajectoryPoints();
      else
         return "Pelvis SE3 trajectory: no SE3 trajectory points";
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validatePelvisTrajectoryMessage(this);
   }

   public final void setTrajectoryPoint(int trajectoryPointIndex, double time, Point3DReadOnly position, QuaternionReadOnly orientation, Vector3DReadOnly linearVelocity, Vector3DReadOnly angularVelocity)
   {
      se3Trajectory.setTrajectoryPoint(trajectoryPointIndex, time, position, orientation, linearVelocity, angularVelocity, WORLD_FRAME_HASH_CODE);
   }
}
