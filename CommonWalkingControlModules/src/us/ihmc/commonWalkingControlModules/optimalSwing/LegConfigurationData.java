package us.ihmc.commonWalkingControlModules.optimalSwing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.jfree.util.ArrayUtilities;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;

public class LegConfigurationData
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   
   
   private final List<LegJointName> jointsToInterpolate;
   private final List<LegJointName> jointsToOptimize;
   private final List<LegJointName> allJoints;
   
   private final EnumYoVariable<RobotSide> robotSide = new EnumYoVariable<RobotSide>("robotSide", registry, RobotSide.class);
   private final BooleanYoVariable currentlyInSwing = new BooleanYoVariable("currentlyInSwing", registry);
   private final DoubleYoVariable swingTimeRemaining = new DoubleYoVariable("swingTimeRemaining", registry);
   
   private final DoubleYoVariable hipRollHeight = new DoubleYoVariable("hipRollHeight", registry);
   
   private final EnumMap<LegJointName, DoubleYoVariable> currentJointAngles = new EnumMap<LegJointName, DoubleYoVariable>(LegJointName.class);
   private final EnumMap<LegJointName, DoubleYoVariable> currentJointVelocities = new EnumMap<LegJointName, DoubleYoVariable>(LegJointName.class);
   
   private final EnumMap<LegJointName, DoubleYoVariable> finalDesiredJointAngles = new EnumMap<LegJointName, DoubleYoVariable>(LegJointName.class);
   private final EnumMap<LegJointName, DoubleYoVariable> finalDesiredJointVelocities = new EnumMap<LegJointName, DoubleYoVariable>(LegJointName.class);
   
   private final YoFrameOrientation upperBodyOrientationInWorld = new YoFrameOrientation("upperBodyOrientation", "InWorld", ReferenceFrame.getWorldFrame(), registry);
   
   private final ArrayList<YoVariable> allVariables;

   public LegConfigurationData(LegJointName[] jointsToInterpolate, LegJointName[] jointsToOptimize, YoVariableRegistry parentRegistry)
   {
      this.jointsToInterpolate = Collections.unmodifiableList(Arrays.asList(jointsToInterpolate));
      this.jointsToOptimize = Collections.unmodifiableList(Arrays.asList(jointsToOptimize));
      
      ArrayList<LegJointName> allJointNames = new ArrayList<LegJointName>();
      allJointNames.addAll(Arrays.asList(jointsToInterpolate));
      allJointNames.addAll(Arrays.asList(jointsToOptimize));
      this.allJoints = Collections.unmodifiableList(allJointNames);
      
      
      for(LegJointName jointName : allJointNames)
      {
         currentJointAngles.put(jointName, new DoubleYoVariable("current"+jointName.getCamelCaseNameForMiddleOfExpression()+"Angle", registry));
         currentJointVelocities.put(jointName, new DoubleYoVariable("current"+jointName.getCamelCaseNameForMiddleOfExpression()+"Velocity", registry));
         
         finalDesiredJointAngles.put(jointName, new DoubleYoVariable("finalDesired"+jointName.getCamelCaseNameForMiddleOfExpression()+"Angle", registry));
         finalDesiredJointVelocities.put(jointName, new DoubleYoVariable("finalDesired"+jointName.getCamelCaseNameForMiddleOfExpression()+"Velocity", registry));
         
      }
      
      if(parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }
      
      allVariables = registry.getAllVariablesIncludingDescendants();

   }
   
   public RobotSide getRobotSide()
   {
      return robotSide.getEnumValue();
   }
   
   public void setRobotSide(RobotSide robotSide)
   {
      this.robotSide.set(robotSide);
   }
   
   public double getSwingTimeRemaining()
   {
      return swingTimeRemaining.getDoubleValue();
   }
   
   public void setSwingTimeRemaining(double swingTimeRemaining)
   {
      this.swingTimeRemaining.set(swingTimeRemaining);
   }
   
   public boolean iscurrentlyInSwing()
   {
      return currentlyInSwing.getBooleanValue();
   }
   
   public void setcurrentlyInSwing(boolean initializeSwing)
   {
      this.currentlyInSwing.set(initializeSwing);
   }
   
   public double getCurrentJointAngle(LegJointName jointName)
   {
      return currentJointAngles.get(jointName).getDoubleValue();
   }
   
   public void setCurrentJointAngle(LegJointName jointName, double value)
   {
      currentJointAngles.get(jointName).set(value);
   }
   
   
   public double getCurrentJointVelocity(LegJointName jointName)
   {
      return currentJointVelocities.get(jointName).getDoubleValue();
   }
   
   public void setCurrentJointVelocity(LegJointName jointName, double value)
   {
      currentJointVelocities.get(jointName).set(value);
   }
   
   
   public double getFinalDesiredJointAngle(LegJointName jointName)
   {
      return finalDesiredJointAngles.get(jointName).getDoubleValue();
   }
   
   public void setFinalDesiredJointAngle(LegJointName jointName, double value)
   {
      finalDesiredJointAngles.get(jointName).set(value);
   }
   
   
   public double getFinalDesiredJointVelocity(LegJointName jointName)
   {
      return finalDesiredJointVelocities.get(jointName).getDoubleValue();
   }
   
   public void setFinalDesiredJointVelocity(LegJointName jointName, double value)
   {
      finalDesiredJointVelocities.get(jointName).set(value);
   }
   
   public List<LegJointName> getJointsToOptimize()
   {
      return jointsToOptimize;
   }
   
   public List<LegJointName> getJointsToInterpolate()
   {
      return jointsToInterpolate;
   }
   
   public List<LegJointName> getAllJoints()
   {
      return allJoints;
   }
   
   public int getNumberOfJointsToOptimize()
   {
      return jointsToOptimize.size();
   }
   
   public int getNumberOfJointsToInterpolate()
   {
      return jointsToInterpolate.size();
   }
   
   public LegJointName getJointToOptimize(int index)
   {
      return jointsToOptimize.get(index);
   }
   
   public LegJointName getJointToInterpolate(int index)
   {
      return jointsToInterpolate.get(index);
   }
   
   public ArrayList<YoVariable> getAllVariables()
   {
      return allVariables;
   }

   public Orientation getUpperBodyOrientationInWorld()
   {
      return upperBodyOrientationInWorld.getFrameOrientationCopy();
   }
   
   public void setUpperBodyOrientationInWorld(Orientation orientation)
   {
      this.upperBodyOrientationInWorld.set(orientation);
   }

   public double getHipRollHeight()
   {
      return hipRollHeight.getDoubleValue();
   }
   
   public void setHipRollHeight(double value)
   {
      hipRollHeight.set(value);
   }

}
