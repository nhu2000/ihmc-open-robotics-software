package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.SpatialForceVector;

import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPolygon;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition.GraphicType;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameOrientation;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class GroundReactionWrenchDistributorVisuzalizer
{
   private static final double FORCE_VECTOR_SCALE = 0.003;
   private static final double MOMENT_VECTOR_SCALE = 0.01;
   private static final double COM_VIZ_RADIUS = 0.1;
   private static final double COP_VIZ_RADIUS = 0.05;

   private final YoVariableRegistry registry = new YoVariableRegistry("GroundReactionWrenchDistributorVisuzalizer");
  
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoFramePoint centerOfMassWorld = new YoFramePoint("centerOfMass", "", worldFrame, registry);
   private final YoFrameVector totalForceWorld = new YoFrameVector("totalForce", worldFrame, registry);
   private final YoFrameVector totalMomentWorld = new YoFrameVector("totalMoment", worldFrame, registry);
   
//   private final ArrayList<YoFrameConvexPolygon2d> contactPolygonsWorld = new ArrayList<YoFrameConvexPolygon2d>();
   private final ArrayList<YoFramePoint> contactReferencePoints = new ArrayList<YoFramePoint>();
   private final ArrayList<YoFrameOrientation> contactPlaneOrientations = new ArrayList<YoFrameOrientation>();
   private final ArrayList<DynamicGraphicPolygon> dynamicGraphicPolygons = new ArrayList<DynamicGraphicPolygon>();
   
   private final ArrayList<YoFramePoint> contactCenterOfPressures = new ArrayList<YoFramePoint>();
   private final ArrayList<YoFrameVector> contactForces = new ArrayList<YoFrameVector>();
   private final ArrayList<YoFrameVector> contactMoments = new ArrayList<YoFrameVector>();
   
   public GroundReactionWrenchDistributorVisuzalizer(int maxNumberOfFeet, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      DynamicGraphicObjectsList dynamicGraphicObjectsList = new DynamicGraphicObjectsList("GroundReactionWrenchDistributorVisuzalizer");

      for (int i=0; i<maxNumberOfFeet; i++)
      {
//         YoFrameConvexPolygon2d contactPolygon = new YoFrameConvexPolygon2d("contact" + i, "", worldFrame, maxNumberOfVertices, registry);
//         contactPolygonsWorld.add(contactPolygon);

         YoFramePoint contactReferencePoint = new YoFramePoint("contactReference" + i, worldFrame, registry);
         contactReferencePoints.add(contactReferencePoint);

         YoFrameOrientation contactPlaneOrientation = new YoFrameOrientation("contactOrientation" + i, worldFrame, registry);
         contactPlaneOrientations.add(contactPlaneOrientation);

         DynamicGraphicPolygon dynamicGraphicPolygon = new DynamicGraphicPolygon("contactPolygon"+i, null, contactReferencePoint, contactPlaneOrientation, 1.0, YoAppearance.Green());
         dynamicGraphicPolygons.add(dynamicGraphicPolygon);
         dynamicGraphicObjectsList.add(dynamicGraphicPolygon);
         
         YoFramePoint contactCenterOfPressure = new YoFramePoint("contactCoP"+i, worldFrame, registry);
         YoFrameVector contactForce = new YoFrameVector("contactForce" + i, worldFrame, registry);
         YoFrameVector contactMoment = new YoFrameVector("contactMoment" + i, worldFrame, registry);
         
         contactCenterOfPressures.add(contactCenterOfPressure);
         contactForces.add(contactForce);
         contactMoments.add(contactMoment);
         
         DynamicGraphicPosition contactCoPViz = new DynamicGraphicPosition("contactCoPViz" + i, contactCenterOfPressure, COP_VIZ_RADIUS, YoAppearance.Brown());
         dynamicGraphicObjectsList.add(contactCoPViz);
         
         DynamicGraphicVector contactForceViz = new DynamicGraphicVector("contactForceViz" + i, contactCenterOfPressure, contactForce, FORCE_VECTOR_SCALE, YoAppearance.Pink());
         dynamicGraphicObjectsList.add(contactForceViz);
         
         DynamicGraphicVector contactMomentViz = new DynamicGraphicVector("contactMomentViz" + i, contactCenterOfPressure, contactMoment, MOMENT_VECTOR_SCALE, YoAppearance.Orange());
         dynamicGraphicObjectsList.add(contactMomentViz);

      }

      DynamicGraphicPosition centerOfMassWorldViz = new DynamicGraphicPosition("centerOfMassViz", centerOfMassWorld, COM_VIZ_RADIUS, YoAppearance.Purple(), GraphicType.BALL_WITH_CROSS);
      DynamicGraphicVector totalForceWorldViz = new DynamicGraphicVector("totalForceViz", centerOfMassWorld, totalForceWorld, FORCE_VECTOR_SCALE, YoAppearance.Yellow());

      dynamicGraphicObjectsList.add(centerOfMassWorldViz);
      dynamicGraphicObjectsList.add(totalForceWorldViz);
      
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(dynamicGraphicObjectsList);
      
      parentRegistry.addChild(registry);
   }
   
   private int index = 0;
   public void update(SimulationConstructionSet scs, GroundReactionWrenchDistributorInterface distributor, ReferenceFrame centerOfMassFrame, ArrayList<PlaneContactState> contactStates, SpatialForceVector totalBodyWrench)
   {
      FramePoint centerOfMassPosition = new FramePoint(centerOfMassFrame);
      centerOfMassPosition.changeFrame(worldFrame);
      
      centerOfMassWorld.set(centerOfMassPosition);
      
      SpatialForceVector totalWrenchOnCenterOfMass = new SpatialForceVector(totalBodyWrench);
      totalWrenchOnCenterOfMass.changeFrame(centerOfMassFrame);
      FrameVector totalForceOnCenterOfMass = totalWrenchOnCenterOfMass.getLinearPartAsFrameVectorCopy();
      FrameVector totalTorqueOnCenterOfMass = totalWrenchOnCenterOfMass.getAngularPartAsFrameVectorCopy();
      totalForceOnCenterOfMass.changeFrame(worldFrame);
      totalTorqueOnCenterOfMass.changeFrame(worldFrame);
      
      totalForceWorld.set(totalForceOnCenterOfMass);
      totalMomentWorld.set(totalTorqueOnCenterOfMass);
      
      for (int i=0; i<contactStates.size(); i++)
      {
         PlaneContactState contactState = contactStates.get(i);
         
         List<FramePoint2d> contactPoints = contactState.getContactPoints2d();
         FrameConvexPolygon2d frameConvexPolygon2d = new FrameConvexPolygon2d(contactPoints);
         
//         DynamicGraphicPolygon dynamicGraphicPolygon = dynamicGraphicPolygons.get(i);
//         dynamicGraphicPolygon.updateConvexPolygon2d(frameConvexPolygon2d.getConvexPolygon2d());
         
         
         // For now make a new polygon on each update:
         DynamicGraphicPolygon dynamicGraphicPolygon = new DynamicGraphicPolygon("hackyPolygon"+index, frameConvexPolygon2d.getConvexPolygon2d(), contactReferencePoints.get(i), contactPlaneOrientations.get(i), 1.0, YoAppearance.Green());
         scs.addDynamicGraphicObject(dynamicGraphicPolygon);
         
         
         YoFramePoint contactCenterOfPressureForViz = contactCenterOfPressures.get(i);
         YoFrameVector contactForceForViz = contactForces.get(i);
         YoFrameVector contactMomentForViz = contactMoments.get(i);
         
         FramePoint2d centerOfPressure2d = distributor.getCenterOfPressure(contactState);
         
         FramePoint centerOfPressure = new FramePoint(centerOfPressure2d.getReferenceFrame(), centerOfPressure2d.getX(), centerOfPressure2d.getY(), 0.0);
         centerOfPressure.changeFrame(worldFrame);
         contactCenterOfPressureForViz.set(centerOfPressure);
         
         FrameVector contactForce = distributor.getForce(contactState);
         contactForceForViz.set(contactForce.changeFrameCopy(worldFrame));
         
         double normalTorque = distributor.getNormalTorque(contactState);
         FrameVector normalForce = new FrameVector(centerOfPressure2d.getReferenceFrame(), 0.0, 0.0, normalTorque);
         contactMomentForViz.set(normalForce.changeFrameCopy(worldFrame));

      }
      
      scs.tickAndUpdate();
   }
    
   
   public static void main(String[] args)
   {
      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      Robot nullRobot = new Robot("null");
      SimulationConstructionSet scs = new SimulationConstructionSet(nullRobot);

      int maxNumberOfFeet = 4;
      int maxNumberOfVertices = 10;
      GroundReactionWrenchDistributorVisuzalizer visualizer = new GroundReactionWrenchDistributorVisuzalizer(maxNumberOfFeet, scs.getRootRegistry(), dynamicGraphicObjectsListRegistry);
      
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      
      ReferenceFrame centerOfMassFrame = new PoseReferenceFrame("centerOfMass", worldFrame);
      FrameVector gravitationalAcceleration = new FrameVector(worldFrame, 0.0, 0.0, -9.81);
      double mass = 100.0;
      int nSupportVectors = 4;

      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);
      scs.startOnAThread();
      YoVariableRegistry parentRegistry = scs.getRootRegistry();
            
      
      GroundReactionWrenchDistributorInterface distributor = new LeeGoswamiGroundReactionWrenchDistributor(centerOfMassFrame, nSupportVectors, parentRegistry);
      ArrayList<PlaneContactState> contactStates = new ArrayList<PlaneContactState>();
      
      while(true)
      {
         SpatialForceVector desiredNetSpatialForceVector = new SpatialForceVector(worldFrame);
         distributor.solve(desiredNetSpatialForceVector);
         
         visualizer.update(scs, distributor, centerOfMassFrame, contactStates, desiredNetSpatialForceVector);
         scs.tickAndUpdate();
         ThreadTools.sleep(100L);
      }
   }
   
}
