package us.ihmc.quadrupedRobotics.planning;

import javax.vecmath.Point3d;

import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.quadrupedRobotics.util.TimeIntervalProvider;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class QuadrupedTimedStep implements TimeIntervalProvider
{
   private RobotQuadrant robotQuadrant;
   private double groundClearance;
   private Point3d goalPosition;
   private final TimeInterval timeInterval;

   public QuadrupedTimedStep()
   {
      this.robotQuadrant = RobotQuadrant.FRONT_RIGHT;
      this.groundClearance = 0.0;
      this.goalPosition = new Point3d(0.0, 0.0, 0.0);
      this.timeInterval = new TimeInterval(0.5, 1.0);
   }

   public QuadrupedTimedStep(RobotQuadrant robotQuadrant, FramePoint goalPosition, double groundClearance, TimeInterval timeInterval)
   {
      this();
      setRobotQuadrant(robotQuadrant);
      setGoalPosition(goalPosition);
      setGroundClearance(groundClearance);
      setTimeInterval(timeInterval);
   }

   public QuadrupedTimedStep(RobotQuadrant robotQuadrant, Point3d goalPosition, double groundClearance, TimeInterval timeInterval)
   {
      this();
      setRobotQuadrant(robotQuadrant);
      setGoalPosition(goalPosition);
      setGroundClearance(groundClearance);
      setTimeInterval(timeInterval);
   }

   public QuadrupedTimedStep(QuadrupedTimedStep other)
   {
      this(other.getRobotQuadrant(), other.getGoalPosition(), other.getGroundClearance(), other.getTimeInterval());
   }

   public void set(QuadrupedTimedStep other)
   {
      setRobotQuadrant(other.getRobotQuadrant());
      setGoalPosition(other.getGoalPosition());
      setGroundClearance(other.getGroundClearance());
      setTimeInterval(other.getTimeInterval());
   }

   public void get(QuadrupedTimedStep other)
   {
      other.setRobotQuadrant(getRobotQuadrant());
      other.setGoalPosition(getGoalPosition());
      other.setGroundClearance(getGroundClearance());
      other.setTimeInterval(getTimeInterval());
   }

   public RobotQuadrant getRobotQuadrant()
   {
      return robotQuadrant;
   }

   public TimeInterval getTimeInterval()
   {
      return timeInterval;
   }

   public void getTimeInterval(TimeInterval timeInterval)
   {
      this.timeInterval.get(timeInterval);
   }

   public void setTimeInterval(TimeInterval timeInterval)
   {
      this.timeInterval.set(timeInterval);
   }

   public void setRobotQuadrant(RobotQuadrant robotQuadrant)
   {
      this.robotQuadrant = robotQuadrant;
   }

   public Point3d getGoalPosition()
   {
      return goalPosition;
   }

   public void getGoalPosition(Point3d goalPosition)
   {
      this.goalPosition.get(goalPosition);
   }

   public void getGoalPosition(FramePoint goalPosition)
   {
      ReferenceFrame originalFrame = goalPosition.getReferenceFrame();
      goalPosition.changeFrame(ReferenceFrame.getWorldFrame());
      goalPosition.setPoint(this.goalPosition);
      goalPosition.changeFrame(originalFrame);
   }

   public void setGoalPosition(Point3d goalPosition)
   {
      this.goalPosition.set(goalPosition);
   }

   public void setGoalPosition(FramePoint goalPosition)
   {
      ReferenceFrame originalFrame = goalPosition.getReferenceFrame();
      goalPosition.changeFrame(ReferenceFrame.getWorldFrame());
      goalPosition.getPoint(this.goalPosition);
      goalPosition.changeFrame(originalFrame);
   }

   public double getGroundClearance()
   {
      return groundClearance;
   }

   public void setGroundClearance(double groundClearance)
   {
      this.groundClearance = groundClearance;
   }

   public boolean epsilonEquals(QuadrupedTimedStep other, double epsilon)
   {
      return getRobotQuadrant() == other.getRobotQuadrant() &&
             getGoalPosition().epsilonEquals(other.getGoalPosition(), epsilon) &&
             MathTools.epsilonEquals(getGroundClearance(), other.getGroundClearance(), epsilon) &&
             getTimeInterval().epsilonEquals(other.getTimeInterval(), epsilon);

   }

   @Override public String toString()
   {
      String string = super.toString();
      string += "\nrobotQuadrant: " + robotQuadrant;
      string += "\ngoalPosition:" + goalPosition;
      string += "\ngroundClearance: " + groundClearance;
      string += "\nstartTime: " + timeInterval.getStartTime();
      string += "\nendTime: " + timeInterval.getEndTime();
      return string;
   }
}

