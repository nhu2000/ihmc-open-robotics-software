package us.ihmc.robotics.math;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.IntegerYoVariable;

public class YoAverager
{
   private final YoDouble average;
   private final IntegerYoVariable nUpdates;

   public YoAverager(String prefix, YoVariableRegistry registry)
   {
      average = new YoDouble(prefix + "Average", registry);
      nUpdates = new IntegerYoVariable(prefix + "AverageNUpdates", registry);
   }

   public void update(double input)
   {
      int nUpdatesOld = nUpdates.getIntegerValue();
      nUpdates.increment();
      int nUpdatesNew = nUpdates.getIntegerValue();
      double ratio = ((double) nUpdatesOld) / ((double) nUpdatesNew);
      average.set(average.getDoubleValue() * ratio + input / nUpdates.getIntegerValue());
   }

   public double val()
   {
      return average.getDoubleValue();
   }

   public void reset()
   {
      nUpdates.set(0);
   }

   public YoDouble getAverageYoVariable()
   {
      return average;
   }
}
