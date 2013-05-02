package us.ihmc.sensorProcessing.stateEstimation;

import java.util.Set;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointPositionDataObject;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.utilities.math.geometry.FrameVector;

public interface StateEstimatorWithPorts extends StateEstimator
{
   public abstract ControlFlowGraph getControlFlowGraph();
   public abstract ControlFlowInputPort<FrameVector> getDesiredAngularAccelerationInputPort();
   public abstract ControlFlowInputPort<FrameVector> getDesiredCenterOfMassAccelerationInputPort();
   public abstract ControlFlowInputPort<Set<PointPositionDataObject>> getPointPositionInputPort();
   public abstract ControlFlowInputPort<Set<PointVelocityDataObject>> getPointVelocityInputPort();
}
